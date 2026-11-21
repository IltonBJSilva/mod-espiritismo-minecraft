package net.mcreator.modespiritismominecraft.espiritismo.itens;

import net.minecraft.world.item.ItemStack;

/**
 * API pública da área "itens" para o integrador (caridade, missões).
 * <p>
 * Itens de doação: o jogador abre mão de algo sem receber nada em troca (LE, q. 886; ESE, cap. XIII).
 */
public final class ItensApi {
	private ItensApi() {
	}

	/** O item é uma doação feita para a caridade (sopa fraterna, agasalho, cesta básica)? */
	public static boolean ehDoacao(ItemStack stack) {
		return valorDoacao(stack) > 0;
	}

	/** Valor de moralidade sugerido por UMA unidade doada (0 = não é doação). */
	public static int valorDoacao(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return 0;
		if (stack.is(RegistroItens.CESTA_BASICA.get()))
			return 5;
		if (stack.is(RegistroItens.SOPA_FRATERNA.get()) || stack.is(RegistroItens.AGASALHO.get()))
			return 3;
		return 0;
	}

	/**
	 * Valor considerando quem recebe: comida (sopa, cesta) dada a quem tem fome vale o dobro; o agasalho vale o dobro
	 * para quem passa frio (o integrador decide o critério, ex.: bioma frio ou noite).
	 */
	public static int valorDoacao(ItemStack stack, boolean necessitado) {
		int base = valorDoacao(stack);
		return necessitado ? base * 2 : base;
	}

	/** É uma das obras legíveis do mod? */
	public static boolean ehObra(ItemStack stack) {
		return stack != null && stack.getItem() instanceof ObraItem;
	}
}
