package net.mcreator.modespiritismominecraft.espiritismo.protecao;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.TriState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.colonia.Lote;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.ColoniaApi;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.Set;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.HashMap;

/**
 * Eventos da proteção de Nosso Lar (lado do servidor). Cobre: quebra, clique esquerdo, colocação/substituição
 * (inclusive múltipla), uso de itens em blocos (ferramentas, farinha de osso, isqueiro), baldes e itens que agem no ar,
 * modificação por ferramentas (descascar, aplainar, arar), pisoteio de plantação, explosões (os blocos protegidos saem
 * da lista), fogo (removido no fim do tick, antes de poder queimar), fluidos que criariam pedra/fogo, pistões, mobs
 * que mexem em blocos (enderman, ovelha, coelho, aldeão, wither...) e molduras/suportes de armadura.
 */
@EventBusSubscriber
public final class EventosProtecao {
	private static final Map<ResourceKey<Level>, Set<BlockPos>> FOGO_A_APAGAR = new HashMap<>();

	private EventosProtecao() {
	}

	private static void registrarAdmin(Player p, String acao, BlockPos pos, BlockState s) {
		ModEspiritismoMinecraftMod.LOGGER.info("[Protecao] {} (operador, criativo) {} {} em {} ({})", p.getName().getString(), acao, s.getBlock().getDescriptionId(), pos.toShortString(),
				p.level().dimension().identifier());
	}

	// ================================================================ Quebra

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void quebrar(BreakBlockEvent event) {
		if (!(event.getLevel() instanceof Level level) || level.isClientSide() || !ColoniaApi.protegido(level, event.getPos()))
			return;
		Player p = event.getPlayer();
		if (PoliticaProtecao.isento(p)) {
			registrarAdmin(p, "quebrou", event.getPos(), event.getState());
			return;
		}
		event.setCanceled(true);
		MensagensProtecao.avisar(p, MensagensProtecao.tipoDe(event.getState()));
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void cliqueEsquerdo(PlayerInteractEvent.LeftClickBlock event) {
		Level level = event.getLevel();
		if (level.isClientSide() || !PoliticaProtecao.impede(event.getEntity(), level, event.getPos()))
			return;
		event.setCanceled(true);
	}

	// ================================================================ Colocação / uso

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void colocar(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getLevel() instanceof Level level) || level.isClientSide() || !ColoniaApi.protegido(level, event.getPos()))
			return;
		Entity e = event.getEntity();
		if (e instanceof Player p && PoliticaProtecao.isento(p)) {
			registrarAdmin(p, "colocou", event.getPos(), event.getPlacedBlock());
			return;
		}
		event.setCanceled(true);
		if (e instanceof Player p)
			MensagensProtecao.avisar(p, MensagensProtecao.Tipo.COLOCAR);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void cliqueDireito(PlayerInteractEvent.RightClickBlock event) {
		Level level = event.getLevel();
		Player p = event.getEntity();
		if (level.isClientSide() || !PoliticaProtecao.impede(p, level, event.getPos()))
			return;
		ItemStack mao = event.getItemStack();
		// Nunca usar o item no bloco (colocar, ferramenta, farinha de osso, isqueiro, ovo, balde...).
		event.setUseItem(TriState.FALSE);
		BlockState s = level.getBlockState(event.getPos());
		if (!PoliticaProtecao.usoPermitido(s, mao.isEmpty())) {
			event.setUseBlock(TriState.FALSE);
			if (!mao.isEmpty())
				MensagensProtecao.avisar(p, mao.getItem() instanceof BlockItem ? MensagensProtecao.Tipo.COLOCAR : avisoItem(mao, s));
			else
				MensagensProtecao.avisar(p, MensagensProtecao.Tipo.OBJETO);
		} else if (!mao.isEmpty() && (mao.getItem() instanceof BlockItem || mao.getItem() instanceof BucketItem || mao.getItem() instanceof FlintAndSteelItem))
			MensagensProtecao.avisar(p, mao.getItem() instanceof BlockItem ? MensagensProtecao.Tipo.COLOCAR : avisoItem(mao, s));
	}

	private static MensagensProtecao.Tipo avisoItem(ItemStack mao, BlockState s) {
		if (mao.getItem() instanceof BucketItem)
			return MensagensProtecao.Tipo.AGUA;
		if (mao.getItem() instanceof FlintAndSteelItem || mao.getItem() instanceof FireChargeItem)
			return MensagensProtecao.Tipo.FOGO;
		return MensagensProtecao.tipoDe(s);
	}

	/** Itens que agem por mira (baldes, vitória-régia, isqueiro, ovos): bloqueados na colônia. */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void usarItem(PlayerInteractEvent.RightClickItem event) {
		Player p = event.getEntity();
		Level level = event.getLevel();
		if (level.isClientSide() || !PoliticaProtecao.impede(p, level, p.blockPosition()))
			return;
		var item = event.getItemStack().getItem();
		if (item instanceof BucketItem || item instanceof BlockItem || item instanceof FlintAndSteelItem || item instanceof FireChargeItem || item instanceof BoneMealItem
				|| item instanceof SpawnEggItem) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.FAIL);
			MensagensProtecao.avisar(p, item instanceof BucketItem ? MensagensProtecao.Tipo.AGUA : item instanceof BlockItem ? MensagensProtecao.Tipo.COLOCAR : MensagensProtecao.Tipo.OBJETO);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void ferramenta(BlockEvent.BlockToolModificationEvent event) {
		if (!(event.getLevel() instanceof Level level) || !ColoniaApi.protegido(level, event.getPos()))
			return;
		Player p = event.getPlayer();
		if (PoliticaProtecao.isento(p))
			return;
		event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void pisotear(BlockEvent.FarmlandTrampleEvent event) {
		if (event.getLevel() instanceof Level level && ColoniaApi.protegido(level, event.getPos()))
			event.setCanceled(true);
	}

	// ================================================================ Explosões, fogo, fluidos, pistões

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void explosao(ExplosionEvent.Detonate event) {
		Level level = event.getLevel();
		event.getAffectedBlocks().removeIf(pos -> ColoniaApi.protegido(level, pos));
		event.getAffectedEntities().removeIf(e -> (e instanceof HangingEntity || e instanceof ArmorStand) && ColoniaApi.protegido(level, e.blockPosition()));
	}

	/** Toda colocação de bloco com atualização de vizinhos passa aqui: fogo/lava novos em área protegida são apagados. */
	@SubscribeEvent
	public static void vizinhos(BlockEvent.NeighborNotifyEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		BlockState s = event.getState();
		boolean fogo = s.getBlock() instanceof BaseFireBlock;
		boolean lava = s.getBlock() instanceof LiquidBlock && s.is(Blocks.LAVA);
		if ((fogo || lava) && ColoniaApi.protegido(level, event.getPos()))
			FOGO_A_APAGAR.computeIfAbsent(level.dimension(), k -> new LinkedHashSet<>()).add(event.getPos().immutable());
	}

	@SubscribeEvent
	public static void fimDoTick(ServerTickEvent.Post event) {
		if (FOGO_A_APAGAR.isEmpty())
			return;
		for (var e : FOGO_A_APAGAR.entrySet()) {
			ServerLevel level = event.getServer().getLevel(e.getKey());
			if (level == null)
				continue;
			for (BlockPos pos : e.getValue()) {
				BlockState s = level.getBlockState(pos);
				if (s.getBlock() instanceof BaseFireBlock || s.is(Blocks.LAVA))
					Lote.escrever(level, pos, Blocks.AIR.defaultBlockState());
			}
		}
		FOGO_A_APAGAR.clear();
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void fluidoCriaBloco(BlockEvent.FluidPlaceBlockEvent event) {
		if (event.getLevel() instanceof Level level && ColoniaApi.protegido(level, event.getPos()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void pistao(PistonEvent.Pre event) {
		if (event.getLevel() instanceof Level level && (ColoniaApi.protegido(level, event.getPos()) || ColoniaApi.protegido(level, event.getFaceOffsetPos())))
			event.setCanceled(true);
	}

	// ================================================================ Mobs e entidades

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void griefing(EntityMobGriefingEvent event) {
		Entity e = event.getEntity();
		if (ColoniaApi.protegido(e.level(), e.blockPosition()))
			event.setCanGrief(false);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void mobQuebra(LivingDestroyBlockEvent event) {
		if (ColoniaApi.protegido(event.getEntity().level(), event.getPos()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void atacarDecoracao(AttackEntityEvent event) {
		Entity alvo = event.getTarget();
		if ((alvo instanceof HangingEntity || alvo instanceof ArmorStand) && !alvo.level().isClientSide() && PoliticaProtecao.impede(event.getEntity(), alvo.level(), alvo.blockPosition())) {
			event.setCanceled(true);
			MensagensProtecao.avisar(event.getEntity(), MensagensProtecao.Tipo.OBJETO);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void interagirDecoracao(PlayerInteractEvent.EntityInteract event) {
		Entity alvo = event.getTarget();
		if ((alvo instanceof HangingEntity || alvo instanceof ArmorStand) && !alvo.level().isClientSide() && PoliticaProtecao.impede(event.getEntity(), alvo.level(), alvo.blockPosition())) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.FAIL);
		}
	}

	@SubscribeEvent
	public static void saiu(PlayerEvent.PlayerLoggedOutEvent event) {
		MensagensProtecao.esquecer(event.getEntity().getUUID());
	}
}
