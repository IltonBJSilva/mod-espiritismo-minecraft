package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.minecraft.world.entity.player.Player;

import net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual;

import javax.annotation.Nullable;

import java.util.function.Predicate;
import java.util.List;
import java.util.ArrayList;

/**
 * Perfil de um NPC de Nosso Lar (ou histórico): quem é, onde trabalha, rotina e falas.
 *
 * @param id           identificador estável (salvo na entidade; também o nome da skin em textures/populacao/)
 * @param nome         nome exibido
 * @param tipo         papel na colônia
 * @param categoria    genérico (criado para o jogo), literário (livro Nosso Lar) ou histórico (pessoa real)
 * @param local        zona de trabalho ({@code ColoniaApi.local})
 * @param rotina       zonas que visita no período de convivência
 * @param descanso     zona de descanso à noite
 * @param fonte        referência (livro/capítulo ou fonte histórica); vazio para genéricos
 * @param apresentacao fala no primeiro encontro
 * @param explicacoes  explicações dadas UMA vez cada, em ordem, quando a condição vale
 * @param casuais      falas soltas (variam, sem repetir a anterior)
 */
public record Perfil(String id, String nome, TipoNpc tipo, Categoria categoria, String local, List<String> rotina, String descanso, String fonte, String apresentacao,
		List<Fala> explicacoes, List<String> casuais) {

	public enum Categoria {
		GENERICO, LITERARIO, HISTORICO
	}

	/** Uma explicação condicionada ao contexto. {@code chave} é registrada na memória ("explicou:perfil:chave"). */
	public record Fala(String chave, Predicate<Contexto> condicao, String texto) {
	}

	/** O que o NPC sabe do jogador na hora da conversa (§8.2). */
	public record Contexto(Player player, EstadoEspiritual estado, boolean nossoLar, boolean primeiroEncontro, Memoria memoria) {
		public boolean leu(String obra) {
			return memoria.tem("leu:" + obra);
		}

		public boolean leuAlguma() {
			return memoria.quantas("leu:") > 0;
		}

		public boolean marca(String m) {
			return memoria.tem(m);
		}

		/** Jogador em sofrimento/tratamento: precisa de cuidado e não recebe tarefas de assistência. */
		public boolean emCuidado() {
			return estado.precisaDeCuidado();
		}

		public long etapaRecemChegado() {
			return memoria.valor(JornadaRecemChegado.ETAPA, 0);
		}
	}

	public boolean nomeado() {
		return categoria != Categoria.GENERICO;
	}

	// ---------------------------------------------------------------- Construtor fluente

	public static Construtor de(String id, String nome, TipoNpc tipo) {
		return new Construtor(id, nome, tipo);
	}

	public static final class Construtor {
		private final String id, nome;
		private final TipoNpc tipo;
		private Categoria categoria = Categoria.GENERICO;
		private String local, descanso = "residencias", fonte = "", apresentacao;
		private List<String> rotina = List.of("praca");
		private final List<Fala> explicacoes = new ArrayList<>();
		private final List<String> casuais = new ArrayList<>();

		private Construtor(String id, String nome, TipoNpc tipo) {
			this.id = id;
			this.nome = nome;
			this.tipo = tipo;
			this.local = tipo.zonas.isEmpty() ? "praca" : tipo.zonas.get(0);
		}

		public Construtor literario(String fonte) {
			this.categoria = Categoria.LITERARIO;
			this.fonte = fonte;
			return this;
		}

		public Construtor historico(String fonte) {
			this.categoria = Categoria.HISTORICO;
			this.fonte = fonte;
			return this;
		}

		public Construtor local(String local) {
			this.local = local;
			return this;
		}

		public Construtor rotina(String... zonas) {
			this.rotina = List.of(zonas);
			return this;
		}

		public Construtor descanso(String zona) {
			this.descanso = zona;
			return this;
		}

		public Construtor ola(String texto) {
			this.apresentacao = texto;
			return this;
		}

		public Construtor explica(String chave, String texto) {
			return explica(chave, c -> true, texto);
		}

		public Construtor explica(String chave, Predicate<Contexto> condicao, String texto) {
			explicacoes.add(new Fala(chave, condicao, texto));
			return this;
		}

		public Construtor diz(String... textos) {
			casuais.addAll(List.of(textos));
			return this;
		}

		public Perfil criar() {
			String ap = apresentacao != null ? apresentacao : "Que a paz esteja com você.";
			return new Perfil(id, nome, tipo, categoria, local, rotina, descanso, fonte, ap, List.copyOf(explicacoes), List.copyOf(casuais));
		}
	}

	@Nullable
	public Fala proximaExplicacao(Contexto ctx) {
		for (Fala f : explicacoes)
			if (!ctx.marca("explicou:" + id + ":" + f.chave()) && f.condicao().test(ctx))
				return f;
		return null;
	}
}
