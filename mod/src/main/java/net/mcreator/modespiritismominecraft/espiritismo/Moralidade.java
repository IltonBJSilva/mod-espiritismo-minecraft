package net.mcreator.modespiritismominecraft.espiritismo;

import com.mojang.serialization.Codec;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

/**
 * Barra de Moralidade do jogador (Documentação inicial, seção "Barra de moralidade").
 * <p>
 * Valor de 0 a 100, começa em 50 (neutro). É salvo no jogador, sobrevive à morte
 * e é sincronizado com o cliente para aparecer no HUD.
 * <p>
 * Este pacote não é gerenciado pelo MCreator, então não é sobrescrito ao regenerar o código.
 */
public final class Moralidade {
	public static final int MIN = 0;
	public static final int MAX = 100;
	public static final int INICIAL = 50;

	/** A partir deste valor o jogador é considerado de moralidade alta. */
	public static final int ALTA = 80;
	/** Até este valor o jogador é considerado de moralidade baixa (sujeito a obsessões). */
	public static final int BAIXA = 20;

	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ModEspiritismoMinecraftMod.MODID);

	public static final Supplier<AttachmentType<Integer>> MORALIDADE = ATTACHMENTS.register("moralidade",
			() -> AttachmentType.builder(() -> INICIAL).serialize(Codec.INT.fieldOf("valor")).sync(Moralidade::soDono, ByteBufCodecs.VAR_INT).copyOnDeath().build());

	/** Se o jogador já recebeu O Livro dos Espíritos de Allan Kardec. */
	public static final Supplier<AttachmentType<Boolean>> RECEBEU_LIVRO = ATTACHMENTS.register("recebeu_livro",
			() -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("valor")).copyOnDeath().build());

	/** Classe espiritual escolhida (índice de {@link ClasseEspiritual}). Sincronizada para o HUD. */
	public static final Supplier<AttachmentType<Integer>> CLASSE = ATTACHMENTS.register("classe",
			() -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("valor")).sync(Moralidade::soDono, ByteBufCodecs.VAR_INT).copyOnDeath().build());

	/** Energia espiritual (doc: "Barra de Energia Espiritual"), 0 a 100. Gasta por habilidades. */
	public static final Supplier<AttachmentType<Integer>> ENERGIA = ATTACHMENTS.register("energia",
			() -> AttachmentType.builder(() -> 100).serialize(Codec.INT.fieldOf("valor")).sync(Moralidade::soDono, ByteBufCodecs.VAR_INT).copyOnDeath().build());

	public static final int ENERGIA_MAX = 100;

	/**
	 * Recargas e contadores por jogador (chave → tick do mundo). Salvo no jogador, então não
	 * vaza entre mundos nem zera ao reiniciar o servidor. Não é sincronizado.
	 */
	public static final Supplier<AttachmentType<Map<String, Long>>> RECARGAS = ATTACHMENTS.register("recargas",
			() -> AttachmentType.<Map<String, Long>>builder(() -> new HashMap<>()).serialize(Codec.unboundedMap(Codec.STRING, Codec.LONG).<Map<String, Long>>xmap(m -> new HashMap<>(m), m -> m).fieldOf("valor")).copyOnDeath().build());

	/** Até quando (tick do mundo) uma criatura fica pacificada pela Evangelização. Não é salvo. */
	public static final Supplier<AttachmentType<Long>> PACIFICADO_ATE = ATTACHMENTS.register("pacificado_ate", () -> AttachmentType.builder(() -> 0L).build());

	/** Etapa do tutorial "Jornada Espiritual" (ver {@link Jornada}). Sincronizada para o HUD. */
	public static final Supplier<AttachmentType<Integer>> JORNADA = ATTACHMENTS.register("jornada",
			() -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("valor")).sync(Moralidade::soDono, ByteBufCodecs.VAR_INT).copyOnDeath().build());

	/** Aldeão passando fome (prova de caridade): UUID do jogador para quem é a prova, ou "" se não está faminto. */
	public static final Supplier<AttachmentType<String>> FAMINTO = ATTACHMENTS.register("faminto",
			() -> AttachmentType.builder(() -> "").serialize(Codec.STRING.fieldOf("valor")).build());

	/** Até quando (tick do mundo) a fome do aldeão dura; depois ele se vira sozinho. */
	public static final Supplier<AttachmentType<Long>> FAMINTO_ATE = ATTACHMENTS.register("faminto_ate",
			() -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("valor")).build());

	/** No Overworld: posição (BlockPos.asLong) da porta do Centro Espírita, ou ausente se ainda não foi construído. */
	public static final Supplier<AttachmentType<Long>> CENTRO = ATTACHMENTS.register("centro",
			() -> AttachmentType.builder(() -> Long.MIN_VALUE).serialize(Codec.LONG.fieldOf("valor")).build());

	/** Máximo de moralidade ganho por dia de jogo com ações de rotina (negociar, plantar...). */
	public static final int TETO_ROTINA_DIA = 8;

	/** Sincroniza os dados só com o próprio jogador (os outros não precisam saber). */
	private static boolean soDono(IAttachmentHolder holder, ServerPlayer para) {
		return holder == para;
	}

	/** Se a ação está liberada; se estiver, já inicia a recarga. */
	public static boolean pronto(Player player, String acao, int recargaTicks) {
		long agora = player.level().getGameTime();
		Map<String, Long> mapa = player.getData(RECARGAS);
		if (agora < mapa.getOrDefault(acao, 0L))
			return false;
		mapa.put(acao, agora + recargaTicks);
		return true;
	}

	/** Ticks restantes de recarga (0 se liberada). */
	public static long restante(Player player, String acao) {
		return Math.max(0, player.getData(RECARGAS).getOrDefault(acao, 0L) - player.level().getGameTime());
	}

	public static void limparRecarga(Player player, String acao) {
		player.getData(RECARGAS).remove(acao);
	}

	/**
	 * Ganho de moralidade por ação de rotina, limitado a {@link #TETO_ROTINA_DIA} por dia de jogo.
	 * Missões e provas usam {@link #alterar} diretamente, sem teto.
	 */
	public static void ganhoRotina(Player player, int delta, String motivo) {
		Map<String, Long> mapa = player.getData(RECARGAS);
		long dia = player.level().getGameTime() / 24000L;
		if (mapa.getOrDefault("rotina_dia", -1L) != dia) {
			mapa.put("rotina_dia", dia);
			mapa.put("rotina_ganho", 0L);
		}
		long ganho = mapa.getOrDefault("rotina_ganho", 0L);
		if (ganho >= TETO_ROTINA_DIA)
			return;
		int efetivo = (int) Math.min(delta, TETO_ROTINA_DIA - ganho);
		mapa.put("rotina_ganho", ganho + efetivo);
		alterar(player, efetivo, motivo);
	}

	private Moralidade() {
	}

	public static ClasseEspiritual classe(Player player) {
		return ClasseEspiritual.porIndice(player.getData(CLASSE));
	}

	public static void definirClasse(Player player, ClasseEspiritual classe) {
		player.setData(CLASSE, classe.ordinal());
	}

	public static int energia(Player player) {
		return player.getData(ENERGIA);
	}

	public static void alterarEnergia(Player player, int delta) {
		int atual = energia(player);
		int novo = Math.clamp(atual + delta, 0, ENERGIA_MAX);
		if (novo != atual)
			player.setData(ENERGIA, novo);
	}

	public static int get(Player player) {
		return player.getData(MORALIDADE);
	}

	public static void set(Player player, int valor) {
		player.setData(MORALIDADE, Math.clamp(valor, MIN, MAX));
	}

	/**
	 * Altera a moralidade e avisa o jogador na barra de ação.
	 *
	 * @param motivo texto curto exibido ao jogador, ex.: "Ajudou um aldeão"
	 */
	public static void alterar(Player player, int delta, String motivo) {
		if (delta == 0 || player.level().isClientSide())
			return;
		int antes = get(player);
		set(player, antes + delta);
		int depois = get(player);
		if (antes == depois)
			return;

		Component msg = Component.literal((delta > 0 ? "+" : "") + delta + " Moralidade").withStyle(delta > 0 ? ChatFormatting.AQUA : ChatFormatting.RED)
				.append(Component.literal(" — " + motivo).withStyle(ChatFormatting.GRAY));
		player.sendOverlayMessage(msg);

		if (antes < ALTA && depois >= ALTA) {
			Jornada.conquista(player, "elevado");
			player.sendSystemMessage(Component.literal("Você sente uma presença benfeitora ao seu lado. Sua moralidade está elevada.").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
		} else if (antes > BAIXA && depois <= BAIXA)
			player.sendSystemMessage(Component.literal("Um peso sombrio envolve seus pensamentos... Espíritos imperfeitos se aproximam.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
	}

	public static String descricao(int valor) {
		if (valor >= ALTA)
			return "Elevada";
		if (valor >= 60)
			return "Boa";
		if (valor > 40)
			return "Neutra";
		if (valor > BAIXA)
			return "Instável";
		return "Perturbada";
	}
}
