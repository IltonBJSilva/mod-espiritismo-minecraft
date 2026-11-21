package net.mcreator.modespiritismominecraft.espiritismo.itens;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;

import java.util.Map;

/**
 * Bloco decorativo com modelo JSON próprio, orientado por FACING (a frente fica voltada para quem coloca, como o
 * atril vanilla) e com forma (VoxelShape) que acompanha o modelo. A forma é dada para FACING=north.
 */
public class BlocoOrientado extends HorizontalDirectionalBlock {
	private final VoxelShape formaNorte;
	private final Map<Direction, VoxelShape> formas;
	/** Texto educativo mostrado ao clicar com a mão vazia (null = nenhum). */
	private final String dica;

	public BlocoOrientado(BlockBehaviour.Properties properties, VoxelShape formaNorte) {
		this(properties, formaNorte, null);
	}

	public BlocoOrientado(BlockBehaviour.Properties properties, VoxelShape formaNorte, String dica) {
		super(properties);
		this.formaNorte = formaNorte;
		this.dica = dica;
		this.formas = Shapes.rotateHorizontal(formaNorte);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return simpleCodec(p -> new BlocoOrientado(p, formaNorte, dica));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return formas.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (dica == null || !player.getMainHandItem().isEmpty())
			return InteractionResult.PASS;
		if (!level.isClientSide() && Moralidade.pronto(player, "dica_bloco", 40))
			player.sendSystemMessage(Component.literal(dica).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}
}
