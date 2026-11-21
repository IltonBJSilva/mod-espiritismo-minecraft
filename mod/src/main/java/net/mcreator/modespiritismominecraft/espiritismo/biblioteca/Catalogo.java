package net.mcreator.modespiritismominecraft.espiritismo.biblioteca;

import net.mcreator.modespiritismominecraft.espiritismo.itens.Obras;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catálogo da Biblioteca Espírita: identificação bibliográfica, contexto, status de direitos, seções legíveis,
 * perguntas de reflexão e quizzes opcionais.
 * <p>
 * As obras que já existem como item ({@link Obras}) NÃO são duplicadas: as seções delas vêm direto de
 * {@code Obras.OBRAS} (classificadas como trecho citado ou resumo) e aqui só se acrescentam ficha, contexto e extras.
 * <p>
 * Regra de direitos (ver docs/fontes_e_licencas.md): nenhum texto integral de tradução protegida; só trechos curtos
 * com referência (Lei 9.610/1998, art. 46, III), resumos e explicações originais do mod. Trechos no original francês de
 * Kardec (falecido em 1869) são de domínio público; a tradução desses trechos para o português é do próprio mod.
 * <p>
 * Classe compartilhada por cliente e servidor (só dados, sem acesso ao mundo).
 */
public final class Catalogo {
	private Catalogo() {
	}

	// ================================================================ Tipos

	/** Situação de direitos autorais da obra (no Brasil, Lei 9.610/1998). */
	public enum Direitos {
		ORIGINAL_DOMINIO_PUBLICO("Original em domínio público",
				"O autor faleceu há mais de 70 anos (Lei 9.610/1998, art. 41): o texto original é de domínio público. Traduções e notas de editoras podem ter "
						+ "direitos próprios (art. 7º, XI e art. 14), por isso o mod usa só trechos curtos com referência, resumos e explicações próprias.",
				0xFF7FD67F),
		PROTEGIDA("Protegida por direitos autorais",
				"Obra protegida (ex.: psicografias de Chico Xavier, falecido em 2002, protegidas até 2072, direitos cedidos a editoras como a FEB). "
						+ "O mod NÃO reproduz o texto: traz apenas sinopse, análise original e referências para a edição autorizada.",
				0xFFFFB060);

		public final String rotulo;
		public final String explicacao;
		public final int cor;

		Direitos(String rotulo, String explicacao, int cor) {
			this.rotulo = rotulo;
			this.explicacao = explicacao;
			this.cor = cor;
		}
	}

	/** Natureza de cada seção: deixa claro o que é texto da obra e o que é do mod. */
	public enum TipoTexto {
		TRECHO_ORIGINAL("Trecho da obra (citação curta, com referência) + comentário do mod", 0xFF8FD3FF),
		RESUMO("Resumo / paráfrase do mod (não é o texto da obra)", 0xFFE0E0A0),
		ADAPTACAO("Adaptação didática do mod (inspirada na obra)", 0xFFD0A0FF),
		NOTA("Nota editorial do mod (contexto histórico)", 0xFFBBBBBB);

		public final String rotulo;
		public final int cor;

		TipoTexto(String rotulo, int cor) {
			this.rotulo = rotulo;
			this.cor = cor;
		}
	}

	public record Secao(String id, String titulo, TipoTexto tipo, String texto, String referencia) {
	}

	/** Pergunta de quiz opcional (múltipla escolha). {@code id} é único no catálogo inteiro. */
	public record Pergunta(String id, String enunciado, List<String> opcoes, int correta, String explicacao, String referencia) {
	}

	public record Obra(String id, String titulo, String autor, String psicografia, String serie, List<String> assuntos, String ano, String edicao,
			String traducao, Direitos direitos, String notaDireitos, String contexto, String itemObra, List<Secao> secoes, List<String> reflexoes,
			List<Pergunta> quiz, String leiaMais) {
		public boolean temItem() {
			return itemObra != null;
		}

		public Secao secao(String idSecao) {
			for (Secao s : secoes)
				if (s.id().equals(idSecao))
					return s;
			return null;
		}

		/** Autor como aparece na ficha (com psicógrafo, quando houver). */
		public String autoria() {
			return psicografia.isEmpty() ? autor : autor + " (psicografia de " + psicografia + ")";
		}
	}

	// ================================================================ Montagem

	public static final Map<String, Obra> OBRAS = new LinkedHashMap<>();

	private static final class B {
		final String id;
		String titulo, autor, psicografia = "", serie, ano, edicao = "", traducao = "", notaDireitos = "", contexto = "", item, leiaMais = "";
		List<String> assuntos = List.of();
		Direitos direitos;
		final List<Secao> secoes = new ArrayList<>();
		final List<String> reflexoes = new ArrayList<>();
		final List<Pergunta> quiz = new ArrayList<>();

		B(String id) {
			this.id = id;
		}

		B t(String titulo, String autor, String serie, String ano) {
			this.titulo = titulo;
			this.autor = autor;
			this.serie = serie;
			this.ano = ano;
			return this;
		}

		B psico(String p) {
			psicografia = p;
			return this;
		}

		B assuntos(String... a) {
			assuntos = List.of(a);
			return this;
		}

		B edicao(String e, String trad) {
			edicao = e;
			traducao = trad;
			return this;
		}

		B direitos(Direitos d, String nota) {
			direitos = d;
			notaDireitos = nota;
			return this;
		}

		B contexto(String c) {
			contexto = c;
			return this;
		}

		B leia(String l) {
			leiaMais = l;
			return this;
		}

		/** Importa as seções da obra legível existente em itens.Obras (sem duplicar o texto). */
		B item(String idObra) {
			item = idObra;
			Obras.Obra o = Obras.OBRAS.get(idObra);
			if (o == null)
				throw new IllegalStateException("Obra sem item: " + idObra);
			int n = 1;
			for (Obras.Secao s : o.secoes()) {
				boolean citacao = s.texto().contains("\"");
				TipoTexto tipo = citacao ? TipoTexto.TRECHO_ORIGINAL : (n == 1 ? TipoTexto.NOTA : TipoTexto.RESUMO);
				secoes.add(new Secao("s" + n, s.titulo(), tipo, s.texto().replace("Paráfrase: ", ""), referenciaDe(s.texto())));
				n++;
			}
			return this;
		}

		B sec(String sid, String titulo, TipoTexto tipo, String texto, String ref) {
			secoes.add(new Secao(sid, titulo, tipo, texto, ref));
			return this;
		}

		B refl(String... r) {
			reflexoes.addAll(List.of(r));
			return this;
		}

		B quiz(String enunciado, String explicacao, String ref, int correta, String... opcoes) {
			quiz.add(new Pergunta("obra:" + id + ":" + (quiz.size() + 1), enunciado, List.of(opcoes), correta, explicacao, ref));
			return this;
		}

		void fim() {
			if (direitos == null || titulo == null || autor == null || ano == null)
				throw new IllegalStateException("Ficha incompleta: " + id);
			OBRAS.put(id, new Obra(id, titulo, autor, psicografia, serie, assuntos, ano, edicao, traducao, direitos, notaDireitos, contexto, item,
					List.copyOf(secoes), List.copyOf(reflexoes), List.copyOf(quiz), leiaMais));
		}
	}

	/** Extrai a última referência entre parênteses do texto (ex.: "(LE, q. 886)"), ou "". */
	static String referenciaDe(String texto) {
		int fim = texto.lastIndexOf(')');
		int ini = fim < 0 ? -1 : texto.lastIndexOf('(', fim);
		if (ini < 0)
			return "";
		String r = texto.substring(ini + 1, fim).trim();
		return r.length() > 80 ? "" : r;
	}

	private static B obra(String id) {
		return new B(id);
	}

	private static final String SERIE_BASICAS = "Obras básicas (Codificação Espírita)";
	private static final String SERIE_KARDEC = "Outras obras de Allan Kardec";
	private static final String SERIE_ANDRE = "A Vida no Mundo Espiritual (André Luiz)";
	private static final String SERIE_EMMANUEL = "Obras de Emmanuel";
	private static final String SERIE_CONTINUADORES = "Continuadores (séc. XIX–XX)";

	private static final String NOTA_KARDEC = "Allan Kardec faleceu em 31/03/1869: o original francês está em domínio público. A tradução brasileira mais "
			+ "difundida (FEB) é de Guillon Ribeiro (falecido em 26/10/1943; prazo encerrado em 31/12/2013), mas edições atuais trazem revisões e notas "
			+ "que podem ter direitos próprios. O mod usa apenas citações curtas com referência, trechos do original francês (domínio público) "
			+ "traduzidos pelo próprio mod e resumos autorais.";

	private static final String NOTA_CHICO = "Psicografia de Francisco Cândido Xavier (falecido em 30/06/2002): protegida no Brasil até 31/12/2072. "
			+ "Os direitos foram cedidos a editoras (principalmente a FEB). O mod traz apenas sinopse e análise originais, sem reproduzir o texto.";

	static {
		// ------------------------------------------------------------ Obras básicas
		obra("o_livro_dos_espiritos").t("O Livro dos Espíritos", "Allan Kardec", SERIE_BASICAS, "1857 (2ª ed. definitiva: 1860)")
				.assuntos("Deus e criação", "Espíritos", "Reencarnação", "Leis morais", "Vida futura").item("o_livro_dos_espiritos")
				.edicao("Paris; 1ª ed. 18/04/1857 (501 questões), 2ª ed. 1860 (1.019 questões).",
						"Citações curtas em português conferidas na tradução de Guillon Ribeiro (FEB); trechos em francês do original de 1860.")
				.direitos(Direitos.ORIGINAL_DOMINIO_PUBLICO, NOTA_KARDEC)
				.contexto("Paris do Segundo Império: as \"mesas girantes\" viraram moda nos salões por volta de 1853–1855. O professor Rivail decidiu estudar o "
						+ "fenômeno com método: perguntas organizadas, vários médiuns, comparação das respostas. O livro inaugura a Doutrina Espírita.")
				.sec("fr", "Texto original (francês, 1860)", TipoTexto.TRECHO_ORIGINAL,
						"Qu'est-ce que Dieu ? — Dieu est l'intelligence suprême, cause première de toutes choses. (q. 1)\n\n"
								+ "Où est écrite la loi de Dieu ? — Dans la conscience. (q. 621)\n\n"
								+ "Tradução do mod: \"Que é Deus? — Deus é a inteligência suprema, causa primeira de todas as coisas.\" / \"Onde está escrita a lei de Deus? — Na consciência.\"",
						"LE, q. 1 e 621 (original francês, domínio público)")
				.sec("mapa", "Mapa das questões (guia do mod)", TipoTexto.NOTA,
						"Parte 1, Das causas primárias: q. 1 a 75.\nParte 2, Do mundo espírita: q. 76 a 613 (escala espírita q. 100 a 113; perispírito q. 93 a 95; "
								+ "reencarnação a partir da q. 166; emancipação da alma a partir da q. 400).\nParte 3, Das leis morais: q. 614 a 919 (lei de liberdade "
								+ "e livre-arbítrio a partir da q. 825; justiça, amor e caridade q. 873 a 892).\nParte 4, Das esperanças e consolações: q. 920 a 1019.",
						"Estrutura da 2ª edição (1860)")
				.refl("Por que Kardec comparava respostas de médiuns diferentes em vez de aceitar uma única mensagem?",
						"Se a lei de Deus está escrita na consciência (q. 621), o que muda na forma como você toma decisões no jogo e fora dele?")
				.quiz("Segundo O Livro dos Espíritos, onde está escrita a lei de Deus?", "A resposta da questão 621 é: \"Na consciência.\"", "LE, q. 621", 1,
						"Somente em livros sagrados", "Na consciência", "Em tábuas guardadas num templo")
				.quiz("Quantas questões tem a 2ª edição (1860), base das edições atuais?", "A 1ª edição (1857) tinha 501; a 2ª, revista e ampliada, 1.019.",
						"LE, 2ª ed., 1860", 2, "501", "1.857", "1.019")
				.leia("Leia a edição completa numa tradução autorizada (FEB, IDE, LAKE etc.) ou o original francês em bibliotecas digitais de domínio público.")
				.fim();

		obra("o_livro_dos_mediuns").t("O Livro dos Médiuns", "Allan Kardec", SERIE_BASICAS, "1861")
				.assuntos("Mediunidade", "Discernimento", "Obsessão", "Reuniões espíritas").item("o_livro_dos_mediuns")
				.edicao("Paris, janeiro de 1861. Subtítulo: Guia dos Médiuns e dos Evocadores.", "Paráfrases do mod com referência ao capítulo/item.")
				.direitos(Direitos.ORIGINAL_DOMINIO_PUBLICO, NOTA_KARDEC)
				.contexto("Depois do sucesso de O Livro dos Espíritos, muitos grupos começaram a fazer experiências sem preparo. Kardec escreveu este guia "
						+ "prático para mostrar o lado experimental com prudência: como as manifestações acontecem, os tipos de médiuns e os perigos da "
						+ "credulidade, da vaidade e da obsessão.")
				.refl("Por que a vaidade é um risco para o médium?", "Quando uma mensagem agrada muito a quem a recebe, isso é prova de que é verdadeira?")
				.quiz("Qual é o caminho para lidar com a obsessão, segundo o mod com base em LM cap. XXIII?",
						"Prece, melhora moral de quem sofre e esclarecimento do Espírito obsessor. Não há exorcismo, e a água fluidificada é só apoio.",
						"LM, cap. XXIII", 0, "Prece, reforma íntima e esclarecimento do obsessor", "Exorcismo e amuletos", "Beber água fluidificada, que cura sozinha")
				.quiz("O que fazer com uma comunicação mediúnica?", "O Espírito Erasto aconselha examinar tudo pelo bom senso: melhor rejeitar dez verdades do "
						+ "que aceitar uma única mentira.", "LM, cap. XX, item 230", 2, "Aceitar tudo, pois veio de um Espírito",
						"Seguir só se o Espírito disser um nome famoso", "Examinar com a razão e o bom senso")
				.leia("Capítulos-chave: XIV (médiuns), XX (influência moral do médium), XXIII (obsessão), XXIX (reuniões).")
				.fim();

		obra("o_evangelho_segundo_o_espiritismo").t("O Evangelho segundo o Espiritismo", "Allan Kardec", SERIE_BASICAS, "1864")
				.assuntos("Moral cristã", "Caridade", "Prece", "Perdão").item("o_evangelho_segundo_o_espiritismo")
				.edicao("Paris, abril de 1864 (1ª ed. com o título Imitation de l'Évangile selon le Spiritisme).", "Citações curtas conferidas (ESE cap. VI, XV, XVII).")
				.direitos(Direitos.ORIGINAL_DOMINIO_PUBLICO, NOTA_KARDEC)
				.sec("fr", "Texto original (francês)", TipoTexto.TRECHO_ORIGINAL,
						"Hors la charité point de salut.\n\nTradução do mod: \"Fora da caridade não há salvação.\"", "ESE, cap. XV (original francês, domínio público)")
				.contexto("Kardec escolheu a parte moral dos Evangelhos, que une as pessoas em vez de dividi-las em disputas teológicas, e a comentou com "
						+ "mensagens dos Espíritos. É a obra usada no Evangelho no Lar.")
				.refl("Por que \"fora da caridade\" e não \"fora de tal religião\"?", "Ajudar alguém em público para ser elogiado tem o mesmo valor que ajudar em silêncio?")
				.quiz("Qual máxima resume o capítulo XV?", "\"Fora da caridade não há salvação\" (ESE, cap. XV).", "ESE, cap. XV", 0, "Fora da caridade não há salvação",
						"Fora da igreja não há salvação", "Fora do estudo não há salvação")
				.quiz("Segundo o ESE, o que dá valor à prece?", "O pensamento e o sentimento sinceros, não fórmulas, palavras decoradas ou rituais.", "ESE, cap. XXVII",
						1, "O número de repetições", "A sinceridade do pensamento e do sentimento", "Rezar num lugar especial com velas")
				.leia("Capítulos citados no jogo: VI, X, XIII, XV, XVII, XIX, XXVII e XXVIII (coletânea de preces).")
				.fim();

		obra("o_ceu_e_o_inferno").t("O Céu e o Inferno", "Allan Kardec", SERIE_BASICAS, "1865")
				.assuntos("Vida futura", "Justiça divina", "Causa e efeito").item("o_ceu_e_o_inferno")
				.edicao("Paris, 1865. Subtítulo: A Justiça Divina segundo o Espiritismo.", "Paráfrases do mod.")
				.direitos(Direitos.ORIGINAL_DOMINIO_PUBLICO, NOTA_KARDEC)
				.contexto("No século XIX predominava a ideia de penas eternas. Kardec confronta essa ideia com a razão e com relatos de Espíritos em diferentes "
						+ "situações depois da morte.")
				.refl("Qual a diferença entre uma consequência que educa e um castigo que só pune?")
				.quiz("Segundo O Céu e o Inferno, as penas futuras são eternas?",
						"Não. O sofrimento dura enquanto dura o mal que o causou; arrependimento, expiação e reparação abrem o caminho.",
						"CI, 1ª parte, cap. VII", 2, "Sim, para sempre", "Dependem só de um ritual", "Não: duram enquanto dura o mal, e há sempre caminho de volta")
				.fim();

		obra("a_genese").t("A Gênese", "Allan Kardec", SERIE_BASICAS, "1868")
				.assuntos("Ciência e Espiritismo", "Fluidos", "Milagres").item("a_genese")
				.edicao("Paris, janeiro de 1868. Subtítulo: Os Milagres e as Predições segundo o Espiritismo.",
						"Paráfrases do mod. Observação: há debate histórico sobre alterações na 5ª edição francesa (1872), publicada após a morte de Kardec.")
				.direitos(Direitos.ORIGINAL_DOMINIO_PUBLICO, NOTA_KARDEC)
				.contexto("Época de grandes avanços da geologia e da biologia. Kardec procura mostrar que o Espiritismo deve caminhar junto com a ciência.")
				.refl("Por que, para o Espiritismo, não existem milagres no sentido de quebra das leis naturais?")
				.quiz("Como A Gênese explica os fatos tidos como milagres?", "Deus não derroga as próprias leis: são efeitos de leis naturais ainda pouco conhecidas.",
						"Gênese, cap. XIII a XV", 1, "Como quebra das leis da natureza", "Por leis naturais ainda pouco conhecidas", "Como truques sem explicação")
				.fim();

		// ------------------------------------------------------------ Outras obras de Kardec
		obra("o_que_e_o_espiritismo").t("O que é o Espiritismo", "Allan Kardec", SERIE_KARDEC, "1859").assuntos("Introdução", "Objeções", "Sem rituais")
				.item("o_que_e_o_espiritismo").edicao("Paris, 1859.", "Paráfrases do mod.").direitos(Direitos.ORIGINAL_DOMINIO_PUBLICO, NOTA_KARDEC)
				.contexto("Pequeno livro de apresentação para quem nunca estudou o assunto, em forma de diálogos com quem duvida ou critica.")
				.refl("Como responder com respeito a quem discorda das suas ideias?")
				.quiz("O Espiritismo tem sacerdotes, sacramentos ou rituais?", "Não: reúne-se para estudar, orar e praticar a caridade, sempre de graça.",
						"O que é o Espiritismo; ESE, cap. XXVI", 0, "Não", "Sim, vários", "Só nos dias de festa")
				.fim();

		obra("revista_espirita").t("Revista Espírita", "Allan Kardec (fundador e diretor)", SERIE_KARDEC, "1858–1869")
				.assuntos("História", "Fenômenos", "Método").item("revista_espirita")
				.edicao("Revue Spirite, Journal d'Études Psychologiques. Paris, mensal, desde 1º/01/1858; 12 volumes anuais dirigidos por Kardec.",
						"Paráfrases do mod.")
				.direitos(Direitos.ORIGINAL_DOMINIO_PUBLICO, NOTA_KARDEC)
				.contexto("Era o \"laboratório\" da Doutrina: notícias de fatos, comunicações, cartas de leitores e estudos que depois entraram nos livros.")
				.sec("volumes", "Volumes (guia do mod)", TipoTexto.NOTA,
						"1858 · 1859 · 1860 · 1861 · 1862 · 1863 · 1864 · 1865 · 1866 · 1867 · 1868 · 1869 (até março, quando Kardec desencarnou). "
								+ "Cada ano reúne 12 números mensais. Use os sumários das edições anuais para localizar artigos.", "Revista Espírita, 1858–1869")
				.quiz("Em que ano começou a Revista Espírita?", "O primeiro número saiu em 1º de janeiro de 1858.", "RE, jan. 1858", 1, "1857", "1858", "1869")
				.fim();

		obra("obras_postumas").t("Obras Póstumas", "Allan Kardec", SERIE_KARDEC, "1890").assuntos("História", "Biografia", "Estudos")
				.edicao("Paris, 1890 (coletânea publicada depois da morte de Kardec).", "Resumo do mod.")
				.direitos(Direitos.ORIGINAL_DOMINIO_PUBLICO, NOTA_KARDEC)
				.contexto("Reúne escritos deixados por Kardec: estudos doutrinários e notas pessoais sobre como ele começou a estudar o Espiritismo.")
				.sec("s1", "Sobre a obra", TipoTexto.RESUMO,
						"Coletânea póstuma: textos que Kardec não chegou a publicar em livro, incluindo anotações sobre o seu primeiro contato com as mesas "
								+ "girantes e reflexões sobre a organização do movimento espírita. Ajuda a entender o homem por trás da Codificação.", "Obras Póstumas")
				.sec("s2", "Para o jogo", TipoTexto.ADAPTACAO,
						"A trilha histórica de Kardec e Amélie usa este livro como fonte de contexto: documentos, correspondências e o cuidado com a continuidade "
								+ "da obra.", "")
				.refl("Por que é importante distinguir o que um autor publicou em vida do que foi reunido depois?")
				.fim();

		obra("viagem_espirita_1862").t("Viagem Espírita em 1862", "Allan Kardec", SERIE_KARDEC, "1862").assuntos("História", "Movimento espírita")
				.edicao("Paris, 1862.", "Resumo do mod.").direitos(Direitos.ORIGINAL_DOMINIO_PUBLICO, NOTA_KARDEC)
				.contexto("Relato das visitas de Kardec a grupos espíritas do interior da França em 1862, com instruções dadas nas reuniões.")
				.sec("s1", "Sobre a obra", TipoTexto.RESUMO,
						"Kardec percorreu cidades como Lyon e Bordeaux, encontrou grupos de estudo e deu orientações: seriedade, união, estudo antes da prática e "
								+ "caridade acima das disputas.", "Viagem Espírita em 1862")
				.fim();

		// ------------------------------------------------------------ Continuadores
		obra("depois_da_morte").t("Depois da Morte", "Léon Denis", SERIE_CONTINUADORES, "1890").assuntos("Vida futura", "Filosofia", "Imortalidade")
				.edicao("Paris, 1890 (Après la mort).", "Resumo do mod. Traduções brasileiras podem ser protegidas: não reproduzidas.")
				.direitos(Direitos.ORIGINAL_DOMINIO_PUBLICO,
						"Léon Denis faleceu em 1927: original francês em domínio público. Traduções em português não foram verificadas e não são reproduzidas.")
				.contexto("Léon Denis (1846–1927), conferencista francês, foi um dos principais continuadores de Kardec, chamado de \"apóstolo do Espiritismo\".")
				.sec("s1", "Sobre a obra", TipoTexto.RESUMO,
						"Exposição filosófica sobre a imortalidade da alma, a vida depois da morte e o sentido moral da existência, escrita em linguagem de "
								+ "conferência, acessível e poética.", "Depois da Morte")
				.fim();

		// ------------------------------------------------------------ André Luiz (Chico Xavier)
		obra("nosso_lar").t("Nosso Lar", "André Luiz (Espírito)", SERIE_ANDRE, "1944").psico("Francisco Cândido Xavier").assuntos("Vida espiritual", "Umbral",
				"Colônias espirituais", "Trabalho").item("nosso_lar").edicao("Rio de Janeiro, FEB, 1944. 1º volume da coleção.", "Sinopse do mod.")
				.direitos(Direitos.PROTEGIDA, NOTA_CHICO)
				.contexto("Primeiro livro da série de André Luiz. Descreve a colônia espiritual Nosso Lar. Importante: é uma obra mediúnica posterior a Kardec; "
						+ "colônias, ministérios e bônus-hora são descrições DESTA obra, não artigos de O Livro dos Espíritos.")
				.sec("analise", "Análise do mod", TipoTexto.ADAPTACAO,
						"No jogo, a dimensão Nosso Lar é uma adaptação livre: muralhas, ministérios e rotinas são inspirados na descrição do livro, mas o desenho da "
								+ "cidade é criação do mod. Temas centrais: a recuperação não é instantânea, o trabalho é terapia, e pedir ajuda com humildade abre portas.", "")
				.refl("Por que André Luiz só foi socorrido quando mudou de atitude?", "O que significa \"trabalho\" numa cidade espiritual?")
				.quiz("Quantos Ministérios tem a colônia Nosso Lar no livro?", "Seis: Regeneração, Auxílio, Comunicação, Esclarecimento, Elevação e União Divina.",
						"Nosso Lar (FEB, 1944)", 1, "Quatro", "Seis", "Doze")
				.leia("Leia na edição autorizada da FEB.").fim();

		andre("os_mensageiros", "Os Mensageiros", "1944", "Mediunidade e missão",
				"Acompanha trabalhadores preparados no plano espiritual para servir na Terra e mostra relatos de quem falhou na tarefa por vaidade ou desânimo.");
		andre("missionarios_da_luz", "Missionários da Luz", "1945", "Reencarnação",
				"Estuda a influência do pensamento, a mediunidade e o processo de preparação de uma reencarnação, sob orientação de um instrutor.");
		andre("obreiros_da_vida_eterna", "Obreiros da Vida Eterna", "1946", "Desencarnação",
				"Uma equipe espiritual assiste pessoas no momento da desencarnação, mostrando diferenças entre quem se preparou e quem viveu preso à matéria.");
		andre("no_mundo_maior", "No Mundo Maior", "1947", "Mente e perispírito",
				"Estudo sobre a mente, o cérebro e as perturbações mentais, com atendimentos a encarnados e desencarnados.");
		andre("libertacao", "Libertação", "1949", "Obsessão",
				"Trata de processos obsessivos e do esclarecimento de Espíritos endurecidos, com destaque para a paciência e o amor no socorro.");
		andre("nos_dominios_da_mediunidade", "Nos Domínios da Mediunidade", "1955", "Mediunidade",
				"Observação de um grupo mediúnico: tipos de mediunidade, sintonia, responsabilidade do médium e importância do estudo.");
		andre("acao_e_reacao", "Ação e Reação", "1957", "Causa e efeito",
				"Casos que ilustram a lei de causa e efeito sem fatalismo: o passado influencia, mas o esforço no bem renova o caminho.");

		// ------------------------------------------------------------ Emmanuel (Chico Xavier)
		emmanuel("ha_dois_mil_anos", "Há Dois Mil Anos", "1939", "Romance histórico",
				"Romance ambientado no Império Romano do tempo de Jesus; segundo a obra, narra uma existência passada do próprio Emmanuel como senador romano.");
		emmanuel("o_consolador", "O Consolador", "1940", "Perguntas e respostas",
				"Perguntas e respostas sobre ciência, filosofia e religião à luz do Espiritismo.");
		emmanuel("paulo_e_estevao", "Paulo e Estêvão", "1942", "Romance histórico",
				"Romance sobre Paulo de Tarso e Estêvão, o primeiro mártir cristão, mostrando a transformação de perseguidor em apóstolo.");
	}

	private static void andre(String id, String titulo, String ano, String assunto, String sinopse) {
		obra(id).t(titulo, "André Luiz (Espírito)", SERIE_ANDRE, ano).psico("Francisco Cândido Xavier").assuntos(assunto, "Vida espiritual")
				.edicao("FEB, " + ano + ".", "Sinopse do mod.").direitos(Direitos.PROTEGIDA, NOTA_CHICO)
				.contexto("Volume da coleção de André Luiz. Descrições do plano espiritual pertencem a esta obra mediúnica, não às obras básicas de Kardec.")
				.sec("sinopse", "Sinopse (do mod)", TipoTexto.RESUMO, sinopse, titulo + " (FEB, " + ano + ")")
				.refl("Que ideia das obras básicas de Kardec este livro ilustra? (Dica: procure no glossário.)").leia("Leia na edição autorizada da FEB.").fim();
	}

	private static void emmanuel(String id, String titulo, String ano, String assunto, String sinopse) {
		obra(id).t(titulo, "Emmanuel (Espírito)", SERIE_EMMANUEL, ano).psico("Francisco Cândido Xavier").assuntos(assunto)
				.edicao("FEB, " + ano + ".", "Sinopse do mod.").direitos(Direitos.PROTEGIDA, NOTA_CHICO)
				.contexto("Emmanuel é apresentado nas obras de Chico Xavier como seu guia espiritual.")
				.sec("sinopse", "Sinopse (do mod)", TipoTexto.RESUMO, sinopse, titulo + " (FEB, " + ano + ")").leia("Leia na edição autorizada da FEB.").fim();
	}

	// ================================================================ Consultas

	public static Obra get(String id) {
		return id == null ? null : OBRAS.get(id);
	}

	public static List<String> autores() {
		Set<String> s = new LinkedHashSet<>();
		OBRAS.values().forEach(o -> s.add(o.autor()));
		return List.copyOf(s);
	}

	public static List<String> series() {
		Set<String> s = new LinkedHashSet<>();
		OBRAS.values().forEach(o -> s.add(o.serie()));
		return List.copyOf(s);
	}

	public static List<String> assuntos() {
		Set<String> s = new LinkedHashSet<>();
		OBRAS.values().forEach(o -> s.addAll(o.assuntos()));
		List<String> l = new ArrayList<>(s);
		Collections.sort(l);
		return l;
	}

	public static List<Obra> filtrar(String autor, String serie, String assunto) {
		List<Obra> l = new ArrayList<>();
		for (Obra o : OBRAS.values())
			if ((autor == null || o.autor().equals(autor)) && (serie == null || o.serie().equals(serie)) && (assunto == null || o.assuntos().contains(assunto)))
				l.add(o);
		return l;
	}

	/** Busca simples de termo em títulos e textos das seções: lista "obraId/secaoId". */
	public static List<String> buscar(String termo) {
		List<String> r = new ArrayList<>();
		if (termo == null || termo.isBlank())
			return r;
		String t = termo.toLowerCase();
		for (Obra o : OBRAS.values())
			for (Secao s : o.secoes())
				if (s.titulo().toLowerCase().contains(t) || s.texto().toLowerCase().contains(t))
					r.add(o.id() + "/" + s.id());
		return r;
	}

	/** Qualquer pergunta de quiz (de obra ou de trilha) pelo id. */
	public static Pergunta pergunta(String id) {
		if (id == null)
			return null;
		for (Obra o : OBRAS.values())
			for (Pergunta p : o.quiz())
				if (p.id().equals(id))
					return p;
		for (Trilhas.Modulo m : Trilhas.MODULOS.values())
			if (m.pergunta() != null && m.pergunta().id().equals(id))
				return m.pergunta();
		return null;
	}
}
