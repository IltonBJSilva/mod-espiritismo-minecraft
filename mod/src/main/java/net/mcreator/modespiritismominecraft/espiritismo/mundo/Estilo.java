package net.mcreator.modespiritismominecraft.espiritismo.mundo;

import net.neoforged.neoforge.common.Tags;

import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Holder;

/**
 * Paleta de materiais de uma construção. Os Centros Espíritas comuns escolhem a paleta pelo bioma
 * (e, nos biomas temperados, sorteiam uma variante); os locais especiais têm paleta fixa.
 */
public enum Estilo {
	// piso, parede, estrutura(coluna), telhado(escada), telhado(bloco), laje, porta, cerca, banco, vidro, fundação, caminho, tapete
	CARVALHO(Blocks.OAK_PLANKS, Blocks.QUARTZ_BLOCK, Blocks.STRIPPED_OAK_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, Blocks.OAK_SLAB, Blocks.OAK_DOOR, Blocks.OAK_FENCE,
			Blocks.OAK_STAIRS, Blocks.GLASS_PANE, Blocks.COBBLESTONE, Blocks.DIRT_PATH, Blocks.LIGHT_BLUE_CARPET),
	TIJOLO(Blocks.SPRUCE_PLANKS, Blocks.BRICKS, Blocks.STRIPPED_SPRUCE_LOG, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_DOOR,
			Blocks.SPRUCE_FENCE, Blocks.SPRUCE_STAIRS, Blocks.GLASS_PANE, Blocks.STONE_BRICKS, Blocks.GRAVEL, Blocks.WHITE_CARPET),
	BRANCO(Blocks.BIRCH_PLANKS, Blocks.WHITE_CONCRETE, Blocks.SMOOTH_QUARTZ, Blocks.BRICK_STAIRS, Blocks.BRICKS, Blocks.SMOOTH_QUARTZ_SLAB, Blocks.BIRCH_DOOR, Blocks.BIRCH_FENCE,
			Blocks.BIRCH_STAIRS, Blocks.GLASS_PANE, Blocks.STONE_BRICKS, Blocks.DIRT_PATH, Blocks.LIGHT_BLUE_CARPET),
	BETULA(Blocks.BIRCH_PLANKS, Blocks.CALCITE, Blocks.BIRCH_LOG, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS, Blocks.BIRCH_SLAB, Blocks.BIRCH_DOOR, Blocks.BIRCH_FENCE,
			Blocks.BIRCH_STAIRS, Blocks.GLASS_PANE, Blocks.COBBLESTONE, Blocks.DIRT_PATH, Blocks.GREEN_CARPET),
	ARENITO(Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SMOOTH_SANDSTONE_STAIRS, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_SANDSTONE_SLAB,
			Blocks.JUNGLE_DOOR, Blocks.JUNGLE_FENCE, Blocks.JUNGLE_STAIRS, Blocks.GLASS_PANE, Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.ORANGE_CARPET),
	TERRACOTA(Blocks.SMOOTH_RED_SANDSTONE, Blocks.WHITE_TERRACOTTA, Blocks.CUT_RED_SANDSTONE, Blocks.RED_SANDSTONE_STAIRS, Blocks.RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE_SLAB,
			Blocks.ACACIA_DOOR, Blocks.ACACIA_FENCE, Blocks.ACACIA_STAIRS, Blocks.GLASS_PANE, Blocks.RED_SANDSTONE, Blocks.COARSE_DIRT, Blocks.ORANGE_CARPET),
	ACACIA(Blocks.ACACIA_PLANKS, Blocks.WHITE_TERRACOTTA, Blocks.ACACIA_LOG, Blocks.BRICK_STAIRS, Blocks.BRICKS, Blocks.ACACIA_SLAB, Blocks.ACACIA_DOOR, Blocks.ACACIA_FENCE,
			Blocks.ACACIA_STAIRS, Blocks.GLASS_PANE, Blocks.COBBLESTONE, Blocks.DIRT_PATH, Blocks.YELLOW_CARPET),
	ABETO(Blocks.SPRUCE_PLANKS, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.SPRUCE_LOG, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE,
			Blocks.SPRUCE_STAIRS, Blocks.GLASS_PANE, Blocks.COBBLESTONE, Blocks.PODZOL, Blocks.BROWN_CARPET),
	NEVE(Blocks.SPRUCE_PLANKS, Blocks.STONE_BRICKS, Blocks.SPRUCE_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE,
			Blocks.SPRUCE_STAIRS, Blocks.GLASS_PANE, Blocks.COBBLESTONE, Blocks.GRAVEL, Blocks.RED_CARPET),
	CEREJEIRA(Blocks.CHERRY_PLANKS, Blocks.WHITE_CONCRETE, Blocks.CHERRY_LOG, Blocks.CHERRY_STAIRS, Blocks.CHERRY_PLANKS, Blocks.CHERRY_SLAB, Blocks.CHERRY_DOOR, Blocks.CHERRY_FENCE,
			Blocks.CHERRY_STAIRS, Blocks.GLASS_PANE, Blocks.STONE_BRICKS, Blocks.DIRT_PATH, Blocks.PINK_CARPET),
	SELVA(Blocks.JUNGLE_PLANKS, Blocks.MOSSY_STONE_BRICKS, Blocks.JUNGLE_LOG, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_SLAB, Blocks.JUNGLE_DOOR, Blocks.JUNGLE_FENCE,
			Blocks.JUNGLE_STAIRS, Blocks.GLASS_PANE, Blocks.MOSSY_COBBLESTONE, Blocks.DIRT_PATH, Blocks.LIME_CARPET),
	MANGUE(Blocks.MANGROVE_PLANKS, Blocks.MUD_BRICKS, Blocks.MANGROVE_LOG, Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_SLAB, Blocks.MANGROVE_DOOR,
			Blocks.MANGROVE_FENCE, Blocks.MANGROVE_STAIRS, Blocks.GLASS_PANE, Blocks.MUD_BRICKS, Blocks.PACKED_MUD, Blocks.GREEN_CARPET),
	// ---- locais especiais
	/** Sobrado parisiense do século XIX: pedra clara, telhado de ardósia (deepslate), madeira escura. */
	PARIS(Blocks.DARK_OAK_PLANKS, Blocks.CALCITE, Blocks.POLISHED_DIORITE, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_DOOR,
			Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_STAIRS, Blocks.GLASS_PANE, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.RED_CARPET),
	/** Casa simples do interior de Minas: paredes caiadas, chão de vermelhão, telha de barro. */
	MINEIRO(Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.STRIPPED_OAK_LOG, Blocks.BRICK_STAIRS, Blocks.BRICKS, Blocks.OAK_SLAB, Blocks.OAK_DOOR, Blocks.OAK_FENCE,
			Blocks.OAK_STAIRS, Blocks.GLASS_PANE, Blocks.COBBLESTONE, Blocks.COARSE_DIRT, Blocks.LIGHT_BLUE_CARPET),
	/** Obra social baiana: prédios brancos, laje plana, janelas azuis. */
	BAIANO(Blocks.SMOOTH_STONE, Blocks.WHITE_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BRICK_STAIRS, Blocks.BRICKS, Blocks.SMOOTH_STONE_SLAB, Blocks.BIRCH_DOOR, Blocks.BIRCH_FENCE,
			Blocks.BIRCH_STAIRS, Blocks.GLASS_PANE, Blocks.STONE_BRICKS, Blocks.DIRT_PATH, Blocks.BLUE_CARPET),
	/** Auditório moderno. */
	AUDITORIO(Blocks.POLISHED_ANDESITE, Blocks.SMOOTH_QUARTZ, Blocks.POLISHED_DEEPSLATE, Blocks.SMOOTH_QUARTZ_STAIRS, Blocks.SMOOTH_QUARTZ, Blocks.SMOOTH_QUARTZ_SLAB,
			Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_STAIRS, Blocks.GLASS_PANE, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE, Blocks.RED_CARPET);

	public final Block piso, parede, estrutura, telhadoEscada, telhadoBloco, laje, porta, cerca, banco, vidro, fundacao, caminho, tapete;

	Estilo(Block piso, Block parede, Block estrutura, Block telhadoEscada, Block telhadoBloco, Block laje, Block porta, Block cerca, Block banco, Block vidro, Block fundacao,
			Block caminho, Block tapete) {
		this.piso = piso;
		this.parede = parede;
		this.estrutura = estrutura;
		this.telhadoEscada = telhadoEscada;
		this.telhadoBloco = telhadoBloco;
		this.laje = laje;
		this.porta = porta;
		this.cerca = cerca;
		this.banco = banco;
		this.vidro = vidro;
		this.fundacao = fundacao;
		this.caminho = caminho;
		this.tapete = tapete;
	}

	/** Escolhe a paleta de um Centro Espírita conforme o bioma (com sorteio nos biomas temperados). */
	public static Estilo doBioma(Holder<Biome> bioma, RandomSource random) {
		if (bioma.is(Tags.Biomes.IS_DESERT))
			return ARENITO;
		if (bioma.is(Tags.Biomes.IS_BADLANDS))
			return TERRACOTA;
		if (bioma.is(Tags.Biomes.IS_SAVANNA))
			return ACACIA;
		if (bioma.is(Tags.Biomes.IS_SNOWY))
			return NEVE;
		if (bioma.is(Tags.Biomes.IS_TAIGA))
			return ABETO;
		if (bioma.is(Biomes.CHERRY_GROVE))
			return CEREJEIRA;
		if (bioma.is(Tags.Biomes.IS_JUNGLE))
			return SELVA;
		if (bioma.is(Tags.Biomes.IS_SWAMP))
			return MANGUE;
		if (bioma.is(Tags.Biomes.IS_BIRCH_FOREST))
			return random.nextBoolean() ? BETULA : BRANCO;
		return switch (random.nextInt(3)) {
			case 0 -> CARVALHO;
			case 1 -> TIJOLO;
			default -> BRANCO;
		};
	}

	public static Estilo porNome(String nome) {
		try {
			return valueOf(nome);
		} catch (IllegalArgumentException e) {
			return CARVALHO;
		}
	}
}
