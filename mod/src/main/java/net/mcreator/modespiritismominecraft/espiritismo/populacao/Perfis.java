package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/**
 * Catálogo de perfis. Fontes e critérios em docs/NPCS_E_PERSONAGENS.md.
 * <ul>
 * <li><b>Literários</b>: personagens confirmados no livro <i>Nosso Lar</i> (André Luiz / Francisco Cândido Xavier, FEB, 1944).
 * As falas são ADAPTAÇÕES originais do jogo (nunca transcrições); quando resumem um fato do livro, trazem o capítulo.</li>
 * <li><b>Histórico</b>: Amélie Gabrielle Boudet — fatos verificados em fontes biográficas; falas são adaptações.</li>
 * <li><b>Genéricos</b>: moradores criados para o jogo, com nomes fictícios.</li>
 * </ul>
 */
public final class Perfis {
	private Perfis() {
	}

	public static final String AMELIE = "amelie";

	private static final Map<String, Perfil> TODOS = new LinkedHashMap<>();
	private static final List<Perfil> NOMEADOS_NOSSO_LAR = new ArrayList<>();
	private static final List<Perfil> GENERICOS = new ArrayList<>();

	private static Perfil add(Perfil p) {
		TODOS.put(p.id(), p);
		if (p.categoria() == Perfil.Categoria.LITERARIO)
			NOMEADOS_NOSSO_LAR.add(p);
		else if (p.categoria() == Perfil.Categoria.GENERICO)
			GENERICOS.add(p);
		return p;
	}

	@Nullable
	public static Perfil get(String id) {
		return id == null ? null : TODOS.get(id);
	}

	public static List<Perfil> nomeadosNossoLar() {
		return NOMEADOS_NOSSO_LAR;
	}

	public static List<Perfil> genericos() {
		return GENERICOS;
	}

	public static Map<String, Perfil> todos() {
		return TODOS;
	}

	/** Perfil genérico sorteado entre os que trabalham/convivem na zona (ou qualquer um, se a zona for desconhecida). */
	public static Perfil genericoPara(String zona, RandomSource r) {
		List<Perfil> lista = new ArrayList<>();
		for (Perfil p : GENERICOS)
			if (p.local().equals(zona) || p.tipo().zonas.contains(zona))
				lista.add(p);
		if (lista.isEmpty())
			lista = GENERICOS;
		return lista.get(r.nextInt(lista.size()));
	}

	// ================================================================ Personagens do livro Nosso Lar

	static {
		add(Perfil.de("clarencio", "Ministro Clarêncio", TipoNpc.MINISTRO).literario("Nosso Lar, caps. 2, 8, 13 e 14 — Ministro do Auxílio")
				.local("auxilio").rotina("auxilio", "chegada", "governadoria").descanso("residencias")
				.ola("Seja bem-vindo, meu irmão. Pode descansar: ninguém chega a esta casa sem amparo.")
				.explica("acolhimento", "Muitos chegam cansados e confusos, às vezes depois de longo tempo em regiões sombrias. O primeiro passo é aceitar o tratamento com paciência e humildade.")
				.explica("ministerios", "A colônia se organiza em seis Ministérios: Regeneração, Auxílio, Comunicação, Esclarecimento, Elevação e União Divina. Os quatro primeiros ficam mais próximos das tarefas ligadas à Terra; os dois últimos, dos planos mais altos (Nosso Lar, cap. 8).")
				.explica("trabalho", c -> c.etapaRecemChegado() >= JornadaRecemChegado.MINISTERIOS,
						"Aqui o trabalho não é castigo: é oportunidade. As horas de serviço útil são registradas e contam a favor de cada um (Nosso Lar, caps. 21 e 22).")
				.explica("obra", c -> c.leu("nosso_lar"),
						"Vejo que você leu o relato de André Luiz. Lembre-se de que é a descrição de uma colônia numa obra mediúnica, não um ensino das obras básicas de Kardec. Estude ambas, sabendo de onde vem cada coisa.")
				.explica("servico_fora", c -> c.nossoLar() && SaidaAcompanhada.podeReceber(c.player()),
						"Quando quiser servir fora da colônia, fale comigo agachado: ofereço uma saída acompanhada até o Posto de Socorro do Umbral. Sair sozinho e sem preparo é imprudente; acompanhado, é serviço.")
				.diz("Peça e receba ajuda sem vergonha. Todos aqui já precisaram dela um dia.",
						"A prece sincera muda primeiro quem ora; por isso abre caminho para o socorro.",
						"Volte sempre que tiver dúvidas. Orientar faz parte do meu serviço.")
				.criar());

		add(Perfil.de("lisias", "Lísias", TipoNpc.ENFERMEIRO).literario("Nosso Lar, caps. 5 a 9 e 17 — enfermeiro visitador")
				.local("hospital").rotina("auxilio", "residencias", "bosque").descanso("residencias")
				.ola("Meu nome é Lísias. Trabalho como enfermeiro visitador e acompanho os que chegam ainda em tratamento.")
				.explica("prece", "No relato de André Luiz, o socorro chegou depois que ele orou com toda a alma. Não porque Deus esperasse a prece para amar, mas porque a prece nos põe em condição de receber (Nosso Lar, cap. 6).")
				.explica("recuperacao", "A recuperação não é instantânea. Há quem leve dias, outros muito mais. Cada passo conta, mesmo os pequenos.")
				.explica("mae", c -> !c.primeiroEncontro(),
						"Minha mãe, a senhora Laura, mora na área residencial. Ela recebe com carinho quem quiser conversar sobre a vida em família por aqui (Nosso Lar, cap. 17).")
				.explica("obra", c -> c.leu("nosso_lar"), "Se você leu Nosso Lar, já sabe que André Luiz também chegou cheio de perguntas. Pergunte: é assim que se aprende.")
				.diz("Descansou bem? O descanso também é tratamento.",
						"Quando estiver mais firme, poderá conhecer outros setores da colônia.",
						"Nos intervalos gosto de caminhar até o bosque.")
				.criar());

		add(Perfil.de("laura", "Senhora Laura", TipoNpc.MORADOR).literario("Nosso Lar, caps. 17 a 22, 39 e 47 — mãe de Lísias")
				.local("residencias").rotina("residencias", "bosque", "praca").descanso("residencias")
				.ola("Entre, a casa é simples, mas está aberta a quem chega em paz. Sou Laura, mãe de Lísias.")
				.explica("lar", "Aqui também há lares e famílias. A convivência se aprende como tudo: com paciência, respeito e a parte de trabalho de cada um (inspirado em Nosso Lar, cap. 20).")
				.explica("bonus", c -> c.etapaRecemChegado() >= JornadaRecemChegado.MINISTERIOS,
						"Já ouviu falar do bônus-hora? É a contagem das horas de serviço útil, que dá certas prerrogativas na vida da colônia (Nosso Lar, cap. 22). Mas o que vale mesmo é o bem que se faz com ele.")
				.explica("retorno", c -> c.etapaRecemChegado() >= JornadaRecemChegado.CONCLUIDA,
						"Um dia também voltarei à Terra numa nova existência. Reencarnar é sempre uma tentativa importante, e ninguém vai sozinho: os amigos daqui continuam por perto (inspirado em Nosso Lar, cap. 47).")
				.diz("Minhas filhas estão no trabalho a esta hora; à noite a casa fica cheia.",
						"Cuide do pensamento à mesa: conversa pesada não alimenta ninguém.",
						"Os jardins das casas dizem muito de quem mora nelas.")
				.criar());

		add(Perfil.de("narcisa", "Narcisa", TipoNpc.ENFERMEIRO).literario("Nosso Lar, caps. 27 a 29, 32 e 42 — servidora das Câmaras de Retificação")
				.local("camaras_retificacao").rotina("regeneracao", "hospital").descanso("residencias")
				.ola("Sou Narcisa e sirvo nas Câmaras de Retificação. Venha devagar: aqui muitos ainda sofrem.")
				.explica("camaras", "Nas Câmaras ficam irmãos muito perturbados, recém-recolhidos de regiões sombrias. O serviço é humilde: limpar, consolar, ouvir, amparar (inspirado em Nosso Lar, caps. 27 e 28).")
				.explica("medo", "Em tempos difíceis, a Governadoria chegou a pôr o tratamento contra o medo acima das próprias aulas de enfermagem: a calma é a garantia do bom serviço (Nosso Lar, cap. 42).")
				.explica("veneranda", c -> !c.primeiroEncontro(),
						"A Ministra Veneranda, da Regeneração, criou muitos recursos para os mais infelizes. Os outros Ministros a ouvem antes de decisões importantes (Nosso Lar, cap. 32).")
				.diz("Paciência com quem sofre: ninguém escolhe a própria confusão.",
						"Trabalho aqui há muito tempo e ainda me comovo.",
						"Uma palavra mansa às vezes alivia mais que qualquer outro recurso.")
				.criar());

		add(Perfil.de("tobias", "Tobias", TipoNpc.ORIENTADOR).literario("Nosso Lar, caps. 26 a 28, 37 e 38 — servidor da Regeneração")
				.local("camaras_retificacao").rotina("regeneracao", "residencias").descanso("residencias")
				.ola("Sou Tobias, trabalho nas Câmaras de Retificação, no Ministério da Regeneração. Veio observar o serviço?")
				.explica("servico", "Quem quer servir começa observando. Depois faz o que está ao seu alcance: carregar material, arrumar, ajudar os enfermeiros (inspirado em Nosso Lar, caps. 27 e 28).")
				.explica("casa", c -> !c.primeiroEncontro(),
						"Em minha casa, a paz da família veio da renúncia de quem soube pensar primeiro nos outros. É uma história longa, contada no livro (Nosso Lar, cap. 38).")
				.diz("Aqui o trabalho não para: sempre chega alguém recolhido das regiões sombrias.",
						"Às Câmaras não se vai por curiosidade: vai-se para servir ou para aprender.",
						"Quer ouvir a Ministra Veneranda? Só aproveita quem vai sinceramente interessado.")
				.criar());

		add(Perfil.de("veneranda", "Ministra Veneranda", TipoNpc.MINISTRO).literario("Nosso Lar, caps. 32 e 37 — Ministra da Regeneração")
				.local("regeneracao").rotina("regeneracao", "bosque").descanso("residencias")
				.ola("Seja bem-vindo à Regeneração. Aqui, recomeçar é a palavra de cada dia.")
				.explica("salao", "Gosto de ensinar ao ar livre: os salões naturais do grande parque foram pensados para que estudo e beleza andassem juntos (inspirado em Nosso Lar, cap. 32).")
				.explica("conferencia", Perfil.Contexto::leuAlguma,
						"Às vezes falo para centenas de ouvintes, servidores e acolhidos da Regeneração e do Auxílio. Aproveita quem vem sinceramente interessado (inspirado em Nosso Lar, cap. 37).")
				.diz("Ninguém é tão pequeno que não possa servir, nem tão grande que não precise aprender.",
						"O recomeço é sempre possível.")
				.criar());

		add(Perfil.de("genesio", "Ministro Genésio", TipoNpc.MINISTRO).literario("Nosso Lar, caps. 26 e 47 — Ministro da Regeneração")
				.local("regeneracao").rotina("regeneracao", "camaras_retificacao").descanso("residencias")
				.ola("Sou Genésio, da Regeneração. Em que posso ajudar?")
				.explica("observacao", "Quem vem de outro Ministério para aprender aqui é bem-vindo. Peça a Tobias que o acompanhe às Câmaras (inspirado em Nosso Lar, cap. 26).")
				.diz("Confie na Providência e também em si mesmo: assim se enfrenta qualquer recomeço (inspirado em Nosso Lar, cap. 47).",
						"Há trabalho para todas as mãos de boa vontade.")
				.criar());

		add(Perfil.de("henrique_de_luna", "Henrique de Luna", TipoNpc.ENFERMEIRO).literario("Nosso Lar, caps. 4 e 5 — Serviço de Assistência Médica")
				.local("hospital").rotina("hospital", "auxilio").descanso("residencias")
				.ola("Sou Henrique de Luna, do Serviço de Assistência Médica. Vamos ver como você está.")
				.explica("corpo", "Muitas das dores que você ainda sente vêm de hábitos e pensamentos que trouxe consigo. O tratamento ajuda, mas precisa da sua colaboração (inspirado em Nosso Lar, cap. 4).")
				.diz("Serenidade no pensamento também é parte do tratamento.",
						"Não tenha pressa de ter alta. Firmeza vale mais que rapidez.")
				.criar());

		add(Perfil.de("governador", "Governador", TipoNpc.GOVERNADOR).literario("Nosso Lar, caps. 3, 8, 9, 11 e 42 — o livro não dá nome ao Governador")
				.local("governadoria").rotina("governadoria", "regeneracao").descanso("governadoria")
				.ola("Paz a você, irmão. Que bom vê-lo de pé.")
				.explica("ministros", "Setenta e dois Ministros, doze em cada Ministério, ajudam a cuidar da cidade. Sozinho, ninguém governa nada (inspirado em Nosso Lar, caps. 3 e 8).")
				.explica("calma", "Diante do medo, a calma. Diante da discórdia, o diálogo paciente: quem discorda de uma medida não precisa ser castigado para compreender (inspirado em Nosso Lar, caps. 9 e 42).")
				.explica("descanso", "Descanse quando for preciso. Quem cuida dos outros também precisa ser cuidado.")
				.diz("Volte ao seu trabalho com alegria: o bem pequeno de cada dia sustenta a colônia.",
						"Ore pela colônia e pelos que ainda sofrem fora dela.")
				.criar());

		// ============================================================ Personagem histórica

		add(Perfil.de(AMELIE, "Amélie Boudet", TipoNpc.HISTORICO)
				.historico("Amélie Gabrielle Boudet (1795–1883), professora e esposa de Allan Kardec — ver docs/NPCS_E_PERSONAGENS.md")
				.local("").rotina().descanso("")
				.ola("Bem-vinda, bem-vindo à nossa casa. Sou Amélie Boudet. Há muito trabalho sobre esta mesa: cartas, anotações, provas de impressão...")
				.diz("Papel bem guardado é memória que não se perde.",
						"Antes de publicar, examinar. Antes de concluir, comparar.",
						"Fui professora antes de tudo isto; ensinar continua sendo o meu jeito de servir.")
				.criar());

		// ============================================================ Genéricos (nomes fictícios, criados para o jogo)

		add(Perfil.de("anselmo", "Anselmo", TipoNpc.RECEM_CHEGADO).local("chegada")
				.ola("Fui carpinteiro a vida toda. Me disseram que aqui mãos dispostas sempre encontram serviço; mas primeiro preciso me refazer.").criar());
		add(Perfil.de("doroteia", "Dorotéia", TipoNpc.RECEM_CHEGADO).local("auxilio")
				.ola("Cheguei faz poucos dias. Ainda procuro os óculos no bolso, acredita? Velhos costumes demoram a sair.").criar());
		add(Perfil.de("severino", "Severino", TipoNpc.PACIENTE).local("regeneracao")
				.ola("Passei muito tempo preso a mágoas. Aqui estou aprendendo que perdoar também é remédio (ESE, cap. X).").criar());
		add(Perfil.de("lucia", "Lúcia", TipoNpc.PACIENTE).local("hospital")
				.ola("Às vezes ainda acordo assustada. A enfermeira diz que o medo vai embora devagar, como a neblina.").criar());
		add(Perfil.de("rosalia", "Irmã Rosália", TipoNpc.ENFERMEIRO).local("hospital")
				.ola("Trabalho no turno da noite. É quando os pacientes mais precisam de uma voz calma.").criar());
		add(Perfil.de("teodoro", "Irmão Teodoro", TipoNpc.ENFERMEIRO).local("auxilio")
				.ola("Aprendi enfermagem aqui mesmo. Na Terra eu nunca tinha cuidado de ninguém.").criar());
		add(Perfil.de("benedita", "Dona Benedita", TipoNpc.ORIENTADOR).local("chegada")
				.ola("Recebo os que chegam. Primeiro o acolhimento, depois as explicações.").criar());
		add(Perfil.de("matias", "Matias", TipoNpc.ORIENTADOR).local("auxilio")
				.ola("Muitos perguntam se vão ver logo os seus queridos. Digo a verdade: cada caso tem seu tempo, e o amor não se perde.").criar());
		add(Perfil.de("ester", "Professora Ester", TipoNpc.INSTRUTOR).local("escola")
				.ola("Minha turma de hoje estuda a escala espírita: os Espíritos se distinguem pelo grau de adiantamento (LE, q. 100 a 113).").criar());
		add(Perfil.de("ambrosio", "Professor Ambrósio", TipoNpc.INSTRUTOR).local("esclarecimento")
				.ola("Gosto de começar a aula com uma pergunta, não com uma resposta. Qual é a sua?").criar());
		add(Perfil.de("clara", "Clara, mensageira", TipoNpc.MENSAGEIRO).local("comunicacao")
				.ola("Corro, mas não atropelo: mensagem dada com pressa demais chega pela metade.").criar());
		add(Perfil.de("davi", "Davi, mensageiro", TipoNpc.MENSAGEIRO).local("praca")
				.ola("Hoje levo convites para as palestras do Ministério do Esclarecimento.").criar());
		add(Perfil.de("helena", "Helena, bibliotecária", TipoNpc.BIBLIOTECARIO).local("biblioteca")
				.ola("Organizar livros parece simples, mas exige cuidado: livro fora do lugar é livro perdido.").criar());
		add(Perfil.de("otavio", "Otávio, bibliotecário", TipoNpc.BIBLIOTECARIO).local("esclarecimento")
				.ola("Para começar, recomendo O Livro dos Espíritos. É a base para entender o resto.").criar());
		add(Perfil.de("bento", "Bento, jardineiro", TipoNpc.JARDINEIRO).local("bosque")
				.ola("Na Terra eu tinha uma horta no quintal. Aqui continuo plantando, só que com mais calma.").criar());
		add(Perfil.de("ines", "Inês, jardineira", TipoNpc.JARDINEIRO).local("praca")
				.ola("Estas flores perfumam o caminho de quem chega cansado.").criar());
		add(Perfil.de("lauro", "Lauro, artífice", TipoNpc.ARTIFICE).local("comunicacao")
				.ola("Conserto o que se quebra e ensino quem quer aprender.").criar());
		add(Perfil.de("marta", "Marta, costureira", TipoNpc.ARTIFICE).local("regeneracao")
				.ola("Preparo roupas para os recém-chegados. Roupa limpa também acolhe.").criar());
		add(Perfil.de("sebastiao", "Seu Sebastião", TipoNpc.MORADOR).local("residencias").rotina("bosque", "praca")
				.ola("Toda tarde caminho até o bosque. Faz bem ver a cidade com calma.").criar());
		add(Perfil.de("noemia", "Noêmia", TipoNpc.MORADOR).local("elevacao").rotina("elevacao", "praca")
				.ola("Ensaiamos canções para a noite. A música também educa os sentimentos.").criar());
		// Família espiritual (§12.3): histórias FICTÍCIAS de vínculo e reencontro, nunca ligadas ao jogador.
		add(Perfil.de("adelia", "Adélia", TipoNpc.MORADOR).local("residencias").rotina("praca", "bosque")
				.ola("Reencontrei aqui uma amiga de infância. Não era parente de sangue, mas era família do coração.")
				.explica("familia", c -> !c.primeiroEncontro(),
						"Os laços de afeição não se desfazem com a morte do corpo; os Espíritos que se amaram se reconhecem e se reencontram (LE, q. 289 e 291 a 303). Mas cada reencontro tem a sua hora, sem pressa e sem ansiedade.")
				.criar());
		add(Perfil.de("jonas", "Jonas", TipoNpc.MORADOR).local("uniao_divina").rotina("uniao_divina", "praca")
				.ola("Tenho um irmão que ficou na Terra. Aprendi a esperar orando por ele, sem aflição.")
				.explica("espera", c -> !c.primeiroEncontro(),
						"No começo eu queria correr até ele. Aqui me ensinaram que ajudar quem amamos começa por nos equilibrar; aflito, eu só o perturbaria.")
				.criar());
		add(Perfil.de("rui", "Rui, auxiliar", TipoNpc.ADMINISTRADOR).local("governadoria")
				.ola("Registro os pedidos de serviço. Cada um é lido com atenção.").criar());
		add(Perfil.de("celina", "Celina, auxiliar", TipoNpc.ADMINISTRADOR).local("governadoria")
				.ola("Organizo as escalas de trabalho. Ninguém trabalha além do que pode.").criar());
	}
}
