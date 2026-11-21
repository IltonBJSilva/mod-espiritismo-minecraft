package net.mcreator.modespiritismominecraft.espiritismo.biblioteca;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comandos da biblioteca e sugestões contextuais de módulos (uma vez por jogador, sem interromper o jogo).
 * <ul>
 * <li>{@code /biblioteca} — abre o catálogo; {@code /biblioteca obra <id>}, {@code /biblioteca trilha <modulo>},
 * {@code /biblioteca glossario}, {@code /biblioteca progresso} (para todos).</li>
 * <li>{@code /biblioteca resetar} — operadores: zera o próprio progresso de leitura (teste).</li>
 * <li>{@code /evangelho_no_lar iniciar|participar|sair} — atividade opcional.</li>
 * </ul>
 */
@EventBusSubscriber
public final class EventosBiblioteca {
	private EventosBiblioteca() {
	}

	@SubscribeEvent
	public static void comandos(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("biblioteca").executes(ctx -> {
			BibliotecaApi.abrirCatalogo(ctx.getSource().getPlayerOrException());
			return 1;
		}).then(Commands.literal("obra").then(Commands.argument("id", StringArgumentType.word())
				.suggests((ctx, b) -> SharedSuggestionProvider.suggest(Catalogo.OBRAS.keySet(), b)).executes(ctx -> {
					BibliotecaApi.abrir(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"));
					return 1;
				}))).then(Commands.literal("trilha").executes(ctx -> {
					BibliotecaApi.abrirTrilha(ctx.getSource().getPlayerOrException(), "");
					return 1;
				}).then(Commands.argument("modulo", StringArgumentType.word()).suggests((ctx, b) -> SharedSuggestionProvider.suggest(Trilhas.MODULOS.keySet(), b))
						.executes(ctx -> {
							BibliotecaApi.abrirTrilha(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "modulo"));
							return 1;
						}))).then(Commands.literal("glossario").executes(ctx -> {
							BibliotecaApi.abrirGlossario(ctx.getSource().getPlayerOrException(), "");
							return 1;
						})).then(Commands.literal("progresso").executes(ctx -> {
							ServerPlayer p = ctx.getSource().getPlayerOrException();
							ProgressoBiblioteca pr = BibliotecaApi.progresso(p);
							p.sendSystemMessage(Component.literal("Conhecimento: " + pr.conhecimento() + " · seções lidas: " + pr.totalLidas() + " · acertos: "
									+ pr.acertos().size() + " · módulos: " + pr.modulos().size() + "/" + Trilhas.MODULOS.size() + " · Evangelho no Lar: " + pr.evangelhos()
									+ ". (Conhecimento não é moralidade.)").withStyle(ChatFormatting.AQUA));
							return 1;
						})).then(Commands.literal("resetar").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
							ServerPlayer p = ctx.getSource().getPlayerOrException();
							p.setData(RegistroBiblioteca.PROGRESSO, ProgressoBiblioteca.VAZIO);
							ctx.getSource().sendSuccess(() -> Component.literal("Progresso da biblioteca zerado."), true);
							return 1;
						})));

		event.getDispatcher().register(Commands.literal("evangelho_no_lar").executes(ctx -> {
			BibliotecaApi.abrirEvangelhoNoLar(ctx.getSource().getPlayerOrException());
			return 1;
		}).then(Commands.literal("iniciar").executes(ctx -> {
			EvangelhoNoLar.iniciar(ctx.getSource().getPlayerOrException());
			return 1;
		})).then(Commands.literal("participar").executes(ctx -> {
			EvangelhoNoLar.participarProximo(ctx.getSource().getPlayerOrException());
			return 1;
		})).then(Commands.literal("sair").executes(ctx -> {
			EvangelhoNoLar.sair(ctx.getSource().getPlayerOrException());
			return 1;
		})));
	}

	// ---------------------------------------------------------------- Sugestões contextuais

	@SubscribeEvent
	public static void acordou(PlayerWakeUpEvent event) {
		// Só quando acordou depois de dormir de verdade (não quando a cama foi interrompida).
		if (event.getEntity() instanceof ServerPlayer sp && !event.wakeImmediately())
			BibliotecaApi.sugerirModulo(sp, "sono_emancipacao");
	}

	@SubscribeEvent
	public static void renasceu(PlayerEvent.PlayerRespawnEvent event) {
		if (!event.isEndConquered() && event.getEntity() instanceof ServerPlayer sp)
			BibliotecaApi.sugerirModulo(sp, "desencarnacao");
	}

	@SubscribeEvent
	public static void mudouDimensao(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer sp))
			return;
		String destino = event.getTo().identifier().getPath();
		if (destino.equals("umbral"))
			BibliotecaApi.sugerirModulo(sp, "livre_arbitrio");
		else if (destino.equals("nosso_lar"))
			BibliotecaApi.sugerirModulo(sp, "desencarnacao");
	}

	@SubscribeEvent
	public static void entrou(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp && BibliotecaApi.progresso(sp).sugeridos().isEmpty())
			BibliotecaApi.sugerirModulo(sp, "espirito_perispirito_corpo");
	}
}
