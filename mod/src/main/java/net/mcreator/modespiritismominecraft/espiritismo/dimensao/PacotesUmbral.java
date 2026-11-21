package net.mcreator.modespiritismominecraft.espiritismo.dimensao;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

/** Pacotes da área "umbral": tela de consentimento do resgate, pedido de ajuda e preferência de acessibilidade. */
@EventBusSubscriber
public final class PacotesUmbral {
	private PacotesUmbral() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, path);
	}

	/** Servidor → cliente: abrir a tela de escolhas do resgate, com a fala do socorrista. */
	public record AbrirResgate(String fala) implements CustomPacketPayload {
		public static final Type<AbrirResgate> TYPE = new Type<>(id("umbral_abrir_resgate"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AbrirResgate> CODEC = ByteBufCodecs.STRING_UTF8.map(AbrirResgate::new, AbrirResgate::fala).cast();

		@Override
		public Type<AbrirResgate> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: escolha (índice de {@link Resgates.Escolha}). */
	public record EscolhaResgate(int escolha) implements CustomPacketPayload {
		public static final Type<EscolhaResgate> TYPE = new Type<>(id("umbral_escolha_resgate"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EscolhaResgate> CODEC = ByteBufCodecs.VAR_INT.map(EscolhaResgate::new, EscolhaResgate::escolha).cast();

		@Override
		public Type<EscolhaResgate> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: o jogador pediu ajuda (tecla H). */
	public record PedirAjuda() implements CustomPacketPayload {
		public static final Type<PedirAjuda> TYPE = new Type<>(id("umbral_pedir_ajuda"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PedirAjuda> CODEC = StreamCodec.unit(new PedirAjuda());

		@Override
		public Type<PedirAjuda> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: preferência de efeitos de perturbação (0 completo, 1 reduzido, 2 desligado). */
	public record PreferenciaEfeitos(int modo) implements CustomPacketPayload {
		public static final Type<PreferenciaEfeitos> TYPE = new Type<>(id("umbral_preferencia_efeitos"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PreferenciaEfeitos> CODEC = ByteBufCodecs.VAR_INT.map(PreferenciaEfeitos::new, PreferenciaEfeitos::modo).cast();

		@Override
		public Type<PreferenciaEfeitos> type() {
			return TYPE;
		}
	}

	@SubscribeEvent
	public static void registrar(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar r = event.registrar(ModEspiritismoMinecraftMod.MODID).versioned("1").optional();
		r.playToClient(AbrirResgate.TYPE, AbrirResgate.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> Cliente.abrir(msg.fala())));
		r.playToServer(EscolhaResgate.TYPE, EscolhaResgate.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> {
			if (ctx.player() instanceof ServerPlayer sp)
				Resgates.escolher(sp, Resgates.Escolha.porIndice(msg.escolha()));
		}));
		r.playToServer(PedirAjuda.TYPE, PedirAjuda.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> {
			if (ctx.player() instanceof ServerPlayer sp)
				Resgates.pedirAjuda(sp);
		}));
		r.playToServer(PreferenciaEfeitos.TYPE, PreferenciaEfeitos.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> Perturbacao.preferencia(ctx.player(), msg.modo())));
	}

	/** Envia ao cliente só se ele tiver o canal (mock players de teste e clientes sem o mod não quebram). */
	static void enviar(ServerPlayer p, CustomPacketPayload payload) {
		try {
			if (p.connection != null && p.connection.hasChannel(payload))
				PacketDistributor.sendToPlayer(p, payload);
		} catch (RuntimeException e) {
			ModEspiritismoMinecraftMod.LOGGER.debug("PacotesUmbral: não enviado a {}: {}", p.getName().getString(), e.toString());
		}
	}

	/** Isolado para não carregar código de cliente no servidor dedicado. */
	static final class Cliente {
		static void abrir(String fala) {
			net.mcreator.modespiritismominecraft.espiritismo.dimensao.client.TelaResgate.abrir(fala);
		}
	}
}
