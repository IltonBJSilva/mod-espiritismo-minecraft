
---

# Anexo: auditoria de Nosso Lar (agente colônia)

# Auditoria — parte de Nosso Lar (área "colonia")

> Seção para ser juntada ao `docs/AUDITORIA_ESTADO_ATUAL.md` do agente de auditoria.

## Bug: flores e plantas que quebravam e viravam itens ao chegar em Nosso Lar

**Sintoma** (prints `referencias/Screenshot_2.png` e `pequeno.png`): na primeira chegada a barra do jogador tinha dezenas
de flores silvestres (wildflowers), dentes-de-leão, sementes de trigo (da grama baixa) e uma placa; a colônia ficava
num "buraco" com paredes de terra.

**Causa exata** (`dimensao/ConstrucoesPlanos.java`, versão anterior):

1. `acertarColuna` limpava cada coluna **de baixo para cima** (`for yy = y+1 .. alto`): o bloco de terra/grama era
   removido **antes** da flor que estava em cima dele.
2. Todas as escritas usavam `FLAGS = Block.UPDATE_CLIENTS` (2), **sem** `UPDATE_KNOWN_SHAPE` (16). Sem essa flag,
   `Level.markAndNotifyBlock` chama `updateNeighbourShapes` nos vizinhos; a flor (e a metade de cima de girassóis /
   grama alta) recebe `updateShape`, vê que `canSurvive` é falso, vira ar e `Block.updateOrDestroy` chama
   `level.destroyBlock(pos, drop = true)`. Detalhe: mesmo passando `UPDATE_SUPPRESS_DROPS` (32) isso não bastaria,
   porque o vanilla tira o bit 32 das flags repassadas aos vizinhos (`updateFlags & -34`); é preciso o 16.
3. O mesmo mecanismo derrubava plantas quando o piso de um prédio substituía a grama sob elas, e deixava folhas de
   árvores cortadas pela borda da área sem tronco: elas "secavam" por minutos (random tick) e soltavam mudas/maçãs.
4. A área plana tinha raio fixo de 46 blocos na altura do centro, sem transição: onde o terreno natural era mais alto
   ficavam paredes verticais de terra (o "buraco").

**Correção na raiz** (`colonia/Lote.java`, usada por toda a nova construção):

* Flags `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE | UPDATE_SUPPRESS_DROPS | UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS`: nenhuma
  cascata de atualização, nenhum item solto, nenhum conteúdo de bloco-entidade derrubado.
* Colunas limpas **de cima para baixo** (`Lote.acertarColuna`): a planta sai antes do suporte.
* Plantas, lanternas e vasos entram por `Lote.planta`, que só coloca se o lugar estiver livre e `canSurvive` for
  verdadeiro **no momento** em que a operação roda — sempre depois do chão final (o paisagismo é a última fase).
* Blocos que se conectam (cercas, vidraças, muros, escadas) têm a forma recalculada no fim de cada peça, sem cascata.
* Folhas na faixa de margem ficam `persistent=true` (não secam).
* Terreno plano até r=164 e talude suave até r=196 (altura natural do gerador, `getBaseHeight`), sem paredes.
* As utilidades compartilhadas de `ConstrucoesPlanos` (`set`, `acertarColuna`, usadas pelo Umbral) receberam a mesma
  correção (flags e ordem de cima para baixo).
* O mesmo padrão existe no tutorial `CentroEspirita.java` (linhas ~118–121; núcleo, não editado) — correção descrita
  no `INTEGRACAO.md`.

**Regressão coberta por GameTest**: `col_cidade_flores_sobrevivem` monta morros com flores, girassóis, grama baixa e uma
árvore com folhas não persistentes cortada pela borda do talude; constrói a cidade inteira (com reinício simulado no
meio) e exige zero `ItemEntity` surgidos na área (um vigia conta todo item que entra no mundo, mesmo que o chunk
descarregue), todas as ~9.300 plantas colocadas ainda no lugar, folhas da margem persistentes e talude sem degraus > 2.
Para provar que o teste pega o bug, voltei temporariamente as flags para `UPDATE_CLIENTS` e a limpeza para baixo→cima:
o teste falhou com 775 itens soltos (girassóis, dentes-de-leão, sementes de trigo — o mesmo padrão do print).
`col_essenciais_rapidas_sem_itens` faz o mesmo só para a zona de chegada (37 itens com o código antigo, 0 com o novo).

## Desempenho da primeira chegada

* Antes: Nosso Lar + Posto num único tick, dentro do evento de renascimento: ~6,7 s (medido pela auditoria).
* Agora: só a zona de chegada (Câmaras de Retificação) é síncrona — 0,2 a 1,5 s nos GameTests (inclui gerar os chunks);
  o resto da cidade (~250 mil operações, ~680 peças) roda no tick com orçamento de 6 ms/tick e chunks gerados em
  segundo plano. Nos testes (orçamento de 45 ms) a cidade inteira fica pronta em ~5–10 s de relógio, com no máximo
  ~45 ms por tick e ~0,6–1,5 s de CPU por metade da obra.

## Estado de Nosso Lar após esta entrega

* Cidade circular grande (muralha r=156) com Governadoria e torre de luz no centro, seis Ministérios em setores,
  Câmaras de Retificação, Hospital, Biblioteca, Escola, Reservatório, Bosque das Águas, Mirante, dezenas de casas,
  8 estações do aeróbus, 4 portões e 12 torres. Ver `docs/NOSSO_LAR_LEVEL_DESIGN.md`.
* Saves antigos (colônia pequena) são atualizados automaticamente na abertura do servidor (detecta a falta de
  `colonia_versao` nos pontos).
* Proteção completa da dimensão (pacote `protecao`).
* Não validado visualmente (sem GUI): aparência final, pose ao sentar, feixe do farol, talude em terreno real.
