package net.mcreator.modespiritismominecraft.espiritismo.npc.client;

import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;

import net.mcreator.modespiritismominecraft.espiritismo.npc.TrabalhadorEntity;

/** Trabalhador encarnado: modelo de jogador (64x64 clássico) com a skin do papel. */
public class TrabalhadorRenderer extends HumanoidMobRenderer<TrabalhadorEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private final Identifier textura;

	public TrabalhadorRenderer(EntityRendererProvider.Context context, Identifier textura) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
		this.textura = textura;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return textura;
	}
}
