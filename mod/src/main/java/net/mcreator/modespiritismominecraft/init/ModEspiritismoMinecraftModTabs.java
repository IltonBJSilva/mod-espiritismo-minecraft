
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.modespiritismominecraft.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModEspiritismoMinecraftModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModEspiritismoMinecraftMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MOD_ESPIRITISMO = REGISTRY.register("mod_espiritismo", () -> CreativeModeTab.builder().title(Component.translatable("item_group.mod_espiritismo_minecraft.mod_espiritismo"))
			.icon(() -> new ItemStack(ModEspiritismoMinecraftModItems.LIVRODOS_ESPIRITOS.get())).displayItems((parameters, tabData) -> {
				tabData.accept(ModEspiritismoMinecraftModItems.LIVRODOS_ESPIRITOS.get());
				tabData.accept(ModEspiritismoMinecraftModBlocks.LIVRO.get().asItem());
			}).withSearchBar().build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
			tabData.accept(ModEspiritismoMinecraftModItems.ALLAN_KARDEC_SPAWN_EGG.get());
			tabData.accept(ModEspiritismoMinecraftModItems.CHICO_XAVIER_SPAWN_EGG.get());
			tabData.accept(ModEspiritismoMinecraftModItems.HAROLDO_DUTRA_SPAWN_EGG.get());
		}
	}
}
