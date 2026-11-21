package net.mcreator.modespiritismominecraft.espiritismo.colonia;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/**
 * Mapa lógico de Nosso Lar — só geometria, sem tocar no mundo (testável).
 * <p>
 * Inspiração: <i>Nosso Lar</i> (André Luiz / Chico Xavier, FEB, 1944), cap. 8 — a Governadoria fica no centro de uma
 * praça e é o "ponto de convergência dos seis ministérios", que dela partem em forma triangular; cap. 10 — o aeróbus,
 * carro aéreo suspenso a uns cinco metros do solo, com paradas pelo caminho, leva ao Bosque das Águas (Rio Azul),
 * cuja distribuição de águas cabe ao Ministério da União Divina. A muralha, os portões e a torre de luz central vêm da
 * representação do filme (2010); o livro não descreve muralhas.
 * <p>
 * Coordenadas relativas ao centro da praça da Governadoria (dx, dz); y = 0 é o nível do chão da cidade (g).
 * Ângulos em graus no plano do Minecraft: 0 = leste (+X), 90 = sul (+Z), 180 = oeste, 270 = norte.
 */
public final class LayoutColonia {
	private LayoutColonia() {
	}

	/** Versão do traçado (gravada nos pontos; muda se o mapa mudar de forma incompatível). */
	public static final int VERSAO = 2;

	public static final int R_PRACA = 27;
	public static final int ANEL1 = 36;
	public static final int R_MINISTERIO = 66;
	public static final int ANEL2 = 96;
	public static final int ANEL3 = 146;
	public static final int R_MURO = 156;
	/** Até aqui o chão é plano na altura da cidade. */
	public static final int R_PLANO = 164;
	/** Entre R_PLANO e R_TALUDE o chão sobe/desce suavemente até o terreno natural (talude). */
	public static final int R_TALUDE = 196;
	/** Faixa externa onde só se fixam as folhas de árvores cortadas pela metade (para não caírem). */
	public static final int R_MARGEM = 204;
	/** Meia-largura das avenidas e anéis (largura total 5). */
	public static final double MEIA_AVENIDA = 2.5;

	/** Avenidas radiais nas divisas dos setores. */
	public static final int[] AVENIDAS = {30, 90, 150, 210, 270, 330};
	/** Portões (as avenidas 90/270 e os eixos 0/180 chegam a eles). */
	public static final int[] PORTOES = {270, 90, 0, 180};
	public static final String[] NOMES_PORTOES = {"portao_norte", "portao_sul", "portao_leste", "portao_oeste"};

	/** Os seis Ministérios (nomes conferidos em Nosso Lar, cap. 8), cada um no centro do seu setor triangular. */
	public enum Setor {
		ESCLARECIMENTO(0, "esclarecimento", "Ministério do", "Esclarecimento"),
		ELEVACAO(60, "elevacao", "Ministério da", "Elevação"),
		UNIAO_DIVINA(120, "uniao_divina", "Ministério da", "União Divina"),
		AUXILIO(180, "auxilio", "Ministério do", "Auxílio"),
		REGENERACAO(240, "regeneracao", "Ministério da", "Regeneração"),
		COMUNICACAO(300, "comunicacao", "Ministério da", "Comunicação");

		public final int angulo;
		public final String id;
		public final String linha1, linha2;

		Setor(int angulo, String id, String linha1, String linha2) {
			this.angulo = angulo;
			this.id = id;
			this.linha1 = linha1;
			this.linha2 = linha2;
		}

		public static Setor de(double anguloGraus) {
			double a = ((anguloGraus % 360) + 360 + 30) % 360;
			return values()[(int) (a / 60)];
		}
	}

	public enum Tipo {
		GOVERNADORIA, MINISTERIO, CAMARAS, HOSPITAL, BIBLIOTECA, ESCOLA, MIRANTE, RESERVATORIO, ESTACAO, CASA
	}

	/**
	 * Um edifício: centro (cx, cz), tamanho em X e Z, lado da porta. {@code essencial} = construído de uma vez na
	 * primeira chegada (área de chegada, atril da reencarnação e mirante), antes da geração incremental.
	 */
	public record Edificio(String id, Tipo tipo, int cx, int cz, int largX, int profZ, Direction porta, boolean essencial, Setor setor, int variante) {
		public int x0() {
			return cx - largX / 2;
		}

		public int z0() {
			return cz - profZ / 2;
		}

		public int x1() {
			return x0() + largX - 1;
		}

		public int z1() {
			return z0() + profZ - 1;
		}

		/** Bloco do chão logo à frente da porta (do lado de fora). */
		public int[] frente(int distancia) {
			return switch (porta) {
				case NORTH -> new int[]{cx, z0() - distancia};
				case SOUTH -> new int[]{cx, z1() + distancia};
				case WEST -> new int[]{x0() - distancia, cz};
				default -> new int[]{x1() + distancia, cz};
			};
		}

		public boolean contem(int dx, int dz, int margem) {
			return dx >= x0() - margem && dx <= x1() + margem && dz >= z0() - margem && dz <= z1() + margem;
		}
	}

	/** Retângulo relativo (inclusivo). */
	public record Retangulo(int x0, int z0, int x1, int z1) {
		public boolean contem(int dx, int dz) {
			return dx >= x0 && dx <= x1 && dz >= z0 && dz <= z1;
		}
	}

	// ================================================================ Geometria básica

	public static int[] polar(double anguloGraus, double raio) {
		double a = Math.toRadians(anguloGraus);
		return new int[]{(int) Math.round(Math.cos(a) * raio), (int) Math.round(Math.sin(a) * raio)};
	}

	public static double raio(double dx, double dz) {
		return Math.sqrt(dx * dx + dz * dz);
	}

	public static double angulo(double dx, double dz) {
		double a = Math.toDegrees(Math.atan2(dz, dx));
		return a < 0 ? a + 360 : a;
	}

	/** Distância perpendicular do ponto ao raio que sai do centro no ângulo dado (infinito se estiver "atrás"). */
	public static double distRaio(double dx, double dz, double anguloGraus) {
		double a = Math.toRadians(anguloGraus);
		double c = Math.cos(a), s = Math.sin(a);
		if (dx * c + dz * s < 0)
			return Double.MAX_VALUE;
		return Math.abs(dx * s - dz * c);
	}

	/** Porta virada para o centro (lado cardeal mais próximo da direção ao centro). */
	public static Direction portaParaCentro(int cx, int cz) {
		if (Math.abs(cx) >= Math.abs(cz))
			return cx > 0 ? Direction.WEST : Direction.EAST;
		return cz > 0 ? Direction.NORTH : Direction.SOUTH;
	}

	// ================================================================ Edifícios

	private static List<Edificio> EDIFICIOS;

	public static synchronized List<Edificio> edificios() {
		if (EDIFICIOS != null)
			return EDIFICIOS;
		List<Edificio> l = new ArrayList<>();
		l.add(new Edificio("governadoria", Tipo.GOVERNADORIA, 0, 0, 25, 25, Direction.SOUTH, false, null, 0));
		for (Setor s : Setor.values()) {
			int[] c = polar(s.angulo, R_MINISTERIO);
			Direction porta = portaParaCentro(c[0], c[1]);
			boolean ns = porta.getAxis() == Direction.Axis.Z;
			if (s == Setor.REGENERACAO)
				l.add(new Edificio("regeneracao", Tipo.CAMARAS, c[0], c[1], ns ? 27 : 19, ns ? 19 : 27, porta, true, s, 0));
			else if (s == Setor.AUXILIO)
				l.add(new Edificio("auxilio", Tipo.MINISTERIO, c[0], c[1], ns ? 23 : 17, ns ? 17 : 23, porta, true, s, 0));
			else
				l.add(new Edificio(s.id, Tipo.MINISTERIO, c[0], c[1], ns ? 23 : 17, ns ? 17 : 23, porta, false, s, 0));
		}
		int[] h = polar(197, 120);
		l.add(new Edificio("hospital", Tipo.HOSPITAL, h[0], h[1], 21, 29, Direction.EAST, false, Setor.AUXILIO, 0));
		int[] b = polar(344, 118);
		l.add(new Edificio("biblioteca", Tipo.BIBLIOTECA, b[0], b[1], 19, 25, Direction.WEST, false, Setor.ESCLARECIMENTO, 0));
		int[] e = polar(16, 118);
		l.add(new Edificio("escola", Tipo.ESCOLA, e[0], e[1], 17, 21, Direction.WEST, false, Setor.ESCLARECIMENTO, 0));
		int[] m = polar(60, 120);
		// O Mirante: terraço 7x7 com escada a oeste (de x-8 a x+3).
		l.add(new Edificio("mirante", Tipo.MIRANTE, m[0] - 2, m[1], 12, 7, Direction.WEST, true, Setor.ELEVACAO, 0));
		int[] r = polar(138, 106);
		l.add(new Edificio("reservatorio", Tipo.RESERVATORIO, r[0], r[1], 13, 11, Direction.NORTH, false, Setor.UNIAO_DIVINA, 0));
		// Estações do aeróbus: a central junto à praça e uma diante de cada Ministério e no Bosque das Águas.
		int[] ec = polar(45, 31);
		l.add(new Edificio("estacao_aerobus", Tipo.ESTACAO, ec[0], ec[1], 7, 7, Direction.NORTH, false, null, 0));
		for (Setor s : Setor.values()) {
			int[] p = polar(s.angulo + 11, ANEL1 + 10);
			l.add(new Edificio("estacao_" + s.id, Tipo.ESTACAO, p[0], p[1], 7, 7, Direction.NORTH, false, s, 0));
		}
		int[] eb = polar(108, 100);
		l.add(new Edificio("estacao_bosque", Tipo.ESTACAO, eb[0], eb[1], 7, 7, Direction.NORTH, false, Setor.UNIAO_DIVINA, 0));
		// Casas nos bairros residenciais (anel externo), fora das avenidas, eixos e áreas reservadas.
		int n = 0;
		for (int raio : new int[]{106, 122, 138}) {
			int quantas = (int) (2 * Math.PI * raio / 16);
			for (int i = 0; i < quantas; i++) {
				double ang = 360.0 * i / quantas + (raio == 122 ? 180.0 / quantas : 0);
				int[] c = polar(ang, raio);
				if (!lugarDeCasa(c[0], c[1], l))
					continue;
				l.add(new Edificio("casa_" + n, Tipo.CASA, c[0], c[1], 7, 7, portaParaCentro(c[0], c[1]), false, Setor.de(ang), n % 4));
				n++;
			}
		}
		EDIFICIOS = List.copyOf(l);
		return EDIFICIOS;
	}

	private static boolean lugarDeCasa(int dx, int dz, List<Edificio> ja) {
		double ang = angulo(dx, dz);
		double r = raio(dx, dz);
		for (int a : AVENIDAS)
			if (distRaio(dx, dz, a) < 10)
				return false;
		for (int a : new int[]{0, 180})
			if (distRaio(dx, dz, a) < 10)
				return false;
		if (Setor.de(ang) == Setor.UNIAO_DIVINA)
			return false; // Bosque das Águas
		for (Edificio e : ja)
			if (e.contem(dx, dz, 9))
				return false;
		return r > ANEL2 + 6 && r < ANEL3 - 6;
	}

	public static Edificio edificio(String id) {
		for (Edificio e : edificios())
			if (e.id().equals(id))
				return e;
		return null;
	}

	/** Distância (Chebyshev, saturada em 15) de cada coluna ao edifício mais próximo — cache para consultas rápidas. */
	private static byte[] DISTANCIA;
	private static final int LADO = 2 * R_MARGEM + 1;

	private static synchronized byte[] distancias() {
		if (DISTANCIA != null)
			return DISTANCIA;
		byte[] d = new byte[LADO * LADO];
		java.util.Arrays.fill(d, (byte) 15);
		for (Edificio e : edificios())
			for (int x = e.x0() - 14; x <= e.x1() + 14; x++)
				for (int z = e.z0() - 14; z <= e.z1() + 14; z++) {
					int ix = x + R_MARGEM, iz = z + R_MARGEM;
					if (ix < 0 || iz < 0 || ix >= LADO || iz >= LADO)
						continue;
					int m = Math.max(Math.max(e.x0() - x, x - e.x1()), Math.max(e.z0() - z, z - e.z1()));
					m = Math.max(0, m);
					int k = ix * LADO + iz;
					if (d[k] > m)
						d[k] = (byte) m;
				}
		DISTANCIA = d;
		return d;
	}

	/** Distância (em blocos, máx. 15) ao edifício mais próximo; 0 = dentro de um edifício. */
	public static int distanciaEdificio(int dx, int dz) {
		int ix = dx + R_MARGEM, iz = dz + R_MARGEM;
		if (ix < 0 || iz < 0 || ix >= LADO || iz >= LADO)
			return 15;
		return distancias()[ix * LADO + iz];
	}

	/** Dentro de algum edifício (com margem) ou do lago do Bosque? */
	public static boolean reservado(int dx, int dz, int margem) {
		return distanciaEdificio(dx, dz) <= margem || lagoBosque(dx, dz) <= 1.6;
	}

	public static boolean dentroDeEdificio(int dx, int dz, int margem) {
		return distanciaEdificio(dx, dz) <= margem;
	}

	/** Lago do Bosque das Águas (elipse); valor <= 1 = água. */
	private static final int[] LAGO = polar(120, 124);

	public static double lagoBosque(int dx, int dz) {
		int[] c = LAGO;
		double ex = (dx - c[0]) / 17.0, ez = (dz - c[1]) / 11.0;
		return ex * ex + ez * ez;
	}

	/** Retângulos (com a margem) já construídos de uma vez: a geração incremental não mexe neles. */
	public static List<Retangulo> zonasEssenciais() {
		List<Retangulo> l = new ArrayList<>();
		for (Edificio e : edificios())
			if (e.essencial())
				l.add(apron(e));
		return l;
	}

	public static final int MARGEM_ESSENCIAL = 6;

	public static Retangulo apron(Edificio e) {
		return new Retangulo(e.x0() - MARGEM_ESSENCIAL, e.z0() - MARGEM_ESSENCIAL, e.x1() + MARGEM_ESSENCIAL, e.z1() + MARGEM_ESSENCIAL);
	}

	public static boolean emZonaEssencial(int dx, int dz) {
		for (Retangulo r : zonasEssenciais())
			if (r.contem(dx, dz))
				return true;
		return false;
	}

	// ================================================================ Chão da cidade

	public enum Chao {
		GRAMA, PRACA, AVENIDA, BORDA, ALAMEDA, MURO, AGUA
	}

	/** O que vai no chão (y = 0) em (dx, dz). Edifícios põem o próprio piso depois. */
	public static Chao chao(int dx, int dz) {
		double r = raio(dx, dz);
		if (r > R_MURO + 1.5)
			return Chao.GRAMA;
		if (r >= R_MURO - 1.5)
			return Chao.MURO;
		if (r <= R_PRACA)
			return Chao.PRACA;
		if (lagoBosque(dx, dz) <= 1.0)
			return Chao.AGUA;
		double d = Double.MAX_VALUE;
		for (int anel : new int[]{ANEL1, ANEL2, ANEL3})
			d = Math.min(d, Math.abs(r - anel));
		for (int a : AVENIDAS)
			d = Math.min(d, distRaio(dx, dz, a));
		// Eixos leste/oeste (até os portões, a partir do anel 2).
		if (r >= ANEL2)
			for (int a : new int[]{0, 180})
				d = Math.min(d, distRaio(dx, dz, a));
		if (d <= MEIA_AVENIDA - 1)
			return Chao.AVENIDA;
		if (d <= MEIA_AVENIDA)
			return Chao.BORDA;
		return Chao.GRAMA;
	}

	// ================================================================ Pontos nomeados

	/**
	 * Todos os pontos nomeados, relativos ao centro (x, y, z), y relativo ao chão (1 = em pé sobre o chão).
	 * Inclui os nomes do contrato {@link ColoniaApi#local} e os que Planos/EventosDimensao usam ("chegada",
	 * "auxilio" = posição do atril, "mirante" = atril do mirante, "mirante_npc", "praca").
	 */
	public static Map<String, BlockPos> pontosRelativos() {
		Map<String, BlockPos> p = new LinkedHashMap<>();
		p.put("centro", new BlockPos(0, 0, 0));
		p.put("governadoria", new BlockPos(0, 1, 9));
		p.put("praca", new BlockPos(0, 1, 18));
		for (Edificio e : edificios()) {
			if (e.tipo() == Tipo.CASA)
				continue;
			int[] f = e.frente(2);
			switch (e.tipo()) {
				case CAMARAS -> {
					p.put("regeneracao", new BlockPos(f[0], 1, f[1]));
					BlockPos centro = new BlockPos(e.cx(), 1, e.cz());
					BlockPos chegada = centro.relative(e.porta(), 3);
					p.put("chegada", chegada);
					p.put("camaras_retificacao", chegada);
				}
				case MINISTERIO -> {
					if (e.setor() == Setor.AUXILIO) {
						// O atril da reencarnação, no fundo do salão, virado para a porta.
						p.put("auxilio", new BlockPos(e.cx(), 1, e.cz()).relative(e.porta().getOpposite(), 4));
						p.put("auxilio_entrada", new BlockPos(f[0], 1, f[1]));
					} else
						p.put(e.id(), new BlockPos(f[0], 1, f[1]));
				}
				case MIRANTE -> {
					int cx = e.cx() + 2, cz = e.cz(); // centro do terraço
					p.put("mirante", new BlockPos(cx, 6, cz));
					p.put("mirante_npc", new BlockPos(cx - 1, 6, cz + 1));
					p.put("mirante_entrada", new BlockPos(cx - 9, 1, cz));
				}
				case ESTACAO -> p.put(e.id(), new BlockPos(e.cx(), 1, e.cz() + 2));
				default -> p.put(e.id(), new BlockPos(f[0], 1, f[1]));
			}
		}
		int[] bosque = polar(120, 104);
		p.put("bosque", new BlockPos(bosque[0], 1, bosque[1]));
		int[] res = polar(300, 122);
		Edificio casa = casaMaisProxima(res[0], res[1]);
		if (casa != null) {
			int[] f = casa.frente(2);
			p.put("residencias", new BlockPos(f[0], 1, f[1]));
		} else
			p.put("residencias", new BlockPos(res[0], 1, res[1]));
		for (int i = 0; i < PORTOES.length; i++) {
			int[] g = polar(PORTOES[i], R_MURO - 8);
			p.put(NOMES_PORTOES[i], new BlockPos(g[0], 1, g[1]));
		}
		return p;
	}

	private static Edificio casaMaisProxima(int dx, int dz) {
		Edificio melhor = null;
		double d = Double.MAX_VALUE;
		for (Edificio e : edificios())
			if (e.tipo() == Tipo.CASA) {
				double dd = raio(e.cx() - dx, e.cz() - dz);
				if (dd < d) {
					d = dd;
					melhor = e;
				}
			}
		return melhor;
	}

	/** Nomes de destino do aeróbus → texto exibido. */
	public static Map<String, String> destinosAerobus() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("estacao_aerobus", "Governadoria (estação central)");
		for (Setor s : Setor.values())
			m.put("estacao_" + s.id, s.linha1 + " " + s.linha2);
		m.put("estacao_bosque", "Bosque das Águas");
		return m;
	}
}
