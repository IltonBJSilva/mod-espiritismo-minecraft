package net.mcreator.modespiritismominecraft.espiritismo.npc;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;

import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModTabs;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/** Registro de tudo da área "npc": entidades, ovos, payload da prece, atributos e aba criativa. */
public final class RegistroNpc {
	public static final String MODID = ModEspiritismoMinecraftMod.MODID;

	public static final DeferredRegister<EntityType<?>> ENTIDADES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
	public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(MODID);

	public static final Map<TrabalhadorEntity.Papel, DeferredHolder<EntityType<?>, EntityType<TrabalhadorEntity>>> TRABALHADORES = new LinkedHashMap<>();
	public static final Map<EspiritoEntity.Tipo, DeferredHolder<EntityType<?>, EntityType<EspiritoEntity>>> ESPIRITOS = new LinkedHashMap<>();
	public static final List<DeferredItem<Item>> OVOS = new ArrayList<>();

	static {
		for (TrabalhadorEntity.Papel papel : TrabalhadorEntity.Papel.values()) {
			var holder = ENTIDADES.register(papel.id, () -> EntityType.Builder.<TrabalhadorEntity>of((t, l) -> new TrabalhadorEntity(t, l, papel), MobCategory.CREATURE)
					.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).sized(0.6F, 1.8F).build(chave(papel.id)));
			TRABALHADORES.put(papel, holder);
			OVOS.add(ITENS.registerItem(papel.id + "_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(holder.get()))));
		}
		for (EspiritoEntity.Tipo tipo : EspiritoEntity.Tipo.values()) {
			var holder = ENTIDADES.register(tipo.id, () -> EntityType.Builder.<EspiritoEntity>of((t, l) -> new EspiritoEntity(t, l, tipo), MobCategory.MISC)
					.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(2).sized(0.6F, 1.8F).fireImmune().build(chave(tipo.id)));
			ESPIRITOS.put(tipo, holder);
			OVOS.add(ITENS.registerItem(tipo.id + "_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(holder.get()))));
		}
	}

	private RegistroNpc() {
	}

	private static ResourceKey<EntityType<?>> chave(String id) {
		return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, id));
	}

	public static void registrar(IEventBus modBus) {
		ENTIDADES.register(modBus);
		ITENS.register(modBus);
		modBus.addListener(RegistroNpc::atributos);
		modBus.addListener(RegistroNpc::abaCriativa);
		modBus.addListener(RegistroNpc::pacotes);
	}

	private static void atributos(EntityAttributeCreationEvent event) {
		TRABALHADORES.values().forEach(h -> event.put(h.get(), TrabalhadorEntity.createAttributes().build()));
		ESPIRITOS.values().forEach(h -> event.put(h.get(), EspiritoEntity.createAttributes().build()));
	}

	private static void abaCriativa(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == ModEspiritismoMinecraftModTabs.MOD_ESPIRITISMO.getKey())
			OVOS.forEach(o -> event.accept(o.get()));
	}

	private static void pacotes(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar r = event.registrar(MODID).versioned("1");
		r.playToServer(Prece.Orar.TYPE, Prece.Orar.CODEC, (msg, ctx) -> ctx.enqueueWork(() -> Prece.orar(ctx.player())));
	}
}
