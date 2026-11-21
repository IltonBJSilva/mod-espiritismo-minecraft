package net.mcreator.modespiritismominecraft.espiritismo.colonia.teste;

import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.protecao.ZonasProtegidas;
import net.mcreator.modespiritismominecraft.espiritismo.protecao.PoliticaProtecao;
import net.mcreator.modespiritismominecraft.espiritismo.protecao.MensagensProtecao;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.PlanoColonia;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.Obra;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.LayoutColonia;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.ConstrucaoColonia;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.ColoniaApi;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.Aerobus;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.function.Consumer;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.ArrayList;

/** GameTests da área "colonia" (cidade de Nosso Lar, flores, aeróbus) e da proteção. Só com GameTest habilitado. */
@EventBusSubscriber
public final class TestesColonia {
	private static final Identifier ESTRUTURA_VAZIA = Identifier.withDefaultNamespace("empty");

	private record Teste(int maxTicks, Consumer<GameTestHelper> funcao) {
	}

	private static final Map<String, Teste> TESTES = new LinkedHashMap<>();

	static {
		TESTES.put("col_layout_pontos", new Teste(20, TestesColonia::layout));
		TESTES.put("col_essenciais_rapidas_sem_itens", new Teste(200, TestesColonia::essenciais));
		// Teste pesado (constrói a cidade inteira, ~300 mil ticks): só roda com -Despiritismo.testesPesados=true.
		// A verificação leve do mesmo bug (chegada sem itens soltos) fica em col_essenciais_rapidas_sem_itens.
		if (Boolean.getBoolean("espiritismo.testesPesados"))
			TESTES.put("col_cidade_flores_sobrevivem", new Teste(600_000, TestesColonia::cidadeCompleta));
		TESTES.put("col_protecao_quebra", new Teste(40, TestesColonia::protecaoQuebra));
		TESTES.put("col_protecao_colocacao", new Teste(40, TestesColonia::protecaoColocacao));
		TESTES.put("col_protecao_explosao", new Teste(40, TestesColonia::protecaoExplosao));
		TESTES.put("col_protecao_fogo_fluido_pistao", new Teste(60, TestesColonia::protecaoFogoFluidoPistao));
		TESTES.put("col_protecao_mobs_e_politica", new Teste(40, TestesColonia::protecaoMobsPolitica));
	}

	private TestesColonia() {
	}

	/** Vigia de itens: conta todo ItemEntity que surgir na área vigiada (mesmo que o chunk descarregue depois). */
	private record Vigia(ResourceKey<Level> dim, AABB area, int[] contagem) {
	}

	private static final List<Vigia> VIGIAS = new java.util.concurrent.CopyOnWriteArrayList<>();

	@SubscribeEvent
	public static void itemSurgiu(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
		if (VIGIAS.isEmpty() || !(event.getEntity() instanceof ItemEntity item) || event.getLevel().isClientSide())
			return;
		for (Vigia v : VIGIAS)
			if (v.dim() == event.getLevel().dimension() && v.area().contains(item.position())) {
				v.contagem()[0]++;
				ModEspiritismoMinecraftMod.LOGGER.warn("[Colonia][teste] item solto: {} em {}", item.getItem(), item.blockPosition());
			}
	}

	private static Vigia vigiar(ServerLevel level, BlockPos centro, int raio) {
		Vigia v = new Vigia(level.dimension(), new AABB(centro).inflate(raio, 120, raio), new int[1]);
		VIGIAS.add(v);
		return v;
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
		Holder<TestEnvironmentDefinition<?>> ambiente = event.registerEnvironment(id("col_padrao"));
		TESTES.forEach((nome, t) -> event.registerTest(id(nome), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(nome)),
				new TestData<>(ambiente, ESTRUTURA_VAZIA, t.maxTicks(), 0, true))));
	}

	// ================================================================ Traçado (puro)

	private static void layout(GameTestHelper helper) {
		Map<String, BlockPos> p = LayoutColonia.pontosRelativos();
		for (String n : new String[]{"governadoria", "regeneracao", "auxilio", "comunicacao", "esclarecimento", "elevacao", "uniao_divina", "camaras_retificacao", "hospital",
				"biblioteca", "escola", "praca", "bosque", "residencias", "estacao_aerobus", "portao_norte", "portao_sul", "portao_leste", "portao_oeste", "chegada", "mirante",
				"mirante_npc", "centro"})
			helper.assertTrue(p.containsKey(n), "ponto ausente: " + n);
		// Todos dentro da muralha
		for (var e : p.entrySet())
			helper.assertTrue(LayoutColonia.raio(e.getValue().getX(), e.getValue().getZ()) < LayoutColonia.R_MURO - 2, "ponto fora da muralha: " + e.getKey());
		// Auxílio a oeste da praça (as mensagens de Planos dizem isso)
		helper.assertTrue(p.get("auxilio").getX() < -40 && Math.abs(p.get("auxilio").getZ()) < 10, "Auxílio deveria ficar a oeste");
		// Edifícios: dentro da muralha e sem sobreposição
		List<LayoutColonia.Edificio> ed = LayoutColonia.edificios();
		Set<LayoutColonia.Setor> setores = new HashSet<>();
		int casas = 0, estacoes = 0;
		for (int i = 0; i < ed.size(); i++) {
			LayoutColonia.Edificio a = ed.get(i);
			if (a.setor() != null && a.tipo() == LayoutColonia.Tipo.MINISTERIO || a.tipo() == LayoutColonia.Tipo.CAMARAS)
				setores.add(a.setor());
			casas += a.tipo() == LayoutColonia.Tipo.CASA ? 1 : 0;
			estacoes += a.tipo() == LayoutColonia.Tipo.ESTACAO ? 1 : 0;
			for (int[] c : new int[][]{{a.x0(), a.z0()}, {a.x1(), a.z1()}, {a.x0(), a.z1()}, {a.x1(), a.z0()}})
				helper.assertTrue(LayoutColonia.raio(c[0], c[1]) < LayoutColonia.R_MURO - 4, "edifício encosta na muralha: " + a.id());
			for (int j = i + 1; j < ed.size(); j++) {
				LayoutColonia.Edificio b = ed.get(j);
				boolean sobrepoe = a.x0() <= b.x1() + 1 && b.x0() <= a.x1() + 1 && a.z0() <= b.z1() + 1 && b.z0() <= a.z1() + 1;
				helper.assertFalse(sobrepoe, "edifícios sobrepostos: " + a.id() + " e " + b.id());
			}
		}
		helper.assertValueEqual(setores.size(), 6, "seis Ministérios");
		helper.assertTrue(casas >= 40, "poucas casas nos bairros: " + casas);
		helper.assertValueEqual(estacoes, 8, "estações do aeróbus");
		// Pontos de chão não caem dentro de paredes nem na água
		for (String n : new String[]{"praca", "governadoria", "chegada", "portao_norte", "portao_sul", "portao_leste", "portao_oeste", "bosque"}) {
			BlockPos b = p.get(n);
			helper.assertTrue(LayoutColonia.chao(b.getX(), b.getZ()) != LayoutColonia.Chao.MURO && LayoutColonia.chao(b.getX(), b.getZ()) != LayoutColonia.Chao.AGUA, "ponto mal posto: " + n);
		}
		helper.assertTrue(LayoutColonia.Setor.de(181) == LayoutColonia.Setor.AUXILIO && LayoutColonia.Setor.de(359) == LayoutColonia.Setor.ESCLARECIMENTO, "setores por ângulo");
		helper.succeed();
	}

	// ================================================================ Utilidades

	private static int itensNaArea(ServerLevel level, BlockPos centro, int raio) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(centro).inflate(raio, 80, raio)).size();
	}

	/** Monta um "terreno natural" bagunçado: morro com flores, plantas altas e árvore sem folhas persistentes. */
	private static List<BlockPos> baguncaNatural(ServerLevel level, BlockPos origem) {
		List<BlockPos> flores = new ArrayList<>();
		int g = origem.getY();
		BlockPos chegada = origem.offset(LayoutColonia.pontosRelativos().get("chegada"));
		int[][] morros = {{chegada.getX() - origem.getX() + 14, chegada.getZ() - origem.getZ()}, {0, 60}, {80, 0}, {-30, 110}, {LayoutColonia.R_PLANO + 6, 0}};
		for (int[] m : morros)
			for (int dx = -3; dx <= 3; dx++)
				for (int dz = -3; dz <= 3; dz++) {
					int x = origem.getX() + m[0] + dx, z = origem.getZ() + m[1] + dz;
					for (int y = g + 1; y <= g + 4; y++)
						level.setBlock(new BlockPos(x, y, z), y == g + 4 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
					BlockPos topo = new BlockPos(x, g + 5, z);
					if ((dx + dz) % 3 == 0) {
						level.setBlock(topo, Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 2);
						level.setBlock(topo.above(), Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 2);
					} else
						level.setBlock(topo, (dx & 1) == 0 ? Blocks.DANDELION.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState(), 2);
					flores.add(topo);
				}
		// Flores no chão plano também
		for (int i = 0; i < 40; i++) {
			BlockPos f = origem.offset(-50 + i * 3, 1, 40);
			level.setBlock(f, Blocks.POPPY.defaultBlockState(), 2);
		}
		// Árvore cortada pela borda do talude: tronco dentro, folhas (não persistentes) para fora
		int bx = origem.getX() + LayoutColonia.R_TALUDE - 1, bz = origem.getZ();
		for (int y = 1; y <= 5; y++)
			level.setBlock(new BlockPos(bx, g + y, bz), Blocks.OAK_LOG.defaultBlockState(), 2);
		for (int dx = -2; dx <= 4; dx++)
			for (int dz = -2; dz <= 2; dz++)
				for (int y = 4; y <= 6; y++) {
					BlockPos f = new BlockPos(bx + dx, g + y, bz + dz);
					if (level.getBlockState(f).isAir())
						level.setBlock(f, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1), 2);
				}
		return flores;
	}

	// ================================================================ Zonas essenciais

	private static void essenciais(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO);
		BlockPos sug = new BlockPos(base.getX() - 1500, 0, base.getZ() + 1500);
		int g = PlanoColonia.alturaDaCidade(level, sug.getX(), sug.getZ());
		BlockPos origem = new BlockPos(sug.getX(), g, sug.getZ());
		baguncaNatural(level, origem);
		Vigia vigia = vigiar(level, origem, LayoutColonia.R_MARGEM + 8);
		long t0 = System.nanoTime();
		Map<String, Long> pontos = ConstrucaoColonia.construir(level, sug);
		long ms = (System.nanoTime() - t0) / 1_000_000;
		ModEspiritismoMinecraftMod.LOGGER.info("[Colonia][teste] zonas essenciais: {} ms", ms);
		helper.assertTrue(ms < 5000, "zonas essenciais lentas demais: " + ms + " ms");
		BlockPos chegada = BlockPos.of(pontos.get("chegada"));
		helper.assertTrue(level.getBlockState(chegada.below()).is(Blocks.SMOOTH_QUARTZ), "piso das Câmaras");
		helper.assertTrue(level.getBlockState(chegada).isAir() && level.getBlockState(chegada.above()).isAir(), "chegada obstruída");
		helper.assertTrue(level.getBlockState(BlockPos.of(pontos.get("auxilio"))).is(Blocks.LECTERN), "atril do Auxílio");
		helper.assertTrue(level.getBlockState(BlockPos.of(pontos.get("mirante"))).is(Blocks.LECTERN), "atril do Mirante");
		helper.assertTrue(ConstrucaoColonia.obra(level) == null, "fora de Nosso Lar não deveria haver obra incremental");
		helper.runAfterDelay(20, () -> {
			VIGIAS.remove(vigia);
			helper.assertValueEqual(vigia.contagem()[0] + itensNaArea(level, chegada, 60), 0, "itens soltos perto da chegada (flores quebradas?)");
			helper.succeed();
		});
	}

	// ================================================================ Cidade completa: flores, itens, retomada, aeróbus

	private static void cidadeCompleta(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO);
		BlockPos sug = new BlockPos(base.getX() + 3000, 0, base.getZ() - 3000);
		int g = PlanoColonia.alturaDaCidade(level, sug.getX(), sug.getZ());
		BlockPos origem = new BlockPos(sug.getX(), g, sug.getZ());
		List<BlockPos> naturais = baguncaNatural(level, origem);
		Vigia vigia = vigiar(level, origem, LayoutColonia.R_MARGEM + 8);
		PlanoColonia plano = new PlanoColonia(origem);
		ConstrucaoColonia.construirEssenciais(level, plano);
		Set<Long> plantas = Obra.novoRegistroDePlantas();
		Obra[] obra = {nova(level, plano, 0, plantas)};
		boolean[] reiniciou = {false};
		long t0 = System.currentTimeMillis();
		Runnable[] passo = new Runnable[1];
		passo[0] = () -> {
			Obra o = obra[0];
			o.tick();
			if (o.ticks % 50000 == 0)
				ModEspiritismoMinecraftMod.LOGGER.info("[Colonia][teste] obra: {}", o.estado());
			if (!reiniciou[0] && o.indice() >= o.total() / 2) {
				// Simula fechar e abrir o mundo: plano recriado do zero, retoma no índice salvo (refaz a peça em curso).
				reiniciou[0] = true;
				obra[0] = nova(level, new PlanoColonia(origem), o.indice(), plantas);
			}
			if (!obra[0].concluida()) {
				helper.runAfterDelay(1, () -> passo[0].run()); // lambda nova: o mapa do GameTest é indexado pelo Runnable
				return;
			}
			ModEspiritismoMinecraftMod.LOGGER.info("[Colonia][teste] cidade completa em {} ticks de teste, {} s, {} plantas, máx {} ms/tick", obra[0].ticks,
					(System.currentTimeMillis() - t0) / 1000, plantas.size(), obra[0].nanosMaxTick / 1_000_000);
			helper.runAfterDelay(40, () -> verificarCidade(helper, level, origem, plano, plantas, naturais, vigia));
		};
		helper.runAfterDelay(1, () -> passo[0].run());
	}

	private static Obra nova(ServerLevel level, PlanoColonia plano, int indice, Set<Long> plantas) {
		Obra o = new Obra(level, plano, indice, false);
		o.orcamentoNanos = 45_000_000L;
		o.chunksPorTick = 6;
		o.plantas = plantas;
		return o;
	}

	private static void verificarCidade(GameTestHelper helper, ServerLevel level, BlockPos origem, PlanoColonia plano, Set<Long> plantas, List<BlockPos> naturais, Vigia vigia) {
		try {
			verificarCidade0(helper, level, origem, plano, plantas, naturais, vigia);
		} catch (RuntimeException e) {
			VIGIAS.remove(vigia);
			throw e;
		}
	}

	private static void verificarCidade0(GameTestHelper helper, ServerLevel level, BlockPos origem, PlanoColonia plano, Set<Long> plantas, List<BlockPos> naturais, Vigia vigia) {
		helper.assertValueEqual(vigia.contagem()[0], 0, "itens soltos na área da colônia durante a construção (plantas quebradas)");
		helper.assertTrue(plantas.size() > 3000, "poucas plantas colocadas: " + plantas.size());
		int perdidas = 0;
		for (long l : plantas)
			if (level.getBlockState(BlockPos.of(l)).isAir())
				perdidas++;
		helper.assertValueEqual(perdidas, 0, "plantas que sumiram depois de colocadas");
		// Flores e plantas do terreno natural antigo saíram sem virar item (o morro foi aplainado)
		for (BlockPos n : naturais)
			helper.assertTrue(!level.getBlockState(n).is(Blocks.SUNFLOWER) || LayoutColonia.raio(n.getX() - origem.getX(), n.getZ() - origem.getZ()) > LayoutColonia.R_TALUDE,
					"girassol do morro deveria ter saído: " + n);
		// Folhas da árvore cortada no talude ficaram persistentes (não secam e não soltam mudas)
		BlockPos folha = new BlockPos(origem.getX() + LayoutColonia.R_TALUDE + 2, origem.getY() + 5, origem.getZ());
		BlockState fs = level.getBlockState(folha);
		helper.assertTrue(!fs.is(BlockTags.LEAVES) || fs.getValue(LeavesBlock.PERSISTENT), "folha na margem deveria ser persistente: " + fs);
		// Estruturas-chave
		helper.assertTrue(level.getBlockState(origem.offset(0, 41, 0)).is(Blocks.BEACON), "torre de luz (farol) da Governadoria");
		int[] muro = LayoutColonia.polar(20, LayoutColonia.R_MURO);
		helper.assertTrue(level.getBlockState(origem.offset(muro[0], 3, muro[1])).is(Blocks.QUARTZ_BRICKS), "muralha");
		int[] portao = LayoutColonia.polar(0, LayoutColonia.R_MURO);
		helper.assertTrue(level.getBlockState(origem.offset(portao[0], 3, portao[1])).isAir(), "portão leste aberto");
		Map<String, Long> pontos = plano.pontos();
		for (String n : new String[]{"praca", "governadoria", "regeneracao", "comunicacao", "esclarecimento", "elevacao", "uniao_divina", "hospital", "biblioteca", "escola",
				"residencias", "bosque", "portao_norte", "portao_sul", "portao_leste", "portao_oeste", "estacao_aerobus"}) {
			BlockPos p = BlockPos.of(pontos.get(n));
			helper.assertTrue(level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir(), "ponto obstruído: " + n + " " + level.getBlockState(p));
			helper.assertTrue(!level.getBlockState(p.below()).isAir(), "ponto sem chão: " + n);
		}
		// Talude: nenhuma "parede" — a diferença de altura entre colunas vizinhas na faixa do talude é no máximo 1
		for (int a = 0; a < 360; a += 45)
			for (int r = LayoutColonia.R_PLANO; r < LayoutColonia.R_TALUDE; r++) {
				int[] p1 = LayoutColonia.polar(a + 7, r), p2 = LayoutColonia.polar(a + 7, r + 1);
				level.getChunk((origem.getX() + p1[0]) >> 4, (origem.getZ() + p1[1]) >> 4);
				level.getChunk((origem.getX() + p2[0]) >> 4, (origem.getZ() + p2[1]) >> 4);
				int h1 = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origem.getX() + p1[0], origem.getZ() + p1[1]);
				int h2 = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origem.getX() + p2[0], origem.getZ() + p2[1]);
				helper.assertTrue(Math.abs(h1 - h2) <= 2, "degrau no talude em " + a + "°, r=" + r + ": " + h1 + " / " + h2);
			}
		// Reconstruir as zonas essenciais de novo não duplica nem solta itens (idempotente)
		ConstrucaoColonia.construirEssenciais(level, plano);
		BlockPos chegada = BlockPos.of(pontos.get("chegada"));
		helper.assertTrue(level.getBlockState(chegada.below()).is(Blocks.SMOOTH_QUARTZ) && level.getBlockState(chegada).isAir(), "chegada após reconstruir");
		// Aeróbus: do Auxílio ao Bosque das Águas
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		try {
			BlockPos est = BlockPos.of(pontos.get("estacao_auxilio"));
			p.teleportTo(level, est.getX() + 0.5, est.getY(), est.getZ() + 0.5, Set.of(), 0F, 0F, true);
			helper.assertTrue(Aerobus.pronta(level, pontos, "estacao_bosque"), "estação do Bosque deveria estar pronta");
			helper.assertValueEqual(Aerobus.embarcar(p, level, pontos, "estacao_auxilio"), "Você já está nesta estação.", "mesma estação");
			String erro = Aerobus.embarcar(p, level, pontos, "estacao_bosque");
			helper.assertTrue(erro == null, "embarque: " + erro);
			Aerobus.chegarAgora(p);
			BlockPos destino = BlockPos.of(pontos.get("estacao_bosque"));
			helper.assertTrue(p.blockPosition().distManhattan(destino) <= 2, "aeróbus não chegou: " + p.blockPosition() + " / " + destino);
		} finally {
			level.getServer().getPlayerList().remove(p);
		}
		helper.runAfterDelay(20, () -> {
			VIGIAS.remove(vigia);
			helper.assertValueEqual(vigia.contagem()[0], 0, "itens soltos após reconstrução");
			helper.succeed();
		});
	}

	// ================================================================ Proteção
	// As áreas de teste da proteção (7x7x7) ficam longe da grade do GameTest, que só deixa 5 blocos entre testes.

	/** Área de teste de proteção, deslocada da grade (índice k por teste). */
	private record Area(GameTestHelper helper, BlockPos base) {
		Area(GameTestHelper helper, int k) {
			this(helper, helper.absolutePos(BlockPos.ZERO).offset(600 + k * 40, 0, 600 + k * 40));
		}

		BlockPos abs(BlockPos rel) {
			return base.offset(rel);
		}

		void set(BlockPos rel, net.minecraft.world.level.block.Block b) {
			helper.getLevel().setBlock(abs(rel), b.defaultBlockState(), 2);
		}

		void set(BlockPos rel, BlockState st) {
			helper.getLevel().setBlock(abs(rel), st, 2);
		}

		BlockState st(BlockPos rel) {
			return helper.getLevel().getBlockState(abs(rel));
		}

		BoundingBox zona() {
			return BoundingBox.fromCorners(abs(BlockPos.ZERO), abs(new BlockPos(6, 6, 6)));
		}
	}

	private static void forcar(GameTestHelper helper, BoundingBox z, boolean sim) {
		for (int cx = z.minX() >> 4; cx <= z.maxX() >> 4; cx++)
			for (int cz = z.minZ() >> 4; cz <= z.maxZ() >> 4; cz++)
				helper.getLevel().setChunkForced(cx, cz, sim);
	}

	/** Registra a zona protegida, força o chunk (para pistões/fogo funcionarem de verdade) e roda o corpo 5 ticks depois. */
	private static void comZona(Area ar, Consumer<BoundingBox> corpo) {
		GameTestHelper helper = ar.helper();
		BoundingBox z = ar.zona();
		forcar(helper, z, true);
		ZonasProtegidas.registrar(helper.getLevel().dimension(), z);
		helper.runAfterDelay(5, () -> {
			try {
				corpo.accept(z);
			} catch (RuntimeException e) {
				ZonasProtegidas.remover(helper.getLevel().dimension(), z);
				forcar(helper, z, false);
				throw e;
			}
		});
	}

	private static void fim(Area ar, int delay, Runnable verificacao) {
		GameTestHelper helper = ar.helper();
		BoundingBox z = ar.zona();
		helper.runAfterDelay(delay, () -> {
			try {
				verificacao.run();
			} finally {
				ZonasProtegidas.remover(helper.getLevel().dimension(), z);
				forcar(helper, z, false);
			}
			helper.succeed();
		});
	}

	private static void chao(Area ar) {
		for (int x = 0; x <= 6; x++)
			for (int z = 0; z <= 6; z++)
				ar.set(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
	}

	private static void protecaoQuebra(GameTestHelper helper) {
		Area ar = new Area(helper, 0);
		comZona(ar, z -> {
			chao(ar);
			Map<BlockPos, BlockState> alvos = new LinkedHashMap<>();
			alvos.put(new BlockPos(1, 1, 1), Blocks.POPPY.defaultBlockState());
			alvos.put(new BlockPos(2, 1, 1), Blocks.STONE_BRICKS.defaultBlockState());
			alvos.put(new BlockPos(3, 1, 1), Blocks.OAK_LOG.defaultBlockState());
			alvos.put(new BlockPos(4, 1, 1), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
			alvos.put(new BlockPos(5, 1, 1), Blocks.POTTED_POPPY.defaultBlockState());
			alvos.forEach((p, s) -> ar.set(p, s));
			alvos.put(new BlockPos(1, 0, 3), Blocks.GRASS_BLOCK.defaultBlockState());
			ServerPlayer p = helper.makeMockServerPlayerInLevel();
			try {
				helper.assertFalse(PoliticaProtecao.isento(p), "jogador de teste não é operador");
				for (BlockPos rel : alvos.keySet()) {
					boolean quebrou = p.gameMode.destroyBlock(ar.abs(rel));
					helper.assertFalse(quebrou, "quebrou bloco protegido em " + rel);
				}
			} finally {
				helper.getLevel().getServer().getPlayerList().remove(p);
			}
			fim(ar, 5, () -> {
				alvos.forEach((rel, s) -> helper.assertTrue(ar.st(rel).is(s.getBlock()), "bloco mudou em " + rel + ": " + ar.st(rel)));
				helper.assertValueEqual(itensNaArea(helper.getLevel(), ar.abs(new BlockPos(3, 1, 3)), 4), 0, "itens soltos");
			});
		});
	}

	private static void protecaoColocacao(GameTestHelper helper) {
		Area ar = new Area(helper, 1);
		comZona(ar, z -> {
			chao(ar);
			ServerPlayer p = helper.makeMockServerPlayerInLevel();
			try {
				ServerLevel level = helper.getLevel();
				BlockPos alvo = ar.abs(new BlockPos(2, 0, 2));
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(alvo).add(0, 0.5, 0), Direction.UP, alvo, false);
				// 1) Pelo caminho do jogador (RightClickBlock)
				ItemStack pedra = new ItemStack(Items.STONE, 8);
				p.setItemInHand(InteractionHand.MAIN_HAND, pedra);
				p.gameMode.useItemOn(p, level, pedra, InteractionHand.MAIN_HAND, hit);
				helper.assertTrue(level.getBlockState(alvo.above()).isAir(), "colocou bloco pelo clique");
				// 2) Direto pelo item (EntityPlaceEvent)
				pedra.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit));
				helper.assertTrue(level.getBlockState(alvo.above()).isAir(), "colocou bloco pelo item");
				// 3) Substituir grama baixa
				ar.set(new BlockPos(4, 1, 4), Blocks.SHORT_GRASS);
				BlockPos grama = ar.abs(new BlockPos(4, 1, 4));
				pedra.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(grama), Direction.UP, grama, false)));
				helper.assertTrue(level.getBlockState(grama).is(Blocks.SHORT_GRASS), "substituiu a grama");
				// 4) Balde de água (mira para baixo)
				ItemStack balde = new ItemStack(Items.WATER_BUCKET);
				p.setItemInHand(InteractionHand.MAIN_HAND, balde);
				BlockPos sobre = ar.abs(new BlockPos(3, 2, 3));
				p.snapTo(sobre.getX() + 0.5, sobre.getY(), sobre.getZ() + 0.5, 0F, 90F);
				p.gameMode.useItem(p, level, balde, InteractionHand.MAIN_HAND);
				p.gameMode.useItemOn(p, level, balde, InteractionHand.MAIN_HAND, hit);
				// 5) Farinha de osso e enxada no gramado
				ItemStack osso = new ItemStack(Items.BONE_MEAL, 4);
				p.setItemInHand(InteractionHand.MAIN_HAND, osso);
				p.gameMode.useItemOn(p, level, osso, InteractionHand.MAIN_HAND, hit);
				ItemStack enxada = new ItemStack(Items.IRON_HOE);
				p.setItemInHand(InteractionHand.MAIN_HAND, enxada);
				p.gameMode.useItemOn(p, level, enxada, InteractionHand.MAIN_HAND, hit);
				helper.assertTrue(level.getBlockState(alvo).is(Blocks.GRASS_BLOCK), "a grama foi arada: " + level.getBlockState(alvo));
			} finally {
				helper.getLevel().getServer().getPlayerList().remove(p);
			}
			fim(ar, 10, () -> {
				for (int x = 0; x <= 6; x++)
					for (int y = 1; y <= 4; y++)
						for (int zz = 0; zz <= 6; zz++)
							helper.assertTrue(ar.st(new BlockPos(x, y, zz)).getFluidState().isEmpty(), "água despejada em " + x + "," + y + "," + zz);
			});
		});
	}

	private static void protecaoExplosao(GameTestHelper helper) {
		Area ar = new Area(helper, 2);
		comZona(ar, z -> {
			chao(ar);
			for (int x = 2; x <= 4; x++)
				for (int y = 1; y <= 3; y++)
					for (int zz = 2; zz <= 4; zz++)
						if (!(x == 3 && y == 2 && zz == 3))
							ar.set(new BlockPos(x, y, zz), Blocks.OAK_PLANKS);
			ar.set(new BlockPos(1, 1, 1), Blocks.POPPY);
			BlockPos c = ar.abs(new BlockPos(3, 2, 3));
			helper.getLevel().explode(null, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 2.5F, Level.ExplosionInteraction.TNT);
			fim(ar, 5, () -> {
				for (int x = 2; x <= 4; x++)
					for (int y = 1; y <= 3; y++)
						for (int zz = 2; zz <= 4; zz++)
							if (!(x == 3 && y == 2 && zz == 3))
								helper.assertTrue(ar.st(new BlockPos(x, y, zz)).is(Blocks.OAK_PLANKS), "explosão destruiu bloco");
				helper.assertTrue(ar.st(new BlockPos(1, 1, 1)).is(Blocks.POPPY), "explosão destruiu a flor");
				helper.assertValueEqual(itensNaArea(helper.getLevel(), c, 5), 0, "itens soltos");
			});
		});
	}

	private static void protecaoFogoFluidoPistao(GameTestHelper helper) {
		Area ar = new Area(helper, 3);
		comZona(ar, z -> {
			chao(ar);
			ServerLevel level = helper.getLevel();
			// Fogo ao lado de madeira: some no fim do tick, antes de queimar
			ar.set(new BlockPos(1, 1, 1), Blocks.OAK_PLANKS);
			level.setBlock(ar.abs(new BlockPos(1, 2, 1)), Blocks.FIRE.defaultBlockState(), 3);
			// Lava + água não viram pedra; a lava é removida
			level.setBlock(ar.abs(new BlockPos(5, 1, 5)), Blocks.LAVA.defaultBlockState(), 3);
			// Pistão energizado não empurra
			BlockPos pistao = new BlockPos(2, 1, 4), pedra = new BlockPos(3, 1, 4);
			ar.set(pistao, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
			ar.set(pedra, Blocks.STONE);
			level.setBlock(ar.abs(new BlockPos(1, 1, 4)), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
			fim(ar, 20, () -> {
				helper.assertTrue(ar.st(new BlockPos(1, 2, 1)).isAir(), "o fogo deveria ter sido apagado");
				helper.assertTrue(ar.st(new BlockPos(1, 1, 1)).is(Blocks.OAK_PLANKS), "a madeira queimou");
				helper.assertTrue(ar.st(new BlockPos(5, 1, 5)).isAir(), "a lava deveria ter sido removida");
				helper.assertTrue(ar.st(pedra).is(Blocks.STONE), "o pistão empurrou a pedra");
				helper.assertFalse(ar.st(pistao).getValue(PistonBaseBlock.EXTENDED), "o pistão estendeu");
			});
		});
	}

	private static void protecaoMobsPolitica(GameTestHelper helper) {
		Area ar = new Area(helper, 4);
		comZona(ar, z -> {
			chao(ar);
			ServerLevel level = helper.getLevel();
			BlockPos dentro = ar.abs(new BlockPos(3, 1, 3));
			for (EntityType<?> t : new EntityType<?>[]{EntityType.ENDERMAN, EntityType.SHEEP, EntityType.VILLAGER, EntityType.RABBIT}) {
				Entity e = t.create(level, EntitySpawnReason.TRIGGERED);
				helper.assertTrue(e != null, "não criou " + t);
				e.snapTo(dentro.getX() + 0.5, dentro.getY(), dentro.getZ() + 0.5, 0F, 0F);
				helper.assertFalse(EventHooks.canEntityGrief(level, e), t + " pode mexer em blocos na área protegida");
				e.snapTo(dentro.getX() + 50.5, dentro.getY(), dentro.getZ() + 0.5, 0F, 0F);
				helper.assertValueEqual(EventHooks.canEntityGrief(level, e), level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING),
						"fora da área a regra normal vale");
			}
			// Política pura
			helper.assertTrue(PoliticaProtecao.isento(true, true), "op criativo é isento");
			helper.assertFalse(PoliticaProtecao.isento(true, false), "op em sobrevivência não é isento");
			helper.assertFalse(PoliticaProtecao.isento(false, true), "criativo sem op não é isento");
			helper.assertTrue(PoliticaProtecao.usoPermitido(Blocks.BIRCH_DOOR.defaultBlockState(), false), "porta pode abrir");
			helper.assertTrue(PoliticaProtecao.usoPermitido(Blocks.LECTERN.defaultBlockState(), true), "atril de mão vazia");
			helper.assertFalse(PoliticaProtecao.usoPermitido(Blocks.LECTERN.defaultBlockState(), false), "atril com item na mão");
			helper.assertFalse(PoliticaProtecao.usoPermitido(Blocks.POTTED_POPPY.defaultBlockState(), true), "vaso não");
			helper.assertFalse(PoliticaProtecao.usoPermitido(Blocks.NOTE_BLOCK.defaultBlockState(), true), "bloco musical não muda de nota");
			helper.assertTrue(ColoniaApi.protegido(level, dentro), "zona registrada protegida");
			helper.assertFalse(ColoniaApi.protegido(level, dentro.offset(50, 0, 0)), "fora da zona não");
			// Mensagens: variam e não fazem spam
			helper.assertTrue(MensagensProtecao.variacoes(MensagensProtecao.Tipo.EDIFICIO) >= 3, "mensagens variadas");
			helper.assertFalse(MensagensProtecao.texto(MensagensProtecao.Tipo.FLOR, 0).equals(MensagensProtecao.texto(MensagensProtecao.Tipo.FLOR, 1)), "mensagens repetidas");
			ServerPlayer p = helper.makeMockServerPlayerInLevel();
			try {
				helper.assertTrue(MensagensProtecao.avisar(p, MensagensProtecao.Tipo.FLOR), "primeiro aviso");
				helper.assertFalse(MensagensProtecao.avisar(p, MensagensProtecao.Tipo.FLOR), "segundo aviso imediato deveria ser suprimido");
			} finally {
				level.getServer().getPlayerList().remove(p);
			}
			fim(ar, 1, () -> {
			});
		});
	}
}
