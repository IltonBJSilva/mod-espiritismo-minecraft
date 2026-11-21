package net.mcreator.modespiritismominecraft.espiritismo.colonia;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Planos;

/**
 * CONTRATO entre as áreas: consultas sobre a colônia Nosso Lar.
 * Assinaturas estáveis; a área "colonia" implementa (muralha, setores, locais nomeados).
 */
public final class ColoniaApi {
	private ColoniaApi() {
	}

	/** Bloco protegido contra modificação por jogador comum? (implementação inicial: toda a dimensão Nosso Lar). */
	public static boolean protegido(Level level, BlockPos pos) {
		return level.dimension() == Planos.NOSSO_LAR;
	}

	/** Está dentro da muralha da colônia? (inicial: toda a dimensão). */
	public static boolean dentroDaMuralha(Level level, BlockPos pos) {
		return level.dimension() == Planos.NOSSO_LAR;
	}

	/**
	 * Posição de um local nomeado da colônia, ou null. Nomes estáveis:
	 * "governadoria", "regeneracao", "auxilio", "comunicacao", "esclarecimento", "elevacao", "uniao_divina",
	 * "camaras_retificacao", "hospital", "biblioteca", "escola", "praca", "bosque", "residencias", "estacao_aerobus",
	 * "portao_norte", "portao_sul", "portao_leste", "portao_oeste", "chegada".
	 */
	public static BlockPos local(ServerLevel level, String nome) {
		if (level.dimension() != Planos.NOSSO_LAR)
			return null;
		return Planos.ponto(level, nome);
	}
}
