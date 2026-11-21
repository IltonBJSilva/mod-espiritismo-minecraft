package net.mcreator.modespiritismominecraft.espiritismo.colonia;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;

import java.util.Map;

/** Eventos da área "colonia": obra incremental, aeróbus, sentar nos bancos e comandos. */
@EventBusSubscriber
public final class EventosColonia {
	public static final String TAG_ASSENTO = "espiritismo_assento";

	private EventosColonia() {
	}

	@SubscribeEvent
	public static void iniciado(ServerStartedEvent event) {
		ConstrucaoColonia.aoIniciarServidor(event.getServer());
	}

	@SubscribeEvent
	public static void parando(ServerStoppingEvent event) {
		ConstrucaoColonia.aoPararServidor();
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post event) {
		var server = event.getServer();
		ConstrucaoColonia.tick(server);
		Aerobus.tick(server);
		ServerLevel nl = server.getLevel(Planos.NOSSO_LAR);
		if (nl == null || server.getTickCount() % 20 != 0)
			return;
		Obra obra = ConstrucaoColonia.obra(nl);
		if (obra == null)
			return;
		for (ServerPlayer p : nl.players()) {
			p.fallDistance = 0; // o chão ao redor ainda está sendo nivelado
			if (server.getTickCount() % 600 == 0)
				p.sendOverlayMessage(Component.literal("Nosso Lar se revela aos poucos ao seu redor... (" + obra.porcentagem() + "%)").withStyle(ChatFormatting.GRAY));
		}
	}

	// ================================================================ Aeróbus e bancos

	@SubscribeEvent
	public static void clique(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p) || p.level().dimension() != Planos.NOSSO_LAR)
			return;
		ServerLevel level = (ServerLevel) p.level();
		BlockPos pos = event.getPos();
		BlockState s = level.getBlockState(pos);
		if (s.is(Blocks.LECTERN)) {
			Map<String, Long> pontos = Planos.pontos(level);
			String estacao = Aerobus.estacaoDoAtril(pontos, pos);
			if (estacao != null) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
				Aerobus.mostrarDestinos(p, level, pontos, estacao);
			}
			return;
		}
		if (s.getBlock() instanceof StairBlock && s.getValue(StairBlock.HALF) == Half.BOTTOM && p.getMainHandItem().isEmpty() && !p.isShiftKeyDown() && !p.isPassenger()
				&& level.getBlockState(pos.above()).isAir()) {
			if (sentar(p, level, pos)) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
			}
		}
	}

	/** Senta o jogador num banco (escada): um assento invisível (block_display) que some quando ele levanta. */
	public static boolean sentar(ServerPlayer p, ServerLevel level, BlockPos pos) {
		if (p.distanceToSqr(pos.getCenter()) > 9)
			return false;
		if (!level.getEntities(EntityType.BLOCK_DISPLAY, new AABB(pos), e -> e.entityTags().contains(TAG_ASSENTO)).isEmpty())
			return false;
		Entity assento = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (assento == null)
			return false;
		assento.snapTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0F, 0F);
		assento.addTag(TAG_ASSENTO);
		assento.setNoGravity(true);
		level.addFreshEntity(assento);
		return p.startRiding(assento, true, true);
	}

	@SubscribeEvent
	public static void levantar(EntityMountEvent event) {
		if (event.isDismounting() && event.getEntityBeingMounted().entityTags().contains(TAG_ASSENTO) && !event.getLevel().isClientSide())
			event.getEntityBeingMounted().discard();
	}

	@SubscribeEvent
	public static void assentoOrfao(EntityJoinLevelEvent event) {
		Entity e = event.getEntity();
		if (event.loadedFromDisk() && !event.getLevel().isClientSide() && e.entityTags().contains(TAG_ASSENTO) && e.getPassengers().isEmpty())
			event.setCanceled(true);
	}

	// ================================================================ Comandos

	@SubscribeEvent
	public static void comandos(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("aerobus").then(Commands.argument("destino", StringArgumentType.word())
				.suggests((ctx, b) -> SharedSuggestionProvider.suggest(LayoutColonia.destinosAerobus().keySet(), b)).executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					if (p.level().dimension() != Planos.NOSSO_LAR) {
						ctx.getSource().sendFailure(Component.literal("O aeróbus só circula em Nosso Lar."));
						return 0;
					}
					ServerLevel level = (ServerLevel) p.level();
					String erro = Aerobus.embarcar(p, level, Planos.pontos(level), StringArgumentType.getString(ctx, "destino"));
					if (erro != null) {
						ctx.getSource().sendFailure(Component.literal(erro));
						return 0;
					}
					return 1;
				})));
		event.getDispatcher().register(Commands.literal("colonia").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("status").executes(ctx -> {
			ServerLevel nl = ctx.getSource().getServer().getLevel(Planos.NOSSO_LAR);
			if (nl == null) {
				ctx.getSource().sendFailure(Component.literal("Nosso Lar não está carregada."));
				return 0;
			}
			Obra o = ConstrucaoColonia.obra(nl);
			RegistroColonia.EstadoObra e = nl.getData(RegistroColonia.OBRA);
			String txt = o != null
					? "Obra em andamento: peça " + o.indice() + "/" + o.total() + " (" + o.porcentagem() + "%), " + o.operacoes + " operações, máx. " + o.nanosMaxTick / 1_000_000 + " ms/tick"
					: e.existe() ? (e.concluida() ? "Nosso Lar concluída (traçado v" + e.versao() + ")." : "Obra parada na peça " + e.indice()) : "Nosso Lar ainda não foi construída.";
			ctx.getSource().sendSuccess(() -> Component.literal(txt), false);
			return 1;
		})).then(Commands.literal("local").then(Commands.argument("nome", StringArgumentType.word())
				.suggests((ctx, b) -> SharedSuggestionProvider.suggest(LayoutColonia.pontosRelativos().keySet(), b)).executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					ServerLevel nl = ctx.getSource().getServer().getLevel(Planos.NOSSO_LAR);
					BlockPos pos = nl == null ? null : ColoniaApi.local(nl, StringArgumentType.getString(ctx, "nome"));
					if (pos == null) {
						ctx.getSource().sendFailure(Component.literal("Local desconhecido (ou Nosso Lar ainda não construída)."));
						return 0;
					}
					Planos.teleportar(p, nl, pos);
					return 1;
				}))));
	}
}
