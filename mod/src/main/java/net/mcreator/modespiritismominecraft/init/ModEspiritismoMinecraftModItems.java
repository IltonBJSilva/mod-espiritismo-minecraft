
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.modespiritismominecraft.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import net.mcreator.modespiritismominecraft.item.LivrodosEspiritosItem;
import net.mcreator.modespiritismominecraft.item.AguaFluidificadaItem;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.function.Function;

public class ModEspiritismoMinecraftModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(ModEspiritismoMinecraftMod.MODID);
	public static final DeferredItem<Item> LIVRODOS_ESPIRITOS = register("livrodos_espiritos", LivrodosEspiritosItem::new);
	public static final DeferredItem<Item> AGUA_FLUIDIFICADA = register("agua_fluidificada", AguaFluidificadaItem::new);
	public static final DeferredItem<Item> ALLAN_KARDEC_SPAWN_EGG = register("allan_kardec_spawn_egg", properties -> new SpawnEggItem(ModEspiritismoMinecraftModEntities.ALLAN_KARDEC.get(), properties));
	public static final DeferredItem<Item> CHICO_XAVIER_SPAWN_EGG = register("chico_xavier_spawn_egg", properties -> new SpawnEggItem(ModEspiritismoMinecraftModEntities.CHICO_XAVIER.get(), properties));
	public static final DeferredItem<Item> HAROLDO_DUTRA_SPAWN_EGG = register("haroldo_dutra_spawn_egg", properties -> new SpawnEggItem(ModEspiritismoMinecraftModEntities.HAROLDO_DUTRA.get(), properties));
	public static final DeferredItem<Item> LIVRO = block(ModEspiritismoMinecraftModBlocks.LIVRO);

	// Start of user code block custom items
	// End of user code block custom items
	private static <I extends Item> DeferredItem<I> register(String name, Function<Item.Properties, ? extends I> supplier) {
		return REGISTRY.registerItem(name, supplier, new Item.Properties());
	}

	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return REGISTRY.registerItem(block.getId().getPath(), properties -> new BlockItem(block.get(), properties), new Item.Properties());
	}
}
