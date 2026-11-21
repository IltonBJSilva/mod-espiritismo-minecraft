package net.mcreator.modespiritismominecraft.espiritismo.dimensao;

import com.mojang.brigadier.arguments.StringArgumentType;

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

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.function.Consumer;
import java.util.UUID;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collection;

/** Eventos da área "dimensao": morte e renascimento, Umbral, reencarnação, sono e comandos. */
@EventBusSubscriber
public final class EventosDimensao {
	/** Destino decidido no evento de posição de renascimento, para as mensagens depois do renascimento. */
	private static final Map<UUID, Planos.Destino> RENASCENDO = new HashMap<>();
	/** Jogadores que acabaram de acordar depois de uma noite inteira (processados no próximo tick do servidor). */
	private static final Set<UUID> ACORDARAM = new HashSet<>();
	/** Confirmação da reencarnação no atril do Ministério do Auxílio: jogador → tick até quando vale. */
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
		BlockPos chegada = Planos.garantir(nivel);
		event.setTeleportTransition(new TeleportTransition(nivel, Vec3.atBottomCenterOf(chegada), Vec3.ZERO, 180F, 0F, TeleportTransition.DO_NOTHING));
	}

	@SubscribeEvent
	public static void renasceu(PlayerEvent.PlayerRespawnEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer p))
			return;
		Planos.Destino d = RENASCENDO.remove(p.getUUID());
		if (d == null)
			return;
		p.setData(RegistroDimensao.RESGATE_EM, 0L);
		p.setData(RegistroDimensao.TEMPO_UMBRAL, 0);
		switch (d) {
			case NOSSO_LAR -> {
				p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0, false, false));
				Planos.msg(p, ChatFormatting.AQUA, "Você desperta nas Câmaras de Retificação, em Nosso Lar. Mãos amigas cuidaram de você.");
				Planos.msg(p, ChatFormatting.GRAY,
						"A perturbação que segue a morte do corpo é passageira, e tanto mais breve quanto mais o Espírito se desprendeu da matéria pelo bem praticado (LE, q. 163–165).");
				nadaMaterial(p);
				Planos.msg(p, ChatFormatting.GRAY, "Explore a colônia. Para renascer na Terra, procure o Ministério do Auxílio, a oeste da praça.");
			}
			case UMBRAL -> {
				p.setData(RegistroDimensao.ARREPENDIMENTO, 0);
				p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 0, false, false));
				Planos.msg(p, ChatFormatting.DARK_PURPLE, "Você desperta numa região escura e enevoada: o Umbral.");
				Planos.msg(p, ChatFormatting.GRAY,
						"Não é um castigo eterno: é o reflexo do próprio estado íntimo, como André Luiz descreve em Nosso Lar (caps. 1–2). Ninguém está abandonado.");
				nadaMaterial(p);
				Planos.msg(p, ChatFormatting.GOLD,
						"O arrependimento sincero abre o caminho: ore (/espiritismo_prece), console os Espíritos sofredores e reflita. Ao norte brilha a luz de um Posto de Socorro.");
			}
			case PERTURBACAO -> {
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
		if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0)
			return;
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
		if ((dim == Planos.UMBRAL || dim == Planos.NOSSO_LAR) && p.tickCount % 200 == 0) {
			ServerLevel level = (ServerLevel) p.level();
			Map<String, Long> pontos = Planos.pontos(level);
			if (!pontos.isEmpty())
				ConstrucoesPlanos.verificarNpcs(level, pontos, dim == Planos.NOSSO_LAR);
		}
	}

	@SubscribeEvent
	public static void aoEntrar(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer p && p.getData(RegistroDimensao.SONHO_FIM) > 0)
			Planos.terminarSonho(p); // saiu do jogo no meio de um sonho
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
		if (ACORDARAM.isEmpty())
			return;
		for (UUID id : ACORDARAM) {
			ServerPlayer p = event.getServer().getPlayerList().getPlayer(id);
			if (p == null || !p.isAlive() || p.isSleeping() || p.isPassenger() || p.level().dimension() != Level.OVERWORLD)
				continue;
			int tipo = Planos.tipoSonho(Moralidade.get(p), p.getRandom().nextFloat());
			if (tipo != 0)
				Planos.iniciarSonho(p, tipo);
		}
		ACORDARAM.clear();
	}

	// ================================================================ Interações

	@SubscribeEvent
	public static void cliqueBloco(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p) || p.level().dimension() != Planos.NOSSO_LAR)
			return;
		ServerLevel level = (ServerLevel) p.level();
		BlockPos pos = event.getPos();
		if (!level.getBlockState(pos).is(Blocks.LECTERN))
			return;
		if (pos.equals(Planos.ponto(level, "auxilio"))) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
			if (p.getData(RegistroDimensao.SONHO_FIM) > 0) {
				Planos.msg(p, ChatFormatting.GRAY, "Em sonho não se planeja uma nova existência. Aguarde o despertar.");
				return;
			}
			long agora = level.getGameTime();
			Long ate = CONFIRMAR.get(p.getUUID());
			if (ate != null && agora <= ate) {
				CONFIRMAR.remove(p.getUUID());
				Planos.reencarnar(p);
			} else {
				CONFIRMAR.put(p.getUUID(), agora + 200);
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

	@SubscribeEvent
	public static void cliqueEntidade(PlayerInteractEvent.EntityInteract event) {
		if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p) || p.level().dimension() != Planos.UMBRAL)
			return;
		EntityType<?> sofredor = ConstrucoesPlanos.tipo("espirito_sofredor");
		if (sofredor == null || event.getTarget().getType() != sofredor)
			return;
		if (Moralidade.pronto(p, "dim_sofredor_" + event.getTarget().getUUID(), 20 * 60 * 5))
			Planos.ajudouSofredor(p);
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
				return 1;
			}
			Planos.aoOrar(p);
			return 1;
		}));
	}
}
