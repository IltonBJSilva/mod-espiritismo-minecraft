package net.mcreator.modespiritismominecraft.espiritismo.npc;

import net.minecraft.world.entity.player.Player;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * Textos dos trabalhadores e Espíritos. Conteúdo fiel às obras básicas, quase sempre PARAFRASEADO
 * (sem aspas) e com a referência entre parênteses. Abreviações: LE = O Livro dos Espíritos,
 * LM = O Livro dos Médiuns, ESE = O Evangelho segundo o Espiritismo, AG = A Gênese.
 * Nenhuma frase é atribuída a pessoas reais.
 */
public final class Falas {
	private Falas() {
	}

	public static void falar(Player player, String nome, String fala) {
		falar(player, nome, fala, ChatFormatting.GOLD);
	}

	public static void falar(Player player, String nome, String fala, ChatFormatting corNome) {
		player.sendSystemMessage(Component.literal("<" + nome + "> ").withStyle(corNome).append(Component.literal(fala).withStyle(ChatFormatting.WHITE)));
	}

	public static void narrar(Player player, String texto) {
		player.sendSystemMessage(Component.literal(texto).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}

	public static String aleatorio(List<String> lista, RandomSource r) {
		return lista.get(r.nextInt(lista.size()));
	}

	// ---------------------------------------------------------------- Dirigente

	public static final List<String> DIRIGENTE = List.of(
			"Seja bem-vindo, irmão! Esta casa é de todos, e aqui nada se cobra: de graça recebemos, de graça damos (ESE, cap. XXVI).",
			"Temos o estudo das obras de Allan Kardec, começando por O Livro dos Espíritos. O estudo é a base de tudo.",
			"Nas palestras públicas, os oradores comentam O Evangelho segundo o Espiritismo. Qualquer pessoa pode assistir.",
			"O passe é uma transmissão de fluidos, feita com prece e boa vontade. Procure o nosso passista quando precisar.",
			"Na evangelização infantil, as crianças aprendem sobre Jesus e os valores do bem, com carinho e alegria.",
			"O atendimento fraterno é uma conversa reservada: ouvimos com respeito quem chega aflito e orientamos à luz do Evangelho.",
			"A assistência social leva alimento, agasalho e amizade a quem precisa. Fora da caridade não há salvação (ESE, cap. XV).",
			"Aqui não há rituais, imagens, velas, amuletos nem sacramentos. O Espiritismo é estudo, prece simples e prática do bem.");

	// ---------------------------------------------------------------- Médium trabalhador

	public static final List<String> MEDIUM = List.of(
			"Mediunidade não é adivinhação nem passatempo. É uma faculdade que pede estudo, disciplina e moral (LM, 2ª parte).",
			"Quem sente, em qualquer grau, a influência dos Espíritos é de certo modo médium; chamamos médiuns os que a têm de forma ostensiva (LM, cap. XIV).",
			"A mediunidade é gratuita. Quem cobra por ela a desvirtua: dai de graça o que de graça recebestes (ESE, cap. XXVI).",
			"Nem todo Espírito que se comunica é sério. Há Espíritos levianos e mistificadores; por isso examinamos tudo com a razão (LM, caps. XXIV e XXVII).",
			"Kardec ensinou o controle universal: um ensino só merece confiança quando é confirmado por muitos Espíritos, por médiuns diferentes e em lugares diferentes (ESE, Introdução, item II).",
			"O orgulho do médium atrai Espíritos levianos; a humildade e a boa conduta atraem os bons (LM, cap. XX).");

	/** Mensagens de consolo genéricas — de um Espírito amigo que NÃO se identifica (sem nomes famosos). */
	public static final List<String> CONSOLO = List.of(
			"Ninguém está esquecido por Deus. A dor de hoje passa; o bem que você faz permanece.",
			"Os que partiram continuam vivos e amando. A saudade é a prova de que o amor não morre.",
			"Não desanime diante dos próprios erros. Cada dia é uma nova chance de recomeçar.",
			"Quando tudo parecer escuro, ore e trabalhe no bem: a luz sempre volta.");

	// ---------------------------------------------------------------- Orador

	public record Palestra(String titulo, List<String> falas) {
	}

	public static final List<Palestra> PALESTRAS = List.of(
			new Palestra("O perdão das ofensas (ESE, cap. X)", List.of(
					"Jesus ensinou a perdoar não sete vezes, mas setenta vezes sete: isto é, sempre.",
					"Perdoar não é aprovar o mal; é não guardar ódio nem desejo de vingança no coração.",
					"Quem perdoa se liberta. O rancor pesa mais em quem o carrega do que em quem o causou.",
					"Reconcilia-te depressa com o teu adversário, aconselha o Evangelho. Comece hoje, por alguém próximo.")),
			new Palestra("A caridade discreta (ESE, cap. XIII)", List.of(
					"Não saiba a vossa mão esquerda o que dá a direita: a caridade verdadeira não busca aplauso.",
					"Ajudar com humildade, sem humilhar quem recebe, é a caridade moral que Jesus ensinou.",
					"Também a viúva que deu duas pequenas moedas deu muito, porque deu do que lhe faltava.",
					"Um sorriso, uma visita, uma palavra amiga: a caridade não se mede só por dinheiro.")),
			new Palestra("Bem-aventurados os aflitos (ESE, cap. V)", List.of(
					"As aflições têm causas: umas nesta vida, outras em existências anteriores. Nada acontece sem justiça.",
					"Mas a dor não é castigo eterno: é oportunidade de aprendizado e de progresso para o Espírito.",
					"Quem aceita a prova com coragem e sem revolta colhe paz; a revolta só aumenta o sofrimento.",
					"E nunca estamos sós: os bons Espíritos amparam quem pede ajuda com sinceridade.")),
			new Palestra("O homem de bem (ESE, cap. XVII)", List.of(
					"O homem de bem cumpre a lei de justiça, de amor e de caridade na sua maior pureza.",
					"Ele faz aos outros o que gostaria que lhe fizessem, e não o contrário.",
					"Reconhece-se o verdadeiro espírita pela sua transformação moral e pelo esforço em domar as más inclinações.",
					"Não se trata de ser perfeito de uma vez, mas de ser hoje um pouco melhor do que ontem.")),
			new Palestra("Pedi e obtereis: a prece (ESE, cap. XXVII)", List.of(
					"A prece é uma conversa com Deus. Vale pelo sentimento, não pelas palavras nem pela fórmula.",
					"Podemos orar pedindo, agradecendo e louvando; e também por outras pessoas e pelos Espíritos sofredores.",
					"A prece não muda as leis divinas, mas nos dá força, coragem e inspiração para atravessar as provas.",
					"Orar e agir: Deus ajuda a quem se ajuda. A prece sincera vem acompanhada do esforço no bem.")),
			new Palestra("Os brandos e pacíficos (ESE, cap. IX)", List.of(
					"Bem-aventurados os brandos, porque possuirão a Terra, ensinou Jesus no Sermão do Monte.",
					"A cólera e a impaciência fazem mal a quem as sente e a quem está por perto.",
					"A brandura não é fraqueza: é a força de quem domina a si mesmo.",
					"Antes de responder com raiva, respire, ore e lembre-se do exemplo de Jesus.")));

	public static final List<String> ORADOR = List.of(
			"Estude O Evangelho segundo o Espiritismo: cada capítulo é um convite à reforma íntima.",
			"Volte daqui a pouco para outra palestra. Enquanto isso, procure viver um dos ensinos de hoje.",
			"O Evangelho não é só para ser lido: é para ser vivido no dia a dia.");

	// ---------------------------------------------------------------- Passista

	public static final List<String> PASSISTA = List.of(
			"O passe é uma transmissão de fluidos: o passista, em prece, doa suas energias, e os bons Espíritos auxiliam (AG, cap. XIV; LM, cap. XIV).",
			"No passe não há toque, ritual, fórmula nem cobrança. Basta receber com fé e boa vontade.",
			"O passe não substitui o médico: cuide também da saúde do corpo.",
			"Depois do passe, procure manter bons pensamentos: é assim que os fluidos benéficos permanecem.");

	// ---------------------------------------------------------------- Evangelizadora

	public static final List<String> EVANGELIZADORA = List.of(
			"Olá, querido! Sabe quem é o maior exemplo para nós? Jesus! Os Espíritos disseram a Kardec que Ele é o guia e modelo da humanidade (LE, q. 625).",
			"Jesus ensinou a amar a Deus sobre todas as coisas e ao próximo como a nós mesmos (ESE, cap. XI).",
			"Fazer aos outros o que gostaríamos que fizessem conosco: essa é a regra de ouro (ESE, cap. XI).",
			"Pedir desculpas e perdoar deixam o coração levinho, sabia? (ESE, cap. X)",
			"Agradecer, respeitar os mais velhos, cuidar dos animais e da natureza: tudo isso é amor em ação.",
			"Deus é a inteligência suprema, causa primeira de todas as coisas (LE, q. 1). Tudo o que é bom vem d'Ele!");

	// ---------------------------------------------------------------- Divaldo Franco (homenagem)

	/** Apresentação com fatos verificados (ver INTEGRACAO.md, fontes). Dita pelo narrador, não pelo personagem. */
	public static final String DIVALDO_APRESENTACAO = "Este personagem homenageia Divaldo Pereira Franco (Feira de Santana-BA, 5/5/1927 — Salvador-BA, 13/5/2025), "
			+ "médium e orador espírita. Em Salvador fundou o Centro Espírita Caminho da Redenção (1947) e, com Nilson de Souza Pereira, "
			+ "a Mansão do Caminho (1952), obra de assistência a crianças e famílias. As falas dele no jogo são ensinos das obras básicas, não citações suas.";

	public static final List<String> DIVALDO = List.of(
			"Caridade, como Jesus a entendia, é benevolência para com todos, indulgência para as imperfeições dos outros e perdão das ofensas (LE, q. 886).",
			"O verdadeiro espírita se reconhece pela transformação moral e pelo esforço em vencer as más inclinações (ESE, cap. XVII, item 4).",
			"Espíritas, amai-vos: este o primeiro ensino; instruí-vos: este o segundo (ESE, cap. VI, item 5 — O Espírito de Verdade).",
			"O trabalho é lei da natureza; por ele o ser humano progride e serve aos outros (LE, q. 674).",
			"Fora da caridade não há salvação: amparar uma criança, uma família, um idoso, é viver o Evangelho (ESE, cap. XV).",
			"As aflições são oportunidades de progresso; com fé e trabalho no bem, nenhuma dor é inútil (ESE, cap. V).");

	// ---------------------------------------------------------------- Espíritos

	public static final List<String> MENTOR = List.of(
			"Não estás só. Cada um tem um Espírito protetor que o acompanha e o inspira para o bem (LE, q. 489–521).",
			"Coragem. A prova que atravessas é passageira; ora e persevera no bem.",
			"Não decido por ti: respeito teu livre-arbítrio. Mas ouve a voz da consciência, que te aconselha o bem (LE, q. 489–521).",
			"A noite não precisa ser solitária. Eleva o pensamento a Deus e descansa em paz.",
			"Estudar é acender uma luz no caminho. Continua lendo e, sobretudo, praticando o que aprendes.");

	public static final List<String> MENTOR_DESPEDIDA = List.of(
			"Sigo contigo, ainda que não me vejas. Vai em paz.",
			"Que a paz de Jesus te acompanhe. Estarei por perto.");

	public static final List<String> OBSESSOR_SUSSURROS = List.of(
			"Ninguém se importa com você...",
			"Para que ajudar os outros? Eles não merecem.",
			"Desista. Você nunca vai mudar.",
			"Vingue-se de quem o ofendeu...",
			"Fique com raiva. Você tem razão em ficar com raiva...");

	public static final List<String> OBSESSOR_CONVERSA = List.of(
			"Me deixe em paz! Você é igual a mim...",
			"Por que eu iria embora? Seus pensamentos me agradam.",
			"Você não me engana com essa cara de bonzinho.");

	public static final String DICA_OBSESSOR = "Obsessores não se vencem pela força: a prece sincera (tecla B), a mudança dos pensamentos e o bem praticado "
			+ "afastam a influência e ajudam o próprio Espírito a se esclarecer (LM, cap. XXIII).";

	public static final List<String> SOFREDOR = List.of(
			"Onde estou? Está tudo tão escuro...",
			"Sinto frio... ninguém me ouve...",
			"Ajude-me, por favor... não sei o que aconteceu comigo.",
			"Estou tão cansado...");

	public static final String DICA_SOFREDOR = "Uma prece sincera (tecla B) pode consolar este Espírito e atrair o socorro dos bons Espíritos (ESE, cap. XXVII).";

	public static final List<String> SOCORRISTA_RESGATE = List.of(
			"Viemos buscá-lo, irmão. Agora você será tratado com carinho.",
			"Obrigado pela sua prece: ela é um auxílio real para os que sofrem (ESE, cap. XXVII).");

	public static final List<String> SOCORRISTA = List.of(
			"Trabalhamos no resgate dos que sofrem, sem descanso e sem cobrança. Toda boa vontade é bem-vinda.",
			"Muitos Espíritos, ao desencarnar, ficam perturbados e não percebem que deixaram o corpo. Nós os amparamos.",
			"Ore pelos que sofrem: cada prece sincera nos ajuda no trabalho de socorro.");

	public static final List<String> ZOMBETEIRO_DICAS = List.of(
			"Psiu! Tem um tesouro enterrado logo ali, a dez passos! Pode cavar!",
			"Os diamantes estão bem embaixo de você. Confie em mim!",
			"Se pular três vezes, a chuva para. Juro!",
			"Eu sou um Espírito muito importante, sabia? Fui rei, general e sábio, tudo ao mesmo tempo!",
			"Aquele aldeão ali está tramando contra você. Hihihi!");

	public static final String DICA_ZOMBETEIRO = "Espíritos levianos gostam de pregar peças e de dar falsas notícias (LE, q. 100–103). "
			+ "Não lhes dê crédito nem atenção: avalie tudo com a razão. Uma prece também os afasta.";
}
