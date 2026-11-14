# Matriz — Plano de implementação

> Sequência de incrementos para construir a v1 (MVP) da Matriz, com componentes e ficheiros previstos, operações da API, critérios de aceitação e formas de verificação. Atualiza-se ao longo da implementação: o estado de cada incremento está na secção 2.
> Criado em 2026-11-12. Documentos de base: [requirements.md](requirements.md), [user-stories.md](user-stories.md), [constraints.md](constraints.md), [architecture.md](architecture.md), [database.md](database.md), [openapi.yaml](../openapi.yaml), [ui/wireframes.md](ui/wireframes.md) e a análise em [plan.md](plan.md).

## 1. Ponto de partida

| Tema | Estado |
|---|---|
| Âmbito | v1: F01 a F10, F17 e F22 ([requirements.md](requirements.md#6-âmbito-do-mvp-v1)) |
| Arquitetura | Monólito modular, com os módulos de [architecture.md](architecture.md#4-componentes-e-responsabilidades) |
| Stack | **Java 21, Spring Boot e PostgreSQL**, interface depois da API, sessão no servidor com cookie. Registada em [architecture.md](architecture.md#8-stack-e-decisões-técnicas) |
| Contrato | [openapi.yaml](../openapi.yaml): 28 operações, uma por cada ação da v1 |
| Permissões | Tabela em [requirements.md](requirements.md#21-permissões-por-papel-e-por-projeto) |
| Onde se desenvolve | Numa **VM Ubuntu**, que a autora indicou ter a stack instalada (por confirmar: versões). O PostgreSQL é o da VM; não se usa PostgreSQL embutido |
| Ordem | **API primeiro**: cada incremento entrega operações da API testadas contra o contrato. A interface escolhe-se depois do incremento 8, com os wireframes já feitos |

**Regras do plano**
- Um incremento só se dá por concluído quando os cenários indicados passam e as verificações estão feitas. Não se avança com um incremento a meio.
- As ferramentas marcadas como *proposta* na secção 3 confirmam-se no incremento 0, com a autora.
- Nada de palavras-passe, chaves ou dados reais nos ficheiros versionados (AGENTS, secção 7). Os dados de demonstração são fictícios.
- Cada incremento termina com a documentação revista: se uma regra mudou, atualizam-se todos os documentos afetados (AGENTS, secção 9).

## 2. Estado dos incrementos

Como atualizar: ao terminar um incremento, passar o estado a **Concluído**, indicar a data e como foi verificado (testes e cenários que passaram, ou o que não foi possível verificar).

| # | Incremento | Estado | Concluído em | Verificado por |
|---|---|---|---|---|
| 0 | Fundações | Concluído | 2026-11-12 | `mvn test`: 12 testes verdes (contrato OpenAPI válido com 28 operações, 9 regras de fronteira ArchUnit, health check e seed idempotente contra `matriz_test`). Aplicação arrancada duas vezes contra `matriz`: health `UP` com `db` UP; 10 utilizadores, 4 projetos, 14 tarefas e 5 comentários nas duas vezes, sem duplicar |
| 1 | Contas e sessão | Concluído | 2026-11-12 | Cenários 10.1 a 10.6, EC31 e EC38 em testes HTTP (15 testes de acesso, 28 no total, verdes): credenciais certas e erradas com a mesma resposta, 401 sem sessão, logout, identificador de sessão novo a cada início, e Ana, Inês, Marta e Pedro com acessos distintos (200 no seu projeto, 403 no alheio, 404 se não existe ou foi apagado). Confirmado com `curl` na aplicação a correr; nenhuma palavra-passe nem hash nos logs. Expiração às 3 horas verificada só pela configuração do servidor (não se espera 3 horas). Também já feitas, só em leitura, `listProjects` e `getProject` (incremento 2) para provar o acesso por projeto |
| 2 | Projetos e equipa | Concluído | 2026-11-13 | Cenários 1.1 a 1.23, 1.29 a 1.32 e 8.1 a 8.3 (os de tarefas, 1.12 a 1.14 e 1.24 a 1.28, ficam para o incremento 3); EC8, EC12 a EC14, EC17, EC25, EC27, EC37, EC39 (é da interface), EC40, EC42, EC44 e EC45, em testes HTTP com relógio controlável (38 testes novos; 70 no total, verdes). `removeMember` só remove membros sem tarefas em aberto (A11). Pressupostos A8 a A13 do contrato confirmados em 2026-11-13. Não verificado: a confirmação antes de concluir um projeto (1.22) é da interface |
| 3 | Tarefas, subtarefas e responsáveis | Concluído | 3a: 2026-11-13; 3b: 2026-11-14 | 3a (tarefas, subtarefas, responsáveis e versão, T13): cenários 2.1 a 2.13 (menos apagar, do incremento 7), 4.1 a 4.7 e 5.1 a 5.4; EC2 a EC5, EC10, EC19, EC22, EC28 a EC30, EC35, EC43, EC46 e EC47, em testes HTTP com relógio controlável e 5 testes de concorrência, que esperam pelo bloqueio em vez de dormir (38 testes novos; 108 no total, verdes). Verificado por mutação: sem o bloqueio de linha, sem a verificação da versão, sem a R4, sem a guarda da reabertura de subtarefas, sem o bloqueio da pessoa e sem a regra "não subir a versão" os testes falham. Pressupostos A14 a A20 do contrato, todos confirmados em 2026-11-13. 3b (remoção de membros com substitutos, R17 e P24): cenários 1.12 a 1.14 e 1.24 a 1.28; EC15, EC18, EC41 e a concorrência com a criação de tarefas, em 23 testes novos (131 no total, verdes); verificado por mutação (limite de 5, cobertura de todas as tarefas, R11 do substituto novo e a isenção dos gestores, P24, validação antes de bloquear, bloqueio das pessoas, guarda da ordem dos bloqueios e a regra ArchUnit). Pressupostos A21 a A23 confirmados em 2026-11-14. Todos os bloqueios seguem uma só ordem: pessoas por id, depois tarefas por id |
| 4 | Lista, Matriz, pesquisa e filtros | Por fazer | | |
| 5 | Painel e progresso | Por fazer | | |
| 6 | Comentários | Por fazer | | |
| 7 | Reciclagem e eliminações | Por fazer | | |
| 8 | Fecho do MVP | Por fazer | | |

## 3. Estrutura prevista

Os nomes são previsões, a rever no incremento 0. A organização segue os módulos da arquitetura: cada módulo só acede aos seus dados e a coordenação entre módulos fica nos casos de uso ([architecture.md](architecture.md#5-regras-de-fronteira)).

```
pom.xml                                   build e dependências
src/main/java/.../MatrizApplication       arranque
src/main/java/.../access/                 Acesso e Visibilidade: contas, sessão, permissões
src/main/java/.../projects/               Projetos e Equipas
src/main/java/.../tasks/                  Tarefas, responsáveis e comentários
src/main/java/.../views/                  Vistas e Progresso: listas, painel, pesquisa
src/main/java/.../recycle/                Reciclagem
src/main/java/.../usecases/               Casos de uso que atravessam módulos (R9, R17, R20, R21, R22, R27)
src/main/java/.../shared/                 Núcleo partilhado: datas e fuso (P26), erros, validação
src/main/java/.../api/                    Controladores REST e erros (o contrato OpenAPI)
src/main/resources/application.yml        configuração, sem segredos
src/main/resources/db/migration/          scripts da base de dados e do seed
src/test/java/...                         testes por cenário Given / When / Then
```

| Decisão técnica | Estado |
|---|---|
| Java 21, Spring Boot, PostgreSQL, sessão com cookie, API primeiro | **Decidido** |
| Build com Maven | Adotado (incremento 0) |
| Migrações com Flyway e acesso aos dados por JDBC (`JdbcTemplate`); o Spring Data JPA não se usa e foi retirado | Adotado (JDBC decidido em 2026-11-13, incremento 2) |
| Testes com JUnit 5 e Spring Boot Test, contra o PostgreSQL da VM Ubuntu, numa base de dados de teste separada | Adotado (incremento 0) |
| Datas e horas com `java.time` e o fuso do projeto (T5) | Proposta |
| Spring Security, com hash BCrypt, 401 e 403 em JSON | Adotado (incremento 1) |
| CSRF desligado e sessões em memória (P31, P32); sem limite de tentativas de início de sessão (P30) | Pressupostos, a rever com a interface e antes de dados reais |
| Edições simultâneas da mesma tarefa (T13): recusar a gravação desatualizada, com uma `version` na tarefa (R30) | Decidido em 2026-11-12; implementa-se no incremento 3 |

## 4. Incrementos

### Incremento 0 — Fundações

| | |
|---|---|
| **Componentes e ficheiros** | `pom.xml` e dependências; `MatrizApplication`; esqueleto dos módulos e de `usecases`, `shared` e `api`; `application.yml` com a ligação à base de dados por variáveis de ambiente; `.env.example` com valores fictícios; migração do esquema de utilizadores, projetos, equipa, tarefas, atribuições e comentários; dados de demonstração reproduzíveis; testes; comandos no `README.md` |
| **API** | Nenhuma operação do contrato. Um ponto de verificação do estado da aplicação (health check) |
| **Depende de** | Confirmar o que a VM Ubuntu tem instalado (Java 21, Maven e PostgreSQL) e criar uma base de dados e um utilizador para a aplicação, com credenciais fora do repositório |
| **Aceitação** | A aplicação arranca e liga à base de dados; o esquema é criado por migrações reproduzíveis; os dados de demonstração têm pelo menos dois utilizadores e projetos que permitem testar acessos distintos (aqui, 10 contas e 4 projetos); arrancar outra vez não apaga nem duplica dados; um teste Given / When / Then de exemplo passa; as fronteiras entre módulos são verificáveis |
| **Verificação** | Compilar e correr os testes; arrancar a aplicação e confirmar o estado da base de dados (health check); arrancar duas vezes e comparar o número de linhas; um teste que lê o [openapi.yaml](../openapi.yaml) e falha se o contrato for inválido; um teste automático que falha se um módulo depender de outro fora das regras da [secção 5 da arquitetura](architecture.md#5-regras-de-fronteira) |

### Incremento 1 — Contas e sessão (F10; US10)

| | |
|---|---|
| **Componentes e ficheiros** | `access/` (utilizadores, sessão, expiração às 3 horas sem atividade, palavras-passe guardadas de forma protegida), migração de `APP_USER`, seed de 10 contas fictícias (2 gestores e 8 membros, P21) |
| **API** | `signIn`, `signOut`, `getAuthenticatedUser` |
| **Depende de** | Incremento 0 |
| **Aceitação** | Cenários 10.1 a 10.6, EC31, EC38; mensagem de erro igual para utilizador ou palavra-passe errados |
| **Verificação** | Testes dos cenários; nenhuma palavra-passe em ficheiros versionados nem em logs; pedido sem sessão devolve 401 |

### Incremento 2 — Projetos e equipa (F01, F06; US01, US08)

| | |
|---|---|
| **Componentes e ficheiros** | `projects/`, `usecases/` (R11, R17 sem tarefas, P22), `access/` (permissões por projeto, R8, R19), `shared/` (datas, fuso, R16, R29), migrações de `PROJECT` e `PROJECT_MEMBER` |
| **API** | `listProjects`, `createProject`, `getProject`, `updateProject`, `listMembers`, `addMember`, `removeMember` (sem tarefas por concluir), `listCandidateMembers` |
| **Depende de** | Incremento 1 |
| **Aceitação** | Cenários 1.1 a 1.23, 1.29 a 1.32 e 8.1 a 8.3; EC8, EC12 a EC14, EC16, EC17, EC25, EC27, EC37, EC39, EC40, EC42, EC44 e EC45 |
| **Verificação** | Testes com relógio controlável; acesso a um projeto alheio recusado no servidor (403), também por ligação direta; conclusão de um projeto congela-o (R28) |

> **R19 confirmado em 2026-11-12:** em `createProject`, quem cria vem da sessão e tem de ser gestor (403 se não for); o `managerId` é indicado por quem cria (R19) e o servidor valida que tem o papel de gestor (M10, `MANAGER_NOT_A_MANAGER`). Se for outra pessoa, o criador deixa de ver o projeto. O exercício pede para inferir o proprietário no servidor: aqui, é o criador (sessão) que decide quem gere, e nunca um valor que prove identidade.

> `listProjects` e `getProject` (só leitura) e o serviço de permissões por projeto (`access/ProjectPermissions`) já foram feitos no incremento 1, para provar o acesso com dois utilizadores. Este incremento acrescenta as escritas e o resto das operações, reutilizando esse serviço.

### Incremento 3 — Tarefas, subtarefas e responsáveis (F02, F03, F08; US02, US04, US05)

| | |
|---|---|
| **Componentes e ficheiros** | `tasks/`, `usecases/` (R17 com substitutos, R21, R22, R26, R27), migrações de `TASK` e `TASK_ASSIGNEE` |
| **API** | `createTask`, `getTask`, `updateTask`, `addAssignee`, `removeAssignee`, `listAssignableUsers`, e `removeMember` completo (cenários 1.24 a 1.28) |
| **Depende de** | Incremento 2 e do wireframe da tarefa |
| **Aceitação** | Cenários 2.1 a 2.13, 4.x, 5.x e 1.24 a 1.28; EC2 a EC5, EC10, EC15, EC18, EC19, EC22, EC28 a EC30, EC35, EC41 e EC43 (edições simultâneas, R30) |
| **Verificação** | Testes de contrato contra o [openapi.yaml](../openapi.yaml); distinguir 400 de 422 e os códigos de erro; limite de 5 tarefas na substituição; duas gravações da mesma versão: a segunda dá 409 `TASK_CHANGED` e nada se perde (R30, T13) |

> **Passagem da gestão (A12), decidida em 2026-11-13:** o novo gestor, se já era membro, perde a linha de membro (M5) e mantém as suas tarefas (R19, R21). O antigo gestor deixa de ver o projeto, exceto se tiver tarefas nele: então fica como membro, para nenhuma tarefa ficar com quem já não a vê (R21). Está implementado no incremento 2 (sem código de erro novo). Como um gestor pode ser membro de projetos de outros gestores, a exceção da R11 aos gestores mantém-se.

> **T13 decidido em 2026-11-12:** a tarefa tem uma `version` (contrato 0.3.0, modelo M13). Como a migração V1 já foi aplicada, a coluna `version` entra numa migração nova (V2), que não altera a V1. O `updateTask` compara a versão recebida com a atual.

### Incremento 4 — Lista, Matriz, pesquisa e filtros (F04, F05, F22; US03, US09, US12)

| | |
|---|---|
| **Componentes e ficheiros** | `views/` (consultas de leitura: lista com ordem, "as minhas", filtros por estado e quadrante, pesquisa de projetos) |
| **API** | `listProjectTasks` com os filtros e `listProjects` com `search` |
| **Depende de** | Incremento 3 |
| **Aceitação** | Cenários 3.1 a 3.6, 9.1 a 9.5 e 12.1 a 12.6; EC20 e EC33 |
| **Verificação** | Testes da ordem (R23, cenários 3.6 e 9.5), da combinação de filtros e de que as subtarefas não aparecem sozinhas (R18) |

### Incremento 5 — Painel e progresso (F07; US06, US13)

| | |
|---|---|
| **Componentes e ficheiros** | `views/` (contagens por estado, progresso, estado calculado "Attention" e "Late"), `shared/` (cálculo do tempo decorrido, T5) |
| **API** | `getDashboard` |
| **Depende de** | Incremento 4 |
| **Aceitação** | Cenários 6.1 a 6.15 e 13.1 a 13.5; EC1, EC11, EC16 e EC24 |
| **Verificação** | Tempo fixo nos limites de 15 e 25 pontos e do dia de fim; arredondamento a duas casas; progresso só visível ao gestor; projetos concluídos fora do painel |

### Incremento 6 — Comentários (F17; US11)

| | |
|---|---|
| **Componentes e ficheiros** | `tasks/` (comentários), migração de `COMMENT` |
| **API** | `listComments`, `addComment`, `editComment`, `deleteComment` |
| **Depende de** | Incremento 3 |
| **Aceitação** | Cenários 11.1 a 11.10; EC32 e EC34 |
| **Verificação** | Testes de autoria (só o autor edita e apaga), projeto suspenso e concluído, subtarefas e texto mostrado literalmente (NF09) |

### Incremento 7 — Reciclagem e eliminações (F09; US07)

| | |
|---|---|
| **Componentes e ficheiros** | `recycle/`, `usecases/` (R9, R20, M4), tarefa agendada que elimina de vez ao fim de 30 dias |
| **API** | `deleteProject`, `deleteTask`, `listRecycleBin`, `restoreProject`, `restoreTask` |
| **Depende de** | Incrementos 2, 3 e 6 |
| **Aceitação** | Cenários 7.1 a 7.8; EC7, EC21, EC23 e EC36; os membros recuperam tarefas e subtarefas dos seus projetos, só o gestor recupera projetos |
| **Decisões** | **P36 (2026-11-14):** ao recuperar uma tarefa ou subtarefa, se algum responsável já não for membro do projeto nem o seu gestor, a recuperação pede primeiro um novo responsável (R21, R26). A remoção de membros (incremento 3b) só passa para substitutos as tarefas em aberto não apagadas |
| **Verificação** | Relógio controlado para os 29 e 31 dias; cascata e recuperação do projeto e das subtarefas; projeto concluído não se apaga (R28) |

### Incremento 8 — Fecho do MVP

| | |
|---|---|
| **Componentes e ficheiros** | Transversais (NF08, NF09), dados de demonstração, documentação |
| **API** | Todas as 28 operações cobertas |
| **Depende de** | Todos os anteriores |
| **Aceitação** | O cenário MVP-1 passa de ponta a ponta; o critério de aceitação do MVP ([requirements.md](requirements.md#6-âmbito-do-mvp-v1)) cumpre-se |
| **Verificação** | Todos os cenários e casos excecionais da v1 verdes; o contrato e a implementação coincidem; documentação revista |

## 5. Operações do contrato por incremento

| Incremento | Operações |
|---|---|
| 1 | `signIn`, `signOut`, `getAuthenticatedUser` |
| 2 | `listProjects`, `createProject`, `getProject`, `updateProject`, `listMembers`, `addMember`, `removeMember`, `listCandidateMembers` |
| 3 | `createTask`, `getTask`, `updateTask`, `addAssignee`, `removeAssignee`, `listAssignableUsers` |
| 4 | `listProjectTasks` |
| 5 | `getDashboard` |
| 6 | `listComments`, `addComment`, `editComment`, `deleteComment` |
| 7 | `deleteProject`, `deleteTask`, `listRecycleBin`, `restoreProject`, `restoreTask` |

## 6. Notas para retomar o incremento 0

Pontos de partida acordados com a autora em 2026-11-12, para quem continuar a implementação (na VM Ubuntu):

| Tema | Nota | Estado |
|---|---|---|
| Ferramentas | Maven, Spring Boot (Web, Validation, JDBC, Actuator; o Data JPA, previsto aqui, foi retirado no incremento 2), Flyway, JUnit 5 e Spring Boot Test, pacote base `com.matriz`, `java.time`. A autora disse não ter preferência | Aceites |
| Spring Boot | Usar a última versão 3.5.x estável (a verificar no Maven Central); evitar a linha 4.x até ser necessária, por ter mudanças de módulos e de bibliotecas | Proposta |
| Segurança | Adicionar o Spring Security só no incremento 1, para não gerar uma palavra-passe aleatória nos registos do incremento 0 | Proposta |
| Base de dados | PostgreSQL da VM, ligação por variáveis de ambiente (`DB_URL`, `DB_USER`, `DB_PASSWORD`), carregadas de um `.env` que não se versiona, com um `.env.example` de valores fictícios | Proposta |
| Esquema | Tabelas `app_user`, `project`, `project_member`, `task`, `task_assignee` e `task_comment`, segundo [database.md](database.md), em snake_case, com chaves, `CHECK` dos domínios e dos limites (R14, urgência e importância de 1 a 4) e a marca `deleted_at` | Proposta |
| Dados de demonstração | Um componente que corre ao arrancar, só se ainda não houver utilizadores, numa só transação. A palavra-passe das contas vem da variável `DEMO_PASSWORD` do `.env`, guardada como valor protegido; nunca em ficheiros versionados (AGENTS, secção 7) | Proposta |
| Dados de demonstração | 10 contas (P21): gestores Marta e Pedro; membros Ana, Rui, Luís, Sofia, Carla, Inês, Tiago e Diana. Projetos: *Website* (Marta; Ana, Rui e Luís), *Mobile App* (Marta; Inês e Tiago), *Intranet* (Pedro; Sofia e Carla) e *Old Site* (Pedro; concluído; Diana). Respeitam o R11 e dão acessos distintos; com tarefas, subtarefas, responsáveis e comentários | Proposta |
| Fronteiras | Um teste ArchUnit para as regras 2 e 3 da [secção 5 da arquitetura](architecture.md#5-regras-de-fronteira) | Proposta |
| Contrato | Um teste que lê o [openapi.yaml](../openapi.yaml) com o `swagger-parser` e falha se for inválido | Proposta |
| Ordem do trabalho | Antes de criar ficheiros, apresentar um plano curto à autora (AGENTS, secção 4). No fim, registar os comandos no `README.md` e passar o incremento 0 a **Concluído** na secção 2, com a data e a verificação | Regra |

## 7. Riscos e pontos em aberto

| Tema | Nota |
|---|---|
| Prazo | O MVP cresceu para 13 user stories; o prazo de cerca de 1 mês mantém-se por decisão da autora |
| Interface | Escolhe-se depois da API. Enquanto isso, a API testa-se por testes automáticos |
| Validação do contrato | Validado no Swagger Editor em 2026-11-12, sem erros. O incremento 0 acrescenta um teste automático que o valida a cada compilação |

## 8. Verificações adiadas para a fase da interface

Itens que a API não consegue verificar sozinha e que não se podem esquecer quando a interface for feita. Quando um item for verificado, indica-se aqui a data e como.

| # | Item | Origem | Incremento | Estado |
|---|---|---|---|---|
| V1 | Pedir uma confirmação antes de marcar um projeto como concluído, a avisar que deixa de poder ser alterado ou reaberto; ao cancelar, o estado mantém-se | Cenário 1.22, EC39, R28 | 2 | Por verificar na interface |
| V2 | Os controlos que a pessoa não pode usar não aparecem (criar projeto para membros, alterar o projeto e a equipa para membros, e tudo num projeto concluído). A API já os recusa com 403 ou 422 | Cenários 1.2, 1.3, 1.6, 1.21, NF08 | 2 | Por verificar na interface |
| V3 | Aviso ao gestor, ao retomar um projeto suspenso, de que há membros noutro projeto em aberto (a API devolve `inOtherOpenProject`) | Cenário 1.8, P22 | 2 | Por verificar na interface |
| V4 | Encaminhar para o início de sessão quando a sessão deixa de ser válida, mostrando a mensagem do servidor ("Your session has ended..." ou "Sign in to continue.") | Cenários 10.5 e 10.6, EC31, EC38, P33, W16 | 1 | Por verificar na interface |
| V6 | O texto das tarefas (título, descrição e motivo de bloqueio) mostra-se literalmente, sem formatação nem execução de código (por exemplo, `<b>x</b>`). A API guarda e devolve o texto tal como chega; a interface tem de o escapar | Cenário 2.8, NF09 | 3a | Por verificar na interface |
| V7 | Os controlos de criar tarefas e subtarefas não aparecem num projeto concluído ou suspenso, e os de editar, mudar de estado, atribuir e remover responsáveis não aparecem num projeto concluído. A API já os recusa (422) | Cenários 2.10 e 2.13, EC29, EC35 | 3a | Por verificar na interface |
| V8 | Ao remover um membro com tarefas, a interface mostra um aviso a pedir substituto (cenário 1.13), deixa escolher um substituto por tarefa, mostra quem está disponível (`listCandidateMembers`) e explica as recusas (substituto noutro projeto, mais de 5 tarefas, já responsável) | Cenários 1.12 a 1.14 e 1.24 a 1.28, EC15, EC18, EC41 | 3b | Por verificar na interface |
| V5 | A expiração às 3 horas sem atividade: hoje só se verifica a configuração do servidor, não uma sessão realmente expirada | Cenário 10.6, EC38, S5 | 1 | Por verificar (esperar o tempo, ou com a interface) |
