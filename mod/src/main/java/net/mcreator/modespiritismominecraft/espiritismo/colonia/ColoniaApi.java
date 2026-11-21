package net.mcreator.modespiritismominecraft.espiritismo.colonia;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.protecao.ZonasProtegidas;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.RegistroDimensao;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;

import java.util.Map;

/**
 * CONTRATO entre as áreas: consultas sobre a colônia Nosso Lar.
 * Assinaturas estáveis; a área "colonia" implementa (muralha, setores, locais nomeados).
 */
public final class ColoniaApi {
	private ColoniaApi() {
	}

	/**
	 * Bloco protegido contra modificação por jogador comum? Toda a dimensão Nosso Lar (a cidade, a muralha e a
	 * paisagem em volta), mais zonas extras registradas em {@link ZonasProtegidas} (usadas pelos testes).
	 * Consulta pura, sem efeitos colaterais.
	 */
	public static boolean protegido(Level level, BlockPos pos) {
		if (level == null)
			return false;
		return level.dimension() == Planos.NOSSO_LAR || ZonasProtegidas.contem(level, pos);
	}

	/** Está dentro da muralha da colônia? (antes de a colônia existir: toda a dimensão). */
	public static boolean dentroDaMuralha(Level level, BlockPos pos) {
		if (level == null || level.dimension() != Planos.NOSSO_LAR)
			return false;
		if (!(level instanceof ServerLevel sl))
			return true;
		Map<String, Long> p = sl.getData(RegistroDimensao.PONTOS);
		Long c = p.get("centro");
		if (c == null)
			return true;
		BlockPos centro = BlockPos.of(c);
		return LayoutColonia.raio(pos.getX() - centro.getX(), pos.getZ() - centro.getZ()) < LayoutColonia.R_MURO - 1.5;
	}

	/**
	 * Posição de um local nomeado da colônia, ou null. Nomes estáveis:
	 * "governadoria", "regeneracao", "auxilio", "comunicacao", "esclarecimento", "elevacao", "uniao_divina",
	 * "camaras_retificacao", "hospital", "biblioteca", "escola", "praca", "bosque", "residencias", "estacao_aerobus",
	 * "portao_norte", "portao_sul", "portao_leste", "portao_oeste", "chegada".
	 * <p>
	 * Extras: "centro" (chão no centro da praça), "mirante", "mirante_npc", "mirante_entrada", "auxilio_entrada",
	 * "reservatorio", "estacao_&lt;setor&gt;" e "estacao_bosque". Atenção: "auxilio" e "mirante" são a posição dos
	 * ATRIS (blocos), não de chão livre — para pôr NPCs use "auxilio_entrada"/"mirante_npc". Os demais pontos são
	 * posições de pé (bloco de ar sobre o chão). A cidade é construída aos poucos: o ponto existe antes de o prédio
	 * ficar pronto (use {@link #pronta(ServerLevel)} se precisar).
	 */
	public static BlockPos local(ServerLevel level, String nome) {
		if (level.dimension() != Planos.NOSSO_LAR)
			return null;
		return Planos.ponto(level, nome);
	}

	/** A construção incremental da cidade já terminou? */
	public static boolean pronta(ServerLevel level) {
		if (level.dimension() != Planos.NOSSO_LAR)
			return false;
		RegistroColonia.EstadoObra e = level.getData(RegistroColonia.OBRA);
		return e.existe() && e.concluida();
	}
}
