package net.mcreator.modespiritismominecraft.espiritismo.npc;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.function.Consumer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Prece (tecla B): conversa sincera com Deus (ESE, cap. XXVII). Não é ritual nem fórmula: vale pelo sentimento.
 * Custa energia espiritual, tem recarga de 30 s e pode esclarecer Espíritos imperfeitos/sofredores próximos.
 */
public final class Prece {
	public static final int CUSTO = 15;
	public static final int RECARGA = 600; // 30 s
	public static final double RAIO = 12;
	public static final String CHAVE_RECARGA = "npc_prece";

	/** Ganchos chamados (no servidor) sempre que um jogador ora com sucesso. Ex.: o Umbral (área "dim"). */
	public static final List<Consumer<Player>> AO_ORAR = new CopyOnWriteArrayList<>();

	/** Efeitos considerados "de obsessão" (a prece remove um deles). */
	private static final List<Holder<MobEffect>> EFEITOS_OBSESSAO = List.of(MobEffects.DARKNESS, MobEffects.SLOWNESS, MobEffects.MINING_FATIGUE, MobEffects.NAUSEA);

	private Prece() {
	}

	/** Cliente → servidor: o jogador apertou a tecla da prece. */
	public record Orar() implements CustomPacketPayload {
		public static final Type<Orar> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, "npc_orar"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Orar> CODEC = StreamCodec.unit(new Orar());

		@Override
		public Type<Orar> type() {
			return TYPE;
		}
	}

	/** Chance (0–100) de esclarecer um Espírito: 40% + metade da moralidade (o sofredor sempre aceita). */
	public static int chance(Player player) {
		return 40 + Moralidade.get(player) / 2;
	}

	/**
	 * O jogador ora (servidor).
	 *
	 * @return true se a prece aconteceu (havia energia e não estava em recarga)
	 */
	public static boolean orar(Player player) {
		if (player == null || !(player.level() instanceof ServerLevel level) || player.isSpectator() || !player.isAlive())
			return false;
		long falta = Moralidade.restante(player, CHAVE_RECARGA);
		if (falta > 0) {
			player.sendOverlayMessage(Component.literal("Você acabou de orar. Aguarde " + (falta / 20 + 1) + "s, mantendo bons pensamentos.").withStyle(ChatFormatting.GRAY));
			return false;
		}
		if (Moralidade.energia(player) < CUSTO) {
			player.sendOverlayMessage(Component.literal("Energia espiritual insuficiente para orar com concentração (" + CUSTO + " necessária).").withStyle(ChatFormatting.RED));
			return false;
		}
		Moralidade.pronto(player, CHAVE_RECARGA, RECARGA);
		Moralidade.alterarEnergia(player, -CUSTO);

		player.sendSystemMessage(Component.literal("Você eleva o pensamento a Deus numa prece sincera...").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
		// Remove um efeito de obsessão (antes de aplicar a breve pausa da própria prece).
		for (Holder<MobEffect> ef : EFEITOS_OBSESSAO)
			if (player.hasEffect(ef)) {
				player.removeEffect(ef);
				player.sendOverlayMessage(Component.literal("A prece dissipa a influência negativa.").withStyle(ChatFormatting.AQUA));
				break;
			}
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false, true));
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 12, 0.4, 0.6, 0.4, 0.01);
		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F);

		// Esclarece Espíritos imperfeitos/sofredores próximos.
		int ajudados = 0;
		for (EspiritoEntity e : level.getEntitiesOfClass(EspiritoEntity.class, player.getBoundingBox().inflate(RAIO), e -> e.tipo().esclarecivel())) {
			boolean aceita = e.tipo() == EspiritoEntity.Tipo.SOFREDOR || player.getRandom().nextInt(100) < chance(player);
			if (aceita && Espiritos.esclarecer(e, player))
				ajudados++;
			else if (!aceita)
				Falas.falar(player, e.tipo().nome, "Hunf... palavras bonitas não me convencem. Ainda.", e.tipo().cor);
		}
		Moralidade.ganhoRotina(player, 1, "Orou com sinceridade");
		if (ajudados == 0)
			player.sendSystemMessage(Component.literal("Uma sensação de paz o envolve.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		for (Consumer<Player> gancho : AO_ORAR) {
			try {
				gancho.accept(player);
			} catch (RuntimeException ex) {
				ModEspiritismoMinecraftMod.LOGGER.error("Erro num gancho Prece.AO_ORAR", ex);
			}
		}
		return true;
	}
}
