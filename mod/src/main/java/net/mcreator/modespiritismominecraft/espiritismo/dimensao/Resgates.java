package net.mcreator.modespiritismominecraft.espiritismo.dimensao;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.estado.Estados;
import net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual;
import net.mcreator.modespiritismominecraft.espiritismo.npc.EspiritoEntity;
import net.mcreator.modespiritismominecraft.espiritismo.Objetivo;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.espiritismo.Jornada;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Resgate COM CONSENTIMENTO (PROMPT §5.4). Fluxo:
 * <ol>
 * <li>{@link #oferecer}: socorristas surgem a ~14 blocos e se aproximam calmamente (fase APROXIMANDO).</li>
 * <li>{@link #chegou}: ao lado do jogador, explicam e abrem as escolhas (fase OFERTA): tela no cliente
 * ({@link PacotesUmbral.AbrirResgate}) + botões clicáveis no chat + comando {@code /espiritismo_socorro}.</li>
 * <li>ACEITAR → fase ACOMPANHANDO (estado EM_RESGATE) por alguns segundos e {@link #concluir}: o jogador é levado ao
 * Posto de Socorro mais próximo (estado EM_ACOLHIMENTO). CONVERSAR → explicam mais e aliviam a perturbação; a
 * oferta continua. PEDIR TEMPO → afastam-se e voltam depois (recarga curta). RECUSAR → respeitado; indicam a rota
 * do posto e só voltam após uma recarga maior. Nenhuma penalidade de moralidade.</li>
 * </ol>
 * Nunca há teleporte sem o "Aceitar". O jogador pode pedir ajuda a qualquer momento ({@link #pedirAjuda}).
 * Os socorristas são temporários: removidos ao concluir/cancelar e, por segurança, também têm tempo de vida e
 * somem se o jogador sair (EspiritoEntity.acompanhar).
 */
public final class Resgates {
	public static final int NENHUM = 0, APROXIMANDO = 1, OFERTA = 2, ACOMPANHANDO = 3;
	public static final int TEMPO_APROXIMACAO = 20 * 20;
	public static final int TEMPO_OFERTA = 60 * 20;
	public static final int TEMPO_ACOMPANHAR = 5 * 20;
	public static final int RECARGA_TEMPO = 2 * 60 * 20;
	public static final int RECARGA_RECUSA = 5 * 60 * 20;
	public static final int RECARGA_PEDIDO = 10 * 20;
	public static final String CHAVE_VOLTA = "umb_resgate_volta";
	public static final String TAG_TEMP = "espiritismo_socorro_temp";

	public enum Escolha {
		ACEITAR, CONVERSAR, PEDIR_TEMPO, RECUSAR;

		public static Escolha porIndice(int i) {
			Escolha[] v = values();
			return i >= 0 && i < v.length ? v[i] : PEDIR_TEMPO;
		}
	}

	/** Socorristas temporários de cada jogador (só UUIDs: nenhuma referência eterna a entidades). */
	private static final Map<UUID, List<UUID>> SOCORRISTAS = new HashMap<>();

	private Resgates() {
	}

	public static int fase(ServerPlayer p) {
		return p.getData(RegistroDimensao.RESGATE_FASE);
	}

	private static void fase(ServerPlayer p, int f, int duracao) {
		p.setData(RegistroDimensao.RESGATE_FASE, f);
		p.setData(RegistroDimensao.RESGATE_ATE, f == NENHUM ? 0L : p.level().getGameTime() + duracao);
		// Compatibilidade: RESGATE_EM > 0 significa "há socorro a caminho/em curso".
		p.setData(RegistroDimensao.RESGATE_EM, f == NENHUM ? 0L : p.level().getGameTime());
	}

	/** O jogador está numa situação em que o socorro do Umbral se aplica? (por estado, não por dimensão) */
	public static boolean precisaDeSocorro(ServerPlayer p) {
		EstadoEspiritual e = Estados.get(p);
		return e == EstadoEspiritual.NO_UMBRAL_PERTURBADO || e == EstadoEspiritual.NO_UMBRAL_ORIENTADO;
	}

	// ================================================================ Início

	/** Socorristas vêm oferecer ajuda (sem teleportar). Devolve false se já há um resgate em curso ou não se aplica. */
	public static boolean oferecer(ServerPlayer p) {
		if (fase(p) != NENHUM || !precisaDeSocorro(p))
			return false;
		fase(p, APROXIMANDO, TEMPO_APROXIMACAO);
		criarSocorristas(p);
		msg(p, ChatFormatting.GOLD, "Uma luz suave se aproxima pela névoa, devagar: são socorristas. Eles vêm em paz e não o levarão sem o seu consentimento.");
		return true;
	}

	/** Pedido de ajuda do jogador (tecla H, comando, interação). Ignora a recarga de quem recusou antes. */
	public static void pedirAjuda(ServerPlayer p) {
		int f = fase(p);
		if (f == OFERTA) {
			abrirEscolhas(p, "Estamos aqui. O que você prefere?");
			return;
		}
		if (f != NENHUM) {
			p.sendOverlayMessage(Component.literal("O socorro já está a caminho.").withStyle(ChatFormatting.AQUA));
			return;
		}
		if (Planos.naZonaDeTransicao(p)) {
			Planos.mensageiroDeVolta(p);
			return;
		}
		if (!precisaDeSocorro(p)) {
			msg(p, ChatFormatting.GRAY, "Você está amparado neste momento. Se algo pesar, converse com um mentor ou socorrista, ou ore (tecla B).");
			return;
		}
		if (!Moralidade.pronto(p, "umb_pedido_ajuda", RECARGA_PEDIDO)) {
			p.sendOverlayMessage(Component.literal("Seu pedido foi ouvido. Aguarde um instante.").withStyle(ChatFormatting.AQUA));
			return;
		}
		Moralidade.limparRecarga(p, CHAVE_VOLTA);
		msg(p, ChatFormatting.AQUA, "Você pede ajuda com sinceridade. Pedir ajuda é um ato de coragem, não de fraqueza.");
		oferecer(p);
	}

	/** Os socorristas chegaram ao lado do jogador: explicam e abrem as escolhas. */
	public static void chegou(ServerPlayer p) {
		if (fase(p) != APROXIMANDO)
			return;
		fase(p, OFERTA, TEMPO_OFERTA);
		abrirEscolhas(p, "— Irmão, viemos em paz. Podemos levá-lo a um Posto de Socorro, onde há repouso, conversa e cuidado. Você decide: ninguém será levado à força.");
	}

	private static void abrirEscolhas(ServerPlayer p, String fala) {
		msg(p, ChatFormatting.GREEN, "Socorrista: " + fala);
		MutableComponent linha = Component.literal("Escolha: ").withStyle(ChatFormatting.GRAY);
		linha.append(botao("[Aceitar ajuda]", "aceitar", "Ser acompanhado até o Posto de Socorro", ChatFormatting.GREEN)).append(" ");
		linha.append(botao("[Conversar primeiro]", "conversar", "Ouvir mais antes de decidir", ChatFormatting.AQUA)).append(" ");
		linha.append(botao("[Pedir tempo]", "tempo", "Eles se afastam e voltam mais tarde", ChatFormatting.YELLOW)).append(" ");
		linha.append(botao("[Recusar]", "recusar", "Sua escolha será respeitada", ChatFormatting.RED));
		p.sendSystemMessage(linha);
		PacotesUmbral.enviar(p, new PacotesUmbral.AbrirResgate(fala));
	}

	private static Component botao(String texto, String sub, String dica, ChatFormatting cor) {
		return Component.literal(texto).withStyle(Style.EMPTY.withColor(cor).withClickEvent(new ClickEvent.RunCommand("/espiritismo_socorro " + sub))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(dica))));
	}

	// ================================================================ Escolhas

	public static void escolher(ServerPlayer p, Escolha e) {
		if (fase(p) != OFERTA) {
			if (e == Escolha.ACEITAR)
				pedirAjuda(p); // mudou de ideia depois: o socorro vem (e pergunta de novo)
			return;
		}
		switch (e) {
			case ACEITAR -> {
				if (!Estados.transicionar(p, EstadoEspiritual.EM_RESGATE, "aceitou o resgate")) {
					cancelar(p, 0);
					return;
				}
				fase(p, ACOMPANHANDO, TEMPO_ACOMPANHAR);
				msg(p, ChatFormatting.GREEN, "Socorrista: — Segure minha mão. Vamos juntos, sem pressa.");
				p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, TEMPO_ACOMPANHAR, 1, false, false));
			}
			case CONVERSAR -> {
				if (!Moralidade.pronto(p, "umb_conversa_resgate", 20 * 15)) {
					abrirEscolhas(p, "Estamos ouvindo. Leve o tempo que precisar.");
					return;
				}
				Perturbacao.aliviar(p, Perturbacao.ALIVIO_CONVERSA);
				p.setData(RegistroDimensao.RESGATE_ATE, p.level().getGameTime() + TEMPO_OFERTA);
				msg(p, ChatFormatting.GREEN, "Socorrista: — O que você sente agora é passageiro. Muitos, ao deixar o corpo, ficam confusos por um tempo; isso não é castigo, é reflexo do estado íntimo, e muda com a boa vontade.");
				msg(p, ChatFormatting.GREEN, "Socorrista: — No posto há leitos, água, conversa e estudo. Você poderá sair quando quiser e, quando estiver sereno, seguir para Nosso Lar.");
				abrirEscolhas(p, "Quer vir conosco?");
			}
			case PEDIR_TEMPO -> {
				cancelar(p, RECARGA_TEMPO);
				msg(p, ChatFormatting.GREEN, "Socorrista: — Tudo bem. Voltaremos em breve. Se precisar antes, é só chamar (tecla H).");
			}
			case RECUSAR -> {
				cancelar(p, RECARGA_RECUSA);
				Perturbacao.agravar(p, 5, Perturbacao.TETO_RECUSA);
				indicarRota(p);
				msg(p, ChatFormatting.GREEN, "Socorrista: — Respeitamos a sua escolha. O caminho do posto fica indicado na sua seta. Quando quiser, chame: viremos.");
				msg(p, ChatFormatting.GRAY, "Sem o amparo, a névoa pesa um pouco mais — mas nada é definitivo. Pedir ajuda: tecla H ou /espiritismo_socorro pedir.");
			}
		}
	}

	/** Encerra a oferta sem levar o jogador. {@code recarga} = ticks até os socorristas voltarem por iniciativa própria. */
	public static void cancelar(ServerPlayer p, int recarga) {
		removerSocorristas(p);
		fase(p, NENHUM, 0);
		if (recarga > 0) {
			Moralidade.limparRecarga(p, CHAVE_VOLTA);
			Moralidade.pronto(p, CHAVE_VOLTA, recarga);
		}
	}

	private static void indicarRota(ServerPlayer p) {
		if (p.level() instanceof ServerLevel level) {
			int i = ConstrucoesUmbral.maisProximo(level, p.blockPosition());
			if (i >= 0)
				Objetivo.definir(p, BlockPos.of(Planos.pontos(level).get("posto_" + i + "_porta")));
		}
	}

	/** Fim do resgate aceito: leva ao Posto de Socorro mais próximo (se no Umbral) e inicia o acolhimento. Idempotente. */
	public static void concluir(ServerPlayer p) {
		if (Estados.get(p) != EstadoEspiritual.EM_RESGATE) {
			fase(p, NENHUM, 0);
			removerSocorristas(p);
			return;
		}
		removerSocorristas(p);
		fase(p, NENHUM, 0);
		MinecraftServer server = p.level().getServer();
		ServerLevel umbral = server.getLevel(Planos.UMBRAL);
		if (umbral != null && p.level() == umbral) {
			int i = ConstrucoesUmbral.maisProximo(umbral, p.blockPosition());
			BlockPos destino = i >= 0 ? BlockPos.of(Planos.pontos(umbral).get("posto_" + i + "_chegada")) : ConstrucoesUmbral.garantir(umbral);
			Planos.teleportar(p, umbral, destino);
			Objetivo.definir(p, BlockPos.of(Planos.pontos(umbral).getOrDefault("posto_" + Math.max(0, i) + "_conversa", destino.asLong())));
		}
		Estados.transicionar(p, EstadoEspiritual.EM_ACOLHIMENTO, "chegou ao posto");
		Perturbacao.set(p, Math.min(Perturbacao.get(p), 20));
		p.removeEffect(MobEffects.DARKNESS);
		p.removeEffect(MobEffects.SLOWNESS);
		p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
		Jornada.conquista(p, "resgate");
		msg(p, ChatFormatting.AQUA, "Você chega ao Posto de Socorro. Mãos amigas o acolhem na triagem.");
		msg(p, ChatFormatting.GRAY,
				"Aqui você pode repousar (clique num leito), conversar e estudar (atril do ponto de conversa) e orar. Quando estiver sereno, a saída assistida o levará a Nosso Lar.");
	}

	// ================================================================ Tick (a cada 2 ticks para o movimento; lógica a cada segundo)

	public static void tick(ServerPlayer p) {
		int f = fase(p);
		if (f == NENHUM)
			return;
		long agora = p.level().getGameTime();
		long ate = p.getData(RegistroDimensao.RESGATE_ATE);
		switch (f) {
			case APROXIMANDO -> {
				boolean perto = aproximar(p);
				if (perto || agora >= ate)
					chegou(p);
			}
			case OFERTA -> {
				aproximar(p);
				if (agora >= ate) {
					cancelar(p, RECARGA_TEMPO);
					msg(p, ChatFormatting.GREEN, "Socorrista: — Vamos deixá-lo pensar com calma. Voltaremos. Chame quando quiser (tecla H).");
				}
			}
			case ACOMPANHANDO -> {
				aproximar(p);
				if (agora >= ate)
					concluir(p);
			}
			default -> fase(p, NENHUM, 0);
		}
	}

	/** Chamado a cada segundo para quem está no Umbral: os socorristas vêm por iniciativa própria quando apropriado. */
	public static void avaliar(ServerPlayer p) {
		if (fase(p) != NENHUM || Estados.get(p) != EstadoEspiritual.NO_UMBRAL_PERTURBADO)
			return;
		if (p.level() instanceof ServerLevel level && ConstrucoesUmbral.distanciaPosto(level, p.blockPosition()) < 24)
			return; // já está no posto
		if (Moralidade.restante(p, CHAVE_VOLTA) > 0)
			return;
		boolean liberado = Planos.resgateLiberado(Moralidade.get(p), p.getData(RegistroDimensao.ARREPENDIMENTO)) || Estados.tempoNoEstado(p) >= 60 * 20;
		if (liberado)
			oferecer(p);
	}

	// ================================================================ Socorristas temporários

	private static void criarSocorristas(ServerPlayer p) {
		removerSocorristas(p);
		EntityType<?> tipo = ConstrucoesUmbral.tipo("socorrista");
		if (tipo == null || !(p.level() instanceof ServerLevel level))
			return;
		List<UUID> ids = new ArrayList<>();
		Vec3 olhar = Vec3.directionFromRotation(0, p.getYRot());
		for (int k = 0; k < 2; k++) {
			Vec3 pos = p.position().add(olhar.scale(14 + k * 2)).add(k == 0 ? 0 : 2, 0.5, 0);
			Entity e = tipo.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
			if (e == null)
				continue;
			e.snapTo(pos.x, pos.y, pos.z, p.getYRot() + 180F, 0F);
			e.addTag(TAG_TEMP);
			e.setCustomName(Component.literal(k == 0 ? "Socorrista" : "Socorrista — acompanhante").withStyle(ChatFormatting.GREEN));
			if (e instanceof EspiritoEntity esp)
				esp.acompanhar(p, TEMPO_APROXIMACAO + TEMPO_OFERTA + TEMPO_ACOMPANHAR + 200); // some sozinho se algo falhar
			level.addFreshEntity(e);
			level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, 12, 0.3, 0.6, 0.3, 0.01);
			ids.add(e.getUUID());
		}
		SOCORRISTAS.put(p.getUUID(), ids);
	}

	/** Move os socorristas devagar em direção ao jogador. Devolve true quando o primeiro está a ≤ 3 blocos. */
	private static boolean aproximar(ServerPlayer p) {
		List<UUID> ids = SOCORRISTAS.get(p.getUUID());
		if (ids == null || ids.isEmpty() || !(p.level() instanceof ServerLevel level))
			return true;
		boolean perto = false;
		for (int k = 0; k < ids.size(); k++) {
			Entity e = level.getEntity(ids.get(k));
			if (e == null || e.isRemoved())
				continue;
			Vec3 alvo = p.position().add(k == 0 ? 0 : 1.5, 0.3, 0);
			Vec3 dif = alvo.subtract(e.position());
			double d = dif.length();
			if (d > 2.5 + k) {
				Vec3 passo = dif.normalize().scale(Math.min(0.25, d - 2.5));
				e.setPos(e.getX() + passo.x, e.getY() + passo.y, e.getZ() + passo.z);
			}
			float yaw = (float) Math.toDegrees(Math.atan2(-dif.x, dif.z));
			e.setYRot(yaw);
			e.setYHeadRot(yaw);
			if (k == 0 && d <= 3.0)
				perto = true;
		}
		return perto;
	}

	/** Remove os socorristas temporários deste jogador (em qualquer Level). */
	public static void removerSocorristas(ServerPlayer p) {
		List<UUID> ids = SOCORRISTAS.remove(p.getUUID());
		if (ids == null)
			return;
		MinecraftServer server = p.level().getServer();
		for (ServerLevel l : server.getAllLevels())
			for (UUID id : ids) {
				Entity e = l.getEntity(id);
				if (e != null) {
					if (e instanceof EspiritoEntity esp)
						esp.sumir(l);
					else
						e.discard();
				}
			}
	}

	/** Quantos socorristas temporários este jogador tem vivos agora (testes/depuração). */
	public static int socorristasVivos(ServerPlayer p) {
		List<UUID> ids = SOCORRISTAS.get(p.getUUID());
		if (ids == null)
			return 0;
		int n = 0;
		for (UUID id : ids) {
			Entity e = ((ServerLevel) p.level()).getEntity(id);
			if (e != null && !e.isRemoved())
				n++;
		}
		return n;
	}

	/** Remove socorristas temporários órfãos (com a etiqueta) em todos os Levels carregados. Comando de admin. */
	public static int limparOrfaos(MinecraftServer server) {
		int n = 0;
		for (ServerLevel l : server.getAllLevels())
			for (Entity e : l.getAllEntities())
				if (e.entityTags().contains(TAG_TEMP)) {
					e.discard();
					n++;
				}
		SOCORRISTAS.clear();
		return n;
	}

	// ================================================================ Conexão

	/** Saída do jogo: remove os socorristas (o estado fica salvo). */
	static void aoSair(ServerPlayer p) {
		removerSocorristas(p);
	}

	/** Reconexão: restaura sem softlock. Resgate já aceito é concluído; ofertas abertas são encerradas (voltam logo). */
	public static void aoReconectar(ServerPlayer p) {
		int f = fase(p);
		if (Estados.get(p) == EstadoEspiritual.EM_RESGATE || f == ACOMPANHANDO) {
			if (Estados.get(p) != EstadoEspiritual.EM_RESGATE)
				Estados.forcar(p, EstadoEspiritual.EM_RESGATE, "reconexão durante resgate aceito");
			concluir(p);
			return;
		}
		if (f != NENHUM)
			cancelar(p, 20 * 20);
	}

	static void msg(ServerPlayer p, ChatFormatting cor, String texto) {
		p.sendSystemMessage(Component.literal(texto).withStyle(cor));
	}
}
