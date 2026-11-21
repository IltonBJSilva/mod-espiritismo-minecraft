package net.mcreator.modespiritismominecraft.espiritismo.protecao;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.colonia.ColoniaApi;

/**
 * Política de proteção de Nosso Lar, centralizada e sem efeitos colaterais (os eventos só consultam).
 * <ul>
 * <li>Jogador comum: não quebra, não coloca, não substitui, não usa ferramentas/baldes/isqueiro/farinha de osso em
 * blocos da colônia; pode abrir portas, alçapões e portões, apertar botões, tocar sinos, usar atris (de mão vazia),
 * ler placas, usar a mesa de trabalho, sentar em bancos e interagir com os blocos do próprio mod (livros, tribuna,
 * jarra etc., que cuidam das próprias regras).</li>
 * <li>Exceção administrativa: operador (nível 2) EM MODO CRIATIVO — toda modificação fica registrada no log.</li>
 * <li>Sistemas do mod (construção da colônia, NPCs) escrevem direto no mundo e não passam por esses eventos.</li>
 * </ul>
 */
public final class PoliticaProtecao {
	private PoliticaProtecao() {
	}

	public static boolean isento(boolean operador, boolean criativo) {
		return operador && criativo;
	}

	public static boolean isento(Player p) {
		return p != null && isento(p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER), p.isCreative());
	}

	/** O jogador (null = entidade/ambiente) está impedido de alterar o bloco em {@code pos}? */
	public static boolean impede(Player p, Level level, BlockPos pos) {
		return ColoniaApi.protegido(level, pos) && !isento(p);
	}

	/** Pode usar (clicar com o botão direito) este bloco protegido? */
	public static boolean usoPermitido(BlockState s, boolean maoVazia) {
		var id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
		if (!"minecraft".equals(id.getNamespace()))
			return true; // blocos do mod e de outros mods tratam as próprias regras
		var b = s.getBlock();
		if (b instanceof DoorBlock || b instanceof TrapDoorBlock || b instanceof FenceGateBlock)
			return !s.is(Blocks.IRON_DOOR) && !s.is(Blocks.IRON_TRAPDOOR);
		if (b instanceof ButtonBlock || b instanceof BellBlock || b instanceof SignBlock || b instanceof CraftingTableBlock)
			return true;
		if (b instanceof LecternBlock)
			return maoVazia; // de mão vazia: ler/clicar; com livro na mão colocaria o livro
		if (b instanceof StairBlock || b instanceof SlabBlock)
			return maoVazia; // sentar
		return s.is(Blocks.ENCHANTING_TABLE) || s.is(Blocks.CARTOGRAPHY_TABLE) || s.is(Blocks.LOOM) || s.is(Blocks.STONECUTTER);
	}
}
