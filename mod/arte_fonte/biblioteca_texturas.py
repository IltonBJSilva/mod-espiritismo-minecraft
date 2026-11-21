"""Texturas pixel-art da área "biblioteca": estante (lado/topo) e item catálogo.

Uso (na pasta do workspace):  python arte_fonte/biblioteca_texturas.py
Preview ampliado em E:/claude-mod-trabalho/tmp/preview_biblioteca.png (fora do src).
"""
import os
import random
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(AQUI, "..", "src", "main", "resources", "assets", "mod_espiritismo_minecraft", "textures")
rnd = random.Random(1858)


def var(c, n=8):
    d = rnd.randint(-n, n)
    return tuple(max(0, min(255, v + d)) for v in c) + (255,)


MADEIRA = (112, 82, 48)
MADEIRA_ESC = (74, 52, 30)
TABUA = (156, 118, 72)
CAPAS = [(40, 60, 120), (120, 40, 40), (40, 100, 60), (150, 120, 50), (90, 50, 110), (60, 90, 110), (170, 150, 110)]


def lado():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), var(MADEIRA, 6))
    for y0 in (1, 9):  # duas prateleiras
        x = 1
        while x < 15:
            w = rnd.choice([1, 2, 2])
            h = rnd.choice([5, 6, 6])
            cor = rnd.choice(CAPAS)
            for xx in range(x, min(15, x + w)):
                for yy in range(y0 + (6 - h), y0 + 6):
                    img.putpixel((xx, yy), var(cor, 10))
                img.putpixel((xx, y0 + (6 - h) + 1), var((220, 200, 140), 6))  # faixa dourada da lombada
            x += w
        for x in range(16):
            img.putpixel((x, y0 + 6), var(MADEIRA_ESC, 4))
    for y in range(16):
        img.putpixel((0, y), var(MADEIRA_ESC, 4))
        img.putpixel((15, y), var(MADEIRA_ESC, 4))
    for x in range(16):
        img.putpixel((x, 0), var(MADEIRA_ESC, 4))
        img.putpixel((x, 15), var(MADEIRA_ESC, 4))
    return img


def topo():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = TABUA if (y // 4) % 2 == 0 else (146, 108, 64)
            img.putpixel((x, y), var(c, 6))
        if y % 4 == 3:
            for x in range(16):
                img.putpixel((x, y), var(MADEIRA, 4))
    return img


def catalogo():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    capa = (60, 90, 70)
    for y in range(2, 15):
        for x in range(3, 13):
            img.putpixel((x, y), var(capa, 6))
    for y in range(2, 15):
        img.putpixel((3, y), var((40, 60, 46), 4))
    for y in range(3, 14):
        img.putpixel((13, y), var((235, 228, 205), 4))
    for x in range(4, 13):
        img.putpixel((x, 15), var((235, 228, 205), 4))
    for x in range(5, 12):  # etiqueta
        for y in range(5, 8):
            img.putpixel((x, y), var((230, 210, 150), 5))
    for x in range(6, 11):
        img.putpixel((x, 6), (90, 70, 40, 255))
    for y in range(10, 13):  # fita marcador
        img.putpixel((10, y), (170, 40, 40, 255))
    img.putpixel((10, 13), (170, 40, 40, 255))
    return img


def salvar(img, *partes):
    p = os.path.join(TEX, *partes)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


l, t, c = lado(), topo(), catalogo()
salvar(l, "block", "biblioteca", "estante_lado.png")
salvar(t, "block", "biblioteca", "estante_topo.png")
salvar(c, "item", "biblioteca", "catalogo.png")
prev = Image.new("RGBA", (16 * 3 * 10 + 40, 160), (40, 40, 40, 255))
for i, im in enumerate((l, t, c)):
    prev.paste(im.resize((160, 160), Image.NEAREST), (i * 180, 0), im.resize((160, 160), Image.NEAREST))
prev.save("E:/claude-mod-trabalho/tmp/preview_biblioteca.png")
print("ok")
