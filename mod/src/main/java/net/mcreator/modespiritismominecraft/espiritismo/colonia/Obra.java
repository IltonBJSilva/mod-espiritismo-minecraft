package net.mcreator.modespiritismominecraft.espiritismo.colonia;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.Set;
import java.util.HashSet;

/**
 * Construção incremental e retomável de Nosso Lar: a cada tick do servidor aplica operações até um limite de tempo
 * ({@link #orcamentoNanos}) e de chunks novos ({@link #chunksPorTick}); ao terminar cada peça grava o índice no Level
 * ({@link RegistroColonia#OBRA}), de modo que, se o mundo fechar no meio, a peça em curso é refeita (as operações são
 * idempotentes) e a obra continua dali.
 */
public final class Obra {
	private final ServerLevel level;
	private final PlanoColonia plano;
	private final boolean persistente;
	private int indice;
	private Lote atual;
	private boolean concluida;
	/** Orçamento por tick (padrão 6 ms: cabe folgado nos 50 ms de um tick). */
	public long orcamentoNanos = 6_000_000L;
	/** Chunks ainda não carregados que podem ser gerados por tick (cada um pode custar alguns ms). */
	public int chunksPorTick = 1;
	/** Se não nulo, guarda as posições de plantas colocadas (testes). */
	public Set<Long> plantas;

	// Telemetria
	public long ticks, nanosTotal, nanosMaxTick, operacoes, nanosGeracaoMax;
	public String pecaMaisLenta = "";
	private final long inicioMs = System.currentTimeMillis();

	public Obra(ServerLevel level, PlanoColonia plano, int indice, boolean persistente) {
		this.level = level;
		this.plano = plano;
		this.indice = Math.max(0, indice);
		this.persistente = persistente;
		this.concluida = this.indice >= plano.incrementais().size();
	}

	public ServerLevel level() {
		return level;
	}

	public PlanoColonia plano() {
		return plano;
	}

	public int indice() {
		return indice;
	}

	public int total() {
		return plano.incrementais().size();
	}

	public boolean concluida() {
		return concluida;
	}

	public int porcentagem() {
		return total() == 0 ? 100 : (int) (100L * indice / total());
	}

	/** Renova o pedido de chunk a cada 5 s; carrega na marra (síncrono) só depois de 20 s de espera. */
	private static final long RENOVAR_MS = 5_000, ESPERA_MAX_MS = 20_000;
	/** Quantas peças à frente têm o chunk pedido com antecedência (carregamento em paralelo). */
	private static final int ADIANTAR = 16;
	private final java.util.Map<Long, Long> pedidos = new java.util.HashMap<>(), renovado = new java.util.HashMap<>();

	private void pedir(int cx, int cz) {
		long chave = ChunkPos.pack(cx, cz);
		if (pedidos.containsKey(chave) || level.getChunkSource().getChunkNow(cx, cz) != null)
			return;
		if (pedidos.size() > 256)
			pedidos.keySet().removeIf(k -> level.getChunkSource().getChunkNow(ChunkPos.getX(k), ChunkPos.getZ(k)) != null);
		if (renovado.size() > 256)
			renovado.keySet().retainAll(pedidos.keySet());
		pedidos.put(chave, System.nanoTime());
		ticket(chave);
	}

	/** Chunks com ticket da obra (carregam em segundo plano; liberados ao fim de cada peça). */
	private final java.util.Set<Long> comTicket = new java.util.HashSet<>(), proximos = new java.util.HashSet<>();

	private void ticket(long chave) {
		if (comTicket.add(chave))
			level.getChunkSource().addTicketWithRadius(RegistroColonia.TICKET_OBRA.get(), new ChunkPos(ChunkPos.getX(chave), ChunkPos.getZ(chave)), 0);
	}

	/** Solta os tickets que as próximas peças não vão usar. */
	private void liberarTickets(boolean todos) {
		var it = comTicket.iterator();
		while (it.hasNext()) {
			long k = it.next();
			if (!todos && proximos.contains(k))
				continue;
			level.getChunkSource().removeTicketWithRadius(RegistroColonia.TICKET_OBRA.get(), new ChunkPos(ChunkPos.getX(k), ChunkPos.getZ(k)), 0);
			it.remove();
			pedidos.remove(k);
			renovado.remove(k);
		}
	}

	/** Texto de depuração do estado atual. */
	public String estado() {
		return "peça " + indice + "/" + total() + (indice < total() ? " (" + plano.incrementais().get(indice).nome() + ")" : "") + ", ops na fila " + (atual == null ? 0 : atual.tamanho())
				+ ", pedidos " + pedidos.size() + ", tickets " + comTicket.size() + ", esperando " + esperando;
	}

	private String esperando = "-";

	/** Solta todos os tickets (fim da obra, servidor parando). */
	public void liberar() {
		liberarTickets(true);
	}

	/** Um tick de obra. Devolve true se concluiu agora. */
	public boolean tick() {
		if (concluida)
			return false;
		long t0 = System.nanoTime();
		int chunksNovos = 0;
		ticks++;
		while (System.nanoTime() - t0 < orcamentoNanos) {
			if (atual == null || atual.vazio()) {
				if (atual != null) {
					indice++;
					atual = null;
					liberarTickets(false);
					salvar();
					if (indice >= total()) {
						concluir(t0);
						return true;
					}
				}
				PlanoColonia.Peca peca = plano.incrementais().get(indice);
				atual = plano.loteDe(peca);
				atual.plantasColocadas = plantas;
				long g0 = System.nanoTime();
				peca.gerar().accept(level, atual);
				long dg = System.nanoTime() - g0;
				if (dg > nanosGeracaoMax) {
					nanosGeracaoMax = dg;
					pecaMaisLenta = peca.nome();
				}
				atual.finalizar();
				// Pede de uma vez todos os chunks que esta peça vai tocar (carregam em paralelo, em segundo plano)
				// e já adianta os das próximas peças.
				proximos.clear();
				for (long c : atual.chunks())
					pedir(ChunkPos.getX(c), ChunkPos.getZ(c));
				for (int k = 1; k <= ADIANTAR && indice + k < total(); k++) {
					PlanoColonia.Peca prox = plano.incrementais().get(indice + k);
					int pcx = SectionPos.blockToSectionCoord(plano.origem().getX() + prox.refX()), pcz = SectionPos.blockToSectionCoord(plano.origem().getZ() + prox.refZ());
					proximos.add(ChunkPos.pack(pcx, pcz));
					pedir(pcx, pcz);
				}
				continue;
			}
			Lote.Op op = atual.proxima();
			long ref = op.pos();
			if (ref != Long.MIN_VALUE) {
				int cx = SectionPos.blockToSectionCoord(BlockPos.getX(ref)), cz = SectionPos.blockToSectionCoord(BlockPos.getZ(ref));
				if (level.getChunkSource().getChunkNow(cx, cz) == null) {
					// Pede o chunk de forma assíncrona (o gerador trabalha fora da thread principal) e espera.
					long chave = ChunkPos.pack(cx, cz);
					Long pedido = pedidos.get(chave);
					esperando = cx + "," + cz;
					if (pedido == null) {
						pedir(cx, cz);
						break;
					}
					long esperaMs = (System.nanoTime() - pedido) / 1_000_000;
					long ultima = renovado.getOrDefault(chave, pedido);
					if ((System.nanoTime() - ultima) / 1_000_000 >= RENOVAR_MS) {
						// o ticket pode ter expirado: renova (sem bloquear) e continua esperando
						renovado.put(chave, System.nanoTime());
						ticket(chave);
					}
					if (esperaMs < ESPERA_MAX_MS || chunksNovos >= chunksPorTick)
						break;
					chunksNovos++;
					level.getChunk(cx, cz); // demorou demais: carrega na hora (raro)
				}
			}
			long o0 = System.nanoTime();
			operacoes += op.aplicar(level);
			long dop = System.nanoTime() - o0;
			if (dop > 50_000_000L)
				ModEspiritismoMinecraftMod.LOGGER.debug("[Colonia] operação lenta ({} ms) em {} na peça {}", dop / 1_000_000, ref == Long.MIN_VALUE ? "-" : BlockPos.of(ref).toShortString(),
						plano.incrementais().get(indice).nome());
			atual.descartarPrimeira();
		}
		long dt = System.nanoTime() - t0;
		nanosTotal += dt;
		nanosMaxTick = Math.max(nanosMaxTick, dt);
		if (dt > 100_000_000L)
			ModEspiritismoMinecraftMod.LOGGER.debug("[Colonia] tick de obra lento: {} ms (peça {})", dt / 1_000_000, indice < total() ? plano.incrementais().get(indice).nome() : "-");
		return false;
	}

	/** Roda até o fim (testes; com orçamento alto). Devolve o número de ticks simulados. */
	public int terminarAgora(int maxIteracoes) {
		int n = 0;
		while (!concluida && n++ < maxIteracoes)
			tick();
		return n;
	}

	private void concluir(long t0) {
		concluida = true;
		liberarTickets(true);
		long dt = System.nanoTime() - t0;
		nanosTotal += dt;
		nanosMaxTick = Math.max(nanosMaxTick, dt);
		salvar();
		ModEspiritismoMinecraftMod.LOGGER.info("[Colonia] Nosso Lar concluída em {}: {} peças, {} operações, {} ticks de obra, {} ms de CPU (máx. {} ms num tick), {} s de relógio; geração de peça mais lenta: {} ({} ms)",
				level.dimension().identifier(), total(), operacoes, ticks, nanosTotal / 1_000_000, nanosMaxTick / 1_000_000, (System.currentTimeMillis() - inicioMs) / 1000, pecaMaisLenta, nanosGeracaoMax / 1_000_000);
	}

	void salvar() {
		if (!persistente)
			return;
		level.setData(RegistroColonia.OBRA, new RegistroColonia.EstadoObra(LayoutColonia.VERSAO, plano.origem().asLong(), indice, concluida));
	}

	/** Chunk de referência da peça em curso (para depuração). */
	public ChunkPos chunkAtual() {
		if (indice >= total())
			return null;
		PlanoColonia.Peca p = plano.incrementais().get(indice);
		return new ChunkPos(SectionPos.blockToSectionCoord(plano.origem().getX() + p.refX()), SectionPos.blockToSectionCoord(plano.origem().getZ() + p.refZ()));
	}

	public static Set<Long> novoRegistroDePlantas() {
		return new HashSet<>();
	}
}
