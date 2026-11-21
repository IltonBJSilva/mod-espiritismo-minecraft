
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.modespiritismominecraft.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import net.mcreator.modespiritismominecraft.entity.HaroldoDutraEntity;
import net.mcreator.modespiritismominecraft.entity.ChicoXavierEntity;
import net.mcreator.modespiritismominecraft.entity.AllanKardecEntity;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModEspiritismoMinecraftModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, ModEspiritismoMinecraftMod.MODID);
	public static final DeferredHolder<EntityType<?>, EntityType<AllanKardecEntity>> ALLAN_KARDEC = register("allan_kardec",
			EntityType.Builder.<AllanKardecEntity>of(AllanKardecEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<ChicoXavierEntity>> CHICO_XAVIER = register("chico_xavier",
			EntityType.Builder.<ChicoXavierEntity>of(ChicoXavierEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<HaroldoDutraEntity>> HAROLDO_DUTRA = register("haroldo_dutra",
			EntityType.Builder.<HaroldoDutraEntity>of(HaroldoDutraEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.6f, 1.8f));

	// Start of user code block custom entities
	// End of user code block custom entities
	private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(ModEspiritismoMinecraftMod.MODID, registryname))));
	}

	@SubscribeEvent
	public static void init(RegisterSpawnPlacementsEvent event) {
		AllanKardecEntity.init(event);
		ChicoXavierEntity.init(event);
		HaroldoDutraEntity.init(event);
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(ALLAN_KARDEC.get(), AllanKardecEntity.createAttributes().build());
		event.put(CHICO_XAVIER.get(), ChicoXavierEntity.createAttributes().build());
		event.put(HAROLDO_DUTRA.get(), HaroldoDutraEntity.createAttributes().build());
	}
}
