package net.mcreator.modespiritismominecraft.espiritismo.mundo;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.Optional;

/**
 * Estrutura procedural (padrão da cabana da bruxa): uma única peça {@link PecaEspirita} desenhada em código.
 * A altura é decidida aqui (a partir do gerador, de forma determinística), para que todos os chunks usem o
 * mesmo piso e para que a adaptação de terreno ("beard_box") consiga nivelar o chão em volta.
 */
public class EstruturaEspirita extends Structure {
	public static final MapCodec<EstruturaEspirita> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(settingsCodec(i), Modelo.CODEC.fieldOf("modelo").forGetter(e -> e.modelo)).apply(i, EstruturaEspirita::new));

	private final Modelo modelo;

	public EstruturaEspirita(Structure.StructureSettings settings, Modelo modelo) {
		super(settings);
		this.modelo = modelo;
	}

	public Modelo modelo() {
		return modelo;
	}

	@Override
	protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext ctx) {
		Direction direcao = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
		ChunkPos chunk = ctx.chunkPos();
		int x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ();
		boolean girado = direcao.getAxis() == Direction.Axis.X;
		int sx = girado ? modelo.profundidade : modelo.largura;
		int sz = girado ? modelo.largura : modelo.profundidade;
		ChunkGenerator gen = ctx.chunkGenerator();
		// Amostra 5x5 pontos do lote: média para o piso, rejeita terreno muito acidentado ou com água.
		int soma = 0, n = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int i = 0; i <= 4; i++)
			for (int j = 0; j <= 4; j++) {
				int x = x0 + (sx - 1) * i / 4, z = z0 + (sz - 1) * j / 4;
				int h = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
				int fundo = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
				if (h != fundo)
					return Optional.empty(); // água no lote (rio, lago, mar)
				soma += h;
				n++;
				min = Math.min(min, h);
				max = Math.max(max, h);
			}
		int limite = Math.max(8, Math.max(sx, sz) / 4);
		if (max - min > limite)
			return Optional.empty();
		int y = Math.round(soma / (float) n);
		if (y < gen.getSeaLevel())
			return Optional.empty();
		BlockPos centro = new BlockPos(x0 + sx / 2, y, z0 + sz / 2);
		Estilo estilo = modelo.estiloFixo;
		if (estilo == null) {
			Holder<Biome> bioma = ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(centro.getX()), QuartPos.fromBlock(y), QuartPos.fromBlock(centro.getZ()),
					ctx.randomState().sampler());
			estilo = Estilo.doBioma(bioma, ctx.random());
		}
		Estilo estiloFinal = estilo;
		return Optional.of(new Structure.GenerationStub(centro, b -> b.addPiece(new PecaEspirita(modelo, estiloFinal, x0, y, z0, direcao, true))));
	}

	@Override
	public StructureType<?> type() {
		return RegistroMundo.CONSTRUCAO_ESPIRITA.get();
	}
}
