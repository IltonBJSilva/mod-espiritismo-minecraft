package net.mcreator.modespiritismominecraft.espiritismo.mundo;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;

/**
 * Os modelos de construção que o tipo {@code construcao_espirita} sabe gerar. O tamanho é o lote inteiro
 * (x = largura, z = profundidade, com a frente/entrada em z = 0), em coordenadas locais da peça.
 */
public enum Modelo implements StringRepresentable {
	CENTRO_PEQUENO("centro_pequeno", 15, 18, 14, null),
	CENTRO_MEDIO("centro_medio", 27, 26, 10, null),
	CENTRO_GRANDE("centro_grande", 41, 40, 10, null),
	CASA_KARDEC("casa_kardec", 21, 21, 19, Estilo.PARIS),
	CASA_CHICO("casa_chico", 27, 22, 13, Estilo.MINEIRO),
	MANSAO_DO_CAMINHO("mansao_do_caminho", 57, 51, 11, Estilo.BAIANO),
	AUDITORIO_HAROLDO("auditorio_haroldo", 33, 36, 14, Estilo.AUDITORIO);

	public static final Codec<Modelo> CODEC = StringRepresentable.fromEnum(Modelo::values);

	public final String id;
	public final int largura, profundidade, altura;
	/** Estilo fixo (locais especiais) ou null para escolher pelo bioma. */
	@Nullable
	public final Estilo estiloFixo;

	Modelo(String id, int largura, int profundidade, int altura, @Nullable Estilo estiloFixo) {
		this.id = id;
		this.largura = largura;
		this.profundidade = profundidade;
		this.altura = altura;
		this.estiloFixo = estiloFixo;
	}

	@Override
	public String getSerializedName() {
		return id;
	}

	public static Modelo porId(String id) {
		for (Modelo m : values())
			if (m.id.equals(id))
				return m;
		return CENTRO_PEQUENO;
	}
}
