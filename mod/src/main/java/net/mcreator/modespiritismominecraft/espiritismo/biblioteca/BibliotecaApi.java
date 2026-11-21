package net.mcreator.modespiritismominecraft.espiritismo.biblioteca;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.mcreator.modespiritismominecraft.espiritismo.Jornada;

/**
 * API pública da Biblioteca Espírita, para NPCs bibliotecários, a Biblioteca central de Nosso Lar, missões etc.
 * <p>
 * Tudo aqui roda no servidor. Nada altera a moralidade: leitura, quizzes e trilhas registram CONHECIMENTO
 * (PROMPT_MESTRE §7: conhecimento ≠ moralidade).
 */
public final class BibliotecaApi {
	private BibliotecaApi() {
	}

	// ---------------------------------------------------------------- Abrir a tela

	/**
	 * Abre a ficha da obra {@code obraId} (ids de {@link Catalogo#OBRAS}, inclusive as obras-item de {@code itens.Obras}).
	 * ID vazio, nulo ou desconhecido abre o catálogo. Retorna {@code true} se a obra existia.
	 */
	public static boolean abrir(ServerPlayer player, String obraId) {
		if (obraId == null || Catalogo.get(obraId) == null) {
			abrirCatalogo(player);
			return obraId == null || obraId.isEmpty();
		}
		enviar(player, "obra", obraId);
		return true;
	}

	public static void abrirCatalogo(ServerPlayer player) {
		enviar(player, "catalogo", "");
	}

	/** Abre um módulo de trilha (ou a lista de trilhas, se o id for desconhecido). */
	public static void abrirTrilha(ServerPlayer player, String modulo) {
		enviar(player, "trilha", Trilhas.MODULOS.containsKey(modulo) ? modulo : "");
	}

	public static void abrirGlossario(ServerPlayer player, String termo) {
		enviar(player, "glossario", termo == null ? "" : termo);
	}

	public static void abrirEvangelhoNoLar(ServerPlayer player) {
		enviar(player, "evangelho", "");
	}

	private static void enviar(ServerPlayer player, String aba, String alvo) {
		if (player.connection == null)
			return; // jogador falso (testes) ou desconectado
		PacketDistributor.sendToPlayer(player, new PacotesBiblioteca.Abrir(aba, alvo));
	}

	// ---------------------------------------------------------------- Progresso

	public static ProgressoBiblioteca progresso(Player player) {
		return player.getData(RegistroBiblioteca.PROGRESSO);
	}

	private static void salvar(Player player, ProgressoBiblioteca p) {
		player.setData(RegistroBiblioteca.PROGRESSO, p);
	}

	/** Pontos de conhecimento (seções lidas, acertos, módulos, Evangelho no Lar). Independente da moralidade. */
	public static int conhecimento(Player player) {
		return progresso(player).conhecimento();
	}

	/** Registra que o jogador leu a seção (e move o marcador para ela). Retorna false se a obra/seção não existe. */
	public static boolean registrarLeitura(Player player, String obraId, String secaoId) {
		Catalogo.Obra obra = Catalogo.get(obraId);
		if (obra == null || obra.secao(secaoId) == null)
			return false;
		ProgressoBiblioteca antes = progresso(player);
		salvar(player, antes.comLeitura(obraId, secaoId));
		// Leitura pela tela da biblioteca também conta para a jornada do recém-chegado e as missões da Amélie.
		net.mcreator.modespiritismominecraft.espiritismo.populacao.PopulacaoApi.registrarLeitura(player, obraId);
		if (antes.totalLidas() == 0)
			Jornada.conquista(player, "biblioteca_leitor");
		return true;
	}

	/** Marca (ou desmarca, com seção vazia) a página da obra. */
	public static boolean marcar(Player player, String obraId, String secaoId) {
		Catalogo.Obra obra = Catalogo.get(obraId);
		if (obra == null || (secaoId != null && !secaoId.isEmpty() && obra.secao(secaoId) == null))
			return false;
		salvar(player, progresso(player).comMarcador(obraId, secaoId));
		return true;
	}

	/** Nota pessoal do jogador sobre a obra (até {@link ProgressoBiblioteca#MAX_NOTA} caracteres; vazia apaga). */
	public static boolean anotar(Player player, String obraId, String texto) {
		if (Catalogo.get(obraId) == null)
			return false;
		salvar(player, progresso(player).comNota(obraId, texto));
		return true;
	}

	/**
	 * Registra a resposta a uma pergunta de quiz (de obra ou de trilha). Retorna {@code true} se acertou. Cada acerto
	 * conta uma única vez no conhecimento; errar não tira nada (quizzes são opcionais e não punitivos).
	 */
	public static boolean responder(Player player, String perguntaId, int opcao) {
		Catalogo.Pergunta p = Catalogo.pergunta(perguntaId);
		if (p == null || opcao < 0 || opcao >= p.opcoes().size())
			return false;
		boolean certa = opcao == p.correta();
		salvar(player, progresso(player).comResposta(perguntaId, certa));
		return certa;
	}

	/** Marca um módulo de trilha como estudado. Retorna false se não existe. */
	public static boolean concluirModulo(Player player, String modulo) {
		Trilhas.Modulo m = Trilhas.MODULOS.get(modulo);
		if (m == null)
			return false;
		ProgressoBiblioteca antes = progresso(player);
		ProgressoBiblioteca depois = antes.comModulo(modulo);
		salvar(player, depois);
		if (antes.modulos().isEmpty())
			Jornada.conquista(player, "biblioteca_trilha");
		boolean trilhaCompleta = Trilhas.daTrilha(m.trilha()).stream().allMatch(x -> depois.concluiu(x.id()));
		if (trilhaCompleta && !Trilhas.daTrilha(m.trilha()).stream().allMatch(x -> antes.concluiu(x.id())))
			Jornada.conquista(player, "biblioteca_trilha_completa");
		return true;
	}

	/**
	 * Sugestão não intrusiva: uma linha no chat com link para o módulo, só na primeira vez (por jogador) e só se o
	 * módulo ainda não foi estudado. Usado em eventos (dormir, renascer, orar, entrar no Umbral...).
	 */
	public static boolean sugerirModulo(ServerPlayer player, String modulo) {
		Trilhas.Modulo m = Trilhas.MODULOS.get(modulo);
		ProgressoBiblioteca p = progresso(player);
		if (m == null || p.sugeridos().contains(modulo) || p.concluiu(modulo))
			return false;
		salvar(player, p.comSugestao(modulo));
		MutableComponent link = Component.literal("[abrir]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true)
				.withClickEvent(new ClickEvent.RunCommand("/biblioteca trilha " + modulo))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Abre o módulo na Biblioteca (opcional)"))));
		player.sendSystemMessage(Component.literal("Biblioteca: quer entender melhor? Módulo \"" + m.titulo() + "\" ").withStyle(ChatFormatting.GRAY).append(link));
		return true;
	}
}
