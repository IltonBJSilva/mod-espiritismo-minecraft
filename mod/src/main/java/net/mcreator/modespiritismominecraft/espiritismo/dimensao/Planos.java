package net.mcreator.modespiritismominecraft.espiritismo.dimensao;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.estado.Estados;
import net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.LayoutColonia;
import net.mcreator.modespiritismominecraft.espiritismo.colonia.ColoniaApi;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.espiritismo.Jornada;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.Set;
import java.util.Map;

/**
 * Os planos espirituais: Nosso Lar (colônia espiritual) e Umbral (região de sofrimento), a passagem entre eles
 * e a Terra (Overworld), o resgate e a reencarnação. API pública para outras áreas:
 * {@link #resgatar(ServerPlayer)}, {@link #reencarnar(ServerPlayer)}, {@link #aoOrar(ServerPlayer)},
 * {@link #ajudouSofredor(ServerPlayer)}, {@link #ir(ServerPlayer, ResourceKey)}.
 */
public final class Planos {
	public static final ResourceKey<Level> NOSSO_LAR = ResourceKey.create(Registries.DIMENSION, id("nosso_lar"));
	public static final ResourceKey<Level> UMBRAL = ResourceKey.create(Registries.DIMENSION, id("umbral"));

	/** Moralidade a partir da qual se desperta em Nosso Lar ao desencarnar. */
	public static final int LIMIAR_NOSSO_LAR = 60;
	/** Moralidade até a qual se desperta no Umbral ao desencarnar. */
	public static final int LIMIAR_UMBRAL = 30;
	/** Pontos de arrependimento que trazem o socorro (prece = 1, ajudar um sofredor = 2, 3 min de reflexão = 1). */
	public static final int ARREPENDIMENTO_PARA_RESGATE = 5;
	public static final int RECARGA_PRECE = 30 * 20;
	public static final int TEMPO_REFLEXAO = 3 * 60 * 20;
	public static final int ESPERA_SOCORRISTA = 5 * 20;
	public static final int DURACAO_SONHO = 90 * 20;
	public static final int DURACAO_PESADELO = 25 * 20;

	public enum Destino {
		NOSSO_LAR, UMBRAL, PERTURBACAO
	}

	private Planos() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, path);
	}

	// ================================================================ Regras puras (testáveis)

	/** Para onde vai o Espírito que desencarna na Terra, segundo sua moralidade. */
	public static Destino destinoAposMorte(int moral) {
		if (moral >= LIMIAR_NOSSO_LAR)
			return Destino.NOSSO_LAR;
		if (moral <= LIMIAR_UMBRAL)
			return Destino.UMBRAL;
		return Destino.PERTURBACAO;
	}

	/** Tipo de sonho ao acordar após uma noite inteira: 0 nenhum, 1 desdobramento a Nosso Lar, 2 pesadelo no Umbral. */
	public static int tipoSonho(int moral, float sorteio) {
		if (moral >= 50 && sorteio < 0.30F)
			return 1;
		if (moral <= 25 && sorteio < 0.25F)
			return 2;
		return 0;
	}

	public static boolean resgateLiberado(int moral, int arrependimento) {
		return moral > LIMIAR_UMBRAL || arrependimento >= ARREPENDIMENTO_PARA_RESGATE;
	}

	// ================================================================ Construções / pontos

	public static Map<String, Long> pontos(ServerLevel level) {
		return level.getData(RegistroDimensao.PONTOS);
	}

	/** Garante as construções do plano e devolve o ponto de chegada. */
	public static BlockPos garantir(ServerLevel level) {
		if (level.dimension() == UMBRAL)
			return ConstrucoesUmbral.garantir(level); // incremental (Postos de Socorro)
		Map<String, Long> p = pontos(level);
		if (!p.containsKey("chegada")) {
			p.putAll(construir(level, 0, 0, level.dimension() == NOSSO_LAR));
			level.setData(RegistroDimensao.PONTOS, p);
		}
		return BlockPos.of(p.get("chegada"));
	}

	/** Constrói as construções de Nosso Lar ({@code nossoLar}) ou o Posto central do Umbral com centro em (x, z), INTEIRAS (testes/admin). */
	public static Map<String, Long> construir(ServerLevel level, int x, int z, boolean nossoLar) {
		int g = Math.clamp(ConstrucoesPlanos.chao(level, x, z), level.getSeaLevel() + 2, 100);
		BlockPos origem = new BlockPos(x, g, z);
		Map<String, Long> novos = nossoLar ? ConstrucoesPlanos.construirNossoLar(level, origem) : ConstrucoesUmbral.construirAgora(level, origem);
		ModEspiritismoMinecraftMod.LOGGER.info("Construções de {} feitas em {} ({})", nossoLar ? "Nosso Lar" : "Umbral", origem, level.dimension().identifier());
		return novos;
	}

	public static BlockPos ponto(ServerLevel level, String nome) {
		Long v = pontos(level).get(nome);
		return v == null ? null : BlockPos.of(v);
	}

	// ================================================================ Viagens

	public static void teleportar(ServerPlayer p, ServerLevel destino, BlockPos pos) {
		if (p.isPassenger())
			p.stopRiding();
		if (p.isSleeping())
			p.stopSleeping();
		if (p.level().dimension() == Level.OVERWORLD)
			p.setData(RegistroDimensao.ULTIMA_TERRA, p.blockPosition().asLong());
		p.teleportTo(destino, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), p.getYRot(), 0F, true);
		p.fallDistance = 0;
	}

	/** Vai a um plano (comando de teste). {@code Level.OVERWORLD} = volta à Terra no ponto de renascimento. */
	public static boolean ir(ServerPlayer p, ResourceKey<Level> plano) {
		MinecraftServer server = p.level().getServer();
		ServerLevel destino = server.getLevel(plano);
		if (destino == null)
			return false;
		BlockPos pos = plano == Level.OVERWORLD ? pontoNaTerra(p) : garantir(destino);
		// Comando de administrador: o estado acompanha o destino (inclusive a entrada em Nosso Lar).
		Resgates.cancelar(p, 0);
		Estados.forcar(p, Estados.deduzir(plano, Moralidade.get(p)), "comando ir " + plano.identifier());
		if (plano == UMBRAL)
			Perturbacao.set(p, Perturbacao.inicialNoUmbral(Moralidade.get(p)));
		else
			Perturbacao.set(p, 0);
		teleportar(p, destino, pos);
		return true;
	}

	/** Ponto de renascimento do jogador na Terra (cama, se for no Overworld) ou o spawn do mundo. */
	public static BlockPos pontoNaTerra(ServerPlayer p) {
		ServerLevel ow = p.level().getServer().overworld();
		ServerPlayer.RespawnConfig cfg = p.getRespawnConfig();
		BlockPos base = cfg != null && cfg.respawnData().dimension() == Level.OVERWORLD ? cfg.respawnData().pos() : ow.getRespawnData().pos();
		return superficie(ow, base.getX(), base.getZ());
	}

	static BlockPos superficie(ServerLevel level, int x, int z) {
		return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
	}

	// ================================================================ Saída assistida (Umbral → Nosso Lar), com consentimento

	/**
	 * Saída assistida até as Câmaras de Retificação de Nosso Lar. Só é chamada por ação do próprio jogador (atril do
	 * ponto de conversa do posto, confirmado duas vezes) ou por comando de administrador. Nunca automática.
	 */
	public static void resgatar(ServerPlayer p) {
		Jornada.conquista(p, "resgate");
		ServerLevel nl = p.level().getServer().getLevel(NOSSO_LAR);
		if (nl == null)
			return;
		boolean doUmbral = p.level().dimension() == UMBRAL;
		Resgates.cancelar(p, 0);
		if (!Estados.transicionar(p, EstadoEspiritual.EM_ACOLHIMENTO, "saída assistida a Nosso Lar"))
			Estados.forcar(p, EstadoEspiritual.EM_ACOLHIMENTO, "saída assistida a Nosso Lar");
		p.setData(RegistroDimensao.ARREPENDIMENTO, 0);
		p.setData(RegistroDimensao.TEMPO_UMBRAL, 0);
		Perturbacao.set(p, Math.min(Perturbacao.get(p), 10));
		teleportar(p, nl, garantir(nl));
		p.removeEffect(MobEffects.DARKNESS);
		p.removeEffect(MobEffects.SLOWNESS);
		p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
		if (doUmbral)
			Moralidade.ganhoRotina(p, 3, "Arrependimento sincero");
		msg(p, ChatFormatting.AQUA, "Acompanhado pelos socorristas, você deixa o Posto de Socorro e chega às Câmaras de Retificação de Nosso Lar.");
		msg(p, ChatFormatting.GRAY, "Ninguém fica esquecido: o arrependimento sincero e a vontade de melhorar abrem o caminho. Para voltar à Terra, procure o Ministério do Auxílio.");
	}

	/** Critério da saída assistida a Nosso Lar (PROMPT §4.1): resgate liberado E mente serena. Regra pura. */
	public static boolean saidaAssistidaLiberada(int moral, int arrependimento, int perturbacao) {
		return resgateLiberado(moral, arrependimento) && perturbacao <= Perturbacao.LIMIAR_ORIENTADO;
	}

	/** Estados que justificam estar em Nosso Lar (desencarne, acolhimento, visita no sono, resgate aceito). Regra pura. */
	public static boolean podeEntrarNossoLar(EstadoEspiritual e) {
		return switch (e) {
			case EM_ACOLHIMENTO, EM_NOSSO_LAR, EM_TRATAMENTO, EM_VISITA_ASSISTIDA, PRONTO_PARA_REENCARNAR, RECEM_DESENCARNADO, REENCARNANDO, EM_RESGATE -> true;
			default -> false;
		};
	}

	/** Chamado por cada prece (gancho {@code npc.Prece.AO_ORAR}, ou o comando /espiritismo_prece). */
	public static void aoOrar(ServerPlayer p) {
		if (Perturbacao.get(p) > 0 && Perturbacao.aliviar(p, Perturbacao.ALIVIO_PRECE) > 0)
			p.sendOverlayMessage(Component.literal("A prece acalma a mente.").withStyle(ChatFormatting.AQUA));
		if (p.level().dimension() != UMBRAL)
			return;
		if (!Moralidade.pronto(p, "dim_prece_umbral", RECARGA_PRECE)) {
			p.sendOverlayMessage(Component.literal("Aquiete o coração por alguns instantes antes de orar de novo.").withStyle(ChatFormatting.GRAY));
			return;
		}
		somarArrependimento(p, 1);
		msg(p, ChatFormatting.AQUA, "A prece sincera atravessa a névoa. Você sente um fio de esperança.");
	}

	/**
	 * O jogador tenta consolar um Espírito sofredor. "Paciente não ajuda paciente" (§5.7): quem ainda precisa de
	 * cuidado recebe a mensagem respeitosa e nada acontece. Devolve true se o consolo aconteceu.
	 */
	public static boolean tentarConsolar(ServerPlayer p) {
		if (!Estados.podeSocorrer(p)) {
			msg(p, ChatFormatting.AQUA, Estados.MSG_PRECISA_CUIDADO);
			return false;
		}
		if (!Moralidade.pronto(p, "dim_sofredor", 20 * 60))
			return false;
		ajudouSofredor(p);
		return true;
	}

	/** Chamado quando o jogador (em condições de socorrer) ajuda um Espírito sofredor no Umbral. */
	public static void ajudouSofredor(ServerPlayer p) {
		if (p.level().dimension() != UMBRAL || !Estados.podeSocorrer(p))
			return;
		Moralidade.ganhoRotina(p, 2, "Consolou um Espírito sofredor");
		somarArrependimento(p, 2);
		msg(p, ChatFormatting.AQUA, "Ao consolar quem sofre, você colabora com o trabalho dos socorristas — sem substituí-los.");
	}

	static void somarArrependimento(ServerPlayer p, int n) {
		p.setData(RegistroDimensao.ARREPENDIMENTO, p.getData(RegistroDimensao.ARREPENDIMENTO) + n);
		checarResgate(p);
	}

	/** Se já pode ser socorrido, os socorristas VÊM OFERECER ajuda (com consentimento; nunca teleportam sozinhos). */
	public static void checarResgate(ServerPlayer p) {
		Resgates.avaliar(p);
	}

	/** Repouso num leito do posto (clique na cama): alivia a perturbação. */
	static void repousar(ServerPlayer p) {
		if (!Moralidade.pronto(p, "umb_repouso", 2 * 60 * 20)) {
			p.sendOverlayMessage(Component.literal("Você acabou de repousar. Fique à vontade, sem pressa.").withStyle(ChatFormatting.GRAY));
			return;
		}
		Perturbacao.aliviar(p, Perturbacao.ALIVIO_REPOUSO);
		p.setData(RegistroDimensao.ARREPENDIMENTO, p.getData(RegistroDimensao.ARREPENDIMENTO) + 1);
		p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false));
		p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
		msg(p, ChatFormatting.AQUA, "Você se deita por alguns instantes. O cansaço da alma também precisa de repouso.");
	}

	/** Ponto de conversa/estudo do posto (atril): orientação; e, se liberada, a saída assistida (confirmada). */
	static void conversaNoPosto(ServerPlayer p, boolean confirmou) {
		int moral = Moralidade.get(p), arr = p.getData(RegistroDimensao.ARREPENDIMENTO), pert = Perturbacao.get(p);
		if (saidaAssistidaLiberada(moral, arr, pert)) {
			if (confirmou) {
				resgatar(p);
				return;
			}
			msg(p, ChatFormatting.GREEN, "Socorrista: — Você está sereno. Se desejar, podemos acompanhá-lo até Nosso Lar.");
			msg(p, ChatFormatting.YELLOW, "Clique de novo no atril em até 10 segundos para a saída assistida. Ou fique, se preferir.");
			return;
		}
		if (Moralidade.pronto(p, "umb_conversa_posto", 60 * 20)) {
			Perturbacao.aliviar(p, Perturbacao.ALIVIO_CONVERSA);
			p.setData(RegistroDimensao.ARREPENDIMENTO, arr + 1);
			msg(p, ChatFormatting.GREEN, "Socorrista: — Conversar já é um passo. A mudança começa no pensamento; cada boa intenção conta.");
		}
		if (pert > Perturbacao.LIMIAR_ORIENTADO)
			msg(p, ChatFormatting.GRAY, "Ainda há confusão na mente. Repouse num leito, ore (tecla B) e converse: a serenidade volta aos poucos.");
		else
			msg(p, ChatFormatting.GRAY, "Falta um pouco mais de reflexão: ore, repouse e converse. O tempo de reflexão também conta.");
	}

	public static final int ACOLHIMENTO_POSTO = 30 * 20;
	public static final int ACOLHIMENTO_NOSSO_LAR = 60 * 20;

	/** Chamado a cada segundo para quem está no Umbral. */
	static void tickUmbral(ServerPlayer p) {
		int t = p.getData(RegistroDimensao.TEMPO_UMBRAL) + 20;
		if (t >= TEMPO_REFLEXAO) {
			t = 0;
			p.setData(RegistroDimensao.ARREPENDIMENTO, p.getData(RegistroDimensao.ARREPENDIMENTO) + 1);
			msg(p, ChatFormatting.GRAY, "O tempo passa devagar no Umbral... e convida à reflexão sobre os próprios atos.");
		}
		p.setData(RegistroDimensao.TEMPO_UMBRAL, t);
		EstadoEspiritual e = Estados.get(p);
		if (e == EstadoEspiritual.EM_ACOLHIMENTO && Estados.tempoNoEstado(p) >= ACOLHIMENTO_POSTO) {
			Estados.transicionar(p, Perturbacao.get(p) >= Perturbacao.LIMIAR_PERTURBADO ? EstadoEspiritual.NO_UMBRAL_PERTURBADO : EstadoEspiritual.NO_UMBRAL_ORIENTADO,
					"fim do acolhimento no posto");
			msg(p, ChatFormatting.GRAY, "A triagem terminou. Você pode repousar, conversar, estudar e, quando sereno, pedir a saída assistida no atril.");
		}
		Perturbacao.tickUmbral(p);
		Resgates.avaliar(p);
	}

	// ================================================================ Nosso Lar: limite da colônia e saída (PROMPT §4.2)

	/** Raio padrão da colônia a partir da praça/chegada, usado enquanto {@code ColoniaApi.dentroDaMuralha} não tiver limite real. */
	public static final int RAIO_COLONIA_PADRAO = 160;
	/** Profundidade da zona de transição antes do risco de se perder. */
	public static final int ZONA_TRANSICAO = 40;

	/** Centro da colônia (praça, senão chegada). */
	static BlockPos centroColonia(ServerLevel level) {
		BlockPos c = ColoniaApi.local(level, "praca");
		return c != null ? c : ColoniaApi.local(level, "chegada");
	}

	/** {@code ColoniaApi.dentroDaMuralha} ainda devolve true para a dimensão inteira? (então usa o raio padrão) */
	static boolean apiSemLimite(ServerLevel level) {
		return ColoniaApi.dentroDaMuralha(level, new BlockPos(1_000_000, 64, 1_000_000));
	}

	/** Distância além do limite da colônia (0 = dentro). */
	public static double alemDoLimite(ServerLevel level, BlockPos pos) {
		if (!apiSemLimite(level)) {
			// Limite real: a muralha circular em volta do "centro" da colônia. A zona de transição começa logo após ela.
			BlockPos centro = ColoniaApi.local(level, "centro");
			if (centro != null) {
				if (ColoniaApi.dentroDaMuralha(level, pos))
					return 0;
				double d = LayoutColonia.raio(pos.getX() - centro.getX(), pos.getZ() - centro.getZ());
				return Math.max(1, d - (LayoutColonia.R_MURO - 1.5));
			}
		}
		BlockPos c = centroColonia(level);
		if (c == null)
			return 0;
		double d = Math.sqrt(Math.pow(pos.getX() - c.getX(), 2) + Math.pow(pos.getZ() - c.getZ(), 2));
		return Math.max(0, d - RAIO_COLONIA_PADRAO);
	}

	/** Risco de desorientação por segundo fora da colônia, sem acompanhamento (pontos de perturbação). Regra pura. */
	public static int riscoDesorientacao(int moral, EstadoEspiritual e) {
		if (moral >= Moralidade.ALTA && e != EstadoEspiritual.ENCARNADO_PERTURBADO)
			return 0;
		return Math.clamp((100 - moral) / 25, 1, 4);
	}

	public static boolean naZonaDeTransicao(ServerPlayer p) {
		return p.level().dimension() == NOSSO_LAR && alemDoLimite((ServerLevel) p.level(), p.blockPosition()) > 0;
	}

	/** Autoriza (acompanhamento) a saída da colônia por {@code ticks}. Para guias/mensageiros (áreas colonia/populacao). */
	public static void autorizarSaida(ServerPlayer p, int ticks) {
		p.setData(RegistroDimensao.SAIDA_ATE, p.level().getGameTime() + ticks);
		msg(p, ChatFormatting.AQUA, "Um mensageiro o acompanhará na saída da colônia. Siga com serenidade.");
	}

	/** Pedido de ajuda na zona de transição: um mensageiro o reconduz à colônia (o jogador pediu). */
	static void mensageiroDeVolta(ServerPlayer p) {
		ServerLevel level = (ServerLevel) p.level();
		BlockPos c = centroColonia(level);
		if (c == null)
			return;
		teleportar(p, level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.south(3)));
		Perturbacao.aliviar(p, 30);
		msg(p, ChatFormatting.AQUA, "Um mensageiro de Nosso Lar o encontra e o reconduz, com paciência, à praça da colônia.");
		msg(p, ChatFormatting.GRAY, "Sair sem preparo não é proibido, mas é imprudente. Para servir fora da colônia, peça acompanhamento.");
	}

	/** A cada segundo em Nosso Lar. */
	static void tickNossoLar(ServerPlayer p) {
		ServerLevel level = (ServerLevel) p.level();
		EstadoEspiritual e = Estados.get(p);
		if (e == EstadoEspiritual.EM_ACOLHIMENTO && Estados.tempoNoEstado(p) >= ACOLHIMENTO_NOSSO_LAR) {
			Estados.transicionar(p, EstadoEspiritual.EM_NOSSO_LAR, "fim do acolhimento");
			msg(p, ChatFormatting.GRAY, "Você se sente refeito. Explore a colônia com calma.");
		}
		double alem = alemDoLimite(level, p.blockPosition());
		if (alem <= 0) {
			if (Perturbacao.get(p) > 0 && level.getGameTime() % 40 < 20)
				Perturbacao.set(p, Perturbacao.get(p) - 1);
			return;
		}
		if (e == EstadoEspiritual.EM_VISITA_ASSISTIDA) {
			if (alem > ZONA_TRANSICAO)
				mensageiroDeVolta(p); // em sonho não se vai além
			return;
		}
		long agora = level.getGameTime();
		if (p.getData(RegistroDimensao.SAIDA_ATE) > agora) {
			if (alem > ZONA_TRANSICAO / 2)
				saidaAcompanhada(p);
			return;
		}
		if (Moralidade.pronto(p, "nl_aviso_limite", 20 * 45)) {
			msg(p, ChatFormatting.GOLD, "Você deixa a luz da colônia para trás. Além daqui começa a região de transição para o Umbral.");
			msg(p, ChatFormatting.GRAY, "Sem acompanhamento, quanto menos serena a mente, maior o risco de se perder. Volte, ou peça ajuda (tecla H / /espiritismo_socorro pedir).");
		}
		int risco = riscoDesorientacao(Moralidade.get(p), e);
		if (risco > 0)
			Perturbacao.agravar(p, risco, 90);
		if (alem > ZONA_TRANSICAO && Perturbacao.get(p) >= Perturbacao.LIMIAR_PERTURBADO)
			perderSeNoUmbral(p);
		else if (alem > ZONA_TRANSICAO && risco == 0 && Moralidade.pronto(p, "nl_aviso_sereno", 20 * 30))
			msg(p, ChatFormatting.AQUA, "Sua serenidade mantém o rumo, mas sem um guia não há por que seguir adiante. A colônia fica para trás de você.");
	}

	/** Saída acompanhada: vai ao Posto central do Umbral, orientado (serviço consciente). */
	static void saidaAcompanhada(ServerPlayer p) {
		ServerLevel umbral = p.level().getServer().getLevel(UMBRAL);
		if (umbral == null)
			return;
		p.setData(RegistroDimensao.SAIDA_ATE, 0L);
		if (!Estados.transicionar(p, EstadoEspiritual.NO_UMBRAL_ORIENTADO, "saída acompanhada"))
			Estados.forcar(p, EstadoEspiritual.NO_UMBRAL_ORIENTADO, "saída acompanhada");
		Perturbacao.set(p, 0);
		BlockPos chegada = garantir(umbral);
		BlockPos porta = ponto(umbral, "posto_porta");
		teleportar(p, umbral, porta != null ? porta : chegada);
		msg(p, ChatFormatting.AQUA, "Acompanhado, você atravessa a região de transição até o Posto de Socorro central do Umbral.");
	}

	/** Desorientação: sem acompanhamento e confuso, o jogador se perde na região de transição — e acaba no Umbral. */
	static void perderSeNoUmbral(ServerPlayer p) {
		ServerLevel umbral = p.level().getServer().getLevel(UMBRAL);
		if (umbral == null)
			return;
		garantir(umbral);
		RandomSource r = p.getRandom();
		double ang = r.nextDouble() * Math.PI * 2;
		int dist = 90 + r.nextInt(60);
		int x = (int) (Math.cos(ang) * dist), z = (int) (Math.sin(ang) * dist);
		BlockPos topo = superficie(umbral, x, z);
		if (!umbral.getFluidState(topo.below()).isEmpty() || !umbral.getFluidState(topo).isEmpty()) {
			umbral.setBlock(topo, net.minecraft.world.level.block.Blocks.PACKED_MUD.defaultBlockState(), 2);
			topo = topo.above();
		}
		if (!Estados.transicionar(p, EstadoEspiritual.NO_UMBRAL_PERTURBADO, "perdeu-se ao sair da colônia"))
			Estados.forcar(p, EstadoEspiritual.NO_UMBRAL_PERTURBADO, "perdeu-se ao sair da colônia");
		teleportar(p, umbral, topo);
		BlockPos porta = ponto(umbral, "posto_porta");
		if (porta != null)
			net.mcreator.modespiritismominecraft.espiritismo.Objetivo.definir(p, porta);
		msg(p, ChatFormatting.DARK_PURPLE, "As referências se confundem, a névoa se fecha... Você se perdeu na região de transição e está no Umbral.");
		msg(p, ChatFormatting.GOLD, "Não é castigo: é a consequência de sair sem preparo. Os Postos de Socorro têm luz no alto, há socorristas por perto, e você pode pedir ajuda a qualquer momento (tecla H).");
	}

	// ================================================================ Reencarnação

	/** Atril do Ministério do Auxílio: começa a reencarnação com busca INCREMENTAL do local. Idempotente (false se já em curso). */
	public static boolean iniciarReencarnacao(ServerPlayer p) {
		if (BuscaLocalSeguro.emAndamento(p.getUUID()))
			return false;
		EstadoEspiritual e = Estados.get(p);
		if (e != EstadoEspiritual.PRONTO_PARA_REENCARNAR && e != EstadoEspiritual.REENCARNANDO && !Estados.transicionar(p, EstadoEspiritual.PRONTO_PARA_REENCARNAR, "planejamento reencarnatório"))
			Estados.forcar(p, EstadoEspiritual.PRONTO_PARA_REENCARNAR, "planejamento reencarnatório");
		if (Estados.get(p) != EstadoEspiritual.REENCARNANDO)
			Estados.transicionar(p, EstadoEspiritual.REENCARNANDO, "reencarnando");
		BuscaLocalSeguro.iniciar(p, baseReencarnacao(p));
		msg(p, ChatFormatting.LIGHT_PURPLE, "Os trabalhadores do Ministério do Auxílio preparam o seu renascimento... Aguarde alguns instantes.");
		return true;
	}

	private static BlockPos baseReencarnacao(ServerPlayer p) {
		long ult = p.getData(RegistroDimensao.ULTIMA_TERRA);
		return ult == Long.MIN_VALUE ? p.level().getServer().overworld().getRespawnData().pos() : BlockPos.of(ult);
	}

	/** Reencarnação IMEDIATA (comando/testes): local limitado a chunks já carregados, sem gerar terreno. */
	public static BlockPos reencarnar(ServerPlayer p) {
		ServerLevel ow = p.level().getServer().overworld();
		BuscaLocalSeguro.cancelar(p.getUUID(), p.level().getServer());
		if (Estados.get(p) != EstadoEspiritual.REENCARNANDO)
			Estados.forcar(p, EstadoEspiritual.REENCARNANDO, "reencarnação imediata");
		BlockPos destino = BuscaLocalSeguro.imediato(p, ow, baseReencarnacao(p));
		concluirReencarnacao(p, destino);
		return destino;
	}

	/** Termina a reencarnação: idempotente (só age em REENCARNANDO), sem tocar no inventário. */
	static void concluirReencarnacao(ServerPlayer p, BlockPos destino) {
		if (Estados.get(p) != EstadoEspiritual.REENCARNANDO)
			return;
		Jornada.conquista(p, "reencarnacao");
		ServerLevel ow = p.level().getServer().overworld();
		p.setData(RegistroDimensao.ARREPENDIMENTO, 0);
		Resgates.cancelar(p, 0);
		Perturbacao.set(p, 0);
		teleportar(p, ow, destino);
		p.setData(RegistroDimensao.ULTIMA_TERRA, destino.asLong());
		Estados.transicionar(p, EstadoEspiritual.ENCARNADO_ESTAVEL, "renasceu");
		p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
		msg(p, ChatFormatting.LIGHT_PURPLE, "Você renasce na Terra para uma nova existência.");
		msg(p, ChatFormatting.GRAY,
				"O esquecimento do passado é uma bênção: permite recomeçar sem o peso das lembranças, enquanto as tendências e o progresso conquistados permanecem como intuição (LE, q. 392–399).");
		aplicarProva(p);
	}

	private static void aplicarProva(ServerPlayer p) {
		switch (p.getRandom().nextInt(4)) {
			case 0 -> {
				p.getFoodData().setFoodLevel(6);
				p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120 * 20, 0));
				msg(p, ChatFormatting.YELLOW, "Prova escolhida: a escassez. Você começa com fome — exercite a paciência e a confiança.");
			}
			case 1 -> {
				p.setHealth(Math.max(4F, p.getMaxHealth() / 2F));
				msg(p, ChatFormatting.YELLOW, "Prova escolhida: um corpo frágil. Você começa com metade da vida — cuide-se com prudência.");
			}
			case 2 -> {
				p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180 * 20, 0));
				msg(p, ChatFormatting.YELLOW, "Prova escolhida: a fraqueza. Por um tempo suas forças serão menores — aprenda a pedir e a aceitar ajuda.");
			}
			default -> {
				p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120 * 20, 0));
				msg(p, ChatFormatting.YELLOW, "Prova escolhida: a lentidão. Por um tempo tudo custará mais — exercite a perseverança.");
			}
		}
		msg(p, ChatFormatting.GRAY,
				"Provas e expiações não são castigos arbitrários: são ocasiões de progresso que o próprio Espírito aceita ou escolhe antes de renascer (LE, q. 258–273; ESE, cap. V).");
	}

	/** Procura na Terra um ponto seco e firme a 600–1200 blocos de {@code base} SÓ entre chunks já carregados (não gera terreno); senão o spawn. */
	public static BlockPos localSeguroDistante(ServerLevel ow, BlockPos base, RandomSource r) {
		for (int tentativa = 0; tentativa < 12; tentativa++) {
			double ang = r.nextDouble() * Math.PI * 2;
			int dist = 600 + r.nextInt(601);
			BlockPos topo = BuscaLocalSeguro.avaliarCarregado(ow, base.getX() + (int) (Math.cos(ang) * dist), base.getZ() + (int) (Math.sin(ang) * dist));
			if (topo != null)
				return topo;
		}
		BlockPos s = ow.getRespawnData().pos();
		return superficie(ow, s.getX(), s.getZ());
	}

	// ================================================================ Emancipação da alma no sono

	/** Inicia um desdobramento (1) ou pesadelo (2): leva o jogador e guarda o ponto ao lado da cama. */
	public static void iniciarSonho(ServerPlayer p, int tipo) {
		ServerLevel destino = p.level().getServer().getLevel(tipo == 2 ? UMBRAL : NOSSO_LAR);
		if (destino == null || p.getData(RegistroDimensao.SONHO_FIM) > 0)
			return;
		BlockPos retorno = p.blockPosition();
		String dim = p.level().dimension().identifier().toString();
		BlockPos chegada = garantir(destino);
		if (tipo == 1) {
			BlockPos praca = ponto(destino, "praca");
			if (praca != null)
				chegada = praca.south(5);
		}
		if (!Estados.transicionar(p, EstadoEspiritual.EM_VISITA_ASSISTIDA, tipo == 2 ? "pesadelo" : "desdobramento no sono"))
			Estados.forcar(p, EstadoEspiritual.EM_VISITA_ASSISTIDA, "sonho");
		teleportar(p, destino, chegada);
		p.setData(RegistroDimensao.SONHO_RETORNO, retorno.asLong());
		p.setData(RegistroDimensao.SONHO_DIM, dim);
		p.setData(RegistroDimensao.SONHO_FIM, p.level().getGameTime() + (tipo == 2 ? DURACAO_PESADELO : DURACAO_SONHO));
		if (tipo == 1) {
			msg(p, ChatFormatting.LIGHT_PURPLE, "Enquanto o corpo repousa, o Espírito se desprende parcialmente e visita outros lugares (LE, q. 400–418).");
			msg(p, ChatFormatting.GRAY, "Você está em Nosso Lar, em sonho. Aproveite estes instantes: logo despertará em sua cama.");
		} else {
			msg(p, ChatFormatting.DARK_PURPLE, "Durante o sono o Espírito busca as companhias que se afinam com seus pensamentos (LE, q. 400–418)...");
			msg(p, ChatFormatting.GRAY, "Um sonho pesado: névoa e lamentos. É um aviso, não uma condenação. Logo você despertará.");
		}
	}

	/** Termina o sonho: volta ao lado da cama. */
	public static void terminarSonho(ServerPlayer p) {
		long fim = p.getData(RegistroDimensao.SONHO_FIM);
		if (fim == 0)
			return;
		MinecraftServer server = p.level().getServer();
		ServerLevel volta = null;
		Identifier dim = Identifier.tryParse(p.getData(RegistroDimensao.SONHO_DIM));
		if (dim != null)
			volta = server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
		if (volta == null)
			volta = server.overworld();
		long ret = p.getData(RegistroDimensao.SONHO_RETORNO);
		BlockPos pos = ret == Long.MIN_VALUE ? pontoNaTerra(p) : BlockPos.of(ret);
		p.setData(RegistroDimensao.SONHO_FIM, 0L);
		p.setData(RegistroDimensao.SONHO_RETORNO, Long.MIN_VALUE);
		p.setData(RegistroDimensao.SONHO_DIM, "");
		p.setData(RegistroDimensao.RESGATE_EM, 0L);
		teleportar(p, volta, pos);
		EstadoEspiritual acordado = Moralidade.get(p) <= Moralidade.BAIXA ? EstadoEspiritual.ENCARNADO_PERTURBADO : EstadoEspiritual.ENCARNADO_ESTAVEL;
		if (!Estados.transicionar(p, acordado, "despertou"))
			Estados.forcar(p, acordado, "despertou");
		msg(p, ChatFormatting.GRAY, "Você desperta em sua cama. Restam impressões vagas, como lembranças de um sonho.");
	}

	// ================================================================ Utilidades

	static void msg(ServerPlayer p, ChatFormatting cor, String texto) {
		p.sendSystemMessage(Component.literal(texto).withStyle(cor));
	}
}
