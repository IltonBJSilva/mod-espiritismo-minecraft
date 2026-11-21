package net.mcreator.modespiritismominecraft.espiritismo.populacao;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;

import com.mojang.serialization.Codec;

import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModTabs;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

/**
 * Registro da área "populacao" (moradores e personagens de Nosso Lar, Amélie Boudet).
 * Chamado por {@code npc.RegistroNpc.registrar} — não precisa de linha nova no mod init.
 */
public final class RegistroPopulacao {
	public static final String MODID = ModEspiritismoMinecraftMod.MODID;

	public static final DeferredRegister<EntityType<?>> ENTIDADES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
	public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(MODID);
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

	/** Morador genérico de Nosso Lar (perfil sorteado por zona). Os criados pelo povoamento não são salvos no disco. */
	public static final DeferredHolder<EntityType<?>, EntityType<MoradorEntity>> MORADOR = entidade("morador_nosso_lar", "");
	/** Personagem nomeado do livro Nosso Lar (um de cada, persistente). O perfil vem de {@link Perfis}. */
	public static final DeferredHolder<EntityType<?>, EntityType<MoradorEntity>> PERSONAGEM = entidade("personagem_nosso_lar", "");
	/** Amélie Gabrielle Boudet (personagem histórica, casa de Kardec). */
	public static final DeferredHolder<EntityType<?>, EntityType<MoradorEntity>> AMELIE = entidade("amelie_boudet", Perfis.AMELIE);

	public static final DeferredItem<Item> OVO_MORADOR = ITENS.registerItem("morador_nosso_lar_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(MORADOR.get())));
	public static final DeferredItem<Item> OVO_AMELIE = ITENS.registerItem("amelie_boudet_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(AMELIE.get())));

	/** Memória dos NPCs sobre o jogador (encontros, explicações, leituras, etapas das jornadas). */
	public static final Supplier<AttachmentType<Memoria>> MEMORIA = ATTACHMENTS.register("memoria_populacao",
			() -> AttachmentType.builder(Memoria::new).serialize(Memoria.CODEC.fieldOf("valor")).copyOnDeath().build());

	/** Por nível: perfil nomeado → UUID da única entidade "oficial" daquele personagem (evita duplicatas). */
	public static final Supplier<AttachmentType<Map<String, String>>> PERSONAGENS = ATTACHMENTS.register("personagens_nomeados",
			() -> AttachmentType.<Map<String, String>>builder(() -> new HashMap<>())
					.serialize(Codec.unboundedMap(Codec.STRING, Codec.STRING).<Map<String, String>>xmap(HashMap::new, m -> m).fieldOf("valor")).build());

	private RegistroPopulacao() {
	}

	private static DeferredHolder<EntityType<?>, EntityType<MoradorEntity>> entidade(String id, String perfilPadrao) {
		return ENTIDADES.register(id, () -> EntityType.Builder.<MoradorEntity>of((t, l) -> new MoradorEntity(t, l, perfilPadrao), MobCategory.MISC)
				.setShouldReceiveVelocityUpdates(true).setTrackingRange(10).setUpdateInterval(3).sized(0.6F, 1.8F).fireImmune()
				.build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, id))));
	}

	public static void registrar(IEventBus modBus) {
		ENTIDADES.register(modBus);
		ITENS.register(modBus);
		ATTACHMENTS.register(modBus);
		modBus.addListener(RegistroPopulacao::atributos);
		modBus.addListener(RegistroPopulacao::abaCriativa);
	}

	private static void atributos(EntityAttributeCreationEvent event) {
		event.put(MORADOR.get(), MoradorEntity.createAttributes().build());
		event.put(PERSONAGEM.get(), MoradorEntity.createAttributes().build());
		event.put(AMELIE.get(), MoradorEntity.createAttributes().build());
	}

	private static void abaCriativa(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == ModEspiritismoMinecraftModTabs.MOD_ESPIRITISMO.getKey()) {
			event.accept(OVO_MORADOR.get());
			event.accept(OVO_AMELIE.get());
		}
	}
}
