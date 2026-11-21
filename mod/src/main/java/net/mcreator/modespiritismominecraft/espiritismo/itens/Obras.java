package net.mcreator.modespiritismominecraft.espiritismo.itens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * As obras legíveis do mod: conteúdo introdutório (resumo + algumas questões/trechos com referência).
 * <p>
 * Regra de fidelidade: só fica entre aspas o que foi conferido (ex.: LE q. 1, 621, 625, 886; ESE cap. XV, cap. VI
 * item 5, cap. XVII item 4). O resto é PARÁFRASE, sem aspas, sempre com a referência.
 * <p>
 * Cada obra é um item próprio ({@link ObraItem}) que já nasce com o componente WRITTEN_BOOK_CONTENT e abre a tela de
 * livro vanilla; também pode ser colocada num atril (tag minecraft:lectern_books).
 */
public final class Obras {
	private Obras() {
	}

	/** Uma seção vira uma ou mais páginas. */
	public record Secao(String titulo, String texto) {
	}

	public record Obra(String id, String autor, List<Secao> secoes) {
	}

	private static Secao s(String titulo, String texto) {
		return new Secao(titulo, texto);
	}

	public static final Map<String, Obra> OBRAS = new LinkedHashMap<>();

	private static void obra(String id, String autor, Secao... secoes) {
		OBRAS.put(id, new Obra(id, autor, List.of(secoes)));
	}

	static {
		obra("o_livro_dos_espiritos", "Allan Kardec",
				s("O Livro dos Espíritos", "Allan Kardec. Paris, 18 de abril de 1857.\n\nPrincípios da Doutrina Espírita sobre a imortalidade da alma, a natureza dos Espíritos e suas relações com os homens, as leis morais, a vida presente, a vida futura e o porvir da Humanidade."),
				s("Como nasceu", "O professor Hippolyte Léon Denizard Rivail, de Lyon, adotou o nome Allan Kardec. Ele fez perguntas aos Espíritos por meio de vários médiuns, em lugares diferentes, comparou as respostas e as passou pelo crivo da razão antes de publicá-las."),
				s("1857 e 1860", "A 1ª edição (1857) tinha 501 questões, em duas colunas. A 2ª edição, revista e ampliada (1860), chegou a 1.019 questões e é a base das edições atuais."),
				s("As quatro partes", "1. Das causas primárias (Deus, o Universo).\n2. Do mundo espírita ou dos Espíritos.\n3. Das leis morais.\n4. Das esperanças e consolações."),
				s("Questão 1", "Que é Deus?\n\n\"Deus é a inteligência suprema, causa primária de todas as coisas.\"\n\n(LE, q. 1)"),
				s("Questão 621", "Onde está escrita a lei de Deus?\n\n\"Na consciência.\"\n\n(LE, q. 621)"),
				s("Questão 625", "Qual o tipo mais perfeito que Deus tem oferecido ao homem, para lhe servir de guia e modelo?\n\n\"Jesus.\"\n\n(LE, q. 625)"),
				s("Questão 886", "Qual o verdadeiro sentido da palavra caridade, como a entendia Jesus?\n\n\"Benevolência para com todos, indulgência para as imperfeições dos outros, perdão das ofensas.\"\n\n(LE, q. 886)"),
				s("Reencarnação", "Paráfrase: a alma que ainda não alcançou a perfeição continua a se depurar passando por novas existências corporais. Cada vida é uma oportunidade de aprender e de reparar erros. (LE, q. 166 e seguintes)"),
				s("Conhece-te a ti mesmo", "Paráfrase: Santo Agostinho aconselha, ao fim de cada dia, rever as próprias ações e perguntar se fez mal a alguém ou deixou de fazer o bem que podia. É o meio prático de melhorar-se. (LE, q. 919-a)"),
				s("Para o jogador", "Estude, reflita e pratique: o Espiritismo é fé raciocinada. Converse com Allan Kardec no Centro e leia as outras obras básicas."));

		obra("o_livro_dos_mediuns", "Allan Kardec",
				s("O Livro dos Médiuns", "ou Guia dos Médiuns e dos Evocadores. Allan Kardec. Paris, janeiro de 1861.\n\nContinuação de O Livro dos Espíritos: trata da parte experimental, isto é, das manifestações dos Espíritos e da mediunidade."),
				s("Duas partes", "1ª parte: Noções preliminares (existem os Espíritos? o maravilhoso, o método).\n2ª parte: Das manifestações espíritas (efeitos físicos e inteligentes, médiuns, obsessão, reuniões)."),
				s("Quem é médium?", "Paráfrase: toda pessoa que sente, em algum grau, a influência dos Espíritos é médium; a palavra se aplica sobretudo a quem tem essa faculdade bem definida. Não é privilégio nem dom sobrenatural. (LM, 2ª parte, cap. XIV, item 159)"),
				s("Psicografia", "Os médiuns escreventes ou psicógrafos transmitem comunicações pela escrita. Kardec distingue médiuns mecânicos, intuitivos e semimecânicos, conforme a parte consciente que o médium tem no que escreve. (LM, cap. XV)"),
				s("O crivo da razão", "Paráfrase: toda comunicação deve ser examinada com bom senso. O Espírito Erasto ensina que é melhor rejeitar dez verdades do que aceitar uma única mentira, uma única teoria falsa. (LM, cap. XX, item 230)"),
				s("Obsessão", "É o domínio que alguns Espíritos exercem sobre certas pessoas. Tem graus: obsessão simples, fascinação e subjugação. (LM, cap. XXIII)\n\nO remédio não é exorcismo: é a prece, a melhora moral de quem sofre e o esclarecimento do Espírito obsessor, que também precisa de ajuda."),
				s("Mediunidade gratuita", "Paráfrase: a mediunidade não deve ser explorada por dinheiro. \"Dai de graça o que de graça recebestes\" (Mateus 10:8; ver O Evangelho segundo o Espiritismo, cap. XXVI e LM, cap. XXVIII)."),
				s("Reuniões sérias", "Paráfrase: as reuniões proveitosas são as de estudo, com recolhimento, propósito sério, união de pensamentos e direção cuidadosa; curiosidade e frivolidade atraem Espíritos levianos. (LM, cap. XXIX)"),
				s("Água magnetizada", "Paráfrase: a vontade, como já mostrava o magnetismo, pode modificar as propriedades da água. É daí que vem a água fluidificada: ela recebe os fluidos de quem ora ou aplica o passe. (LM, 2ª parte, cap. VIII, item 131)"));

		obra("o_evangelho_segundo_o_espiritismo", "Allan Kardec",
				s("O Evangelho segundo o Espiritismo", "Allan Kardec. Paris, abril de 1864 (a 1ª edição se chamava Imitação do Evangelho segundo o Espiritismo).\n\nExplica as máximas morais de Jesus e sua aplicação às situações da vida."),
				s("A parte moral", "Paráfrase: Kardec separou dos Evangelhos o ensino moral, que todos podem aceitar e que não provoca disputas, e o comentou à luz dos ensinos dos Espíritos. (ESE, Introdução)"),
				s("Caridade", "\"Fora da caridade não há salvação.\"\n\n(ESE, cap. XV)\n\nNão diz \"fora de tal religião\": a caridade une todos os homens de bem."),
				s("Amai-vos", "\"Espíritas! amai-vos, este o primeiro ensinamento; instruí-vos, este o segundo.\"\n\n(O Espírito de Verdade, ESE, cap. VI, item 5)"),
				s("O verdadeiro espírita", "\"Reconhece-se o verdadeiro espírita pela sua transformação moral e pelos esforços que emprega para domar suas inclinações más.\"\n\n(ESE, cap. XVII, item 4)"),
				s("Fé raciocinada", "Paráfrase: a fé inabalável é aquela que pode olhar a razão de frente, em todas as épocas da Humanidade. (ESE, cap. XIX, item 7)"),
				s("A prece", "Paráfrase: a prece vale pelo pensamento e pelo sentimento, não pelas palavras nem por fórmulas. Não há rituais: basta o coração sincero. (ESE, cap. XXVII; preces no cap. XXVIII)"),
				s("Perdão", "Paráfrase: perdoar as ofensas, não uma vez, mas setenta vezes sete vezes, como ensinou Jesus; quem não perdoa não pode esperar perdão. (ESE, cap. X)"),
				s("Beneficência discreta", "\"Que a vossa mão esquerda não saiba o que faz a vossa mão direita\" (Mateus 6:3).\n\nParáfrase: o bem feito sem ostentação é o que tem mérito. (ESE, cap. XIII)"));

		obra("o_ceu_e_o_inferno", "Allan Kardec",
				s("O Céu e o Inferno", "ou A Justiça Divina segundo o Espiritismo. Allan Kardec. Paris, 1865.\n\nExamina as ideias sobre a vida futura: céu, inferno, purgatório, anjos, demônios e penas."),
				s("1ª parte: Doutrina", "Paráfrase: Kardec analisa o temor da morte, o céu, o inferno, o purgatório e a doutrina das penas eternas, mostrando-as à luz da razão e da bondade de Deus."),
				s("Não há penas eternas", "Paráfrase: o sofrimento do Espírito dura enquanto dura o mal que o causou. Arrependimento, expiação e reparação abrem sempre o caminho de volta ao bem. (CI, 1ª parte, cap. VII, Código penal da vida futura)"),
				s("Demônios?", "Paráfrase: não existem seres criados para o mal eterno. Os chamados demônios são Espíritos imperfeitos, que ainda se melhorarão. Por isso o espírita ora por eles em vez de temê-los. (CI, 1ª parte, cap. IX)"),
				s("2ª parte: Exemplos", "Comunicações de Espíritos depois da morte: felizes, em condição mediana, sofredores, suicidas, criminosos arrependidos, endurecidos, e expiações terrestres, cada uma com análise de Kardec."),
				s("A lição", "Paráfrase: cada um constrói a própria situação futura pelos seus atos. A justiça divina não é vingança: é educação, e a misericórdia de Deus nunca fecha a porta."));

		obra("a_genese", "Allan Kardec",
				s("A Gênese", "Os Milagres e as Predições segundo o Espiritismo. Allan Kardec. Paris, janeiro de 1868.\n\nÚltima das cinco obras básicas."),
				s("Três partes", "1. A Gênese: Deus, a criação, a formação da Terra e dos seres vivos, à luz da ciência da época.\n2. Os Milagres.\n3. As Predições."),
				s("Ciência e Espiritismo", "Paráfrase: o Espiritismo caminha com o progresso; se a ciência demonstrar que ele está errado num ponto, ele se corrigirá nesse ponto. Por isso não será ultrapassado. (Gênese, cap. I, item 55)"),
				s("Deus", "Paráfrase: não há efeito sem causa. Diante de uma obra inteligente, procuramos o autor; a ordem do Universo revela uma inteligência suprema. (Gênese, cap. II)"),
				s("Os fluidos", "Paráfrase: o fluido cósmico universal é a matéria elementar de tudo. A ação dos Espíritos sobre os fluidos explica os passes e as curas sem nada de sobrenatural. (Gênese, cap. XIV)"),
				s("Milagres", "Paráfrase: Deus não derroga as próprias leis. Fatos tidos como milagres têm explicação em leis naturais ainda pouco conhecidas, como as que regem os fluidos e o perispírito. (Gênese, cap. XIII a XV)"),
				s("O Espírito progride", "Paráfrase: os Espíritos são criados simples e ignorantes e progridem pelo próprio esforço, através das encarnações, rumo à perfeição. (LE, q. 115; Gênese, cap. XI)"));

		obra("nosso_lar", "André Luiz / Chico Xavier",
				s("Nosso Lar", "Pelo Espírito André Luiz, psicografia de Francisco Cândido Xavier. Federação Espírita Brasileira (FEB), 1944.\n\nPrimeiro livro da série A Vida no Mundo Espiritual."),
				s("O médico", "André Luiz foi médico no Rio de Janeiro. Ao desencarnar, não encontrou céu nem inferno: passou um longo período sofrendo no Umbral, região de Espíritos presos às próprias faltas e ao egoísmo."),
				s("O socorro", "Quando enfim pediu ajuda a Deus com humildade, foi socorrido pelo benfeitor Clarêncio e levado à colônia espiritual Nosso Lar, onde foi tratado e começou a aprender."),
				s("A colônia", "Nosso Lar é uma cidade espiritual administrada por um Governador e organizada em seis Ministérios: Regeneração, Auxílio, Comunicação, Esclarecimento, Elevação e União Divina."),
				s("Trabalho", "Ninguém vive ocioso em Nosso Lar. O trabalho é contado em bônus-hora. André aprende com Lísias, com a enfermeira Narcisa e outros a servir no bem, inclusive socorrendo quem está no Umbral."),
				s("Lições do livro", "A vida continua depois da morte; colhemos o que plantamos; o trabalho, a humildade e a prece abrem os caminhos do progresso. No jogo, a dimensão Nosso Lar é inspirada nesta obra."));

		obra("o_que_e_o_espiritismo", "Allan Kardec",
				s("O que é o Espiritismo", "Allan Kardec. Paris, 1859.\n\nIntrodução ao conhecimento do mundo invisível, para quem nunca estudou o assunto."),
				s("Diálogos", "Paráfrase: o primeiro capítulo é uma pequena conferência em forma de diálogos com um crítico, um cético e um padre, respondendo às objeções mais comuns."),
				s("Ciência e filosofia", "Paráfrase: o Espiritismo é ao mesmo tempo uma ciência de observação, que estuda as relações com os Espíritos, e uma doutrina filosófica, com todas as consequências morais dessas relações. (Preâmbulo)"),
				s("Sem rituais", "O Espiritismo não tem sacerdotes, sacramentos, velas, amuletos nem cobrança: reúne-se para estudar, orar e praticar a caridade."));

		obra("revista_espirita", "Allan Kardec",
				s("Revista Espírita", "Jornal de Estudos Psicológicos. Fundada e dirigida por Allan Kardec. Primeiro número: 1º de janeiro de 1858, Paris. Publicação mensal."),
				s("Doze volumes", "Kardec dirigiu a Revista de 1858 até desencarnar, em 31 de março de 1869: são doze volumes, um por ano, com fatos, comunicações, cartas de leitores e estudos."),
				s("A Sociedade", "Em 1º de abril de 1858, Kardec fundou em Paris a Sociedade Parisiense de Estudos Espíritas. Muitas comunicações estudadas nas reuniões dela foram publicadas na Revista."),
				s("Laboratório", "Paráfrase: a Revista funcionava como laboratório da Doutrina. Kardec comparava comunicações recebidas em lugares diferentes, por médiuns que não se conheciam (controle universal do ensino dos Espíritos)."),
				s("Curiosidade", "Kardec, nascido em Lyon em 3 de outubro de 1804, foi educador, discípulo de Pestalozzi e autor de livros de gramática e aritmética antes de se dedicar ao Espiritismo."),
				s("Curiosidade", "Em 1862 Kardec visitou grupos espíritas em várias cidades da França, como Lyon e Bordeaux, e publicou o relato em Viagem Espírita em 1862."));
	}

	// ---------------------------------------------------------------- Montagem das páginas

	/** Caracteres por página (14 linhas de ~20 caracteres, com folga). */
	private static final int LIMITE = 230;
	private static final int LIMITE_COM_TITULO = 170;

	public static List<Filterable<Component>> paginas(Obra obra) {
		List<Filterable<Component>> paginas = new ArrayList<>();
		for (Secao secao : obra.secoes()) {
			List<String> partes = quebrar(secao.texto());
			for (int i = 0; i < partes.size(); i++) {
				MutableComponent pagina = Component.empty();
				if (i == 0)
					pagina.append(Component.literal(secao.titulo()).withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD)).append("\n\n");
				pagina.append(Component.literal(partes.get(i)).withStyle(ChatFormatting.BLACK));
				paginas.add(Filterable.passThrough(pagina));
			}
		}
		return paginas;
	}

	/** Quebra o texto em pedaços por palavras; o primeiro pedaço é menor (divide a página com o título). */
	static List<String> quebrar(String texto) {
		List<String> partes = new ArrayList<>();
		StringBuilder atual = new StringBuilder();
		int limite = LIMITE_COM_TITULO;
		for (String palavra : texto.split(" ")) {
			if (atual.length() > 0 && atual.length() + 1 + palavra.length() > limite) {
				partes.add(atual.toString());
				atual.setLength(0);
				limite = LIMITE;
			}
			if (atual.length() > 0)
				atual.append(' ');
			atual.append(palavra);
		}
		if (atual.length() > 0)
			partes.add(atual.toString());
		return partes;
	}

	/** Conteúdo de livro escrito da obra (título vazio: o nome vem da tradução do item). */
	public static WrittenBookContent conteudo(String id) {
		Obra obra = OBRAS.get(id);
		if (obra == null)
			throw new IllegalArgumentException("obra desconhecida: " + id);
		return new WrittenBookContent(Filterable.passThrough(""), obra.autor(), 0, paginas(obra), true);
	}

	/**
	 * Cria a pilha do item da obra (já com as páginas). IDs: o_livro_dos_espiritos, o_livro_dos_mediuns,
	 * o_evangelho_segundo_o_espiritismo, o_ceu_e_o_inferno, a_genese, nosso_lar, o_que_e_o_espiritismo,
	 * revista_espirita. Retorna ItemStack.EMPTY para ID desconhecido.
	 */
	public static ItemStack criar(String id) {
		Supplier<? extends net.minecraft.world.item.Item> item = RegistroItens.OBRAS.get(id);
		return item == null ? ItemStack.EMPTY : new ItemStack(item.get());
	}
}
