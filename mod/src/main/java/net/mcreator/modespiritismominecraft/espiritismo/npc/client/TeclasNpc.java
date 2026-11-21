package net.mcreator.modespiritismominecraft.espiritismo.npc.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import net.mcreator.modespiritismominecraft.espiritismo.npc.Prece;
import net.mcreator.modespiritismominecraft.espiritismo.client.TeclasEspirituais;

/** Tecla B: fazer uma prece. Mesma categoria "Espiritismo" das outras teclas do mod (registrada em TeclasEspirituais). */
@EventBusSubscriber(value = Dist.CLIENT)
public final class TeclasNpc {
	public static final KeyMapping PRECE = new KeyMapping("key.mod_espiritismo_minecraft.prece", InputConstants.KEY_B, TeclasEspirituais.CATEGORIA);

	private TeclasNpc() {
	}

	@SubscribeEvent
	public static void registrar(RegisterKeyMappingsEvent event) {
		event.register(PRECE);
	}

	@SubscribeEvent
	public static void aoTick(ClientTickEvent.Post event) {
		while (PRECE.consumeClick()) {
			if (Minecraft.getInstance().player != null)
				ClientPacketDistributor.sendToServer(new Prece.Orar());
		}
	}
}
