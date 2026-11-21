package net.mcreator.modespiritismominecraft.espiritismo.dimensao;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.estado.Estados;
import net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.espiritismo.ClasseEspiritual;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Perturbação/confusão temporária (PROMPT §5.5 e §5.8): um número de 0 a 100 no jogador
 * ({@link RegistroDimensao#PERTURBACAO}, sincronizado com o dono para os efeitos visuais/sonoros no cliente).
 * <p>
 * Sobe devagar no Umbral longe dos postos (mais com moralidade baixa, um pouco mais para médiuns) e ao se afastar
 * de Nosso Lar sem acompanhamento; desce com acolhimento, repouso, conversa, prece e orientação (perto de um posto).
 * Não depende de ajudar outros Espíritos ("paciente não ajuda paciente"). Não é afetada pela água fluidificada.
 * <p>
 * Histerese: ≥ {@link #LIMIAR_PERTURBADO} → NO_UMBRAL_PERTURBADO; ≤ {@link #LIMIAR_ORIENTADO} → NO_UMBRAL_ORIENTADO.
 */
public final class Perturbacao {
	public static final int MAX = 100;
	public static final int LIMIAR_PERTURBADO = 50;
	public static final int LIMIAR_ORIENTADO = 25;
	public static final int ALIVIO_PRECE = 12;
	public static final int ALIVIO_CONVERSA = 10;
	public static final int ALIVIO_REPOUSO = 20;
	public static final int TETO_RECUSA = 90;

	/** Preferência de efeitos informada pelo cliente: 0 completo, 1 reduzido, 2 desligado (só textos essenciais). */
	private static final Map<UUID, Integer> PREFERENCIA = new ConcurrentHashMap<>();

	private Perturbacao() {
	}

	public static int get(Player p) {
		return p.getData(RegistroDimensao.PERTURBACAO);
	}

	public static void set(Player p, int v) {
		int n = Math.clamp(v, 0, MAX);
		if (n != get(p))
			p.setData(RegistroDimensao.PERTURBACAO, n);
	}

	/** Alívio (prece, conversa, repouso, acolhimento). Devolve quanto reduziu. */
	public static int aliviar(Player p, int quanto) {
		int antes = get(p);
		set(p, antes - quanto);
		atualizarEstado(p);
		return antes - get(p);
	}

	public static void agravar(Player p, int quanto, int teto) {
		int antes = get(p);
		if (antes < teto)
			set(p, Math.min(teto, antes + quanto));
		atualizarEstado(p);
	}

	/** Nível para o qual a perturbação tende, no Umbral, longe dos postos. Regra pura. */
	public static int alvo(int moral, boolean medium) {
		return Math.clamp(75 - moral, 0, 80) + (medium ? 10 : 0);
	}

	/** Perturbação ao despertar no Umbral. Regra pura. */
	public static int inicialNoUmbral(int moral) {
		return Math.clamp(85 - moral, 20, 90);
	}

	public static boolean medium(Player p) {
		return Moralidade.classe(p) == ClasseEspiritual.MEDIUM;
	}

	public static void preferencia(Player p, int modo) {
		PREFERENCIA.put(p.getUUID(), Math.clamp(modo, 0, 2));
	}

	public static int preferencia(Player p) {
		return PREFERENCIA.getOrDefault(p.getUUID(), 0);
	}

	static void esquecer(Player p) {
		PREFERENCIA.remove(p.getUUID());
	}

	/** Ajusta NO_UMBRAL_PERTURBADO/ORIENTADO conforme a perturbação (com histerese). */
	public static void atualizarEstado(Player p) {
		EstadoEspiritual e = Estados.get(p);
		int v = get(p);
		if (e == EstadoEspiritual.NO_UMBRAL_ORIENTADO && v >= LIMIAR_PERTURBADO) {
			Estados.transicionar(p, EstadoEspiritual.NO_UMBRAL_PERTURBADO, "perturbação alta");
			p.sendSystemMessage(Component.literal("Os pensamentos se embaralham... É um estado passageiro. Você pode pedir ajuda a qualquer momento (tecla H ou /espiritismo_socorro pedir).")
					.withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
		} else if (e == EstadoEspiritual.NO_UMBRAL_PERTURBADO && v <= LIMIAR_ORIENTADO) {
			Estados.transicionar(p, EstadoEspiritual.NO_UMBRAL_ORIENTADO, "perturbação baixa");
			p.sendSystemMessage(Component.literal("A mente clareia. Você se sente mais orientado — e pode, com prudência, buscar o caminho do Posto de Socorro.").withStyle(ChatFormatting.AQUA));
		}
	}

	// ---------------------------------------------------------------- Tick (a cada segundo, no Umbral)

	private static final List<String> ECOS = List.of("...ninguém vem...", "...volte... volte...", "...você se lembra?...", "...por aqui... não, por ali...", "...a luz... onde está a luz?...");
	/** Para o médium: vozes ambíguas (umas confusas, outras de orientação). As seguras vêm identificadas pelos socorristas. */
	private static final List<String> VOZES_MEDIUM = List.of("Uma voz insiste: não confie em ninguém, siga sozinho.", "Uma voz suave: procure a luz do posto; lá há quem o ampare.",
			"Uma voz irritada: eles não ligam para você.", "Uma voz serena: respire. Ore. Peça ajuda.", "Uma voz confusa repete palavras sem sentido.");

	static void tickUmbral(ServerPlayer p) {
		EstadoEspiritual e = Estados.get(p);
		if (e != EstadoEspiritual.NO_UMBRAL_PERTURBADO && e != EstadoEspiritual.NO_UMBRAL_ORIENTADO)
			return;
		ServerLevel level = (ServerLevel) p.level();
		boolean med = medium(p);
		double dPosto = ConstrucoesUmbral.distanciaPosto(level, p.blockPosition());
		int v = get(p);
		long t = level.getGameTime() / 20;
		if (dPosto <= 20) {
			// Orientação/acolhimento perto do posto
			if (v > 0)
				set(p, v - 2);
		} else {
			int alvo = alvo(Moralidade.get(p), med);
			if (v < alvo && t % (med ? 3 : 5) == 0)
				set(p, v + 1);
			else if (v > alvo && t % 10 == 0)
				set(p, v - 1);
		}
		atualizarEstado(p);
		v = get(p);
		int pref = preferencia(p);
		if (pref == 2 || v < 35)
			return;
		// Mensagens incompletas / ecos (texto, sem som nem flash). Médium: mais estímulos, ambíguos.
		int intervalo = (med ? 20 : 45) * (pref == 1 ? 2 : 1);
		if (p.getRandom().nextInt(intervalo) == 0) {
			if (med)
				p.sendSystemMessage(Component.literal(VOZES_MEDIUM.get(p.getRandom().nextInt(VOZES_MEDIUM.size()))).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			else
				p.sendOverlayMessage(Component.literal(ECOS.get(p.getRandom().nextInt(ECOS.size()))).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		}
		if (med && Moralidade.pronto(p, "umb_dica_medium", 20 * 60 * 5))
			p.sendSystemMessage(Component.literal(
					"Médium: sua percepção está mais aberta e difícil de filtrar. Nem toda voz merece confiança (LM, cap. XXIV). Orientação segura vem dos socorristas identificados (nome em verde). Prece, repouso e estudo estabilizam.")
					.withStyle(ChatFormatting.LIGHT_PURPLE));
	}
}
