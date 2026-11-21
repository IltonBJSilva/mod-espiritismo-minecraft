package net.mcreator.modespiritismominecraft.espiritismo.protecao;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/** Mensagens gentis (na barra de ação) quando a proteção de Nosso Lar impede algo. Variam e não fazem spam. */
public final class MensagensProtecao {
	public enum Tipo {
		EDIFICIO, FLOR, ARVORE, AGUA, FOGO, COLOCAR, OBJETO
	}

	private static final Map<Tipo, String[]> TEXTOS = Map.of(Tipo.EDIFICIO,
			new String[]{"Este ambiente é preservado para o acolhimento e o trabalho coletivo. As construções de Nosso Lar não podem ser modificadas.",
					"Cada parede aqui foi erguida pelo esforço de muitos. Em Nosso Lar, cuidamos juntos do que é de todos.",
					"As construções da colônia são preservadas. Que tal conversar com alguém ou visitar um dos Ministérios?"},
			Tipo.FLOR,
			new String[]{"As flores de Nosso Lar alegram a todos que passam. Elas ficam onde estão.", "Os jardins da colônia são cuidados com carinho: aprecie, sem colher.",
					"Esta planta faz parte do jardim comum. Admire-a e deixe-a para os próximos visitantes."},
			Tipo.ARVORE, new String[]{"As árvores dos bosques de Nosso Lar dão sombra a todos. Elas são preservadas.", "Os bosques da colônia são lugares de descanso: aqui nada se corta."},
			Tipo.AGUA, new String[]{"As águas de Nosso Lar são cuidadas pelo Ministério da União Divina. Não é possível alterá-las.",
					"A água da colônia é tratada com zelo: baldes não são usados aqui."},
			Tipo.FOGO, new String[]{"Em Nosso Lar não se acende fogo: a colônia é um lugar de paz.", "O fogo não tem lugar aqui. A luz da colônia vem de outra fonte."},
			Tipo.COLOCAR, new String[]{"Não é possível construir em Nosso Lar: a colônia já está organizada para o bem de todos.",
					"Aqui não se constrói por conta própria. Se quiser ajudar, procure os trabalhadores da colônia.",
					"Os blocos que você traz ficam guardados. Em Nosso Lar, o trabalho se faz em equipe."},
			Tipo.OBJETO, new String[]{"Este objeto faz parte da colônia e é preservado.", "Melhor deixar como está: este objeto é de uso comum."});

	private static final Map<UUID, Long> ULTIMA = new HashMap<>();
	private static final Map<UUID, Integer> VEZ = new HashMap<>();
	/** Intervalo mínimo entre mensagens para o mesmo jogador (ticks). */
	public static final int INTERVALO = 40;

	private MensagensProtecao() {
	}

	public static Tipo tipoDe(BlockState s) {
		if (s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS) || s.is(BlockTags.FLOWER_POTS) || s.is(Blocks.LILY_PAD)
				|| s.is(Blocks.FERN) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT_PATH))
			return Tipo.FLOR;
		if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES))
			return Tipo.ARVORE;
		if (!s.getFluidState().isEmpty())
			return Tipo.AGUA;
		return Tipo.EDIFICIO;
	}

	/** Envia (se não enviou há pouco) uma mensagem do tipo dado. Devolve true se enviou. */
	public static boolean avisar(Player player, Tipo tipo) {
		if (!(player instanceof ServerPlayer p))
			return false;
		long agora = p.level().getGameTime();
		Long ult = ULTIMA.get(p.getUUID());
		if (ult != null && agora - ult < INTERVALO && agora >= ult)
			return false;
		ULTIMA.put(p.getUUID(), agora);
		String[] textos = TEXTOS.get(tipo);
		int v = VEZ.merge(p.getUUID(), 1, Integer::sum);
		p.sendOverlayMessage(Component.literal(textos[v % textos.length]).withStyle(ChatFormatting.AQUA));
		return true;
	}

	static void esquecer(UUID id) {
		ULTIMA.remove(id);
		VEZ.remove(id);
	}

	/** Para testes: texto da mensagem n de um tipo. */
	public static String texto(Tipo tipo, int n) {
		String[] t = TEXTOS.get(tipo);
		return t[Math.floorMod(n, t.length)];
	}

	public static int variacoes(Tipo tipo) {
		return TEXTOS.get(tipo).length;
	}
}
