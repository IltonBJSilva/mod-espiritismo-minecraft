# Biblioteca Espírita e conteúdo educativo

Interpretação educativa **não oficial**. Fontes e direitos: `docs/fontes_e_licencas.md`.

## 1. Como o jogador acessa

| Acesso | Como |
|---|---|
| Estante da Biblioteca Espírita (`mod_espiritismo_minecraft:estante_espirita`) | Bloco. Clicar com a mão vazia abre o catálogo. Feita para centros, casas e a Biblioteca central de Nosso Lar |
| Catálogo da Biblioteca Espírita (`catalogo_biblioteca`) | Item. Usar abre o catálogo em qualquer lugar |
| Obras-item (`o_livro_dos_espiritos` etc.) | Clicar abre o livro vanilla, como antes. **Agachar + clicar** abre a ficha da obra na Biblioteca |
| `/biblioteca` | Abre o catálogo. Variações: `/biblioteca obra <id>`, `/biblioteca trilha [modulo]`, `/biblioteca glossario`, `/biblioteca progresso`, `/biblioteca resetar` (só operador) |
| `/evangelho_no_lar` | Abre a aba do Evangelho no Lar. Variações: `iniciar`, `participar`, `sair` |
| API | `BibliotecaApi.abrir(ServerPlayer, String obraId)`, `abrirCatalogo`, `abrirTrilha`, `abrirGlossario`, `abrirEvangelhoNoLar` |

## 2. Tela (GUI cliente, `biblioteca.client.TelaBiblioteca`)

- **Catálogo:** agrupa por Autor, Série ou Assunto (ou mostra todas) e tem busca de termo nos títulos e textos. Obras protegidas aparecem com "©". Uma legenda explica os tipos de texto e o status de direitos.
- **Ficha da obra:** autoria (com o psicógrafo, quando houver), ano, série, assuntos, **status de direitos e justificativa**, edição de referência, tradução ou fonte, contexto histórico e editorial, perguntas de reflexão, "aprofunde", progresso, **sumário navegável** (✔ = lida, ★ = marcador), botão "Continuar" (vai ao marcador), quiz e **nota pessoal** salva.
- **Seção:** título, rótulo colorido do tipo (*Trecho da obra* / *Resumo do mod* / *Adaptação do mod* / *Nota editorial*), texto, referência, status de direitos, Anterior/Próxima e "Marcar página". Abrir uma seção registra a leitura e move o marcador.
- **Quiz opcional:** múltipla escolha com explicação e fonte. Errar não tem penalidade, e cada acerto conta uma única vez.
- **Trilhas, Glossário, Evangelho no Lar, Progresso:** descritos abaixo.

## 3. Catálogo (21 obras)

Séries: Obras básicas (5); Outras obras de Allan Kardec (O que é o Espiritismo, Revista Espírita, Obras Póstumas, Viagem Espírita em 1862); Continuadores (Léon Denis, *Depois da Morte*); A Vida no Mundo Espiritual (André Luiz: Nosso Lar e mais 7 volumes); Obras de Emmanuel (3).

- As 8 obras que já existiam como item (`itens.Obras`) **não foram duplicadas**. `Catalogo` importa as seções delas e classifica cada uma: com aspas vira *Trecho da obra*; a primeira seção vira *Nota*; o restante vira *Resumo*. A ficha, o contexto e as seções extras ficam no próprio `Catalogo`, como o original francês, o mapa das questões do LE e a análise de Nosso Lar.
- Obras protegidas não têm nenhuma seção do tipo *Trecho da obra*. Um GameTest garante isso.
- Para acrescentar obras, use `obra("id").t(...).assuntos(...).edicao(...).direitos(...).contexto(...).sec(...).quiz(...).fim()` no bloco static de `Catalogo`, ou os atalhos `andre(...)` e `emmanuel(...)`. Registre a obra também em `fontes_e_licencas.md`.

## 4. Trilhas de aprendizagem (PROMPT_MESTRE §11 e §9)

Cada módulo tem: situação jogável, explicação breve, fonte, exemplo, pergunta de reflexão (não é prova), aprofundamento (obra do catálogo) e, opcionalmente, um quiz.

- **Fundamentos (13):** espírito, perispírito e corpo; reencarnação e esquecimento; livre-arbítrio; causa e efeito (sem punição simplista, sofrimento não é prova de maldade); escala espírita; pluralidade dos mundos; mediunidade (gratuita); obsessão e desobsessão (prece, reforma íntima, esclarecimento; **a água fluidificada é apoio, não cura**; cuidado médico); prece; caridade material e moral; perdão; sono e emancipação; desencarnação e perturbação.
- **Discernimento mediúnico (5):** percepção, interpretação e certeza; nem toda mensagem deve ser seguida; estudo e disciplina; atividade mediúnica com cuidado; procurar orientação quando perturbado.
- **Caridade cotidiana (4):** ajudar sem buscar recompensa; acolher sem humilhar; respeitar limites ("paciente não ajuda paciente" é regra do jogo); a ajuda material e a moral se completam.
- **Situações jogáveis com sugestão automática:** o jogo manda uma linha no chat com o link `[abrir]`, só uma vez por jogador. Os gatilhos são: primeiro login (espírito/perispírito), acordar depois de dormir (sono), renascer (desencarnação), entrar no Umbral (livre-arbítrio), entrar em Nosso Lar (desencarnação) e a primeira prece com a tecla B (prece, pelo gancho `npc.Prece.AO_ORAR`).

## 5. Glossário (25 termos)

Cada termo traz definição autoral, referência e **origem**: obras básicas, obra mediúnica posterior (ex.: Umbral, colônia), prática do movimento espírita (desobsessão, passe, água fluidificada, reforma íntima, Evangelho no Lar) ou termo do jogo (moralidade). Isso atende ao §2.2: não apresentar o que vem de Nosso Lar como se fosse de O Livro dos Espíritos.

## 6. Evangelho no Lar (§12.4)

- É opcional e funciona sozinho ou em grupo. Quem inicia convida os jogadores a até 12 blocos por uma mensagem clicável `[participar]`. Ninguém entra sem aceitar, e quem sai no meio não sofre penalidade.
- São 4 etapas de cerca de 15 s: prece inicial com palavras próprias → leitura (resumo do mod de um trecho do ESE, com referência e o convite "leia o texto completo no seu exemplar") → conversa com uma pergunta → prece final. A água aparece só como "apoio, não remédio nem proteção".
- Ao terminar, o jogo registra `evangelhos+1` no **progresso de conhecimento** e concede a conquista `jornada/evangelho_no_lar`. **Não altera a moralidade, não dá efeitos nem proteção.** A sessão termina se o anfitrião sair ou desconectar, e quem se afasta mais de 18 blocos sai dela.

## 7. Progresso e conhecimento (§7: conhecimento ≠ moralidade)

- O attachment `mod_espiritismo_minecraft:biblioteca_progresso` (registro `ProgressoBiblioteca`, com versão de esquema 1) guarda seções lidas, marcadores, notas (até 256 caracteres), acertos, tentativas, módulos, sugestões já feitas e Evangelho no Lar. É salvo no jogador, copiado na morte e sincronizado só com o dono. Saves antigos carregam como vazio.
- Conhecimento = 1 por seção lida + 2 por acerto + 3 por módulo + 2 por Evangelho no Lar. Não há ranking. **Ler, responder quizzes e estudar módulos não muda a moralidade.** A única exceção é o ganho de rotina que já existia ao abrir um livro-item.
- Conquistas novas (`jornada/`): `biblioteca_leitor`, `biblioteca_trilha`, `biblioteca_trilha_completa`, `evangelho_no_lar`.

## 8. Ainda não feito / ideias

- Texto integral em português: depende de uma fonte de domínio público comprovada (ver fontes_e_licencas §6).
- Volumes e artigos da Revista Espírita: por enquanto só o guia dos anos.
- Busca na tela: procura apenas nas obras, não no glossário nem nas trilhas.
- NPC bibliotecário (área população) e a Biblioteca central de Nosso Lar (área colônia) devem chamar `BibliotecaApi`. Ver INTEGRACAO.md.
