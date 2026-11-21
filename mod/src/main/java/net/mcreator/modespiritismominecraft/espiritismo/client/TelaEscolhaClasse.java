package net.mcreator.modespiritismominecraft.espiritismo.client;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;

import net.mcreator.modespiritismominecraft.espiritismo.Pacotes;
import net.mcreator.modespiritismominecraft.espiritismo.ClasseEspiritual;

/** Tela aberta por Allan Kardec para o jogador escolher sua classe espiritual. */
public class TelaEscolhaClasse extends Screen {
	private static final ClasseEspiritual[] OPCOES = {ClasseEspiritual.MEDIUM, ClasseEspiritual.DOUTRINADOR, ClasseEspiritual.TRABALHADOR};

	public TelaEscolhaClasse() {
		super(Component.literal("Escolha seu caminho"));
	}

	public static void abrir() {
		Minecraft.getInstance().setScreen(new TelaEscolhaClasse());
	}

	@Override
	protected void init() {
		int largura = 200;
		int x = (this.width - largura) / 2;
		int y = this.height / 2 - 40;
		for (ClasseEspiritual c : OPCOES) {
			Button b = Button.builder(Component.literal(c.nome + " — " + c.habilidade), btn -> escolher(c)).bounds(x, y, largura, 20)
					.tooltip(Tooltip.create(Component.literal(c.descricao + "\nHabilidade (V): " + c.habilidade + " — custa " + c.custo + " de energia."))).build();
			this.addRenderableWidget(b);
			y += 26;
		}
		this.addRenderableWidget(Button.builder(Component.literal("Decidir depois"), btn -> this.onClose()).bounds(x + 50, y + 8, 100, 20).build());
	}

	private void escolher(ClasseEspiritual c) {
		ClientPacketDistributor.sendToServer(new Pacotes.EscolherClasse(c.ordinal()));
		this.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractRenderState(g, mouseX, mouseY, partial);
		g.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 75, 0xFFFFD86B);
		g.centeredText(this.font, "Passe o mouse sobre cada classe para ver os detalhes.", this.width / 2, this.height / 2 - 60, 0xFFBBBBBB);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
