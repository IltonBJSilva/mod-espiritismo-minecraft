package net.mcreator.modespiritismominecraft.espiritismo.biblioteca.teste;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.BibliotecaApi;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.Catalogo;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.EvangelhoNoLar;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.Glossario;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.ProgressoBiblioteca;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.RegistroBiblioteca;
import net.mcreator.modespiritismominecraft.espiritismo.biblioteca.Trilhas;
import net.mcreator.modespiritismominecraft.espiritismo.itens.Obras;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** GameTests da Biblioteca Espírita (registrados só com GameTest habilitado). */
@EventBusSubscriber
public final class TestesBiblioteca {
	private static final Identifier ESTRUTURA_VAZIA = Identifier.withDefaultNamespace("empty");

	private record Teste(int maxTicks, Consumer<GameTestHelper> funcao) {
	}

	private static final Map<String, Teste> TESTES = new LinkedHashMap<>();

	static {
		TESTES.put("biblioteca_catalogo_carrega", new Teste(20, TestesBiblioteca::catalogo));
		TESTES.put("biblioteca_direitos_em_cada_obra", new Teste(20, TestesBiblioteca::direitos));
		TESTES.put("biblioteca_progresso_salvo", new Teste(20, TestesBiblioteca::progresso));
		TESTES.put("biblioteca_quiz_registra_acerto", new Teste(20, TestesBiblioteca::quiz));
		TESTES.put("biblioteca_trilhas_completas", new Teste(20, TestesBiblioteca::trilhas));
		TESTES.put("biblioteca_evangelho_no_lar", new Teste(20, TestesBiblioteca::evangelho));
	}

	private TestesBiblioteca() {
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
		Holder<TestEnvironmentDefinition<?>> ambiente = event.registerEnvironment(id("biblioteca_padrao"));
		TESTES.forEach((nome, t) -> event.registerTest(id(nome), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(nome)),
				new TestData<>(ambiente, ESTRUTURA_VAZIA, t.maxTicks(), 0, true))));
	}

	private static boolean vazio(String s) {
		return s == null || s.isBlank();
	}

	private static void catalogo(GameTestHelper helper) {
		helper.assertTrue(Catalogo.OBRAS.size() >= 15, "catálogo pequeno demais: " + Catalogo.OBRAS.size());
		// Integração com itens.Obras: toda obra-item está no catálogo, sem duplicar.
		for (String idObra : Obras.OBRAS.keySet()) {
			Catalogo.Obra o = Catalogo.get(idObra);
			helper.assertTrue(o != null && idObra.equals(o.itemObra()), "obra-item fora do catálogo: " + idObra);
			helper.assertTrue(o.secoes().size() >= Obras.OBRAS.get(idObra).secoes().size(), "seções do item não importadas: " + idObra);
		}
		Set<String> perguntas = new HashSet<>();
		for (Catalogo.Obra o : Catalogo.OBRAS.values()) {
			helper.assertFalse(vazio(o.titulo()) || vazio(o.autor()) || vazio(o.ano()) || vazio(o.serie()) || vazio(o.edicao()) || vazio(o.contexto()),
					"ficha bibliográfica incompleta: " + o.id());
			helper.assertFalse(o.secoes().isEmpty(), "obra sem seções: " + o.id());
			helper.assertFalse(o.assuntos().isEmpty(), "obra sem assunto: " + o.id());
			Set<String> ids = new HashSet<>();
			for (Catalogo.Secao s : o.secoes()) {
				helper.assertTrue(ids.add(s.id()), "seção repetida em " + o.id() + ": " + s.id());
				helper.assertTrue(s.tipo() != null && !vazio(s.texto()), "seção sem tipo/texto: " + o.id() + "/" + s.id());
			}
			for (Catalogo.Pergunta p : o.quiz()) {
				helper.assertTrue(perguntas.add(p.id()), "pergunta repetida: " + p.id());
				helper.assertTrue(p.correta() >= 0 && p.correta() < p.opcoes().size(), "resposta fora das opções: " + p.id());
				helper.assertFalse(vazio(p.referencia()), "pergunta sem fonte: " + p.id());
			}
		}
		helper.assertFalse(Catalogo.autores().isEmpty() || Catalogo.series().isEmpty() || Catalogo.assuntos().isEmpty(), "agrupamentos vazios");
		helper.assertFalse(Catalogo.buscar("prece").isEmpty(), "busca por 'prece' deveria achar algo");
		for (String termo : new String[]{"perispirito", "mediunidade", "obsessao", "reencarnacao", "livre_arbitrio", "desobsessao", "agua_fluidificada"}) {
			Glossario.Termo t = Glossario.TERMOS.get(termo);
			helper.assertTrue(t != null && !vazio(t.definicao()) && !vazio(t.referencia()), "glossário sem " + termo);
		}
		helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id("estante_espirita")), "estante não registrada");
		helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id("estante_espirita")) && BuiltInRegistries.ITEM.containsKey(id("catalogo_biblioteca")),
				"itens da biblioteca não registrados");
		helper.assertTrue(NeoForgeRegistries.ATTACHMENT_TYPES.containsKey(id("biblioteca_progresso")), "attachment biblioteca_progresso não registrado");
		helper.succeed();
	}

	private static void direitos(GameTestHelper helper) {
		for (Catalogo.Obra o : Catalogo.OBRAS.values()) {
			helper.assertTrue(o.direitos() != null, "obra sem status de direitos: " + o.id());
			helper.assertFalse(vazio(o.notaDireitos()), "obra sem justificativa de direitos: " + o.id());
			boolean chico = o.psicografia().contains("Xavier");
			if (chico)
				helper.assertTrue(o.direitos() == Catalogo.Direitos.PROTEGIDA, "psicografia de Chico Xavier deveria ser PROTEGIDA: " + o.id());
			if (o.autor().startsWith("Allan Kardec"))
				helper.assertTrue(o.direitos() == Catalogo.Direitos.ORIGINAL_DOMINIO_PUBLICO, "Kardec: original em domínio público: " + o.id());
			for (Catalogo.Secao s : o.secoes()) {
				if (o.direitos() == Catalogo.Direitos.PROTEGIDA)
					helper.assertFalse(s.tipo() == Catalogo.TipoTexto.TRECHO_ORIGINAL, "obra protegida não pode ter trecho do texto: " + o.id() + "/" + s.id());
				if (s.tipo() == Catalogo.TipoTexto.TRECHO_ORIGINAL)
					helper.assertFalse(vazio(s.referencia()), "citação sem referência: " + o.id() + "/" + s.id());
				// Nada de texto longo copiado: cada seção é curta (resumo, trecho ou nota).
				helper.assertTrue(s.texto().length() <= 700, "seção longa demais (texto integral?): " + o.id() + "/" + s.id());
			}
		}
		helper.succeed();
	}

	private static void progresso(GameTestHelper helper) {
		Player p = helper.makeMockPlayer(GameType.SURVIVAL);
		int moral = Moralidade.get(p);
		helper.assertValueEqual(BibliotecaApi.conhecimento(p), 0, "conhecimento inicial");
		helper.assertTrue(BibliotecaApi.registrarLeitura(p, "o_livro_dos_espiritos", "s5"), "leitura válida");
		helper.assertTrue(BibliotecaApi.registrarLeitura(p, "o_livro_dos_espiritos", "s5"), "leitura repetida é aceita");
		helper.assertTrue(BibliotecaApi.registrarLeitura(p, "nosso_lar", "analise"), "leitura de obra protegida (análise do mod)");
		helper.assertFalse(BibliotecaApi.registrarLeitura(p, "obra_que_nao_existe", "s1"), "obra inexistente");
		helper.assertFalse(BibliotecaApi.registrarLeitura(p, "a_genese", "secao_inexistente"), "seção inexistente");
		helper.assertTrue(BibliotecaApi.marcar(p, "a_genese", "s3"), "marcador");
		helper.assertTrue(BibliotecaApi.anotar(p, "a_genese", "Reler o capítulo dos fluidos " + "x".repeat(400)), "nota");
		ProgressoBiblioteca pr = BibliotecaApi.progresso(p);
		helper.assertValueEqual(pr.totalLidas(), 2, "leitura repetida não conta duas vezes");
		helper.assertValueEqual(pr.marcadores().get("o_livro_dos_espiritos"), "s5", "ler move o marcador");
		helper.assertValueEqual(pr.marcadores().get("a_genese"), "s3", "marcador manual");
		helper.assertTrue(pr.notas().get("a_genese").length() <= ProgressoBiblioteca.MAX_NOTA, "nota limitada");
		helper.assertValueEqual(pr.conhecimento(), 2, "conhecimento = seções lidas");
		helper.assertValueEqual(Moralidade.get(p), moral, "ler não mexe na moralidade (conhecimento ≠ moralidade)");

		// Salvo em disco: ida e volta pelo codec do attachment.
		Tag nbt = ProgressoBiblioteca.CODEC.codec().encodeStart(NbtOps.INSTANCE, pr).getOrThrow();
		ProgressoBiblioteca lido = ProgressoBiblioteca.CODEC.codec().parse(NbtOps.INSTANCE, nbt).getOrThrow();
		helper.assertValueEqual(lido, pr, "progresso após salvar/carregar");
		// Save antigo sem o campo: carrega como vazio (compatível).
		ProgressoBiblioteca antigo = ProgressoBiblioteca.CODEC.codec().parse(NbtOps.INSTANCE, new net.minecraft.nbt.CompoundTag()).getOrThrow();
		helper.assertValueEqual(antigo, ProgressoBiblioteca.VAZIO, "save antigo");
		// Attachment do jogador guarda o progresso (e é copiado na morte: copyOnDeath).
		helper.assertTrue(p.hasData(RegistroBiblioteca.PROGRESSO), "attachment presente no jogador");
		helper.succeed();
	}

	private static void quiz(GameTestHelper helper) {
		Player p = helper.makeMockPlayer(GameType.SURVIVAL);
		int moral = Moralidade.get(p);
		Catalogo.Pergunta q = Catalogo.get("o_livro_dos_espiritos").quiz().get(0);
		int errada = (q.correta() + 1) % q.opcoes().size();
		helper.assertFalse(BibliotecaApi.responder(p, q.id(), errada), "resposta errada");
		helper.assertFalse(BibliotecaApi.progresso(p).acertou(q.id()), "errar não registra acerto");
		helper.assertTrue(BibliotecaApi.responder(p, q.id(), q.correta()), "resposta certa");
		helper.assertTrue(BibliotecaApi.progresso(p).acertou(q.id()), "acerto registrado");
		helper.assertTrue(BibliotecaApi.responder(p, q.id(), q.correta()), "responder de novo");
		helper.assertValueEqual(BibliotecaApi.progresso(p).acertos().size(), 1, "acerto conta uma vez só (sem farm)");
		helper.assertValueEqual(BibliotecaApi.progresso(p).tentativas().get(q.id()), 3, "tentativas");
		helper.assertFalse(BibliotecaApi.responder(p, q.id(), 99), "opção inválida");
		helper.assertFalse(BibliotecaApi.responder(p, "nao_existe", 0), "pergunta inexistente");
		// Pergunta de trilha também é encontrada.
		Catalogo.Pergunta t = Trilhas.MODULOS.get("prece").pergunta();
		helper.assertTrue(BibliotecaApi.responder(p, t.id(), t.correta()), "quiz de trilha");
		helper.assertValueEqual(BibliotecaApi.conhecimento(p), 4, "2 pontos por acerto");
		helper.assertValueEqual(Moralidade.get(p), moral, "quiz não mexe na moralidade");
		helper.succeed();
	}

	private static void trilhas(GameTestHelper helper) {
		for (String m : new String[]{"espirito_perispirito_corpo", "reencarnacao", "livre_arbitrio", "causa_efeito", "escala_espirita", "pluralidade_mundos",
				"mediunidade", "obsessao", "prece", "caridade", "perdao", "sono_emancipacao", "desencarnacao", "percepcao_certeza", "nem_toda_mensagem",
				"sem_recompensa", "acolher_sem_humilhar"}) {
			Trilhas.Modulo mod = Trilhas.MODULOS.get(m);
			helper.assertTrue(mod != null, "módulo ausente: " + m);
			helper.assertFalse(vazio(mod.situacao()) || vazio(mod.explicacao()) || vazio(mod.fonte()) || vazio(mod.exemplo()) || vazio(mod.reflexao()),
					"módulo incompleto: " + m);
			helper.assertTrue(Catalogo.get(mod.aprofundamento()) != null, "aprofundamento inexistente: " + m);
			helper.assertTrue(Trilhas.TRILHAS.containsKey(mod.trilha()), "trilha inexistente: " + m);
		}
		// Obsessão: a água fluidificada é apoio, não cura.
		helper.assertTrue(Trilhas.MODULOS.get("obsessao").explicacao().contains("não cura"), "módulo de obsessão deve dizer que a água não cura");
		Player p = helper.makeMockPlayer(GameType.SURVIVAL);
		int moral = Moralidade.get(p);
		helper.assertTrue(BibliotecaApi.concluirModulo(p, "perdao"), "concluir módulo");
		helper.assertTrue(BibliotecaApi.concluirModulo(p, "perdao"), "concluir de novo");
		helper.assertFalse(BibliotecaApi.concluirModulo(p, "nao_existe"), "módulo inexistente");
		helper.assertValueEqual(BibliotecaApi.progresso(p).modulos().size(), 1, "módulo conta uma vez");
		helper.assertValueEqual(BibliotecaApi.conhecimento(p), 3, "3 pontos por módulo");
		helper.assertValueEqual(Moralidade.get(p), moral, "trilha não mexe na moralidade");
		helper.succeed();
	}

	private static void evangelho(GameTestHelper helper) {
		EvangelhoNoLar.limparTudo();
		Player anfitriao = helper.makeMockPlayer(GameType.SURVIVAL);
		Player amigo = helper.makeMockPlayer(GameType.SURVIVAL);
		Player outro = helper.makeMockPlayer(GameType.SURVIVAL);
		int moral = Moralidade.get(anfitriao);
		EvangelhoNoLar.Sessao s = EvangelhoNoLar.iniciar(anfitriao);
		helper.assertTrue(s != null && EvangelhoNoLar.sessaoDe(anfitriao) == s, "sessão iniciada");
		helper.assertTrue(EvangelhoNoLar.sessaoDe(amigo) == null, "ninguém entra sem aceitar");
		EvangelhoNoLar.participar(s, amigo);
		EvangelhoNoLar.participar(s, outro);
		EvangelhoNoLar.sair(outro); // sair no meio: sem penalidade
		helper.assertTrue(EvangelhoNoLar.sessaoDe(outro) == null, "saiu");
		boolean fim = false;
		for (int i = 0; i < EvangelhoNoLar.ETAPAS && !fim; i++)
			fim = EvangelhoNoLar.avancar(s);
		helper.assertTrue(fim, "sessão termina após as etapas");
		helper.assertTrue(EvangelhoNoLar.sessaoDe(anfitriao) == null, "sessão removida");
		helper.assertValueEqual(BibliotecaApi.progresso(anfitriao).evangelhos(), 1, "anfitrião registrou");
		helper.assertValueEqual(BibliotecaApi.progresso(amigo).evangelhos(), 1, "amigo registrou (cooperativo)");
		helper.assertValueEqual(BibliotecaApi.progresso(outro).evangelhos(), 0, "quem saiu não registra, mas não perde nada");
		helper.assertValueEqual(Moralidade.get(anfitriao), moral, "não é ritual de ganho: moralidade igual");
		helper.assertTrue(anfitriao.getActiveEffects().isEmpty(), "nenhum efeito/proteção sobrenatural");
		EvangelhoNoLar.limparTudo();
		helper.succeed();
	}
}
