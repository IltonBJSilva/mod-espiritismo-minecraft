package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;

import javax.annotation.Nullable;

/**
 * Morador / personagem de Nosso Lar (e Amélie Boudet). O comportamento vem do {@link Perfil} (id sincronizado com o
 * cliente para a skin). Pacífico e protegido: não é ferido por jogadores (só /kill e o vazio).
 * <ul>
 * <li><b>Genéricos do povoamento</b> ({@link #temporario()}): não são salvos no disco e somem quando não há jogador perto —
 * o povoamento os recria por proximidade, então nunca duplicam ao recarregar.</li>
 * <li><b>Nomeados</b>: persistentes, um de cada por nível (ver {@link Povoamento#aoEntrarNoNivel}).</li>
 * </ul>
 */
public class MoradorEntity extends PathfinderMob {
	private static final EntityDataAccessor<String> PERFIL = SynchedEntityData.defineId(MoradorEntity.class, EntityDataSerializers.STRING);

	/** Distância (blocos) sem jogador por perto a partir da qual um genérico temporário some. */
	public static final int RAIO_DESPAWN = 80;

	private final String perfilPadrao;
	/** Zona de origem (genéricos do povoamento): onde trabalham durante o dia. */
	private String zona = "";
	private boolean temporario;
	/** Índice da última fala casual (para não repetir). Não salvo. */
	int ultimaFala = -1;

	public MoradorEntity(EntityType<? extends MoradorEntity> type, Level level, String perfilPadrao) {
		super(type, level);
		this.perfilPadrao = perfilPadrao;
		this.xpReward = 0;
		setPersistenceRequired();
		if (!perfilPadrao.isEmpty())
			definirPerfil(Perfis.get(perfilPadrao));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PERFIL, "");
	}

	// ---------------------------------------------------------------- Perfil

	public String perfilId() {
		return entityData.get(PERFIL);
	}

	@Nullable
	public Perfil perfil() {
		return Perfis.get(perfilId());
	}

	public boolean nomeado() {
		Perfil p = perfil();
		return p != null && p.nomeado();
	}

	public void definirPerfil(@Nullable Perfil p) {
		if (p == null)
			return;
		entityData.set(PERFIL, p.id());
		setCustomName(Component.literal(p.nome()));
		setCustomNameVisible(p.nomeado());
	}

	public String zona() {
		return zona;
	}

	public void definirZona(String zona) {
		this.zona = zona == null ? "" : zona;
	}

	public boolean temporario() {
		return temporario;
	}

	public void marcarTemporario() {
		this.temporario = true;
	}

	// ---------------------------------------------------------------- IA

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new RotinaGoal(this));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6F));
		this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.4, 0.02F));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel sl))
			return;
		if (perfilId().isEmpty() && tickCount > 1) {
			if (getType() == RegistroPopulacao.MORADOR.get())
				definirPerfil(Perfis.genericoPara(zona, getRandom())); // ex.: colocado por ovo
			else {
				discard(); // personagem sem perfil (ex.: /summon sem dados) não tem identidade
				return;
			}
		}
		if (tickCount % 20 == 7 && getNavigation().isDone())
			RotinaGoal.atividade(this, sl);
		if (temporario && tickCount % 100 == 0 && sl.getNearestPlayer(this, RAIO_DESPAWN) == null)
			discard(); // despawn seguro: longe de todos os jogadores
	}

	@Override
	public boolean shouldBeSaved() {
		return !temporario && super.shouldBeSaved();
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
	}

	/** Protegido: só dano que ignora invulnerabilidade (/kill, vazio). Agredir custa moralidade (com recarga). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return super.hurtServer(level, source, damage);
		if (source.getEntity() instanceof Player player && Moralidade.pronto(player, "npc_agredir", 40)) {
			Moralidade.alterar(player, -5, "Agrediu um morador de Nosso Lar");
			Perfil p = perfil();
			if (p != null)
				Dialogos.falar(player, p, "Ninguém aqui vai revidar. Se algo o aflige, podemos conversar.");
		}
		return false;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND)
			return InteractionResult.PASS;
		if (!level().isClientSide() && Moralidade.pronto(player, "pop_conversar", 20)) {
			getNavigation().stop();
			getLookControl().setLookAt(player, 30F, 30F);
			Dialogos.conversar(player, this);
		}
		return InteractionResult.SUCCESS;
	}

	// ---------------------------------------------------------------- Persistência

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("populacao_perfil", perfilId());
		output.putString("populacao_zona", zona);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		String id = input.getStringOr("populacao_perfil", perfilPadrao);
		if (!id.isEmpty())
			definirPerfil(Perfis.get(id));
		zona = input.getStringOr("populacao_zona", "");
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.MAX_HEALTH, 20).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.STEP_HEIGHT, 1.0);
	}

	/** Casa atual (para testes/depuração). */
	@Nullable
	public BlockPos casa() {
		return hasHome() ? getHomePosition() : null;
	}
}
