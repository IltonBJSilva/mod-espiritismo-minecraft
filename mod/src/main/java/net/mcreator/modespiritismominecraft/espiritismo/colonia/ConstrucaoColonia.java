package net.mcreator.modespiritismominecraft.espiritismo.colonia;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.RegistroDimensao;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.Map;
import java.util.HashMap;

/**
 * Ponto de entrada da construção de Nosso Lar.
 * <p>
 * {@link #construir} (chamado por {@code ConstrucoesPlanos.construirNossoLar}, que {@code Planos.garantir} chama na
 * primeira chegada): faz de uma vez só as zonas essenciais (Câmaras de Retificação — onde se desperta —, o atril do
 * Ministério do Auxílio e o Mirante), devolve TODOS os pontos nomeados e, na dimensão Nosso Lar, agenda o resto da
 * cidade para a obra incremental ({@link Obra}), que roda no tick do servidor e é retomada após reiniciar.
 */
public final class ConstrucaoColonia {
	private static final Map<ResourceKey<Level>, Obra> ATIVAS = new HashMap<>();

	private ConstrucaoColonia() {
	}

	/** Constrói o essencial agora e agenda o resto (só em Nosso Lar). {@code sugestao}: centro (x, z); y é recalculado. */
	public static Map<String, Long> construir(ServerLevel level, BlockPos sugestao) {
		int g = PlanoColonia.alturaDaCidade(level, sugestao.getX(), sugestao.getZ());
		BlockPos origem = new BlockPos(sugestao.getX(), g, sugestao.getZ());
		PlanoColonia plano = new PlanoColonia(origem);
		long t0 = System.nanoTime();
		boolean nossoLar = level.dimension() == Planos.NOSSO_LAR;
		if (nossoLar) {
			// Só a área de chegada agora; Auxílio e Mirante são as primeiras peças da obra (segundos depois).
			Lote l = plano.loteEssencial();
			plano.chegada().gerar().accept(level, l);
			l.executarTudo(level);
		} else
			construirEssenciais(level, plano);
		ModEspiritismoMinecraftMod.LOGGER.info("[Colonia] Zona(s) essencial(is) de Nosso Lar prontas em {} ms (origem {}, {})", (System.nanoTime() - t0) / 1_000_000, origem,
				level.dimension().identifier());
		if (nossoLar)
			iniciarObra(level, plano, 0);
		return plano.pontos();
	}

	public static void construirEssenciais(ServerLevel level, PlanoColonia plano) {
		for (PlanoColonia.Peca p : plano.essenciais()) {
			Lote l = plano.loteEssencial();
			p.gerar().accept(level, l);
			l.executarTudo(level);
		}
	}

	static Obra iniciarObra(ServerLevel level, PlanoColonia plano, int indice) {
		Obra o = new Obra(level, plano, indice, true);
		o.salvar();
		if (!o.concluida())
			ATIVAS.put(level.dimension(), o);
		ModEspiritismoMinecraftMod.LOGGER.info("[Colonia] Obra de Nosso Lar {} na peça {}/{}", indice == 0 ? "iniciada" : "retomada", indice, o.total());
		return o;
	}

	public static Obra obra(ServerLevel level) {
		return ATIVAS.get(level.dimension());
	}

	/** Uma obra está em andamento nesse Level? */
	public static boolean emObras(Level level) {
		return ATIVAS.containsKey(level.dimension());
	}

	static void tick(net.minecraft.server.MinecraftServer server) {
		if (ATIVAS.isEmpty())
			return;
		var it = ATIVAS.entrySet().iterator();
		while (it.hasNext()) {
			var e = it.next();
			Obra o = e.getValue();
			if (server.getLevel(e.getKey()) != o.level()) {
				it.remove();
				continue;
			}
			o.tick();
			if (o.concluida())
				it.remove();
		}
	}

	/**
	 * Ao iniciar o servidor: retoma a obra salva; ou, num save antigo (colônia pequena de antes, sem
	 * "colonia_versao"), reconstrói Nosso Lar no novo traçado, no mesmo centro.
	 */
	static void aoIniciarServidor(net.minecraft.server.MinecraftServer server) {
		ATIVAS.clear();
		ServerLevel nl = server.getLevel(Planos.NOSSO_LAR);
		if (nl == null)
			return;
		RegistroColonia.EstadoObra estado = nl.getData(RegistroColonia.OBRA);
		Map<String, Long> pontos = nl.getData(RegistroDimensao.PONTOS);
		if (estado.existe() && estado.versao() == LayoutColonia.VERSAO) {
			if (!estado.concluida())
				iniciarObra(nl, new PlanoColonia(BlockPos.of(estado.origem())), estado.indice());
			return;
		}
		if (pontos.containsKey("chegada") && !pontos.containsKey("colonia_versao")) {
			// Save antigo: a praça antiga ficava em (0, g, 0). Refaz no traçado novo, mantendo o centro.
			Long praca = pontos.get("praca");
			BlockPos centro = praca == null ? BlockPos.ZERO : BlockPos.of(praca);
			ModEspiritismoMinecraftMod.LOGGER.info("[Colonia] Save antigo de Nosso Lar detectado: atualizando para a cidade nova (traçado v{})", LayoutColonia.VERSAO);
			Map<String, Long> novos = construir(nl, new BlockPos(centro.getX(), 0, centro.getZ()));
			Map<String, Long> m = new HashMap<>(pontos);
			m.putAll(novos);
			nl.setData(RegistroDimensao.PONTOS, m);
		}
	}

	static void aoPararServidor() {
		ATIVAS.values().forEach(Obra::liberar);
		ATIVAS.clear();
	}
}
