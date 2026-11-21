package net.mcreator.modespiritismominecraft.espiritismo.dimensao;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Construções do Umbral: vários Postos de Socorro (PROMPT §5.2) distribuídos deterministicamente pela seed, com
 * farol de luz visível de longe, área de triagem e acolhimento, leitos de repouso, ponto de conversa/estudo,
 * pequena biblioteca com mensagens educativas e um ponto de chegada dos resgates / saída assistida; e marcos de
 * orientação (lanternas) ligando o Posto central aos demais.
 * <p>
 * Construção INCREMENTAL: cada posto vira uma lista determinística de operações ({@link Op}); o servidor executa
 * no máximo {@link #BLOCOS_POR_TICK} por tick, só em chunks já carregados (nunca gera chunks em massa), e o índice
 * do progresso fica salvo no Level ({@link RegistroDimensao#OBRAS}) — retomável após reinício.
 * Um posto só começa quando o chunk dele é carregado por algum jogador (aproximação), exceto o Posto central
 * (índice 0, na origem), garantido na primeira chegada ao Umbral.
 */
public final class ConstrucoesUmbral {
	public static final int POSTOS = 7;
	public static final int BLOCOS_POR_TICK = 256;
	private static final int FLAGS = Block.UPDATE_CLIENTS;
	private static final int MARGEM = 4;
	private static final int PASSO_MARCO = 48;
	public static final String TAG_POSTO = "espiritismo_posto";

	/** Posto planejado (antes de construir): índice, centro aproximado e tamanho (0 pequeno, 1 médio, 2 avançado). */
	public record Planejado(int indice, int x, int z, int tamanho) {
	}

	enum Tipo {
		SET, COLUNA, PLACA
	}

	record Op(BlockPos pos, BlockState estado, Tipo tipo, String[] texto, ChatFormatting cor) {
		static Op set(BlockPos p, BlockState s) {
			return new Op(p, s, Tipo.SET, null, null);
		}
	}

	/** Obra em andamento (transiente; reconstruída do Level após reinício). */
	private static final class Obra {
		final int indice;
		final List<Op> ops;
		int i;

		Obra(int indice, List<Op> ops, int i) {
			this.indice = indice;
			this.ops = ops;
			this.i = i;
		}
	}

	private static final Map<ResourceKey<Level>, Map<Integer, Obra>> ATIVAS = new HashMap<>();
	/** Caixas protegidas por Level (cache de {@link #dentroDePosto}). */
	private static final Map<ResourceKey<Level>, List<int[]>> CAIXAS = new HashMap<>();

	private ConstrucoesUmbral() {
	}

	// ================================================================ Planejamento determinístico

	/** Posições dos postos para uma seed (o índice 0 é o Posto central na origem). Regra pura e determinística. */
	public static List<Planejado> planejar(long seed) {
		List<Planejado> r = new ArrayList<>();
		r.add(new Planejado(0, 0, 0, 1));
		RandomSource rand = RandomSource.create(seed ^ 0x5EED_0F_50C0L);
		double giro = rand.nextDouble() * Math.PI * 2;
		int[] aneis = {170, 280, 390};
		for (int i = 1; i < POSTOS; i++) {
			int anel = (i - 1) % 3;
			double ang = giro + (i - 1) * Math.PI / 3 + (rand.nextDouble() - 0.5) * Math.toRadians(24);
			int dist = aneis[anel] + rand.nextInt(41);
			r.add(new Planejado(i, (int) Math.round(Math.cos(ang) * dist), (int) Math.round(Math.sin(ang) * dist), anel));
		}
		return r;
	}

	static int[] dimensoes(int tamanho) {
		return switch (tamanho) {
			case 0 -> new int[]{4, 4, 3};
			case 1 -> new int[]{4, 4, 3}; // o central mantém a planta 9x9 conhecida
			default -> new int[]{7, 5, 4};
		};
	}

	// ================================================================ Plano de operações (geometria pura)

	/** Operações para construir o posto {@code indice} com o centro do piso em {@code o}. */
	static List<Op> plano(BlockPos o, int tamanho, int indice) {
		int[] dim = dimensoes(tamanho);
		int w = dim[0], d = dim[1], h = dim[2];
		List<Op> ops = new ArrayList<>();
		BlockState barro = Blocks.PACKED_MUD.defaultBlockState();
		// 1. Terreno firme (inclui a frente, a trilha e o ponto de chegada)
		for (int x = -w - MARGEM; x <= w + MARGEM; x++)
			for (int z = -d - MARGEM; z <= d + MARGEM + 4; z++)
				ops.add(new Op(o.offset(x, 0, z), Math.abs(x) <= 1 && z > d ? Blocks.COARSE_DIRT.defaultBlockState() : barro, Tipo.COLUNA, null, null));
		// 2. Piso, paredes e telhado
		BlockState parede = Blocks.STONE_BRICKS.defaultBlockState();
		for (int x = -w; x <= w; x++)
			for (int z = -d; z <= d; z++) {
				ops.add(Op.set(o.offset(x, 0, z), Blocks.SPRUCE_PLANKS.defaultBlockState()));
				boolean borda = Math.abs(x) == w || Math.abs(z) == d;
				boolean canto = Math.abs(x) == w && Math.abs(z) == d;
				for (int hh = 1; hh <= h; hh++) {
					BlockState s;
					if (!borda)
						s = Blocks.AIR.defaultBlockState();
					else if (canto)
						s = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
					else if (hh == 2 && (Math.abs(x) % 3 == 2 || Math.abs(z) % 3 == 2))
						s = Blocks.GLASS_PANE.defaultBlockState();
					else
						s = parede;
					ops.add(Op.set(o.offset(x, hh, z), s));
				}
				ops.add(Op.set(o.offset(x, h + 1, z), Blocks.SPRUCE_SLAB.defaultBlockState()));
			}
		// 3. Porta (arco aberto) ao sul e placa
		ops.add(Op.set(o.offset(0, 1, d), Blocks.AIR.defaultBlockState()));
		ops.add(Op.set(o.offset(0, 2, d), Blocks.AIR.defaultBlockState()));
		String nome = indice == 0 ? "Entre e" : tamanho == 2 ? "(avançado)" : "nº " + (indice + 1);
		String fim = indice == 0 ? "descanse" : "Entre e descanse";
		ops.add(new Op(o.offset(0, Math.min(3, h), d + 1), wallSign(Direction.SOUTH), Tipo.PLACA, new String[]{"Posto de", "Socorro", nome, fim}, ChatFormatting.DARK_BLUE));
		// 4. Luz suave interna
		for (int lx = -w + 2; lx <= w - 2; lx += w - 2)
			for (int lz : new int[]{-2, 2})
				ops.add(Op.set(o.offset(lx, h, lz), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true)));
		// 5. Leitos de repouso (parede norte)
		for (int x = -w + 1; x <= w - 1; x += 2) {
			BlockState cama = Blocks.LIGHT_GRAY_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
			ops.add(Op.set(o.offset(x, 1, -d + 2), cama.setValue(BedBlock.PART, BedPart.FOOT)));
			ops.add(Op.set(o.offset(x, 1, -d + 1), cama.setValue(BedBlock.PART, BedPart.HEAD)));
		}
		ops.add(new Op(o.offset(0, Math.min(3, h), -d + 1), wallSign(Direction.SOUTH), Tipo.PLACA, new String[]{"Leitos de", "repouso", "clique na cama", "para descansar"}, ChatFormatting.DARK_GREEN));
		// 6. Triagem e acolhimento (perto da porta), água e o ponto de conversa/estudo
		ops.add(Op.set(o.offset(w - 1, 1, d - 2), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)));
		ops.add(new Op(o.offset(w - 1, Math.min(3, h), d - 1), wallSign(Direction.NORTH), Tipo.PLACA, new String[]{"Triagem e", "acolhimento", "fale com o", "socorrista"}, ChatFormatting.DARK_GREEN));
		ops.add(Op.set(o.offset(-w + 1, 1, d - 2), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST)));
		ops.add(Op.set(o.offset(-w + 1, 1, d - 1), Blocks.BOOKSHELF.defaultBlockState()));
		ops.add(Op.set(o.offset(-w + 1, 2, d - 1), Blocks.BOOKSHELF.defaultBlockState()));
		if (tamanho >= 2)
			for (int z = -1; z <= 1; z++)
				ops.add(Op.set(o.offset(-w + 1, 1, z), Blocks.BOOKSHELF.defaultBlockState()));
		// Mensagens educativas (paráfrases com referência)
		String[][] ensinos = {{"Ponto de", "conversa", "clique no", "atril"}, {"A prece sincera", "é sempre", "ouvida.", "ESE cap. XXVII"},
				{"Fora da caridade", "não há", "salvação.", "ESE cap. XV"}, {"O Espírito", "progride sempre;", "não retrograda.", "LE q. 118"}};
		int[] xs = {-w + 1, -w + 2, w - 2, -w + 3};
		for (int k = 0; k < ensinos.length; k++) {
			if (xs[k] >= w - 1 || (k == 3 && w < 5))
				continue;
			ops.add(new Op(o.offset(xs[k], Math.min(3, h), d - 1), wallSign(Direction.NORTH), Tipo.PLACA, ensinos[k], k == 0 ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_BLUE));
		}
		// 7. Farol: luz visível de longe na névoa (feixe claro) e postes de lanterna na entrada
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				ops.add(Op.set(o.offset(x, h + 1, z), Blocks.IRON_BLOCK.defaultBlockState()));
		ops.add(Op.set(o.offset(0, h + 2, 0), Blocks.BEACON.defaultBlockState()));
		ops.add(Op.set(o.offset(0, h + 3, 0), Blocks.WHITE_STAINED_GLASS.defaultBlockState()));
		for (int x : new int[]{-2, 2}) {
			ops.add(Op.set(o.offset(x, 1, d + 2), Blocks.SPRUCE_FENCE.defaultBlockState()));
			ops.add(Op.set(o.offset(x, 2, d + 2), Blocks.SPRUCE_FENCE.defaultBlockState()));
			ops.add(Op.set(o.offset(x, 3, d + 2), Blocks.LANTERN.defaultBlockState()));
		}
		// 8. Ponto de chegada dos resgates e de saída assistida (pequeno pátio iluminado)
		for (int x = -1; x <= 1; x++)
			for (int z = 4; z <= 6; z++)
				ops.add(Op.set(o.offset(x, 0, d + z), x == 0 && z == 5 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.CALCITE.defaultBlockState()));
		return ops;
	}

	/** Pontos nomeados do posto {@code i} com centro do piso em {@code o}. O central (0) também recebe os nomes antigos. */
	static Map<String, Long> pontosDoPosto(BlockPos o, int tamanho, int i) {
		int[] dim = dimensoes(tamanho);
		int w = dim[0], d = dim[1];
		Map<String, Long> p = new LinkedHashMap<>();
		String b = "posto_" + i;
		p.put(b, o.above().asLong());
		p.put(b + "_porta", o.offset(0, 1, d + 2).asLong());
		p.put(b + "_chegada", o.offset(0, 1, d + 5).asLong());
		p.put(b + "_saida", o.offset(0, 1, d + 5).asLong());
		p.put(b + "_triagem", o.offset(w - 2, 1, d - 1).asLong());
		p.put(b + "_conversa", o.offset(-w + 1, 1, d - 2).asLong());
		p.put(b + "_leito", o.offset(0, 1, -d + 3).asLong());
		if (i == 0) {
			p.put("posto", o.above().asLong());
			p.put("posto_porta", o.offset(0, 1, d + 2).asLong());
		}
		return p;
	}

	private static BlockState wallSign(Direction frente) {
		return Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, frente);
	}

	// ================================================================ Execução

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		if (level.isOutsideBuildHeight(pos))
			return;
		BlockState atual = level.getBlockState(pos);
		if (atual == state || atual.is(Blocks.BEDROCK))
			return;
		level.setBlock(pos, state, FLAGS);
	}

	static int chao(ServerLevel level, int x, int z) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
	}

	private static void executar(ServerLevel level, Op op) {
		switch (op.tipo()) {
			case SET -> set(level, op.pos(), op.estado());
			case COLUNA -> {
				BlockPos c = op.pos();
				int alto = Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE, c.getX(), c.getZ()), c.getY() + 16);
				for (int yy = c.getY() + 1; yy <= alto; yy++)
					set(level, new BlockPos(c.getX(), yy, c.getZ()), Blocks.AIR.defaultBlockState());
				set(level, c, op.estado());
				for (int yy = c.getY() - 1; yy >= c.getY() - 8; yy--) {
					BlockPos p = new BlockPos(c.getX(), yy, c.getZ());
					BlockState s = level.getBlockState(p);
					if (!s.canBeReplaced() && s.getFluidState().isEmpty())
						break;
					set(level, p, Blocks.PACKED_MUD.defaultBlockState());
				}
			}
			case PLACA -> {
				set(level, op.pos(), op.estado());
				if (level.getBlockEntity(op.pos()) instanceof SignBlockEntity sign) {
					SignText texto = new SignText();
					for (int i = 0; i < Math.min(4, op.texto().length); i++)
						texto = texto.setMessage(i, Component.literal(op.texto()[i]).withStyle(op.cor()));
					sign.setText(texto, true);
					sign.setWaxed(true);
					sign.setChanged();
					level.sendBlockUpdated(op.pos(), sign.getBlockState(), sign.getBlockState(), FLAGS);
				}
			}
		}
	}

	// ================================================================ Persistência das obras

	private static Map<String, Long> obras(ServerLevel level) {
		return level.getData(RegistroDimensao.OBRAS);
	}

	private static void salvarObras(ServerLevel level, Map<String, Long> m) {
		level.setData(RegistroDimensao.OBRAS, m);
	}

	public static boolean iniciado(ServerLevel level, int i) {
		return obras(level).containsKey("p" + i + ".y");
	}

	public static boolean concluido(ServerLevel level, int i) {
		return obras(level).getOrDefault("p" + i + ".ok", 0L) == 1L;
	}

	/** Registra (persistente) e enfileira a construção incremental do posto {@code i} com centro do piso em {@code o}. */
	public static void iniciarObra(ServerLevel level, int i, BlockPos o, int tamanho) {
		Map<String, Long> m = obras(level);
		if (m.containsKey("p" + i + ".y"))
			return;
		m.put("p" + i + ".x", (long) o.getX());
		m.put("p" + i + ".y", (long) o.getY());
		m.put("p" + i + ".z", (long) o.getZ());
		m.put("p" + i + ".t", (long) tamanho);
		m.put("p" + i + ".i", 0L);
		salvarObras(level, m);
		Map<String, Long> pontos = Planos.pontos(level);
		pontos.putAll(pontosDoPosto(o, tamanho, i));
		level.setData(RegistroDimensao.PONTOS, pontos);
		CAIXAS.remove(level.dimension());
		ATIVAS.computeIfAbsent(level.dimension(), k -> new LinkedHashMap<>()).put(i, new Obra(i, plano(o, tamanho, i), 0));
		ModEspiritismoMinecraftMod.LOGGER.info("Umbral: Posto de Socorro {} iniciado em {} (tamanho {})", i, o, tamanho);
	}

	/** Recoloca na fila as obras começadas e não concluídas (após reinício do servidor). */
	public static void retomar(ServerLevel level) {
		Map<String, Long> m = obras(level);
		Map<Integer, Obra> fila = ATIVAS.computeIfAbsent(level.dimension(), k -> new LinkedHashMap<>());
		for (int i = 0; i < 100; i++) {
			Long y = m.get("p" + i + ".y");
			if (y == null || m.getOrDefault("p" + i + ".ok", 0L) == 1L || fila.containsKey(i))
				continue;
			BlockPos o = new BlockPos(m.get("p" + i + ".x").intValue(), y.intValue(), m.get("p" + i + ".z").intValue());
			fila.put(i, new Obra(i, plano(o, m.getOrDefault("p" + i + ".t", 0L).intValue(), i), m.getOrDefault("p" + i + ".i", 0L).intValue()));
		}
	}

	/** Executa até {@code orcamento} operações das obras deste Level. Devolve quantas executou. */
	public static int processar(ServerLevel level, int orcamento) {
		Map<Integer, Obra> fila = ATIVAS.get(level.dimension());
		if (fila == null || fila.isEmpty())
			return 0;
		int feitos = 0;
		Map<String, Long> m = obras(level);
		var it = fila.values().iterator();
		while (it.hasNext() && feitos < orcamento) {
			Obra ob = it.next();
			while (ob.i < ob.ops.size() && feitos < orcamento) {
				Op op = ob.ops.get(ob.i);
				if (!level.isLoaded(op.pos()))
					break; // chunk descarregado: pausa sem gerar nada
				executar(level, op);
				ob.i++;
				feitos++;
			}
			m.put("p" + ob.indice + ".i", (long) ob.i);
			if (ob.i >= ob.ops.size()) {
				m.put("p" + ob.indice + ".ok", 1L);
				it.remove();
				ModEspiritismoMinecraftMod.LOGGER.info("Umbral: Posto de Socorro {} concluído", ob.indice);
			}
		}
		salvarObras(level, m);
		return feitos;
	}

	/** Esquece as obras em memória (simula reinício; usado nos testes). */
	public static void esquecerFila(ServerLevel level) {
		ATIVAS.remove(level.dimension());
	}

	// ================================================================ Garantir / construir

	/** Garante o Posto central (incremental) e o ponto de chegada dos que despertam no Umbral; devolve a chegada. */
	public static BlockPos garantir(ServerLevel level) {
		Map<String, Long> p = Planos.pontos(level);
		if (p.containsKey("chegada"))
			return BlockPos.of(p.get("chegada"));
		BlockPos o = new BlockPos(0, Math.clamp(chao(level, 0, 0), level.getSeaLevel() + 2, 100), 0);
		BlockPos chegada = chegadaLegada(level, o);
		p = Planos.pontos(level);
		p.put("chegada", chegada.asLong());
		level.setData(RegistroDimensao.PONTOS, p);
		iniciarObra(level, 0, o, 1);
		return chegada;
	}

	/** Ponto de chegada a ~30 blocos ao sul do Posto central, sobre um pequeno chão firme (construído na hora: 9 colunas). */
	private static BlockPos chegadaLegada(ServerLevel level, BlockPos o) {
		int cz = o.getZ() + 30;
		int cy = Math.max(chao(level, o.getX(), cz), level.getSeaLevel());
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				executar(level, new Op(new BlockPos(o.getX() + x, cy, cz + z), Blocks.MUD.defaultBlockState(), Tipo.COLUNA, null, null));
		return new BlockPos(o.getX(), cy + 1, cz);
	}

	/** Constrói o Posto central INTEIRO agora, com centro em {@code o} (testes e administração). Devolve os pontos. */
	public static Map<String, Long> construirAgora(ServerLevel level, BlockPos o) {
		for (Op op : plano(o, 1, 0))
			executar(level, op);
		Map<String, Long> pontos = new HashMap<>(pontosDoPosto(o, 1, 0));
		pontos.put("chegada", chegadaLegada(level, o).asLong());
		RandomSource r = RandomSource.create(1943L);
		for (int i = 0; i < 9; i++) {
			int x = o.getX() + r.nextInt(31) - 15, z = o.getZ() + 8 + r.nextInt(30);
			if (Math.abs(x - o.getX()) <= 5)
				continue;
			int y = chao(level, x, z);
			if (level.getFluidState(new BlockPos(x, y, z)).isEmpty())
				arvoreSeca(level, new BlockPos(x, y + 1, z), r);
		}
		return pontos;
	}

	private static void arvoreSeca(ServerLevel level, BlockPos base, RandomSource r) {
		BlockState tronco = Blocks.DARK_OAK_LOG.defaultBlockState();
		int h = 3 + r.nextInt(3);
		for (int y = 0; y < h; y++)
			set(level, base.above(y), tronco);
		Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
		set(level, base.above(h - 1).relative(d), Blocks.DARK_OAK_FENCE.defaultBlockState());
		set(level, base.above(h).relative(d.getOpposite()), Blocks.DARK_OAK_FENCE.defaultBlockState());
	}

	// ================================================================ Tick do servidor

	/** Chamado a cada tick do servidor: executa as obras (orçamento por tick) e, a cada segundo, inicia postos e marcos próximos de jogadores. */
	public static void tickServidor(MinecraftServer server) {
		ServerLevel umbral = server.getLevel(Planos.UMBRAL);
		if (umbral != null && server.getTickCount() % 20 == 7 && !umbral.players().isEmpty()) {
			retomar(umbral);
			if (Planos.pontos(umbral).containsKey("chegada")) {
				iniciarProximos(umbral);
				marcos(umbral);
			}
		}
		int orcamento = BLOCOS_POR_TICK;
		for (ResourceKey<Level> k : List.copyOf(ATIVAS.keySet())) {
			ServerLevel l = server.getLevel(k);
			if (l == null) {
				ATIVAS.remove(k);
				continue;
			}
			orcamento -= processar(l, orcamento);
			if (orcamento <= 0)
				break;
		}
	}

	/** Inicia os postos planejados cuja área já está carregada (sem gerar chunks). */
	static void iniciarProximos(ServerLevel level) {
		for (Planejado pl : planejar(level.getSeed())) {
			if (pl.indice() == 0 || iniciado(level, pl.indice()))
				continue;
			int[] dim = dimensoes(pl.tamanho());
			int rx = dim[0] + MARGEM, rz = dim[1] + MARGEM + 4;
			if (!carregado(level, pl.x(), pl.z(), rx, rz))
				continue;
			BlockPos o = escolherLocal(level, pl, rx, rz);
			iniciarObra(level, pl.indice(), o, pl.tamanho());
		}
	}

	private static boolean carregado(ServerLevel level, int x, int z, int rx, int rz) {
		return level.isLoaded(new BlockPos(x - rx, 0, z - rx)) && level.isLoaded(new BlockPos(x + rx, 0, z + rz)) && level.isLoaded(new BlockPos(x - rx, 0, z + rz))
				&& level.isLoaded(new BlockPos(x + rx, 0, z - rx));
	}

	/** Evita água/lava: procura, entre posições já carregadas perto do planejado, um chão seco; senão faz um aterro. */
	private static BlockPos escolherLocal(ServerLevel level, Planejado pl, int rx, int rz) {
		int[][] tentativas = {{0, 0}, {12, 0}, {-12, 0}, {0, 12}, {0, -12}, {16, 16}, {-16, 16}, {16, -16}, {-16, -16}};
		for (int[] t : tentativas) {
			int x = pl.x() + t[0], z = pl.z() + t[1];
			if (!carregado(level, x, z, rx, rz))
				continue;
			int y = chao(level, x, z);
			BlockPos topo = new BlockPos(x, y, z);
			if (level.getFluidState(topo).isEmpty() && y > level.getSeaLevel() && y < 110)
				return topo;
		}
		int y = Math.clamp(chao(level, pl.x(), pl.z()), level.getSeaLevel() + 1, 100);
		return new BlockPos(pl.x(), y, pl.z());
	}

	/** Marcos de orientação (raros): um poste com lanterna a cada {@link #PASSO_MARCO} blocos entre o Posto central e os demais. */
	static void marcos(ServerLevel level) {
		Map<String, Long> m = obras(level);
		boolean mudou = false;
		for (Planejado pl : planejar(level.getSeed())) {
			if (pl.indice() == 0)
				continue;
			double dist = Math.sqrt((double) pl.x() * pl.x() + (double) pl.z() * pl.z());
			for (int k = 1; k * PASSO_MARCO < dist - 24; k++) {
				String chave = "m" + pl.indice() + "_" + k + ".ok";
				if (m.containsKey(chave))
					continue;
				int x = (int) Math.round(pl.x() * (k * PASSO_MARCO / dist)) + 3, z = (int) Math.round(pl.z() * (k * PASSO_MARCO / dist));
				BlockPos base = new BlockPos(x, 0, z);
				if (!level.isLoaded(base))
					continue;
				int y = chao(level, x, z);
				BlockPos c = new BlockPos(x, y, z);
				set(level, c, Blocks.PACKED_MUD.defaultBlockState());
				set(level, c.above(), Blocks.SPRUCE_FENCE.defaultBlockState());
				set(level, c.above(2), Blocks.SPRUCE_FENCE.defaultBlockState());
				set(level, c.above(3), Blocks.LANTERN.defaultBlockState());
				m.put(chave, 1L);
				mudou = true;
			}
		}
		if (mudou)
			salvarObras(level, m);
	}

	// ================================================================ Consultas

	/** Centros (piso + 1) dos postos já iniciados neste Level. */
	public static List<BlockPos> postos(ServerLevel level) {
		List<BlockPos> r = new ArrayList<>();
		Map<String, Long> p = Planos.pontos(level);
		for (int i = 0; i < POSTOS; i++) {
			Long v = p.get("posto_" + i);
			if (v != null)
				r.add(BlockPos.of(v));
		}
		return r;
	}

	/** Índice do posto iniciado mais próximo (ou -1). */
	public static int maisProximo(ServerLevel level, BlockPos de) {
		Map<String, Long> p = Planos.pontos(level);
		int melhor = -1;
		double md = Double.MAX_VALUE;
		for (int i = 0; i < POSTOS; i++) {
			Long v = p.get("posto_" + i);
			if (v == null)
				continue;
			double d = BlockPos.of(v).distSqr(de);
			if (d < md) {
				md = d;
				melhor = i;
			}
		}
		return melhor;
	}

	/** Distância (blocos) até o posto iniciado mais próximo; MAX_VALUE se não houver. */
	public static double distanciaPosto(ServerLevel level, BlockPos de) {
		int i = maisProximo(level, de);
		return i < 0 ? Double.MAX_VALUE : Math.sqrt(BlockPos.of(Planos.pontos(level).get("posto_" + i)).distSqr(de));
	}

	/** O bloco pertence à construção de algum Posto de Socorro (proteção contra quebra/colocação/explosão)? */
	public static boolean dentroDePosto(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel sl))
			return false;
		List<int[]> caixas = CAIXAS.computeIfAbsent(sl.dimension(), k -> caixas(sl));
		for (int[] c : caixas)
			if (pos.getX() >= c[0] && pos.getX() <= c[3] && pos.getY() >= c[1] && pos.getY() <= c[4] && pos.getZ() >= c[2] && pos.getZ() <= c[5])
				return true;
		return false;
	}

	private static List<int[]> caixas(ServerLevel level) {
		List<int[]> r = new ArrayList<>();
		Map<String, Long> m = obras(level);
		for (int i = 0; i < 100; i++) {
			Long y = m.get("p" + i + ".y");
			if (y == null)
				continue;
			int x = m.get("p" + i + ".x").intValue(), z = m.get("p" + i + ".z").intValue();
			int[] dim = dimensoes(m.getOrDefault("p" + i + ".t", 0L).intValue());
			r.add(new int[]{x - dim[0] - 1, y.intValue() - 1, z - dim[1] - 1, x + dim[0] + 1, y.intValue() + dim[2] + 4, z + dim[1] + 7});
		}
		return r;
	}

	// ================================================================ NPCs dos postos (lookup por ID, tolerante)

	public static EntityType<?> tipo(String id) {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, id)).orElse(null);
	}

	@Nullable
	public static Entity colocar(ServerLevel level, EntityType<?> tipo, BlockPos pos, float yRot) {
		Entity e = tipo.create(level, EntitySpawnReason.STRUCTURE);
		if (e == null)
			return null;
		e.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yRot, 0F);
		if (e instanceof Mob mob) {
			mob.setYHeadRot(yRot);
			mob.setYBodyRot(yRot);
			mob.setPersistenceRequired();
			mob.setHomeTo(pos, 4);
		}
		level.addFreshEntity(e);
		return e;
	}

	/** Coloca um NPC de papel fixo num ponto, se ainda não houver um do mesmo tipo a até {@code raio} blocos. Só com chunk ativo. */
	@Nullable
	static Entity colocarSeFaltar(ServerLevel level, String id, BlockPos pos, float yRot, double raio, @Nullable String papel) {
		EntityType<?> tipo = tipo(id);
		if (tipo == null || !level.isPositionEntityTicking(pos))
			return null;
		if (!level.getEntities(tipo, new AABB(pos).inflate(raio), e -> true).isEmpty())
			return null;
		Entity e = colocar(level, tipo, pos, yRot);
		if (e != null) {
			e.addTag(TAG_POSTO);
			if (papel != null) {
				e.setCustomName(Component.literal(papel).withStyle(ChatFormatting.GREEN));
				e.setCustomNameVisible(true);
			}
		}
		return e;
	}

	/** Equipes dos postos concluídos perto de {@code perto} (papéis diferentes) e Espíritos perdidos nas rotas. */
	public static void verificarNpcs(ServerLevel level, BlockPos perto) {
		Map<String, Long> p = Planos.pontos(level);
		for (int i = 0; i < POSTOS; i++) {
			Long centro = p.get("posto_" + i);
			if (centro == null || !concluido(level, i) || BlockPos.of(centro).distSqr(perto) > 80 * 80)
				continue;
			colocarSeFaltar(level, "socorrista", BlockPos.of(p.get("posto_" + i + "_triagem")), 180F, 2, "Socorrista — triagem e acolhimento");
			colocarSeFaltar(level, "socorrista", BlockPos.of(p.get("posto_" + i + "_conversa")).east(), 90F, 2, "Socorrista — orientação");
			colocarSeFaltar(level, "socorrista", BlockPos.of(p.get("posto_" + i + "_leito")), 0F, 2, "Socorrista — enfermagem");
			BlockPos rota = BlockPos.of(p.get("posto_" + i + "_chegada")).offset(9, 0, 7);
			EntityType<?> sofredor = tipo("espirito_sofredor");
			if (i > 0 && sofredor != null && level.isPositionEntityTicking(rota) && level.getEntities(sofredor, new AABB(rota).inflate(16), e -> true).isEmpty())
				colocar(level, sofredor, new BlockPos(rota.getX(), chao(level, rota.getX(), rota.getZ()) + 1, rota.getZ()), 0F);
		}
		if (p.containsKey("chegada")) {
			BlockPos c = BlockPos.of(p.get("chegada"));
			EntityType<?> sofredor = tipo("espirito_sofredor");
			if (sofredor != null && c.distSqr(perto) < 64 * 64 && level.isPositionEntityTicking(c) && level.getEntities(sofredor, new AABB(c).inflate(24), e -> true).size() < 2)
				for (int[] d : new int[][]{{7, -4}, {-8, 3}}) {
					int x = c.getX() + d[0], z = c.getZ() + d[1];
					colocar(level, sofredor, new BlockPos(x, chao(level, x, z) + 1, z), 0F);
				}
		}
	}
}
