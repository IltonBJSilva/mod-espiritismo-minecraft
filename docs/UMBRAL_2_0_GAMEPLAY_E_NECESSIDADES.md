# Umbral 2.0 — Gameplay e necessidades subjetivas

Agente **pesquisa** (papel de game designer). Classes: **[A]** Kardec · **[B]** obra mediúnica · **[C]**
interpretação · **[D]** criação · **[D←B]** adaptação fundamentada. Fontes em `UMBRAL_2_0_FONTES_E_FIDELIDADE.md`
(FF). Os números são **sugestões iniciais** para o arquiteto e o QA calibrarem.

## 1. Princípios (não negociáveis)

1. **Ninguém morre de necessidade.** Barras chegam a um piso de desconforto e param ali [D←B *Nosso Lar* cap. 2:
   esgotamento sem extenuação absoluta; A LE 257].
2. **Sofrimento não é punição em pontos.** A moralidade influencia a **velocidade**, nunca a existência de saída.
   Recusar, explorar ou questionar não reduz a moralidade (regra já vigente no mod).
3. **O alívio verdadeiro é o acolhimento.** Itens do caminho dão alívio parcial e passageiro; o posto e o hospital
   recuperam [B *Nosso Lar* cap. 3; *Memórias* cap. III].
4. **Livre-arbítrio com proteção contra softlock** [B *Ação e Reação* cap. 1 + §26.8].
5. **Legível, gradual e ajustável:** no máximo **dois** efeitos sensoriais ativos ao mesmo tempo.

## 2. Modelo de necessidades

Variáveis novas no jogador, todas de 0 a 100 (0 = bem, 100 = desconforto máximo). Persistem no save e só são
sincronizadas com o próprio jogador. **Separadas da fome vanilla:** a fome vanilla fica congelada enquanto o
jogador está em estado espiritual (§26.4).

| Variável | Fonte | Ativa quando |
|---|---|---|
| `sede` | [B] NL 2; Memórias I | dimensão Umbral, fora de posto |
| `fome` | [B] NL 2; Memórias I | idem |
| `frio` | [B] Memórias I–II; [A] LE 257 | regiões com `frio_amb > 0` (R3, R5, R6) |
| `exaustao` | [B] NL 1; Memórias I–II (insônia, prostração) | sempre no Umbral; sobe mais ao correr |
| `perturbacao` | **já existe** (0–100) | sem mudança de semântica |

**Não usar** “falta de ar” como mecânica (gatilho de ansiedade); só o som ambiente fica abafado.

### 2.1 Taxas (por minuto real, fora de posto)

| Variável | Base | Multiplicadores | Piso de desconforto |
|---|---|---|---|
| sede | +1,2 | região (R1 ×1,5; R6 ×1,3); perturbação ≥50 ×1,2 | trava em 85 |
| fome | +0,8 | região R6 ×1,2 | trava em 85 |
| frio | +`frio_amb` (R3 0,8; R5 1,2; R6 1,5) | parado ×0,7 (abrigado em caverna) | trava em 80 |
| exaustao | +0,6 andando; +2,0 correndo; −1,0 parado e sentado | perturbação ≥50 ×1,3 | trava em 90 |

**Moralidade** só altera a velocidade: fator `0,8 + 0,4 × (1 − moral/100)`, ou seja, de ×0,8 (moral 100) a ×1,2
(moral 0). **Nunca** zera as taxas e nunca bloqueia a recuperação.

### 2.2 Efeitos por faixa (máximo de 2 efeitos ao mesmo tempo; prioridade: exaustão > perturbação > frio > sede > fome)

| Faixa | Sede | Fome | Frio | Exaustão |
|---|---|---|---|---|
| 0–39 | nada | nada | nada | nada |
| 40–69 | som de garganta seca a cada 2–3 min; fala interna (“preciso de água…”) | fala interna, leve tremor no ícone | respiração visível (partícula), postura encolhida ao parar | corrida limitada a 6 s |
| 70–85/90 | leve desfoque nas bordas; dificuldade de “ler” placas distantes (texto embaralhado em 1 de 3 letras) | lentidão −10% | lentidão −10%; tremor de câmera **desligável** | sem corrida; lentidão −15%; oferta de sentar |

Sem dano, sem tela vermelha, sem animação grotesca (§26.4 “Apresentação visual”).

### 2.3 HUD contextual
- Nada aparece abaixo de 40.
- A partir de 40, aparece um **ícone pequeno** (gota, pão, floco, passo) com cor neutra. Barras numéricas ficam
  só no **Diário** (tecla J) ou no modo de acessibilidade “HUD sempre visível”.
- O relógio fica oculto no Vale (R6) [D←B *Memórias* I].

### 2.4 Alívio e recuperação

| Ação | Onde | Efeito | Fonte |
|---|---|---|---|
| Beber da Fonte Turva / filetes d’água | R1, R6 | sede −15, **teto**: não desce abaixo de 40; recarga de 3 min por fonte | [D←B] NL 2 |
| Comer ervas silvestres | R1, R3 | fome −10, mesmo teto 40 | [D←B] NL 2 |
| Sentar ou descansar | qualquer lugar | exaustão −1/min; em caverna, frio ×0,7 | [D] |
| Prece (`/espiritismo_prece`, já existe) | qualquer lugar | perturbação −8; durante 60 s as taxas de sede e fome caem pela metade; recarga de 2 min | [D←B] NL 2 |
| Água do posto | postos | sede −40 ao longo de 20 s (gradual) | [D←B] NL 3 |
| Caldo do posto ou do hospital | postos / R7 | fome −40 ao longo de 30 s; no hospital, frase de memória afetiva (“lembrou a mesa de casa”) | [B] NL 3; Memórias III |
| Lareira / cobertor | postos / R7 | frio −5/s até 0 | [B] Memórias III |
| Leito (repouso, já existe) | postos / R7 | exaustão a 0 em 60 s; perturbação −20 | [B] Memórias III; [D] |
| Conversa com trabalhador | postos | perturbação −10; necessidades congeladas por 2 min | [D] |

**Água fluidificada** não é poção: no posto ela só **acelera** a recuperação da sede e da perturbação (×1,5 por
60 s). Não cura nada sozinha (§26.4 regra 8). Itens trazidos da Terra **não** funcionam no Umbral (§26.4).

Dentro do raio do escudo de um posto, **todas** as taxas param e a recuperação natural é de −1/min em cada
variável.

## 3. Risco por região

Parâmetros por região (valores no MAPA §5): `visibilidade`, `navegacao` (fácil, média, circular, enganosa,
labirinto), `perturb_amb`, `max_entidades`, `freq_encontros`, `dist_auxilio_max`, `prob_rota_segura`,
`risco_narrativo` (0–4), `frio_amb`.

**Risco efetivo** (usado para escolher encontros e eventos) [D]:
```
risco = risco_narrativo
      + 1 se perturbacao ≥ 50
      + 1 se (sede ou fome ou frio) ≥ 70
      + 1 se distância ao posto/mirante mais próximo > dist_auxilio_max
      − 1 se acompanhado por socorrista ou por outro jogador
      (limite 0..5; a moralidade NÃO entra nesta soma)
```

| Risco | Eventos possíveis (todos têm sinal prévio e contrajogo) |
|---|---|
| 0–1 | nenhum; sofredores calmos |
| 2 | eco de voz; trilha que some; neblina que adensa por 30 s |
| 3 | instigador com falsa direção; luz falsa; caminho circular |
| 4 | enxurrada rara no Vale (empurra para a encosta; **não** afoga); grupo que bloqueia a passagem e se dispersa com conversa ou prece |
| 5 | **oferta de socorro reforçada** (o jogo ajuda mais quanto pior a situação) |

Nunca existe dano direto à vida. Se um efeito vanilla (lava, queda, sufocamento) ocorrer por bug, o estado
espiritual o converte em perturbação e o teleporte seguro leva ao posto mais próximo.

## 4. Ciclo de amparo

### 4.1 No Umbral geral (já existe; ajustes)
O fluxo atual continua: socorristas chegam devagar, oferecem Aceitar, Conversar, Pedir tempo ou Recusar,
acompanham até o posto e fazem o acolhimento. Ajustes:
- **Detecção de “perdido”**: mais de 8 min sem se aproximar de nenhum posto, OU 3 passagens pela mesma área
  circular, OU qualquer necessidade ≥ 80 por 3 min. Isso gera uma **oferta discreta**: uma lanterna azul
  aparece a 30–40 blocos na direção do auxílio mais próximo (sem NPC). Recarga de 4 min.
- **Pedido ativo**: a tecla H (já existe) continua disponível sempre, sem custo.

### 4.2 Caravana do Vale (piloto) [D←B *Memórias* I–II]
- **Ciclo:** a caravana chega à Praça do Embarque a cada **12 min** de jogo dentro do Vale e fica **3 min**.
  Antes de chegar, um **sino** e a **voz do ar** anunciam a chegada 60 s antes.
- **Chamado:** a voz chama os NPCs assistidos pelo nome e chama o jogador (“Você que está na Via 48, ouça…”).
- **Elegibilidade do jogador** [D]: é **sempre** elegível se aceitar. O critério “vibratório” do livro **não**
  bloqueia o jogador; ele aparece só nos NPCs como narrativa (“ele ainda está dormindo; virão por ele”).
  Motivo: §26.3 diz que a saída não pode depender de pagar pontuação.
- **Quem não está na praça:** um socorrista desce até a caverna marcada (Ref. N · Via N), como o livro descreve,
  e acompanha a pé.
- **Embarque:** tela de transição com persianas → chegada à Colônia (R7).
- **Se recusar:** a caravana parte; o jogador recebe a mensagem de que ela volta e uma lanterna azul marca o
  Mirante.

### 4.3 Colônia e Hospital (R7) [B *Memórias* III]
1. Recepção na Vigilância: nome, e a pergunta “Como você se sente agora?” com opções livres e sem julgamento.
2. Trenó até o Hospital (transição curta).
3. Enfermaria: banho (troca a skin para roupa branca de convalescente), caldo, lareira, leito.
4. **Sono reparador** (fade de 10 s): perturbação −30 e frio a 0.
5. **Conversas de convalescença** com outros assistidos (ver NPCS) e com os enfermeiros.
6. **Alta gradual:** depois de 3 “dias de convalescença” (cada um = 1 repouso + 1 conversa), o jogador escolhe
   entre continuar como **aprendiz** na Colônia (missões de cuidado), voltar ao Umbral **acompanhado** até um
   posto ou seguir para as Câmaras de Retificação de Nosso Lar (se `saidaAssistidaLiberada`). As três opções
   ficam sempre disponíveis, sem trava de moral para a primeira e a segunda.

## 5. Livre-arbítrio, recusa e anti-softlock

| Situação | Regra | Base |
|---|---|---|
| Recusa consciente (botão Recusar) | Respeitada. Recarga de abordagem 5 min (já existe), +5 de perturbação com teto 90 (já existe). **Sem** teleporte. | [B] Ação e Reação 1; §26.8 |
| Recusa repetida (3 vezes em 30 min) | Recarga sobe para 10 min; as lanternas azuis continuam a aparecer. Nada mais. | [D] |
| Incapacidade temporária (perturbação ≥ 80 **e** necessidade ≥ 80) | A oferta muda: o socorrista **senta perto** e espera 30 s em silêncio antes de falar; o texto fica mais simples; o botão principal passa a ser “Só fique aqui comigo”. | [D←B] *Memórias* III (presença constante) |
| Condição crítica após recusa | Uma nova oferta só depois da recarga e só se a condição piorou (+15 em qualquer variável). **Sem spam.** | §26.8 |
| Preso fisicamente (sem caminho em 60 s, pathfinding falhou) | Uma ponte, escada ou passarela aparece no próximo ciclo; em último caso, teleporte seguro ao mirante ou posto com mensagem narrativa (“uma equipe o encontrou desacordado”). | §26.5 |
| Saída do jogo no meio do resgate | Ao voltar, aparece no posto ou mirante mais próximo com o estado preservado. Nada de entidades órfãs. | regra já vigente |
| Morte vanilla no Umbral | Vira RECEM_DESENCARNADO (já existe). No Vale, desperta no **Mirante** e nunca no fundo. | [D] |

## 6. Conteúdo educativo contextual

Um **painel** por região (placa lendária ou atril). Abre um livro de 1–2 páginas com 4 blocos fixos [D]:
1. **Inspiração:** obra, autor espiritual, médium, parte e capítulo.
2. **O que a obra descreve** (paráfrase curta).
3. **O que é adaptação do jogo.**
4. **Para refletir** (uma pergunta aberta, sem sermão).

Exemplo para o Vale (texto original do jogo):
> **Inspiração:** *Memórias de um Suicida*, narrado pelo Espírito Camilo Cândido Botelho e psicografado por
> Yvonne A. Pereira (FEB), 1ª parte, caps. I–III.
> **Na obra:** um grupo de Espíritos sofre num vale escuro e frio, desorientado, até que uma caravana de
> socorro os leva a um hospital onde recebem banho, alimento, repouso e companhia.
> **No jogo:** os caminhos, os nomes das trilhas, os personagens e as missões são criação nossa. Há leituras
> diferentes desse livro dentro do próprio movimento espírita, e nenhuma pessoa real pode ter seu destino
> afirmado por ele.
> **Para refletir:** o que ajuda alguém que se sente sem saída a perceber que não está sozinho?
> *Se você estiver passando por um momento difícil: CVV, 188, 24 h, gratuito (cvv.org.br).*

## 7. Persistência e API sugeridas (para o arquiteto; sem código)

- No jogador: `necessidades{sede,fome,frio,exaustao}`, `regiao_atual`, `ultima_oferta_tick`,
  `recusas_janela[]`, `caravana_proxima_tick` (por dimensão), `convalescenca_dias`, `vale_aviso_visto`,
  `vale_desativado_jogador`.
- No servidor: `vale_habilitado`, multiplicadores globais de necessidade (0–2), `max_efeitos_simultaneos`
  (padrão 2).
- Eventos: `AoEntrarRegiao`, `AoNecessidadeFaixa`, `AoOfertaSocorro`, `AoRecusa`, `AoEmbarqueCaravana`,
  `AoAltaHospital`.
- Todo teleporte passa por `seguranca.TeleporteSeguro.teleportar(...)` (CONTRATO3).

## 8. Acessibilidade
- `efeitos_perturbacao=completo|reduzido|desligado` (já existe) passa a valer também para as necessidades.
- Opções: “HUD de necessidades sempre visível”, “sem tremor de câmera”, “sem desfoque”, “sons subjetivos
  reduzidos”, “ocultar região Vale”.
- Legendas para a voz do ar e para os ecos.
