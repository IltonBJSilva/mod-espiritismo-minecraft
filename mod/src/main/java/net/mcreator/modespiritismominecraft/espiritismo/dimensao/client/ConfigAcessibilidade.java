package net.mcreator.modespiritismominecraft.espiritismo.dimensao.client;

import net.neoforged.fml.loading.FMLPaths;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.Properties;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.Reader;
import java.io.Writer;

/**
 * Configuração de acessibilidade do cliente (arquivo simples {@code config/espiritismo_acessibilidade.properties}):
 * <pre>efeitos_perturbacao=completo|reduzido|desligado</pre>
 * Completo: névoa leve, sussurros baixos, leve "respiração" do campo de visão. Reduzido: névoa mais leve, sussurros
 * raros e mais baixos, sem efeito de câmera. Desligado: nenhum efeito visual/sonoro (só os textos informativos).
 * Nunca há flashes. Alterável pela tecla O, pelo comando de cliente {@code /espiritismo_efeitos} ou editando o arquivo.
 */
public final class ConfigAcessibilidade {
	public enum Modo {
		COMPLETO, REDUZIDO, DESLIGADO
	}

	private static Modo modo = null;

	private ConfigAcessibilidade() {
	}

	private static Path arquivo() {
		return FMLPaths.CONFIGDIR.get().resolve("espiritismo_acessibilidade.properties");
	}

	public static Modo modo() {
		if (modo == null) {
			modo = Modo.COMPLETO;
			try {
				Path f = arquivo();
				if (Files.exists(f))
					try (Reader r = Files.newBufferedReader(f)) {
						Properties p = new Properties();
						p.load(r);
						modo = Modo.valueOf(p.getProperty("efeitos_perturbacao", "completo").trim().toUpperCase());
					}
			} catch (Exception e) {
				ModEspiritismoMinecraftMod.LOGGER.warn("Acessibilidade: não foi possível ler a configuração ({}); usando 'completo'", e.toString());
			}
		}
		return modo;
	}

	public static void definir(Modo novo) {
		modo = novo;
		try {
			Path f = arquivo();
			Files.createDirectories(f.getParent());
			Properties p = new Properties();
			p.setProperty("efeitos_perturbacao", novo.name().toLowerCase());
			try (Writer w = Files.newBufferedWriter(f)) {
				p.store(w, "Mod Espiritismo - efeitos de perturbacao: completo | reduzido | desligado");
			}
		} catch (Exception e) {
			ModEspiritismoMinecraftMod.LOGGER.warn("Acessibilidade: não foi possível salvar a configuração: {}", e.toString());
		}
	}

	public static String nome(Modo m) {
		return switch (m) {
			case COMPLETO -> "completos (suaves)";
			case REDUZIDO -> "reduzidos";
			case DESLIGADO -> "desligados";
		};
	}
}
