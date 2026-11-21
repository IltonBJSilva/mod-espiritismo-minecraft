"""Gera os JSON da área "itens": modelos de bloco (Blockbench-style), blockstates com FACING, definições de item,
modelos de item, loot tables, receitas e a tag de atril.

Uso (na pasta do workspace):  python arte_fonte/itens_json.py
"""
import json
import os

AQUI = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(AQUI, "..", "src", "main", "resources")
NS = "mod_espiritismo_minecraft"
A = f"{R}/assets/{NS}"
D = f"{R}/data/{NS}"


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def faces(tex, uv, which=("north", "south", "east", "west", "up", "down"), **extra):
    return {k: {"uv": uv, "texture": tex, **extra} for k in which}


def el(fr, to, fcs, rot=None):
    e = {"from": fr, "to": to, "faces": fcs}
    if rot:
        e["rotation"] = rot
    return e


def T(n):
    return f"{NS}:block/itens/{n}"


def translucida(n):
    return {"sprite": T(n), "force_translucent": True}


DISPLAY_PEQUENO = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [1.1, 1.1, 1.1]},
    "fixed": {"rotation": [-90, 0, 0], "translation": [0, 0, -4], "scale": [1, 1, 1]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.6, 0.6, 0.6]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.7, 0.7, 0.7]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 3, 0], "scale": [0.7, 0.7, 0.7]},
}


def modelos():
    # ---------------- livro_1857: 7x2x9, lombada a leste; título legível para quem colocou o bloco
    borda = [15, 0, 16, 1]
    livro = {"parent": "block/block", "display": DISPLAY_PEQUENO,
             "textures": {"particle": T("livro_1857"), "l": T("livro_1857")},
             "elements": [
                 el([4.5, 0, 3.5], [11.5, 0.5, 12.5], {"down": {"uv": [7, 0, 14, 9], "texture": "#l", "cullface": "down"},
                                                       "up": {"uv": [7, 0, 14, 9], "texture": "#l"},
                                                       **faces("#l", borda, ("north", "south", "east", "west"))}),
                 el([5, 0.5, 4], [11, 1.5, 12], {"west": {"uv": [0, 11, 8, 12], "texture": "#l"},
                                                 "north": {"uv": [9, 11, 15, 12], "texture": "#l"},
                                                 "south": {"uv": [9, 11, 15, 12], "texture": "#l"}}),
                 el([11, 0.5, 3.5], [11.5, 1.5, 12.5], {"east": {"uv": [0, 9, 9, 10], "texture": "#l"},
                                                        **faces("#l", borda, ("north", "south"))}),
                 el([4.5, 1.5, 3.5], [11.5, 2, 12.5], {"up": {"uv": [0, 0, 7, 9], "texture": "#l", "rotation": 180},
                                                      "down": {"uv": [7, 0, 14, 9], "texture": "#l"},
                                                      **faces("#l", borda, ("north", "south", "east", "west"))}),
             ]}
    w(f"{A}/models/block/itens_livro_1857.json", livro)

    # ---------------- mesa_mediunica: pernas, toalha branca com barrado, papel, lápis e jarra
    m = []
    for (x, z) in [(1, 1), (13, 1), (1, 13), (13, 13)]:
        m.append(el([x, 0, z], [x + 2, 13.5, z + 2], {**faces("#madeira", [0, 0, 2, 13.5], ("north", "south", "east", "west")),
                                                     "down": {"uv": [0, 0, 2, 2], "texture": "#madeira", "cullface": "down"}}))
    m.append(el([0, 13.5, 0], [16, 14, 16], {"up": {"uv": [0, 0, 16, 16], "texture": "#toalha"},
                                             "down": {"uv": [0, 0, 16, 16], "texture": "#toalha"},
                                             **faces("#toalha", [0, 0, 16, 0.5], ("north", "south", "east", "west"))}))

    def saia_ns():
        return {"north": {"uv": [0, 12.5, 16, 16], "texture": "#toalha"}, "south": {"uv": [0, 12.5, 16, 16], "texture": "#toalha"},
                "down": {"uv": [0, 15, 16, 15.5], "texture": "#toalha"},
                "east": {"uv": [0, 12.5, 0.5, 16], "texture": "#toalha"}, "west": {"uv": [0, 12.5, 0.5, 16], "texture": "#toalha"}}

    def saia_ew():
        return {"east": {"uv": [0.5, 12.5, 15.5, 16], "texture": "#toalha"}, "west": {"uv": [0.5, 12.5, 15.5, 16], "texture": "#toalha"},
                "down": {"uv": [0, 15, 0.5, 15.5], "texture": "#toalha"}}

    m += [el([0, 10, 0], [16, 13.5, 0.5], saia_ns()), el([0, 10, 15.5], [16, 13.5, 16], saia_ns()),
          el([0, 10, 0.5], [0.5, 13.5, 15.5], saia_ew()), el([15.5, 10, 0.5], [16, 13.5, 15.5], saia_ew())]
    m.append(el([3, 14, 5], [8, 14.05, 11], {"up": {"uv": [1, 2, 6, 8], "texture": "#papel"}},
                {"angle": 22.5, "axis": "y", "origin": [5.5, 14, 8]}))
    m.append(el([8.5, 14, 6], [9, 14.5, 11], {"up": {"uv": [0, 0, 15, 0.5], "texture": "#lapis", "rotation": 90},
                                              "east": {"uv": [0, 0, 15, 0.5], "texture": "#lapis"},
                                              "west": {"uv": [0, 0, 15, 0.5], "texture": "#lapis"},
                                              "north": {"uv": [15, 0, 16, 1], "texture": "#lapis"},
                                              "south": {"uv": [0, 0, 1, 1], "texture": "#lapis"}},
                {"angle": -22.5, "axis": "y", "origin": [8.75, 14, 8.5]}))
    m.append(el([11.25, 14, 10.25], [13.75, 17, 12.75], faces("#agua", [0, 0, 16, 16])))
    m.append(el([11, 14, 10], [14, 18, 13], faces("#vidro", [0, 0, 16, 16])))
    m.append(el([14, 15, 11], [14.75, 17.5, 12], faces("#vidro", [0, 0, 2, 2])))
    w(f"{A}/models/block/itens_mesa_mediunica.json", {"parent": "block/block",
                                                      "textures": {"particle": T("toalha"), "madeira": T("madeira_mesa"), "toalha": T("toalha"),
                                                                   "papel": T("papel"), "lapis": T("lapis"),
                                                                   "vidro": translucida("vidro_jarra"), "agua": translucida("agua_jarra")},
                                                      "elements": m})

    # ---------------- tribuna: quem fala fica do lado norte (lado de quem colocou, como no atril); plateia ao sul
    rot_tampo = {"angle": -22.5, "axis": "x", "origin": [8, 13, 9]}
    t = [
        el([2, 0, 3], [14, 1, 14], {**faces("#madeira", [0, 0, 12, 1], ("north", "south", "east", "west")),
                                    "up": {"uv": [2, 2, 14, 13], "texture": "#madeira"},
                                    "down": {"uv": [2, 2, 14, 13], "texture": "#madeira", "cullface": "down"}}),
        el([3, 1, 5], [13, 12, 13], {"south": {"uv": [3, 2, 13, 13], "texture": "#frente"}, "north": {"uv": [3, 2, 13, 13], "texture": "#madeira"},
                                     "east": {"uv": [3, 2, 11, 13], "texture": "#madeira"}, "west": {"uv": [3, 2, 11, 13], "texture": "#madeira"}}),
        el([1, 12, 3], [15, 14, 16], {**faces("#madeira", [1, 0, 15, 2], ("north", "south")), **faces("#madeira", [3, 0, 16, 2], ("east", "west")),
                                      "up": {"uv": [1, 3, 15, 16], "texture": "#madeira"}, "down": {"uv": [1, 3, 15, 16], "texture": "#madeira"}}, rot_tampo),
        el([3, 14, 5], [13, 14.3, 14], {"up": {"uv": [0, 0, 16, 12], "texture": "#livro", "rotation": 180},
                                        **faces("#livro", [0, 12, 16, 13], ("north", "south", "east", "west"))}, rot_tampo),
    ]
    w(f"{A}/models/block/itens_tribuna.json", {"parent": "block/block",
                                               "textures": {"particle": T("madeira_tribuna"), "madeira": T("madeira_tribuna"),
                                                            "frente": T("tribuna_frente"), "livro": T("livro_aberto")},
                                               "elements": t})

    # ---------------- jarra_agua: corpo de vidro, água dentro, gargalo, bico e alça
    j = [
        el([5.5, 0.5, 5.5], [10.5, 8, 10.5], faces("#agua", [0, 0, 16, 16])),
        el([5, 0, 5], [11, 9, 11], {**faces("#vidro", [0, 0, 16, 16], ("north", "south", "east", "west", "up")),
                                    "down": {"uv": [0, 0, 16, 16], "texture": "#vidro", "cullface": "down"}}),
        el([6, 9, 6], [10, 11, 10], faces("#vidro", [0, 0, 16, 16], ("north", "south", "east", "west", "up"))),
        el([4, 8.5, 7], [5, 10.5, 9], faces("#vidro", [0, 0, 4, 4])),
        el([11, 7, 7.5], [13, 8, 8.5], faces("#vidro", [0, 0, 4, 2])),
        el([12, 3, 7.5], [13, 7, 8.5], faces("#vidro", [0, 0, 2, 8])),
        el([11, 2, 7.5], [13, 3, 8.5], faces("#vidro", [0, 0, 4, 2])),
    ]
    w(f"{A}/models/block/itens_jarra_agua.json", {"parent": "block/block",
                                                  "display": {**DISPLAY_PEQUENO, "gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.9, 0.9, 0.9]}},
                                                  "textures": {"particle": T("vidro_jarra"), "vidro": translucida("vidro_jarra"), "agua": translucida("agua_jarra")},
                                                  "elements": j})


BLOCOS = ["livro_1857", "mesa_mediunica", "tribuna", "jarra_agua"]
OBRAS = ["o_livro_dos_espiritos", "o_livro_dos_mediuns", "o_evangelho_segundo_o_espiritismo", "o_ceu_e_o_inferno", "a_genese", "nosso_lar",
         "o_que_e_o_espiritismo", "revista_espirita"]
ITENS = OBRAS + ["caderno_psicografia", "sopa_fraterna", "agasalho", "cesta_basica"]


def registros():
    for b in BLOCOS:
        mod = f"{NS}:block/itens_{b}"
        w(f"{A}/blockstates/{b}.json", {"variants": {
            "facing=north": {"model": mod}, "facing=east": {"model": mod, "y": 90},
            "facing=south": {"model": mod, "y": 180}, "facing=west": {"model": mod, "y": 270}}})
        w(f"{A}/items/{b}.json", {"model": {"type": "minecraft:model", "model": mod}})
        w(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/{b}",
                                              "pools": [{"rolls": 1.0, "conditions": [{"condition": "minecraft:survives_explosion"}],
                                                         "entries": [{"type": "minecraft:item", "name": f"{NS}:{b}"}]}]})
    for i in ITENS:
        w(f"{A}/models/item/itens_{i}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/itens/{i}"}})
        w(f"{A}/items/{i}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/itens_{i}"}})
    # o atril vanilla aceita as obras e o caderno
    w(f"{R}/data/minecraft/tags/item/lectern_books.json", {"replace": False, "values": [f"{NS}:{i}" for i in OBRAS + ["caderno_psicografia"]]})


RECEITAS = []


def sl(nome, ingr, cat="misc"):
    RECEITAS.append(nome)
    w(f"{D}/recipe/itens_{nome}.json", {"type": "minecraft:crafting_shapeless", "category": cat, "ingredients": ingr, "result": {"id": f"{NS}:{nome}"}})


def sh(nome, pat, key, cat="building"):
    RECEITAS.append(nome)
    w(f"{D}/recipe/itens_{nome}.json", {"type": "minecraft:crafting_shaped", "category": cat, "key": key, "pattern": pat, "result": {"id": f"{NS}:{nome}"}})


def receitas():
    B = "minecraft:book"
    sl("livro_1857", [B, "minecraft:paper", "minecraft:brown_dye"], cat="building")
    sl("o_livro_dos_espiritos", [B, "minecraft:white_dye", "minecraft:ink_sac"])  # capa clara de 1857
    sl("o_livro_dos_mediuns", [B, "minecraft:feather", "minecraft:paper"])  # pena e papel: psicografia
    sl("o_evangelho_segundo_o_espiritismo", [B, "minecraft:bread", "minecraft:blue_dye"])  # o pão partilhado: caridade
    sl("o_ceu_e_o_inferno", [B, "minecraft:black_dye", "minecraft:gold_nugget"])
    sl("a_genese", [B, "minecraft:compass", "minecraft:green_dye"])  # ciência e a Terra
    sl("nosso_lar", [B, "minecraft:light_blue_dye", "minecraft:paper"])
    sl("o_que_e_o_espiritismo", [B, "minecraft:orange_dye"])
    sl("revista_espirita", ["minecraft:paper", "minecraft:paper", "minecraft:paper", "minecraft:ink_sac"])
    sl("caderno_psicografia", ["minecraft:paper", "minecraft:paper", "minecraft:string", "minecraft:blue_dye"])
    sl("sopa_fraterna", ["minecraft:bowl", "minecraft:carrot", "minecraft:potato", "minecraft:beetroot"])
    sl("cesta_basica", ["minecraft:bread", "minecraft:potato", "minecraft:carrot", "minecraft:apple"])
    sh("agasalho", ["W W", "WWW", "WWW"], {"W": "#minecraft:wool"}, cat="misc")
    sh("mesa_mediunica", ["CCC", "PPP", "S S"], {"C": "minecraft:white_carpet", "P": "#minecraft:planks", "S": "minecraft:stick"})
    sh("tribuna", ["SSS", " B ", " P "], {"S": "#minecraft:wooden_slabs", "B": B, "P": "#minecraft:planks"})
    sh("jarra_agua", ["G G", "GWG", "GGG"], {"G": "minecraft:glass", "W": "minecraft:water_bucket"})


if __name__ == "__main__":
    modelos()
    registros()
    receitas()
    print("ok:", len(RECEITAS), "receitas")
