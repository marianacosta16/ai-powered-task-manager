# Matriz — API HTTP

> A API é descrita pelo contrato [OpenAPI 3.1](../openapi.yaml) (versão **1.1.0** do contrato; a aplicação é a versão 1.0.0). O contrato é a fonte de verdade: o teste `ContractConformanceIntegrationTest` chama as 28 operações a sério e confirma que as rotas, os estados HTTP e os corpos coincidem com ele. Esta página é só um índice para o ler mais depressa; em caso de dúvida vale o `openapi.yaml`.

## Como consultar o contrato

- Ler o ficheiro [openapi.yaml](../openapi.yaml) (tem os esquemas, os exemplos e os códigos de erro de cada operação).
- Ou abri-lo num visualizador de OpenAPI à escolha (por exemplo, colar o conteúdo no *Swagger Editor* em editor.swagger.io, que corre no navegador). A aplicação **não serve** uma página de documentação própria: não tem o Swagger UI, de propósito, para não acrescentar uma dependência e uma rota ao backend só para isso.
- Executar a aplicação (ver o [README](../README.md)) e experimentar com `curl`, como nos exemplos do README.

## Convenções

| Tema | Regra |
|---|---|
| Autenticação | `POST /auth/login` devolve o cookie `SESSION` (`HttpOnly`, `SameSite=Lax`, 3 horas sem atividade). Todas as outras operações exigem-no: sem sessão válida, `401 UNAUTHENTICATED` |
| Quem faz o pedido | Vem sempre da sessão, nunca do corpo do pedido |
| Acesso | Sem acesso ao projeto, `403 FORBIDDEN` (também por ligação direta); um projeto ou recurso que não existe, `404` |
| Erros | Sempre com um `code` (por exemplo `VALIDATION_ERROR`, `DEADLINE_IN_PAST`, `TASK_CHANGED`) e uma `message` |
| Corpo desconhecido | Campos que o contrato não conhece são recusados (`400`) |
| Listas | As de tarefas de um projeto e de comentários são paginadas (`page` a partir de 1, `size` de 1 a 200, 50 por omissão), com o total no cabeçalho `X-Total-Count` |
| Edições simultâneas | `updateTask` leva a `version` lida; se a tarefa mudou entretanto, `409 TASK_CHANGED` |
| Interface | O navegador chama a API de outra origem com CORS (as origens de `CORS_ALLOWED_ORIGINS`) ou, com o Docker Compose, pelo mesmo endereço, em `/api` |

## As 28 operações

| Grupo | Operação | Método e caminho | O que faz |
|---|---|---|---|
| Auth | `signIn` | `POST /auth/login` | Sign in |
| Auth | `signOut` | `POST /auth/logout` | Sign out |
| Auth | `getAuthenticatedUser` | `GET /auth/me` | Get the authenticated user |
| Projects | `listProjects` | `GET /projects` | List and search projects |
| Projects | `createProject` | `POST /projects` | Create a project |
| Projects | `getProject` | `GET /projects/{projectId}` | Get a project |
| Projects | `updateProject` | `PATCH /projects/{projectId}` | Update a project |
| Projects | `deleteProject` | `DELETE /projects/{projectId}` | Delete a project |
| Projects | `listMembers` | `GET /projects/{projectId}/members` | List the members of a project |
| Projects | `addMember` | `POST /projects/{projectId}/members` | Add a member to a project |
| Projects | `removeMember` | `POST /projects/{projectId}/members/{userId}/removal` | Remove a member from a project |
| Projects | `listCandidateMembers` | `GET /projects/{projectId}/candidate-members` | List the people who can be added to a project |
| Projects | `listAssignableUsers` | `GET /projects/{projectId}/assignable-users` | List the users who can be assigned to tasks of a project |
| Tasks | `createTask` | `POST /projects/{projectId}/tasks` | Create a task or a subtask |
| Tasks | `listProjectTasks` | `GET /projects/{projectId}/tasks` | List the tasks of a project |
| Tasks | `getTask` | `GET /tasks/{taskId}` | Get a task or a subtask |
| Tasks | `updateTask` | `PATCH /tasks/{taskId}` | Update a task or a subtask |
| Tasks | `deleteTask` | `DELETE /tasks/{taskId}` | Delete a task or a subtask |
| Tasks | `addAssignee` | `POST /tasks/{taskId}/assignees` | Assign a user to a task |
| Tasks | `removeAssignee` | `DELETE /tasks/{taskId}/assignees/{userId}` | Remove an assignee from a task |
| Comments | `listComments` | `GET /tasks/{taskId}/comments` | List the comments of a task or subtask |
| Comments | `addComment` | `POST /tasks/{taskId}/comments` | Add a comment to a task or subtask |
| Comments | `editComment` | `PATCH /comments/{commentId}` | Edit a comment |
| Comments | `deleteComment` | `DELETE /comments/{commentId}` | Delete a comment |
| Dashboard | `getDashboard` | `GET /dashboard` | Get the dashboard |
| Recycle bin | `listRecycleBin` | `GET /recycle-bin` | List the deleted items |
| Recycle bin | `restoreProject` | `POST /recycle-bin/projects/{projectId}/restore` | Restore a deleted project |
| Recycle bin | `restoreTask` | `POST /recycle-bin/tasks/{taskId}/restore` | Restore a deleted task or subtask |

Os detalhes de cada regra (códigos de erro, limites, estados) estão nas secções por funcionalidade do [README](../README.md#regras-e-exemplos-por-funcionalidade).
