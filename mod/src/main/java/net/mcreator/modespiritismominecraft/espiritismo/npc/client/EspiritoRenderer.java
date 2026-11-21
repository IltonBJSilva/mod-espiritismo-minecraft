package net.mcreator.modespiritismominecraft.espiritismo.npc.client;

import net.minecraft.util.Mth;
import net.minecraft.util.ARGB;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;

import net.mcreator.modespiritismominecraft.espiritismo.npc.EspiritoEntity;

/**
 * Espírito desencarnado: modelo de jogador translúcido, tingido e com a transparência "respirando".
 * Espíritos bons (mentor, socorrista) usam a variante emissiva (brilham no escuro).
 */
public class EspiritoRenderer extends HumanoidMobRenderer<EspiritoEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private final Identifier textura;
	private final int rgb;
	private final float alfa;
	private final boolean luminoso;

	public EspiritoRenderer(EntityRendererProvider.Context context, Identifier textura, int rgb, float alfa, boolean luminoso) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0F);
		this.textura = textura;
		this.rgb = rgb;
		this.alfa = alfa;
		this.luminoso = luminoso;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return textura;
	}

	@Override
	protected RenderType getRenderType(HumanoidRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
		return luminoso ? RenderTypes.entityTranslucentEmissive(textura) : RenderTypes.entityTranslucent(textura);
	}

	@Override
	protected int getModelTint(HumanoidRenderState state) {
		float a = Mth.clamp(alfa + 0.08F * Mth.sin(state.ageInTicks * 0.1F), 0.1F, 1F);
		return ARGB.color(a, rgb);
	}

	@Override
	protected boolean shouldRenderLayers(HumanoidRenderState state) {
		return false;
	}
}
