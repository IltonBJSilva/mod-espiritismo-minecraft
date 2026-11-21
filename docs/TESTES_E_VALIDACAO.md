# Testes e validação — Mod Espiritismo Minecraft

Data: 2026-11-21. Cópia: `E:/claude-mod-trabalho/ws2_auditoria` (estado anterior às mudanças da Fase 4).

## 1. Como os testes rodam

- Tipo: **GameTests do NeoForge**, registrados só quando `GameTestHooks.isGametestEnabled()` (nunca em produção).
- Configuração: `testes/gametest.init.gradle` (fora do `mcreator.gradle`, que o MCreator apaga). Cria o run
  `gameTestServer` com `--tests mod_espiritismo_minecraft:*` e relatório `run-gametest/gametest-report.xml`.
- Ambiente (Git Bash, dentro da cópia):
  ```
  export JAVA_HOME="/c/Program Files/Pylo/MCreator/jdk"; export GRADLE_USER_HOME="$HOME/.mcreator/gradle"
  export TMP='E:\claude-mod-trabalho\tmp' TEMP='E:\claude-mod-trabalho\tmp'; export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=E:/claude-mod-trabalho/tmp"
  ./gradlew -I testes/gametest.init.gradle runGameTestServer --offline 2>&1 | grep -E "required tests|   - |BUILD"
  ```
- Não precisa de EULA. Roda com `Dist = DEDICATED_SERVER` (log: "FancyModLoader 11.0.15 (DEDICATED_SERVER in DEV)"),
  o que também serve de verificação de que o código comum não carrega classes de cliente.
- Não rode dois Gradle ao mesmo tempo na mesma cópia.

## 2. Resultado real desta auditoria

```
[14:57:10] GameTestServer: ========= 31 GAME TESTS COMPLETE IN 18.46 s ======
[14:57:10] GameTestServer: All 31 required tests passed :)
BUILD SUCCESSFUL in 2m 6s
```
Log completo: `E:/claude-mod-trabalho/tmp/auditoria_gametest.log` (21.617 linhas). Sem erros. 87 avisos
`ChunkAccess: Trying to mark a block for PostProcessing … not supported` durante os testes de `mundo` (peça de estrutura
colocada num `ServerLevel` fora do worldgen — cercas/escadas não se conectam; ver AUDITORIA B10).

### Tempos medidos (relatório XML) — baseline de performance
| Teste | Tempo | Leitura |
|---|---|---|
| `dim_construcoes_de_chegada` | **6,68 s** | Nosso Lar + Posto do Umbral, num único tick cada, em mundo **superplano** (log: Nosso Lar 14:57:03→14:57:07). Em terreno noise real deve ser pior (geração síncrona de chunks). |
| `mundo_centro_pequeno_construido` | 6,55 s | inclui esperar chunks ficarem ativos |
| `mundo_todos_modelos_constroem` | 4,62 s | 7 modelos |
| `dim_reencarnar_volta_a_terra` | **1,03 s** | busca de local a 600–1200 blocos (gera chunks na hora) — superplano |
| `centro_espirita_construido` | 0,84 s | Centro do tutorial (feito no 1º login) |
| demais 26 testes | < 0,3 s cada | |

## 3. Inventário dos 31 testes e o que cobrem

| Suite (arquivo) | Teste | Cobre |
|---|---|---|
| Núcleo (`teste/TestesEspirituais.java`) | `mod_carrega` | entidades MCreator, attachments, itens, comandos `/moralidade` `/espiritismo` `/centro`, conquistas, payloads |
| | `moralidade_padrao_e_limites` | início 50, limites 0/100, energia 0..100 |
| | `allan_kardec_nao_hostil` | Kardec não é Monster e não mira ninguém |
| | `escolher_classe_requer_kardec` | escolha só perto do NPC; não troca depois |
| | `habilidade_gasta_energia_e_recarga` | custo, recarga e energia insuficiente |
| | `matar_aldeao_reduz_moralidade` | −10 |
| | `kardec_invulneravel` | espada/lava não ferem; −5 por agressão |
| | `teto_diario_rotina` | +8/dia; `alterar` sem teto |
| | `centro_espirita_construido` | blocos-chave do Centro; NPCs só com chunk ativo, sem duplicar |
| | `caridade_aldeao_faminto` | prova é do jogador certo; +4; avanço da jornada; partilha |
| | `jornada_libera_classe` | etapas 0→5 não pulam |
| NPC (`npc/teste/TestesNpc.java`) | `npc_entidades_registradas` | 11 entidades + ovos + payload da prece |
| | `npc_trabalhador_invulneravel` | invulnerável, −5 com recarga, lava |
| | `npc_passe_remove_nocivo_e_recarga` | passe remove veneno, energia, recarga |
| | `npc_espada_nao_fere_obsessor` | Espírito não morre por arma |
| | `npc_esclarecer_obsessor_vira_sofredor` | obsessor → sofredor em resgate; +5 |
| | `npc_prece_gasta_energia` | custo, remove escuridão, recarga, gancho `AO_ORAR` |
| Itens (`itens/teste/TestesItens.java`) | `itens_registrados` | 4 blocos + itens + FACING/formas |
| | `itens_obras_tem_paginas` | 8 obras com autor, ≥4 páginas, cabem no atril |
| | `itens_receitas_carregadas` | receitas |
| | `itens_jarra_fluidificacao` | água comum sem prece; moral 30 não conta; prece a 20 blocos não vale |
| | `itens_doacoes` | valores de doação |
| Mundo (`mundo/teste/TestesMundo.java`) | `mundo_estruturas_registradas` | tipo/peça/estruturas/sets/tags |
| | `mundo_estilo_por_bioma` | estilo por bioma |
| | `mundo_centro_pequeno_construido` | porta e peça construída |
| | `mundo_todos_modelos_constroem` | 7 modelos constroem sem exceção |
| Dimensão (`dimensao/teste/TestesDimensao.java`) | `dim_planos_carregam` | JSON de dimension/type/biome/noise decodificam com o codec do jogo; attachments; comandos |
| | `dim_roteamento_moralidade` | funções puras: destino após a morte, sonho, resgate liberado |
| | `dim_construcoes_de_chegada` | Nosso Lar e Posto construídos **no Overworld de teste**: chegada livre, atris, placas, luz |
| | `dim_reencarnar_volta_a_terra` | reencarnar leva ao Overworld, moral conservada, distância > 300 |
| | `dim_umbral_resgate` | prece fora do Umbral não soma |

## 4. Limitações importantes dos testes atuais

1. **As dimensões Nosso Lar e Umbral NÃO existem no GameTestServer** (ele cria o mundo sem dimensões de datapack; o log
   mostra construções apenas em `minecraft:overworld`). Consequência: nos testes `dim_reencarnar_volta_a_terra` e
   `dim_umbral_resgate` os blocos `if (server.getLevel(Planos.NOSSO_LAR/UMBRAL) != null)` são **pulados**. Nunca foram
   exercitados por teste: teleporte a Nosso Lar/Umbral, `posicaoRenascimento` (morte → plano), resgate real (5 preces →
   socorrista → Nosso Lar), clique no atril, sonho, `verificarNpcs`. O "passou" desses testes não prova essas rotas.
2. Nenhum teste observa **drops/itens no chão** após construir Nosso Lar, nem `canSurvive` das plantas — por isso o bug
   das flores (AUDITORIA B1) passa despercebido. O teste de construção usa mundo superplano (sem relevo nem flores
   naturais), onde o bug não aparece.
3. Nenhum teste de **proteção** (não há proteção implementada).
4. Nenhum teste de **multiplayer** (dois jogadores), de reconexão ou de reinício do servidor (persistência de
   `plano_pontos`, `RESGATE_EM`, `SONHO_FIM`).
5. Nenhum teste de **performance com limite** (ticks/ms por tick). Os tempos acima são só observação.
6. Nenhum teste de **estados espirituais** (o contrato `Estados` não tem regras ainda).
7. Mock players (`makeMockPlayer`/`makeMockServerPlayerInLevel`) não passam por login real, pacotes de cliente, HUD,
   tela de classe nem teclas.
8. **Nenhuma validação visual** foi feita (o contrato proíbe `runClient` aos agentes). Tudo marcado "Visual" na matriz
   da auditoria continua pendente.

## 5. Lacunas → testes que faltam (mapeados ao PROMPT_MESTRE §15)

| §15 | Teste sugerido | Como viabilizar no GameTest |
|---|---|---|
| 15.1 Proteção | quebrar/colocar/explodir/fluido/pistão em área protegida; admin via comando | Chamar os handlers com `BlockEvent.BreakEvent`/`level.explode` num Level de teste, usando `ColoniaApi.protegido` parametrizável por região (não só por dimensão) |
| 15.2 Flores | construir a colônia sobre terreno com **relevo e flores** (montar colunas de terra + flores no teste) e afirmar: 0 `ItemEntity` na caixa, todas as plantas `canSurvive`; construir 2× não duplica | Gerar o relevo artificial no próprio teste antes de chamar a construção |
| 15.2 Chunks | descarregar/recarregar e reconferir plantas | `ServerChunkCache` + tickets, como `TestesMundo.soltar` |
| 15.3 Umbral/estados | perturbado não ataca NPC/jogador, não quebra/coloca; aceitar/rejeitar/aceitar socorro; socorrista removido | lógica de política testável sem a dimensão (funções puras + mock player) |
| 15.3 Órfãos | iniciar resgate de sofredor, descarregar o chunk no tick 100, recarregar: nenhum socorrista deve sobrar | ver AUDITORIA B6 |
| 15.4 Ajudar outros | `Estados.podeSocorrer` false em estados de cuidado | unitário |
| 15.5 Entrada/saída | rotas sem softlock | precisa das dimensões: ver item abaixo |
| 15.6 NPCs | `verificarNpcs` não duplica; sofredores ≤ 2 | chamar com pontos de um Level de teste |
| 15.7 Performance | construção incremental: nenhum tick > X ms; reencarnação sem gerar > N chunks por tick | medir `System.nanoTime()` por tick dentro do teste |

**Para testar com as dimensões de verdade:** o GameTestServer não carrega dimensões de datapack. Alternativas:
(a) projetar as funções de construção/proteção para receber qualquer `ServerLevel` (como já é feito em
`Planos.construir`) e testar no Overworld de teste; (b) rotas que dependem de `ResourceKey` da dimensão (morte → plano,
resgate) precisam de validação manual no cliente (roteiro em `COMO_TESTAR_NO_MINECRAFT.md`).

## 6. O que precisa de validação manual/visual (não feita)

- Aparência de Nosso Lar, Umbral (névoa, céu, luz do Posto), Centro, estruturas da Terra, texturas/ovos/skins.
- Translucidez dos Espíritos (`EspiritoRenderer`).
- HUD (moralidade, energia, objetivo, seta), tela de escolha de classe, teclas V e B.
- Fluxos com dimensões reais: morte com moral 80/45/10, resgate, reencarnação, sonho/pesadelo.
- Flores quebrando (reproduzir o bug antes de corrigir; confirmar depois que acabou).
- Travamentos: sentir/medir a primeira entrada em Nosso Lar e a reencarnação (F3 + `/debug start`/`stop` para ver TPS).
