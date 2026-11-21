package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import java.util.List;

/**
 * Papéis dos NPCs de Nosso Lar (§3.5 / §8.1). Cada tipo define onde os genéricos aparecem, o "jeito" de trabalhar,
 * falas comuns e se o trabalho dele é de ASSISTÊNCIA a outros (vedado a quem ainda precisa de cuidado).
 * Falas: orientações originais do jogo (não são citações).
 */
public enum TipoNpc {
	RECEM_CHEGADO("Recém-chegado", Atividade.DESCANSAR, false, null, List.of("chegada", "auxilio"), List.of(
			"Ainda não sei bem onde estou... mas aqui ninguém me apressa.",
			"Disseram que dormi muito tempo. Agora, aos poucos, vou entendendo.",
			"Sinto saudade dos que ficaram. Me disseram que a saudade também se cuida, com prece e com trabalho.")),
	PACIENTE("Em recuperação", Atividade.DESCANSAR, false, null, List.of("hospital", "regeneracao", "camaras_retificacao"), List.of(
			"O tratamento é lento, mas sinto que melhoro um pouco a cada dia.",
			"Ainda me canso fácil. Os enfermeiros dizem que é assim mesmo: ninguém se refaz de uma vez.",
			"Quando a mente se agita, peço ajuda. Já não me envergonho disso.")),
	ENFERMEIRO("Enfermagem", Atividade.TRABALHAR, true, "trabalho", List.of("hospital", "auxilio", "camaras_retificacao", "regeneracao"), List.of(
			"Cuidar é trabalho de paciência: ninguém se recupera às pressas.",
			"Aqui o remédio vem junto com a escuta e o bom ânimo.",
			"Se você estiver cansado, descanse. Ajudar os outros vem depois de estar firme.")),
	ORIENTADOR("Orientação", Atividade.CONVERSAR, true, null, List.of("chegada", "auxilio", "praca"), List.of(
			"Pergunte o que quiser: dúvida não é falta de fé.",
			"Cada recém-chegado tem o seu tempo. Ninguém aqui é comparado a ninguém.",
			"Perdeu-se? Procure a praça: dela se chega a quase todos os lugares.")),
	INSTRUTOR("Instrução", Atividade.TRABALHAR, false, "estudo", List.of("esclarecimento", "escola"), List.of(
			"Há muitas moradas na casa de meu Pai: o Evangelho fala da variedade dos mundos (ESE, cap. III).",
			"A pluralidade das existências é um dos ensinos centrais de O Livro dos Espíritos (LE, q. 166 e seguintes).",
			"Estudar não é decorar: é compreender e depois viver o que se compreendeu.")),
	MENSAGEIRO("Mensageiro", Atividade.ANDAR, false, null, List.of("comunicacao", "praca", "governadoria"), List.of(
			"Levo recados entre os Ministérios. Comunicação também é serviço!",
			"Mensagem boa é a que chega na hora certa e com a palavra certa.",
			"Hoje já cruzei a colônia três vezes. Ainda bem que gosto de caminhar.",
			"Precisa servir fora da colônia? Fale comigo agachado: posso acompanhá-lo até o Posto de Socorro do Umbral.")),
	BIBLIOTECARIO("Biblioteca", Atividade.TRABALHAR, false, "estudo", List.of("biblioteca", "esclarecimento"), List.of(
			"Os livros estão organizados por assunto. Leia com calma e pergunte quando não entender.",
			"Estudar sem praticar é como guardar sementes sem nunca plantar.",
			"Cada obra traz autor e fonte. Saber de onde vem um ensino também é estudo.")),
	JARDINEIRO("Jardins", Atividade.TRABALHAR, false, "trabalho", List.of("bosque", "praca", "residencias"), List.of(
			"Cuidar das plantas acalma a mente. Aqui nenhuma flor é arrancada à toa.",
			"Cada casa cultiva as suas flores preferidas; dá gosto passear pelas ruas.",
			"Regar, podar, esperar. O jardim ensina paciência.")),
	ARTIFICE("Oficinas", Atividade.TRABALHAR, false, "trabalho", List.of("comunicacao", "estacao_aerobus", "regeneracao"), List.of(
			"Na oficina consertamos e preparamos o que os Ministérios precisam.",
			"Todo trabalho honesto tem valor. Aqui ninguém fica sem o que fazer, se quiser servir.",
			"Ferramenta limpa e guardada no lugar: ordem também é cuidado com o próximo.")),
	MORADOR("Morador", Atividade.CONVERSAR, false, null, List.of("residencias", "praca", "bosque", "elevacao", "uniao_divina"), List.of(
			"Bom dia! A colônia é grande; se se perder, peça ajuda a qualquer um de nós.",
			"À tarde gosto de ouvir música perto do bosque.",
			"Moro aqui há alguns anos e ainda aprendo todo dia.")),
	ADMINISTRADOR("Administração", Atividade.TRABALHAR, false, null, List.of("governadoria"), List.of(
			"Na Governadoria passam pedidos e relatórios de toda a colônia.",
			"Organizar bem é uma forma de servir: ninguém fica esquecido numa pilha de papéis.",
			"Fale baixo, por favor: há quem esteja em reunião.")),
	MINISTRO("Ministério", Atividade.TRABALHAR, false, null, List.of("auxilio", "regeneracao"), List.of(
			"O trabalho é a melhor oração em ação.",
			"Quem serve com alegria descansa melhor.")),
	GOVERNADOR("Governadoria", Atividade.TRABALHAR, false, null, List.of("governadoria"), List.of(
			"Paz a você.")),
	HISTORICO("Personagem histórica", Atividade.TRABALHAR, false, null, List.of(), List.of());

	/** O que o NPC faz quando chega ao destino. */
	public enum Atividade {
		TRABALHAR, CONVERSAR, DESCANSAR, ANDAR
	}

	public final String rotulo;
	public final Atividade atividade;
	/** Trabalho de assistência a outros (enfermagem/orientação): exige que o jogador esteja estável para participar. */
	public final boolean assistencia;
	/** Se conversar com este tipo permite escolher "estudo" ou "trabalho" na jornada do recém-chegado (null = não). */
	public final String escolha;
	/** Zonas onde genéricos deste tipo aparecem (a primeira é o local de trabalho padrão). */
	public final List<String> zonas;
	public final List<String> falas;

	TipoNpc(String rotulo, Atividade atividade, boolean assistencia, String escolha, List<String> zonas, List<String> falas) {
		this.rotulo = rotulo;
		this.atividade = atividade;
		this.assistencia = assistencia;
		this.escolha = escolha != null ? escolha : (assistencia ? "trabalho" : null);
		this.zonas = zonas;
		this.falas = falas;
	}

	/** Falas comuns a todos os NPCs quando o jogador está em sofrimento/tratamento: acolhem, sem cobrar nada. */
	public static final List<String> FALAS_CUIDADO = List.of(
			"Calma. Você está em segurança aqui. Respire devagar; ninguém vai lhe exigir nada agora.",
			"Agora é tempo de receber cuidado, não de dar. Isso não é fraqueza: é parte da recuperação.",
			"Se a confusão voltar, procure um enfermeiro ou faça uma prece simples (tecla B). Ajuda não falta.",
			"Descanse. As tarefas podem esperar até você estar firme.");

	/** Falas quando o NPC está fora de Nosso Lar (colocado por ovo em outro lugar). */
	public static final List<String> FALAS_FORA = List.of(
			"Estou longe da colônia, mas o trabalho do bem se faz em qualquer lugar.",
			"Onde houver alguém precisando de uma palavra amiga, há serviço.");
}
