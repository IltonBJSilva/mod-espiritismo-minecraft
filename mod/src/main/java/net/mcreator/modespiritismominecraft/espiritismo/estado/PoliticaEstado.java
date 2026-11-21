package net.mcreator.modespiritismominecraft.espiritismo.estado;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.minecraft.util.TriState;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.ConstrucoesUmbral;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/**
 * Política "sem violência" (PROMPT §5.6), aplicada NO SERVIDOR (e também no cliente quando o evento dispara lá, só
 * para evitar animações): enquanto {@link Estados#semViolencia} for verdadeiro o jogador não ataca entidades nem
 * jogadores, não causa dano indireto (projéteis, armas de arremesso, fogo), não quebra nem coloca blocos e não usa
 * baldes/isqueiro/ovos de criação. Pode andar, conversar, usar portas/atris/camas e ler.
 * <p>
 * Também protege as construções dos Postos de Socorro do Umbral contra quebra/colocação/explosões (§14.1),
 * independentemente do estado (exceto criativo).
 * <p>
 * Esta política é do ESTADO; a proteção territorial de Nosso Lar é da área "colonia" ({@code ColoniaApi.protegido}).
 */
@EventBusSubscriber
public final class PoliticaEstado {
	private static final Map<UUID, Long> AVISADO = new HashMap<>();

	private PoliticaEstado() {
	}

	/** Item considerado ofensivo/destrutivo (bloqueado ao usar enquanto sem violência). */
	public static boolean itemOfensivo(ItemStack stack) {
		if (stack.isEmpty())
			return false;
		Item i = stack.getItem();
		return i instanceof ProjectileWeaponItem || i instanceof TridentItem || i instanceof ProjectileItem || i instanceof MaceItem || i instanceof BucketItem
				|| i instanceof FlintAndSteelItem || i instanceof FireChargeItem || i instanceof SpawnEggItem || i instanceof BlockItem;
	}

	private static void avisar(Player p) {
		if (p.level().isClientSide())
			return;
		long agora = p.level().getGameTime();
		Long ultimo = AVISADO.get(p.getUUID());
		if (ultimo != null && agora - ultimo < 60)
			return;
		AVISADO.put(p.getUUID(), agora);
		if (AVISADO.size() > 256)
			AVISADO.clear();
		p.sendOverlayMessage(Component.literal("Em perturbação a força não ajuda: respire, converse, ore ou peça auxílio (tecla H).").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}

	private static boolean bloqueia(Player p) {
		return p != null && Estados.semViolencia(p);
	}

	// ---------------------------------------------------------------- Ataques diretos e dano indireto

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void atacar(AttackEntityEvent event) {
		if (bloqueia(event.getEntity())) {
			event.setCanceled(true);
			avisar(event.getEntity());
		}
	}

	/** Pega todos os caminhos de dano em que a "fonte" é um jogador sem violência: golpe, flecha, tridente, poção, fogo de isqueiro... */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void dano(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide() || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return;
		Entity fonte = event.getSource().getEntity();
		if (fonte instanceof Player p && p != event.getEntity() && bloqueia(p)) {
			event.setCanceled(true);
			avisar(p);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void projetil(ProjectileImpactEvent event) {
		Projectile pr = event.getProjectile();
		if (pr.level().isClientSide())
			return;
		if (pr.getOwner() instanceof Player p && bloqueia(p)) {
			event.setCanceled(true);
			pr.discard();
		}
	}

	// ---------------------------------------------------------------- Blocos

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void quebrar(BreakBlockEvent event) {
		Player p = event.getPlayer();
		if (bloqueia(p)) {
			event.setCanceled(true);
			avisar(p);
			return;
		}
		if (p != null && !p.isCreative() && event.getLevel() instanceof Level l && postoProtegido(l, event.getPos())) {
			event.setCanceled(true);
			avisarPosto(p);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void colocar(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getEntity() instanceof Player p))
			return;
		if (bloqueia(p)) {
			event.setCanceled(true);
			avisar(p);
			return;
		}
		if (!p.isCreative() && event.getLevel() instanceof Level l && postoProtegido(l, event.getPos())) {
			event.setCanceled(true);
			avisarPosto(p);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void cliqueEsquerdo(PlayerInteractEvent.LeftClickBlock event) {
		if (bloqueia(event.getEntity()))
			event.setCanceled(true);
	}

	/** Usar blocos (portas, atris, camas) continua liberado; só o USO DO ITEM ofensivo/de construção é negado. */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void cliqueDireitoBloco(PlayerInteractEvent.RightClickBlock event) {
		if (bloqueia(event.getEntity()) && itemOfensivo(event.getItemStack())) {
			event.setUseItem(TriState.FALSE);
			avisar(event.getEntity());
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void usarItem(PlayerInteractEvent.RightClickItem event) {
		if (bloqueia(event.getEntity()) && itemOfensivo(event.getItemStack())) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.FAIL);
			avisar(event.getEntity());
		}
	}

	// ---------------------------------------------------------------- Postos de Socorro do Umbral

	private static boolean postoProtegido(Level level, BlockPos pos) {
		return level.dimension() == Planos.UMBRAL && ConstrucoesUmbral.dentroDePosto(level, pos);
	}

	private static void avisarPosto(Player p) {
		if (!p.level().isClientSide())
			p.sendOverlayMessage(Component.literal("As instalações do Posto de Socorro são cuidadas pelos trabalhadores e servem a todos.").withStyle(ChatFormatting.GRAY));
	}

	@SubscribeEvent
	public static void explosao(ExplosionEvent.Detonate event) {
		Level level = event.getLevel();
		if (level.isClientSide() || level.dimension() != Planos.UMBRAL)
			return;
		event.getAffectedBlocks().removeIf(pos -> ConstrucoesUmbral.dentroDePosto(level, pos));
	}
}
