package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;

/**
 * Comando de depuração (só operadores): {@code /espiritismo_populacao info | personagem <perfil> | amelie | jornada_resetar}.
 */
@EventBusSubscriber
public final class ComandosPopulacao {
	private ComandosPopulacao() {
	}

	@SubscribeEvent
	public static void comandos(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("espiritismo_populacao").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("info").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					ServerLevel l = p.level();
					ctx.getSource().sendSuccess(() -> Component.literal("Personagens registrados neste nível: " + Povoamento.registro(l).keySet()
							+ " | genéricos a 96 blocos: " + Povoamento.contarGenericos(l, p.blockPosition(), 96)
							+ " | jornada do recém-chegado: " + JornadaRecemChegado.etapa(p) + "/" + JornadaRecemChegado.CONCLUIDA
							+ " | Amélie: " + MissoesAmelie.etapa(p) + "/" + MissoesAmelie.CONCLUIDA
							+ " | memória: " + Memoria.de(p).marcas().size() + " marcas"), false);
					return 1;
				}))
				.then(Commands.literal("personagem").then(Commands.argument("perfil", StringArgumentType.word())
						.suggests((ctx, b) -> SharedSuggestionProvider.suggest(Perfis.todos().values().stream().filter(Perfil::nomeado).map(Perfil::id), b))
						.executes(ctx -> {
							ServerPlayer p = ctx.getSource().getPlayerOrException();
							String id = StringArgumentType.getString(ctx, "perfil");
							MoradorEntity m = Perfis.AMELIE.equals(id) ? PopulacaoApi.colocarAmelie(p.level(), p.blockPosition())
									: PopulacaoApi.garantirPersonagem(p.level(), id, p.blockPosition());
							ctx.getSource().sendSuccess(() -> Component.literal(m == null ? "Não colocado (perfil desconhecido, ou o oficial existe noutro lugar descarregado)."
									: "Personagem " + m.getName().getString() + " em " + m.blockPosition().toShortString()), false);
							return m == null ? 0 : 1;
						})))
				.then(Commands.literal("amelie").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					MoradorEntity m = PopulacaoApi.colocarAmelie(p.level(), p.blockPosition());
					ctx.getSource().sendSuccess(() -> Component.literal(m == null ? "Amélie já existe noutro lugar (descarregado)." : "Amélie em " + m.blockPosition().toShortString()), false);
					return m == null ? 0 : 1;
				}))
				.then(Commands.literal("jornada_resetar").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					p.setData(RegistroPopulacao.MEMORIA, new Memoria());
					ctx.getSource().sendSuccess(() -> Component.literal("Memória dos NPCs e jornadas do jogador zeradas."), false);
					return 1;
				})));
	}
}
