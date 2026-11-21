# Umbral — level design (área "umbral")

Representação lúdica inspirada na descrição de André Luiz em *Nosso Lar* (caps. 1–2: região de sofrimento e
névoa, onde Espíritos recém-desencarnados se demoram conforme o próprio estado íntimo, e onde trabalham equipes
de socorro). Os Postos de Socorro, seus papéis e a disposição espacial são **adaptação criativa para o jogo**,
não descrição literal da obra.

## 1. Ambiente
- Dimensão `mod_espiritismo_minecraft:umbral` (data-driven): tufo e lama, mar de água em y = 62 (pântano),
  luz ambiente baixa, névoa a 2–40 blocos, sem monstros naturais, camas não funcionam (o repouso é feito pelos
  leitos dos postos, por clique).
- Perturbação do jogador (ver ARQUITETURA) acrescenta névoa leve, sussurros e seta oscilante, sempre ajustáveis.

## 2. Postos de Socorro (7 por mundo)

Planejados por `ConstrucoesUmbral.planejar(seed)` — determinístico pela seed do mundo:

| Índice | Posição | Tamanho | Observação |
|---|---|---|---|
| 0 | origem (0, 0) | central, 9×9 | garantido na primeira chegada; pátio de chegada dos recém-despertos 30 blocos ao sul |
| 1, 4 | anel ~170–210 blocos | pequeno 9×9 | ângulos separados por 180° |
| 2, 5 | anel ~280–320 | pequeno 9×9 | |
| 3, 6 | anel ~390–430 | **avançado** 15×11 | mais longe; exige rota (marcos) ou acompanhamento |

Giro inicial aleatório pela seed, ângulos a cada 60° com ±12°. Distância mínima entre postos ≥ 120 (testado).

Cada posto (planta em `plano(...)`):
- **Farol**: sinalizador sobre base 3×3 de ferro no telhado com vidro branco — feixe de luz visível de longe
  através da névoa; postes de lanterna na entrada.
- **Triagem e acolhimento**: perto da porta (placa + "Socorrista — triagem e acolhimento"). Clicar num
  socorrista de posto estando perturbado = buscar ajuda por conta própria → acolhimento (alivia 15).
- **Leitos** na parede norte (placa "clique na cama"): repouso (−20 perturbação, +1 arrependimento, 2 min).
  "Socorrista — enfermagem".
- **Ponto de conversa e estudo**: atril + estantes + placas educativas (paráfrases com referência: ESE cap.
  XXVII, ESE cap. XV, LE q. 118). Clique: conversa (−10, +1 arrependimento, 1 min); se
  `saidaAssistidaLiberada`, oferece a **saída assistida** para Nosso Lar (confirmar com segundo clique).
  "Socorrista — orientação".
- Caldeirão com água; luz interna suave (lanternas penduradas).
- **Ponto de chegada dos resgates e de saída assistida**: pátio de calcita com lanterna do mar no centro,
  5 blocos à frente da porta (ponto `posto_<i>_chegada`).
- Espíritos perdidos na rota: um sofredor perto de cada posto externo; dois perto do pátio do central.

Pontos nomeados por posto (em `plano_pontos`): `posto_<i>`, `posto_<i>_porta`, `posto_<i>_chegada`,
`posto_<i>_saida`, `posto_<i>_triagem`, `posto_<i>_conversa`, `posto_<i>_leito`; o central mantém os nomes
antigos `posto`, `posto_porta` e `chegada`.

### Escolha do local
Ao ser iniciado, cada posto verifica o planejado e até 8 deslocamentos (12–16 blocos) entre posições já
carregadas, preferindo chão seco acima do nível do mar; se não houver, faz um aterro (colunas de barro
compactado até 8 blocos abaixo, substituindo água). Nunca em caverna (usa o heightmap da superfície).

### Construção incremental
- Um posto externo só começa quando os quatro cantos da área estão carregados (algum jogador chegou perto) —
  nenhum chunk é gerado para isso.
- `BLOCOS_POR_TICK = 256` no total do servidor; ~1.000–2.500 operações por posto (4–10 ticks).
- Se o chunk descarregar no meio, a obra pausa; após reinício, `retomar` reconstrói a fila do índice salvo.
- Executar de novo uma operação já feita não altera nada (idempotente).

## 3. Rotas e marcos de orientação
Entre o Posto central e cada posto externo, um marco (barro + 2 cercas + lanterna) a cada 48 blocos, colocado
quando o chunk é carregado. São os "pontos de orientação raros" da travessia.

## 4. Riscos
- Perturbação sobe longe dos postos (moral baixa e médiuns mais rápido); acima de 50 vira
  NO_UMBRAL_PERTURBADO (sem violência; ecos e vozes).
- Recusar o socorro deixa o jogador mais exposto (+5, teto 90) por um tempo — sem loop: os socorristas só
  voltam por iniciativa própria após 5 min, e o jogador pode chamá-los antes.
- Sair de Nosso Lar sem acompanhamento pode terminar no Umbral (ver ARQUITETURA §7).

## 5. Rotas de resgate (fluxo)
1. Perturbado fora de um posto → socorristas surgem e se aproximam devagar.
2. Explicam e oferecem: Aceitar / Conversar / Pedir tempo / Recusar.
3. Aceitar → 5 s acompanhado → pátio do posto mais próximo → acolhimento (30 s) → orientado.
4. No posto: repouso, conversa, estudo, prece → saída assistida para as Câmaras de Retificação de Nosso Lar.

## 6. Comandos de depuração (operadores)
`/espiritismo_plano postos` (estado de cada posto), `estado [novo]`, `perturbacao <0-100>`,
`limpar_temporarios`, `ir umbral|nosso_lar|terra`, `resgatar`, `reencarnar`, `sonho ...`.
Jogadores: `/espiritismo_socorro [pedir|aceitar|conversar|tempo|recusar]`, `/espiritismo_prece`.

## 7. Precisa de validação visual (não feita: sem cliente gráfico)
Feixe do farol através da névoa do Umbral; aparência dos postos sobre lama/água; leitura das placas;
aproximação dos socorristas (interpolação); névoa/sussurros/FOV nos três modos; tela de consentimento.
