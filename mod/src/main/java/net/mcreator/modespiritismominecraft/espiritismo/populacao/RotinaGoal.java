package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

import java.util.List;
import java.util.EnumSet;

/**
 * Rotina diária simples e barata (§3.5): de dia vai ao local de trabalho, à tarde convive (praça, bosque...), à noite
 * descansa. Decide no máximo a cada 10–30 s; caminhos longos são feitos em etapas de até 32 blocos.
 * Ao chegar, a "atividade" do tipo (trabalhar, conversar, descansar, andar) é encenada em {@link #atividade}.
 */
public class RotinaGoal extends Goal {
	/** Período do dia pelo relógio do Overworld (ticks de 0 a 24000; 0 = nascer do sol) — Nosso Lar pode ter horário fixo. */
	public enum Periodo {
		TRABALHO, CONVIVENCIA, DESCANSO;

		public static Periodo de(long dayTime) {
			long t = Math.floorMod(dayTime, 24000L);
			if (t < 9000)
				return TRABALHO;
			if (t < 13000)
				return CONVIVENCIA;
			return DESCANSO;
		}
	}

	private static final int PASSO_MAX = 32;

	private final MoradorEntity m;
	private long proximaDecisao;
	@Nullable
	private BlockPos destino;

	public RotinaGoal(MoradorEntity m) {
		this.m = m;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	/** Zona para onde o perfil vai neste período (null = fica onde está). */
	@Nullable
	public static String zonaDoPeriodo(Perfil p, String zonaOrigem, Periodo periodo, int sorteio) {
		if (p.tipo().atividade == TipoNpc.Atividade.ANDAR && periodo != Periodo.DESCANSO) {
			List<String> rota = p.tipo().zonas.isEmpty() ? p.rotina() : p.tipo().zonas;
			return rota.isEmpty() ? null : rota.get(Math.floorMod(sorteio, rota.size()));
		}
		return switch (periodo) {
			case TRABALHO -> !zonaOrigem.isEmpty() && !p.nomeado() ? zonaOrigem : p.local();
			case CONVIVENCIA -> p.rotina().isEmpty() ? p.local() : p.rotina().get(Math.floorMod(sorteio, p.rotina().size()));
			case DESCANSO -> p.descanso();
		};
	}

	@Override
	public boolean canUse() {
		if (!(m.level() instanceof ServerLevel sl))
			return false;
		long agora = sl.getGameTime();
		if (agora < proximaDecisao)
			return false;
		proximaDecisao = agora + 200 + m.getRandom().nextInt(400);
		Perfil p = m.perfil();
		if (p == null)
			return false;
		String zona = zonaDoPeriodo(p, m.zona(), Periodo.de(sl.getOverworldClockTime()), m.getRandom().nextInt(16));
		BlockPos centro = zona == null || zona.isEmpty() ? null : Zonas.posicao(sl, zona);
		if (centro == null)
			return false;
		destino = centro.offset(m.getRandom().nextInt(9) - 4, 0, m.getRandom().nextInt(9) - 4);
		if (m.blockPosition().distSqr(destino) < 16) {
			m.setHomeTo(destino, 6);
			return false;
		}
		return true;
	}

	@Override
	public void start() {
		if (destino == null)
			return;
		Vec3 alvo = Vec3.atBottomCenterOf(destino);
		Vec3 dif = alvo.subtract(m.position());
		if (dif.horizontalDistance() > PASSO_MAX) // etapa intermediária: caminho curto, barato de calcular
			alvo = m.position().add(dif.normalize().scale(PASSO_MAX));
		m.clearHome();
		m.getNavigation().moveTo(alvo.x, alvo.y, alvo.z, 0.6);
	}

	@Override
	public boolean canContinueToUse() {
		return !m.getNavigation().isDone();
	}

	@Override
	public void stop() {
		if (destino != null && m.blockPosition().distSqr(destino) < 64)
			m.setHomeTo(destino, 6);
		else
			proximaDecisao = Math.min(proximaDecisao, m.level().getGameTime() + 40); // etapa intermediária: segue logo
	}

	/** Encena a atividade do tipo no lugar (chamado a cada segundo quando o NPC está parado). */
	public static void atividade(MoradorEntity m, ServerLevel level) {
		Perfil p = m.perfil();
		if (p == null)
			return;
		Periodo periodo = Periodo.de(level.getOverworldClockTime());
		TipoNpc.Atividade a = periodo == Periodo.DESCANSO ? TipoNpc.Atividade.DESCANSAR : periodo == Periodo.CONVIVENCIA ? TipoNpc.Atividade.CONVERSAR : p.tipo().atividade;
		switch (a) {
			case TRABALHAR -> {
				if (m.getRandom().nextInt(4) == 0)
					m.swing(InteractionHand.MAIN_HAND);
			}
			case CONVERSAR -> {
				for (MoradorEntity outro : level.getEntitiesOfClass(MoradorEntity.class, m.getBoundingBox().inflate(5), o -> o != m)) {
					m.getLookControl().setLookAt(outro, 20F, 20F);
					break;
				}
			}
			case DESCANSAR, ANDAR -> {
			}
		}
	}
}
