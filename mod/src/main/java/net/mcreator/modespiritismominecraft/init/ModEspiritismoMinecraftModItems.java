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
	public static final DeferredItem<Item> LIVRODOS_ESPIRITOS;
	public static final DeferredItem<Item> AGUA_FLUIDIFICADA;
	public static final DeferredItem<Item> ALLAN_KARDEC_SPAWN_EGG;
	public static final DeferredItem<Item> CHICO_XAVIER_SPAWN_EGG;
	public static final DeferredItem<Item> HAROLDO_DUTRA_SPAWN_EGG;
	public static final DeferredItem<Item> LIVRO;
	static {
		LIVRODOS_ESPIRITOS = register("livrodos_espiritos", LivrodosEspiritosItem::new);
		AGUA_FLUIDIFICADA = register("agua_fluidificada", AguaFluidificadaItem::new);
		ALLAN_KARDEC_SPAWN_EGG = register("allan_kardec_spawn_egg", properties -> new SpawnEggItem(properties.spawnEgg(ModEspiritismoMinecraftModEntities.ALLAN_KARDEC.get())));
		CHICO_XAVIER_SPAWN_EGG = register("chico_xavier_spawn_egg", properties -> new SpawnEggItem(properties.spawnEgg(ModEspiritismoMinecraftModEntities.CHICO_XAVIER.get())));
		HAROLDO_DUTRA_SPAWN_EGG = register("haroldo_dutra_spawn_egg", properties -> new SpawnEggItem(properties.spawnEgg(ModEspiritismoMinecraftModEntities.HAROLDO_DUTRA.get())));
		LIVRO = block(ModEspiritismoMinecraftModBlocks.LIVRO);
	}

	// Start of user code block custom items
	// End of user code block custom items
	private static <I extends Item> DeferredItem<I> register(String name, Function<Item.Properties, ? extends I> supplier) {
		return REGISTRY.registerItem(name, supplier, Item.Properties::new);
	}

	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return block(block, new Item.Properties());
	}

	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block, Item.Properties properties) {
		return REGISTRY.registerItem(block.getId().getPath(), prop -> new BlockItem(block.get(), prop), () -> properties);
	}
}