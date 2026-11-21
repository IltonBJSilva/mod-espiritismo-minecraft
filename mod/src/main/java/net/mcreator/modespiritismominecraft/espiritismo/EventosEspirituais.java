package net.mcreator.modespiritismominecraft.espiritismo;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModItems;
import net.mcreator.modespiritismominecraft.entity.HaroldoDutraEntity;
import net.mcreator.modespiritismominecraft.entity.ChicoXavierEntity;
import net.mcreator.modespiritismominecraft.entity.AllanKardecEntity;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * Liga a Barra de Moralidade ao jogo: ações boas e más, itens espirituais,
 * conversa com os NPCs e efeitos de moralidade alta/baixa.
 */
@EventBusSubscriber
public final class EventosEspirituais {
	/** Intervalo (em ticks) entre verificações de efeitos da moralidade. 200 ticks = 10 s. */
	private static final int INTERVALO_EFEITOS = 200;

	private EventosEspirituais() {
	}

	private static boolean pronto(Player player, String acao, int recargaTicks) {
		return Moralidade.pronto(player, acao, recargaTicks);
	}

	private static boolean ehBenfeitor(Entity entity) {
		return entity instanceof AllanKardecEntity || entity instanceof ChicoXavierEntity || entity instanceof HaroldoDutraEntity;
	}

	private static void falar(Player player, String npc, String fala) {
		player.sendSystemMessage(Component.literal("<" + npc + "> ").withStyle(ChatFormatting.GOLD).append(Component.literal(fala).withStyle(ChatFormatting.WHITE)));
	}

	private static void dar(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack))
			player.drop(stack, false);
	}

	// ---------------------------------------------------------------- Ações más

	@SubscribeEvent
	public static void aoMorrer(LivingDeathEvent event) {
		if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide())
			return;
		Entity vitima = event.getEntity();
		if (ehBenfeitor(vitima))
			Moralidade.alterar(player, -20, "Tirou a vida de um benfeitor");
		else if (vitima instanceof AbstractVillager)
			Moralidade.alterar(player, -10, "Tirou a vida de um aldeão");
		else if (vitima instanceof TamableAnimal pet && pet.isTame())
			Moralidade.alterar(player, -8, "Tirou a vida de um animal domesticado");
		else if (vitima instanceof IronGolem)
			Moralidade.alterar(player, -5, "Destruiu um protetor da vila");
	}

	@SubscribeEvent
	public static void aoReceberDano(LivingIncomingDamageEvent event) {
		Entity alvo = event.getEntity();
		if (alvo.level().isClientSide())
			return;
		// Os benfeitores não podem ser feridos (só /kill e o vazio). Agredi-los ainda pesa na moralidade.
		if (ehBenfeitor(alvo) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			event.setCanceled(true);
			if (event.getSource().getEntity() instanceof Player player && pronto(player, "agredir", 40)) {
				Moralidade.alterar(player, -5, "Agrediu um benfeitor");
				falar(player, nomeBenfeitor(alvo), "Que a paz esteja com você, meu irmão.");
			}
			return;
		}
		// Doutrinador — Aura de Paz: dano de criaturas hostis próximas reduzido em 20%.
		if (alvo instanceof Player player && Moralidade.classe(player) == ClasseEspiritual.DOUTRINADOR && event.getSource().getEntity() instanceof Monster m && m.distanceToSqr(player) <= 16)
			event.setAmount(event.getAmount() * 0.8F);
	}

	/** Evangelização: criaturas pacificadas não voltam a mirar em jogadores até o efeito acabar. */
	@SubscribeEvent
	public static void aoMudarAlvo(LivingChangeTargetEvent event) {
		if (event.getNewAboutToBeSetTarget() instanceof Player && Habilidades.estaPacificado(event.getEntity()))
			event.setCanceled(true);
	}

	private static String nomeBenfeitor(Entity e) {
		return e instanceof AllanKardecEntity ? "Allan Kardec" : e instanceof ChicoXavierEntity ? "Chico Xavier" : "Haroldo Dutra";
	}

	// ---------------------------------------------------------------- Ações boas

	@SubscribeEvent
	public static void aoNegociar(TradeWithVillagerEvent event) {
		if (pronto(event.getEntity(), "negociar", 1200))
			Moralidade.ganhoRotina(event.getEntity(), 1, "Negociou honestamente");
	}

	@SubscribeEvent
	public static void aoProcriar(BabyEntitySpawnEvent event) {
		Player player = event.getCausedByPlayer();
		if (player != null && pronto(player, "procriar", 600))
			Moralidade.ganhoRotina(player, 1, "Cuidou dos animais");
	}

	@SubscribeEvent
	public static void aoDomesticar(AnimalTameEvent event) {
		if (pronto(event.getTamer(), "domesticar", 6000))
			Moralidade.ganhoRotina(event.getTamer(), 3, "Acolheu um animal");
	}

	@SubscribeEvent
	public static void aoPlantar(BlockEvent.EntityPlaceEvent event) {
		if (event.getEntity() instanceof Player player && event.getPlacedBlock().is(BlockTags.SAPLINGS) && pronto(player, "plantar", 1200))
			Moralidade.ganhoRotina(player, 1, "Plantou uma árvore");
	}

	// ---------------------------------------------------------------- Itens espirituais

	@SubscribeEvent
	public static void aoUsarItem(PlayerInteractEvent.RightClickItem event) {
		Player player = event.getEntity();
		ItemStack stack = event.getItemStack();
		Level level = player.level();

		if (stack.is(ModEspiritismoMinecraftModItems.AGUA_FLUIDIFICADA.get())) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
			if (level.isClientSide())
				return;
			// Cura estados negativos (doc: Itens > Água Fluidificada)
			List<MobEffectInstance> nocivos = new ArrayList<>();
			for (MobEffectInstance efeito : player.getActiveEffects())
				if (efeito.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
					nocivos.add(efeito);
			nocivos.forEach(efeito -> player.removeEffect(efeito.getEffect()));

			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.sendSystemMessage(Component.literal(nocivos.isEmpty() ? "Uma sensação de paz percorre seu corpo." : "Os fluidos benéficos dissipam as influências negativas.")
					.withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
			Moralidade.alterarEnergia(player, 25);
			if (pronto(player, "agua", 2400))
				Moralidade.ganhoRotina(player, 2, "Recebeu fluidos de paz");
			if (!player.isCreative()) {
				stack.shrink(1);
				dar(player, new ItemStack(Items.GLASS_BOTTLE));
			}
		} else if (stack.is(ModEspiritismoMinecraftModItems.LIVRODOS_ESPIRITOS.get())) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
			if (level.isClientSide())
				return;
			Ensinamentos.Questao q = Ensinamentos.aleatorio(Ensinamentos.LIVRO_DOS_ESPIRITOS, player.getRandom());
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.sendSystemMessage(Component.literal("— O Livro dos Espíritos, questão " + q.numero() + " —").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
			player.sendSystemMessage(Component.literal(q.pergunta()).withStyle(ChatFormatting.YELLOW));
			player.sendSystemMessage(Component.literal("“" + q.resposta() + "”").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
			if (pronto(player, "estudar", 6000))
				Moralidade.ganhoRotina(player, 1, "Estudou a Doutrina");
			Jornada.conquista(player, "primeira_leitura");
			if (Jornada.avancar(player, Jornada.LER_LIVRO))
				falar(player, KARDEC, "Muito bem. Agora procure Chico Xavier, aqui no Centro. Ele tem algo para você.");
		}
	}

	// ---------------------------------------------------------------- NPCs

	@SubscribeEvent
	public static void aoConversar(PlayerInteractEvent.EntityInteract event) {
		Entity alvo = event.getTarget();
		if (alvo instanceof Villager aldeao) {
			aoInteragirAldeao(event, aldeao);
			return;
		}
		if (!ehBenfeitor(alvo) || event.getHand() != InteractionHand.MAIN_HAND)
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
		Player player = event.getEntity();
		if (player.level().isClientSide() || !pronto(player, "conversar", 20))
			return;
		if (alvo instanceof AllanKardecEntity)
			conversarKardec(player);
		else if (alvo instanceof ChicoXavierEntity)
			conversarChico(player);
		else if (alvo instanceof HaroldoDutraEntity)
			conversarHaroldo(player);
	}

	private static final String KARDEC = "Allan Kardec", CHICO = "Chico Xavier", HAROLDO = "Haroldo Dutra";

	private static void conversarKardec(Player player) {
		int etapa = Jornada.etapa(player);
		Jornada.conquista(player, "raiz");
		if (etapa == Jornada.FALAR_KARDEC) {
			falar(player, KARDEC, "Seja bem-vindo ao Centro, amigo. Tome este livro: nele estão as respostas dos Espíritos às grandes questões da vida.");
			falar(player, KARDEC, "Clique com o botão direito para lê-lo.");
			if (!player.getData(Moralidade.RECEBEU_LIVRO)) {
				player.setData(Moralidade.RECEBEU_LIVRO, true);
				dar(player, new ItemStack(ModEspiritismoMinecraftModItems.LIVRODOS_ESPIRITOS.get()));
			}
			Jornada.avancar(player, Jornada.FALAR_KARDEC);
		} else if (etapa == Jornada.LER_LIVRO) {
			if (!player.getInventory().contains(new ItemStack(ModEspiritismoMinecraftModItems.LIVRODOS_ESPIRITOS.get()))) {
				falar(player, KARDEC, "Perdeu o livro? Tome outro. O estudo é o primeiro passo.");
				dar(player, new ItemStack(ModEspiritismoMinecraftModItems.LIVRODOS_ESPIRITOS.get()));
			} else {
				falar(player, KARDEC, "Leia O Livro dos Espíritos: clique com o botão direito segurando o livro.");
			}
		} else if (etapa == Jornada.FALAR_CHICO) {
			falar(player, KARDEC, "Chico Xavier está aqui no Centro. Converse com ele.");
		} else if (etapa == Jornada.ESCOLHER_CLASSE && Moralidade.classe(player) != ClasseEspiritual.NENHUMA) {
			Jornada.avancar(player, Jornada.ESCOLHER_CLASSE);
		} else if (etapa == Jornada.CARIDADE) {
			falar(player, KARDEC, "Fora da caridade não há salvação. Há um irmão com fome por perto: leve-lhe algo para comer.");
			Jornada.garantirAldeaoFaminto(player, false);
		} else if (Moralidade.classe(player) == ClasseEspiritual.NENHUMA && player instanceof ServerPlayer sp) {
			falar(player, KARDEC, "Você estudou, recebeu e deu. Cada Espírito serve ao bem de um modo. Qual caminho deseja seguir?");
			PacketDistributor.sendToPlayer(sp, new Pacotes.AbrirEscolhaClasse());
		} else {
			falar(player, KARDEC, Ensinamentos.aleatorio(Ensinamentos.KARDEC, player.getRandom()));
		}
	}

	private static void conversarChico(Player player) {
		int etapa = Jornada.etapa(player);
		if (etapa == Jornada.FALAR_CHICO) {
			falar(player, CHICO, "Que bom que veio, meu filho. Os amigos espirituais pediram que eu lhe entregasse esta água fluidificada.");
			falar(player, CHICO, "Beba quando sentir o peso das influências negativas. E lembre-se: quem recebe também é chamado a dar.");
			dar(player, new ItemStack(ModEspiritismoMinecraftModItems.AGUA_FLUIDIFICADA.get()));
			pronto(player, "presente_chico", 12000);
			Jornada.conquista(player, "agua");
			Jornada.avancar(player, Jornada.FALAR_CHICO);
			return;
		}
		if (etapa < Jornada.FALAR_CHICO) {
			falar(player, CHICO, "Seja bem-vindo. Converse primeiro com o nosso irmão Allan Kardec, e estude o livro que ele lhe der.");
			return;
		}
		if (etapa == Jornada.CARIDADE) {
			falar(player, CHICO, "Há alguém com fome por perto. Pão, maçã, cenoura, batata... qualquer alimento serve, quando é dado com amor.");
			return;
		}
		int moral = Moralidade.get(player);
		if (moral >= 50 && pronto(player, "presente_chico", 12000)) {
			falar(player, CHICO, "Os amigos espirituais pediram que eu lhe entregasse mais esta água fluidificada.");
			dar(player, new ItemStack(ModEspiritismoMinecraftModItems.AGUA_FLUIDIFICADA.get()));
			Jornada.conquista(player, "agua");
		} else if (moral < 50) {
			falar(player, CHICO, "Sinto que seu coração anda pesado. Faça o bem a alguém hoje e volte para conversarmos.");
		} else {
			falar(player, CHICO, Ensinamentos.aleatorio(Ensinamentos.CHICO, player.getRandom()));
		}
	}

	private static void conversarHaroldo(Player player) {
		if (Jornada.emPalestra(player)) {
			player.sendOverlayMessage(Component.literal("Haroldo Dutra está no meio da palestra...").withStyle(ChatFormatting.GRAY));
			return;
		}
		int moral = Moralidade.get(player);
		falar(player, HAROLDO, "Sua moralidade está " + Moralidade.descricao(moral).toLowerCase() + " (" + moral + "/" + Moralidade.MAX + ").");
		if (pronto(player, "palestra", 6000)) {
			falar(player, HAROLDO, "Sente-se, vamos estudar juntos.");
			Jornada.iniciarPalestra(player, Ensinamentos.aleatorio(Ensinamentos.PALESTRAS, player.getRandom()));
		} else {
			falar(player, HAROLDO, Ensinamentos.aleatorio(Ensinamentos.HAROLDO, player.getRandom()));
		}
	}

	// ---------------------------------------------------------------- Caridade

	private static void aoInteragirAldeao(PlayerInteractEvent.EntityInteract event, Villager aldeao) {
		Player player = event.getEntity();
		ItemStack stack = event.getItemStack();
		if (event.getHand() != InteractionHand.MAIN_HAND || !stack.has(DataComponents.FOOD))
			return;
		// Com comida na mão: o aldeão faminto sempre aceita; os demais só se o jogador estiver agachado (para não atrapalhar o comércio).
		if (!Jornada.famintoPara(aldeao, player) && !player.isShiftKeyDown())
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
		if (player.level().isClientSide())
			return;
		alimentar(player, aldeao, stack);
	}

	/** Dar comida a um aldeão. Público para os testes. */
	public static void alimentar(Player player, Villager aldeao, ItemStack comida) {
		// A recompensa grande é só de quem recebeu a prova (evita "farm" de aldeões famintos de outros jogadores).
		boolean faminto = Jornada.famintoPara(aldeao, player);
		if (!player.isCreative())
			comida.shrink(1);
		if (player.level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.HEART, aldeao.getX(), aldeao.getY() + 2, aldeao.getZ(), faminto ? 6 : 2, 0.3, 0.2, 0.3, 0);
			sl.playSound(null, aldeao.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
		if (faminto) {
			Jornada.saciar(aldeao);
			if (aldeao.hasCustomName() && "Irmão faminto".equals(aldeao.getCustomName().getString()))
				aldeao.setCustomName(Component.literal("Irmão amparado"));
			aldeao.heal(4);
			Moralidade.alterar(player, 4, "Saciou a fome de um irmão");
			player.sendSystemMessage(Component.literal("O aldeão sorri, agradecido. “Que Deus lhe pague, irmão.”").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
			Jornada.conquista(player, "caridade");
			if (Jornada.avancar(player, Jornada.CARIDADE))
				player.sendSystemMessage(Component.literal("Você sente que Allan Kardec gostaria de falar com você.").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
		} else {
			if (Jornada.faminto(aldeao)) // matou a fome de alguém que era prova de outro jogador: o aldeão agradece, mas só vale como rotina
				Jornada.saciar(aldeao);
			if (pronto(player, "partilhar", 1200))
				Moralidade.ganhoRotina(player, 1, "Partilhou o pão");
		}
	}

	/** Aldeões famintos soltam fumaça acima da cabeça; de vez em quando surge uma nova prova de caridade. */
	private static void tickCaridade(Player player) {
		if (!(player.level() instanceof ServerLevel sl))
			return;
		if (player.tickCount % 40 == 0)
			for (Villager v : sl.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(24), Jornada::faminto))
				sl.sendParticles(ParticleTypes.SMOKE, v.getX(), v.getY() + 2.3, v.getZ(), 3, 0.15, 0.1, 0.15, 0.01);
		if (player.tickCount % 200 != 0)
			return;
		int etapa = Jornada.etapa(player);
		if (etapa == Jornada.CARIDADE) {
			// Garante um aldeão faminto por perto; se não houver nenhum aldeão, um "Irmão faminto" chega (no máximo 1 a cada 5 min).
			Jornada.garantirAldeaoFaminto(player, true);
		} else if (etapa == Jornada.CONCLUIDA && player.getRandom().nextInt(30) == 0 && pronto(player, "prova_caridade", 12000)) {
			// Provas morais aleatórias: em média uma a cada ~10 min de jogo, perto de aldeias.
			Jornada.garantirAldeaoFaminto(player, false);
		}
	}

	// ---------------------------------------------------------------- Efeitos da moralidade

	@SubscribeEvent
	public static void aoTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide() || player.isSpectator())
			return;
		if (player.tickCount % 20 == 0)
			Habilidades.passivas(player);
		Jornada.tickPalestra(player, HAROLDO);
		if (player.tickCount % 100 == 0 && player.level() instanceof ServerLevel sl && sl == sl.getServer().overworld()) {
			BlockPos porta = Jornada.centro(sl);
			if (porta != null && porta.distSqr(player.blockPosition()) < 64 * 64)
				CentroEspirita.verificarBenfeitores(sl, porta);
		}
		tickCaridade(player);
		if (player.tickCount % 40 == 0) {
			// Energia espiritual: recupera mais rápido com moralidade alta, mais devagar com baixa.
			int moralAtual = Moralidade.get(player);
			Moralidade.alterarEnergia(player, moralAtual >= Moralidade.ALTA ? 3 : moralAtual <= Moralidade.BAIXA ? 1 : 2);
		}
		if (player.tickCount % INTERVALO_EFEITOS != 0)
			return;
		int moral = Moralidade.get(player);
		if (moral >= Moralidade.ALTA) {
			// Alta moralidade: assistência dos bons Espíritos (regeneração leve)
			if (player.getHealth() < player.getMaxHealth())
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
		} else if (moral <= Moralidade.BAIXA && player.getRandom().nextFloat() < (Moralidade.classe(player) == ClasseEspiritual.TRABALHADOR ? 0.07F : 0.15F)) {
			// Trabalhador Espiritual: "Resiliência" — metade da chance de obsessão.
			// Baixa moralidade: influências obsessivas (doc: Obsessões Espirituais)
			switch (player.getRandom().nextInt(3)) {
				case 0 -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 0));
				case 1 -> player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 0));
				default -> player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
			}
			player.sendOverlayMessage(Component.literal("Você ouve sussurros perturbadores...").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
		}
	}

	// ---------------------------------------------------------------- Entrada no mundo

	@SubscribeEvent
	public static void aoEntrar(PlayerEvent.PlayerLoggedInEvent event) {
		Player player = event.getEntity();
		if (!(player.level() instanceof ServerLevel) || player.level().getServer() == null)
			return;
		Jornada.migrar(player);
		ServerLevel overworld = player.level().getServer().overworld();
		BlockPos porta = CentroEspirita.garantir(overworld);
		int etapa = Jornada.etapa(player);
		if (etapa < Jornada.CONCLUIDA) {
			player.sendSystemMessage(Component.literal("✦ Jornada Espiritual — objetivo: ").withStyle(ChatFormatting.GOLD).append(Component.literal(Jornada.objetivo(etapa)).withStyle(ChatFormatting.YELLOW)));
			if (etapa == Jornada.FALAR_KARDEC && player.level() == overworld)
				player.sendSystemMessage(Component.literal("O Centro Espírita fica em X " + porta.getX() + ", Z " + porta.getZ() + Jornada.direcao(player, porta.getCenter()) + ". Use /centro para ver de novo.")
						.withStyle(ChatFormatting.GRAY));
		}
	}

	@SubscribeEvent
	public static void aoSair(PlayerEvent.PlayerLoggedOutEvent event) {
		Jornada.encerrarPalestra(event.getEntity());
	}

	// ---------------------------------------------------------------- Comando /moralidade

	@SubscribeEvent
	public static void aoRegistrarComandos(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("moralidade").executes(ctx -> {
			Player player = ctx.getSource().getPlayerOrException();
			int moral = Moralidade.get(player);
			ctx.getSource().sendSuccess(() -> Component.literal("Moralidade: " + moral + "/" + Moralidade.MAX + " (" + Moralidade.descricao(moral) + ")"), false);
			return moral;
		}).then(Commands.literal("definir").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("valor", IntegerArgumentType.integer(Moralidade.MIN, Moralidade.MAX)).executes(ctx -> {
					Player player = ctx.getSource().getPlayerOrException();
					int valor = IntegerArgumentType.getInteger(ctx, "valor");
					Moralidade.set(player, valor);
					ctx.getSource().sendSuccess(() -> Component.literal("Moralidade definida para " + valor), true);
					return valor;
				}))));

		event.getDispatcher().register(Commands.literal("centro").executes(ctx -> {
			Player player = ctx.getSource().getPlayerOrException();
			ServerLevel overworld = ctx.getSource().getServer().overworld();
			BlockPos porta = CentroEspirita.garantir(overworld);
			String dir = player.level() == overworld ? Jornada.direcao(player, porta.getCenter()) : " (no mundo normal)";
			ctx.getSource().sendSuccess(() -> Component.literal("Centro Espírita: X " + porta.getX() + ", Y " + porta.getY() + ", Z " + porta.getZ() + dir).withStyle(ChatFormatting.AQUA), false);
			return 1;
		}));

		event.getDispatcher().register(Commands.literal("espiritismo").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("construir_centro").executes(ctx -> {
					Player player = ctx.getSource().getPlayerOrException();
					ServerLevel level = (ServerLevel) player.level();
					BlockPos origem = player.blockPosition().relative(player.getDirection(), 3).below();
					BlockPos porta = CentroEspirita.construir(level, origem, true);
					if (level == ctx.getSource().getServer().overworld())
						level.setData(Moralidade.CENTRO, porta.asLong());
					ctx.getSource().sendSuccess(() -> Component.literal("Centro Espírita construído (porta em " + porta.toShortString() + ")."), true);
					return 1;
				}))
				.then(Commands.literal("jornada").then(Commands.argument("etapa", IntegerArgumentType.integer(Jornada.FALAR_KARDEC, Jornada.CONCLUIDA)).executes(ctx -> {
					Player player = ctx.getSource().getPlayerOrException();
					int etapa = IntegerArgumentType.getInteger(ctx, "etapa");
					Jornada.definir(player, etapa);
					if (etapa == Jornada.CARIDADE)
						Jornada.garantirAldeaoFaminto(player, true);
					ctx.getSource().sendSuccess(() -> Component.literal("Jornada na etapa " + etapa + ": " + Jornada.objetivo(etapa)), false);
					return etapa;
				})))
				.then(Commands.literal("resetar_classe").executes(ctx -> {
					Player player = ctx.getSource().getPlayerOrException();
					Moralidade.definirClasse(player, ClasseEspiritual.NENHUMA);
					ctx.getSource().sendSuccess(() -> Component.literal("Classe removida. Fale com Allan Kardec para escolher de novo."), false);
					return 1;
				})).then(Commands.literal("energia_cheia").executes(ctx -> {
					Player player = ctx.getSource().getPlayerOrException();
					Moralidade.alterarEnergia(player, Moralidade.ENERGIA_MAX);
					player.setData(Moralidade.RECARGAS, new java.util.HashMap<>());
					ctx.getSource().sendSuccess(() -> Component.literal("Energia restaurada e recarga zerada."), false);
					return 1;
				})));
	}
}
