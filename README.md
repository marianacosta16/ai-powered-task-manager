# Matriz

Aplicação web de gestão de projetos e tarefas para equipas, com uma **matriz urgente/importante** como vista principal para decidir o que fazer primeiro. Versão **1.0.0** (a v1, o MVP). Projeto de aprendizagem.

**Objetivo:** organizar projetos e equipas, gerir tarefas (com subtarefas) por urgência e importância, e permitir ao gestor acompanhar o progresso e os prazos.

**Stack:** backend em Java 21 e Spring Boot com PostgreSQL; interface em React, TypeScript e Vite; Docker Compose para executar; GitHub Actions para o CI.

## Arrancar

Precisa do Docker com Docker Compose v2.

```bash
cp .env.example .env     # preencher DB_PASSWORD e DEMO_PASSWORD
docker compose up --build --wait
```

Abra `https://localhost:8443` (o navegador avisa do certificado autoassinado) e entre com `marta` (gestora) ou `ana` (membro), com a palavra-passe que pôs em `DEMO_PASSWORD`.

## Onde está cada coisa

| Pasta ou ficheiro | Conteúdo |
|---|---|
| [matriz/](matriz/) | O backend (API REST) e **toda a documentação**: o [README principal](matriz/README.md) (funcionalidades, instalação, execução, testes, API), os documentos em [matriz/docs/](matriz/docs/) (requisitos, arquitetura, dados, plano), o contrato [openapi.yaml](matriz/openapi.yaml), o [CHANGELOG](matriz/CHANGELOG.md) e as [notas de lançamento](matriz/docs/release-notes-1.0.0.md) |
| [frontend/](frontend/) | A interface web e os testes ponta a ponta ([README](frontend/README.md)) |
| [compose.yaml](compose.yaml) | Base de dados, API e interface com Docker Compose |
| [.github/](.github/) | O CI (GitHub Actions) e o teste de fumo do Docker Compose |

Para tudo o resto (desenvolvimento sem Docker, variáveis de configuração, testes, regras por funcionalidade), comece pelo [README em matriz/](matriz/README.md).
