
package net.mcreator.modespiritismominecraft.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class LivrodosEspiritosItem extends Item {
	public LivrodosEspiritosItem(Item.Properties properties) {
		super(properties.rarity(Rarity.COMMON).stacksTo(64));
	}
}
