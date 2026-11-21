package net.mcreator.modespiritismominecraft.espiritismo.mundo;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Direction;

import net.mcreator.modespiritismominecraft.espiritismo.mundo.PecaEspirita.Telhado;

/**
 * As plantas (desenhos) de cada modelo, em coordenadas locais da peça: x = largura, z = profundidade, a frente
 * (rua/entrada) fica em z = 0 e o piso em y = 0. Nos blocos com direção, "SOUTH" aponta para a frente (z menor) e
 * "NORTH" para o fundo; a peça gira/espelha tudo conforme a orientação sorteada.
 * <p>
 * Sem velas, imagens ou altares: o Espiritismo não tem rituais. Decoração com estantes, plantas, tapetes, água.
 */
public final class Plantas {
	/** Porta da frente do centro pequeno (coordenadas locais). */
	public static final int[] PORTA_CENTRO_PEQUENO = {7, 1, 2};

	private static final Direction S = Direction.SOUTH, N = Direction.NORTH, E = Direction.EAST, W = Direction.WEST;

	private static final Block[] TAPETES_CORES = {Blocks.RED_CARPET, Blocks.YELLOW_CARPET, Blocks.LIME_CARPET, Blocks.LIGHT_BLUE_CARPET, Blocks.ORANGE_CARPET, Blocks.PINK_CARPET};

	private Plantas() {
	}

	public static void construir(PecaEspirita p) {
		Modelo m = p.modelo();
		int w = m.largura - 1, d = m.profundidade - 1;
		Estilo e = p.estilo();
		// Fundação até o chão, terreno do lote nivelado e volume interno limpo.
		p.fundacao(0, 0, w, d, e.fundacao.defaultBlockState());
		p.caixa(0, 0, 0, w, 0, d, chaoExterno(e));
		p.ar(0, 1, 0, w, m.altura - 1, d);
		switch (m) {
			case CENTRO_PEQUENO -> centroPequeno(p);
			case CENTRO_MEDIO -> centroMedio(p);
			case CENTRO_GRANDE -> centroGrande(p);
			case CASA_KARDEC -> casaKardec(p);
			case CASA_CHICO -> casaChico(p);
			case MANSAO_DO_CAMINHO -> mansaoDoCaminho(p);
			case AUDITORIO_HAROLDO -> auditorioHaroldo(p);
		}
		p.deslocar(0, 0);
	}

	private static BlockState chaoExterno(Estilo e) {
		return switch (e) {
			case ARENITO -> Blocks.SAND.defaultBlockState();
			case TERRACOTA -> Blocks.RED_SAND.defaultBlockState();
			case NEVE -> Blocks.SNOW_BLOCK.defaultBlockState();
			case MANGUE -> Blocks.MUD.defaultBlockState();
			default -> Blocks.GRASS_BLOCK.defaultBlockState();
		};
	}

	private static boolean temGrama(Estilo e) {
		return chaoExterno(e).is(Blocks.GRASS_BLOCK);
	}

	// ================================================================== utilidades de decoração

	/** Poste de luz: cerca de 2 blocos + lanterna. */
	private static void poste(PecaEspirita p, int x, int z) {
		p.b(x, 1, z, p.estilo().cerca);
		p.b(x, 2, z, p.estilo().cerca);
		p.lanterna(x, 3, z, false);
	}

	/** Canteiro de flores (só onde há grama). */
	private static void canteiro(PecaEspirita p, int x0, int z0, int x1, int z1) {
		if (!temGrama(p.estilo()))
			return;
		for (int x = x0; x <= x1; x++)
			for (int z = z0; z <= z1; z++)
				if (p.aleatorio().nextInt(3) > 0)
					p.flor(x, 1, z);
	}

	private static void caminho(PecaEspirita p, int x0, int z0, int x1, int z1) {
		p.caixa(x0, 0, z0, x1, 0, z1, p.estilo().caminho);
	}

	/** Árvore pequena (folhas persistentes para não sumirem). */
	private static void arvore(PecaEspirita p, int x, int z, Block tronco, Block folha) {
		BlockState f = folha.defaultBlockState().trySetValue(LeavesBlock.PERSISTENT, true);
		for (int y = 3; y <= 5; y++) {
			int r = y == 5 ? 1 : 2;
			for (int dx = -r; dx <= r; dx++)
				for (int dz = -r; dz <= r; dz++)
					if (Math.abs(dx) + Math.abs(dz) <= r + 1)
						p.b(x + dx, y, z + dz, f);
		}
		p.b(x, 6, z, f);
		for (int y = 1; y <= 4; y++)
			p.b(x, y, z, tronco);
	}

	/** Mesa comprida (lajes em cima) de x0 a x1 em z. */
	private static void mesa(PecaEspirita p, int x0, int x1, int z) {
		for (int x = x0; x <= x1; x++)
			p.b(x, 1, z, PecaEspirita.lajeCima(p.estilo().laje));
	}

	private static void cadeiras(PecaEspirita p, int x0, int x1, int y, int z, Direction encosto) {
		for (int x = x0; x <= x1; x++)
			p.b(x, y, z, PecaEspirita.escada(p.estilo().banco, encosto));
	}

	private static void estante(PecaEspirita p, int x0, int y0, int z0, int x1, int y1, int z1) {
		p.caixa(x0, y0, z0, x1, y1, z1, Blocks.BOOKSHELF);
	}

	private static BlockState caldeiraoCheio() {
		return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
	}

	/** Fonte rasa: água em y = 0 cercada por pedra, borda de laje em y = 1. */
	private static void fonte(PecaEspirita p, int x0, int z0, int x1, int z1) {
		p.caixa(x0, -1, z0, x1, -1, z1, Blocks.STONE_BRICKS);
		p.caixa(x0, 0, z0, x1, 0, z1, Blocks.STONE_BRICKS);
		p.contorno(x0, 1, z0, x1, z1, Blocks.STONE_BRICK_SLAB.defaultBlockState());
		p.caixa(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, Blocks.WATER);
	}

	// ================================================================== Centro pequeno (casinha de bairro)

	private static void centroPequeno(PecaEspirita p) {
		Estilo e = p.estilo();
		p.edificio(1, 2, 13, 15, 4, Telhado.DUAS_AGUAS_Z);
		caminho(p, 6, 0, 8, 1);
		canteiro(p, 1, 0, 4, 1);
		canteiro(p, 10, 0, 13, 1);
		poste(p, 5, 1);
		poste(p, 9, 1);
		p.porta(7, 2, S);

		// Salão (z 3..10): bancos, corredor com tapete, mesa simples com o livro.
		p.caixa(7, 1, 3, 7, 1, 7, e.tapete);
		for (int z : new int[]{4, 6}) {
			p.fileiraBancos(2, 5, 1, z);
			p.fileiraBancos(9, 12, 1, z);
		}
		p.b(6, 1, 8, PecaEspirita.lajeCima(e.laje));
		p.b(8, 1, 8, PecaEspirita.lajeCima(e.laje));
		p.livroNaMesa(7, 1, 8, S);
		p.b(7, 1, 9, PecaEspirita.escada(e.banco, N)); // cadeira do dirigente, de frente para o público
		estante(p, 2, 1, 10, 3, 2, 10);
		estante(p, 11, 1, 10, 12, 2, 10);
		p.vaso(2, 1, 3);
		p.vaso(12, 1, 3);
		p.jarraAgua(12, 1, 8);
		p.lanterna(4, 4, 5, true);
		p.lanterna(10, 4, 5, true);
		p.lanterna(7, 4, 9, true);

		// Sala de passe (z 12..14), separada por parede em z = 11.
		p.paredeInterna(2, 11, 12, 11, 4);
		p.abrirPorta(10, 11, S);
		p.caixa(2, 1, 12, 12, 1, 14, e.tapete);
		p.b(4, 1, 13, PecaEspirita.escada(e.banco, S));
		p.b(6, 1, 13, PecaEspirita.escada(e.banco, S));
		p.jarraAgua(11, 1, 14);
		estante(p, 2, 1, 14, 2, 2, 14);
		p.lanterna(6, 4, 13, true);

		p.npc("dirigente", 5, 1, 9, 4);
		p.npc("passista", 8, 1, 13, 2);
	}

	// ================================================================== Centro médio

	private static void centroMedio(PecaEspirita p) {
		Estilo e = p.estilo();
		p.edificio(1, 2, 25, 23, 5, Telhado.PLANO);
		caminho(p, 7, 0, 9, 1);
		canteiro(p, 1, 0, 5, 1);
		canteiro(p, 11, 0, 25, 1);
		poste(p, 6, 1);
		poste(p, 10, 1);
		p.porta(8, 2, S);

		// Paredes internas.
		p.paredeInterna(15, 3, 15, 22, 5);
		p.paredeInterna(2, 15, 14, 15, 5);
		p.paredeInterna(16, 8, 24, 8, 5);
		p.paredeInterna(16, 14, 24, 14, 5);
		p.abrirPorta(15, 5, E);
		p.abrirPorta(15, 11, E);
		p.abrirPorta(15, 19, E);
		p.abrirPorta(8, 15, S);
		p.abrirPorta(4, 23, N);

		// Salão de palestras (x 2..14, z 3..14) com tablado e tribuna.
		p.caixa(5, 1, 12, 11, 1, 14, e.piso);
		for (int x = 5; x <= 11; x++)
			p.b(x, 1, 11, PecaEspirita.escada(e.banco, N));
		p.tribuna(8, 2, 13, S);
		p.vaso(5, 2, 14);
		p.vaso(11, 2, 14);
		p.caixa(8, 1, 3, 8, 1, 10, e.tapete);
		for (int z : new int[]{4, 6, 8}) {
			p.fileiraBancos(2, 6, 1, z);
			p.fileiraBancos(10, 14, 1, z);
		}
		estante(p, 2, 1, 13, 3, 3, 14);
		estante(p, 13, 1, 13, 14, 3, 14);
		p.jarraAgua(14, 1, 3);
		p.lanterna(5, 5, 6, true);
		p.lanterna(11, 5, 6, true);
		p.lanterna(8, 5, 12, true);

		// Biblioteca (x 16..24, z 3..7).
		estante(p, 16, 1, 7, 24, 3, 7);
		estante(p, 24, 1, 3, 24, 3, 6);
		p.b(19, 1, 5, PecaEspirita.lajeCima(e.laje));
		p.b(21, 1, 5, PecaEspirita.lajeCima(e.laje));
		p.livroNaMesa(20, 1, 5, S);
		p.b(19, 1, 4, PecaEspirita.escada(e.banco, S));
		p.b(21, 1, 4, PecaEspirita.escada(e.banco, S));
		p.vaso(16, 1, 3);
		p.lanterna(20, 5, 5, true);

		// Sala de passes (x 16..24, z 9..13).
		p.caixa(17, 1, 9, 24, 1, 13, Blocks.LIGHT_BLUE_CARPET);
		for (int x : new int[]{18, 20, 22})
			p.b(x, 1, 11, PecaEspirita.escada(e.banco, S));
		p.jarraAgua(23, 1, 13);
		p.vaso(24, 1, 9);
		p.lanterna(20, 5, 11, true);

		// Sala da mesa mediúnica (x 16..24, z 15..22).
		p.mesaMediunica(18, 1, 18, 22, 19);
		cadeiras(p, 18, 22, 1, 17, S);
		cadeiras(p, 18, 22, 1, 20, N);
		estante(p, 24, 1, 15, 24, 2, 16);
		p.jarraAgua(24, 1, 22);
		p.lanterna(20, 5, 18, true);
		p.lanterna(20, 5, 20, true);

		// Cozinha e refeitório da sopa fraterna (x 2..14, z 16..22).
		p.b(2, 1, 17, caldeiraoCheio());
		p.b(2, 1, 18, caldeiraoCheio());
		p.b(2, 1, 20, PecaEspirita.virado(Blocks.SMOKER, E));
		p.b(2, 1, 21, PecaEspirita.virado(Blocks.SMOKER, E));
		p.b(2, 1, 22, Blocks.BARREL);
		p.b(3, 1, 22, Blocks.BARREL);
		mesa(p, 7, 12, 18);
		cadeiras(p, 7, 12, 1, 17, S);
		cadeiras(p, 7, 12, 1, 19, N);
		mesa(p, 7, 12, 21);
		cadeiras(p, 7, 12, 1, 20, S);
		cadeiras(p, 7, 12, 1, 22, N);
		p.lanterna(9, 5, 19, true);
		p.lanterna(4, 5, 19, true);

		p.npc("orador", 8, 2, 14, 3);
		p.npc("passista", 20, 1, 12, 3);
		p.npc("dirigente", 17, 1, 18, 3);
		p.npc("medium_trabalhador", 23, 1, 19, 3);
	}

	// ================================================================== Centro grande

	private static void centroGrande(PecaEspirita p) {
		Estilo e = p.estilo();
		centroMedio(p);

		// Escola de evangelização infantil: três salinhas (x 29..39, z 2..23).
		p.edificio(29, 2, 39, 23, 4, Telhado.PLANO);
		p.paredeInterna(30, 9, 38, 9, 4);
		p.paredeInterna(30, 16, 38, 16, 4);
		caminho(p, 26, 0, 28, 24);
		int[][] salas = {{2, 9}, {9, 16}, {16, 23}};
		for (int[] s : salas) {
			int za = s[0], zb = s[1], meio = (za + zb) / 2;
			p.abrirPorta(29, meio, W);
			Block tapete = TAPETES_CORES[p.aleatorio().nextInt(TAPETES_CORES.length)];
			p.caixa(30, 1, za + 1, 37, 1, zb - 1, tapete);
			p.caixa(38, 2, za + 2, 38, 3, zb - 2, Blocks.GREEN_TERRACOTTA); // lousa
			for (int x : new int[]{32, 34}) // bancos pequenos
				for (int z = za + 2; z <= zb - 2; z++)
					p.b(x, 1, z, e.laje);
			p.livroNaMesa(37, 1, za + 1, W);
			p.vaso(30, 1, zb - 1);
			p.lanterna(34, 4, meio, true);
		}

		// Pátio (z 26..38) com fonte, árvores, bancos e canteiros.
		caminho(p, 1, 25, 39, 25);
		caminho(p, 19, 26, 21, 38);
		caminho(p, 2, 31, 38, 33);
		fonte(p, 17, 29, 23, 35);
		boolean grama = temGrama(e);
		if (grama) {
			arvore(p, 5, 28, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
			arvore(p, 35, 28, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
			arvore(p, 5, 36, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
			arvore(p, 35, 36, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
			canteiro(p, 9, 27, 15, 29);
			canteiro(p, 25, 35, 31, 37);
		}
		cadeiras(p, 10, 14, 1, 30, S);
		cadeiras(p, 26, 30, 1, 34, N);
		poste(p, 16, 26);
		poste(p, 24, 26);
		poste(p, 16, 38);
		poste(p, 24, 38);

		p.npc("evangelizadora", 33, 1, 13, 4);
		p.npc("evangelizadora", 33, 1, 20, 4);
	}

	// ================================================================== Casa de Allan Kardec (Paris, séc. XIX)

	private static void casaKardec(PecaEspirita p) {
		Estilo e = p.estilo();
		// Rua calçada com lampiões.
		caminho(p, 0, 0, 20, 2);
		poste(p, 3, 1);
		poste(p, 17, 1);

		// Sobrado: térreo y 1..4, sobrado y 6..9, telhado de ardósia.
		p.edificio(2, 4, 18, 16, 9, Telhado.DUAS_AGUAS_X);
		p.caixa(3, 5, 5, 17, 5, 15, e.piso); // piso do andar de cima
		p.contorno(2, 5, 4, 18, 16, e.estrutura.defaultBlockState()); // cornija entre os andares
		for (int y = 7; y <= 8; y++) { // janelas altas do sobrado
			for (int x = 4; x <= 16; x += 3) {
				p.b(x, y, 4, PecaEspirita.conectado(e.vidro, true));
				p.b(x, y, 16, PecaEspirita.conectado(e.vidro, true));
			}
			for (int z = 7; z <= 13; z += 3)
				p.b(18, y, z, PecaEspirita.conectado(e.vidro, false));
		}
		p.porta(10, 4, S);
		p.vaso(9, 1, 3);
		p.vaso(11, 1, 3);

		// Térreo: sala das reuniões de estudo (x 3..12) e escada/cozinha (x 14..17).
		p.paredeInterna(13, 5, 13, 15, 4);
		p.abrirPorta(13, 10, E);
		p.caixa(4, 1, 6, 11, 1, 14, Blocks.RED_CARPET);
		mesa(p, 5, 10, 9);
		mesa(p, 5, 10, 10);
		mesa(p, 5, 10, 11);
		cadeiras(p, 5, 10, 1, 8, S);
		cadeiras(p, 5, 10, 1, 12, N);
		p.livroNaMesa(4, 1, 10, E);
		estante(p, 3, 1, 5, 3, 3, 6);
		estante(p, 3, 1, 14, 3, 3, 15);
		p.lanterna(7, 4, 10, true);
		// Escada para o sobrado (x 16..17, sobe para o fundo).
		for (int k = 0; k <= 4; k++) {
			int z = 6 + k, y = 1 + k;
			if (y > 1)
				p.caixa(16, 1, z, 17, y - 1, z, e.piso);
			p.b(16, y, z, PecaEspirita.escada(e.banco, N));
			p.b(17, y, z, PecaEspirita.escada(e.banco, N));
		}
		p.ar(16, 5, 5, 17, 5, 9);
		p.cercaZ(15, 6, 5, 9);
		p.b(14, 1, 15, PecaEspirita.virado(Blocks.SMOKER, N));
		p.b(15, 1, 15, Blocks.BARREL);
		p.b(17, 1, 15, PecaEspirita.lajeCima(e.laje));
		p.jarraAgua(17, 1, 14);
		p.lanterna(15, 4, 12, true);

		// Sobrado: gabinete de estudos cheio de livros, com escrivaninha.
		p.caixa(4, 6, 6, 11, 6, 14, Blocks.RED_CARPET);
		estante(p, 3, 6, 5, 3, 8, 15);
		estante(p, 4, 6, 15, 12, 8, 15);
		estante(p, 4, 6, 5, 12, 8, 5);
		p.caixa(13, 6, 5, 13, 9, 15, e.parede);
		p.porta(13, 5, 12, E);
		p.b(13, 8, 12, e.parede);
		p.b(6, 6, 10, PecaEspirita.lajeCima(e.laje));
		p.b(8, 6, 10, PecaEspirita.lajeCima(e.laje));
		p.livroNaMesa(7, 6, 10, S);
		p.b(7, 6, 9, PecaEspirita.escada(e.banco, S));
		p.b(10, 6, 13, PecaEspirita.virado(Blocks.LECTERN, W));
		p.b(4, 6, 13, Blocks.CHISELED_BOOKSHELF);
		p.vaso(11, 6, 6);
		p.lanterna(7, 9, 9, true);
		// Quarto simples (x 14..17, z 10..15).
		p.cama(16, 6, 13, Blocks.WHITE_BED, N);
		p.b(14, 6, 15, Blocks.BARREL);
		p.lanterna(15, 9, 12, true);

		// Pequeno pátio nos fundos.
		canteiro(p, 2, 18, 18, 19);
		cadeiras(p, 8, 12, 1, 20, N);

		p.npc("allan_kardec", 5, 6, 12, 4);
	}

	// ================================================================== Casa de Chico Xavier (interior de Minas)

	private static void casaChico(PecaEspirita p) {
		Estilo e = p.estilo();
		// Casa simples (x 2..10, z 3..11).
		p.edificio(2, 3, 10, 11, 4, Telhado.DUAS_AGUAS_X);
		caminho(p, 6, 0, 6, 2);
		p.porta(6, 3, S);
		p.b(4, 1, 2, Blocks.LIGHT_BLUE_TERRACOTTA); // floreiras azuis sob as janelas
		p.b(7, 1, 2, Blocks.LIGHT_BLUE_TERRACOTTA);
		p.vaso(4, 2, 2);
		p.vaso(7, 2, 2);
		p.cama(3, 1, 9, Blocks.WHITE_BED, N);
		p.livroNaMesa(8, 1, 9, S); // escrivaninha simples
		p.b(8, 1, 8, PecaEspirita.escada(e.banco, S));
		mesa(p, 4, 5, 5);
		p.b(4, 1, 4, PecaEspirita.escada(e.banco, S));
		p.b(5, 1, 6, PecaEspirita.escada(e.banco, N));
		p.jarraAgua(9, 1, 4); // filtro de água
		estante(p, 9, 1, 10, 9, 2, 10);
		p.lanterna(6, 4, 7, true);

		// Salão do Grupo Espírita da Prece (x 13..24, z 3..13).
		p.edificio(13, 3, 24, 13, 4, Telhado.DUAS_AGUAS_X);
		caminho(p, 18, 0, 18, 2);
		p.porta(18, 3, S);
		p.caixa(18, 1, 4, 18, 1, 9, e.tapete);
		for (int z : new int[]{5, 7, 9}) {
			p.fileiraBancos(14, 17, 1, z);
			p.fileiraBancos(19, 23, 1, z);
		}
		p.b(17, 1, 11, PecaEspirita.lajeCima(e.laje));
		p.b(19, 1, 11, PecaEspirita.lajeCima(e.laje));
		p.livroNaMesa(18, 1, 11, S);
		p.b(18, 1, 12, PecaEspirita.escada(e.banco, N));
		p.jarraAgua(23, 1, 12);
		estante(p, 14, 1, 12, 14, 2, 12);
		p.vaso(23, 1, 4);
		p.lanterna(16, 4, 8, true);
		p.lanterna(21, 4, 8, true);

		// Horta (x 2..10, z 13..20), cercada, com canal de água.
		p.cercaX(2, 10, 1, 13);
		p.cercaX(2, 10, 1, 20);
		p.cercaZ(2, 1, 14, 19);
		p.cercaZ(10, 1, 14, 19);
		p.b(6, 1, 13, PecaEspirita.virado(Blocks.OAK_FENCE_GATE, S));
		BlockState terra = Blocks.FARMLAND.defaultBlockState().trySetValue(BlockStateProperties.MOISTURE, 7);
		Block[] culturas = {Blocks.CARROTS, Blocks.POTATOES, Blocks.WHEAT, Blocks.BEETROOTS};
		for (int z = 14; z <= 19; z++) {
			if (z == 17) {
				p.caixa(3, -1, z, 9, -1, z, Blocks.DIRT);
				p.caixa(3, 0, z, 9, 0, z, Blocks.WATER);
				continue;
			}
			Block c = culturas[(z - 14) % culturas.length];
			for (int x = 3; x <= 9; x++) {
				p.b(x, 0, z, terra);
				BlockState planta = c.defaultBlockState();
				planta = planta.hasProperty(BlockStateProperties.AGE_7) ? planta.setValue(BlockStateProperties.AGE_7, 4 + p.aleatorio().nextInt(4))
						: planta.trySetValue(BlockStateProperties.AGE_3, 2 + p.aleatorio().nextInt(2));
				p.b(x, 1, z, planta);
			}
		}
		p.b(11, 1, 14, Blocks.COMPOSTER);

		// Distribuição de alimentos (x 13..24, z 15..20): toldo, mesas, barris e fardos.
		for (int[] c : new int[][]{{13, 15}, {24, 15}, {13, 20}, {24, 20}})
			p.caixa(c[0], 1, c[1], c[0], 3, c[1], e.cerca);
		p.caixa(13, 4, 15, 24, 4, 20, e.laje);
		mesa(p, 15, 22, 17);
		for (int x = 16; x <= 21; x++)
			p.b(x, 1, 19, x % 2 == 0 ? Blocks.BARREL.defaultBlockState() : Blocks.HAY_BLOCK.defaultBlockState());
		p.lanterna(18, 3, 17, true);
		caminho(p, 13, 14, 24, 14);

		p.npc("chico_xavier", 16, 1, 11, 6);
	}

	// ================================================================== Mansão do Caminho (obra social)

	private static void mansaoDoCaminho(PecaEspirita p) {
		Estilo e = p.estilo();
		// Avenida central, portão, praça com fonte e rua das casas-lar.
		caminho(p, 27, 0, 29, 50);
		caminho(p, 23, 11, 26, 11);
		caminho(p, 23, 33, 26, 33);
		caminho(p, 30, 24, 33, 26);
		caminho(p, 34, 16, 54, 17);
		caminho(p, 2, 41, 54, 42);
		for (int y = 1; y <= 3; y++) {
			p.b(25, y, 0, e.estrutura);
			p.b(31, y, 0, e.estrutura);
		}
		p.lanterna(25, 4, 0, false);
		p.lanterna(31, 4, 0, false);
		fonte(p, 25, 21, 31, 27);
		p.caixa(27, 0, 20, 29, 0, 20, e.caminho);
		p.caixa(27, 0, 28, 29, 0, 28, e.caminho);
		arvore(p, 24, 6, Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES);
		arvore(p, 32, 6, Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES);
		arvore(p, 24, 36, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
		arvore(p, 32, 36, Blocks.OAK_LOG, Blocks.OAK_LEAVES);
		canteiro(p, 23, 14, 26, 19);
		canteiro(p, 30, 28, 33, 32);
		canteiro(p, 23, 28, 26, 31);
		cadeiras(p, 23, 25, 1, 29, S);
		cadeiras(p, 31, 33, 1, 19, N);
		poste(p, 26, 12);
		poste(p, 30, 12);
		poste(p, 26, 38);
		poste(p, 30, 38);

		// Salão de palestras (x 2..22, z 3..20): tribuna a oeste, plateia olhando para o oeste.
		p.edificio(2, 3, 22, 20, 5, Telhado.PLANO);
		p.porta(22, 11, E);
		p.caixa(3, 1, 6, 5, 1, 16, e.piso);
		for (int z = 6; z <= 16; z++)
			p.b(6, 1, z, PecaEspirita.escada(e.banco, W));
		p.tribuna(5, 2, 11, E);
		for (int x = 9; x <= 19; x += 2) {
			for (int z = 4; z <= 9; z++)
				p.b(x, 1, z, PecaEspirita.escada(e.banco, E));
			for (int z = 13; z <= 19; z++)
				p.b(x, 1, z, PecaEspirita.escada(e.banco, E));
		}
		p.caixa(8, 1, 10, 21, 1, 12, e.tapete);
		p.vaso(3, 2, 6);
		p.vaso(3, 2, 16);
		p.jarraAgua(21, 1, 4);
		for (int x = 8; x <= 18; x += 5) {
			p.lanterna(x, 5, 7, true);
			p.lanterna(x, 5, 16, true);
		}

		// Escola (x 34..54, z 3..15): três salas de aula com lousa.
		p.edificio(34, 3, 54, 15, 4, Telhado.PLANO);
		p.paredeInterna(41, 4, 41, 14, 4);
		p.paredeInterna(47, 4, 47, 14, 4);
		int[][] salas = {{34, 41}, {41, 47}, {47, 54}};
		for (int[] s : salas) {
			int xa = s[0], xb = s[1], meio = (xa + xb) / 2;
			p.porta(meio, 15, N);
			p.caixa(xa + 2, 2, 4, xb - 2, 3, 4, Blocks.GREEN_TERRACOTTA);
			for (int z : new int[]{8, 10, 12})
				for (int x = xa + 2; x <= xb - 2; x++)
					p.b(x, 1, z, e.laje);
			p.livroNaMesa(meio, 1, 6, N);
			p.lanterna(meio, 4, 9, true);
		}

		// Creche (x 34..54, z 19..31): caminhas e tapetes coloridos.
		p.edificio(34, 19, 54, 31, 4, Telhado.PLANO);
		p.porta(34, 25, W);
		caminho(p, 30, 25, 33, 25);
		for (int x = 36; x <= 52; x += 2)
			p.cama(x, 1, 20, Blocks.LIGHT_BLUE_BED, N);
		for (int x = 36; x <= 52; x++)
			for (int z = 24; z <= 30; z++)
				p.b(x, 1, z, TAPETES_CORES[(x / 2 + z / 2) % TAPETES_CORES.length]);
		estante(p, 53, 1, 28, 53, 2, 30);
		p.vaso(35, 1, 30);
		p.jarraAgua(53, 1, 24);
		p.lanterna(40, 4, 26, true);
		p.lanterna(48, 4, 26, true);

		// Refeitório (x 2..22, z 23..38) com cozinha.
		p.edificio(2, 23, 22, 38, 4, Telhado.PLANO);
		p.porta(22, 33, E);
		for (int z = 25; z <= 26; z++)
			p.b(3, 1, z, caldeiraoCheio());
		for (int z = 27; z <= 29; z++)
			p.b(3, 1, z, PecaEspirita.virado(Blocks.SMOKER, E));
		for (int z = 30; z <= 33; z++)
			p.b(3, 1, z, Blocks.BARREL);
		for (int z : new int[]{27, 33}) {
			mesa(p, 8, 20, z);
			cadeiras(p, 8, 20, 1, z - 1, S);
			cadeiras(p, 8, 20, 1, z + 1, N);
		}
		p.jarraAgua(21, 1, 37);
		p.lanterna(10, 4, 30, true);
		p.lanterna(16, 4, 30, true);
		p.lanterna(13, 4, 36, true);

		// Casas-lar (z 43..49).
		for (int xa : new int[]{2, 13, 34, 45}) {
			p.edificio(xa, 43, xa + 8, 49, 4, Telhado.DUAS_AGUAS_X);
			p.porta(xa + 4, 43, S);
			p.cama(xa + 1, 1, 47, Blocks.RED_BED, N);
			p.cama(xa + 3, 1, 47, Blocks.BLUE_BED, N);
			mesa(p, xa + 5, xa + 6, 46);
			p.b(xa + 5, 1, 45, PecaEspirita.escada(e.banco, S));
			p.b(xa + 6, 1, 47, PecaEspirita.escada(e.banco, N));
			estante(p, xa + 7, 1, 48, xa + 7, 2, 48);
			p.vaso(xa + 1, 1, 44);
			p.lanterna(xa + 4, 4, 46, true);
		}

		p.npc("divaldo_franco", 4, 2, 11, 4);
		p.npc("evangelizadora", 44, 1, 9, 6);
		p.npc("evangelizadora", 44, 1, 27, 6);
		p.npc("dirigente", 6, 1, 36, 5);
	}

	// ================================================================== Auditório (palestras de Haroldo Dutra Dias)

	private static void auditorioHaroldo(PecaEspirita p) {
		Estilo e = p.estilo();
		caminho(p, 0, 0, 32, 2);
		poste(p, 12, 1);
		poste(p, 20, 1);
		canteiro(p, 1, 0, 10, 1);
		canteiro(p, 22, 0, 31, 1);

		p.edificio(1, 3, 31, 33, 8, Telhado.PLANO);
		for (int y = 5; y <= 6; y++) // janelas altas
			for (int z = 5; z <= 31; z += 3) {
				p.b(1, y, z, PecaEspirita.conectado(e.vidro, false));
				p.b(31, y, z, PecaEspirita.conectado(e.vidro, false));
			}
		p.porta(15, 3, S);
		p.porta(17, 3, S);
		p.b(16, 1, 3, e.estrutura);

		// Palco com tribuna.
		p.caixa(4, 1, 27, 28, 1, 32, e.estrutura);
		for (int x = 13; x <= 19; x++)
			p.b(x, 1, 26, PecaEspirita.escada(e.banco, N));
		p.tribuna(16, 2, 29, S);
		estante(p, 4, 2, 32, 7, 4, 32);
		estante(p, 25, 2, 32, 28, 4, 32);
		p.vaso(4, 2, 27);
		p.vaso(28, 2, 27);
		p.jarraAgua(18, 2, 30);

		// Plateia: muitas fileiras de bancos com corredor central.
		p.caixa(15, 1, 4, 17, 1, 25, e.tapete);
		for (int z = 6; z <= 24; z += 2) {
			p.fileiraBancos(3, 14, 1, z);
			p.fileiraBancos(18, 29, 1, z);
		}
		p.jarraAgua(30, 1, 4);
		p.vaso(2, 1, 4);
		for (int x = 6; x <= 26; x += 5)
			for (int z = 8; z <= 26; z += 6)
				p.lanterna(x, 8, z, true);

		p.npc("haroldo_dutra", 16, 2, 30, 3);
	}
}
