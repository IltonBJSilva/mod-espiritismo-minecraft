package net.mcreator.modespiritismominecraft.espiritismo.dimensao;

import com.mojang.serialization.Codec;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.bus.api.IEventBus;

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

	private RegistroDimensao() {
	}

	public static void registrar(IEventBus modBus) {
		ATTACHMENTS.register(modBus);
	}
}
