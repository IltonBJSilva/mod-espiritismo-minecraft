package net.mcreator.modespiritismominecraft.espiritismo.biblioteca;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Catálogo de bolso da Biblioteca Espírita: abre a tela da biblioteca em qualquer lugar. */
public class CatalogoItem extends Item {
	public CatalogoItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer sp)
			BibliotecaApi.abrirCatalogo(sp);
		return InteractionResult.SUCCESS;
	}
}
