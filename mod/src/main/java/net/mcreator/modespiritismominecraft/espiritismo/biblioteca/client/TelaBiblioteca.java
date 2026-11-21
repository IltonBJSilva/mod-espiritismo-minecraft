package net.mcreator.modespiritismominecraft.espiritismo.biblioteca.client;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.Catalogo;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.Glossario;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.PacotesBiblioteca;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.ProgressoBiblioteca;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.RegistroBiblioteca;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.Trilhas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Tela da Biblioteca Espírita (só cliente). Abas: catálogo (por autor/série/assunto, com busca), ficha da obra
 * (identificação, contexto, direitos, sumário, nota pessoal), leitura de seção, quiz opcional, trilhas, glossário,
 * Evangelho no Lar e progresso. Todo conteúdo vem de {@link Catalogo}/{@link Trilhas}/{@link Glossario} (compartilhados);
 * o progresso vem do attachment sincronizado. Ações vão ao servidor, que valida.
 */
public class TelaBiblioteca extends Screen {
	private enum Modo {
		CATALOGO, OBRA, SECAO, QUIZ, TRILHAS, MODULO, GLOSSARIO, EVANGELHO, PROGRESSO
	}

	private static final int BRANCO = 0xFFFFFFFF, CINZA = 0xFFAAAAAA, OURO = 0xFFFFD86B, AZUL = 0xFF8FD3FF, VERDE = 0xFF7FD67F;
	private static final String[] AGRUPAR = {"Todas", "Autor", "Série", "Assunto"};

	private Modo modo = Modo.CATALOGO;
	private int pagina = 0, rolagem = 0;
	private int agrupar = 2, grupo = 0;
	private String busca = "", nota = "";
	private String obraId, moduloId, termoId;
	private int secaoIdx = 0;
	private String trilhaId = "fundamentos";
	private List<Catalogo.Pergunta> quiz = List.of();
	private int quizIdx = 0, resposta = -1;
	private Modo voltarQuiz = Modo.OBRA;
	private ProgressoBiblioteca visto;

	// Área de texto
	private record Linha(FormattedCharSequence seq, int cor) {
	}

	private final List<Linha> linhas = new ArrayList<>();
	private int tx, ty, tw, th;
	private int px, py, pw, ph;

	public TelaBiblioteca() {
		super(Component.literal("Biblioteca Espírita"));
	}

	public static void abrir(String aba, String alvo) {
		TelaBiblioteca t = new TelaBiblioteca();
		switch (aba) {
			case "obra" -> {
				if (Catalogo.get(alvo) != null) {
					t.obraId = alvo;
					t.modo = Modo.OBRA;
				}
			}
			case "trilha" -> {
				if (Trilhas.MODULOS.containsKey(alvo)) {
					t.moduloId = alvo;
					t.trilhaId = Trilhas.MODULOS.get(alvo).trilha();
					t.modo = Modo.MODULO;
				} else
					t.modo = Modo.TRILHAS;
			}
			case "glossario" -> {
				t.modo = Modo.GLOSSARIO;
				if (Glossario.TERMOS.containsKey(alvo))
					t.termoId = alvo;
			}
			case "evangelho" -> t.modo = Modo.EVANGELHO;
			default -> t.modo = Modo.CATALOGO;
		}
		Minecraft.getInstance().setScreen(t);
	}

	private ProgressoBiblioteca prog() {
		var p = Minecraft.getInstance().player;
		return p == null ? ProgressoBiblioteca.VAZIO : p.getData(RegistroBiblioteca.PROGRESSO);
	}

	private void enviar(int tipo, String a, String b, int n) {
		ClientPacketDistributor.sendToServer(new PacotesBiblioteca.Acao(tipo, a == null ? "" : a, b == null ? "" : b, n));
	}

	private void ir(Modo m) {
		modo = m;
		pagina = 0;
		rolagem = 0;
		rebuildWidgets();
	}

	private Button botao(String texto, int x, int y, int w, Button.OnPress acao) {
		return addRenderableWidget(Button.builder(Component.literal(texto), acao).bounds(x, y, w, 16).build());
	}

	// ================================================================ Montagem

	@Override
	protected void init() {
		pw = Math.min(width - 16, 460);
		ph = Math.min(height - 16, 260);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		visto = prog();
		linhas.clear();

		// Abas
		String[] abas = {"Catálogo", "Trilhas", "Glossário", "Evangelho no Lar", "Progresso"};
		Modo[] destinos = {Modo.CATALOGO, Modo.TRILHAS, Modo.GLOSSARIO, Modo.EVANGELHO, Modo.PROGRESSO};
		int wa = (pw - 8) / abas.length;
		for (int i = 0; i < abas.length; i++) {
			Modo d = destinos[i];
			Button b = botao(abas[i], px + 4 + i * wa, py + 4, wa - 2, x -> ir(d));
			b.active = !(modo == d || (d == Modo.CATALOGO && (modo == Modo.OBRA || modo == Modo.SECAO)) || (d == Modo.TRILHAS && modo == Modo.MODULO));
		}
		int topo = py + 26, base = py + ph - 22;
		switch (modo) {
			case CATALOGO -> initCatalogo(topo, base);
			case OBRA -> initObra(topo, base);
			case SECAO -> initSecao(topo, base);
			case QUIZ -> initQuiz(topo, base);
			case TRILHAS -> initTrilhas(topo, base);
			case MODULO -> initModulo(topo, base);
			case GLOSSARIO -> initGlossario(topo, base);
			case EVANGELHO -> initEvangelho(topo, base);
			case PROGRESSO -> initProgresso(topo, base);
		}
	}

	/** Lista paginada de botões. */
	private void lista(List<String> rotulos, IntConsumer acao, int x, int y, int w, int h) {
		int porPagina = Math.max(1, (h - 18) / 17);
		int paginas = Math.max(1, (rotulos.size() + porPagina - 1) / porPagina);
		pagina = Math.min(pagina, paginas - 1);
		for (int k = 0; k < porPagina; k++) {
			int i = pagina * porPagina + k;
			if (i >= rotulos.size())
				break;
			int idx = i;
			botao(rotulos.get(i), x, y + k * 17, w, b -> acao.accept(idx));
		}
		if (paginas > 1) {
			Button ant = botao("◀", x, y + h - 16, 20, b -> {
				pagina--;
				rebuildWidgets();
			});
			ant.active = pagina > 0;
			Button prox = botao("▶", x + w - 20, y + h - 16, 20, b -> {
				pagina++;
				rebuildWidgets();
			});
			prox.active = pagina < paginas - 1;
			Button info = botao((pagina + 1) + "/" + paginas, x + 22, y + h - 16, w - 44, b -> {
			});
			info.active = false;
		}
	}

	private void areaTexto(int x, int y, int w, int h) {
		tx = x;
		ty = y;
		tw = w;
		th = h;
	}

	private void par(String texto, int cor) {
		if (texto == null || texto.isEmpty())
			return;
		for (FormattedCharSequence s : font.split(FormattedText.of(texto), tw - 6))
			linhas.add(new Linha(s, cor));
	}

	private void titulo(String texto, int cor) {
		if (!linhas.isEmpty())
			linhas.add(new Linha(FormattedCharSequence.EMPTY, BRANCO));
		par(texto, cor);
	}

	// ---------------------------------------------------------------- Catálogo

	private List<String> valoresGrupo() {
		return switch (agrupar) {
			case 1 -> Catalogo.autores();
			case 2 -> Catalogo.series();
			case 3 -> Catalogo.assuntos();
			default -> List.of();
		};
	}

	private void initCatalogo(int topo, int base) {
		int lw = pw * 11 / 20;
		EditBox caixa = new EditBox(font, px + 6, topo, lw - 6, 16, Component.literal("Buscar"));
		caixa.setMaxLength(40);
		caixa.setHint(Component.literal("Buscar termo (ex.: prece)"));
		caixa.setValue(busca);
		caixa.setResponder(s -> {
			if (!s.equals(busca)) {
				busca = s;
				pagina = 0;
				rebuildWidgets();
				setFocused(children().stream().filter(c -> c instanceof EditBox).findFirst().orElse(null));
			}
		});
		addRenderableWidget(caixa);
		int y = topo + 19;
		if (busca.isBlank()) {
			botao("Agrupar: " + AGRUPAR[agrupar], px + 6, y, lw / 2 - 4, b -> {
				agrupar = (agrupar + 1) % AGRUPAR.length;
				grupo = 0;
				pagina = 0;
				rebuildWidgets();
			});
			List<String> valores = valoresGrupo();
			if (!valores.isEmpty()) {
				grupo = Math.floorMod(grupo, valores.size());
				Button g = botao(valores.get(grupo), px + lw / 2 + 4, y, lw / 2 - 4, b -> {
					grupo++;
					pagina = 0;
					rebuildWidgets();
				});
				g.setTooltip(Tooltip.create(Component.literal("Clique para ver o próximo grupo")));
			}
			y += 19;
			String autor = agrupar == 1 ? valores.get(grupo) : null, serie = agrupar == 2 ? valores.get(grupo) : null,
					assunto = agrupar == 3 ? valores.get(grupo) : null;
			List<Catalogo.Obra> obras = Catalogo.filtrar(autor, serie, assunto);
			List<String> rot = new ArrayList<>();
			ProgressoBiblioteca p = visto;
			for (Catalogo.Obra o : obras)
				rot.add((o.direitos() == Catalogo.Direitos.PROTEGIDA ? "© " : "") + o.titulo() + " (" + o.ano().split(" ")[0] + ")"
						+ (p.lidasDa(o.id()) > 0 ? " · " + p.lidasDa(o.id()) + "/" + o.secoes().size() : ""));
			lista(rot, i -> {
				obraId = obras.get(i).id();
				ir(Modo.OBRA);
			}, px + 6, y, lw - 6, base - y);
		} else {
			List<String> achados = Catalogo.buscar(busca);
			List<String> rot = new ArrayList<>();
			for (String a : achados) {
				String[] partes = a.split("/");
				Catalogo.Obra o = Catalogo.get(partes[0]);
				rot.add(o.titulo() + " › " + o.secao(partes[1]).titulo());
			}
			lista(rot, i -> {
				String[] partes = achados.get(i).split("/");
				abrirSecao(partes[0], partes[1]);
			}, px + 6, y, lw - 6, base - y);
		}
		areaTexto(px + lw + 6, topo, pw - lw - 12, base - topo);
		par("Catálogo da Biblioteca", OURO);
		par(Catalogo.OBRAS.size() + " obras catalogadas. Clique numa obra para ver ficha, contexto, direitos e sumário.", BRANCO);
		titulo("Como ler as indicações", OURO);
		for (Catalogo.TipoTexto t : Catalogo.TipoTexto.values())
			par("■ " + t.rotulo, t.cor);
		titulo("Direitos autorais", OURO);
		par("Obras marcadas com © são protegidas: o mod traz só sinopse e análise próprias. Nas demais, o original é de domínio público, "
				+ "mas o mod usa resumos e citações curtas com referência (Lei 9.610/1998).", CINZA);
		titulo("Interpretação educativa não oficial", OURO);
		par("Este conteúdo é uma adaptação didática feita para o jogo. Para estudo sério, leia as obras completas em edições autorizadas.", CINZA);
		if (busca.isBlank() && busca.length() == 0 && visto.totalLidas() == 0)
			par("Dica: comece por O Livro dos Espíritos.", AZUL);
	}

	// ---------------------------------------------------------------- Obra

	private void initObra(int topo, int base) {
		Catalogo.Obra o = Catalogo.get(obraId);
		if (o == null) {
			modo = Modo.CATALOGO;
			initCatalogo(topo, base);
			return;
		}
		ProgressoBiblioteca p = visto;
		int rw = Math.min(170, pw * 2 / 5);
		int rx = px + pw - rw - 6;
		List<String> rot = new ArrayList<>();
		String marcador = p.marcadores().get(o.id());
		for (Catalogo.Secao s : o.secoes())
			rot.add((p.lida(o.id(), s.id()) ? "✔ " : "") + (s.id().equals(marcador) ? "★ " : "") + s.titulo());
		lista(rot, i -> abrirSecao(o.id(), o.secoes().get(i).id()), rx, topo, rw, base - topo);

		botao("◀ Catálogo", px + 6, base + 3, 70, b -> ir(Modo.CATALOGO));
		Button cont = botao("Continuar", px + 78, base + 3, 62, b -> abrirSecao(o.id(), marcador));
		cont.active = marcador != null && o.secao(marcador) != null;
		Button q = botao("Quiz (" + o.quiz().size() + ")", px + 142, base + 3, 54, b -> iniciarQuiz(o.quiz(), Modo.OBRA));
		q.active = !o.quiz().isEmpty();
		if (nota.isEmpty())
			nota = p.notas().getOrDefault(o.id(), "");
		EditBox caixa = new EditBox(font, px + 200, base + 3, pw - 200 - 56, 16, Component.literal("Nota pessoal"));
		caixa.setMaxLength(ProgressoBiblioteca.MAX_NOTA);
		caixa.setHint(Component.literal("Sua nota pessoal…"));
		caixa.setValue(nota);
		caixa.setResponder(s -> nota = s);
		addRenderableWidget(caixa);
		botao("Salvar", px + pw - 54, base + 3, 48, b -> enviar(PacotesBiblioteca.NOTA, o.id(), nota, 0));

		areaTexto(px + 6, topo, rx - px - 10, base - topo);
		par(o.titulo(), OURO);
		par(o.autoria() + " · " + o.ano(), BRANCO);
		par("Série: " + o.serie(), CINZA);
		par("Assuntos: " + String.join(", ", o.assuntos()), CINZA);
		titulo("Direitos: " + o.direitos().rotulo, o.direitos().cor);
		par(o.notaDireitos(), CINZA);
		titulo("Edição de referência", OURO);
		par(o.edicao(), BRANCO);
		par(o.traducao(), CINZA);
		titulo("Contexto histórico e editorial", OURO);
		par(o.contexto(), BRANCO);
		if (!o.reflexoes().isEmpty()) {
			titulo("Para refletir", OURO);
			for (String r : o.reflexoes())
				par("• " + r, AZUL);
		}
		if (!o.leiaMais().isEmpty()) {
			titulo("Aprofunde", OURO);
			par(o.leiaMais(), BRANCO);
		}
		if (o.temItem())
			par("Esta obra também existe como livro no jogo (aba criativa / receitas). Agache + clique com o livro para abrir esta ficha.", CINZA);
		titulo("Seu progresso", OURO);
		par(p.lidasDa(o.id()) + " de " + o.secoes().size() + " seções lidas.", VERDE);
	}

	private void abrirSecao(String obra, String secao) {
		Catalogo.Obra o = Catalogo.get(obra);
		if (o == null || o.secoes().isEmpty())
			return;
		obraId = obra;
		nota = "";
		int i = 0;
		for (int k = 0; k < o.secoes().size(); k++)
			if (o.secoes().get(k).id().equals(secao))
				i = k;
		secaoIdx = i;
		enviar(PacotesBiblioteca.LER, obra, o.secoes().get(i).id(), 0);
		ir(Modo.SECAO);
	}

	private void initSecao(int topo, int base) {
		Catalogo.Obra o = Catalogo.get(obraId);
		if (o == null) {
			modo = Modo.CATALOGO;
			initCatalogo(topo, base);
			return;
		}
		Catalogo.Secao s = o.secoes().get(secaoIdx);
		botao("◀ Sumário", px + 6, base + 3, 70, b -> ir(Modo.OBRA));
		Button ant = botao("◀ Anterior", px + 80, base + 3, 70, b -> abrirSecao(o.id(), o.secoes().get(secaoIdx - 1).id()));
		ant.active = secaoIdx > 0;
		Button prox = botao("Próxima ▶", px + 154, base + 3, 70, b -> abrirSecao(o.id(), o.secoes().get(secaoIdx + 1).id()));
		prox.active = secaoIdx < o.secoes().size() - 1;
		botao("★ Marcar página", px + 228, base + 3, 90, b -> enviar(PacotesBiblioteca.MARCAR, o.id(), s.id(), 0));

		areaTexto(px + 6, topo, pw - 12, base - topo);
		par(o.titulo() + " · " + (secaoIdx + 1) + "/" + o.secoes().size(), CINZA);
		par(s.titulo(), OURO);
		par("[" + s.tipo().rotulo + "]", s.tipo().cor);
		linhas.add(new Linha(FormattedCharSequence.EMPTY, BRANCO));
		for (String paragrafo : s.texto().split("\n"))
			par(paragrafo.isEmpty() ? " " : paragrafo, BRANCO);
		if (!s.referencia().isEmpty()) {
			linhas.add(new Linha(FormattedCharSequence.EMPTY, BRANCO));
			par("Referência: " + s.referencia(), AZUL);
		}
		par("Direitos: " + o.direitos().rotulo, o.direitos().cor);
	}

	// ---------------------------------------------------------------- Quiz

	private void iniciarQuiz(List<Catalogo.Pergunta> perguntas, Modo voltar) {
		quiz = perguntas;
		quizIdx = 0;
		resposta = -1;
		voltarQuiz = voltar;
		ir(Modo.QUIZ);
	}

	private void initQuiz(int topo, int base) {
		if (quiz.isEmpty()) {
			modo = voltarQuiz;
			rebuildWidgets();
			return;
		}
		Catalogo.Pergunta q = quiz.get(quizIdx);
		areaTexto(px + 6, topo, pw - 12, 70);
		par("Quiz opcional · pergunta " + (quizIdx + 1) + " de " + quiz.size() + (visto.acertou(q.id()) ? " · ✔ já acertada" : ""), CINZA);
		par(q.enunciado(), OURO);
		if (resposta >= 0) {
			boolean certa = resposta == q.correta();
			par(certa ? "Isso mesmo!" : "Não foi desta vez — e tudo bem, errar faz parte do estudo.", certa ? VERDE : 0xFFFFB060);
			par(q.explicacao(), BRANCO);
			par("Fonte: " + q.referencia(), AZUL);
		}
		int y = topo + 74;
		for (int i = 0; i < q.opcoes().size(); i++) {
			int op = i;
			String marca = resposta < 0 ? "" : (i == q.correta() ? "✔ " : (i == resposta ? "✘ " : ""));
			Button b = botao(marca + q.opcoes().get(i), px + 20, y + i * 19, pw - 40, x -> {
				resposta = op;
				enviar(PacotesBiblioteca.RESPONDER, q.id(), "", op);
				rebuildWidgets();
			});
			b.active = resposta < 0;
		}
		botao("◀ Voltar", px + 6, base + 3, 70, b -> ir(voltarQuiz));
		Button prox = botao("Próxima ▶", px + pw - 86, base + 3, 80, b -> {
			quizIdx++;
			resposta = -1;
			rebuildWidgets();
		});
		prox.active = resposta >= 0 && quizIdx < quiz.size() - 1;
	}

	// ---------------------------------------------------------------- Trilhas

	private void initTrilhas(int topo, int base) {
		List<String> ids = new ArrayList<>(Trilhas.TRILHAS.keySet());
		int lw = pw / 2;
		Trilhas.Trilha t = Trilhas.TRILHAS.get(trilhaId);
		botao("Trilha: " + t.titulo() + " ▶", px + 6, topo, lw - 6, b -> {
			trilhaId = ids.get((ids.indexOf(trilhaId) + 1) % ids.size());
			pagina = 0;
			rebuildWidgets();
		});
		List<Trilhas.Modulo> mods = Trilhas.daTrilha(trilhaId);
		List<String> rot = new ArrayList<>();
		for (Trilhas.Modulo m : mods)
			rot.add((visto.concluiu(m.id()) ? "✔ " : "") + m.titulo());
		lista(rot, i -> {
			moduloId = mods.get(i).id();
			ir(Modo.MODULO);
		}, px + 6, topo + 19, lw - 6, base - topo - 19);
		areaTexto(px + lw + 6, topo, pw - lw - 12, base - topo);
		par(t.titulo(), OURO);
		par(t.descricao(), BRANCO);
		long feitos = mods.stream().filter(m -> visto.concluiu(m.id())).count();
		par(feitos + " de " + mods.size() + " módulos estudados.", VERDE);
		titulo("Como funciona", OURO);
		par("Cada módulo é curto: uma situação do jogo, uma explicação breve, a fonte, um exemplo, uma pergunta para refletir (não é prova) e onde "
				+ "aprofundar. Nada é obrigatório. Estudar registra conhecimento, que é diferente de moralidade.", CINZA);
	}

	private void initModulo(int topo, int base) {
		Trilhas.Modulo m = Trilhas.MODULOS.get(moduloId);
		if (m == null) {
			modo = Modo.TRILHAS;
			initTrilhas(topo, base);
			return;
		}
		boolean feito = visto.concluiu(m.id());
		botao("◀ Trilhas", px + 6, base + 3, 64, b -> ir(Modo.TRILHAS));
		Button c = botao(feito ? "✔ Estudado" : "Marcar como estudado", px + 74, base + 3, 120, b -> {
			enviar(PacotesBiblioteca.CONCLUIR, m.id(), "", 0);
			b.active = false;
		});
		c.active = !feito;
		Button q = botao("Quiz", px + 198, base + 3, 44, b -> iniciarQuiz(List.of(m.pergunta()), Modo.MODULO));
		q.active = m.pergunta() != null;
		Button ler = botao("Ler a obra", px + 246, base + 3, 70, b -> {
			obraId = m.aprofundamento();
			ir(Modo.OBRA);
		});
		ler.active = Catalogo.get(m.aprofundamento()) != null;

		areaTexto(px + 6, topo, pw - 12, base - topo);
		par(Trilhas.TRILHAS.get(m.trilha()).titulo() + " · " + m.titulo(), OURO);
		titulo("Situação", AZUL);
		par(m.situacao(), BRANCO);
		titulo("Explicação", AZUL);
		par(m.explicacao(), BRANCO);
		titulo("Fonte", AZUL);
		par(m.fonte(), CINZA);
		titulo("Exemplo", AZUL);
		par(m.exemplo(), BRANCO);
		titulo("Pergunta para refletir", AZUL);
		par(m.reflexao(), OURO);
		titulo("Para aprender mais", AZUL);
		Catalogo.Obra o = Catalogo.get(m.aprofundamento());
		par(o == null ? "-" : o.titulo() + " (botão \"Ler a obra\").", BRANCO);
		par("[Explicação do mod, com referência às obras; não é citação literal.]", CINZA);
	}

	// ---------------------------------------------------------------- Glossário

	private void initGlossario(int topo, int base) {
		List<Glossario.Termo> termos = new ArrayList<>(Glossario.TERMOS.values());
		int lw = pw * 2 / 5;
		List<String> rot = new ArrayList<>();
		for (Glossario.Termo t : termos)
			rot.add(t.nome());
		lista(rot, i -> {
			termoId = termos.get(i).id();
			rolagem = 0;
			rebuildWidgets();
		}, px + 6, topo, lw - 6, base - topo);
		areaTexto(px + lw + 6, topo, pw - lw - 12, base - topo);
		Glossario.Termo t = termoId == null ? null : Glossario.TERMOS.get(termoId);
		if (t == null) {
			par("Glossário", OURO);
			par("Escolha um termo à esquerda. Cada definição é uma explicação do mod, com a referência e a origem do termo (obras básicas, obra "
					+ "mediúnica posterior, prática do movimento espírita ou termo do jogo).", BRANCO);
		} else {
			par(t.nome(), OURO);
			par(t.definicao(), BRANCO);
			titulo("Referência", AZUL);
			par(t.referencia(), CINZA);
			titulo("Origem", AZUL);
			par(t.origem().rotulo, CINZA);
		}
	}

	// ---------------------------------------------------------------- Evangelho no Lar

	private void initEvangelho(int topo, int base) {
		areaTexto(px + 6, topo, pw - 12, base - topo - 22);
		par("Evangelho no Lar (atividade opcional)", OURO);
		par("Um encontro simples, sozinho ou com amigos que estejam perto: prece inicial com palavras próprias, leitura de um trecho de O Evangelho "
				+ "segundo o Espiritismo (o jogo mostra um resumo com referência), conversa sobre o tema e prece final. Dura cerca de um minuto.", BRANCO);
		titulo("O que NÃO é", OURO);
		par("Não é ritual nem tem fórmula, horário mágico ou objeto obrigatório. Não dá poderes, efeitos ou proteção sobrenatural. Ninguém é obrigado "
				+ "a participar, e sair no meio não tem penalidade.", CINZA);
		titulo("No multiplayer", OURO);
		par("Quem estiver a até 12 blocos recebe um convite clicável e escolhe se quer participar. Cada um tem o próprio progresso.", CINZA);
		par("Você já participou " + visto.evangelhos() + " vez(es).", VERDE);
		botao("Iniciar aqui", px + 6, base - 19, 110, b -> {
			enviar(PacotesBiblioteca.EVANGELHO, "iniciar", "", 0);
			onClose();
		});
		botao("Participar do mais próximo", px + 120, base - 19, 160, b -> {
			enviar(PacotesBiblioteca.EVANGELHO, "participar", "", 0);
			onClose();
		});
		botao("Sair", px + 284, base - 19, 60, b -> {
			enviar(PacotesBiblioteca.EVANGELHO, "sair", "", 0);
			onClose();
		});
	}

	// ---------------------------------------------------------------- Progresso

	private void initProgresso(int topo, int base) {
		areaTexto(px + 6, topo, pw - 12, base - topo);
		ProgressoBiblioteca p = visto;
		int secoes = Catalogo.OBRAS.values().stream().mapToInt(o -> o.secoes().size()).sum();
		int perguntas = (int) (Catalogo.OBRAS.values().stream().mapToLong(o -> o.quiz().size()).sum()
				+ Trilhas.MODULOS.values().stream().filter(m -> m.pergunta() != null).count());
		par("Seu progresso de estudo", OURO);
		par("Conhecimento: " + p.conhecimento() + " pontos", VERDE);
		par("Seções lidas: " + p.totalLidas() + " de " + secoes, BRANCO);
		par("Perguntas acertadas: " + p.acertos().size() + " de " + perguntas, BRANCO);
		par("Módulos estudados: " + p.modulos().size() + " de " + Trilhas.MODULOS.size(), BRANCO);
		par("Evangelho no Lar: " + p.evangelhos(), BRANCO);
		titulo("Conhecimento não é moralidade", OURO);
		par("Estudar mostra o que você aprendeu, não quanto você é \"bom\". Conhecimento não garante elevação moral, e a barra de moralidade é só uma "
				+ "mecânica abstrata do jogo. Seu progresso é só seu: não há ranking.", CINZA);
		if (!p.notas().isEmpty()) {
			titulo("Suas notas", OURO);
			p.notas().forEach((obra, texto) -> {
				Catalogo.Obra o = Catalogo.get(obra);
				par((o == null ? obra : o.titulo()) + ": " + texto, AZUL);
			});
		}
	}

	// ================================================================ Desenho e entrada

	@Override
	public void tick() {
		super.tick();
		// O servidor confirmou alguma ação (attachment sincronizado): refaz a tela mantendo a rolagem.
		if (prog() != visto) {
			int r = rolagem;
			rebuildWidgets();
			rolagem = Math.min(r, maxRolagem());
		}
	}

	private int linhasVisiveis() {
		return Math.max(1, th / 10);
	}

	private int maxRolagem() {
		return Math.max(0, linhas.size() - linhasVisiveis());
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (x >= tx && x <= tx + tw && y >= ty && y <= ty + th) {
			rolagem = Math.max(0, Math.min(maxRolagem(), rolagem - (int) Math.signum(scrollY) * 3));
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractBackground(g, mouseX, mouseY, a);
		g.fill(px, py, px + pw, py + ph, 0xE0181410);
		g.outline(px, py, pw, ph, 0xFF8B6A3E);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractRenderState(g, mouseX, mouseY, partial);
		if (tw <= 0 || linhas.isEmpty())
			return;
		g.fill(tx, ty, tx + tw, ty + th, 0x40000000);
		g.enableScissor(tx, ty, tx + tw, ty + th);
		int y = ty + 3;
		for (int i = rolagem; i < linhas.size() && y < ty + th; i++, y += 10)
			g.text(font, linhas.get(i).seq(), tx + 3, y, linhas.get(i).cor());
		g.disableScissor();
		if (maxRolagem() > 0) {
			int barra = Math.max(10, th * linhasVisiveis() / linhas.size());
			int pos = ty + (th - barra) * rolagem / maxRolagem();
			g.fill(tx + tw - 3, pos, tx + tw - 1, pos + barra, 0xFF8B6A3E);
			if (rolagem < maxRolagem())
				g.text(font, "▼ role", tx + tw - 36, ty + th - 10, CINZA);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
