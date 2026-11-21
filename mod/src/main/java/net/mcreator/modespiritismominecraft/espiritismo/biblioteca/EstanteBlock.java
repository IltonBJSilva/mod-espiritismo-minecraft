package net.mcreator.modespiritismominecraft.espiritismo.biblioteca;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Estante "Biblioteca Espírita": clicar abre o catálogo. Pode ser colocada em centros, casas e na biblioteca de Nosso Lar. */
public class EstanteBlock extends Block {
	public EstanteBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer sp) {
			level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
			BibliotecaApi.abrirCatalogo(sp);
		}
		return InteractionResult.SUCCESS;
	}
}
