# Nosso Lar — level design (área "colonia")

Código: `espiritismo/colonia/` (traçado em `LayoutColonia`, peças em `Pecas`, ordem da obra em `PlanoColonia`,
construção incremental em `Obra`, entrada em `ConstrucaoColonia`, aeróbus em `Aerobus`) e `espiritismo/protecao/`.

## 1. Fontes e o que é do livro × do jogo

| Elemento | Fonte | Uso no jogo |
|---|---|---|
| Governadoria no centro de uma praça, "ponto de convergência dos seis ministérios", que dela partem "em forma triangular" | *Nosso Lar*, cap. 8 "Organização de serviços" (André Luiz / F. C. Xavier, FEB, 1944) — conferido em https://search.nepebrasil.org/book-part/?book=53&id=4171 | Cidade circular: praça e palácio no centro, seis setores em cunha (60° cada), um por Ministério |
| Os seis Ministérios: Regeneração, Auxílio, Comunicação, Esclarecimento, Elevação, União Divina | idem, cap. 8 | Um prédio por setor, a 66 blocos do centro |
| Câmaras de Retificação (onde o recém-chegado se refaz) no Ministério da Regeneração | livro (caps. iniciais e de Narcisa); já usado pelo mod | Ponto `chegada` / `camaras_retificacao` |
| Aeróbus: "carro aéreo", "suspenso do solo a uma altura de cinco metros mais ou menos", com "paradas de três em três quilômetros"; leva ao Bosque das Águas | *Nosso Lar*, cap. 10 "No Bosque das Águas" — https://search.nepebrasil.org/book-part/?book=53&id=4173 ; resumo em https://textosparareflexao.blogspot.com/2010/03/passeios-de-aerobus.html | Estações com atril de embarque e um carro decorativo suspenso a ~5 blocos; viagem curta com a vista velada |
| Bosque das Águas, Rio Azul, grande reservatório; distribuição das águas é tarefa do Ministério da União Divina | idem, cap. 10 | Lago, bosque denso e Reservatório no setor da União Divina |
| Muralha, portões, torres, torre de luz central, avenidas radiais e anéis | Filme *Nosso Lar* (2010) e imagens de referência (`referencias/`) — o livro **não** descreve muralhas | Muralha circular (r=156) com 4 portões e 12 torres; farol (beacon) azul-claro no alto da Governadoria |
| Hospital, Biblioteca, Escola, bairros residenciais | Ambientes de vida comunitária sugeridos pelo PROMPT_MESTRE §3.4; o livro cita parques hospitalares e famílias em suas casas | Prédios funcionais (leitos, estantes, quadro e bancos, casas com porta) |

Textos de placas são orientações do jogo ou paráfrases; nenhuma frase do livro é atribuída entre aspas no jogo além das
indicações de capítulo.

## 2. Mapa lógico (relativo ao centro da praça, y = chão da cidade)

```
                         N (portão norte, avenida 270°)
                 Comunicação (300°)    |    Regeneração (240°)
                          \   ___ muralha r=156 ___   /
              bairros      \ /    anel 3 r=146    \ /   bairros
                            X      anel 2 r=96     X        Hospital (197°, r=120)
   Esclarecimento (0°)  ----+---  anel 1 r=36  ---+----  Auxílio (180°)   <- portão oeste
   Biblioteca (344°) e      |    PRAÇA r=27       |       (atril da reencarnação)
   Escola (16°), portão L   |  GOVERNADORIA+farol |
                            X                     X
                           / \                   / \
              Elevação (60°)  Mirante (60°,r=120)   União Divina (120°): Bosque das Águas,
                         \                           lago, Reservatório (138°)
                                 S (portão sul, avenida 90°)
```

* Ângulos no plano do Minecraft: 0° = leste (+X), 90° = sul (+Z), 180° = oeste, 270° = norte.
* **Avenidas radiais** (5 de largura, calcita com borda de diorito) nas divisas dos setores: 30°, 90°, 150°, 210°, 270°, 330°.
  Eixos leste/oeste (0°/180°) do anel 2 até os portões. **Anéis** em r = 36, 96 e 146.
* **Ministérios** em r = 66, no eixo do setor, com a porta virada para o centro e acesso pavimentado até o anel 1.
* **Estações do aeróbus**: central (45°, r=31, junto à praça), uma diante de cada Ministério (eixo+11°, r=46) e a do
  Bosque das Águas (108°, r=100) — 8 ao todo.
* **Bairros residenciais**: dezenas de casas (pelo menos 40, verificado no GameTest) (7×7, quatro variantes) em três anéis (r = 106, 122, 138), fora de avenidas,
  eixos, áreas reservadas e do setor do Bosque.
* **Muralha**: r = 156 (3 blocos de espessura, 9 de altura, ameias, faixa de vidro azul), portões (7 de largura, 7 de
  altura) a 0°/90°/180°/270° com duas torres cada, e mais 4 torres a 45°/135°/225°/315°. Estradas de terra batida saem
  dos portões e descem o talude.
* **Terreno**: plano até r = 164; talude suave (smoothstep) de r = 164 a 196 até a altura natural do gerador; faixa
  204 só fixa folhas (árvores cortadas pela metade não secam). Sem "paredes de terra".
* **Paisagismo**: árvores (carvalho, bétula, cerejeira) nas faixas entre anéis, bosque denso na União Divina, ~9 mil
  flores e gramíneas (testado), vitórias-régias no lago, canteiros e espelhos d'água na praça, postes de luz ao longo das
  avenidas e anéis.

### Pontos nomeados (`ColoniaApi.local`, gravados em `Planos.pontos` = attachment `plano_pontos`)

`governadoria`, `praca` (o sonho de desdobramento chega em `praca.south(5)`, mantido livre), `regeneracao`,
`camaras_retificacao` = `chegada`, `auxilio` (posição do ATRIL — usada por `EventosDimensao`), `auxilio_entrada`,
`comunicacao`, `esclarecimento`, `elevacao`, `uniao_divina`, `hospital`, `biblioteca`, `escola`, `reservatorio`,
`bosque`, `residencias`, `estacao_aerobus`, `estacao_<setor>`, `estacao_bosque`, `portao_norte|sul|leste|oeste`,
`mirante` (atril), `mirante_npc`, `mirante_entrada`, `centro`, `colonia_versao`.
Todos os pontos de chão ficam livres (ar em y e y+1, chão embaixo) — verificado no GameTest.

## 3. Fluxos e rotas

1. **Desencarne / resgate / comando** → `Planos.garantir` → `ConstrucoesPlanos.construirNossoLar` →
   `ConstrucaoColonia.construir`: em Nosso Lar só a zona da chegada (Câmaras de Retificação + 6 blocos de margem) é feita
   na hora (~0,2–1,5 s, contra 6,7 s da versão antiga); todos os pontos já são devolvidos; o resto entra na obra.
2. **Obra incremental** (`Obra`, tick do servidor): 1º Auxílio e Mirante; 2º terreno chunk a chunk, do mais perto da
   chegada para o mais longe; 3º estruturas; 4º paisagismo. Orçamento de 6 ms por tick; chunks pedidos em segundo
   plano (ticket próprio `colonia_obra`, sem expiração, liberado ao fim de cada peça, mantém a dimensão ativa); o índice
   da peça é salvo no Level (`colonia_obra`) e a obra é retomada ao reabrir o mundo. Mensagem discreta de progresso a
   cada 30 s para quem está em Nosso Lar. `/colonia status` (op) mostra o andamento.
3. **Recém-chegado**: acorda nas Câmaras (setor norte-noroeste), sai pela porta sul até o anel 1, segue para a praça
   (Governadoria) ou pega o aeróbus na estação ao lado.
4. **Reencarnar**: Ministério do Auxílio, a oeste da praça (atril no fundo do salão) — as mensagens de `Planos`
   continuam corretas.
5. **Aeróbus**: clicar no atril de uma estação mostra os destinos clicáveis (`/aerobus <destino>`, aberto a todos, só
   funciona perto de uma estação e para estações já construídas). A viagem dura ~2,5 s com a vista velada e termina na
   plataforma de destino. Áreas como bairros, bosques, Mirante e muralha continuam sendo descobertas a pé.
6. **Saída**: portões nos 4 pontos cardeais com estradas que descem o talude (a transição ao Umbral é da área "umbral").
7. **Sentar**: clique com a mão vazia num banco (escada) em Nosso Lar.

## 4. Proteção (resumo; código em `protecao/`)

Toda a dimensão Nosso Lar é protegida (`ColoniaApi.protegido`). Jogador comum não quebra, não coloca, não substitui, não
usa ferramentas/baldes/isqueiro/farinha de osso, não pisoteia plantação; explosões não removem blocos; fogo e lava são
apagados no fim do tick em que aparecem; fluidos não criam pedra; pistões não se movem; mobs não mexem em blocos
(`EntityMobGriefingEvent`, `LivingDestroyBlockEvent`); molduras e suportes de armadura não são atacados. Permitido:
portas, alçapões, portões, botões, sinos, atris de mão vazia, placas, mesa de trabalho, sentar, conversar, blocos do
próprio mod. Operador em modo criativo pode manter (cada quebra/colocação vai para o log). Mensagens gentis e
variadas na barra de ação, no máximo uma a cada 2 s por jogador.

## 5. O que ainda precisa de validação visual (sem GUI aqui)

Proporções e cores dos prédios, leitura das placas, o feixe do farol, as torres/ameias, a pose ao sentar
(assento `block_display` a +0,5 bloco), o carro do aeróbus suspenso, a suavidade do talude em terreno real de Nosso Lar
(os testes rodam em mundo plano) e o tempo total da obra numa máquina comum. Roteiro: mundo novo → `/espiritismo_plano ir
nosso_lar` → observar a chegada; `/colonia status`; `/colonia local governadoria` (e os demais nomes); andar até os
portões; clicar no atril de uma estação.
