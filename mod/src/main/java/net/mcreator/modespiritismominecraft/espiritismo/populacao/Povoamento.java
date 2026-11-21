package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;

import javax.annotation.Nullable;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Povoamento de Nosso Lar por proximidade (§3.5 / §14.2):
 * <ul>
 * <li>A cada {@value #INTERVALO} ticks, por jogador em Nosso Lar: no máximo UM morador genérico novo, numa zona a até
 * {@value #RAIO_ZONA} blocos, respeitando a cota da zona ({@link Zonas#COTA}) e o limite da região
 * ({@value #MAX_GENERICOS_REGIAO} num raio de 96 blocos).</li>
 * <li>Genéricos são "temporários": não são salvos no disco e somem quando não há jogador a {@value MoradorEntity#RAIO_DESPAWN}
 * blocos. Logo, recarregar o mundo nunca os duplica.</li>
 * <li>Personagens nomeados: um de cada por nível. O UUID oficial fica no attachment {@link RegistroPopulacao#PERSONAGENS};
 * qualquer outra cópia que tente entrar no nível é recusada ({@link #duplicata}).</li>
 * </ul>
 */
public final class Povoamento {
	private Povoamento() {
	}

	public static final int INTERVALO = 40;
	public static final int RAIO_ZONA = 64;
	public static final int RAIO_NOMEADO = 80;
	public static final int RAIO_REGIAO = 96;
	public static final int MAX_GENERICOS_REGIAO = 24;

	public static void tick(Player p) {
		if (!(p.level() instanceof ServerLevel l) || l.dimension() != Planos.NOSSO_LAR || p.isSpectator())
			return;
		garantirNomeados(l, p);
		povoar(l, p);
	}

	// ---------------------------------------------------------------- Genéricos

	/** Cria no máximo um genérico perto do jogador. Devolve o criado (ou null). */
	@Nullable
	public static MoradorEntity povoar(ServerLevel l, Player p) {
		List<MoradorEntity> regiao = l.getEntitiesOfClass(MoradorEntity.class, p.getBoundingBox().inflate(RAIO_REGIAO), MoradorEntity::temporario);
		if (regiao.size() >= MAX_GENERICOS_REGIAO)
			return null;
		List<String> zonas = new ArrayList<>(Zonas.COTA.keySet());
		int inicio = l.getRandom().nextInt(zonas.size());
		for (int k = 0; k < zonas.size(); k++) {
			String zona = zonas.get((inicio + k) % zonas.size());
			BlockPos c = Zonas.posicao(l, zona);
			if (c == null || distH2(p, c) > RAIO_ZONA * RAIO_ZONA || !l.isPositionEntityTicking(c))
				continue;
			long n = regiao.stream().filter(m -> zona.equals(m.zona())).count();
			if (n >= Zonas.COTA.getOrDefault(zona, 3))
				continue;
			BlockPos pos = Zonas.chao(l, c, l.getRandom(), 6);
			if (pos == null || pos.distSqr(p.blockPosition()) < 36)
				continue; // não surge colado no jogador
			return criarGenerico(l, zona, pos);
		}
		return null;
	}

	@Nullable
	public static MoradorEntity criarGenerico(ServerLevel l, String zona, BlockPos pos) {
		MoradorEntity m = RegistroPopulacao.MORADOR.get().create(l, EntitySpawnReason.EVENT);
		if (m == null)
			return null;
		m.definirZona(zona);
		m.definirPerfil(Perfis.genericoPara(zona, l.getRandom()));
		m.marcarTemporario();
		m.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, l.getRandom().nextFloat() * 360F, 0);
		m.setHomeTo(pos, 8);
		l.addFreshEntity(m);
		return m;
	}

	/** Quantos genéricos temporários há num raio (para testes e depuração). */
	public static int contarGenericos(ServerLevel l, BlockPos centro, int raio) {
		return l.getEntitiesOfClass(MoradorEntity.class, new AABB(centro).inflate(raio), MoradorEntity::temporario).size();
	}

	// ---------------------------------------------------------------- Nomeados (um de cada)

	public static Map<String, String> registro(ServerLevel l) {
		return l.getData(RegistroPopulacao.PERSONAGENS);
	}

	static void registrar(ServerLevel l, String perfil, @Nullable UUID uuid) {
		Map<String, String> m = new HashMap<>(registro(l));
		if (uuid == null)
			m.remove(perfil);
		else
			m.put(perfil, uuid.toString());
		l.setData(RegistroPopulacao.PERSONAGENS, m);
	}

	/** A entidade oficial do personagem, se estiver carregada. */
	@Nullable
	public static MoradorEntity nomeado(ServerLevel l, String perfil) {
		String u = registro(l).get(perfil);
		if (u == null)
			return null;
		Entity e;
		try {
			e = l.getEntity(UUID.fromString(u));
		} catch (IllegalArgumentException ex) {
			return null;
		}
		return e instanceof MoradorEntity m && m.isAlive() && perfil.equals(m.perfilId()) ? m : null;
	}

	static void garantirNomeados(ServerLevel l, Player p) {
		for (Perfil perfil : Perfis.nomeadosNossoLar()) {
			BlockPos alvo = Zonas.posicao(l, perfil.local());
			if (alvo == null || distH2(p, alvo) > RAIO_NOMEADO * RAIO_NOMEADO || !l.isPositionEntityTicking(alvo))
				continue;
			garantir(l, perfil, alvo);
		}
	}

	/** Garante UM personagem nomeado perto de {@code alvo} (área carregada). Idempotente. */
	@Nullable
	public static MoradorEntity garantir(ServerLevel l, Perfil perfil, BlockPos alvo) {
		return garantir(l, perfil, alvo, perfil.categoria() == Perfil.Categoria.HISTORICO && Perfis.AMELIE.equals(perfil.id())
				? RegistroPopulacao.AMELIE.get() : RegistroPopulacao.PERSONAGEM.get());
	}

	@Nullable
	static MoradorEntity garantir(ServerLevel l, Perfil perfil, BlockPos alvo, EntityType<MoradorEntity> tipo) {
		MoradorEntity m = nomeado(l, perfil.id());
		if (m != null) {
			// Perdeu-se longe do seu lugar (ex.: a colônia mudou o ponto da zona): volta quando ninguém está olhando.
			if (perdido(l, perfil, m.blockPosition(), alvo) && l.getNearestPlayer(m, 24) == null) {
				BlockPos pos = Zonas.chao(l, alvo, l.getRandom(), 4);
				if (pos != null) {
					m.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, m.getYRot(), 0);
					m.setHomeTo(pos, 8);
				}
			}
			return m;
		}
		for (MoradorEntity o : l.getEntitiesOfClass(MoradorEntity.class, new AABB(alvo).inflate(64), o -> o.isAlive() && perfil.id().equals(o.perfilId()))) {
			registrar(l, perfil.id(), o.getUUID()); // registro perdido: adota o que já existe
			return o;
		}
		BlockPos pos = Zonas.chao(l, alvo, l.getRandom(), 4);
		if (pos == null)
			pos = alvo;
		MoradorEntity novo = tipo.create(l, EntitySpawnReason.EVENT);
		if (novo == null)
			return null;
		novo.definirPerfil(perfil);
		novo.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, l.getRandom().nextFloat() * 360F, 0);
		novo.setHomeTo(pos, 8);
		registrar(l, perfil.id(), novo.getUUID()); // registra ANTES de entrar no nível (a checagem de duplicata aceita)
		l.addFreshEntity(novo);
		return novo;
	}

	/** Distância (horizontal) além da qual o personagem é considerado longe de uma zona do perfil. */
	public static final int RAIO_PERDIDO = 96;

	/**
	 * O personagem só está "perdido" se estiver longe de TODAS as zonas do seu perfil (local, descanso, rotina e,
	 * para quem anda, as zonas do tipo). Assim a ida ao descanso ou à rotina não o faz ser puxado de volta.
	 */
	public static boolean perdido(ServerLevel l, Perfil perfil, BlockPos pos, BlockPos local) {
		if (perto(pos, local))
			return false;
		List<String> zonas = new ArrayList<>();
		zonas.add(perfil.descanso());
		zonas.addAll(perfil.rotina());
		if (perfil.tipo().atividade == TipoNpc.Atividade.ANDAR)
			zonas.addAll(perfil.tipo().zonas);
		for (String z : zonas) {
			if (z == null || z.isEmpty())
				continue;
			BlockPos c = Zonas.posicao(l, z);
			if (c != null && perto(pos, c))
				return false;
		}
		return true;
	}

	private static boolean perto(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz <= (double) RAIO_PERDIDO * RAIO_PERDIDO;
	}

	/**
	 * Ao entrar no nível (criação ou carregamento do disco): se for personagem nomeado e já existir outro "oficial",
	 * esta cópia é recusada. Se não houver registro, esta passa a ser a oficial.
	 *
	 * @return true se a entidade é duplicata e não deve entrar
	 */
	public static boolean duplicata(ServerLevel l, MoradorEntity m) {
		if (!m.nomeado())
			return false;
		String u = registro(l).get(m.perfilId());
		if (u == null) {
			registrar(l, m.perfilId(), m.getUUID());
			return false;
		}
		return !u.equals(m.getUUID().toString());
	}

	/** Ao sair do nível: se o oficial foi morto/removido (não apenas descarregado), libera o registro. */
	public static void aoSair(ServerLevel l, MoradorEntity m) {
		Entity.RemovalReason r = m.getRemovalReason();
		if (!m.nomeado() || r == null || !r.shouldDestroy())
			return;
		if (m.getUUID().toString().equals(registro(l).get(m.perfilId())))
			registrar(l, m.perfilId(), null);
	}

	private static double distH2(Player p, BlockPos c) {
		double dx = p.getX() - (c.getX() + 0.5), dz = p.getZ() - (c.getZ() + 0.5);
		return dx * dx + dz * dz;
	}
}
