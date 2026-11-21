package net.mcreator.modespiritismominecraft.espiritismo.itens.teste;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.core.Direction;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.espiritismo.itens.Fluidificacao;
import net.mcreator.modespiritismominecraft.espiritismo.itens.ItensApi;
import net.mcreator.modespiritismominecraft.espiritismo.itens.JarraAguaBlock;
import net.mcreator.modespiritismominecraft.espiritismo.itens.Obras;
import net.mcreator.modespiritismominecraft.espiritismo.itens.RegistroItens;
import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModItems;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** GameTests da área "itens" (registrados só com GameTest habilitado). */
@EventBusSubscriber
public final class TestesItens {
	private static final Identifier ESTRUTURA_VAZIA = Identifier.withDefaultNamespace("empty");

	private record Teste(int maxTicks, Consumer<GameTestHelper> funcao) {
	}

	private static final Map<String, Teste> TESTES = new LinkedHashMap<>();

	static {
		TESTES.put("itens_registrados", new Teste(20, TestesItens::registrados));
		TESTES.put("itens_obras_tem_paginas", new Teste(20, TestesItens::obras));
		TESTES.put("itens_receitas_carregadas", new Teste(20, TestesItens::receitas));
		TESTES.put("itens_jarra_fluidificacao", new Teste(20, TestesItens::jarra));
		TESTES.put("itens_doacoes", new Teste(20, TestesItens::doacoes));
	}

	private TestesItens() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, path);
	}

	@SubscribeEvent
	public static void registrarFuncoes(RegisterEvent event) {
		if (!GameTestHooks.isGametestEnabled())
			return;
		event.register(Registries.TEST_FUNCTION, helper -> TESTES.forEach((nome, t) -> helper.register(id(nome), t.funcao())));
	}

	@SubscribeEvent
	public static void registrarTestes(RegisterGameTestsEvent event) {
		Holder<TestEnvironmentDefinition<?>> ambiente = event.registerEnvironment(id("itens_padrao"));
		TESTES.forEach((nome, t) -> event.registerTest(id(nome), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(nome)),
				new TestData<>(ambiente, ESTRUTURA_VAZIA, t.maxTicks(), 0, true))));
	}

	static final String[] BLOCOS = {"livro_1857", "mesa_mediunica", "tribuna", "jarra_agua"};
	static final String[] ITENS = {"o_livro_dos_espiritos", "o_livro_dos_mediuns", "o_evangelho_segundo_o_espiritismo", "o_ceu_e_o_inferno", "a_genese",
			"nosso_lar", "o_que_e_o_espiritismo", "revista_espirita", "caderno_psicografia", "sopa_fraterna", "agasalho", "cesta_basica"};

	private static void registrados(GameTestHelper helper) {
		for (String b : BLOCOS) {
			helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id(b)), "bloco " + b + " não registrado");
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(b)), "item do bloco " + b + " não registrado");
		}
		for (String i : ITENS)
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(i)), "item " + i + " não registrado");
		helper.assertTrue(NeoForgeRegistries.ATTACHMENT_TYPES.containsKey(id("preces_recentes")), "attachment preces_recentes não registrado");
		// Os blocos não são cheios e giram com FACING.
		helper.setBlock(new BlockPos(1, 1, 1), RegistroItens.TRIBUNA.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
		var estado = helper.getBlockState(new BlockPos(1, 1, 1));
		helper.assertTrue(estado.getValue(HorizontalDirectionalBlock.FACING) == Direction.EAST, "FACING da tribuna");
		helper.assertFalse(estado.isCollisionShapeFullBlock(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))), "tribuna não deveria ser bloco cheio");
		helper.setBlock(new BlockPos(2, 1, 1), RegistroItens.LIVRO_1857.get().defaultBlockState());
		var forma = helper.getBlockState(new BlockPos(2, 1, 1)).getShape(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 1)));
		helper.assertTrue(forma.max(Direction.Axis.Y) <= 2.0 / 16 + 1e-6, "livro_1857 deveria ser baixinho (2 px)");
		helper.succeed();
	}

	private static void obras(GameTestHelper helper) {
		helper.assertValueEqual(Obras.OBRAS.size(), 8, "quantidade de obras");
		for (String idObra : Obras.OBRAS.keySet()) {
			ItemStack livro = Obras.criar(idObra);
			helper.assertFalse(livro.isEmpty(), "Obras.criar(" + idObra + ") vazio");
			WrittenBookContent c = livro.get(DataComponents.WRITTEN_BOOK_CONTENT);
			helper.assertTrue(c != null, idObra + " sem WRITTEN_BOOK_CONTENT");
			helper.assertTrue(c.pages().size() >= 4, idObra + " com poucas páginas: " + c.pages().size());
			helper.assertFalse(c.author().isBlank(), idObra + " sem autor");
			for (var p : c.pages())
				helper.assertTrue(p.raw().getString().length() <= 260, idObra + ": página longa demais");
			helper.assertTrue(livro.is(ItemTags.LECTERN_BOOKS), idObra + " não vai no atril");
		}
		helper.assertTrue(Obras.criar("nao_existe").isEmpty(), "id desconhecido deveria dar EMPTY");
		helper.succeed();
	}

	private static void receitas(GameTestHelper helper) {
		var gerenciador = helper.getLevel().getServer().getRecipeManager();
		for (String r : new String[]{"livro_1857", "mesa_mediunica", "tribuna", "jarra_agua", "o_livro_dos_espiritos", "o_livro_dos_mediuns",
				"o_evangelho_segundo_o_espiritismo", "o_ceu_e_o_inferno", "a_genese", "nosso_lar", "o_que_e_o_espiritismo", "revista_espirita",
				"caderno_psicografia", "sopa_fraterna", "agasalho", "cesta_basica"})
			helper.assertTrue(gerenciador.byKey(ResourceKey.create(Registries.RECIPE, id("itens_" + r))).isPresent(), "receita itens_" + r + " não carregou");
		helper.succeed();
	}

	private static void jarra(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Bem acima do grid (preces valem para o nível inteiro num raio de 8 blocos).
		BlockPos jarra = helper.absolutePos(BlockPos.ZERO).offset(0, 140, 0);
		level.setBlockAndUpdate(jarra, RegistroItens.JARRA_AGUA.get().defaultBlockState());
		Player p = helper.makeMockPlayer(GameType.SURVIVAL);
		p.setPos(jarra.getX() + 0.5, jarra.getY(), jarra.getZ() + 1.5);

		helper.assertFalse(Fluidificacao.podeFluidificar(level, jarra), "pré-condição: sem prece por perto");
		ItemStack comum = JarraAguaBlock.encher(level, jarra, p);
		PotionContents pc = comum.get(DataComponents.POTION_CONTENTS);
		helper.assertTrue(comum.is(Items.POTION) && pc != null && pc.is(Potions.WATER), "sem prece deveria dar água comum: " + comum);

		// Moralidade baixa: a prece do jogador não fluidifica.
		Moralidade.set(p, 30);
		helper.assertFalse(Fluidificacao.registrarPrece(p), "prece com moralidade 30 não deveria contar");
		helper.assertFalse(Fluidificacao.podeFluidificar(level, jarra), "ainda sem fluidificação");

		Fluidificacao.registrarPrece(level, jarra.offset(3, 0, 0));
		ItemStack fluidificada = JarraAguaBlock.encher(level, jarra, p);
		helper.assertTrue(fluidificada.is(ModEspiritismoMinecraftModItems.AGUA_FLUIDIFICADA.get()), "com prece deveria dar água fluidificada: " + fluidificada);

		// Recarga por jogador: a segunda garrafa logo em seguida sai com água comum (não há estoque ilimitado).
		ItemStack segunda = JarraAguaBlock.encher(level, jarra, p);
		PotionContents pc2 = segunda.get(DataComponents.POTION_CONTENTS);
		helper.assertTrue(segunda.is(Items.POTION) && pc2 != null && pc2.is(Potions.WATER), "2ª garrafa na recarga deveria ser água comum: " + segunda);
		// Outro jogador não é afetado pela recarga do primeiro.
		Player outroJogador = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertTrue(JarraAguaBlock.encher(level, jarra, outroJogador).is(ModEspiritismoMinecraftModItems.AGUA_FLUIDIFICADA.get()), "recarga é por jogador");
		// Passada a recarga, volta a fluidificar.
		Moralidade.limparRecarga(p, JarraAguaBlock.CHAVE_RECARGA);
		helper.assertTrue(JarraAguaBlock.encher(level, jarra, p).is(ModEspiritismoMinecraftModItems.AGUA_FLUIDIFICADA.get()), "depois da recarga volta a fluidificar");

		// Prece longe (20 blocos) não vale para esta jarra.
		BlockPos outra = jarra.offset(0, 0, 20);
		helper.assertFalse(Fluidificacao.podeFluidificar(level, outra), "prece a 20 blocos não deveria valer");
		level.removeBlock(jarra, false);
		helper.succeed();
	}

	private static void doacoes(GameTestHelper helper) {
		helper.assertTrue(ItensApi.ehDoacao(new ItemStack(RegistroItens.SOPA_FRATERNA.get())), "sopa é doação");
		helper.assertTrue(ItensApi.ehDoacao(new ItemStack(RegistroItens.AGASALHO.get())), "agasalho é doação");
		helper.assertTrue(ItensApi.ehDoacao(new ItemStack(RegistroItens.CESTA_BASICA.get())), "cesta é doação");
		helper.assertFalse(ItensApi.ehDoacao(new ItemStack(Items.BREAD)), "pão comum não é item de doação do mod");
		helper.assertFalse(ItensApi.ehDoacao(ItemStack.EMPTY), "vazio");
		helper.assertValueEqual(ItensApi.valorDoacao(new ItemStack(RegistroItens.CESTA_BASICA.get())), 5, "valor cesta");
		helper.assertValueEqual(ItensApi.valorDoacao(new ItemStack(RegistroItens.SOPA_FRATERNA.get()), true), 6, "sopa a faminto vale o dobro");
		helper.assertTrue(new ItemStack(RegistroItens.SOPA_FRATERNA.get()).has(DataComponents.FOOD), "sopa é comida (aldeão aceita)");
		helper.assertTrue(ItensApi.ehObra(Obras.criar("nosso_lar")), "nosso_lar é obra");
		helper.succeed();
	}
}
