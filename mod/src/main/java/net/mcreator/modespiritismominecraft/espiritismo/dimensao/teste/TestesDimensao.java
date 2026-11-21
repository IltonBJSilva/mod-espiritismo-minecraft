package net.mcreator.modespiritismominecraft.espiritismo.dimensao.teste;

import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.dimensao.RegistroDimensao;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

/** GameTests da área "dimensao" (Nosso Lar, Umbral, morte, reencarnação). Só com GameTest habilitado. */
@EventBusSubscriber
public final class TestesDimensao {
	private static final Identifier ESTRUTURA_VAZIA = Identifier.withDefaultNamespace("empty");

	private record Teste(int maxTicks, Consumer<GameTestHelper> funcao) {
	}

	private static final Map<String, Teste> TESTES = new LinkedHashMap<>();

	static {
		TESTES.put("dim_planos_carregam", new Teste(20, TestesDimensao::planosCarregam));
		TESTES.put("dim_roteamento_moralidade", new Teste(20, TestesDimensao::roteamento));
		TESTES.put("dim_construcoes_de_chegada", new Teste(300, TestesDimensao::construcoes));
		TESTES.put("dim_reencarnar_volta_a_terra", new Teste(300, TestesDimensao::reencarnar));
		TESTES.put("dim_umbral_resgate", new Teste(200, TestesDimensao::resgate));
	}

	private TestesDimensao() {
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
		Holder<TestEnvironmentDefinition<?>> ambiente = event.registerEnvironment(id("dim_padrao"));
		TESTES.forEach((nome, t) -> event.registerTest(id(nome), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(nome)),
				new TestData<>(ambiente, ESTRUTURA_VAZIA, t.maxTicks(), 0, true))));
	}

	// ---------------------------------------------------------------- Dimensões carregam

	/**
	 * O GameTestServer cria o mundo sem as dimensões de datapack (usa {@code noDatapackDimensions}), então aqui se
	 * verifica que tipo, bioma, noise settings e o próprio {@code dimension/*.json} decodificam com o codec do jogo; e,
	 * se o servidor tiver carregado os Levels (jogo normal), que {@code getLevel} não é nulo.
	 */
	private static void planosCarregam(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		var reg = server.registryAccess();
		for (String n : new String[]{"nosso_lar", "umbral"}) {
			helper.assertTrue(reg.lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(id(n)), "dimension_type " + n + " ausente");
			helper.assertTrue(reg.lookupOrThrow(Registries.BIOME).containsKey(id(n)), "bioma " + n + " ausente");
			helper.assertTrue(reg.lookupOrThrow(Registries.NOISE_SETTINGS).containsKey(id(n)), "noise_settings " + n + " ausente");
			var recurso = server.getResourceManager().getResource(id("dimension/" + n + ".json"));
			helper.assertTrue(recurso.isPresent(), "dimension/" + n + ".json ausente");
			try (var leitor = recurso.get().openAsReader()) {
				var json = com.google.gson.JsonParser.parseReader(leitor);
				var r = net.minecraft.world.level.dimension.LevelStem.CODEC.parse(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, reg), json);
				helper.assertTrue(r.isSuccess(), "dimension/" + n + " não decodifica: " + r.error().map(Object::toString).orElse(""));
				var stem = r.getOrThrow();
				helper.assertTrue(stem.type().is(id(n)), "tipo da dimensão " + n);
				if (n.equals("nosso_lar"))
					helper.assertTrue(stem.type().value().hasFixedTime(), "Nosso Lar deveria ter tempo fixo");
			} catch (java.io.IOException e) {
				throw new RuntimeException(e);
			}
		}
		if (server.getLevel(Planos.NOSSO_LAR) != null || server.getLevel(Planos.UMBRAL) != null) {
			helper.assertTrue(server.getLevel(Planos.NOSSO_LAR) != null, "dimensão nosso_lar não carregou");
			helper.assertTrue(server.getLevel(Planos.UMBRAL) != null, "dimensão umbral não carregou");
		}
		for (String a : new String[]{"plano_pontos", "ultima_terra", "arrependimento", "sonho_fim"})
			helper.assertTrue(NeoForgeRegistries.ATTACHMENT_TYPES.containsKey(id(a)), "attachment " + a + " não registrado");
		var raiz = server.getCommands().getDispatcher().getRoot();
		helper.assertTrue(raiz.getChild("espiritismo_plano") != null, "comando /espiritismo_plano ausente");
		helper.assertTrue(raiz.getChild("espiritismo_prece") != null, "comando /espiritismo_prece ausente");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Roteamento (função pura)

	private static void roteamento(GameTestHelper helper) {
		helper.assertValueEqual(Planos.destinoAposMorte(100), Planos.Destino.NOSSO_LAR, "moral 100");
		helper.assertValueEqual(Planos.destinoAposMorte(60), Planos.Destino.NOSSO_LAR, "moral 60");
		helper.assertValueEqual(Planos.destinoAposMorte(59), Planos.Destino.PERTURBACAO, "moral 59");
		helper.assertValueEqual(Planos.destinoAposMorte(31), Planos.Destino.PERTURBACAO, "moral 31");
		helper.assertValueEqual(Planos.destinoAposMorte(30), Planos.Destino.UMBRAL, "moral 30");
		helper.assertValueEqual(Planos.destinoAposMorte(0), Planos.Destino.UMBRAL, "moral 0");
		helper.assertValueEqual(Planos.tipoSonho(80, 0.1F), 1, "sonho bom");
		helper.assertValueEqual(Planos.tipoSonho(80, 0.5F), 0, "sem sonho");
		helper.assertValueEqual(Planos.tipoSonho(40, 0.1F), 0, "moral neutra não sonha");
		helper.assertValueEqual(Planos.tipoSonho(10, 0.1F), 2, "pesadelo");
		helper.assertFalse(Planos.resgateLiberado(20, 4), "resgate cedo demais");
		helper.assertTrue(Planos.resgateLiberado(20, 5), "resgate por arrependimento");
		helper.assertTrue(Planos.resgateLiberado(31, 0), "resgate por moralidade");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Construções

	/** Constrói Nosso Lar e o Posto de Socorro no próprio mundo de teste, longe do grid, e confere blocos-chave. */
	private static void construcoes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO);
		Map<String, Long> nl = Planos.construir(level, base.getX() + 700, base.getZ() + 700, true);
		BlockPos chegada = BlockPos.of(nl.get("chegada"));
		helper.assertTrue(level.getBlockState(chegada.below()).is(Blocks.SMOOTH_QUARTZ), "piso do hospital ausente: " + level.getBlockState(chegada.below()));
		helper.assertTrue(level.getBlockState(chegada).isAir() && level.getBlockState(chegada.above()).isAir(), "chegada obstruída");
		BlockPos aux = BlockPos.of(nl.get("auxilio"));
		helper.assertTrue(level.getBlockState(aux).is(Blocks.LECTERN), "atril do Ministério do Auxílio ausente");
		BlockPos mirante = BlockPos.of(nl.get("mirante"));
		helper.assertTrue(level.getBlockState(mirante).is(Blocks.LECTERN), "atril do Mirante ausente");
		helper.assertTrue(level.getBlockState(mirante.north(2).above()).is(Blocks.BIRCH_WALL_SIGN), "placas da escala dos mundos ausentes: " + level.getBlockState(mirante.north(2).above()));
		Map<String, Long> um = Planos.construir(level, base.getX() + 700, base.getZ() - 700, false);
		BlockPos posto = BlockPos.of(um.get("posto"));
		helper.assertTrue(level.getBlockState(posto.below()).is(Blocks.SPRUCE_PLANKS), "piso do Posto de Socorro ausente");
		helper.assertTrue(level.getBlockState(posto.offset(-2, 2, -2)).is(Blocks.LANTERN), "luz do Posto ausente");
		BlockPos chegadaUm = BlockPos.of(um.get("chegada"));
		helper.assertTrue(level.getBlockState(chegadaUm).isAir() && level.getBlockState(chegadaUm.below()).is(Blocks.MUD), "chegada no Umbral");
		// Dá tempo ao servidor de se recuperar da construção antes do próximo lote de testes (que mede chunks ativos).
		helper.runAfterDelay(100, helper::succeed);
	}

	// ---------------------------------------------------------------- Reencarnação

	private static void reencarnar(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		try {
			if (helper.getLevel().getServer().getLevel(Planos.NOSSO_LAR) != null) {
				helper.assertTrue(Planos.ir(p, Planos.NOSSO_LAR), "ir a Nosso Lar falhou");
				helper.assertValueEqual(p.level().dimension(), Planos.NOSSO_LAR, "jogador deveria estar em Nosso Lar");
			}
			BlockPos antes = p.blockPosition();
			p.setData(RegistroDimensao.ULTIMA_TERRA, antes.asLong());
			Moralidade.set(p, 72);
			BlockPos destino = Planos.reencarnar(p);
			helper.assertValueEqual(p.level().dimension(), Level.OVERWORLD, "reencarnar deveria levar à Terra");
			helper.assertValueEqual(Moralidade.get(p), 72, "moralidade conservada");
			helper.assertTrue(p.blockPosition().distManhattan(destino) <= 2, "jogador não foi ao destino: " + p.blockPosition() + " / " + destino);
			helper.assertTrue(antes.distManhattan(destino) > 300 || destino.equals(Planos.pontoNaTerra(p)), "reencarnação muito perto do local anterior");
		} finally {
			helper.getLevel().getServer().getPlayerList().remove(p);
		}
		helper.runAfterDelay(100, helper::succeed);
	}

	// ---------------------------------------------------------------- Umbral → resgate

	private static void resgate(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		try {
			// Fora do Umbral a prece não soma arrependimento (lá ela é tratada pela área npc).
			Planos.aoOrar(p);
			helper.assertValueEqual(p.getData(RegistroDimensao.ARREPENDIMENTO), 0, "prece fora do Umbral");
			if (server.getLevel(Planos.UMBRAL) != null) {
				Moralidade.set(p, 10);
				helper.assertTrue(Planos.ir(p, Planos.UMBRAL), "ir ao Umbral falhou");
				for (int i = 0; i < 5; i++) {
					Moralidade.limparRecarga(p, "dim_prece_umbral");
					Planos.aoOrar(p);
				}
				helper.assertTrue(p.getData(RegistroDimensao.ARREPENDIMENTO) >= 5, "5 preces somam arrependimento");
				net.mcreator.modespiritismominecraft.espiritismo.dimensao.Resgates.pedirAjuda(p);
				helper.assertTrue(p.getData(RegistroDimensao.RESGATE_EM) > 0 || net.mcreator.modespiritismominecraft.espiritismo.dimensao.Resgates.fase(p) == 0 && p.level().dimension() == Planos.UMBRAL,
						"pedir ajuda deveria chamar o socorrista (sem teleportar)");
				helper.assertValueEqual(p.level().dimension(), Planos.UMBRAL, "socorro não teleporta sem consentimento");
				Planos.resgatar(p);
				helper.assertValueEqual(p.level().dimension(), Planos.NOSSO_LAR, "resgate deveria levar a Nosso Lar");
				helper.assertValueEqual(p.getData(RegistroDimensao.ARREPENDIMENTO), 0, "arrependimento zerado");
			}
		} finally {
			server.getPlayerList().remove(p);
		}
		helper.succeed();
	}
}
