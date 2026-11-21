package net.mcreator.modespiritismominecraft.espiritismo.dimensao;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.Map;
import java.util.HashMap;

/**
 * Construções feitas por código na primeira chegada a cada plano (no estilo de {@code CentroEspirita}).
 * As posições importantes ficam no attachment {@link RegistroDimensao#PONTOS} do próprio Level.
 * <p>
 * Nosso Lar (centro da praça na origem): ao norte o Ministério da Regeneração com as Câmaras de Retificação
 * (onde se acorda), a oeste o Ministério do Auxílio (planejamento reencarnatório), a leste o da Comunicação,
 * ao sul Esclarecimento, Elevação e União Divina; um Mirante a nordeste e um bosque a oeste.
 * <p>
 * Umbral: movido para {@link ConstrucoesUmbral} (vários Postos de Socorro, construção incremental).
 */
public final class ConstrucoesPlanos {
	private static final int FLAGS = Block.UPDATE_CLIENTS;
	private static final int RAIO = 46;

	private ConstrucoesPlanos() {
	}

	// ================================================================ Utilidades

	static void set(ServerLevel level, BlockPos pos, BlockState state) {
		if (level.isOutsideBuildHeight(pos))
			return;
		BlockState atual = level.getBlockState(pos);
		if (atual == state || atual.is(Blocks.BEDROCK))
			return;
		level.setBlock(pos, state, FLAGS);
	}

	/** Altura do chão (último bloco sólido) na coluna, ignorando folhas. */
	static int chao(ServerLevel level, int x, int z) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
	}

	/** Acerta uma coluna: chão em y com {@code topo}, terra embaixo onde houver vazio/água, ar acima. */
	private static void acertarColuna(ServerLevel level, int x, int z, int y, BlockState topo, BlockState sub) {
		int alto = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
		for (int yy = y + 1; yy <= Math.max(alto, y + 1); yy++)
			set(level, new BlockPos(x, yy, z), Blocks.AIR.defaultBlockState());
		set(level, new BlockPos(x, y, z), topo);
		for (int yy = y - 1; yy >= y - 8; yy--) {
			BlockPos p = new BlockPos(x, yy, z);
			BlockState s = level.getBlockState(p);
			if (!s.canBeReplaced() && s.getFluidState().isEmpty())
				break;
			set(level, p, sub);
		}
	}

	static void placa(ServerLevel level, BlockPos pos, Direction frente, ChatFormatting cor, String... linhas) {
		set(level, pos, Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, frente));
		if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
			SignText texto = new SignText();
			for (int i = 0; i < Math.min(4, linhas.length); i++)
				texto = texto.setMessage(i, Component.literal(linhas[i]).withStyle(cor));
			sign.setText(texto, true);
			sign.setWaxed(true);
			sign.setChanged();
			level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), FLAGS);
		}
	}

	private static void cama(ServerLevel level, BlockPos pe, Direction direcao, Block bloco) {
		BlockState s = bloco.defaultBlockState().setValue(BedBlock.FACING, direcao);
		set(level, pe, s.setValue(BedBlock.PART, BedPart.FOOT));
		set(level, pe.relative(direcao), s.setValue(BedBlock.PART, BedPart.HEAD));
	}

	private static void poste(ServerLevel level, BlockPos base, BlockState coluna, BlockState luz, int altura) {
		for (int i = 0; i < altura; i++)
			set(level, base.above(i), coluna);
		set(level, base.above(altura), luz);
	}

	private static void arvore(ServerLevel level, BlockPos base, RandomSource r, BlockState tronco, BlockState folha) {
		int h = 4 + r.nextInt(3);
		for (int y = 0; y < h; y++)
			set(level, base.above(y), tronco);
		for (int dy = h - 2; dy <= h + 1; dy++) {
			int raio = dy >= h ? 1 : 2;
			for (int dx = -raio; dx <= raio; dx++)
				for (int dz = -raio; dz <= raio; dz++) {
					if (Math.abs(dx) == raio && Math.abs(dz) == raio && (dy == h + 1 || r.nextBoolean()))
						continue;
					BlockPos p = base.offset(dx, dy, dz);
					if (level.getBlockState(p).isAir())
						set(level, p, folha);
				}
		}
	}

	/**
	 * Prédio retangular de paredes claras centrado em (cx, cz), piso em y. A porta (arco aberto 1x2) fica no lado
	 * {@code porta}, com a placa do nome acima. Retorna o centro do piso interno (y + 1).
	 */
	private static BlockPos predio(ServerLevel level, int cx, int y, int cz, int larg, int prof, int altura, Direction porta, BlockState parede, BlockState telhado,
			String... nome) {
		int x0 = cx - larg / 2, z0 = cz - prof / 2, x1 = x0 + larg - 1, z1 = z0 + prof - 1;
		BlockState piso = Blocks.SMOOTH_QUARTZ.defaultBlockState();
		BlockState pilar = Blocks.QUARTZ_PILLAR.defaultBlockState();
		for (int x = x0; x <= x1; x++)
			for (int z = z0; z <= z1; z++) {
				set(level, new BlockPos(x, y, z), piso);
				boolean borda = x == x0 || x == x1 || z == z0 || z == z1;
				boolean canto = (x == x0 || x == x1) && (z == z0 || z == z1);
				for (int h = 1; h <= altura; h++) {
					BlockState s;
					if (!borda)
						s = Blocks.AIR.defaultBlockState();
					else if (canto)
						s = pilar;
					else if ((h == 2 || h == 3) && ((x - x0) % 3 == 1 || (z - z0) % 3 == 1))
						s = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
					else
						s = parede;
					set(level, new BlockPos(x, y + h, z), s);
				}
				set(level, new BlockPos(x, y + altura + 1, z), borda ? Blocks.QUARTZ_SLAB.defaultBlockState() : telhado);
			}
		// Luz interna suave
		for (int x = x0 + 2; x <= x1 - 2; x += 3)
			for (int z = z0 + 2; z <= z1 - 2; z += 3)
				set(level, new BlockPos(x, y + altura + 1, z), Blocks.SEA_LANTERN.defaultBlockState());
		// Porta (arco aberto) e placa
		BlockPos centroParede = switch (porta) {
			case NORTH -> new BlockPos(cx, y + 1, z0);
			case SOUTH -> new BlockPos(cx, y + 1, z1);
			case WEST -> new BlockPos(x0, y + 1, cz);
			default -> new BlockPos(x1, y + 1, cz);
		};
		set(level, centroParede, Blocks.AIR.defaultBlockState());
		set(level, centroParede.above(), Blocks.AIR.defaultBlockState());
		set(level, centroParede.above(2), parede);
		placa(level, centroParede.above(2).relative(porta), porta, ChatFormatting.DARK_BLUE, nome);
		return new BlockPos(cx, y + 1, cz);
	}

	// ================================================================ Nosso Lar

	/** Constrói a área de chegada de Nosso Lar com o centro da praça em {@code o} (y = chão). Retorna os pontos. */
	public static Map<String, Long> construirNossoLar(ServerLevel level, BlockPos o) {
		int g = o.getY();
		RandomSource r = RandomSource.create(1944L); // Nosso Lar foi publicado em 1944
		BlockState grama = Blocks.GRASS_BLOCK.defaultBlockState();
		BlockState terra = Blocks.DIRT.defaultBlockState();
		BlockState caminho = Blocks.CALCITE.defaultBlockState();

		// 1. Terreno: chão plano e gramado num quadrado de raio RAIO, avenidas em cruz
		for (int x = -RAIO; x <= RAIO; x++)
			for (int z = -RAIO; z <= RAIO; z++) {
				boolean avenida = Math.abs(x) <= 1 || Math.abs(z) <= 1;
				acertarColuna(level, o.getX() + x, o.getZ() + z, g, avenida ? caminho : grama, terra);
			}

		// 2. Praça central com fonte
		for (int x = -9; x <= 9; x++)
			for (int z = -9; z <= 9; z++) {
				boolean anel = Math.max(Math.abs(x), Math.abs(z)) == 9;
				set(level, o.offset(x, 0, z), anel ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : ((x + z) % 2 == 0 ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : caminho));
			}
		for (int x = -2; x <= 2; x++)
			for (int z = -2; z <= 2; z++) {
				boolean borda = Math.abs(x) == 2 || Math.abs(z) == 2;
				set(level, o.offset(x, 0, z), Blocks.QUARTZ_BRICKS.defaultBlockState());
				set(level, o.offset(x, 1, z), borda ? Blocks.QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM) : Blocks.WATER.defaultBlockState());
			}
		set(level, o.offset(0, 1, 0), Blocks.QUARTZ_PILLAR.defaultBlockState());
		set(level, o.offset(0, 2, 0), Blocks.QUARTZ_PILLAR.defaultBlockState());
		set(level, o.offset(0, 3, 0), Blocks.SEA_LANTERN.defaultBlockState());
		// Bancos e canteiros na praça
		for (int[] b : new int[][]{{-6, -4, 0}, {-6, 4, 0}, {6, -4, 1}, {6, 4, 1}})
			for (int dz = 0; dz < 2; dz++)
				set(level, o.offset(b[0], 1, b[1] + dz), Blocks.BIRCH_STAIRS.defaultBlockState().setValue(StairBlock.FACING, b[2] == 0 ? Direction.WEST : Direction.EAST));
		for (int[] c : new int[][]{{-6, -7}, {6, -7}, {-6, 7}, {6, 7}}) {
			set(level, o.offset(c[0], 0, c[1]), grama);
			set(level, o.offset(c[0], 1, c[1]), flor(r));
		}
		for (int[] p : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}})
			poste(level, o.offset(p[0], 1, p[1]), Blocks.QUARTZ_PILLAR.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState(), 3);
		set(level, o.offset(0, 1, -6), Blocks.QUARTZ_BRICKS.defaultBlockState());
		set(level, o.offset(0, 2, -6), Blocks.QUARTZ_BRICKS.defaultBlockState());
		placa(level, o.offset(0, 2, -7), Direction.NORTH, ChatFormatting.DARK_GREEN, "Nosso Lar", "colônia", "espiritual", "(André Luiz)");

		// 3. Postes ao longo das avenidas
		for (int d = 14; d <= 40; d += 9)
			for (int[] s : new int[][]{{d, 2}, {d, -2}, {-d, 2}, {-d, -2}, {2, d}, {-2, d}, {2, -d}, {-2, -d}})
				poste(level, o.offset(s[0], 1, s[1]), Blocks.QUARTZ_PILLAR.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState(), 2);

		Map<String, Long> pontos = new HashMap<>();

		// 4. Ministério da Regeneração — Câmaras de Retificação (onde se desperta)
		BlockPos hosp = predio(level, o.getX(), g, o.getZ() - 24, 15, 11, 5, Direction.SOUTH, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
				"Ministério da", "Regeneração", "Câmaras de", "Retificação");
		for (int x = -6; x <= 6; x += 3)
			if (x != 0)
				cama(level, hosp.offset(x, 0, -1), Direction.NORTH, Blocks.WHITE_BED);
		for (int x : new int[]{-6, 6})
			set(level, hosp.offset(x, 0, 3), Blocks.POTTED_LILY_OF_THE_VALLEY.defaultBlockState());
		placa(level, hosp.offset(0, 2, -4), Direction.SOUTH, ChatFormatting.DARK_BLUE, "Câmaras de", "Retificação", "Aqui se refaz", "o recém-chegado");
		BlockPos chegada = hosp.offset(0, 0, 2);
		pontos.put("chegada", chegada.asLong());

		// 5. Ministério do Auxílio — planejamento reencarnatório
		BlockPos aux = predio(level, o.getX() - 24, g, o.getZ(), 11, 11, 4, Direction.EAST, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.QUARTZ_BLOCK.defaultBlockState(),
				"Ministério do", "Auxílio", "", "Reencarnação");
		set(level, aux, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST));
		placa(level, aux.offset(-4, 1, 0), Direction.EAST, ChatFormatting.DARK_BLUE, "Planejamento", "reencarnatório", "Clique no", "atril (púlpito)");
		pontos.put("auxilio", aux.asLong());

		// 6. Demais Ministérios
		predio(level, o.getX() + 24, g, o.getZ(), 11, 11, 4, Direction.WEST, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.QUARTZ_BLOCK.defaultBlockState(),
				"Ministério da", "Comunicação", "", "");
		predio(level, o.getX() - 15, g, o.getZ() + 22, 9, 9, 4, Direction.NORTH, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.QUARTZ_BLOCK.defaultBlockState(),
				"Ministério do", "Esclarecimento", "", "");
		predio(level, o.getX() + 15, g, o.getZ() + 22, 9, 9, 4, Direction.NORTH, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.QUARTZ_BLOCK.defaultBlockState(),
				"Ministério da", "Elevação", "", "");
		BlockPos uniao = predio(level, o.getX(), g, o.getZ() + 36, 9, 9, 6, Direction.NORTH, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
				"Ministério da", "União Divina", "", "");
		set(level, uniao.offset(0, 0, 2), Blocks.POTTED_LILY_OF_THE_VALLEY.defaultBlockState());
		// Estantes no Esclarecimento
		for (int x = -2; x <= 2; x++)
			set(level, new BlockPos(o.getX() - 15 + x, g + 1, o.getZ() + 25), Blocks.BOOKSHELF.defaultBlockState());

		// 7. Mirante (nordeste): terraço elevado com as escalas
		construirMirante(level, o.offset(32, 0, -26), pontos);

		// 8. Bosque a oeste
		for (int x = -44; x <= -32; x += 4)
			for (int z = -40; z <= 40; z += 5) {
				if (Math.abs(z) <= 3)
					continue;
				int jx = r.nextInt(3) - 1, jz = r.nextInt(3) - 1;
				boolean betula = r.nextInt(3) == 0;
				arvore(level, o.offset(x + jx, 1, z + jz), r, (betula ? Blocks.BIRCH_LOG : Blocks.OAK_LOG).defaultBlockState(),
						(betula ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
			}
		// Flores espalhadas pelos gramados
		for (int i = 0; i < 260; i++) {
			int x = r.nextInt(2 * RAIO + 1) - RAIO, z = r.nextInt(2 * RAIO + 1) - RAIO;
			BlockPos p = o.offset(x, 1, z);
			if (level.getBlockState(p.below()).is(Blocks.GRASS_BLOCK) && level.getBlockState(p).isAir())
				set(level, p, r.nextInt(3) == 0 ? Blocks.SHORT_GRASS.defaultBlockState() : flor(r));
		}

		pontos.put("praca", o.above().asLong());
		return pontos;
	}

	private static BlockState flor(RandomSource r) {
		Block[] flores = {Blocks.POPPY, Blocks.DANDELION, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.ALLIUM, Blocks.WHITE_TULIP};
		return flores[r.nextInt(flores.length)].defaultBlockState();
	}

	private static void construirMirante(ServerLevel level, BlockPos c, Map<String, Long> pontos) {
		int topo = 5; // o piso do terraço fica em c.y + 5
		BlockState pedra = Blocks.QUARTZ_BRICKS.defaultBlockState();
		for (int x = -3; x <= 3; x++)
			for (int z = -3; z <= 3; z++)
				for (int y = 1; y <= topo; y++)
					set(level, c.offset(x, y, z), (y == topo) ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : pedra);
		// Escada a oeste, subindo para leste
		for (int i = 0; i < topo; i++) {
			BlockPos p = c.offset(-8 + i, 1 + i, 0);
			for (int dz = -1; dz <= 1; dz++) {
				set(level, p.offset(0, 0, dz), Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
				for (int y = 1; y <= i; y++)
					set(level, p.offset(0, -y, dz), pedra);
			}
		}
		// Parapeito: mureta ao norte e a leste (placas), cerca no sul
		for (int x = -3; x <= 3; x++) {
			set(level, c.offset(x, topo + 1, -3), pedra);
			set(level, c.offset(x, topo + 2, -3), pedra);
			set(level, c.offset(x, topo + 1, 3), Blocks.BIRCH_FENCE.defaultBlockState());
		}
		for (int z = -3; z <= 3; z++) {
			set(level, c.offset(3, topo + 1, z), pedra);
			set(level, c.offset(3, topo + 2, z), pedra);
			if (z != 0)
				set(level, c.offset(-3, topo + 1, z), Blocks.BIRCH_FENCE.defaultBlockState());
		}
		// Escala dos mundos (O Evangelho segundo o Espiritismo, cap. III, item 4)
		String[][] mundos = {{"Mundos", "primitivos", "primeiras", "encarnações"}, {"Mundos de", "expiação", "e provas", "(a Terra)"},
				{"Mundos de", "regeneração", "as almas se", "renovam"}, {"Mundos ditosos", "ou felizes", "o bem supera", "o mal"},
				{"Mundos celestes", "ou divinos", "morada dos", "Espíritos puros"}};
		for (int i = 0; i < 5; i++)
			placa(level, c.offset(-2 + i, topo + 2, -2), Direction.SOUTH, ChatFormatting.DARK_BLUE, mundos[i]);
		placa(level, c.offset(0, topo + 1, -2), Direction.SOUTH, ChatFormatting.DARK_GREEN, "Escala dos", "mundos", "ESE, cap. III", "");
		// Escala espírita (O Livro dos Espíritos, q. 100–113)
		String[][] ordens = {{"3ª ordem", "Espíritos", "imperfeitos", "LE q. 101-106"}, {"2ª ordem", "Bons", "Espíritos", "LE q. 107-111"},
				{"1ª ordem", "Espíritos", "puros", "LE q. 112-113"}};
		for (int i = 0; i < 3; i++)
			placa(level, c.offset(2, topo + 2, 1 - i), Direction.WEST, ChatFormatting.DARK_PURPLE, ordens[i]);
		placa(level, c.offset(2, topo + 1, 0), Direction.WEST, ChatFormatting.DARK_GREEN, "Escala", "espírita", "LE q. 100", "");
		BlockPos atril = c.offset(0, topo + 1, 0);
		set(level, atril, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.WEST));
		pontos.put("mirante", atril.asLong());
		pontos.put("mirante_npc", c.offset(-1, topo + 1, 1).asLong());
		for (int[] l : new int[][]{{-3, -3}, {3, 3}, {3, -3}})
			set(level, c.offset(l[0], topo + 3, l[1]), Blocks.LANTERN.defaultBlockState());
	}

	// ================================================================ NPCs de outra área (lookup por ID)

	/** Tipo de entidade de outro agente por ID; null se não existir. */
	public static EntityType<?> tipo(String id) {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, id)).orElse(null);
	}

	/** Coloca um NPC (por ID) se ainda não houver um daquele tipo perto. Só age com o chunk ativo, para não duplicar. */
	public static Entity colocarSeFaltar(ServerLevel level, String id, BlockPos pos, float yRot) {
		EntityType<?> tipo = tipo(id);
		if (tipo == null || !level.isPositionEntityTicking(pos))
			return null;
		if (!level.getEntities(tipo, new AABB(pos).inflate(12), e -> true).isEmpty())
			return null;
		return colocar(level, tipo, pos, yRot);
	}

	public static Entity colocar(ServerLevel level, EntityType<?> tipo, BlockPos pos, float yRot) {
		Entity e = tipo.create(level, EntitySpawnReason.STRUCTURE);
		if (e == null)
			return null;
		e.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yRot, 0F);
		if (e instanceof Mob mob) {
			mob.setYHeadRot(yRot);
			mob.setYBodyRot(yRot);
			mob.setPersistenceRequired();
			mob.setHomeTo(pos, 4);
		}
		level.addFreshEntity(e);
		return e;
	}

	/** Verifica os NPCs fixos do plano (chamado de tempos em tempos com jogador por perto). */
	public static void verificarNpcs(ServerLevel level, Map<String, Long> pontos, boolean nossoLar) {
		if (nossoLar && pontos.containsKey("auxilio")) {
			BlockPos atril = BlockPos.of(pontos.get("auxilio"));
			if (level.isLoaded(atril) && !level.getBlockState(atril).is(net.minecraft.world.level.block.Blocks.LECTERN))
				level.setBlock(atril, net.minecraft.world.level.block.Blocks.LECTERN.defaultBlockState(), 3);
		}
		if (nossoLar) {
			if (pontos.containsKey("mirante_npc"))
				colocarSeFaltar(level, "mentor", BlockPos.of(pontos.get("mirante_npc")), 90F);
			if (pontos.containsKey("chegada"))
				colocarSeFaltar(level, "socorrista", BlockPos.of(pontos.get("chegada")).north(2).east(2), 180F);
			if (pontos.containsKey("praca"))
				colocarSeFaltar(level, "orador", BlockPos.of(pontos.get("praca")).offset(4, 0, 4), 0F);
		}
		// (Umbral: ver ConstrucoesUmbral.verificarNpcs)
	}
}
