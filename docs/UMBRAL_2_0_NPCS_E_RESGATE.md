# Umbral 2.0 — NPCs, equipes de socorro e diálogos

Agente **pesquisa** (papel de NPC designer). Classes: **[A]** Kardec · **[B]** obra mediúnica · **[C]**
interpretação · **[D]** criação · **[D←B]** adaptação fundamentada. Fontes em `UMBRAL_2_0_FONTES_E_FIDELIDADE.md`.

> **Todos os diálogos deste documento são ORIGINAIS DO JOGO (adaptação).** Nenhuma fala é citação de livro, e
> nenhuma é atribuída a pessoa real ou a personagem nominal das obras. Personagens das obras (Clarêncio, Narcisa,
> Carlos e Roberto de Canalejas etc.) **não falam** no jogo nesta rodada. Quando aparecerem no futuro, devem
> aparecer só como referência no painel educativo ou com falas marcadas “diálogo original do jogo, inspirado em…”.

## 1. Equipes de socorro

| Equipe | Inspiração | Composição | Área e rotina | Capacidade e ciclo |
|---|---|---|---|---|
| **Reconhecimento** | Lanceiros exploradores e uso de planta do lugar (*Memórias* I–II) [D←B]; cães dos Samaritanos (*Nosso Lar* 33) [B] | 2 batedores + 1 cão | Rondas entre marcos; colocam lanternas azuis temporárias | 1 ronda a cada 6 min por setor |
| **Acolhimento** | Enfermeiras da caravana (*Memórias* II); Narcisa (*Nosso Lar* 34) [B] | 1 orientador + 1 enfermeiro | Abordam perturbados; conversam; acompanham a pé | 1 assistido por vez; 2 equipes por faixa |
| **Transporte** | Samaritanos (*Nosso Lar* 33); comboio da Legião (*Memórias* I–II) [B] | Condutor, carroça ou comboio, 2 maqueiros | Rota fixa posto ↔ posto ou Praça ↔ Colônia | 4 assistidos por viagem; volta à base |
| **Trabalhadores de posto** | Casa Transitória (*Obreiros* 4) [B] | Porteiro, triagem, enfermagem, orientação (já existem) | Fixos no posto | — |
| **Vigilância do Mirante** | Torre de Vigia (*Memórias* II parte) [D←B] | 1 vigia + 1 sinaleiro | Fixos; acendem o farol quando a caravana se aproxima | — |
| **Equipe hospitalar** | Hospital Maria de Nazaré (*Memórias* III) [B] | Médico, enfermeiros, assistentes | Fixos na Colônia | — |

**Comportamento obrigatório (§6 e §26.6 do PROMPT):** andar no chão e perto do assistido, nunca flutuar para
demonstrar superioridade; chegar caminhando de um ponto visível, sem surgir do nada; sair pelo mesmo caminho;
despawn só fora do campo de visão ou ao voltar à base; nunca mais de 2 equipes ativas por jogador; recarga de
abordagem respeitada.

**Aparência [D←B]:** branco ou cinza-claro, pequena **cruz azul-celeste** no peito só para a Legião do Vale
(*Memórias* I) e emblema próprio dos postos. **Sem armas.** Onde o livro tem lanceiros, o jogo usa **cajados com
lanterna** (o cajado luminoso de Clarêncio está em *Nosso Lar* cap. 3 [B]).

## 2. Pacientes e assistidos (arquétipos)

**Regras:** nenhum arquétipo é rotulado pela causa da morte. Os nomes visíveis são “Assistido”, “Irmão em
repouso”, “Viajante confuso” ou um prenome fictício. Nenhuma história cita meio, ferimento ou cena da morte.

| Arquétipo | Região | Traço | Como ajudar | Desfecho |
|---|---|---|---|---|
| Viajante confuso | R0, R1 | Acha que ainda está a caminho de casa | Ouvir e mostrar o farol | Segue o jogador até o posto |
| Quem espera | R2 Ruínas | Repete a mesma tarefa (arrumar a mesa, abrir a loja) | Escutar até o fim (3 falas) e perguntar por ela mesma | O laço circular se desfaz e ela aceita a lanterna |
| Eremita | R3 Bosque | Não quer companhia | Respeitar; deixar uma lanterna | Recusa respeitada; aparece depois num posto (continuidade) |
| Seguidor de rumores | R4 | Repete que “não há saída” | Mostrar a luz verdadeira; prece | Pode acompanhar ou ficar |
| Assistido do Vale (vários) | R6 | Desânimo, sensação de abandono, vergonha | Presença, escuta, esperar a caravana junto | Embarca ou “ainda dorme, virão por ele” |
| Companheiro de caverna | R6 (Ref. 36 · Via 48) | Irritado, depois aliviado | Missão “Aprendiz da caravana” | Embarca com o jogador |
| Convalescente | R7 | Começa a refletir e a ter esperança | Conversas de convalescença | Alta / escolhe ficar aprendendo |

**Inspiração de *Memórias* III [B]:** os internos conversam entre si, contam arrependimentos e redescobrem a fé.
O jogo **mantém** a ideia de grupo que se apoia e **remove** detalhes de ferimentos e de como cada um morreu.

## 3. Missão-piloto: “Aprendiz da caravana” [D←B]

1. No posto avançado, o orientador convida o jogador (qualquer estado com `podeSocorrer`; se o jogador estiver
   perturbado, ele vai **como assistido**, e o fluxo do Hospital acontece com ele).
2. **Aviso de conteúdo** (FF §7.2) com a opção “Prefiro não ir”, que é respeitada sem perda nenhuma.
3. Descida pelo Desfiladeiro com o batedor e o cão.
4. No Vale: encontrar o refúgio “Ref. 36 · Via 48” usando a **planta** (item de missão) [D←B *Memórias* II].
5. No refúgio, um assistido não responde ao chamado. O jogador deve **sentar ao lado** e esperar, sem pressa
   (barra de presença enche em 20 s; sair cancela sem penalidade).
6. Acompanhar até a Praça; o embarque acontece no próximo ciclo da caravana.
7. Chegada à Colônia e entrega à enfermaria. O médico agradece e explica o cuidado.
8. **Reflexão final** no diário (texto original) + painel educativo + contato do CVV.

Não há falha permanente: se o jogador sair, a missão pausa e pode ser retomada.

## 4. Diálogos sensíveis — exemplos (ORIGINAIS DO JOGO)

### 4.1 Diretrizes de escrita
- Frases curtas, voz calma, segunda pessoa acolhedora.
- **Nunca:** “covarde”, “condenado”, “castigo”, “merece”, “pecado”, “culpa”, “escolheu sofrer”, “suicida” como
  rótulo, nem qualquer descrição de método ou ferimento.
- **Sempre:** validar o sentimento → oferecer presença → oferecer próximo passo → deixar a escolha.
- Recusa: aceitar sem insistir e indicar como pedir ajuda depois.

### 4.2 Socorrista de acolhimento — primeira oferta (Umbral geral)
> **Socorrista:** Boa noite. Posso ficar um pouco aqui com você?
> **[Aceitar ajuda]** · **[Conversar primeiro]** · **[Preciso de um tempo]** · **[Não quero]**
>
> *(Conversar primeiro)*
> **Socorrista:** Você parece cansado. Muita gente que chega aqui se sente perdida no começo, e isso passa.
> Há um posto perto, com água, um lugar para deitar e gente para conversar. Quer ir comigo?
>
> *(Não quero)*
> **Socorrista:** Tudo bem. A escolha é sua. Vou deixar uma lanterna acesa no caminho. Se mudar de ideia, é só
> chamar (tecla H) ou seguir a luz azul.

### 4.3 Incapacidade temporária (perturbação e necessidade altas)
> *(O socorrista senta ao lado e espera em silêncio.)*
> **Socorrista:** Não precisa dizer nada. Eu fico aqui.
> **[Só fique aqui comigo]** · **[Quero ir]** · **[Vá embora]**
>
> *(Só fique aqui comigo)* → passam 30 s; perturbação −10.
> **Socorrista:** Quando quiser, a gente levanta junto. Sem pressa.

### 4.4 Assistido do Vale (exemplo de escuta)
> **Assistido:** Achei que tudo ia acabar. E não acabou… estou aqui, e está tão frio.
> **[Sentar ao lado]** · **[“Você não está sozinho.”]** · **[Seguir em frente]**
>
> *(“Você não está sozinho.”)*
> **Assistido:** Faz tanto tempo que ninguém fala comigo.
> **Jogador (fala sugerida):** Uma caravana passa por aqui. Eles cuidam de quem está cansado. Eu espero com você.
> **Assistido:** …Você espera mesmo?

*Observação:* a ideia de “sentir-se vivo apesar de tudo” é central em *Memórias* I–III [B]. O jogo a mostra como
**surpresa que abre esperança**, sem descrever a morte.

### 4.5 Voz da caravana (anúncio) [D←B *Memórias* I–II]
> *(sino)* **Voz:** Atenção, Vale. A caravana de socorro chega à praça. Quem puder, venha. Quem não puder, fique
> onde está: iremos buscá-lo. Via 48, refúgio 36: há alguém esperando por você.

### 4.6 NPC que ainda não embarca (substitui a cena das lanças)
> **Maqueiro:** Ele ainda dorme profundamente. Não é abandono: voltaremos por ele no próximo ciclo, e alguém
> fica de olho daqui do Mirante.

### 4.7 Instigador (região obsessiva) e contrajogo
> **Instigador:** Os de branco? Eles só levam quem eles querem. Siga a luz vermelha, é mais perto.
> *(Contrajogo: a luz verdadeira é azul e constante. Se o jogador escolher [Fazer uma prece] ou [Não, obrigado],
> o instigador perde interesse e se afasta. Nunca há combate.)*

### 4.8 Enfermaria do Hospital (R7)
> **Enfermeiro:** Seja bem-vindo. Aqui você pode descansar. Tem caldo quente e a lareira está acesa. Ninguém vai
> deixá-lo sozinho: as portas são de vidro justamente para isso.
>
> **Médico (após o sono):** Você perguntou se morreu. A vida continua; o que acabou foi o corpo. Agora o trabalho
> é cuidar de você, um dia de cada vez.
> *(Inspirado na explicação do médico em* Memórias *III, parafraseada; fala original do jogo.)*

### 4.9 Alta
> **Orientador:** Você melhorou muito. Pode ficar e ajudar aqui, voltar acompanhado a um posto ou seguir para
> Nosso Lar. Qualquer caminho é um recomeço.

## 5. Regras de spawn e limpeza (para QA e arquiteto)
- No máximo 14 NPCs no Vale (8 assistidos + 6 presenças/obsessores) e no máximo 1 caravana por dimensão.
- Socorristas de resgate têm a tag `temporario` (já existe) e são removidos ao concluir, recusar, cancelar,
  desconectar ou ao descarregar o chunk.
- Assistidos que embarcam reaparecem como convalescentes na Colônia (contagem fixa, reciclada) e **não**
  acumulam.
- Obsessores somem quando a caravana chega e reaparecem só 2 min depois que ela parte.
- Sem mediunidade: o jogador vivo (encarnado) não vê nada disso (regra de `Mediunidade.percebe`). O jogador
  desencarnado no Umbral vê os Espíritos daquele plano normalmente, por coerência narrativa.
