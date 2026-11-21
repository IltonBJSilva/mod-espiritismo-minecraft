package net.mcreator.modespiritismominecraft.espiritismo.colonia;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.bus.api.IEventBus;

import net.mcreator.modespiritismominecraft.ModEspiritismoMinecraftMod;

import java.util.function.Supplier;

/** Registro da área "colonia": o progresso salvo da construção incremental de Nosso Lar (no próprio Level). */
public final class RegistroColonia {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ModEspiritismoMinecraftMod.MODID);

	/**
	 * Estado da obra. {@code versao} = versão do traçado ({@link LayoutColonia#VERSAO}); {@code origem} = centro com y
	 * do chão (BlockPos.asLong); {@code indice} = próxima peça incremental; {@code concluida}.
	 */
	public record EstadoObra(int versao, long origem, int indice, boolean concluida) {
		public static final EstadoObra NENHUMA = new EstadoObra(0, 0L, 0, false);
		public static final Codec<EstadoObra> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("versao").forGetter(EstadoObra::versao),
				Codec.LONG.fieldOf("origem").forGetter(EstadoObra::origem), Codec.INT.fieldOf("indice").forGetter(EstadoObra::indice),
				Codec.BOOL.fieldOf("concluida").forGetter(EstadoObra::concluida)).apply(i, EstadoObra::new));

		public boolean existe() {
			return versao > 0;
		}
	}

	public static final DeferredRegister<net.minecraft.server.level.TicketType> TICKETS = DeferredRegister.create(net.minecraft.core.registries.Registries.TICKET_TYPE,
			ModEspiritismoMinecraftMod.MODID);
	/**
	 * Ticket de chunk da obra: carrega (gera) o chunk em segundo plano, sem expirar sozinho e sem ser salvo, mantendo a dimensão ativa mesmo sem jogadores; a obra
	 * remove os seus tickets quando termina cada peça.
	 */
	public static final Supplier<net.minecraft.server.level.TicketType> TICKET_OBRA = TICKETS.register("colonia_obra",
			() -> new net.minecraft.server.level.TicketType(net.minecraft.server.level.TicketType.NO_TIMEOUT, net.minecraft.server.level.TicketType.FLAG_LOADING | net.minecraft.server.level.TicketType.FLAG_KEEP_DIMENSION_ACTIVE));

	public static final Supplier<AttachmentType<EstadoObra>> OBRA = ATTACHMENTS.register("colonia_obra",
			() -> AttachmentType.builder(() -> EstadoObra.NENHUMA).serialize(EstadoObra.CODEC.fieldOf("valor")).build());

	private RegistroColonia() {
	}

	public static void registrar(IEventBus modBus) {
		ATTACHMENTS.register(modBus);
		TICKETS.register(modBus);
	}
}
