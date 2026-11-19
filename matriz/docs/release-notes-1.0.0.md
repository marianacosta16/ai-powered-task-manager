# Matriz 1.0.0 — Notas de lançamento

> Versão 1.0.0 (a v1, o MVP). Preparada em 2026-11-19; **não foi publicada** nenhuma *release* nem criada uma *tag* (não era necessário). A versão está no `pom.xml` e no `frontend/package.json`. Histórico completo: [CHANGELOG.md](../CHANGELOG.md).

## Porquê 1.0.0 (Semantic Versioning)

- É a **primeira versão** e o âmbito do MVP (v1: F01 a F10, F17 e F22) está completo, por isso não é uma `0.x`: a autora decidiu que a v1 é a versão final e pronta.
- A API tem um contrato estável (OpenAPI 1.1.0, 28 operações). Daqui em diante: uma correção sem mudar o contrato sobe o **patch** (1.0.1); uma funcionalidade nova compatível, como as da v2, sobe o **minor** (1.1.0); uma mudança que quebre o contrato ou os dados sobe o **major** (2.0.0).
- A versão da aplicação é independente da do contrato (`info.version` do [openapi.yaml](../openapi.yaml)), que só muda quando o contrato muda.

## O que traz

- **Gestão de projetos e equipas:** projetos com fase e estado, equipa por projeto, um projeto em aberto por membro, remoção de membros com substituto.
- **Tarefas e subtarefas:** prazo obrigatório, vários responsáveis, urgência e importância (4 níveis), estados, regras de prazo e de fecho, deteção de edições simultâneas.
- **Vistas:** Matriz (*Do first*, *Schedule*, *Delegate*, *Later*), Lista com filtros, Painel com progresso e estados *Attention* e *Late*, pesquisa de projetos.
- **Comentários** nas tarefas e **reciclagem** de projetos e tarefas (30 dias).
- **Acesso:** sessão com cookie, permissões por papel e por projeto (também por ligação direta).
- **Interface web** em inglês, para computador.
- **Execução:** `docker compose up --build --wait` arranca base de dados, API e interface com HTTPS.

## Como experimentar

```bash
git clone https://github.com/marianacosta16/ai-powered-task-manager.git
cd ai-powered-task-manager
cp .env.example .env     # preencher DB_PASSWORD e DEMO_PASSWORD
docker compose up --build --wait
```

Abrir `https://localhost:8443` (o navegador avisa do certificado autoassinado) e entrar com `marta` (gestora) ou `ana` (membro), com a palavra-passe de `DEMO_PASSWORD`. Mais detalhes, o desenvolvimento sem Docker e as variáveis no [README](../README.md).

## Compatibilidade e atualização

- Primeira versão: não há versão anterior de que atualizar.
- Base de dados: PostgreSQL 16; o esquema é criado pelo Flyway (3 migrações: esquema, versão das tarefas e título com 150 caracteres).
- Contrato da API: [openapi.yaml](../openapi.yaml) 1.1.0, 28 operações.

## Verificação desta versão

Resultados de 2026-11-19, executados nesta máquina:

| Verificação | Resultado |
|---|---|
| Backend (`mvn -B verify`) | 352 testes, 0 falhas |
| Frontend (`npm test`) | 272 testes, 0 falhas |
| Lint e build do frontend | Sem erros |
| Teste ponta a ponta (Playwright, MVP-1) | 1 teste, passou |
| CI no GitHub (`test`, `frontend`, `e2e`, `docker`) | Passa na `main` (PR 29) |
| Execução com Docker Compose | Os três serviços saudáveis; criar uma tarefa e encontrá-la depois de recriar os contentores, verificado em 2026-11-18 |

Os cenários do MVP, as evidências e os defeitos encontrados estão em [verification.md](verification.md).

## Limitações conhecidas

- Só existem as contas de demonstração (10); a criação de contas é da v2.
- As sessões estão em memória: reiniciar a API termina-as, e só pode correr uma instância da API.
- O certificado do Docker Compose é autoassinado; não há domínio nem certificado público.
- Por decidir: onde se publica a aplicação (T8), os navegadores e a acessibilidade suportados (T10) e as cópias de segurança (T11). O teste ponta a ponta só corre em Chromium.
- Não foi pensada para dados reais (C8).
- O backend não tem ferramenta de análise estática nem verificação de dependências; a revisão de segurança foi feita por revisões de código e por testes de controlo de acesso ([security-review.md](security-review.md)). O `npm audit` indica 6 avisos de um só pacote de desenvolvimento, sem versão corrigida.
- A interface não foi verificada manualmente em navegadores que não o Chromium, nem em telemóvel real (só em larguras emuladas).

## Informação não confirmada nesta preparação

- O número de testes e os resultados de CI são os de 2026-11-19; mudam com cada alteração.
- Os exemplos `curl` do README foram escritos em incrementos anteriores e não foram todos executados de novo nesta passagem; o contrato e o código estão cobertos pelos testes de conformidade.
- Não existe página de *release* no GitHub nem *tag*: a versão 1.0.0 está só nos ficheiros do repositório.
