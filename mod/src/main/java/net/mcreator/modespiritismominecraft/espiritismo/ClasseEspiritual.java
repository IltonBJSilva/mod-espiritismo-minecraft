package net.mcreator.modespiritismominecraft.espiritismo;

/** As classes do documento (seção "Classes"). */
public enum ClasseEspiritual {
	NENHUMA("Sem classe", "", "", 0, 0),
	MEDIUM("Médium", "Comunica-se com os Espíritos. Agachado, vê presenças ocultas (Visão Espiritual).", "Psicografia", 30, 200),
	DOUTRINADOR("Doutrinador", "Pacifica criaturas hostis. Aura de Paz: sofre 20% menos dano de hostis próximos.", "Evangelização", 35, 300),
	TRABALHADOR("Trabalhador Espiritual", "Pilar de caridade e perseverança. Mais resistente às obsessões.", "Prece Sincera", 30, 900);

	public final String nome;
	public final String descricao;
	public final String habilidade;
	/** Energia espiritual gasta pela habilidade ativa. */
	public final int custo;
	/** Recarga da habilidade ativa, em ticks. */
	public final int recarga;

	ClasseEspiritual(String nome, String descricao, String habilidade, int custo, int recarga) {
		this.nome = nome;
		this.descricao = descricao;
		this.habilidade = habilidade;
		this.custo = custo;
		this.recarga = recarga;
	}

	public static ClasseEspiritual porIndice(int i) {
		ClasseEspiritual[] v = values();
		return i >= 0 && i < v.length ? v[i] : NENHUMA;
	}
}
