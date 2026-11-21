package net.mcreator.modespiritismominecraft.espiritismo.itens;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;

/**
 * Obra legível: abre a tela de livro vanilla (o item já traz WRITTEN_BOOK_CONTENT como componente padrão).
 * Estudar rende um pequeno ganho de rotina (com recarga), como o Livro dos Espíritos do MCreator.
 */
public class ObraItem extends Item {
	public ObraItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		player.openItemGui(stack, hand);
		player.awardStat(Stats.ITEM_USED.get(this));
		if (!level.isClientSide()) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
			if (Moralidade.pronto(player, "estudar_obra", 6000))
				Moralidade.ganhoRotina(player, 1, "Estudou uma obra espírita");
		}
		return InteractionResult.SUCCESS;
	}
}
