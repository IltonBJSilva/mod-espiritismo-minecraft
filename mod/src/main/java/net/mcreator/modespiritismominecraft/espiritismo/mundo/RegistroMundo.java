package net.mcreator.modespiritismominecraft.espiritismo.mundo;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

/**
 * Registro da área "mundo": tipo de estrutura procedural (Centros Espíritas e locais especiais) e o tipo de peça.
 * As estruturas em si (worldgen/structure), os conjuntos (worldgen/structure_set) e as tags ficam em JSON
 * em data/mod_espiritismo_minecraft/.
 */
public final class RegistroMundo {
	public static final DeferredRegister<StructureType<?>> TIPOS_ESTRUTURA = DeferredRegister.create(Registries.STRUCTURE_TYPE, ModEspiritismoMinecraftMod.MODID);
	public static final DeferredRegister<StructurePieceType> TIPOS_PECA = DeferredRegister.create(Registries.STRUCTURE_PIECE, ModEspiritismoMinecraftMod.MODID);

	/** Tipo único: o JSON escolhe o "modelo" (centro_pequeno, casa_kardec...). */
	public static final DeferredHolder<StructureType<?>, StructureType<EstruturaEspirita>> CONSTRUCAO_ESPIRITA = TIPOS_ESTRUTURA.register("construcao_espirita",
			() -> () -> EstruturaEspirita.CODEC);

	public static final DeferredHolder<StructurePieceType, StructurePieceType> PECA_ESPIRITA = TIPOS_PECA.register("peca_espirita",
			() -> (StructurePieceType.ContextlessType) PecaEspirita::new);

	public static final TagKey<Structure> CENTROS_ESPIRITAS = TagKey.create(Registries.STRUCTURE, id("centros_espiritas"));
	public static final TagKey<Structure> LOCAIS_ESPECIAIS = TagKey.create(Registries.STRUCTURE, id("locais_especiais"));
	public static final TagKey<Structure> VILAS_GRANDES = TagKey.create(Registries.STRUCTURE, id("vilas_grandes"));

	private RegistroMundo() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, path);
	}

	public static void registrar(IEventBus modBus) {
		TIPOS_ESTRUTURA.register(modBus);
		TIPOS_PECA.register(modBus);
	}
}
