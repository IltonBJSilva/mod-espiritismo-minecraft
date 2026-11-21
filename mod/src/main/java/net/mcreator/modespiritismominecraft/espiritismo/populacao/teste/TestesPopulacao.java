package net.mcreator.modespiritismominecraft.espiritismo.populacao.teste;

import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.populacao.Zonas;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.TipoNpc;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.RotinaGoal;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.RegistroPopulacao;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.Povoamento;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.PopulacaoApi;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.Perfis;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.Perfil;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.MoradorEntity;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.MissoesAmelie;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.Memoria;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.JornadaRecemChegado;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.Dialogos;
import net.mcreator.modespiritismominecraft.espiritismo.npc.NpcApi;
import net.mcreator.modespiritismominecraft.espiritismo.estado.Estados;
import net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.function.Consumer;

/** GameTests da área "populacao" (§15.6). Só registrados com o GameTest habilitado. */
@EventBusSubscriber
public final class TestesPopulacao {
	private static final Identifier ESTRUTURA_VAZIA = Identifier.withDefaultNamespace("empty");

	private record Teste(int maxTicks, Consumer<GameTestHelper> funcao) {
	}

	private static final Map<String, Teste> TESTES = new LinkedHashMap<>();

	static {
		TESTES.put("pop_entidades_e_perfis", new Teste(20, TestesPopulacao::entidadesEPerfis));
		TESTES.put("pop_nomeado_nao_duplica", new Teste(40, TestesPopulacao::nomeadoNaoDuplica));
		TESTES.put("pop_amelie_unica", new Teste(40, TestesPopulacao::amelieUnica));
		TESTES.put("pop_dialogo_muda_com_estado", new Teste(20, TestesPopulacao::dialogoMudaComEstado));
		TESTES.put("pop_generico_temporario_protegido", new Teste(40, TestesPopulacao::genericoTemporario));
		TESTES.put("pop_jornada_recem_chegado", new Teste(20, TestesPopulacao::jornadaRecemChegado));
		TESTES.put("pop_missoes_amelie", new Teste(20, TestesPopulacao::missoesAmelie));
		TESTES.put("pop_rotina_periodos", new Teste(20, TestesPopulacao::rotina));
	}

	private TestesPopulacao() {
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
		Holder<TestEnvironmentDefinition<?>> ambiente = event.registerEnvironment(id("populacao_padrao"));
		TESTES.forEach((nome, t) -> event.registerTest(id(nome), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(nome)),
				new TestData<>(ambiente, ESTRUTURA_VAZIA, t.maxTicks(), 0, true))));
	}

	private static Player jogador(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 abs = helper.absoluteVec(new Vec3(0.5, 1, 0.5));
		player.setPos(abs.x, abs.y, abs.z);
		return player;
	}

	private static int contar(ServerLevel l, BlockPos centro, String perfil) {
		return l.getEntitiesOfClass(MoradorEntity.class, new AABB(centro).inflate(80), m -> m.isAlive() && perfil.equals(m.perfilId())).size();
	}

	/** Limpa o registro de um personagem no nível de teste (testes independentes entre si). */
	private static void limparRegistro(ServerLevel l, String perfil) {
		Map<String, String> m = new HashMap<>(Povoamento.registro(l));
		m.remove(perfil);
		l.setData(RegistroPopulacao.PERSONAGENS, m);
		l.getEntitiesOfClass(MoradorEntity.class, new AABB(BlockPos.ZERO).inflate(30_000_000), e -> perfil.equals(e.perfilId())).forEach(MoradorEntity::discard);
	}

	// ----------------------------------------------------------------

	private static void entidadesEPerfis(GameTestHelper helper) {
		for (String e : List.of("morador_nosso_lar", "personagem_nosso_lar", "amelie_boudet"))
			helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id(e)), "entidade " + e + " não registrada");
		for (String o : List.of("morador_nosso_lar_spawn_egg", "amelie_boudet_spawn_egg"))
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(o)), "ovo " + o + " não registrado");
		Set<String> nomes = new HashSet<>();
		for (Perfil p : Perfis.todos().values()) {
			helper.assertTrue(nomes.add(p.nome()), "nome repetido: " + p.nome());
			if (p.nomeado())
				helper.assertFalse(p.fonte().isEmpty(), "personagem " + p.id() + " sem fonte");
			helper.assertFalse(p.apresentacao().isEmpty(), "perfil " + p.id() + " sem apresentação");
		}
		for (String literario : List.of("clarencio", "lisias", "laura", "narcisa", "tobias", "veneranda", "genesio", "henrique_de_luna", "governador"))
			helper.assertValueEqual(Perfis.get(literario).categoria(), Perfil.Categoria.LITERARIO, "categoria de " + literario);
		helper.assertTrue(Perfis.genericos().size() >= 20, "poucos perfis genéricos");
		// Cada tipo de morador da cidade tem ao menos um perfil genérico (variedade, não clones).
		for (TipoNpc t : List.of(TipoNpc.RECEM_CHEGADO, TipoNpc.PACIENTE, TipoNpc.ENFERMEIRO, TipoNpc.ORIENTADOR, TipoNpc.INSTRUTOR, TipoNpc.MENSAGEIRO, TipoNpc.BIBLIOTECARIO,
				TipoNpc.JARDINEIRO, TipoNpc.ARTIFICE, TipoNpc.MORADOR, TipoNpc.ADMINISTRADOR))
			helper.assertTrue(Perfis.genericos().stream().anyMatch(p -> p.tipo() == t), "sem perfil genérico do tipo " + t);
		helper.succeed();
	}

	private static void nomeadoNaoDuplica(GameTestHelper helper) {
		ServerLevel l = helper.getLevel();
		limparRegistro(l, "lisias");
		BlockPos alvo = helper.absolutePos(new BlockPos(1, 1, 1));
		MoradorEntity a = PopulacaoApi.garantirPersonagem(l, "lisias", alvo);
		MoradorEntity b = PopulacaoApi.garantirPersonagem(l, "lisias", alvo);
		helper.assertTrue(a != null && a == b, "garantir deveria devolver o mesmo Lísias");
		helper.assertValueEqual(contar(l, alvo, "lisias"), 1, "Lísias no mundo");
		// Uma cópia (ex.: chunk recarregado com entidade antiga, ovo, /summon) não entra no nível.
		MoradorEntity copia = RegistroPopulacao.PERSONAGEM.get().create(l, EntitySpawnReason.EVENT);
		copia.definirPerfil(Perfis.get("lisias"));
		copia.snapTo(alvo.getX() + 0.5, alvo.getY(), alvo.getZ() + 0.5);
		helper.assertFalse(l.addFreshEntity(copia), "cópia de Lísias não deveria entrar no nível");
		helper.assertValueEqual(contar(l, alvo, "lisias"), 1, "Lísias após a cópia");
		helper.assertTrue(a.nomeado() && !a.temporario(), "personagem nomeado deve ser persistente");
		// Removido de verdade: o registro é liberado e ele pode ser recolocado.
		a.discard();
		helper.assertFalse(Povoamento.registro(l).containsKey("lisias"), "registro deveria ser liberado após remoção");
		MoradorEntity c = PopulacaoApi.garantirPersonagem(l, "lisias", alvo);
		helper.assertTrue(c != null && c != a, "Lísias deveria ser recolocado");
		limparRegistro(l, "lisias");
		helper.succeed();
	}

	private static void amelieUnica(GameTestHelper helper) {
		ServerLevel l = helper.getLevel();
		limparRegistro(l, Perfis.AMELIE);
		BlockPos casa = helper.absolutePos(new BlockPos(2, 1, 2));
		MoradorEntity a = PopulacaoApi.colocarAmelie(l, casa);
		helper.assertTrue(a != null, "Amélie deveria ser colocada");
		helper.assertTrue(a.getType() == RegistroPopulacao.AMELIE.get(), "tipo da Amélie");
		helper.assertValueEqual(a.perfilId(), Perfis.AMELIE, "perfil da Amélie");
		helper.assertTrue(PopulacaoApi.colocarAmelie(l, casa.east(3)) == a, "segunda chamada deveria devolver a mesma Amélie");
		MoradorEntity ovo = helper.spawn(RegistroPopulacao.AMELIE.get(), new Vec3(4.5, 1, 4.5)); // como um ovo gerador
		helper.assertTrue(l.getEntity(ovo.getUUID()) == null, "segunda Amélie não deveria entrar no nível");
		helper.assertValueEqual(contar(l, casa, Perfis.AMELIE), 1, "Amélies no mundo");
		limparRegistro(l, Perfis.AMELIE);
		helper.succeed();
	}

	private static void dialogoMudaComEstado(GameTestHelper helper) {
		Player p = jogador(helper);
		Perfil clarencio = Perfis.get("clarencio");
		RandomSource r = RandomSource.create(1);
		// 1º encontro: apresentação.
		Dialogos.Escolha e = Dialogos.escolher(clarencio, Dialogos.contexto(p, clarencio, EstadoEspiritual.EM_NOSSO_LAR, true), r, -1);
		helper.assertValueEqual(e.texto(), clarencio.apresentacao(), "primeiro encontro");
		Memoria.marcar(p, "conheceu:clarencio");
		// Estável em Nosso Lar: explicação ainda não dada.
		e = Dialogos.escolher(clarencio, Dialogos.contexto(p, clarencio, EstadoEspiritual.EM_NOSSO_LAR, true), r, -1);
		helper.assertValueEqual(e.chave(), "acolhimento", "primeira explicação");
		// Em tratamento: só acolhimento, nenhuma explicação/tarefa.
		e = Dialogos.escolher(clarencio, Dialogos.contexto(p, clarencio, EstadoEspiritual.EM_TRATAMENTO, true), r, -1);
		helper.assertTrue(TipoNpc.FALAS_CUIDADO.contains(e.texto()) && e.chave() == null, "fala de cuidado para quem está em tratamento");
		// Fora de Nosso Lar.
		e = Dialogos.escolher(clarencio, Dialogos.contexto(p, clarencio, EstadoEspiritual.ENCARNADO_ESTAVEL, false), r, -1);
		helper.assertTrue(TipoNpc.FALAS_FORA.contains(e.texto()), "fala fora da colônia");
		// Explicações dadas não se repetem; a da obra só aparece depois de ler Nosso Lar.
		Memoria.marcar(p, "explicou:clarencio:acolhimento");
		Memoria.marcar(p, "explicou:clarencio:ministerios");
		e = Dialogos.escolher(clarencio, Dialogos.contexto(p, clarencio, EstadoEspiritual.EM_NOSSO_LAR, true), r, -1);
		helper.assertTrue(e.chave() == null, "sem leitura: fala casual, não a explicação da obra");
		PopulacaoApi.registrarLeitura(p, "nosso_lar");
		e = Dialogos.escolher(clarencio, Dialogos.contexto(p, clarencio, EstadoEspiritual.EM_NOSSO_LAR, true), r, -1);
		helper.assertValueEqual(e.chave(), "obra", "explicação depois de ler Nosso Lar");
		// Casuais não repetem a anterior.
		Memoria.marcar(p, "explicou:clarencio:obra");
		Dialogos.Escolha c1 = Dialogos.escolher(clarencio, Dialogos.contexto(p, clarencio, EstadoEspiritual.EM_NOSSO_LAR, true), r, -1);
		for (int i = 0; i < 10; i++) {
			Dialogos.Escolha c2 = Dialogos.escolher(clarencio, Dialogos.contexto(p, clarencio, EstadoEspiritual.EM_NOSSO_LAR, true), r, c1.indiceCasual());
			helper.assertTrue(c2.indiceCasual() != c1.indiceCasual(), "fala casual repetida");
			c1 = c2;
		}
		helper.succeed();
	}

	private static void genericoTemporario(GameTestHelper helper) {
		ServerLevel l = helper.getLevel();
		Player p = jogador(helper);
		MoradorEntity m = Povoamento.criarGenerico(l, "praca", helper.absolutePos(new BlockPos(2, 1, 1)));
		helper.assertTrue(m != null && m.isAlive(), "genérico deveria ser criado");
		helper.assertTrue(m.temporario() && !m.shouldBeSaved(), "genérico do povoamento não é salvo no disco (não duplica ao recarregar)");
		helper.assertFalse(m.nomeado(), "genérico não é nomeado");
		helper.assertTrue(NpcApi.ehBenfeitorOuTrabalhador(m), "morador é protegido como trabalhador do bem");
		m.hurtServer(l, l.damageSources().playerAttack(p), 1000F);
		helper.assertTrue(m.isAlive(), "morador não deveria ser ferido");
		helper.assertValueEqual(Moralidade.get(p), 45, "moralidade após agredir morador");
		helper.assertValueEqual(Povoamento.contarGenericos(l, m.blockPosition(), 8), 1, "genéricos contados");
		m.discard();
		helper.succeed();
	}

	private static void jornadaRecemChegado(GameTestHelper helper) {
		Player p = jogador(helper);
		Estados.transicionar(p, EstadoEspiritual.EM_NOSSO_LAR, "teste");
		helper.assertValueEqual(JornadaRecemChegado.etapa(p), JornadaRecemChegado.RECEPCAO, "etapa inicial");
		// NPC errado para a etapa não avança.
		helper.assertFalse(conversa(p, "bento"), "jardineiro não faz a recepção");
		helper.assertTrue(conversa(p, "clarencio"), "Clarêncio recebe");
		helper.assertTrue(conversa(p, "lisias"), "Lísias acolhe");
		helper.assertValueEqual(JornadaRecemChegado.etapa(p), JornadaRecemChegado.MINISTERIOS, "após acolhimento");
		Memoria.definir(p, JornadaRecemChegado.ETAPA, JornadaRecemChegado.ESCOLHA);
		// Paciente não cuida de paciente: em tratamento, a enfermagem é recusada.
		Estados.forcar(p, EstadoEspiritual.EM_TRATAMENTO, "teste"); // direto (a tabela de transições não permite pular do estado inicial)
		helper.assertTrue(conversa(p, "narcisa"), "Narcisa responde");
		helper.assertValueEqual(JornadaRecemChegado.etapa(p), JornadaRecemChegado.ESCOLHA, "trabalho de assistência recusado em tratamento");
		// O estudo é sempre possível.
		helper.assertTrue(conversa(p, "helena"), "bibliotecária oferece estudo");
		helper.assertTrue(Memoria.tem(p, "escolha:estudo"), "escolha registrada");
		helper.assertValueEqual(JornadaRecemChegado.etapa(p), JornadaRecemChegado.RECUPERACAO, "após a escolha");
		// Recuperação não é instantânea.
		Estados.transicionar(p, EstadoEspiritual.EM_NOSSO_LAR, "teste");
		helper.assertTrue(conversa(p, "lisias"), "Lísias responde");
		helper.assertValueEqual(JornadaRecemChegado.etapa(p), JornadaRecemChegado.RECUPERACAO, "cedo demais");
		Memoria.definir(p, JornadaRecemChegado.INICIO, p.level().getGameTime() - JornadaRecemChegado.TEMPO_RECUPERACAO);
		helper.assertTrue(conversa(p, "laura"), "Laura conclui");
		helper.assertValueEqual(JornadaRecemChegado.etapa(p), JornadaRecemChegado.CONCLUIDA, "jornada concluída");
		helper.succeed();
	}

	private static boolean conversa(Player p, String perfil) {
		Perfil pf = Perfis.get(perfil);
		return JornadaRecemChegado.conversar(p, null, pf, Dialogos.contexto(p, pf, Estados.get(p), true));
	}

	private static void missoesAmelie(GameTestHelper helper) {
		Player p = jogador(helper);
		Perfil amelie = Perfis.get(Perfis.AMELIE);
		helper.assertTrue(MissoesAmelie.conversar(p, null, amelie), "encontro");
		helper.assertValueEqual(MissoesAmelie.etapa(p), MissoesAmelie.DOCUMENTOS, "após encontro");
		MissoesAmelie.conversar(p, null, amelie);
		helper.assertValueEqual(MissoesAmelie.etapa(p), MissoesAmelie.DOCUMENTOS, "sem papel não avança");
		p.getInventory().add(new ItemStack(Items.PAPER, 8));
		MissoesAmelie.conversar(p, null, amelie);
		helper.assertValueEqual(MissoesAmelie.etapa(p), MissoesAmelie.METODO, "papéis entregues");
		helper.assertValueEqual(MissoesAmelie.contar(p, Items.PAPER), 2, "papéis restantes");
		MissoesAmelie.conversar(p, null, amelie);
		helper.assertValueEqual(MissoesAmelie.etapa(p), MissoesAmelie.METODO, "sem estudo não avança");
		PopulacaoApi.registrarLeitura(p, "o_livro_dos_espiritos");
		MissoesAmelie.conversar(p, null, amelie);
		helper.assertValueEqual(MissoesAmelie.etapa(p), MissoesAmelie.CONTINUIDADE, "após estudar");
		helper.assertFalse(MissoesAmelie.entregarAoDirigente(p), "sem livro na mão não entrega");
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK));
		helper.assertTrue(MissoesAmelie.entregarAoDirigente(p), "entrega do livro ao Dirigente");
		helper.assertTrue(p.getMainHandItem().isEmpty(), "livro consumido");
		helper.assertFalse(MissoesAmelie.entregarAoDirigente(p), "não entrega duas vezes");
		MissoesAmelie.conversar(p, null, amelie);
		helper.assertValueEqual(MissoesAmelie.etapa(p), MissoesAmelie.CONCLUIDA, "cadeia concluída");
		helper.assertFalse(MissoesAmelie.conversar(p, null, amelie), "depois de concluída, conversa normal");
		helper.succeed();
	}

	private static void rotina(GameTestHelper helper) {
		helper.assertValueEqual(RotinaGoal.Periodo.de(1000), RotinaGoal.Periodo.TRABALHO, "manhã");
		helper.assertValueEqual(RotinaGoal.Periodo.de(10000), RotinaGoal.Periodo.CONVIVENCIA, "tarde");
		helper.assertValueEqual(RotinaGoal.Periodo.de(15000), RotinaGoal.Periodo.DESCANSO, "noite");
		helper.assertValueEqual(RotinaGoal.Periodo.de(24000L * 5 + 2000), RotinaGoal.Periodo.TRABALHO, "outro dia");
		Perfil clarencio = Perfis.get("clarencio");
		helper.assertValueEqual(RotinaGoal.zonaDoPeriodo(clarencio, "", RotinaGoal.Periodo.TRABALHO, 0), "auxilio", "Clarêncio trabalha no Auxílio");
		helper.assertValueEqual(RotinaGoal.zonaDoPeriodo(clarencio, "", RotinaGoal.Periodo.DESCANSO, 0), "residencias", "descanso nas residências");
		Perfil bento = Perfis.get("bento");
		helper.assertValueEqual(RotinaGoal.zonaDoPeriodo(bento, "praca", RotinaGoal.Periodo.TRABALHO, 0), "praca", "genérico trabalha na zona de origem");
		for (Perfil p : Perfis.todos().values())
			if (!p.local().isEmpty())
				helper.assertTrue(Zonas.COTA.containsKey(p.local()), "zona desconhecida em " + p.id() + ": " + p.local());
		helper.succeed();
	}
}
