package net.mcreator.modespiritismominecraft.espiritismo.populacao.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;

import net.mcreator.modespiritismominecraft.espiritismo.populacao.RegistroPopulacao;
import net.mcreator.modespiritismominecraft.espiritismo.populacao.MoradorEntity;

/** Renderizador dos moradores/personagens: modelo de jogador (skin 64x64) escolhida pelo perfil (textures/populacao/<perfil>.png). */
@EventBusSubscriber(value = Dist.CLIENT)
public final class RenderizadorPopulacao {
	private RenderizadorPopulacao() {
	}

	@SubscribeEvent
	public static void registrar(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(RegistroPopulacao.MORADOR.get(), MoradorRenderer::new);
		event.registerEntityRenderer(RegistroPopulacao.PERSONAGEM.get(), MoradorRenderer::new);
		event.registerEntityRenderer(RegistroPopulacao.AMELIE.get(), MoradorRenderer::new);
	}

	public static class Estado extends HumanoidRenderState {
		public String perfil = "";
	}

	public static class MoradorRenderer extends HumanoidMobRenderer<MoradorEntity, Estado, HumanoidModel<Estado>> {
		public MoradorRenderer(EntityRendererProvider.Context context) {
			super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
		}

		@Override
		public Estado createRenderState() {
			return new Estado();
		}

		@Override
		public void extractRenderState(MoradorEntity entity, Estado state, float partialTicks) {
			super.extractRenderState(entity, state, partialTicks);
			state.perfil = entity.perfilId();
		}

		@Override
		public Identifier getTextureLocation(Estado state) {
			String id = state.perfil == null || state.perfil.isEmpty() ? "morador_padrao" : state.perfil;
			return Identifier.fromNamespaceAndPath(RegistroPopulacao.MODID, "textures/populacao/" + id + ".png");
		}
	}
}
