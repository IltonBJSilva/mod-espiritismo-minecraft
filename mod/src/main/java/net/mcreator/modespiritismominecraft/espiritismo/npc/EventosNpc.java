package net.mcreator.modespiritismominecraft.espiritismo.npc;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.registries.BuiltInRegistries;

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;

/** Eventos da área "npc" (barramento do jogo). */
@EventBusSubscriber
public final class EventosNpc {
	private EventosNpc() {
	}

	/** Trabalhadores encarnados não podem ser feridos (só /kill e o vazio); agredi-los custa -5 de moralidade. */
	@SubscribeEvent
	public static void aoReceberDano(LivingIncomingDamageEvent event) {
		Entity alvo = event.getEntity();
		if (alvo.level().isClientSide() || !(alvo instanceof TrabalhadorEntity t) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return;
		event.setCanceled(true);
		if (event.getSource().getEntity() instanceof Player player && Moralidade.pronto(player, "npc_agredir", 40)) {
			Moralidade.alterar(player, -5, "Agrediu um trabalhador do bem");
			Falas.falar(player, t.papel().nome, "Que a paz esteja com você, meu irmão. Se precisar conversar, estou aqui.");
		}
	}

	@SubscribeEvent
	public static void aoConversar(PlayerInteractEvent.EntityInteract event) {
		Entity alvo = event.getTarget();
		if (!(alvo instanceof TrabalhadorEntity || alvo instanceof EspiritoEntity) || event.getHand() != InteractionHand.MAIN_HAND)
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
		Player player = event.getEntity();
		if (player.level().isClientSide() || !Moralidade.pronto(player, "npc_conversar", 20))
			return;
		if (alvo instanceof TrabalhadorEntity t) {
			if (t.papel() == TrabalhadorEntity.Papel.DIVALDO)
				net.mcreator.modespiritismominecraft.espiritismo.Jornada.conquista(player, "mansao");
			if (!net.mcreator.modespiritismominecraft.espiritismo.EventosEspirituais.tutorialTrabalhador(player, t))
				Atendimentos.conversar(player, t);
		}
		else if (alvo instanceof EspiritoEntity e)
			Atendimentos.conversar(player, e);
	}

	/** Ao ler uma obra (o Livro dos Espíritos e, se existirem, as obras da área "itens"), o mentor pode aparecer. */
	@SubscribeEvent(receiveCanceled = true)
	public static void aoUsarItem(PlayerInteractEvent.RightClickItem event) {
		Player player = event.getEntity();
		if (player.level().isClientSide() || !ehObra(event.getItemStack()))
			return;
		Espiritos.aoLer(player);
	}

	private static boolean ehObra(ItemStack stack) {
		if (stack.isEmpty())
			return false;
		if (net.mcreator.modespiritismominecraft.espiritismo.itens.ItensApi.ehObra(stack))
			return true;
		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!id.getNamespace().equals(RegistroNpc.MODID))
			return false;
		String p = id.getPath();
		return p.startsWith("livro") && !p.equals("livro") && !p.startsWith("livro_1857") || p.contains("evangelho") || p.contains("genese") || p.contains("ceu_e_inferno") || p.contains("nosso_lar");
	}

	@SubscribeEvent
	public static void aoTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide())
			return;
		Atendimentos.tickPalestra(player);
		Espiritos.tickJogador(player);
	}

	@SubscribeEvent
	public static void aoSair(PlayerEvent.PlayerLoggedOutEvent event) {
		Atendimentos.encerrarPalestra(event.getEntity());
	}
}
