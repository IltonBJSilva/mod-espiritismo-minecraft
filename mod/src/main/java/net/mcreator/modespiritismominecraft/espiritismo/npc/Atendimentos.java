package net.mcreator.modespiritismominecraft.espiritismo.npc;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.espiritismo.Jornada;

import javax.annotation.Nullable;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.ArrayDeque;

import static net.mcreator.modespiritismominecraft.espiritismo.npc.TrabalhadorEntity.Papel;
import static net.mcreator.modespiritismominecraft.espiritismo.npc.EspiritoEntity.Tipo;

/** O que cada trabalhador/Espírito faz quando o jogador conversa com ele (botão direito). */
public final class Atendimentos {
	/** Recarga do passe por jogador: 10 min. */
	public static final int RECARGA_PASSE = 12000;
	public static final int RECARGA_PALESTRA = 6000;
	/** Missão de caridade da Evangelizadora: no máximo uma por dia de jogo. */
	public static final int RECARGA_MISSAO = 24000;

	private Atendimentos() {
	}

	/** Próxima fala sequencial (contador salvo nas recargas do jogador). */
	private static String proxima(Player p, String chave, List<String> falas) {
		Map<String, Long> mapa = p.getData(Moralidade.RECARGAS);
		long i = mapa.getOrDefault("npc_seq_" + chave, 0L);
		mapa.put("npc_seq_" + chave, i + 1);
		return falas.get((int) (i % falas.size()));
	}

	public static void conversar(Player p, TrabalhadorEntity t) {
		Papel papel = t.papel();
		String nome = papel.nome;
		switch (papel) {
			case DIRIGENTE -> Falas.falar(p, nome, proxima(p, "dirigente", Falas.DIRIGENTE));
			case MEDIUM -> {
				if (p.getRandom().nextInt(4) == 0 && Moralidade.pronto(p, "npc_consolo", 2400)) {
					Falas.falar(p, nome, "Numa das nossas reuniões, um Espírito amigo, que não quis se identificar, deixou esta mensagem:");
					p.sendSystemMessage(Component.literal(Falas.aleatorio(Falas.CONSOLO, p.getRandom())).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
				} else
					Falas.falar(p, nome, proxima(p, "medium", Falas.MEDIUM));
			}
			case ORADOR -> {
				if (emPalestra(p))
					p.sendOverlayMessage(Component.literal("O orador está no meio da palestra...").withStyle(ChatFormatting.GRAY));
				else if (Moralidade.pronto(p, "npc_palestra", RECARGA_PALESTRA)) {
					Falas.falar(p, nome, "Sente-se, por favor. Vamos estudar o Evangelho juntos.");
					List<Falas.Palestra> ps = Falas.PALESTRAS;
					Map<String, Long> mapa = p.getData(Moralidade.RECARGAS);
					long i = mapa.getOrDefault("npc_seq_palestra", 0L);
					mapa.put("npc_seq_palestra", i + 1);
					iniciarPalestra(p, nome, ps.get((int) (i % ps.size())));
				} else
					Falas.falar(p, nome, Falas.aleatorio(Falas.ORADOR, p.getRandom()));
			}
			case PASSISTA -> {
				long falta = Moralidade.restante(p, "npc_passe");
				if (!passe(p, t)) {
					Falas.falar(p, nome, proxima(p, "passista", Falas.PASSISTA));
					p.sendOverlayMessage(Component.literal("Novo passe em " + (falta / 1200 + 1) + " min. Enquanto isso, ore e mantenha bons pensamentos.").withStyle(ChatFormatting.GRAY));
				}
			}
			case EVANGELIZADORA -> {
				if (missaoCaridadeDisponivel(p) && p.getRandom().nextInt(3) == 0) {
					Moralidade.pronto(p, "npc_missao_evangelizadora", RECARGA_MISSAO);
					// Só marca um aldeão que JÁ existe por perto (nunca cria aldeões novos); se não houver vila, não há missão.
					if (Jornada.garantirAldeaoFaminto(p, false) != null)
						Falas.falar(p, nome, "Tenho uma missãozinha para você: alguém por perto está com fome. Leve um pouco de comida para ele, com carinho!");
					else
						Falas.falar(p, nome, "Ah, hoje parece que todos já comeram. Que bom! Fica para a próxima.");
				} else
					Falas.falar(p, nome, proxima(p, "evangelizadora", Falas.EVANGELIZADORA));
			}
			case DIVALDO -> {
				Map<String, Long> mapa = p.getData(Moralidade.RECARGAS);
				long vezes = mapa.getOrDefault("npc_seq_divaldo", 0L);
				if (vezes % 6 == 0)
					Falas.narrar(p, Falas.DIVALDO_APRESENTACAO);
				Falas.falar(p, nome, proxima(p, "divaldo", Falas.DIVALDO));
			}
		}
	}

	/**
	 * A Evangelizadora só propõe a missão do aldeão faminto no Overworld, no máximo uma vez por dia de jogo e quando o
	 * jogador ainda não tem uma prova de caridade pendente por perto (evita acumular aldeões).
	 */
	public static boolean missaoCaridadeDisponivel(Player p) {
		if (!(p.level() instanceof ServerLevel level) || level != level.getServer().overworld())
			return false;
		if (Moralidade.restante(p, "npc_missao_evangelizadora") > 0)
			return false;
		return level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class, p.getBoundingBox().inflate(64), v -> Jornada.famintoPara(v, p)).isEmpty();
	}

	public static void conversar(Player p, EspiritoEntity e) {
		Tipo tipo = e.tipo();
		switch (tipo) {
			case MENTOR -> Falas.falar(p, tipo.nome, Falas.aleatorio(Falas.MENTOR, p.getRandom()), tipo.cor);
			case OBSESSOR -> {
				Falas.falar(p, tipo.nome, Falas.aleatorio(Falas.OBSESSOR_CONVERSA, p.getRandom()), tipo.cor);
				Falas.narrar(p, Falas.DICA_OBSESSOR);
			}
			case SOFREDOR -> {
				Falas.falar(p, tipo.nome, e.emResgate() ? "Eles vieram... vieram me buscar..." : Falas.aleatorio(Falas.SOFREDOR, p.getRandom()), tipo.cor);
				if (!e.emResgate())
					Falas.narrar(p, Falas.DICA_SOFREDOR);
			}
			case ZOMBETEIRO -> {
				Falas.falar(p, tipo.nome, Falas.aleatorio(Falas.ZOMBETEIRO_DICAS, p.getRandom()), tipo.cor);
				Falas.narrar(p, Falas.DICA_ZOMBETEIRO);
				e.prolongar(200); // atenção alimenta a zombaria
			}
			case SOCORRISTA -> Falas.falar(p, tipo.nome, Falas.aleatorio(Falas.SOCORRISTA, p.getRandom()), tipo.cor);
		}
	}

	// ---------------------------------------------------------------- Passe

	/**
	 * Aplica o passe (gratuito): remove efeitos nocivos, recupera um pouco de vida e de energia espiritual.
	 * Recarga de 10 min por jogador.
	 *
	 * @param passista quem aplica (para as partículas); pode ser null
	 * @return true se o passe foi aplicado; false se estava em recarga
	 */
	public static boolean passe(Player p, @Nullable TrabalhadorEntity passista) {
		long falta = Moralidade.restante(p, "npc_passe");
		if (falta > 0)
			return false;
		Moralidade.pronto(p, "npc_passe", RECARGA_PASSE);
		List<MobEffectInstance> nocivos = new ArrayList<>();
		for (MobEffectInstance ef : p.getActiveEffects())
			if (ef.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
				nocivos.add(ef);
		nocivos.forEach(ef -> p.removeEffect(ef.getEffect()));
		p.heal(6F);
		Moralidade.alterarEnergia(p, 30);
		Falas.falar(p, Papel.PASSISTA.nome, "Sente-se um instante e eleve o pensamento. Vamos orar juntos...");
		Falas.narrar(p, "O passista ora em silêncio e, sem tocá-lo, estende as mãos. Você sente uma serenidade suave.");
		Falas.falar(p, Papel.PASSISTA.nome, "Pronto, irmão. Que Jesus o ampare. O passe é sempre gratuito.");
		if (p.level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.5, p.getZ(), 15, 0.3, 0.5, 0.3, 0.01);
			sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8F, 1.3F);
		}
		Moralidade.ganhoRotina(p, 1, "Recebeu o passe");
		return true;
	}

	// ---------------------------------------------------------------- Palestras do orador (uma fala a cada 3,5 s)

	private record Fila(String palestrante, ArrayDeque<String> falas) {
	}

	private static final Map<UUID, Fila> PALESTRAS = new HashMap<>();
	private static final int INTERVALO_FALA = 70;

	public static boolean emPalestra(Player p) {
		return PALESTRAS.containsKey(p.getUUID());
	}

	public static void iniciarPalestra(Player p, String palestrante, Falas.Palestra palestra) {
		p.sendSystemMessage(Component.literal("— Palestra: " + palestra.titulo() + " —").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
		PALESTRAS.put(p.getUUID(), new Fila(palestrante, new ArrayDeque<>(palestra.falas())));
	}

	static void tickPalestra(Player p) {
		Fila fila = PALESTRAS.get(p.getUUID());
		if (fila == null || p.tickCount % INTERVALO_FALA != 0)
			return;
		String fala = fila.falas().poll();
		if (fala != null)
			Falas.falar(p, fila.palestrante(), fala);
		if (fila.falas().isEmpty()) {
			PALESTRAS.remove(p.getUUID());
			Moralidade.ganhoRotina(p, 2, "Ouviu uma palestra");
			Jornada.conquista(p, "palestra");
		}
	}

	static void encerrarPalestra(Player p) {
		PALESTRAS.remove(p.getUUID());
	}
}
