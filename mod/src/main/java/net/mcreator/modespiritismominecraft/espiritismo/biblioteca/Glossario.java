package net.mcreator.modespiritismominecraft.espiritismo.biblioteca;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Glossário de termos espíritas. Definições são explicações do próprio mod (paráfrases), com a referência onde o tema
 * é tratado. A coluna "origem" separa o que é das obras básicas do que vem de obras/práticas posteriores (PROMPT_MESTRE §2.2).
 */
public final class Glossario {
	private Glossario() {
	}

	public enum Origem {
		OBRAS_BASICAS("Obras básicas de Kardec"),
		OBRA_MEDIUNICA("Obra mediúnica posterior (ex.: André Luiz)"),
		PRATICA("Prática do movimento espírita"),
		JOGO("Termo do jogo");

		public final String rotulo;

		Origem(String rotulo) {
			this.rotulo = rotulo;
		}
	}

	public record Termo(String id, String nome, String definicao, String referencia, Origem origem) {
	}

	public static final Map<String, Termo> TERMOS = new LinkedHashMap<>();

	private static void t(String id, String nome, Origem origem, String ref, String def) {
		TERMOS.put(id, new Termo(id, nome, def, ref, origem));
	}

	static {
		Origem K = Origem.OBRAS_BASICAS;
		t("espirito", "Espírito", K, "LE, q. 23 e 76",
				"O ser inteligente da criação. Quando está ligado a um corpo, chama-se alma (encarnado); fora do corpo, Espírito desencarnado.");
		t("perispirito", "Perispírito", K, "LE, q. 93 a 95 e 135; Gênese, cap. XIV",
				"Envoltório semimaterial do Espírito, que o liga ao corpo físico durante a encarnação e o acompanha depois da morte.");
		t("encarnacao", "Encarnação / desencarnação", K, "LE, q. 132 e seguintes; q. 149 a 165",
				"Encarnar é o Espírito unir-se a um corpo; desencarnar é deixá-lo na morte do corpo. O Espírito continua o mesmo.");
		t("reencarnacao", "Reencarnação", K, "LE, q. 166 e seguintes; ESE, cap. IV",
				"Retorno do Espírito a uma nova vida corporal para aprender e progredir. O esquecimento do passado protege e dá novas oportunidades.");
		t("perturbacao", "Perturbação espiritual", K, "LE, q. 163 a 165",
				"Estado de confusão que pode seguir a morte do corpo, mais ou menos longo conforme o apego à matéria e a preparação de cada um.");
		t("livre_arbitrio", "Livre-arbítrio", K, "LE, q. 843 e seguintes",
				"Liberdade de escolher os próprios atos e, com ela, a responsabilidade pelas consequências.");
		t("causa_efeito", "Lei de causa e efeito", K, "CI, 1ª parte, cap. VII",
				"Cada um colhe o que planta, mas não como castigo de vingança: as consequências educam, e o bem praticado sempre pode reparar o mal.");
		t("escala_espirita", "Escala espírita", K, "LE, q. 100 a 113",
				"Classificação didática dos Espíritos em ordens (imperfeitos, bons, puros) conforme o progresso. Todos podem progredir.");
		t("pluralidade_mundos", "Pluralidade dos mundos habitados", K, "LE, q. 55; ESE, cap. III",
				"Ideia de que há muitos mundos habitados, em diferentes graus de adiantamento (\"Há muitas moradas na casa de meu Pai\" — Jo 14:2; ESE, cap. III).");
		t("mediunidade", "Mediunidade / médium", K, "LM, 2ª parte, cap. XIV, item 159",
				"Faculdade de sentir a influência dos Espíritos ou servir de intermediário a eles. Não é privilégio nem poder; exige estudo e moral.");
		t("psicografia", "Psicografia", K, "LM, cap. XV",
				"Comunicação dos Espíritos pela escrita do médium (mecânica, semimecânica ou intuitiva).");
		t("obsessao", "Obsessão", K, "LM, cap. XXIII",
				"Domínio que um Espírito imperfeito exerce sobre alguém. Graus: obsessão simples, fascinação e subjugação. O obsessor também precisa de ajuda.");
		t("desobsessao", "Desobsessão", Origem.PRATICA, "Base: LM, cap. XXIII; ESE, cap. XXVIII",
				"Nome dado pelo movimento espírita ao tratamento da obsessão: prece, reforma íntima de quem sofre e esclarecimento do Espírito, em reunião séria e gratuita.");
		t("fluido_universal", "Fluido cósmico universal", K, "LE, q. 27; Gênese, cap. XIV",
				"Matéria elementar de que tudo se forma; a ação da vontade sobre os fluidos explica o passe sem nada de sobrenatural.");
		t("passe", "Passe", Origem.PRATICA, "Base: Gênese, cap. XIV",
				"Transmissão de fluidos por imposição de mãos e prece. Apoio ao equilíbrio, não substitui o tratamento médico.");
		t("agua_fluidificada", "Água fluidificada", Origem.PRATICA, "Base: LM, 2ª parte, cap. VIII, item 131",
				"Água que recebe fluidos pela prece ou passe. É apoio, nunca cura mágica, remédio contra obsessão ou proteção garantida.");
		t("prece", "Prece", K, "ESE, cap. XXVII e XXVIII",
				"Pensamento sincero dirigido a Deus. Vale pelo sentimento, não por fórmulas, horários ou objetos.");
		t("caridade", "Caridade", K, "LE, q. 886",
				"Benevolência para com todos, indulgência para as imperfeições dos outros, perdão das ofensas. Material e moral.");
		t("reforma_intima", "Reforma íntima", Origem.PRATICA, "Base: ESE, cap. XVII, item 4",
				"Esforço diário de cada um para vencer as próprias más inclinações. Expressão do movimento espírita.");
		t("emancipacao", "Emancipação da alma", K, "LE, q. 400 e seguintes",
				"Relativa liberdade do Espírito durante o sono do corpo, quando pode se afastar dele sem romper o laço.");
		t("erraticidade", "Erraticidade", K, "LE, q. 223 e seguintes",
				"Estado dos Espíritos entre uma encarnação e outra, durante o qual também aprendem e progridem.");
		t("umbral", "Umbral", Origem.OBRA_MEDIUNICA, "Nosso Lar (André Luiz / Chico Xavier, FEB, 1944)",
				"Região espiritual de sofrimento descrita nas obras de André Luiz, onde ficam Espíritos presos ao egoísmo, ao remorso ou à revolta. Não é inferno eterno.");
		t("colonia", "Colônia espiritual", Origem.OBRA_MEDIUNICA, "Nosso Lar (FEB, 1944)",
				"Cidade do plano espiritual descrita por André Luiz, como Nosso Lar, com trabalho, estudo e socorro.");
		t("evangelho_no_lar", "Evangelho no Lar", Origem.PRATICA, "Prática difundida pela FEB; leitura de O Evangelho segundo o Espiritismo",
				"Encontro semanal da família para prece, leitura e conversa sobre o Evangelho. Não é ritual nem garante proteção: é estudo e convivência.");
		t("moralidade_jogo", "Moralidade (no jogo)", Origem.JOGO, "Mecânica do mod",
				"Barra abstrata do jogo que representa escolhas. Não é julgamento de pessoas reais, e conhecimento não é o mesmo que elevação moral.");
	}
}
