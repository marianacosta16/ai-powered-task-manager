# Plano de testes: tarefas e permissões

> Define os testes **a partir dos requisitos**, e não do comportamento atual do código. Cobre o Exercício 1 (matriz de cenários, escolha dos essenciais e decisões) e o Exercício 2 (testes unitários de uma regra de negócio). O relatório do que foi executado e observado em cada cenário da aplicação está em [verification.md](verification.md).
> Data: 2026-11-18. Níveis: **U** unitário (uma regra, sem base de dados nem HTTP), **I** integração (pedidos HTTP reais ao backend e à base de dados), **C** componente (o frontend com a API simulada), **E** ponta a ponta (navegador, backend e base de dados reais).

## 1. De onde vêm os cenários

| Fonte | Usada para |
|---|---|
| [R1](requirements.md#4-regras-de-negócio) prazo obrigatório; [R13](requirements.md#4-regras-de-negócio) prazo não passado (hoje aceite); [R14](requirements.md#4-regras-de-negócio) título de 3 a 150 caracteres, só espaços recusado; [R21](requirements.md#4-regras-de-negócio) responsáveis do projeto; [R22](requirements.md#4-regras-de-negócio) só em projeto aberto | Entradas válidas e inválidas de uma tarefa |
| [R8](requirements.md#4-regras-de-negócio) e [S1](constraints.md) acesso só aos projetos de que se é membro ou gestor; R9 quem apaga; [R24](requirements.md#4-regras-de-negócio) comentários: só o autor muda o seu | Permissões |
| [A46](../openapi.yaml): o título tem de 3 a 150 caracteres, sem contar os espaços e os invisíveis nas pontas | O valor-limite exato |
| Cenários 2.1 a 2.15 e 6.x de [user-stories.md](user-stories.md), US02 | Resultados esperados |
| [openapi.yaml](../openapi.yaml) (`createTask`, `getTask`, `listProjectTasks`, os códigos de erro) | Estados HTTP e códigos de erro |

## 2. Matriz de cenários

| # | Cenário | Entrada | Resultado esperado | Nível | Onde está |
|---|---|---|---|---|---|
| TC01 | Título válido | "Prepare the demo" (3 a 150), prazo hoje ou depois, urgência e importância 1 a 4, um responsável do projeto | `201`; estado "To do"; aparece na lista e no detalhe | I, C, E | `TasksIntegrationTest.aMemberCreatesATaskWithToDoStatusAndItsPriorities`; `ProjectDetailPage.test.tsx`; `mvp1.spec.ts` |
| TC02 | Título no limite | 3 e 150 caracteres (aceites); 2 e 151 (recusados); 150 emojis aceites (conta-se por caracteres) | Aceites ou `400 VALIDATION_ERROR` | **U**, I, C | `TextsTest`; `ValidationRulesIntegrationTest`; `TasksIntegrationTest.titleAndDescriptionHaveMaximumLengths`; `TaskPage.test.tsx` |
| TC03 | Título vazio ou só com espaços | "", espaços, tabulação, espaço sem quebra, espaço de largura zero, "  a" | `400 VALIDATION_ERROR` e **nada fica guardado** | **U**, I, C | `TextsTest`; `ValidationRulesIntegrationTest`; `TasksIntegrationTest.aTitleOfOnlySpacesOrOfFewerThanThreeCharactersIsRefused` |
| TC04 | Prazo em falta | Sem `deadline` | `400` | I, C | `TasksIntegrationTest.theDeadlineIsRequired` |
| TC05 | Prazo no passado; prazo hoje | Ontem; hoje | `422 DEADLINE_IN_PAST`; hoje é aceite | I, C | `ValidationRulesIntegrationTest`; `TasksIntegrationTest.theDeadlineIsNotInThePastButTodayIsAccepted` |
| TC06 | Urgência ou importância fora de 1 a 4 | 0, 5 | `400` | I | `ValidationRulesIntegrationTest` |
| TC07 | Responsável | Nenhum; alguém de fora do projeto | `400`; `422 ASSIGNEE_NOT_MEMBER` | I | `ValidationRulesIntegrationTest`; `TasksIntegrationTest.anAssigneeMustBeAMemberOfTheProjectOrItsManager` |
| TC08 | Criar e consultar | Criar e depois `GET /tasks/{id}` e a lista | Os mesmos dados; mantêm-se depois de reiniciar o backend | I, E | `PersistenceAfterRestartIntegrationTest`; `mvp1.spec.ts` |
| TC09 | Sem sessão | Qualquer pedido sem cookie | `401 UNAUTHENTICATED` | I | `AccessIntegrationTest.requestsWithoutASessionAreAskedToSignIn` |
| TC10 | Utilizador de outro projeto | Criar, ler, alterar ou apagar uma tarefa do projeto alheio, **também com um corpo inválido** | `403`; o `403` vem antes do `400` | I | `TasksIntegrationTest.aMemberOfAnotherProjectGets403EvenWithAnInvalidBody` |
| TC11 | Mudar os identificadores do pedido | Os ids reais de um projeto, tarefa e comentário alheios, em 25 operações | `403` ou `404`, e a base fica igual (impressão digital das tabelas) | I | `AuthorizationByIdentifierIntegrationTest` |
| TC12 | Só o gestor | Um membro altera ou apaga o projeto, mexe na equipa, restaura um projeto | `403` | I | `AuthorizationByIdentifierIntegrationTest` |
| TC13 | Comentário de outra pessoa | O gestor ou outro membro edita ou apaga o comentário da Ana | `403` (nem o gestor, A26) | I | `AuthorizationByIdentifierIntegrationTest`; `CommentsIntegrationTest.nobodyButTheAuthorEditsOrDeletes` |
| TC14 | Projeto concluído ou suspenso | Criar tarefa | `422 PROJECT_NOT_OPEN`; o botão não aparece | I, C | `TasksIntegrationTest.tasksAreNotCreatedInACompletedOrSuspendedProjectUntilItIsResumed`; `ProjectDetailPage.test.tsx` |
| TC15 | Apagar com dependências | Apagar uma tarefa com subtarefas em aberto | Recusado (`PARENT_HAS_OPEN_SUBTASKS`) e a tarefa fica | I, C | `RecycleIntegrationTest.aTaskWithOpenSubtasksIsNotDeleted`; `TaskPage.test.tsx` |

## 3. Cenários essenciais escolhidos, e porquê

| Escolhido | Porquê | Níveis | Requisitos |
|---|---|---|---|
| **Título inválido** (TC02, TC03) | É a regra de entrada que mais se testa à mão e a mais fácil de estragar sem dar por isso (o limite exato, os espaços invisíveis, contar por caracteres e não por unidades). Uma regra pura permite um **teste unitário** barato, que corre sem base de dados, e o de integração mostra que o pedido recusado não deixa nada guardado | U, I, C | R14, A46, cenário 2.14 |
| **Criação e consulta de tarefa** (TC01, TC08) | É o percurso principal da US02. O que importa é que, depois de criada, a tarefa se encontre, com os mesmos dados, na lista e no detalhe e se mantenha depois de reiniciar: um teste só de criação não prova a persistência | I, E | R1, R13, R21, cenário 2.1, US02 |
| **Acesso de um utilizador sem autorização** (TC10, TC11) | É o risco de segurança que mais custa se falhar (ler ou alterar o que é de outros) e o que um teste "feliz" nunca apanha. Testa-se com os ids reais do projeto alheio, em muitas operações, e confirma-se que **os dados ficam intactos** | I | R8, S1, A26 |

Ficaram de fora do mínimo, mas estão cobertos: TC04 a TC07, TC09, TC12 a TC15.

## 4. Pressupostos corrigidos na revisão

O plano foi sendo corrigido pelo que se encontrou ao escrevê-lo e ao executar os testes, e não só pelo que parecia certo:

| Pressuposto inicial | O que se verificou | Correção |
|---|---|---|
| O título de uma tarefa tem no máximo 100 caracteres (o que dizia o texto do contrato e do wireframe) | O requisito R14 e o A46 dizem 150, e a base de dados foi alargada (migração V3) | Contrato, wireframes e o `limits.test.ts` (que compara os limites com o `openapi.yaml`) |
| Qualquer valor inválido dá `400` | Os de regra de negócio dão `422` com o seu código (`END_BEFORE_START`, `START_DATE_IN_PAST`, `DEADLINE_IN_PAST`); só os de formato dão `400 VALIDATION_ERROR` | O teste de validação passou a exigir o código de cada caso, e não só "recusado" |
| A Marta gere só o *Website* | Gere também o *Mobile App* nos dados de demonstração, por isso "um gestor de outro projeto" tem de ser o Pedro | O cenário TC11 usa o Pedro e o projeto dele |
| Os dados de demonstração não têm comentários | Têm, por isso contar comentários em absoluto falhava | Os testes comparam com a contagem de antes |
| "Recusado" chega como teste de autorização | Um `404` também esconde um recurso: aceita-se `403` ou `404`, mas nunca `2xx` e nunca uma alteração | A base fica igual (impressão digital) |
| O cenário MVP-1 usa a data 2026-01-01 | O backend real usa o relógio do sistema e recusa inícios no passado (R29) | O teste de ponta a ponta usa datas relativas a hoje; o resto está nos testes do backend com relógio controlado |

## 5. Exercício 2: testes unitários da regra do título

**Regra escolhida:** o título de uma tarefa tem de 3 a 150 caracteres, contados por caracteres (não por unidades UTF-16), sem contar os espaços e os caracteres invisíveis nas pontas, e não pode ser só espaços ou invisíveis (R14, A46). Está escrita num só sítio, `Texts.check(texto, mínimo, máximo, visível)`, que o validador dos pedidos e os *parsers* das alterações chamam.

**Framework:** JUnit 5 e AssertJ (a do projeto, no backend). Sem base de dados nem HTTP: `TextsTest`.

| Caso | Teste (`TextsTest`) | Entrada e o que se afirma | Requisito |
|---|---|---|---|
| **Válido** | `aTextOfTheRightSizeHasNoProblem` | `"abc"` (3) e 150 caracteres não têm problema; `"  abc  "` e `" a b "` também (o texto fica como foi escrito) | R14, A46 |
| **Inválido** | `theProblemsAreFoundInTheOrderTooLongOnlySpacesTooShort` | 151 caracteres: `TOO_LONG`; só espaços, tabulação e invisíveis: `ONLY_SPACES`; `""` e `"ab"`: `TOO_SHORT`; `"  a"` e `" ab "`: `TOO_SHORT` (as pontas não contam) | R14, A46, cenário 2.14 |
| **Valor-limite** | os dois anteriores, e `charactersAreCountedNotUtf16Units` | 3 e 150 aceites, 2 e 151 recusados; 150 emojis aceites e 151 recusados (conta-se por caracteres) | R14 |
| Sem a regra do "visível" | `withoutTheVisibleRuleTheSpacesCountAsCharacters` | Para a descrição, que pode ser só espaços: `"   "` é aceite | R14 (a descrição) |
| Mensagens | `theMessagesSayWhatIsWrong` | A mensagem diz o limite ("Longer than 150 characters.") | NF08 |

### Execução (2026-11-18, nesta máquina)

| Comando | Resultado |
|---|---|
| `mvn -q -Dtest=TextsTest test` (com o `.env` carregado) | **5 testes, 0 falhas** |
| `mvn -q -Dtest='ValidationRulesIntegrationTest#aTaskTitleThatIsEmptyOrOnlySpacesIsRefused+anInvalidValueIsRefusedAndNothingIsStored' test` | **22 testes, 0 falhas** (os 9 títulos vazios ou só com espaços e os 13 valores inválidos) |
| `mvn -q -Dtest='TasksIntegrationTest#aMemberCreatesATaskWithToDoStatusAndItsPriorities+aTitleOfOnlySpacesOrOfFewerThanThreeCharactersIsRefused+titleAndDescriptionHaveMaximumLengths+aMemberOfAnotherProjectGets403EvenWithAnInvalidBody+theDeadlineIsRequired' test` | **5 testes, 0 falhas** |
| `mvn -q -Dtest=PersistenceAfterRestartIntegrationTest test` | **1 teste, 0 falhas** |
| `mvn -q -Dtest=AuthorizationByIdentifierIntegrationTest test` | **5 testes, 0 falhas** |
| `npx vitest run` em seis testes de componentes do título (`ProjectDetailPage.test.tsx` e `TaskPage.test.tsx`, pelo nome) | **6 testes, 0 falhas** |

### Confirmar que as asserções verificam o requisito, e não só o código de hoje

Para ter a certeza de que os testes **falham quando a regra está errada** (e não apenas passam porque a regra de hoje está certa), mudou-se a regra de propósito, uma de cada vez, e correu-se só o `TextsTest`. A regra foi sempre reposta; **nenhum teste foi alterado nem a regra mudada para ficar verde**.

| Alteração (mutante) | O que o requisito diz | `TextsTest` |
|---|---|---|
| O máximo passa a exclusivo (150 recusado) | 150 é aceite | **falha (apanhado)** |
| O mínimo passa a exclusivo (3 recusado) | 3 é aceite | **falha (apanhado)** |
| A regra de só espaços removida | só espaços é recusado | **falha (apanhado)** |
| Os espaços das pontas passam a contar | `"  a"` não vale 3 | **falha (apanhado)** |
| Conta-se por unidades UTF-16 e não por caracteres | 150 emojis são 150 | **falha (apanhado)** |

**Testes incoerentes:** não se encontrou nenhum. A única correção de asserção desta frente foi no teste de validação de integração, que aceitava qualquer `400` ou `422` e passou a exigir o código certo (secção 4).

## 6. Limites e o que falta

- **No frontend não há um teste unitário isolado da regra do título** (`validateTask` e `validateEdit`): ela só é testada através dos testes de componentes (os seis acima). Pode acrescentar-se um ficheiro de testes unitários puro, com o caso válido, o inválido e os limites de 3 e 150, como o do backend. Não se fez nesta passagem, que foi só de documentação.
- O `TextsTest` verifica a regra, não a sua **ligação** ao pedido HTTP; essa está nos testes de integração (TC02, TC03).
- Os níveis C e E dependem de o contrato e o frontend se manterem alinhados; o `limits.test.ts` compara os limites do frontend com o `openapi.yaml`.
- Esta lista é a matriz mínima pedida, e não todos os cenários do sistema (esses estão nos cenários de [user-stories.md](user-stories.md) e no relatório [verification.md](verification.md)).
