package net.mcreator.modespiritismominecraft.espiritismo.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.resources.Identifier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import net.mcreator.modespiritismominecraft.espiritismo.Pacotes;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

/** Tecla V: usar a habilidade ativa da classe. Pode ser trocada em Opções → Controles. */
@EventBusSubscriber(value = Dist.CLIENT)
public final class TeclasEspirituais {
	public static final KeyMapping.Category CATEGORIA = new KeyMapping.Category(Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, "espiritismo"));
	public static final KeyMapping HABILIDADE = new KeyMapping("key.mod_espiritismo_minecraft.habilidade", InputConstants.KEY_V, CATEGORIA);

	private TeclasEspirituais() {
	}

	@SubscribeEvent
	public static void registrar(RegisterKeyMappingsEvent event) {
		event.registerCategory(CATEGORIA);
		event.register(HABILIDADE);
	}

	@SubscribeEvent
	public static void aoTick(ClientTickEvent.Post event) {
		while (HABILIDADE.consumeClick()) {
			if (Minecraft.getInstance().player != null)
				ClientPacketDistributor.sendToServer(new Pacotes.UsarHabilidade());
		}
	}
}
