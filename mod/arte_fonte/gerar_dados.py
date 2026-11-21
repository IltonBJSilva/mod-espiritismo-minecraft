import json, os, sys
raiz = sys.argv[1]  # src/main/resources/data/mod_espiritismo_minecraft
M = "mod_espiritismo_minecraft:"
adv = os.path.join(raiz, "advancement", "jornada")
os.makedirs(adv, exist_ok=True)

# id: (pai, ícone, título, descrição, frame, oculta)
CONQUISTAS = {
    "raiz": (None, M + "livrodos_espiritos", "Jornada Espiritual", "Visite o Centro Espírita e fale com o Dirigente", "task", False),
    "primeira_leitura": ("raiz", M + "livrodos_espiritos", "Que é Deus?", "Leia O Livro dos Espíritos pela primeira vez", "task", False),
    "agua": ("primeira_leitura", M + "agua_fluidificada", "Fluidos de Paz", "Receba o passe e a água fluidificada no Centro", "task", False),
    "caridade": ("agua", "minecraft:bread", "Fora da Caridade Não Há Salvação", "Sacie a fome de um aldeão necessitado", "task", False),
    "caminho": ("caridade", "minecraft:lantern", "Um Caminho de Serviço", "Escolha sua classe espiritual com o Dirigente", "goal", False),
    "habilidade": ("caminho", "minecraft:amethyst_shard", "Primeiro Serviço", "Use a habilidade da sua classe (tecla V)", "task", False),
    "palestra": ("raiz", "minecraft:lectern", "Leis Morais", "Ouça uma palestra inteira sobre as leis morais", "task", False),
    "elevado": ("caminho", "minecraft:end_rod", "Espírito Elevado", "Alcance moralidade elevada (80 ou mais)", "challenge", False),
    "codificador": ("caminho", M + "o_livro_dos_espiritos", "O Codificador", "Encontre a casa de Allan Kardec (use a Bússola Espiritual agachado)", "goal", False),
    "uberaba": ("caminho", M + "nosso_lar", "O Mineiro de Uberaba", "Encontre a casa de Chico Xavier", "goal", False),
    "mansao": ("caminho", M + "sopa_fraterna", "A Mansão do Caminho", "Visite a Mansão do Caminho e conheça Divaldo Franco", "goal", False),
    "nosso_lar": ("raiz", "minecraft:white_tulip", "Nosso Lar", "Desperte na colônia espiritual depois de uma vida de bem", "challenge", False),
    "resgate": ("raiz", "minecraft:lantern", "Socorro no Umbral", "Seja resgatado do Umbral pelo arrependimento sincero", "goal", False),
    "reencarnacao": ("raiz", "minecraft:oak_sapling", "Nascer, Morrer, Renascer", "Reencarne a partir do plano espiritual", "goal", False),
    "nl_recepcao": ("raiz", "minecraft:white_tulip", "Recebido em Nosso Lar", "Fale com quem recebe os recém-chegados", "task", False),
    "nl_acolhimento": ("nl_recepcao", "minecraft:white_bed", "Acolhimento", "Entenda como funciona o tratamento na colônia", "task", False),
    "nl_ministerios": ("nl_acolhimento", "minecraft:quartz_pillar", "Os seis Ministérios", "Conheça os Ministérios de Nosso Lar", "task", False),
    "nl_biblioteca": ("nl_ministerios", "minecraft:bookshelf", "Na biblioteca", "Conheça a biblioteca ou estude uma obra na colônia", "task", False),
    "nl_mentor": ("nl_biblioteca", "minecraft:lantern", "Uma conversa com o mentor", "Converse com um mentor sobre o que viu", "task", False),
    "nl_escolha": ("nl_mentor", "minecraft:wooden_hoe", "Estudo ou trabalho", "Escolha como ocupar o seu tempo na colônia", "task", False),
    "nl_recuperacao": ("nl_escolha", "minecraft:oak_sapling", "Morador de Nosso Lar", "Compreenda que a recuperação leva tempo", "goal", False),
    "amelie_encontro": ("raiz", "minecraft:writable_book", "Amélie Boudet", "Conheça a companheira de trabalho de Allan Kardec", "task", False),
    "amelie_documentos": ("amelie_encontro", "minecraft:paper", "Papel bem guardado", "Ajude a copiar e arquivar a correspondência", "task", False),
    "amelie_metodo": ("amelie_documentos", "minecraft:book", "O método", "Estude como as comunicações eram examinadas", "task", False),
    "amelie_continuidade": ("amelie_metodo", "minecraft:lectern", "Continuidade", "Leve um livro a um Centro Espírita", "goal", False),
}

for nome, (pai, icone, titulo, desc, frame, oculta) in CONQUISTAS.items():
    d = {}
    if pai:
        d["parent"] = M + "jornada/" + pai
    display = {
        "icon": {"id": icone},
        "title": {"text": titulo},
        "description": {"text": desc},
        "frame": frame,
        "show_toast": True,
        "announce_to_chat": nome != "raiz",
        "hidden": oculta,
    }
    if not pai:
        display["background"] = "minecraft:block/calcite"
    d["display"] = display
    d["criteria"] = {"concedido": {"trigger": "minecraft:impossible"}}
    with open(os.path.join(adv, nome + ".json"), "w", encoding="utf-8") as f:
        json.dump(d, f, indent=2, ensure_ascii=False)

rec = os.path.join(raiz, "recipe")
os.makedirs(rec, exist_ok=True)
with open(os.path.join(rec, "livro_dos_espiritos.json"), "w", encoding="utf-8") as f:
    json.dump({"type": "minecraft:crafting_shapeless", "category": "misc",
               "ingredients": ["minecraft:book", "minecraft:feather", "minecraft:ink_sac"],
               "result": {"id": M + "livrodos_espiritos"}}, f, indent=2)
print("ok")
