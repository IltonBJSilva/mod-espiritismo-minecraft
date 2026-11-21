# Auditoria do estado atual — Mod Espiritismo Minecraft

Data: 2026-11-21. Agente: "auditoria" (somente leitura + testes). Cópia auditada: `E:/claude-mod-trabalho/ws2_auditoria`
(cópia do workspace MCreator antes das alterações da Fase 4). Nenhum arquivo de código foi alterado.

Referência das linhas: caminhos relativos a `src/main/java/net/mcreator/modespiritismominecraft/`.

---

## 1. Inventário técnico (confirmado nos arquivos)

| Item | Valor | Onde foi confirmado |
|---|---|---|
| Gerador MCreator | `neoforge-26.1.2` | `mod_espiritismo_minecraft.mcreator` → `"currentGenerator"` |
| Versão do MCreator que salvou o workspace | `202600233518` (campo `mcreatorVersion`; plugin `generator-26.1.x.zip` instalado) | `.mcreator`, `C:/Program Files/Pylo/MCreator/plugins` |
| Minecraft | 26.1.2 | `neoforge.mods.toml` (`versionRange="[26.1.2]"`) |
| NeoForge | 26.1.2.95 | `build.gradle` (`neoForge { version = "26.1.2.95" }`), `neoforge.mods.toml` |
| ModDevGradle | 2.0.141 | `build.gradle` |
| Gradle | 9.6.1 | `gradle/wrapper/gradle-wrapper.properties` |
| Java | 25 (toolchain); JDK do MCreator = OpenJDK 25.0.2 (JBR) | `build.gradle`, `java -version` |
| FancyModLoader | 11.0.15 | log do GameTest |
| Mod ID / versão | `mod_espiritismo_minecraft` 1.0.0 | `neoforge.mods.toml` |
| Tamanho | 296 arquivos em `src/`, 8.916 linhas de Java (66 arquivos) | `find`/`wc` |
| Git | O workspace MCreator NÃO é repositório git (não há histórico local para inspecionar) | memória do projeto |

Arquivos gerados pelo MCreator (sobrescritos a cada Build — não editar): `ModEspiritismoMinecraftMod.java` (exceto blocos
`// Start of user code`), `init/*`, `item/*`, `block/LivroBlock.java`, `entity/*`, `client/renderer/*`,
`elements/*.mod.json`, `assets/mod_espiritismo_minecraft/lang/*.json`, `mcreator.gradle` (o MCreator o apaga ao abrir;
por isso a configuração de GameTest fica em `testes/gametest.init.gradle`).

Scripts auxiliares (`arte_fonte/`): `gerar_texturas.py` (ovos e livro do MCreator — rodar após cada Build),
`itens_texturas.py` (rodar depois do anterior), `npc_texturas.py`, `itens_json.py`, `gerar_dados.py` (conquistas).

## 2. Mapa de pacotes e sistemas

| Pacote / arquivo | Responsabilidade |
|---|---|
| `espiritismo/Moralidade.java` | Attachments do jogador (moralidade 0–100, classe, energia, recargas, jornada, objetivo, caridade oculta, `ESTADO`), teto diário `ganhoRotina` (8/dia), `caridadeOculta`. Attachments do Overworld (`CENTRO`) e do aldeão (`FAMINTO`). |
| `espiritismo/EventosEspirituais.java` | Ações boas/más, água fluidificada, Livro dos Espíritos, Kardec/Chico/Haroldo, caridade (aldeão faminto), efeitos por moralidade, `/moralidade`, `/centro`, `/espiritismo …`, abertura da história no login. |
| `espiritismo/Jornada.java` | Tutorial 0–5, conquistas `jornada/*`, aldeão faminto, palestras do Haroldo. |
| `espiritismo/Habilidades.java`, `ClasseEspiritual.java` | Classes Médium/Doutrinador/Trabalhador, tecla V, passivas. |
| `espiritismo/CentroEspirita.java` | Centro do tutorial construído por código perto do spawn no 1º login + NPCs do tutorial. |
| `espiritismo/BussolaEspiritual.java`, `Objetivo.java` | Bússola (estruturas por tag) e seta "GPS" do HUD. |
| `espiritismo/Pacotes.java`, `client/*` | Payloads, HUD, teclas, tela de escolha de classe (cliente isolado). |
| `espiritismo/npc/*` | 6 trabalhadores encarnados (`TrabalhadorEntity`), 5 Espíritos (`EspiritoEntity`), prece (tecla B, gancho `AO_ORAR`), atendimentos, falas, aparições (mentor/obsessor/zombeteiro). |
| `espiritismo/itens/*` | Blocos `livro_1857`, `mesa_mediunica`, `tribuna`, `jarra_agua`; 8 obras legíveis; caderno; doações; fluidificação. |
| `espiritismo/mundo/*` | Tipo de estrutura `construcao_espirita` (centros P/M/G, casa_kardec, casa_chico, mansao_do_caminho, auditorio_haroldo) + JSON de vilas grandes. |
| `espiritismo/dimensao/*` | Nosso Lar/Umbral (JSON data-driven), destino após a morte, resgate, reencarnação, sonho, construções de chegada, `/espiritismo_plano`, `/espiritismo_prece`. |
| `espiritismo/estado/*` | **Somente contrato**: enum `EstadoEspiritual` (13 estados) e `Estados` (transição aceita qualquer coisa). Nenhum chamador. |
| `espiritismo/colonia/ColoniaApi.java` | **Somente contrato**: `protegido()` = "toda a dimensão Nosso Lar", mas nenhum evento o consulta. |
| `*/teste/*` | 31 GameTests (5 classes). |

---

## 3. Bugs e riscos encontrados (mais graves primeiro)

### B1 — CRÍTICO — Flores/plantas quebram e viram itens ao chegar em Nosso Lar (causa exata)
- **Arquivos/linhas:** `espiritismo/dimensao/ConstrucoesPlanos.java:47` (`FLAGS = Block.UPDATE_CLIENTS`),
  `:55-62` (`set`), `:70-82` (`acertarColuna`, laço de limpeza **de baixo para cima** nas linhas 71-73),
  chamado em `:192-197` para um quadrado de 93×93 colunas (`RAIO = 46`, linha 48).
- **Causa:** a origem é posta em `g = chao(0,0)` (Planos.java:103). Em toda coluna onde o terreno natural é mais alto
  que `g`, `acertarColuna` remove os blocos de `g+1` até o topo, **começando por baixo**. Quando o bloco de grama/terra que
  sustenta uma planta vira ar, o `Level.setBlock` com flag 2 (sem `UPDATE_KNOWN_SHAPE`=16) executa
  `markAndNotifyBlock` → `updateNeighbourShapes` (Level.java:278-282 da fonte descompilada, `neighbourUpdateFlags = flags & -34`).
  A planta acima recebe `updateShape`; `VegetationBlock.updateShape` devolve AR porque `canSurvive` falhou; e
  `Block.updateOrDestroy` (Block.java:221-233) chama `level.destroyBlock(pos, (flags & 32) == 0, …)` — como a flag 32
  (`UPDATE_SUPPRESS_DROPS`) não está presente, **a planta é destruída com drop**. Os itens ficam no chão da área de
  chegada e o jogador os recolhe ao andar.
- **Evidência:** a barra do jogador em `referencias/Screenshot_2.png` mostra exatamente o que as features do bioma
  geram e que dropam com `destroyBlock`: flores silvestres (`wildflowers_meadow`, 64+26 — o drop é a quantidade de
  flores do canteiro), dentes-de-leão (`flower_plains`, 34) e sementes de trigo (5, drop de `short_grass` de
  `patch_grass_meadow`). O bioma `worldgen/biome/nosso_lar.json` pede `flower_meadow`, `wildflowers_meadow`,
  `flower_plains`, `patch_grass_meadow`, `patch_sunflower`, `patch_sugar_cane`, `trees_meadow/plains`. O noise de Nosso Lar
  (`final_density` = gradiente 38→98 + 0,2·ruído) dá ±5–6 blocos de relevo, por isso muitas colunas ficam acima de `g`.
  As flores que o próprio mod coloca (linhas 282-287) NÃO quebram (são postas por último, sobre grama, sem alterar suporte).
- **Efeitos colaterais da mesma causa:** (1) cana-de-açúcar perto de água aterrada quebra no tick agendado (drop);
  (2) folhas de árvores naturais cortadas pela limpeza ficam sem tronco e **decaem por random tick durante minutos**,
  soltando mudas/maçãs/gravetos — "itens aparecendo" mesmo depois da chegada; (3) `trees_meadow` tem colmeias: abelhas
  ficam soltas em Nosso Lar; (4) o mesmo padrão existe no Umbral (`:356-358`, `:391-393` — arbustos secos dropam
  gravetos) e no Centro do tutorial no Overworld (`CentroEspirita.java:50` e `:118-121`, limpa y+1..y+12 de baixo para
  cima no 1º login).
- **Cenário de reprodução:** mundo novo → `/moralidade definir 80` → `/kill` → desperta em Nosso Lar → olhar o chão em
  volta da praça e o inventário. Ou `/espiritismo_plano ir nosso_lar` num mundo novo.
- **"Buraco com paredes de terra" (pequeno.png):** mesma função — o quadrado inteiro é nivelado em `g` sem transição,
  então o terreno natural em volta (mais alto) vira paredes de terra/pedra de até ~6 blocos.
- **Correção recomendada (para a área "colonia"):** limpar de cima para baixo e remover plantas/folhas ANTES do suporte,
  usando flags `Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS` (2|16|32) nas operações
  de terreno; recolocar suporte antes da planta; geração por etapas; teste GameTest que conte `ItemEntity` na área após
  a construção (deve ser 0) e verifique `canSurvive` de cada planta.

### B2 — CRÍTICO — Nosso Lar não tem NENHUMA proteção
- `colonia/ColoniaApi.java:18-20` declara `protegido()` mas nada o consulta. Não existe handler de `BlockEvent.BreakEvent`,
  `EntityPlaceEvent` (o único, `EventosEspirituais.java:155`, só dá moralidade por muda), `ExplosionEvent`, fluidos,
  pistões, fogo ou `AttackEntityEvent` em nenhum pacote (busca por `BreakEvent|ExplosionEvent|Detonate|PistonEvent` vazia).
- **Cenário:** sonho de desdobramento (moral ≥ 50, 30% por noite, `Planos.iniciarSonho`, Planos.java:300-323) leva o
  jogador a Nosso Lar **com o inventário inteiro** (picareta, TNT, baldes de lava) por 90 s. Com `keepInventory`, o mesmo
  vale para quem desencarna. O jogador pode quebrar prédios, o atril e as flores. Só o atril do Auxílio é recolocado
  (ConstrucoesPlanos.java:442-446, a cada 10 s e com `FACING` padrão NORTH em vez de EAST).

### B3 — ALTO — Travamento na primeira construção de Nosso Lar (e do Umbral)
- `Planos.garantir` (Planos.java:91-99) constrói tudo **num único tick**, chamado de dentro do evento de renascimento
  (`EventosDimensao.java:87`), de `Planos.ir`, `resgatar` (:163) e `iniciarSonho` (:306).
- Custo: 93×93 colunas × (`getHeight` + até ~10 `setBlock`) + 7 prédios + 260 flores ≈ dezenas de milhares de
  `setBlock` com envio ao cliente e atualização de luz; cada `getBlockState`/`getHeight` em chunk ainda não gerado força
  **geração síncrona** de ~36 chunks (6×6) mais vizinhos até o estágio de features.
- **Medido (GameTest, mundo superplano, chunks baratos):** `dim_construcoes_de_chegada` = 6,68 s para Nosso Lar + Posto
  (log: 14:57:03 → 14:57:07 só Nosso Lar). Em mundo real (noise) o custo é maior.
- `garantir` recalcula `g` a partir de `chao(0,0)`; se o attachment `plano_pontos` se perder, uma segunda colônia seria
  construída ~3 blocos acima (o topo da fonte vira o novo chão) — não idempotente.

### B4 — ALTO — Travamento na reencarnação
- `Planos.localSeguroDistante` (Planos.java:276-295): até 12 tentativas a 600–1200 blocos; cada `superficie()`
  (`getHeight`, :147-149) num chunk inexistente força geração síncrona completa (chunk + vizinhos até features). Em
  oceano/rio as tentativas falham e cada uma gera outra região nova. Depois o teleporte ainda leva a uma região recém-gerada.
- **Medido:** `dim_reencarnar_volta_a_terra` = 1,03 s mesmo em superplano. O fallback (spawn do mundo, :293-294) não
  verifica segurança.
- Relacionado: `BussolaEspiritual.java:440` procura locais especiais num raio de **200 chunks** (`findNearestMapStructure`
  síncrono) — pode congelar o servidor ao usar a bússola agachado.

### B5 — ALTO — Resgate do Umbral sem consentimento e "paciente ajudando paciente"
- `Planos.checarResgate` (Planos.java:200-211), chamado a cada segundo por `tickUmbral` (:214-228): assim que
  `moral > 30` ou arrependimento ≥ 5, o socorrista é criado e o jogador é **teleportado 5 s depois sem escolha**
  (contraria §5.4 e §19).
- O caminho para o resgate exige que o jogador perturbado "console" sofredores (`EventosDimensao.java:249-258` →
  `Planos.ajudouSofredor`, :186-192) e ore por eles (`Espiritos.esclarecer`, ganho +3) — exatamente o que §5.7 proíbe.
- Rejeitar/adiar ajuda, pedir socorro depois e "conversar primeiro": não existem.

### B6 — MÉDIO — Socorrista órfão permanente no Umbral (e em qualquer resgate de sofredor)
- `npc/EspiritoEntity.java:71` (campo `socorrista` não salvo), `:252-270` (`tickResgate`): o socorrista é criado com
  vida **-1 (permanente)** em `resgate == 30` (:255) e só é removido pelo sofredor em `resgate >= 140` (:265-268).
- **Cenário:** no Umbral, o jogador ora pelo sofredor (arrependimento chega a 5) → `RESGATE_EM` = +100 ticks → o
  jogador é levado a Nosso Lar antes do tick 140 do sofredor → o chunk descarrega → ao recarregar, o sofredor some no
  tick 140 mas a referência ao socorrista se perdeu → **socorrista fica para sempre**. Repetível a cada visita. O mesmo
  ocorre ao sair/reiniciar o servidor durante os 7 s do resgate no Overworld.
- Também: `ConstrucoesPlanos.verificarNpcs` (:457-464) cria **2** sofredores sempre que houver menos de 2 (com 1 vivo,
  fica com 3).

### B7 — MÉDIO — Água fluidificada como "cura automática" e fonte infinita
- `EventosEspirituais.java:173-178` remove **todos** os efeitos nocivos (inclusive Escuridão/Lentidão das obsessões e do
  Umbral) + 25 de energia. Contraria §5.5/§19 ("não fazer a água agir como cura automática"), embora a fala diga o contrário.
- Fonte infinita: `CentroEspirita.java:216` põe a jarra em (11,1,7) e `:239` o Passista em (9,1,7) — a 2 blocos;
  `Fluidificacao.passistaPerto` (itens/Fluidificacao.java:276-287, raio 8) é sempre verdadeiro → qualquer garrafa vazia
  vira água fluidificada no Centro do tutorial, sem limite.

### B8 — MÉDIO — Exploits de moralidade restantes
1. `EventosEspirituais.java:444-446`: `Moralidade.set(+4)` fora do teto diário a cada 12.000 ticks (meio dia) — é o
   desenho do inventário, mas combinado com a missão da Evangelizadora (`npc/Atendimentos.java:78-83`, recarga 12.000,
   1/3 de chance) e com `Jornada.garantirAldeaoFaminto(p, true)` (Jornada.java:167-196) é **determinístico**: +4 a cada
   ~10 min além dos +8/dia. Além disso `set()` não passa por `alterar()`: cruzar 80 por aqui não dá a conquista
   "elevado" nem a mensagem.
2. `garantirAldeaoFaminto(..., true)` cria um **aldeão novo** ("Irmão faminto") a cada 5 min quando não há aldeões em
   64 blocos — conversar com a Evangelizadora num lugar sem aldeias gera aldeões infinitos (exploit de economia/comércio).
3. `Moralidade.caridadeOculta` (Moralidade.java:332-346) incrementa `CARIDADE_OCULTA` mesmo quando o ganho efetivo é 0 —
   a mensagem "N boas ações feitas em silêncio" em Nosso Lar fica inflada.
4. No sonho de pesadelo (Umbral), `aoOrar`/`ajudouSofredor` acumulam arrependimento/moralidade (rotina, limitada) e
   `checarResgate` cria um socorrista durante o sonho (`tickJogador` ignora o Umbral no sonho, mas a prece não).
5. Todos os demais ganhos positivos passam por `ganhoRotina` (verificado por busca: Planos 168/189, Espiritos 90/97/108,
   Prece 102, Atendimentos 147/177, Jornada 238, ObraItem 32, EventosEspirituais 138-201). O teto diário usa
   `getGameTime()/24000` (não é afetado por dormir nem por `/time set`). OK.

### B9 — MÉDIO — Testes de dimensão passam sem testar a dimensão
- O GameTestServer cria o mundo **sem as dimensões de datapack** (confirmado no log: as únicas construções foram em
  `minecraft:overworld`). Por isso os ramos `if (server.getLevel(Planos.NOSSO_LAR) != null)` em
  `dimensao/teste/TestesDimensao.java:100-103`, `:170-173` e `:188-200` são **pulados**: teleporte a Nosso Lar/Umbral,
  resgate real e reencarnação a partir de Nosso Lar nunca foram executados por teste. Ver `TESTES_E_VALIDACAO.md`.

### B10 — BAIXO — Outros
- `Habilidades.java:339`: "Converse com Allan Kardec" (o tutorial agora é com o Dirigente).
- `ConstrucoesPlanos.java:445`: atril recolocado com FACING padrão (NORTH) em vez de EAST.
- Mensagem de prece/obsessor em `Espiritos.tickJogador` só roda no Overworld (Espiritos.java:121): no Umbral não há
  obsessores nem influências — o Umbral só tem sofredores e um socorrista.
- `MundoApi.colocar` em runtime (ServerLevel) gera 87 avisos "Trying to mark a block for PostProcessing … not supported"
  no GameTest (cercas/escadas não se conectam quando a peça é colocada fora do worldgen) e tem o mesmo padrão de limpeza
  de baixo para cima de B1 (`PecaEspirita.caixa/ar`), sem drops no worldgen mas com drops se usado por comando.
- `EventosEspirituais.aoEntrar` (:563) constrói o Centro **sincronamente** no primeiro login (0,84 s no teste).
- NPCs de pessoas reais: Chico Xavier fala em 1ª pessoa frases escritas para o jogo (EventosEspirituais.java:357-389,
  Ensinamentos.CHICO) — marcadas no código como "não são citações", mas o contrato pede não inventar falas atribuídas a
  pessoas reais; precisa revisão de conteúdo (sugestão: narrador em 3ª pessoa ou ensinos das obras com referência).
  Divaldo e Haroldo já usam só conteúdo das obras com referência.
- Bloco MCreator `livro` (OBJ "livro gigante") ainda registrado.

### Servidor dedicado (investigação c)
- O GameTest roda em `DEDICATED_SERVER` (log: "FancyModLoader … (DEDICATED_SERVER in DEV)") e carregou o mod, os
  registros e os 31 testes sem erro de classe de cliente → não há import de `net.minecraft.client` em código comum.
- Todas as classes de cliente estão em `@EventBusSubscriber(Dist.CLIENT)` (`client/*`, `npc/client/*`,
  `init/ModEspiritismoMinecraftModEntityRenderers`). `Pacotes.java:348` só referencia `TelaEscolhaClasse` através da classe
  aninhada `PacotesCliente`, dentro de um lambda executado apenas no cliente — seguro. `clientPlayer()` usa reflexão
  protegida por `FMLEnvironment.getDist()`.
- Risco residual baixo: nenhum caminho de crash encontrado. Não foi feito teste com `runServer` + cliente real conectado
  (exige EULA, proibido pelo contrato).

### Pontos onde o jogador pode ficar preso (investigação e)
| Situação | Resultado hoje |
|---|---|
| Em Nosso Lar | A única saída é o atril do Ministério do Auxílio (clicar 2×). Morrer em Nosso Lar renasce em Nosso Lar. Se o atril for quebrado, volta em ≤10 s (com o jogador por perto). Não há saída a pé (o terreno é infinito e não leva ao Umbral). Sem softlock duro, mas a saída não é explicada no mundo além da placa. |
| No Umbral com moral ≤ 30 | Precisa de 5 pontos (prece 1/30 s, sofredor 2/60 s, 1 a cada 3 min parado). Máx. ~15 min. Morrer no Umbral **zera o arrependimento** (EventosDimensao.java:116) e renasce no Umbral — ciclo punitivo, não softlock. |
| Sonho | Termina por tempo; logout no meio do sonho termina no login (EventosDimensao.java:165-169). Morte no sonho → renasce na cama normalmente. OK. |
| Pontos perdidos | Se `plano_pontos` se perder, `garantir` reconstrói (B3: duplicação vertical). |
| Reencarnação | Pode cair no spawn do mundo sem verificação (fallback). Sem prisão. |
| Dimensão ausente | Se Nosso Lar/Umbral não carregarem, `posicaoRenascimento` desiste e o renascimento é normal. OK. |

---

## 4. Matriz de funcionalidades (PROMPT_MESTRE inteiro)

Legenda: **Já existe** · **Parcial** (existe parcialmente) · **Bug** (está com bug) · **Não existe** · **Visual** (precisa
de teste visual — nada foi aberto no jogo nesta auditoria).

### 4.1 Sistemas centrais (§1.1, §7)
| Item | Status | Observação |
|---|---|---|
| Moralidade 0–100, início 50, persistida, sobrevive à morte, faixas | Já existe | Moralidade.java; testes `moralidade_padrao_e_limites` |
| Teto diário +8 de rotina | Já existe | `teto_diario_rotina`; exceções em B8 |
| Recompensa do faminto 1×/meio dia | Bug (exploit) | B8.1/B8.2 |
| Regeneração com 80+, lentidão/cansaço/escuridão/sussurros com ≤20 | Já existe + Visual | EventosEspirituais.java:524-537 |
| Energia espiritual 0–100 | Já existe | `habilidade_gasta_energia_e_recarga` |
| Classes + tecla V | Já existe + Visual | tela de escolha só visual |
| Prece tecla B (custo, recarga, esclarecimento) | Já existe | `npc_prece_gasta_energia` |
| Tutorial, história inicial, HUD, seta GPS, Bússola | Já existe + Visual | bússola com raio 200 chunks (B4) |
| Conquistas da Jornada (14) | Já existe | `mod_carrega` confere |
| Comandos `/moralidade`, `/centro`, `/espiritismo`, `/espiritismo_plano`, `/espiritismo_prece` | Já existe | |
| Três dimensões separadas (moralidade × estado × conhecimento) | Parcial | só moralidade real; estado = contrato sem uso; conhecimento só por recargas/conquistas |
| Aviso de que moralidade é mecânica abstrata | Não existe | |
| Ranking público de moralidade | Não existe (correto) | sync só para o dono |

### 4.2 Estados espirituais (§6)
| Item | Status | Observação |
|---|---|---|
| Enum dos 13 estados + attachment persistido | Parcial | `estado/EstadoEspiritual.java`, `Moralidade.ESTADO` |
| Transições válidas/inválidas | Não existe | `Estados.transicionar` aceita tudo; nenhum chamador |
| Resgate não duplicado | Parcial | `RESGATE_EM > 0` impede 2º agendamento |
| Reencarnação idempotente | Parcial | confirmação em 2 cliques; sem trava de estado |
| Reconexão durante resgate/tratamento | Parcial | `RESGATE_EM` salvo; tratamento não existe |

### 4.3 Nosso Lar (§3, §4.1, §4.2)
| Item | Status | Observação |
|---|---|---|
| Dimensão + bioma + céu próprio | Já existe + Visual | JSON data-driven; tempo fixo; camas não explodem |
| Proteção contra quebra/colocação/explosão/fogo/fluidos/pistões | Não existe | B2 |
| Mensagens de proteção variadas | Não existe | |
| Exceção admin/manutenção | Não existe | |
| Flores não quebram | Bug | B1 |
| Escala grande, muralha, portões, torres | Não existe | quadrado 93×93 num "buraco" |
| Governadoria | Não existe | |
| 6 Ministérios com nomes corretos | Parcial | só fachadas; Auxílio (atril de reencarnação) e Regeneração (camas) têm função |
| Câmaras de Retificação (despertar) | Já existe + Visual | |
| Hospital, biblioteca central, escola, residências, oficinas, lagos, bosque | Parcial/Não existe | bosque e estantes simples; resto não |
| Mirante com escala dos mundos/espírita | Já existe + Visual | |
| Geração incremental/retomável | Não existe | B3 |
| População/rotinas (§3.5) | Não existe | só mentor, socorrista e orador fixos |
| Governador, mentores do livro, mensageiros (§3.6) | Não existe | |
| Amélie Boudet (§3.7) | Não existe | |
| Entrada condicionada (morte ≥60, sonho ≥50, resgate) | Parcial | sem avaliação por mentor; portal de jogador não existe (ok) |
| Saída perigosa/transição ao Umbral | Não existe | |
| Transporte (aeróbus, estações, rotas, mapas) | Não existe | |

### 4.4 Umbral (§5)
| Item | Status | Observação |
|---|---|---|
| Dimensão escura com névoa | Já existe + Visual | |
| Entrada por moralidade ≤30 ao desencarnar; pesadelo ≤25 | Já existe | |
| Postos de Socorro múltiplos | Não existe | só 1 posto 9×9 |
| Influências/obsessores no Umbral | Não existe | obsessores só no Overworld |
| Resgate com escolha (aceitar/conversar/tempo/rejeitar) | Não existe | B5 (resgate automático) |
| Socorristas temporários removidos | Bug | B6 |
| Confusão/alucinação controlada | Não existe | |
| Acessibilidade (reduzir efeitos) | Não existe | náusea/escuridão sem opção |
| Bloqueio de violência no estado perturbado | Não existe | |
| "Paciente não ajuda paciente" | Bug (contrário) | B5 |
| Médium perturbado | Não existe | |
| Água fluidificada não é cura automática | Bug | B7 |

### 4.5 NPCs e missões (§8, §9)
| Item | Status | Observação |
|---|---|---|
| Kardec, Chico, Haroldo (MCreator), Divaldo | Já existe + Visual | revisar falas de Chico (B10) |
| Dirigente, Médium, Orador, Passista, Evangelizadora | Já existe + Visual | |
| Mentor, Obsessor, Sofredor, Zombeteiro, Socorrista (translúcidos) | Já existe + Visual | translucidez nunca vista no jogo |
| Proteção contra agressão de benfeitores/trabalhadores | Já existe | testes `kardec_invulneravel`, `npc_trabalhador_invulneravel` |
| Diálogo contextual (dimensão, estado, histórico) | Parcial | sequência por jogador; não usa dimensão/estado |
| Arquitetura de tipos/rotinas/memória | Não existe | |
| Cadeias de missões (§9) | Parcial | só tutorial e faminto |
| Psicografia com textos reais, reuniões mediúnicas | Parcial | habilidade com frases fixas; caderno vazio; mesa só decorativa |
| Família espiritual, Evangelho no Lar | Não existe | |

### 4.6 Biblioteca e conteúdo (§10, §11)
| Item | Status | Observação |
|---|---|---|
| 8 obras legíveis (LE, LM, ESE, CI, Gênese, O que é, Revista, Nosso Lar) | Já existe | `itens_obras_tem_paginas` |
| Catálogo, sumário, busca, marcadores, notas, quizzes, glossário | Não existe | |
| `docs/fontes_e_licencas.md`, status de direitos | Não existe (nesta cópia) | área biblioteca |
| Citações com referência | Parcial | Obras.java declara regra; revisar Ensinamentos (q. 132/642 entre aspas) |
| Trilhas educativas (§11) | Parcial | palestras sobre leis morais; mirante |

### 4.7 Estruturas da Terra
| Item | Status | Observação |
|---|---|---|
| Centros P/M/G, casa_kardec, casa_chico, mansao, auditório, vilas grandes | Já existe + Visual | `mundo_todos_modelos_constroem` |
| Centro do tutorial perto do spawn | Já existe | flores quebram também (B1 efeito 4); construção síncrona |
| Proteção de estruturas históricas | Não existe | |

### 4.8 Qualidade, performance, multiplayer, acessibilidade, docs (§12-§17)
| Item | Status | Observação |
|---|---|---|
| Performance: geração incremental, limites por tick, telemetria | Não existe | B3/B4 |
| Spawn de NPC por proximidade / sem duplicar | Parcial | `colocarSeFaltar` checa raio 12; sofredores podem ir a 3 |
| Multiplayer: estado por jogador | Já existe | attachments por jogador; mapas estáticos por UUID |
| Multiplayer: testado com 2 jogadores | Não existe | |
| Um jogador forçar resgate do outro | Não existe (ok) | |
| Acessibilidade (efeitos, sons, câmera) | Não existe | |
| Ferramentas de debug §16 (estado, proteção, flores, órfãos) | Parcial | só ir/reencarnar/resgatar/sonho/moralidade/jornada |
| Texturas sem ícone padrão | Já existe + Visual | todas as referências de modelo/textura existem (verificado por script) |
| Docs §17 | Parcial | só os desta auditoria; README do repositório não auditado aqui |

---

## 5. Resultado dos testes (execução real)

`./gradlew -I testes/gametest.init.gradle runGameTestServer --offline` (ambiente do CONTRATO) →
**"All 31 required tests passed :)" — BUILD SUCCESSFUL em 2 min 6 s; 31 testes em 18,46 s.** Detalhes, tempos e
lacunas em `docs/TESTES_E_VALIDACAO.md`.

## 6. Prioridades sugeridas
1. B1 (flores) e B2 (proteção) — Fase 1 do PROMPT_MESTRE (área colonia).
2. B3/B4 (travamentos) — construção incremental e busca de local de reencarnação incremental/cacheada.
3. B5/B6 (consentimento, paciente/paciente, órfãos) — área umbral.
4. B9 — teste que carregue as dimensões de verdade (ou testar a lógica em Level de teste com `ResourceKey` simulado).
5. B7/B8 — revisão de balanceamento.
