package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.ColoniaApi;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;

/** Zonas da colônia (nomes de {@link ColoniaApi#local}) e utilidades de posição para o povoamento e as rotinas. */
public final class Zonas {
	private Zonas() {
	}

	public static final List<String> MINISTERIOS = List.of("regeneracao", "auxilio", "comunicacao", "esclarecimento", "elevacao", "uniao_divina");

	/** Nome exibido de cada zona. */
	public static final Map<String, String> NOMES = new LinkedHashMap<>();

	/** Limite de moradores genéricos por zona (perto do ponto da zona). */
	public static final Map<String, Integer> COTA = new LinkedHashMap<>();

	static {
		zona("chegada", "Portão de Chegada", 3);
		zona("auxilio", "Ministério do Auxílio", 5);
		zona("hospital", "Hospital", 5);
		zona("regeneracao", "Ministério da Regeneração", 4);
		zona("camaras_retificacao", "Câmaras de Retificação", 3);
		zona("comunicacao", "Ministério da Comunicação", 3);
		zona("esclarecimento", "Ministério do Esclarecimento", 4);
		zona("escola", "Escola", 3);
		zona("biblioteca", "Biblioteca", 3);
		zona("elevacao", "Ministério da Elevação", 3);
		zona("uniao_divina", "Ministério da União Divina", 2);
		zona("governadoria", "Governadoria", 3);
		zona("praca", "Praça", 6);
		zona("bosque", "Bosque", 4);
		zona("residencias", "Residências", 5);
		zona("estacao_aerobus", "Estação", 2);
	}

	private static void zona(String id, String nome, int cota) {
		NOMES.put(id, nome);
		COTA.put(id, cota);
	}

	public static String nome(String zona) {
		return NOMES.getOrDefault(zona, zona);
	}

	/** Ponto REAL da zona definido pela área "colonia" (ou null se ainda não existe). */
	@Nullable
	public static BlockPos real(ServerLevel level, String zona) {
		if (zona == null || zona.isEmpty())
			return null;
		return ColoniaApi.local(level, zona);
	}

	/**
	 * Ponto da zona; se a colônia ainda não definiu essa zona, uma posição estimada num anel ao redor da praça
	 * (ou da chegada), para a cidade não ficar vazia enquanto cresce. Fora de Nosso Lar: null.
	 */
	@Nullable
	public static BlockPos posicao(ServerLevel level, String zona) {
		if (level.dimension() != Planos.NOSSO_LAR || zona == null || zona.isEmpty())
			return null;
		BlockPos p = real(level, zona);
		if (p != null)
			return p;
		BlockPos base = real(level, "praca");
		if (base == null)
			base = real(level, "chegada");
		if (base == null)
			return null;
		int i = List.copyOf(NOMES.keySet()).indexOf(zona);
		if (i < 0)
			i = Math.floorMod(zona.hashCode(), 16);
		double ang = i * (Math.PI * 2 / 16);
		return base.offset((int) Math.round(Math.cos(ang) * 12), 0, (int) Math.round(Math.sin(ang) * 12));
	}

	/** Procura um lugar onde caiba uma pessoa em pé (chão firme, 2 blocos livres, sem líquido) perto de {@code base}. */
	@Nullable
	public static BlockPos chao(ServerLevel level, BlockPos base, RandomSource r, int raio) {
		for (int tentativa = 0; tentativa < 10; tentativa++) {
			int x = base.getX() + (tentativa == 0 ? 0 : r.nextInt(raio * 2 + 1) - raio);
			int z = base.getZ() + (tentativa == 0 ? 0 : r.nextInt(raio * 2 + 1) - raio);
			for (int dy = 4; dy >= -6; dy--) {
				BlockPos p = new BlockPos(x, base.getY() + dy, z);
				if (!level.isLoaded(p))
					break;
				if (cabe(level, p))
					return p;
			}
		}
		return null;
	}

	public static boolean cabe(ServerLevel level, BlockPos p) {
		BlockState chao = level.getBlockState(p.below());
		return chao.isFaceSturdy(level, p.below(), Direction.UP) && livre(level, p) && livre(level, p.above());
	}

	private static boolean livre(ServerLevel level, BlockPos p) {
		BlockState s = level.getBlockState(p);
		return s.getCollisionShape(level, p).isEmpty() && s.getFluidState().isEmpty();
	}
}
