package net.mcreator.modespiritismominecraft.espiritismo.npc;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;

import javax.annotation.Nullable;

import java.util.List;

import static net.mcreator.modespiritismominecraft.espiritismo.npc.EspiritoEntity.Tipo;

/**
 * Aparições de Espíritos (mentor, obsessor, zombeteiro) e o esclarecimento pela prece.
 * Nada aqui é spawn natural: tudo é controlado por jogador, com recargas longas.
 */
public final class Espiritos {
	/** Moralidade mínima para o mentor aparecer. */
	public static final int MORAL_MENTOR = 70;
	/** Moralidade máxima para atrair obsessores. */
	public static final int MORAL_OBSESSOR = 30;
	/** Moralidade a partir da qual zombeteiros já não se aproximam (afinidade). */
	public static final int MORAL_ZOMBETEIRO = 70;

	public static final int RECARGA_MENTOR = 24000; // 20 min
	public static final int RECARGA_OBSESSOR = 6000; // 5 min
	public static final int RECARGA_ZOMBETEIRO = 18000; // 15 min

	private Espiritos() {
	}

	/** Cria um Espírito na posição. {@code alvo} = jogador acompanhado (ou null); {@code vida} em ticks (-1 = permanente). */
	@Nullable
	public static EspiritoEntity criar(ServerLevel level, Tipo tipo, Vec3 pos, @Nullable Player alvo, int vida) {
		EspiritoEntity e = RegistroNpc.ESPIRITOS.get(tipo).get().create(level, EntitySpawnReason.EVENT);
		if (e == null)
			return null;
		e.snapTo(pos.x, pos.y, pos.z, alvo == null ? 0 : alvo.getYRot() + 180, 0);
		e.acompanhar(alvo, vida);
		level.addFreshEntity(e);
		return e;
	}

	/** Posição ao redor do jogador, a {@code dist} blocos. */
	private static Vec3 aoRedor(Player p, double dist) {
		double ang = p.getRandom().nextDouble() * Math.PI * 2;
		return new Vec3(p.getX() + Math.cos(ang) * dist, p.getY() + 0.3, p.getZ() + Math.sin(ang) * dist);
	}

	// ---------------------------------------------------------------- Esclarecimento

	/**
	 * Esclarece um Espírito (prece, Evangelização, doutrinação): o obsessor vira Espírito sofredor e um socorrista
	 * vem buscá-lo; o zombeteiro desiste e vai embora; o sofredor é amparado por um socorrista.
	 *
	 * @return true se havia algo a esclarecer
	 */
	public static boolean esclarecer(Entity espirito, @Nullable Player quem) {
		if (!(espirito instanceof EspiritoEntity e) || !e.isAlive() || !(e.level() instanceof ServerLevel level))
			return false;
		// "Paciente não ajuda paciente": amparar um sofredor exige estar bem.
		if (quem != null && e.tipo() == EspiritoEntity.Tipo.SOFREDOR && !net.mcreator.modespiritismominecraft.espiritismo.estado.Estados.podeSocorrer(quem)) {
			quem.sendSystemMessage(net.minecraft.network.chat.Component.literal(net.mcreator.modespiritismominecraft.espiritismo.estado.Estados.MSG_PRECISA_CUIDADO).withStyle(net.minecraft.ChatFormatting.AQUA));
			return false;
		}
		switch (e.tipo()) {
			case OBSESSOR -> {
				EspiritoEntity sofredor = criar(level, Tipo.SOFREDOR, e.position(), null, -1);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, e.getX(), e.getY() + 1, e.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
				e.discard();
				if (sofredor == null)
					return false;
				sofredor.iniciarResgate();
				if (quem != null) {
					Falas.falar(quem, Tipo.SOFREDOR.nome, "Por que... por que você ora por mim? Eu só lhe quis mal... Estou tão cansado.", Tipo.SOFREDOR.cor);
					Falas.narrar(quem, "Tocado pela prece, o obsessor reconhece o próprio sofrimento. Ele também é um irmão que precisa de ajuda.");
					Moralidade.ganhoRotina(quem, 5, "Ajudou um obsessor a se esclarecer");
				}
				return true;
			}
			case ZOMBETEIRO -> {
				if (quem != null) {
					Falas.falar(quem, Tipo.ZOMBETEIRO.nome, "Está bem, está bem! Já entendi... vou procurar algo útil para fazer.", Tipo.ZOMBETEIRO.cor);
					Moralidade.ganhoRotina(quem, 2, "Respondeu à zombaria com uma prece");
				}
				e.sumir(level);
				return true;
			}
			case SOFREDOR -> {
				if (e.emResgate())
					return false;
				e.iniciarResgate();
				if (quem != null) {
					Falas.falar(quem, Tipo.SOFREDOR.nome, "Que alívio... sinto um calor bom. Obrigado...", Tipo.SOFREDOR.cor);
					Moralidade.ganhoRotina(quem, 3, "Orou por um Espírito sofredor");
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	// ---------------------------------------------------------------- Aparições (chamado a cada tick do jogador)

	public static void tickJogador(Player p) {
		if (!(p.level() instanceof ServerLevel level) || p.isSpectator() || !p.isAlive() || level.dimension() != Level.OVERWORLD)
			return;
		if ((p.tickCount + p.getId()) % 100 != 0)
			return;
		int moral = Moralidade.get(p);
		var r = p.getRandom();
		boolean escuro = level.isDarkOutside();

		if (moral >= MORAL_MENTOR) {
			boolean vidaBaixa = p.getHealth() <= p.getMaxHealth() * 0.4F;
			boolean sozinho = escuro && level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(32), o -> o != p).isEmpty();
			if ((vidaBaixa && r.nextInt(2) == 0 || sozinho && r.nextInt(40) == 0) && semEspirito(level, p, Tipo.MENTOR) && Moralidade.pronto(p, "npc_mentor", RECARGA_MENTOR))
				mentor(level, p, vidaBaixa ? 0 : 3);
		}
		if (moral <= MORAL_OBSESSOR && escuro && r.nextInt(12) == 0 && semEspirito(level, p, Tipo.OBSESSOR) && Moralidade.pronto(p, "npc_obsessor", RECARGA_OBSESSOR)) {
			if (criar(level, Tipo.OBSESSOR, aoRedor(p, 8), p, 6000) != null)
				p.sendOverlayMessage(Component.literal("Você sente uma presença pesada acompanhando seus passos...").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
		}
		if (moral < MORAL_ZOMBETEIRO && r.nextInt(80) == 0 && semEspirito(level, p, Tipo.ZOMBETEIRO) && Moralidade.pronto(p, "npc_zombeteiro", RECARGA_ZOMBETEIRO)) {
			EspiritoEntity z = criar(level, Tipo.ZOMBETEIRO, aoRedor(p, 5), p, 2400);
			if (z != null)
				Falas.falar(p, Tipo.ZOMBETEIRO.nome, "Hihihi! Olá, olá! Tenho tanta coisa para lhe contar...", Tipo.ZOMBETEIRO.cor);
		}
	}

	/** Mentor ao ler uma obra (chamado pelo evento de usar item). Raro e com a mesma recarga longa. */
	public static void aoLer(Player p) {
		if (!(p.level() instanceof ServerLevel level) || Moralidade.get(p) < MORAL_MENTOR || p.getRandom().nextInt(6) != 0)
			return;
		if (semEspirito(level, p, Tipo.MENTOR) && Moralidade.pronto(p, "npc_mentor", RECARGA_MENTOR))
			mentor(level, p, 4);
	}

	private static boolean semEspirito(ServerLevel level, Player p, Tipo tipo) {
		return level.getEntitiesOfClass(EspiritoEntity.class, p.getBoundingBox().inflate(48), e -> e.tipo() == tipo && p.getUUID().equals(e.alvo())).isEmpty();
	}

	/** Faz o Espírito protetor aparecer por ~30 s, com uma orientação e regeneração leve. */
	@Nullable
	public static EspiritoEntity mentor(ServerLevel level, Player p, int fala) {
		EspiritoEntity m = criar(level, Tipo.MENTOR, aoRedor(p, 2.5), p, 600);
		if (m == null)
			return null;
		level.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + 1, m.getZ(), 30, 0.3, 0.8, 0.3, 0.02);
		level.playSound(null, m.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1F, 1.4F);
		String texto = fala >= 0 && fala < Falas.MENTOR.size() ? Falas.MENTOR.get(fala) : Falas.aleatorio(Falas.MENTOR, p.getRandom());
		Falas.falar(p, Tipo.MENTOR.nome, texto, Tipo.MENTOR.cor);
		p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
		return m;
	}

	// ---------------------------------------------------------------- Comportamentos

	/** Obsessor: sussurra e aplica efeitos leves; vai embora de dia ou se o jogador elevar os pensamentos. */
	static void tickObsessor(EspiritoEntity e, ServerLevel level, Player p) {
		if (level.isBrightOutside() && level.dimension() == Level.OVERWORLD || Moralidade.get(p) > 50) {
			p.sendOverlayMessage(Component.literal("A presença sombria se afasta, sem afinidade com seus pensamentos.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
			e.sumir(level);
			return;
		}
		if ((e.tickCount + e.getId()) % 200 != 0)
			return;
		p.sendOverlayMessage(Component.literal(Falas.aleatorio(Falas.OBSESSOR_SUSSURROS, e.getRandom())).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
		if (e.getRandom().nextInt(3) == 0)
			p.addEffect(e.getRandom().nextBoolean() ? new MobEffectInstance(MobEffects.SLOWNESS, 100, 0) : new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
	}

	private static final List<SoundEvent> SUSTOS = List.of(SoundEvents.CREEPER_PRIMED, SoundEvents.GHAST_AMBIENT, SoundEvents.VEX_AMBIENT, SoundEvents.WITCH_CELEBRATE);

	/** Zombeteiro: falsas dicas, sustos e "brincadeiras" com itens comuns (nunca destrói nada). */
	static void tickZombeteiro(EspiritoEntity e, ServerLevel level, Player p) {
		if ((e.tickCount + e.getId()) % 160 != 0)
			return;
		var r = e.getRandom();
		switch (r.nextInt(3)) {
			case 0 -> Falas.falar(p, Tipo.ZOMBETEIRO.nome, Falas.aleatorio(Falas.ZOMBETEIRO_DICAS, r), Tipo.ZOMBETEIRO.cor);
			case 1 -> {
				Vec3 atras = p.position().subtract(p.getLookAngle().scale(2));
				level.playSound(null, atras.x, atras.y, atras.z, SUSTOS.get(r.nextInt(SUSTOS.size())), SoundSource.HOSTILE, 0.8F, 1.0F);
			}
			default -> jogarItem(level, p, e);
		}
	}

	/** Tira 1 unidade de um item comum do inventário principal e o põe no chão, perto do jogador. */
	private static void jogarItem(ServerLevel level, Player p, EspiritoEntity e) {
		var inv = p.getInventory();
		for (int tentativa = 0; tentativa < 6; tentativa++) {
			int slot = 9 + e.getRandom().nextInt(27); // só a mochila (não a barra de atalhos nem armadura)
			ItemStack s = inv.getItem(slot);
			if (s.isEmpty() || s.isEnchanted() || s.getRarity() != Rarity.COMMON || s.isDamageableItem()
					|| !(s.getItem() instanceof BlockItem || s.getMaxStackSize() > 1) || BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals(RegistroNpc.MODID))
				continue;
			ItemStack um = s.split(1);
			Vec3 pos = p.position().add(e.getRandom().nextDouble() * 4 - 2, 0.5, e.getRandom().nextDouble() * 4 - 2);
			ItemEntity item = new ItemEntity(level, pos.x, pos.y, pos.z, um);
			item.setPickUpDelay(20);
			level.addFreshEntity(item);
			Falas.falar(p, Tipo.ZOMBETEIRO.nome, "Opa! Caiu alguma coisa sua ali. Hihihi!", Tipo.ZOMBETEIRO.cor);
			return;
		}
		Falas.falar(p, Tipo.ZOMBETEIRO.nome, Falas.aleatorio(Falas.ZOMBETEIRO_DICAS, e.getRandom()), Tipo.ZOMBETEIRO.cor);
	}
}
