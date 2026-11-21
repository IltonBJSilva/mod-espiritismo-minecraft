package net.mcreator.modespiritismominecraft.espiritismo.dimensao.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.blaze3d.platform.InputConstants;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.ChatFormatting;

import net.mcreator.modespiritismominecraft.espiritismo.estado.Estados;
import net.mcreator.modespiritismominecraft.espiritismo.estado.EstadoEspiritual;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.RegistroDimensao;
import net.mcreator.modespiritismominecraft.espiritismo.dimensao.PacotesUmbral;
import net.mcreator.modespiritismominecraft.espiritismo.client.TeclasEspirituais;
import net.mcreator.modespiritismominecraft.espiritismo.Objetivo;
import net.mcreator.modespiritismominecraft.espiritismo.Moralidade;
import net.mcreator.modespiritismominecraft.espiritismo.ClasseEspiritual;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

/**
 * Efeitos de perturbação no CLIENTE (PROMPT §5.5), sempre suaves e sem flashes, respeitando
 * {@link ConfigAcessibilidade}: névoa leve (encurta a distância da névoa), sussurros posicionais em volume baixo,
 * leve oscilação do campo de visão (só no modo completo) e uma linha de HUD dizendo que o estado é temporário e
 * como pedir ajuda. Teclas: H = pedir ajuda; O = alternar efeitos (completo → reduzido → desligado).
 * <p>
 * GANCHO para o HUD do núcleo (seta/bússola): {@link #seta(Player, BlockPos)} e {@link #distancia(Player, BlockPos)}
 * devolvem a seta e a distância já "desorientadas" conforme a perturbação (ver INTEGRACAO.md).
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class EfeitosPerturbacao {
	public static final KeyMapping PEDIR_AJUDA = new KeyMapping("key.mod_espiritismo_minecraft.pedir_ajuda", InputConstants.KEY_H, TeclasEspirituais.CATEGORIA);
	public static final KeyMapping EFEITOS = new KeyMapping("key.mod_espiritismo_minecraft.efeitos_perturbacao", InputConstants.KEY_O, TeclasEspirituais.CATEGORIA);

	private static int proximoSussurro = 200;

	private EfeitosPerturbacao() {
	}

	/** Perturbação (0–100) do jogador local, já ponderada pela preferência de acessibilidade para efeitos sensoriais. */
	public static int perturbacao(Player p) {
		return p == null ? 0 : p.getData(RegistroDimensao.PERTURBACAO);
	}

	private static float intensidade(Player p) {
		float base = perturbacao(p) / 100F;
		return switch (ConfigAcessibilidade.modo()) {
			case COMPLETO -> base;
			case REDUZIDO -> base * 0.5F;
			case DESLIGADO -> 0F;
		};
	}

	/** Desvio (graus) da seta do objetivo: oscila devagar conforme a perturbação. Mesmo desligado, mantém um mínimo (é jogabilidade, não efeito visual). */
	public static float desvioSeta(Player p) {
		int v = perturbacao(p);
		if (v < 25)
			return 0F;
		float amp = (v - 20) / 80F * 70F * (ConfigAcessibilidade.modo() == ConfigAcessibilidade.Modo.COMPLETO ? 1F : 0.6F);
		return (float) Math.sin(p.tickCount * 0.04) * amp;
	}

	/** Seta de 8 direções com o desvio da perturbação (substitui {@code Objetivo.seta} no HUD). */
	public static String seta(Player p, BlockPos alvo) {
		double dx = alvo.getX() + 0.5 - p.getX(), dz = alvo.getZ() + 0.5 - p.getZ();
		double yawAlvo = Math.toDegrees(Math.atan2(-dx, dz)) + desvioSeta(p);
		double rel = ((yawAlvo - p.getYRot()) % 360 + 540) % 360 - 180;
		int i = (int) Math.round(rel / 45.0) & 7;
		return "↑↗→↘↓↙←↖".substring(i, i + 1);
	}

	/** Distância do objetivo; muito perturbado, vira uma estimativa vaga. */
	public static String distancia(Player p, BlockPos alvo) {
		int d = Objetivo.distancia(p, alvo);
		return perturbacao(p) >= 70 ? "~" + (Math.round(d / 50F) * 50) + "?" : String.valueOf(d);
	}

	// ---------------------------------------------------------------- Registro

	@SubscribeEvent
	public static void teclas(RegisterKeyMappingsEvent event) {
		event.register(PEDIR_AJUDA);
		event.register(EFEITOS);
	}

	@SubscribeEvent
	public static void camada(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.EFFECTS, Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, "umbral_perturbacao"), EfeitosPerturbacao::desenhar);
	}

	@SubscribeEvent
	public static void comandos(RegisterClientCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("espiritismo_efeitos").then(Commands.argument("modo", StringArgumentType.word())
				.suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"completo", "reduzido", "desligado"}, b)).executes(c -> {
					try {
						definir(ConfigAcessibilidade.Modo.valueOf(StringArgumentType.getString(c, "modo").toUpperCase()));
						return 1;
					} catch (IllegalArgumentException e) {
						c.getSource().sendFailure(Component.literal("Use: completo, reduzido ou desligado."));
						return 0;
					}
				})));
	}

	@SubscribeEvent
	public static void conectou(ClientPlayerNetworkEvent.LoggingIn event) {
		enviarPreferencia();
	}

	private static void enviarPreferencia() {
		try {
			var con = Minecraft.getInstance().getConnection();
			if (con != null && con.hasChannel(PacotesUmbral.PreferenciaEfeitos.TYPE))
				ClientPacketDistributor.sendToServer(new PacotesUmbral.PreferenciaEfeitos(ConfigAcessibilidade.modo().ordinal()));
		} catch (RuntimeException ignorado) {
		}
	}

	private static void definir(ConfigAcessibilidade.Modo m) {
		ConfigAcessibilidade.definir(m);
		enviarPreferencia();
		LocalPlayer p = Minecraft.getInstance().player;
		if (p != null)
			p.sendOverlayMessage(Component.literal("Efeitos de perturbação: " + ConfigAcessibilidade.nome(m)).withStyle(ChatFormatting.AQUA));
	}

	// ---------------------------------------------------------------- Tick, névoa, FOV

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		while (PEDIR_AJUDA.consumeClick())
			if (p != null) {
				var con = mc.getConnection();
				if (con != null && con.hasChannel(PacotesUmbral.PedirAjuda.TYPE))
					ClientPacketDistributor.sendToServer(new PacotesUmbral.PedirAjuda());
			}
		while (EFEITOS.consumeClick()) {
			ConfigAcessibilidade.Modo[] v = ConfigAcessibilidade.Modo.values();
			definir(v[(ConfigAcessibilidade.modo().ordinal() + 1) % v.length]);
		}
		if (p == null || mc.level == null || mc.isPaused())
			return;
		float in = intensidade(p);
		if (in <= 0.3F) {
			proximoSussurro = Math.max(proximoSussurro, 100);
			return;
		}
		boolean medium = Moralidade.classe(p) == ClasseEspiritual.MEDIUM;
		if (--proximoSussurro <= 0) {
			int base = ConfigAcessibilidade.modo() == ConfigAcessibilidade.Modo.REDUZIDO ? 600 : 300;
			if (medium)
				base = base * 3 / 5;
			proximoSussurro = base + p.getRandom().nextInt(base);
			double ang = p.getRandom().nextDouble() * Math.PI * 2, r = 4 + p.getRandom().nextDouble() * 4;
			float vol = ConfigAcessibilidade.modo() == ConfigAcessibilidade.Modo.REDUZIDO ? 0.12F : 0.25F;
			mc.level.playLocalSound(p.getX() + Math.cos(ang) * r, p.getY() + 1, p.getZ() + Math.sin(ang) * r, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS.value(), SoundSource.AMBIENT, vol,
					0.8F + p.getRandom().nextFloat() * 0.2F, false);
		}
	}

	@SubscribeEvent
	public static void nevoa(ViewportEvent.RenderFog event) {
		float in = intensidade(Minecraft.getInstance().player);
		if (in <= 0F)
			return;
		event.scaleFarPlaneDistance(1F - 0.35F * in); // névoa leve: no máximo 35% mais próxima
	}

	@SubscribeEvent
	public static void campoDeVisao(ViewportEvent.ComputeFov event) {
		LocalPlayer p = Minecraft.getInstance().player;
		if (p == null || ConfigAcessibilidade.modo() != ConfigAcessibilidade.Modo.COMPLETO)
			return;
		float in = intensidade(p);
		if (in < 0.4F)
			return;
		event.setFOV(event.getFOV() + (float) Math.sin((p.tickCount + event.getPartialTick()) * 0.03) * 1.2F * in); // "respiração" lenta, ±1,2°
	}

	// ---------------------------------------------------------------- HUD informativo

	private static void desenhar(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || mc.options.hideGui || p.isSpectator())
			return;
		EstadoEspiritual e = Estados.get(p);
		int v = perturbacao(p);
		String texto = null;
		int cor = 0xFFCFA8FF;
		if (e == EstadoEspiritual.NO_UMBRAL_PERTURBADO || v >= 50)
			texto = "Perturbação (temporária) · H: pedir ajuda · B: orar";
		else if (e == EstadoEspiritual.EM_RESGATE) {
			texto = "Acompanhado pelos socorristas...";
			cor = 0xFF9CFFB0;
		} else if (e == EstadoEspiritual.EM_ACOLHIMENTO) {
			texto = "Em acolhimento · descanse com calma";
			cor = 0xFF9CFFB0;
		} else if (v >= 25)
			texto = "Mente inquieta · H: pedir ajuda";
		if (texto != null)
			g.centeredText(mc.font, texto, mc.getWindow().getGuiScaledWidth() / 2, 4, cor);
	}
}
