package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.minecraft.world.entity.player.Player;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.estado.Estados;
import net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;

import javax.annotation.Nullable;

import java.util.List;
import java.util.ArrayList;

/**
 * Diálogos contextuais (§8.2): o NPC considera dimensão, estado do jogador, se já se conheceram, o que já explicou,
 * livros lidos e o andamento das jornadas. Variação sem repetir a fala anterior; explicações dadas uma só vez.
 * <p>
 * Toda fala de personagem literário/histórico leva uma nota (ao passar o mouse no nome) dizendo que é ADAPTAÇÃO do jogo.
 */
public final class Dialogos {
	private Dialogos() {
	}

	/** Resultado da escolha de fala. {@code chave} != null: é uma explicação que deve ser registrada como dada. */
	public record Escolha(String texto, @Nullable String chave, int indiceCasual) {
	}

	public static Perfil.Contexto contexto(Player player, Perfil p) {
		Memoria mem = Memoria.de(player);
		return new Perfil.Contexto(player, Estados.get(player), player.level().dimension() == Planos.NOSSO_LAR, !mem.tem("conheceu:" + p.id()), mem);
	}

	/** Escolha pura (sem efeitos colaterais) — usada pela conversa e pelos testes. */
	public static Escolha escolher(Perfil p, Perfil.Contexto ctx, RandomSource r, int ultimaFala) {
		if (ctx.primeiroEncontro())
			return new Escolha(p.apresentacao(), null, -1);
		if (ctx.emCuidado() && p.categoria() != Perfil.Categoria.HISTORICO)
			return new Escolha(TipoNpc.FALAS_CUIDADO.get(r.nextInt(TipoNpc.FALAS_CUIDADO.size())), null, -1);
		if (!ctx.nossoLar() && p.categoria() != Perfil.Categoria.HISTORICO)
			return new Escolha(TipoNpc.FALAS_FORA.get(r.nextInt(TipoNpc.FALAS_FORA.size())), null, -1);
		Perfil.Fala f = p.proximaExplicacao(ctx);
		if (f != null)
			return new Escolha(f.texto(), f.chave(), -1);
		List<String> pool = new ArrayList<>(p.casuais());
		pool.addAll(p.tipo().falas);
		if (pool.isEmpty())
			return new Escolha(p.apresentacao(), null, -1);
		int i = r.nextInt(pool.size());
		if (i == ultimaFala && pool.size() > 1)
			i = (i + 1) % pool.size();
		return new Escolha(pool.get(i), null, i);
	}

	/** Conversa completa com um NPC (servidor). */
	public static void conversar(Player player, MoradorEntity m) {
		Perfil p = m.perfil();
		if (p == null)
			return;
		Perfil.Contexto ctx = contexto(player, p);
		if (ctx.primeiroEncontro()) {
			Memoria.marcar(player, "conheceu:" + p.id());
			falar(player, p, p.apresentacao());
			notaDeFonte(player, p);
		}
		// Jornadas e missões têm prioridade quando o NPC participa da etapa atual.
		if (p.id().equals(Perfis.AMELIE) && MissoesAmelie.conversar(player, m, p))
			return;
		if (JornadaRecemChegado.conversar(player, m, p, ctx))
			return;
		if (ctx.primeiroEncontro())
			return;
		Escolha e = escolher(p, ctx, m.getRandom(), m.ultimaFala);
		if (e.chave() != null)
			Memoria.marcar(player, "explicou:" + p.id() + ":" + e.chave());
		if (e.indiceCasual() >= 0)
			m.ultimaFala = e.indiceCasual();
		falar(player, p, e.texto());
	}

	public static void notaDeFonte(Player player, Perfil p) {
		switch (p.categoria()) {
			case LITERARIO -> narrar(player, p.nome() + " é personagem do livro Nosso Lar (André Luiz, psicografia de Francisco Cândido Xavier, FEB, 1944) — " + p.fonte()
					+ ". As falas no jogo são adaptações, não transcrições do livro.");
			case HISTORICO -> narrar(player, MissoesAmelie.APRESENTACAO_HISTORICA);
			case GENERICO -> {
			}
		}
	}

	// ---------------------------------------------------------------- Saída no chat

	public static void falar(Player player, Perfil p, String texto) {
		player.sendSystemMessage(linha(p, texto));
	}

	public static Component linha(Perfil p, String texto) {
		String nota = switch (p.categoria()) {
			case LITERARIO -> "Personagem do livro Nosso Lar (André Luiz / Chico Xavier, FEB, 1944).\nFala ADAPTADA para o jogo — não é transcrição.\nFonte: " + p.fonte();
			case HISTORICO -> "Personagem histórica.\nFala ADAPTADA para o jogo — não é citação.\nFonte: " + p.fonte();
			case GENERICO -> "Personagem criado para o jogo (" + p.tipo().rotulo + ").";
		};
		ChatFormatting cor = switch (p.categoria()) {
			case LITERARIO -> ChatFormatting.AQUA;
			case HISTORICO -> ChatFormatting.LIGHT_PURPLE;
			case GENERICO -> ChatFormatting.YELLOW;
		};
		MutableComponent nome = Component.literal("<" + p.nome() + "> ").withStyle(Style.EMPTY.withColor(cor).withHoverEvent(new HoverEvent.ShowText(Component.literal(nota))));
		return nome.append(Component.literal(texto).withStyle(ChatFormatting.WHITE));
	}

	public static void narrar(Player player, String texto) {
		player.sendSystemMessage(Component.literal(texto).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}

	public static void objetivo(Player player, String titulo, String texto) {
		player.sendSystemMessage(Component.literal("✦ " + titulo + ": ").withStyle(ChatFormatting.GOLD).append(Component.literal(texto).withStyle(ChatFormatting.YELLOW)));
	}

	/** Atalho para testes: estado do jogador ignorando o attachment. */
	public static Perfil.Contexto contexto(Player player, Perfil p, EstadoEspiritual estado, boolean nossoLar) {
		Memoria mem = Memoria.de(player);
		return new Perfil.Contexto(player, estado, nossoLar, !mem.tem("conheceu:" + p.id()), mem);
	}
}
