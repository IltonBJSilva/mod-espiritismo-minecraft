package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

/**
 * API pública da área "populacao" para as outras áreas e o integrador.
 * <ul>
 * <li>{@link #colocarAmelie} — põe Amélie Boudet (uma só por nível) na casa de Kardec.</li>
 * <li>{@link #garantirPersonagem} — garante um personagem nomeado de Nosso Lar num ponto.</li>
 * <li>{@link #registrarLeitura} — para a área "biblioteca" avisar que o jogador leu uma obra (se tiver fluxo próprio).</li>
 * <li>{@link #ehMorador}, {@link #perfil}.</li>
 * </ul>
 */
public final class PopulacaoApi {
	private PopulacaoApi() {
	}

	/**
	 * Coloca Amélie Gabrielle Boudet em {@code pos} (ex.: dentro da estrutura {@code casa_kardec}). Idempotente: se ela já
	 * existe e está carregada neste nível, devolve a existente (sem duplicar); se o registro aponta para uma Amélie
	 * descarregada noutro lugar, nada é criado (a oficial continua sendo aquela). Se a oficial for removida
	 * (/kill), o registro é liberado e uma nova chamada a recoloca.
	 *
	 * @return a Amélie oficial carregada, ou null se ela existe mas está num chunk descarregado
	 */
	@Nullable
	public static MoradorEntity colocarAmelie(ServerLevel level, BlockPos pos) {
		MoradorEntity existente = Povoamento.nomeado(level, Perfis.AMELIE);
		if (existente != null)
			return existente;
		if (Povoamento.registro(level).containsKey(Perfis.AMELIE))
			return null; // já existe uma oficial (descarregada noutro lugar): um de cada
		Perfil p = Perfis.get(Perfis.AMELIE);
		MoradorEntity m = Povoamento.garantir(level, p, pos, RegistroPopulacao.AMELIE.get());
		if (m != null)
			m.setHomeTo(m.blockPosition(), 6);
		return m;
	}

	/** Garante o personagem nomeado {@code perfilId} (ver {@link Perfis}) perto de {@code pos}. */
	@Nullable
	public static MoradorEntity garantirPersonagem(ServerLevel level, String perfilId, BlockPos pos) {
		Perfil p = Perfis.get(perfilId);
		return p == null || !p.nomeado() ? null : Povoamento.garantir(level, p, pos);
	}

	/** Registra que o jogador leu a obra {@code obraId} (id do item, ex.: "o_livro_dos_espiritos"). */
	public static void registrarLeitura(Player player, String obraId) {
		if (player.level().isClientSide() || obraId == null || obraId.isEmpty())
			return;
		Memoria.marcar(player, "leu:" + obraId);
		JornadaRecemChegado.aoLer(player);
	}

	public static boolean leu(Player player, String obraId) {
		return Memoria.leu(player, obraId);
	}

	public static boolean ehMorador(Entity e) {
		return e instanceof MoradorEntity;
	}

	@Nullable
	public static Perfil perfil(Entity e) {
		return e instanceof MoradorEntity m ? m.perfil() : null;
	}

	/** Etapa da jornada do recém-chegado (0–7) e da cadeia de Amélie (0–4). */
	public static int etapaRecemChegado(Player p) {
		return JornadaRecemChegado.etapa(p);
	}

	public static int etapaAmelie(Player p) {
		return MissoesAmelie.etapa(p);
	}
}
