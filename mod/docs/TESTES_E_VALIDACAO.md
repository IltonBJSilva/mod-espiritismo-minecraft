
---

## Estado na v1.0.0-beta
- **66 GameTests obrigatórios passando** (`./gradlew -I testes/gametest.init.gradle runGameTestServer`, ~45 s).
- **Teste pesado opcional** `col_cidade_flores_sobrevivem`: constrói a cidade inteira de Nosso Lar (~300 mil ticks) e confere que nenhuma das ~9.300 plantas vira item. Só roda com `-Despiritismo.testesPesados=true` (adicione ao `jvmArgument` do run `gameTestServer` em `testes/gametest.init.gradle`). A verificação leve do mesmo bug roda sempre em `col_essenciais_rapidas_sem_itens`.
- Limitação do ambiente de teste: o servidor de GameTest não carrega as dimensões de datapack (Nosso Lar/Umbral); a lógica delas é testada por funções puras e construção no Overworld de teste. Viagens reais entre os planos precisam de teste no jogo (ver `COMO_TESTAR_NO_MINECRAFT.md`).
