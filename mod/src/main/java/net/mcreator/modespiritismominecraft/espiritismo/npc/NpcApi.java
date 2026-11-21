package net.mcreator.modespiritismominecraft.espiritismo.npc;

import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;

import net.mcreator.modespiritismominecraft.entity.HaroldoDutraEntity;
import net.mcreator.modespiritismominecraft.entity.ChicoXavierEntity;
import net.mcreator.modespiritismominecraft.entity.AllanKardecEntity;

import static net.mcreator.modespiritismominecraft.espiritismo.npc.TrabalhadorEntity.Papel;
import static net.mcreator.modespiritismominecraft.espiritismo.npc.EspiritoEntity.Tipo;

/**
 * API pública da área "npc" para as outras áreas e o integrador.
 * <ul>
 * <li>Entidades: os {@code DeferredHolder} abaixo.</li>
 * <li>{@link Espiritos#esclarecer(Entity, net.minecraft.world.entity.player.Player)} — esclarece obsessor/zombeteiro/sofredor.</li>
 * <li>{@link Espiritos#criar} — faz um Espírito surgir (ex.: Umbral).</li>
 * <li>{@link Prece#orar} e o gancho {@link Prece#AO_ORAR}.</li>
 * </ul>
 */
public final class NpcApi {
	private NpcApi() {
	}

	// Trabalhadores encarnados
	public static final DeferredHolder<EntityType<?>, EntityType<TrabalhadorEntity>> DIRIGENTE = RegistroNpc.TRABALHADORES.get(Papel.DIRIGENTE);
	public static final DeferredHolder<EntityType<?>, EntityType<TrabalhadorEntity>> MEDIUM_TRABALHADOR = RegistroNpc.TRABALHADORES.get(Papel.MEDIUM);
	public static final DeferredHolder<EntityType<?>, EntityType<TrabalhadorEntity>> ORADOR = RegistroNpc.TRABALHADORES.get(Papel.ORADOR);
	public static final DeferredHolder<EntityType<?>, EntityType<TrabalhadorEntity>> PASSISTA = RegistroNpc.TRABALHADORES.get(Papel.PASSISTA);
	public static final DeferredHolder<EntityType<?>, EntityType<TrabalhadorEntity>> EVANGELIZADORA = RegistroNpc.TRABALHADORES.get(Papel.EVANGELIZADORA);
	public static final DeferredHolder<EntityType<?>, EntityType<TrabalhadorEntity>> DIVALDO_FRANCO = RegistroNpc.TRABALHADORES.get(Papel.DIVALDO);

	// Espíritos
	public static final DeferredHolder<EntityType<?>, EntityType<EspiritoEntity>> MENTOR = RegistroNpc.ESPIRITOS.get(Tipo.MENTOR);
	public static final DeferredHolder<EntityType<?>, EntityType<EspiritoEntity>> OBSESSOR = RegistroNpc.ESPIRITOS.get(Tipo.OBSESSOR);
	public static final DeferredHolder<EntityType<?>, EntityType<EspiritoEntity>> ESPIRITO_SOFREDOR = RegistroNpc.ESPIRITOS.get(Tipo.SOFREDOR);
	public static final DeferredHolder<EntityType<?>, EntityType<EspiritoEntity>> ZOMBETEIRO = RegistroNpc.ESPIRITOS.get(Tipo.ZOMBETEIRO);
	public static final DeferredHolder<EntityType<?>, EntityType<EspiritoEntity>> SOCORRISTA = RegistroNpc.ESPIRITOS.get(Tipo.SOCORRISTA);

	/** Benfeitores (Kardec, Chico, Haroldo), trabalhadores do Centro e Espíritos bons (mentor, socorrista). */
	public static boolean ehBenfeitorOuTrabalhador(Entity e) {
		if (e instanceof AllanKardecEntity || e instanceof ChicoXavierEntity || e instanceof HaroldoDutraEntity || e instanceof TrabalhadorEntity)
			return true;
		return e instanceof EspiritoEntity esp && (esp.tipo() == Tipo.MENTOR || esp.tipo() == Tipo.SOCORRISTA);
	}

	/** Se é um Espírito desencarnado desta área (qualquer tipo). */
	public static boolean ehEspirito(Entity e) {
		return e instanceof EspiritoEntity;
	}
}
