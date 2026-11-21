package net.mcreator.modespiritismominecraft.espiritismo.biblioteca;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;
import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModTabs;

import java.util.function.Supplier;

/**
 * Registro da área "biblioteca": attachment de progresso/conhecimento, estante, catálogo (item), pacotes de rede.
 * Chamado por {@code itens.RegistroItens.registrar} (mesma área, sem mexer no mod init).
 */
public final class RegistroBiblioteca {
	private RegistroBiblioteca() {
	}

	public static final String MODID = ModEspiritismoMinecraftMod.MODID;
	public static final DeferredRegister.Blocks BLOCOS = DeferredRegister.createBlocks(MODID);
	public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(MODID);
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

	/** Progresso de leitura e conhecimento: salvo no jogador, mantido após a morte, sincronizado só com o dono. */
	public static final Supplier<AttachmentType<ProgressoBiblioteca>> PROGRESSO = ATTACHMENTS.register("biblioteca_progresso",
			() -> AttachmentType.builder(() -> ProgressoBiblioteca.VAZIO).serialize(ProgressoBiblioteca.CODEC)
					.sync(RegistroBiblioteca::soDono, ByteBufCodecs.fromCodec(ProgressoBiblioteca.CODEC.codec())).copyOnDeath().build());

	private static boolean soDono(IAttachmentHolder holder, ServerPlayer para) {
		return holder == para;
	}

	public static final DeferredBlock<Block> ESTANTE = BLOCOS.registerBlock("estante_espirita",
			p -> new EstanteBlock(p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD)));
	public static final DeferredItem<?> ESTANTE_ITEM = ITENS.registerSimpleBlockItem(ESTANTE);
	public static final DeferredItem<Item> CATALOGO = ITENS.registerItem("catalogo_biblioteca", CatalogoItem::new, () -> new Item.Properties().stacksTo(1));

	public static void registrar(IEventBus modBus) {
		BLOCOS.register(modBus);
		ITENS.register(modBus);
		ATTACHMENTS.register(modBus);
		modBus.addListener(PacotesBiblioteca::registrar);
		modBus.addListener(RegistroBiblioteca::aba);
		// Sugestão do módulo "Prece" na primeira vez que o jogador ora (tecla B).
		net.mcreator.modespiritismominecraft.espiritismo.npc.Prece.AO_ORAR.add(p -> {
			if (p instanceof ServerPlayer sp)
				BibliotecaApi.sugerirModulo(sp, "prece");
		});
	}

	private static void aba(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() != ModEspiritismoMinecraftModTabs.MOD_ESPIRITISMO.getKey())
			return;
		event.accept(ESTANTE_ITEM.get());
		event.accept(CATALOGO.get());
	}
}
