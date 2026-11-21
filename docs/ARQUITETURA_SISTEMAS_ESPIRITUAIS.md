# Arquitetura dos sistemas espirituais (área "umbral" — Fase 4)

Interpretação educativa e não oficial, inspirada nas obras básicas de Allan Kardec e no livro *Nosso Lar*
(André Luiz, psicografia de Francisco Cândido Xavier, FEB, 1944). Os valores numéricos (moralidade, perturbação)
são mecânicas abstratas de jogo, não julgamentos sobre pessoas.

## 1. Três dimensões que não se confundem (PROMPT §7)

| Dimensão | Onde | Natureza |
|---|---|---|
| Moralidade (0–100) | `Moralidade.MORALIDADE` (núcleo) | histórico de condutas; teto diário de rotina preservado |
| Estado espiritual | `Moralidade.ESTADO` + `estado.Estados` | situação atual (máquina de estados) |
| Perturbação (0–100) | `RegistroDimensao.PERTURBACAO` + `dimensao.Perturbacao` | confusão temporária; sobe/desce com o contexto |

Recusar ajuda, explorar ou questionar **nunca** reduz a moralidade.

## 2. Máquina de estados (`estado.Estados`)

Tabela explícita `TRANSICOES` (EnumMap). `transicionar(p, novo, motivo)` recusa transições fora da tabela
(devolve `false` sem mudar nada). `forcar(...)` só para migração, reconexão e comandos de admin.
Cada mudança grava `ESTADO_DESDE` (tick) e `ESTADO_VERSAO` e chama os ganchos `Estados.AO_MUDAR`.

```
ENCARNADO_ESTAVEL ⇄ ENCARNADO_PERTURBADO          (moralidade ≤ 20 / > 25 na Terra)
ENCARNADO_* → EM_VISITA_ASSISTIDA → ENCARNADO_*    (desdobramento/pesadelo no sono)
ENCARNADO_* → DESENCARNANDO → RECEM_DESENCARNADO   (morte do corpo)
RECEM_DESENCARNADO → EM_ACOLHIMENTO → EM_NOSSO_LAR (moral ≥ 60; 60 s de acolhimento)
RECEM_DESENCARNADO → NO_UMBRAL_PERTURBADO | NO_UMBRAL_ORIENTADO (moral ≤ 30)
RECEM_DESENCARNADO → REENCARNANDO → ENCARNADO_ESTAVEL (31–59: perturbação breve e volta à Terra)
NO_UMBRAL_PERTURBADO ⇄ NO_UMBRAL_ORIENTADO         (perturbação ≥ 50 / ≤ 25, histerese)
NO_UMBRAL_* → EM_RESGATE (aceitou) → EM_ACOLHIMENTO (no Posto, 30 s) → NO_UMBRAL_ORIENTADO
NO_UMBRAL_* / EM_ACOLHIMENTO → EM_ACOLHIMENTO em Nosso Lar (saída assistida pelo atril do posto)
EM_NOSSO_LAR → PRONTO_PARA_REENCARNAR → REENCARNANDO → ENCARNADO_ESTAVEL (atril do Ministério do Auxílio)
EM_NOSSO_LAR → NO_UMBRAL_PERTURBADO (perdeu-se na saída) | NO_UMBRAL_ORIENTADO (saída acompanhada)
qualquer estado espiritual → RECEM_DESENCARNADO    (nova morte em NL/Umbral)
```

Nenhum estado fica sem saída (testado). Consultas sem efeitos colaterais: `perturbado`, `podeSocorrer`,
`semViolencia`, `coerente(estado, dim)`, `deduzir(dim, moral)`.

### Migração e reconexão
- Save antigo (`estado_versao` = 0): `Estados.migrar` deduz o estado pela dimensão (ou EM_VISITA_ASSISTIDA se
  estava sonhando). Chamado no login e preguiçosamente no tick do jogador.
- `EventosDimensao.aoReconectar`: termina sonho interrompido; resgate já aceito é **concluído**; oferta aberta é
  encerrada (sem decidir pelo jogador; volta em 20 s); REENCARNANDO fora da Terra reinicia a busca; estado
  incoerente com a dimensão é corrigido por `deduzir`. Logout remove socorristas temporários e cancela buscas.
- `PlayerChangedDimensionEvent`: corrige estados incoerentes após viagens por outros meios.

## 3. Políticas por estado (`estado.PoliticaEstado`)

"Sem violência" (`EstadoEspiritual.semViolencia`: NO_UMBRAL_PERTURBADO, EM_RESGATE, EM_ACOLHIMENTO, EM_TRATAMENTO,
DESENCARNANDO, REENCARNANDO), **no servidor**:
- `AttackEntityEvent` (golpe em entidade ou jogador) cancelado;
- `LivingIncomingDamageEvent` cancelado quando `source.getEntity()` é o jogador (flecha, tridente, poção, etc.);
- `ProjectileImpactEvent` de projétil cujo dono está sem violência: cancelado e o projétil removido;
- `BreakBlockEvent`, `EntityPlaceEvent`, `LeftClickBlock` cancelados;
- uso de itens ofensivos/de construção (arcos, tridente, maça, arremessáveis, baldes, isqueiro, carga de fogo,
  ovos de criação, blocos) negado; usar portas, atris, camas e ler livros continua livre.

Separada da proteção territorial de Nosso Lar (área "colonia"). Os **Postos de Socorro** têm proteção própria
(quebra/colocação/explosão) para qualquer estado, exceto criativo.

## 4. Perturbação (`dimensao.Perturbacao`)

- Sobe no Umbral longe dos postos em direção a `alvo(moral, médium) = clamp(75 − moral, 0, 80) (+10 médium)`;
  desce perto de um posto (−2/s), com prece (−12), conversa (−10), repouso num leito do posto (−20),
  acolhimento (→ ≤ 20) e dentro de Nosso Lar. A água fluidificada **não** mexe nela.
- Recusar o socorro soma +5, com teto 90 (sem punição infinita).
- Médium: sobe mais rápido, recebe mais "vozes" ambíguas (umas confusas, outras de orientação) e uma dica a cada
  5 min sobre discernimento (LM, 2ª parte, cap. XXIV) — estabiliza pelos mesmos meios.
- Cliente (`dimensao.client.EfeitosPerturbacao`): névoa no máximo 35% mais próxima, sussurros posicionais em
  volume 0,25 (0,12 reduzido), "respiração" do FOV de ±1,2° só no modo completo, seta do objetivo oscilando
  (gancho para o HUD), linha de HUD informando que é temporário e como pedir ajuda. Nunca flashes.
- Acessibilidade: `config/espiritismo_acessibilidade.properties` (`efeitos_perturbacao=completo|reduzido|desligado`),
  tecla **O** (alterna), comando de cliente `/espiritismo_efeitos <modo>`. O servidor recebe a preferência
  (`PreferenciaEfeitos`) e reduz/desliga os ecos de texto.

## 5. Resgate com consentimento (`dimensao.Resgates`)

Fases persistidas (`RESGATE_FASE`, `RESGATE_ATE`; `RESGATE_EM` > 0 mantido por compatibilidade):
`NENHUM → APROXIMANDO (até 20 s) → OFERTA (até 60 s) → ACOMPANHANDO (5 s) → concluir`.
- Início: automático para quem está NO_UMBRAL_PERTURBADO fora de um posto, sem recarga e com
  `resgateLiberado(moral, arrependimento)` ou 60 s perturbado; ou a qualquer momento pelo jogador (tecla **H**,
  `/espiritismo_socorro pedir`, clicar num socorrista de posto).
- Dois socorristas temporários surgem a ~14 blocos e se aproximam a ≤ 0,25 bloco/2 ticks. Eles têm etiqueta
  `espiritismo_socorro_temp`, tempo de vida próprio (`EspiritoEntity.acompanhar`) e somem se o jogador sair;
  além disso são removidos explicitamente ao concluir, cancelar, recusar, pedir tempo, sair do jogo e morrer.
  `/espiritismo_plano limpar_temporarios` remove órfãos.
- Escolhas (tela `TelaResgate` + botões clicáveis no chat + comando): Aceitar, Conversar primeiro (alivia e
  mantém a oferta), Pedir tempo (recarga 2 min), Recusar (recarga 5 min, +5 de perturbação com teto, rota do
  posto na seta). Nenhuma escolha mexe na moralidade. **Só "Aceitar" leva o jogador.**
- Não há dois resgates simultâneos (fase ≠ NENHUM bloqueia `oferecer`). `concluir` é idempotente.

### Saída do Umbral sem depender de ajudar outros (auditoria #1)
`saidaAssistidaLiberada(moral, arrependimento, perturbação) = resgateLiberado(...) && perturbação ≤ 25`.
O arrependimento vem de prece (1/30 s), tempo de reflexão (1/3 min), conversa no posto (1/min) e repouso
(1/2 min). Consolar sofredores só conta para quem `podeSocorrer` — o perturbado nunca precisa disso.
Morrer de novo no Umbral **não** zera o arrependimento e desperta à porta do Posto central (auditoria #2).

## 6. "Paciente não ajuda paciente" (§5.7)

`Estados.podeSocorrer(estado, moral, perturbação)`: não estar em estado de cuidado, moral > 20 e
perturbação < 40. Aplicado em `Planos.tentarConsolar` (clique num sofredor no Umbral) — sem condições, o
clique é cancelado e o jogador recebe: "Neste momento, você também precisa de cuidado. Permita-se receber
auxílio primeiro." Ganchos para npc/Prece descritos em INTEGRACAO.md.

## 7. Nosso Lar: entrada e saída (§4.1, §4.2)

- Entrada: `PlayerChangedDimensionEvent` para Nosso Lar exige `Planos.podeEntrarNossoLar(estado)` (desencarne,
  acolhimento, visita no sono, resgate/saída assistida, planejamento reencarnatório). Senão, o jogador é
  reconduzido no tick seguinte com uma explicação. Criativo/espectador e `/espiritismo_plano ir` passam.
- Limite: `Planos.alemDoLimite` usa `ColoniaApi.dentroDaMuralha`; enquanto a API devolver `true` para a
  dimensão inteira (testado com um ponto a 1.000.000 blocos), usa `RAIO_COLONIA_PADRAO = 160` a partir de
  `ColoniaApi.local("praca")` (ou "chegada").
- Fora do limite sem acompanhamento: aviso (a cada 45 s), perturbação +`riscoDesorientacao(moral)` por segundo
  (0 com moral ≥ 80; 1–4 caso contrário). Além de 40 blocos e perturbado ≥ 50: perde-se e desperta no Umbral
  (90–150 blocos do Posto central), com a seta apontando o posto. Sempre há como pedir ajuda: na zona de
  transição, **H** chama um mensageiro que reconduz à praça.
- Saída acompanhada: `Planos.autorizarSaida(p, ticks)` (para mentores/mensageiros) → ao cruzar, vai ao Posto
  central do Umbral, orientado.

## 8. Reencarnação (§14.2) — `dimensao.BuscaLocalSeguro`

Idempotente: `Planos.iniciarReencarnacao` devolve `false` se já houver busca; `concluirReencarnacao` só age
em REENCARNANDO; o inventário nunca é tocado. A busca testa **um** candidato por vez: pede o chunk com ticket
(`addTicketAndLoadWithRadius`, geração nos workers), espera até 100 ticks e lê o heightmap só com o chunk pronto
(`getChunkNow`). Máx. 6 candidatos / 600 ticks; depois, fallback (cache de locais seguros ou ponto de
renascimento). `/espiritismo_plano reencarnar` e `Planos.reencarnar` usam a versão imediata, que só olha chunks
já carregados (nunca gera terreno).

## 9. Postos de Socorro e construção incremental

Ver `UMBRAL_LEVEL_DESIGN.md`. Resumo: `ConstrucoesUmbral.plano(...)` gera uma lista determinística de operações;
o servidor executa até 256 por tick, só em chunks carregados; progresso em `RegistroDimensao.OBRAS` no Level
(`p<i>.x/.y/.z/.t/.i/.ok`), retomado após reinício. A chegada ao Umbral no renascimento só faz o pátio de 3×3.

## 10. Persistência (attachments da área)

No jogador: `perturbacao` (sync dono), `estado_versao`, `estado_desde`, `resgate_fase`, `resgate_ate`, `saida_ate`
(+ os anteriores `arrependimento`, `tempo_umbral`, `resgate_em`, `sonho_*`, `ultima_terra`).
No Level: `plano_pontos`, `plano_obras`. Tudo por jogador, compatível com multiplayer e reinício.
