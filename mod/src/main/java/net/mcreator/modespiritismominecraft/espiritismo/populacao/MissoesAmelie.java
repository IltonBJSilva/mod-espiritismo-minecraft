package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;

import net.mcreator.modespiritismominecraft.espiritismo.Jornada;

import java.util.List;

/**
 * Jornada histórica de Kardec e Amélie (§3.7 / §9): pequena cadeia sobre organização e preservação de documentos e
 * continuidade do trabalho. Baseada em fatos verificados (docs/NPCS_E_PERSONAGENS.md); as falas são adaptações.
 * <pre>
 * 0 ENCONTRO      conversar com Amélie                                           → jornada/amelie_encontro
 * 1 DOCUMENTOS    trazer 6 folhas de papel para copiar e guardar a correspondência → jornada/amelie_documentos
 * 2 METODO        estudar O Livro dos Espíritos (ou dos Médiuns / o Evangelho)      → jornada/amelie_metodo
 * 3 CONTINUIDADE  levar um livro ao Dirigente de um Centro Espírita e voltar        → jornada/amelie_continuidade
 * 4 CONCLUIDA
 * </pre>
 */
public final class MissoesAmelie {
	private MissoesAmelie() {
	}

	public static final String ETAPA = "amelie_etapa";
	public static final String ENTREGUE = "amelie:entregue";
	public static final int ENCONTRO = 0, DOCUMENTOS = 1, METODO = 2, CONTINUIDADE = 3, CONCLUIDA = 4;
	public static final int PAPEIS = 6;
	public static final List<String> CONQUISTAS = List.of("amelie_encontro", "amelie_documentos", "amelie_metodo", "amelie_continuidade");
	private static final List<String> OBRAS_METODO = List.of("o_livro_dos_espiritos", "o_livro_dos_mediuns", "o_evangelho_segundo_o_espiritismo");

	/** Nota histórica dita pelo NARRADOR (fatos com fonte; não é fala da personagem). */
	public static final String APRESENTACAO_HISTORICA = "Amélie Gabrielle Boudet (1795–1883) existiu de fato. Professora, publicou livros didáticos na década de 1820 e, "
			+ "em 1832, casou-se com o professor Hippolyte Léon Denizard Rivail, que mais tarde adotaria o pseudônimo Allan Kardec. Apoiou e auxiliou o trabalho dele "
			+ "na codificação espírita e, após a morte de Kardec (31/3/1869), apoiou a continuidade da Revista Espírita e da publicação das obras. Faleceu em Paris, "
			+ "em 21/1/1883, e foi sepultada ao lado do marido no cemitério do Père-Lachaise. As falas dela no jogo são adaptações, não citações.";

	public static int etapa(Player p) {
		return (int) Memoria.de(p).valor(ETAPA, ENCONTRO);
	}

	private static void avancar(Player p, int de, String proximo) {
		Memoria.definir(p, ETAPA, de + 1);
		Jornada.conquista(p, CONQUISTAS.get(de));
		if (p.level() instanceof ServerLevel sl)
			sl.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1F, 1F);
		if (proximo != null)
			Dialogos.objetivo(p, "Com Amélie Boudet", proximo);
	}

	/** @return true se a conversa foi usada pela cadeia de missões. */
	public static boolean conversar(Player p, MoradorEntity m, Perfil perfil) {
		switch (etapa(p)) {
			case ENCONTRO -> {
				Dialogos.falar(p, perfil, "Meu marido recebia cartas de muitos lugares: perguntas, relatos, comunicações de médiuns. Nada disso podia se perder, e ainda há muito a copiar e guardar. Você me ajudaria com a correspondência?");
				avancar(p, ENCONTRO, "Traga " + PAPEIS + " folhas de papel para Amélie copiar e arquivar as cartas");
				return true;
			}
			case DOCUMENTOS -> {
				if (contar(p, Items.PAPER) >= PAPEIS) {
					remover(p, Items.PAPER, PAPEIS);
					Dialogos.falar(p, perfil, "Obrigada! Cada carta ganha uma cópia, a data e o lugar de onde veio. Assim é possível comparar mensagens recebidas em lugares diferentes, por médiuns que nem se conhecem.");
					avancar(p, DOCUMENTOS, "Estude O Livro dos Espíritos (ou O Livro dos Médiuns) e volte a falar com Amélie");
				} else
					Dialogos.falar(p, perfil, "Ainda preciso de " + PAPEIS + " folhas de papel para as cópias. Sem pressa: papel bem guardado é memória que não se perde.");
				return true;
			}
			case METODO -> {
				if (OBRAS_METODO.stream().anyMatch(o -> Memoria.leu(p, o))) {
					Dialogos.falar(p, perfil, "Então você viu como se trabalha: nenhuma comunicação é aceita só porque veio de um Espírito. Examina-se com a razão e compara-se com o que outros médiuns, em outros lugares, receberam.");
					Dialogos.narrar(p, "Esse critério é o \"controle universal do ensino dos Espíritos\", explicado por Kardec em O Evangelho segundo o Espiritismo, Introdução, item II.");
					avancar(p, METODO, "Leve um livro (comum) ao Dirigente de um Centro Espírita, para o estudo de todos, e volte a Amélie");
				} else
					Dialogos.falar(p, perfil, "Antes de organizar, é preciso entender o que se organiza. Leia O Livro dos Espíritos e depois conversaremos sobre o método.");
				return true;
			}
			case CONTINUIDADE -> {
				if (Memoria.tem(p, ENTREGUE)) {
					Dialogos.falar(p, perfil, "Você entendeu o essencial: um trabalho assim não pertence a uma só pessoa. Quando meu marido partiu, em 1869, as cartas, a Revista e os livros precisaram de quem os continuasse, e houve quem continuasse.");
					Dialogos.narrar(p, "Fato histórico: após a morte de Kardec, organizou-se uma sociedade para continuar a Revista Espírita e a publicação das obras, com o apoio de Amélie Boudet; Pierre-Gaëtan Leymarie passou a administrá-la. Fontes em docs/NPCS_E_PERSONAGENS.md.");
					avancar(p, CONTINUIDADE, null);
					darRevista(p);
				} else
					Dialogos.falar(p, perfil, "Leve um livro a um Centro Espírita e entregue-o ao Dirigente. Conhecimento guardado numa gaveta não serve a ninguém.");
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	/** Gancho chamado pela área "npc" ao conversar com um Dirigente: entrega o livro da missão. */
	public static boolean entregarAoDirigente(Player p) {
		if (etapa(p) != CONTINUIDADE || Memoria.tem(p, ENTREGUE))
			return false;
		ItemStack mao = p.getItemInHand(InteractionHand.MAIN_HAND);
		if (!mao.is(Items.BOOK))
			return false;
		mao.shrink(1);
		Memoria.marcar(p, ENTREGUE);
		Dialogos.objetivo(p, "Com Amélie Boudet", "livro entregue. Volte a Amélie Boudet");
		return true;
	}

	private static void darRevista(Player p) {
		if (!Memoria.marcar(p, "recebeu:revista_espirita"))
			return;
		Item item = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(RegistroPopulacao.MODID, "revista_espirita")).orElse(null);
		if (item == null)
			return;
		ItemStack s = new ItemStack(item);
		if (!p.getInventory().add(s))
			p.drop(s, false);
	}

	public static int contar(Player p, Item item) {
		int n = 0;
		for (ItemStack s : p.getInventory().getNonEquipmentItems())
			if (s.is(item))
				n += s.getCount();
		return n;
	}

	static void remover(Player p, Item item, int quantos) {
		for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
			if (quantos <= 0)
				return;
			if (s.is(item)) {
				int t = Math.min(quantos, s.getCount());
				s.shrink(t);
				quantos -= t;
			}
		}
	}
}
