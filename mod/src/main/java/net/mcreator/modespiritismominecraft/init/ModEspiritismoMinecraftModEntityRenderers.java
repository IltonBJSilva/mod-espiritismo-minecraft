/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.modespiritismominecraft.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.modespiritismominecraft.client.renderer.HaroldoDutraRenderer;
import net.mcreator.modespiritismominecraft.client.renderer.ChicoXavierRenderer;
import net.mcreator.modespiritismominecraft.client.renderer.AllanKardecRenderer;

@EventBusSubscriber(Dist.CLIENT)
public class ModEspiritismoMinecraftModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEspiritismoMinecraftModEntities.ALLAN_KARDEC.get(), AllanKardecRenderer::new);
		event.registerEntityRenderer(ModEspiritismoMinecraftModEntities.CHICO_XAVIER.get(), ChicoXavierRenderer::new);
		event.registerEntityRenderer(ModEspiritismoMinecraftModEntities.HAROLDO_DUTRA.get(), HaroldoDutraRenderer::new);
	}
}