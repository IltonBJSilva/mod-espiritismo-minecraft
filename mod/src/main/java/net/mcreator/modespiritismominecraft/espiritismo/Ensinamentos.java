package net.mcreator.modespiritismominecraft.espiritismo;

import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Textos usados pelo Livro dos Espíritos e pelos NPCs.
 * Para adicionar conteúdo, basta acrescentar entradas nas listas.
 */
public final class Ensinamentos {
	/** Perguntas e respostas de "O Livro dos Espíritos" (Allan Kardec, 1857). */
	public record Questao(int numero, String pergunta, String resposta) {
	}

	public static final List<Questao> LIVRO_DOS_ESPIRITOS = List.of(
			new Questao(1, "Que é Deus?", "Deus é a inteligência suprema, causa primária de todas as coisas."),
			new Questao(76, "Que definição se pode dar dos Espíritos?", "Pode-se dizer que os Espíritos são os seres inteligentes da criação."),
			new Questao(132, "Qual o objetivo da encarnação dos Espíritos?", "Deus lhes impõe a encarnação com o fim de fazê-los chegar à perfeição."),
			new Questao(621, "Onde está escrita a lei de Deus?", "Na consciência."),
			new Questao(625, "Qual o tipo mais perfeito que Deus tem oferecido ao homem, para lhe servir de guia e modelo?", "Jesus."),
			new Questao(642, "Bastará não fazer o mal para agradar a Deus?", "Não; é preciso fazer o bem no limite das próprias forças."),
			new Questao(886, "Qual o verdadeiro sentido da palavra caridade, como a entendia Jesus?", "Benevolência para com todos, indulgência para as imperfeições dos outros, perdão das ofensas."),
			new Questao(919, "Qual o meio prático mais eficaz para se melhorar nesta vida e resistir à atração do mal?", "Um sábio da Antiguidade vos disse: conhece-te a ti mesmo."));

	/** Falas de Allan Kardec — o guia que ensina os fundamentos (doc: NPCs > Allan Kardec). */
	public static final List<String> KARDEC = List.of(
			"Fora da caridade não há salvação.",
			"Nascer, morrer, renascer ainda e progredir sempre, tal é a lei.",
			"Estude O Livro dos Espíritos: cada leitura ilumina um pouco mais o caminho.",
			"Suas ações moldam sua moralidade. Ajude os aldeões, cuide dos animais, plante árvores.",
			"Tirar vidas inocentes pesa sobre o Espírito. Lembre-se disso ao empunhar a espada.",
			"Se sentir influências perturbadoras, procure Chico Xavier e a água fluidificada.");

	/**
	 * Falas de Chico Xavier no jogo. São orientações escritas para o jogo, NÃO citações reais —
	 * evite atribuir frases sem fonte a pessoas reais.
	 */
	public static final List<String> CHICO = List.of(
			"Os amigos espirituais pedem que você não desanime. Siga fazendo o bem.",
			"Quando a moralidade está baixa, espíritos imperfeitos encontram as portas abertas. A água fluidificada ajuda.",
			"Se escolheu ser médium, use a Psicografia com humildade: nem todo espírito que se comunica é sério.",
			"Há aldeões precisando de ajuda. Negociar com honestidade também é caridade.",
			"Descanse quando a energia espiritual acabar. Ela volta mais rápido para quem vive no bem.");

	/** Falas de Haroldo Dutra — leis morais (O Livro dos Espíritos, Parte Terceira). */
	public static final List<String> HAROLDO = List.of(
			"O Livro dos Espíritos apresenta dez leis morais: adoração, trabalho, reprodução, conservação, destruição, sociedade, progresso, igualdade, liberdade e, acima de todas, justiça, amor e caridade.",
			"Lei do Trabalho: o trabalho é lei da natureza e meio de progresso.",
			"Lei de Destruição: destruir além da necessidade é abuso. Caçar para se alimentar não é o mesmo que matar por prazer.",
			"Lei de Sociedade: ninguém foi feito para viver isolado. Os aldeões também são seus irmãos de caminhada.",
			"Lei de Progresso: todo Espírito progride; ninguém está condenado a permanecer no erro.",
			"Lei de Justiça, Amor e Caridade: fazer aos outros o que gostaríamos que nos fizessem.");

	/**
	 * Palestras de Haroldo Dutra sobre as leis morais (O Livro dos Espíritos, Parte Terceira).
	 * O conteúdo é a Doutrina codificada por Kardec, em paráfrase — não são falas reais do palestrante.
	 */
	public record Palestra(String titulo, List<String> falas) {
	}

	public static final List<Palestra> PALESTRAS = List.of(
			new Palestra("Lei do Trabalho", List.of(
					"Vamos refletir hoje sobre a Lei do Trabalho, estudada no capítulo III da terceira parte de O Livro dos Espíritos.",
					"Para os Espíritos, o trabalho é lei da natureza e, por isso mesmo, uma necessidade.",
					"E todo trabalho útil conta: não só o das mãos, mas também o da inteligência.",
					"O repouso também faz parte da lei: serve para recuperar as forças.",
					"Plante, construa, ajude a vila. O trabalho honesto é caminho de progresso.")),
			new Palestra("Lei de Destruição", List.of(
					"A natureza se renova o tempo todo; por isso existe uma destruição necessária.",
					"Mas a Doutrina ensina que destruir além da necessidade é abuso.",
					"Caçar para se alimentar não é o mesmo que matar por prazer.",
					"Antes de empunhar a espada, pergunte a si mesmo: isto é mesmo necessário?")),
			new Palestra("Lei de Sociedade", List.of(
					"Os Espíritos ensinam que o ser humano foi feito para viver em sociedade.",
					"Ninguém tem todas as faculdades: precisamos uns dos outros para progredir.",
					"O isolamento absoluto é contrário à lei natural.",
					"Os aldeões, os animais, os outros jogadores: todos são companheiros de jornada.")),
			new Palestra("Lei do Progresso", List.of(
					"O progresso é lei da natureza: todos os seres caminham, ainda que devagar.",
					"Nenhum Espírito está condenado a permanecer no erro para sempre.",
					"O erro de hoje pode ser a lição de amanhã.",
					"Por isso a sua moralidade sempre pode subir. Recomece pelo bem mais próximo.")),
			new Palestra("Lei de Liberdade", List.of(
					"Kardec perguntou aos Espíritos se o homem tem o livre-arbítrio de seus atos.",
					"A resposta foi que sim: sem o livre-arbítrio, o homem seria uma máquina.",
					"Liberdade traz responsabilidade: cada escolha tem consequência.",
					"Ninguém o obriga a ser bom ou mau. A escolha é sua, e a moralidade registra o caminho.")),
			new Palestra("Lei de Justiça, Amor e Caridade", List.of(
					"Esta é a lei que resume todas as outras.",
					"A justiça consiste em cada um respeitar os direitos dos demais.",
					"E a caridade, como Jesus a entendia, é benevolência para com todos, indulgência para as imperfeições dos outros e perdão das ofensas.",
					"Fazer aos outros o que gostaríamos que nos fizessem: esse é o caminho.",
					"Fora da caridade não há salvação.")));

	private Ensinamentos() {
	}

	public static <T> T aleatorio(List<T> lista, RandomSource random) {
		return lista.get(random.nextInt(lista.size()));
	}
}
