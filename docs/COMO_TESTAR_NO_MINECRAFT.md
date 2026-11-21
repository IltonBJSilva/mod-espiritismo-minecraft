# Como testar no Minecraft — Mod Espiritismo

Guia para o mantenedor validar o mod no jogo de verdade (o que os agentes não podem fazer: o contrato proíbe `runClient`
e aceitar EULA). Escrito sobre o estado atual (antes da Fase 4); onde há bug conhecido, o resultado **esperado hoje**
está marcado com ⚠ e aponta para `docs/AUDITORIA_ESTADO_ATUAL.md`.

---

## 1. Preparar o workspace no MCreator

1. **Se o MCreator estiver aberto enquanto alguém (integrador/agentes) mexeu nos arquivos do workspace:** feche o
   MCreator **sem salvar** (ele guarda em memória a versão antiga dos elementos e a gravaria por cima). Reabra depois.
2. Abra o MCreator e o workspace `C:\Users\ilton\MCreatorWorkspaces\mod_espiritismo_minecraft`. Espere a configuração do
   Gradle terminar (barra de status embaixo).
   - Observação: ao abrir, o MCreator apaga o `mcreator.gradle`. Isso é normal; os GameTests usam
     `testes/gametest.init.gradle` e não dependem dele.
3. Faça o **Build** do workspace (botão de build/martelo na barra superior). Espere "Build successful".
4. **Depois de todo Build**, recrie as texturas que o MCreator sobrescreve (o Build recria os ovos com a textura padrão).
   Num terminal na pasta do workspace, nesta ordem:
   ```
   python arte_fonte/gerar_texturas.py
   python arte_fonte/itens_texturas.py
   ```
   - A ordem importa: `itens_texturas.py` regrava `textures/item/livro_dos_espiritos.png` no estilo 1857 por cima do que
     `gerar_texturas.py` grava.
   - `python arte_fonte/npc_texturas.py` só é necessário se as skins/ovos dos NPCs (`textures/npc/`, `textures/item/npc/`)
     sumirem — o MCreator não mexe nelas.
   - Se o jogo já estiver aberto quando rodar os scripts, aperte **F3+T** para recarregar os recursos.
5. Rode o cliente pelo MCreator (botão **Run client** / ▶). Na primeira vez demora mais.

## 2. Criar um mundo novo para teste

Sempre teste num **mundo novo** (as construções de Nosso Lar/Umbral e o Centro são feitos uma vez por mundo e ficam
salvos; um mundo antigo esconde bugs de geração).

1. Um jogador → Criar novo mundo.
2. Modo **Sobrevivência** (para testar quebra/coleta/moralidade de verdade) e **Permitir comandos: SIM** (para os comandos
   de teste). Tipo de mundo: Padrão. Anote a seed.
3. Opcional, um segundo mundo em Criativo para olhar construções voando.
4. Para medir travamentos: `/tick query` (mostra o tempo médio por tick) antes e logo depois da ação; ou abra o F3 e
   observe o gráfico de tempo de tick. Anote o tempo de "congelamento" percebido.

## 3. Comandos úteis

Do jogador comum:
| Comando / tecla | Função |
|---|---|
| `/moralidade` | mostra moralidade e faixa |
| `/centro` | coordenadas do Centro do tutorial |
| `/espiritismo_prece` | prece (no Umbral soma arrependimento) |
| tecla **V** | habilidade da classe |
| tecla **B** | prece (custa 15 de energia, recarga 30 s) |

De operador (precisa de "Permitir comandos"):
| Comando | Função |
|---|---|
| `/moralidade definir <0-100>` | define a moralidade |
| `/espiritismo jornada <0-5>` | pula etapas do tutorial (3 = caridade, cria um aldeão faminto) |
| `/espiritismo resetar_classe` · `/espiritismo energia_cheia` | classe / energia e recargas |
| `/espiritismo construir_centro` | constrói um Centro na sua frente |
| `/espiritismo_plano ir nosso_lar` · `ir umbral` · `ir terra` | viagem direta (constrói o plano na 1ª vez) |
| `/espiritismo_plano reencarnar` · `resgatar` | atalhos |
| `/espiritismo_plano sonho desdobramento` · `pesadelo` · `acordar` | sono / emancipação da alma |
| `/locate structure mod_espiritismo_minecraft:casa_kardec` (também `casa_chico`, `mansao_do_caminho`, `auditorio_haroldo`, `centro_pequeno`, `centro_medio`, `centro_grande`, `vila_grande`) | achar estruturas |
| `/give @s mod_espiritismo_minecraft:<id>` | itens: `livrodos_espiritos`, `agua_fluidificada`, `bussola_espiritual`, `o_livro_dos_espiritos`, `o_livro_dos_mediuns`, `o_evangelho_segundo_o_espiritismo`, `o_ceu_e_o_inferno`, `a_genese`, `o_que_e_o_espiritismo`, `revista_espirita`, `nosso_lar`, `caderno_psicografia`, `sopa_fraterna`, `agasalho`, `cesta_basica`, `livro_1857`, `mesa_mediunica`, `tribuna`, `jarra_agua`, ovos `<npc>_spawn_egg` |

---

## 4. Roteiro de verificação visual (passo a passo)

Para cada passo: faça a ação, compare com "Esperado" e anote OK / problema (com print e coordenadas — F3).

### 4.1 Primeiro login, HUD e Centro do tutorial
1. Entre no mundo novo. **Esperado:** mensagem "— Uma nova encarnação —", recebe a Bússola Espiritual; HUD no canto
   superior esquerdo com "Moralidade: Neutra" e barra cinza; objetivo "Visite o Centro Espírita…"; seta com distância.
2. Note se houve congelamento ao entrar (o Centro é construído nesse momento). Anote o tempo.
3. Siga a seta até o Centro. **Esperado:** prédio 13×11 com placa "Centro Espírita / Allan Kardec", porta de abeto, bancos,
   livro 1857 na mesa, velas, estantes, jarra d'água; Dirigente, Passista e Evangelizadora dentro.
   ⚠ Olhe o chão em volta: flores/grama do terreno podem ter virado itens (mesma causa do B1).
4. Visual: skins dos 3 NPCs, se olham para o jogador, se não atravessam paredes.

### 4.2 Tutorial (Jornada) e classes
1. Fale com o Dirigente → recebe O Livro dos Espíritos. Leia (botão direito) → questão no chat; objetivo avança.
2. Fale com o Passista → passe + água fluidificada. Partículas e sons.
3. Objetivo "Dê comida a um aldeão faminto": siga a fumaça acima do aldeão; dê pão (botão direito com comida).
4. Volte ao Dirigente → abre a **tela de escolha de classe** (3 botões com dica ao passar o mouse). Escolha uma.
5. Aperte **V**: Médium (psicografia no chat), Doutrinador (partículas, pacifica monstros), Trabalhador (resistência/
   regeneração em quem está perto). Médium agachado: monstros a 10 blocos brilham.
6. Aperte **B** (prece): mensagem, partículas, gasta 15 de energia; repetir antes de 30 s mostra a recarga.
7. Confira as conquistas (tecla L): árvore "Jornada Espiritual".

### 4.3 Moralidade e efeitos
1. `/moralidade definir 85` → HUD "Elevada" azul-claro; com pouca vida, regeneração leve a cada 10 s.
2. `/moralidade definir 15` → HUD "Perturbada" roxa; de tempos em tempos sussurros + lentidão/cansaço/escuridão.
   À noite podem surgir obsessor (fumaça) e zombeteiro (pode jogar 1 item comum da mochila no chão).
3. Ataque um obsessor com espada → não morre, mensagem sugere a prece. Aperte B perto dele → vira sofredor, socorrista
   aparece e os dois somem em ~7 s.
4. Visual: **translucidez dos Espíritos** (mentor/socorrista claros, obsessor escuro), sem sumir no fundo.
5. Beba água fluidificada com escuridão ativa. ⚠ Hoje remove **todos** os efeitos nocivos (B7).
6. Teto diário: com `/moralidade definir 50`, faça muitas ações de rotina (orar, ler, negociar) — não deve passar de 58 no
   mesmo dia de jogo.

### 4.4 Estruturas da Terra e bússola
1. Use a bússola (botão direito): aponta o Centro mais próximo. Agachado: locais especiais. Anote se o jogo congela ao
   usar agachado (busca em raio grande — B4).
2. `/locate` + `/tp` para cada estrutura: casa_kardec (estilo Paris, Kardec dentro), casa_chico (estilo mineiro),
   mansao_do_caminho (Divaldo), auditorio_haroldo (Haroldo), centros P/M/G, vila grande. Verifique telhados, portas,
   cercas/vidraças conectadas, NPCs dentro e sem duplicar ao sair e voltar.

### 4.5 Nosso Lar — chegada pela morte (fluxo principal)
1. Em Sobrevivência, `/moralidade definir 80`, guarde algumas flores/blocos no inventário e `/kill`.
2. **Meça** o tempo entre clicar "Renascer" e aparecer (a colônia é construída nesse instante — B3).
3. **Esperado:** desperta nas Câmaras de Retificação (camas brancas), mensagens sobre a perturbação passageira,
   conquista "Nosso Lar"; céu azul claro, sem chuva, tempo fixo.
4. Saia do prédio (porta ao sul) para a praça com fonte, bancos, postes de luz do mar, avenidas de calcita, 6 ministérios
   com placas, Mirante a nordeste e bosque a oeste.
5. ⚠ **Bug das flores (B1):** olhe o chão e o inventário/barra — esperado HOJE: flores silvestres, dentes-de-leão e
   sementes caídos/coletados; folhas soltas caindo por alguns minutos. Depois da correção: **nenhum item no chão**.
   Tire print do inventário logo ao chegar e 3 min depois.
6. ⚠ **Buraco:** a colônia fica num quadrado rebaixado com paredes de terra (pequeno.png). Suba no Mirante e fotografe.
7. Mirante: clique no atril → texto da escala dos mundos e da escala espírita.
8. NPCs: mentor no Mirante, socorrista nas Câmaras, orador na praça (aparecem em até ~10 s com você por perto).
9. ⚠ **Proteção (B2):** tente quebrar uma flor, a parede de um ministério e o atril; coloque um bloco. Esperado HOJE:
   tudo é permitido (sem proteção; o atril volta sozinho em ~10 s). Depois da Fase 1: nada pode ser alterado, com
   mensagem amigável.
10. Durma numa cama: mensagem "Em Nosso Lar o repouso é outro…" (a cama não explode).

### 4.6 Reencarnação (saída de Nosso Lar)
1. Ministério do Auxílio (oeste da praça): clique no atril → explicação; clique de novo em até 10 s.
2. **Meça** o congelamento (busca de local seguro a 600–1200 blocos — B4).
3. **Esperado:** volta à Terra longe do local anterior, cegueira breve, mensagens sobre esquecimento do passado e a
   "prova" sorteada (fome, metade da vida, fraqueza ou lentidão); conquista "Nascer, Morrer, Renascer".
4. Verifique que não caiu em água/lava/dentro de bloco.

### 4.7 Umbral e resgate
1. `/moralidade definir 10` e `/kill`. **Esperado:** desperta no Umbral: escuro, névoa curta, cinzas no ar, chão de
   lama, árvores secas; luz do Posto de Socorro ao norte (~30 blocos); sofredores perto.
2. ⚠ Olhe o chão: arbustos secos quebrados podem ter virado gravetos (B1, mesma causa).
3. Fale com um sofredor (botão direito) e ore (B ou `/espiritismo_prece`) — mensagens de esperança.
4. Após 5 pontos de arrependimento (≈ 5 preces espaçadas de 30 s, ou sofredores), **esperado HOJE:** um socorrista
   aparece e em 5 s você é levado às Câmaras de Nosso Lar **sem perguntar** (⚠ B5 — a Fase 4 deve trocar por escolha).
5. Ao voltar ao Umbral depois (`/espiritismo_plano ir umbral`), procure socorristas parados perto da chegada que não
   somem (⚠ B6 — órfãos). Anote quantos.
6. Visual: Posto de Socorro 9×9 com camas, lanternas, caldeirão, placa "Posto de Socorro / Entre e descanse".
7. Acessibilidade: avalie se escuridão/náusea/névoa incomodam (ainda não há opção para reduzir).

### 4.8 Sono: desdobramento e pesadelo
1. Na Terra, `/moralidade definir 80`, durma a noite inteira (pode precisar repetir: 30% de chance) ou use
   `/espiritismo_plano sonho desdobramento`. **Esperado:** acorda em Nosso Lar, na praça, por 90 s; o atril do Auxílio
   recusa ("Em sonho não se planeja…"); volta à cama sozinho.
   ⚠ Você chega com o inventário inteiro — teste se consegue quebrar/colocar blocos (B2).
2. `/moralidade definir 10` + `/espiritismo_plano sonho pesadelo`: Umbral por 25 s, depois cama.
3. Saia do jogo no meio de um sonho e entre de novo: deve acordar na cama.

### 4.9 Itens e blocos
1. Coloque `livro_1857`, `mesa_mediunica`, `tribuna`, `jarra_agua`: viram para o jogador; clique com a mão vazia → dica
   educativa no chat.
2. Jarra: garrafa vazia sem prece = água comum; agache + mão vazia (moral ≥ 50) = prece; garrafa de novo = água
   fluidificada. ⚠ No Centro do tutorial a jarra está a 2 blocos do Passista e sempre dá água fluidificada (B7).
3. Abra cada obra (botão direito) e no atril: páginas legíveis, autor, referências.
4. Doações: dê sopa/cesta/agasalho a um aldeão agachado.

### 4.10 Multiplayer (LAN)
1. Abra o mundo para LAN e entre com uma segunda conta (ou peça a alguém).
2. Confira: cada um vê só a própria moralidade no HUD; o aldeão faminto de um não dá a recompensa grande ao outro;
   morrer com moralidades diferentes leva cada um ao seu plano; NPCs de Nosso Lar não duplicam com os dois por perto;
   um não consegue forçar o resgate/reencarnação do outro.

### 4.11 Servidor dedicado (opcional, quando o mantenedor quiser)
- Gere o jar (`./gradlew build`), coloque num servidor NeoForge 26.1.2.95 limpo, aceite a EULA você mesmo e entre com o
  cliente. Esperado: o servidor sobe sem erro de classe de cliente (o GameTest já roda em modo dedicado e passou).

---

## 5. Como relatar
Para cada problema: seção do roteiro, print (com F3 visível para coordenadas), seed do mundo, o que esperava, o que
aconteceu e o log (`run/logs/latest.log` do MCreator). Para travamentos: o tempo percebido e o resultado de `/tick query`.
