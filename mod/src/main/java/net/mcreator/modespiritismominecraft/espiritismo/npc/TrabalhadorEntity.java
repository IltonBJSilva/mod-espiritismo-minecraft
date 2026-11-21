package net.mcreator.modespiritismominecraft.espiritismo.npc;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;

/**
 * Trabalhador encarnado de um Centro Espírita (dirigente, médium, orador, passista, evangelizadora)
 * e a homenagem a Divaldo Franco. Pacífico, persistente e invulnerável (ver {@link EventosNpc#aoReceberDano}).
 * O papel define as falas ({@link Falas}) e a skin.
 */
public class TrabalhadorEntity extends PathfinderMob {
	/** Papéis dos trabalhadores. {@code id} = ID de registro da entidade. */
	public enum Papel {
		DIRIGENTE("dirigente", "Dirigente"),
		MEDIUM("medium_trabalhador", "Médium"),
		ORADOR("orador", "Orador"),
		PASSISTA("passista", "Passista"),
		EVANGELIZADORA("evangelizadora", "Evangelizadora"),
		DIVALDO("divaldo_franco", "Divaldo Franco");

		public final String id;
		/** Nome exibido no chat ("<Nome> fala"). */
		public final String nome;

		Papel(String id, String nome) {
			this.id = id;
			this.nome = nome;
		}
	}

	private final Papel papel;

	public TrabalhadorEntity(EntityType<? extends TrabalhadorEntity> type, Level level, Papel papel) {
		super(type, level);
		this.papel = papel;
		this.xpReward = 0;
		setPersistenceRequired();
	}

	public Papel papel() {
		return papel;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.5));
		this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8F));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		// Fica perto de onde foi colocado (no Centro), salvo se quem o colocou já definiu outra "casa".
		if (!level().isClientSide() && tickCount == 20 && !hasHome())
			setHomeTo(blockPosition(), 10);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.MAX_HEALTH, 20).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.STEP_HEIGHT, 0.6);
	}
}
