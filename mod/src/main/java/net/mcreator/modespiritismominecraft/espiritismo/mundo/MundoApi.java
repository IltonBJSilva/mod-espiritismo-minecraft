package net.mcreator.modespiritismominecraft.espiritismo.mundo;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

/**
 * API pública da área "mundo" para o integrador (tutorial, bússola, comandos).
 */
public final class MundoApi {
	/** Raio de busca padrão de {@link #localizar}, em chunks (o mesmo do /locate). */
	public static final int RAIO_BUSCA_CHUNKS = 100;

	private MundoApi() {
	}

	/**
	 * Constrói um Centro Espírita pequeno no lote que começa em {@code origemNoChao} (canto de x e z mínimos; o piso
	 * fica nessa altura; o lote ocupa 15 x 18 blocos em x/z e a entrada fica voltada para o sul, z maior).
	 * Usa exatamente a mesma peça do worldgen.
	 *
	 * @return a posição (bloco de baixo) da porta da frente
	 */
	public static BlockPos colocarCentroPequeno(ServerLevel level, BlockPos origemNoChao, boolean comNpcs) {
		return colocar(level, origemNoChao, Modelo.CENTRO_PEQUENO, Estilo.CARVALHO, comNpcs);
	}

	/**
	 * Constrói qualquer modelo (orientação fixa: entrada para o sul, z maior). Devolve a posição da porta do centro pequeno
	 * ou, nos outros modelos, o meio da frente do lote.
	 */
	public static BlockPos colocar(ServerLevel level, BlockPos origemNoChao, Modelo modelo, Estilo estilo, boolean comNpcs) {
		// Orientação NORTH: a coordenada local z = 0 (frente) fica no maxZ do lote, ou seja, a entrada olha para o sul.
		PecaEspirita peca = new PecaEspirita(modelo, estilo, origemNoChao.getX(), origemNoChao.getY(), origemNoChao.getZ(), Direction.NORTH, comNpcs);
		BoundingBox b = peca.getBoundingBox();
		BoundingBox tudo = new BoundingBox(b.minX() - 2, level.getMinY(), b.minZ() - 2, b.maxX() + 2, level.getMaxY(), b.maxZ() + 2);
		peca.construir(level, tudo);
		if (modelo == Modelo.CENTRO_PEQUENO)
			return peca.posicaoLocal(Plantas.PORTA_CENTRO_PEQUENO[0], Plantas.PORTA_CENTRO_PEQUENO[1], Plantas.PORTA_CENTRO_PEQUENO[2]);
		return peca.posicaoLocal(modelo.largura / 2, 1, 0);
	}

	/**
	 * Posição da estrutura mais próxima da tag (ex.: {@link RegistroMundo#CENTROS_ESPIRITAS}), ou null se não houver
	 * no raio padrão (ou se a geração de estruturas estiver desligada).
	 */
	@Nullable
	public static BlockPos localizar(ServerLevel level, BlockPos origem, TagKey<Structure> tag) {
		return localizar(level, origem, tag, RAIO_BUSCA_CHUNKS);
	}

	@Nullable
	public static BlockPos localizar(ServerLevel level, BlockPos origem, TagKey<Structure> tag, int raioChunks) {
		return level.findNearestMapStructure(tag, origem, raioChunks, false);
	}
}
