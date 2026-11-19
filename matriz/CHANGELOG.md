# Changelog

Todas as alterações importantes da Matriz ficam registadas aqui. O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e a numeração segue o [Semantic Versioning](https://semver.org/lang/pt-BR/).

A versão da aplicação (`pom.xml` e `frontend/package.json`) é independente da versão do contrato da API (`info.version` do [openapi.yaml](openapi.yaml), hoje 1.1.0), que só muda quando o contrato muda. Os números dos pull requests são os de [marianacosta16/ai-powered-task-manager](https://github.com/marianacosta16/ai-powered-task-manager/pulls).

## [Não lançado]

Nada por agora.

## [1.0.0] - 2026-11-19

A primeira versão (a v1, o MVP): backend, interface web, execução com Docker Compose e integração contínua. Não foi publicada nenhuma *release* nem criada uma *tag*: a versão está definida nos ficheiros de build e nestes documentos. Notas de lançamento: [docs/release-notes-1.0.0.md](docs/release-notes-1.0.0.md).

### Adicionado

**Backend (API REST)**
- Contas de demonstração (10 utilizadores e 4 projetos de exemplo, criados ao arrancar) e sessão no servidor com cookie `SESSION` (3 horas sem atividade); permissões por papel e por projeto, com `403` também por ligação direta (incrementos 0 e 1; PR 1).
- Projetos e equipa: criar, alterar, apagar e listar projetos; adicionar e remover membros, com a regra de um projeto em aberto por membro e a passagem das tarefas a um substituto (incrementos 2 e 3b; PR 3 e 5).
- Tarefas e subtarefas com prazo, urgência e importância em 4 níveis, vários responsáveis, estados, regras de prazo e de fecho, e deteção de edições simultâneas (`409 TASK_CHANGED`) (incremento 3; PR 4).
- Vistas de leitura: Matriz, Lista com filtros, pesquisa de projetos, e Painel com contagens, progresso e estados *Attention* e *Late* (incrementos 4 e 5; PR 7 e 8).
- Comentários nas tarefas (autor e data; só o autor edita ou apaga) (incremento 6; PR 6).
- Reciclagem de projetos e tarefas, com eliminação definitiva aos 30 dias (incremento 7; PR 9).
- Contrato OpenAPI 3.1 com as 28 operações (versão 1.1.0) e testes que o comparam com a aplicação, de concorrência nos caminhos de escrita, de fronteiras entre módulos (ArchUnit) e de cobertura dos cenários de aceitação (incremento 8; PR 10 e 11).

**Interface web (React, TypeScript e Vite)**
- Base e início de sessão; painel e projetos; detalhe do projeto com Matriz, Lista e criação de tarefas; detalhe da tarefa com responsáveis, subtarefas e comentários; equipa e reciclagem; fecho com revisão do layout nas larguras 375, 800 e 1280 px (incrementos 9 a 13b; PR 13 a 18).
- Teste ponta a ponta do cenário MVP-1 com o Playwright (PR 18).

**Verificação e documentação**
- Verificação do MVP com os 9 cenários, as evidências e os defeitos encontrados, e três testes automáticos novos: validação, autorização por identificadores e persistência depois de reiniciar (PR 19; [docs/verification.md](docs/verification.md)).
- Plano de testes, testes unitários da regra do título, e revisão de código e de segurança ([docs/test-plan.md](docs/test-plan.md) e [docs/security-review.md](docs/security-review.md); PR 20 a 22).
- Documentação da API ([docs/api.md](docs/api.md)), diagramas Mermaid de arquitetura e de execução, README atualizado e este registo.

**Entrega**
- Imagens Docker da API e da interface (nginx com HTTPS e proxy de `/api`), sem credenciais dentro das imagens (PR 23).
- `compose.yaml` com `db`, `api` e `web`, rede privada, volumes persistentes, healthchecks e `.env.example`; só o `web` publica portas (PR 24 e 28).
- CI no GitHub Actions: testes do backend, lint, testes e build do frontend, teste ponta a ponta e teste de fumo do Docker Compose; as imagens só se constroem depois de `test` e `frontend` passarem (PR 2, 25, 27 e 29).

### Alterado
- O título de uma tarefa passou de 100 para 150 caracteres, e os limites de texto ficaram num só sítio (`Limits`), comparado com o contrato e a base de dados por um teste (PR 11).
- Os testes que esvaziam a base de dados só correm numa base cujo nome acaba em `_test` (PR 1).
- O gestor de um projeto novo é quem o cria (vem da sessão, não do pedido); a passagem da gestão faz-se ao alterar o projeto (PR 11).

### Corrigido
- Defeitos encontrados na verificação do MVP (D1 a D3): depois de uma pessoa terminar sessão, quem entrava a seguir era levado à página onde a anterior estava, o botão de adicionar responsável aparecia esticado, e o erro `403` oferecia "Try again" (PR 19).
- Correções dos achados das revisões de código a cada pull request (cerca de 250 achados distintos, quase todos corrigidos), incluindo a ordem `403` antes de `400`, bloqueios de linhas na ordem certa e o tratamento de texto invisível (PR 1 a 29; ver [docs/security-review.md](docs/security-review.md)).
- Entrega: cabeçalhos de segurança do nginx em todas as páginas, o nginx a seguir a API recriada, renovação do certificado autoassinado, repetição das descargas do Maven e o healthcheck do `web` (PR 26 e 28).

### Segurança
- Palavras-passe só como hash BCrypt, nunca em ficheiros versionados; as credenciais do CI e dos testes são valores falsos.
- Sem casos de acesso indevido por identificador alterado nas 25 operações testadas ([docs/security-review.md](docs/security-review.md)).

### Limitações conhecidas
Ver as [notas de lançamento](docs/release-notes-1.0.0.md#limitações-conhecidas).

[Não lançado]: https://github.com/marianacosta16/ai-powered-task-manager/compare/main...HEAD
[1.0.0]: https://github.com/marianacosta16/ai-powered-task-manager/commits/main
