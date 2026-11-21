package net.mcreator.modespiritismominecraft.espiritismo.estado;

/**
 * Estados espirituais do jogador (máquina de estados). A ORDEM é persistida (índice salvo no jogador):
 * só acrescente valores NO FIM.
 */
public enum EstadoEspiritual {
	ENCARNADO_ESTAVEL,
	ENCARNADO_PERTURBADO,
	DESENCARNANDO,
	RECEM_DESENCARNADO,
	EM_ACOLHIMENTO,
	EM_NOSSO_LAR,
	EM_TRATAMENTO,
	EM_VISITA_ASSISTIDA,
	NO_UMBRAL_ORIENTADO,
	NO_UMBRAL_PERTURBADO,
	EM_RESGATE,
	PRONTO_PARA_REENCARNAR,
	REENCARNANDO;

	public static EstadoEspiritual porIndice(int i) {
		EstadoEspiritual[] v = values();
		return i >= 0 && i < v.length ? v[i] : ENCARNADO_ESTAVEL;
	}

	/** Estados de sofrimento/confusão: o jogador precisa de cuidado e NÃO assume o socorro de outros. */
	public boolean precisaDeCuidado() {
		return this == ENCARNADO_PERTURBADO || this == RECEM_DESENCARNADO || this == EM_ACOLHIMENTO || this == EM_TRATAMENTO || this == NO_UMBRAL_PERTURBADO || this == EM_RESGATE;
	}

	/** Estados em que ataques e destruição de blocos são bloqueados. */
	public boolean semViolencia() {
		return this == NO_UMBRAL_PERTURBADO || this == EM_RESGATE || this == EM_ACOLHIMENTO || this == EM_TRATAMENTO || this == DESENCARNANDO || this == REENCARNANDO;
	}
}
