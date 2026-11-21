package net.mcreator.modespiritismominecraft.espiritismo.biblioteca;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

/** Pacotes de rede da Biblioteca (registrados no barramento do mod por {@link RegistroBiblioteca}). */
public final class PacotesBiblioteca {
	private PacotesBiblioteca() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, path);
	}

	/** Servidor → cliente: abrir a tela da biblioteca numa aba ("catalogo", "obra", "trilha", "glossario", "evangelho"), com alvo opcional. */
	public record Abrir(String aba, String alvo) implements CustomPacketPayload {
		public static final Type<Abrir> TYPE = new Type<>(id("biblioteca_abrir"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Abrir> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(32), Abrir::aba,
				ByteBufCodecs.stringUtf8(128), Abrir::alvo, Abrir::new);

		@Override
		public Type<Abrir> type() {
			return TYPE;
		}
	}

	/** Tipos de ação do cliente. */
	public static final int LER = 0, MARCAR = 1, NOTA = 2, RESPONDER = 3, CONCLUIR = 4, EVANGELHO = 5;

	/** Cliente → servidor: uma ação na tela. O servidor valida tudo (ids existentes, tamanhos). */
	public record Acao(int tipo, String a, String b, int n) implements CustomPacketPayload {
		public static final Type<Acao> TYPE = new Type<>(id("biblioteca_acao"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Acao> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Acao::tipo, ByteBufCodecs.stringUtf8(128),
				Acao::a, ByteBufCodecs.stringUtf8(ProgressoBiblioteca.MAX_NOTA * 4), Acao::b, ByteBufCodecs.VAR_INT, Acao::n, Acao::new);

		@Override
		public Type<Acao> type() {
			return TYPE;
		}
	}

	static void registrar(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar r = event.registrar(ModEspiritismoMinecraftMod.MODID).versioned("biblioteca-1");
		r.playToClient(Abrir.TYPE, Abrir.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> Cliente.abrir(msg.aba(), msg.alvo())));
		r.playToServer(Acao.TYPE, Acao.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> {
			if (ctx.player() instanceof ServerPlayer sp)
				tratar(sp, msg);
		}));
	}

	static void tratar(ServerPlayer p, Acao msg) {
		switch (msg.tipo()) {
			case LER -> BibliotecaApi.registrarLeitura(p, msg.a(), msg.b());
			case MARCAR -> BibliotecaApi.marcar(p, msg.a(), msg.b());
			case NOTA -> BibliotecaApi.anotar(p, msg.a(), msg.b());
			case RESPONDER -> BibliotecaApi.responder(p, msg.a(), msg.n());
			case CONCLUIR -> BibliotecaApi.concluirModulo(p, msg.a());
			case EVANGELHO -> {
				switch (msg.a()) {
					case "iniciar" -> EvangelhoNoLar.iniciar(p);
					case "participar" -> EvangelhoNoLar.participarProximo(p);
					case "sair" -> EvangelhoNoLar.sair(p);
					default -> {
					}
				}
			}
			default -> {
			}
		}
	}

	/** Isolado para não carregar classes de cliente no servidor dedicado. */
	static final class Cliente {
		static void abrir(String aba, String alvo) {
			net.mcreator.modespiritismominecraft.espiritismo.biblioteca.client.TelaBiblioteca.abrir(aba, alvo);
		}
	}
}
