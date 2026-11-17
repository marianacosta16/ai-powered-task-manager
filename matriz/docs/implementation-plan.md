# Matriz — Plano de implementação

> Sequência de incrementos para construir a v1 (MVP) da Matriz: 0 a 8 são o backend (concluídos) e 9 a 13 a interface, com componentes e ficheiros previstos, operações da API, critérios de aceitação e formas de verificação. Atualiza-se ao longo da implementação: o estado de cada incremento está na secção 2.
> Criado em 2026-11-12. Documentos de base: [requirements.md](requirements.md), [user-stories.md](user-stories.md), [constraints.md](constraints.md), [architecture.md](architecture.md), [database.md](database.md), [openapi.yaml](../openapi.yaml), [ui/wireframes.md](ui/wireframes.md) e a análise em [plan.md](plan.md).

## 1. Ponto de partida

| Tema | Estado |
|---|---|
| Âmbito | v1: F01 a F10, F17 e F22 ([requirements.md](requirements.md#6-âmbito-do-mvp-v1)) |
| Arquitetura | Monólito modular, com os módulos de [architecture.md](architecture.md#4-componentes-e-responsabilidades) |
| Stack | **Java 21, Spring Boot e PostgreSQL** no backend, sessão no servidor com cookie; **React com TypeScript e Vite** na interface (decidido em 2026-11-14). Registada em [architecture.md](architecture.md#8-stack-e-decisões-técnicas) |
| Contrato | [openapi.yaml](../openapi.yaml): 28 operações, uma por cada ação da v1 |
| Permissões | Tabela em [requirements.md](requirements.md#21-permissões-por-papel-e-por-projeto) |
| Onde se desenvolve | Numa **VM Ubuntu**, que a autora indicou ter a stack instalada (por confirmar: versões). O PostgreSQL é o da VM; não se usa PostgreSQL embutido |
| Ordem | **API primeiro**: os incrementos 0 a 8 entregaram as 28 operações da API, testadas contra o contrato. A interface vem depois, em 5 incrementos (9 a 13), um por grupo de ecrãs dos wireframes já feitos, e cada um começa por uma análise e um plano que a autora valida (Exercício 7) |

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
| 2 | Projetos e equipa | Concluído | 2026-11-13 | Cenários 1.1 a 1.23, 1.29 a 1.32 e 8.1 a 8.3 (os de tarefas, 1.12 a 1.14 e 1.24 a 1.28, ficam para o incremento 3); EC8, EC12 a EC14, EC17, EC25, EC27, EC37, EC39 (é da interface), EC40, EC42, EC44 e EC45, em testes HTTP com relógio controlável (38 testes novos; 70 no total, verdes). `removeMember` só remove membros sem tarefas em aberto (A11). Pressupostos A8 a A13 do contrato confirmados em 2026-11-13. Não verificado: a confirmação antes de concluir um projeto (1.22) é da interface. Alterado em 2026-11-14: o gestor de um projeto novo passou a ser o criador, sem `managerId` no pedido (A44), e o nome do projeto passou a ter de 3 a 100 caracteres e a descrição até 1000 (A45) |
| 3 | Tarefas, subtarefas e responsáveis | Concluído | 3a: 2026-11-13; 3b: 2026-11-14 | 3a (tarefas, subtarefas, responsáveis e versão, T13): cenários 2.1 a 2.13 (menos apagar, do incremento 7), 4.1 a 4.7 e 5.1 a 5.4; EC2 a EC5, EC10, EC19, EC22, EC28 a EC30, EC35, EC43, EC46 e EC47, em testes HTTP com relógio controlável e 5 testes de concorrência, que esperam pelo bloqueio em vez de dormir (38 testes novos; 108 no total, verdes). Verificado por mutação: sem o bloqueio de linha, sem a verificação da versão, sem a R4, sem a guarda da reabertura de subtarefas, sem o bloqueio da pessoa e sem a regra "não subir a versão" os testes falham. Pressupostos A14 a A20 do contrato, todos confirmados em 2026-11-13. 3b (remoção de membros com substitutos, R17 e P24): cenários 1.12 a 1.14 e 1.24 a 1.28; EC15, EC18, EC41 e a concorrência com a criação de tarefas, em 23 testes novos (131 no total, verdes); verificado por mutação (limite de 5, cobertura de todas as tarefas, R11 do substituto novo e a isenção dos gestores, P24, validação antes de bloquear, bloqueio das pessoas, guarda da ordem dos bloqueios e a regra ArchUnit). Pressupostos A21 a A23 confirmados em 2026-11-14. Todos os bloqueios seguem uma só ordem: pessoas por id, depois tarefas por id |
| 4 | Lista, Matriz, pesquisa e filtros | Concluído | 2026-11-14 | Cenários 3.1 a 3.6, 9.1 a 9.5 e 12.1 a 12.6; EC20 e EC33, em 24 testes HTTP novos (178 no total, verdes): quadrante de cada tarefa, tarefas fechadas com o estado final (a Matriz esconde-as na interface, P10), "as minhas" com a subtarefa a trazer a tarefa principal (P25), subtarefas só dentro da principal (R18), ordem (R23), filtros combinados (R25, P29), 400 para um valor de filtro desconhecido, 403 antes de 400, 404, 401, tarefas e subtarefas apagadas fora da lista e projeto concluído ainda legível. Verificado por mutação (9 mutantes mortos): sem a subtarefa em "as minhas", sem abertas primeiro, sem só principais, sem excluir apagadas (tarefas e subtarefas), sem o desempate por id, com a urgência na ordem errada, com o limiar do quadrante errado e sem o 403 antes do 400. Pressupostos A28 e A29 do contrato, confirmados em 2026-11-14. A revisão do PR 7 levou a: validar `mine` depois do 403, ler a lista num só instantâneo (REPEATABLE_READ) e paginar a lista de tarefas e a de comentários (A30 e A31, confirmados em 2026-11-14; 28 testes da lista, 184 no total, verdes, e 7 mutantes da paginação mortos: deslocamento, tamanho máximo, página 0, total sem filtros, deslocamento e total dos comentários e 403 antes de 400) Não verificado: a Matriz e os controlos são da interface (V10) |
| 5 | Painel e progresso | Concluído | 2026-11-14 | Cenários 6.1 a 6.15 e 13.1 a 13.5; EC1, EC11, EC16 e EC24, em 27 testes novos (213 no total, verdes): 13 do cálculo com instantes fixos nos limites de 15 e 25 pontos, do fim com e sem hora, do fuso horário e do arredondamento; 14 HTTP com relógio controlável (contagens por estado, subtarefas fora, só os projetos do utilizador, só o gestor vê a barra e o estado calculado, suspenso e não iniciado sem estado calculado, sem tarefas contáveis, concluídos, apagados e ordem). Verificado por mutação (12 mutantes mortos): limites de 15 e 25, fim exato, contagem de canceladas, dia de fim sem hora, estado só em andamento, arredondamento, subtarefas, apagadas, barra para todos, concluídos e ordem. Pressupostos A32 a A34 do contrato, confirmados em 2026-11-14. Não verificado: a etiqueta "sem tarefas", a barra e a navegação para a Matriz são da interface (V11) |
| 6 | Comentários | Concluído | 2026-11-14 | Cenários 11.1 a 11.6 e 11.8 a 11.11, a primeira metade do 11.7 (os comentários de uma tarefa apagada não aparecem), EC32, EC34 e EC48, em testes HTTP com relógio controlável e um de concorrência (23 testes novos; 154 no total, verdes). Verificado por mutação: sem a regra do autor, sem a da tarefa fechada (a adicionar e a editar ou apagar), sem o bloqueio da tarefa nas escritas de comentários, sem a contagem de caracteres e sem a leitura do corpo depois do acesso, sem a do projeto concluído, sem a ordenação por data e sem a verificação das linhas atualizadas os testes falham. O autor e a data vêm sempre do servidor. Pressupostos A24 a A27 do contrato, confirmados em 2026-11-14. Os limites de 100 e 2000 passaram a contar caracteres (pontos de código) em todos os textos, também nos de tarefas e projetos (R14): um emoji conta uma vez. A segunda metade do 11.7 (voltam quando a tarefa é recuperada) é do incremento 7 |
| 7 | Reciclagem e eliminações | Concluído | 2026-11-14 | Cenários 7.1 a 7.10 e a segunda metade do 11.7; EC7, EC21, EC23, EC36 e EC49 a EC51, em 35 testes HTTP novos com relógio controlável (inclui 5 de concorrência, que esperam pelo bloqueio): apagar e recuperar projetos e tarefas, só o que foi apagado junto volta (R9), 29 e 31 dias e exatamente 30, dias restantes, o que cada papel vê e recupera, P36 (novo responsável), subtarefa em curso sob mãe fechada, prazo da subtarefa, projeto concluído, R11 com projeto apagado e a eliminação definitiva com comentários, responsáveis, membros e o original de duplicados. Verificado por mutação (25 mutantes mortos): sem a regra das subtarefas em aberto, sem a cascata, sem as tarefas do projeto, a recuperar tudo, o limite dos 30 dias (listar e recuperar), tarefas do projeto listadas à parte, utilizadores sem acesso, membro a recuperar projetos, projeto concluído, ligação ao original, comentários, substituição de responsáveis, mãe fechada, prazo, bloqueio da tarefa, dias restantes e as redes de segurança da eliminação. Pressupostos A35 a A41 e A43 do contrato confirmados em 2026-11-14 (a A41 com a nota de que o original foi eliminado, `originalEliminated`, e outra de que está na reciclagem, `originalInRecycleBin`); A42 confirmada em 2026-11-14 (o corpo substitui só quem saiu). Não verificado: o ecrã da reciclagem, a tarefa agendada às 03:00 com o relógio real (só se chama a eliminação nos testes) e a ordem dos bloqueios da eliminação definitiva contra pedidos ao mesmo tempo (V12) |
| 8 | Fecho do MVP | Concluído | 2026-11-14 | 28 testes novos (278 no total, verdes). **R28 e a eliminação:** todos os caminhos de escrita num projeto (criar e alterar tarefas, responsáveis, comentários, membros, apagar e recuperar tarefas, alterar e apagar o projeto) bloqueiam a linha do projeto e leem o estado com o bloqueio; 10 testes de concorrência, que cobrem cada caminho de escrita, em que o projeto é concluído ou apagado enquanto o pedido espera, mais 2 (a conclusão que espera pelas escritas em curso e várias escritas ao mesmo tempo no mesmo projeto), e a ordem projeto, pessoas, tarefas imposta por `Locks` e pelo ArchUnit. **Contrato igual à implementação:** um teste percorre as 28 operações com pedidos reais e confirma o estado HTTP e o corpo contra o esquema do `openapi.yaml` (campos obrigatórios, tipos, valores dos enumerados e campos que o contrato não conhece), e que as rotas da aplicação são as do contrato; a recusa traz sempre um código e uma mensagem (NF08). **Cenário MVP-1** de ponta a ponta. **Cenários e códigos:** um teste confirma que todos os cenários e casos excecionais da v1 são nomeados por um teste (só o EC39 fica para a interface, V1) e outro que os códigos P, T, R, EC, M, V e A são únicos, sem falhas e sem referências a códigos que não existem. Verificado por mutação (18 mutantes mortos): os 7 caminhos de bloqueio, o modo partilhado, a guarda da ordem, uma rota, um estado HTTP, um campo e um enumerado do contrato, e os testes de códigos (3) e de cobertura (2). O contrato passa à versão 1.0.0, com as 28 operações. Todas as verificações da v1 que dependem da interface ficam nos itens V1 a V12 |
| 9 | Interface: base e início de sessão | Concluído | 2026-11-15 | **Backend:** CORS com credenciais (`CorsConfig`, `matriz.cors.allowed-origins`), com 9 testes (origem permitida e recusada, pedidos que alteram dados de outra origem recusados e não executados, pedido de verificação prévia, cabeçalhos, `X-Total-Count`, erros 401 e 403 legíveis, início de sessão com o cookie, e um `*` ou uma `/` na lista que impedem o arranque); 6 mutantes mortos (CORS sem credenciais, sem `X-Total-Count`, todas as origens, sem a verificação do `*`, sem `PATCH`) e um equivalente (o `.cors()` explícito, que o Spring Security já aplica quando há o bean). `mvn verify`: 302 testes, verdes. **Interface** (`frontend/`, React, TypeScript e Vite): endereço do backend num só módulo, cliente da API com os erros do contrato, sessão (`AuthProvider`), componentes comuns (botão, campo, aviso, carregamento), barra superior, rota protegida, início de sessão e painel provisório, com estilos das três larguras. 46 testes de componentes (Vitest e Testing Library), com o `fetch` como única simulação; 21 mutantes mortos (credenciais, submissão repetida de duas formas, palavra-passe preservada, rota protegida, tratamento do 401, um 401 atrasado de uma sessão antiga, nome aparado, regresso ao ecrã pedido, erro de rede, corpo que não é JSON o *build* sem endereço, de qualquer modo, e desfazer a mudança de sessão quando sair falha). `npm audit`: 0 vulnerabilidades. **Verificado num navegador a sério, com a API real e a base de teste** (cenários 10.1, 10.2, 10.4, 10.5 e 10.6): mensagens de campo vazio, credenciais erradas (mensagem da API e campos preservados), entrar e voltar ao ecrã pedido, o cookie de sessão que sobrevive a recarregar a página, sair, ligação direta sem sessão e a mensagem "Your session has ended" depois de reiniciar o backend; barra superior com o menu em ecrã estreito. Fecha o V4. Não verificado: a expiração real às 3 horas (V5), o Playwright (adiado por decisão da autora) e o CI do passo da interface, que só corre no GitHub |
| 10 | Interface: painel e projetos | Concluído | 2026-11-16 | **Painel** (cartões com os 7 contadores, barra e estado calculado só ao gestor, "No tasks", vazio e erro com *Try again*), **Projetos** (lista que passa a cartões no telemóvel, pesquisa por nome, *+ New project* só ao gestor, *Edit* e *Delete* só nos projetos abertos que gere) e **formulário** de criar e editar (campos de data e hora nativos com `min`, avisos antes do pedido, só o que mudou no `PATCH`, o campo *Manager* só ao editar, com os gestores vindos de `listMembers` e `listCandidateMembers`, e confirmações ao concluir e ao eliminar). Detalhe do projeto **provisório** em `/projects/:id` (dados do `getProject`; as tarefas ficam para o incremento 11). Wireframe de Projetos redesenhado e `wireframes.md` atualizado (gestor só ao editar; nome de 3 a 100 e descrição até 1000; campos de data nativos). Sem alterações no backend nem no contrato. **Testes:** 90 de componentes (Vitest), verdes, com `mockApi` (respostas por pedido, um pedido não previsto falha o teste); `tsc`, `oxlint` e `build` limpos. **Verificado por mutação** (cerca de 25 mutantes, todos mortos menos 2 equivalentes: o `Escape` com o pedido em curso, que o foco já impede, e a percentagem nula com tarefas contáveis, que a API não devolve). A mutação encontrou um erro real, corrigido: a resposta de sucesso de uma pesquisa antiga podia substituir a da pesquisa atual (`useLoad`). **Verificado em 2026-11-16 num navegador**, com a API real e a base de teste: iniciar sessão como gestor, criar um projeto, vê-lo na lista, recarregar, editar, concluir (com confirmação; deixa de ter *Edit* e *Delete*), eliminar (com confirmação), largura de telemóvel e, depois de reiniciar o backend, o início de sessão com "Your session has ended..." e os dados mantidos. Decisões da autora: o botão *Recycle bin* do painel só aparece no incremento 13; o gestor só se escolhe ao editar. Depois da revisão: as horas `HH:mm` foram enviadas à API real (criar e alterar, incluindo limpar com `null`) e o formulário, a caixa de confirmação (foco e `Escape`), o menu e o `useLoad` foram corrigidos, com mutantes mortos. Não verificado: criar como membro (só em testes de componentes), a Matriz ao selecionar um projeto (incremento 11) e o painel de um membro num navegador |
| 11 | Interface: detalhe do projeto e tarefas | Concluído | 2026-11-16 | **Detalhe do projeto** em `/projects/:id` com os dados, os separadores *Matrix* e *List*, os filtros (*My tasks*, estado e quadrante, que se combinam) e a paginação com o total do servidor (`X-Total-Count`); o separador e os filtros vivem no endereço (recarregar e voltar atrás mantêm a vista; um valor inválido é ignorado). **Matriz** com os quatro quadrantes, sem as tarefas em estado final, a pedir 200 e a avisar quando há mais (decisão da autora). **Lista** com título, subtarefas, estado, prazo, urgência, importância e responsáveis (cartões no telemóvel). **Nova tarefa** num painel (título 3 a 150, descrição até 2000, prazo com `min`, urgência e importância sem valor por omissão, responsáveis numa lista de caixas), só se o projeto não estiver suspenso nem concluído. *Edit project* e *Delete project* no detalhe, só ao gestor de um projeto não concluído; *Team* fica para o incremento 13. Ligações das tarefas para `/tasks/:id`, com uma página provisória até ao incremento 12. Cliente com `requestPage` (total do cabeçalho), `useLoad` que mantém a lista quando uma atualização falha e testes de limites contra o `openapi.yaml`. Wireframe do detalhe do projeto e `wireframes.md` atualizados (título de 3 a 150, campo de data nativo, responsáveis em caixas); o texto do contrato passa de "title up to 100" para 3 a 150 (A46), sem mudar o comportamento. **Testes:** 142 de componentes (Vitest), verdes; `tsc`, `oxlint` e `build` limpos. **Verificado por mutação** (cerca de 30 mutantes, todos mortos; os que sobreviveram à primeira obrigaram a mais testes ou a simplificar o código: o `NaN` do cabeçalho, a página fora do separador *List*, o quadrante calculado, o prazo de hoje, o título de 150, a volta à primeira página e o fim da paginação). **Verificado em 2026-11-16 num navegador**, com a API real e a base de teste: abrir a Matriz de um projeto, criar uma tarefa com título `<b>...</b>` (mostra-se literalmente; entra no quadrante certo e na Lista, com o total), abrir a Lista pelo endereço com o filtro de estado, o painel com as contagens a subir, e a largura de telemóvel. Depois da revisão: "hoje" do prazo e da data de início lê-se no fuso do projeto (como o servidor, P26), o gestor que passa o projeto sai do detalhe, uma página para lá da última mantém a paginação, os separadores são ligações com `aria-current`, o eliminar projeto é um só componente e as ligações de volta da tarefa regressam à vista de origem. Numa segunda revisão: a mensagem que vem no estado da navegação usa-se uma só vez, o dia num fuso constrói-se com `formatToParts`, a mensagem de sucesso desaparece quando a vista muda e o erro dos responsáveis liga-se ao grupo. Não verificado: o aviso de mais de 200 tarefas e a paginação contra a API real (só em testes de componentes), e os filtros por quadrante e *My tasks* num navegador |
| 12 | Interface: detalhe da tarefa, responsáveis e comentários | Concluído | 2026-11-17 | **Detalhe da tarefa e da subtarefa** no mesmo ecrã (`/tasks/:id`): caminho com a tarefa-mãe, formulário único com *Save changes* que envia só o que mudou e a `version` lida, quadrante calculado ao vivo, motivo de bloqueio só em *Blocked* (obrigatório, até 100) e tarefa original só em *Duplicate* (uma tarefa principal do projeto, sem ela própria nem duplicadas), com as notas de original eliminado ou na reciclagem (A41). **Conflito 409 (R30, decisão da autora):** a mensagem da API com *Reload latest*, que lê a tarefa de novo, mantém o que o utilizador escreveu, toma o resto do servidor e diz que campos a outra pessoa também mudou; guardar de novo envia só o que o utilizador alterou. **Aviso ao sair** com alterações por guardar (ligações, botão voltar e fechar o separador), para o que a aplicação passou a um *router* com dados (`createBrowserRouter`, `useBlocker`). **Responsáveis** (juntar numa lista dos que faltam, remover, sem o botão no último, 409 `ASSIGNEE_ALREADY_ASSIGNED` com a lista atualizada), **subtarefas** (criar com `parentTaskId`, prazo não posterior ao da tarefa-mãe, só numa tarefa principal aberta de um projeto aberto), **eliminar** com confirmação (volta ao projeto ou à tarefa-mãe, com mensagem) e **comentários** em páginas de 50 (da mais antiga à mais recente, autor e data, texto literal, editar e apagar só ao autor, apagar de vez com confirmação, depois de comentar vai-se à última página). Quem vê o quê: o membro numa tarefa fechada só escolhe estados finais (P8); sem comentários numa tarefa fechada (P37); só leitura num projeto concluído; sem *Add subtask* num projeto suspenso. O prazo novo lê-se no fuso do projeto. Wireframe e `wireframes.md` da tarefa atualizados. **Testes:** 216 de componentes (Vitest), verdes; `tsc`, `oxlint` e `build` limpos. **Verificado por mutação** (44 mutantes; 10 sobreviveram à primeira: 8 morreram com testes novos e 2 resolveram-se simplificando o código; o teste já tinha apanhado um erro real, a tarefa anterior mostrada depois de mudar de endereço). **Verificado em 2026-11-17 num navegador**, com a API real e a base de teste: abrir uma tarefa, guardar (descrição com `<b>` mostrada literalmente), um conflito real (a Marta alterou a tarefa por `curl`): 409, *Reload latest* manteve o título e o estado escritos, trouxe a urgência nova e avisou do título, guardar de novo, comentar (só o autor vê *Edit* e *Delete*), o aviso ao sair com alterações, criar uma subtarefa (prazo acima do da tarefa-mãe recusado) e a largura de telemóvel. Depois da revisão: o aviso ao sair passou a proteger também um comentário ou uma subtarefa por enviar, o endereço de origem compara o segmento do caminho, cada caixa de confirmação tem o seu `id`, uma edição de comentário cai ao mudar de página, a falha ao atualizar as subtarefas é dita e a lista de originais avisa quando há mais de 200 tarefas. Não verificado contra a API real: juntar e remover responsáveis, editar e apagar comentários, eliminar a tarefa, as páginas de comentários e os estados *Blocked* e *Duplicate* (só em testes de componentes) |
| 13 | Interface: equipa, reciclagem e fecho | Por fazer | | |

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

> **R19 confirmado em 2026-11-12 (substituído em 2026-11-14, ver a nota seguinte):** em `createProject`, quem cria vem da sessão e tem de ser gestor (403 se não for); o `managerId` é indicado por quem cria (R19) e o servidor valida que tem o papel de gestor (M10, `MANAGER_NOT_A_MANAGER`). Se for outra pessoa, o criador deixa de ver o projeto. O exercício pede para inferir o proprietário no servidor: aqui, é o criador (sessão) que decide quem gere, e nunca um valor que prove identidade.

> **R19 alterado em 2026-11-14, a pedido da autora (Exercício 3):** o proprietário do projeto é inferido no servidor. Em `createProject` o gestor é o criador, que vem da sessão e tem de ser gestor (403 se não for); o pedido deixou de ter `managerId` e um `managerId` enviado dá 400 (A44). O gestor só muda com `updateProject` (A12, M10). Também mudaram os limites dos projetos, para seguir o exemplo do enunciado: nome de 3 a 100 caracteres e descrição até 1000 (P17, R14, A45); as tarefas mantêm 100 e 2000.

> **Exercícios 5 e 6, 2026-11-14:** o título de uma tarefa passou a ter de 3 a 150 caracteres (migração V3), e um nome de projeto ou um título só com espaços, de qualquer tipo, é recusado (A46); atribuir quem já é responsável passou a dar 409 `ASSIGNEE_ALREADY_ASSIGNED` (A16, A47); e há um teste de que os comentários e os responsáveis permanecem depois de reabrir a tarefa (cenário 11.12). Os estados e a prioridade (urgência e importância) mantêm-se como estão, por decisão da autora.

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
| **Aceitação** | Cenários 7.1 a 7.10; EC7, EC21, EC23, EC36 e EC49 a EC51; os membros recuperam tarefas e subtarefas dos seus projetos, só o gestor recupera projetos |
| **Decisões** | **Cenário 11.7, segunda metade:** os comentários voltam a aparecer quando a tarefa é recuperada (já só dependem de a tarefa deixar de estar apagada). **P36 (2026-11-14):** ao recuperar uma tarefa ou subtarefa, se algum responsável já não for membro do projeto nem o seu gestor, a recuperação pede primeiro um novo responsável (R21, R26). A remoção de membros (incremento 3b) só passa para substitutos as tarefas em aberto não apagadas |
| **Verificação** | Relógio controlado para os 29 e 31 dias; cascata e recuperação do projeto e das subtarefas; projeto concluído não se apaga (R28) |

### Incremento 8 — Fecho do MVP

| | |
|---|---|
| **Componentes e ficheiros** | Transversais (NF08, NF09), dados de demonstração, documentação |
| **API** | Todas as 28 operações cobertas |
| **Depende de** | Todos os anteriores |
| **Aceitação** | O cenário MVP-1 passa de ponta a ponta; o critério de aceitação do MVP ([requirements.md](requirements.md#6-âmbito-do-mvp-v1)) cumpre-se |
| **Verificação** | Todos os cenários e casos excecionais da v1 verdes; o contrato e a implementação coincidem; documentação revista |

### A interface: regras para os incrementos 9 a 13

A interface é a aplicação web **React com TypeScript e Vite**, em inglês (NF01), numa pasta `frontend/` na raiz do repositório (a confirmar no plano do incremento 9). Liga-se **à API real**, nunca a dados simulados; ao fechar cada incremento não fica nenhum mock das funcionalidades concluídas. Seguem-se as regras do Exercício 7, que valem para todos os ecrãs:

| Regra | O que significa |
|---|---|
| Configuração do endereço | O endereço do backend está num só sítio (uma variável de ambiente, `VITE_API_URL`, lida por um só módulo). O endereço de exemplo do OpenAPI não corresponde a um serviço ativo |
| Estados de cada pedido | Todo o ecrã trata o carregamento, a lista vazia, o sucesso e o erro, com a mensagem que a API dá (NF08) |
| Formulários | Os mesmos componentes, estilos e mensagens em todos os formulários; os erros de validação aparecem junto ao campo. Se a submissão falha, o que a pessoa escreveu fica no formulário |
| Sem submissões repetidas | Enquanto um pedido está em curso, o botão fica desativado; a interface usa a resposta confirmada pela API para se atualizar, sem criar um registo duas vezes |
| Listas e contagens | Depois de criar, alterar ou apagar, as listas, os filtros e as contagens atualizam-se sem recarregar a página. Os totais vêm do servidor (`X-Total-Count`, `getDashboard`), por isso refletem todos os dados que interessam e não só a página que se vê |
| Sessão | Um 401 leva ao início de sessão com a mensagem que a API dá ("Your session has ended..." ou "Sign in to continue.", P33) |
| Controlos | O que a pessoa não pode fazer não aparece (NF08), com a API a recusar na mesma |
| Larguras de ecrã | O layout adapta-se a três larguras: computador, tablet e telemóvel (NF02, decidido em 2026-11-14). Em ecrãs estreitos a tabela passa a cartões, a barra superior fica recolhida e os quadrantes da Matriz empilham-se; os ecrãs desenham-se com larguras fluidas desde o início |
| Verificação | Testes de componentes (Vitest e Testing Library) e a verificação dos percursos contra o backend real e a base de dados de teste, num navegador a sério. Os testes de ponta a ponta com **Playwright** ficam para quando forem precisos, com nova aprovação (decidido em 2026-11-15). Cada incremento fecha os itens V que lhe cabem |

As dependências de cada incremento listam-se no plano do incremento e instalam-se só depois de a autora as aprovar. No incremento 9 instalou-se o essencial: React, React Router, Vite, TypeScript, Vitest, Testing Library, jsdom e o oxlint do modelo do Vite. O `openapi-typescript` (tipos gerados do contrato) e o **Playwright** ficam para mais tarde, por decisão da autora em 2026-11-15, com nova aprovação; entretanto os tipos escrevem-se à mão, a partir do contrato.

### Incremento 9 — Interface: base e início de sessão (F10; US10)

| | |
|---|---|
| **Componentes e ficheiros** | `frontend/` (projeto Vite, configuração do endereço do backend, cliente da API com os erros `{code, message, details}`, tipos escritos à mão, rotas, barra superior, componentes comuns: botão, campo de texto com erro, aviso e carregamento; a lista vazia e a caixa de confirmação chegam com o primeiro ecrã que as usa); no backend, a configuração de CORS |
| **Ecrãs** | [Início de sessão](ui/wireframes.md#1-início-de-sessão-f10-us10) e a barra superior (com o fim de sessão) |
| **API** | `signIn`, `signOut`, `getAuthenticatedUser` |
| **Backend** | **CORS com credenciais**, decidido pela autora em 2026-11-14: só as origens indicadas numa propriedade (`matriz.cors.allowed-origins`), com cookies, os métodos e cabeçalhos que a API usa e o cabeçalho `X-Total-Count` exposto. Revê-se a P31 (CSRF desligado) e regista-se a decisão: o cookie é `SameSite=Lax`, a API só aceita JSON e as origens estão limitadas; um teste mostra que uma origem não permitida não recebe os cabeçalhos CORS |
| **Depende de** | Incrementos 1 e 8 |
| **Aceitação** | Cenários 10.1 a 10.6 pela interface, incluindo o encaminhamento para o início de sessão e a mensagem quando a sessão deixa de ser válida; mensagens de campo vazio do W6 |
| **Fecha** | V4 (e a parte do V5 que se vê) |
| **Verificação** | Testes de componentes (validação, erro da API, botão desativado durante o pedido, dados preservados, 401); teste do backend para o CORS; verificação num navegador a sério com a API real e a base de teste (o Playwright ficou para mais tarde) |

### Incremento 10 — Interface: painel e projetos (F07, F01, F22; US13, US06, US01, US12)

| | |
|---|---|
| **Componentes e ficheiros** | `frontend/` (ecrã do painel e dos projetos, cartão de projeto com os 7 mosaicos, barra de progresso, formulário de projeto num painel ao lado da lista, caixas de confirmação) |
| **Ecrãs** | [Painel inicial](ui/wireframes.md#2-painel-inicial-f07-us13) e [Projetos](ui/wireframes.md#3-projetos-f01-f22-us01-us12) |
| **API** | `getDashboard`, `listProjects` (com `search`), `getProject`, `createProject`, `updateProject`, `deleteProject` |
| **Depende de** | Incremento 9 |
| **Aceitação** | Cenários 13.1 a 13.5 e 6.1 a 6.15 como se veem no painel; 1.1 a 1.3, 1.9 a 1.11, 1.15, 1.19 a 1.23 e 12.1 a 12.3 pela interface; criar, editar, concluir (com confirmação) e eliminar (com confirmação) um projeto, com a lista e as contagens atualizadas sem recarregar |
| **Fecha** | V1, V2 (nos projetos), V11, V12 (a confirmação ao apagar um projeto), V13 (**o primeiro percurso completo do Exercício 4**: iniciar sessão, criar um projeto, vê-lo na lista, recarregar a página e reiniciar o backend, e voltar a consultá-lo depois de iniciar sessão de novo, P32) |
| **Verificação** | Testes de componentes (estados de carregamento, vazio, erro e sucesso; formulário; confirmação); testes de ponta a ponta contra o backend e a base de dados reais, com o percurso do V13 |

### Incremento 11 — Interface: detalhe do projeto e tarefas (F02, F04, F05, F22; US02, US03, US09, US12)

| | |
|---|---|
| **Componentes e ficheiros** | `frontend/` (detalhe do projeto com os separadores *Matrix* e *List*, filtros "as minhas", estado e quadrante, paginação com o total do servidor, formulário de tarefa num painel ao lado da tabela, seletor de data comum) |
| **Ecrãs** | [Detalhe do projeto](ui/wireframes.md#4-detalhe-do-projeto-f01-f02-f05-f22-us01-us02-us09-us12) e a [Matriz](ui/matrix-wireframe.md) |
| **API** | `listProjectTasks`, `createTask`, `listAssignableUsers` |
| **Depende de** | Incremento 10 |
| **Aceitação** | Cenários 2.1 a 2.8, 3.1 a 3.6, 9.1 a 9.5 e 12.4 a 12.6 pela interface; os filtros combinam-se; depois de criar uma tarefa a lista, a Matriz e as contagens atualizam-se; o total mostrado é o do servidor |
| **Fecha** | V7 (os controlos de criar), V10 (a Matriz esconde as tarefas finais), V14 (os erros de validação junto ao campo), V15 (as listas e as contagens que se atualizam) |
| **Verificação** | Testes de componentes (filtros, paginação, lista vazia, erro); teste de ponta a ponta: criar uma tarefa e ver-a na Matriz, na Lista e nas contagens do painel |

### Incremento 12 — Interface: detalhe da tarefa, responsáveis e comentários (F03, F08, F17; US04, US05, US11)

| | |
|---|---|
| **Componentes e ficheiros** | `frontend/` (detalhe da tarefa num só ecrã, edição com a `version`, estado e prioridade (urgência e importância), responsáveis, subtarefas, comentários, apagar com confirmação) |
| **Ecrãs** | [Detalhe da tarefa](ui/wireframes.md#5-detalhe-da-tarefa-f02-f03-f08-f17-us02-us04-us11) |
| **API** | `getTask`, `updateTask`, `deleteTask`, `addAssignee`, `removeAssignee`, `listAssignableUsers`, `listComments`, `addComment`, `editComment`, `deleteComment` |
| **Depende de** | Incrementos 6, 7 e 11 |
| **Aceitação** | Cenários 2.9 a 2.15, 4.1 a 4.7, 5.1 a 5.4 e 11.1 a 11.12 pela interface: um 409 `TASK_CHANGED` pede para recarregar sem perder o que se escreveu; um responsável repetido mostra a mensagem do 409; só o autor vê editar e apagar o comentário; as tarefas e comentários fechados só se leem |
| **Fecha** | V6 (o texto mostra-se literalmente), V7, V9, V16 (a escolha de responsáveis e os comentários com autor e data) |
| **Verificação** | Testes de componentes (conflito, erros, texto com `<b>x</b>`); teste de ponta a ponta: editar, mudar o estado, atribuir, comentar e reabrir, com os comentários e os responsáveis que continuam lá |

### Incremento 13 — Interface: equipa, reciclagem e fecho (F01, F09; US01, US07; Exercício 7)

| | |
|---|---|
| **Componentes e ficheiros** | `frontend/` (equipa, reciclagem, ajustes finais de layout e de componentes) |
| **Ecrãs** | [Equipa](ui/wireframes.md#6-equipa-f01-us01) e [Reciclagem](ui/wireframes.md#7-reciclagem-f09-us07) |
| **API** | `listMembers`, `addMember`, `removeMember`, `listCandidateMembers`, `listRecycleBin`, `restoreProject`, `restoreTask` |
| **Depende de** | Incrementos 10 a 12 |
| **Aceitação** | Cenários 1.4 a 1.8, 1.12 a 1.14, 1.24 a 1.28, 7.1 a 7.10 pela interface; o cenário MVP-1 de ponta a ponta pela interface; as larguras de ecrã decididas; nenhum mock a restar |
| **Fecha** | V3, V8, V12 (o resto) e a revisão final de V1 a V16 |
| **Verificação** | Testes de ponta a ponta do cenário MVP-1 e dos percursos da equipa e da reciclagem; revisão do diff; relatório do que ficou por verificar |

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
| 9 | (interface) `signIn`, `signOut`, `getAuthenticatedUser` |
| 10 | (interface) `getDashboard`, `listProjects`, `getProject`, `createProject`, `updateProject`, `deleteProject` |
| 11 | (interface) `listProjectTasks`, `createTask`, `listAssignableUsers` |
| 12 | (interface) `getTask`, `updateTask`, `deleteTask`, `addAssignee`, `removeAssignee`, `listComments`, `addComment`, `editComment`, `deleteComment` |
| 13 | (interface) `listMembers`, `addMember`, `removeMember`, `listCandidateMembers`, `listRecycleBin`, `restoreProject`, `restoreTask` |

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
| Interface | **React com TypeScript e Vite**, escolhida pela autora em 2026-11-14, em 5 incrementos (9 a 13). Para quem nunca a usou, a adaptação leva alguns dias: por isso o incremento 9 começa por um corte vertical (início de sessão ligado à API real) que a autora revê antes de se replicar o padrão aos outros ecrãs |
| Larguras de ecrã | **Decidido em 2026-11-14:** layout fluido com três larguras (computador, tablet e telemóvel), como pede o Exercício 7; o NF02 foi alterado. A tabela passa a cartões e os quadrantes da Matriz empilham-se em ecrãs estreitos. É trabalho de estilos da interface, sem mudar a API, que se desenha desde o incremento 9 para não ter de se refazer; os wireframes mostram o computador e as regras para as larguras estreitas estão em [ui/wireframes.md](ui/wireframes.md#10-larguras-de-ecrã). Estima-se 15 a 25% mais trabalho na interface |
| CORS, CSRF e sessões com a interface | O backend não tem CORS. Decidido em 2026-11-14: CORS com credenciais para as origens indicadas (incremento 9), em vez de um *proxy* do servidor de desenvolvimento; a P31 revê-se nesse incremento. As sessões continuam em memória (P32): reiniciar o backend leva a interface ao início de sessão |
| Corpo que não é JSON válido | Um corpo malformado (por exemplo só `{`) é recusado pelo Spring com 400 antes de o caso de uso verificar o acesso, por isso quem não tem acesso recebe 400 em vez de 403 ou 404. Só revela que o corpo estava malformado, não dados. Registado em 2026-11-14 por decisão da autora, sem mudar o padrão dos endpoints com corpo |
| Ligação do que foi apagado junto | O que foi apagado numa ação liga-se pelo mesmo `deleted_at` (microssegundos). Duas eliminações no mesmo instante, ou com o relógio do sistema atrás, juntam-se como se fossem uma só: uma tarefa apagada à parte e o seu projeto, por exemplo, voltam juntos. Na prática é raro, e nos testes o relógio controla-se. Uma coluna com o identificador de cada eliminação resolveria, com uma migração. Registado em 2026-11-14 por decisão da autora, a rever se aparecer um caso real |
| Concluir ou apagar um projeto e escrever nele ao mesmo tempo | **Resolvido no incremento 8 (2026-11-14).** Toda a escrita num projeto bloqueia a sua linha em modo partilhado e lê o estado com o bloqueio; concluir, alterar ou apagar o projeto bloqueia-a em modo exclusivo, por isso uma escrita ou acontece antes ou vê o estado novo. Um teste de concorrência por caminho de escrita. A rede de segurança da eliminação definitiva (incremento 7) mantém-se |
| Estados e prioridade face ao exemplo da Aula 02 | O exemplo do enunciado tem 3 estados (`TODO`, `IN_PROGRESS`, `DONE`) e uma prioridade `LOW`, `MEDIUM` ou `HIGH` com `MEDIUM` por omissão. A aplicação tem 7 estados (To do, In progress, In review, Blocked, Done, Cancelled e Duplicate, F03, R4 a R6) e a prioridade é a urgência e a importância (1 a 4 cada), de que se calcula o quadrante da Matriz (F04, R25); filtra-se por estado e por quadrante. Decidido pela autora em 2026-11-14: mantém-se, porque a Matriz precisa dos dois eixos. Se for preciso aproximar do exemplo, a alternativa pequena é devolver um campo derivado `priority` (`HIGH` para Do first, `MEDIUM` para Schedule e Delegate, `LOW` para Later) e aceitar o filtro `priority`, sem mexer no modelo |
| Paginação das listas | `listProjectTasks` e `listComments` vêm em páginas (`page` a partir de 1 e `size` de 1 a 200, por omissão 50); o total vai no cabeçalho `X-Total-Count` e o corpo continua a ser uma lista. Decidido pela autora em 2026-11-14 (pressupostos A30 e A31, confirmados). `listProjects`, `getDashboard` e as listas de membros ficam sem paginação: são pequenas (P18, 10 utilizadores) |
| Validação do contrato | Validado no Swagger Editor em 2026-11-12, sem erros. O incremento 0 acrescenta um teste automático que o valida a cada compilação |

## 8. Verificações adiadas para a fase da interface

Itens que a API não consegue verificar sozinha e que não se podem esquecer quando a interface for feita. Cada incremento da interface (9 a 13, secção 4) diz quais fecha. Quando um item for verificado, indica-se aqui a data e como.

| # | Item | Origem | Incremento | Estado |
|---|---|---|---|---|
| V1 | Pedir uma confirmação antes de marcar um projeto como concluído, a avisar que deixa de poder ser alterado ou reaberto; ao cancelar, o estado mantém-se | Cenário 1.22, EC39, R28 | 2 | **Verificado em 2026-11-16** no navegador, com a API real: a confirmação aparece ao guardar com o estado *Completed*; *Cancel* não guarda e o formulário mantém o que estava; confirmar conclui o projeto, que deixa de ter *Edit* e *Delete* |
| V2 | Os controlos que a pessoa não pode usar não aparecem (criar projeto para membros, alterar o projeto e a equipa para membros, e tudo num projeto concluído). A API já os recusa com 403 ou 422 | Cenários 1.2, 1.3, 1.6, 1.21, NF08 | 2 | **Verificado nos projetos em 2026-11-16** (testes de componentes e navegador): *+ New project*, *Edit* e *Delete* só para o gestor, e nenhum deles num projeto concluído ou gerido por outra pessoa. A equipa e o detalhe ficam para o incremento 11 |
| V3 | Aviso ao gestor, ao retomar um projeto suspenso, de que há membros noutro projeto em aberto (a API devolve `inOtherOpenProject`) | Cenário 1.8, P22 | 2 | Por verificar na interface |
| V4 | Encaminhar para o início de sessão quando a sessão deixa de ser válida, mostrando a mensagem do servidor ("Your session has ended..." ou "Sign in to continue.") | Cenários 10.5 e 10.6, EC31, EC38, P33, W16 | 1 | **Verificado em 2026-11-15** no navegador, com a API real: ligação direta sem sessão ("Sign in to continue.") e sessão perdida ao reiniciar o backend ("Your session has ended. Sign in again to continue.") |
| V5 | A expiração às 3 horas sem atividade: hoje só se verifica a configuração do servidor, não uma sessão realmente expirada | Cenário 10.6, EC38, S5 | 1 | Por verificar a espera real. A mensagem de sessão terminada já se vê na interface (V4); falta esperar as 3 horas |
| V6 | O texto das tarefas (título, descrição e motivo de bloqueio) mostra-se literalmente, sem formatação nem execução de código (por exemplo, `<b>x</b>`). A API guarda e devolve o texto tal como chega; a interface tem de o escapar | Cenário 2.8, NF09 | 3a | **Verificado em 2026-11-17** (testes e navegador): o título, a descrição, o motivo de bloqueio e os comentários mostram-se como texto, sem executar o HTML (descrição com `<b>` no navegador) |
| V7 | Os controlos de criar tarefas e subtarefas não aparecem num projeto concluído ou suspenso, e os de editar, mudar de estado, atribuir e remover responsáveis não aparecem num projeto concluído. A API já os recusa (422) | Cenários 2.10 e 2.13, EC29, EC35 | 3a | **Verificado em 2026-11-17** (testes de componentes): sem *+ Add subtask* num projeto suspenso, concluído ou numa tarefa fechada; num projeto concluído a tarefa só se lê (sem guardar, eliminar, responsáveis nem comentários) |
| V8 | Ao remover um membro com tarefas, a interface mostra um aviso a pedir substituto (cenário 1.13), deixa escolher um substituto por tarefa, mostra quem está disponível (`listCandidateMembers`) e explica as recusas (substituto noutro projeto, mais de 5 tarefas, já responsável) | Cenários 1.12 a 1.14 e 1.24 a 1.28, EC15, EC18, EC41 | 3b | Por verificar na interface |
| V9 | Os comentários: o controlo de editar e apagar só aparece ao autor (não aos outros, nem ao gestor), e os de comentar, editar e apagar não aparecem num projeto concluído nem numa tarefa fechada, onde só se lê. A API já recusa (403 e 422). O texto dos comentários mostra-se literalmente, como o das tarefas (V6) | Cenários 11.4, 11.6, 11.10 e 11.11, EC34, EC48, NF09 | 6 | **Verificado em 2026-11-17** (testes de componentes; no navegador, só o autor via *Edit* e *Delete* no seu comentário): editar e apagar só ao autor, nem ao gestor; nada num projeto concluído nem numa tarefa fechada; texto literal |
| V10 | A Matriz esconde as tarefas em estado final (Done, Cancelled, Duplicate), porque a API as devolve na lista (P10); a Lista mostra-as depois das abertas. Os controlos "as minhas", estado e quadrante combinam-se, e a lista vazia mostra uma mensagem em vez de um ecrã em branco | Cenários 3.2, 9.3, 12.4 a 12.6, EC33, P10 | 4 | **Verificado em 2026-11-16** (testes de componentes e navegador): a Matriz esconde as tarefas finais e a Lista mostra-as; *My tasks*, estado e quadrante combinam-se; a lista vazia diz "No tasks match the filters." ou "This project has no tasks yet." |
| V11 | O painel mostra a etiqueta "sem tarefas" no lugar da percentagem quando `hasCountableTasks` é falso; a barra e o estado "Attention" ou "Late" só aparecem ao gestor do projeto (a API só os devolve a ele); os projetos concluídos não aparecem; ao selecionar um projeto abre-se a Matriz | Cenários 6.2, 6.7, 6.9, 6.10, 13.3 e 13.5, EC1 | 5 | **Verificado em parte em 2026-11-16** (testes de componentes e navegador): "No tasks" no lugar da percentagem, barra e estado calculado só ao gestor (testes), projetos concluídos fora do painel (a API não os devolve). **Por verificar:** abrir a Matriz ao selecionar um projeto (incremento 11) |
| V12 | A reciclagem (e as notas "the original task was eliminated" e "the original task is in the recycle bin" numa tarefa com `originalEliminated` ou `originalInRecycleBin`): o botão *Restore* fica desativado nos itens que não podem voltar, com o motivo que a API dá; ao recuperar uma tarefa cujo responsável saiu, a interface mostra quem saiu e deixa escolher quem o substitui (`ASSIGNEE_REQUIRED`); pede-se confirmação antes de apagar um projeto, a avisar que fica 30 dias na reciclagem | Cenários 7.3, 7.9 e 7.10, EC49 a EC51, R20 | 7 | **Verificado em parte em 2026-11-16**: a confirmação ao eliminar um projeto, que avisa dos 30 dias na reciclagem (testes e navegador). **Por verificar:** a reciclagem e as notas das tarefas (incrementos 11 e 13) |
| V13 | O primeiro percurso completo (Exercício 4): iniciar sessão, criar um projeto, vê-lo na listagem e voltar a consultá-lo depois de recarregar a página e de reiniciar o backend. Os dados mantêm-se; as sessões estão em memória, por isso depois de reiniciar o backend a interface leva ao início de sessão (V4, P32) | Cenários 1.1, 10.1 e 10.6, P32 | 2 | **Verificado em 2026-11-16** no navegador, com a API real e a base de teste: iniciar sessão, criar um projeto, vê-lo na lista, recarregar a página, reiniciar o backend (a interface leva ao início de sessão com "Your session has ended. Sign in again to continue.") e, depois de iniciar sessão de novo, o projeto continua na lista |
| V14 | Os erros de validação do backend aparecem na interface, junto ao campo, com a mensagem que a API dá: nome ou título só com espaços ou fora dos limites, prazo, texto demasiado longo, campos em falta (Exercício 5, NF08) | Cenários 1.9, 1.15, 2.7, 2.14, EC52, NF08 | 3a | **Verificado em 2026-11-17** (testes de componentes): título, descrição, prazo, motivo de bloqueio, original e comentário vazio ou longo aparecem junto ao campo, e a mensagem e os campos que a API indica também, com o foco no primeiro |
| V15 | Depois de cada operação (criar, alterar, mudar de estado, atribuir, comentar, apagar, recuperar) as listas, a Matriz e as contagens do painel atualizam-se sem o utilizador recarregar a página (Exercício 5) | Cenários 3.1 a 3.6, 9.1 a 9.5, 13.1 | 4 | **Verificado em parte em 2026-11-17** (testes e navegador): depois de guardar, juntar ou remover responsáveis, criar subtarefas e comentar, a tarefa, a lista de subtarefas e os comentários atualizam-se sem recarregar. **Por verificar:** apagar e recuperar na reciclagem (incremento 13) |
| V16 | A escolha de responsáveis mostra só os utilizadores elegíveis (`listAssignableUsers`), deixa associar vários e remover cada um, mostra a mensagem quando a pessoa já é responsável (409) e os comentários mostram o autor e a data; ao reabrir uma tarefa os comentários e os responsáveis continuam lá (Exercício 6) | Cenários 2.9, 2.11, 2.15, 11.1, 11.12, EC53 | 3a, 6 | **Verificado em parte em 2026-11-17:** a lista de responsáveis só tem os elegíveis, deixa juntar vários e remover cada um, sem o botão no último, e mostra a mensagem do 409 (testes de componentes); os comentários mostram autor e data (também no navegador). **Por verificar:** juntar e remover contra a API real, e o que fica ao reabrir uma tarefa (incremento 13) |
