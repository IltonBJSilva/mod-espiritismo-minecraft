package net.mcreator.modespiritismominecraft.espiritismo.mundo.teste;

import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.mundo.RegistroMundo;
import net.mcreator.modespiritismominecraft.espiritismo.mundo.PecaEspirita;
import net.mcreator.modespiritismominecraft.espiritismo.mundo.MundoApi;
import net.mcreator.modespiritismominecraft.espiritismo.mundo.Modelo;
import net.mcreator.modespiritismominecraft.espiritismo.mundo.EstruturaEspirita;
import net.mcreator.modespiritismominecraft.espiritismo.mundo.Estilo;
import net.mcreator.modespiritismominecraft.entity.AllanKardecEntity;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.function.Consumer;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/**
 * GameTests da área "mundo" (estruturas). Só registrados com o GameTest habilitado.
 */
@EventBusSubscriber
public final class TestesMundo {
	private static final Identifier ESTRUTURA_VAZIA = Identifier.withDefaultNamespace("empty");

	private record Teste(int maxTicks, Consumer<GameTestHelper> funcao) {
	}

	private static final Map<String, Teste> TESTES = new LinkedHashMap<>();

	static {
		TESTES.put("mundo_estruturas_registradas", new Teste(20, TestesMundo::estruturasRegistradas));
		TESTES.put("mundo_estilo_por_bioma", new Teste(20, TestesMundo::estiloPorBioma));
		TESTES.put("mundo_centro_pequeno_construido", new Teste(120, TestesMundo::centroPequeno));
		TESTES.put("mundo_todos_modelos_constroem", new Teste(120, TestesMundo::todosModelos));
	}

	private static final String[] ESTRUTURAS = {"centro_pequeno", "centro_medio", "centro_grande", "casa_kardec", "casa_chico", "mansao_do_caminho", "auditorio_haroldo",
			"vila_grande", "vila_grande_deserto", "vila_grande_savana", "vila_grande_taiga", "vila_grande_neve"};
	private static final String[] CONJUNTOS = {"centro_pequeno", "centro_medio", "centro_grande", "casa_kardec", "casa_chico", "mansao_do_caminho", "auditorio_haroldo",
			"vila_grande"};

	private TestesMundo() {
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
		Holder<TestEnvironmentDefinition<?>> ambiente = event.registerEnvironment(id("mundo_padrao"));
		TESTES.forEach((nome, t) -> event.registerTest(id(nome), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(nome)),
				new TestData<>(ambiente, ESTRUTURA_VAZIA, t.maxTicks(), 0, true))));
	}

	// ---------------------------------------------------------------- utilidades

	/** Força os chunks de uma área (blocos) e devolve a lista para soltar depois. */
	private static List<long[]> forcar(ServerLevel level, int x0, int z0, int x1, int z1) {
		List<long[]> chunks = new ArrayList<>();
		for (int cx = x0 >> 4; cx <= x1 >> 4; cx++)
			for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
				level.setChunkForced(cx, cz, true);
				chunks.add(new long[]{cx, cz});
			}
		return chunks;
	}

	private static void soltar(ServerLevel level, List<long[]> chunks) {
		chunks.forEach(c -> level.setChunkForced((int) c[0], (int) c[1], false));
	}

	/** Coordenada local -> mundo para uma peça com orientação NORTH (a usada pela API). */
	private static BlockPos local(BlockPos origem, Modelo m, int x, int y, int z) {
		return origem.offset(x, y, m.profundidade - 1 - z);
	}

	// ---------------------------------------------------------------- testes

	private static void estruturasRegistradas(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.STRUCTURE_TYPE.containsKey(id("construcao_espirita")), "tipo de estrutura não registrado");
		helper.assertTrue(BuiltInRegistries.STRUCTURE_PIECE.containsKey(id("peca_espirita")), "tipo de peça não registrado");
		var acesso = helper.getLevel().getServer().registryAccess();
		Registry<Structure> estruturas = acesso.lookupOrThrow(Registries.STRUCTURE);
		for (String s : ESTRUTURAS)
			helper.assertTrue(estruturas.containsKey(id(s)), "estrutura " + s + " não carregou");
		for (Modelo m : Modelo.values()) {
			Structure st = estruturas.getValue(id(m.id));
			helper.assertTrue(st instanceof EstruturaEspirita ee && ee.modelo() == m, "estrutura " + m.id + " com modelo errado");
		}
		Registry<StructureSet> conjuntos = acesso.lookupOrThrow(Registries.STRUCTURE_SET);
		for (String s : CONJUNTOS)
			helper.assertTrue(conjuntos.containsKey(id(s)), "structure_set " + s + " não carregou");
		helper.assertValueEqual(estruturas.get(RegistroMundo.CENTROS_ESPIRITAS).map(h -> h.size()).orElse(0), 3, "tag centros_espiritas");
		helper.assertValueEqual(estruturas.get(RegistroMundo.LOCAIS_ESPECIAIS).map(h -> h.size()).orElse(0), 4, "tag locais_especiais");
		helper.assertValueEqual(estruturas.get(RegistroMundo.VILAS_GRANDES).map(h -> h.size()).orElse(0), 5, "tag vilas_grandes");
		helper.succeed();
	}

	private static void estiloPorBioma(GameTestHelper helper) {
		var biomas = helper.getLevel().getServer().registryAccess().lookupOrThrow(Registries.BIOME);
		RandomSource r = RandomSource.create(1);
		helper.assertValueEqual(Estilo.doBioma(biomas.getOrThrow(Biomes.DESERT), r), Estilo.ARENITO, "deserto");
		helper.assertValueEqual(Estilo.doBioma(biomas.getOrThrow(Biomes.SAVANNA), r), Estilo.ACACIA, "savana");
		helper.assertValueEqual(Estilo.doBioma(biomas.getOrThrow(Biomes.TAIGA), r), Estilo.ABETO, "taiga");
		helper.assertValueEqual(Estilo.doBioma(biomas.getOrThrow(Biomes.SNOWY_PLAINS), r), Estilo.NEVE, "planície nevada");
		Estilo plan = Estilo.doBioma(biomas.getOrThrow(Biomes.PLAINS), r);
		helper.assertTrue(plan == Estilo.CARVALHO || plan == Estilo.TIJOLO || plan == Estilo.BRANCO, "planície: " + plan);
		helper.succeed();
	}

	private static void centroPequeno(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origem = helper.absolutePos(BlockPos.ZERO).offset(300, 0, -300);
		Modelo m = Modelo.CENTRO_PEQUENO;
		List<long[]> chunks = forcar(level, origem.getX() - 4, origem.getZ() - 4, origem.getX() + m.largura + 4, origem.getZ() + m.profundidade + 4);
		BlockPos porta = MundoApi.colocarCentroPequeno(level, origem, true);
		helper.assertTrue(level.getBlockState(porta).getBlock() instanceof DoorBlock, "porta ausente: " + level.getBlockState(porta));
		helper.assertValueEqual(porta, local(origem, m, 7, 1, 2), "posição da porta");
		helper.assertTrue(level.getBlockState(local(origem, m, 3, 0, 5)).is(Blocks.OAK_PLANKS), "piso do salão");
		helper.assertTrue(level.getBlockState(local(origem, m, 3, 2, 5)).isAir(), "interior não foi limpo");
		helper.assertTrue(level.getBlockState(local(origem, m, 2, 1, 4)).is(Blocks.OAK_STAIRS), "bancos do salão");
		boolean temLivro = PecaEspirita.blocoMod("livro_1857").isPresent();
		if (temLivro)
			helper.assertTrue(level.getBlockState(local(origem, m, 7, 2, 8)).is(PecaEspirita.blocoMod("livro_1857").get()), "livro_1857 na mesa");
		else
			helper.assertTrue(level.getBlockState(local(origem, m, 7, 1, 8)).is(Blocks.LECTERN), "atril (substituto do livro) ausente");
		helper.assertTrue(level.getBlockState(local(origem, m, 7, 1, 9)).is(Blocks.OAK_STAIRS), "cadeira do dirigente");
		// Fundação: nada flutuando sob os cantos do prédio.
		helper.assertFalse(level.getBlockState(local(origem, m, 1, -1, 2)).isAir(), "fundação ausente");
		AABB caixa = new AABB(origem).expandTowards(m.largura, 8, m.profundidade).inflate(4);
		helper.runAfterDelay(10, () -> {
			for (String npc : new String[]{"dirigente", "passista"}) {
				var tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(id(npc));
				if (tipo.isEmpty())
					continue; // a área "npc" ainda não está presente nesta cópia
				EntityType<?> t = tipo.get();
				List<Entity> achados = level.getEntities((Entity) null, caixa, en -> en.getType() == t);
				helper.assertValueEqual(achados.size(), 1, "NPC " + npc + " no centro");
				achados.forEach(Entity::discard);
			}
			soltar(level, chunks);
			// Espera o descarregamento para não atrasar os testes do lote seguinte.
			helper.runAfterDelay(50, helper::succeed);
		});
	}

	private static void todosModelos(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(600, 0, -600);
		List<long[]> chunks = new ArrayList<>();
		List<AABB> areas = new ArrayList<>();
		int x = 0;
		for (Modelo m : Modelo.values()) {
			BlockPos origem = base.offset(x, 0, 0);
			chunks.addAll(forcar(level, origem.getX() - 2, origem.getZ() - 2, origem.getX() + m.largura + 2, origem.getZ() + m.profundidade + 2));
			Estilo estilo = m.estiloFixo != null ? m.estiloFixo : Estilo.values()[m.ordinal() % 4];
			MundoApi.colocar(level, origem, m, estilo, m == Modelo.CASA_KARDEC);
			// algum bloco de piso no lote e o volume limpo
			helper.assertFalse(level.getBlockState(local(origem, m, m.largura / 2, 0, m.profundidade / 2)).isAir(), m.id + ": sem piso");
			areas.add(new AABB(origem).expandTowards(m.largura, m.altura, m.profundidade).inflate(2));
			x += m.largura + 8;
		}
		AABB kardec = areas.get(Modelo.CASA_KARDEC.ordinal());
		helper.runAfterDelay(10, () -> {
			var achados = level.getEntitiesOfClass(AllanKardecEntity.class, kardec);
			helper.assertValueEqual(achados.size(), 1, "Allan Kardec na casa_kardec");
			helper.assertTrue(achados.get(0).isPersistenceRequired() && achados.get(0).hasHome(), "Kardec deveria ficar em casa");
			helper.assertTrue(achados.get(0).getY() > kardec.minY + 5, "Kardec deveria estar no gabinete do sobrado");
			achados.forEach(Entity::discard);
			soltar(level, chunks);
			// Espera o descarregamento para não atrasar os testes do lote seguinte.
			helper.runAfterDelay(50, helper::succeed);
		});
	}
}
