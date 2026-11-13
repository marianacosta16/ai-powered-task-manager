# Matriz — Restrições, Pressupostos e Decisões Pendentes

> Documento de referência das restrições e dos pressupostos da Matriz. Não propõe tecnologias nem contém código.
> Documentos relacionados: [requirements.md](requirements.md) e [user-stories.md](user-stories.md).

## 1. Restrições conhecidas

| ID | Restrição | Origem |
|---|---|---|
| C1 | Este é um trabalho de formação: não há cliente real. A autora cria a aplicação e decide em nome de um cliente fictício; as decisões só teriam de ser validadas com um cliente real se a aplicação viesse a ter um | Contexto do exercício |
| C2 | A interface é em inglês | Decisão |
| C3 | A aplicação é usada em computador | Decisão |
| C4 | Existem apenas dois papéis: gestor de projeto e membro da equipa | Decisão |
| C5 | A IA é usada como apoio ao desenvolvimento; a aplicação não tem funcionalidades de IA | Decisão |
| C6 | Não há indicadores de sucesso, orçamento nem prazo definidos por um cliente, porque não há cliente real (C1). O prazo de cerca de 1 mês do MVP é decisão da autora | Contexto do exercício (C1) |
| C7 | Os documentos estão em português; os termos da interface (por exemplo, os nomes dos quadrantes como "Do first") estão em inglês | Convenção |
| C8 | Na v1 usam-se só dados fictícios e contas de demonstração criadas por seed, num ambiente controlado, porque não se definiram regras de palavra-passe nem segurança avançada (S5) | Decisão |

## 2. Pressupostos assumidos

Propostas aceites para preencher lacunas. Devem ser validadas com um cliente real, se a aplicação vier a ter um (C1). Quando uma decisão depender de um pressuposto, deve ser questionada à autora do projeto. A coluna "Estado" indica se a autora já aceitou o pressuposto (*Confirmado*), se falta validá-lo (*Por validar*) ou se é uma condição do contexto do exercício (*Premissa*).

| ID | Pressuposto | Justificação | Estado |
|---|---|---|---|
| P1 | Existe uma única empresa, sem utilizadores externos | Não foi pedido nenhum acesso externo | Confirmado |
| P2 | O único papel de gestão é o gestor de projeto, que também forma as equipas (adicionando membros aos projetos) | Confirmado nas respostas | Confirmado |
| P3 | Fase e estado do projeto coexistem; só o gestor altera a fase e define o estado, exceto "atenção" e "atrasado", que são só calculados (ver P5) | A fase descreve o ciclo de vida e o estado, a situação atual | Confirmado |
| P4 | "Projeto em aberto" é qualquer projeto que não esteja concluído, suspenso nem arquivado | Um projeto suspenso não vai ser trabalhado durante algum tempo, por isso não conta como projeto em aberto | Confirmado |
| P5 | Para projetos "em andamento", os estados "atenção" e "atrasado" são calculados. O progresso esperado é o tempo decorrido, (agora − início) ÷ (fim − início), medido no fuso horário do projeto (P26); o desvio é o progresso esperado menos o progresso real, em pontos percentuais. **Atenção** quando o desvio é igual ou superior a 15 e inferior a 25; **atrasado** quando o desvio é igual ou superior a 25, ou quando a data de fim passou e o projeto não está concluído. Os estados "não iniciado", "suspenso" e "concluído" são definidos só pelo gestor. Um projeto "não iniciado" nunca é marcado como "atenção" nem "atrasado", mesmo depois da data de fim. Os valores calculados (progresso e desvio) são arredondados à segunda casa decimal | Permite ao gestor ver se está dentro do prazo; confirmado pela autora | Confirmado |
| P6 | O estado "adiantado" não existe | A autora decidiu ignorá-lo | Confirmado |
| P7 | Progresso = tarefas principais concluídas ÷ tarefas principais totais (as subtarefas não contam, R18), sem contar canceladas e duplicadas | Evita distorcer a barra com tarefas que não vão ser feitas | Confirmado |
| P8 | Qualquer membro move uma tarefa ou subtarefa entre todos os estados, incluindo os finais (concluído, cancelado e duplicado); reabrir uma tarefa final só o gestor, que escolhe o estado para onde a reabre | Define quem pode fazer cada mudança de estado; confirmado pela autora | Confirmado |
| P9 | A Matriz mostra todas as tarefas da equipa, com o filtro "as minhas" | Dá visão da equipa sem perder a visão pessoal | Confirmado |
| P10 | Tarefas em estado final ficam escondidas por defeito na Matriz | Mantém a Matriz focada no que falta fazer | Confirmado |
| P11 | Quando existirem lembretes (v2), os avisos aparecem só com a aplicação aberta | Consequência de não haver canais externos antes da v3 | Confirmado |
| P12 | Os projetos e tarefas apagados ficam numa reciclagem; arquivar é diferente de apagar | Separa remoção de encerramento | Confirmado |
| P13 | (v2, com a gestão de contas) Quando um utilizador sai, as suas tarefas em aberto passam para quem o substitui; o nome mantém-se no histórico e nas tarefas arquivadas ou em estado final, e os comentários que escreveu mantêm-se no histórico (confirmado em 2026-11-12) | Evita tarefas sem responsável e preserva o histórico | Confirmado |
| P14 | O registo de auditoria guarda quem fez o quê e quando (criar, editar, mudar de estado, apagar) | A resposta foi só "sim" | Por validar |
| P15 | A aplicação precisa de controlo de acesso por papel desde a v1; o utilizador inicia sessão (F10, S5) | Necessário para cumprir as regras de visibilidade e de apagar | Confirmado |
| P16 | Se não houver tarefas contáveis, o projeto mostra a etiqueta "sem tarefas" em vez de percentagem, mesmo que a data de fim tenha passado, e os estados "atenção" e "atrasado" não lhe são atribuídos por cálculo | Evita confundir "sem tarefas" com "tarefas por fazer"; confirmado pela autora | Confirmado |
| P17 | Os limites de 100 caracteres (título) e 2000 (descrição) aplicam-se também ao nome e à descrição do projeto | A autora aceitou os limites para "títulos e descrições" e confirmou que valem para tarefas e projetos | Confirmado |
| P18 | Dimensão da v1: 10 utilizadores de demonstração (2 gestores e 8 membros, P21) e os projetos que os gestores criarem; a dimensão de cada equipa é definida por quem cria o projeto (F01). Não há requisitos de volume | Substitui a referência anterior (cerca de 30 utilizadores, 3 a 5 projetos em aberto e equipas de 5 a 8 pessoas); decisão da autora em 2026-11-12 | Confirmado |
| P19 | A empresa não tem hoje nenhuma aplicação deste tipo | Premissa do exercício | Premissa |
| P20 | Ao concluir ou suspender um projeto, os seus membros ficam livres para serem adicionados a outro; a lista de membros do projeto mantém-se e eles continuam a vê-lo | Num projeto suspenso não se vai trabalhar durante algum tempo, por isso os membros têm de ser realocados; sem isto a regra de um projeto em aberto por membro (R11) não seria aplicável | Confirmado |
| P21 | Na v1 os utilizadores são contas de demonstração criadas por seed (não são criadas na aplicação); o gestor escolhe os membros entre essas contas; a criação de contas pelo gestor chega na v2 (F23). Existem dez contas: os gestores Marta e Pedro (gestor de projeto) e os membros Ana, Rui, Luís, Sofia, Carla, Inês, Tiago e Diana (membro da equipa). As palavras-passe são valores de exemplo claramente falsos, definidos no seed e não escritos na documentação | O novo âmbito aceita contas de demonstração por seed; sem criação de contas, tem de haver utilizadores pré-existentes de onde o gestor escolhe os membros | Confirmado |
| P22 | Ao retomar um projeto suspenso, os membros que entretanto entraram noutro projeto em aberto continuam na lista de membros, sinalizados com uma etiqueta vermelha, e o gestor vê um aviso; as suas tarefas mantêm-se atribuídas. A etiqueta desaparece quando o membro passa a estar em um só projeto em aberto. É a única exceção à regra R11 | Evita perder a equipa original e obriga o gestor a decidir a realocação | Confirmado |
| P23 | A data de início e a de fim do projeto são obrigatórias e o instante de fim é posterior ao de início. Se hoje é anterior à data de início, o tempo decorrido é 0% | O P5 divide por (fim − início) e precisa de datas válidas | Confirmado |
| P24 | Ao remover um membro com tarefas ou subtarefas por concluir, mesmo partilhadas com outros responsáveis (R17), o gestor passa cada uma dessas tarefas para um substituto. Um substituto é um membro já no projeto ou um novo membro, que tem de ser livre (R11) e é adicionado nessa altura; as tarefas podem ser repartidas por vários substitutos, e nenhum fica com mais de 5 tarefas e subtarefas em aberto no total (as subtarefas contam). O substituto de uma tarefa não pode já ser responsável por ela. O limite de 5 vale só nesta substituição. "Por concluir" são as tarefas que não estão em estado final | Completa a regra de remoção decidida pela autora | Confirmado |
| P25 | O filtro "as minhas" mostra a tarefa principal se o membro for responsável por ela ou por alguma das suas subtarefas | Interpretação de "o filtro considera as subtarefas", dado que as subtarefas não aparecem sozinhas (R18) | Confirmado |
| P26 | O fuso horário é definido ao criar o projeto (por defeito, o de Lisboa) e aplica-se a todas as datas e horas do projeto, incluindo os prazos das tarefas. A data de fim pode ter hora; sem hora, o dia de fim conta inteiro (até ao fim desse dia). A data de início sem hora conta a partir das 00:00. O tempo decorrido mede-se entre o instante de início e o de fim | O fuso e a hora de fim são decisão da autora; o dia inteiro e as 00:00 são interpretações minhas, aceites pela autora | Confirmado |
| P27 | O painel inicial conta só as tarefas principais (R18). O painel inicial, tanto do gestor como do membro, não mostra os projetos concluídos (decisão sobre a vista global); encontram-se em Projetos e na pesquisa (F22), onde cada um vê os projetos a que tem acesso (R8) | Concilia o painel inicial novo com a vista global do gestor (F07) | Confirmado |
| P28 | Os comentários têm no máximo 2000 caracteres e são listados do mais antigo para o mais recente. O autor edita e apaga os seus comentários; apagar elimina o comentário de vez, sem reciclagem, e a edição não guarda histórico. Aplicam-se às tarefas e subtarefas, em projetos em aberto e suspensos; num projeto concluído não se adiciona, edita nem apaga. Os comentários seguem a tarefa (apagar ou recuperar a tarefa faz o mesmo aos comentários) | O novo âmbito pede só adicionar e listar; o resto foi decidido pela autora | Confirmado |
| P29 | A pesquisa de projetos procura o texto em qualquer parte do nome, sem distinguir maiúsculas de minúsculas. Os filtros por estado e por quadrante aplicam-se na Lista, sobre as tarefas principais, e podem combinar-se com "as minhas" | O novo âmbito diz "pesquisar pelo nome" e "filtrar por estado e prioridade" sem mais detalhe | Confirmado |
| P30 | Na v1 não há limite de tentativas de início de sessão nem bloqueio de contas | A documentação não define este limite e as contas são fictícias, num ambiente controlado (C8, S5). Seria preciso defini-lo antes de usar dados reais | Por validar |
| P31 | A proteção CSRF está desligada na v1: a API só aceita JSON e o cookie de sessão é `SameSite=Lax`. Reve-se quando se escolher a interface (T1) | Simplifica a API enquanto não há interface; a decisão fica registada para não se perder | Por validar |
| P32 | As sessões ficam guardadas em memória no servidor: reiniciar a aplicação termina-as e o utilizador tem de iniciar sessão outra vez | Suficiente para uma só instância na v1 (T8); evita uma tabela de sessões | Por validar |
| P33 | Um pedido sem sessão válida recebe 401. Se trazia o cookie de uma sessão que já terminou (por terminar sessão ou por 3 horas sem atividade), a mensagem diz que a sessão terminou ("Your session has ended. Sign in again to continue."); sem cookie, diz "Sign in to continue.". A interface leva o utilizador ao início de sessão e mostra a mensagem (cenários 10.5 e 10.6, EC31, EC38) | Cumpre o pedido de uma mensagem compreensível quando a sessão deixa de ser válida, sem acrescentar estados novos | Por validar |
| P34 | Um pedido de um projeto que não existe (ou foi apagado) recebe 404, e o de um projeto que existe mas a que o utilizador não tem acesso recebe 403 (S1). Por isso quem tem sessão pode descobrir que um identificador de projeto existe, sem ver o seu conteúdo | Mantém-se a distinção do contrato para dar mensagens claras (NF08); sem dados reais (C8), o risco é aceitável. Decisão da autora em 2026-11-13 | Confirmado |
| P35 | Não se cria uma subtarefa numa tarefa-mãe fechada (concluída, cancelada ou duplicada), nem se reabre uma subtarefa fechada enquanto a mãe está fechada: é recusado, com uma mensagem a pedir para reabrir primeiro a mãe (cenários 4.6 e 4.7). Não se pode deixar uma mãe fechada com uma subtarefa em aberto, porque contradiria a R4 | A documentação não dizia o que fazer; decisão da autora em 2026-11-13, ao aprovar o plano do incremento 3 | Confirmado |

## 3. Requisitos de segurança e privacidade

### Definidos
| ID | Requisito | Origem | Versão |
|---|---|---|---|
| S1 | Um membro só acede aos projetos de que é membro e às suas tarefas, também por ligação direta; o gestor acede só aos projetos de que é gestor | R8, F06 | v1 |
| S2 | Só o gestor apaga e recupera projetos e reabre tarefas finais; os membros recuperam tarefas e subtarefas dos projetos a que têm acesso | R9, P8 | v1 |
| S3 | Os dados apagados ficam numa reciclagem recuperável | P12, F09 | v1 |
| S4 | **Substituído por S5.** Antes, o utilizador escolhia quem era num seletor, sem palavra-passe | Decisão, P15 | Substituído |
| S5 | Todos os utilizadores iniciam sessão para aceder à aplicação, com contas de demonstração criadas por seed. A sessão termina ao terminar sessão ou após 3 horas sem atividade. As palavras-passe têm pelo menos 8 caracteres e guardam-se só de forma protegida, nunca em texto simples. A mensagem de erro não diz se falhou o utilizador ou a palavra-passe. Não há registo público nem recuperação de palavra-passe (extensões opcionais) | P15, F10 | v1 |
| S6 | As contas passam a ser criadas pelo gestor de projeto; não há registo livre | Decisão, F23 | v2 |
| S7 | As ações relevantes ficam registadas (quem, o quê, quando) | P14, F14 | v2 |

### Riscos aceites de forma consciente na v1
- As contas de demonstração e as regras de palavra-passe não estão definidas para segurança real (S5, C8).
- Sem registo de auditoria na v1, não fica registado quem apagou o quê.
- Sem limite de tentativas de início de sessão (P30) e sem proteção CSRF (P31): aceitável só com dados fictícios num ambiente controlado.
- Quem tem sessão consegue saber se um identificador de projeto existe, pela diferença entre 404 e 403 (P34).

Mitigação decidida: só dados fictícios num ambiente controlado (C8). Se a aplicação passar a usar dados reais, é preciso definir primeiro as regras de autenticação e de proteção de dados.

### Excluídos ou adiados
- **RGPD e segurança avançada:** fora de âmbito por decisão da autora. É um risco se a aplicação for usada com dados reais.
- **Regras de palavra-passe, recuperação de acesso e registo público:** não se estipulam para já (extensões opcionais).
- **Conteúdo do registo de auditoria** (além de P14): fica para a user story da v2.

## 4. Decisões técnicas pendentes

O estilo de arquitetura (monólito modular) e a stack (Java 21, Spring Boot e PostgreSQL, com a interface depois da API) estão decididos em [architecture.md](architecture.md#8-stack-e-decisões-técnicas). A coluna indica o que já está decidido; o resto tem de ser decidido no incremento 0 do [plano de implementação](implementation-plan.md).

| ID | Decisão pendente | Condiciona |
|---|---|---|
| T1 | Forma de execução da aplicação (por exemplo, acesso por navegador ou aplicação instalada). **Decidido em parte:** API REST primeiro; a interface decide-se depois | NF02 |
| T2 | Onde e como se guardam os dados, e se há um local central partilhado pelas equipas. **Decidido:** PostgreSQL, uma base central | F10, F06, F09, F13 |
| T3 | Mecanismo de autenticação e de controlo de acesso por papel e por projeto. **Decidido:** sessão no servidor com cookie, e controlo de acesso feito no servidor. **Implementado no incremento 1** com Spring Security: cookie `SESSION` (`HttpOnly`, `SameSite=Lax`), 3 horas sem atividade, palavras-passe com hash BCrypt, permissões por projeto no módulo Acesso (ver P30 a P33) | S1, S2, S5 e S6 |
| T4 | Como se disparam os lembretes e alertas de atraso dentro da aplicação | F12, P11 |
| T5 | Como se implementam as datas e horas (fuso horário do projeto, P26, e cálculo do tempo decorrido) | P5, cenários 6.3 a 6.6, 6.14 e 6.15 |
| T6 | Como se guarda o registo de auditoria | F14, S7 |
| T7 | Tecnologias de interface, lógica e armazenamento. **Decidido:** Java 21, Spring Boot e PostgreSQL para a lógica e o armazenamento; a interface por decidir | Todas |
| T8 | Ambiente onde a aplicação corre e como se publica | T1 |
| T9 | Estratégia de testes, incluindo como automatizar os cenários Given / When / Then | [user-stories.md](user-stories.md) |
| T10 | Navegadores ou sistemas suportados, e requisitos de acessibilidade | NF02 |
| T11 | Cópias de segurança e recuperação de dados | T2 |
| T12 | Como se criam as contas de demonstração por seed e como se guardam as credenciais (decisão funcional já tomada: S5, P21). **Decidido no incremento 0:** um componente que corre ao arrancar, só se ainda não houver utilizadores, numa só transação; a palavra-passe vem da variável `DEMO_PASSWORD` do `.env` (que não se versiona), com pelo menos 8 caracteres, e guarda-se como hash BCrypt | F10, F06, S1, S2 |
| T13 | Como se tratam as edições simultâneas da mesma tarefa por vários utilizadores. **Decidido em 2026-11-12:** quem grava depois de outra pessoa ter alterado a tarefa vê a gravação recusada, com uma mensagem a pedir para recarregar e tentar outra vez (R30, EC43); a tarefa tem uma versão que o contrato e o modelo de dados passam a ter (M13). Implementa-se no incremento 3 | F02, F08, R30 |

### Questões funcionais em aberto
- Valores permitidos para a antecedência do lembrete e para o intervalo de repetição do alerta, e até quando se repete (F12).

**Fechadas em 2026-11-12:**
- As tarefas não ficam ligadas a uma fase: pertencem só ao projeto, que é quem tem fase (P3).
- Integrações: só as que a v1 já define; as restantes ficam na v3 (F19).
- O nível 2 chama-se "medium" e conta como "baixo" na Matriz (1 e 2 baixo, 3 e 4 alto): nomenclatura confirmada.
- Antecedência e repetição dos lembretes (F12) e conteúdo da auditoria (P14) continuam por definir, na v2.

## 5. Limitações de âmbito

- Sem RGPD nem segurança avançada.
- Sem requisitos de desempenho, disponibilidade, compatibilidade, acessibilidade ou cópias de segurança.
- Sem indicadores de sucesso, orçamento ou prazo definidos por um cliente (C1, C6).
- Sem utilizadores externos à empresa e sem papel transversal (por exemplo, direção ou administração): só gestor de projeto e membro (C4).
- Sem tarefas confidenciais: quem tem acesso ao projeto vê todas as suas tarefas.
- Sem dados reais (C8): só dados fictícios de demonstração.
- Desempenho esperado e navegadores suportados por definir (T10); não há requisitos de disponibilidade nem de cópias de segurança (T11).
- Sem estado "adiantado" do projeto.
- Sem funcionalidades de IA na aplicação.
- Fora do MVP, na v2: criação de contas pelo gestor, etiquetas, lembretes e alertas de atraso, arquivo de projetos e registo de auditoria.
- Fora do MVP, na v3: Calendário, anexos, avisos por email ou SMS, importação e exportação, registo de tempo, tarefas repetidas, modelos de projeto e quadro por estados (ver [requirements.md](requirements.md#7-funcionalidades-fora-do-âmbito-inicial)).
- Os avisos só funcionam com a aplicação aberta (P11).
