# Umbral 2.0 — Cenários de teste

Agente **pesquisa** (papel de QA conceitual). Os testes complementam §26.11 do PROMPT e os 66 GameTests já
existentes. A coluna “Tipo” indica: **GT** = GameTest automatizável; **MAN** = teste manual no cliente; **REV**
= revisão de conteúdo (texto, imagem, som).

Convenções: “posto” inclui o Mirante da Legião; números referem-se a `UMBRAL_2_0_GAMEPLAY_E_NECESSIDADES.md` (GP).

## 1. Necessidades sem morte

| ID | Cenário | Passos | Esperado | Tipo |
|---|---|---|---|---|
| N1 | Necessidade no máximo não mata | Pôr sede, fome e frio em 100 por comando; esperar 30 min de ticks acelerados | Vida inalterada; valores travados no piso (85/85/80/90); nenhum respawn | GT |
| N2 | Fome vanilla congelada | Desencarnado no Umbral por 20 min | A barra de fome vanilla não muda | GT |
| N3 | Taxas por região | Jogador parado em R1 e depois em R6 | A sede sobe mais rápido em R1 (×1,5) do que no padrão | GT |
| N4 | Moral só altera velocidade | Moral 0 e moral 100 no mesmo cenário | As duas sobem; razão ≈ 1,2/0,8; nenhuma zera | GT |
| N5 | Fonte Turva tem teto | Sede 80 → beber 3 vezes | Desce até 40 e não passa disso; recarga de 3 min | GT |
| N6 | Recuperação no posto | Entrar no escudo com sede 85 | As taxas param; a água leva a sede a ~45 em 20 s (gradual, não instantâneo) | GT |
| N7 | Máximo de 2 efeitos | Exaustão, perturbação, frio e sede ≥ 70 ao mesmo tempo | Só exaustão e perturbação ativas no cliente | MAN |
| N8 | Acessibilidade | `efeitos_perturbacao=desligado` | Sem desfoque e sem tremor; ícones continuam; legendas presentes | MAN |
| N9 | Água fluidificada não é cura | Usar água fluidificada fora do posto | Só multiplicador ×1,5 por 60 s; nada é zerado | GT |

## 2. Recusa de ajuda e livre-arbítrio

| ID | Cenário | Esperado | Tipo |
|---|---|---|---|
| R1 | Recusa simples | Socorristas se despedem e saem andando; recarga de 5 min; perturbação +5 (teto 90); **sem** teleporte; lanterna azul aparece | GT |
| R2 | Nova oferta só depois da recarga | Nenhuma abordagem por NPC antes de 5 min, mesmo com necessidades críticas | GT |
| R3 | Recusa repetida (3 vezes em 30 min) | Recarga passa a 10 min; as lanternas continuam; nenhuma perseguição | GT |
| R4 | Pedido ativo depois de recusar | Tecla H funciona imediatamente, ignorando a recarga | GT |
| R5 | Incapacidade temporária | Perturbação 85 + sede 85 → o socorrista senta e espera 30 s; a opção “Só fique aqui comigo” aparece | GT + MAN |
| R6 | Recusar a caravana do Vale | A caravana parte; a mensagem diz que ela volta; o próximo ciclo chega em 12 min; o Mirante fica marcado | GT |
| R7 | “Prefiro não ir” no aviso do Vale | A missão fica pendente sem penalidade; moral e perturbação inalteradas | GT |
| R8 | Não há perseguição infinita | Recusar 10 vezes em 2 h | Nunca mais de 1 equipe por vez; nenhum NPC segue o jogador por mais de 40 blocos depois da recusa | GT |
| R9 | Recusa não reduz moralidade | Comparar a moral antes e depois de R1–R8 | Igual | GT |

## 3. Jogador perdido / softlock

| ID | Cenário | Esperado | Tipo |
|---|---|---|---|
| P1 | Perdido por tempo | 8 min sem aproximar-se de auxílio | Lanterna azul a 30–40 blocos na direção certa; recarga de 4 min | GT |
| P2 | Caminho circular (R2) | Passar 3 vezes pela mesma praça | Oferta discreta + rua de flores pálidas destacada | GT |
| P3 | Preso fisicamente | Cercar o jogador com blocos não quebráveis no Vale | Em até 60 s surge uma passagem; se falhar, teleporte seguro ao Mirante com mensagem narrativa | GT |
| P4 | Sem caminho até um posto | Para 50 pontos aleatórios do Vale, calcular o caminho até o Mirante | 100% com caminho ≤ 400 blocos de trilha | GT |
| P5 | Morte vanilla no Vale | Matar o jogador por comando dentro do Vale | Desperta no Mirante (não no fundo); estado RECEM_DESENCARNADO; nenhum loop | GT |
| P6 | Desconectar no meio do resgate | Sair durante o EM_RESGATE e voltar | Aparece no posto mais próximo; nenhum socorrista órfão | GT |
| P7 | Desconectar dentro do comboio | Sair durante a transição | Ao voltar, está na Colônia ou na Praça, nunca no void; usa TeleporteSeguro | GT |
| P8 | Bússola no Vale | Abrir a bússola espiritual | Oscila, mas aponta o Mirante com erro ≤ 30° quando a perturbação é < 50 | MAN |
| P9 | Jogador sem mapa e sem itens | Entrar no Vale sem nada | Consegue sair seguindo luzes e sinais, sem item obrigatório | MAN |

## 4. Ciclo completo da região-piloto (entrada → saída)

| ID | Cenário | Esperado | Tipo |
|---|---|---|---|
| C1 | Entrada com aviso | A tela de aviso com CVV 188 aparece na 1ª entrada e pode ser pulada nas seguintes; está também no menu de pausa | GT + REV |
| C2 | Missão “Aprendiz da caravana” | Todos os 8 passos concluídos; diário atualizado; painel educativo aberto | MAN |
| C3 | Caravana e voz | Sino 60 s antes; legenda da voz; permanência de 3 min; parte sem levar entidades sobrando | GT |
| C4 | Hospital | Banho → caldo → lareira → leito → sono → conversa; a perturbação cai; o frio zera | GT |
| C5 | Alta com três opções | As três opções aparecem; “voltar acompanhado” e “ficar como aprendiz” não exigem moral | GT |
| C6 | Encaminhamento a Nosso Lar | Só com `saidaAssistidaLiberada`; vai para as Câmaras de Retificação | GT |
| C7 | Vale desativado no servidor | `vale_habilitado=false` | A descida não leva ao Vale; nenhuma missão o menciona; nada quebra | GT |
| C8 | Vale ocultado pelo jogador | Opção de perfil ativada | Para esse jogador, a missão não é oferecida e o aviso de entrada bloqueia a descida | GT |

## 5. Geração, entidades e persistência

| ID | Cenário | Esperado | Tipo |
|---|---|---|---|
| G1 | Determinismo | Mesma seed → mesmas posições de regiões, mirante e refúgios numerados | GT |
| G2 | Limites de entidades | Lotar o Vale | ≤ 14 NPCs; ≤ 1 caravana por dimensão; ≤ 2 equipes por jogador | GT |
| G3 | Chunks descarregados | Afastar-se com a caravana em curso | A caravana pausa ou é despawnada com segurança; reaparece no ciclo seguinte; nenhuma carga forçada de chunk | GT |
| G4 | Reinício do servidor | Reiniciar com o jogador no Vale, no comboio e no hospital | Necessidades, convalescença, recargas e `vale_aviso_visto` preservados | GT |
| G5 | Escudo persistente | Reiniciar e verificar o escudo de postos e Mirante | Taxas param dentro do raio; obsessores não entram | GT |
| G6 | Abrigo Transitório móvel | Ciclo longo | Muda entre 3 pontos fixos; nunca no meio de um jogador; ninguém fica preso dentro | GT |
| G7 | Sem loops de teleporte | Entrar e sair do escudo repetidamente | Nenhum teleporte automático disparado | GT |

## 6. Mediunidade e multiplayer

| ID | Cenário | Esperado | Tipo |
|---|---|---|---|
| M1 | Encarnado sem mediunidade | Não vê nem ouve NPCs do Umbral (se houver vazamento para a Terra) | GT |
| M2 | Encarnado médium vidente | Percebe conforme `Mediunidade.percebe` | GT |
| M3 | Dois jogadores no Vale | Um aceita e o outro recusa: cada um tem sua recarga; a caravana leva só quem aceitou; o acompanhado tem risco −1 | GT |
| M4 | Jogador ajudando jogador | Um jogador orientado “senta ao lado” de outro perturbado | A perturbação do outro cai como na interação com o socorrista; não há bônus de moral pela quantidade de ajudas (evitar farm) | GT |

## 7. Revisão de conteúdo (obrigatória antes do release)

| ID | Verificação | Critério de aprovação | Tipo |
|---|---|---|---|
| E1 | Varredura de termos | Grep no lang/diálogos por: covarde, condenad, castigo, merece, réprob, escória, presídio, “cometeu suicídio”, arma, corda, veneno, sangue, “suicida” (como rótulo de NPC) | 0 ocorrências fora do painel bibliográfico | REV |
| E2 | Nenhum método ou ferimento | Revisar texturas, modelos, partículas e sons do Vale e do hospital | Nada sugere método, lesão, corpo ou decomposição | REV |
| E3 | Esperança em toda cena | Cada diálogo do Vale termina com presença, próximo passo ou continuidade | 100% | REV |
| E4 | Painéis com 4 blocos | Inspiração / descrição / adaptação / reflexão + CVV no Vale | 100% | REV |
| E5 | Sem citação longa de obra protegida | Comparar textos com os livros | Nenhuma frase copiada; só paráfrase e referência de capítulo | REV |
| E6 | Títulos de capítulos conferidos | Títulos PT conferidos na edição FEB (a pesquisa usou tradução espanhola) | Conferido | REV |
| E7 | Nenhuma pessoa real associada | Nomes de NPC fictícios; Camilo/Yvonne só no painel bibliográfico | OK | REV |
| E8 | Humilhação e softlock | QA joga 1 h recusando tudo | Sempre há saída; nenhuma mensagem humilhante | MAN |
