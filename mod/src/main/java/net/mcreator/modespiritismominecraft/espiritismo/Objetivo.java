package net.mcreator.modespiritismominecraft.espiritismo;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

/** Alvo da seta do HUD ("GPS" espiritual): objetivo do tutorial ou local achado pela Bússola Espiritual. */
public final class Objetivo {
	private Objetivo() {
	}

	public static BlockPos get(Player player) {
		long v = player.getData(Moralidade.OBJETIVO);
		return v == Long.MIN_VALUE ? null : BlockPos.of(v);
	}

	public static void definir(Player player, BlockPos pos) {
		long v = pos == null ? Long.MIN_VALUE : pos.asLong();
		if (player.getData(Moralidade.OBJETIVO) != v)
			player.setData(Moralidade.OBJETIVO, v);
	}

	public static void limpar(Player player) {
		definir(player, null);
	}

	/** Seta de 8 direções relativa para onde o jogador olha. */
	public static String seta(Player player, BlockPos alvo) {
		double dx = alvo.getX() + 0.5 - player.getX(), dz = alvo.getZ() + 0.5 - player.getZ();
		double yawAlvo = Math.toDegrees(Math.atan2(-dx, dz));
		double rel = ((yawAlvo - player.getYRot()) % 360 + 540) % 360 - 180;
		int i = (int) Math.round(rel / 45.0) & 7;
		return "↑↗→↘↓↙←↖".substring(i, i + 1);
	}

	public static int distancia(Player player, BlockPos alvo) {
		return (int) Math.sqrt(player.blockPosition().distSqr(alvo));
	}
}
