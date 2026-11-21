package net.mcreator.modespiritismominecraft.espiritismo.itens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;

import java.util.Map;
import java.util.Optional;

/**
 * Água fluidificada na jarra.
 * <p>
 * Doutrina: a água não tem nada de mágico. A vontade e o pensamento de quem ora ou aplica o passe podem modificar as
 * propriedades da água, como o magnetismo já mostrava (O Livro dos Médiuns, 2ª parte, cap. VIII, item 131; A Gênese,
 * cap. XIV, sobre os fluidos). Por isso a jarra só dá água fluidificada se alguém com boa disposição moral orou ali
 * perto há pouco tempo, ou se há um passista trabalhando por perto.
 * <p>
 * As preces recentes ficam num attachment do nível (não salvo em disco: é só um "tempo" curto).
 */
public final class Fluidificacao {
	/** Por quanto tempo uma prece "vale" para fluidificar a água (2 minutos). */
	public static final int DURACAO_PRECE = 2400;
	/** Distância máxima (em blocos) entre a prece/o passista e a jarra. */
	public static final double RAIO = 8.0;
	/** Moralidade mínima de quem ora para a prece contar. */
	public static final int MORAL_MINIMA = 50;
	public static final Identifier PASSISTA = Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, "passista");

	private Fluidificacao() {
	}

	private static Map<Long, Long> preces(ServerLevel level) {
		return level.getData(RegistroItens.PRECES_RECENTES);
	}

	/**
	 * Registra uma prece feita em {@code pos} agora. O integrador chama isto quando alguém ora (ex.: habilidade Prece
	 * Sincera, comando, palestra) — de preferência pela sobrecarga com Player, que confere a moralidade.
	 */
	public static void registrarPrece(ServerLevel level, BlockPos pos) {
		Map<Long, Long> mapa = preces(level);
		long agora = level.getGameTime();
		mapa.values().removeIf(t -> agora - t > DURACAO_PRECE);
		mapa.put(pos.asLong(), agora);
	}

	/** Registra a prece de um jogador se a moralidade dele for ≥ 50. Retorna se contou. */
	public static boolean registrarPrece(Player player) {
		if (!(player.level() instanceof ServerLevel sl) || Moralidade.get(player) < MORAL_MINIMA)
			return false;
		registrarPrece(sl, player.blockPosition());
		return true;
	}

	/** Houve prece recente a até {@link #RAIO} blocos de {@code pos}? */
	public static boolean precePerto(ServerLevel level, BlockPos pos) {
		long agora = level.getGameTime();
		double r2 = RAIO * RAIO;
		for (Map.Entry<Long, Long> e : preces(level).entrySet())
			if (agora - e.getValue() <= DURACAO_PRECE && BlockPos.of(e.getKey()).distSqr(pos) <= r2)
				return true;
		return false;
	}

	/** Há um NPC passista (da área npc, procurado pelo ID) a até 8 blocos? */
	public static boolean passistaPerto(ServerLevel level, BlockPos pos) {
		Optional<EntityType<?>> tipo = BuiltInRegistries.ENTITY_TYPE.getOptional(PASSISTA);
		if (tipo.isEmpty())
			return false;
		AABB caixa = new AABB(pos).inflate(RAIO);
		EntityType<?> t = tipo.get();
		return !level.getEntities((Entity) null, caixa, e -> e.getType() == t && e.isAlive()).isEmpty();
	}

	public static boolean podeFluidificar(ServerLevel level, BlockPos pos) {
		return precePerto(level, pos) || passistaPerto(level, pos);
	}

	static Component explicacaoAguaComum() {
		return Component.literal("Você encheu a garrafa com água comum. A jarra não tem nada de mágico: a água é fluidificada pela prece sincera "
				+ "de quem tem boa vontade ou pelo passe de um trabalhador do bem (O Livro dos Médiuns, cap. VIII, item 131). "
				+ "Agache e clique na jarra com a mão vazia para orar, ou peça ajuda a um passista.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
	}
}
