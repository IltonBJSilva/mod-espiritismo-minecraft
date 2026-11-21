"""Skins (64x64, formato clássico) dos moradores e personagens de Nosso Lar e de Amélie Boudet (área "populacao").

Uso (na pasta da cópia):  python arte_fonte/populacao_texturas.py [--preview pasta]
Saída: textures/populacao/<perfil>.png e textures/item/populacao/<id>_spawn_egg.png
Reaproveita o gerador da área "npc" (npc_texturas.py). Roupas simples e claras para Nosso Lar (túnicas, camisas,
vestidos), cada perfil com pele, cabelo, expressão e cor próprias — nada de clones. Amélie: vestido escuro do séc. XIX,
gola branca, cabelo grisalho preso.
"""
import os
import sys
import importlib.util
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("npc_texturas", os.path.join(AQUI, "npc_texturas.py"))
nt = importlib.util.module_from_spec(spec)
spec.loader.exec_module(nt)
hexc, PELES = nt.hexc, nt.PELES

RAIZ = os.path.join(AQUI, "..", "src", "main", "resources", "assets", "mod_espiritismo_minecraft", "textures")
SKINS = os.path.join(RAIZ, "populacao")
OVOS = os.path.join(RAIZ, "item", "populacao")


def tunica(pele, cabelo, estilo, expr, cor, faixa, barba=None, olho="3B2A20"):
    return dict(pele=pele, cabelo=hexc(cabelo), olho=hexc(olho), estilo=estilo, expr=expr, barba=hexc(barba) if barba else None,
                corpo="tunica", roupa=hexc(cor), detalhe=hexc(faixa), calca=hexc(cor), saia=9, cor_saia=hexc(cor), sapato=hexc("CFC8B8"), manga=10)


def camisa(pele, cabelo, estilo, expr, cor, calca, punho=None, olho="3B2A20", manga=10, sapato="5A4636"):
    d = dict(pele=pele, cabelo=hexc(cabelo), olho=hexc(olho), estilo=estilo, expr=expr, corpo="camisa", roupa=hexc(cor), gola=hexc("FFFFFF"),
             calca=hexc(calca), sapato=hexc(sapato), manga=manga)
    if punho:
        d["punho"] = hexc(punho)
    return d


def vestido(pele, cabelo, estilo, expr, cor, saia, detalhe=None, olho="4A3020", manga=8, sapato="6B4A3A"):
    return dict(pele=pele, cabelo=hexc(cabelo), olho=hexc(olho), estilo=estilo, expr=expr, corpo="vestido", roupa=hexc(cor),
                detalhe=hexc(detalhe or cor), calca=hexc(cor), saia=9, cor_saia=hexc(saia), sapato=hexc(sapato), manga=manga)


def blusa(pele, cabelo, estilo, expr, cor, calca, olho="3B2A20", manga=6, sapato="4A3A2E"):
    return dict(pele=pele, cabelo=hexc(cabelo), olho=hexc(olho), estilo=estilo, expr=expr, corpo="blusa", roupa=hexc(cor), calca=hexc(calca),
                sapato=hexc(sapato), manga=manga)


P = PELES
PERFIS = {
    # ---- Personagens do livro Nosso Lar (aparência livre, sóbria; o livro não descreve roupas em detalhe)
    "clarencio": tunica(P["idosa"], "EDEBE4", "calvo", "sorriso", "F3F1EA", "C9B98A", barba="E8E6DE", olho="5A6A7A"),
    "lisias": camisa(P["morena"], "3A2A1E", "curto", "sorriso", "F4F6F2", "E6EAE4", punho="9FD0B0"),
    "laura": vestido(P["clara"], "B8B2A6", "coque", "sorriso", "C9D6E6", "AEBFD6", detalhe="E8EEF6"),
    "narcisa": vestido(P["parda"], "D8D4CC", "coque", "sorriso", "F2F2EE", "E4E6E0", detalhe="9FD0B0"),
    "tobias": camisa(P["clara"], "5A4632", "curto", "neutro", "D9CFB8", "8C7A5E"),
    "veneranda": tunica(P["clara"], "F2EEE6", "longo", "sorriso", "F7F4FB", "C8B8E6", olho="4A6A8A"),
    "genesio": tunica(P["idosa"], "DAD6CC", "calvo", "neutro", "E8EEF2", "8FB0C8", barba="D6D2C8"),
    "henrique_de_luna": camisa(P["clara"], "2A2420", "curto", "neutro", "FFFFFF", "F0F0EC", punho="BFD8EA"),
    "governador": tunica(P["idosa"], "FAFAF6", "curto", "sorriso", "FFFFFF", "F2E6B0", barba="F6F6F0", olho="6A8AAA"),
    # ---- Personagem histórica
    "amelie": vestido(P["idosa"], "B9B5AE", "coque", "neutro", "2E2A36", "26222C", detalhe="6E6878", manga=10, sapato="1E1A18"),
    # ---- Genéricos (criados para o jogo)
    "anselmo": camisa(P["morena"], "6A645C", "baguncado", "triste", "D8D2C4", "9A9282", manga=6),
    "doroteia": vestido(P["clara"], "CFCBC4", "coque", "neutro", "DCD6E2", "C4BECE"),
    "severino": camisa(P["parda"], "2A2420", "baguncado", "triste", "E4E8EC", "CDD3D9", manga=4),
    "lucia": vestido(P["negra"], "1E1814", "longo", "triste", "E9EEF2", "D8E0E6"),
    "rosalia": vestido(P["morena"], "2E2018", "coque", "sorriso", "FFFFFF", "EEF2F0", detalhe="9FD0B0"),
    "teodoro": camisa(P["negra"], "141010", "curto", "sorriso", "FFFFFF", "E8ECE6", punho="9FD0B0"),
    "benedita": vestido(P["negra"], "D9D6CF", "coque", "sorriso", "F2E2B6", "D8C48E"),
    "matias": camisa(P["clara"], "8A5A2E", "curto", "sorriso", "CFE3F2", "6E8296"),
    "ester": vestido(P["parda"], "2A1C14", "longo", "sorriso", "B9D9C4", "7FA88E"),
    "ambrosio": tunica(P["clara"], "9A9690", "calvo", "neutro", "E6DCC6", "8A6E4A", barba="A8A49C"),
    "clara": blusa(P["clara"], "C9833E", "longo", "sorriso", "BFE0F2", "5E7A96"),
    "davi": blusa(P["negra"], "141010", "curto", "riso", "F2D9A6", "6A5A46"),
    "helena": vestido(P["clara"], "4A2E1C", "coque", "neutro", "8E9FC0", "5E6E90"),
    "otavio": camisa(P["morena"], "1E1A18", "curto", "neutro", "E6E0D0", "4A4D57", olho="2A1C14"),
    "bento": blusa(P["parda"], "3A2A1E", "baguncado", "sorriso", "8FB06A", "6A5236", manga=4, sapato="3A2A1E"),
    "ines": vestido(P["morena"], "5A3A22", "longo", "sorriso", "E6C9D9", "7FA06A"),
    "lauro": blusa(P["clara"], "6A4A2E", "curto", "neutro", "A8B2BC", "5A5046", manga=4),
    "marta": vestido(P["negra"], "1A1410", "coque", "neutro", "D6B8A0", "A88A70"),
    "sebastiao": camisa(P["idosa"], "E6E4DE", "calvo", "sorriso", "D2DCC6", "8A8A7A"),
    "noemia": vestido(P["clara"], "E0C070", "longo", "sorriso", "F2D6E6", "C8A2C0"),
    "adelia": vestido(P["parda"], "6A6660", "coque", "sorriso", "F6E7C8", "D8C49E"),
    "jonas": camisa(P["morena"], "2A2420", "curto", "neutro", "DDE6F2", "8A9AB0"),
    "rui": camisa(P["clara"], "3A2A1E", "curto", "neutro", "E8E4DA", "3E4250"),
    "celina": vestido(P["negra"], "2A1E18", "coque", "sorriso", "C6D2E6", "3E4A66"),
    "morador_padrao": camisa(P["morena"], "3A2A1E", "curto", "sorriso", "E8E4DA", "8A8A7A"),
}

OVOS_CORES = {"morador_nosso_lar": ("F3F1EA", "9FD0B0"), "amelie_boudet": ("2E2A36", "F2EFE8")}


def gerar(preview=None):
    os.makedirs(SKINS, exist_ok=True)
    os.makedirs(OVOS, exist_ok=True)
    nt.OVOS = OVOS
    for nome, p in PERFIS.items():
        s = nt.Skin()
        nt.cabeca(s, p)
        nt.corpo(s, p)
        s.img.save(os.path.join(SKINS, nome + ".png"))
    for i, (nome, (b, pt)) in enumerate(OVOS_CORES.items()):
        nt.ovo(nome, hexc(b), hexc(pt), nt.MANCHAS[i % 3], False)
    if preview:
        previa(preview)


def previa(pasta):
    os.makedirs(pasta, exist_ok=True)
    nomes = list(PERFIS)
    esc = 5
    por_linha = 12
    linhas = (len(nomes) + por_linha - 1) // por_linha
    folha = Image.new("RGBA", (por_linha * 18 * esc, linhas * 36 * esc), (70, 90, 110, 255))
    for i, nome in enumerate(nomes):
        sk = Image.open(os.path.join(SKINS, nome + ".png"))
        frente = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
        frente.paste(sk.crop((8, 8, 16, 16)), (4, 0))
        frente.paste(sk.crop((20, 20, 28, 32)), (4, 8))
        frente.paste(sk.crop((44, 20, 48, 32)), (0, 8))
        frente.paste(sk.crop((36, 52, 40, 64)), (12, 8))
        frente.paste(sk.crop((4, 20, 8, 32)), (4, 20))
        frente.paste(sk.crop((20, 52, 24, 64)), (8, 20))
        f = frente.resize((16 * esc, 32 * esc), Image.NEAREST)
        x, y = (i % por_linha) * 18 * esc + esc, (i // por_linha) * 36 * esc + 2 * esc
        folha.alpha_composite(f, (x, y))
    folha.save(os.path.join(pasta, "previa_populacao.png"))


if __name__ == "__main__":
    pv = sys.argv[sys.argv.index("--preview") + 1] if "--preview" in sys.argv else None
    gerar(pv)
    print("Skins em", os.path.normpath(SKINS))
