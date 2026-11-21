package net.mcreator.modespiritismominecraft.espiritismo;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.IEventBus;

import net.mcreator.modespiritismominecraft.init.ModEspiritismoMinecraftModTabs;
import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

/** Itens do núcleo do mod feitos à mão (fora do MCreator). */
public final class RegistroBase {
	public static final DeferredRegister.Items ITENS = DeferredRegister.createItems(ModEspiritismoMinecraftMod.MODID);

	public static final DeferredItem<BussolaEspiritual> BUSSOLA_ESPIRITUAL = ITENS.registerItem("bussola_espiritual", BussolaEspiritual::new);

	private RegistroBase() {
	}

	public static void registrar(IEventBus modBus) {
		ITENS.register(modBus);
		modBus.addListener(RegistroBase::aba);
		// A prece (tecla B) também fluidifica a água das jarras próximas.
		net.mcreator.modespiritismominecraft.espiritismo.npc.Prece.AO_ORAR.add(net.mcreator.modespiritismominecraft.espiritismo.itens.Fluidificacao::registrarPrece);
	}

	private static void aba(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == ModEspiritismoMinecraftModTabs.MOD_ESPIRITISMO.getKey())
			event.accept(BUSSOLA_ESPIRITUAL.get());
	}
}
