# Revisão de código e de segurança

> Regista o que a revisão encontrou, o que foi confirmado e corrigido, e o que **não** foi verificado. Os achados vêm dos relatórios do `/code-review` (nível alto) corridos a cada pull request, do 14 ao 19 e nos incrementos anteriores. Cada correção foi aplicada depois de a autora a rever, e cada uma tem um teste de regressão. Data: 2026-11-18.
> Facto observado = reproduzido por um teste ou visto no código. Hipótese = suspeita sem prova (nenhuma ficou aberta).

## 1. Números (do registo da sessão)

| | |
|---|---|
| Relatórios de revisão | 74, de 2026-11-13 a 2026-11-18 |
| Achados distintos | cerca de 250 (o número é aproximado: o mesmo achado aparece reescrito entre rondas) |
| Corrigidos, com teste | a grande maioria (cerca de 145) |
| Não corrigidos de propósito | cerca de 20 (adiados ou recusados com razão, ver a secção 3) |
| Achados sem efeito (o código já estava certo) | 6 |
| Por tipo | correção de comportamento cerca de 100, testes em falta cerca de 35, simplificação, reutilização e eficiência cerca de 85, segurança 7 |

## 2. Controlo de acesso (IDOR / Broken Access Control)

| Verificação | Resultado | Evidência |
|---|---|---|
| Um utilizador sem acesso lê, altera ou apaga projetos, tarefas ou comentários de outro, **mudando só o identificador** | **Nenhum caso confirmado.** Todas as 25 operações recusam com `403` (ou `404`) e a base fica igual | `AuthorizationByIdentifierIntegrationTest` (5 testes, executados em 2026-11-18, passam) |
| Corpo inválido de quem não tem acesso | `403` antes do `400` (não se descobre nada pela validação) | `TasksIntegrationTest.aMemberOfAnotherProjectGets403EvenWithAnInvalidBody`; dois achados em que isto falhava foram corrigidos (secção 3, linhas 4 e 5) |
| Comentário de outra pessoa, mesmo pelo gestor | `403` | testes de comentários |
| Sem sessão | `401` | `AccessIntegrationTest` |
| Exposição de dados e *secrets* | Nada encontrado: as palavras-passe só em variáveis de ambiente, o `.env` fora do Git, as credenciais do CI são valores falsos (AGENTS.md, secção 7) | revisão de `.gitignore`, do `ci.yml` e do `.env.example` |

**Não foi feito:** uma sessão manual com duas contas num navegador. A verificação foi só por testes automáticos de pedidos HTTP.

## 3. Achados relevantes

| # | Ficheiro | Evidência (facto observado) | Impacto | Gravidade | Estado | Teste de regressão |
|---|---|---|---|---|---|---|
| 1 | `ProjectReadUseCases` | `404` para um projeto que não existe e `403` para um que existe sem acesso: quem tem sessão descobre que um identificador existe | Baixo: só revela a existência, sem ver o conteúdo; sem dados reais (C8) | Baixa | **Aceite** como decisão da autora (P34), e não corrigido | o `403` e o `404` estão testados |
| 2 | `TaskUseCases.createTask` | Bloqueava a linha da tarefa-pai antes de verificar que pertence ao projeto do pedido | Um utilizador podia travar tarefas de projetos alheios | Média | **Corrigido** | testes de concorrência e de acesso das tarefas |
| 3 | `TeamUseCases.removeMember` | Bloqueava as linhas dos substitutos antes de validar o pedido | O mesmo: bloquear dados fora do projeto | Média | **Corrigido** | `MemberRemovalIntegrationTest` |
| 4 | `ProjectController` | `@Valid` corria antes da verificação do papel: um membro com corpo inválido recebia `400` em vez de `403` | Mostra regras de validação a quem não tem acesso | Baixa | **Corrigido** | teste do `403` antes do `400` |
| 5 | `CommentController` | O corpo era lido antes da verificação de acesso | O mesmo | Baixa | **Corrigido** | `CommentsIntegrationTest` |
| 6 | `RecycleController` | Um corpo que não é JSON recebe `400` antes do `403` e do `404` | O mesmo, só para corpos mal formados | Baixa | **Adiado** (recusado por agora, sem efeito em dados) | — |
| 7 | `AccessIntegrationTest` | Os testes esvaziavam **qualquer** base de dados a que o `TEST_DB_URL` apontasse | Perda de dados reais por engano | Alta (para quem desenvolve) | **Corrigido** (`TestDatabase` só aceita bases acabadas em `_test`) | `TestDatabaseTest` |
| 8 | `SecurityConfig` | Erros fora dos tratados (tipo de conteúdo, método, caminho) não saíam no formato de erro do contrato | Respostas inesperadas | Baixa | **Corrigido** | `ContractConformanceIntegrationTest` |
| 9 | `ApiExceptionHandler` | Todo o erro de autenticação virava `401 INVALID_CREDENTIALS` | Mensagem errada | Baixa | **Corrigido** | `AccessIntegrationTest` |
| 10 | `CorsConfig` | O texto dizia que só aceitar JSON impede formulários de outros sítios, mas há `POST` sem corpo | Garantia escrita maior do que a real | Baixa | **Corrigido** (o texto foi corrigido) | — |
| 11 | `Texts` | Texto só com caracteres invisíveis passava a regra do "só espaços" | Títulos aparentemente vazios | Média | **Corrigido** | `TextsTest` e os mutantes do plano de testes |
| 12 | `AuthProvider.tsx` | Um `401` tardio de uma sessão antiga terminava a sessão nova | Terminar a sessão a quem acabou de entrar | Média | **Corrigido** | `AuthProvider` (testes de componentes) |
| 13 | `ValidationRulesIntegrationTest` | Cada caso inválido passava com qualquer `400` ou `422`, não com o erro certo | Teste que não detetava erros trocados | Média | **Corrigido** (cada caso exige o seu código) | o próprio teste |
| 14 | `LoadError` | Um `403` oferecia "Try again", que não pode resultar | Má indicação ao utilizador | Baixa | **Corrigido** | `LoadError.test.tsx` |

O resto dos achados corrigidos são de correção de comportamento (concorrência entre escritas, reabrir subtarefas, páginas fora de limites, rascunhos perdidos), de testes em falta e de simplificação. Estão nos relatórios e nos commits de cada pull request.

## 4. Ferramentas estáticas

| Ferramenta | Onde | Resultado |
|---|---|---|
| `oxlint` | frontend, no CI | Sem erros |
| `npm audit` | frontend | `--omit=dev`: **0** vulnerabilidades. Com as de desenvolvimento: 6 avisos altos, todos do mesmo pacote (`picomatch`, ferramentas de desenvolvimento, **sem versão corrigida**). **Suspeita sem efeito em produção, não corrigida** |
| TypeScript (`tsc -b`) e testes de contrato (`OpenApiContractTest`) | CI | Passam |
| **Backend** (SpotBugs, Semgrep, verificação de dependências do Maven) | — | **Não existe nenhuma configurada.** O backend só foi revisto pelo `/code-review` e pelos testes |

## 5. O que ficou por validar

- Sessão manual com duas contas no navegador (IDOR).
- Uma ferramenta estática e uma verificação de dependências para o backend (mexe no `pom.xml`, precisa de aprovação).
- Testes de intrusão e de limites de pedidos (por exemplo, abuso de pedidos repetidos): fora do âmbito deste trabalho de formação.
- Os achados que ficaram **adiados** (linha 6) e os 20 aproximados não corrigidos continuam só nos relatórios.
