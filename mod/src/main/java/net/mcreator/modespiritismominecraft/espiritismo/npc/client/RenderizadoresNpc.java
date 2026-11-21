package net.mcreator.modespiritismominecraft.espiritismo.npc.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.resources.Identifier;

import net.mcreator.modespiritismominecraft.espiritismo.npc.TrabalhadorEntity;
import net.mcreator.modespiritismominecraft.espiritismo.npc.RegistroNpc;
import net.mcreator.modespiritismominecraft.espiritismo.npc.EspiritoEntity;

/** Registra os renderizadores das entidades da área "npc". Skins em textures/npc/<id>.png. */
@EventBusSubscriber(value = Dist.CLIENT)
public final class RenderizadoresNpc {
	private RenderizadoresNpc() {
	}

	static Identifier skin(String id) {
		return Identifier.fromNamespaceAndPath(RegistroNpc.MODID, "textures/npc/" + id + ".png");
	}

	@SubscribeEvent
	public static void registrar(EntityRenderersEvent.RegisterRenderers event) {
		RegistroNpc.TRABALHADORES.forEach((papel, holder) -> event.registerEntityRenderer(holder.get(), ctx -> new TrabalhadorRenderer(ctx, skin(papel.id))));
		RegistroNpc.ESPIRITOS.forEach((tipo, holder) -> event.registerEntityRenderer(holder.get(), ctx -> switch (tipo) {
			case MENTOR -> new EspiritoRenderer(ctx, skin(tipo.id), 0xDDF4FF, 0.55F, true);
			case SOCORRISTA -> new EspiritoRenderer(ctx, skin(tipo.id), 0xE6FFEE, 0.6F, true);
			case OBSESSOR -> new EspiritoRenderer(ctx, skin(tipo.id), 0x8A7A99, 0.5F, false);
			case SOFREDOR -> new EspiritoRenderer(ctx, skin(tipo.id), 0xB8BCC4, 0.45F, false);
			case ZOMBETEIRO -> new EspiritoRenderer(ctx, skin(tipo.id), 0xF0D8FF, 0.55F, false);
		}));
	}
}
