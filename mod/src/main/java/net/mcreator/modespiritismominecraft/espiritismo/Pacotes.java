package net.mcreator.modespiritismominecraft.espiritismo;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

/** Pacotes de rede entre cliente e servidor. */
@EventBusSubscriber
public final class Pacotes {
	private Pacotes() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, path);
	}

	/** Servidor → cliente: abrir a tela de escolha de classe (enviado por Allan Kardec). */
	public record AbrirEscolhaClasse() implements CustomPacketPayload {
		public static final Type<AbrirEscolhaClasse> TYPE = new Type<>(id("abrir_escolha_classe"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AbrirEscolhaClasse> CODEC = StreamCodec.unit(new AbrirEscolhaClasse());

		@Override
		public Type<AbrirEscolhaClasse> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: o jogador escolheu uma classe. */
	public record EscolherClasse(int classe) implements CustomPacketPayload {
		public static final Type<EscolherClasse> TYPE = new Type<>(id("escolher_classe"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EscolherClasse> CODEC = ByteBufCodecs.VAR_INT.map(EscolherClasse::new, EscolherClasse::classe).cast();

		@Override
		public Type<EscolherClasse> type() {
			return TYPE;
		}
	}

	/** Cliente → servidor: o jogador apertou a tecla de habilidade. */
	public record UsarHabilidade() implements CustomPacketPayload {
		public static final Type<UsarHabilidade> TYPE = new Type<>(id("usar_habilidade"));
		public static final StreamCodec<RegistryFriendlyByteBuf, UsarHabilidade> CODEC = StreamCodec.unit(new UsarHabilidade());

		@Override
		public Type<UsarHabilidade> type() {
			return TYPE;
		}
	}

	@SubscribeEvent
	public static void registrar(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar r = event.registrar(ModEspiritismoMinecraftMod.MODID).versioned("1");
		r.playToClient(AbrirEscolhaClasse.TYPE, AbrirEscolhaClasse.CODEC, (msg, ctx) -> ctx.enqueueWork(PacotesCliente::abrirEscolhaClasse));
		r.playToServer(EscolherClasse.TYPE, EscolherClasse.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> Habilidades.escolherClasse(ctx.player(), msg.classe())));
		r.playToServer(UsarHabilidade.TYPE, UsarHabilidade.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> Habilidades.usar(ctx.player())));
	}

	/** Isolado em outra classe para não carregar código de cliente no servidor dedicado. */
	static final class PacotesCliente {
		static void abrirEscolhaClasse() {
			net.mcreator.modespiritismominecraft.espiritismo.client.TelaEscolhaClasse.abrir();
		}
	}
}
