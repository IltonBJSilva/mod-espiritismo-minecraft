package net.mcreator.modespiritismominecraft.espiritismo.colonia;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.colonia.LayoutColonia.Edificio;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.LayoutColonia.Tipo;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.LayoutColonia.Chao;

import java.util.function.BiConsumer;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.ArrayList;

/**
 * A lista ordenada de etapas ("peças") da construção de Nosso Lar. É determinística: depende só da origem (centro e
 * altura do chão), para que a obra possa ser retomada pelo índice depois de fechar o mundo.
 * <ol>
 * <li>fase 0 — zonas essenciais (Câmaras de Retificação, Ministério do Auxílio, Mirante): feitas de uma vez na
 * primeira chegada;</li>
 * <li>fase 1 — terreno, chunk a chunk, do mais perto da chegada para o mais longe;</li>
 * <li>fase 2 — estruturas (praça, Governadoria, ministérios, muralha, casas, estações...);</li>
 * <li>fase 3 — paisagismo (árvores e flores), sempre depois do chão final.</li>
 * </ol>
 */
public final class PlanoColonia {
	public record Peca(String nome, int fase, int refX, int refZ, boolean essencial, BiConsumer<ServerLevel, Lote> gerar) {
		public Peca(String nome, int fase, int refX, int refZ, BiConsumer<ServerLevel, Lote> gerar) {
			this(nome, fase, refX, refZ, false, gerar);
		}
	}

	private final BlockPos origem;
	private final List<Peca> essenciais = new ArrayList<>();
	private final List<Peca> incrementais = new ArrayList<>();

	public PlanoColonia(BlockPos origem) {
		this.origem = origem.immutable();
		montar();
	}

	public BlockPos origem() {
		return origem;
	}

	public List<Peca> essenciais() {
		return essenciais;
	}

	/** A peça essencial da chegada (Câmaras de Retificação). */
	public Peca chegada() {
		for (Peca p : essenciais)
			if (p.nome().equals("essencial_regeneracao"))
				return p;
		throw new IllegalStateException();
	}

	public Lote loteDe(Peca p) {
		return p.essencial() ? loteEssencial() : loteIncremental();
	}

	public List<Peca> incrementais() {
		return incrementais;
	}

	/** Pontos nomeados absolutos (BlockPos.asLong), mais "centro" e a versão do traçado. */
	public Map<String, Long> pontos() {
		Map<String, Long> m = new LinkedHashMap<>();
		LayoutColonia.pontosRelativos().forEach((k, v) -> m.put(k, origem.offset(v).asLong()));
		m.put("colonia_versao", (long) LayoutColonia.VERSAO);
		return m;
	}

	/** Altura do chão da cidade: média do terreno natural (do gerador, sem árvores) numa grade sobre o disco. */
	public static int alturaDaCidade(ServerLevel level, int cx, int cz) {
		var fonte = level.getChunkSource();
		long soma = 0;
		int n = 0;
		for (int i = -4; i <= 4; i++)
			for (int k = -4; k <= 4; k++) {
				if (i * i + k * k > 16)
					continue;
				int x = cx + i * 36, z = cz + k * 36;
				soma += fonte.getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, fonte.randomState()) - 1;
				n++;
			}
		int g = (int) Math.round((double) soma / Math.max(1, n));
		int minimo = Math.max(level.getMinY() + 4, level.getSeaLevel() + 2);
		return Math.clamp(g, minimo, level.getMaxY() - 64);
	}

	// ================================================================ Montagem

	private void montar() {
		Edificio camaras = LayoutColonia.edificio("regeneracao");
		int chegX = camaras.cx(), chegZ = camaras.cz();
		List<LayoutColonia.Retangulo> zonas = LayoutColonia.zonasEssenciais();

		// Fase 0: zonas essenciais
		for (Edificio e : LayoutColonia.edificios())
			if (e.essencial())
				essenciais.add(new Peca("essencial_" + e.id(), 0, e.cx(), e.cz(), true, (level, l) -> {
					LayoutColonia.Retangulo r = LayoutColonia.apron(e);
					Pecas.terrenoRetangulo(l, r);
					construirEdificio(l, e);
					RandomSource rnd = RandomSource.create(e.id().hashCode());
					for (int x = r.x0(); x <= r.x1(); x++)
						for (int z = r.z0(); z <= r.z1(); z++)
							if (!e.contem(x, z, 1) && LayoutColonia.chao(x, z) == Chao.GRAMA && rnd.nextInt(4) == 0)
								l.planta(l.origem().offset(x, 1, z), rnd.nextInt(4) == 0 ? Blocks.SHORT_GRASS.defaultBlockState() : Pecas.flor(rnd));
				}));

		List<Peca> l1 = new ArrayList<>(), l2 = new ArrayList<>(), l3 = new ArrayList<>();
		// Fase 1: terreno por chunk
		int R = LayoutColonia.R_MARGEM;
		int c0 = Math.floorDiv(origem.getX() - R, 16), c1 = Math.floorDiv(origem.getX() + R, 16);
		int k0 = Math.floorDiv(origem.getZ() - R, 16), k1 = Math.floorDiv(origem.getZ() + R, 16);
		for (int cx = c0; cx <= c1; cx++)
			for (int cz = k0; cz <= k1; cz++) {
				int bx = cx * 16, bz = cz * 16;
				double dx = Math.max(Math.max(origem.getX() - (bx + 15), bx - origem.getX()), 0);
				double dz = Math.max(Math.max(origem.getZ() - (bz + 15), bz - origem.getZ()), 0);
				if (dx * dx + dz * dz > (double) R * R)
					continue;
				final int fbx = bx, fbz = bz;
				l1.add(new Peca("terreno_" + cx + "_" + cz, 1, bx + 8 - origem.getX(), bz + 8 - origem.getZ(), (level, l) -> terrenoChunk(level, l, fbx, fbz)));
			}
		// Fase 2: estruturas
		l2.add(new Peca("praca", 2, 0, 20, (level, l) -> Pecas.praca(l, RandomSource.create(1944L))));
		for (Edificio e : LayoutColonia.edificios())
			if (!e.essencial())
				l2.add(new Peca("edificio_" + e.id(), 2, e.cx(), e.cz(), (level, l) -> construirEdificio(l, e)));
		for (Edificio e : LayoutColonia.edificios())
			if (e.tipo() != Tipo.CASA && e.tipo() != Tipo.ESTACAO && e.tipo() != Tipo.GOVERNADORIA)
				l2.add(new Peca("acesso_" + e.id(), 2, e.cx(), e.cz(), (level, l) -> acesso(l, e)));
		for (int a = 0; a < 360; a += 30) {
			final int fa = a;
			int[] p = LayoutColonia.polar(a + 15, LayoutColonia.R_MURO);
			l2.add(new Peca("muralha_" + a, 2, p[0], p[1], (level, l) -> Pecas.muralha(l, fa, fa + 30)));
		}
		int[] lago = LayoutColonia.polar(120, 124);
		l2.add(new Peca("lago_bosque", 2, lago[0], lago[1], (level, l) -> lago(l)));
		l2.add(new Peca("postes", 2, 0, 0, (level, l) -> postes(l)));
		for (int i = 0; i < LayoutColonia.PORTOES.length; i++) {
			final int ang = LayoutColonia.PORTOES[i];
			int[] p = LayoutColonia.polar(ang, LayoutColonia.R_MURO + 20);
			l2.add(new Peca("estrada_" + LayoutColonia.NOMES_PORTOES[i], 2, p[0], p[1], (level, l) -> estradaExterna(l, ang)));
		}
		// Fase 3: paisagismo por arco de 30°
		for (int a = 0; a < 360; a += 30) {
			final int fa = a;
			int[] p = LayoutColonia.polar(a + 15, 90);
			l3.add(new Peca("jardins_" + a, 3, p[0], p[1], (level, l) -> paisagismo(l, fa, fa + 30)));
		}
		Comparator<Peca> perto = Comparator.comparingDouble(p -> LayoutColonia.raio(p.refX() - chegX, p.refZ() - chegZ));
		l1.sort(perto);
		l2.sort(perto);
		l3.sort(perto);
		// As zonas essenciais que não são a da chegada (Auxílio, Mirante) abrem a fila: em Nosso Lar só a chegada é
		// feita na hora, para não travar o primeiro despertar; fora de Nosso Lar (testes) todas já foram feitas e
		// refazê-las é inofensivo (idempotente).
		for (Peca p : essenciais)
			if (!p.nome().equals("essencial_regeneracao"))
				incrementais.add(p);
		incrementais.addAll(l1);
		incrementais.addAll(l2);
		incrementais.addAll(l3);
	}

	/** Lote para uma peça incremental: não escreve nas zonas essenciais já prontas. */
	public Lote loteIncremental() {
		return new Lote(origem, LayoutColonia.zonasEssenciais());
	}

	public Lote loteEssencial() {
		return new Lote(origem, null);
	}

	static void construirEdificio(Lote l, Edificio e) {
		switch (e.tipo()) {
			case GOVERNADORIA -> Pecas.governadoria(l, e);
			case CAMARAS -> Pecas.camaras(l, e);
			case MINISTERIO -> Pecas.ministerio(l, e);
			case HOSPITAL -> Pecas.hospital(l, e);
			case BIBLIOTECA -> Pecas.biblioteca(l, e);
			case ESCOLA -> Pecas.escola(l, e);
			case RESERVATORIO -> Pecas.reservatorio(l, e);
			case MIRANTE -> Pecas.mirante(l, l.origem().offset(e.cx() + 2, 0, e.cz()));
			case ESTACAO -> Pecas.estacao(l, e, LayoutColonia.destinosAerobus().getOrDefault(e.id(), "").replace("Ministério d", "M. d"));
			case CASA -> Pecas.casa(l, e, RandomSource.create(e.id().hashCode()));
		}
	}

	// ================================================================ Terreno

	private void terrenoChunk(ServerLevel level, Lote l, int bx, int bz) {
		var fonte = level.getChunkSource();
		int g = origem.getY();
		for (int x = bx; x < bx + 16; x++)
			for (int z = bz; z < bz + 16; z++) {
				int dx = x - origem.getX(), dz = z - origem.getZ();
				double r = LayoutColonia.raio(dx, dz);
				if (r <= LayoutColonia.R_PLANO) {
					Chao c = LayoutColonia.chao(dx, dz);
					if (c == Chao.AGUA)
						l.coluna(x, z, g - 2, Blocks.SAND.defaultBlockState(), false);
					else
						l.coluna(x, z, g, Pecas.chaoBloco(c, dx, dz), false);
				} else if (r <= LayoutColonia.R_TALUDE) {
					int natural = fonte.getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, fonte.randomState()) - 1;
					double t = (r - LayoutColonia.R_PLANO) / (LayoutColonia.R_TALUDE - LayoutColonia.R_PLANO);
					double s = t * t * (3 - 2 * t); // smoothstep: começa e termina sem quina
					int alvo = (int) Math.round(g + (natural - g) * s);
					l.coluna(x, z, alvo, Pecas.GRAMA, false);
				} else if (r <= LayoutColonia.R_MARGEM)
					l.coluna(x, z, g, Pecas.GRAMA, true);
			}
	}

	// ================================================================ Estruturas soltas

	private static void acesso(Lote l, Edificio e) {
		int[] f = e.frente(1);
		int sx = e.porta().getStepX(), sz = e.porta().getStepZ();
		int lx = e.porta().getClockWise().getStepX(), lz = e.porta().getClockWise().getStepZ();
		for (int k = 0; k < 70; k++) {
			int x = f[0] + sx * k, z = f[1] + sz * k;
			Chao c = LayoutColonia.chao(x, z);
			if (k > 2 && c != Chao.GRAMA)
				break;
			if (LayoutColonia.raio(x, z) > LayoutColonia.R_MURO - 3)
				break;
			for (int d = -1; d <= 1; d++) {
				int px = x + lx * d, pz = z + lz * d;
				if (LayoutColonia.chao(px, pz) == Chao.GRAMA && !reservadoOutro(px, pz, e))
					l.r(px, 0, pz, d == 0 ? Pecas.CALCITA : Pecas.DIORITO);
			}
		}
	}

	private static boolean reservadoOutro(int x, int z, Edificio proprio) {
		for (Edificio e : LayoutColonia.edificios())
			if (e != proprio && e.contem(x, z, 0))
				return true;
		return false;
	}

	private void lago(Lote l) {
		int[] c = LayoutColonia.polar(120, 124);
		for (int x = c[0] - 19; x <= c[0] + 19; x++)
			for (int z = c[1] - 13; z <= c[1] + 13; z++)
				if (LayoutColonia.lagoBosque(x, z) <= 1.0) {
					l.r(x, -1, z, Pecas.AGUA);
					l.r(x, 0, z, Pecas.AGUA);
				}
		l.placa(l.origem().offset(c[0], 1, c[1] - 13), net.minecraft.core.Direction.NORTH, net.minecraft.ChatFormatting.DARK_AQUA, "Bosque das", "Águas", "Rio Azul", "(Nosso Lar, 10)");
		l.r(c[0], 1, c[1] - 12, Pecas.TIJOLO_QUARTZO);
	}

	private void postes(Lote l) {
		List<int[]> pos = new ArrayList<>();
		for (int a : LayoutColonia.AVENIDAS)
			for (int r = LayoutColonia.R_PRACA + 6; r <= LayoutColonia.R_MURO - 8; r += 16)
				for (int lado : new int[]{-1, 1}) {
					double rad = Math.toRadians(a);
					int x = (int) Math.round(Math.cos(rad) * r - Math.sin(rad) * 4 * lado);
					int z = (int) Math.round(Math.sin(rad) * r + Math.cos(rad) * 4 * lado);
					pos.add(new int[]{x, z});
				}
		int[][] aneis = {{LayoutColonia.ANEL1 + 4, 30}, {LayoutColonia.ANEL2 + 4, 15}, {LayoutColonia.ANEL3 - 4, 10}};
		for (int[] an : aneis)
			for (int a = 5; a < 360; a += an[1])
				pos.add(LayoutColonia.polar(a, an[0]));
		for (int[] p : pos) {
			if (LayoutColonia.chao(p[0], p[1]) != Chao.GRAMA || LayoutColonia.reservado(p[0], p[1], 2))
				continue;
			Pecas.poste(l, l.origem().offset(p[0], 1, p[1]), 3);
		}
	}

	private void estradaExterna(Lote l, int ang) {
		double rad = Math.toRadians(ang);
		double c = Math.cos(rad), s = Math.sin(rad);
		for (int r = LayoutColonia.R_MURO + 2; r <= LayoutColonia.R_TALUDE + 24; r++)
			for (int d = -1; d <= 1; d++) {
				int x = origem.getX() + (int) Math.round(c * r - s * d), z = origem.getZ() + (int) Math.round(s * r + c * d);
				l.superficie(x, z, Blocks.DIRT_PATH.defaultBlockState());
			}
	}

	// ================================================================ Paisagismo

	private void paisagismo(Lote l, int a0, int a1) {
		RandomSource r = RandomSource.create(1944L * 31 + a0);
		BlockPos o = l.origem();
		List<int[]> arvores = new ArrayList<>();
		// Árvores: faixa interna (entre os anéis 1 e 2), faixa externa (bairros) e o Bosque das Águas (denso).
		int[][] faixas = {{LayoutColonia.ANEL1 + 5, LayoutColonia.ANEL2 - 5, 16}, {LayoutColonia.ANEL2 + 5, LayoutColonia.ANEL3 - 5, 14}};
		for (int[] fx : faixas)
			for (int i = 0; i < fx[2] * 3; i++) {
				double ang = a0 + r.nextDouble() * (a1 - a0);
				double raio = fx[0] + r.nextDouble() * (fx[1] - fx[0]);
				boolean bosque = LayoutColonia.Setor.de(ang) == LayoutColonia.Setor.UNIAO_DIVINA && raio > LayoutColonia.ANEL2;
				if (!bosque && i >= fx[2])
					continue;
				int[] p = LayoutColonia.polar(ang, raio);
				if (!livreParaArvore(p[0], p[1], arvores))
					continue;
				arvores.add(p);
				int tipo = bosque ? (r.nextInt(3) == 0 ? 1 : 0) : r.nextInt(3);
				Pecas.arvore(l, o.offset(p[0], 1, p[1]), r, tipo);
			}
		// Flores e grama baixa nos gramados (sempre depois do chão final; a planta checa canSurvive ao ser posta).
		int[] cx = caixaArco(a0, a1, LayoutColonia.R_PRACA, LayoutColonia.R_MURO);
		for (int x = cx[0]; x <= cx[2]; x++)
			for (int z = cx[1]; z <= cx[3]; z++) {
				if (pertoDePonto(x, z, 1))
					continue;
				double raio = LayoutColonia.raio(x, z);
				if (raio <= LayoutColonia.R_PRACA + 1 || raio >= LayoutColonia.R_MURO - 3)
					continue;
				if (!Pecas.entre(LayoutColonia.angulo(x, z), a0, a1))
					continue;
				Chao c = LayoutColonia.chao(x, z);
				if (c == Chao.AGUA) {
					if (r.nextInt(14) == 0)
						l.planta(o.offset(x, 1, z), Blocks.LILY_PAD.defaultBlockState());
					continue;
				}
				if (c != Chao.GRAMA || LayoutColonia.dentroDeEdificio(x, z, 0))
					continue;
				boolean bosque = LayoutColonia.Setor.de(LayoutColonia.angulo(x, z)) == LayoutColonia.Setor.UNIAO_DIVINA && raio > LayoutColonia.ANEL2;
				int sorte = r.nextInt(100);
				BlockState planta = null;
				if (sorte < (bosque ? 16 : 9))
					planta = Pecas.flor(r);
				else if (sorte < (bosque ? 24 : 14))
					planta = Blocks.SHORT_GRASS.defaultBlockState();
				else if (sorte == 99 && !bosque)
					planta = Blocks.ROSE_BUSH.defaultBlockState();
				if (planta != null)
					l.planta(o.offset(x, 1, z), planta);
			}
	}

	private static java.util.List<int[]> PONTOS_CHAO;

	/** Perto (raio quadrado) de um ponto nomeado? Os pontos ficam sempre livres de plantas e árvores. */
	static synchronized boolean pertoDePonto(int x, int z, int raio) {
		if (PONTOS_CHAO == null) {
			PONTOS_CHAO = new ArrayList<>();
			for (BlockPos p : LayoutColonia.pontosRelativos().values())
				PONTOS_CHAO.add(new int[]{p.getX(), p.getZ()});
		}
		for (int[] p : PONTOS_CHAO)
			if (Math.abs(p[0] - x) <= raio && Math.abs(p[1] - z) <= raio)
				return true;
		return false;
	}

	/** Caixa (x0, z0, x1, z1) que contém o setor de anel entre os ângulos a0..a1 e raios rMin..rMax. */
	static int[] caixaArco(double a0, double a1, double rMin, double rMax) {
		double x0 = Double.MAX_VALUE, z0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
		for (double a = a0; a <= a1 + 1e-6; a += Math.min(5, a1 - a0)) {
			for (double r : new double[]{rMin, rMax}) {
				double x = Math.cos(Math.toRadians(a)) * r, z = Math.sin(Math.toRadians(a)) * r;
				x0 = Math.min(x0, x);
				z0 = Math.min(z0, z);
				x1 = Math.max(x1, x);
				z1 = Math.max(z1, z);
			}
			if (a1 - a0 <= 0)
				break;
		}
		return new int[]{(int) Math.floor(x0) - 2, (int) Math.floor(z0) - 2, (int) Math.ceil(x1) + 2, (int) Math.ceil(z1) + 2};
	}

	private static boolean livreParaArvore(int x, int z, List<int[]> outras) {
		for (int dx = -2; dx <= 2; dx++)
			for (int dz = -2; dz <= 2; dz++)
				if (LayoutColonia.chao(x + dx, z + dz) != Chao.GRAMA)
					return false;
		if (LayoutColonia.reservado(x, z, 4) || LayoutColonia.lagoBosque(x, z) <= 2.2 || pertoDePonto(x, z, 4))
			return false;
		for (int[] o : outras)
			if (Math.abs(o[0] - x) < 6 && Math.abs(o[1] - z) < 6)
				return false;
		return true;
	}
}
