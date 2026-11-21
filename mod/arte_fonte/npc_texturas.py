"""Skins (64x64, formato clássico de jogador) e ovos dos NPCs da área "npc".

Uso (na pasta da cópia de trabalho):  python arte_fonte/npc_texturas.py [--preview pasta]
Saída: textures/npc/<id>.png e textures/item/npc/<id>_spawn_egg.png
Estilo vanilla: cores chapadas com leve ruído, roupas comuns e dignas, sem caricatura.
"""
import os
import sys
import random
from PIL import Image

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "mod_espiritismo_minecraft", "textures")
SKINS = os.path.join(RAIZ, "npc")
OVOS = os.path.join(RAIZ, "item", "npc")
rnd = random.Random(1857)


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def mul(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def var(c, n=5):
    d = rnd.randint(-n, n)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


class Skin:
    def __init__(self):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def px(self, x, y, c, n=4):
        if c is not None:
            self.img.putpixel((x, y), var(c, n))

    def rect(self, x0, y0, w, h, fn):
        """fn(x, y) -> cor, em coordenadas locais."""
        for y in range(h):
            for x in range(w):
                self.px(x0 + x, y0 + y, fn(x, y))


# ------------------------------------------------------------------ Cabeça

def cabeca(s, p):
    pele, cab, olho = p["pele"], p["cabelo"], p["olho"]
    estilo = p.get("estilo", "curto")
    expr = p.get("expr", "neutro")
    sombra = mul(pele, 0.88)
    labio = mul(pele, 0.72)
    sobr = mul(cab, 0.8) if estilo != "calvo" else mul(cab, 0.7)
    barba = p.get("barba")

    # topo
    def topo(x, y):
        if estilo == "calvo":
            return cab if x in (0, 7) or y in (0, 7) else pele
        return cab
    s.rect(8, 0, 8, 8, topo)
    # base (queixo)
    s.rect(16, 0, 8, 8, lambda x, y: barba if barba else (cab if estilo == "longo" and y > 4 else sombra))

    # frente
    def frente(x, y):
        if estilo in ("curto", "coque", "baguncado"):
            if y == 0:
                return cab
            if y == 1 and (x in (0, 7) or (estilo == "baguncado" and x in (1, 3, 6)) or (estilo == "curto" and x < 3)):
                return cab
        elif estilo == "longo":
            if y <= 1 or x in (0, 7):
                return cab
        elif estilo == "calvo":
            if y >= 1 and y <= 4 and x in (0, 7):
                return cab
        # sobrancelhas
        if y == 3:
            if expr == "bravo":
                return sobr if x in (2, 5) else pele
            if expr == "triste":
                return sobr if x in (1, 6) else pele
            return sobr if x in (1, 2, 5, 6) else pele
        if y == 2 and expr == "bravo" and x in (1, 6):
            return sobr
        if y == 2 and expr == "triste" and x in (2, 5):
            return sobr
        # olhos
        if y == 4:
            if x in (1, 6):
                return hexc("F2F0EA")
            if x in (2, 5):
                return olho
            if x in (3, 4):
                return pele
        if y == 5 and x in (3, 4):
            return sombra  # nariz
        if barba and y >= 5 and (y >= 6 or x in (0, 1, 6, 7)):
            if y == 6 and x in (3, 4):
                return labio
            return barba
        if y == 6:
            if expr == "sorriso" and x in (2, 3, 4, 5):
                return labio if x in (3, 4) else (labio if False else pele)
            if expr == "riso" and x in (2, 3, 4, 5):
                return hexc("3A1A1A") if x in (3, 4) else labio
            if expr == "triste" and x in (3, 4):
                return labio
            if x in (3, 4):
                return labio
        if y == 7:
            if expr == "triste" and x in (2, 5):
                return labio
            if expr == "sorriso" and x in (2, 5):
                return pele
            return sombra if x in (0, 7) else pele
        if y == 5 and expr in ("sorriso", "riso") and x in (2, 5):
            return labio
        return pele
    s.rect(8, 8, 8, 8, frente)

    # lados (direito em x=0: coluna 0 = nuca, 7 = rosto; esquerdo em x=16: coluna 0 = rosto)
    def lado(espelho):
        def f(x, y):
            c = 7 - x if espelho else x  # c: 0 = rosto, 7 = nuca
            if estilo == "longo":
                return cab if (c >= 2 or y <= 1) else (pele if y < 7 else cab)
            if estilo == "calvo":
                if 1 <= y <= 5 and c >= 3:
                    return cab
                if barba and y >= 5 and c <= 3:
                    return barba
                return sombra if c >= 5 else pele
            lim = 5 if estilo == "coque" else 4
            if y <= 1 or (y <= lim and c >= 3) or (y <= 6 and c >= 5):
                return cab
            if c == 4 and y in (4, 5):
                return sombra  # orelha
            if barba and y >= 5:
                return barba
            return pele
        return f
    s.rect(0, 8, 8, 8, lado(True))
    s.rect(16, 8, 8, 8, lado(False))

    # nuca
    def nuca(x, y):
        if estilo == "calvo":
            return cab if 1 <= y <= 5 else (pele if y == 0 else sombra)
        if estilo == "longo":
            return cab
        if estilo == "coque":
            return mul(cab, 0.85) if 2 <= y <= 4 and 2 <= x <= 5 else (cab if y <= 6 else sombra)
        return cab if y <= 6 else sombra
    s.rect(24, 8, 8, 8, nuca)


# ------------------------------------------------------------------ Corpo, braços e pernas

def corpo(s, p):
    pele = p["pele"]
    roupa = p["roupa"]
    det = p.get("detalhe", roupa)
    calca = p["calca"]
    estilo = p.get("corpo", "camisa")
    gola = p.get("gola", hexc("EDEBE6"))
    gravata = p.get("gravata")

    def frente(x, y):
        if y >= 10:
            if estilo in ("vestido", "tunica"):
                return mul(roupa, 0.9) if y == 10 and estilo == "tunica" else roupa
            return p.get("cinto", mul(calca, 0.7)) if y == 10 else calca
        if estilo == "terno":
            if x in (3, 4):
                if y == 0:
                    return gola
                return gravata if gravata and y <= 7 else (gola if y <= 7 else roupa)
            if x in (2, 5) and y <= 4:
                return mul(roupa, 0.82)  # lapela
            return roupa
        if estilo == "colete":
            if x in (3, 4):
                return gola if y <= 1 else (det if y % 3 != 1 else mul(det, 0.6))
            if x in (0, 7):
                return gola
            return det
        if estilo == "blusa":
            if y == 0 and 2 <= x <= 5:
                return pele
            if y == 1 and x in (3, 4):
                return pele
            return roupa
        if estilo == "camiseta":
            if y == 0 and 2 <= x <= 5:
                return mul(roupa, 0.8)
            if p.get("remendo") and (x, y) in ((1, 6), (2, 6), (1, 7), (6, 3), (6, 4)):
                return det
            return roupa
        if estilo in ("vestido", "tunica"):
            if y == 0 and 2 <= x <= 5:
                return pele if estilo == "vestido" else mul(roupa, 0.85)
            if y == 6 and estilo == "vestido":
                return det
            if estilo == "tunica" and y == 7:
                return det  # faixa
            return roupa
        # camisa social
        if y == 0 and x in (2, 3, 4, 5):
            return gola
        if x in (3, 4) and gravata and y <= 8:
            return gravata
        if x == 4 and y in (2, 5, 8):
            return mul(roupa, 0.75)  # botões
        return roupa

    def costas(x, y):
        if y >= 10 and estilo not in ("vestido", "tunica"):
            return p.get("cinto", mul(calca, 0.7)) if y == 10 else calca
        if estilo == "terno" and y == 0:
            return mul(roupa, 0.85)
        if p.get("remendo") and (x, y) in ((5, 2), (6, 2), (5, 3)):
            return det
        return mul(roupa, 0.95)

    s.rect(20, 20, 8, 12, frente)
    s.rect(32, 20, 8, 12, costas)
    s.rect(16, 20, 4, 12, lambda x, y: mul(costas(x, y), 0.92))
    s.rect(28, 20, 4, 12, lambda x, y: mul(costas(x, y), 0.92))
    s.rect(20, 16, 8, 4, lambda x, y: pele if estilo in ("blusa", "vestido") and 2 <= x <= 5 and y == 1 else roupa)
    s.rect(28, 16, 8, 4, lambda x, y: roupa if estilo in ("vestido", "tunica") else calca)

    # braços
    manga = p.get("manga", 10)
    cor_manga = p.get("cor_manga", roupa)
    punho = p.get("punho")

    def braco(xt, yt, faces):
        s.rect(xt, yt, 4, 4, lambda x, y: cor_manga if manga > 0 else pele)
        s.rect(xt + 4, yt, 4, 4, lambda x, y: mul(pele, 0.9))
        for i, fx in enumerate(faces):
            f = 1.0 if i == 1 else 0.92

            def fn(x, y, f=f):
                if y < manga:
                    return mul(cor_manga, f)
                if punho and y == manga:
                    return mul(punho, f)
                return mul(pele, f)
            s.rect(fx, yt + 4, 4, 12, fn)

    braco(44, 16, [40, 44, 48, 52])
    braco(36, 48, [32, 36, 40, 44])

    # pernas
    saia = p.get("saia", 0)  # linhas cobertas pela saia (vestido/túnica)
    cor_saia = p.get("cor_saia", roupa)
    meia = p.get("meia", pele)
    sapato = p["sapato"]

    def perna(xt, yt, faces):
        s.rect(xt, yt, 4, 4, lambda x, y: cor_saia if saia else calca)
        s.rect(xt + 4, yt, 4, 4, lambda x, y: mul(sapato, 0.8))
        for i, fx in enumerate(faces):
            f = 1.0 if i == 1 else 0.92

            def fn(x, y, f=f):
                if y == 11 or (y == 10 and i == 1 and p.get("sapato_alto")):
                    return mul(sapato, f)
                if saia and y < saia:
                    return mul(cor_saia, f) if y < saia - 1 else mul(cor_saia, f * 0.85)
                if saia:
                    return mul(meia, f)
                if p.get("barra") and y == 10:
                    return mul(calca, f * 0.8)
                return mul(calca, f)
            s.rect(fx, yt + 4, 4, 12, fn)

    perna(4, 16, [0, 4, 8, 12])
    perna(20, 48, [16, 20, 24, 28])


# ------------------------------------------------------------------ Personagens

PELES = {"clara": hexc("E8BC9A"), "morena": hexc("C68B62"), "parda": hexc("A86E4A"), "negra": hexc("6E4630"), "idosa": hexc("D9A88A"), "palida": hexc("C9B8AC")}

PERSONAGENS = {
    # Trabalhadores encarnados
    "dirigente": dict(pele=PELES["morena"], cabelo=hexc("6B6A66"), olho=hexc("3B2A20"), estilo="curto", expr="sorriso",
                      corpo="colete", roupa=hexc("9CC0DE"), detalhe=hexc("4A4D57"), gola=hexc("9CC0DE"), calca=hexc("5A5C62"), sapato=hexc("2E2018"), manga=10),
    "medium_trabalhador": dict(pele=PELES["clara"], cabelo=hexc("4A2E1C"), olho=hexc("4B6B3A"), estilo="longo", expr="neutro",
                               corpo="blusa", roupa=hexc("5E8F6A"), calca=hexc("2F3A55"), sapato=hexc("3A2A22"), manga=8),
    "orador": dict(pele=PELES["parda"], cabelo=hexc("1E1A18"), olho=hexc("2A1C14"), estilo="curto", expr="neutro",
                   corpo="camisa", roupa=hexc("EEEDEA"), gola=hexc("FFFFFF"), gravata=hexc("1F2D4A"), calca=hexc("2A2C33"), sapato=hexc("1A1412"), manga=10,
                   punho=hexc("FFFFFF")),
    "passista": dict(pele=PELES["negra"], cabelo=hexc("1A1410"), olho=hexc("21160F"), estilo="coque", expr="sorriso",
                     corpo="blusa", roupa=hexc("F2F2EE"), calca=hexc("8FB3CF"), sapato=hexc("E0DED8"), manga=4),
    "evangelizadora": dict(pele=PELES["clara"], cabelo=hexc("8A4A26"), olho=hexc("4A3020"), estilo="longo", expr="sorriso",
                           corpo="vestido", roupa=hexc("F2C94C"), detalhe=hexc("E0A93A"), calca=hexc("F2C94C"), saia=8, cor_saia=hexc("4A6FA5"),
                           sapato=hexc("6B3A2A"), manga=4),
    "divaldo_franco": dict(pele=PELES["idosa"], cabelo=hexc("ECEBE6"), olho=hexc("3B2A20"), estilo="calvo", expr="sorriso",
                           corpo="terno", roupa=hexc("B9A98A"), gola=hexc("FAFAF7"), gravata=hexc("6E5A3C"), calca=hexc("A8987A"), sapato=hexc("3A2A20"), manga=10,
                           punho=hexc("FAFAF7")),
    # Espíritos (a translucidez e o tom vêm do renderizador)
    "mentor": dict(pele=PELES["clara"], cabelo=hexc("E6E2D6"), olho=hexc("5A7A9A"), estilo="curto", expr="sorriso",
                   corpo="tunica", roupa=hexc("F4F6FA"), detalhe=hexc("BFD6EA"), calca=hexc("F4F6FA"), saia=10, cor_saia=hexc("EEF1F6"),
                   sapato=hexc("D8D4CA"), manga=10),
    "socorrista": dict(pele=PELES["morena"], cabelo=hexc("3A2A1E"), olho=hexc("3B2A20"), estilo="curto", expr="neutro",
                       corpo="camisa", roupa=hexc("F3F5F2"), gola=hexc("FFFFFF"), calca=hexc("E8ECE6"), sapato=hexc("D8DCD4"), manga=10,
                       punho=hexc("BFE3C8")),
    "obsessor": dict(pele=PELES["palida"], cabelo=hexc("221E22"), olho=hexc("6A2A2A"), estilo="baguncado", expr="bravo",
                     corpo="camiseta", roupa=hexc("3A3640"), detalhe=hexc("2A262E"), calca=hexc("2C2A2E"), sapato=hexc("1A1818"), manga=10, remendo=True,
                     barra=True),
    "espirito_sofredor": dict(pele=PELES["palida"], cabelo=hexc("5A5650"), olho=hexc("4A4A4A"), estilo="baguncado", expr="triste",
                              corpo="camiseta", roupa=hexc("7A766E"), detalhe=hexc("5E5A54"), calca=hexc("5A5650"), sapato=hexc("3A3632"), manga=6, remendo=True,
                              barra=True),
    "zombeteiro": dict(pele=PELES["clara"], cabelo=hexc("C9632A"), olho=hexc("2E6A2E"), estilo="baguncado", expr="riso",
                       corpo="camiseta", roupa=hexc("E08A2E"), detalhe=hexc("7A3FA0"), calca=hexc("4C8A3A"), sapato=hexc("7A3FA0"), manga=4, remendo=True),
}

# ------------------------------------------------------------------ Ovos (mesma máscara dos ovos existentes)

EGG_MASK = [
    "......####......", ".....######.....", "....########....", "....########....",
    "...##########...", "...##########...", "...##########...", "..############..",
    "..############..", "..############..", "..############..", "..############..",
    "...##########...", "...##########...", "....########....", "......####......",
]
MANCHAS = [{(7, 4), (8, 4), (7, 5), (10, 7), (11, 7), (10, 8), (4, 9), (5, 9), (5, 10), (8, 11), (9, 11), (9, 12), (11, 11), (6, 13)},
           {(8, 3), (9, 3), (5, 6), (6, 6), (6, 7), (10, 9), (11, 9), (11, 10), (4, 11), (7, 10), (8, 13), (9, 13), (12, 8)},
           {(6, 4), (7, 4), (10, 5), (10, 6), (4, 8), (5, 8), (8, 8), (9, 8), (8, 9), (12, 10), (6, 12), (7, 12), (10, 12)}]

CORES_OVO = {
    "dirigente": ("4A4D57", "9CC0DE"), "medium_trabalhador": ("5E8F6A", "2F3A55"), "orador": ("2A2C33", "EEEDEA"),
    "passista": ("F2F2EE", "8FB3CF"), "evangelizadora": ("F2C94C", "4A6FA5"), "divaldo_franco": ("B9A98A", "FAFAF7"),
    "mentor": ("DDEEFF", "FFF6C8"), "socorrista": ("E8F3EA", "6FBF85"), "obsessor": ("2E2A34", "6A2A4A"),
    "espirito_sofredor": ("7A766E", "B8BCC4"), "zombeteiro": ("E08A2E", "7A3FA0"),
}


def ovo(nome, base, pinta, manchas, translucido):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    dentro = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and EGG_MASK[y][x] == "#"
    for y in range(16):
        for x in range(16):
            if not dentro(x, y):
                continue
            borda = not (dentro(x - 1, y) and dentro(x + 1, y) and dentro(x, y - 1) and dentro(x, y + 1))
            c = pinta if (x, y) in manchas else base
            if borda:
                c = mul(c, 0.55)
            elif x + y < 12 and (x, y) not in manchas:
                c = mul(c, 1.15)
            elif x + y > 20:
                c = mul(c, 0.82)
            c = var(c, 5)
            if translucido and not borda:
                c = c[:3] + (215,)
            img.putpixel((x, y), c)
    for pp in [(6, 2), (5, 3)]:
        img.putpixel(pp, (255, 255, 255, 255))
    img.save(os.path.join(OVOS, nome + "_spawn_egg.png"))


ESPIRITOS = {"mentor", "socorrista", "obsessor", "espirito_sofredor", "zombeteiro"}


def gerar(preview=None):
    os.makedirs(SKINS, exist_ok=True)
    os.makedirs(OVOS, exist_ok=True)
    for i, (nome, p) in enumerate(PERSONAGENS.items()):
        s = Skin()
        cabeca(s, p)
        corpo(s, p)
        s.img.save(os.path.join(SKINS, nome + ".png"))
        b, pt = CORES_OVO[nome]
        ovo(nome, hexc(b), hexc(pt), MANCHAS[i % 3], nome in ESPIRITOS)
    if preview:
        previa(preview)


def previa(pasta):
    """Folha de contato: skin crua ampliada + visão frontal montada (cabeça, corpo, braços, pernas)."""
    os.makedirs(pasta, exist_ok=True)
    nomes = list(PERSONAGENS)
    esc = 6
    folha = Image.new("RGBA", (len(nomes) * 18 * esc, 36 * esc), (70, 90, 110, 255))
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
        folha.alpha_composite(f, (i * 18 * esc + esc, 2 * esc))
    folha.save(os.path.join(pasta, "previa_frente.png"))
    crus = Image.new("RGBA", (len(nomes) * 66 * 3, 64 * 3), (70, 90, 110, 255))
    for i, nome in enumerate(nomes):
        sk = Image.open(os.path.join(SKINS, nome + ".png")).resize((64 * 3, 64 * 3), Image.NEAREST)
        crus.alpha_composite(sk, (i * 66 * 3, 0))
    crus.save(os.path.join(pasta, "previa_skins.png"))
    ovos = Image.new("RGBA", (len(nomes) * 18 * 4, 16 * 4), (70, 90, 110, 255))
    for i, nome in enumerate(nomes):
        o = Image.open(os.path.join(OVOS, nome + "_spawn_egg.png")).resize((64, 64), Image.NEAREST)
        ovos.alpha_composite(o, (i * 72, 0))
    ovos.save(os.path.join(pasta, "previa_ovos.png"))


if __name__ == "__main__":
    pv = sys.argv[sys.argv.index("--preview") + 1] if "--preview" in sys.argv else None
    gerar(pv)
    print("Skins em", os.path.normpath(SKINS), "e ovos em", os.path.normpath(OVOS))
