package net.mcreator.modespiritismominecraft.espiritismo.colonia;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import java.util.function.Consumer;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayDeque;

/**
 * Fila de operações de bloco de uma etapa da obra. Todas as escritas usam {@link #FLAGS}:
 * <ul>
 * <li>{@code UPDATE_KNOWN_SHAPE} (16): não propaga "atualização de forma" aos vizinhos — é essa propagação que fazia
 * uma flor cujo suporte foi trocado/removido chamar {@code Block.updateOrDestroy → destroyBlock(..., drop=true)} e
 * virar item (o bug das flores);</li>
 * <li>{@code UPDATE_SUPPRESS_DROPS} (32) e {@code UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS} (256): trocar/remover um bloco
 * nunca solta itens (nem o livro de um atril antigo);</li>
 * <li>{@code UPDATE_CLIENTS} (2): os clientes veem a mudança.</li>
 * </ul>
 * Plantas só entram depois do suporte final e só se {@code canSurvive} ({@link #planta}); colunas de terreno são
 * limpas de cima para baixo ({@link #coluna}). Blocos que se conectam (cercas, vidraças, muros, escadas) têm a forma
 * recalculada no fim da etapa ({@link #finalizar}).
 */
public final class Lote {
	public static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

	/** Uma operação; devolve o "custo" aproximado (blocos tocados). */
	interface Op {
		int aplicar(ServerLevel level);

		/** Posição de referência (para carregar o chunk antes), ou Long.MIN_VALUE. */
		long pos();
	}

	private final ArrayDeque<Op> ops = new ArrayDeque<>();
	private final BlockPos origem;
	private final List<LayoutColonia.Retangulo> excluir;
	private final Set<Long> conectar = new LinkedHashSet<>();
	/** Se não nulo, recebe a posição de cada planta efetivamente colocada (para testes/reparo). */
	Set<Long> plantasColocadas;

	/**
	 * @param origem  centro da cidade (y = chão)
	 * @param excluir retângulos relativos onde esta etapa não escreve (zonas já construídas), ou null
	 */
	public Lote(BlockPos origem, List<LayoutColonia.Retangulo> excluir) {
		this.origem = origem;
		this.excluir = excluir;
	}

	public BlockPos origem() {
		return origem;
	}

	public int tamanho() {
		return ops.size();
	}

	public boolean vazio() {
		return ops.isEmpty();
	}

	/** Chunks (ChunkPos.pack) tocados pelas operações, na ordem em que aparecem. */
	public java.util.LinkedHashSet<Long> chunks() {
		java.util.LinkedHashSet<Long> c = new java.util.LinkedHashSet<>();
		for (Op op : ops)
			if (op.pos() != Long.MIN_VALUE)
				c.add(net.minecraft.world.level.ChunkPos.pack(BlockPos.getX(op.pos()) >> 4, BlockPos.getZ(op.pos()) >> 4));
		return c;
	}

	Op proxima() {
		return ops.peekFirst();
	}

	void descartarPrimeira() {
		ops.pollFirst();
	}

	private boolean excluido(int x, int z) {
		if (excluir == null)
			return false;
		int dx = x - origem.getX(), dz = z - origem.getZ();
		for (LayoutColonia.Retangulo r : excluir)
			if (r.contem(dx, dz))
				return true;
		return false;
	}

	// ================================================================ Escrita segura (usada também fora do Lote)

	/** Escreve um bloco sem cascata de atualizações e sem soltar itens. */
	public static boolean escrever(ServerLevel level, BlockPos pos, BlockState state) {
		if (level.isOutsideBuildHeight(pos))
			return false;
		BlockState atual = level.getBlockState(pos);
		if (atual == state || atual.is(Blocks.BEDROCK))
			return false;
		return level.setBlock(pos, state, FLAGS);
	}

	// ================================================================ Operações

	/** Bloco em coordenadas absolutas. */
	public void bloco(BlockPos p, BlockState s) {
		if (excluido(p.getX(), p.getZ()))
			return;
		final BlockPos pos = p.immutable();
		if (conecta(s))
			conectar.add(pos.asLong());
		ops.add(new Op() {
			public int aplicar(ServerLevel level) {
				escrever(level, pos, s);
				return 1;
			}

			public long pos() {
				return pos.asLong();
			}
		});
	}

	/** Bloco relativo à origem. */
	public void r(int dx, int dy, int dz, BlockState s) {
		bloco(origem.offset(dx, dy, dz), s);
	}

	/** Coloca só se o lugar estiver livre (ar ou planta substituível) — folhas, decoração. */
	public void seLivre(BlockPos p, BlockState s) {
		if (excluido(p.getX(), p.getZ()))
			return;
		final BlockPos pos = p.immutable();
		ops.add(new Op() {
			public int aplicar(ServerLevel level) {
				BlockState atual = level.getBlockState(pos);
				if (atual.isAir())
					escrever(level, pos, s);
				return 1;
			}

			public long pos() {
				return pos.asLong();
			}
		});
	}

	/**
	 * Planta (flor, grama, muda, lanterna, vaso...): só é colocada se o lugar estiver livre E o bloco puder sobreviver
	 * ali ({@code canSurvive}) no momento em que a operação roda — ou seja, depois do suporte final.
	 */
	public void planta(BlockPos p, BlockState s) {
		if (excluido(p.getX(), p.getZ()))
			return;
		final BlockPos pos = p.immutable();
		ops.add(new Op() {
			public int aplicar(ServerLevel level) {
				BlockState atual = level.getBlockState(pos);
				if (!(atual.isAir() || atual.is(s.getBlock())) || !s.canSurvive(level, pos))
					return 1;
				if (s.getBlock() instanceof net.minecraft.world.level.block.DoublePlantBlock) {
					BlockPos cima = pos.above();
					if (!level.getBlockState(cima).isAir())
						return 1;
					escrever(level, pos, s.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
					escrever(level, cima, s.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
				} else
					escrever(level, pos, s);
				if (plantasColocadas != null)
					plantasColocadas.add(pos.asLong());
				return 2;
			}

			public long pos() {
				return pos.asLong();
			}
		});
	}

	/** Ação livre (texto de placa, ajuste de forma...). */
	public void acao(BlockPos ref, Consumer<ServerLevel> acao) {
		final long p = ref == null ? Long.MIN_VALUE : ref.asLong();
		ops.add(new Op() {
			public int aplicar(ServerLevel level) {
				acao.accept(level);
				return 4;
			}

			public long pos() {
				return p;
			}
		});
	}

	/**
	 * Acerta uma coluna de terreno: tudo acima de {@code alvo} sai DE CIMA PARA BAIXO (planta antes do suporte), o
	 * topo vira {@code topo} e os vazios logo abaixo são preenchidos com terra. {@code soFolhas}: só fixa as folhas
	 * (persistentes) da coluna, sem mexer no chão — usado na margem externa para as árvores cortadas não secarem.
	 */
	public void coluna(int x, int z, int alvo, BlockState topo, boolean soFolhas) {
		if (excluido(x, z))
			return;
		final long ref = BlockPos.asLong(x, alvo, z);
		ops.add(new Op() {
			public int aplicar(ServerLevel level) {
				return acertarColuna(level, x, z, alvo, topo, soFolhas);
			}

			public long pos() {
				return ref;
			}
		});
	}

	static boolean terreno(BlockState s) {
		return !s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced() && !s.is(BlockTags.LOGS) && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.FLOWERS)
				&& !s.is(BlockTags.SAPLINGS) && !s.is(Blocks.SUGAR_CANE) && !s.is(Blocks.CACTUS) && !s.is(Blocks.BAMBOO) && !s.is(Blocks.MOSS_CARPET);
	}

	/** Altura do chão natural atual (último bloco de "terreno", ignorando árvores e plantas). */
	static int chaoAtual(ServerLevel level, int x, int z) {
		int topo = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, topo, z);
		while (m.getY() > level.getMinY()) {
			if (terreno(level.getBlockState(m)))
				return m.getY();
			m.move(Direction.DOWN);
		}
		return level.getMinY();
	}

	static int acertarColuna(ServerLevel level, int x, int z, int alvo, BlockState topoBloco, boolean soFolhas) {
		int topo = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		int custo = 1;
		if (soFolhas) {
			for (int y = topo; y > alvo - 2 && y > level.getMinY(); y--) {
				m.set(x, y, z);
				BlockState s = level.getBlockState(m);
				if (s.getBlock() instanceof LeavesBlock && s.hasProperty(LeavesBlock.PERSISTENT) && !s.getValue(LeavesBlock.PERSISTENT)) {
					escrever(level, m, s.setValue(LeavesBlock.PERSISTENT, true));
					custo++;
				}
			}
			return custo;
		}
		// 1. De cima para baixo: plantas e folhas saem antes do bloco que as sustenta.
		for (int y = topo; y > alvo; y--) {
			m.set(x, y, z);
			if (!level.getBlockState(m).isAir()) {
				escrever(level, m, Blocks.AIR.defaultBlockState());
				custo++;
			}
		}
		// 2. Topo e preenchimento abaixo (água, vazios, plantas soterradas viram terra).
		m.set(x, alvo, z);
		escrever(level, m, topoBloco);
		for (int y = alvo - 1; y >= alvo - 16 && y > level.getMinY(); y--) {
			m.set(x, y, z);
			BlockState s = level.getBlockState(m);
			if (terreno(s))
				break;
			escrever(level, m, Blocks.DIRT.defaultBlockState());
			custo++;
		}
		return custo + 2;
	}

	/** Troca só o bloco do topo do chão atual (estradas fora da muralha, que acompanham o talude). */
	public void superficie(int x, int z, BlockState s) {
		if (excluido(x, z))
			return;
		final long ref = BlockPos.asLong(x, 0, z);
		ops.add(new Op() {
			public int aplicar(ServerLevel level) {
				int y = chaoAtual(level, x, z);
				BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, y + 1, z);
				// Tira de cima para baixo o que houver sobre o chão (grama alta, flores) antes de trocar o chão.
				for (int yy = y + 2; yy >= y + 1; yy--) {
					m.setY(yy);
					if (!level.getBlockState(m).isAir() && !terreno(level.getBlockState(m)))
						escrever(level, m, Blocks.AIR.defaultBlockState());
				}
				m.setY(y);
				escrever(level, m, s);
				return 3;
			}

			public long pos() {
				return ref;
			}
		});
	}

	// ================================================================ Peças compostas

	public void porta(BlockPos baixo, Direction frente, Block bloco) {
		BlockState s = bloco.defaultBlockState().setValue(DoorBlock.FACING, frente);
		bloco(baixo, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		bloco(baixo.above(), s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	public void cama(BlockPos pe, Direction direcao, Block bloco) {
		BlockState s = bloco.defaultBlockState().setValue(BedBlock.FACING, direcao);
		bloco(pe, s.setValue(BedBlock.PART, BedPart.FOOT));
		bloco(pe.relative(direcao), s.setValue(BedBlock.PART, BedPart.HEAD));
	}

	/** Placa de parede presa ao bloco atrás dela (que deve existir antes). Texto encerado. */
	public void placa(BlockPos pos, Direction frente, ChatFormatting cor, String... linhas) {
		bloco(pos, Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, frente));
		if (excluido(pos.getX(), pos.getZ()))
			return;
		final BlockPos p = pos.immutable();
		acao(p, level -> {
			if (level.getBlockEntity(p) instanceof SignBlockEntity sign) {
				SignText texto = new SignText();
				for (int i = 0; i < Math.min(4, linhas.length); i++)
					texto = texto.setMessage(i, Component.literal(linhas[i]).withStyle(cor));
				sign.setText(texto, true);
				sign.setWaxed(true);
				sign.setChanged();
				level.sendBlockUpdated(p, sign.getBlockState(), sign.getBlockState(), Block.UPDATE_CLIENTS);
			}
		});
	}

	private static boolean conecta(BlockState s) {
		Block b = s.getBlock();
		return b instanceof FenceBlock || b instanceof IronBarsBlock || b instanceof WallBlock || b instanceof StairBlock || b instanceof FenceGateBlock;
	}

	/** Recalcula a forma dos blocos que se conectam (cercas, vidraças, muros, escadas) — sem cascata. */
	public void finalizar() {
		if (conectar.isEmpty())
			return;
		final long[] lista = conectar.stream().mapToLong(Long::longValue).toArray();
		conectar.clear();
		// Agrupa por chunk (até 64 por ação), para a obra carregar o chunk certo antes de cada grupo.
		java.util.Arrays.sort(lista);
		java.util.List<long[]> grupos = new java.util.ArrayList<>();
		java.util.Map<Long, java.util.List<Long>> porChunk = new java.util.LinkedHashMap<>();
		for (long p : lista)
			porChunk.computeIfAbsent(net.minecraft.world.level.ChunkPos.pack(BlockPos.of(p)), k -> new java.util.ArrayList<>()).add(p);
		for (java.util.List<Long> g : porChunk.values())
			for (int i = 0; i < g.size(); i += 64)
				grupos.add(g.subList(i, Math.min(g.size(), i + 64)).stream().mapToLong(Long::longValue).toArray());
		for (long[] grupo : grupos) {
			final long[] lista2 = grupo;
			final int ini = 0, fim = grupo.length;
			acao(BlockPos.of(lista2[ini]), level -> {
				for (int k = ini; k < fim; k++) {
					BlockPos p = BlockPos.of(lista2[k]);
					BlockState s = level.getBlockState(p);
					if (!conecta(s))
						continue;
					BlockState n = Block.updateFromNeighbourShapes(s, level, p);
					if (n != s && !n.isAir())
						level.setBlock(p, n, FLAGS);
				}
			});
		}
	}

	/** Executa tudo agora (construção síncrona das zonas essenciais e testes). */
	public int executarTudo(ServerLevel level) {
		finalizar();
		int custo = 0;
		while (!ops.isEmpty())
			custo += ops.pollFirst().aplicar(level);
		return custo;
	}
}
