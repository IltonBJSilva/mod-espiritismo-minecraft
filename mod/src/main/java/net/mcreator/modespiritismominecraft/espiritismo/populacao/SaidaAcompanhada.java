package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import net.mcreator.modespiritismominecraft.espiritismo.estado.Estados;
import net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tópico "servir fora da colônia" (PROMPT §4.2): o Ministro Clarêncio e os mensageiros oferecem uma saída
 * ACOMPANHADA para serviço no Umbral a quem está em condições de socorrer. O jogador fala com o NPC agachado; na
 * primeira vez recebe a oferta e, falando de novo agachado em até {@value #JANELA_TICKS} ticks, confirma. Então
 * {@link Planos#autorizarSaida} libera a travessia por um minuto.
 */
public final class SaidaAcompanhada {
	private SaidaAcompanhada() {
	}

	public static final int DURACAO = 20 * 60;
	public static final int JANELA_TICKS = 20 * 30;

	/** Oferta pendente por jogador (memória volátil: não precisa sobreviver a um reinício). */
	private static final Map<UUID, Long> OFERTAS = new ConcurrentHashMap<>();

	/** Este personagem oferece a saída acompanhada? */
	public static boolean oferece(Perfil p) {
		return p.tipo() == TipoNpc.MENSAGEIRO || "clarencio".equals(p.id());
	}

	/** Pode receber a oferta agora? (Nosso Lar, fora do sonho, em condições de socorrer) */
	public static boolean podeReceber(Player player) {
		return player.level().dimension() == Planos.NOSSO_LAR && Estados.get(player) != EstadoEspiritual.EM_VISITA_ASSISTIDA && Estados.podeSocorrer(player);
	}

	/**
	 * Trata a conversa agachada com quem oferece a saída. Devolve true se o tópico foi tratado (a conversa comum não
	 * segue).
	 */
	public static boolean conversar(Player player, Perfil p) {
		if (!(player instanceof ServerPlayer sp) || !player.isShiftKeyDown() || !oferece(p) || player.level().dimension() != Planos.NOSSO_LAR)
			return false;
		if (Estados.get(player) == EstadoEspiritual.EM_VISITA_ASSISTIDA) {
			Dialogos.falar(player, p, "Você nos visita durante o sono do corpo. Nesta condição não se vai além da colônia; aproveite para aprender e repousar.");
			return true;
		}
		if (!Estados.podeSocorrer(player)) {
			Dialogos.falar(player, p, Estados.MSG_PRECISA_CUIDADO + " O serviço fora da colônia fica para quando você estiver firme.");
			return true;
		}
		long agora = player.level().getGameTime();
		Long oferta = OFERTAS.get(player.getUUID());
		if (oferta != null && agora - oferta <= JANELA_TICKS) {
			OFERTAS.remove(player.getUUID());
			Dialogos.falar(player, p, "Combinado. Esta é uma saída ACOMPANHADA, para serviço: siga além da muralha e um de nós irá com você até o Posto de Socorro do Umbral. Lá, ajude como puder, sem pretender substituir os socorristas.");
			Planos.autorizarSaida(sp, DURACAO);
			return true;
		}
		OFERTAS.put(player.getUUID(), agora);
		Dialogos.falar(player, p, "Vejo que você está sereno e disposto a servir. Há trabalho no Posto de Socorro do Umbral, mas ninguém sai daqui sozinho: posso acompanhá-lo.");
		Dialogos.narrar(player, "Para aceitar a saída acompanhada, fale de novo agachado nos próximos 30 segundos. Depois terá 1 minuto para atravessar a muralha.");
		return true;
	}
}
