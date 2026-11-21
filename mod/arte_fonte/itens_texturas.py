"""Texturas pixel-art da área "itens" do Mod Espiritismo (blocos, obras, itens de caridade).

Uso (na pasta do workspace):  python arte_fonte/itens_texturas.py
Gera também previews ampliados em E:/claude-mod-trabalho/tmp/preview_itens (fora do src).

Observação: arte_fonte/gerar_texturas.py (antigo) também grava textures/item/livro_dos_espiritos.png;
rode ESTE script depois dele (ou remova a chamada livro() de lá) para manter a capa no estilo 1857.
"""
import os
import random
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(AQUI, "..", "src", "main", "resources", "assets", "mod_espiritismo_minecraft", "textures")
ITEM = os.path.join(TEX, "item", "itens")
BLOCO = os.path.join(TEX, "block", "itens")
PREVIEW = "E:/claude-mod-trabalho/tmp/preview_itens"
rnd = random.Random(1857)


def hexc(h, a=255):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def var(c, n=6):
    d = rnd.randint(-n, n)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def mul(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def nova():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def ret(img, x0, y0, x1, y1, cor, n=0):
    """Preenche [x0,x1] x [y0,y1] (inclusive)."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            img.putpixel((x, y), var(cor, n) if n else cor)


def px(img, x, y, cor):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), cor)


def salvar(img, pasta, nome):
    os.makedirs(pasta, exist_ok=True)
    img.save(os.path.join(pasta, nome))
    TODAS.append((nome, img))


TODAS = []

# ------------------------------------------------------------------ Ícones de livros (obras)
# Livro visto de frente: lombada à esquerda, borda das páginas à direita e embaixo.


def icone_livro(capa, lombada, desenho, pagina=hexc("efe6cf")):
    img = nova()
    contorno = mul(lombada, 0.55)
    # contorno
    ret(img, 2, 1, 13, 14, contorno)
    # páginas (direita e base)
    ret(img, 12, 2, 12, 13, pagina)
    ret(img, 4, 13, 12, 13, pagina)
    for y in range(3, 13, 2):
        px(img, 12, y, mul(pagina, 0.85))
    # capa e lombada
    ret(img, 4, 2, 11, 12, capa, 4)
    ret(img, 3, 2, 3, 13, lombada, 3)
    for y in (3, 6, 9, 12):
        px(img, 3, y, mul(lombada, 1.25))
    desenho(img)
    return img


def capa_1857(img, x0=4, y0=2, x1=11, y1=12):
    """Capa de brochura de 1857: bege claro, fio fino de moldura, título em tinta escura,
    pequeno ornamento, linha do autor e, embaixo, "Paris 1857"."""
    tinta = hexc("2b2118")
    fio = hexc("8c7656")
    for x in range(x0, x1 + 1):
        px(img, x, y0, fio)
        px(img, x, y1, fio)
    for y in range(y0, y1 + 1):
        px(img, x0, y, fio)
        px(img, x1, y, fio)
    alto = y1 - y0 >= 10
    def linha(y, xa, xb, cor, lacunas=()):
        for x in range(xa, xb + 1):
            if x - xa not in lacunas:
                px(img, x, y, cor)
    # título "LE LIVRE DES ESPRITS": uma linha forte de largura total, outra mais clara
    linha(y0 + 2, x0 + 1, x1 - 1, mul(tinta, 1.3), ())
    linha(y0 + 3, x0 + 2, x1 - 2, mul(tinta, 2.6), ())
    # subtítulo (texto miúdo pontilhado)
    sub = mul(tinta, 3.6)
    for x in range(x0 + 1, x1):
        if (x - x0) % 2:
            px(img, x, y0 + 5, sub)
    if alto:
        for x in range(x0 + 2, x1 - 1):
            if (x - x0) % 2 == 0:
                px(img, x, y0 + 6, sub)
    # autor "ALLAN KARDEC"
    linha(y0 + (8 if alto else 6), x0 + 2, x1 - 2, mul(tinta, 2.8), ())
    # "PARIS 1857"
    linha(y1 - 1, (x0 + x1) // 2 - 1, (x0 + x1) // 2 + 1, sub, ())


def titulo_dourado(cor_titulo, ornamento=None):
    def f(img):
        for x in range(5, 11):
            if x != 7:
                px(img, x, 3, cor_titulo)
            if x != 9:
                px(img, x, 4, cor_titulo)
        if ornamento:
            ornamento(img, cor_titulo)
        for x in range(6, 10):
            px(img, x, 11, mul(cor_titulo, 0.85))
    return f


def orn_cruz_luz(img, c):  # LM: pena (psicografia)
    for i in range(4):
        px(img, 9 - i, 7 + i, c)
    px(img, 10, 7, mul(c, 1.1))


def orn_coracao(img, c):  # ESE: caridade
    for (x, y) in [(6, 7), (7, 7), (9, 7), (10, 7), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (7, 9), (8, 9), (9, 9), (8, 10)]:
        px(img, x - 0 if x < 11 else x, y, c)


def orn_balanca(img, c):  # Céu e Inferno: justiça divina
    for x in range(5, 11):
        px(img, x, 7, c)
    for y in range(7, 11):
        px(img, 8, y, c)
    px(img, 5, 8, c), px(img, 10, 8, c)
    px(img, 4 + 1, 9, mul(c, 0.8)), px(img, 10, 9, mul(c, 0.8))


def orn_terra(img, c):  # Gênese: planeta
    azul, verde = hexc("4a78b8"), hexc("5f9a4a")
    for (x, y) in [(7, 7), (8, 7), (6, 8), (7, 8), (8, 8), (9, 8), (6, 9), (7, 9), (8, 9), (9, 9), (7, 10), (8, 10)]:
        px(img, x, y, azul)
    for (x, y) in [(7, 8), (8, 9), (9, 8)]:
        px(img, x, y, verde)


def orn_cidade(img, c):  # Nosso Lar: cidade de luz
    for (x0, h) in [(5, 2), (6, 3), (7, 4), (8, 3), (9, 4), (10, 2)]:
        for y in range(10 - h, 10):
            px(img, x0, y, c)
    for x in range(5, 11):
        px(img, x, 10, mul(c, 0.8))


def orn_interrogacao(img, c):  # O que é o Espiritismo
    for (x, y) in [(7, 7), (8, 7), (9, 8), (8, 9), (8, 10)]:
        px(img, x, y, c)
    px(img, 6, 8, c)


def icone_revista():
    """Revista Espírita: fascículo de papel envelhecido com cabeçalho e duas colunas."""
    img = nova()
    papel = hexc("e2d3ad")
    borda = hexc("6e5a3a")
    tinta = hexc("3a2e22")
    ret(img, 3, 1, 12, 14, borda)
    ret(img, 4, 2, 11, 13, papel, 5)
    px(img, 13, 2, borda)
    ret(img, 13, 2, 13, 14, mul(papel, 0.8))  # folhas por baixo
    ret(img, 4, 14, 13, 14, mul(papel, 0.75))
    for x in range(5, 11):
        px(img, x, 3, tinta)
    for x in range(6, 10):
        px(img, x, 4, mul(tinta, 1.8))
    for x in range(4, 12):
        px(img, x, 5, borda)
    for y in range(7, 13):
        for x in (5, 6, 9, 10):
            if (x + y) % 3:
                px(img, x, y, mul(tinta, 2.4))
    for y in range(6, 13):
        px(img, 8 - 0, y, mul(papel, 0.88))
    return img


def icone_caderno():
    """Caderno de psicografia: capa azul simples, espiral e etiqueta; lápis ao lado."""
    img = nova()
    capa = hexc("3d5f8c")
    ret(img, 3, 1, 12, 14, mul(capa, 0.6))
    ret(img, 4, 2, 11, 13, capa, 4)
    ret(img, 12, 2, 12, 13, hexc("f2efe6"))
    for y in range(2, 14, 2):
        px(img, 3, y, hexc("c8c8c8"))
        px(img, 2, y, hexc("8a8a8a"))
    ret(img, 6, 4, 10, 6, hexc("efe9d8"))
    for x in range(7, 10):
        px(img, x, 5, hexc("6b6b6b"))
    # lápis diagonal no canto
    for i in range(5):
        px(img, 9 + i // 1 - 0, 13 - i, hexc("e6b830"))
    px(img, 14, 8, hexc("f0c8a0"))
    px(img, 14, 7, hexc("303030"))
    px(img, 8, 14, hexc("e88a9a"))
    return img


def icone_sopa():
    img = nova()
    tigela = hexc("8a5a2b")
    sopa = hexc("d98b3a")
    # tigela (vista 3/4)
    for x in range(2, 14):
        px(img, x, 8, mul(tigela, 1.15))
    ret(img, 3, 9, 12, 9, sopa, 6)
    ret(img, 2, 10, 13, 10, tigela, 4)
    ret(img, 3, 11, 12, 11, mul(tigela, 0.9), 4)
    ret(img, 4, 12, 11, 12, mul(tigela, 0.8), 4)
    ret(img, 5, 13, 10, 13, mul(tigela, 0.65))
    ret(img, 3, 7, 12, 7, sopa, 8)
    ret(img, 4, 6, 11, 6, mul(sopa, 1.08), 8)
    # legumes
    for (x, y, c) in [(5, 6, "e8702a"), (8, 7, "f0d070"), (10, 6, "e8702a"), (6, 7, "6aa040"), (9, 6, "a8304a")]:
        px(img, x, y, hexc(c))
    # vapor
    for (x, y) in [(6, 4), (7, 3), (6, 2), (9, 4), (10, 3), (9, 2)]:
        px(img, x, y, (235, 235, 235, 150))
    return img


def icone_agasalho():
    img = nova()
    la = hexc("b8463a")
    m = [
        "....##....##....",
        "..############..",
        ".##############.",
        "################",
        "###.########.###",
        "###.########.###",
        "##..########..##",
        "##..########..##",
        "....########....",
        "....########....",
        "....########....",
        "....########....",
        "....########....",
        "....########....",
    ]
    for y, linha in enumerate(m):
        for x, ch in enumerate(linha):
            if ch == "#":
                c = var(la, 6)
                if (x + y) % 4 == 0:
                    c = mul(c, 0.9)
                px(img, x, y + 1, c)
    # gola e barra canelada
    for x in range(6, 10):
        px(img, x, 1, mul(la, 0.7))
    for x in range(4, 12):
        px(img, x, 14, mul(la, 0.75))
    for y in (7, 8):
        px(img, 0, y, mul(la, 0.75)), px(img, 15, y, mul(la, 0.75))
    # faixa branca
    for x in range(4, 12):
        px(img, x, 6, hexc("eeeeee"))
    # contorno escuro nas bordas
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] == 0:
                continue
            viz = [(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if any(not (0 <= a < 16 and 0 <= b < 16) or img.getpixel((a, b))[3] == 0 for a, b in viz):
                out.putpixel((x, y), mul(img.getpixel((x, y)), 0.7))
    return out


def icone_cesta():
    img = nova()
    vime = hexc("b88a4a")
    # alça
    for (x, y) in [(4, 5), (4, 4), (5, 3), (6, 2), (7, 2), (8, 2), (9, 2), (10, 3), (11, 4), (11, 5)]:
        px(img, x, y, mul(vime, 0.8))
    # conteúdo: pão, maçã, cenoura, batata
    ret(img, 3, 6, 7, 7, hexc("c88a3a"), 6)  # pão
    px(img, 4, 6, hexc("e0b060")), px(img, 6, 6, hexc("e0b060"))
    ret(img, 9, 5, 11, 7, hexc("c8202a"), 6)  # maçã
    px(img, 10, 4, hexc("4a7a2a"))
    for i in range(4):
        px(img, 8 + 0, 7 - i, hexc("e8762a"))  # cenoura em pé
    px(img, 8, 3, hexc("5aa040"))
    ret(img, 11, 7, 12, 8, hexc("c8a060"), 4)  # batata
    # cesto trançado
    for y in range(8, 14):
        x0, x1 = (2, 13) if y < 12 else (3, 12)
        for x in range(x0, x1 + 1):
            c = vime if (x + y) % 2 else mul(vime, 0.8)
            px(img, x, y, var(c, 5))
    for x in range(2, 14):
        px(img, x, 8, mul(vime, 1.15))
    for x in range(4, 12):
        px(img, x, 14, mul(vime, 0.6))
    return img


# ------------------------------------------------------------------ Texturas de bloco


def bloco_livro_1857():
    """Atlas do livro deitado (modelo 7x2x9):
    capa de cima [0,0,7,9]; contracapa [7,0,14,9]; borda das capas: coluna x=15;
    lombada [0,9,9,10]; páginas (lado comprido) [0,11,9,12]; páginas (lado curto) [9,11,16,12]."""
    img = nova()
    bege = hexc("e3d4b0")
    ret(img, 0, 0, 6, 8, bege, 4)
    capa_1857(img, 0, 0, 6, 8)
    ret(img, 7, 0, 13, 8, mul(bege, 0.95), 4)
    for x in range(7, 14):
        px(img, x, 0, hexc("8c7656")), px(img, x, 8, hexc("8c7656"))
    for y in range(0, 9):
        px(img, 7, y, hexc("8c7656")), px(img, 13, y, hexc("8c7656"))
    ret(img, 15, 0, 15, 8, hexc("b8a47e"))
    # lombada: bege mais escuro com faixas
    ret(img, 0, 9, 8, 9, hexc("c8b48a"), 3)
    for x in (1, 4, 7):
        px(img, x, 9, hexc("5a4630"))
    # páginas
    ret(img, 0, 11, 15, 11, hexc("f4eedd"), 3)
    for x in range(1, 16, 3):
        px(img, x, 11, hexc("ddd3b8"))
    return img


def bloco_livro_aberto():
    """Livro aberto sobre a tribuna: duas páginas com linhas, dobra ao centro."""
    img = nova()
    ret(img, 0, 0, 15, 11, hexc("f4eedd"), 3)
    for y in range(2, 11, 2):
        for x in list(range(1, 7)) + list(range(9, 15)):
            if rnd.random() < 0.8:
                px(img, x, y, hexc("8a8070"))
    ret(img, 7, 0, 8, 11, hexc("d8cfb6"))
    ret(img, 0, 12, 15, 15, hexc("6a3a22"), 3)  # capa (bordas)
    return img


def madeira(cor, nome, veios=True):
    img = nova()
    for y in range(16):
        for x in range(16):
            c = var(cor, 5)
            if veios and (y % 4 == 0 or (x * 7 + y * 3) % 11 == 0):
                c = mul(c, 0.88)
            px(img, x, y, c)
    return img


def toalha():
    img = nova()
    branco = hexc("f3f1ea")
    ret(img, 0, 0, 15, 15, branco, 3)
    for x in range(16):
        if x % 2 == 0:
            px(img, x, 15, hexc("d8d4c8"))  # renda na barra
        px(img, x, 14, hexc("e2ded2"))
    for i in range(0, 16, 4):  # trama discreta
        for j in range(0, 16, 4):
            px(img, i, j, hexc("e8e5dc"))
    return img


def papel():
    img = nova()
    ret(img, 0, 0, 15, 15, hexc("f7f5ee"), 2)
    for y in range(3, 15, 2):
        for x in range(2, 14):
            px(img, x, y, hexc("c9d6e8"))
    # escrita a lápis em algumas linhas (sugestão de psicografia)
    for y in (3, 5, 7, 9):
        for x in range(2, rnd.randint(8, 13)):
            if rnd.random() < 0.7:
                px(img, x, y - 1 + 1, hexc("5a5a62"))
    return img


def lapis():
    """Faixa: corpo amarelo (x 0..11), madeira apontada (12..13), grafite (14..15)."""
    img = nova()
    for y in range(16):
        for x in range(16):
            if x < 12:
                c = hexc("e6b830") if y % 4 else hexc("c89a20")
            elif x < 14:
                c = hexc("e8c89a")
            else:
                c = hexc("3a3a3a")
            px(img, x, y, c)
    return img


def vidro():
    img = nova()
    for y in range(16):
        for x in range(16):
            borda = x in (0, 15) or y in (0, 15)
            a = 150 if borda else 60
            px(img, x, y, (220, 240, 245, a))
    for y in range(2, 9):  # reflexo
        px(img, 3, y, (255, 255, 255, 170))
    return img


def agua():
    img = nova()
    for y in range(16):
        for x in range(16):
            c = (60, 120, 210, 170)
            if (x + 2 * y) % 7 == 0:
                c = (110, 160, 230, 170)
            px(img, x, y, c)
    return img


def tribuna_frente():
    """Painel frontal da tribuna: madeira escura com moldura em relevo."""
    base = hexc("6b4426")
    img = madeira(base, "", veios=False)
    for x in range(16):
        for y in range(16):
            if x in (2, 13) and 2 <= y <= 13 or y in (2, 13) and 2 <= x <= 13:
                px(img, x, y, mul(base, 1.25))
            if x in (3, 12) and 3 <= y <= 12 or y in (3, 12) and 3 <= x <= 12:
                px(img, x, y, mul(base, 0.7))
    return img


def main():
    os.makedirs(PREVIEW, exist_ok=True)
    bege, lomb = hexc("e3d4b0"), hexc("b89f74")

    def desenho_1857(img):
        capa_1857(img)

    # Ícone do item MCreator livrodos_espiritos (substitui o antigo)
    salvar(icone_livro(bege, lomb, desenho_1857), os.path.join(TEX, "item"), "livro_dos_espiritos.png")
    # Obras legíveis
    salvar(icone_livro(hexc("e6dcc0"), hexc("a8936a"), desenho_1857), ITEM, "o_livro_dos_espiritos.png")
    ouro = hexc("e0c060")
    salvar(icone_livro(hexc("6e2a22"), hexc("4a1a14"), titulo_dourado(ouro, orn_cruz_luz)), ITEM, "o_livro_dos_mediuns.png")
    salvar(icone_livro(hexc("23407a"), hexc("172a52"), titulo_dourado(ouro, orn_coracao)), ITEM, "o_evangelho_segundo_o_espiritismo.png")
    salvar(icone_livro(hexc("2e2a3a"), hexc("1c1a26"), titulo_dourado(hexc("d8c070"), orn_balanca)), ITEM, "o_ceu_e_o_inferno.png")
    salvar(icone_livro(hexc("2f5a34"), hexc("1d3a22"), titulo_dourado(ouro, orn_terra)), ITEM, "a_genese.png")
    salvar(icone_livro(hexc("9cc8e8"), hexc("6a9cc0"), titulo_dourado(hexc("ffffff"), orn_cidade)), ITEM, "nosso_lar.png")
    salvar(icone_livro(hexc("a8642a"), hexc("7a4418"), titulo_dourado(hexc("f2e2b0"), orn_interrogacao)), ITEM, "o_que_e_o_espiritismo.png")
    salvar(icone_revista(), ITEM, "revista_espirita.png")
    salvar(icone_caderno(), ITEM, "caderno_psicografia.png")
    salvar(icone_sopa(), ITEM, "sopa_fraterna.png")
    salvar(icone_agasalho(), ITEM, "agasalho.png")
    salvar(icone_cesta(), ITEM, "cesta_basica.png")
    # Blocos
    salvar(bloco_livro_1857(), BLOCO, "livro_1857.png")
    salvar(bloco_livro_aberto(), BLOCO, "livro_aberto.png")
    salvar(madeira(hexc("8a5a32"), ""), BLOCO, "madeira_mesa.png")
    salvar(madeira(hexc("5e3a20"), ""), BLOCO, "madeira_tribuna.png")
    salvar(tribuna_frente(), BLOCO, "tribuna_frente.png")
    salvar(toalha(), BLOCO, "toalha.png")
    salvar(papel(), BLOCO, "papel.png")
    salvar(lapis(), BLOCO, "lapis.png")
    salvar(vidro(), BLOCO, "vidro_jarra.png")
    salvar(agua(), BLOCO, "agua_jarra.png")

    # Preview: todas lado a lado, 8x, sobre fundo cinza
    esc = 8
    cols = 8
    linhas = (len(TODAS) + cols - 1) // cols
    prev = Image.new("RGBA", (cols * 17 * esc, linhas * 17 * esc), (120, 120, 120, 255))
    for i, (nome, img) in enumerate(TODAS):
        big = img.resize((16 * esc, 16 * esc), Image.NEAREST)
        prev.alpha_composite(big, ((i % cols) * 17 * esc, (i // cols) * 17 * esc))
    prev.save(os.path.join(PREVIEW, "todas.png"))
    print("\n".join(f"{i}: {n}" for i, (n, _) in enumerate(TODAS)))


if __name__ == "__main__":
    main()
