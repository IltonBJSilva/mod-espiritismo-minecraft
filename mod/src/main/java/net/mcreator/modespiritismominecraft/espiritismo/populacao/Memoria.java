package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;

import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Memória dos NPCs de Nosso Lar sobre UM jogador (attachment {@link RegistroPopulacao#MEMORIA}, copiada na morte).
 * <ul>
 * <li>{@code marcas}: fatos sim/não — {@code conheceu:<perfil>}, {@code explicou:<perfil>:<chave>}, {@code leu:<obra>},
 * {@code visitou:<zona>}, {@code escolha:estudo|trabalho}, {@code amelie:entregue}...</li>
 * <li>{@code valores}: números — etapas das jornadas ({@code rc_etapa}, {@code amelie_etapa}) e marcos de tempo
 * ({@code rc_inicio} = tick do mundo).</li>
 * </ul>
 * Schema versionado ({@link #VERSAO}) para migrações futuras.
 */
public final class Memoria {
	public static final int VERSAO = 1;

	private final Set<String> marcas;
	private final Map<String, Long> valores;
	private int versao;

	public Memoria() {
		this(VERSAO, new HashSet<>(), new HashMap<>());
	}

	private Memoria(int versao, Set<String> marcas, Map<String, Long> valores) {
		this.versao = versao;
		this.marcas = marcas;
		this.valores = valores;
	}

	public static final Codec<Memoria> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("versao", VERSAO).forGetter(m -> m.versao),
			Codec.STRING.listOf().<Set<String>>xmap(HashSet::new, ArrayList::new).optionalFieldOf("marcas", Set.of()).forGetter(m -> m.marcas),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).<Map<String, Long>>xmap(HashMap::new, m -> m).optionalFieldOf("valores", Map.of()).forGetter(m -> m.valores))
			.apply(i, (v, s, m) -> new Memoria(v, new HashSet<>(s), new HashMap<>(m))));

	public boolean tem(String marca) {
		return marcas.contains(marca);
	}

	/** Marca; devolve true se é novidade. */
	public boolean marcar(String marca) {
		return marcas.add(marca);
	}

	public void desmarcar(String marca) {
		marcas.remove(marca);
	}

	public long valor(String chave, long padrao) {
		return valores.getOrDefault(chave, padrao);
	}

	public void definir(String chave, long valor) {
		valores.put(chave, valor);
	}

	public int quantas(String prefixo) {
		int n = 0;
		for (String m : marcas)
			if (m.startsWith(prefixo))
				n++;
		return n;
	}

	public List<String> marcas() {
		return List.copyOf(marcas);
	}

	public int versao() {
		return versao;
	}

	// ---------------------------------------------------------------- Acesso pelo jogador

	public static Memoria de(Player player) {
		return player.getData(RegistroPopulacao.MEMORIA);
	}

	/** Marca e salva (setData marca o jogador como modificado). */
	public static boolean marcar(Player player, String marca) {
		Memoria m = de(player);
		boolean novo = m.marcar(marca);
		if (novo)
			player.setData(RegistroPopulacao.MEMORIA, m);
		return novo;
	}

	public static void definir(Player player, String chave, long valor) {
		Memoria m = de(player);
		m.definir(chave, valor);
		player.setData(RegistroPopulacao.MEMORIA, m);
	}

	public static boolean tem(Player player, String marca) {
		return de(player).tem(marca);
	}

	/** Obra lida pelo jogador (id do item, ex.: "o_livro_dos_espiritos", "nosso_lar"). */
	public static boolean leu(Player player, String obra) {
		return tem(player, "leu:" + obra);
	}
}
