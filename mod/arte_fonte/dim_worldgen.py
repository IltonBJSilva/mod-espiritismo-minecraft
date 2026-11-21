"""Gera os JSON de worldgen das dimensões Nosso Lar e Umbral (noise leve, bioma fixo).
Uso: python arte_fonte/dim_worldgen.py  (a partir da raiz da cópia)"""
import json, os

BASE = "src/main/resources/data/mod_espiritismo_minecraft/worldgen"

def bloco(nome, **props):
    d = {"Name": nome}
    if props:
        d["Properties"] = props
    return {"type": "minecraft:block", "result_state": d}

def cond(se, entao):
    return {"type": "minecraft:condition", "if_true": se, "then_run": entao}

def seq(*itens):
    return {"type": "minecraft:sequence", "sequence": list(itens)}

CHAO = {"type": "minecraft:stone_depth", "add_surface_depth": False, "offset": 0, "secondary_depth_range": 0, "surface_type": "floor"}
SUBSOLO = {"type": "minecraft:stone_depth", "add_surface_depth": True, "offset": 0, "secondary_depth_range": 0, "surface_type": "floor"}
SECO = {"type": "minecraft:water", "add_stone_depth": False, "offset": 0, "surface_depth_multiplier": 0}
BEDROCK = cond({"type": "minecraft:vertical_gradient", "false_at_and_above": {"above_bottom": 5},
                "random_name": "minecraft:bedrock_floor", "true_at_and_below": {"above_bottom": 0}}, bloco("minecraft:bedrock"))

def ruido(lim_min, lim_max):
    return {"type": "minecraft:noise_threshold", "noise": "minecraft:surface", "min_threshold": lim_min, "max_threshold": lim_max}

def roteador(altura_zero, amplitude, escala):
    # densidade = gradiente vertical (zero em altura_zero) + ruído 2D suave -> colinas baixas
    dens = {"type": "minecraft:interpolated", "argument": {"type": "minecraft:add",
        "argument1": {"type": "minecraft:y_clamped_gradient", "from_y": altura_zero - 30, "to_y": altura_zero + 30,
                      "from_value": 1.0, "to_value": -1.0},
        "argument2": {"type": "minecraft:mul", "argument1": amplitude,
                      "argument2": {"type": "minecraft:noise", "noise": "minecraft:surface", "xz_scale": escala, "y_scale": 0.0}}}}
    r = {k: 0.0 for k in ["barrier", "continents", "depth", "erosion", "fluid_level_floodedness", "fluid_level_spread", "lava",
                          "preliminary_surface_level", "ridges", "temperature", "vegetation", "vein_gap", "vein_ridged", "vein_toggle"]}
    r["final_density"] = dens
    return r

def settings(padrao, regra, altura_zero, amplitude, escala, nivel_mar):
    return {
        "aquifers_enabled": False,
        "default_block": {"Name": padrao},
        "default_fluid": {"Name": "minecraft:water", "Properties": {"level": "0"}},
        "disable_mob_generation": True,
        "legacy_random_source": False,
        "noise": {"height": 128, "min_y": 0, "size_horizontal": 1, "size_vertical": 2},
        "noise_router": roteador(altura_zero, amplitude, escala),
        "ore_veins_enabled": False,
        "sea_level": nivel_mar,
        "spawn_target": [],
        "surface_rule": regra,
    }

nosso_lar_regra = seq(
    BEDROCK,
    cond(CHAO, seq(cond(SECO, bloco("minecraft:grass_block", snowy="false")), bloco("minecraft:sand"))),
    cond(SUBSOLO, seq(cond(SECO, bloco("minecraft:dirt")), bloco("minecraft:sand"))),
)
umbral_regra = seq(
    BEDROCK,
    cond(CHAO, seq(cond(ruido(0.25, 9.0), bloco("minecraft:soul_soil")),
                   cond(ruido(-9.0, -0.35), bloco("minecraft:coarse_dirt")),
                   bloco("minecraft:mud"))),
    cond(SUBSOLO, bloco("minecraft:packed_mud")),
)

VAZIO = {k: [] for k in ["ambient", "axolotls", "creature", "misc", "monster", "underground_water_creature", "water_ambient", "water_creature"]}

def features(vegetacao):
    f = [[] for _ in range(11)]
    f[9] = vegetacao
    return f

nosso_lar_bioma = {
    "attributes": {
        "minecraft:visual/sky_color": "#8cc6ff",
        "minecraft:visual/fog_color": "#dcecff",
        "minecraft:visual/water_fog_color": "#4fb8f0",
        "minecraft:visual/ambient_particles": [{"particle": {"type": "minecraft:white_ash"}, "probability": 0.0006}],
    },
    "carvers": [],
    "downfall": 0.5,
    "effects": {"water_color": "#3fc6ff", "grass_color": "#6fd468", "foliage_color": "#5cc94a"},
    "features": features(["minecraft:trees_meadow", "minecraft:trees_plains", "minecraft:flower_meadow", "minecraft:wildflowers_meadow",
                          "minecraft:flower_plains", "minecraft:patch_grass_meadow", "minecraft:patch_sunflower",
                          "minecraft:patch_sugar_cane", "minecraft:patch_waterlily"]),
    "has_precipitation": False,
    "spawn_costs": {},
    "spawners": VAZIO,
    "temperature": 0.7,
}
umbral_bioma = {
    "attributes": {
        "minecraft:visual/sky_color": "#100e0e",
        "minecraft:visual/fog_color": "#24201f",
        "minecraft:visual/water_fog_color": "#2a2618",
        "minecraft:visual/water_fog_end_distance": 12.0,
        "minecraft:visual/ambient_particles": [{"particle": {"type": "minecraft:ash"}, "probability": 0.004}],
    },
    "carvers": [],
    "downfall": 0.0,
    "effects": {"water_color": "#4a4632", "grass_color": "#5a5640", "foliage_color": "#4e4a36", "dry_foliage_color": "#3e3528"},
    "features": features(["minecraft:patch_dead_bush", "minecraft:patch_dead_bush_2"]),
    "has_precipitation": False,
    "spawn_costs": {},
    "spawners": VAZIO,
    "temperature": 0.6,
}

def grava(caminho, dado):
    os.makedirs(os.path.dirname(caminho), exist_ok=True)
    with open(caminho, "w", encoding="utf-8") as f:
        json.dump(dado, f, indent=2, ensure_ascii=False)
        f.write("\n")

grava(f"{BASE}/noise_settings/nosso_lar.json", settings("minecraft:stone", nosso_lar_regra, 68, 0.2, 0.5, 62))
grava(f"{BASE}/noise_settings/umbral.json", settings("minecraft:tuff", umbral_regra, 64, 0.3, 0.7, 62))
grava(f"{BASE}/biome/nosso_lar.json", nosso_lar_bioma)
grava(f"{BASE}/biome/umbral.json", umbral_bioma)
print("ok")
