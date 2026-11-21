package net.mcreator.modespiritismominecraft.espiritismo.protecao;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Zonas protegidas extras, além da dimensão Nosso Lar (que é protegida inteira por {@code ColoniaApi.protegido}).
 * Usadas pelos GameTests (que rodam no Overworld) e disponíveis para outras áreas (ex.: proteger um Posto de Socorro).
 * Só em memória: quem registra deve remover.
 */
public final class ZonasProtegidas {
	private record Zona(ResourceKey<Level> dim, BoundingBox caixa) {
	}

	private static final List<Zona> ZONAS = new CopyOnWriteArrayList<>();

	private ZonasProtegidas() {
	}

	public static void registrar(ResourceKey<Level> dim, BoundingBox caixa) {
		ZONAS.add(new Zona(dim, caixa));
	}

	public static void remover(ResourceKey<Level> dim, BoundingBox caixa) {
		ZONAS.remove(new Zona(dim, caixa));
	}

	public static boolean contem(Level level, BlockPos pos) {
		if (ZONAS.isEmpty())
			return false;
		for (Zona z : ZONAS)
			if (z.dim() == level.dimension() && z.caixa().isInside(pos))
				return true;
		return false;
	}
}
