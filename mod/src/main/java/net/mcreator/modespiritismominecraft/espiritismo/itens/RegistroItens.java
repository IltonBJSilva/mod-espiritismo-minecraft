package net.mcreator.modespiritismominecraft.espiritismo.itens;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;
import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModTabs;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Registro de tudo da área "itens": blocos, itens, attachment de preces e entradas na aba criativa do mod. */
public final class RegistroItens {
	private RegistroItens() {
	}

	public static final String MODID = ModEspiritismoMinecraftMod.MODID;
	public static final DeferredRegister.Blocks BLOCOS = DeferredRegister.createBlocks(MODID);
	public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(MODID);
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

	/** Preces recentes por nível: posição (BlockPos.asLong) → tick do jogo. Não é salvo em disco. */
	public static final Supplier<AttachmentType<Map<Long, Long>>> PRECES_RECENTES = ATTACHMENTS.register("preces_recentes",
			() -> AttachmentType.<Map<Long, Long>>builder(() -> new HashMap<>()).build());

	// ---------------------------------------------------------------- Formas (FACING = north)

	private static VoxelShape b(double x0, double y0, double z0, double x1, double y1, double z1) {
		return Block.box(x0, y0, z0, x1, y1, z1);
	}

	static final VoxelShape FORMA_LIVRO = b(4.5, 0, 3.5, 11.5, 2, 12.5);
	static final VoxelShape FORMA_MESA = Shapes.or(b(0, 10, 0, 16, 14, 16), b(1, 0, 1, 3, 10, 3), b(13, 0, 1, 15, 10, 3), b(1, 0, 13, 3, 10, 15),
			b(13, 0, 13, 15, 10, 15), b(11, 14, 10, 14, 18.5, 13));
	static final VoxelShape FORMA_TRIBUNA = Shapes.or(b(2, 0, 3, 14, 1, 14), b(3, 1, 5, 13, 12, 13), b(1, 12, 3, 15, 16, 16));
	static final VoxelShape FORMA_JARRA = Shapes.or(b(5, 0, 5, 11, 9, 11), b(6, 9, 6, 10, 11, 10), b(11, 2, 7, 13, 8, 9));

	// ---------------------------------------------------------------- Blocos

	public static final DeferredBlock<Block> LIVRO_1857 = BLOCOS.registerBlock("livro_1857",
			p -> new BlocoOrientado(p.mapColor(MapColor.SAND).strength(0.5F).sound(SoundType.WOOL).noOcclusion(), FORMA_LIVRO,
					"O Livro dos Espíritos, 1ª edição: Paris, 18 de abril de 1857, 501 questões. A 2ª edição (1860) chegou a 1.019 questões."));
	public static final DeferredBlock<Block> MESA_MEDIUNICA = BLOCOS.registerBlock("mesa_mediunica",
			p -> new BlocoOrientado(p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion(), FORMA_MESA,
					"Mesa da reunião mediúnica: reunião séria, gratuita, com estudo, prece e um dirigente experiente (O Livro dos Médiuns, cap. XXIX). "
							+ "Todo comunicado passa pelo crivo da razão (LM, cap. XX, item 230)."));
	public static final DeferredBlock<Block> TRIBUNA = BLOCOS.registerBlock("tribuna",
			p -> new BlocoOrientado(p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion(), FORMA_TRIBUNA,
					"Tribuna de palestras: no Centro Espírita o estudo e a divulgação da Doutrina são abertos a todos e sempre gratuitos."));
	public static final DeferredBlock<Block> JARRA_AGUA = BLOCOS.registerBlock("jarra_agua",
			p -> new JarraAguaBlock(p.mapColor(MapColor.WATER).strength(0.3F).sound(SoundType.GLASS).noOcclusion(), FORMA_JARRA));

	public static final DeferredItem<?> LIVRO_1857_ITEM = ITENS.registerSimpleBlockItem(LIVRO_1857);
	public static final DeferredItem<?> MESA_MEDIUNICA_ITEM = ITENS.registerSimpleBlockItem(MESA_MEDIUNICA);
	public static final DeferredItem<?> TRIBUNA_ITEM = ITENS.registerSimpleBlockItem(TRIBUNA);
	public static final DeferredItem<?> JARRA_AGUA_ITEM = ITENS.registerSimpleBlockItem(JARRA_AGUA);

	// ---------------------------------------------------------------- Obras legíveis

	/** id → item, na ordem de Obras.OBRAS. */
	public static final Map<String, DeferredItem<Item>> OBRAS = new LinkedHashMap<>();

	static {
		for (String id : Obras.OBRAS.keySet())
			OBRAS.put(id, ITENS.registerItem(id, ObraItem::new,
					() -> new Item.Properties().stacksTo(16).component(DataComponents.WRITTEN_BOOK_CONTENT, Obras.conteudo(id))));
	}

	public static final DeferredItem<Item> CADERNO_PSICOGRAFIA = ITENS.registerItem("caderno_psicografia", CadernoPsicografiaItem::new,
			() -> new Item.Properties().stacksTo(1).component(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY));

	// ---------------------------------------------------------------- Caridade

	public static final DeferredItem<Item> SOPA_FRATERNA = ITENS.registerItem("sopa_fraterna", Item::new,
			() -> new Item.Properties().stacksTo(1).food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.6F).build()).usingConvertsTo(Items.BOWL));
	public static final DeferredItem<Item> AGASALHO = ITENS.registerItem("agasalho", Item::new, () -> new Item.Properties().stacksTo(16));
	public static final DeferredItem<Item> CESTA_BASICA = ITENS.registerItem("cesta_basica", Item::new,
			() -> new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.8F).build()));

	// ---------------------------------------------------------------- Registro

	public static void registrar(IEventBus modBus) {
		BLOCOS.register(modBus);
		ITENS.register(modBus);
		ATTACHMENTS.register(modBus);
		modBus.addListener(RegistroItens::abaCriativa);
		// Biblioteca Espírita (mesma área "biblioteca/itens"): catálogo, estante, progresso de leitura, trilhas.
		net.mcreator.modespiritismominecraft.espiritismo.biblioteca.RegistroBiblioteca.registrar(modBus);
	}

	private static void abaCriativa(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() != ModEspiritismoMinecraftModTabs.MOD_ESPIRITISMO.getKey())
			return;
		event.accept(LIVRO_1857_ITEM.get());
		event.accept(MESA_MEDIUNICA_ITEM.get());
		event.accept(TRIBUNA_ITEM.get());
		event.accept(JARRA_AGUA_ITEM.get());
		OBRAS.values().forEach(o -> event.accept(o.get()));
		event.accept(CADERNO_PSICOGRAFIA.get());
		event.accept(SOPA_FRATERNA.get());
		event.accept(AGASALHO.get());
		event.accept(CESTA_BASICA.get());
	}
}
