package net.mcreator.modespiritismominecraft.espiritismo.dimensao;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import javax.annotation.Nullable;

import java.util.UUID;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Deque;
import java.util.ArrayDeque;

/**
 * Busca INCREMENTAL de um local seguro na Terra para a reencarnação (PROMPT §14.2). Em vez de gerar até 12 chunks
 * de uma vez no tick, testa UM candidato por vez: pede o carregamento assíncrono do chunk com um ticket (os
 * workers do jogo geram fora da thread principal), espera ele ficar pronto ({@code getChunkNow}) e só então lê o
 * heightmap. No máximo {@link #MAX_TENTATIVAS} candidatos ou {@link #PRAZO} ticks; depois, o fallback seguro
 * (último local encontrado em cache ou o ponto de renascimento).
 */
public final class BuscaLocalSeguro {
	public static final int MAX_TENTATIVAS = 6;
	public static final int ESPERA_CHUNK = 100;
	public static final int PRAZO = 600;
	private static final TicketType TICKET = TicketType.PORTAL;

	private static final class Busca {
		final UUID jogador;
		final BlockPos base;
		final RandomSource rand;
		final long inicio;
		int tentativas;
		@Nullable
		ChunkPos chunk;
		int alvoX, alvoZ, espera;
		@Nullable
		java.util.concurrent.CompletableFuture<?> futuro;

		Busca(UUID jogador, BlockPos base, long seed, long inicio) {
			this.jogador = jogador;
			this.base = base;
			this.rand = RandomSource.create(seed);
			this.inicio = inicio;
		}
	}

	private static final Map<UUID, Busca> BUSCAS = new LinkedHashMap<>();
	/** Locais seguros já encontrados (cache em memória, para o fallback). */
	private static final Deque<BlockPos> CACHE = new ArrayDeque<>();

	private BuscaLocalSeguro() {
	}

	public static boolean emAndamento(UUID jogador) {
		return BUSCAS.containsKey(jogador);
	}

	/** Começa a busca para o jogador. Devolve false se já houver uma (idempotente). */
	static boolean iniciar(ServerPlayer p, BlockPos base) {
		if (BUSCAS.containsKey(p.getUUID()))
			return false;
		BUSCAS.put(p.getUUID(), new Busca(p.getUUID(), base, p.getRandom().nextLong(), p.level().getGameTime()));
		return true;
	}

	static void cancelar(UUID jogador, MinecraftServer server) {
		Busca b = BUSCAS.remove(jogador);
		if (b != null && b.chunk != null)
			server.overworld().getChunkSource().removeTicketWithRadius(TICKET, b.chunk, 1);
	}

	/** Regra pura: a coluna serve? (chão firme e seco, sem folhas/magma, dois blocos de ar). */
	static boolean seguro(BlockState chao, BlockState pe, BlockState cabeca) {
		if (!chao.getFluidState().isEmpty() || chao.is(BlockTags.LEAVES) || chao.is(Blocks.MAGMA_BLOCK) || chao.isAir() || chao.is(Blocks.POWDER_SNOW) || chao.is(Blocks.CACTUS))
			return false;
		return pe.getFluidState().isEmpty() && !pe.blocksMotion() && cabeca.isAir();
	}

	/** Avalia um local SÓ se o chunk já estiver carregado (nunca gera). Devolve o topo ou null. */
	@Nullable
	static BlockPos avaliarCarregado(ServerLevel ow, int x, int z) {
		LevelChunk c = ow.getChunkSource().getChunkNow(x >> 4, z >> 4);
		if (c == null)
			return null;
		int y = c.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
		BlockPos topo = new BlockPos(x, y, z);
		if (y <= ow.getMinY() + 2)
			return null;
		return seguro(ow.getBlockState(topo.below()), ow.getBlockState(topo), ow.getBlockState(topo.above())) ? topo : null;
	}

	/** Processa no máximo uma busca por tick do servidor. */
	public static void tick(MinecraftServer server) {
		if (BUSCAS.isEmpty())
			return;
		Busca b = BUSCAS.values().iterator().next();
		ServerLevel ow = server.overworld();
		ServerPlayer p = server.getPlayerList().getPlayer(b.jogador);
		if (p == null) {
			cancelar(b.jogador, server);
			return;
		}
		long agora = ow.getGameTime();
		if (b.tentativas >= MAX_TENTATIVAS || agora - b.inicio > PRAZO) {
			cancelar(b.jogador, server);
			BlockPos f = CACHE.isEmpty() ? Planos.pontoNaTerra(p) : CACHE.peekLast();
			ModEspiritismoMinecraftMod.LOGGER.info("Reencarnação: sem local novo em {} ticks; usando fallback {}", agora - b.inicio, f);
			Planos.concluirReencarnacao(p, f);
			return;
		}
		if (b.chunk == null) {
			double ang = b.rand.nextDouble() * Math.PI * 2;
			int dist = 600 + b.rand.nextInt(601);
			b.alvoX = Math.clamp(b.base.getX() + (int) (Math.cos(ang) * dist), -29_000_000, 29_000_000);
			b.alvoZ = Math.clamp(b.base.getZ() + (int) (Math.sin(ang) * dist), -29_000_000, 29_000_000);
			b.chunk = new ChunkPos(b.alvoX >> 4, b.alvoZ >> 4);
			b.espera = 0;
			b.futuro = ow.getChunkSource().addTicketAndLoadWithRadius(TICKET, b.chunk, 1);
			return;
		}
		if ((b.futuro != null && !b.futuro.isDone()) || ow.getChunkSource().getChunkNow(b.chunk.x(), b.chunk.z()) == null) {
			if (++b.espera > ESPERA_CHUNK) {
				ModEspiritismoMinecraftMod.LOGGER.debug("Reencarnação: chunk {} não carregou em {} ticks", b.chunk, ESPERA_CHUNK);
				ow.getChunkSource().removeTicketWithRadius(TICKET, b.chunk, 1);
				b.chunk = null;
				b.tentativas++;
			}
			return;
		}
		BlockPos topo = avaliarCarregado(ow, b.alvoX, b.alvoZ);
		ow.getChunkSource().removeTicketWithRadius(TICKET, b.chunk, 1);
		b.chunk = null;
		b.tentativas++;
		if (topo != null) {
			BUSCAS.remove(b.jogador);
			CACHE.addLast(topo);
			while (CACHE.size() > 8)
				CACHE.removeFirst();
			ModEspiritismoMinecraftMod.LOGGER.info("Reencarnação: local seguro {} após {} tentativa(s), {} ticks", topo, b.tentativas, agora - b.inicio);
			Planos.concluirReencarnacao(p, topo);
		}
	}

	/** Versão imediata e limitada (comando/teste): só olha chunks já carregados; senão cache ou ponto de renascimento. */
	static BlockPos imediato(ServerPlayer p, ServerLevel ow, BlockPos base) {
		RandomSource r = p.getRandom();
		for (int t = 0; t < 12; t++) {
			double ang = r.nextDouble() * Math.PI * 2;
			int dist = 600 + r.nextInt(601);
			BlockPos topo = avaliarCarregado(ow, base.getX() + (int) (Math.cos(ang) * dist), base.getZ() + (int) (Math.sin(ang) * dist));
			if (topo != null)
				return topo;
		}
		for (BlockPos c : CACHE)
			if (c.distManhattan(base) > 300)
				return c;
		return Planos.pontoNaTerra(p);
	}
}
