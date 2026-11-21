package net.mcreator.modespiritismominecraft.espiritismo.biblioteca;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.mcreator.modespiritismominecraft.espiritismo.Jornada;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evangelho no Lar (PROMPT_MESTRE §12.4): encontro OPCIONAL de leitura e reflexão, sozinho ou com amigos por perto.
 * <p>
 * Não é ritual: não há fórmula, horário certo, objeto obrigatório, efeito de poção nem proteção sobrenatural. O jogo
 * só registra o encontro no progresso de conhecimento (não mexe na moralidade). Ninguém é obrigado a participar: os
 * jogadores próximos recebem um convite clicável e podem sair a qualquer momento sem penalidade.
 * <p>
 * Etapas: prece inicial (palavras próprias) → leitura (resumo do mod de um trecho de O Evangelho segundo o Espiritismo,
 * com referência) → conversa/reflexão → prece final e encerramento.
 */
@EventBusSubscriber
public final class EvangelhoNoLar {
	private EvangelhoNoLar() {
	}

	/** Duração de cada etapa (ticks). Total: ~1 minuto. */
	public static final int TICKS_ETAPA = 300;
	/** Distância máxima (blocos) para convidar e para continuar participando. */
	public static final double RAIO = 12.0;
	public static final int ETAPAS = 4;

	/** Leituras: resumo do mod + referência (ESE, original de Kardec em domínio público; texto aqui é paráfrase própria). */
	public record Leitura(String titulo, String resumo, String referencia, String pergunta) {
	}

	public static final List<Leitura> LEITURAS = List.of(
			new Leitura("Amar o próximo como a si mesmo", "Jesus resume a lei em amar a Deus e ao próximo. Fazer ao outro o que gostaríamos que nos fizessem é a regra prática.",
					"ESE, cap. XI", "O que você gostaria que fizessem por você nesta semana, e pode fazer por alguém?"),
			new Leitura("Perdoar setenta vezes sete", "O perdão não tem limite de vezes. Quem guarda mágoa sofre duas vezes; quem perdoa abre caminho para a paz.",
					"ESE, cap. X", "Há alguém com quem você gostaria de se reconciliar?"),
			new Leitura("A mão esquerda e a direita", "O bem feito em silêncio, sem buscar elogio, é o que tem verdadeiro valor.", "ESE, cap. XIII",
					"Que pequeno bem você pode fazer sem que ninguém saiba?"),
			new Leitura("Bem-aventurados os aflitos", "As dificuldades da vida podem ser ocasião de aprendizado; a resignação ativa não é desistir, é seguir com esperança.",
					"ESE, cap. V", "Que dificuldade recente também lhe ensinou algo?"),
			new Leitura("Fora da caridade não há salvação", "A caridade une todos os que fazem o bem, qualquer que seja a crença.", "ESE, cap. XV",
					"Como praticar a caridade moral (paciência, escuta) dentro de casa?"),
			new Leitura("A prece", "A prece vale pela sinceridade do pensamento, não pelas palavras decoradas nem por fórmulas.", "ESE, cap. XXVII",
					"Como seria uma prece com as suas próprias palavras?"));

	/** Uma sessão ativa, conduzida por quem a iniciou. Guarda só os jogadores enquanto a sessão dura (~1 min). */
	public static final class Sessao {
		public final UUID anfitriao;
		public final Leitura leitura;
		final Set<Player> participantes = new LinkedHashSet<>();
		public int etapa = 0;
		int ticks = 0;

		Sessao(UUID anfitriao, Leitura leitura) {
			this.anfitriao = anfitriao;
			this.leitura = leitura;
		}

		public List<Player> participantes() {
			return new ArrayList<>(participantes);
		}
	}

	private static final Map<UUID, Sessao> SESSOES = new ConcurrentHashMap<>();

	public static Sessao sessaoDe(Player p) {
		for (Sessao s : SESSOES.values())
			if (s.participantes.contains(p))
				return s;
		return null;
	}

	// ---------------------------------------------------------------- Ações

	/** Começa um Evangelho no Lar onde o jogador está e convida quem estiver por perto. */
	public static Sessao iniciar(Player anfitriao) {
		if (anfitriao.level().isClientSide())
			return null;
		Sessao existente = sessaoDe(anfitriao);
		if (existente != null) {
			msg(anfitriao, ChatFormatting.GRAY, "Você já está num Evangelho no Lar.");
			return existente;
		}
		int n = Math.floorMod(BibliotecaApi.progresso(anfitriao).evangelhos() + (int) (anfitriao.level().getGameTime() / 24000L), LEITURAS.size());
		Sessao s = new Sessao(anfitriao.getUUID(), LEITURAS.get(n));
		s.participantes.add(anfitriao);
		SESSOES.put(anfitriao.getUUID(), s);
		msg(anfitriao, ChatFormatting.GOLD, "Evangelho no Lar: um momento simples de prece, leitura e conversa. Não é ritual e não dá poderes nem proteção; "
				+ "é estudo e convivência. Fique por perto até o fim ou saia quando quiser.");
		for (Player outro : anfitriao.level().players())
			if (outro != anfitriao && outro.distanceTo(anfitriao) <= RAIO && sessaoDe(outro) == null)
				convidar(outro, anfitriao);
		anunciarEtapa(s);
		return s;
	}

	private static void convidar(Player convidado, Player anfitriao) {
		Component link = Component.literal("[participar]").withStyle(st -> st.withColor(ChatFormatting.AQUA).withUnderlined(true)
				.withClickEvent(new ClickEvent.RunCommand("/evangelho_no_lar participar"))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Opcional: você pode ignorar o convite"))));
		convidado.sendSystemMessage(Component.literal(anfitriao.getName().getString() + " convidou você para um Evangelho no Lar (opcional). ")
				.withStyle(ChatFormatting.GRAY).append(link));
	}

	/** Entra na sessão mais próxima (dentro do raio). Retorna a sessão ou null. */
	public static Sessao participarProximo(Player p) {
		if (sessaoDe(p) != null)
			return sessaoDe(p);
		Sessao melhor = null;
		double dist = Double.MAX_VALUE;
		for (Sessao s : SESSOES.values()) {
			Player anf = anfitriao(s);
			if (anf == null || anf.level() != p.level())
				continue;
			double d = anf.distanceTo(p);
			if (d <= RAIO && d < dist) {
				dist = d;
				melhor = s;
			}
		}
		if (melhor == null) {
			msg(p, ChatFormatting.GRAY, "Não há Evangelho no Lar acontecendo perto de você.");
			return null;
		}
		participar(melhor, p);
		return melhor;
	}

	public static void participar(Sessao s, Player p) {
		if (s.participantes.add(p)) {
			for (Player outro : s.participantes)
				msg(outro, ChatFormatting.GRAY, p.getName().getString() + " participa do Evangelho no Lar.");
			msg(p, ChatFormatting.GOLD, "Leitura de hoje: " + s.leitura.titulo() + " (" + s.leitura.referencia() + ").");
		}
	}

	/** Sai da sessão sem nenhuma penalidade. Se o anfitrião sair, a sessão termina para todos (sem penalidade). */
	public static void sair(Player p) {
		Sessao s = sessaoDe(p);
		if (s == null)
			return;
		if (s.anfitriao.equals(p.getUUID())) {
			for (Player outro : s.participantes)
				msg(outro, ChatFormatting.GRAY, "O Evangelho no Lar foi encerrado. Vocês podem fazê-lo outro dia, quando quiserem.");
			SESSOES.remove(s.anfitriao);
		} else {
			s.participantes.remove(p);
			msg(p, ChatFormatting.GRAY, "Você saiu do Evangelho no Lar. Tudo bem: a participação é livre.");
		}
	}

	private static Player anfitriao(Sessao s) {
		for (Player p : s.participantes)
			if (p.getUUID().equals(s.anfitriao))
				return p;
		return null;
	}

	// ---------------------------------------------------------------- Andamento

	/** Avança uma etapa (usado pelo tick e pelos testes). Retorna true se a sessão terminou. */
	public static boolean avancar(Sessao s) {
		s.etapa++;
		s.ticks = 0;
		if (s.etapa >= ETAPAS) {
			concluir(s);
			return true;
		}
		anunciarEtapa(s);
		return false;
	}

	private static void anunciarEtapa(Sessao s) {
		String texto = switch (s.etapa) {
			case 0 -> "1/4 · Prece inicial: alguém pode orar com as próprias palavras, em voz alta ou em silêncio. Não há fórmula. (ESE, cap. XXVII)";
			case 1 -> "2/4 · Leitura: " + s.leitura.titulo() + ". Resumo do mod: " + s.leitura.resumo() + " (" + s.leitura.referencia()
					+ "; leia o texto completo no seu exemplar.)";
			case 2 -> "3/4 · Conversa: " + s.leitura.pergunta() + " Cada um comenta se quiser; ninguém é obrigado a falar.";
			default -> "4/4 · Prece final: pensamentos de paz para quem precisa. Algumas famílias deixam um copo de água para fluidificar; é só apoio, "
					+ "não remédio nem proteção.";
		};
		for (Player p : s.participantes)
			msg(p, ChatFormatting.YELLOW, texto);
	}

	private static void concluir(Sessao s) {
		SESSOES.remove(s.anfitriao);
		for (Player p : s.participantes) {
			ProgressoBiblioteca prog = BibliotecaApi.progresso(p);
			p.setData(RegistroBiblioteca.PROGRESSO, prog.comEvangelho(p.level().getGameTime() / 24000L));
			msg(p, ChatFormatting.GREEN, "Evangelho no Lar concluído. Obrigado pela companhia! (Registrado no seu progresso de estudo.)");
			Jornada.conquista(p, "evangelho_no_lar");
		}
	}

	/** Remove participantes que saíram, morreram, mudaram de dimensão ou se afastaram; termina sessões vazias. */
	static void limpar(Sessao s) {
		Player anf = anfitriao(s);
		Iterator<Player> it = s.participantes.iterator();
		while (it.hasNext()) {
			Player p = it.next();
			boolean longe = anf != null && p != anf && (p.level() != anf.level() || p.distanceTo(anf) > RAIO * 1.5);
			if (p.isRemoved() || !p.isAlive() || longe) {
				it.remove();
				if (!p.isRemoved())
					msg(p, ChatFormatting.GRAY, "Você se afastou do Evangelho no Lar. Pode voltar quando quiser.");
			}
		}
		if (anfitriao(s) == null) {
			for (Player p : s.participantes)
				msg(p, ChatFormatting.GRAY, "O Evangelho no Lar terminou antes do fim. Sem problema: podem fazê-lo outro dia.");
			SESSOES.remove(s.anfitriao);
		}
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post event) {
		if (SESSOES.isEmpty())
			return;
		for (Sessao s : new ArrayList<>(SESSOES.values())) {
			limpar(s);
			if (!SESSOES.containsKey(s.anfitriao))
				continue;
			if (++s.ticks >= TICKS_ETAPA)
				avancar(s);
		}
	}

	@SubscribeEvent
	public static void saiu(PlayerEvent.PlayerLoggedOutEvent event) {
		sair(event.getEntity());
	}

	/** Só para testes: encerra todas as sessões. */
	public static void limparTudo() {
		SESSOES.clear();
	}

	private static void msg(Player p, ChatFormatting cor, String texto) {
		if (p instanceof ServerPlayer sp && sp.connection == null)
			return;
		p.sendSystemMessage(Component.literal(texto).withStyle(cor));
	}
}
