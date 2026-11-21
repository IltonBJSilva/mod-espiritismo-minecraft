package net.mcreator.modespiritismominecraft.espiritismo.biblioteca;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Progresso de leitura e CONHECIMENTO do jogador (separado da moralidade, PROMPT_MESTRE §7). Imutável: toda alteração
 * devolve uma cópia nova, que é gravada com {@code setData} (o que também sincroniza com o cliente).
 *
 * @param versao      versão do esquema (para migrações futuras)
 * @param lidas       obraId → ids das seções lidas
 * @param marcadores  obraId → id da seção marcada
 * @param notas       obraId → nota pessoal do jogador (até {@link #MAX_NOTA} caracteres)
 * @param acertos     ids de perguntas acertadas (cada uma conta uma vez)
 * @param tentativas  id da pergunta → número de tentativas
 * @param modulos     módulos de trilha concluídos
 * @param sugeridos   módulos já sugeridos pelo jogo (cada sugestão aparece uma vez)
 * @param evangelhos  quantas vezes participou do Evangelho no Lar até o fim
 * @param ultimoEvangelhoDia dia de jogo do último Evangelho no Lar concluído
 */
public record ProgressoBiblioteca(int versao, Map<String, List<String>> lidas, Map<String, String> marcadores, Map<String, String> notas, List<String> acertos,
		Map<String, Integer> tentativas, List<String> modulos, List<String> sugeridos, int evangelhos, long ultimoEvangelhoDia) {

	public static final int VERSAO = 1;
	public static final int MAX_NOTA = 256;
	public static final ProgressoBiblioteca VAZIO = new ProgressoBiblioteca(VERSAO, Map.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of(), List.of(), 0, -1L);

	public static final MapCodec<ProgressoBiblioteca> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("versao", VERSAO).forGetter(ProgressoBiblioteca::versao),
			Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("lidas", Map.of()).forGetter(ProgressoBiblioteca::lidas),
			Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("marcadores", Map.of()).forGetter(ProgressoBiblioteca::marcadores),
			Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("notas", Map.of()).forGetter(ProgressoBiblioteca::notas),
			Codec.STRING.listOf().optionalFieldOf("acertos", List.of()).forGetter(ProgressoBiblioteca::acertos),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("tentativas", Map.of()).forGetter(ProgressoBiblioteca::tentativas),
			Codec.STRING.listOf().optionalFieldOf("modulos", List.of()).forGetter(ProgressoBiblioteca::modulos),
			Codec.STRING.listOf().optionalFieldOf("sugeridos", List.of()).forGetter(ProgressoBiblioteca::sugeridos),
			Codec.INT.optionalFieldOf("evangelhos", 0).forGetter(ProgressoBiblioteca::evangelhos),
			Codec.LONG.optionalFieldOf("ultimo_evangelho_dia", -1L).forGetter(ProgressoBiblioteca::ultimoEvangelhoDia)).apply(i, ProgressoBiblioteca::new));

	// ---------------------------------------------------------------- Consultas

	public boolean lida(String obra, String secao) {
		return lidas.getOrDefault(obra, List.of()).contains(secao);
	}

	public int lidasDa(String obra) {
		return lidas.getOrDefault(obra, List.of()).size();
	}

	public int totalLidas() {
		int n = 0;
		for (List<String> l : lidas.values())
			n += l.size();
		return n;
	}

	public boolean acertou(String pergunta) {
		return acertos.contains(pergunta);
	}

	public boolean concluiu(String modulo) {
		return modulos.contains(modulo);
	}

	/** Pontos de conhecimento: 1 por seção lida, 2 por pergunta acertada, 3 por módulo concluído, 2 por Evangelho no Lar. */
	public int conhecimento() {
		return totalLidas() + 2 * acertos.size() + 3 * modulos.size() + 2 * evangelhos;
	}

	// ---------------------------------------------------------------- Alterações (cópias)

	public ProgressoBiblioteca comLeitura(String obra, String secao) {
		Map<String, List<String>> l = new HashMap<>(lidas);
		Set<String> s = new LinkedHashSet<>(l.getOrDefault(obra, List.of()));
		s.add(secao);
		l.put(obra, List.copyOf(s));
		Map<String, String> m = new HashMap<>(marcadores);
		m.put(obra, secao);
		return new ProgressoBiblioteca(VERSAO, Map.copyOf(l), Map.copyOf(m), notas, acertos, tentativas, modulos, sugeridos, evangelhos, ultimoEvangelhoDia);
	}

	public ProgressoBiblioteca comMarcador(String obra, String secao) {
		Map<String, String> m = new HashMap<>(marcadores);
		if (secao == null || secao.isEmpty())
			m.remove(obra);
		else
			m.put(obra, secao);
		return new ProgressoBiblioteca(VERSAO, lidas, Map.copyOf(m), notas, acertos, tentativas, modulos, sugeridos, evangelhos, ultimoEvangelhoDia);
	}

	public ProgressoBiblioteca comNota(String obra, String texto) {
		Map<String, String> n = new HashMap<>(notas);
		String t = texto == null ? "" : texto.strip();
		if (t.length() > MAX_NOTA)
			t = t.substring(0, MAX_NOTA);
		if (t.isEmpty())
			n.remove(obra);
		else
			n.put(obra, t);
		return new ProgressoBiblioteca(VERSAO, lidas, marcadores, Map.copyOf(n), acertos, tentativas, modulos, sugeridos, evangelhos, ultimoEvangelhoDia);
	}

	public ProgressoBiblioteca comResposta(String pergunta, boolean certa) {
		Map<String, Integer> t = new HashMap<>(tentativas);
		t.merge(pergunta, 1, Integer::sum);
		List<String> a = acertos;
		if (certa && !acertos.contains(pergunta)) {
			a = new ArrayList<>(acertos);
			a.add(pergunta);
			a = List.copyOf(a);
		}
		return new ProgressoBiblioteca(VERSAO, lidas, marcadores, notas, a, Map.copyOf(t), modulos, sugeridos, evangelhos, ultimoEvangelhoDia);
	}

	public ProgressoBiblioteca comModulo(String modulo) {
		if (modulos.contains(modulo))
			return this;
		List<String> l = new ArrayList<>(modulos);
		l.add(modulo);
		return new ProgressoBiblioteca(VERSAO, lidas, marcadores, notas, acertos, tentativas, List.copyOf(l), sugeridos, evangelhos, ultimoEvangelhoDia);
	}

	public ProgressoBiblioteca comSugestao(String modulo) {
		if (sugeridos.contains(modulo))
			return this;
		List<String> l = new ArrayList<>(sugeridos);
		l.add(modulo);
		return new ProgressoBiblioteca(VERSAO, lidas, marcadores, notas, acertos, tentativas, modulos, List.copyOf(l), evangelhos, ultimoEvangelhoDia);
	}

	public ProgressoBiblioteca comEvangelho(long dia) {
		return new ProgressoBiblioteca(VERSAO, lidas, marcadores, notas, acertos, tentativas, modulos, sugeridos, evangelhos + 1, dia);
	}
}
