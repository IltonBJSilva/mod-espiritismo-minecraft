package net.mcreator.modespiritismominecraft.espiritismo;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Comparator;
import java.util.ArrayDeque;

/**
 * Tutorial "Jornada Espiritual" (doc: Tutorial / Início do jogo), conquistas e palestras.
 * <pre>
 * 0 Fale com Allan Kardec  → recebe O Livro dos Espíritos
 * 1 Leia o livro
 * 2 Converse com Chico Xavier → recebe a Água Fluidificada
 * 3 Dê comida a um aldeão faminto (caridade)
 * 4 Volte a Allan Kardec e escolha sua classe
 * 5 Concluída
 * </pre>
 */
public final class Jornada {
	public static final int FALAR_KARDEC = 0;
	public static final int LER_LIVRO = 1;
	public static final int FALAR_CHICO = 2;
	public static final int CARIDADE = 3;
	public static final int ESCOLHER_CLASSE = 4;
	public static final int CONCLUIDA = 5;

	/** Texto do objetivo atual (mostrado no HUD). */
	public static final List<String> OBJETIVOS = List.of(
			"Visite o Centro Espírita e fale com o Dirigente",
			"Leia O Livro dos Espíritos (botão direito)",
			"Receba o passe com o Passista do Centro",
			"Dê comida a um aldeão faminto",
			"Volte ao Dirigente e escolha seu caminho",
			"");

	private Jornada() {
	}

	public static int etapa(Player player) {
		return player.getData(Moralidade.JORNADA);
	}

	public static void definir(Player player, int etapa) {
		player.setData(Moralidade.JORNADA, Math.clamp(etapa, FALAR_KARDEC, CONCLUIDA));
	}

	public static String objetivo(int etapa) {
		return etapa >= 0 && etapa < OBJETIVOS.size() ? OBJETIVOS.get(etapa) : "";
	}

	/** Avança para a próxima etapa se o jogador estiver exatamente na etapa {@code de}. */
	public static boolean avancar(Player player, int de) {
		if (etapa(player) != de || player.level().isClientSide())
			return false;
		definir(player, de + 1);
		if (de + 1 == CONCLUIDA)
			Objetivo.limpar(player);
		atualizarObjetivo(player);
		if (player.level() instanceof ServerLevel sl)
			sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
		if (de + 1 < CONCLUIDA)
			player.sendSystemMessage(Component.literal("✦ Novo objetivo: ").withStyle(ChatFormatting.GOLD).append(Component.literal(objetivo(de + 1)).withStyle(ChatFormatting.YELLOW)));
		else
			player.sendSystemMessage(Component.literal("✦ Jornada inicial concluída! Continue fazendo o bem: sua moralidade é o seu caminho.").withStyle(ChatFormatting.GOLD));
		if (de + 1 == CARIDADE)
			garantirAldeaoFaminto(player, true);
		return true;
	}

	/** Aponta a seta do HUD para onde a etapa atual acontece. */
	public static void atualizarObjetivo(Player player) {
		if (!(player.level() instanceof ServerLevel sl))
			return;
		int etapa = etapa(player);
		if (etapa == FALAR_KARDEC || etapa == FALAR_CHICO || etapa == ESCOLHER_CLASSE) {
			BlockPos porta = centro(sl.getServer().overworld());
			Objetivo.definir(player, sl == sl.getServer().overworld() ? porta : null);
		} else if (etapa == CARIDADE) {
			for (Villager v : sl.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(64), v -> famintoPara(v, player))) {
				Objetivo.definir(player, v.blockPosition());
				return;
			}
		} else if (etapa == LER_LIVRO) {
			Objetivo.limpar(player);
		}
		// CONCLUIDA: a seta passa a ser da Bússola Espiritual (não mexe aqui).
	}

	/** Jogadores de versões anteriores do mod (que já tinham livro/classe) pulam as etapas já cumpridas. */
	public static void migrar(Player player) {
		if (Moralidade.classe(player) != ClasseEspiritual.NENHUMA)
			definir(player, CONCLUIDA);
		else if (player.getData(Moralidade.RECEBEU_LIVRO) && etapa(player) == FALAR_KARDEC)
			definir(player, LER_LIVRO);
	}

	// ---------------------------------------------------------------- Conquistas (data/.../advancement/jornada)

	/** Concede a conquista {@code jornada/<id>} (e a raiz, para a árvore aparecer). */
	public static void conquista(Player player, String id) {
		if (!(player instanceof ServerPlayer sp) || sp.level().getServer() == null)
			return;
		var gerenciador = sp.level().getServer().getAdvancements();
		for (String nome : id.equals("raiz") ? new String[]{"raiz"} : new String[]{"raiz", id}) {
			AdvancementHolder holder = gerenciador.get(Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, "jornada/" + nome));
			if (holder != null)
				sp.getAdvancements().award(holder, "concedido");
		}
	}

	// ---------------------------------------------------------------- Caridade: aldeões famintos

	/** Duração de uma prova de caridade: 1 dia de jogo. */
	public static final long DURACAO_FOME = 24000L;

	/** Se o aldeão está faminto (para qualquer jogador). */
	public static boolean faminto(Villager v) {
		return !v.getData(Moralidade.FAMINTO).isEmpty() && v.level().getGameTime() < v.getData(Moralidade.FAMINTO_ATE);
	}

	/** Se o aldeão está faminto e a prova é deste jogador (só ele recebe a recompensa grande). */
	public static boolean famintoPara(Villager v, Player player) {
		return faminto(v) && v.getData(Moralidade.FAMINTO).equals(player.getStringUUID());
	}

	public static void marcarFaminto(Villager v, Player player) {
		v.setData(Moralidade.FAMINTO, player.getStringUUID());
		v.setData(Moralidade.FAMINTO_ATE, v.level().getGameTime() + DURACAO_FOME);
	}

	public static void saciar(Villager v) {
		v.removeData(Moralidade.FAMINTO);
		v.removeData(Moralidade.FAMINTO_ATE);
	}

	/**
	 * Garante que haja um aldeão faminto perto do jogador: marca o mais próximo ou,
	 * se não houver nenhum, faz um "Irmão faminto" chegar.
	 *
	 * Cada jogador tem no máximo uma prova pendente por perto; a fome expira em 1 dia de jogo.
	 *
	 * @param permitirChegada se pode criar um aldeão quando não houver nenhum por perto
	 *                        (ainda limitado a uma chegada a cada 5 min por jogador)
	 * @return o aldeão faminto, ou null
	 */
	public static Villager garantirAldeaoFaminto(Player player, boolean permitirChegada) {
		if (!(player.level() instanceof ServerLevel level))
			return null;
		List<Villager> perto = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(64), v -> v.isAlive() && !v.isBaby());
		for (Villager v : perto)
			if (famintoPara(v, player))
				return v;
		Villager alvo = perto.stream().filter(v -> !faminto(v) && v.distanceToSqr(player) < 48 * 48).min(Comparator.comparingDouble(v -> v.distanceToSqr(player))).orElse(null);
		if (alvo == null) {
			if (!permitirChegada || level != level.getServer().overworld() || !Moralidade.pronto(player, "chegada_faminto", 6000))
				return null;
			alvo = EntityType.VILLAGER.create(level, EntitySpawnReason.EVENT);
			if (alvo == null)
				return null;
			double ang = player.getRandom().nextDouble() * Math.PI * 2;
			int x = (int) Math.floor(player.getX() + Math.cos(ang) * 5), z = (int) Math.floor(player.getZ() + Math.sin(ang) * 5);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (Math.abs(y - player.getY()) > 6)
				y = player.getBlockY();
			alvo.snapTo(x + 0.5, y, z + 0.5);
			alvo.setCustomName(Component.literal("Irmão faminto"));
			level.addFreshEntity(alvo);
			level.sendParticles(ParticleTypes.POOF, alvo.getX(), alvo.getY() + 1, alvo.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
		}
		marcarFaminto(alvo, player);
		Objetivo.definir(player, alvo.blockPosition());
		player.sendSystemMessage(Component.literal("Você sente que alguém por perto passa fome. Procure a fumaça acima de um aldeão" + direcao(player, alvo.position()) + ".")
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
		return alvo;
	}

	/** Texto como " (a 12 blocos, ao norte)". */
	public static String direcao(Player player, Vec3 alvo) {
		double dx = alvo.x - player.getX(), dz = alvo.z - player.getZ();
		int dist = (int) Math.sqrt(dx * dx + dz * dz);
		if (dist < 3)
			return "";
		String dir;
		if (Math.abs(dx) > Math.abs(dz) * 2)
			dir = dx > 0 ? "leste" : "oeste";
		else if (Math.abs(dz) > Math.abs(dx) * 2)
			dir = dz > 0 ? "sul" : "norte";
		else
			dir = (dz > 0 ? "sud" : "nord") + (dx > 0 ? "este" : "oeste");
		return " (a " + dist + " blocos, ao " + dir + ")";
	}

	// ---------------------------------------------------------------- Palestras (uma fala a cada 3 s)

	private static final Map<UUID, ArrayDeque<String>> PALESTRAS = new HashMap<>();
	private static final int INTERVALO_FALA = 70;

	public static boolean emPalestra(Player player) {
		return PALESTRAS.containsKey(player.getUUID());
	}

	public static void iniciarPalestra(Player player, Ensinamentos.Palestra palestra) {
		player.sendSystemMessage(Component.literal("— Palestra: " + palestra.titulo() + " —").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
		PALESTRAS.put(player.getUUID(), new ArrayDeque<>(palestra.falas()));
	}

	/** Chamado a cada tick do jogador (servidor). */
	public static void tickPalestra(Player player, String palestrante) {
		ArrayDeque<String> fila = PALESTRAS.get(player.getUUID());
		if (fila == null || player.tickCount % INTERVALO_FALA != 0)
			return;
		String fala = fila.poll();
		if (fala != null)
			player.sendSystemMessage(Component.literal("<" + palestrante + "> ").withStyle(ChatFormatting.GOLD).append(Component.literal(fala).withStyle(ChatFormatting.WHITE)));
		if (fila.isEmpty()) {
			PALESTRAS.remove(player.getUUID());
			Moralidade.ganhoRotina(player, 2, "Ouviu uma palestra");
			conquista(player, "palestra");
		}
	}

	public static void encerrarPalestra(Player player) {
		PALESTRAS.remove(player.getUUID());
	}

	public static BlockPos centro(ServerLevel overworld) {
		long v = overworld.getData(Moralidade.CENTRO);
		return v == Long.MIN_VALUE ? null : BlockPos.of(v);
	}
}
