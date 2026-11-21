package net.mcreator.modespiritismominecraft.espiritismo.itens;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModItems;

/**
 * Jarra de vidro com água. Garrafa vazia → água; fluidificada só com prece recente ou passista por perto
 * (ver {@link Fluidificacao}). Mão vazia → o jogador faz uma prece junto à jarra.
 */
public class JarraAguaBlock extends BlocoOrientado {
	private final VoxelShape forma;

	public JarraAguaBlock(BlockBehaviour.Properties properties, VoxelShape formaNorte) {
		super(properties, formaNorte);
		this.forma = formaNorte;
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return simpleCodec(p -> new JarraAguaBlock(p, forma));
	}

	/** Enche uma garrafa na jarra em {@code pos}. Retorna o item resultante (público para os testes e o integrador). */
	public static ItemStack encher(ServerLevel level, BlockPos pos, Player player) {
		if (Fluidificacao.podeFluidificar(level, pos)) {
			level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.15, 0.2, 0.15, 0.01);
			if (player != null)
				player.sendSystemMessage(Component.literal("A água recebeu os fluidos benéficos da prece e do amparo espiritual. É água fluidificada.")
						.withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
			return new ItemStack(ModEspiritismoMinecraftModItems.AGUA_FLUIDIFICADA.get());
		}
		if (player != null)
			player.sendSystemMessage(Fluidificacao.explicacaoAguaComum());
		return PotionContents.createItemStack(Items.POTION, Potions.WATER);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(Items.GLASS_BOTTLE))
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (level instanceof ServerLevel sl) {
			ItemStack resultado = encher(sl, pos, player);
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, resultado));
			level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel sl))
			return InteractionResult.SUCCESS;
		if (!Moralidade.pronto(player, "prece_jarra", 100))
			return InteractionResult.SUCCESS;
		if (Fluidificacao.registrarPrece(player)) {
			Fluidificacao.registrarPrece(sl, pos);
			sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.005);
			player.sendSystemMessage(Component.literal("Você faz uma prece em silêncio, pedindo a Deus amparo para quem beber desta água. "
					+ "Por alguns minutos, a água desta jarra sairá fluidificada.").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
		} else {
			player.sendSystemMessage(Component.literal("Toda prece sincera é ouvida. Mas a água recebe os fluidos de quem ora, e os seus ainda "
					+ "estão pesados: pratique o bem e a caridade (moralidade 50 ou mais) e tente de novo.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		}
		return InteractionResult.SUCCESS;
	}
}
