package net.mcreator.modespiritismominecraft.espiritismo.teste;

import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;

import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModItems;
import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModEntities;
import net.mcreator.modespiritismominecraft.espiritismo.Pacotes;
import net.mcreator.modespiritismominecraft.espiritismo.Jornada;
import net.mcreator.modespiritismominecraft.espiritismo.EventosEspirituais;
import net.mcreator.modespiritismominecraft.espiritismo.CentroEspirita;
import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModBlocks;
import net.mcreator.modespiritismominecraft.entity.HaroldoDutraEntity;
import net.mcreator.modespiritismominecraft.entity.ChicoXavierEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.BlockPos;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.espiritismo.Habilidades;
import net.mcreator.modespiritismominecraft.espiritismo.ClasseEspiritual;
import net.mcreator.modespiritismominecraft.entity.AllanKardecEntity;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GameTests de runtime do mod. Só são registrados quando o GameTest está habilitado
 * (servidor de gametest / runs de desenvolvimento com neoforge.enableGameTest), nunca em produção.
 * <p>
 * Rodar: ./gradlew runGameTestServer
 */
@EventBusSubscriber
public final class TestesEspirituais {
	private static final Identifier ESTRUTURA_VAZIA = Identifier.withDefaultNamespace("empty");

	/** nome do teste -> (maxTicks, função) */
	private record Teste(int maxTicks, Consumer<GameTestHelper> funcao) {
	}

	private static final Map<String, Teste> TESTES = new LinkedHashMap<>();

	static {
		TESTES.put("mod_carrega", new Teste(20, TestesEspirituais::modCarrega));
		TESTES.put("moralidade_padrao_e_limites", new Teste(20, TestesEspirituais::moralidadeLimites));
		TESTES.put("allan_kardec_nao_hostil", new Teste(40, TestesEspirituais::kardecNaoHostil));
		TESTES.put("escolher_classe_requer_kardec", new Teste(20, TestesEspirituais::escolherClasse));
		TESTES.put("habilidade_gasta_energia_e_recarga", new Teste(260, TestesEspirituais::usarHabilidade));
		TESTES.put("matar_aldeao_reduz_moralidade", new Teste(40, TestesEspirituais::matarAldeao));
		TESTES.put("kardec_invulneravel", new Teste(40, TestesEspirituais::matarKardec));
		TESTES.put("teto_diario_rotina", new Teste(40, TestesEspirituais::tetoRotina));
		TESTES.put("centro_espirita_construido", new Teste(60, TestesEspirituais::centroConstruido));
		TESTES.put("caridade_aldeao_faminto", new Teste(40, TestesEspirituais::caridade));
		TESTES.put("jornada_libera_classe", new Teste(20, TestesEspirituais::jornadaLiberaClasse));
	}

	private TestesEspirituais() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, path);
	}

	// ---------------------------------------------------------------- Registro (somente com GameTest habilitado)

	@SubscribeEvent
	public static void registrarFuncoes(RegisterEvent event) {
		if (!GameTestHooks.isGametestEnabled())
			return;
		event.register(Registries.TEST_FUNCTION, helper -> TESTES.forEach((nome, t) -> helper.register(id(nome), t.funcao())));
	}

	@SubscribeEvent
	public static void registrarTestes(RegisterGameTestsEvent event) {
		Holder<TestEnvironmentDefinition<?>> ambiente = event.registerEnvironment(id("padrao"));
		TESTES.forEach((nome, t) -> event.registerTest(id(nome), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(nome)),
				new TestData<>(ambiente, ESTRUTURA_VAZIA, t.maxTicks(), 0, true))));
	}

	// ---------------------------------------------------------------- Utilitários

	private static Player jogador(GameTestHelper helper, Vec3 relativo) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 abs = helper.absoluteVec(relativo);
		player.setPos(abs.x, abs.y, abs.z);
		return player;
	}

	// ---------------------------------------------------------------- 1. Mod carrega

	private static void modCarrega(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id("allan_kardec")), "entidade allan_kardec não registrada");
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id("chico_xavier")), "entidade chico_xavier não registrada");
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id("haroldo_dutra")), "entidade haroldo_dutra não registrada");
		for (String a : new String[]{"moralidade", "recebeu_livro", "classe", "energia"})
			helper.assertTrue(NeoForgeRegistries.ATTACHMENT_TYPES.containsKey(id(a)), "attachment " + a + " não registrado");
		helper.assertTrue(ModEspiritismoMinecraftModItems.AGUA_FLUIDIFICADA.get() != null, "item agua_fluidificada ausente");
		helper.assertTrue(ModEspiritismoMinecraftModItems.LIVRODOS_ESPIRITOS.get() != null, "item livro dos espiritos ausente");
		var raiz = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();
		helper.assertTrue(raiz.getChild("moralidade") != null, "comando /moralidade não registrado");
		helper.assertTrue(raiz.getChild("espiritismo") != null, "comando /espiritismo não registrado");
		helper.assertTrue(raiz.getChild("centro") != null, "comando /centro não registrado");
		for (String c : new String[]{"raiz", "primeira_leitura", "agua", "caridade", "caminho", "habilidade", "palestra", "elevado"})
			helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(id("jornada/" + c)) != null, "conquista jornada/" + c + " não carregou");
		helper.assertTrue(NetworkRegistry.getCodec(Pacotes.EscolherClasse.TYPE.id(), ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND) != null, "payload escolher_classe não registrado");
		helper.assertTrue(NetworkRegistry.getCodec(Pacotes.UsarHabilidade.TYPE.id(), ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND) != null, "payload usar_habilidade não registrado");
		helper.assertTrue(NetworkRegistry.getCodec(Pacotes.AbrirEscolhaClasse.TYPE.id(), ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND) != null, "payload abrir_escolha_classe não registrado");
		helper.succeed();
	}

	// ---------------------------------------------------------------- 2. Moralidade

	private static void moralidadeLimites(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 1, 0.5));
		helper.assertValueEqual(Moralidade.get(p), 50, "moralidade inicial");
		helper.assertValueEqual(Moralidade.energia(p), 100, "energia inicial");
		helper.assertValueEqual(Moralidade.classe(p), ClasseEspiritual.NENHUMA, "classe inicial");
		Moralidade.alterar(p, 10, "x");
		helper.assertValueEqual(Moralidade.get(p), 60, "moralidade após +10");
		Moralidade.alterar(p, 500, "x");
		helper.assertValueEqual(Moralidade.get(p), 100, "moralidade limitada em 100");
		Moralidade.alterar(p, -1000, "x");
		helper.assertValueEqual(Moralidade.get(p), 0, "moralidade limitada em 0");
		Moralidade.set(p, 150);
		helper.assertValueEqual(Moralidade.get(p), 100, "set(150) limitado em 100");
		Moralidade.alterarEnergia(p, -500);
		helper.assertValueEqual(Moralidade.energia(p), 0, "energia limitada em 0");
		helper.succeed();
	}

	// ---------------------------------------------------------------- 3. Allan Kardec

	private static void kardecNaoHostil(GameTestHelper helper) {
		AllanKardecEntity kardec = helper.spawn(ModEspiritismoMinecraftModEntities.ALLAN_KARDEC.get(), new Vec3(0.5, 1, 0.5));
		helper.assertTrue(kardec != null && kardec.isAlive(), "Allan Kardec não spawnou");
		Object k = kardec;
		helper.assertFalse(k instanceof Monster || k instanceof net.minecraft.world.entity.monster.Enemy, "Allan Kardec é Monster/Enemy");
		helper.assertTrue(kardec.getType().getCategory() != MobCategory.MONSTER, "categoria MONSTER");
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(kardec.isAlive(), "Allan Kardec morreu/despawnou");
			helper.assertTrue(kardec.getTarget() == null, "Allan Kardec tem alvo: " + kardec.getTarget());
			kardec.discard();
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- 4. Escolha de classe

	private static void escolherClasse(GameTestHelper helper) {
		// Bem acima do grid de testes, para nenhum Kardec de outro teste estar a menos de 8 blocos.
		Player p = jogador(helper, new Vec3(0.5, 80, 0.5));
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.getEntitiesOfClass(AllanKardecEntity.class, p.getBoundingBox().inflate(8)).isEmpty(), "pré-condição: já existe um Kardec por perto");
		Habilidades.escolherClasse(p, ClasseEspiritual.MEDIUM.ordinal());
		helper.assertValueEqual(Moralidade.classe(p), ClasseEspiritual.NENHUMA, "classe sem Kardec por perto");

		AllanKardecEntity kardec = helper.spawn(ModEspiritismoMinecraftModEntities.ALLAN_KARDEC.get(), new Vec3(3.5, 80, 0.5));
		kardec.setNoGravity(true);
		Moralidade.alterarEnergia(p, -60);
		Jornada.definir(p, Jornada.ESCOLHER_CLASSE);
		Habilidades.escolherClasse(p, ClasseEspiritual.MEDIUM.ordinal());
		helper.assertValueEqual(Moralidade.classe(p), ClasseEspiritual.MEDIUM, "classe com Kardec a 3 blocos");
		helper.assertValueEqual(Moralidade.energia(p), 100, "energia restaurada ao escolher classe");
		// Não pode trocar de classe depois de escolhida.
		Habilidades.escolherClasse(p, ClasseEspiritual.DOUTRINADOR.ordinal());
		helper.assertValueEqual(Moralidade.classe(p), ClasseEspiritual.MEDIUM, "classe não deve mudar");
		kardec.discard();
		helper.succeed();
	}

	// ---------------------------------------------------------------- 5. Habilidade ativa

	private static void usarHabilidade(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 1, 0.5));
		Habilidades.usar(p);
		helper.assertValueEqual(Moralidade.energia(p), 100, "sem classe, energia não muda");
		Moralidade.definirClasse(p, ClasseEspiritual.MEDIUM);
		Habilidades.usar(p);
		helper.assertValueEqual(Moralidade.energia(p), 70, "energia após Psicografia");
		Habilidades.usar(p);
		helper.assertValueEqual(Moralidade.energia(p), 70, "energia durante recarga");
		helper.runAfterDelay(ClasseEspiritual.MEDIUM.recarga + 1, () -> {
			Habilidades.usar(p);
			helper.assertValueEqual(Moralidade.energia(p), 40, "energia após recarga terminar");
			Habilidades.usar(p);
			helper.assertValueEqual(Moralidade.energia(p), 40, "recarga reiniciada");
			Habilidades.limparRecarga(p);
			Habilidades.usar(p);
			helper.assertValueEqual(Moralidade.energia(p), 10, "após limparRecarga");
			Habilidades.limparRecarga(p);
			Habilidades.usar(p);
			helper.assertValueEqual(Moralidade.energia(p), 10, "energia insuficiente não deve gastar");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- 6. Matar aldeão / benfeitor

	private static void matarAldeao(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 1, 1.5));
		Villager aldeao = helper.spawn(EntityType.VILLAGER, new Vec3(0.5, 1, 0.5));
		aldeao.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(p), 1000F);
		helper.assertFalse(aldeao.isAlive(), "aldeão não morreu");
		helper.assertValueEqual(Moralidade.get(p), 40, "moralidade após matar aldeão");
		helper.succeed();
	}

	private static void matarKardec(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 1, 1.5));
		AllanKardecEntity kardec = helper.spawn(ModEspiritismoMinecraftModEntities.ALLAN_KARDEC.get(), new Vec3(0.5, 1, 0.5));
		kardec.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(p), 1000F);
		helper.assertTrue(kardec.isAlive(), "Kardec deveria ser invulnerável");
		helper.assertValueEqual(kardec.getHealth(), kardec.getMaxHealth(), "vida de Kardec após ataque");
		// Agredir um benfeitor ainda custa -5.
		helper.assertValueEqual(Moralidade.get(p), 45, "moralidade após agredir Allan Kardec");
		// Dano do ambiente (lava, queda) também é ignorado.
		kardec.hurtServer(helper.getLevel(), helper.getLevel().damageSources().lava(), 1000F);
		helper.assertTrue(kardec.isAlive(), "Kardec morreu para lava");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Teto diário de ganhos de rotina

	private static void tetoRotina(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 1, 0.5));
		for (int i = 0; i < 20; i++)
			Moralidade.ganhoRotina(p, 1, "teste");
		helper.assertValueEqual(Moralidade.get(p), 50 + Moralidade.TETO_ROTINA_DIA, "moralidade após 20 ganhos de rotina");
		Moralidade.alterar(p, 5, "missão");
		helper.assertValueEqual(Moralidade.get(p), 55 + Moralidade.TETO_ROTINA_DIA, "missões não têm teto");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Centro Espírita

	private static void centroConstruido(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Longe do grid de testes para não interferir nos outros; os chunks ficam carregados durante o teste.
		BlockPos origem = helper.absolutePos(BlockPos.ZERO).offset(-300, 0, -300);
		java.util.List<long[]> chunks = new java.util.ArrayList<>();
		for (int cx = (origem.getX() - 4) >> 4; cx <= (origem.getX() + 16) >> 4; cx++)
			for (int cz = (origem.getZ() - 8) >> 4; cz <= (origem.getZ() + 14) >> 4; cz++) {
				level.setChunkForced(cx, cz, true);
				chunks.add(new long[]{cx, cz});
			}
		BlockPos porta = CentroEspirita.construir(level, origem, false);
		helper.assertTrue(level.getBlockState(porta).is(Blocks.SPRUCE_DOOR), "porta ausente: " + level.getBlockState(porta));
		helper.assertTrue(level.getBlockState(origem.offset(6, 2, 8)).is(ModEspiritismoMinecraftModBlocks.LIVRO.get()), "livro na mesa ausente");
		helper.assertTrue(level.getBlockState(origem.offset(6, 9, 5)).is(Blocks.END_ROD), "luz do telhado ausente");
		AABB caixa = new AABB(origem).inflate(20);
		helper.assertTrue(level.getEntitiesOfClass(AllanKardecEntity.class, caixa).isEmpty(), "construir(false) não deve colocar NPCs");
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(level.isPositionEntityTicking(origem), "chunk do Centro não ficou ativo");
			CentroEspirita.verificarBenfeitores(level, porta);
			CentroEspirita.verificarBenfeitores(level, porta); // a segunda chamada não pode duplicar
			var kardecs = level.getEntitiesOfClass(AllanKardecEntity.class, caixa);
			var chicos = level.getEntitiesOfClass(ChicoXavierEntity.class, caixa);
			var haroldos = level.getEntitiesOfClass(HaroldoDutraEntity.class, caixa);
			helper.assertValueEqual(kardecs.size(), 1, "Allan Kardec no Centro");
			helper.assertValueEqual(chicos.size(), 1, "Chico Xavier no Centro");
			helper.assertValueEqual(haroldos.size(), 1, "Haroldo Dutra no Centro");
			Mob k = kardecs.get(0);
			helper.assertTrue(k.hasHome() && k.isPersistenceRequired(), "Kardec deveria ficar no Centro");
			kardecs.forEach(Mob::discard);
			chicos.forEach(Mob::discard);
			haroldos.forEach(Mob::discard);
			chunks.forEach(c -> level.setChunkForced((int) c[0], (int) c[1], false));
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- Caridade / tutorial

	private static void caridade(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 1, 1.5));
		Jornada.definir(p, Jornada.CARIDADE);
		Villager aldeao = helper.spawn(EntityType.VILLAGER, new Vec3(0.5, 1, 0.5));
		Jornada.marcarFaminto(aldeao, p);
		// Outro jogador não recebe a recompensa da prova alheia.
		Player outro = jogador(helper, new Vec3(1.5, 1, 1.5));
		helper.assertFalse(Jornada.famintoPara(aldeao, outro), "prova deveria ser só do primeiro jogador");
		ItemStack pao = new ItemStack(Items.BREAD, 2);
		EventosEspirituais.alimentar(p, aldeao, pao);
		helper.assertValueEqual(pao.getCount(), 1, "pão consumido");
		helper.assertFalse(Jornada.faminto(aldeao), "aldeão continua faminto");
		helper.assertValueEqual(Moralidade.get(p), 54, "moralidade após caridade");
		helper.assertValueEqual(Jornada.etapa(p), Jornada.ESCOLHER_CLASSE, "etapa após caridade");
		// Dar comida a quem não tem fome rende pouco (rotina) e não avança nada.
		EventosEspirituais.alimentar(p, aldeao, pao);
		helper.assertValueEqual(Moralidade.get(p), 55, "partilhar com aldeão saciado");
		aldeao.discard();
		helper.succeed();
	}

	private static void jornadaLiberaClasse(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 120, 0.5));
		AllanKardecEntity kardec = helper.spawn(ModEspiritismoMinecraftModEntities.ALLAN_KARDEC.get(), new Vec3(2.5, 120, 0.5));
		kardec.setNoGravity(true);
		helper.assertValueEqual(Jornada.etapa(p), Jornada.FALAR_KARDEC, "etapa inicial");
		Habilidades.escolherClasse(p, ClasseEspiritual.TRABALHADOR.ordinal());
		helper.assertValueEqual(Moralidade.classe(p), ClasseEspiritual.NENHUMA, "classe antes do tutorial");
		helper.assertTrue(Jornada.avancar(p, Jornada.FALAR_KARDEC), "avançar 0");
		helper.assertFalse(Jornada.avancar(p, Jornada.FALAR_KARDEC), "não avança duas vezes");
		Jornada.avancar(p, Jornada.LER_LIVRO);
		Jornada.definir(p, Jornada.CARIDADE); // (avancar daqui faria um aldeão faminto chegar)
		Jornada.avancar(p, Jornada.CARIDADE);
		helper.assertValueEqual(Jornada.etapa(p), Jornada.ESCOLHER_CLASSE, "etapa de escolha");
		Habilidades.escolherClasse(p, ClasseEspiritual.TRABALHADOR.ordinal());
		helper.assertValueEqual(Moralidade.classe(p), ClasseEspiritual.TRABALHADOR, "classe após tutorial");
		helper.assertValueEqual(Jornada.etapa(p), Jornada.CONCLUIDA, "jornada concluída");
		kardec.discard();
		helper.succeed();
	}
}
