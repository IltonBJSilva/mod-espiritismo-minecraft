# Changelog — Mod Espiritismo Minecraft

Todas as versões são *pré-lançamentos*: testadas por GameTests automáticos, ainda em fase de testes dentro do jogo.

## v0.9.0 — Nosso Lar: cidade grande e protegida
- **Bug das flores corrigido na raiz**: a construção removia o terreno de baixo para cima e as plantas perdiam o apoio e viravam itens. Agora a obra limpa de cima para baixo, sem atualizar vizinhos, e só planta depois do chão pronto (teste automático: antes 775 itens soltos, agora zero)
- **Proteção total de Nosso Lar**: ninguém quebra, coloca, queima, inunda ou explode blocos da colônia; pistões, mobs e pisoteio também bloqueados. Portas, atris, placas, conversar e sentar continuam permitidos. Operador em modo criativo pode fazer manutenção (registrada no log)
- **Cidade circular** inspirada na descrição do livro: muralha com 4 portões e 12 torres, Governadoria no centro com torre de luz, os 6 Ministérios em setores, Câmaras de Retificação, Hospital, Biblioteca, Escola, Bosque das Águas, Mirante e mais de 40 casas
- **Construção aos poucos** (sem travar): só a área de chegada é feita na hora; o resto é erguido em segundo plano e retomado se o mundo fechar
- **Aeróbus** (descrito no livro *Nosso Lar*, cap. 10): 8 estações com destinos clicáveis (`/aerobus`)
- Terreno nivelado com talude suave — sem o "buraco" com paredes de terra

## v0.8.0 — Umbral, estados espirituais e resgate com consentimento
- **Estados espirituais** com regras de transição explícitas (estável, perturbado, recém-desencarnado, em acolhimento, em tratamento, no Umbral, em resgate, pronto para reencarnar...), salvos no jogador e restaurados ao reconectar
- **Resgate com consentimento**: socorristas se aproximam com calma e oferecem *Aceitar*, *Conversar primeiro*, *Pedir tempo* ou *Recusar* — recusar é respeitado, sem punição; a tecla **H** pede ajuda a qualquer momento
- **7 Postos de Socorro** no Umbral, com farol visível de longe, triagem, leitos, ponto de conversa e saída assistida; construídos aos poucos, sem travar o jogo
- **Perturbação** (névoa leve, sussurros, seta oscilando) com **acessibilidade**: modos completo, reduzido ou desligado (tecla **O** ou `/espiritismo_efeitos`); médium perturbado sente mais estímulos e aprende a se estabilizar
- Enquanto perturbado, o jogador **não ataca nem quebra blocos** (bloqueado no servidor)
- **"Paciente não ajuda paciente"**: quem precisa de cuidado não assume o socorro de outros Espíritos
- Sair do Umbral não depende mais de ajudar outros: vem da prece, da reflexão, do repouso e da conversa no posto; morrer lá não zera o progresso
- Sair de Nosso Lar sem acompanhamento leva a uma zona de transição onde é possível se perder — sempre com como pedir ajuda
- Reencarnação sem travamento (busca de local seguro aos poucos)

## v0.7.0 — Moradores de Nosso Lar e Amélie Boudet
- **Personagens do livro *Nosso Lar*** (André Luiz / Chico Xavier), com papéis conferidos na obra: Clarêncio (Ministro do Auxílio), Lísias (enfermeiro visitador), Laura, Narcisa, Tobias, Veneranda, Genésio, Henrique de Luna e o Governador (sem nome, como no livro) — falas identificadas como adaptação do jogo
- **24 moradores fictícios** de 11 tipos (recém-chegados, pacientes, enfermeiros, instrutores, mensageiros, bibliotecários, jardineiros...) com skins próprias, rotinas (trabalho, convivência e descanso) e memória de cada jogador
- Diálogos que mudam conforme o estado espiritual, as leituras e as missões do jogador
- **Jornada do recém-chegado** em 7 etapas; quem ainda está em tratamento não assume trabalho de assistência ("paciente não ajuda paciente")
- **Amélie Gabrielle Boudet** (1795–1883) na casa de Allan Kardec, com 4 missões sobre a preservação e a continuidade do trabalho, baseadas em fatos históricos
- Moradores surgem por proximidade e não se acumulam; personagens nomeados existem uma só vez por mundo
- Espíritos temporários (socorristas, sofredores) agora sempre desaparecem no prazo, mesmo com o chunk descarregado
- Missão da Evangelizadora limitada a uma vez por dia e sem criar aldeões
- 11 conquistas novas

## v0.6.0 — Biblioteca Espírita
- **Catálogo com 21 obras** (Kardec, André Luiz, Emmanuel e outros) em tela própria: ficha bibliográfica, contexto histórico, sumário navegável, busca, marcadores, notas pessoais e progresso de leitura salvo
- Cada trecho identificado como **texto da obra, resumo do mod, adaptação ou nota**, com o **status de direitos autorais** de cada obra (Lei 9.610/1998): obras protegidas aparecem só como sinopse e análise originais
- **Glossário** com 25 termos e **quizzes** opcionais
- **22 trilhas de aprendizagem** (perispírito, reencarnação, livre-arbítrio, mediunidade e discernimento, obsessão, prece, caridade, perdão, sono, desencarnação...) sugeridas em momentos do jogo, uma vez por jogador
- **Conhecimento** registrado separado da moralidade (estudar não "compra" moralidade)
- **Evangelho no Lar**: atividade opcional e cooperativa em multiplayer, com convite e sem efeitos "mágicos"
- Estante Espírita (bloco), Catálogo da Biblioteca (item) e comandos `/biblioteca` e `/evangelho_no_lar`
- Jarra d'água: no máximo 1 água fluidificada a cada 10 minutos por jogador
- 4 conquistas novas

## v0.5.1 — Correções da auditoria
- Centro Espírita do tutorial: a construção não quebra mais flores e plantas em volta (limpeza de cima para baixo, sem soltar itens) — mesma causa do bug das flores de Nosso Lar, que será corrigido na próxima versão
- Aldeão faminto: a recompensa maior vale uma vez por dia de jogo e respeita o teto diário de moralidade
- Água fluidificada: agora alivia um efeito nocivo e dá uma regeneração breve — apoio, não cura de tudo (o remédio real é prece e reforma íntima)
- Documentos novos em `docs/`: auditoria do estado atual, testes e validação, e roteiro de como testar no Minecraft

## v0.5.0 — Tutorial com os trabalhadores do Centro e caridade discreta
- Tutorial conduzido pelo Dirigente, Passista e Evangelizadora do Centro Espírita
- Allan Kardec, Chico Xavier e Divaldo Franco viram encontros especiais nos seus locais, com presentes e conquistas
- Bússola Espiritual e seta de objetivo (GPS) no HUD
- Caridade discreta: sem "+moralidade" na hora; as boas ações são reveladas ao despertar em Nosso Lar (ESE, cap. XIII)
- Fumaça bem visível sobre aldeões famintos; doações (sopa fraterna, cesta básica, agasalho) valem mais
- 14 conquistas na árvore "Jornada Espiritual"
- Correções: ganhos de moralidade com teto diário, atril de reencarnação que se recoloca, textos doutrinários revisados

## v0.4.0 — Plano espiritual e mundo
- Dimensões **Nosso Lar** (6 Ministérios, Câmaras de Retificação, Mirante com a escala dos mundos e a escala espírita) e **Umbral** (Posto de Socorro)
- Destino após a morte conforme a moralidade; resgate do Umbral pelo arrependimento; reencarnação; emancipação da alma durante o sono
- Centros Espíritas pequeno, médio e grande espalhados pelo mundo, com materiais por bioma
- Locais especiais raros: casa de Allan Kardec, casa de Chico Xavier, Mansão do Caminho, auditório
- Vilas grandes em 5 biomas

## v0.3.0 — NPCs, Espíritos, obras e blocos 3D
- Trabalhadores: dirigente, médium, orador, passista (passe gratuito), evangelizadora, Divaldo Franco (homenagem)
- Espíritos translúcidos: mentor, obsessor (esclarecido pela prece, não pela espada), sofredor, zombeteiro, socorrista
- Prece na tecla **B**
- Livro pequeno no estilo da 1ª edição de 1857, mesa mediúnica, tribuna, jarra d'água com fluidificação pela prece/passe
- Obras básicas legíveis, O que é o Espiritismo, Revista Espírita e Nosso Lar; itens de caridade

## v0.2.0 — Moralidade, classes e Centro Espírita
- Barra de Moralidade (0–100) com ações boas e más e efeitos de moralidade alta/baixa
- Classes Médium, Doutrinador e Trabalhador Espiritual com Energia Espiritual e habilidades (tecla **V**)
- Centro Espírita perto do spawn e tutorial "Jornada Espiritual"
- Palestras sobre as leis morais, conquistas, novas texturas e GameTests

## v0.1.0 — Workspace original
- Primeiro workspace no MCreator: NPCs Allan Kardec, Chico Xavier e Haroldo Dutra, O Livro dos Espíritos, Água Fluidificada
