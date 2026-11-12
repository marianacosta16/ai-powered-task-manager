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
| 0 | Fundações | Por fazer | | |
| 1 | Contas e sessão | Por fazer | | |
| 2 | Projetos e equipa | Por fazer | | |
| 3 | Tarefas, subtarefas e responsáveis | Por fazer | | |
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
| Build com Maven | Proposta |
| Acesso aos dados com Spring Data JPA e migrações com Flyway | Proposta |
| Testes com JUnit 5 e Spring Boot Test, contra o PostgreSQL da VM Ubuntu, numa base de dados de teste separada | Proposta |
| Datas e horas com `java.time` e o fuso do projeto (T5) | Proposta |
| Edições simultâneas da mesma tarefa (T13) | Por decidir |

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
| **Aceitação** | Cenários 1.1 a 1.23, 1.29, 1.30 e 8.1 a 8.3; EC8, EC12 a EC14, EC16, EC17, EC25, EC27, EC37, EC39, EC40 e EC42 |
| **Verificação** | Testes com relógio controlável; acesso a um projeto alheio recusado no servidor (403), também por ligação direta; conclusão de um projeto congela-o (R28) |

### Incremento 3 — Tarefas, subtarefas e responsáveis (F02, F03, F08; US02, US04, US05)

| | |
|---|---|
| **Componentes e ficheiros** | `tasks/`, `usecases/` (R17 com substitutos, R21, R22, R26, R27), migrações de `TASK` e `TASK_ASSIGNEE` |
| **API** | `createTask`, `getTask`, `updateTask`, `addAssignee`, `removeAssignee`, `listAssignableUsers`, e `removeMember` completo (cenários 1.24 a 1.28) |
| **Depende de** | Incremento 2 e do wireframe da tarefa |
| **Aceitação** | Cenários 2.1 a 2.13, 4.x, 5.x e 1.24 a 1.28; EC2 a EC5, EC10, EC15, EC18, EC19, EC22, EC28 a EC30, EC35 e EC41 |
| **Verificação** | Testes de contrato contra o [openapi.yaml](../openapi.yaml); distinguir 400 de 422 e os códigos de erro; limite de 5 tarefas na substituição |

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
| Ferramentas | Maven, Spring Boot (Web, Validation, Data JPA, Actuator), Flyway, JUnit 5 e Spring Boot Test, pacote base `com.matriz`, `java.time`. A autora disse não ter preferência | Aceites |
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
| Edições simultâneas | T13 por decidir |
| Validação do contrato | Validado no Swagger Editor em 2026-11-12, sem erros. O incremento 0 acrescenta um teste automático que o valida a cada compilação |
