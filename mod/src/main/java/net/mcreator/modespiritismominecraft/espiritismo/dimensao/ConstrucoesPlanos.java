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
	/** Sem cascata de atualização de forma e sem soltar itens (ver colonia.Lote: causa do bug das flores). */
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
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
		// De cima para baixo: a planta sai antes do bloco que a sustenta (nunca "cai" como item).
		for (int yy = Math.max(alto, y + 1); yy >= y + 1; yy--)
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

	/**
	 * Nosso Lar agora é construída pela área "colonia" ({@code colonia.ConstrucaoColonia}): as zonas essenciais
	 * (Câmaras de Retificação, atril do Ministério do Auxílio, Mirante) ficam prontas na hora e o resto da cidade é
	 * erguido aos poucos no tick do servidor. {@code o} = centro sugerido (x, z); a altura é recalculada.
	 * Devolve todos os pontos nomeados (inclui "chegada", "auxilio", "mirante", "mirante_npc", "praca").
	 */
	public static Map<String, Long> construirNossoLar(ServerLevel level, BlockPos o) {
		return net.mcreator.modespiritismominecraft.espiritismo.colonia.ConstrucaoColonia.construir(level, o);
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
