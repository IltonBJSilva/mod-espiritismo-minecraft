package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;

import net.mcreator.modespiritismominecraft.espiritismo.itens.ItensApi;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;

/** Eventos da área "populacao" (barramento do jogo). */
@EventBusSubscriber
public final class EventosPopulacao {
	private EventosPopulacao() {
	}

	/** Povoamento por proximidade e visitas da jornada: a cada 2 s por jogador em Nosso Lar (escalonado pelo id). */
	@SubscribeEvent
	public static void aoTick(PlayerTickEvent.Post event) {
		Player p = event.getEntity();
		if (p.level().isClientSide() || p.level().dimension() != Planos.NOSSO_LAR || (p.tickCount + p.getId()) % Povoamento.INTERVALO != 0)
			return;
		Povoamento.tick(p);
		JornadaRecemChegado.tick(p);
	}

	/** Recusa cópias de personagens nomeados (um de cada por nível), inclusive ao carregar chunks do disco. */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void aoEntrar(EntityJoinLevelEvent event) {
		if (event.getEntity() instanceof MoradorEntity m && event.getLevel() instanceof ServerLevel sl && Povoamento.duplicata(sl, m))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void aoSairDoNivel(EntityLeaveLevelEvent event) {
		if (event.getEntity() instanceof MoradorEntity m && event.getLevel() instanceof ServerLevel sl)
			Povoamento.aoSair(sl, m);
	}

	/** Registra as obras lidas (memória dos NPCs: "já estudou X?"). */
	@SubscribeEvent(receiveCanceled = true)
	public static void aoUsarItem(PlayerInteractEvent.RightClickItem event) {
		Player p = event.getEntity();
		if (p.level().isClientSide())
			return;
		String obra = obra(event.getItemStack());
		if (obra != null)
			PopulacaoApi.registrarLeitura(p, obra);
	}

	/** Id da obra lida, ou null. O Livro dos Espíritos do MCreator ("livrodos_espiritos") conta como "o_livro_dos_espiritos". */
	public static String obra(ItemStack stack) {
		if (stack.isEmpty())
			return null;
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!RegistroPopulacao.MODID.equals(id.getNamespace()))
			return null;
		if (id.getPath().equals("livrodos_espiritos"))
			return "o_livro_dos_espiritos";
		return ItensApi.ehObra(stack) ? id.getPath() : null;
	}

	/** Ao chegar a Nosso Lar pela primeira vez: orienta o início da jornada do recém-chegado. */
	@SubscribeEvent
	public static void aoMudarDeDimensao(PlayerEvent.PlayerChangedDimensionEvent event) {
		Player p = event.getEntity();
		if (event.getTo() != Planos.NOSSO_LAR || p.level().isClientSide())
			return;
		if (JornadaRecemChegado.etapa(p) == JornadaRecemChegado.RECEPCAO && Memoria.marcar(p, "chegou:nosso_lar")) {
			Dialogos.narrar(p, "Você está em Nosso Lar, colônia espiritual descrita no livro de André Luiz (psicografia de Francisco Cândido Xavier). "
					+ "Aqui ninguém tem pressa: converse com os moradores, conheça os lugares e descanse.");
			Dialogos.objetivo(p, "Jornada do recém-chegado", JornadaRecemChegado.OBJETIVOS.get(0));
		}
		JornadaRecemChegado.apontar(p);
	}
}
