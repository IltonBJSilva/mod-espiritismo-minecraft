package net.mcreator.modespiritismominecraft.espiritismo;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.entity.AllanKardecEntity;

import java.util.List;

import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Habilidades ativas e passivas das classes (doc: "Menu de Habilidades", nível 1 de cada classe).
 * Toda a lógica roda no servidor; o cliente só envia "apertei a tecla".
 */
public final class Habilidades {
	private static final String RECARGA = "habilidade";
	/** Máximo de criaturas afetadas por uma Evangelização. */
	private static final int MAX_ALVOS = 5;
	/** Por quanto tempo (ticks) a criatura pacificada não mira jogadores. */
	private static final int DURACAO_PAZ = 300;


	private static final List<String> MENSAGENS_BENFEITORAS = List.of(
			"Não temas. Cada dificuldade é uma oportunidade de crescimento.",
			"A caridade que fazes ilumina o teu próprio caminho.",
			"Persevera no bem; não estás só.",
			"Perdoa, e o peso que carregas ficará mais leve.",
			"Estuda e trabalha: são as duas asas do progresso.");

	private static final List<String> MENSAGENS_LEVIANAS = List.of(
			"Hahaha! Procure um tesouro... ou não!",
			"Os diamantes estão logo abaixo de você. Talvez.",
			"Por que se esforçar? Ninguém está olhando.");

	private Habilidades() {
	}

	// ---------------------------------------------------------------- Escolha de classe

	public static void escolherClasse(Player player, int indice) {
		ClasseEspiritual escolha = ClasseEspiritual.porIndice(indice);
		if (escolha == ClasseEspiritual.NENHUMA || Moralidade.classe(player) != ClasseEspiritual.NENHUMA)
			return;
		// Só depois de cumprir o tutorial (livro → Chico → caridade).
		if (Jornada.etapa(player) < Jornada.ESCOLHER_CLASSE) {
			player.sendOverlayMessage(Component.literal("Antes de escolher seu caminho: " + Jornada.objetivo(Jornada.etapa(player))).withStyle(ChatFormatting.GRAY));
			return;
		}
		// Só vale perto de Allan Kardec, para não dar para escolher de qualquer lugar.
		if (player.level().getEntitiesOfClass(AllanKardecEntity.class, player.getBoundingBox().inflate(8)).isEmpty()) {
			player.sendOverlayMessage(Component.literal("Fique perto de Allan Kardec para escolher seu caminho.").withStyle(ChatFormatting.GRAY));
			return;
		}

		Moralidade.definirClasse(player, escolha);
		Moralidade.alterarEnergia(player, Moralidade.ENERGIA_MAX);
		Jornada.avancar(player, Jornada.ESCOLHER_CLASSE);
		Jornada.conquista(player, "caminho");
		player.sendSystemMessage(Component.literal("<Allan Kardec> ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal("Que assim seja. Siga o caminho do " + escolha.nome + " com humildade e estudo.").withStyle(ChatFormatting.WHITE)));
		player.sendSystemMessage(Component.literal("Você agora é " + escolha.nome + ". Aperte V para usar " + escolha.habilidade + ".").withStyle(ChatFormatting.AQUA));
		if (player.level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 1, 0.5, 0.05);
			sl.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
		}
	}

	// ---------------------------------------------------------------- Habilidade ativa (tecla V)

	public static void usar(Player player) {
		if (player.isSpectator() || !player.isAlive())
			return;
		ClasseEspiritual classe = Moralidade.classe(player);
		if (classe == ClasseEspiritual.NENHUMA) {
			player.sendOverlayMessage(Component.literal("Você ainda não tem uma classe. Converse com Allan Kardec.").withStyle(ChatFormatting.GRAY));
			return;
		}
		long falta = Moralidade.restante(player, RECARGA);
		if (falta > 0) {
			player.sendOverlayMessage(Component.literal(classe.habilidade + " em recarga (" + (falta / 20 + 1) + "s)").withStyle(ChatFormatting.GRAY));
			return;
		}
		if (Moralidade.energia(player) < classe.custo) {
			player.sendOverlayMessage(Component.literal("Energia espiritual insuficiente (" + classe.custo + " necessária)").withStyle(ChatFormatting.RED));
			return;
		}
		Moralidade.alterarEnergia(player, -classe.custo);
		Jornada.conquista(player, "habilidade");
		Moralidade.pronto(player, RECARGA, classe.recarga);

		switch (classe) {
			case MEDIUM -> psicografia(player);
			case DOUTRINADOR -> evangelizacao(player);
			case TRABALHADOR -> preceSincera(player);
			default -> {
			}
		}
	}

	/** Chance de sucesso: 40% + metade da moralidade (moral 100 → 90%). */
	private static boolean sucesso(Player player) {
		return player.getRandom().nextInt(100) < 40 + Moralidade.get(player) / 2;
	}

	/** Médium — Psicografia: recebe mensagem dos Espíritos com informação útil. */
	private static void psicografia(Player player) {
		RandomSource r = player.getRandom();
		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
		player.sendSystemMessage(Component.literal("✎ Psicografia").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));

		if (sucesso(player)) {
			List<Monster> perigos = level.getEntitiesOfClass(Monster.class, new AABB(player.blockPosition()).inflate(32));
			String alerta;
			if (perigos.isEmpty()) {
				alerta = "Os arredores estão em paz.";
			} else {
				Monster m = perigos.stream().min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player))).get();
				alerta = "Cuidado: " + perigos.size() + " presença(s) hostil(is) por perto, a mais próxima ao " + direcao(player, m.getX(), m.getZ()) + ".";
			}
			player.sendSystemMessage(Component.literal("Um Espírito benfeitor escreve: “" + Ensinamentos.aleatorio(MENSAGENS_BENFEITORAS, r) + "”").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
			player.sendSystemMessage(Component.literal(alerta).withStyle(ChatFormatting.YELLOW));
		} else {
			// Falha: um Espírito leviano interfere (doc: "Espíritos Levianos ... procuram enganar").
			player.sendSystemMessage(Component.literal("Um Espírito leviano interfere: “" + Ensinamentos.aleatorio(MENSAGENS_LEVIANAS, r) + "”").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
			if (Moralidade.get(player) <= Moralidade.BAIXA) {
				player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0));
				player.sendOverlayMessage(Component.literal("A comunicação perturbada abalou seu equilíbrio.").withStyle(ChatFormatting.DARK_PURPLE));
			}
		}
	}

	/** Doutrinador — Evangelização: tenta pacificar as criaturas hostis próximas. */
	private static void evangelizacao(Player player) {
		ServerLevel level = (ServerLevel) player.level();
		List<Monster> alvos = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(8));
		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.9F);
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 40, 4, 0.5, 4, 0.02);
		if (alvos.isEmpty()) {
			player.sendOverlayMessage(Component.literal("Suas palavras de paz ecoam, mas não há ninguém para ouvi-las.").withStyle(ChatFormatting.GRAY));
			return;
		}
		alvos.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
		int pacificados = 0, revoltados = 0;
		long ate = level.getGameTime() + DURACAO_PAZ;
		for (Monster m : alvos.subList(0, Math.min(MAX_ALVOS, alvos.size()))) {
			if (sucesso(player)) {
				m.setTarget(null);
				m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
				m.setData(Moralidade.PACIFICADO_ATE, ate);
				m.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DURACAO_PAZ, 0));
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, m.getX(), m.getY() + 1, m.getZ(), 6, 0.3, 0.5, 0.3, 0);
				pacificados++;
			} else if (revoltados == 0) {
				// Risco do documento: "Se falhar, pode intensificar a hostilidade" — só um se revolta.
				m.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0));
				level.sendParticles(ParticleTypes.ANGRY_VILLAGER, m.getX(), m.getY() + 1, m.getZ(), 3, 0.3, 0.5, 0.3, 0);
				revoltados++;
			}
		}
		player.sendSystemMessage(Component.literal("Evangelização: " + pacificados + " pacificado(s)" + (revoltados > 0 ? ", " + revoltados + " resistiram e ficaram mais hostis" : "") + ".")
				.withStyle(revoltados > pacificados ? ChatFormatting.RED : ChatFormatting.AQUA));
	}

	/** Trabalhador — Prece Sincera: fortalece a si e aos aliados próximos; exige parar por um instante. */
	private static void preceSincera(Player player) {
		ServerLevel level = (ServerLevel) player.level();
		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3)); // custo: pausar as atividades
		int ajudados = 0;
		for (Player aliado : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(8))) {
			aliado.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0));
			aliado.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0));
			level.sendParticles(ParticleTypes.END_ROD, aliado.getX(), aliado.getY() + 2, aliado.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
			ajudados++;
		}
		player.sendSystemMessage(Component.literal("Prece Sincera: você e " + (ajudados - 1) + " aliado(s) foram fortalecidos.").withStyle(ChatFormatting.AQUA));
	}

	private static String direcao(Player player, double x, double z) {
		double ang = Math.toDegrees(Math.atan2(z - player.getZ(), x - player.getX()));
		String[] dirs = {"leste", "sudeste", "sul", "sudoeste", "oeste", "noroeste", "norte", "nordeste"};
		return dirs[(int) Math.floorMod(Math.round(ang / 45.0), 8)];
	}

	// ---------------------------------------------------------------- Passivas (chamado a cada segundo)

	public static void passivas(Player player) {
		ServerLevel level = (ServerLevel) player.level();
		switch (Moralidade.classe(player)) {
			case MEDIUM -> {
				// Visão Espiritual (agachado): presenças hostis a até 10 blocos brilham. Gasta 1 de energia/s.
				if (player.isShiftKeyDown() && Moralidade.energia(player) > 0) {
					List<Monster> vistos = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(10));
					for (Monster m : vistos)
						m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
					Moralidade.alterarEnergia(player, -1);
				}
			}
			default -> {
			}
		}
	}

	public static boolean estaPacificado(net.minecraft.world.entity.LivingEntity e) {
		return e.hasData(Moralidade.PACIFICADO_ATE) && e.getData(Moralidade.PACIFICADO_ATE) > e.level().getGameTime();
	}

	/** Zera a recarga da habilidade (comandos e testes). */
	public static void limparRecarga(Player player) {
		Moralidade.limparRecarga(player, RECARGA);
	}
}
