package net.mcreator.modespiritismominecraft.espiritismo.estado;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;

import net.mcreator.modespiritismominecraft.espiritismo.dimensao.RegistroDimensao;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.function.BiConsumer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.EnumSet;
import java.util.EnumMap;

import static net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual.*;

/**
 * CONTRATO entre as áreas: consulta e transição do estado espiritual do jogador.
 * Assinaturas estáveis; a área "umbral/estado" implementa as regras de transição.
 * <p>
 * A máquina de estados é explícita: {@link #TRANSICOES} lista, para cada estado, os estados para os quais se pode ir.
 * {@link #transicionar} recusa (devolve false e não muda nada) qualquer transição fora da tabela. Só a migração de
 * saves antigos, a restauração após reconexão e os comandos de administrador usam {@link #forcar}.
 */
public final class Estados {
	/** Versão do esquema persistido (attachment {@code estado_versao}). 0 = save anterior à máquina de estados. */
	public static final int VERSAO = 1;

	/** Ganchos chamados (no servidor) depois de cada mudança de estado: (jogador, estado anterior). */
	public static final List<BiConsumer<Player, EstadoEspiritual>> AO_MUDAR = new CopyOnWriteArrayList<>();

	private static final Map<EstadoEspiritual, Set<EstadoEspiritual>> TRANSICOES = new EnumMap<>(EstadoEspiritual.class);

	static {
		// Encarnado
		permitir(ENCARNADO_ESTAVEL, ENCARNADO_PERTURBADO, DESENCARNANDO, EM_VISITA_ASSISTIDA);
		permitir(ENCARNADO_PERTURBADO, ENCARNADO_ESTAVEL, DESENCARNANDO, EM_VISITA_ASSISTIDA);
		// Passagem: a morte do corpo
		permitir(DESENCARNANDO, RECEM_DESENCARNADO);
		// Despertar: Nosso Lar (acolhimento), Umbral (perturbado ou orientado) ou volta direta à Terra (perturbação breve)
		permitir(RECEM_DESENCARNADO, EM_ACOLHIMENTO, NO_UMBRAL_PERTURBADO, NO_UMBRAL_ORIENTADO, REENCARNANDO);
		// Nosso Lar
		permitir(EM_ACOLHIMENTO, EM_NOSSO_LAR, EM_TRATAMENTO, NO_UMBRAL_ORIENTADO, NO_UMBRAL_PERTURBADO, RECEM_DESENCARNADO);
		permitir(EM_NOSSO_LAR, EM_TRATAMENTO, PRONTO_PARA_REENCARNAR, NO_UMBRAL_ORIENTADO, NO_UMBRAL_PERTURBADO, RECEM_DESENCARNADO);
		permitir(EM_TRATAMENTO, EM_NOSSO_LAR, PRONTO_PARA_REENCARNAR, RECEM_DESENCARNADO);
		// Desdobramento no sono (visita assistida) e o despertar na cama
		permitir(EM_VISITA_ASSISTIDA, ENCARNADO_ESTAVEL, ENCARNADO_PERTURBADO);
		// Umbral
		permitir(NO_UMBRAL_ORIENTADO, NO_UMBRAL_PERTURBADO, EM_RESGATE, EM_ACOLHIMENTO, EM_NOSSO_LAR, RECEM_DESENCARNADO);
		permitir(NO_UMBRAL_PERTURBADO, NO_UMBRAL_ORIENTADO, EM_RESGATE, EM_ACOLHIMENTO, RECEM_DESENCARNADO);
		// Resgate aceito: termina no acolhimento (Posto ou Nosso Lar); cancelado volta ao Umbral
		permitir(EM_RESGATE, EM_ACOLHIMENTO, NO_UMBRAL_PERTURBADO, NO_UMBRAL_ORIENTADO, RECEM_DESENCARNADO);
		// Reencarnação
		permitir(PRONTO_PARA_REENCARNAR, REENCARNANDO, EM_NOSSO_LAR, RECEM_DESENCARNADO);
		permitir(REENCARNANDO, ENCARNADO_ESTAVEL);
	}

	private static void permitir(EstadoEspiritual de, EstadoEspiritual... para) {
		Set<EstadoEspiritual> s = EnumSet.noneOf(EstadoEspiritual.class);
		s.addAll(List.of(para));
		TRANSICOES.put(de, s);
	}

	private Estados() {
	}

	public static EstadoEspiritual get(Player player) {
		return EstadoEspiritual.porIndice(player.getData(Moralidade.ESTADO));
	}

	/** Regra pura: a transição {@code de → para} é válida? (Ficar no mesmo estado é sempre válido.) */
	public static boolean valida(EstadoEspiritual de, EstadoEspiritual para) {
		return de == para || TRANSICOES.getOrDefault(de, Set.of()).contains(para);
	}

	/** Estados para os quais se pode ir a partir de {@code de} (cópia). */
	public static Set<EstadoEspiritual> proximos(EstadoEspiritual de) {
		Set<EstadoEspiritual> s = EnumSet.noneOf(EstadoEspiritual.class);
		s.addAll(TRANSICOES.getOrDefault(de, Set.of()));
		return s;
	}

	/**
	 * Muda o estado. Devolve false se a transição for inválida (o estado não muda).
	 * Só no servidor. Mudar para o mesmo estado é aceito e não faz nada.
	 */
	public static boolean transicionar(Player player, EstadoEspiritual novo, String motivo) {
		if (player.level().isClientSide())
			return false;
		EstadoEspiritual atual = get(player);
		if (atual == novo)
			return true;
		if (!valida(atual, novo)) {
			ModEspiritismoMinecraftMod.LOGGER.debug("Estados: transição recusada {} -> {} ({}) para {}", atual, novo, motivo, player.getName().getString());
			return false;
		}
		aplicar(player, atual, novo, motivo);
		return true;
	}

	/**
	 * Define o estado sem validar a tabela. Uso restrito: migração de saves antigos, restauração após reconexão
	 * (anti-softlock) e comandos de administrador.
	 */
	public static void forcar(Player player, EstadoEspiritual novo, String motivo) {
		if (player.level().isClientSide())
			return;
		EstadoEspiritual atual = get(player);
		if (atual == novo && player.getData(RegistroDimensao.ESTADO_VERSAO) >= VERSAO)
			return;
		aplicar(player, atual, novo, "forçado: " + motivo);
	}

	private static void aplicar(Player player, EstadoEspiritual atual, EstadoEspiritual novo, String motivo) {
		player.setData(Moralidade.ESTADO, novo.ordinal());
		player.setData(RegistroDimensao.ESTADO_DESDE, player.level().getGameTime());
		if (player.getData(RegistroDimensao.ESTADO_VERSAO) < VERSAO)
			player.setData(RegistroDimensao.ESTADO_VERSAO, VERSAO);
		ModEspiritismoMinecraftMod.LOGGER.debug("Estados: {} {} -> {} ({})", player.getName().getString(), atual, novo, motivo);
		for (BiConsumer<Player, EstadoEspiritual> g : AO_MUDAR) {
			try {
				g.accept(player, atual);
			} catch (RuntimeException e) {
				ModEspiritismoMinecraftMod.LOGGER.error("Erro num gancho Estados.AO_MUDAR", e);
			}
		}
	}

	/** Ticks desde que o estado atual começou. */
	public static long tempoNoEstado(Player player) {
		return Math.max(0, player.level().getGameTime() - player.getData(RegistroDimensao.ESTADO_DESDE));
	}

	// ================================================================ Consultas (sem efeitos colaterais)

	/** Perturbado/confuso: efeitos de confusão, sem violência nem destruição. */
	public static boolean perturbado(Player player) {
		EstadoEspiritual e = get(player);
		return e == EstadoEspiritual.NO_UMBRAL_PERTURBADO || e == EstadoEspiritual.ENCARNADO_PERTURBADO;
	}

	/** Perturbação acima da qual o jogador ainda não tem serenidade para socorrer outros. */
	public static final int PERTURBACAO_MAX_SOCORRO = 40;

	/**
	 * "Paciente não ajuda paciente" (PROMPT §5.7): pode assumir tarefas de socorro/assistência a outros?
	 * Exige: não estar em estado de cuidado (perturbado, acolhimento, tratamento, resgate, recém-desencarnado),
	 * moralidade acima da faixa "Perturbada" e perturbação momentânea baixa.
	 */
	public static boolean podeSocorrer(Player player) {
		return podeSocorrer(get(player), Moralidade.get(player), player.getData(RegistroDimensao.PERTURBACAO));
	}

	/** Regra pura de {@link #podeSocorrer(Player)}. */
	public static boolean podeSocorrer(EstadoEspiritual estado, int moral, int perturbacao) {
		return !estado.precisaDeCuidado() && moral > Moralidade.BAIXA && perturbacao < PERTURBACAO_MAX_SOCORRO;
	}

	/** Mensagem respeitosa para quem ainda precisa de cuidado (PROMPT §5.7). */
	public static final String MSG_PRECISA_CUIDADO = "Neste momento, você também precisa de cuidado. Permita-se receber auxílio primeiro.";

	/** Ataques e quebra/colocação de blocos bloqueados pelo estado (não pela região). */
	public static boolean semViolencia(Player player) {
		return get(player).semViolencia();
	}

	// ================================================================ Coerência com a dimensão / migração

	/** Estado deduzido só pela dimensão e moralidade (saves antigos, sem estado salvo). Regra pura. */
	public static EstadoEspiritual deduzir(ResourceKey<Level> dim, int moral) {
		if (dim == Planos.NOSSO_LAR)
			return EM_NOSSO_LAR;
		if (dim == Planos.UMBRAL)
			return moral <= Planos.LIMIAR_UMBRAL ? NO_UMBRAL_PERTURBADO : NO_UMBRAL_ORIENTADO;
		return moral <= Moralidade.BAIXA ? ENCARNADO_PERTURBADO : ENCARNADO_ESTAVEL;
	}

	/** O estado faz sentido na dimensão onde o jogador está? (Usado para corrigir softlocks.) Regra pura. */
	public static boolean coerente(EstadoEspiritual e, ResourceKey<Level> dim) {
		if (dim == Planos.NOSSO_LAR)
			return switch (e) {
				case EM_ACOLHIMENTO, EM_NOSSO_LAR, EM_TRATAMENTO, EM_VISITA_ASSISTIDA, PRONTO_PARA_REENCARNAR, RECEM_DESENCARNADO, REENCARNANDO, EM_RESGATE -> true;
				default -> false;
			};
		if (dim == Planos.UMBRAL)
			return switch (e) {
				case NO_UMBRAL_ORIENTADO, NO_UMBRAL_PERTURBADO, EM_RESGATE, EM_ACOLHIMENTO, EM_VISITA_ASSISTIDA, RECEM_DESENCARNADO -> true;
				default -> false;
			};
		return switch (e) {
			case ENCARNADO_ESTAVEL, ENCARNADO_PERTURBADO, DESENCARNANDO, RECEM_DESENCARNADO, REENCARNANDO -> true;
			default -> false;
		};
	}

	/** Save antigo (sem versão de estado): deduz o estado pela dimensão. Devolve true se migrou. */
	public static boolean migrar(Player player) {
		if (player.level().isClientSide() || player.getData(RegistroDimensao.ESTADO_VERSAO) >= VERSAO)
			return false;
		EstadoEspiritual e = player.getData(RegistroDimensao.SONHO_FIM) > 0 ? EM_VISITA_ASSISTIDA : deduzir(player.level().dimension(), Moralidade.get(player));
		forcar(player, e, "migração de save antigo");
		return true;
	}
}
