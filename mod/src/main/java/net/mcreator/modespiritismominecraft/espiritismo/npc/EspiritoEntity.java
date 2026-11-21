package net.mcreator.modespiritismominecraft.espiritismo.npc;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;

import javax.annotation.Nullable;

import java.util.UUID;

/**
 * Espírito desencarnado (mentor, obsessor, sofredor, zombeteiro, socorrista).
 * <p>
 * Flutua devagar atravessando blocos (o perispírito não é barrado pela matéria), não deixa drops e não é ferido
 * por armas: só {@code /kill} e o vazio o removem. Obsessores não são "monstros a matar": eles se esclarecem com
 * a prece e o bem ({@link Espiritos#esclarecer}).
 */
public class EspiritoEntity extends PathfinderMob {
	public enum Tipo {
		MENTOR("mentor", "Espírito protetor", ChatFormatting.AQUA),
		OBSESSOR("obsessor", "Espírito obsessor", ChatFormatting.DARK_PURPLE),
		SOFREDOR("espirito_sofredor", "Espírito sofredor", ChatFormatting.GRAY),
		ZOMBETEIRO("zombeteiro", "Espírito zombeteiro", ChatFormatting.LIGHT_PURPLE),
		SOCORRISTA("socorrista", "Socorrista espiritual", ChatFormatting.GREEN);

		public final String id;
		public final String nome;
		public final ChatFormatting cor;

		Tipo(String id, String nome, ChatFormatting cor) {
			this.id = id;
			this.nome = nome;
			this.cor = cor;
		}

		/** Espíritos que se beneficiam da prece / esclarecimento. */
		public boolean esclarecivel() {
			return this == OBSESSOR || this == SOFREDOR || this == ZOMBETEIRO;
		}
	}

	private final Tipo tipo;
	/** Jogador que o Espírito acompanha (ou null). */
	@Nullable
	private UUID alvo;
	/** Ticks até sumir; -1 = permanente (ex.: colocado por ovo ou por outra área do mod). */
	private int vida = -1;
	/** Sofredor: ticks desde o início do resgate (-1 = sem resgate). */
	private int resgate = -1;
	/** Socorrista chamado para este sofredor (não salvo). */
	@Nullable
	private EspiritoEntity socorrista;
	/**
	 * Tick absoluto do mundo em que o Espírito temporário some (-1 = nunca). Salvo no NBT: mesmo que o chunk descarregue
	 * e a referência ao acompanhante/sofredor se perca, ele expira sozinho ao voltar a carregar.
	 */
	private long expiraEm = -1;

	/** Duração máxima de um resgate (o sofredor e o socorrista somem até lá, com ou sem referência entre eles). */
	public static final int DURACAO_MAX_RESGATE = 400;

	public EspiritoEntity(EntityType<? extends EspiritoEntity> type, Level level, Tipo tipo) {
		super(type, level);
		this.tipo = tipo;
		this.xpReward = 0;
		setNoGravity(true);
		setPersistenceRequired();
	}

	public Tipo tipo() {
		return tipo;
	}

	@Nullable
	public UUID alvo() {
		return alvo;
	}

	public void acompanhar(@Nullable Player player, int vidaTicks) {
		this.alvo = player == null ? null : player.getUUID();
		this.vida = vidaTicks;
		this.expiraEm = vidaTicks > 0 ? level().getGameTime() + vidaTicks : -1;
	}

	public void prolongar(int ticks) {
		if (vida >= 0)
			vida += ticks;
		if (expiraEm >= 0)
			expiraEm += ticks;
	}

	/** Faz o Espírito expirar daqui a {@code ticks} (tempo absoluto salvo no NBT). */
	public void expirarEm(int ticks) {
		this.expiraEm = level().getGameTime() + ticks;
	}

	public long expiraEm() {
		return expiraEm;
	}

	public boolean emResgate() {
		return resgate >= 0;
	}

	/** Sofredor: começa o resgate (um socorrista virá buscá-lo). */
	public void iniciarResgate() {
		if (resgate < 0)
			resgate = 0;
		vida = -1;
		expirarEm(DURACAO_MAX_RESGATE);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10F));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	// ---------------------------------------------------------------- Física: flutua e atravessa blocos

	@Override
	public void tick() {
		setNoGravity(true);
		this.noPhysics = true;
		if (level() instanceof ServerLevel sl)
			logica(sl);
		if (isRemoved())
			return;
		super.tick();
		this.noPhysics = false;
		this.fallDistance = 0;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
	}

	/** Armas comuns atravessam o corpo espiritual. Só dano que ignora invulnerabilidade (/kill, vazio) funciona. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return super.hurtServer(level, source, damage);
		if (source.getEntity() instanceof Player player) {
			level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0, getZ(), 8, 0.2, 0.4, 0.2, 0.01);
			level.playSound(null, blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 0.5F, 0.6F);
			player.sendOverlayMessage(Component.literal("A lâmina atravessa o corpo espiritual sem feri-lo. A força não resolve: tente a prece (tecla B).")
					.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			if (tipo == Tipo.ZOMBETEIRO)
				prolongar(600); // a irritação do jogador diverte o zombeteiro
		}
		return false;
	}

	// ---------------------------------------------------------------- Comportamento (servidor)

	@Nullable
	public Player jogadorAlvo(ServerLevel level) {
		if (alvo == null)
			return null;
		Player p = level.getPlayerByUUID(alvo);
		return p != null && p.isAlive() && p.level() == level ? p : null;
	}

	private void logica(ServerLevel level) {
		if (expiraEm >= 0 && level.getGameTime() >= expiraEm) {
			sumir(level); // temporário expirado (também cobre referências perdidas ao descarregar o chunk)
			return;
		}
		Player p = jogadorAlvo(level);
		if (alvo != null && (p == null || p.distanceToSqr(this) > 64 * 64)) {
			discard(); // o jogador foi embora: o Espírito também
			return;
		}
		if (vida > 0 && --vida == 0) {
			despedir(level, p);
			return;
		}
		Vec3 destino = null;
		double distancia = 0;
		switch (tipo) {
			case MENTOR -> {
				destino = p == null ? null : p.position();
				distancia = 2.5;
			}
			case OBSESSOR -> {
				destino = p == null ? null : p.position();
				distancia = 5;
				if (p != null)
					Espiritos.tickObsessor(this, level, p);
			}
			case ZOMBETEIRO -> {
				destino = p == null ? null : p.position();
				distancia = 4;
				if (p != null)
					Espiritos.tickZombeteiro(this, level, p);
			}
			case SOCORRISTA -> {
			}
			case SOFREDOR -> {
				if (resgate >= 0)
					tickResgate(level);
				else if (tickCount % 300 == 150)
					for (Player perto : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(8)))
						perto.sendOverlayMessage(Component.literal(Falas.aleatorio(Falas.SOFREDOR, getRandom())).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			}
		}
		mover(destino, distancia);
		if (tickCount % 10 == 0)
			level.sendParticles(particula(), getX(), getY() + 1.0, getZ(), 1, 0.25, 0.5, 0.25, 0.0);
	}

	private void mover(@Nullable Vec3 destino, double distancia) {
		Vec3 v = Vec3.ZERO;
		if (destino != null) {
			Vec3 alvoPos = destino.add(0, 0.6, 0);
			Vec3 dif = alvoPos.subtract(position());
			double d = dif.length();
			if (d > distancia + 0.5)
				v = dif.normalize().scale(Math.min(0.12, (d - distancia) * 0.05));
			else if (d < distancia - 1.5)
				v = dif.normalize().scale(-0.05);
			if (getLookControl() != null)
				getLookControl().setLookAt(destino.x, destino.y + 1.5, destino.z);
		}
		// flutuação suave
		double balanco = Math.sin(tickCount * 0.08) * 0.01;
		setDeltaMovement(v.x, v.y + balanco, v.z);
	}

	private SimpleParticleType particula() {
		return switch (tipo) {
			case MENTOR, SOCORRISTA -> ParticleTypes.END_ROD;
			case OBSESSOR -> ParticleTypes.SMOKE;
			case SOFREDOR -> ParticleTypes.WHITE_ASH;
			case ZOMBETEIRO -> ParticleTypes.WITCH;
		};
	}

	private void tickResgate(ServerLevel level) {
		resgate++;
		if (resgate == 30) {
			EspiritoEntity s = Espiritos.criar(level, Tipo.SOCORRISTA, position().add(1.5, 0.5, 0), null, 200); // vida limitada: nunca fica órfão
			if (s != null) {
				s.expirarEm(DURACAO_MAX_RESGATE);
				socorrista = s;
				level.sendParticles(ParticleTypes.END_ROD, s.getX(), s.getY() + 1, s.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
				level.playSound(null, s.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1F, 1.3F);
			}
		} else if (resgate == 60) {
			for (Player perto : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(16)))
				for (String fala : Falas.SOCORRISTA_RESGATE)
					Falas.falar(perto, Tipo.SOCORRISTA.nome, fala, Tipo.SOCORRISTA.cor);
		} else if (resgate >= 140) {
			if (socorrista != null && socorrista.isAlive())
				socorrista.sumir(level);
			sumir(level);
		}
	}

	private void despedir(ServerLevel level, @Nullable Player p) {
		if (p != null) {
			if (tipo == Tipo.MENTOR)
				Falas.falar(p, tipo.nome, Falas.aleatorio(Falas.MENTOR_DESPEDIDA, getRandom()), tipo.cor);
			else if (tipo == Tipo.ZOMBETEIRO)
				Falas.falar(p, tipo.nome, "Que sem graça... ninguém me dá atenção. Vou procurar outra diversão.", tipo.cor);
			else if (tipo == Tipo.OBSESSOR)
				p.sendOverlayMessage(Component.literal("A presença pesada se afasta... por enquanto.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
		}
		sumir(level);
	}

	/** Some com partículas (sem morrer, sem drops). */
	public void sumir(ServerLevel level) {
		level.sendParticles(tipo == Tipo.OBSESSOR ? ParticleTypes.LARGE_SMOKE : ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 25, 0.3, 0.7, 0.3, 0.03);
		level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.7F, tipo == Tipo.OBSESSOR ? 0.6F : 1.5F);
		discard();
	}

	// ---------------------------------------------------------------- Persistência

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("espiritismo_vida", vida);
		output.putInt("espiritismo_resgate", resgate);
		output.putLong("espiritismo_expira", expiraEm);
		if (alvo != null)
			output.putString("espiritismo_alvo", alvo.toString());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		vida = input.getIntOr("espiritismo_vida", -1);
		resgate = input.getIntOr("espiritismo_resgate", -1);
		expiraEm = input.getLongOr("espiritismo_expira", -1L);
		String a = input.getStringOr("espiritismo_alvo", "");
		try {
			alvo = a.isEmpty() ? null : UUID.fromString(a);
		} catch (IllegalArgumentException e) {
			alvo = null;
		}
		// Espírito temporário salvo no disco: ao recarregar, não há mais quem acompanhar.
		if (alvo != null)
			vida = Math.max(vida, 1);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.MAX_HEALTH, 20).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.FLYING_SPEED, 0.2);
	}
}
