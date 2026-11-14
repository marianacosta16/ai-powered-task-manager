# Matriz

Aplicação de gestão de projetos e tarefas para equipas de trabalho, com uma **matriz urgente/importante** como vista principal para decidir o que fazer primeiro.

> Projeto de aprendizagem. Por agora contém só documentação (requisitos, arquitetura, dados, contrato da API, wireframes e plano); ainda não há código.

## O problema

Uma empresa quer uma ferramenta, do género Jira, Azure DevOps ou Trello, onde as equipas possam organizar projetos, gerir tarefas e acompanhar o progresso do trabalho. Não tem hoje nenhuma aplicação deste tipo e ainda não definiu funcionalidades, regras ou prioridades. A Matriz parte desse pedido e transforma-o numa especificação clara e verificável.

## Objetivos principais

1. **Organizar projetos:** criar projetos com fases e estados e formar a equipa de cada um.
2. **Gerir tarefas:** criar, atribuir e priorizar tarefas (com subtarefas) por urgência e importância.
3. **Acompanhar o progresso:** permitir ao gestor de projeto ver, por projeto e no conjunto, se o trabalho está dentro do prazo.

## Utilizadores

| Papel | Resumo |
|---|---|
| **Membro da equipa** | Gere as tarefas dos projetos de que faz parte |
| **Gestor de projeto** | Cria projetos e forma as equipas, gere as tarefas e acompanha o progresso dos seus projetos |

## Versões

A entrega segue a prioridade MoSCoW: **v1 (MVP) = Must**, **v2 = Should**, **v3 = Could**.

### Funcionalidades previstas para o MVP (v1)

- Projetos (nome, descrição, datas, fase e estado) e respetiva equipa: o gestor cria o projeto e adiciona membros.
- Tarefas com prazo obrigatório, vários responsáveis, urgência e importância em 4 níveis, e subtarefas com as suas regras de prazo e fecho.
- Vista **Matriz** por projeto (quadrantes *Do first*, *Schedule*, *Delegate* e *Later*) e vista **Lista**.
- **Painel inicial** com os projetos acessíveis e as contagens de tarefas por estado; para o gestor, também a barra de progresso e o estado calculado por projeto.
- Visibilidade por papel: o membro só vê os projetos de que faz parte; o gestor vê só os projetos de que é gestor. O utilizador inicia e termina sessão com uma conta de demonstração criada por seed.
- Reciclagem para recuperar projetos e tarefas apagados.
- Comentários nas tarefas (adicionar e listar, com autor e data).
- Pesquisa de projetos pelo nome e filtros de tarefas por estado e por quadrante.

### Versões seguintes
- **v2 (Should):** criação de contas pelo gestor, etiquetas, lembretes e alertas de atraso, arquivo de projetos e registo de auditoria.
- **v3 (Could):** Calendário, anexos, avisos por email ou SMS, importação e exportação, registo de tempo, tarefas repetidas, modelos de projeto e quadro por estados.

A lista completa, com justificações, está em [docs/requirements.md](docs/requirements.md).

## Organização da documentação

| Ficheiro | Conteúdo |
|---|---|
| [docs/requirements.md](docs/requirements.md) | Visão e objetivos, tipos de utilizadores, requisitos funcionais e não funcionais, regras de negócio, âmbito do MVP e funcionalidades fora do âmbito inicial |
| [docs/user-stories.md](docs/user-stories.md) | User stories do MVP, prioridades, critérios de aceitação (Given / When / Then) e casos excecionais |
| [docs/constraints.md](docs/constraints.md) | Restrições, pressupostos, requisitos de segurança e privacidade, decisões técnicas pendentes e limitações de âmbito |
| [docs/architecture.md](docs/architecture.md) | Decisão de arquitetura (monólito modular), justificação, componentes e responsabilidades |
| [docs/database.md](docs/database.md) | Modelo de dados da v1: entidades, atributos, relacionamentos, cardinalidades e diagrama ER (Mermaid) |
| [openapi.yaml](openapi.yaml) | Contrato OpenAPI 3.1 com as 28 operações da v1: sessão, projetos, equipa, tarefas, responsáveis, comentários, painel e reciclagem |
| [docs/ui/matrix-wireframe.md](docs/ui/matrix-wireframe.md) | Wireframe de baixa fidelidade da vista Matrix, que é o separador Matrix do detalhe do projeto (SVG), elementos, navegação e validação face ao MVP |
| [docs/ui/wireframes.md](docs/ui/wireframes.md) | Wireframes de início de sessão, painel inicial, projetos, detalhe do projeto, detalhe da tarefa, equipa e reciclagem, com elementos e navegação |
| [docs/implementation-plan.md](docs/implementation-plan.md) | Plano de implementação: incrementos, componentes, operações da API, critérios de aceitação, verificações e estado |
| [docs/plan.md](docs/plan.md) | Análise de lacunas, contradições e decisões pendentes |
| [AGENTS.md](AGENTS.md) | Instruções persistentes para agentes de desenvolvimento que trabalhem neste repositório |

### Convenções
- Os códigos identificam os itens em todos os documentos: **F** (funcionalidade), **R** (regra de negócio), **NF** (requisito não funcional), **C** (restrição), **P** (pressuposto), **S** (segurança), **T** (decisão técnica pendente), **US** (user story) e **EC** (caso excecional).
- Cada item está marcado como **Decisão** ou **Pressuposto**. As decisões foram tomadas pela autora do projeto em nome de um cliente fictício, por isso os pressupostos só teriam de ser validados com um cliente real se a aplicação viesse a ter um (é um trabalho de formação, C1).
- Os documentos estão em português; a interface da aplicação é em inglês.
- Estes documentos servem de contexto para as fases seguintes. Quando se usar IA para apoiar a escrita ou o desenvolvimento, convém rever a consistência entre eles.

## Como executar (incremento 0)

Pré-requisitos na VM Ubuntu: Java 21, Maven 3.8+ e PostgreSQL 16 (`java -version`, `mvn -v`, `psql --version`).

Todos os comandos abaixo correm na pasta `matriz/` do repositório (onde estão o `pom.xml` e o `.env.example`):

```bash
cd matriz
```

**1. Instalar o PostgreSQL** (uma vez):

```bash
sudo apt install postgresql postgresql-client
```

```bash
sudo systemctl enable --now postgresql
```

**2. Criar o utilizador e as bases de dados** (uma vez). O primeiro comando abre o terminal do PostgreSQL como administrador (`postgres`); pede a palavra-passe de sudo da VM e o prompt passa a `postgres=#`. A palavra-passe de `matriz_app` é escolhida por quem executa e não se guarda no repositório. Para sair, `\q`:

```bash
sudo -u postgres psql
```

Dentro do `psql`, um comando de cada vez (cada um deve responder `CREATE ROLE` ou `CREATE DATABASE`):

```sql
CREATE ROLE matriz_app LOGIN PASSWORD '<escolher-palavra-passe>';
CREATE DATABASE matriz OWNER matriz_app;
CREATE DATABASE matriz_test OWNER matriz_app;
```

**3. Configurar o ambiente.** Copiar o exemplo e preencher com as credenciais reais; o `.env` não é versionado:

```bash
cp .env.example .env
```

**4. Testar** (usa a base `matriz_test`, que os testes esvaziam):

```bash
set -a; . ./.env; set +a; mvn test
```

**5. Arrancar a aplicação.** O Flyway cria o esquema e, se ainda não houver utilizadores, cria as 10 contas e 4 projetos de demonstração (a palavra-passe das contas é `DEMO_PASSWORD`). Arrancar de novo não apaga nem duplica dados:

```bash
set -a; . ./.env; set +a; mvn spring-boot:run
```

Se a porta 8080 estiver ocupada por outro serviço, usar outra: `SERVER_PORT=8081 mvn spring-boot:run` (e `localhost:8081` abaixo).

**6. Confirmar o estado e a base de dados:**

```bash
curl -s localhost:8080/actuator/health
```

```bash
PGPASSWORD="$DB_PASSWORD" psql -h localhost -U matriz_app -d matriz -c "SELECT (SELECT count(*) FROM app_user) AS users, (SELECT count(*) FROM project) AS projects, (SELECT count(*) FROM task) AS tasks"
```

## Autenticação e sessão (incremento 1)

Mecanismo (T3, S5): **sessão no servidor, identificada por um cookie `SESSION`** (`HttpOnly`, `SameSite=Lax`). O ciclo completo está no contrato, em `securitySchemes` de [openapi.yaml](openapi.yaml).

| Passo | O que acontece |
|---|---|
| Iniciar sessão | `POST /auth/login` com `username` e `password`. Credenciais certas: `200` com `id`, `name` e `role`, e o cookie com um identificador novo. Erradas (utilizador ou palavra-passe): `401 INVALID_CREDENTIALS`, igual nos dois casos |
| Durante a sessão | O cliente envia o cookie em cada pedido. O servidor tira a identidade da sessão: quem faz o pedido e o autor dos comentários nunca vêm do pedido. O gestor de um projeto novo é indicado por quem o cria (R19) e validado no servidor (M10) |
| Perfil | `GET /auth/me` devolve o nome e o papel |
| Fim | `POST /auth/logout` (`204`), ou 3 horas sem qualquer pedido |
| Sem sessão válida | `401 UNAUTHENTICATED`. Se o pedido trazia cookie, a mensagem é "Your session has ended. Sign in again to continue."; caso contrário, "Sign in to continue." A interface deve levar o utilizador ao início de sessão e mostrar a mensagem |
| Sem acesso ao projeto | `403 FORBIDDEN`, também por ligação direta. Um projeto que não existe ou foi apagado dá `404` |

Regras de acesso (R8, R19, S1): o gestor só vê os projetos que gere e o membro só aqueles de que faz parte. Estão no módulo `access` (`ProjectPermissions`), que as tarefas, comentários e atribuições dos próximos incrementos vão reutilizar. Já estão implementadas as operações de leitura `listProjects` e `getProject`, para as testar com dois utilizadores.

Palavras-passe: guardadas só como hash BCrypt (nunca em texto simples, nem em respostas ou logs); mínimo de 8 caracteres (S5). As contas são de demonstração, com a palavra-passe de `DEMO_PASSWORD`. Utilizadores de demonstração: `marta` e `pedro` (gestores), `ana`, `rui`, `luis`, `sofia`, `carla`, `ines`, `tiago` e `diana` (membros).

**Decisões e limites:** a proteção CSRF está desligada por agora (API só JSON e cookie `SameSite=Lax`); rever quando houver interface (T1). Ao servir por HTTPS, ativar `server.servlet.session.cookie.secure`. As sessões ficam em memória: reiniciar a aplicação termina-as. Não há limite de tentativas de início de sessão (não está definido na documentação).

Experimentar (com a aplicação a correr em `localhost:8081`; substituir `<DEMO_PASSWORD>` pela do `.env`):

```bash
curl -i -c /tmp/ana.txt -H 'Content-Type: application/json' -d '{"username":"ana","password":"<DEMO_PASSWORD>"}' localhost:8081/auth/login
```

```bash
curl -s -b /tmp/ana.txt localhost:8081/auth/me
```

```bash
curl -s -b /tmp/ana.txt localhost:8081/projects
```

```bash
curl -i -b /tmp/ana.txt -X POST localhost:8081/auth/logout
```

## Projetos e equipa (incremento 2)

Operações: `createProject`, `updateProject`, `listMembers`, `addMember`, `removeMember` e `listCandidateMembers` (mais `listProjects` e `getProject`, do incremento 1). Quem faz o pedido vem sempre da sessão.

| Regra | Como se aplica |
|---|---|
| Criar projetos (F01, R19) | Só um gestor (403 se for membro). Quem cria indica o gestor, que tem de ter o papel de gestor (422 `MANAGER_NOT_A_MANAGER`); se for outra pessoa, o criador deixa de ver o projeto |
| Datas (R16, R29, P26) | Obrigatórias (400). O fim não é anterior ao início (422 `END_BEFORE_START`; um só dia sem horas é aceite). O início não está no passado, sendo hoje aceite (422 `START_DATE_IN_PAST`), no fuso do projeto (Lisboa por defeito) |
| Alterar o projeto (R12) | Só o gestor (403), que pode passá-lo a outro gestor: o novo gestor perde a linha de membro e o antigo fica como membro se tiver tarefas nele (A12). `Attention` e `Late` não se definem (400). Um projeto concluído não se altera nem reabre (422 `PROJECT_COMPLETED`, R28) |
| Equipa (R11, P20, P22) | Só o gestor adiciona e remove membros. Um membro de outro projeto em aberto é recusado (422 `MEMBER_IN_OTHER_OPEN_PROJECT`), exceto num projeto suspenso e para gestores. Ao retomar o projeto, quem entretanto entrou noutro projeto em aberto aparece com `inOtherOpenProject: true` (etiqueta vermelha) |
| Remover membros (R17, P24) | Cada tarefa e subtarefa em aberto do membro, também as partilhadas, passa a um substituto indicado em `reassignments` (422 `REPLACEMENT_REQUIRED` se faltar). O substituto é alguém do projeto, o gestor ou uma pessoa nova, que é adicionada ao projeto (R11, 422 `MEMBER_IN_OTHER_OPEN_PROJECT`); não pode já ser responsável pela tarefa (422 `REPLACEMENT_NOT_ELIGIBLE`); nenhum fica com mais de 5 tarefas em aberto no projeto (422 `ASSIGNEE_LIMIT_EXCEEDED`). Tudo ou nada, numa só transação |

Os pressupostos A8 a A13 estão confirmados no [openapi.yaml](openapi.yaml). Adicionar alguém que já está no projeto (ou o seu gestor) dá 422 `MEMBER_ALREADY_IN_PROJECT`.

```bash
curl -s -b /tmp/marta.txt -H 'Content-Type: application/json' -d '{"name":"Demo","startDate":"2027-01-10","endDate":"2027-03-31","phase":"Initiation","status":"Not started","managerId":1}' localhost:8081/projects
```

```bash
curl -s -b /tmp/marta.txt localhost:8081/projects/1/members
```

## Tarefas (incremento 3a)

Operações: `createTask`, `getTask`, `updateTask`, `addAssignee`, `removeAssignee` e `listAssignableUsers`. Quem faz o pedido vem sempre da sessão, e o acesso ao projeto (403) e o estado do projeto são verificados antes dos valores do pedido.

| Regra | Como se aplica |
|---|---|
| Criar (R1, R13, R14, R22) | Qualquer membro ou o gestor, num projeto em aberto (422 `PROJECT_NOT_OPEN` se concluído ou suspenso). Prazo obrigatório (400) e não no passado, sendo hoje aceite (422 `DEADLINE_IN_PAST`). Estado inicial "To do" |
| Responsáveis (R21, R26) | Membros do projeto ou o gestor (422 `ASSIGNEE_NOT_MEMBER`); há sempre pelo menos um (422 `LAST_ASSIGNEE`) |
| Subtarefas (R2, R3, A4) | O prazo da subtarefa não é posterior ao da mãe (422 `SUBTASK_DEADLINE_AFTER_PARENT`); antecipar a mãe para antes de uma subtarefa é bloqueado (422 `PARENT_DEADLINE_BEFORE_SUBTASK`); só um nível (422 `PARENT_IS_SUBTASK`); não se cria numa mãe fechada nem se reabre uma subtarefa com a mãe fechada (422 `PARENT_TASK_CLOSED`, P35) |
| Estados (R4, R5, R6, P8) | A mãe não se conclui nem cancela com subtarefas em aberto (422 `PARENT_HAS_OPEN_SUBTASKS`). "Blocked" exige motivo (422 `BLOCK_REASON_REQUIRED`) e "Duplicate" exige uma original válida (422 `ORIGINAL_TASK_REQUIRED` ou `ORIGINAL_TASK_NOT_VALID`). Reabrir uma tarefa final só o gestor (403) |
| Projeto concluído (R27, R28) | Nada se altera (422 `PROJECT_COMPLETED`); num projeto suspenso edita-se e atribui-se, mas não se cria |
| Edições simultâneas (T13, R30) | O `updateTask` leva a `version` lida; se a tarefa mudou entretanto, 409 `TASK_CHANGED` e nada se grava. Uma gravação que não altera nada não sobe a versão (A20) |

Os pressupostos A14 a A23 estão confirmados no [openapi.yaml](openapi.yaml). Apagar tarefas é do incremento 7.

```bash
curl -s -b /tmp/ana.txt -H 'Content-Type: application/json' -d '{"title":"Write report","deadline":"2027-01-15","urgency":3,"importance":4,"assigneeIds":[3]}' localhost:8081/projects/1/tasks
```

```bash
curl -s -X PATCH -b /tmp/ana.txt -H 'Content-Type: application/json' -d '{"version":1,"status":"In progress"}' localhost:8081/tasks/1
```

## Verificação automática (CI)

O GitHub Actions corre `mvn verify` (testes, contrato OpenAPI e fronteiras entre módulos) em cada pull request e em cada push à `main`, com um PostgreSQL de teste criado pelo próprio workflow ([.github/workflows/ci.yml](../.github/workflows/ci.yml)). As credenciais dessa base são valores fictícios.

## Estado atual

| Fase | Estado |
|---|---|
| Descrição do problema e perguntas de clarificação | Concluído |
| Análise de requisitos e âmbito do MVP | Concluído (só o P14, da v2, por validar) |
| User stories e critérios de aceitação da v1 | Concluído (13 stories, cobrem F01 a F10, F17 e F22) |
| User stories da v2 | Por fazer (F11 a F14 e F23) |
| Conciliação com o novo âmbito funcional | Registada em [docs/requirements.md](docs/requirements.md#8-conciliação-com-o-novo-âmbito-funcional); pressupostos P27 a P29 confirmados |
| Base de conhecimento (esta documentação) | Concluído |
| Decisões técnicas e desenho | Concluído em grande parte: arquitetura e stack decididas (ver [docs/architecture.md](docs/architecture.md)), modelo de dados ([docs/database.md](docs/database.md)), contrato OpenAPI com as 28 operações da v1 ([openapi.yaml](openapi.yaml)) e wireframes dos ecrãs. Restam decisões técnicas (ver T1, T8, T10 e T11 em [docs/constraints.md](docs/constraints.md#4-decisões-técnicas-pendentes)) |
| Plano de implementação | Criado, com 9 incrementos (ver [docs/implementation-plan.md](docs/implementation-plan.md)) |
| Implementação | Em curso: incrementos 0 (fundações), 1 (contas e sessão) e 2 (projetos e equipa) concluídos; próximo, o incremento 3 (tarefas, subtarefas e responsáveis) |

Stack decidida: Java 21, Spring Boot e PostgreSQL, com a interface do utilizador escolhida depois da API.
