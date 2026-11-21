package net.mcreator.modespiritismominecraft.espiritismo;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

/**
 * Bússola Espiritual: aponta (pela seta do HUD) o Centro Espírita mais próximo.
 * Agachado, procura os locais especiais (casas de Kardec, Chico, Mansão do Caminho...).
 * Clique de novo perto do alvo para limpar.
 */
public class BussolaEspiritual extends Item {
	public static final TagKey<Structure> CENTROS = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, "centros_espiritas"));
	public static final TagKey<Structure> ESPECIAIS = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, "locais_especiais"));

	public BussolaEspiritual(Item.Properties props) {
		super(props.stacksTo(1));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel sl))
			return InteractionResult.SUCCESS;
		if (!Moralidade.pronto(player, "bussola", 60)) {
			player.sendOverlayMessage(Component.literal("A bússola ainda está se orientando...").withStyle(ChatFormatting.GRAY));
			return InteractionResult.FAIL;
		}
		boolean especiais = player.isShiftKeyDown();
		BlockPos alvo = sl.findNearestMapStructure(especiais ? ESPECIAIS : CENTROS, player.blockPosition(), especiais ? 200 : 100, false);
		// No mundo normal, o Centro do tutorial também conta.
		if (!especiais && sl == sl.getServer().overworld()) {
			BlockPos tutorial = Jornada.centro(sl);
			if (tutorial != null && (alvo == null || tutorial.distSqr(player.blockPosition()) < alvo.distSqr(player.blockPosition())))
				alvo = tutorial;
		}
		if (alvo == null) {
			player.sendSystemMessage(Component.literal(especiais ? "Nenhum local especial foi encontrado nesta região. Viaje mais longe." : "Nenhum Centro Espírita por perto.").withStyle(ChatFormatting.GRAY));
			return InteractionResult.FAIL;
		}
		Objetivo.definir(player, alvo);
		sl.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.5F);
		player.sendSystemMessage(Component.literal((especiais ? "Um local especial" : "Um Centro Espírita") + Jornada.direcao(player, alvo.getCenter()) + ". Siga a seta no canto da tela.")
				.withStyle(ChatFormatting.AQUA));
		return InteractionResult.SUCCESS;
	}
}
