package net.mcreator.modespiritismominecraft.espiritismo.dimensao.client;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;

import net.mcreator.modespiritismominecraft.espiritismo.dimensao.Resgates;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.PacotesUmbral;

import java.util.List;

/**
 * Tela de consentimento do resgate (PROMPT §5.4): a fala do socorrista e quatro escolhas claras. Fechar a tela
 * (Esc) não decide nada — as opções continuam no chat e no comando {@code /espiritismo_socorro}.
 */
public class TelaResgate extends Screen {
	private final String fala;
	private List<FormattedCharSequence> linhas = List.of();

	public TelaResgate(String fala) {
		super(Component.literal("Socorristas se aproximam"));
		this.fala = fala;
	}

	public static void abrir(String fala) {
		Minecraft.getInstance().setScreen(new TelaResgate(fala));
	}

	@Override
	protected void init() {
		linhas = this.font.split(Component.literal(fala), Math.min(300, this.width - 40));
		int largura = 220;
		int x = (this.width - largura) / 2;
		int y = this.height / 2 - 10;
		botao(x, y, largura, "Aceitar ajuda", "Ser acompanhado até um Posto de Socorro.", Resgates.Escolha.ACEITAR);
		botao(x, y + 24, largura, "Conversar primeiro", "Ouvir mais antes de decidir. A oferta continua.", Resgates.Escolha.CONVERSAR);
		botao(x, y + 48, largura, "Pedir um tempo", "Os socorristas se afastam e voltam mais tarde.", Resgates.Escolha.PEDIR_TEMPO);
		botao(x, y + 72, largura, "Recusar por agora", "Sua escolha é respeitada. Você pode pedir ajuda depois (tecla H).", Resgates.Escolha.RECUSAR);
	}

	private void botao(int x, int y, int largura, String texto, String dica, Resgates.Escolha escolha) {
		this.addRenderableWidget(Button.builder(Component.literal(texto), b -> escolher(escolha)).bounds(x, y, largura, 20).tooltip(Tooltip.create(Component.literal(dica))).build());
	}

	private void escolher(Resgates.Escolha e) {
		ClientPacketDistributor.sendToServer(new PacotesUmbral.EscolhaResgate(e.ordinal()));
		this.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractRenderState(g, mouseX, mouseY, partial);
		int y = this.height / 2 - 30 - linhas.size() * 10;
		g.centeredText(this.font, this.title, this.width / 2, y - 16, 0xFF9CFFB0);
		for (FormattedCharSequence l : linhas) {
			g.text(this.font, l, (this.width - this.font.width(l)) / 2, y, 0xFFFFFFFF, true);
			y += 10;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
