package net.mcreator.modespiritismominecraft.espiritismo.dimensao.teste;

import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.estado.Estados;
import net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Resgates;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.RegistroDimensao;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Perturbacao;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.EventosDimensao;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.ConstrucoesUmbral;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.BuscaLocalSeguro;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.espiritismo.ClasseEspiritual;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import static net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual.*;

/**
 * GameTests da área "umbral": máquina de estados, política sem violência, "paciente não ajuda paciente",
 * resgate com consentimento (aceitar/recusar/reaceitar, reconexão), reencarnação incremental e idempotente,
 * Postos de Socorro (planejamento e construção incremental retomável), perturbação/médium e saída de Nosso Lar.
 * O GameTestServer não carrega as dimensões de datapack: a lógica é testada por estado e funções puras, e as
 * construções no Overworld de teste.
 */
@EventBusSubscriber
public final class TestesUmbral {
	private static final Identifier ESTRUTURA_VAZIA = Identifier.withDefaultNamespace("empty");

	private record Teste(int maxTicks, Consumer<GameTestHelper> funcao) {
	}

	private static final Map<String, Teste> TESTES = new LinkedHashMap<>();

	static {
		TESTES.put("umb_estados_transicoes", new Teste(20, TestesUmbral::estados));
		TESTES.put("umb_estados_migracao_reconexao", new Teste(40, TestesUmbral::migracao));
		TESTES.put("umb_sem_violencia", new Teste(40, TestesUmbral::semViolencia));
		TESTES.put("umb_paciente_nao_ajuda_paciente", new Teste(20, TestesUmbral::podeSocorrer));
		TESTES.put("umb_nada_material_se_leva", new Teste(20, TestesUmbral::nadaMaterial));
		TESTES.put("umb_resgate_consentimento", new Teste(60, TestesUmbral::consentimento));
		TESTES.put("umb_resgate_reconexao", new Teste(40, TestesUmbral::resgateReconexao));
		TESTES.put("umb_perturbacao_e_medium", new Teste(20, TestesUmbral::perturbacao));
		TESTES.put("umb_saida_entrada_nosso_lar", new Teste(20, TestesUmbral::saidaEntrada));
		TESTES.put("umb_postos_planejamento", new Teste(20, TestesUmbral::postosPlanejamento));
		TESTES.put("umb_posto_incremental_retomavel", new Teste(400, TestesUmbral::postoIncremental));
		TESTES.put("umb_reencarnacao_incremental_idempotente", new Teste(900, TestesUmbral::reencarnacao));
	}

	private TestesUmbral() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, path);
	}

	@SubscribeEvent
	public static void registrarFuncoes(RegisterEvent event) {
		if (!GameTestHooks.isGametestEnabled())
			return;
		event.register(Registries.TEST_FUNCTION, helper -> TESTES.forEach((nome, t) -> helper.register(id(nome), t.funcao())));
	}

	@SubscribeEvent
	public static void registrarTestes(RegisterGameTestsEvent event) {
		Holder<TestEnvironmentDefinition<?>> ambiente = event.registerEnvironment(id("umb_padrao"));
		TESTES.forEach((nome, t) -> event.registerTest(id(nome), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(nome)),
				new TestData<>(ambiente, ESTRUTURA_VAZIA, t.maxTicks(), 0, true))));
	}

	@SuppressWarnings("deprecation")
	private static ServerPlayer jogador(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		BlockPos b = helper.absolutePos(new BlockPos(1, 1, 1));
		p.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0F, 0F);
		return p;
	}

	private static void remover(GameTestHelper helper, ServerPlayer p) {
		Resgates.removerSocorristas(p);
		helper.getLevel().getServer().getPlayerList().remove(p);
	}

	// ---------------------------------------------------------------- Máquina de estados

	private static void estados(GameTestHelper helper) {
		helper.assertTrue(Estados.valida(ENCARNADO_ESTAVEL, DESENCARNANDO), "encarnado → desencarnando");
		helper.assertTrue(Estados.valida(DESENCARNANDO, RECEM_DESENCARNADO), "desencarnando → recém");
		helper.assertTrue(Estados.valida(RECEM_DESENCARNADO, EM_ACOLHIMENTO), "recém → acolhimento");
		helper.assertTrue(Estados.valida(EM_ACOLHIMENTO, EM_NOSSO_LAR), "acolhimento → Nosso Lar");
		helper.assertTrue(Estados.valida(NO_UMBRAL_PERTURBADO, EM_RESGATE), "perturbado → resgate");
		helper.assertTrue(Estados.valida(EM_RESGATE, EM_ACOLHIMENTO), "resgate → acolhimento");
		helper.assertTrue(Estados.valida(PRONTO_PARA_REENCARNAR, REENCARNANDO) && Estados.valida(REENCARNANDO, ENCARNADO_ESTAVEL), "reencarnação");
		helper.assertTrue(Estados.valida(ENCARNADO_ESTAVEL, EM_VISITA_ASSISTIDA) && Estados.valida(EM_VISITA_ASSISTIDA, ENCARNADO_ESTAVEL), "sono");
		helper.assertFalse(Estados.valida(ENCARNADO_ESTAVEL, EM_NOSSO_LAR), "vivo não entra em Nosso Lar direto");
		helper.assertFalse(Estados.valida(ENCARNADO_ESTAVEL, REENCARNANDO), "vivo não reencarna");
		helper.assertFalse(Estados.valida(NO_UMBRAL_PERTURBADO, EM_NOSSO_LAR), "perturbado não vai a Nosso Lar sem acolhimento");
		helper.assertFalse(Estados.valida(REENCARNANDO, EM_NOSSO_LAR), "reencarnando só termina encarnado");
		for (EstadoEspiritual e : EstadoEspiritual.values())
			helper.assertFalse(Estados.proximos(e).isEmpty(), "estado sem saída (softlock): " + e);

		ServerPlayer p = jogador(helper);
		try {
			Estados.forcar(p, ENCARNADO_ESTAVEL, "teste");
			helper.assertFalse(Estados.transicionar(p, EM_NOSSO_LAR, "inválida"), "transição inválida aceita");
			helper.assertValueEqual(Estados.get(p), ENCARNADO_ESTAVEL, "estado mudou numa transição inválida");
			helper.assertTrue(Estados.transicionar(p, DESENCARNANDO, "t") && Estados.transicionar(p, RECEM_DESENCARNADO, "t") && Estados.transicionar(p, NO_UMBRAL_PERTURBADO, "t"),
					"cadeia da morte ao Umbral");
			helper.assertTrue(Estados.semViolencia(p) && Estados.perturbado(p), "perturbado sem violência");
			helper.assertTrue(Estados.transicionar(p, NO_UMBRAL_PERTURBADO, "mesmo"), "mesmo estado é aceito");
		} finally {
			remover(helper, p);
		}
		helper.assertValueEqual(Estados.deduzir(Planos.NOSSO_LAR, 10), EM_NOSSO_LAR, "deduzir NL");
		helper.assertValueEqual(Estados.deduzir(Planos.UMBRAL, 10), NO_UMBRAL_PERTURBADO, "deduzir Umbral baixa");
		helper.assertValueEqual(Estados.deduzir(Planos.UMBRAL, 70), NO_UMBRAL_ORIENTADO, "deduzir Umbral alta");
		helper.assertValueEqual(Estados.deduzir(Level.OVERWORLD, 10), ENCARNADO_PERTURBADO, "deduzir Terra baixa");
		helper.assertTrue(Estados.coerente(EM_RESGATE, Planos.UMBRAL) && !Estados.coerente(EM_NOSSO_LAR, Level.OVERWORLD), "coerência");
		helper.succeed();
	}

	private static void migracao(GameTestHelper helper) {
		ServerPlayer p = jogador(helper);
		try {
			// Save antigo: sem versão de estado; moralidade baixa na Terra.
			p.setData(RegistroDimensao.ESTADO_VERSAO, 0);
			p.setData(Moralidade.ESTADO, 0);
			Moralidade.set(p, 10);
			helper.assertTrue(Estados.migrar(p), "deveria migrar");
			helper.assertValueEqual(Estados.get(p), ENCARNADO_PERTURBADO, "estado deduzido pela dimensão");
			helper.assertFalse(Estados.migrar(p), "migração deve acontecer uma vez só");
			// Reconexão com estado incoerente (ex.: Nosso Lar estando na Terra) é corrigida, sem softlock.
			Estados.forcar(p, EM_NOSSO_LAR, "teste");
			Moralidade.set(p, 50);
			EventosDimensao.aoReconectar(p);
			helper.assertValueEqual(Estados.get(p), ENCARNADO_ESTAVEL, "reconexão corrige estado incoerente");
			// REENCARNANDO na Terra sem busca ativa também é destravado.
			Estados.forcar(p, REENCARNANDO, "teste");
			EventosDimensao.aoReconectar(p);
			helper.assertValueEqual(Estados.get(p), ENCARNADO_ESTAVEL, "reencarnação interrompida destravada");
		} finally {
			remover(helper, p);
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- Sem violência

	private static void semViolencia(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = jogador(helper);
		ServerPlayer outro = jogador(helper);
		try {
			Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 2));
			float vida = v.getHealth();
			Estados.forcar(p, NO_UMBRAL_PERTURBADO, "teste");
			// Ataque direto
			p.attack(v);
			helper.assertValueEqual(v.getHealth(), vida, "perturbado feriu NPC com ataque");
			// Dano por qualquer fonte cuja origem é o jogador (golpe, flecha)
			v.hurtServer(level, p.damageSources().playerAttack(p), 4F);
			Arrow flecha = new Arrow(EntityType.ARROW, level);
			flecha.setOwner(p);
			v.hurtServer(level, p.damageSources().arrow(flecha, p), 4F);
			helper.assertValueEqual(v.getHealth(), vida, "perturbado feriu NPC indiretamente");
			// Outro jogador
			float vidaOutro = outro.getHealth();
			outro.hurtServer(level, p.damageSources().playerAttack(p), 4F);
			helper.assertValueEqual(outro.getHealth(), vidaOutro, "perturbado feriu jogador");
			// Quebrar e colocar blocos
			BlockPos bloco = helper.absolutePos(new BlockPos(0, 1, 3));
			level.setBlockAndUpdate(bloco, Blocks.STONE.defaultBlockState());
			helper.assertFalse(p.gameMode.destroyBlock(bloco), "perturbado quebrou bloco");
			helper.assertTrue(level.getBlockState(bloco).is(Blocks.STONE), "bloco sumiu");
			var colocar = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, bloco.above()), Blocks.STONE.defaultBlockState(), p);
			NeoForge.EVENT_BUS.post(colocar);
			helper.assertTrue(colocar.isCanceled(), "perturbado colocou bloco");
			// Estabilizado: volta às ações normais previstas
			Estados.forcar(p, NO_UMBRAL_ORIENTADO, "teste");
			helper.assertFalse(Estados.semViolencia(p), "orientado não deveria ter bloqueio");
			v.hurtServer(level, p.damageSources().playerAttack(p), 2F);
			helper.assertTrue(v.getHealth() < vida, "orientado deveria poder agir normalmente");
			var colocar2 = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, bloco.above()), Blocks.STONE.defaultBlockState(), p);
			NeoForge.EVENT_BUS.post(colocar2);
			helper.assertFalse(colocar2.isCanceled(), "orientado deveria poder colocar fora dos postos");
			helper.assertTrue(p.gameMode.destroyBlock(bloco), "orientado deveria poder quebrar fora dos postos");
		} finally {
			remover(helper, p);
			remover(helper, outro);
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- Paciente não ajuda paciente

	private static void nadaMaterial(GameTestHelper helper) {
		helper.assertTrue(EventosDimensao.impedeLargar(Planos.NOSSO_LAR, false, EM_NOSSO_LAR), "não larga item em Nosso Lar");
		helper.assertTrue(EventosDimensao.impedeLargar(Planos.UMBRAL, false, NO_UMBRAL_ORIENTADO), "não larga item no Umbral");
		helper.assertTrue(EventosDimensao.impedeLargar(net.minecraft.world.level.Level.OVERWORLD, true, ENCARNADO_ESTAVEL), "não larga item em sonho");
		helper.assertTrue(EventosDimensao.impedeLargar(net.minecraft.world.level.Level.OVERWORLD, false, EM_VISITA_ASSISTIDA), "não larga item em visita assistida");
		helper.assertFalse(EventosDimensao.impedeLargar(net.minecraft.world.level.Level.OVERWORLD, false, ENCARNADO_ESTAVEL), "na Terra, acordado, larga normalmente");
		helper.succeed();
	}

	private static void podeSocorrer(GameTestHelper helper) {
		helper.assertFalse(Estados.podeSocorrer(NO_UMBRAL_PERTURBADO, 70, 0), "perturbado não socorre");
		helper.assertFalse(Estados.podeSocorrer(EM_TRATAMENTO, 70, 0), "em tratamento não socorre");
		helper.assertFalse(Estados.podeSocorrer(EM_ACOLHIMENTO, 70, 0), "em acolhimento não socorre");
		helper.assertFalse(Estados.podeSocorrer(EM_RESGATE, 70, 0), "em resgate não socorre");
		helper.assertFalse(Estados.podeSocorrer(EM_NOSSO_LAR, 10, 0), "moralidade perturbada não socorre");
		helper.assertFalse(Estados.podeSocorrer(NO_UMBRAL_ORIENTADO, 60, 60), "muito confuso não socorre");
		helper.assertTrue(Estados.podeSocorrer(NO_UMBRAL_ORIENTADO, 60, 10), "orientado e sereno socorre");
		helper.assertTrue(Estados.podeSocorrer(EM_NOSSO_LAR, 60, 0), "em Nosso Lar socorre");
		ServerPlayer p = jogador(helper);
		try {
			Estados.forcar(p, NO_UMBRAL_PERTURBADO, "teste");
			Moralidade.set(p, 60);
			int moral = Moralidade.get(p);
			helper.assertFalse(Planos.tentarConsolar(p), "paciente recebeu a tarefa de consolar");
			helper.assertValueEqual(Moralidade.get(p), moral, "não deveria haver recompensa");
			Estados.forcar(p, NO_UMBRAL_ORIENTADO, "teste");
			Perturbacao.set(p, 0);
			helper.assertTrue(Estados.podeSocorrer(p), "estável pode ajudar");
		} finally {
			remover(helper, p);
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- Resgate com consentimento

	private static void consentimento(GameTestHelper helper) {
		ServerPlayer p = jogador(helper);
		try {
			Estados.forcar(p, NO_UMBRAL_PERTURBADO, "teste");
			Moralidade.set(p, 50);
			Perturbacao.set(p, 70);
			BlockPos antes = p.blockPosition();
			ResourceKey<Level> dim = p.level().dimension();
			boolean temTipo = ConstrucoesUmbral.tipo("socorrista") != null;

			helper.assertTrue(Resgates.oferecer(p), "oferta deveria começar");
			helper.assertFalse(Resgates.oferecer(p), "dois resgates simultâneos");
			helper.assertValueEqual(Resgates.fase(p), Resgates.APROXIMANDO, "fase aproximando");
			if (temTipo)
				helper.assertTrue(Resgates.socorristasVivos(p) >= 1, "socorristas deveriam surgir");
			Resgates.chegou(p);
			helper.assertValueEqual(Resgates.fase(p), Resgates.OFERTA, "fase oferta");
			helper.assertTrue(p.blockPosition().equals(antes) && p.level().dimension() == dim, "teleportou sem consentimento");

			// Recusar: respeitado, sem punição de moralidade, socorristas removidos, volta só depois da recarga
			int moral = Moralidade.get(p);
			Resgates.escolher(p, Resgates.Escolha.RECUSAR);
			helper.assertValueEqual(Resgates.fase(p), Resgates.NENHUM, "recusa encerra a oferta");
			helper.assertValueEqual(Estados.get(p), NO_UMBRAL_PERTURBADO, "recusa não muda o estado");
			helper.assertValueEqual(Moralidade.get(p), moral, "recusar não reduz moralidade");
			helper.assertValueEqual(Resgates.socorristasVivos(p), 0, "socorristas temporários não removidos");
			helper.assertTrue(Moralidade.restante(p, Resgates.CHAVE_VOLTA) > 0, "recarga após recusa");
			helper.assertTrue(p.blockPosition().equals(antes), "recusa não pode teleportar");
			Resgates.avaliar(p);
			helper.assertValueEqual(Resgates.fase(p), Resgates.NENHUM, "socorristas voltaram antes da recarga (loop)");

			// Mudou de ideia: pede ajuda (ignora a recarga), conversa primeiro e depois aceita
			Resgates.pedirAjuda(p);
			helper.assertValueEqual(Resgates.fase(p), Resgates.APROXIMANDO, "pedir ajuda chama o socorro");
			Resgates.chegou(p);
			int pert = Perturbacao.get(p);
			Resgates.escolher(p, Resgates.Escolha.CONVERSAR);
			helper.assertValueEqual(Resgates.fase(p), Resgates.OFERTA, "conversar mantém a oferta");
			helper.assertTrue(Perturbacao.get(p) < pert, "conversa alivia a perturbação");
			Resgates.escolher(p, Resgates.Escolha.ACEITAR);
			helper.assertValueEqual(Resgates.fase(p), Resgates.ACOMPANHANDO, "aceitar acompanha");
			helper.assertValueEqual(Estados.get(p), EM_RESGATE, "estado em resgate");
			Resgates.concluir(p);
			helper.assertValueEqual(Estados.get(p), EM_ACOLHIMENTO, "chegada ao posto = acolhimento");
			helper.assertValueEqual(Resgates.fase(p), Resgates.NENHUM, "fase encerrada");
			helper.assertValueEqual(Resgates.socorristasVivos(p), 0, "socorristas removidos após o resgate");
			Resgates.concluir(p); // idempotente
			helper.assertValueEqual(Estados.get(p), EM_ACOLHIMENTO, "concluir duas vezes não muda nada");
			if (temTipo)
				helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(p.blockPosition()).inflate(40),
						e -> e.entityTags().contains(Resgates.TAG_TEMP) && !e.isRemoved()).isEmpty(), "sobrou socorrista temporário no mundo");
		} finally {
			remover(helper, p);
		}
		helper.succeed();
	}

	private static void resgateReconexao(GameTestHelper helper) {
		ServerPlayer p = jogador(helper);
		try {
			// Desconectou com o resgate já aceito: ao voltar, o resgate é concluído (não fica preso em EM_RESGATE).
			Estados.forcar(p, NO_UMBRAL_PERTURBADO, "teste");
			Resgates.oferecer(p);
			Resgates.chegou(p);
			Resgates.escolher(p, Resgates.Escolha.ACEITAR);
			Resgates.removerSocorristas(p); // o que acontece ao sair do jogo
			EventosDimensao.aoReconectar(p);
			// No Umbral real fica EM_ACOLHIMENTO no posto; aqui (Overworld de teste) a coerência com a dimensão também é restaurada.
			helper.assertTrue(Estados.get(p) != EM_RESGATE && Estados.coerente(Estados.get(p), p.level().dimension()), "reconexão durante resgate aceito: " + Estados.get(p));
			helper.assertValueEqual(Resgates.fase(p), Resgates.NENHUM, "fase limpa após reconexão");
			// Desconectou com a oferta aberta: a oferta é encerrada sem decidir por ele.
			Estados.forcar(p, NO_UMBRAL_PERTURBADO, "teste");
			Resgates.oferecer(p);
			Resgates.chegou(p);
			EventosDimensao.aoReconectar(p);
			helper.assertValueEqual(Resgates.fase(p), Resgates.NENHUM, "oferta aberta encerrada na reconexão");
			helper.assertTrue(Estados.get(p) != EM_RESGATE && Estados.get(p) != EM_ACOLHIMENTO, "sem decidir pelo jogador: " + Estados.get(p));
		} finally {
			remover(helper, p);
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- Perturbação e médium

	private static void perturbacao(GameTestHelper helper) {
		helper.assertTrue(Perturbacao.alvo(20, true) > Perturbacao.alvo(20, false), "médium mais sensível");
		helper.assertTrue(Perturbacao.alvo(10, false) > Perturbacao.alvo(70, false), "moral baixa mais perturbada");
		helper.assertTrue(Perturbacao.inicialNoUmbral(10) >= Perturbacao.LIMIAR_PERTURBADO, "despertar perturbado com moral baixa");
		ServerPlayer p = jogador(helper);
		try {
			Moralidade.definirClasse(p, ClasseEspiritual.MEDIUM);
			Estados.forcar(p, NO_UMBRAL_ORIENTADO, "teste");
			Perturbacao.set(p, 40);
			Perturbacao.agravar(p, 20, 90);
			helper.assertValueEqual(Estados.get(p), NO_UMBRAL_PERTURBADO, "histerese para perturbado");
			Perturbacao.aliviar(p, Perturbacao.ALIVIO_REPOUSO);
			helper.assertValueEqual(Estados.get(p), NO_UMBRAL_PERTURBADO, "ainda perturbado (histerese)");
			Planos.aoOrar(p); // prece reduz em qualquer lugar
			Perturbacao.aliviar(p, Perturbacao.ALIVIO_CONVERSA);
			helper.assertTrue(Perturbacao.get(p) <= Perturbacao.LIMIAR_ORIENTADO, "repouso+prece+conversa estabilizam: " + Perturbacao.get(p));
			helper.assertValueEqual(Estados.get(p), NO_UMBRAL_ORIENTADO, "médium estabilizado volta a orientado");
			Perturbacao.agravar(p, 100, Perturbacao.TETO_RECUSA);
			helper.assertTrue(Perturbacao.get(p) <= Perturbacao.TETO_RECUSA, "recusa nunca leva ao máximo (sem punição infinita)");
		} finally {
			remover(helper, p);
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- Entrada/saída de Nosso Lar

	private static void saidaEntrada(GameTestHelper helper) {
		helper.assertFalse(Planos.podeEntrarNossoLar(ENCARNADO_ESTAVEL), "vivo por portal não entra");
		helper.assertFalse(Planos.podeEntrarNossoLar(NO_UMBRAL_PERTURBADO), "perturbado sem acolhimento não entra");
		helper.assertTrue(Planos.podeEntrarNossoLar(EM_ACOLHIMENTO) && Planos.podeEntrarNossoLar(EM_VISITA_ASSISTIDA) && Planos.podeEntrarNossoLar(RECEM_DESENCARNADO), "eventos narrativos");
		helper.assertValueEqual(Planos.riscoDesorientacao(90, EM_NOSSO_LAR), 0, "elevado não se perde");
		helper.assertTrue(Planos.riscoDesorientacao(20, EM_NOSSO_LAR) > Planos.riscoDesorientacao(60, EM_NOSSO_LAR), "risco proporcional à moral");
		helper.assertTrue(Planos.riscoDesorientacao(60, EM_NOSSO_LAR) > 0, "risco existe sem acompanhamento");
		helper.assertFalse(Planos.saidaAssistidaLiberada(20, 0, 10), "saída assistida exige arrependimento ou moral");
		helper.assertFalse(Planos.saidaAssistidaLiberada(50, 0, 60), "saída assistida exige serenidade");
		helper.assertTrue(Planos.saidaAssistidaLiberada(20, 5, 10), "arrependimento + serenidade liberam");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Postos de Socorro

	private static void postosPlanejamento(GameTestHelper helper) {
		List<ConstrucoesUmbral.Planejado> a = ConstrucoesUmbral.planejar(12345L), b = ConstrucoesUmbral.planejar(12345L), c = ConstrucoesUmbral.planejar(999L);
		helper.assertValueEqual(a.size(), ConstrucoesUmbral.POSTOS, "quantidade de postos");
		helper.assertTrue(a.equals(b), "planejamento não determinístico");
		helper.assertFalse(a.equals(c), "seeds diferentes deveriam variar");
		for (int i = 0; i < a.size(); i++)
			for (int j = i + 1; j < a.size(); j++) {
				var p = a.get(i);
				var q = a.get(j);
				double d = Math.hypot(p.x() - q.x(), p.z() - q.z());
				helper.assertTrue(d >= 120, "postos sobrepostos/próximos demais: " + i + "," + j + " d=" + d);
			}
		helper.assertTrue(a.stream().anyMatch(p -> p.tamanho() == 2), "deveria haver posto avançado");
		helper.succeed();
	}

	private static void postoIncremental(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-1500, 0, 1500);
		for (int cx = (base.getX() - 24) >> 4; cx <= (base.getX() + 24) >> 4; cx++)
			for (int cz = (base.getZ() - 24) >> 4; cz <= (base.getZ() + 24) >> 4; cz++)
				level.setChunkForced(cx, cz, true);
		int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX(), base.getZ()) - 1;
		BlockPos o = new BlockPos(base.getX(), Math.max(y, level.getSeaLevel() + 1), base.getZ());
		int indice = 90;
		boolean jaFeito = ConstrucoesUmbral.concluido(level, indice);
		ConstrucoesUmbral.iniciarObra(level, indice, o, 2);
		if (!jaFeito)
			helper.assertFalse(ConstrucoesUmbral.concluido(level, indice), "não pode construir tudo num tick");
		helper.runAfterDelay(2, () -> {
			// Simula reinício do servidor no meio da obra: a fila em memória some e é retomada do Level.
			ConstrucoesUmbral.esquecerFila(level);
			ConstrucoesUmbral.retomar(level);
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(ConstrucoesUmbral.concluido(level, indice), "obra não concluída");
			BlockPos centro = BlockPos.of(Planos.pontos(level).get("posto_" + indice));
			helper.assertTrue(level.getBlockState(centro.below()).is(Blocks.SPRUCE_PLANKS), "piso do posto");
			helper.assertTrue(level.getBlockState(centro.offset(0, 5, 0)).is(Blocks.BEACON), "farol do posto: " + level.getBlockState(centro.offset(0, 5, 0)));
			helper.assertTrue(level.getBlockState(BlockPos.of(Planos.pontos(level).get("posto_" + indice + "_conversa"))).is(Blocks.LECTERN), "ponto de conversa");
			helper.assertTrue(level.getBlockState(centro.offset(-6, 0, -4)).getBlock() instanceof net.minecraft.world.level.block.BedBlock, "leitos");
			for (int cx = (base.getX() - 24) >> 4; cx <= (base.getX() + 24) >> 4; cx++)
				for (int cz = (base.getZ() - 24) >> 4; cz <= (base.getZ() + 24) >> 4; cz++)
					level.setChunkForced(cx, cz, false);
		});
	}

	// ---------------------------------------------------------------- Reencarnação incremental e idempotente

	private static void reencarnacao(GameTestHelper helper) {
		ServerPlayer p = jogador(helper);
		var server = helper.getLevel().getServer();
		BlockPos antes = p.blockPosition();
		p.setData(RegistroDimensao.ULTIMA_TERRA, antes.asLong());
		Moralidade.set(p, 66);
		Estados.forcar(p, EM_NOSSO_LAR, "teste");
		helper.assertTrue(Planos.iniciarReencarnacao(p), "deveria iniciar");
		helper.assertFalse(Planos.iniciarReencarnacao(p), "reencarnação duplicada");
		helper.assertValueEqual(Estados.get(p), REENCARNANDO, "estado reencarnando");
		helper.assertTrue(BuscaLocalSeguro.emAndamento(p.getUUID()), "busca deveria estar em andamento");
		helper.succeedWhen(() -> {
			helper.assertValueEqual(Estados.get(p), ENCARNADO_ESTAVEL, "não terminou de reencarnar");
			helper.assertFalse(BuscaLocalSeguro.emAndamento(p.getUUID()), "busca não encerrada");
			helper.assertValueEqual(Moralidade.get(p), 66, "moralidade conservada");
			helper.assertValueEqual(p.level().dimension(), Level.OVERWORLD, "deveria estar na Terra");
			helper.assertValueEqual(p.getData(RegistroDimensao.ULTIMA_TERRA), p.blockPosition().asLong(), "última posição atualizada uma vez");
			server.getPlayerList().remove(p);
		});
	}
}
