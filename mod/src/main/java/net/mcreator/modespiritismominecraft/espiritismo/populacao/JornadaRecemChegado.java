package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.estado.Estados;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;
import net.mcreator.modespiritismominecraft.espiritismo.Objetivo;
import net.mcreator.modespiritismominecraft.espiritismo.Jornada;

import java.util.Set;
import java.util.List;

/**
 * Jornada do recém-chegado em Nosso Lar (§9). Etapas guardadas em {@link Memoria} ({@value #ETAPA}):
 * <pre>
 * 0 RECEPCAO     fale com quem recebe (Ministro Clarêncio ou um orientador)          → conquista jornada/nl_recepcao
 * 1 ACOLHIMENTO  fale com Lísias (ou Henrique de Luna / um enfermeiro)                → jornada/nl_acolhimento
 * 2 MINISTERIOS  visite 3 dos 6 Ministérios (ou ouça um mensageiro, se ainda não há) → jornada/nl_ministerios
 * 3 BIBLIOTECA   vá à biblioteca, fale com um bibliotecário ou leia uma obra na colônia → jornada/nl_biblioteca
 * 4 MENTOR       converse com um mentor (Clarêncio, orientador ou Espírito protetor) → jornada/nl_mentor
 * 5 ESCOLHA      escolha estudo (instrutor/bibliotecário) ou trabalho (enfermagem, jardins, oficinas)
 *                — trabalho de ASSISTÊNCIA exige estar estável (paciente não cuida de paciente) → jornada/nl_escolha
 * 6 RECUPERACAO  depois de pelo menos 1 dia de jogo desde o acolhimento, fale com Lísias/Laura → jornada/nl_recuperacao
 * 7 CONCLUIDA
 * </pre>
 * As recompensas são conhecimento e acesso (conquistas, a obra Nosso Lar) — não moralidade.
 */
public final class JornadaRecemChegado {
	private JornadaRecemChegado() {
	}

	public static final String ETAPA = "rc_etapa";
	public static final String INICIO = "rc_inicio";

	public static final int RECEPCAO = 0, ACOLHIMENTO = 1, MINISTERIOS = 2, BIBLIOTECA = 3, MENTOR = 4, ESCOLHA = 5, RECUPERACAO = 6, CONCLUIDA = 7;

	/** Tempo mínimo de recuperação (ticks de jogo) entre o acolhimento e a conclusão: 1 dia de jogo. */
	public static final long TEMPO_RECUPERACAO = 24000L;
	public static final int MINISTERIOS_NECESSARIOS = 3;

	/** Conquistas (ids em data/mod_espiritismo_minecraft/advancement/jornada/<id>.json, criados pelo integrador). */
	public static final List<String> CONQUISTAS = List.of("nl_recepcao", "nl_acolhimento", "nl_ministerios", "nl_biblioteca", "nl_mentor", "nl_escolha", "nl_recuperacao");

	public static final List<String> OBJETIVOS = List.of(
			"Procure quem recebe os recém-chegados (Ministro Clarêncio, no Ministério do Auxílio, ou um orientador)",
			"Converse com Lísias ou outro enfermeiro sobre o acolhimento",
			"Conheça pelo menos 3 dos 6 Ministérios da colônia",
			"Conheça a biblioteca (ou estude uma obra aqui na colônia)",
			"Converse com um mentor sobre o que você viu",
			"Escolha uma atividade: estudo (instrutores, bibliotecários) ou trabalho (enfermagem, jardins, oficinas)",
			"Dê tempo ao tempo: depois de descansar, volte a conversar com Lísias ou com a senhora Laura",
			"");

	private static final Set<String> RECEBEM = Set.of("clarencio");
	private static final Set<String> ACOLHEM = Set.of("lisias", "henrique_de_luna");
	private static final Set<String> CONCLUEM = Set.of("lisias", "laura", "henrique_de_luna", "clarencio");

	public static int etapa(Player p) {
		return (int) Memoria.de(p).valor(ETAPA, RECEPCAO);
	}

	private static boolean emNossoLar(Player p) {
		return p.level().dimension() == Planos.NOSSO_LAR;
	}

	/** Avança da etapa {@code de} (só se o jogador estiver nela). */
	public static boolean avancar(Player p, int de) {
		if (p.level().isClientSide() || etapa(p) != de || de >= CONCLUIDA)
			return false;
		Memoria.definir(p, ETAPA, de + 1);
		Jornada.conquista(p, CONQUISTAS.get(de));
		if (p.level() instanceof ServerLevel sl)
			sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.5F);
		if (de + 1 < CONCLUIDA) {
			Dialogos.objetivo(p, "Jornada do recém-chegado", OBJETIVOS.get(de + 1));
			apontar(p);
		} else {
			Dialogos.objetivo(p, "Jornada do recém-chegado", "concluída. Agora você é morador de Nosso Lar; continue estudando e servindo no seu ritmo.");
			Objetivo.limpar(p);
		}
		return true;
	}

	/** Seta do HUD para o próximo lugar da jornada (quando houver). */
	public static void apontar(Player p) {
		if (!(p.level() instanceof ServerLevel sl) || !emNossoLar(p))
			return;
		String zona = switch (etapa(p)) {
			case ACOLHIMENTO -> "hospital";
			case BIBLIOTECA -> "biblioteca";
			case MENTOR, RECEPCAO -> "auxilio";
			case RECUPERACAO -> "residencias";
			default -> null;
		};
		BlockPos pos = zona == null ? null : Zonas.posicao(sl, zona);
		if (pos != null)
			Objetivo.definir(p, pos);
	}

	// ---------------------------------------------------------------- Conversas

	/** @return true se a conversa foi usada pela jornada. */
	public static boolean conversar(Player p, MoradorEntity m, Perfil perfil, Perfil.Contexto ctx) {
		if (!ctx.nossoLar())
			return false;
		String id = perfil.id();
		TipoNpc tipo = perfil.tipo();
		switch (etapa(p)) {
			case RECEPCAO -> {
				if (RECEBEM.contains(id) || tipo == TipoNpc.ORIENTADOR) {
					Dialogos.falar(p, perfil, "Você chegou a Nosso Lar. Aqui será acolhido e tratado, sem pressa e sem cobrança. Procure Lísias, ou qualquer enfermeiro do hospital: ele explicará como funciona o acolhimento.");
					return avancar(p, RECEPCAO);
				}
			}
			case ACOLHIMENTO -> {
				if (ACOLHEM.contains(id) || tipo == TipoNpc.ENFERMEIRO) {
					Dialogos.falar(p, perfil, "O acolhimento é simples: descanso, alimentação leve, prece e conversa. Ninguém lhe cobra nada agora. Quando se sentir mais firme, conheça os Ministérios para entender como a colônia vive.");
					Memoria.definir(p, INICIO, p.level().getGameTime());
					return avancar(p, ACOLHIMENTO);
				}
			}
			case MINISTERIOS -> {
				if (p.level() instanceof ServerLevel sl && tipo == TipoNpc.MENSAGEIRO && ministeriosExistentes(sl) < MINISTERIOS_NECESSARIOS) {
					// A colônia ainda não tem os Ministérios construídos: o mensageiro descreve-os (Nosso Lar, cap. 8).
					Dialogos.falar(p, perfil, "Eu passo por todos os Ministérios todos os dias! Regeneração e Auxílio acolhem e tratam; Comunicação liga os setores; Esclarecimento cuida do estudo; Elevação e União Divina, das atividades mais elevadas. Um dia você os verá de perto.");
					return avancar(p, MINISTERIOS);
				}
				if (RECEBEM.contains(id) || tipo == TipoNpc.ORIENTADOR || tipo == TipoNpc.MENSAGEIRO) {
					Memoria mem = Memoria.de(p);
					StringBuilder faltam = new StringBuilder();
					for (String min : Zonas.MINISTERIOS)
						if (!mem.tem("visitou:" + min))
							faltam.append(faltam.isEmpty() ? "" : ", ").append(Zonas.nome(min));
					Dialogos.falar(p, perfil, "Você já conheceu " + visitados(p) + " de " + MINISTERIOS_NECESSARIOS + " Ministérios. Ainda pode visitar: " + faltam + ".");
					return true;
				}
			}
			case BIBLIOTECA -> {
				if (tipo == TipoNpc.BIBLIOTECARIO) {
					Dialogos.falar(p, perfil, "Bem-vindo à biblioteca! Aqui cada obra traz autor e fonte. Comece pelo que despertar sua curiosidade, e pergunte sempre.");
					return avancar(p, BIBLIOTECA);
				}
			}
			case MENTOR -> {
				if (RECEBEM.contains(id) || tipo == TipoNpc.ORIENTADOR) {
					Dialogos.falar(p, perfil, "Você já viu bastante coisa. Não precisa entender tudo de uma vez. Agora escolha como quer ocupar o seu tempo: estudando, com os instrutores e bibliotecários, ou trabalhando, nos jardins, nas oficinas ou, quando estiver firme, na enfermagem.");
					return avancar(p, MENTOR);
				}
			}
			case ESCOLHA -> {
				String escolha = tipo.escolha;
				if (escolha == null && ("narcisa".equals(id) || "tobias".equals(id)))
					escolha = "trabalho";
				if (escolha == null)
					return false;
				if (tipo.assistencia || "narcisa".equals(id) || "tobias".equals(id)) {
					if (!Estados.podeSocorrer(p)) {
						Dialogos.falar(p, perfil, "Ainda não. Quem ainda está em tratamento não assume o cuidado de outros pacientes: primeiro você se firma. Que tal começar pelo estudo, na biblioteca, ou pelos jardins?");
						return true;
					}
				}
				Memoria.marcar(p, "escolha:" + escolha);
				Dialogos.falar(p, perfil, escolha.equals("estudo")
						? "Que bom! Venha às aulas e às leituras quando quiser. Estudar com calma também é parte da recuperação."
						: "Seja bem-vindo ao serviço. Comece devagar, observando; o trabalho aqui é oportunidade, não peso.");
				return avancar(p, ESCOLHA);
			}
			case RECUPERACAO -> {
				if (CONCLUEM.contains(id) || tipo == TipoNpc.ENFERMEIRO) {
					long passou = p.level().getGameTime() - Memoria.de(p).valor(INICIO, p.level().getGameTime());
					if (passou < TEMPO_RECUPERACAO) {
						Dialogos.falar(p, perfil, "Ainda é cedo. A recuperação não é instantânea: descanse, continue sua atividade e volte a conversar comigo mais tarde.");
						return true;
					}
					Dialogos.falar(p, perfil, "Você já não é o mesmo que chegou aqui. Ainda há muito a aprender, mas agora caminha com as próprias pernas. Seja bem-vindo, de verdade, a Nosso Lar.");
					darObraNossoLar(p);
					return avancar(p, RECUPERACAO);
				}
			}
			default -> {
			}
		}
		return false;
	}

	private static void darObraNossoLar(Player p) {
		if (!Memoria.marcar(p, "recebeu:obra_nosso_lar"))
			return;
		// Lookup por ID (a obra é da área "biblioteca/itens"; o item já traz o conteúdo como componente padrão).
		Item item = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(RegistroPopulacao.MODID, "nosso_lar")).orElse(null);
		if (item == null)
			return;
		ItemStack obra = new ItemStack(item);
		if (!p.getInventory().add(obra))
			p.drop(obra, false);
	}

	// ---------------------------------------------------------------- Visitas, leitura e mentor (eventos)

	public static int visitados(Player p) {
		int n = 0;
		for (String min : Zonas.MINISTERIOS)
			if (Memoria.tem(p, "visitou:" + min))
				n++;
		return n;
	}

	public static int ministeriosExistentes(ServerLevel level) {
		int n = 0;
		for (String min : Zonas.MINISTERIOS)
			if (Zonas.real(level, min) != null)
				n++;
		return n;
	}

	/** A cada ~2 s em Nosso Lar: registra Ministérios visitados (pontos REAIS da colônia) e a biblioteca. */
	public static void tick(Player p) {
		if (!(p.level() instanceof ServerLevel sl) || sl.dimension() != Planos.NOSSO_LAR)
			return;
		for (String zona : Zonas.MINISTERIOS) {
			BlockPos c = Zonas.real(sl, zona);
			if (c != null && perto(p, c, 16) && Memoria.marcar(p, "visitou:" + zona))
				Dialogos.narrar(p, "Você está no " + Zonas.nome(zona) + ".");
		}
		int e = etapa(p);
		if (e == MINISTERIOS && visitados(p) >= Math.min(MINISTERIOS_NECESSARIOS, Math.max(1, ministeriosExistentes(sl))) && ministeriosExistentes(sl) > 0)
			avancar(p, MINISTERIOS);
		else if (e == BIBLIOTECA) {
			BlockPos b = Zonas.real(sl, "biblioteca");
			if (b != null && perto(p, b, 10))
				avancar(p, BIBLIOTECA);
		}
	}

	private static boolean perto(Player p, BlockPos c, int raio) {
		double dx = p.getX() - (c.getX() + 0.5), dz = p.getZ() - (c.getZ() + 0.5);
		return dx * dx + dz * dz <= raio * raio && Math.abs(p.getY() - c.getY()) < 12;
	}

	/** Leu uma obra: na etapa da biblioteca, estudar dentro da colônia também vale. */
	public static void aoLer(Player p) {
		if (emNossoLar(p) && etapa(p) == BIBLIOTECA)
			avancar(p, BIBLIOTECA);
	}

	/** Conversou com um Espírito protetor (mentor) da área "npc". */
	public static void conversouComMentor(Player p) {
		if (emNossoLar(p) && etapa(p) == MENTOR) {
			Dialogos.narrar(p, "O mentor ouve você em silêncio e o anima a escolher uma atividade de estudo ou de trabalho.");
			avancar(p, MENTOR);
		}
	}
}
