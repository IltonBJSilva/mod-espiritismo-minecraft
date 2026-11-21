package net.mcreator.modespiritismominecraft.espiritismo.npc.teste;

import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
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

import net.mcreator.modespiritismominecraft.espiritismo.npc.TrabalhadorEntity;
import net.mcreator.modespiritismominecraft.espiritismo.npc.Prece;
import net.mcreator.modespiritismominecraft.espiritismo.npc.NpcApi;
import net.mcreator.modespiritismominecraft.espiritismo.npc.Espiritos;
import net.mcreator.modespiritismominecraft.espiritismo.npc.EspiritoEntity;
import net.mcreator.modespiritismominecraft.espiritismo.npc.Atendimentos;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

/** GameTests da área "npc". Só registrados com o GameTest habilitado. */
@EventBusSubscriber
public final class TestesNpc {
	private static final Identifier ESTRUTURA_VAZIA = Identifier.withDefaultNamespace("empty");

	private record Teste(int maxTicks, Consumer<GameTestHelper> funcao) {
	}

	private static final Map<String, Teste> TESTES = new LinkedHashMap<>();

	static {
		TESTES.put("npc_entidades_registradas", new Teste(20, TestesNpc::entidadesRegistradas));
		TESTES.put("npc_trabalhador_invulneravel", new Teste(40, TestesNpc::trabalhadorInvulneravel));
		TESTES.put("npc_passe_remove_nocivo_e_recarga", new Teste(40, TestesNpc::passe));
		TESTES.put("npc_espada_nao_fere_obsessor", new Teste(40, TestesNpc::espadaObsessor));
		TESTES.put("npc_esclarecer_obsessor_vira_sofredor", new Teste(40, TestesNpc::esclarecer));
		TESTES.put("npc_prece_gasta_energia", new Teste(40, TestesNpc::prece));
	}

	private TestesNpc() {
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
		Holder<TestEnvironmentDefinition<?>> ambiente = event.registerEnvironment(id("npc_padrao"));
		TESTES.forEach((nome, t) -> event.registerTest(id(nome), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(nome)),
				new TestData<>(ambiente, ESTRUTURA_VAZIA, t.maxTicks(), 0, true))));
	}

	private static Player jogador(GameTestHelper helper, Vec3 relativo) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 abs = helper.absoluteVec(relativo);
		player.setPos(abs.x, abs.y, abs.z);
		return player;
	}

	// ----------------------------------------------------------------

	private static void entidadesRegistradas(GameTestHelper helper) {
		for (String e : List.of("dirigente", "medium_trabalhador", "orador", "passista", "evangelizadora", "divaldo_franco", "mentor", "obsessor", "espirito_sofredor", "zombeteiro",
				"socorrista")) {
			helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id(e)), "entidade " + e + " não registrada");
			helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(e + "_spawn_egg")), "ovo de " + e + " não registrado");
		}
		helper.assertTrue(NetworkRegistry.getCodec(Prece.Orar.TYPE.id(), ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND) != null, "payload npc_orar não registrado");
		helper.succeed();
	}

	private static void trabalhadorInvulneravel(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 1, 1.5));
		TrabalhadorEntity t = helper.spawn(NpcApi.DIRIGENTE.get(), new Vec3(0.5, 1, 0.5));
		helper.assertTrue(NpcApi.ehBenfeitorOuTrabalhador(t), "dirigente deveria ser trabalhador");
		t.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(p), 1000F);
		helper.assertTrue(t.isAlive(), "dirigente deveria ser invulnerável");
		helper.assertValueEqual(t.getHealth(), t.getMaxHealth(), "vida do dirigente após ataque");
		helper.assertValueEqual(Moralidade.get(p), 45, "moralidade após agredir trabalhador");
		// Recarga: um segundo golpe logo em seguida não pesa de novo.
		t.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(p), 1000F);
		helper.assertValueEqual(Moralidade.get(p), 45, "recarga da penalidade");
		t.hurtServer(helper.getLevel(), helper.getLevel().damageSources().lava(), 1000F);
		helper.assertTrue(t.isAlive(), "dirigente morreu para lava");
		t.discard();
		helper.succeed();
	}

	private static void passe(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 1, 0.5));
		p.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 0));
		Moralidade.alterarEnergia(p, -50);
		helper.assertTrue(Atendimentos.passe(p, null), "passe deveria ser aplicado");
		helper.assertFalse(p.hasEffect(MobEffects.POISON), "veneno deveria ter sido removido");
		helper.assertValueEqual(Moralidade.energia(p), 80, "energia após o passe");
		p.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 0));
		helper.assertFalse(Atendimentos.passe(p, null), "passe em recarga não deveria ser aplicado");
		helper.assertTrue(p.hasEffect(MobEffects.POISON), "veneno não deveria sair durante a recarga");
		helper.succeed();
	}

	private static void espadaObsessor(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 1, 1.5));
		EspiritoEntity o = helper.spawn(NpcApi.OBSESSOR.get(), new Vec3(0.5, 2, 0.5));
		o.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(p), 1000F);
		helper.assertTrue(o.isAlive(), "obsessor não deveria morrer por espadada");
		helper.assertValueEqual(o.getHealth(), o.getMaxHealth(), "vida do obsessor");
		helper.assertValueEqual(Moralidade.get(p), 50, "golpear Espírito não muda moralidade");
		o.discard();
		helper.succeed();
	}

	private static void esclarecer(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 1, 1.5));
		ServerLevel level = helper.getLevel();
		EspiritoEntity o = helper.spawn(NpcApi.OBSESSOR.get(), new Vec3(0.5, 2, 0.5));
		Vec3 pos = o.position();
		helper.assertTrue(Espiritos.esclarecer(o, p), "esclarecer deveria funcionar");
		helper.assertFalse(o.isAlive(), "obsessor deveria ter saído");
		var sofredores = level.getEntitiesOfClass(EspiritoEntity.class, new AABB(pos, pos).inflate(3), e -> e.tipo() == EspiritoEntity.Tipo.SOFREDOR);
		helper.assertValueEqual(sofredores.size(), 1, "sofredores no lugar do obsessor");
		helper.assertTrue(sofredores.get(0).emResgate(), "sofredor deveria estar em resgate");
		helper.assertValueEqual(Moralidade.get(p), 55, "moralidade após esclarecer");
		helper.assertFalse(Espiritos.esclarecer(p, p), "jogador não é Espírito");
		sofredores.forEach(EspiritoEntity::discard);
		helper.succeed();
	}

	private static void prece(GameTestHelper helper) {
		Player p = jogador(helper, new Vec3(0.5, 1, 0.5));
		p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
		int[] chamadas = {0};
		Consumer<Player> gancho = j -> chamadas[0]++;
		Prece.AO_ORAR.add(gancho);
		try {
			helper.assertTrue(Prece.orar(p), "prece deveria acontecer");
			helper.assertValueEqual(Moralidade.energia(p), 100 - Prece.CUSTO, "energia após a prece");
			helper.assertFalse(p.hasEffect(MobEffects.DARKNESS), "prece deveria remover a escuridão");
			helper.assertValueEqual(Moralidade.get(p), 51, "moralidade após a prece (rotina)");
			helper.assertFalse(Prece.orar(p), "prece em recarga");
			helper.assertValueEqual(Moralidade.energia(p), 100 - Prece.CUSTO, "recarga não gasta energia");
			helper.assertValueEqual(chamadas[0], 1, "gancho AO_ORAR");
		} finally {
			Prece.AO_ORAR.remove(gancho);
		}
		helper.succeed();
	}
}
