"""Gera as texturas pixel-art do Mod Espiritismo.

Uso (na pasta do workspace):  python arte_fonte/gerar_texturas.py
Rode de novo depois de um Build no MCreator, que recria os ovos com a textura padrão.
"""
import os
import random
from PIL import Image

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "mod_espiritismo_minecraft", "textures")
rnd = random.Random(1857)  # ano de O Livro dos Espíritos


def var(c, n=8):
    """Pequena variação de cor para dar textura (estilo Minecraft)."""
    d = rnd.randint(-n, n)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def escurecer(c, f):
    return tuple(int(v * f) for v in c[:3]) + (255,)


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


# ------------------------------------------------------------------ Ovos

EGG_MASK = [
    "......####......",
    ".....######.....",
    "....########....",
    "....########....",
    "...##########...",
    "...##########...",
    "...##########...",
    "..############..",
    "..############..",
    "..############..",
    "..############..",
    "..############..",
    "...##########...",
    "...##########...",
    "....########....",
    "......####......",
]


def ovo(base, pinta, manchas, nome):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    dentro = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and EGG_MASK[y][x] == "#"
    for y in range(16):
        for x in range(16):
            if not dentro(x, y):
                continue
            borda = not (dentro(x - 1, y) and dentro(x + 1, y) and dentro(x, y - 1) and dentro(x, y + 1))
            c = base
            if (x, y) in manchas:
                c = pinta
            if borda:
                c = escurecer(c, 0.55)
            elif x + y < 12 and (x, y) not in manchas:
                c = tuple(min(255, int(v * 1.18)) for v in c[:3]) + (255,)  # brilho
            elif x + y > 20:
                c = escurecer(c, 0.82)  # sombra
            img.putpixel((x, y), var(c, 5))
    # reflexo
    for p in [(6, 2), (5, 3)]:
        img.putpixel(p, (255, 255, 255, 255))
    img.save(os.path.join(RAIZ, "item", nome))


MANCHAS_A = {(7, 4), (8, 4), (7, 5), (10, 7), (11, 7), (10, 8), (4, 9), (5, 9), (5, 10), (8, 11), (9, 11), (9, 12), (11, 11), (6, 13)}
MANCHAS_B = {(8, 3), (9, 3), (5, 6), (6, 6), (6, 7), (10, 9), (11, 9), (11, 10), (4, 11), (7, 10), (8, 13), (9, 13), (12, 8)}
MANCHAS_C = {(6, 4), (7, 4), (10, 5), (10, 6), (4, 8), (5, 8), (8, 8), (9, 8), (8, 9), (12, 10), (6, 12), (7, 12), (10, 12)}

# ------------------------------------------------------------------ Livro dos Espíritos (item 16x16)

LIVRO = [
    "................",
    "..CCCCCCCCCCC...",
    ".CMMMMMMMMMMMC..",
    ".CMGGGGGGGGGMCP.",
    ".CMGMMMMMMMGMCP.",
    ".CMGMYYYYYMGMCP.",
    ".CMGMMMMMMMGMCP.",
    ".CMGMYYYYMMGMCP.",
    ".CMGMMMMMMMGMCP.",
    ".CMGMYYYYYMGMCP.",
    ".CMGMMMMMMMGMCP.",
    ".CMGMMSSMMMGMCP.",
    ".CMGGGGGGGGGMCP.",
    ".CMMMMMMMMMMMCP.",
    "..CCCCCCCCCCCPP.",
    "...PPPPPPPPPPP..",
]
COR_LIVRO = {
    "C": hexc("3A2212"),  # contorno
    "M": hexc("7A4A24"),  # couro
    "G": hexc("C9A23C"),  # moldura dourada
    "Y": hexc("E8CF7A"),  # título
    "S": hexc("F3E6A8"),  # assinatura "A. Kardec"
    "P": hexc("EDE3C8"),  # páginas
}


def livro():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, linha in enumerate(LIVRO):
        for x, ch in enumerate(linha):
            if ch != ".":
                c = COR_LIVRO[ch]
                img.putpixel((x, y), var(c, 6 if ch == "M" else 3))
    # lombada sombreada
    for y in range(2, 14):
        img.putpixel((2, y), var(escurecer(COR_LIVRO["M"], 0.75), 3))
    img.save(os.path.join(RAIZ, "item", "livro_dos_espiritos.png"))


# ------------------------------------------------------------------ Skin de Allan Kardec (64x64, modelo clássico)
# Retrato clássico (c. 1860): calvo no alto, cabelos e barba grisalhos curtos,
# sobrecasaca preta, colete escuro, camisa branca de colarinho alto e gravata preta.

PELE = hexc("D9A88A")
PELE_S = hexc("C08E70")
CINZA = hexc("B5B3AE")
CINZA_E = hexc("8E8C88")
CINZA_C = hexc("D6D4CF")
OLHO = hexc("3B2A20")
BRANCO = hexc("ECE8E0")
CASACO = hexc("1E1E24")
CASACO_C = hexc("2C2C34")
COLETE = hexc("3A3430")
GRAVATA = hexc("0F0F12")
BOTAO = hexc("6A6A70")
CALCA = hexc("26262C")
SAPATO = hexc("161210")


def pintar(img, x0, y0, linhas, mapa):
    for y, linha in enumerate(linhas):
        for x, ch in enumerate(linha):
            if ch in mapa and mapa[ch] is not None:
                img.putpixel((x0 + x, y0 + y), var(mapa[ch], 4))


def preencher(img, x0, y0, w, h, cor, n=4):
    for y in range(h):
        for x in range(w):
            img.putpixel((x0 + x, y0 + y), var(cor, n))


M = {"s": PELE, "d": PELE_S, "g": CINZA, "e": CINZA_E, "c": CINZA_C, "o": OLHO, "w": BRANCO,
     "k": CASACO, "K": CASACO_C, "v": COLETE, "t": GRAVATA, "b": BOTAO, "p": CALCA, "z": SAPATO}


def kardec():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    # --- Cabeça
    pintar(img, 8, 0, ["eggggggе"[:7] + "e", "gssssssg", "gssssssg", "ssssssss", "ssssssss", "gssssssg", "ggsssggg"[:8], "gggggggg"], M)  # topo (calvo)
    pintar(img, 16, 0, ["eeeeeeee", "egggggge", "eggccgge", "egggggge", "eggcggge", "egggggge", "eggggcge", "eeeeeeee"], M)  # baixo (barba)
    pintar(img, 8, 8, [  # frente
        "gsssssss"[:7] + "g",
        "gssssssg",
        "seessees",
        "swosswos"[:8],
        "gssddssg",
        "ggcggcgg",
        "gggeeggg",
        "eggggggе"[:7] + "e",
    ], M)
    pintar(img, 0, 8, ["ssssssss", "gsssssss", "ggssssss", "gggsdsss", "gggsssgg", "ggggggge", "egggggge", "eegggggg"], M)  # lado direito
    pintar(img, 16, 8, ["ssssssss", "ssssssg"[:7] + "g", "ssssssgg", "sssdsggg", "ggsssggg", "eggggggg", "egggggge", "ggggggee"], M)  # lado esquerdo
    pintar(img, 24, 8, ["gsssssssg"[:8], "gggggggg", "gcgggcgg", "gggggggg", "ggggcggg", "egggggge", "eggggggе"[:7] + "e", "eeeeeeee"], M)  # nuca
    # --- Corpo (sobrecasaca)
    preencher(img, 20, 16, 8, 4, CASACO)  # topo
    preencher(img, 28, 16, 8, 4, CASACO)  # base
    pintar(img, 20, 20, [  # frente
        "kKwwwwKk",
        "kKwttwKk",
        "kkKwwKkk",
        "kkvbvvkk"[:8],
        "kkvvvvkk",
        "kkvbvvkk",
        "kkvvvvkk",
        "kkvbvvkk",
        "kkkvvkkk",
        "kkkKKkkk",
        "kkkkkkkk",
        "kkkkkkkk",
    ], M)
    preencher(img, 16, 20, 4, 12, CASACO)  # lado direito
    preencher(img, 28, 20, 4, 12, CASACO)  # lado esquerdo
    preencher(img, 32, 20, 8, 12, CASACO)  # costas
    for x in range(32, 40):  # gola nas costas
        img.putpixel((x, 20), var(CASACO_C, 3))
    for y in range(26, 32):  # fenda da casaca
        img.putpixel((36, y), var(GRAVATA, 2))

    # --- Braços: manga de casaca, punho branco e mão
    def braco(xt, yt, faces):
        preencher(img, xt, yt, 4, 4, CASACO)  # topo
        preencher(img, xt + 4, yt, 4, 4, PELE)  # base = mão
        for fx in faces:
            for y in range(12):
                for x in range(4):
                    cor = CASACO if y < 10 else (BRANCO if y == 10 else PELE)
                    img.putpixel((fx + x, yt + 4 + y), var(cor, 4))

    braco(44, 16, [40, 44, 48, 52])
    braco(36, 48, [32, 36, 40, 44])

    # --- Pernas: barra da casaca até o joelho, calça e sapatos
    def perna(xt, yt, faces, frente):
        preencher(img, xt, yt, 4, 4, CASACO)
        preencher(img, xt + 4, yt, 4, 4, SAPATO)
        for fx in faces:
            for y in range(12):
                for x in range(4):
                    if y < 4:
                        cor = CASACO
                    elif y < 11:
                        cor = CALCA
                    else:
                        cor = SAPATO
                    img.putpixel((fx + x, yt + 4 + y), var(cor, 4))
        # abertura da casaca na frente
        if frente is not None:
            img.putpixel((frente[0], yt + 4), var(COLETE, 2))

    perna(4, 16, [0, 4, 8, 12], (7, 0))
    perna(20, 48, [16, 20, 24, 28], (20, 0))
    img.save(os.path.join(RAIZ, "entities", "allankardec.png"))


if __name__ == "__main__":
    ovo(hexc("3B2A1E"), hexc("E8E1D0"), MANCHAS_A, "allan_kardec_spawn_egg_generated.png")
    ovo(hexc("6B4F3A"), hexc("4A7BB7"), MANCHAS_B, "chico_xavier_spawn_egg_generated.png")
    ovo(hexc("1F2D4A"), hexc("D4AF37"), MANCHAS_C, "haroldo_dutra_spawn_egg_generated.png")
    livro()
    kardec()
    print("Texturas geradas em", os.path.normpath(RAIZ))


# ------------------------------------------------------------------ Bússola Espiritual (16x16)
def bussola():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= 7.2:
                if d > 6.0:
                    c = hexc("6B5A3A") if d > 6.6 else hexc("C9A23C")  # aro dourado
                else:
                    c = hexc("1E3550") if (x + y) % 5 else hexc("24405E")  # mostrador azul-noite
                img.putpixel((x, y), var(c, 4))
    # agulha: ponta clara (luz) para cima-direita, cauda escura
    for i, (x, y) in enumerate([(10, 4), (9, 5), (8, 6), (7, 7)]):
        img.putpixel((x, y), hexc("F3F7FF") if i < 2 else hexc("9FD6FF"))
    for x, y in [(6, 8), (5, 9), (4, 10)]:
        img.putpixel((x, y), hexc("5B6B80"))
    img.putpixel((7, 8), hexc("E8CF7A"))
    # brilho
    for x, y in [(4, 3), (3, 4)]:
        img.putpixel((x, y), (190, 220, 255, 255))
    img.save(os.path.join(RAIZ, "item", "bussola_espiritual.png"))


bussola()
