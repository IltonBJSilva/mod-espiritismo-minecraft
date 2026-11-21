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

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
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
		Map<String, Long> p = pontos(level);
		if (!p.containsKey("chegada")) {
			p.putAll(construir(level, 0, 0, level.dimension() == NOSSO_LAR));
			level.setData(RegistroDimensao.PONTOS, p);
		}
		return BlockPos.of(p.get("chegada"));
	}

	/** Constrói as construções de Nosso Lar ({@code nossoLar}) ou do Umbral com centro em (x, z), em qualquer Level. */
	public static Map<String, Long> construir(ServerLevel level, int x, int z, boolean nossoLar) {
		int g = Math.clamp(ConstrucoesPlanos.chao(level, x, z), level.getSeaLevel() + 2, 100);
		BlockPos origem = new BlockPos(x, g, z);
		Map<String, Long> novos = nossoLar ? ConstrucoesPlanos.construirNossoLar(level, origem) : ConstrucoesPlanos.construirUmbral(level, origem);
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

	// ================================================================ Resgate (sair do Umbral)

	/** Os socorristas levam o jogador do Umbral (ou de onde estiver) às Câmaras de Retificação de Nosso Lar. */
	public static void resgatar(ServerPlayer p) {
		ServerLevel nl = p.level().getServer().getLevel(NOSSO_LAR);
		if (nl == null)
			return;
		boolean doUmbral = p.level().dimension() == UMBRAL;
		p.setData(RegistroDimensao.ARREPENDIMENTO, 0);
		p.setData(RegistroDimensao.TEMPO_UMBRAL, 0);
		p.setData(RegistroDimensao.RESGATE_EM, 0L);
		teleportar(p, nl, garantir(nl));
		p.removeEffect(MobEffects.DARKNESS);
		p.removeEffect(MobEffects.SLOWNESS);
		p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
		if (doUmbral)
			Moralidade.alterar(p, 3, "Arrependimento sincero");
		msg(p, ChatFormatting.AQUA, "Mãos amigas o amparam. Os socorristas o trouxeram, pelo Posto de Socorro, até as Câmaras de Retificação de Nosso Lar.");
		msg(p, ChatFormatting.GRAY, "Ninguém fica esquecido: basta o arrependimento sincero e a vontade de melhorar para que o auxílio chegue. Para voltar à Terra, procure o Ministério do Auxílio (a oeste da praça).");
	}

	/** Chamado por cada prece (gancho {@code npc.Prece.AO_ORAR}, ou o comando /espiritismo_prece). */
	public static void aoOrar(ServerPlayer p) {
		if (p.level().dimension() != UMBRAL)
			return;
		if (!Moralidade.pronto(p, "dim_prece_umbral", RECARGA_PRECE)) {
			p.sendOverlayMessage(Component.literal("Aquiete o coração por alguns instantes antes de orar de novo.").withStyle(ChatFormatting.GRAY));
			return;
		}
		somarArrependimento(p, 1);
		msg(p, ChatFormatting.AQUA, "A prece sincera atravessa a névoa. Você sente um fio de esperança.");
	}

	/** Chamado quando o jogador ajuda um Espírito sofredor no Umbral. */
	public static void ajudouSofredor(ServerPlayer p) {
		if (p.level().dimension() != UMBRAL)
			return;
		Moralidade.alterar(p, 2, "Consolou um Espírito sofredor");
		somarArrependimento(p, 2);
		msg(p, ChatFormatting.AQUA, "Ao consolar quem sofre ao seu lado, você mesmo se sente mais leve.");
	}

	private static void somarArrependimento(ServerPlayer p, int n) {
		p.setData(RegistroDimensao.ARREPENDIMENTO, p.getData(RegistroDimensao.ARREPENDIMENTO) + n);
		checarResgate(p);
	}

	/** Se já pode ser socorrido, chama o socorrista (que leva o jogador alguns segundos depois). */
	public static void checarResgate(ServerPlayer p) {
		if (p.level().dimension() != UMBRAL || p.getData(RegistroDimensao.RESGATE_EM) > 0)
			return;
		if (!resgateLiberado(Moralidade.get(p), p.getData(RegistroDimensao.ARREPENDIMENTO)))
			return;
		p.setData(RegistroDimensao.RESGATE_EM, p.level().getGameTime() + ESPERA_SOCORRISTA);
		EntityType<?> soc = ConstrucoesPlanos.tipo("socorrista");
		if (soc != null)
			ConstrucoesPlanos.colocar((ServerLevel) p.level(), soc, p.blockPosition().relative(p.getDirection(), 2), p.getYRot() + 180F);
		msg(p, ChatFormatting.GOLD, "Uma luz suave se aproxima pela névoa: é um socorrista. — Venha, irmão. O seu pedido foi ouvido.");
	}

	/** Chamado a cada segundo para quem está no Umbral. */
	static void tickUmbral(ServerPlayer p) {
		long resgate = p.getData(RegistroDimensao.RESGATE_EM);
		if (resgate > 0 && p.level().getGameTime() >= resgate) {
			resgatar(p);
			return;
		}
		int t = p.getData(RegistroDimensao.TEMPO_UMBRAL) + 20;
		if (t >= TEMPO_REFLEXAO) {
			t = 0;
			p.setData(RegistroDimensao.ARREPENDIMENTO, p.getData(RegistroDimensao.ARREPENDIMENTO) + 1);
			msg(p, ChatFormatting.GRAY, "O tempo passa devagar no Umbral... e convida à reflexão sobre os próprios atos.");
		}
		p.setData(RegistroDimensao.TEMPO_UMBRAL, t);
		checarResgate(p);
	}

	// ================================================================ Reencarnação

	/** Volta à Terra numa nova existência: local seguro distante do anterior, esquecimento do passado e uma prova. */
	public static BlockPos reencarnar(ServerPlayer p) {
		ServerLevel ow = p.level().getServer().overworld();
		long ult = p.getData(RegistroDimensao.ULTIMA_TERRA);
		BlockPos base = ult == Long.MIN_VALUE ? ow.getRespawnData().pos() : BlockPos.of(ult);
		BlockPos destino = localSeguroDistante(ow, base, p.getRandom());
		p.setData(RegistroDimensao.ARREPENDIMENTO, 0);
		p.setData(RegistroDimensao.RESGATE_EM, 0L);
		teleportar(p, ow, destino);
		p.setData(RegistroDimensao.ULTIMA_TERRA, destino.asLong());
		p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
		msg(p, ChatFormatting.LIGHT_PURPLE, "Você renasce na Terra para uma nova existência.");
		msg(p, ChatFormatting.GRAY,
				"O esquecimento do passado é uma bênção: permite recomeçar sem o peso das lembranças, enquanto as tendências e o progresso conquistados permanecem como intuição (LE, q. 392–399).");
		aplicarProva(p);
		return destino;
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

	/** Procura na Terra um ponto seco e firme a 600–1200 blocos de {@code base}. */
	public static BlockPos localSeguroDistante(ServerLevel ow, BlockPos base, RandomSource r) {
		for (int tentativa = 0; tentativa < 12; tentativa++) {
			double ang = r.nextDouble() * Math.PI * 2;
			int dist = 600 + r.nextInt(601);
			int x = base.getX() + (int) (Math.cos(ang) * dist), z = base.getZ() + (int) (Math.sin(ang) * dist);
			x = Math.clamp(x, -29_000_000, 29_000_000);
			z = Math.clamp(z, -29_000_000, 29_000_000);
			BlockPos topo = superficie(ow, x, z);
			BlockState chao = ow.getBlockState(topo.below());
			if (!chao.getFluidState().isEmpty() || chao.is(BlockTags.LEAVES) || chao.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) || chao.isAir())
				continue;
			if (!ow.getBlockState(topo).getFluidState().isEmpty() || !ow.getBlockState(topo.above()).isAir())
				continue;
			if (topo.getY() <= ow.getMinY() + 2)
				continue;
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
		msg(p, ChatFormatting.GRAY, "Você desperta em sua cama. Restam impressões vagas, como lembranças de um sonho.");
	}

	// ================================================================ Utilidades

	static void msg(ServerPlayer p, ChatFormatting cor, String texto) {
		p.sendSystemMessage(Component.literal(texto).withStyle(cor));
	}
}
