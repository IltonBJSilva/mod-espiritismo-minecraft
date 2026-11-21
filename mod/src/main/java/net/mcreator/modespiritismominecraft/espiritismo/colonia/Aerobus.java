package net.mcreator.modespiritismominecraft.espiritismo.colonia;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import java.util.UUID;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;

/**
 * O aeróbus de Nosso Lar (cap. 10 do livro: carro aéreo suspenso a uns cinco metros do solo, com paradas pelo
 * caminho; Lísias leva André Luiz nele até o Bosque das Águas). No jogo: estações com um atril de embarque; ao clicar,
 * aparecem os destinos (clicáveis), e a viagem leva alguns segundos com a vista velada antes de chegar à estação de
 * destino. Só viaja para estações cuja construção já terminou. Andar a pé continua sendo o jeito de descobrir o resto.
 */
public final class Aerobus {
	/** Tempo de viagem (ticks). */
	public static final int DURACAO = 50;

	private record Viagem(ServerLevel level, BlockPos destino, String nome, long chegada) {
	}

	private static final Map<UUID, Viagem> EM_VIAGEM = new HashMap<>();

	private Aerobus() {
	}

	/** Lectern de embarque da estação cujo ponto (de pé, à frente do atril) é {@code ponto}. */
	public static BlockPos atrilDe(BlockPos ponto) {
		return ponto.north(2);
	}

	/** A estação (id) cujo atril está em {@code pos}, ou null. */
	public static String estacaoDoAtril(Map<String, Long> pontos, BlockPos pos) {
		for (String id : LayoutColonia.destinosAerobus().keySet()) {
			Long p = pontos.get(id);
			if (p != null && atrilDe(BlockPos.of(p)).equals(pos))
				return id;
		}
		return null;
	}

	/** Estação mais próxima do jogador (até {@code raio} blocos), ou null. */
	public static String estacaoProxima(Map<String, Long> pontos, BlockPos pos, double raio) {
		String melhor = null;
		double d = raio * raio;
		for (String id : LayoutColonia.destinosAerobus().keySet()) {
			Long p = pontos.get(id);
			if (p == null)
				continue;
			double dd = BlockPos.of(p).distSqr(pos);
			if (dd <= d) {
				d = dd;
				melhor = id;
			}
		}
		return melhor;
	}

	/** A estação já está construída (atril no lugar)? */
	public static boolean pronta(ServerLevel level, Map<String, Long> pontos, String id) {
		Long p = pontos.get(id);
		return p != null && level.getBlockState(atrilDe(BlockPos.of(p))).is(Blocks.LECTERN);
	}

	/** Mostra o painel de destinos (componentes clicáveis que rodam /aerobus &lt;destino&gt;). */
	public static void mostrarDestinos(ServerPlayer p, ServerLevel level, Map<String, Long> pontos, String origem) {
		p.sendSystemMessage(Component.literal("— Estação do Aeróbus: " + LayoutColonia.destinosAerobus().get(origem) + " —").withStyle(ChatFormatting.DARK_AQUA));
		p.sendSystemMessage(Component.literal("O aeróbus é um carro aéreo que corre suspenso a uns cinco metros do solo, com paradas pelo caminho (Nosso Lar, cap. 10).")
				.withStyle(ChatFormatting.GRAY));
		MutableComponent linha = Component.literal("Destinos: ").withStyle(ChatFormatting.WHITE);
		boolean algum = false;
		for (var e : LayoutColonia.destinosAerobus().entrySet()) {
			if (e.getKey().equals(origem))
				continue;
			boolean ok = pronta(level, pontos, e.getKey());
			MutableComponent d = Component.literal("[" + e.getValue() + "]");
			if (ok)
				d = d.withStyle(s -> s.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent.RunCommand("/aerobus " + e.getKey()))
						.withHoverEvent(new HoverEvent.ShowText(Component.literal("Embarcar para " + e.getValue()))));
			else
				d = d.withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY).withHoverEvent(new HoverEvent.ShowText(Component.literal("Estação ainda em construção"))));
			linha.append(d).append(" ");
			algum |= ok;
		}
		p.sendSystemMessage(linha);
		if (!algum)
			p.sendSystemMessage(Component.literal("As outras estações ainda estão sendo preparadas. Volte daqui a pouco.").withStyle(ChatFormatting.GRAY));
	}

	/** Começa uma viagem. Devolve uma mensagem de erro, ou null se embarcou. */
	public static String embarcar(ServerPlayer p, ServerLevel level, Map<String, Long> pontos, String destino) {
		if (!LayoutColonia.destinosAerobus().containsKey(destino))
			return "Destino desconhecido: " + destino;
		if (EM_VIAGEM.containsKey(p.getUUID()))
			return "Você já está a caminho.";
		String aqui = estacaoProxima(pontos, p.blockPosition(), 8);
		if (aqui == null)
			return "Para embarcar, vá até uma estação do aeróbus.";
		if (aqui.equals(destino))
			return "Você já está nesta estação.";
		if (!pronta(level, pontos, destino))
			return "A estação de destino ainda está em construção.";
		String nome = LayoutColonia.destinosAerobus().get(destino);
		p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DURACAO + 20, 0, false, false));
		p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, DURACAO + 40, 0, false, false));
		p.sendOverlayMessage(Component.literal("O aeróbus desliza suavemente acima das ruas... próxima parada: " + nome).withStyle(ChatFormatting.AQUA));
		EM_VIAGEM.put(p.getUUID(), new Viagem(level, BlockPos.of(pontos.get(destino)), nome, level.getGameTime() + DURACAO));
		return null;
	}

	static void tick(net.minecraft.server.MinecraftServer server) {
		if (EM_VIAGEM.isEmpty())
			return;
		Iterator<Map.Entry<UUID, Viagem>> it = EM_VIAGEM.entrySet().iterator();
		while (it.hasNext()) {
			var e = it.next();
			Viagem v = e.getValue();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			if (p == null || !p.isAlive() || p.level() != v.level()) {
				it.remove();
				continue;
			}
			if (v.level().getGameTime() < v.chegada())
				continue;
			it.remove();
			chegar(p, v.level(), v.destino(), v.nome());
		}
	}

	static void chegar(ServerPlayer p, ServerLevel level, BlockPos destino, String nome) {
		if (p.isPassenger())
			p.stopRiding();
		p.teleportTo(level, destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5, Set.of(), 180F, 0F, true);
		p.fallDistance = 0;
		p.removeEffect(MobEffects.BLINDNESS);
		p.sendOverlayMessage(Component.literal("Chegada: " + nome).withStyle(ChatFormatting.AQUA));
	}

	/** Viagem imediata (testes). */
	public static void chegarAgora(ServerPlayer p) {
		Viagem v = EM_VIAGEM.remove(p.getUUID());
		if (v != null)
			chegar(p, v.level(), v.destino(), v.nome());
	}
}
