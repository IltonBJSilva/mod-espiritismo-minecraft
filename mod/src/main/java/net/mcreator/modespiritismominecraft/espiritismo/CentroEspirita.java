package net.mcreator.modespiritismominecraft.espiritismo;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.npc.NpcApi;
import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModBlocks;

/**
 * Centro Espírita Allan Kardec (doc: "Centro Espírita"). Construído uma vez por mundo, perto do
 * ponto de spawn, quando o primeiro jogador entra. Dentro ficam o Dirigente, o Passista e a Evangelizadora (tutorial).
 * <p>
 * Planta (13 x 11), porta ao norte (z = 0):
 * <pre>
 *  z=10  parede dos fundos (vitral azul)
 *  z=9   estantes | Kardec | estantes
 *  z=8   mesa com O Livro dos Espíritos, velas e lírios
 *  z=7   Haroldo           Chico  (moringa d'água)
 *  z=5,3 bancos  | corredor (tapete) |  bancos
 *  z=0   porta + placa
 * </pre>
 */
public final class CentroEspirita {
	public static final int LARGURA = 13; // x
	public static final int PROFUNDIDADE = 11; // z
	private static final int FLAGS = Block.UPDATE_CLIENTS;

	private CentroEspirita() {
	}

	// ---------------------------------------------------------------- Construção automática

	/** Constrói o Centro no Overworld se ainda não existir. Retorna a posição da porta. */
	public static BlockPos garantir(ServerLevel overworld) {
		BlockPos existente = Jornada.centro(overworld);
		if (existente != null)
			return existente;
		BlockPos origem = escolherLocal(overworld, overworld.getRespawnData().globalPos().pos());
		// Os benfeitores entram depois, por verificarBenfeitores, quando o chunk estiver ativo.
		BlockPos porta = construir(overworld, origem, false);
		overworld.setData(Moralidade.CENTRO, porta.asLong());
		// Segurança: quem estiver dentro da área da obra vai para o caminho em frente à porta.
		AABB obra = new AABB(origem.getX() - 2, origem.getY() - 10, origem.getZ() - 7, origem.getX() + LARGURA + 2, origem.getY() + 13, origem.getZ() + PROFUNDIDADE + 2);
		BlockPos fora = porta.north(5);
		for (var p : overworld.players())
			if (obra.contains(p.position()))
				p.teleportTo(fora.getX() + 0.5, fora.getY(), fora.getZ() + 0.5);
		return porta;
	}

	/** Procura, em volta do spawn, um terreno seco e razoavelmente plano. */
	static BlockPos escolherLocal(ServerLevel level, BlockPos spawn) {
		// Todos os candidatos deixam a área da obra (com margem) a mais de 12 blocos do spawn, para não soterrar ninguém.
		int[][] candidatos = {{16, -5}, {-30, -5}, {-6, 22}, {-6, -34}, {28, 16}, {-40, 16}, {16, -36}, {-34, -34}, {32, 32}, {-6, 36}};
		BlockPos melhor = null;
		int melhorVariacao = Integer.MAX_VALUE;
		for (int[] c : candidatos) {
			int x0 = spawn.getX() + c[0], z0 = spawn.getZ() + c[1];
			int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
			boolean molhado = false;
			int[] alturas = new int[9];
			int i = 0;
			for (int dx = 0; dx <= LARGURA - 1; dx += (LARGURA - 1) / 2)
				for (int dz = 0; dz <= PROFUNDIDADE - 1; dz += (PROFUNDIDADE - 1) / 2) {
					int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x0 + dx, z0 + dz) - 1;
					alturas[i++] = h;
					min = Math.min(min, h);
					max = Math.max(max, h);
					if (!level.getFluidState(new BlockPos(x0 + dx, h, z0 + dz)).isEmpty())
						molhado = true;
				}
			java.util.Arrays.sort(alturas);
			BlockPos pos = new BlockPos(x0, alturas[4], z0);
			int variacao = (max - min) + (molhado ? 100 : 0);
			if (variacao <= 3)
				return pos;
			if (variacao < melhorVariacao) {
				melhorVariacao = variacao;
				melhor = pos;
			}
		}
		return melhor;
	}

	// ---------------------------------------------------------------- Construção

	/**
	 * Constrói o Centro com o canto noroeste do piso em {@code o} e coloca os benfeitores dentro.
	 *
	 * @return a posição (bloco de baixo) da porta
	 */
	public static BlockPos construir(ServerLevel level, BlockPos o, boolean comBenfeitores) {
		// 1. Limpa o volume e acerta o terreno em volta
		for (int x = -2; x <= LARGURA + 1; x++)
			for (int z = -7; z <= PROFUNDIDADE + 1; z++) {
				for (int y = 1; y <= 12; y++)
					set(level, o.offset(x, y, z), Blocks.AIR.defaultBlockState());
				boolean dentro = x >= 0 && x < LARGURA && z >= 0 && z < PROFUNDIDADE;
				if (!dentro) {
					boolean caminho = z < 0 && x >= 5 && x <= 7;
					set(level, o.offset(x, 0, z), caminho ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
					preencherAbaixo(level, o.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 6);
				}
			}

		// 2. Fundação e piso
		for (int x = 0; x < LARGURA; x++)
			for (int z = 0; z < PROFUNDIDADE; z++) {
				boolean borda = x == 0 || z == 0 || x == LARGURA - 1 || z == PROFUNDIDADE - 1;
				set(level, o.offset(x, 0, z), borda ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState());
				preencherAbaixo(level, o.offset(x, -1, z), Blocks.STONE_BRICKS.defaultBlockState(), 10);
			}

		// 3. Paredes, pilares e janelas
		for (int y = 1; y <= 4; y++)
			for (int x = 0; x < LARGURA; x++)
				for (int z = 0; z < PROFUNDIDADE; z++) {
					boolean ladoX = x == 0 || x == LARGURA - 1;
					boolean ladoZ = z == 0 || z == PROFUNDIDADE - 1;
					if (!ladoX && !ladoZ)
						continue;
					boolean pilar = (ladoX && (z == 0 || z == 5 || z == PROFUNDIDADE - 1)) || (ladoZ && x % 4 == 0);
					BlockState s;
					if (pilar)
						s = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
					else if (y == 1)
						s = Blocks.STONE_BRICKS.defaultBlockState();
					else if ((y == 2 || y == 3) && ladoX && (z == 2 || z == 3 || z == 7 || z == 8))
						s = vidraca(Blocks.GLASS_PANE, true);
					else if ((y == 2 || y == 3) && z == 0 && (x == 2 || x == 10))
						s = vidraca(Blocks.GLASS_PANE, false);
					else if ((y == 2 || y == 3) && z == PROFUNDIDADE - 1 && x >= 5 && x <= 7)
						s = vidraca(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, false);
					else
						s = Blocks.SMOOTH_QUARTZ.defaultBlockState();
					set(level, o.offset(x, y, z), s);
				}

		// 4. Porta e placa
		BlockPos porta = o.offset(6, 1, 0);
		BlockState portaBase = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
		set(level, porta, portaBase.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		set(level, porta.above(), portaBase.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		BlockPos placa = o.offset(6, 3, -1);
		set(level, placa, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH));
		if (level.getBlockEntity(placa) instanceof SignBlockEntity sign) {
			SignText texto = new SignText().setMessage(0, Component.literal("Centro Espírita").withStyle(ChatFormatting.DARK_BLUE))
					.setMessage(1, Component.literal("Allan Kardec").withStyle(ChatFormatting.DARK_BLUE)).setMessage(2, Component.literal("~"))
					.setMessage(3, Component.literal("Entre, irmão"));
			sign.setText(texto, true);
			sign.setChanged();
			level.sendBlockUpdated(placa, sign.getBlockState(), sign.getBlockState(), FLAGS);
		}
		// Postes com lanterna no caminho
		for (int x : new int[]{3, 9}) {
			set(level, o.offset(x, 1, -3), Blocks.SPRUCE_FENCE.defaultBlockState());
			set(level, o.offset(x, 2, -3), Blocks.LANTERN.defaultBlockState());
		}

		// 5. Forro e telhado em degraus
		for (int x = -1; x <= LARGURA; x++)
			for (int z = -1; z <= PROFUNDIDADE; z++) {
				boolean beiral = x == -1 || z == -1 || x == LARGURA || z == PROFUNDIDADE;
				boolean borda = x == 0 || z == 0 || x == LARGURA - 1 || z == PROFUNDIDADE - 1;
				set(level, o.offset(x, 5, z), beiral ? laje() : borda ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		for (int n = 0; n <= 2; n++)
			for (int x = n; x < LARGURA - n; x++)
				for (int z = n; z < PROFUNDIDADE - n; z++) {
					boolean anel = x == n || z == n || x == LARGURA - 1 - n || z == PROFUNDIDADE - 1 - n;
					set(level, o.offset(x, 6 + n, z), anel || n == 2 ? laje() : Blocks.DEEPSLATE_TILES.defaultBlockState());
				}
		set(level, o.offset(6, 9, 5), Blocks.END_ROD.defaultBlockState()); // a luz no alto do Centro

		// 6. Interior
		for (int z = 1; z <= 6; z++)
			set(level, o.offset(6, 1, z), Blocks.LIGHT_BLUE_CARPET.defaultBlockState());
		BlockState banco = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
		for (int z : new int[]{3, 5})
			for (int x : new int[]{2, 3, 4, 8, 9, 10})
				set(level, o.offset(x, 1, z), banco);
		for (int x = 4; x <= 8; x++)
			set(level, o.offset(x, 1, 8), Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
		set(level, o.offset(6, 2, 8), net.mcreator.modespiritismominecraft.espiritismo.itens.RegistroItens.LIVRO_1857.get().defaultBlockState());
		for (int x : new int[]{4, 8})
			set(level, o.offset(x, 2, 8), Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
		for (int x : new int[]{5, 7})
			set(level, o.offset(x, 2, 8), Blocks.POTTED_LILY_OF_THE_VALLEY.defaultBlockState());
		for (int y = 1; y <= 2; y++)
			for (int x : new int[]{1, 2, 3, 9, 10, 11})
				set(level, o.offset(x, y, 9), Blocks.BOOKSHELF.defaultBlockState());
		set(level, o.offset(11, 1, 7), net.mcreator.modespiritismominecraft.espiritismo.itens.RegistroItens.JARRA_AGUA.get().defaultBlockState());
		set(level, o.offset(1, 1, 1), Blocks.POTTED_FERN.defaultBlockState());
		set(level, o.offset(11, 1, 1), Blocks.POTTED_FERN.defaultBlockState());
		for (int[] l : new int[][]{{3, 3}, {9, 3}, {3, 7}, {9, 7}, {6, 5}})
			set(level, o.offset(l[0], 4, l[1]), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));

		// 7. Os benfeitores
		if (comBenfeitores)
			verificarBenfeitores(level, porta);
		return porta;
	}

	/**
	 * Coloca no Centro os benfeitores que estiverem faltando (primeira visita, ou se alguém usou /kill).
	 * Só age com o chunk ativo; senão a busca não enxergaria os que já estão lá e os duplicaria.
	 */
	public static void verificarBenfeitores(ServerLevel level, BlockPos porta) {
		BlockPos o = porta.offset(-6, -1, 0);
		if (!level.isPositionEntityTicking(o) || !level.isPositionEntityTicking(o.offset(LARGURA - 1, 0, PROFUNDIDADE - 1)))
			return;
		AABB area = new AABB(o.getX(), o.getY(), o.getZ(), o.getX() + LARGURA, o.getY() + 6, o.getZ() + PROFUNDIDADE).inflate(8);
		// Kardec, Chico e Haroldo ficam em locais especiais pelo mundo; aqui atendem os trabalhadores do Centro.
		colocarSeFaltar(level, area, NpcApi.DIRIGENTE.get(), o.offset(6, 1, 9));
		colocarSeFaltar(level, area, NpcApi.PASSISTA.get(), o.offset(9, 1, 7));
		colocarSeFaltar(level, area, NpcApi.EVANGELIZADORA.get(), o.offset(3, 1, 7));
	}

	private static void colocarSeFaltar(ServerLevel level, AABB area, EntityType<? extends PathfinderMob> tipo, BlockPos pos) {
		if (level.getEntities(tipo, area, e -> true).isEmpty())
			colocar(level, tipo, pos);
	}

	private static void colocar(ServerLevel level, EntityType<? extends PathfinderMob> tipo, BlockPos pos) {
		PathfinderMob npc = tipo.create(level, EntitySpawnReason.STRUCTURE);
		if (npc == null)
			return;
		npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		npc.setYRot(180F); // olhando para a porta (norte)
		npc.setYHeadRot(180F);
		npc.setYBodyRot(180F);
		npc.setPersistenceRequired();
		npc.setHomeTo(pos, 2); // passeia só perto do seu lugar
		level.addFreshEntity(npc);
	}

	private static BlockState laje() {
		return Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState();
	}

	/** Vidraça já conectada ao longo da parede (ao longo de z se {@code norteSul}). */
	private static BlockState vidraca(Block bloco, boolean norteSul) {
		BlockState s = bloco.defaultBlockState();
		return norteSul ? s.setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true) : s.setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
	}

	private static void set(Level level, BlockPos pos, BlockState state) {
		if (!level.getBlockState(pos).is(Blocks.BEDROCK))
			level.setBlock(pos, state, FLAGS);
	}

	/** Preenche para baixo enquanto houver ar, água ou plantas (até {@code max} blocos). */
	private static void preencherAbaixo(Level level, BlockPos inicio, BlockState state, int max) {
		BlockPos.MutableBlockPos p = inicio.mutable();
		for (int i = 0; i < max && p.getY() > level.getMinY(); i++, p.move(Direction.DOWN)) {
			BlockState atual = level.getBlockState(p);
			if (!atual.canBeReplaced() && atual.getFluidState().isEmpty())
				break;
			level.setBlock(p, state, FLAGS);
		}
	}
}
