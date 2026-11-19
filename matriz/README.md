# Matriz

Aplicação web de gestão de projetos e tarefas para equipas de trabalho, com uma **matriz urgente/importante** como vista principal para decidir o que fazer primeiro.

**Versão 1.0.0** (a v1, o MVP): backend (API REST) e interface web completos, com testes automáticos e execução com Docker Compose. É um projeto de aprendizagem: as decisões foram tomadas pela autora em nome de um cliente fictício. O histórico de alterações está no [CHANGELOG](CHANGELOG.md) e as notas de lançamento em [docs/release-notes-1.0.0.md](docs/release-notes-1.0.0.md).

## Índice

[Funcionalidades](#funcionalidades) · [Tecnologias](#tecnologias) · [Pré-requisitos](#pré-requisitos) · [Instalação e configuração](#instalação-e-configuração) · [Execução com Docker Compose](#execução-com-docker-compose) · [Execução em desenvolvimento](#execução-em-desenvolvimento-sem-docker) · [Testes](#executar-os-testes) · [API](#api-http) · [Regras e exemplos por funcionalidade](#regras-e-exemplos-por-funcionalidade) · [Documentação adicional](#documentação-adicional) · [Estado e limitações](#estado-e-limitações-conhecidas)

## O problema e os objetivos

Uma empresa quer uma ferramenta, do género Jira, Azure DevOps ou Trello, onde as equipas organizem projetos, giram tarefas e acompanhem o progresso do trabalho. A Matriz parte desse pedido e transforma-o numa especificação clara e verificável.

1. **Organizar projetos:** criar projetos com fases e estados e formar a equipa de cada um.
2. **Gerir tarefas:** criar, atribuir e priorizar tarefas (com subtarefas) por urgência e importância.
3. **Acompanhar o progresso:** permitir ao gestor de projeto ver, por projeto e no conjunto, se o trabalho está dentro do prazo.

| Papel | Resumo |
|---|---|
| **Membro da equipa** | Gere as tarefas dos projetos de que faz parte |
| **Gestor de projeto** | Cria projetos e forma as equipas, gere as tarefas e acompanha o progresso dos seus projetos |

## Funcionalidades

Tudo o que se segue está implementado e testado na versão 1.0.0:

- **Projetos** (nome, descrição, datas, fuso horário, fase e estado) e respetiva **equipa**: o gestor cria o projeto e adiciona ou remove membros (um membro só pode estar num projeto em aberto de cada vez; ao remover alguém, as suas tarefas passam a um substituto).
- **Tarefas** com prazo obrigatório, vários responsáveis, urgência e importância em 4 níveis, **subtarefas** com as suas regras de prazo e de fecho, e deteção de edições simultâneas.
- Vista **Matriz** por projeto (quadrantes *Do first*, *Schedule*, *Delegate* e *Later*) e vista **Lista**, com filtros por estado, quadrante e "as minhas".
- **Painel inicial**: os projetos acessíveis e as contagens de tarefas por estado; para o gestor, também a barra de progresso e o estado calculado (*Attention* e *Late*).
- **Visibilidade por papel**: o membro só vê os projetos de que faz parte e o gestor só os que gere (também por ligação direta). Início e fim de sessão com contas de demonstração.
- **Comentários** nas tarefas (autor e data; só o autor edita ou apaga o seu).
- **Reciclagem**: recuperar projetos e tarefas apagados, durante 30 dias.
- **Pesquisa** de projetos pelo nome.
- Interface em inglês, para computador.

Ficam para a v2 (criação de contas pelo gestor, etiquetas, lembretes e alertas de atraso, arquivo de projetos e registo de auditoria) e para a v3 (calendário, anexos, avisos por email ou SMS, importação e exportação, registo de tempo, tarefas repetidas, modelos de projeto e quadro por estados). A lista completa, com justificações, está em [docs/requirements.md](docs/requirements.md).

## Tecnologias

| Camada | Tecnologias |
|---|---|
| Backend (`matriz/`) | Java 21, Spring Boot 3.5.16 (Web, Validation, JDBC, Security, Actuator), Flyway (esquema), PostgreSQL 16, Maven. Arquitetura de monólito modular ([docs/architecture.md](docs/architecture.md)) |
| Contrato | OpenAPI 3.1 ([openapi.yaml](openapi.yaml)), 28 operações |
| Frontend (`frontend/`) | React 19, TypeScript, Vite 8, React Router 7 |
| Testes | Backend: JUnit 5, Spring Boot Test (com PostgreSQL de teste), ArchUnit, swagger-parser. Frontend: Vitest 5 e Testing Library, oxlint. Ponta a ponta: Playwright (Chromium) |
| Entrega | Docker e Docker Compose (nginx 1.29 para a interface), GitHub Actions |

## Pré-requisitos

Para **executar** (Docker): Docker Engine com Docker Compose v2.

Para **desenvolver** sem Docker: Java 21, Maven 3.8+, PostgreSQL 16 (com o cliente `psql`), Node 24 com npm e, para o teste ponta a ponta, o Chromium do Playwright (`npx playwright install chromium`, uma vez). Estas versões são as que o projeto usa; foi verificado em Ubuntu 24.04 com Docker 29.7.2, Compose 2.40.3, Java 21.0.13, Maven 3.8.7, Node 24.19.0 e npm 11.17.0.

## Instalação e configuração

```bash
git clone https://github.com/marianacosta16/ai-powered-task-manager.git
cd ai-powered-task-manager
```

A configuração vem de ficheiros `.env`, que **não são versionados** (cada um tem um `.env.example` com valores falsos). Nenhuma palavra-passe está no repositório.

| Ficheiro | Para quê | Variáveis |
|---|---|---|
| `.env` (raiz) | Docker Compose | `DB_PASSWORD` (obrigatória: palavra-passe da base de dados), `DEMO_PASSWORD` (obrigatória: palavra-passe das 10 contas de demonstração, com 8 caracteres ou mais) e, opcionais, `DB_USER`, `DB_NAME`, `WEB_HTTP_PORT` (por omissão 8080) e `WEB_HTTPS_PORT` (por omissão 8443) |
| `matriz/.env` | Backend e testes sem Docker | `DB_URL`, `DB_USER`, `DB_PASSWORD` (base de desenvolvimento), `TEST_DB_URL`, `TEST_DB_USER`, `TEST_DB_PASSWORD` (base de teste, cujo nome tem de acabar em `_test`: os testes esvaziam-na), `DEMO_PASSWORD` e `CORS_ALLOWED_ORIGINS` (origens da interface, exatas e sem `*`; por omissão `http://localhost:5173`). A porta do backend é `SERVER_PORT` (por omissão 8080) |
| `frontend/.env` | Interface em desenvolvimento | `VITE_API_URL`: o endereço do backend (por omissão `http://localhost:8081`). Num *build* é obrigatório; nas imagens Docker é `/api` |

Sem `DEMO_PASSWORD` a aplicação arranca mas **não há contas** (não existe registo de utilizadores na v1).

**Contas de demonstração** (criadas no primeiro arranque, com a palavra-passe de `DEMO_PASSWORD`): `marta` e `pedro` (gestores) e `ana`, `rui`, `luis`, `sofia`, `carla`, `ines`, `tiago` e `diana` (membros), com 4 projetos de exemplo (*Website*, *Mobile App*, *Intranet* e *Old Site*, este concluído). Arrancar de novo não apaga nem duplica dados.

## Execução com Docker Compose

A base de dados, a API e a interface arrancam com um só comando, sem instalar Java, Node nem PostgreSQL ([compose.yaml](../compose.yaml)).

```bash
cp .env.example .env     # na raiz do projeto; preencha DB_PASSWORD e DEMO_PASSWORD
docker compose up --build --wait
```

Abra `https://localhost:8443` e entre com uma conta de demonstração. O certificado é autoassinado, por isso o navegador avisa; para usar um certificado a sério, monte `tls.crt` e `tls.key` em `/etc/nginx/certs` do serviço `web` (um certificado montado nunca é substituído; o autoassinado renova-se sozinho quando faltam menos de 30 dias). `http://localhost:8080` redireciona para o HTTPS. As portas mudam-se no `.env` (`WEB_HTTP_PORT` e `WEB_HTTPS_PORT`, por exemplo 80 e 443 onde estiverem livres).

| Serviço | Imagem | Notas |
|---|---|---|
| `db` | `postgres:16` | Volume `matriz-db`; **sem porta publicada**; healthcheck `pg_isready` |
| `api` | `matriz/Dockerfile` (Maven, depois JRE 21, utilizador sem privilégios) | **Sem porta publicada**; healthcheck em `/actuator/health`; espera que a `db` esteja saudável |
| `web` | `frontend/Dockerfile` (Node, depois nginx) | Publica as portas; serve a interface e encaminha `/api` para a `api`; healthcheck pede `/api/actuator/health` pelo proxy; volume `matriz-certs` |

Os três serviços partilham uma rede privada do Compose. A interface e a API ficam no mesmo endereço (a API em `/api`), por isso o cookie de sessão não atravessa sites. Diagrama em [docs/architecture.md](docs/architecture.md#execução-como-os-componentes-correm).

Comandos úteis (na raiz do projeto, com o `.env`):

```bash
docker compose ps                  # estado e saúde dos serviços
docker compose logs --tail=50      # registos
docker compose down                # para; os dados ficam no volume
docker compose down -v             # para e apaga os dados
```

- Os dados persistem em volumes (`down` mantém-nos, `down -v` apaga-os).
- As sessões estão em memória: reiniciar a API termina-as, e só pode haver uma instância da API.
- O estado `unhealthy` do `docker compose ps` é informativo: o Docker não reinicia um contentor só por isso (reinicia se o processo terminar). Veja `docker compose logs` e reinicie o serviço com `docker compose restart <serviço>`.
- A `DB_PASSWORD` só é usada quando a base de dados é criada pela primeira vez. Se a mudar no `.env` depois disso, a API deixa de entrar na base e reinicia em ciclo; para recomeçar com outra, `docker compose down -v` (apaga os dados) e volte a arrancar. Os comandos `docker compose` precisam do `.env` (ou das variáveis) também para parar.

## Execução em desenvolvimento (sem Docker)

Todos os comandos do backend correm na pasta `matriz/` (onde estão o `pom.xml` e o `.env.example`).

**1. Instalar o PostgreSQL** (uma vez):

```bash
sudo apt install postgresql postgresql-client
sudo systemctl enable --now postgresql
```

**2. Criar o utilizador e as bases de dados** (uma vez). O primeiro comando abre o terminal do PostgreSQL como administrador (`postgres`); a palavra-passe de `matriz_app` é escolhida por quem executa e não se guarda no repositório. Para sair, `\q`:

```bash
sudo -u postgres psql
```

```sql
CREATE ROLE matriz_app LOGIN PASSWORD '<escolher-palavra-passe>';
CREATE DATABASE matriz OWNER matriz_app;
CREATE DATABASE matriz_test OWNER matriz_app;
```

**3. Configurar o ambiente** (o `.env` não é versionado):

```bash
cd matriz
cp .env.example .env     # preencher DB_*, TEST_DB_*, DEMO_PASSWORD e CORS_ALLOWED_ORIGINS
```

**4. Arrancar o backend.** O Flyway cria o esquema e, se ainda não houver utilizadores, cria as 10 contas e os projetos de demonstração. A interface em desenvolvimento espera o backend na porta 8081:

```bash
set -a; . ./.env; set +a; SERVER_PORT=8081 mvn spring-boot:run
```

**5. Confirmar o estado** (numa segunda janela):

```bash
curl -s localhost:8081/actuator/health
```

**6. Arrancar a interface** (noutra janela; abre em `http://localhost:5173`):

```bash
cd frontend
npm ci
npm run dev
```

Se a porta 8081 estiver ocupada, use outra no backend (`SERVER_PORT`) e diga-a à interface com `VITE_API_URL=http://localhost:<porta> npm run dev`.

## Executar os testes

| O quê | Comando | Pré-requisitos | Último resultado (2026-11-19) |
|---|---|---|---|
| Backend: testes, contrato OpenAPI, fronteiras entre módulos | `cd matriz && set -a && . ./.env && set +a && mvn -B verify` | PostgreSQL com a base `matriz_test` e o `matriz/.env`; demora cerca de 2 minutos | **352 testes, 0 falhas** |
| Frontend: componentes | `cd frontend && npm test` | `npm ci` | **272 testes, 0 falhas** (21 ficheiros) |
| Frontend: lint e build | `npm run lint` e `VITE_API_URL=https://matriz.invalid npm run build` | `npm ci` | Sem erros |
| Ponta a ponta (Playwright, cenário MVP-1) | `cd frontend && npm run e2e` | Backend e `matriz_test` como acima, `psql` e o Chromium do Playwright; as portas 8081 e 5173 livres | **1 teste, passou** |
| Teste de fumo do Docker Compose | `DB_PASSWORD=... DEMO_PASSWORD=... .github/scripts/docker-smoke.sh` | Docker e `jq`; usa as portas 8080 e 8443 e o seu próprio projeto Compose (`matriz-smoke`), que apaga no fim | Passou nas últimas execuções do CI |

O que os testes do backend cobrem, e como se garante que o contrato e o código não se desviam, está em [Contrato, concorrência e cenários de aceitação](#contrato-concorrência-e-cenários-de-aceitação). O plano de testes e o relatório de verificação estão em [docs/test-plan.md](docs/test-plan.md) e [docs/verification.md](docs/verification.md).

**Integração contínua.** O GitHub Actions ([.github/workflows/ci.yml](../.github/workflows/ci.yml)) corre em cada pull request e em cada push à `main`: `test` (backend, com um PostgreSQL criado pelo próprio workflow), `frontend` (lint, testes e build), `e2e` (Playwright) e `docker` (constrói as imagens e faz o teste de fumo do Compose, **só depois de `test` e `frontend` passarem**). As credenciais do workflow são valores falsos que só existem durante o job.

## API HTTP

A API é descrita pelo contrato [openapi.yaml](openapi.yaml) (28 operações, versão 1.1.0 do contrato) e resumida em [docs/api.md](docs/api.md): convenções (sessão, erros, listas paginadas) e uma tabela com todas as operações. A aplicação não serve uma página de documentação interativa (Swagger UI); o contrato lê-se no ficheiro ou num visualizador de OpenAPI.

## Regras e exemplos por funcionalidade

Os exemplos usam `curl` contra o backend em `localhost:8081` (execução em desenvolvimento); com o Docker Compose use `-k https://localhost:8443/api/...`. Substitua `<DEMO_PASSWORD>` pela palavra-passe do `.env`.

### Autenticação e sessão

Mecanismo (T3, S5): **sessão no servidor, identificada por um cookie `SESSION`** (`HttpOnly`, `SameSite=Lax`). O ciclo completo está no contrato, em `securitySchemes` de [openapi.yaml](openapi.yaml).

| Passo | O que acontece |
|---|---|
| Iniciar sessão | `POST /auth/login` com `username` e `password`. Credenciais certas: `200` com `id`, `name` e `role`, e o cookie com um identificador novo. Erradas (utilizador ou palavra-passe): `401 INVALID_CREDENTIALS`, igual nos dois casos |
| Durante a sessão | O cliente envia o cookie em cada pedido. O servidor tira a identidade da sessão: quem faz o pedido e o autor dos comentários nunca vêm do pedido. O gestor de um projeto novo é quem o cria, tirado da sessão (R19, A44); o pedido não leva gestor |
| Perfil | `GET /auth/me` devolve o nome e o papel |
| Fim | `POST /auth/logout` (`204`), ou 3 horas sem qualquer pedido |
| Sem sessão válida | `401 UNAUTHENTICATED`. Se o pedido trazia cookie, a mensagem é "Your session has ended. Sign in again to continue."; caso contrário, "Sign in to continue." A interface deve levar o utilizador ao início de sessão e mostrar a mensagem |
| Sem acesso ao projeto | `403 FORBIDDEN`, também por ligação direta. Um projeto que não existe ou foi apagado dá `404` |
| CORS | Só as origens de `matriz.cors.allowed-origins` (variável `CORS_ALLOWED_ORIGINS`; por omissão `http://localhost:5173`) chamam a API de um navegador, com o cookie, e podem ler o cabeçalho `X-Total-Count`. Um `*` ou uma `/` no fim impedem o arranque. O CSRF continua desligado (P31) |

Regras de acesso (R8, R19, S1): o gestor só vê os projetos que gere e o membro só aqueles de que faz parte. Estão no módulo `access` (`ProjectPermissions`), que as tarefas, os comentários, as atribuições e as vistas reutilizam.

Palavras-passe: guardadas só como hash BCrypt (nunca em texto simples, nem em respostas ou logs); mínimo de 8 caracteres (S5). As contas são de demonstração, com a palavra-passe de `DEMO_PASSWORD`. Utilizadores de demonstração: `marta` e `pedro` (gestores), `ana`, `rui`, `luis`, `sofia`, `carla`, `ines`, `tiago` e `diana` (membros).

**Decisões e limites:** a proteção CSRF está desligada por agora (API só JSON e cookie `SameSite=Lax`); revisto com a interface (P31). Ao servir por HTTPS, ativar `server.servlet.session.cookie.secure` (o Docker Compose fá-lo com `SERVER_SERVLET_SESSION_COOKIE_SECURE=true`). As sessões ficam em memória: reiniciar a aplicação termina-as. Não há limite de tentativas de início de sessão (não está definido na documentação).

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

### Projetos e equipa

Operações: `createProject`, `updateProject`, `listMembers`, `addMember`, `removeMember` e `listCandidateMembers` (mais `listProjects` e `getProject`). Quem faz o pedido vem sempre da sessão.

| Regra | Como se aplica |
|---|---|
| Criar projetos (F01, R19, A44) | Só um gestor (403 se for membro), que passa a ser o gestor do projeto: o pedido não leva `managerId` (um `managerId` dá 400). Para passar a gestão usa-se `updateProject` (A12, M10). O nome tem de 3 a 100 caracteres, sem contar os espaços das pontas para o mínimo, e não pode ser só espaços nem só caracteres invisíveis comuns (400, A46); a descrição até 1000 (A45) |
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

### Tarefas

Operações: `createTask`, `getTask`, `updateTask`, `addAssignee`, `removeAssignee` e `listAssignableUsers`. Quem faz o pedido vem sempre da sessão, e o acesso ao projeto (403) e o estado do projeto são verificados antes dos valores do pedido.

| Regra | Como se aplica |
|---|---|
| Criar (R1, R13, R14, R22) | Qualquer membro ou o gestor, num projeto em aberto (422 `PROJECT_NOT_OPEN` se concluído ou suspenso). Título de 3 a 150 caracteres e não só espaços (400, A46). Prazo obrigatório (400) e não no passado, sendo hoje aceite (422 `DEADLINE_IN_PAST`). Estado inicial "To do" |
| Responsáveis (R21, R26) | Membros do projeto ou o gestor (422 `ASSIGNEE_NOT_MEMBER`); há sempre pelo menos um (422 `LAST_ASSIGNEE`); atribuir quem já é responsável é recusado (409 `ASSIGNEE_ALREADY_ASSIGNED`, A16) |
| Subtarefas (R2, R3, A4) | O prazo da subtarefa não é posterior ao da mãe (422 `SUBTASK_DEADLINE_AFTER_PARENT`); antecipar a mãe para antes de uma subtarefa é bloqueado (422 `PARENT_DEADLINE_BEFORE_SUBTASK`); só um nível (422 `PARENT_IS_SUBTASK`); não se cria numa mãe fechada nem se reabre uma subtarefa com a mãe fechada (422 `PARENT_TASK_CLOSED`, P35) |
| Estados (R4, R5, R6, P8) | A mãe não se conclui nem cancela com subtarefas em aberto (422 `PARENT_HAS_OPEN_SUBTASKS`). "Blocked" exige motivo (422 `BLOCK_REASON_REQUIRED`) e "Duplicate" exige uma original válida (422 `ORIGINAL_TASK_REQUIRED` ou `ORIGINAL_TASK_NOT_VALID`). Reabrir uma tarefa final só o gestor (403) |
| Projeto concluído (R27, R28) | Nada se altera (422 `PROJECT_COMPLETED`); num projeto suspenso edita-se e atribui-se, mas não se cria |
| Edições simultâneas (T13, R30) | O `updateTask` leva a `version` lida; se a tarefa mudou entretanto, 409 `TASK_CHANGED` e nada se grava. Uma gravação que não altera nada não sobe a versão (A20) |

Os pressupostos A14 a A23 (e A46 e A47) estão confirmados no [openapi.yaml](openapi.yaml). Apagar tarefas está em [Reciclagem](#reciclagem).

```bash
curl -s -b /tmp/ana.txt -H 'Content-Type: application/json' -d '{"title":"Write report","deadline":"2027-01-15","urgency":3,"importance":4,"assigneeIds":[3]}' localhost:8081/projects/1/tasks
```

```bash
curl -s -X PATCH -b /tmp/ana.txt -H 'Content-Type: application/json' -d '{"version":1,"status":"In progress"}' localhost:8081/tasks/1
```

### Comentários

Operações: `listComments`, `addComment`, `editComment` e `deleteComment`. O autor e a data são atribuídos pelo servidor, a partir da sessão e do relógio (Exercício 3): o pedido só aceita o texto, e um `authorId` ou um `createdAt` dá 400.

| Regra | Como se aplica |
|---|---|
| Quem vê e comenta (R24) | O gestor e os membros do projeto, em tarefas e subtarefas (403 para os outros). Lista do mais antigo para o mais recente |
| Páginas (A30, A31) | `listComments` vem em páginas, como a lista de tarefas: `page` e `size`, total em `X-Total-Count`; com a mesma data, o primeiro a ser criado vem primeiro |
| Texto (NF09, P28) | Até 2000 caracteres (400 acima) e não só espaços (A25); guardado e devolvido tal como chega |
| Editar e apagar (R24) | Só o autor, nem o gestor (403). A edição mantém o autor e a data; apagar elimina de vez, sem reciclagem |
| Estado do projeto e da tarefa (R27, R28, P37) | Em projetos em aberto e suspensos comenta-se, edita-se e apaga-se; num concluído só se lê (422 `PROJECT_COMPLETED`). Numa tarefa ou subtarefa fechada também só se lê (422 `TASK_CLOSED`) até o gestor a reabrir |
| Seguem a tarefa (R9) | Os comentários de uma tarefa apagada deixam de aparecer; voltam quando ela for recuperada |

Os pressupostos A24 a A27 estão confirmados no [openapi.yaml](openapi.yaml).

```bash
curl -s -b /tmp/ana.txt -H 'Content-Type: application/json' -d '{"content":"Falta rever o capítulo 2"}' localhost:8081/tasks/1/comments
```

```bash
curl -s -b /tmp/ana.txt localhost:8081/tasks/1/comments
```

### Lista e Matriz

Operação nova: `listProjectTasks` (a pesquisa de projetos, `listProjects` com `search`, já existia). Só lê: o módulo `views` consulta os dados das Tarefas sem os escrever (ver [docs/architecture.md](docs/architecture.md)).

| Regra | Como se aplica |
|---|---|
| Quem vê (R8) | Quem vê o projeto vê as tarefas, em qualquer estado do projeto, também concluído (403 para os outros, 404 se não existe) |
| Só tarefas principais (R18) | As subtarefas vêm dentro da sua tarefa principal e nunca sozinhas; tarefas e subtarefas apagadas não aparecem |
| Ordem (R23) | Abertas primeiro e depois as fechadas; em cada grupo por prazo, depois urgência e importância (mais altas primeiro); em empate, a ordem de criação (A29) |
| Filtros (R25, P29) | `mine`, `status` e `quadrant` combinam-se e aplicam-se às tarefas principais. "As minhas" inclui a tarefa principal se a pessoa for responsável por uma das suas subtarefas (P25) |
| Quadrante | Urgência e importância de nível 3 ou mais são altas: `Do first` (ambas), `Schedule` (só importante), `Delegate` (só urgente), `Later` |
| Matriz (P10) | A API devolve também as tarefas fechadas, com o estado; é a interface que as esconde na Matriz (V10) |
| Valor de filtro desconhecido (A28) | 400, depois da verificação de acesso |
| Páginas (A30, A31) | `page` (a partir de 1) e `size` (1 a 200, por omissão 50); o total de tarefas principais que passam os filtros vai em `X-Total-Count`; uma página depois da última dá 200 com lista vazia; as subtarefas não contam para a página. Um valor fora destes dá 400, depois do 403 |

Os pressupostos A28 e A29 estão confirmados no [openapi.yaml](openapi.yaml); A30 e A31 (páginas) estão confirmados.

```bash
curl -s -b /tmp/ana.txt 'localhost:8081/projects/1/tasks?mine=true&status=To%20do&quadrant=Do%20first'
```

### Painel

Operação nova: `getDashboard`. Só lê: o módulo `views` conta as tarefas e calcula o progresso sem escrever. O cálculo está em `ProjectProgress`, que não lê dados nem relógio, para se testar com instantes fixos.

| Regra | Como se aplica |
|---|---|
| Que projetos (P27, R8) | Os do utilizador que não estão concluídos nem apagados, por ordem de nome (A34) |
| Contagens (R18) | Tarefas principais por estado, sem as subtarefas nem as apagadas |
| Progresso (P7) | Concluídas ÷ (todas menos canceladas e duplicadas), a duas casas; só o gestor do projeto o recebe |
| Sem tarefas contáveis (P16) | `hasCountableTasks` falso, sem percentagem e sem estado calculado, mesmo depois do fim |
| Estado calculado (P5) | Só em projetos "In progress": `Attention` com desvio de 15 a menos de 25 pontos; `Late` com 25 ou mais, ou com o fim atingido. "Not started" e "Suspended" nunca o têm |
| Tempo (P26, P23) | No fuso do projeto; o início sem hora conta das 00:00 e o fim sem hora conta o dia inteiro; antes do início passou 0% |
| Arredondamento (A32) | O progresso e o tempo passado arredondam-se a duas casas antes de se calcular o desvio |

Os pressupostos A32 a A34 estão confirmados no [openapi.yaml](openapi.yaml).

```bash
curl -s -b /tmp/marta.txt localhost:8081/dashboard
```

### Reciclagem

Operações: `deleteProject`, `deleteTask`, `listRecycleBin`, `restoreProject` e `restoreTask`. A reciclagem não tem tabela própria: `deleted_at` em projetos e tarefas (M4). Tudo o que se apaga numa ação leva o mesmo instante, e é isso que liga um item ao que foi apagado com ele.

| Regra | Como se aplica |
|---|---|
| Quem apaga (R9) | O gestor apaga o projeto (não um concluído, R28). Os membros e o gestor apagam tarefas e subtarefas (A35), em projetos em aberto e suspensos (R27) |
| Tarefa-mãe (R9) | Com subtarefas em aberto não se apaga (422 `PARENT_HAS_OPEN_SUBTASKS`); as fechadas vão com ela, e os comentários seguem a tarefa |
| O que volta (R9) | Só o que tem o mesmo instante: ao recuperar um projeto voltam as tarefas apagadas com ele e as apagadas antes ficam na reciclagem; o mesmo para as subtarefas |
| O que cada um vê | O gestor vê os seus projetos apagados; os membros e o gestor vêem as tarefas e subtarefas dos seus projetos, e só o gestor recupera projetos (403 aos outros) |
| Não se recupera já (A38) | Se o projeto ou a tarefa-mãe está na reciclagem (`CANNOT_RESTORE`), ou o projeto foi concluído (`PROJECT_COMPLETED`); a lista mostra `restorable: false` e o motivo |
| Subtarefa (A43) | Em curso sob uma mãe fechada: 422 `PARENT_TASK_CLOSED`; com prazo depois do da mãe: 422 `SUBTASK_DEADLINE_AFTER_PARENT` |
| Responsável que saiu (P36, A42) | 422 `ASSIGNEE_REQUIRED` com o nome de quem saiu; volta-se a recuperar com `{"assigneeIds": [...]}`, que substitui só quem saiu; se ninguém saiu, o corpo é recusado com 400 |
| 30 dias (R20, A39) | Com 30 dias exatos já não existe; a lista e a recuperação ignoram o que expirou, mesmo que a eliminação ainda não tenha corrido; `daysLeft` arredonda para cima |
| Eliminação definitiva (M14) | `RecyclePurgeJob`, todos os dias às 03:00 (`matriz.recycle.purge.cron`), apaga também comentários, responsáveis e membros; um duplicado cujo original sai fica `Duplicate` sem original e com `originalEliminated: true`, para a interface mostrar uma nota, e `originalInRecycleBin: true` enquanto o original só está na reciclagem (A41); continua a poder editar-se |

Os pressupostos A35 a A43 estão no [openapi.yaml](openapi.yaml): todos confirmados.

```bash
curl -s -b /tmp/ana.txt -X DELETE localhost:8081/tasks/1
```

```bash
curl -s -b /tmp/ana.txt localhost:8081/recycle-bin
```

### Contrato, concorrência e cenários de aceitação

As 28 operações do contrato (versão 1.1.0 do [openapi.yaml](openapi.yaml)) estão implementadas e o cenário de aceitação do MVP passa. O que garante que o contrato, a concorrência e a documentação não se desviam do código:

| O quê | Como se garante |
|---|---|
| Escrever num projeto que é concluído ou apagado ao mesmo tempo (R22, R27, R28, R9) | Toda a escrita bloqueia a linha do projeto em modo partilhado e lê o estado com o bloqueio; concluir, alterar ou apagar o projeto bloqueia-a em modo exclusivo (`Locks`). Ordem: projeto, pessoas, tarefas. Um teste de concorrência por caminho de escrita |
| O contrato e a aplicação dizem o mesmo | `ContractConformanceIntegrationTest`: as rotas são as do contrato; as 28 operações, chamadas a sério, respondem com um estado e um corpo que o contrato descreve, sem campos a mais, e cada recusa traz código e mensagem (NF08) |
| O cenário MVP-1 | `MvpAcceptanceIntegrationTest`: da criação do projeto ao painel com 50% de progresso |
| Nenhum cenário esquecido | `ScenarioCoverageTest`: cada cenário e caso excecional da v1 é nomeado por um teste; só o EC39 (uma confirmação) fica para a interface |
| Documentação coerente | `DocumentationCodesTest`: os códigos P, T, R, EC, M, V e A são únicos, sem falhas, e nenhuma referência aponta para um código que não existe |

```bash
cd matriz && set -a && . ./.env && set +a && mvn -B verify
```

As verificações que a API não consegue fazer sozinha (V1 a V16 do [plano](docs/implementation-plan.md#8-verificações-adiadas-para-a-fase-da-interface)) foram feitas com a interface e estão registadas, com a evidência, em [docs/verification.md](docs/verification.md).

## Documentação adicional

| Ficheiro | Conteúdo |
|---|---|
| [CHANGELOG.md](CHANGELOG.md) | Histórico de alterações por versão |
| [docs/release-notes-1.0.0.md](docs/release-notes-1.0.0.md) | Notas de lançamento da versão 1.0.0 |
| [docs/requirements.md](docs/requirements.md) | Visão e objetivos, utilizadores, requisitos funcionais e não funcionais, regras de negócio, âmbito do MVP e o que fica fora |
| [docs/user-stories.md](docs/user-stories.md) | User stories do MVP, critérios de aceitação (Given / When / Then) e casos excecionais |
| [docs/constraints.md](docs/constraints.md) | Restrições, pressupostos, segurança e privacidade, decisões técnicas e limitações de âmbito |
| [docs/architecture.md](docs/architecture.md) | Monólito modular, componentes, fronteiras, diagramas (módulos e execução) e stack |
| [docs/database.md](docs/database.md) | Modelo de dados: entidades, relacionamentos e diagrama ER (Mermaid) |
| [docs/api.md](docs/api.md) e [openapi.yaml](openapi.yaml) | Índice da API e contrato OpenAPI 3.1 |
| [docs/ui/matrix-wireframe.md](docs/ui/matrix-wireframe.md) e [docs/ui/wireframes.md](docs/ui/wireframes.md) | Wireframes dos ecrãs |
| [docs/implementation-plan.md](docs/implementation-plan.md) | Plano de implementação por incrementos, com o estado de cada um |
| [docs/plan.md](docs/plan.md) | Análise de lacunas, contradições e decisões pendentes |
| [docs/test-plan.md](docs/test-plan.md) | Plano de testes de tarefas e permissões e testes unitários da regra do título |
| [docs/verification.md](docs/verification.md) | Verificação do MVP: cenários, evidências, defeitos encontrados |
| [docs/security-review.md](docs/security-review.md) | Revisão de código e de segurança: achados, controlo de acesso, o que ficou por validar |
| [AGENTS.md](AGENTS.md) | Instruções persistentes para agentes de desenvolvimento neste repositório |
| [../frontend/README.md](../frontend/README.md) | Guia da interface (estrutura, comandos, teste ponta a ponta) |

**Convenções dos documentos.** Os códigos identificam os itens: **F** (funcionalidade), **R** (regra de negócio), **NF** (requisito não funcional), **C** (restrição), **P** (pressuposto), **S** (segurança), **T** (decisão técnica), **US** (user story), **EC** (caso excecional) e **A** (pressuposto do contrato). Cada item está marcado como decisão ou pressuposto; as decisões foram tomadas pela autora em nome de um cliente fictício (um trabalho de formação, C1). Os documentos estão em português; a interface da aplicação é em inglês.

## Estado e limitações conhecidas

**Versão 1.0.0 (a v1, o MVP): concluída.** Os 14 incrementos do [plano de implementação](docs/implementation-plan.md) estão concluídos (0 a 8 do backend, 9 a 13 da interface), tal como a verificação do MVP, a execução com Docker Compose e o CI.

Limitações conhecidas, em aberto:

- **Contas:** só existem as de demonstração; a criação de contas pelo gestor é da v2.
- **Sessões em memória:** reiniciar a API termina-as, e só pode correr uma instância da API.
- **Certificado:** o do Docker Compose é autoassinado (o navegador avisa); não há um certificado público nem um domínio.
- **Decisões por tomar** ([docs/constraints.md](docs/constraints.md#4-decisões-técnicas-pendentes)): onde se publica a aplicação (T8), os navegadores e a acessibilidade suportados (T10, o teste ponta a ponta só corre em Chromium) e as cópias de segurança e recuperação (T11).
- **Dados reais:** a aplicação não foi pensada para dados reais (C8).

Stack escolhida em 2026-11-12 (backend) e 2026-11-14 (interface), e execução com Docker Compose em 2026-11-18: ver [docs/architecture.md](docs/architecture.md#8-stack-e-decisões-técnicas).
