package net.mcreator.modespiritismominecraft.espiritismo.dimensao;

import com.mojang.serialization.Codec;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.codec.ByteBufCodecs;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

/**
 * Registro da área "dimensao" (Nosso Lar, Umbral, morte/renascimento, sono). As dimensões e biomas são
 * data-driven (data/mod_espiritismo_minecraft/dimension, dimension_type, worldgen/...); aqui ficam só os attachments.
 */
public final class RegistroDimensao {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ModEspiritismoMinecraftMod.MODID);

	// ---------------------------------------------------------------- No Level (Nosso Lar / Umbral)

	/** Pontos das construções feitas por código (nome → BlockPos.asLong): "chegada", "auxilio", "mirante", "posto"... Vazio = não construído. */
	public static final Supplier<AttachmentType<Map<String, Long>>> PONTOS = ATTACHMENTS.register("plano_pontos",
			() -> AttachmentType.<Map<String, Long>>builder(() -> new HashMap<>()).serialize(Codec.unboundedMap(Codec.STRING, Codec.LONG).<Map<String, Long>>xmap(HashMap::new, m -> m).fieldOf("valor")).build());

	// ---------------------------------------------------------------- No jogador

	/** Última posição conhecida na Terra (Overworld), para a reencarnação cair longe dela. */
	public static final Supplier<AttachmentType<Long>> ULTIMA_TERRA = ATTACHMENTS.register("ultima_terra",
			() -> AttachmentType.builder(() -> Long.MIN_VALUE).serialize(Codec.LONG.fieldOf("valor")).copyOnDeath().build());

	/** Pontos de arrependimento acumulados no Umbral (preces, ajuda a sofredores, tempo de reflexão). */
	public static final Supplier<AttachmentType<Integer>> ARREPENDIMENTO = ATTACHMENTS.register("arrependimento",
			() -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("valor")).copyOnDeath().build());

	/** Ticks passados no Umbral desde a última contagem de "tempo de reflexão". */
	public static final Supplier<AttachmentType<Integer>> TEMPO_UMBRAL = ATTACHMENTS.register("tempo_umbral",
			() -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("valor")).copyOnDeath().build());

	/** Tick (do Overworld) em que o socorrista leva o jogador; 0 = nenhum resgate a caminho. */
	public static final Supplier<AttachmentType<Long>> RESGATE_EM = ATTACHMENTS.register("resgate_em",
			() -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("valor")).build());

	/** Desdobramento no sono: tick (do Overworld) em que o sonho termina; 0 = não está sonhando. */
	public static final Supplier<AttachmentType<Long>> SONHO_FIM = ATTACHMENTS.register("sonho_fim",
			() -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("valor")).build());

	/** Desdobramento no sono: posição (asLong) ao lado da cama para onde o jogador volta. */
	public static final Supplier<AttachmentType<Long>> SONHO_RETORNO = ATTACHMENTS.register("sonho_retorno",
			() -> AttachmentType.builder(() -> Long.MIN_VALUE).serialize(Codec.LONG.fieldOf("valor")).build());

	/** Desdobramento no sono: dimensão (id) da cama. */
	public static final Supplier<AttachmentType<String>> SONHO_DIM = ATTACHMENTS.register("sonho_dim",
			() -> AttachmentType.builder(() -> "").serialize(Codec.STRING.fieldOf("valor")).build());


	// ---------------------------------------------------------------- Fase 4 (área "umbral": estados, resgate, postos)

	/** No Level: progresso das construções incrementais (chave "p<i>.x/.y/.z/.t/.i/.ok", "m<i>_<k>.ok"). */
	public static final Supplier<AttachmentType<Map<String, Long>>> OBRAS = ATTACHMENTS.register("plano_obras",
			() -> AttachmentType.<Map<String, Long>>builder(() -> new HashMap<>()).serialize(Codec.unboundedMap(Codec.STRING, Codec.LONG).<Map<String, Long>>xmap(HashMap::new, m -> m).fieldOf("valor")).build());

	/** Perturbação/confusão atual (0–100). Temporária, reduzida por acolhimento, repouso, conversa e prece. Sincronizada com o dono (efeitos no cliente). */
	public static final Supplier<AttachmentType<Integer>> PERTURBACAO = ATTACHMENTS.register("perturbacao",
			() -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("valor")).sync(RegistroDimensao::soDono, ByteBufCodecs.VAR_INT).copyOnDeath().build());

	/** Versão do esquema do estado espiritual (0 = save antigo, ainda não migrado). */
	public static final Supplier<AttachmentType<Integer>> ESTADO_VERSAO = ATTACHMENTS.register("estado_versao",
			() -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("valor")).copyOnDeath().build());

	/** Tick (do mundo) em que o estado atual começou. */
	public static final Supplier<AttachmentType<Long>> ESTADO_DESDE = ATTACHMENTS.register("estado_desde",
			() -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("valor")).copyOnDeath().build());

	/** Resgate com consentimento: fase (ver {@link Resgates}). Não sobrevive à morte. */
	public static final Supplier<AttachmentType<Integer>> RESGATE_FASE = ATTACHMENTS.register("resgate_fase",
			() -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("valor")).build());

	/** Resgate com consentimento: tick-limite da fase atual. */
	public static final Supplier<AttachmentType<Long>> RESGATE_ATE = ATTACHMENTS.register("resgate_ate",
			() -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("valor")).build());

	/** Saída de Nosso Lar autorizada/acompanhada até este tick (0 = sem acompanhamento). */
	public static final Supplier<AttachmentType<Long>> SAIDA_ATE = ATTACHMENTS.register("saida_ate",
			() -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("valor")).build());

	private static boolean soDono(IAttachmentHolder holder, ServerPlayer para) {
		return holder == para;
	}

	private RegistroDimensao() {
	}

	public static void registrar(IEventBus modBus) {
		ATTACHMENTS.register(modBus);
	}
}
