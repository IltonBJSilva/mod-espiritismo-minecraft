package net.mcreator.modespiritismominecraft.espiritismo.colonia;

import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.colonia.LayoutColonia.Edificio;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.LayoutColonia.Setor;

/**
 * Construtores das peças de Nosso Lar. Escrevem num {@link Lote} (nada toca o mundo aqui). Ordem dentro de cada
 * peça: estrutura → interior → placas (depois do bloco que as sustenta) → plantas (depois do chão final).
 */
public final class Pecas {
	private Pecas() {
	}

	static final BlockState AR = Blocks.AIR.defaultBlockState();
	static final BlockState GRAMA = Blocks.GRASS_BLOCK.defaultBlockState();
	static final BlockState CALCITA = Blocks.CALCITE.defaultBlockState();
	static final BlockState DIORITO = Blocks.POLISHED_DIORITE.defaultBlockState();
	static final BlockState QUARTZO_LISO = Blocks.SMOOTH_QUARTZ.defaultBlockState();
	static final BlockState TIJOLO_QUARTZO = Blocks.QUARTZ_BRICKS.defaultBlockState();
	static final BlockState PILAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
	static final BlockState LANTERNA_MAR = Blocks.SEA_LANTERN.defaultBlockState();
	static final BlockState AGUA = Blocks.WATER.defaultBlockState();

	static BlockState chaoBloco(LayoutColonia.Chao c, int dx, int dz) {
		return switch (c) {
			case PRACA -> ((dx + dz) & 1) == 0 ? QUARTZO_LISO : CALCITA;
			case AVENIDA -> CALCITA;
			case BORDA -> DIORITO;
			case MURO -> QUARTZO_LISO;
			case AGUA -> Blocks.SAND.defaultBlockState();
			default -> GRAMA;
		};
	}

	static BlockState flor(RandomSource r) {
		Block[] flores = {Blocks.POPPY, Blocks.DANDELION, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.ALLIUM, Blocks.WHITE_TULIP,
				Blocks.PINK_TULIP, Blocks.BLUE_ORCHID};
		return flores[r.nextInt(flores.length)].defaultBlockState();
	}

	static BlockState escada(Block b, Direction f) {
		return b.defaultBlockState().setValue(StairBlock.FACING, f);
	}

	static BlockState laje(Block b, SlabType t) {
		return b.defaultBlockState().setValue(SlabBlock.TYPE, t);
	}

	// ================================================================ Utilidades

	/** Poste de luz: coluna de pilar e lanterna em cima (a lanterna entra como planta: só com o suporte pronto). */
	static void poste(Lote l, BlockPos base, int altura) {
		for (int i = 0; i < altura; i++)
			l.bloco(base.above(i), PILAR);
		l.planta(base.above(altura), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
	}

	static void arvore(Lote l, BlockPos base, RandomSource r, int tipo) {
		BlockState tronco, folha;
		switch (tipo) {
			case 1 -> {
				tronco = Blocks.BIRCH_LOG.defaultBlockState();
				folha = Blocks.BIRCH_LEAVES.defaultBlockState();
			}
			case 2 -> {
				tronco = Blocks.CHERRY_LOG.defaultBlockState();
				folha = Blocks.CHERRY_LEAVES.defaultBlockState();
			}
			default -> {
				tronco = Blocks.OAK_LOG.defaultBlockState();
				folha = Blocks.OAK_LEAVES.defaultBlockState();
			}
		}
		folha = folha.setValue(LeavesBlock.PERSISTENT, true);
		int h = 4 + r.nextInt(3);
		for (int y = 0; y < h; y++)
			l.bloco(base.above(y), tronco);
		int raioMax = tipo == 2 ? 3 : 2;
		for (int dy = h - 2; dy <= h + 1; dy++) {
			int raio = dy >= h ? raioMax - 1 : raioMax;
			for (int dx = -raio; dx <= raio; dx++)
				for (int dz = -raio; dz <= raio; dz++) {
					if (Math.abs(dx) == raio && Math.abs(dz) == raio && (dy == h + 1 || r.nextBoolean()))
						continue;
					if (dx == 0 && dz == 0 && dy < h)
						continue;
					l.seLivre(base.offset(dx, dy, dz), folha);
				}
		}
	}

	/**
	 * Prédio retangular (piso em y = 0 da cidade) com pilares, faixas de vidro, telhado plano com claraboias e
	 * entrada em arco de 3 blocos no lado da porta, com placa do nome acima. Limpa o interior.
	 */
	static void predio(Lote l, Edificio e, int altura, BlockState parede, BlockState vidro, BlockState teto, ChatFormatting cor, String... nome) {
		BlockPos o = l.origem();
		int x0 = e.x0(), z0 = e.z0(), x1 = e.x1(), z1 = e.z1();
		for (int x = x0; x <= x1; x++)
			for (int z = z0; z <= z1; z++) {
				boolean borda = x == x0 || x == x1 || z == z0 || z == z1;
				boolean canto = (x == x0 || x == x1) && (z == z0 || z == z1);
				boolean pilar = borda && (canto || ((x - x0) % 4 == 0 && (z == z0 || z == z1)) || ((z - z0) % 4 == 0 && (x == x0 || x == x1)));
				l.r(x, 0, z, borda ? TIJOLO_QUARTZO : QUARTZO_LISO);
				for (int h = 1; h <= altura; h++) {
					BlockState s;
					if (!borda)
						s = AR;
					else if (pilar)
						s = PILAR;
					else if (h >= 2 && h <= altura - 1)
						s = vidro;
					else
						s = parede;
					l.r(x, h, z, s);
				}
				l.r(x, altura + 1, z, borda ? TIJOLO_QUARTZO : ((x - x0) % 4 == 2 && (z - z0) % 4 == 2 ? LANTERNA_MAR : teto));
				if (borda)
					l.r(x, altura + 2, z, (x + z) % 2 == 0 ? laje(Blocks.QUARTZ_SLAB, SlabType.BOTTOM) : AR);
			}
		// Entrada (3 de largura, 3 de altura) e placa acima, presa à parede.
		int[] f = e.frente(0);
		Direction lado = e.porta().getClockWise();
		for (int d = -1; d <= 1; d++)
			for (int h = 1; h <= 3; h++)
				l.r(f[0] + lado.getStepX() * d, h, f[1] + lado.getStepZ() * d, AR);
		for (int d = -1; d <= 1; d++)
			l.r(f[0] + lado.getStepX() * d, 4, f[1] + lado.getStepZ() * d, parede);
		BlockPos placa = o.offset(f[0], 4, f[1]).relative(e.porta());
		l.placa(placa, e.porta(), cor, nome);
		// Degrau de calçada diante da porta
		for (int d = -2; d <= 2; d++)
			for (int k = 1; k <= 2; k++)
				l.r(f[0] + lado.getStepX() * d + e.porta().getStepX() * k, 0, f[1] + lado.getStepZ() * d + e.porta().getStepZ() * k, CALCITA);
	}

	/** Banco (escadas de bétula viradas para {@code olhando}) ao longo de {@code ao longo}. */
	static void banco(Lote l, BlockPos inicio, Direction aoLongo, int n, Direction olhando) {
		for (int i = 0; i < n; i++)
			l.bloco(inicio.relative(aoLongo, i), escada(Blocks.BIRCH_STAIRS, olhando.getOpposite()));
	}

	// ================================================================ Terreno de uma zona essencial

	static void terrenoRetangulo(Lote l, LayoutColonia.Retangulo r) {
		BlockPos o = l.origem();
		for (int x = r.x0(); x <= r.x1(); x++)
			for (int z = r.z0(); z <= r.z1(); z++) {
				LayoutColonia.Chao c = LayoutColonia.chao(x, z);
				l.coluna(o.getX() + x, o.getZ() + z, o.getY(), chaoBloco(c == LayoutColonia.Chao.AGUA ? LayoutColonia.Chao.GRAMA : c, x, z), false);
			}
	}

	// ================================================================ Governadoria e praça

	static void governadoria(Lote l, Edificio e) {
		BlockPos o = l.origem();
		predio(l, e, 8, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.WHITE_STAINED_GLASS.defaultBlockState(), QUARTZO_LISO, ChatFormatting.DARK_GREEN, "Governadoria", "de", "Nosso Lar", "");
		// Segundo corpo
		for (int x = -7; x <= 7; x++)
			for (int z = -7; z <= 7; z++) {
				boolean borda = Math.abs(x) == 7 || Math.abs(z) == 7;
				for (int h = 10; h <= 15; h++)
					l.r(x, h, z, !borda ? AR : (Math.abs(x) == 7 && Math.abs(z) == 7) ? PILAR : (h >= 11 && h <= 14 ? Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState() : TIJOLO_QUARTZO));
				l.r(x, 16, z, borda ? TIJOLO_QUARTZO : QUARTZO_LISO);
			}
		// Torre de luz: fuste 5x5 até y=40, com farol (beacon) no topo — o feixe azul-claro é visto de longe.
		for (int h = 17; h <= 40; h++)
			for (int x = -2; x <= 2; x++)
				for (int z = -2; z <= 2; z++) {
					boolean borda = Math.abs(x) == 2 || Math.abs(z) == 2;
					boolean canto = Math.abs(x) == 2 && Math.abs(z) == 2;
					l.r(x, h, z, !borda ? (h == 40 ? Blocks.IRON_BLOCK.defaultBlockState() : AR)
							: canto ? PILAR : (h % 6 == 0 ? LANTERNA_MAR : Blocks.WHITE_STAINED_GLASS.defaultBlockState()));
				}
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				l.r(x, 40, z, Blocks.IRON_BLOCK.defaultBlockState());
		l.r(0, 41, 0, Blocks.BEACON.defaultBlockState());
		l.r(0, 42, 0, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
		for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}})
			l.r(c[0], 41, c[1], Blocks.END_ROD.defaultBlockState());
		// Salão: balcão de recepção, atris de consulta, bancos e vasos.
		for (int x = -4; x <= 4; x++)
			l.r(x, 1, -3, x == 0 ? AR : laje(Blocks.SMOOTH_QUARTZ_SLAB, SlabType.TOP));
		l.r(-6, 1, -6, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH));
		l.r(6, 1, -6, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH));
		banco(l, o.offset(-8, 1, 3), Direction.SOUTH, 5, Direction.EAST);
		banco(l, o.offset(8, 1, 3), Direction.SOUTH, 5, Direction.WEST);
		l.placa(o.offset(0, 3, -10).relative(Direction.SOUTH), Direction.SOUTH, ChatFormatting.DARK_BLUE, "Governadoria", "ponto de", "convergência dos", "seis Ministérios");
		l.r(0, 3, -10, TIJOLO_QUARTZO);
		for (int[] c : new int[][]{{-10, -10}, {10, -10}, {-10, 10}, {10, 10}})
			l.planta(o.offset(c[0], 1, c[1]), Blocks.POTTED_WHITE_TULIP.defaultBlockState());
		l.finalizar();
	}

	/** Chão decorado da praça já vem do terreno; aqui: canteiros, fonte, bancos e postes. */
	static void praca(Lote l, RandomSource r) {
		BlockPos o = l.origem();
		// Oito canteiros em volta do palácio
		for (int i = 0; i < 8; i++) {
			int[] c = LayoutColonia.polar(22.5 + 45 * i, 20);
			for (int dx = -2; dx <= 2; dx++)
				for (int dz = -2; dz <= 2; dz++)
					if (dx * dx + dz * dz <= 5) {
						boolean borda = dx * dx + dz * dz >= 4;
						l.r(c[0] + dx, 0, c[1] + dz, borda ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : GRAMA);
					}
			for (int dx = -1; dx <= 1; dx++)
				for (int dz = -1; dz <= 1; dz++)
					l.planta(o.offset(c[0] + dx, 1, c[1] + dz), flor(r));
		}
		// Espelhos d'água a leste e a oeste da Governadoria
		for (int lado : new int[]{-1, 1})
			for (int dx = -2; dx <= 2; dx++)
				for (int dz = -5; dz <= 5; dz++) {
					int x = lado * 18 + dx;
					boolean borda = Math.abs(dx) == 2 || Math.abs(dz) == 5;
					l.r(x, 0, dz, borda ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : AGUA);
					if (!borda)
						l.r(x, -1, dz, TIJOLO_QUARTZO);
				}
		for (int i = 0; i < 8; i++) {
			int[] c = LayoutColonia.polar(45 * i, 25);
			poste(l, o.offset(c[0], 1, c[1]), 3);
		}
		banco(l, o.offset(-3, 1, 23), Direction.EAST, 2, Direction.NORTH);
		banco(l, o.offset(2, 1, 23), Direction.EAST, 2, Direction.NORTH);
		// (praca.south(5) = (0, 1, 23) é onde o sonho de desdobramento chega: deixar livre)
		l.r(0, 1, 26, TIJOLO_QUARTZO);
		l.r(0, 2, 26, TIJOLO_QUARTZO);
		l.placa(o.offset(0, 2, 27), Direction.SOUTH, ChatFormatting.DARK_GREEN, "Nosso Lar", "colônia", "espiritual", "(André Luiz)");
		l.finalizar();
	}

	// ================================================================ Ministérios

	static void ministerio(Lote l, Edificio e) {
		BlockPos o = l.origem();
		Setor s = e.setor();
		BlockState parede = Blocks.WHITE_CONCRETE.defaultBlockState();
		BlockState vidro = switch (s) {
			case UNIAO_DIVINA, ELEVACAO -> Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
			case COMUNICACAO -> Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
			case ESCLARECIMENTO -> Blocks.LIME_STAINED_GLASS.defaultBlockState();
			default -> Blocks.WHITE_STAINED_GLASS.defaultBlockState();
		};
		int altura = s == Setor.UNIAO_DIVINA ? 9 : 7;
		predio(l, e, altura, parede, vidro, QUARTZO_LISO, ChatFormatting.DARK_BLUE, s.linha1, s.linha2, "", "");
		BlockPos c = o.offset(e.cx(), 1, e.cz());
		Direction frente = e.porta(), fundo = frente.getOpposite(), lado = frente.getClockWise();
		int meioLargura = (frente.getAxis() == Direction.Axis.Z ? e.largX() : e.profZ()) / 2 - 2;
		switch (s) {
			case AUXILIO -> {
				// O atril do planejamento reencarnatório (EventosDimensao usa o ponto "auxilio").
				BlockPos atril = c.relative(fundo, 4);
				l.bloco(atril, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, frente));
				l.bloco(atril.relative(fundo, 2).above(), TIJOLO_QUARTZO);
				l.bloco(atril.relative(fundo, 2).above(2), TIJOLO_QUARTZO);
				l.placa(atril.relative(fundo, 1).above(2), frente, ChatFormatting.DARK_BLUE, "Planejamento", "reencarnatório", "Clique no", "atril (púlpito)");
				for (int fila = 0; fila < 3; fila++) {
					banco(l, c.relative(frente, 1 + fila * 2).relative(lado, -4), lado, 3, fundo);
					banco(l, c.relative(frente, 1 + fila * 2).relative(lado, 2), lado, 3, fundo);
				}
				for (int sinal : new int[]{-1, 1})
					l.bloco(c.relative(lado, sinal * meioLargura).relative(fundo, 3), Blocks.WATER_CAULDRON.defaultBlockState()
							.setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
			}
			case COMUNICACAO -> {
				for (int i = -1; i <= 1; i++) {
					BlockPos m = c.relative(lado, i * 4);
					l.bloco(m, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, frente));
				}
				l.bloco(c.relative(fundo, 5), Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR).setValue(BellBlock.FACING, lado));
				l.placa(c.relative(fundo, 6).above(2), frente, ChatFormatting.DARK_BLUE, "Intercâmbio", "entre os", "setores e os", "planos");
				l.bloco(c.relative(fundo, 7).above(2), TIJOLO_QUARTZO);
			}
			case ESCLARECIMENTO -> {
				for (int i = -meioLargura; i <= meioLargura; i++)
					for (int h = 0; h <= 2; h++)
						l.bloco(c.relative(fundo, 6).relative(lado, i).above(h), Blocks.BOOKSHELF.defaultBlockState());
				for (int sinal : new int[]{-1, 1})
					l.bloco(c.relative(lado, sinal * 3), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, frente));
				for (int fila = 0; fila < 2; fila++)
					banco(l, c.relative(frente, 2 + fila * 2).relative(lado, -3), lado, 7, fundo);
			}
			case ELEVACAO -> {
				for (int i = -4; i <= 4; i += 2)
					l.bloco(c.relative(fundo, 5).relative(lado, i), Blocks.NOTE_BLOCK.defaultBlockState());
				for (int fila = 0; fila < 3; fila++)
					banco(l, c.relative(frente, fila * 2 - 1).relative(lado, -4), lado, 9, fundo);
				l.placa(c.relative(fundo, 6).above(2), frente, ChatFormatting.DARK_BLUE, "Arte, música", "e estudo", "elevam o", "pensamento");
				l.bloco(c.relative(fundo, 7).above(2), TIJOLO_QUARTZO);
			}
			case UNIAO_DIVINA -> {
				// Sala de recolhimento e prece: espelho d'água e flores, sem altar nem ritual.
				for (int a = -2; a <= 2; a++)
					for (int b = -1; b <= 1; b++) {
						BlockPos p = c.relative(fundo, 3 + b).relative(lado, a).below();
						boolean borda = Math.abs(a) == 2 || Math.abs(b) == 1;
						l.bloco(p, borda ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : AGUA);
						if (!borda)
							l.bloco(p.below(), TIJOLO_QUARTZO);
					}
				for (int fila = 0; fila < 2; fila++)
					banco(l, c.relative(frente, 2 + fila * 2).relative(lado, -3), lado, 7, fundo);
				for (int sinal : new int[]{-1, 1})
					l.planta(c.relative(lado, sinal * meioLargura).relative(fundo, 5), Blocks.POTTED_LILY_OF_THE_VALLEY.defaultBlockState());
				l.placa(c.relative(fundo, 6).above(2), frente, ChatFormatting.DARK_BLUE, "Recolhimento", "e prece", "A prece não", "é ritual");
				l.bloco(c.relative(fundo, 7).above(2), TIJOLO_QUARTZO);
			}
			default -> {
			}
		}
		l.finalizar();
	}

	/** Câmaras de Retificação (Ministério da Regeneração): onde o recém-chegado desperta. */
	static void camaras(Lote l, Edificio e) {
		BlockPos o = l.origem();
		predio(l, e, 6, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.WHITE_STAINED_GLASS.defaultBlockState(), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
				ChatFormatting.DARK_BLUE, "Ministério da", "Regeneração", "Câmaras de", "Retificação");
		BlockPos c = o.offset(e.cx(), 1, e.cz());
		Direction frente = e.porta(), fundo = frente.getOpposite(), lado = frente.getClockWise();
		int meio = (frente.getAxis() == Direction.Axis.Z ? e.largX() : e.profZ()) / 2 - 2;
		// Leitos em duas fileiras, cabeceira para o fundo, com vasos de lírio entre eles.
		for (int i = -meio + 1; i <= meio - 1; i += 3) {
			if (Math.abs(i) <= 1)
				continue;
			l.cama(c.relative(lado, i).relative(fundo, 4), fundo, Blocks.WHITE_BED);
			l.cama(c.relative(lado, i).relative(frente, 0).relative(fundo, 0), fundo, Blocks.LIGHT_BLUE_BED);
		}
		for (int sinal : new int[]{-1, 1}) {
			l.planta(c.relative(lado, sinal * meio).relative(fundo, 6), Blocks.POTTED_LILY_OF_THE_VALLEY.defaultBlockState());
			l.planta(c.relative(lado, sinal * meio).relative(frente, 3), Blocks.POTTED_AZURE_BLUET.defaultBlockState());
		}
		BlockPos placa = c.relative(fundo, (frente.getAxis() == Direction.Axis.Z ? e.profZ() : e.largX()) / 2 - 1).above(2);
		l.placa(placa, frente, ChatFormatting.DARK_BLUE, "Câmaras de", "Retificação", "Aqui se refaz", "o recém-chegado");
		l.finalizar();
	}

	static void hospital(Lote l, Edificio e) {
		BlockPos o = l.origem();
		predio(l, e, 6, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), QUARTZO_LISO, ChatFormatting.DARK_BLUE,
				"Hospital", "do Auxílio", "tratamento e", "recuperação");
		BlockPos c = o.offset(e.cx(), 1, e.cz());
		// Porta a leste: alas de leitos ao longo do eixo Z.
		for (int z = -11; z <= 11; z += 3) {
			if (Math.abs(z) <= 1)
				continue;
			l.cama(c.offset(-7, 0, z), Direction.WEST, Blocks.WHITE_BED);
			l.cama(c.offset(-2, 0, z), Direction.EAST, Blocks.WHITE_BED);
			l.planta(c.offset(-4, 0, z), Blocks.POTTED_OXEYE_DAISY.defaultBlockState());
		}
		l.bloco(c.offset(5, 0, -3), laje(Blocks.SMOOTH_QUARTZ_SLAB, SlabType.TOP));
		l.bloco(c.offset(5, 0, 3), laje(Blocks.SMOOTH_QUARTZ_SLAB, SlabType.TOP));
		l.finalizar();
	}

	static void biblioteca(Lote l, Edificio e) {
		BlockPos o = l.origem();
		predio(l, e, 8, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.LIME_STAINED_GLASS.defaultBlockState(), QUARTZO_LISO, ChatFormatting.DARK_GREEN,
				"Biblioteca", "Central", "de Nosso Lar", "");
		BlockPos c = o.offset(e.cx(), 1, e.cz());
		for (int x = -6; x <= 6; x += 3)
			for (int z = -10; z <= 10; z++) {
				if (Math.abs(z) <= 1)
					continue;
				for (int h = 0; h <= 2; h++) {
					// Estantes espíritas (catálogo da biblioteca) à beira do corredor central, ao alcance de quem entra.
					boolean espirita = h == 0 && z == 2 && (x == -6 || x == 0 || x == 6);
					l.bloco(c.offset(x, h, z), espirita ? net.mcreator.modespiritismominecraft.espiritismo.biblioteca.RegistroBiblioteca.ESTANTE.get().defaultBlockState()
							: Blocks.BOOKSHELF.defaultBlockState());
				}
			}
		for (int z : new int[]{-1, 1})
			l.bloco(c.offset(-7, 0, z * 3), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST));
		l.finalizar();
	}

	static void escola(Lote l, Edificio e) {
		BlockPos o = l.origem();
		predio(l, e, 6, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.YELLOW_STAINED_GLASS.defaultBlockState(), QUARTZO_LISO, ChatFormatting.DARK_GREEN,
				"Escola", "Centro de", "formação", "");
		BlockPos c = o.offset(e.cx(), 1, e.cz());
		// Quadro na parede leste interna, atril do instrutor e bancos voltados para ele.
		for (int z = -4; z <= 4; z++)
			for (int h = 1; h <= 2; h++)
				l.bloco(c.offset(e.largX() / 2 - 1, h, z), Blocks.BLACK_CONCRETE.defaultBlockState());
		l.bloco(c.offset(e.largX() / 2 - 3, 0, 0), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.WEST));
		for (int x = -5; x <= 3; x += 2)
			for (int sinal : new int[]{-1, 1})
				banco(l, c.offset(x, 0, sinal * 2), sinal > 0 ? Direction.SOUTH : Direction.NORTH, 5, Direction.EAST);
		l.finalizar();
	}

	static void reservatorio(Lote l, Edificio e) {
		BlockPos o = l.origem();
		predio(l, e, 5, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
				ChatFormatting.DARK_BLUE, "Reservatório", "Rio Azul", "(União Divina)", "");
		BlockPos c = o.offset(e.cx(), 0, e.cz());
		for (int x = -4; x <= 4; x++)
			for (int z = -2; z <= 2; z++) {
				boolean borda = Math.abs(x) == 4 || Math.abs(z) == 2;
				l.bloco(c.offset(x, 0, z), borda ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : AGUA);
				if (!borda)
					l.bloco(c.offset(x, -1, z), TIJOLO_QUARTZO);
			}
		l.finalizar();
	}

	/** Estação do aeróbus: plataforma, atril de embarque, placa e o carro aéreo suspenso a ~5 blocos do solo. */
	static void estacao(Lote l, Edificio e, String titulo) {
		BlockPos o = l.origem();
		BlockPos c = o.offset(e.cx(), 0, e.cz());
		for (int x = -3; x <= 3; x++)
			for (int z = -3; z <= 3; z++) {
				boolean borda = Math.abs(x) == 3 || Math.abs(z) == 3;
				l.bloco(c.offset(x, 0, z), borda ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : QUARTZO_LISO);
				for (int h = 1; h <= 10; h++)
					l.bloco(c.offset(x, h, z), AR);
			}
		l.bloco(c.offset(0, 1, 0), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH));
		for (int h = 1; h <= 3; h++)
			l.bloco(c.offset(0, h, -3), PILAR);
		l.placa(c.offset(0, 3, -2), Direction.SOUTH, ChatFormatting.DARK_AQUA, "Estação do", "Aeróbus", titulo, "clique no atril");
		// O carro: casco de quartzo com janelas de vidro, suspenso (é só cenário).
		for (int x = -3; x <= 3; x++)
			for (int y = 6; y <= 8; y++)
				for (int z = -1; z <= 1; z++) {
					boolean ponta = Math.abs(x) == 3;
					boolean meio = y == 7 && z != 0 && !ponta;
					l.bloco(c.offset(x, y, z), meio ? Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState() : (z == 0 && y == 7 && !ponta) ? AR : QUARTZO_LISO);
				}
		for (int x : new int[]{-4, 4})
			l.bloco(c.offset(x, 7, 0), Blocks.SEA_LANTERN.defaultBlockState());
		l.finalizar();
	}

	static void casa(Lote l, Edificio e, RandomSource r) {
		BlockPos o = l.origem();
		BlockState parede = switch (e.variante()) {
			case 0 -> Blocks.WHITE_TERRACOTTA.defaultBlockState();
			case 1 -> Blocks.CALCITE.defaultBlockState();
			case 2 -> Blocks.BIRCH_PLANKS.defaultBlockState();
			default -> Blocks.SMOOTH_SANDSTONE.defaultBlockState();
		};
		Block telhado = switch (e.variante()) {
			case 0 -> Blocks.PRISMARINE_BRICK_SLAB;
			case 1 -> Blocks.BIRCH_SLAB;
			case 2 -> Blocks.QUARTZ_SLAB;
			default -> Blocks.SMOOTH_SANDSTONE_SLAB;
		};
		int x0 = e.x0(), z0 = e.z0(), x1 = e.x1(), z1 = e.z1();
		for (int x = x0; x <= x1; x++)
			for (int z = z0; z <= z1; z++) {
				boolean borda = x == x0 || x == x1 || z == z0 || z == z1;
				boolean canto = (x == x0 || x == x1) && (z == z0 || z == z1);
				l.r(x, 0, z, borda ? TIJOLO_QUARTZO : Blocks.BIRCH_PLANKS.defaultBlockState());
				for (int h = 1; h <= 3; h++) {
					BlockState s = !borda ? AR : canto ? Blocks.STRIPPED_BIRCH_LOG.defaultBlockState()
							: (h == 2 && ((x - x0) % 2 == 0 || (z - z0) % 2 == 0)) ? Blocks.GLASS.defaultBlockState() : parede;
					l.r(x, h, z, s);
				}
				l.r(x, 4, z, borda ? laje(telhado, SlabType.BOTTOM) : laje(telhado, SlabType.TOP));
			}
		l.r(e.cx(), 5, e.cz(), laje(telhado, SlabType.BOTTOM));
		int[] f = e.frente(0);
		l.porta(o.offset(f[0], 1, f[1]), e.porta(), Blocks.BIRCH_DOOR);
		BlockPos c = o.offset(e.cx(), 1, e.cz());
		Direction fundo = e.porta().getOpposite(), lado = e.porta().getClockWise();
		l.bloco(c.relative(fundo, 2).relative(lado, -2), Blocks.BOOKSHELF.defaultBlockState());
		l.bloco(c.relative(fundo, 2).relative(lado, 2), laje(Blocks.BIRCH_SLAB, SlabType.TOP));
		l.planta(c.relative(fundo, 2).relative(lado, 2).above(), Blocks.POTTED_RED_TULIP.defaultBlockState());
		l.bloco(c.relative(lado, 2), escada(Blocks.BIRCH_STAIRS, lado));
		// Caminho até a porta e jardim
		for (int k = 1; k <= 3; k++)
			l.r(f[0] + e.porta().getStepX() * k, 0, f[1] + e.porta().getStepZ() * k, Blocks.DIRT_PATH.defaultBlockState());
		for (int x = x0 - 2; x <= x1 + 2; x++)
			for (int z = z0 - 2; z <= z1 + 2; z++) {
				if (x >= x0 && x <= x1 && z >= z0 && z <= z1)
					continue;
				if (LayoutColonia.chao(x, z) != LayoutColonia.Chao.GRAMA)
					continue;
				if (r.nextInt(3) == 0)
					l.planta(o.offset(x, 1, z), flor(r));
			}
		l.finalizar();
	}

	// ================================================================ Mirante (mesma geometria da versão anterior)

	static void mirante(Lote l, BlockPos c) {
		int topo = 5;
		BlockState pedra = TIJOLO_QUARTZO;
		for (int x = -3; x <= 3; x++)
			for (int z = -3; z <= 3; z++)
				for (int y = 1; y <= topo; y++)
					l.bloco(c.offset(x, y, z), (y == topo) ? QUARTZO_LISO : pedra);
		for (int i = 0; i < topo; i++) {
			BlockPos p = c.offset(-8 + i, 1 + i, 0);
			for (int dz = -1; dz <= 1; dz++) {
				l.bloco(p.offset(0, 0, dz), escada(Blocks.QUARTZ_STAIRS, Direction.EAST));
				for (int y = 1; y <= i; y++)
					l.bloco(p.offset(0, -y, dz), pedra);
			}
		}
		for (int x = -3; x <= 3; x++) {
			l.bloco(c.offset(x, topo + 1, -3), pedra);
			l.bloco(c.offset(x, topo + 2, -3), pedra);
			l.bloco(c.offset(x, topo + 1, 3), Blocks.BIRCH_FENCE.defaultBlockState());
		}
		for (int z = -3; z <= 3; z++) {
			l.bloco(c.offset(3, topo + 1, z), pedra);
			l.bloco(c.offset(3, topo + 2, z), pedra);
			if (z != 0)
				l.bloco(c.offset(-3, topo + 1, z), Blocks.BIRCH_FENCE.defaultBlockState());
		}
		// Escala dos mundos (O Evangelho segundo o Espiritismo, cap. III, item 4)
		String[][] mundos = {{"Mundos", "primitivos", "primeiras", "encarnações"}, {"Mundos de", "expiação", "e provas", "(a Terra)"},
				{"Mundos de", "regeneração", "as almas se", "renovam"}, {"Mundos ditosos", "ou felizes", "o bem supera", "o mal"},
				{"Mundos celestes", "ou divinos", "morada dos", "Espíritos puros"}};
		for (int i = 0; i < 5; i++)
			l.placa(c.offset(-2 + i, topo + 2, -2), Direction.SOUTH, ChatFormatting.DARK_BLUE, mundos[i]);
		l.placa(c.offset(0, topo + 1, -2), Direction.SOUTH, ChatFormatting.DARK_GREEN, "Escala dos", "mundos", "ESE, cap. III", "");
		// Escala espírita (O Livro dos Espíritos, q. 100–113)
		String[][] ordens = {{"3ª ordem", "Espíritos", "imperfeitos", "LE q. 101-106"}, {"2ª ordem", "Bons", "Espíritos", "LE q. 107-111"},
				{"1ª ordem", "Espíritos", "puros", "LE q. 112-113"}};
		for (int i = 0; i < 3; i++)
			l.placa(c.offset(2, topo + 2, 1 - i), Direction.WEST, ChatFormatting.DARK_PURPLE, ordens[i]);
		l.placa(c.offset(2, topo + 1, 0), Direction.WEST, ChatFormatting.DARK_GREEN, "Escala", "espírita", "LE q. 100", "");
		l.bloco(c.offset(0, topo + 1, 0), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.WEST));
		for (int[] p : new int[][]{{-3, -3}, {3, 3}, {3, -3}})
			l.planta(c.offset(p[0], topo + 3, p[1]), Blocks.LANTERN.defaultBlockState());
		l.finalizar();
	}

	// ================================================================ Muralha, torres e portões

	/** Um arco da muralha entre os ângulos a0 e a1 (graus). */
	static void muralha(Lote l, double a0, double a1) {
		BlockPos o = l.origem();
		int R = LayoutColonia.R_MURO;
		int[] cx = PlanoColonia.caixaArco(a0, a1, R - 2, R + 2);
		for (int x = cx[0]; x <= cx[2]; x++)
			for (int z = cx[1]; z <= cx[3]; z++) {
				double r = LayoutColonia.raio(x, z);
				if (r < R - 1.5 || r > R + 1.5)
					continue;
				double ang = LayoutColonia.angulo(x, z);
				if (!entre(ang, a0, a1))
					continue;
				boolean portao = false;
				for (int p : LayoutColonia.PORTOES)
					if (Math.abs(difAng(ang, p)) * Math.PI / 180 * R <= 3.5)
						portao = true;
				boolean externo = r > R + 0.5;
				for (int h = 1; h <= 9; h++) {
					BlockState s;
					if (portao && h <= 7)
						s = AR;
					else if (h == 1)
						s = QUARTZO_LISO;
					else if (h == 6)
						s = externo ? Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState() : TIJOLO_QUARTZO;
					else
						s = TIJOLO_QUARTZO;
					l.r(x, h, z, s);
				}
				if (externo && ((int) Math.round(ang * R * Math.PI / 180)) % 2 == 0)
					l.r(x, 10, z, TIJOLO_QUARTZO);
				if (portao)
					l.r(x, 0, z, CALCITA);
			}
		// Torres: duas em cada portão e uma a cada 45° entre eles
		for (int t = 0; t < 360; t += 45) {
			if (t % 90 == 0) {
				double d = Math.toDegrees(7.0 / R);
				for (double off : new double[]{-d, d})
					if (entre(t + off, a0, a1))
						torre(l, LayoutColonia.polar(t + off, R));
			} else if (entre(t, a0, a1))
				torre(l, LayoutColonia.polar(t, R));
		}
		l.finalizar();
	}

	private static void torre(Lote l, int[] c) {
		BlockPos o = l.origem();
		for (int x = -3; x <= 3; x++)
			for (int z = -3; z <= 3; z++) {
				double d = Math.sqrt(x * x + z * z);
				if (d > 3.3)
					continue;
				boolean borda = d > 2.3;
				for (int h = 1; h <= 15; h++) {
					BlockState s = !borda ? (h == 15 ? QUARTZO_LISO : TIJOLO_QUARTZO)
							: (h >= 11 && h <= 13 && (x == 0 || z == 0)) ? Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState() : (h == 1 ? QUARTZO_LISO : TIJOLO_QUARTZO);
					l.r(c[0] + x, h, c[1] + z, s);
				}
				if (borda && (x + z) % 2 == 0)
					l.r(c[0] + x, 16, c[1] + z, TIJOLO_QUARTZO);
			}
		l.r(c[0], 16, c[1], LANTERNA_MAR);
	}

	static boolean entre(double ang, double a0, double a1) {
		ang = ((ang % 360) + 360) % 360;
		return ang >= a0 && ang < a1;
	}

	static double difAng(double a, double b) {
		double d = ((a - b) % 360 + 540) % 360 - 180;
		return d;
	}
}
