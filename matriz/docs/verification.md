# Verificação do MVP (Exercício 8)

> Data: 2026-11-17. Objetivo: demonstrar que a aplicação cumpre os requisitos e conserva os dados. Para cada um dos 9 cenários: o resultado esperado, o observado e a evidência. **Distingue-se sempre o que é um teste automático (corre sozinho, repete-se com um comando) do que é uma verificação manual (feita por uma pessoa ou por um agente a conduzir o navegador, com capturas de ecrã, que não faz parte da bateria).**

## 1. Como foi feito, e com que limites

| | |
|---|---|
| **Ambiente** | Linux, Java 21 e Maven, PostgreSQL 16 local com a base `matriz_test`, Node 24, Chromium do Playwright. O backend usa a base de teste, com as contas de demonstração (`marta` e `pedro`, gestores; `ana`, `rui`, `luis`, `sofia`, `carla`, `ines`, `tiago` e `diana`, membros), e uma palavra-passe feita para a ocasião (nunca escrita nos ficheiros) |
| **Testes automáticos** | O backend (Spring Boot, JUnit, pedidos HTTP reais contra a base de dados) e o frontend (Vitest e Testing Library) e um teste de ponta a ponta (Playwright). Os resultados estão na secção 2 |
| **Verificações manuais** | Conduzidas por mim (um agente) com um navegador Chromium e pedidos à API, por um *script* pontual que **não está no repositório nem na bateria** e que foi apagado depois de usado. Cada passo e o que se observou estão descritos abaixo, e as capturas de ecrã estão em [`evidence/`](evidence/). Podem repetir-se à mão com os passos indicados |
| **Dados** | As verificações manuais usaram os dados de demonstração e os que elas próprias criaram. Entre elas a base não foi reposta, por isso alguns números (por exemplo o `id` 16 de uma tarefa) dependem da ordem |
| **O que não foi possível executar** | Ver a secção 4 |

## 2. Comandos executados e resultados

| Comando | Onde | Resultado |
|---|---|---|
| `mvn -B verify` (com o `.env` carregado) | `matriz/` | **352 testes, 0 falhas, 0 erros**, `BUILD SUCCESS`, 2 min 9 s. Inclui os 3 testes novos deste exercício e as verificações do contrato (`openapi.yaml` 1.1.0, 28 operações), da arquitetura (ArchUnit) e da documentação (códigos únicos, cenários nomeados por testes) |
| `mvn -q -Dtest='ValidationRulesIntegrationTest,AuthorizationByIdentifierIntegrationTest,PersistenceAfterRestartIntegrationTest' test` | `matriz/` | **49 testes, 0 falhas** (43 de validação, 5 de autorização, 1 de persistência) |
| `npm test` | `frontend/` | **268 testes de componentes, 0 falhas** (20 ficheiros), 10,8 s |
| `npm run lint` | `frontend/` | Sem avisos nem erros |
| `VITE_API_URL=http://localhost:8081 npm run build` | `frontend/` | Compila (`tsc` e Vite) sem erros |
| `npm run e2e` | `frontend/` | **1 teste de ponta a ponta passa** (o cenário MVP-1), num navegador a sério, 15,7 s |

### Os três testes automáticos novos (pedidos pelo exercício)

Foram escritos de propósito para os casos que a bateria existente cobria pouco. Cada um foi posto à prova com mutantes (alterar o backend para ver se o teste o apanha).

| Teste | Cobre | O que verifica |
|---|---|---|
| `ValidationRulesIntegrationTest` (43) | Caso 04, uma regra de validação | O nome de um projeto, o título de uma tarefa e um comentário vazios, só com espaços, com espaços de largura zero ou com o espaço sem quebra são recusados (400 ou 422, com o corpo de erro do contrato) e **não ficam guardados** (o número de linhas na base não muda); campos obrigatórios em falta; 13 valores inválidos (datas, limites de tamanho, urgência 0 e importância 5, fase inexistente, estado calculado, sem responsável); os limites exatos (3 e 100 caracteres, 2000 num comentário) são aceites |
| `AuthorizationByIdentifierIntegrationTest` (5) | Caso 08, um cenário de autorização | Quem está fora do projeto (um membro de outro projeto e um gestor de outro projeto) tenta, com os identificadores reais do projeto alheio, **25 operações cada** (ler, alterar e apagar o projeto, a equipa, as tarefas, os responsáveis, os comentários e o restauro) e recebe sempre 403 ou 404; **a base fica exatamente igual** (uma impressão digital das cinco tabelas antes e depois). Um membro não faz o que é só do gestor nem mexe no comentário de outro (nem o gestor, A26); ninguém restaura um projeto apagado sem ser o gestor; não se mete uma tarefa ou uma pessoa de outro projeto pelo corpo do pedido. O controlo: os utilizadores legítimos fazem o mesmo com sucesso. Mutantes mortos: um de fora tratado como membro, a verificação do gestor, a do autor e a do restauro (esta última só morreu depois de acrescentar o caso do projeto já apagado) |
| `PersistenceAfterRestartIntegrationTest` (1) | Caso 03, criação com consulta posterior | Cria uma tarefa e um comentário pela API e consulta-os; depois arranca **uma segunda aplicação sobre a mesma base**, com a memória vazia: o cookie da sessão antiga já não vale (as sessões estão em memória, P32), a pessoa inicia sessão de novo e a tarefa e o comentário estão lá, com os mesmos dados |

## 3. Os nove cenários

Convenção: **[A]** automático, **[M]** manual. As capturas estão em `docs/evidence/`.

### 01 Acesso à aplicação
- **Esperado:** credenciais válidas entram; inválidas e pedidos sem autenticação são tratados.
- **Observado:**
  - **[M]** Ana com uma palavra-passe errada: "Credentials are not correct. Check your username and password and try again." ([captura](evidence/01-invalid-credentials.png)). Com a certa: entra e o cabeçalho diz "Ana / Team member" ([captura](evidence/01-valid-credentials.png)).
  - **[M]** Abrir `/projects/1` sem sessão leva ao início de sessão com "Sign in to continue." ([captura](evidence/01-request-without-session.png)). `GET /projects` sem cookie: `401 {"code":"UNAUTHENTICATED","message":"Sign in to continue."}`.
- **Evidência automática:** `AccessIntegrationTest` (`validCredentialsStartASessionWithTheRoleOfTheUser`, `wrongPasswordAndUnknownUserGetTheSameAnswer`, `missingFieldsAreRejectedBeforeAnyCheck`, `requestsWithoutASessionAreAskedToSignIn`, `afterSigningOutTheProjectsCannotBeOpenedAnymore`, `sessionLastsThreeHoursWithoutActivity`); testes de componentes `SignInPage.test.tsx` e `AuthProvider.test.tsx`; o teste de ponta a ponta inicia sessão duas vezes.
- **Resultado:** conforme.

### 02 Gestão de projetos
- **Esperado:** criar, pesquisar, editar e eliminar funciona de acordo com as permissões.
- **Observado:**
  - **[M]** Como Marta (gestora): criar "Verification Project" ([captura](evidence/02-project-created.png)); pesquisar "verif" mostra só esse ([captura](evidence/02-project-search.png)); editar o nome para "Verification Project (edited)" ([captura](evidence/02-project-edited.png)); eliminar pede confirmação ([captura](evidence/02-project-delete-confirmation.png)) e, ao confirmar, deixa de estar na lista, que fica com "Website, Mobile App" ([captura](evidence/02-project-deleted.png)).
  - **[M]** Como Ana (membro): a página de projetos só tem "Search"; não há "+ New project" nem *Edit* ou *Delete* ([captura](evidence/02-member-has-no-manager-controls.png)). Pela API, um membro a criar um projeto recebe 403 "Only a project manager can create projects.".
- **Evidência automática:** `ProjectsAndTeamIntegrationTest` (`aManagerCreatesAProjectThatShowsInTheirList`, `aTeamMemberCannotCreateProjects`, `onlyTheManagerChangesPhaseAndStatus`, `aCompletedProjectIsFrozen`), `AccessIntegrationTest` (`searchFindsProjectsByPartOfTheNameIgnoringCase`, `eachUserListsOnlyTheirOwnProjects`), `RecycleIntegrationTest` (`deletingAProjectSendsItWithItsTasksToTheBin`), `ProjectsPage.test.tsx`.
- **Resultado:** conforme.

### 03 Persistência de tarefas
- **Esperado:** a tarefa aparece sem recarregar e mantém-se depois de reiniciar o backend.
- **Observado:**
  - **[M]** Como Ana, em *Website* (Lista): criar "Persisted task". A lista mostra-a logo, sem recarregar a página, e o total passa a "6 tasks" ([captura](evidence/03-task-appears-without-reload.png)).
  - **[M] Reiniciar o backend de verdade** (`kill` do processo, a porta 8081 fica livre, `mvn spring-boot:run` outra vez sobre a mesma base, 6 segundos): o cookie da sessão antiga dá `401 "Your session has ended. Sign in again to continue."` (as sessões estão em memória); depois de iniciar sessão outra vez, a Lista tem "Design homepage, Persisted task, Verification task, Set up hosting, Write contact page copy, Contact page text" ([captura](evidence/03-task-after-restart.png)) e a tarefa tem título "Persisted task", estado "To do", prazo 2026-12-02, urgência 3 e importância 3, como foi criada.
- **Evidência automática:** `PersistenceAfterRestartIntegrationTest` (novo); `TasksIntegrationTest` (`aMemberCreatesATaskWithToDoStatusAndItsPriorities`); `ProjectDetailPage.test.tsx` (a lista e o total voltam a ser lidos depois de criar); o teste de ponta a ponta cria duas tarefas e vê-as na Matriz e na Lista.
- **Resultado:** conforme.

### 04 Validação de dados
- **Esperado:** campos obrigatórios vazios ou só com espaços e valores inválidos são rejeitados.
- **Observado:**
  - **[M]** No formulário de nova tarefa, com o título só com espaços: "The title must have from 3 to 150 characters and not be only spaces.", e os avisos "Choose the deadline.", "Choose the urgency.", "Choose the importance." e "Choose at least one assignee."; **a descrição escrita ficou** ([captura](evidence/04-task-form-validation.png)). O nome de um projeto só com espaços sem quebra: "The name must have from 3 to 100 characters and not be only spaces." e pede as datas.
  - **[M]** Os mesmos valores enviados direto à API: nome vazio, `400 VALIDATION_ERROR`; nome só com espaços sem quebra, `400 VALIDATION_ERROR`; fim antes do início, `422 END_BEFORE_START`.
- **Evidência automática:** `ValidationRulesIntegrationTest` (novo, 43 testes); `ProjectsAndTeamIntegrationTest` (`aNameOfOnlySpacesIsRefused`), `TasksIntegrationTest` (`aTitleOfOnlySpacesOrOfFewerThanThreeCharactersIsRefused`, `theDeadlineIsRequired`), `CommentsIntegrationTest` (`aCommentHasAtMost2000Characters`); `ProjectsPage.test.tsx`, `ProjectDetailPage.test.tsx` e `TaskPage.test.tsx` (avisos junto ao campo, nenhum pedido, foco no primeiro erro); `limits.test.ts` (os limites do frontend são os do contrato).
- **Resultado:** conforme.

### 05 Estado e prioridade
- **Esperado:** a edição atualiza a tarefa, os filtros e as contagens de forma coerente.
- **Observado (como Ana, em *Website*):**
  - **[M]** Criada "Verification task" com urgência 1 e importância 1: fica no quadrante **Later**; o painel diz "To do 2, In progress 1, In review 0, Blocked 1, Done 0, Cancelled 0, Duplicate 1" (5 tarefas principais).
  - **[M]** Editada para **estado In review, urgência 4 e importância 4** ([captura](evidence/05-task-edited.png)): passa para o quadrante **Do first**; o painel passa a "To do 1, In review 1" e as outras contagens ficam iguais ([captura](evidence/05-dashboard-counts.png)); a Lista filtrada por estado "In review" tem só "Verification task" (total "1 task"), por "In review" e quadrante "Do first" também ([captura](evidence/05-list-filtered.png)) e por "To do" tem "Persisted task, Write contact page copy". As contagens batem com o que a lista mostra.
- **Evidência automática:** `TasksIntegrationTest` (`anyMemberMovesTasksAndSubtasksToFinalStatuses`, `anUpdateThatChangesNothingKeepsTheVersion`), `ProjectTaskListIntegrationTest` (`eachTaskIsInTheQuadrantOfItsUrgencyAndImportance`, `theListIsFilteredByStatus`, `theFiltersCombine`), `DashboardIntegrationTest` (`theCountsAreTheMainTasksByStatus`), `TaskPage.test.tsx`, o teste de ponta a ponta (o filtro "To do" e as contagens).
- **Resultado:** conforme.

### 06 Responsáveis
- **Esperado:** associar e remover funciona; a mesma associação não fica duplicada.
- **Observado:**
  - **[M]** Em "Verification task": juntar o Rui fica "Ana, Rui" ([captura](evidence/06-assignees.png)) e o Rui deixa de ser oferecido na lista de quem se pode juntar ("Choose... | Marta | Luís"). Uma segunda atribuição do Rui **pela API** dá `409 ASSIGNEE_ALREADY_ASSIGNED` "This person is already assigned to the task." e os responsáveis continuam "Ana, Rui". Remover o Rui deixa "Ana"; com um só responsável o botão de remover deixa de aparecer (R26).
- **Evidência automática:** `TasksIntegrationTest` (`assigningSomeoneWhoIsAlreadyAnAssigneeIsRefused`, `theLastAssigneeCannotBeRemoved`, `aTaskCanHaveSeveralAssignees`, `twoRemovalsAtTheSameTimeCannotLeaveATaskWithoutAssignees`), `TaskPage.test.tsx`.
- **Resultado:** conforme.

### 07 Comentários
- **Esperado:** o conteúdo, o autor autenticado e a data ficam guardados e visíveis.
- **Observado:** **[M]** a Ana comenta "Verification comment: Falta o capítulo 2" e a lista mostra "Ana · 17 Nov 2026, 23:34" com o texto; a data de criação guardada é `2026-11-17T23:34:39.234011Z`; só a autora vê *Edit* e *Delete* ([captura](evidence/07-comment.png)).
- **Evidência automática:** `CommentsIntegrationTest` (`theAuthorAndTheDateAreSetByTheServerFromTheSession`, `theClientCannotChooseTheAuthorOrTheDate`, `commentsAreListedFromTheOldestToTheNewestByDate`, `nobodyButTheAuthorEditsOrDeletes`, `textIsStoredAndReturnedLiterally`), `PersistenceAfterRestartIntegrationTest` (o comentário sobrevive ao reinício), o teste de ponta a ponta (autor e data).
- **Resultado:** conforme.

### 08 Autorização na API
- **Esperado:** outro utilizador não acede nem altera recursos sem permissão, mesmo mudando os identificadores no pedido.
- **Observado:**
  - **[M]** Como Sofia (membro de outro projeto), com os ids reais do *Website* (`/projects/1`, `/tasks/1`, `/comments/1`): ler, alterar e apagar o projeto, a equipa, as tarefas, os responsáveis e os comentários, e injetar uma tarefa, um membro e um comentário: **todos os 15 pedidos dão 403 FORBIDDEN**. Na interface, abrir `/projects/1` e `/tasks/1` mostra "You do not have access to this project." ([captura](evidence/08-outsider-project.png)).
  - **[M]** Percorrer `GET /projects/{1..8}` como Sofia: só lê o `3` (o dela); `1`, `2` e `4` dão 403 e `5` a `8` dão 404.
  - **[M]** Ana (membro de *Website*): `PATCH` e `DELETE /projects/1`, `POST /projects/1/members` e `GET /projects/1/candidate-members` dão 403. Marta (gestora de *Website* e *Mobile App*): `PATCH` e `DELETE /projects/3` (de Pedro) dão 403. `GET /auth/me` responde sempre sobre quem tem a sessão: a identidade vem da sessão, não do pedido.
- **Evidência automática:** `AuthorizationByIdentifierIntegrationTest` (novo: 25 operações por cada um de dois utilizadores de fora, o que é só do gestor, o comentário alheio, o restauro, a injeção pelo corpo, e o controlo); `AccessIntegrationTest` (`aMemberOpensTheirProjectButNotSomeoneElses`, `identityComesFromTheSessionNotFromTheRequest`), `ProjectsAndTeamIntegrationTest` (`aTeamMemberGets403EvenWithAnInvalidBody`), `CommentsIntegrationTest` (`whoMayNotCommentGetsNoHintsAboutTheBody`).
- **Resultado:** conforme. **Observação (não alterada):** um id que existe mas é de outro projeto dá 403 e um que não existe dá 404, o que deixa quem tem sessão descobrir que ids existem. É o comportamento do contrato (403 sem acesso, 404 se não existe) e o risco é baixo; fica registado.

### 09 Eliminação e erros
- **Esperado:** a eliminação respeita as dependências; erros da API não geram mensagens de sucesso nem perda dos dados do formulário.
- **Observado:**
  - **[M] Dependências.** Eliminar "Design homepage" (tem uma subtarefa em aberto): "This task has subtasks that are still open. Close or delete them first.", a tarefa fica, não aparece nenhuma mensagem de sucesso ([captura](evidence/09-delete-with-open-subtask.png)). Eliminar o projeto *Website* leva as suas 6 tarefas: `GET /projects/1` e a sua lista de tarefas dão 404, a reciclagem mostra "Website · 6 tasks deleted with it" ([captura](evidence/09-project-in-recycle-bin.png)), e **restaurar** devolve as mesmas 6 tarefas.
  - **[M] Erro da API.** A Ana escreve uma descrição e a Marta altera a mesma tarefa entretanto; ao guardar: `409` "This task was changed by someone else. Reload it and try again.", não aparece "Changes saved." e **a descrição escrita ficou** ([captura](evidence/09-api-error-keeps-data.png)).
  - **[M] Servidor inacessível.** Com o pedido de guardar recusado pelo navegador (simulado com `route.abort`, não com o servidor parado): "The server cannot be reached. Check your connection and try again.", nenhum "Changes saved." e a descrição ficou ([captura](evidence/09-network-error-keeps-data.png)).
- **Evidência automática:** `RecycleIntegrationTest` (`aTaskWithOpenSubtasksIsNotDeleted` e os restantes de eliminar e recuperar), `TasksIntegrationTest` (`aParentWithOpenSubtasksCannotBeCompletedOrCancelled`, `aSaveFromAnOldVersionIsRefusedAndNothingIsLost`); testes de componentes que fixam o comportamento do erro: `TaskPage.test.tsx` ("keeps what was typed and shows the message of the API", "shows the message of the API when the task cannot be deleted, and stays"), `ProjectsPage.test.tsx` (criar com erro da API mantém os dados), `client.test.ts` (o erro de rede).
- **Resultado:** conforme.

## 4. Limitações, e o que não foi possível executar

- **As verificações manuais são feitas por um agente** com um navegador, e não por uma pessoa a olhar; as capturas e os textos são o que ficou. Um *script* de condução pontual foi usado e apagado, por isso não se repete com um comando: repete-se à mão com os passos acima, ou com a bateria automática, que cobre os mesmos cenários.
- **A rede em baixo** foi simulada no navegador (o pedido é recusado), não parando o servidor.
- **As 3 horas da sessão** não foram esperadas. Com o tempo limite encurtado para 1 minuto numa corrida à parte, a sessão respondeu 200 logo a seguir e `401 "Your session has ended. Sign in again to continue."` ao fim de 75 segundos sem atividade; o valor de 3 horas só se confirma pela configuração (`application.yml`).
- **O teste de ponta a ponta só corre localmente**, não no CI, e usa datas relativas a hoje (o "cerca de 30% do tempo decorrido" do cenário MVP-1 está nos testes do backend com relógio controlado).
- **Só o Chromium** foi usado; não se testaram outros navegadores, nem acessibilidade com leitor de ecrã, nem carga e desempenho.
- **A recusa por mais de 5 tarefas** num substituto e **a criação de um projeto por um membro** foram verificadas também no navegador com a API real, mas as outras recusas de regras de negócio estão só nos testes automáticos.
- **Dependências:** o `npm audit` indica 6 vulnerabilidades altas de um só aviso no `picomatch` (ferramentas de desenvolvimento, sem versão corrigida); `npm audit --omit=dev` dá 0. Ver o plano de implementação.

## 5. Defeitos encontrados e corrigidos

Seguiu-se o mesmo percurso em cada um: reproduzir, recolher evidência, investigar a causa, corrigir o mínimo e acrescentar um teste de regressão.

### D1 — Depois de terminar sessão, quem entra a seguir vai parar à página da pessoa anterior (encontrado pelo teste de ponta a ponta, incremento 13b)
- **Reprodução:** a Marta, em `/projects`, carrega em *Sign out*; a Ana inicia sessão. **Esperado:** o painel. **Observado:** `/projects`, a página onde a Marta estava (captura de ecrã da falha do teste de ponta a ponta).
- **Causa:** o `RequireAuth` guarda a página de origem (`state.from`) sempre que a sessão acaba, para voltar a ela depois de iniciar sessão; isso é certo quando a sessão expirou (cenário 10.5), mas não quando a pessoa terminou sessão de propósito, porque quem entra a seguir pode ser outra.
- **Correção:** só guarda a página de origem quando há uma mensagem (sessão que acabou ou ligação direta sem sessão); um *Sign out* voluntário não tem mensagem e não a guarda.
- **Regressão:** o teste `does not take whoever signs in next to the screen where the previous person signed out` (`AppShell.test.tsx`); o mutante que volta a guardar sempre a origem morre.

### D2 — O botão *Add* dos responsáveis ficava esticado e parecia desativado (encontrado ao rever as capturas desta verificação)
- **Reprodução:** abrir uma tarefa em que se possa juntar responsáveis. **Esperado:** um botão da altura do campo ao lado. **Observado:** um retângulo alto, com o texto "Add" a cinzento, ao lado da lista (ver as capturas 06 e 07 antes da correção).
- **Causa:** a linha do botão e da lista usava a classe `actions`, cujos filhos se esticam à altura da etiqueta mais a lista; já existe a variante `actions--end` que os alinha por baixo.
- **Correção:** a linha passa a ter `actions actions--end`, e o botão de apagar a tarefa ganhou uma margem para não colar ao de juntar subtarefa (visível nas mesmas capturas). Medido no navegador depois: botão e lista com 44 px.
- **Regressão:** o teste `aligns the Add button with the bottom of the list it belongs to` (`TaskPage.test.tsx`); com a marcação antiga falha. É um teste da classe, não do desenho: o aspeto só se confirma olhando (as capturas 06, 07 e 09 já são as de depois).

Não se encontrou nenhum outro defeito nesta verificação.

## 6. Problemas conhecidos

- A diferença entre 403 e 404 permite saber que ids existem (secção 3, caso 08).
- As 3 horas de sessão só se confirmam pela configuração (secção 4).
- O teste de ponta a ponta não está no CI (decisão da autora: fica para depois deste exercício).
- Vulnerabilidades de desenvolvimento sem correção disponível (`picomatch`).
