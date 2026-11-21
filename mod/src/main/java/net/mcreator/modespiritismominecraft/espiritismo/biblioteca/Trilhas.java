package net.mcreator.modespiritismominecraft.espiritismo.biblioteca;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Trilhas de aprendizagem (PROMPT_MESTRE §11 e jornadas do §9: discernimento mediúnico e caridade cotidiana).
 * <p>
 * Cada módulo é curto: situação jogável, explicação breve, fonte, exemplo, pergunta de reflexão (não é prova) e um
 * caminho de aprofundamento (obra do catálogo). Concluir módulos registra CONHECIMENTO, nunca moralidade (§7).
 */
public final class Trilhas {
	private Trilhas() {
	}

	public record Trilha(String id, String titulo, String descricao) {
	}

	public record Modulo(String id, String trilha, String titulo, String situacao, String explicacao, String fonte, String exemplo, String reflexao,
			String aprofundamento, Catalogo.Pergunta pergunta) {
	}

	public static final Map<String, Trilha> TRILHAS = new LinkedHashMap<>();
	public static final Map<String, Modulo> MODULOS = new LinkedHashMap<>();

	private static void trilha(String id, String titulo, String descricao) {
		TRILHAS.put(id, new Trilha(id, titulo, descricao));
	}

	/** opcoes[0..2]; correta = índice. */
	private static void m(String trilha, String id, String titulo, String situacao, String explicacao, String fonte, String exemplo, String reflexao,
			String aprofundamento, String enunciado, int correta, String explicacaoResposta, String... opcoes) {
		Catalogo.Pergunta p = enunciado == null ? null
				: new Catalogo.Pergunta("trilha:" + id, enunciado, List.of(opcoes), correta, explicacaoResposta, fonte);
		MODULOS.put(id, new Modulo(id, trilha, titulo, situacao, explicacao, fonte, exemplo, reflexao, aprofundamento, p));
	}

	public static List<Modulo> daTrilha(String trilha) {
		List<Modulo> l = new ArrayList<>();
		for (Modulo m : MODULOS.values())
			if (m.trilha().equals(trilha))
				l.add(m);
		return l;
	}

	static {
		trilha("fundamentos", "Fundamentos", "Os conceitos centrais da Doutrina Espírita, um de cada vez.");
		trilha("discernimento", "Discernimento mediúnico", "Percepção, interpretação e certeza: estudo, disciplina e responsabilidade.");
		trilha("caridade", "Caridade cotidiana", "Ajudar sem buscar recompensa, acolher sem humilhar, respeitar limites.");

		// ------------------------------------------------------------ Fundamentos (§11)
		m("fundamentos", "espirito_perispirito_corpo", "Espírito, perispírito e corpo",
				"Você acabou de chegar ao mundo e ainda está aprendendo quem é o seu personagem.",
				"Para a Doutrina Espírita, o ser humano tem três partes: o Espírito (o ser inteligente), o corpo físico e o perispírito, um envoltório "
						+ "semimaterial que liga os dois e acompanha o Espírito depois da morte.",
				"LE, q. 23, 93 a 95 e 135", "Quando o corpo do jogador \"morre\" no jogo, ele continua sendo o mesmo: memórias, escolhas e moralidade seguem com ele.",
				"Se o corpo muda a cada existência, o que permanece em você?", "o_livro_dos_espiritos",
				"O que liga o Espírito ao corpo durante a vida?", 2, "É o perispírito, envoltório semimaterial (LE, q. 135).", "O cérebro, apenas", "Nada liga os dois",
				"O perispírito");
		m("fundamentos", "reencarnacao", "Reencarnação e esquecimento",
				"Ao reencarnar no jogo, você volta sem lembrar exatamente do que viveu.",
				"O Espírito volta a novas vidas para aprender e reparar. O esquecimento do passado não é castigo: evita que antigas mágoas e vaidades atrapalhem o recomeço.",
				"LE, q. 166 e seguintes; q. 392 e seguintes (esquecimento do passado); ESE, cap. IV",
				"Dois antigos inimigos podem nascer na mesma família e aprender a se amar sem o peso das lembranças.",
				"Que vantagem você vê em recomeçar sem lembrar dos erros passados?", "o_livro_dos_espiritos",
				"Por que esquecemos as vidas passadas, segundo o Espiritismo?", 1, "O esquecimento protege e permite recomeçar (LE, q. 392 e seguintes).",
				"Por castigo", "Para recomeçar sem o peso de mágoas e vaidades", "Porque não existiram");
		m("fundamentos", "livre_arbitrio", "Livre-arbítrio e responsabilidade",
				"No Umbral, um socorrista oferece ajuda e você pode aceitar ou recusar.",
				"Cada pessoa escolhe os próprios atos e responde pelas consequências. Ninguém pode ser forçado ao bem; a ajuda é oferecida, não imposta.",
				"LE, q. 843 e seguintes", "No jogo, recusar socorro não gera punição: você pode reconsiderar depois.",
				"Por que uma ajuda imposta à força perderia o sentido?", "o_livro_dos_espiritos",
				"O que acompanha o livre-arbítrio?", 0, "A responsabilidade pelas escolhas (LE, q. 843 e seguintes).", "A responsabilidade", "A sorte", "Um castigo garantido");
		m("fundamentos", "causa_efeito", "Causa e efeito, sem punição simplista",
				"Sua moralidade caiu depois de uma má escolha. E agora?",
				"As ações têm consequências, mas a justiça divina não é vingança: ela educa. O arrependimento, a expiação e a reparação abrem o caminho de volta, "
						+ "e o sofrimento de alguém NÃO prova que ele é mau.",
				"CI, 1ª parte, cap. VII", "No jogo, ações boas e diversas recuperam a moralidade aos poucos; nada é perdido para sempre.",
				"Qual a diferença entre reparar um erro e apenas ser punido por ele?", "o_ceu_e_o_inferno",
				"Ver alguém sofrendo significa que ele é uma pessoa má?", 2, "Não. Sofrimento não é prova de inferioridade moral; não julgue (CI, cap. VII; ESE, cap. V).",
				"Sim, sempre", "Sim, se for pobre", "Não; o sofrimento tem muitas causas e não autoriza julgamento");
		m("fundamentos", "escala_espirita", "Progresso e escala espírita",
				"Você encontra Espíritos muito diferentes: alguns brincalhões, outros sábios, outros sofredores.",
				"Kardec classifica os Espíritos em ordens conforme o adiantamento moral e intelectual. É uma escala didática: todos são criados simples e "
						+ "ignorantes e todos progridem.",
				"LE, q. 100 a 113; q. 115", "O zombeteiro do jogo não é um monstro: é um Espírito leviano que ainda vai aprender.",
				"Se todos progridem, como você deveria tratar quem ainda está no começo?", "o_livro_dos_espiritos",
				"Um Espírito imperfeito ficará assim para sempre?", 1, "Não: todos progridem rumo à perfeição (LE, q. 115 e 116).", "Sim", "Não, todos progridem",
				"Depende da sorte");
		m("fundamentos", "pluralidade_mundos", "Pluralidade dos mundos habitados",
				"À noite, você olha o céu estrelado do jogo.",
				"O Espiritismo ensina que há muitos mundos habitados, em graus diferentes de adiantamento, e que a Terra é um deles, de provas e expiações.",
				"LE, q. 55; ESE, cap. III", "\"Há muitas moradas na casa de meu Pai\" (João 14:2) é comentado em ESE, cap. III.",
				"O que muda na sua visão quando você imagina o Universo cheio de vida?", "o_evangelho_segundo_o_espiritismo",
				"Segundo ESE cap. III, como a Terra é classificada?", 0, "Mundo de provas e expiações (ESE, cap. III).", "Mundo de provas e expiações", "O único mundo habitado",
				"Mundo perfeito");
		m("fundamentos", "mediunidade", "Mediunidade",
				"No Centro Espírita, um médium trabalha na reunião de quinta-feira.",
				"Médium é quem sente a influência dos Espíritos. A faculdade é natural, não é privilégio nem poder, e deve ser exercida com estudo, moral e de graça.",
				"LM, 2ª parte, cap. XIV, item 159; cap. XXVIII", "A mediunidade no jogo nunca é cobrada nem usada como arma.",
				"Por que a gratuidade protege a mediunidade?", "o_livro_dos_mediuns",
				"A mediunidade pode ser cobrada?", 1, "Não: \"Dai de graça o que de graça recebestes\" (Mt 10:8; ESE, cap. XXVI).", "Sim, por consulta", "Não, deve ser gratuita",
				"Só em casos graves");
		m("fundamentos", "obsessao", "Obsessão e desobsessão",
				"Você sente sussurros e cansaço quando sua moralidade está muito baixa.",
				"Obsessão é a influência persistente de um Espírito imperfeito. O caminho é a prece, a reforma íntima de quem sofre e o esclarecimento do "
						+ "Espírito, que também é alguém precisando de ajuda. A água fluidificada é apoio, não cura. Problemas de saúde pedem também cuidado médico.",
				"LM, cap. XXIII; ESE, cap. XXVIII", "No jogo, obsessores não são monstros a matar: a prece (tecla B) e o exemplo esclarecem.",
				"Por que tratar o obsessor com compaixão em vez de raiva?", "o_livro_dos_mediuns",
				"Qual destes NÃO é um meio espírita de lidar com a obsessão?", 2, "Exorcismo e amuletos não fazem parte do Espiritismo (LM, cap. XXIII).",
				"Prece sincera", "Melhora moral de quem sofre", "Exorcismo com amuletos");
		m("fundamentos", "prece", "Prece e pensamento",
				"Você aperta B para orar.",
				"A prece é um pensamento sincero dirigido a Deus. Vale pelo sentimento, não pelas palavras decoradas, pelo horário ou por objetos.",
				"ESE, cap. XXVII", "Uma prece curta e sincera vale mais que uma longa e distraída.", "O que você pediria, e o que agradeceria, numa prece?",
				"o_evangelho_segundo_o_espiritismo",
				"O que dá valor à prece?", 0, "O sentimento sincero (ESE, cap. XXVII).", "O sentimento sincero", "Velas e incenso", "O número de repetições");
		m("fundamentos", "caridade", "Caridade material e moral",
				"Um aldeão passa fome perto de você.",
				"Caridade é benevolência para com todos, indulgência para as imperfeições dos outros e perdão das ofensas. Inclui o pão e também a palavra, a "
						+ "paciência e o respeito.",
				"LE, q. 886; ESE, cap. XIII", "Dar comida ao faminto é caridade material; ouvi-lo sem humilhar é caridade moral.",
				"Qual caridade é mais difícil para você: a material ou a moral?", "o_evangelho_segundo_o_espiritismo",
				"Qual destes é caridade moral?", 1, "Indulgência e perdão são caridade moral (LE, q. 886).", "Doar uma cesta básica", "Perdoar uma ofensa",
				"Comprar comida para si");
		m("fundamentos", "perdao", "Perdão e reconciliação",
				"Outro jogador destruiu sua plantação sem querer (ou querendo).",
				"Jesus ensinou a perdoar setenta vezes sete vezes. Perdoar não é concordar com o erro: é não guardar ódio e abrir espaço para a reconciliação.",
				"ESE, cap. X", "Conversar com calma e propor ajudar a replantar juntos.", "Existe algo que você ainda precisa perdoar?",
				"o_evangelho_segundo_o_espiritismo",
				"Perdoar significa:", 2, "Perdoar é não guardar ódio, sem precisar aprovar o erro (ESE, cap. X).", "Fingir que nada aconteceu", "Aprovar o erro",
				"Não guardar ódio e abrir caminho para a reconciliação");
		m("fundamentos", "sono_emancipacao", "Sono e emancipação da alma",
				"Você dorme numa cama e tem um sonho estranho.",
				"Durante o sono, o Espírito fica parcialmente livre do corpo, sem romper o laço com ele. Nem todo sonho é mensagem: muitos são lembranças "
						+ "misturadas da própria mente.",
				"LE, q. 400 e seguintes", "No jogo, as visitas durante o sono são uma representação lúdica desse ensino.",
				"Como diferenciar um sonho comum de uma intuição que merece reflexão?", "o_livro_dos_espiritos",
				"O que acontece com o Espírito durante o sono, segundo o LE?", 0, "Fica parcialmente livre do corpo (LE, q. 400 e seguintes).",
				"Fica parcialmente livre, sem romper o laço", "Morre e volta", "Nada, só o corpo descansa");
		m("fundamentos", "desencarnacao", "Desencarnação e perturbação",
				"Seu personagem morreu e despertou no plano espiritual.",
				"A morte é a separação entre o Espírito e o corpo. Pode haver um período de perturbação, mais ou menos longo conforme o apego à matéria e a "
						+ "preparação moral. A recuperação não é instantânea.",
				"LE, q. 149 a 165; Nosso Lar (FEB, 1944)", "No jogo, despertar em Nosso Lar ou no Umbral depende das suas escolhas, e sempre há socorro possível.",
				"O que ajudaria alguém a se preparar para a vida depois da morte?", "o_ceu_e_o_inferno",
				"A perturbação depois da morte é igual para todos?", 1, "Não: varia conforme o apego e a preparação (LE, q. 163 a 165).", "Sim, sempre igual",
				"Não, varia conforme cada um", "Não existe perturbação");

		// ------------------------------------------------------------ Discernimento mediúnico (§9)
		m("discernimento", "percepcao_certeza", "Percepção, interpretação e certeza",
				"Você \"sentiu\" algo perto da mesa mediúnica.",
				"Uma coisa é perceber (sentir algo), outra é interpretar (dar sentido) e outra é ter certeza. O médium prudente separa as três e não "
						+ "transforma impressão em verdade absoluta.",
				"LM, cap. XX", "Sentir um arrepio não prova a presença de um Espírito específico.", "Quando você confundiu uma impressão com uma certeza?",
				"o_livro_dos_mediuns", "Sentir algo estranho prova a presença de um Espírito específico?", 1, "Não: percepção não é certeza (LM, cap. XX).", "Sim",
				"Não, é preciso examinar", "Só à noite");
		m("discernimento", "nem_toda_mensagem", "Nem toda mensagem deve ser seguida",
				"Uma comunicação manda você abandonar seus amigos para \"cumprir uma missão\".",
				"Espíritos podem enganar ou se enganar. Toda mensagem passa pelo crivo da razão: se pede orgulho, medo, isolamento ou dinheiro, desconfie.",
				"LM, cap. XX, item 230; cap. XXIV", "O Espírito Erasto: melhor rejeitar dez verdades do que aceitar uma única mentira (paráfrase, LM item 230).",
				"Que sinais indicariam que uma mensagem não merece confiança?", "o_livro_dos_mediuns",
				"Uma mensagem assinada com um nome famoso deve ser aceita sem exame?", 0, "Não: nomes podem ser usados indevidamente; julgue pelo conteúdo (LM, cap. XXIV).",
				"Não, julga-se pelo conteúdo", "Sim, o nome garante", "Sim, se for longa");
		m("discernimento", "estudo_disciplina", "Estudo, disciplina e equilíbrio",
				"Você quer desenvolver a mediunidade rapidamente.",
				"Kardec recomenda estudar antes de praticar, participar de reuniões sérias, com horário, propósito e dirigente experiente, e cuidar do equilíbrio pessoal.",
				"LM, cap. XXIX", "No jogo, a reunião mediúnica acontece no Centro, com dirigente e estudo, nunca sozinho por curiosidade.",
				"Por que a pressa é inimiga da boa prática mediúnica?", "o_livro_dos_mediuns",
				"O que Kardec recomenda antes da prática mediúnica?", 2, "Estudo e reuniões sérias (LM, cap. XXIX).", "Curiosidade", "Fazer sozinho em casa",
				"Estudar e participar de reuniões sérias");
		m("discernimento", "atividade_mediunica", "Uma atividade mediúnica com cuidado",
				"Você participa como observador de uma reunião mediúnica no Centro do jogo.",
				"Na reunião séria há prece, recolhimento, estudo, dirigente e análise posterior do que foi recebido. Nada de espetáculo, cobrança ou adivinhação.",
				"LM, cap. XXIX; cap. XXVIII", "Depois da reunião, o grupo conversa sobre o conteúdo e descarta o que não faz sentido.",
				"O que diferencia uma reunião séria de uma sessão de curiosidade?", "o_livro_dos_mediuns", null, 0, null);
		m("discernimento", "pedir_orientacao", "Procurar orientação quando perturbado",
				"Sua estabilidade está baixa e as mensagens parecem confusas.",
				"Quem está perturbado não deve conduzir trabalho mediúnico. O certo é parar, orar, procurar orientação no Centro e cuidar de si, inclusive "
						+ "com ajuda médica e psicológica quando necessário.",
				"LM, cap. XXIII; ESE, cap. V", "No jogo, um médium perturbado percebe com mais ruído: o mentor e o Centro ajudam a filtrar.",
				"Por que pedir ajuda é sinal de maturidade e não de fraqueza?", "o_livro_dos_mediuns",
				"Um médium perturbado deve:", 1, "Parar e buscar orientação (LM, cap. XXIII).", "Trabalhar mais para provar que está bem", "Parar e procurar orientação",
				"Esconder o que sente");

		// ------------------------------------------------------------ Caridade cotidiana (§9)
		m("caridade", "sem_recompensa", "Ajudar sem buscar recompensa",
				"Você poderia ajudar o aldeão faminto escondido, sem ninguém ver.",
				"O bem feito sem ostentação é o que tem mérito: que a mão esquerda não saiba o que faz a direita.", "ESE, cap. XIII; Mt 6:3",
				"No jogo, boas ações discretas são registradas em silêncio e reveladas só no plano espiritual.", "Você ajudaria se ninguém nunca soubesse?",
				"o_evangelho_segundo_o_espiritismo", "Segundo ESE cap. XIII, qual caridade tem mais mérito?", 0, "A discreta, sem ostentação (ESE, cap. XIII).",
				"A discreta", "A anunciada em público", "A que rende mais pontos");
		m("caridade", "acolher_sem_humilhar", "Acolher sem humilhar",
				"Um espírito sofredor chega confuso e mal-humorado ao Posto de Socorro.",
				"Ajudar inclui respeitar a dignidade de quem recebe: sem sermão, sem lembrar a todo momento o favor, sem tratar a pessoa como inferior.",
				"ESE, cap. XIII; LE, q. 886", "Oferecer água e escuta antes de qualquer conselho.", "Já se sentiu humilhado ao receber ajuda? Como teria sido melhor?",
				"o_evangelho_segundo_o_espiritismo", "Ao ajudar alguém, o que evitar?", 2, "Humilhar quem recebe (ESE, cap. XIII).", "Escutar", "Ter paciência",
				"Fazer sermão e lembrar o favor");
		m("caridade", "respeitar_limites", "Respeitar o limite de quem ajuda e de quem recebe",
				"Você está perturbado e quer socorrer outro paciente no Umbral.",
				"Quem está em sofrimento primeiro se cuida (\"paciente não ajuda paciente\" é regra do jogo). Quem recebe também tem o direito de recusar.",
				"Adaptação do mod; base: LE, q. 843 (livre-arbítrio)", "No jogo, missões de socorro só aparecem quando você está estável.",
				"Por que cuidar de si também é responsabilidade?", "o_livro_dos_espiritos", "Quem está perturbado deve assumir o socorro de outro paciente?", 1,
				"Não: primeiro se estabiliza (regra do jogo inspirada no bom senso).", "Sim, sempre", "Não, primeiro se estabiliza", "Só se for forte");
		m("caridade", "material_e_moral", "Ajuda material e moral se completam",
				"Você entrega uma sopa fraterna e fica para conversar.",
				"O pão alivia o corpo; a palavra amiga, o respeito e o exemplo alcançam a alma. Uma não substitui a outra.", "LE, q. 886; ESE, cap. XIII",
				"Distribuir agasalhos e ouvir as histórias de quem os recebe.", "Que tipo de ajuda você costuma esquecer?", "o_evangelho_segundo_o_espiritismo",
				null, 0, null);
	}
}
