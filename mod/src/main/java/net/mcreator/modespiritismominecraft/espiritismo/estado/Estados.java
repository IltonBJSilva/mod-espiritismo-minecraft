package net.mcreator.modespiritismominecraft.espiritismo.estado;

import net.minecraft.world.entity.player.Player;

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;

/**
 * CONTRATO entre as áreas: consulta e transição do estado espiritual do jogador.
 * Assinaturas estáveis; a área "umbral/estado" implementa as regras de transição.
 */
public final class Estados {
	private Estados() {
	}

	public static EstadoEspiritual get(Player player) {
		return EstadoEspiritual.porIndice(player.getData(Moralidade.ESTADO));
	}

	/**
	 * Muda o estado. Devolve false se a transição for inválida.
	 * (Implementação inicial: aceita qualquer transição; a área "umbral" define as válidas.)
	 */
	public static boolean transicionar(Player player, EstadoEspiritual novo, String motivo) {
		if (player.level().isClientSide())
			return false;
		if (get(player) != novo)
			player.setData(Moralidade.ESTADO, novo.ordinal());
		return true;
	}

	/** Perturbado/confuso: efeitos de confusão, sem violência nem destruição. */
	public static boolean perturbado(Player player) {
		EstadoEspiritual e = get(player);
		return e == EstadoEspiritual.NO_UMBRAL_PERTURBADO || e == EstadoEspiritual.ENCARNADO_PERTURBADO;
	}

	/** "Paciente não ajuda paciente": pode assumir tarefas de socorro/assistência a outros? */
	public static boolean podeSocorrer(Player player) {
		return !get(player).precisaDeCuidado() && Moralidade.get(player) > Moralidade.BAIXA;
	}

	/** Ataques e quebra/colocação de blocos bloqueados pelo estado (não pela região). */
	public static boolean semViolencia(Player player) {
		return get(player).semViolencia();
	}
}
