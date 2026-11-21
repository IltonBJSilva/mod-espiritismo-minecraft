package net.mcreator.modespiritismominecraft.espiritismo.mundo;

import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.BlockTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.Optional;

/**
 * Peça única das construções espíritas. Segue o padrão do {@code SwampHutPiece}: tudo é desenhado em coordenadas
 * locais (x = largura, z = profundidade com a frente em z = 0, y = 0 é o piso) através de {@link #placeBlock}, que
 * aplica a orientação da peça e respeita o bounding box do chunk em geração. Os NPCs são colocados no próprio
 * {@code postProcess}, no chunk que contém a posição de cada um (assim cada NPC nasce uma única vez).
 * <p>
 * Os métodos de desenho são públicos para que {@link Plantas} (as plantas baixas) os use, e a mesma peça é
 * usada por {@link MundoApi#colocarCentroPequeno} fora do worldgen.
 */
public class PecaEspirita extends ScatteredFeaturePiece {
	private final Modelo modelo;
	private final Estilo estilo;
	private final boolean comNpcs;

	// Estado transitório durante a construção
	private WorldGenLevel level;
	private BoundingBox caixaChunk;
	private RandomSource aleatorio;
	private int ox, oz;

	public PecaEspirita(Modelo modelo, Estilo estilo, int oeste, int piso, int norte, Direction direcao, boolean comNpcs) {
		super(RegistroMundo.PECA_ESPIRITA.get(), oeste, piso, norte, modelo.largura, modelo.altura, modelo.profundidade, direcao);
		this.modelo = modelo;
		this.estilo = estilo;
		this.comNpcs = comNpcs;
		this.heightPosition = piso; // altura já decidida na estrutura
	}

	public PecaEspirita(CompoundTag tag) {
		super(RegistroMundo.PECA_ESPIRITA.get(), tag);
		this.modelo = Modelo.porId(tag.getStringOr("Modelo", "centro_pequeno"));
		this.estilo = Estilo.porNome(tag.getStringOr("Estilo", "CARVALHO"));
		this.comNpcs = tag.getBooleanOr("Npcs", true);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Modelo", modelo.id);
		tag.putString("Estilo", estilo.name());
		tag.putBoolean("Npcs", comNpcs);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos,
			BlockPos referencePos) {
		construir(level, chunkBB);
	}

	/** Desenha a parte da construção que cai dentro de {@code caixa}. */
	public void construir(WorldGenLevel level, BoundingBox caixa) {
		this.level = level;
		this.caixaChunk = caixa;
		// Aleatório determinístico pela posição: todos os chunks sorteiam a mesma decoração.
		this.aleatorio = RandomSource.create(Mth.getSeed(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ()));
		this.ox = 0;
		this.oz = 0;
		try {
			Plantas.construir(this);
		} finally {
			this.level = null;
			this.caixaChunk = null;
		}
	}

	// ------------------------------------------------------------------ acesso

	public Modelo modelo() {
		return modelo;
	}

	public Estilo estilo() {
		return estilo;
	}

	public RandomSource aleatorio() {
		return aleatorio;
	}

	/** Desloca a origem local (para reutilizar uma planta dentro de outra). */
	public void deslocar(int dx, int dz) {
		this.ox = dx;
		this.oz = dz;
	}

	/** Posição no mundo de uma coordenada local (sem o deslocamento de {@link #deslocar}). */
	public BlockPos posicaoLocal(int x, int y, int z) {
		return getWorldPos(x, y, z).immutable();
	}

	/** Posição no mundo de uma coordenada local. */
	public BlockPos mundo(int x, int y, int z) {
		return getWorldPos(x + ox, y, z + oz).immutable();
	}

	// ------------------------------------------------------------------ desenho básico

	public void b(int x, int y, int z, BlockState s) {
		placeBlock(level, s, x + ox, y, z + oz, caixaChunk);
	}

	public void b(int x, int y, int z, Block bloco) {
		b(x, y, z, bloco.defaultBlockState());
	}

	public void caixa(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
		for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
			for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
					b(x, y, z, s);
	}

	public void caixa(int x0, int y0, int z0, int x1, int y1, int z1, Block bloco) {
		caixa(x0, y0, z0, x1, y1, z1, bloco.defaultBlockState());
	}

	public void ar(int x0, int y0, int z0, int x1, int y1, int z1) {
		caixa(x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
	}

	/** Anel (só o perímetro) no nível y. */
	public void contorno(int x0, int y, int z0, int x1, int z1, BlockState s) {
		for (int x = x0; x <= x1; x++) {
			b(x, y, z0, s);
			b(x, y, z1, s);
		}
		for (int z = z0; z <= z1; z++) {
			b(x0, y, z, s);
			b(x1, y, z, s);
		}
	}

	/** Preenche de y = -1 para baixo até achar chão firme (sem estruturas flutuando). */
	public void fundacao(int x0, int z0, int x1, int z1, BlockState s) {
		for (int x = x0; x <= x1; x++)
			for (int z = z0; z <= z1; z++) {
				BlockPos.MutableBlockPos pos = getWorldPos(x + ox, -1, z + oz);
				if (!caixaChunk.isInside(pos))
					continue;
				for (int i = 0; i < 64 && pos.getY() > level.getMinY() + 1; i++) {
					BlockState atual = level.getBlockState(pos);
					if (!(atual.isAir() || atual.liquid() || atual.canBeReplaced() || atual.is(BlockTags.LEAVES) || atual.is(BlockTags.LOGS)))
						break;
					level.setBlock(pos, s, Block.UPDATE_CLIENTS);
					pos.move(Direction.DOWN);
				}
			}
	}

	// ------------------------------------------------------------------ estados úteis

	public static BlockState escada(Block bloco, Direction lado) {
		return bloco.defaultBlockState().trySetValue(StairBlock.FACING, lado);
	}

	public static BlockState escadaInvertida(Block bloco, Direction lado) {
		return escada(bloco, lado).trySetValue(StairBlock.HALF, Half.TOP);
	}

	public static BlockState lajeCima(Block bloco) {
		return bloco.defaultBlockState().trySetValue(SlabBlock.TYPE, SlabType.TOP);
	}

	public static BlockState virado(Block bloco, Direction lado) {
		BlockState s = bloco.defaultBlockState();
		if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
			s = s.setValue(BlockStateProperties.HORIZONTAL_FACING, lado);
		return s;
	}

	/** Vidraça/grade/cerca já conectada ao longo de um eixo local. */
	public static BlockState conectado(Block bloco, boolean aoLongoDeX) {
		BlockState s = bloco.defaultBlockState();
		if (s.hasProperty(CrossCollisionBlock.EAST)) {
			s = s.setValue(aoLongoDeX ? CrossCollisionBlock.EAST : CrossCollisionBlock.NORTH, true).setValue(aoLongoDeX ? CrossCollisionBlock.WEST : CrossCollisionBlock.SOUTH,
					true);
		}
		return s;
	}

	/** Bloco de outro agente (por ID) ou um substituto vanilla. Ajusta a direção se o bloco tiver FACING. */
	public static Optional<Block> blocoMod(String id) {
		return BuiltInRegistries.BLOCK.getOptional(RegistroMundo.id(id)).filter(bl -> bl != Blocks.AIR);
	}

	public static BlockState modOu(String id, BlockState substituto, Direction lado) {
		return blocoMod(id).map(bl -> virado(bl, lado)).orElse(substituto);
	}

	// ------------------------------------------------------------------ elementos

	public void porta(int x, int z, Direction lado) {
		BlockState base = estilo.porta.defaultBlockState().setValue(DoorBlock.FACING, lado);
		b(x, 1, z, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		b(x, 2, z, base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** Porta num andar qualquer (y = piso do andar). */
	public void porta(int x, int y, int z, Direction lado) {
		BlockState base = estilo.porta.defaultBlockState().setValue(DoorBlock.FACING, lado);
		b(x, y + 1, z, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		b(x, y + 2, z, base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	public void lanterna(int x, int y, int z, boolean pendurada) {
		b(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, pendurada));
	}

	/** Cama: pé em (x, z), cabeceira no sentido {@code lado}. */
	public void cama(int x, int y, int z, Block cama, Direction lado) {
		BlockState s = cama.defaultBlockState().setValue(BedBlock.FACING, lado);
		b(x, y, z, s.setValue(BedBlock.PART, BedPart.FOOT));
		b(x + lado.getStepX(), y, z + lado.getStepZ(), s.setValue(BedBlock.PART, BedPart.HEAD));
	}

	private static final Block[] VASOS = {Blocks.POTTED_FERN, Blocks.POTTED_RED_TULIP, Blocks.POTTED_OXEYE_DAISY, Blocks.POTTED_AZURE_BLUET, Blocks.POTTED_CORNFLOWER,
			Blocks.POTTED_OAK_SAPLING, Blocks.POTTED_LILY_OF_THE_VALLEY, Blocks.POTTED_DANDELION};

	public void vaso(int x, int y, int z) {
		b(x, y, z, VASOS[aleatorio.nextInt(VASOS.length)]);
	}

	private static final Block[] FLORES = {Blocks.POPPY, Blocks.DANDELION, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET, Blocks.CORNFLOWER, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
			Blocks.ALLIUM};

	public void flor(int x, int y, int z) {
		b(x, y, z, FLORES[aleatorio.nextInt(FLORES.length)]);
	}

	/** Livro sobre uma mesa: com o bloco {@code livro_1857} vai uma mesa (laje) + livro; senão, um atril (lectern). */
	public void livroNaMesa(int x, int y, int z, Direction lado) {
		Optional<Block> livro = blocoMod("livro_1857");
		if (livro.isPresent()) {
			b(x, y, z, lajeCima(estilo.laje));
			b(x, y + 1, z, virado(livro.get(), lado));
		} else {
			b(x, y, z, virado(Blocks.LECTERN, lado));
		}
	}

	/** Jarra de água sobre um apoio (substituto: vaso de flor). */
	public void jarraAgua(int x, int y, int z) {
		b(x, y, z, lajeCima(estilo.laje));
		b(x, y + 1, z, modOu("jarra_agua", Blocks.FLOWER_POT.defaultBlockState(), Direction.SOUTH));
	}

	/** Tribuna (substituto: atril). {@code lado} = para onde fica a plateia. */
	public void tribuna(int x, int y, int z, Direction lado) {
		b(x, y, z, modOu("tribuna", virado(Blocks.LECTERN, lado), lado));
	}

	/** Mesa mediúnica num retângulo (substituto: laje de abeto em cima + tapete branco). */
	public void mesaMediunica(int x0, int y, int z0, int x1, int z1) {
		Optional<Block> mesa = blocoMod("mesa_mediunica");
		for (int x = x0; x <= x1; x++)
			for (int z = z0; z <= z1; z++) {
				if (mesa.isPresent()) {
					b(x, y, z, virado(mesa.get(), Direction.SOUTH));
				} else {
					b(x, y, z, lajeCima(Blocks.SPRUCE_SLAB));
					b(x, y + 1, z, Blocks.WHITE_CARPET);
				}
			}
	}

	// ------------------------------------------------------------------ NPCs

	/** Coloca um NPC (por ID, ignorado se não existir) se a posição cair neste chunk. */
	public void npc(String id, int x, int y, int z, int raio) {
		if (!comNpcs)
			return;
		BlockPos pos = mundo(x, y, z);
		if (!caixaChunk.isInside(pos))
			return;
		Optional<EntityType<?>> tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(RegistroMundo.id(id));
		if (tipo.isEmpty())
			return;
		Entity e = tipo.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (e == null)
			return;
		e.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, aleatorio.nextFloat() * 360F, 0F);
		if (e instanceof Mob mob) {
			mob.setPersistenceRequired();
			mob.setHomeTo(pos, raio);
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		}
		level.addFreshEntityWithPassengers(e);
	}

	// ------------------------------------------------------------------ edifícios

	public enum Telhado {
		/** duas águas com a cumeeira ao longo de x */
		DUAS_AGUAS_X,
		/** duas águas com a cumeeira ao longo de z */
		DUAS_AGUAS_Z,
		/** laje plana com platibanda */
		PLANO
	}

	/**
	 * Prédio retangular: piso em y = 0, paredes de y = 1 a h com colunas nos cantos e janelas, interior limpo e
	 * telhado. As portas são abertas depois por quem chama.
	 */
	public void edificio(int x0, int z0, int x1, int z1, int h, Telhado telhado) {
		caixa(x0, 0, z0, x1, 0, z1, estilo.piso);
		for (int y = 1; y <= h; y++)
			contorno(x0, y, z0, x1, z1, estilo.parede.defaultBlockState());
		ar(x0 + 1, 1, z0 + 1, x1 - 1, h, z1 - 1);
		for (int y = 1; y <= h; y++) {
			b(x0, y, z0, estilo.estrutura);
			b(x1, y, z0, estilo.estrutura);
			b(x0, y, z1, estilo.estrutura);
			b(x1, y, z1, estilo.estrutura);
		}
		janelas(x0, z0, x1, z1, h >= 5 ? 3 : 2);
		telhado(x0, z0, x1, z1, h, telhado);
	}

	/** Janelas em todas as paredes, de y = 2 até {@code yMax}, uma a cada 3 blocos. */
	public void janelas(int x0, int z0, int x1, int z1, int yMax) {
		for (int y = 2; y <= yMax; y++) {
			for (int x = x0 + 2; x <= x1 - 2; x += 3) {
				b(x, y, z0, conectado(estilo.vidro, true));
				b(x, y, z1, conectado(estilo.vidro, true));
			}
			for (int z = z0 + 2; z <= z1 - 2; z += 3) {
				b(x0, y, z, conectado(estilo.vidro, false));
				b(x1, y, z, conectado(estilo.vidro, false));
			}
		}
	}

	public void telhado(int x0, int z0, int x1, int z1, int h, Telhado tipo) {
		int yb = h + 1;
		caixa(x0, yb, z0, x1, yb, z1, estilo.telhadoBloco);
		switch (tipo) {
			case PLANO -> {
				contorno(x0, yb + 1, z0, x1, z1, estilo.laje.defaultBlockState());
			}
			case DUAS_AGUAS_X -> {
				for (int k = 0;; k++) {
					int za = z0 - 1 + k, zb = z1 + 1 - k, y = yb + k;
					if (za > zb)
						break;
					if (za == zb) {
						for (int x = x0 - 1; x <= x1 + 1; x++)
							b(x, y, za, estilo.laje);
						break;
					}
					for (int x = x0 - 1; x <= x1 + 1; x++) {
						b(x, y, za, escada(estilo.telhadoEscada, Direction.NORTH));
						b(x, y, zb, escada(estilo.telhadoEscada, Direction.SOUTH));
					}
					if (k > 0)
						for (int z = za + 1; z <= zb - 1; z++) {
							b(x0, y, z, estilo.parede);
							b(x1, y, z, estilo.parede);
						}
				}
			}
			case DUAS_AGUAS_Z -> {
				for (int k = 0;; k++) {
					int xa = x0 - 1 + k, xb = x1 + 1 - k, y = yb + k;
					if (xa > xb)
						break;
					if (xa == xb) {
						for (int z = z0 - 1; z <= z1 + 1; z++)
							b(xa, y, z, estilo.laje);
						break;
					}
					for (int z = z0 - 1; z <= z1 + 1; z++) {
						b(xa, y, z, escada(estilo.telhadoEscada, Direction.EAST));
						b(xb, y, z, escada(estilo.telhadoEscada, Direction.WEST));
					}
					if (k > 0)
						for (int x = xa + 1; x <= xb - 1; x++) {
							b(x, y, z0, estilo.parede);
							b(x, y, z1, estilo.parede);
						}
				}
			}
		}
	}

	/** Parede interna (ao longo de x ou z) de y = 1 a h. */
	public void paredeInterna(int x0, int z0, int x1, int z1, int h) {
		caixa(x0, 1, z0, x1, h, z1, estilo.parede);
	}

	/** Abre uma passagem de porta (com porta) numa parede. */
	public void abrirPorta(int x, int z, Direction lado) {
		b(x, 3, z, estilo.parede); // verga
		porta(x, z, lado);
	}

	/** Fileira de bancos (escadas) de x0 a x1 em z, olhando para +z (a frente local onde fica a mesa/tribuna). */
	public void fileiraBancos(int x0, int x1, int y, int z) {
		for (int x = x0; x <= x1; x++)
			b(x, y, z, escada(estilo.banco, Direction.SOUTH));
	}

	/** Linha de cerca conectada. */
	public void cercaX(int x0, int x1, int y, int z) {
		for (int x = x0; x <= x1; x++)
			b(x, y, z, conectado(estilo.cerca, true));
	}

	public void cercaZ(int x, int y, int z0, int z1) {
		for (int z = z0; z <= z1; z++)
			b(x, y, z, conectado(estilo.cerca, false));
	}
}
