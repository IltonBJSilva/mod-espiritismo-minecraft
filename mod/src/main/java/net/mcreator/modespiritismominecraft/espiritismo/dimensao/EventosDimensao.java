package net.mcreator.modespiritismominecraft.espiritismo.dimensao;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.estado.Estados;
import net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.function.Consumer;
import java.util.UUID;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collection;
import java.util.Arrays;

/**
 * Eventos da área "dimensao"/"umbral": morte e renascimento (com a máquina de estados), Umbral (perturbação,
 * resgate com consentimento, Postos de Socorro), saída e entrada de Nosso Lar, reencarnação incremental, sono,
 * reconexão sem softlock e comandos.
 */
@EventBusSubscriber
public final class EventosDimensao {
	/** Destino decidido no evento de posição de renascimento, para as mensagens depois do renascimento. */
	private static final Map<UUID, Planos.Destino> RENASCENDO = new HashMap<>();
	/** Jogadores que acabaram de acordar depois de uma noite inteira (processados no próximo tick do servidor). */
	private static final Set<UUID> ACORDARAM = new HashSet<>();
	/** Entraram em Nosso Lar sem critério: são reconduzidos no próximo tick (fora do evento de troca de dimensão). */
	private static final Map<UUID, ResourceKey<Level>> RECONDUZIR = new HashMap<>();
	/** Confirmação (atril do Auxílio / saída assistida no posto): jogador → tick até quando vale. */
	private static final Map<UUID, Long> CONFIRMAR = new HashMap<>();
	private static boolean ganchoPrece = false;

	private EventosDimensao() {
	}

	// ================================================================ Morte → renascimento

	@SubscribeEvent
	public static void posicaoRenascimento(PlayerRespawnPositionEvent event) {
		if (event.isFromEndFight() || !(event.getEntity() instanceof ServerPlayer velho))
			return;
		if (velho.getData(RegistroDimensao.SONHO_FIM) > 0)
			return; // morreu sonhando: acorda na cama, como de costume
		ResourceKey<Level> origem = velho.level().dimension();
		int moral = Moralidade.get(velho);
		Planos.Destino destino;
		if (origem == Level.OVERWORLD) {
			velho.setData(RegistroDimensao.ULTIMA_TERRA, velho.blockPosition().asLong());
			destino = Planos.destinoAposMorte(moral);
		} else if (origem == Planos.UMBRAL)
			destino = moral <= Planos.LIMIAR_UMBRAL ? Planos.Destino.UMBRAL : Planos.Destino.NOSSO_LAR;
		else if (origem == Planos.NOSSO_LAR)
			destino = Planos.Destino.NOSSO_LAR;
		else
			return;
		RENASCENDO.put(velho.getUUID(), destino);
		if (destino == Planos.Destino.PERTURBACAO)
			return;
		ServerLevel nivel = velho.level().getServer().getLevel(destino == Planos.Destino.NOSSO_LAR ? Planos.NOSSO_LAR : Planos.UMBRAL);
		if (nivel == null) {
			RENASCENDO.remove(velho.getUUID());
			return;
		}
		BlockPos chegada = Planos.garantir(nivel); // Umbral: só o pátio de chegada; o posto é construído aos poucos
		// Quem já estava no Umbral desperta à porta do Posto central (não recomeça do zero longe dele).
		if (origem == Planos.UMBRAL && destino == Planos.Destino.UMBRAL) {
			BlockPos porta = Planos.ponto(nivel, "posto_porta");
			if (porta != null)
				chegada = porta;
		}
		event.setTeleportTransition(new TeleportTransition(nivel, Vec3.atBottomCenterOf(chegada), Vec3.ZERO, 180F, 0F, TeleportTransition.DO_NOTHING));
	}

	/** Leva o estado até RECEM_DESENCARNADO, passando por DESENCARNANDO quando vinha da Terra. */
	private static void desencarnou(ServerPlayer p) {
		EstadoEspiritual e = Estados.get(p);
		if (e == EstadoEspiritual.ENCARNADO_ESTAVEL || e == EstadoEspiritual.ENCARNADO_PERTURBADO)
			Estados.transicionar(p, EstadoEspiritual.DESENCARNANDO, "morte do corpo");
		if (!Estados.transicionar(p, EstadoEspiritual.RECEM_DESENCARNADO, "despertar após a morte"))
			Estados.forcar(p, EstadoEspiritual.RECEM_DESENCARNADO, "despertar após a morte");
	}

	@SubscribeEvent
	public static void renasceu(PlayerEvent.PlayerRespawnEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer p))
			return;
		Planos.Destino d = RENASCENDO.remove(p.getUUID());
		if (d == null)
			return;
		Resgates.cancelar(p, 0);
		p.setData(RegistroDimensao.TEMPO_UMBRAL, 0);
		desencarnou(p);
		switch (d) {
			case NOSSO_LAR -> {
				Estados.transicionar(p, EstadoEspiritual.EM_ACOLHIMENTO, "despertou em Nosso Lar");
				Perturbacao.set(p, 0);
				p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0, false, false));
				Planos.msg(p, ChatFormatting.AQUA, "Você desperta nas Câmaras de Retificação, em Nosso Lar. Mãos amigas cuidaram de você.");
				Planos.msg(p, ChatFormatting.GRAY,
						"A perturbação que segue a morte do corpo é passageira, e tanto mais breve quanto mais o Espírito se desprendeu da matéria pelo bem praticado (LE, q. 163–165).");
				nadaMaterial(p);
				net.mcreator.modespiritismominecraft.espiritismo.Jornada.conquista(p, "nosso_lar");
				// O bem feito em silêncio aparece agora (ESE, cap. XIII).
				int discretas = p.getData(Moralidade.CARIDADE_OCULTA);
				if (discretas > 0)
					Planos.msg(p, ChatFormatting.GOLD, "Um benfeitor lhe mostra seus registros: " + discretas + (discretas == 1 ? " boa ação feita" : " boas ações feitas")
							+ " em silêncio, sem esperar nada em troca. Nenhuma delas se perdeu.");
				Planos.msg(p, ChatFormatting.GRAY, "Explore a colônia. Para renascer na Terra, procure o Ministério do Auxílio, a oeste da praça.");
			}
			case UMBRAL -> {
				// O arrependimento acumulado NÃO se perde ao desencarnar de novo no Umbral (sem ciclo punitivo).
				int moral = Moralidade.get(p);
				Perturbacao.set(p, Math.max(Perturbacao.get(p), Perturbacao.inicialNoUmbral(moral)));
				Estados.transicionar(p, Perturbacao.get(p) >= Perturbacao.LIMIAR_PERTURBADO ? EstadoEspiritual.NO_UMBRAL_PERTURBADO : EstadoEspiritual.NO_UMBRAL_ORIENTADO, "despertou no Umbral");
				p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 0, false, false));
				Planos.msg(p, ChatFormatting.DARK_PURPLE, "Você desperta numa região escura e enevoada: o Umbral.");
				Planos.msg(p, ChatFormatting.GRAY,
						"Não é um castigo eterno: é o reflexo do próprio estado íntimo, como André Luiz descreve em Nosso Lar (caps. 1–2). Ninguém está abandonado.");
				nadaMaterial(p);
				Planos.msg(p, ChatFormatting.GOLD,
						"Ore (tecla B), reflita, procure a luz de um Posto de Socorro (ao norte) ou peça ajuda a qualquer momento (tecla H / /espiritismo_socorro pedir). Os socorristas virão, e você decide se aceita.");
			}
			case PERTURBACAO -> {
				Estados.transicionar(p, EstadoEspiritual.REENCARNANDO, "perturbação breve");
				Estados.transicionar(p, EstadoEspiritual.ENCARNADO_ESTAVEL, "retorno à Terra");
				p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
				p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 140, 0, false, false));
				Planos.msg(p, ChatFormatting.LIGHT_PURPLE, "Tudo parece confuso por um instante... é a perturbação que acompanha a passagem (LE, q. 163–165).");
				nadaMaterial(p);
				Planos.msg(p, ChatFormatting.GRAY,
						"Você retorna à Terra para uma nova experiência. O esquecimento do passado permite recomeçar; o que você aprendeu permanece (LE, q. 392–399).");
			}
		}
	}

	private static void nadaMaterial(ServerPlayer p) {
		Planos.msg(p, ChatFormatting.GRAY,
				"Os bens materiais ficaram na Terra: nada material se leva. Sua moralidade, sua classe e seu progresso continuam — o Espírito não retrograda (LE, q. 118).");
	}

	// ================================================================ Tick do jogador

	@SubscribeEvent
	public static void tickJogador(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer p))
			return;
		if (p.tickCount % 2 == 0 && Resgates.fase(p) != Resgates.NENHUM)
			Resgates.tick(p);
		if (p.tickCount % 20 != 0)
			return;
		Estados.migrar(p);
		long agora = p.level().getGameTime();
		long sonho = p.getData(RegistroDimensao.SONHO_FIM);
		if (sonho > 0) {
			if (agora >= sonho)
				Planos.terminarSonho(p);
			return;
		}
		ResourceKey<Level> dim = p.level().dimension();
		if (dim == Planos.UMBRAL)
			Planos.tickUmbral(p);
		else if (dim == Planos.NOSSO_LAR)
			Planos.tickNossoLar(p);
		else {
			// Na Terra: ENCARNADO_PERTURBADO acompanha a moralidade "Perturbada" (com folga para não oscilar).
			EstadoEspiritual e = Estados.get(p);
			int moral = Moralidade.get(p);
			if (e == EstadoEspiritual.ENCARNADO_ESTAVEL && moral <= Moralidade.BAIXA)
				Estados.transicionar(p, EstadoEspiritual.ENCARNADO_PERTURBADO, "moralidade baixa");
			else if (e == EstadoEspiritual.ENCARNADO_PERTURBADO && moral > Moralidade.BAIXA + 5)
				Estados.transicionar(p, EstadoEspiritual.ENCARNADO_ESTAVEL, "moralidade recuperada");
			if (Perturbacao.get(p) > 0 && p.tickCount % 100 == 0)
				Perturbacao.set(p, Perturbacao.get(p) - 5);
		}
		if (p.tickCount % 200 == 0) {
			ServerLevel level = (ServerLevel) p.level();
			if (dim == Planos.UMBRAL)
				ConstrucoesUmbral.verificarNpcs(level, p.blockPosition());
			else if (dim == Planos.NOSSO_LAR) {
				Map<String, Long> pontos = Planos.pontos(level);
				if (!pontos.isEmpty())
					ConstrucoesPlanos.verificarNpcs(level, pontos, true);
			}
		}
	}

	// ================================================================ Conexão / reconexão (sem softlock)

	@SubscribeEvent
	public static void aoEntrar(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer p)
			aoReconectar(p);
	}

	/** Restaura um estado jogável após (re)conexão. Público para os testes. */
	public static void aoReconectar(ServerPlayer p) {
		Estados.migrar(p);
		if (p.getData(RegistroDimensao.SONHO_FIM) > 0) {
			Planos.terminarSonho(p); // saiu do jogo no meio de um sonho
			return;
		}
		Resgates.aoReconectar(p);
		EstadoEspiritual e = Estados.get(p);
		if (e == EstadoEspiritual.REENCARNANDO && p.level().dimension() != Level.OVERWORLD) {
			Planos.iniciarReencarnacao(p); // a busca recomeça (incremental)
			return;
		}
		if (e == EstadoEspiritual.DESENCARNANDO || e == EstadoEspiritual.RECEM_DESENCARNADO && p.isAlive() || !Estados.coerente(e, p.level().dimension())
				|| e == EstadoEspiritual.REENCARNANDO)
			Estados.forcar(p, Estados.deduzir(p.level().dimension(), Moralidade.get(p)), "reconexão: estado incoerente com a dimensão");
	}

	@SubscribeEvent
	public static void aoSair(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer p) {
			Resgates.aoSair(p);
			BuscaLocalSeguro.cancelar(p.getUUID(), p.level().getServer());
			Perturbacao.esquecer(p);
			CONFIRMAR.remove(p.getUUID());
		}
	}

	/** Entrada em Nosso Lar só pelos critérios (§4.1); e correção de estados incoerentes após qualquer viagem. */
	@SubscribeEvent
	public static void trocouDimensao(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer p) || RENASCENDO.containsKey(p.getUUID()))
			return;
		EstadoEspiritual e = Estados.get(p);
		if (event.getTo() == Planos.NOSSO_LAR && !Planos.podeEntrarNossoLar(e) && !p.isCreative() && !p.isSpectator()) {
			RECONDUZIR.put(p.getUUID(), event.getFrom());
			return;
		}
		if (!Estados.coerente(e, event.getTo()) && e != EstadoEspiritual.REENCARNANDO && e != EstadoEspiritual.EM_RESGATE)
			Estados.forcar(p, Estados.deduzir(event.getTo(), Moralidade.get(p)), "viagem por outro meio");
	}

	private static void reconduzir(ServerPlayer p, ResourceKey<Level> de) {
		var server = p.level().getServer();
		Planos.msg(p, ChatFormatting.GOLD, "Uma força serena o impede de entrar: Nosso Lar acolhe por desencarne, sono, resgate aceito ou convite — não por passagens construídas.");
		ServerLevel volta = server.getLevel(de);
		if (volta == null || volta.dimension() == Planos.NOSSO_LAR)
			volta = server.overworld();
		BlockPos pos = volta.dimension() == Level.OVERWORLD ? Planos.pontoNaTerra(p) : Planos.garantir(volta);
		Planos.teleportar(p, volta, pos);
		Estados.forcar(p, Estados.deduzir(volta.dimension(), Moralidade.get(p)), "entrada recusada em Nosso Lar");
	}

	// ================================================================ Sono: emancipação da alma

	@SubscribeEvent
	public static void acordou(PlayerWakeUpEvent event) {
		// (false, false) = acordado pelo servidor depois que a noite passou (ServerLevel.wakeUpAllPlayers)
		if (event.wakeImmediately() || event.updateLevel() || !(event.getEntity() instanceof ServerPlayer p))
			return;
		if (p.level().dimension() == Level.OVERWORLD && !p.isPassenger() && p.getData(RegistroDimensao.SONHO_FIM) == 0)
			ACORDARAM.add(p.getUUID());
	}

	@SubscribeEvent
	public static void tickServidor(ServerTickEvent.Post event) {
		var server = event.getServer();
		ConstrucoesUmbral.tickServidor(server);
		BuscaLocalSeguro.tick(server);
		if (!RECONDUZIR.isEmpty()) {
			for (var en : Map.copyOf(RECONDUZIR).entrySet()) {
				ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
				if (p != null && p.level().dimension() == Planos.NOSSO_LAR)
					reconduzir(p, en.getValue());
			}
			RECONDUZIR.clear();
		}
		if (ACORDARAM.isEmpty())
			return;
		for (UUID id : ACORDARAM) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null || !p.isAlive() || p.isSleeping() || p.isPassenger() || p.level().dimension() != Level.OVERWORLD)
				continue;
			int tipo = Planos.tipoSonho(Moralidade.get(p), p.getRandom().nextFloat());
			if (tipo != 0)
				Planos.iniciarSonho(p, tipo);
		}
		ACORDARAM.clear();
	}

	// ================================================================ Interações

	private static boolean confirmou(ServerPlayer p, String chave) {
		long agora = p.level().getGameTime();
		UUID id = p.getUUID();
		Long ate = CONFIRMAR.get(id);
		if (ate != null && agora <= ate) {
			CONFIRMAR.remove(id);
			return true;
		}
		CONFIRMAR.put(id, agora + 200);
		return false;
	}

	@SubscribeEvent
	public static void cliqueBloco(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p))
			return;
		ServerLevel level = (ServerLevel) p.level();
		BlockPos pos = event.getPos();
		if (level.dimension() == Planos.UMBRAL) {
			var estadoBloco = level.getBlockState(pos);
			if (estadoBloco.getBlock() instanceof BedBlock && ConstrucoesUmbral.dentroDePosto(level, pos)) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
				Planos.repousar(p);
			} else if (estadoBloco.is(Blocks.LECTERN) && ehAtrilDeConversa(level, pos)) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
				Planos.conversaNoPosto(p, confirmou(p, "saida"));
			}
			return;
		}
		if (level.dimension() != Planos.NOSSO_LAR || !level.getBlockState(pos).is(Blocks.LECTERN))
			return;
		if (pos.equals(Planos.ponto(level, "auxilio"))) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
			if (p.getData(RegistroDimensao.SONHO_FIM) > 0) {
				Planos.msg(p, ChatFormatting.GRAY, "Em sonho não se planeja uma nova existência. Aguarde o despertar.");
				return;
			}
			if (BuscaLocalSeguro.emAndamento(p.getUUID())) {
				p.sendOverlayMessage(Component.literal("O seu renascimento já está sendo preparado.").withStyle(ChatFormatting.LIGHT_PURPLE));
				return;
			}
			if (confirmou(p, "auxilio")) {
				Planos.iniciarReencarnacao(p);
			} else {
				Planos.msg(p, ChatFormatting.AQUA, "Ministério do Auxílio — planejamento reencarnatório.");
				Planos.msg(p, ChatFormatting.GRAY,
						"Antes de renascer, o Espírito, conforme seu adiantamento, escolhe ou aceita as provas que o ajudarão a progredir (LE, q. 258–273). Ao renascer, esquecerá o passado, mas conservará o progresso.");
				Planos.msg(p, ChatFormatting.YELLOW, "Clique de novo no atril em até 10 segundos para reencarnar na Terra.");
			}
		} else if (pos.equals(Planos.ponto(level, "mirante"))) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
			mirante(p);
		}
	}

	private static boolean ehAtrilDeConversa(ServerLevel level, BlockPos pos) {
		Map<String, Long> pontos = Planos.pontos(level);
		for (int i = 0; i < ConstrucoesUmbral.POSTOS; i++) {
			Long v = pontos.get("posto_" + i + "_conversa");
			if (v != null && v == pos.asLong())
				return true;
		}
		return false;
	}

	static void mirante(ServerPlayer p) {
		Planos.msg(p, ChatFormatting.GOLD, "— Mirante: a escala dos mundos (O Evangelho segundo o Espiritismo, cap. III) —");
		Planos.msg(p, ChatFormatting.WHITE, "1. Mundos primitivos: lugar das primeiras encarnações da alma humana.");
		Planos.msg(p, ChatFormatting.WHITE, "2. Mundos de expiação e provas: neles o mal ainda predomina — é o caso atual da Terra.");
		Planos.msg(p, ChatFormatting.WHITE, "3. Mundos de regeneração: as almas que ainda têm o que expiar retemperam as forças, repousando das lutas.");
		Planos.msg(p, ChatFormatting.WHITE, "4. Mundos ditosos ou felizes: o bem supera o mal.");
		Planos.msg(p, ChatFormatting.WHITE, "5. Mundos celestes ou divinos: habitação dos Espíritos depurados, onde reina exclusivamente o bem.");
		Planos.msg(p, ChatFormatting.GOLD, "— A escala espírita (O Livro dos Espíritos, q. 100–113) —");
		Planos.msg(p, ChatFormatting.WHITE, "3ª ordem, Espíritos imperfeitos: predomínio da matéria sobre o espírito, propensão ao mal ou à ignorância (q. 101–106).");
		Planos.msg(p, ChatFormatting.WHITE, "2ª ordem, bons Espíritos: predomínio do espírito sobre a matéria e desejo do bem (q. 107–111).");
		Planos.msg(p, ChatFormatting.WHITE, "1ª ordem, Espíritos puros: nenhuma influência da matéria; superioridade intelectual e moral absoluta (q. 112–113).");
		Planos.msg(p, ChatFormatting.GRAY, "Todos os Espíritos são criados simples e ignorantes e progridem pelo próprio esforço — ninguém está condenado a ficar para trás.");
	}

	// Prioridade alta: roda antes do EventosNpc, que cancela o clique em Espíritos.
	@SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
	public static void cliqueEntidade(PlayerInteractEvent.EntityInteract event) {
		if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p) || p.level().dimension() != Planos.UMBRAL)
			return;
		EntityType<?> tipo = event.getTarget().getType();
		if (tipo == ConstrucoesUmbral.tipo("espirito_sofredor")) {
			// "Paciente não ajuda paciente": quem ainda precisa de cuidado não assume o consolo (e não recebe a conversa de socorro do npc).
			if (!Planos.tentarConsolar(p) && !Estados.podeSocorrer(p)) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
			}
		} else if (tipo == ConstrucoesUmbral.tipo("socorrista") && !event.getTarget().entityTags().contains(Resgates.TAG_TEMP)) {
			// Socorrista de posto: o próprio jogador procura ajuda → triagem/acolhimento (consentido).
			EstadoEspiritual e = Estados.get(p);
			if (e == EstadoEspiritual.NO_UMBRAL_PERTURBADO || e == EstadoEspiritual.NO_UMBRAL_ORIENTADO && Perturbacao.get(p) > 0) {
				if (Estados.transicionar(p, EstadoEspiritual.EM_ACOLHIMENTO, "triagem no posto")) {
					Resgates.cancelar(p, 0);
					Perturbacao.aliviar(p, 15);
					Planos.msg(p, ChatFormatting.GREEN, "Socorrista: — Seja bem-vindo. Descanse aqui; ninguém vai apressá-lo.");
				}
			}
		}
	}

	// ================================================================ Gancho da prece (área npc), tolerante

	@SuppressWarnings({"unchecked", "rawtypes"})
	@SubscribeEvent
	public static void servidorIniciado(ServerStartedEvent event) {
		if (ganchoPrece)
			return;
		ganchoPrece = true;
		try {
			Class<?> prece = Class.forName("net.mcreator.modespiritismominecraft.espiritismo.npc.Prece");
			Object campo = prece.getField("AO_ORAR").get(null);
			Consumer<Object> ouvinte = o -> {
				if (o instanceof ServerPlayer sp)
					Planos.aoOrar(sp);
			};
			if (campo instanceof Collection c) {
				c.add(ouvinte);
				ModEspiritismoMinecraftMod.LOGGER.info("Planos: registrado no gancho npc.Prece.AO_ORAR");
			} else
				ModEspiritismoMinecraftMod.LOGGER.info("Planos: npc.Prece.AO_ORAR tem tipo inesperado ({}); usando só /espiritismo_prece", campo == null ? "null" : campo.getClass());
		} catch (ReflectiveOperationException | RuntimeException e) {
			ModEspiritismoMinecraftMod.LOGGER.info("Planos: gancho npc.Prece.AO_ORAR ausente; usando só /espiritismo_prece");
		}
	}

	// ================================================================ Comandos

	@SubscribeEvent
	public static void comandos(RegisterCommandsEvent event) {
		String[] estados = Arrays.stream(EstadoEspiritual.values()).map(e -> e.name().toLowerCase()).toArray(String[]::new);
		event.getDispatcher().register(Commands.literal("espiritismo_plano").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("ir").then(Commands.argument("plano", StringArgumentType.word())
						.suggests((ctx, b) -> SharedSuggestionProvider.suggest(new String[]{"nosso_lar", "umbral", "terra"}, b)).executes(ctx -> {
							ServerPlayer p = ctx.getSource().getPlayerOrException();
							String nome = StringArgumentType.getString(ctx, "plano");
							ResourceKey<Level> chave = switch (nome) {
								case "nosso_lar" -> Planos.NOSSO_LAR;
								case "umbral" -> Planos.UMBRAL;
								case "terra" -> Level.OVERWORLD;
								default -> null;
							};
							if (chave == null || !Planos.ir(p, chave)) {
								ctx.getSource().sendFailure(Component.literal("Plano desconhecido ou não carregado: " + nome));
								return 0;
							}
							ctx.getSource().sendSuccess(() -> Component.literal("Levado a " + nome + "."), true);
							return 1;
						})))
				.then(Commands.literal("reencarnar").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					BlockPos pos = Planos.reencarnar(p);
					ctx.getSource().sendSuccess(() -> Component.literal("Reencarnou em " + pos.toShortString() + "."), true);
					return 1;
				}))
				.then(Commands.literal("resgatar").executes(ctx -> {
					Planos.resgatar(ctx.getSource().getPlayerOrException());
					return 1;
				}))
				.then(Commands.literal("estado").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					ctx.getSource().sendSuccess(() -> Component.literal("Estado: " + Estados.get(p) + " há " + Estados.tempoNoEstado(p) / 20 + " s · perturbação " + Perturbacao.get(p)
							+ " · arrependimento " + p.getData(RegistroDimensao.ARREPENDIMENTO) + " · resgate fase " + Resgates.fase(p) + " · pode socorrer: " + Estados.podeSocorrer(p)), false);
					return 1;
				}).then(Commands.argument("novo", StringArgumentType.word()).suggests((ctx, b) -> SharedSuggestionProvider.suggest(estados, b)).executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					String n = StringArgumentType.getString(ctx, "novo").toUpperCase();
					try {
						EstadoEspiritual e = EstadoEspiritual.valueOf(n);
						Estados.forcar(p, e, "comando de administrador");
						ctx.getSource().sendSuccess(() -> Component.literal("Estado definido: " + e), true);
						return 1;
					} catch (IllegalArgumentException ex) {
						ctx.getSource().sendFailure(Component.literal("Estado desconhecido: " + n));
						return 0;
					}
				})))
				.then(Commands.literal("perturbacao").then(Commands.argument("valor", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					Perturbacao.set(p, IntegerArgumentType.getInteger(ctx, "valor"));
					Perturbacao.atualizarEstado(p);
					ctx.getSource().sendSuccess(() -> Component.literal("Perturbação = " + Perturbacao.get(p) + " (estado " + Estados.get(p) + ")"), true);
					return 1;
				})))
				.then(Commands.literal("postos").executes(ctx -> {
					ServerLevel umbral = ctx.getSource().getServer().getLevel(Planos.UMBRAL);
					if (umbral == null) {
						ctx.getSource().sendFailure(Component.literal("Umbral não carregado."));
						return 0;
					}
					for (ConstrucoesUmbral.Planejado pl : ConstrucoesUmbral.planejar(umbral.getSeed())) {
						String st = ConstrucoesUmbral.concluido(umbral, pl.indice()) ? "concluído" : ConstrucoesUmbral.iniciado(umbral, pl.indice()) ? "em construção" : "aguardando aproximação";
						ctx.getSource().sendSuccess(() -> Component.literal("Posto " + pl.indice() + " (" + new String[]{"pequeno", "médio", "avançado"}[pl.tamanho()] + ") ~ " + pl.x() + ", "
								+ pl.z() + ": " + st), false);
					}
					return 1;
				}))
				.then(Commands.literal("limpar_temporarios").executes(ctx -> {
					int n = Resgates.limparOrfaos(ctx.getSource().getServer());
					ctx.getSource().sendSuccess(() -> Component.literal(n + " socorrista(s) temporário(s) removido(s)."), true);
					return 1;
				}))
				.then(Commands.literal("sonho").then(Commands.argument("tipo", StringArgumentType.word())
						.suggests((ctx, b) -> SharedSuggestionProvider.suggest(new String[]{"desdobramento", "pesadelo", "acordar"}, b)).executes(ctx -> {
							ServerPlayer p = ctx.getSource().getPlayerOrException();
							switch (StringArgumentType.getString(ctx, "tipo")) {
								case "pesadelo" -> Planos.iniciarSonho(p, 2);
								case "acordar" -> Planos.terminarSonho(p);
								default -> Planos.iniciarSonho(p, 1);
							}
							return 1;
						}))));

		// Prece de arrependimento (fallback caso a área npc não exponha o gancho). Aberto a todos.
		event.getDispatcher().register(Commands.literal("espiritismo_prece").executes(ctx -> {
			ServerPlayer p = ctx.getSource().getPlayerOrException();
			if (p.level().dimension() != Planos.UMBRAL) {
				Planos.msg(p, ChatFormatting.AQUA, "Você eleva o pensamento em prece. A prece sincera é sempre ouvida.");
				if (Perturbacao.get(p) > 0)
					Perturbacao.aliviar(p, Perturbacao.ALIVIO_PRECE);
				return 1;
			}
			Planos.aoOrar(p);
			return 1;
		}));

		// Socorro: pedir ajuda e responder à oferta (acessível pelo chat clicável). Aberto a todos.
		event.getDispatcher().register(Commands.literal("espiritismo_socorro").executes(ctx -> {
			Resgates.pedirAjuda(ctx.getSource().getPlayerOrException());
			return 1;
		}).then(Commands.argument("escolha", StringArgumentType.word())
				.suggests((ctx, b) -> SharedSuggestionProvider.suggest(new String[]{"pedir", "aceitar", "conversar", "tempo", "recusar"}, b)).executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					switch (StringArgumentType.getString(ctx, "escolha")) {
						case "aceitar" -> Resgates.escolher(p, Resgates.Escolha.ACEITAR);
						case "conversar" -> Resgates.escolher(p, Resgates.Escolha.CONVERSAR);
						case "tempo" -> Resgates.escolher(p, Resgates.Escolha.PEDIR_TEMPO);
						case "recusar" -> Resgates.escolher(p, Resgates.Escolha.RECUSAR);
						default -> Resgates.pedirAjuda(p);
					}
					return 1;
				})));
	}
}
