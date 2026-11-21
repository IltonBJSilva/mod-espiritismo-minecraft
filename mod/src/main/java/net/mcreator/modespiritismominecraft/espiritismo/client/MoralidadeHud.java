package net.mcreator.modespiritismominecraft.espiritismo.client;

import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.resources.Identifier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;

import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.espiritismo.Jornada;
import net.mcreator.modespiritismominecraft.espiritismo.ClasseEspiritual;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

/** Desenha a Barra de Moralidade no canto superior esquerdo da tela. */
@EventBusSubscriber(value = Dist.CLIENT)
public final class MoralidadeHud {
	private static final int LARGURA = 82;

	private MoralidadeHud() {
	}

	@SubscribeEvent
	public static void registrar(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.EFFECTS, Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, "moralidade"), MoralidadeHud::desenhar);
	}

	private static void desenhar(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.options.hideGui || player.isSpectator())
			return;

		int valor = Moralidade.get(player);
		int x = 6, y = 6;
		int preenchido = LARGURA * valor / Moralidade.MAX;

		g.text(mc.font, "Moralidade: " + Moralidade.descricao(valor), x, y, 0xFFFFFFFF, true);
		y += 11;
		g.fill(x - 1, y - 1, x + LARGURA + 1, y + 6, 0xA0000000);
		g.fill(x, y, x + preenchido, y + 5, cor(valor));

		y += 10;
		ClasseEspiritual classe = Moralidade.classe(player);
		if (classe != ClasseEspiritual.NENHUMA) {
			int energia = Moralidade.energia(player);
			g.text(mc.font, classe.nome + " · Energia " + energia, x, y, 0xFFFFE9A8, true);
			y += 11;
			g.fill(x - 1, y - 1, x + LARGURA + 1, y + 4, 0xA0000000);
			g.fill(x, y, x + LARGURA * energia / Moralidade.ENERGIA_MAX, y + 3, energia >= classe.custo ? 0xFFFFD24A : 0xFF8A7A4A);
			y += 8;
		}

		// Objetivo atual da Jornada Espiritual (tutorial)
		int etapa = Jornada.etapa(player);
		if (etapa < Jornada.CONCLUIDA)
			g.text(mc.font, "✦ " + Jornada.objetivo(etapa), x, y, 0xFFFFF27A, true);
	}

	/** Roxo (perturbada) → cinza (neutra) → azul-claro (elevada). */
	private static int cor(int valor) {
		if (valor >= Moralidade.ALTA)
			return 0xFF7FE5FF;
		if (valor >= 60)
			return 0xFF5FB4E8;
		if (valor > 40)
			return 0xFFC8C8C8;
		if (valor > Moralidade.BAIXA)
			return 0xFFB07AD6;
		return 0xFF6A2C91;
	}
}
