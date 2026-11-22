# Umbral 2.0 — Mapa conceitual e regiões

Agente **pesquisa** (papel de world builder conceitual). Classificação: **[A]** Kardec · **[B]** obra mediúnica ·
**[C]** interpretação · **[D]** criação do jogo · **[D←B]** adaptação fundamentada. As fontes completas estão em
`UMBRAL_2_0_FONTES_E_FIDELIDADE.md` (FF).

> **Aviso de fidelidade [D]:** nenhuma obra traz um *mapa* do Umbral nem divide o Umbral em regiões com nomes.
> O mapa abaixo é **criação do jogo**. Ele organiza, em geografia jogável, ambientes e instituições que aparecem
> em relatos específicos. Só o **Vale** (de *Memórias de um Suicida*) e as **instituições** citadas têm nome de
> obra; tudo o mais leva rótulo de região fictícia.

## 1. Visão geral (integração com o que já existe)

Hoje o mod já tem: a dimensão `umbral` (pântano, névoa, sem monstros), sete Postos de Socorro em anéis (central
em 0,0; pequenos a cerca de 170–320 blocos; avançados a cerca de 390–430), marcos de rota a cada 48 blocos,
perturbação de 0 a 100, socorristas e saída assistida para as Câmaras de Retificação. O Umbral 2.0 **não
substitui** isso: transforma os anéis em **faixas** e acrescenta regiões em setores angulares.

```
                         (N)  Muralha / Portão de Nosso Lar (outra dimensão; acesso só assistido)
                                       │  rota do aeróbus NÃO existe no Umbral [B NL cap.33]
                                       ▼
   FAIXA 0  ── Campos de Chegada ── Posto Central (0,0) ── Pátio dos Recém-Despertos
   (r 0–150)        névoa leve, marcos frequentes, socorristas visíveis
                                       │
   FAIXA 1  ── Charcos da Indecisão (setores livres) ── Postos pequenos ── Ruínas do Costume
   (r 150–350)      “vale da indecisão / pântano dos erros” [imagens do NL cap.12 → D←B]
                    │                           │                           │
   FAIXA 2  ── Bosque dos Ecos (isolamento) ─ Postos avançados ─ Encruzilhada dos Rumores (obsessão)
   (r 350–600)      Abrigo Transitório móvel [D←B Obreiros cap.4]
                                       │
   FAIXA 3  ── Desfiladeiros de Cinza → VALE (piloto) → Colônia de Recuperação / Hospital Maria de Nazaré
   (r 600–1100, setor sul ~70°)        [B Memórias 1ª parte caps. I–III]
                                       │
   (fora de escopo) Trevas [B NL cap.44] — NÃO implementar; apenas mencionadas no painel educativo.
```

Raios em blocos a partir do Posto Central. Valores de referência [D]; o arquiteto ajusta.

## 2. Fichas de região

Cada ficha segue §26.2: identidade, justificativa, NPCs, perigos, entradas e saídas, amparo, postos, conteúdo
educativo e fonte.

---

### R0 — Campos de Chegada (faixa 0)
- **Fonte:** *Nosso Lar* caps. 1–2 (névoa cinzenta, caminhos ermos) [B]. Nome [D].
- **Identidade:** névoa leve, lama rasa, luz cinzenta “de sol distante”, vento baixo, lanternas dos marcos.
  Som: vento, passos na lama, um sino distante do Posto Central.
- **Justificativa:** é onde despertam os recém-desencarnados e onde o jogador aprende o sistema.
- **NPCs:** sofredores calmos (2–4), socorristas de posto, um orientador fixo no pátio.
- **Perigos:** só desorientação leve; necessidades sobem devagar.
- **Rotas:** marcos a cada 48 blocos (já existem) até os postos da faixa 1.
- **Amparo:** Posto Central a menos de 150 blocos; feixe de farol sempre visível.
- **Painel:** “Nas zonas inferiores” — *Nosso Lar*, caps. 1–3.

### R1 — Charcos da Indecisão (faixa 1, setores livres)
- **Fonte:** imagem do cap. 12 de *Nosso Lar*, em que o Umbral é a zona de quem fica no “vale da indecisão” e no
  “pântano dos erros” [B]. O **nome da região é criação** [D←B].
- **Identidade:** lamaçais largos, ilhotas, juncos secos, árvores mortas, névoa média. Som: borbulhar, coaxar
  abafado, vozes que discutem ao longe.
- **NPCs:** grupos pequenos que discutem entre si, viajantes que andam em círculos [D←B NL cap.44, “andar em
  curvas”].
- **Perigos:** lama que deixa o jogador mais lento (sem afogar), trilhas que somem na névoa, sede subindo mais
  rápido.
- **Rotas:** passarelas de troncos que ligam ilhotas; marcos de rota com lanterna.
- **Amparo:** dois postos pequenos já existentes; equipes de reconhecimento passam em ronda.
- **Painel:** “O Umbral segundo Lísias” — *Nosso Lar*, cap. 12 (paráfrase).

### R2 — Ruínas do Costume (faixa 1, região de isolamento C)
- **Fonte:** criação [D], inspirada no tema “presos a lembranças e hábitos” (§26.3 C) e no comentário do cap. 12
  de que os desesperados se demoram em “edificações mesquinhas” [B, imagem do texto].
- **Identidade:** casas e lojas em ruína, com fachadas de época variada; portas que dão para o mesmo pátio.
  Som: relógio que não bate, sineta de loja.
- **NPCs:** “o comerciante que espera o cliente”, “a mulher que arruma uma mesa para quem não vem”. Repetem
  falas por confusão (§26.3 C). São personagens fictícios.
- **Perigos:** **caminho circular**: a mesma praça três vezes seguidas aumenta a perturbação; a bússola perde
  precisão.
- **Saída:** uma rua com flores pálidas brotando entre as pedras (sinal discreto) leva a um ponto de observação de
  equipe.
- **Amparo:** orientador itinerante; o jogador pode **escutar** um NPC até o fim para desfazer o laço circular.
- **Painel:** “Hábitos que nos acompanham” — LE 257 [A] e *Nosso Lar* cap. 12 [B]; o mais é adaptação.

### R3 — Bosque dos Ecos (faixa 2, isolamento)
- **Fonte:** criação [D]; ambientação de neblina e vozes de *Nosso Lar* caps. 1–2 [B].
- **Identidade:** troncos negros muito próximos, neblina densa (visibilidade de 8–12 blocos), eco que repete a
  última fala do jogador ou de NPCs.
- **NPCs:** poucos; um eremita que não quer companhia (exemplo de **recusa** respeitada).
- **Perigos:** visibilidade, eco que confunde a direção do som, frio moderado.
- **Rotas:** clareiras com marcos; um **Abrigo Transitório** móvel (ver Postos).
- **Painel:** “A prece no caminho” — *Nosso Lar*, cap. 2 (a virada pela prece).

### R4 — Encruzilhada dos Rumores (faixa 2, influência obsessiva D)
- **Fonte:** criação [D]. Tema: *Nosso Lar* cap. 2 (vozes que perseguem e impedem de pensar), cap. 12
  (afinidade de pensamentos, “verdugos e vítimas”) [B]; *Memórias* cap. I (obsessores que se passam por juízes)
  [B, **sem** os abusos].
- **Identidade:** cruzamento de caminhos com placas tortas e contraditórias; fogueiras frias; grupos reunidos.
- **NPCs:** “instigadores”, que dão falsas direções, zombam de forma leve, dizem que “não há saída” e que “os
  socorristas mentem”; “seguidores”, que repetem rumores. **Ninguém ataca fisicamente.**
- **Perigos:** trilhas falsas (§26.3 D); perturbação sobe ao ficar perto dos grupos; falsos sinais de posto (luz
  avermelhada, não azul).
- **Contrajogo:** discernimento. A luz **verdadeira** dos postos é branca ou azul e constante; a falsa tremula.
  Prece e conversa calma reduzem a influência.
- **Painel:** “Afinidade e pensamento” — *Nosso Lar* cap. 12; LE (escala de influências; referência a conferir
  pelo pesquisador antes de publicar).

### R5 — Desfiladeiros de Cinza (faixa 3, transição para o Vale)
- **Fonte:** [D←B] *Memórias* cap. I (gargantas, paredões, “como cinza”).
- **Identidade:** trilha descendente entre paredões, chão escuro e escorregadio, céu de nuvens baixas.
- **Função:** **portal narrativo**. Aqui aparece o aviso de conteúdo (FF §7.2) na primeira vez. A entrada
  principal é **com a caravana** (missão “Aprendiz da caravana”, ver NPCS).
- **Perigos:** escorregões (empurrão leve, sem dano de queda), frio crescente.

### R6 — VALE (região-piloto) — inspirado no “Vale dos Suicidas” de *Memórias de um Suicida*
**Nome visível sugerido [D]:** “Vale das Sombras (inspirado em *Memórias de um Suicida*)”. O nome da obra aparece
no painel e no livro de estudo, mas não em letreiros gigantes nem em falas de NPC. (Decisão do orquestrador; ver
FF §7.)

| Item | Conteúdo | Classe |
|---|---|---|
| Relevo | Vale profundo, gargantas, cavernas nas encostas, lamaçal no fundo, paredões “como de pedra e ferro” | [B] Memórias I |
| Clima | Ar frio e pesado; nuvens baixas; chuva e enxurradas raras (evento) | [B] Memórias I |
| Tempo | Sem ciclo de dia e noite visível; o HUD esconde o relógio | [B] Memórias I |
| Labirinto | Trilhas que voltam ao fundo do vale; “correnteza” leve que puxa para o centro quando o jogador corre sem rumo | [D←B] Memórias I (“atraídos de volta como por ímãs”) |
| Refúgios numerados | Cavernas com marcos de pedra numerados (ex.: “Ref. 36 · Via 48”) que servem à **navegação** e ao chamado da caravana | [D←B] Memórias II |
| Assistidos | NPCs sentados, encolhidos, que choram, repetem frases de desânimo ou pedem ajuda. **Sem ferimentos.** | [D←B] |
| Obsessores | Figuras sem rosto à distância que tentam convencer de que “não há saída”; somem quando a caravana chega | [D←B] Memórias I (sem abusos) |
| Caravana | Chega a cada ciclo (ver GAMEPLAY): comboio branco, cavalos brancos, cruz azul-celeste, voz que chama nomes | [B] Memórias I–II |
| Saída | 1) Embarcar na caravana. 2) Trilha de **lírios brancos** (sinal discreto) que sobe até o Mirante da Legião (posto de observação). 3) Retorno ao Desfiladeiro com acompanhamento. | [D] |
| Proibido | Gore, métodos, vermes, corpos, armas, cordas, sangue, gritos de dor realistas, insultos | ética |

**Pontos de interesse do Vale [D salvo indicação]:**
1. **Mirante da Legião**: posto de observação na borda, com farol azul. É a **saída garantida** e funciona como
   ponto de apoio às equipes (§26.7). [D←B: a obra fala de aparelhos que acompanham o Vale de longe.]
2. **Praça do Embarque**: lamaçal plano onde a caravana para [B, Memórias II: “praça enlameada”].
3. **Ref. 36 · Via 48**: caverna de referência para a missão de resgate guiado [D←B].
4. **Fonte Turva**: filete d’água turva que dá alívio de sede **pequeno e temporário** [D←B NL cap. 2].
5. **Pedra da Prece**: rocha alisada onde a prece tem efeito ampliado na perturbação; não teleporta [D].

### R7 — Colônia de Recuperação e Hospital Maria de Nazaré (fora do Vale)
- **Fonte:** *Memórias* 1ª parte cap. III [B]. Construir **só** o que está descrito; o resto fica marcado [D].
- **Acesso:** só pelo comboio. Viagem com persianas fechadas (tela de transição) e chegada à **fortaleza branca**.
  É estrutura separada em área própria ou subdimensão (a decidir pelo arquiteto), **não** no meio do Umbral.

| Área | Descrição usada | Classe |
|---|---|---|
| Fortaleza de entrada | Muralha, 6 torres, fosso e ponte, portão com inscrição da Legião; **Torre de Vigia** | [B] |
| Paisagem | Planícies e colinas brancas “como neve”, névoa, sol fraco, sem frio intenso | [B] |
| Bairro da Vigilância | Prédios em estilo português clássico; recepção e “matrícula” (no jogo: nome e “como se sente”, **sem** pergunta sobre causa ou meio) | [B] / ética |
| Estrada interna | Trenós leves, arbustos brancos “como flores dos Alpes”, arcos de entrada | [B] |
| Bifurcação | Placas: Psiquiátrico (direita), Isolamento (esquerda), Hospital Maria de Nazaré (centro) | [B] |
| Hospital Matriz | Parque, arcadas, colunas, torres, terraços, trepadeiras, tanques e cascatas, pombas, lírios; **sem muralha** (cercas vivas de árvores, rosas e lírios) | [B] |
| Enfermaria | Galeria com portas de vidro de moldura azul; quartos de 10 leitos; sacada; lareira; quadro luminoso de Maria | [B] |
| Cuidados | Banho, roupa limpa, caldo morno, sono, presença constante | [B] |
| Psiquiátrico e Isolamento | Só fachada e placa nesta rodada; interior **não** construído (a obra trata deles na 2ª parte; pesquisar antes) | [B] fachada / pendente |
| Escola, “Cidade Universitária”, “Mansão da Esperança” | **Fora do escopo do piloto** (3ª parte da obra) | pendente |
| Separação por gênero | A obra separa; o jogo **não** reproduz (decisão de design) | [D] |

## 3. Postos e instituições do Umbral 2.0

| Tipo | Inspirado em | Onde | Classe |
|---|---|---|---|
| Posto de acolhimento inicial (central, existente) | Postos de socorro em geral; Casa Transitória | R0 | [D←B] |
| Postos pequenos (existentes) | idem | R1 | [D] |
| **Abrigo Transitório móvel** (novo): casarão baixo, sem jardim, barreira magnética, portaria com comunicador; muda de lugar entre 3 pontos fixos a cada ciclo longo | *Obreiros da Vida Eterna* cap. 4, Casa Transitória de Fabiano | R3/R4 | [D←B] |
| Posto avançado / ponto de apoio a equipes | idem | faixa 2 | [D] |
| **Mirante da Legião** (observação e saída) | *Memórias* I–II | borda do Vale | [D←B] |
| Instituição de recuperação prolongada (escola de reajuste, fortificada, entre tempestades) | *Ação e Reação* cap. 1, Mansão Paz | faixa 2–3 (rodada futura) | [D←B] |
| Colônia de Recuperação / Hospital Maria de Nazaré | *Memórias* III | área própria | [B] |
| Câmaras de Retificação (existente, em Nosso Lar) | *Nosso Lar* caps. 26–27 | destino da saída assistida | [B] |

**Escudos dos postos** (§7.2 do PROMPT) [D←B]: barreira magnética visível como halo azul tênue a cerca de 6–10
blocos do portão. Do lado de dentro, a perturbação não sobe; obsessores não entram. Base: *Obreiros* cap. 4
(“barreiras magnéticas”) e *Ação e Reação* cap. 1 (“recursos de segurança e defesa”).

## 4. Rotas

| Rota | De → para | Sinalização | Classe |
|---|---|---|---|
| Linhas de marcos (existentes) | Central ↔ cada posto | marco com lanterna a cada 48 blocos | [D] |
| Trilha das Lanternas Azuis | faixa 1 → faixa 2 | lanternas azuis constantes (as falsas tremulam e são avermelhadas) | [D] |
| Passarelas dos Charcos | ilhotas da R1 | troncos e cordas | [D] |
| Descida do Desfiladeiro | faixa 2 → Vale | placa de aviso + ponto de encontro com a caravana | [D] |
| Trilha dos Lírios | fundo do Vale → Mirante | lírios brancos esparsos | [D] (o lírio aparece no Hospital [B]) |
| Comboio da Legião | Praça do Embarque → Colônia | veículo; transição | [B] |
| Caravana dos Samaritanos | postos → Câmaras de Retificação | carroças, cães, muares, íbis | [B] *Nosso Lar* cap. 33 |

**Regra de navegação [D]:** de **qualquer** ponto jogável deve existir um caminho até um posto ou mirante em
menos de 400 blocos de trilha. Nenhuma área é fechada sem saída (§26.5, §26.8).

## 5. Parâmetros de risco por região (resumo; detalhes em GAMEPLAY)

| Região | Visib. (blocos) | Navegação | Perturb. amb. (/min) | Entidades (máx./região) | Distância a auxílio | Risco narrativo |
|---|---|---|---|---|---|---|
| R0 Campos | 40 | fácil | +0,5 | 6 | ≤150 | 1 |
| R1 Charcos | 28 | média | +1 | 10 | ≤200 | 2 |
| R2 Ruínas | 24 | circular | +1,5 | 8 | ≤250 | 2 |
| R3 Bosque | 10 | difícil | +1,5 | 6 | ≤250 (abrigo) | 3 |
| R4 Encruzilhada | 20 | enganosa | +2,5 (perto de grupos) | 12 | ≤250 | 3 |
| R5 Desfiladeiro | 16 | linear | +1,5 | 4 | ≤150 (mirante) | 3 |
| R6 Vale | 12 | labirinto | +2 | 14 (8 assistidos + 6 presenças) | ≤200 (mirante) | 4 |
| R7 Colônia | 60 | fácil | −3 (recupera) | 20 | — | 0 |

Visibilidade e perturbação respeitam a configuração de acessibilidade já existente (`efeitos_perturbacao`).

## 6. O que fica para depois (não implementar no piloto)
- Trevas (*Nosso Lar* cap. 44): só menção educativa.
- Psiquiátrico, Isolamento, Departamentos e Cidade Universitária de *Memórias* (2ª e 3ª partes).
- Mansão Paz completa (*Ação e Reação*).
- Regiões com nomes de grupos humanos (“drogados”, “ateus” etc.): **proibidas** (§26.3 E).
