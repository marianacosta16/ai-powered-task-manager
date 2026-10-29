# Matriz — Requisitos

> Documento de referência dos requisitos da Matriz. Não propõe tecnologias nem contém código.
> Documentos relacionados: [user-stories.md](user-stories.md) e [constraints.md](constraints.md).

## 1. Visão e objetivos

### Contexto
Uma empresa quer uma aplicação chamada **Matriz**, para gestão de projetos e tarefas de equipas de trabalho (do género Jira, Azure DevOps ou Trello).

O pedido inicial do cliente é:

> "Precisamos de uma aplicação onde as nossas equipas possam organizar projetos, gerir tarefas e acompanhar o progresso do trabalho."

O cliente não definiu funcionalidades, regras de negócio, requisitos técnicos nem prioridades. Como se trata de um exercício de aprendizagem, o cliente não está disponível: **as respostas às perguntas de clarificação foram dadas pela autora do projeto em nome do cliente** (ver anexo A).

### Visão
Uma aplicação em que as equipas organizam projetos, gerem tarefas e acompanham o progresso, com uma **matriz urgente/importante** como vista principal para decidir o que fazer primeiro.

### Objetivos
1. **Organizar projetos:** criar projetos com fases e estados e formar a equipa de cada um.
2. **Gerir tarefas:** criar, atribuir e priorizar tarefas (com subtarefas) segundo urgência e importância.
3. **Acompanhar o progresso:** permitir ao gestor de projeto ver, por projeto e no conjunto, se o trabalho está dentro do prazo.

### Convenções dos documentos
| Etiqueta | Significado |
|---|---|
| **Facto** | Vem do enunciado |
| **Decisão** | Definição tomada pela autora do projeto em nome do cliente |
| **Pressuposto** | Proposta aceite para preencher uma lacuna; deve ser validada com o cliente real (ver [constraints.md](constraints.md#2-pressupostos-assumidos)) |
| **Fora de âmbito** | Excluído conscientemente (ver [constraints.md](constraints.md#5-limitações-de-âmbito)) |

Os códigos F (funcionalidade), R (regra), NF (requisito não funcional) e P (pressuposto) são usados em todos os documentos. Os códigos US e EC identificam user stories e casos excecionais em [user-stories.md](user-stories.md).

### Versões
A entrega segue a prioridade MoSCoW: **v1 (MVP) = Must**, **v2 = Should**, **v3 = Could**.

## 2. Tipos de utilizadores

Existem apenas dois papéis (decisão).

| Papel | Descrição | Pode |
|---|---|---|
| **Membro da equipa** | Executa o trabalho | Ver os projetos de que é membro e as suas tarefas; criar, editar e apagar tarefas; mudar estados (ver P8) |
| **Gestor de projeto** | Planeia e acompanha | Tudo o que o membro pode, mais: criar projetos e adicionar-lhes membros (formar a equipa); definir a fase e o estado do projeto; ver todos os projetos e o progresso global; apagar e recuperar projetos. Na v2: gerir contas e arquivar projetos |

O gestor de projeto também forma as equipas; não existe um papel separado de "gestor de equipas" (P2).

## 3. Requisitos funcionais

### 3.1 Informação gerida

**Projeto:** nome, descrição, datas de início e de fim, fase, estado e membros.
- Fases: iniciação, planeamento, execução, monitorização, avaliação.
- Estados: não iniciado, em andamento, atenção, atrasado, suspenso, concluído. "Atenção" e "atrasado" são calculados (P5); os restantes são definidos pelo gestor.

**Equipa:** são os membros que o gestor adiciona ao projeto. Um membro só pode estar num projeto em aberto de cada vez (R11).

**Tarefa:** título, descrição, estado, prazo (obrigatório), urgência, importância, subtarefas e um ou mais responsáveis. Na v2 passa a ter também etiquetas (tipo de trabalho e tempo de execução) e um lembrete.
- Estados: por fazer, em curso, em revisão, bloqueado, concluído, cancelado, duplicado. Os três últimos são finais.
- Urgência e importância têm 4 níveis cada: 1 = low, 2 = medium, 3 = high, 4 = critical.

**Subtarefa:** tem estado, prazo e prioridades (urgência e importância) próprios.

### 3.2 Vistas
- **Matriz** (vista principal, por projeto): 4 quadrantes. Cada eixo é dividido em duas metades: níveis 1 e 2 são "baixo" e níveis 3 e 4 são "alto". Quadrantes: *Do first* (urgente e importante), *Schedule* (importante, não urgente), *Delegate* (urgente, não importante) e *Later* (nem urgente nem importante). Esconde as tarefas em estado final (P10).
- **Lista** (por projeto): todas as tarefas do projeto, incluindo as de estado final; as tarefas em aberto aparecem antes das fechadas; tem o filtro "as minhas" (R15).
- **Vista global do gestor:** projetos em aberto e suspensos, com barra de progresso e estado; não mostra os concluídos.

### 3.3 Funcionalidades
Prioridade segundo MoSCoW: **Must** (essencial), **Should** (importante), **Could** (desejável).

| ID | Funcionalidade | Justificação | Prioridade | Versão |
|---|---|---|---|---|
| F01 | Gestão de projetos (nome, descrição, datas, fase, estado) e da sua equipa: o gestor cria o projeto e adiciona membros | Responde a "organizar projetos"; adicionar membros forma a equipa | Must | v1 |
| F02 | Gestão de tarefas (título, descrição, prazo, estado, vários responsáveis) | Responde a "gerir tarefas" | Must | v1 |
| F03 | Urgência e importância em 4 níveis | Sem elas a Matriz não existe | Must | v1 |
| F04 | Vista Matriz por projeto | Vista principal da aplicação | Must | v1 |
| F05 | Vista Lista por projeto | Dá uma visão completa e filtrável das tarefas | Must | v1 |
| F06 | Visibilidade por papel e por projeto | Protege o que cada utilizador deve ver | Must | v1 |
| F07 | Vista global do gestor com barra de progresso e estado calculado por projeto | Responde a "acompanhar o progresso" | Must | v1 |
| F08 | Subtarefas com estado e prioridades próprios, e regras de prazo e fecho | Permite dividir trabalho grande | Must | v1 |
| F09 | Reciclagem (recuperar projetos e tarefas apagados) | Evita perdas acidentais | Must | v1 |
| F10 | Login e contas criadas pelo gestor de projeto | Identifica os utilizadores e protege o acesso | Should | v2 |
| F11 | Etiquetas por tipo de trabalho e tempo de execução | Ajuda a organizar e filtrar | Should | v2 |
| F12 | Lembretes e alertas de atraso, configuráveis | Evita falhar prazos | Should | v2 |
| F13 | Arquivo de projetos fechados, com histórico | Mantém o histórico sem poluir os projetos ativos | Should | v2 |
| F14 | Registo de auditoria | Rastreia ações no trabalho partilhado | Should | v2 |
| F15 | Vista Calendário | Mostra prazos no tempo | Could | v3 |
| F16 | Anexos nas tarefas | Útil, mas aumenta o âmbito | Could | v3 |
| F17 | Comentários nas tarefas | Colaboração entre membros | Could | v3 |
| F18 | Avisos por email ou SMS | Avisar com a aplicação fechada | Could | v3 |
| F19 | Importação e exportação de dados | Interesse apontado como "talvez" | Could | v3 |
| F20 | Registo de tempo; tarefas repetidas; modelos de projeto | Funcionalidades comuns em ferramentas semelhantes, pedidas como "para já não" | Could | v3 |
| F21 | Quadro por estados | Descartado na primeira fase | Could | v3 |

## 4. Regras de negócio

| ID | Regra | Versão |
|---|---|---|
| R1 | O prazo de cada tarefa e subtarefa é obrigatório | v1 |
| R2 | O prazo de uma subtarefa não pode ser posterior ao da tarefa-mãe | v1 |
| R3 | Antecipar o prazo da mãe para antes do de uma subtarefa é bloqueado, com aviso para rever primeiro as subtarefas | v1 |
| R4 | Uma tarefa-mãe não pode ser concluída nem cancelada com subtarefas em aberto | v1 |
| R5 | Uma subtarefa cancelada ou duplicada conta como fechada | v1 |
| R6 | Uma tarefa bloqueada exige motivo; uma tarefa duplicada exige indicar de qual | v1 |
| R7 | Cada tarefa tem um lembrete, com antecedência definida por tarefa; o alerta de atraso repete-se segundo um intervalo definido pelo utilizador | v2 |
| R8 | Um membro só vê os projetos de que é membro, incluindo os concluídos e suspensos; o gestor vê tudo | v1 |
| R9 | Só o gestor apaga projetos e recupera apagados; qualquer membro da equipa apaga tarefas | v1 |
| R10 | Só se arquiva um projeto concluído (um projeto suspenso não se arquiva); o arquivo guarda tarefas e histórico | v2 |
| R11 | Um membro pertence a um projeto em aberto de cada vez; adicionar a um projeto um membro que já está noutro projeto em aberto é recusado, com um aviso. Projetos concluídos e suspensos não são projetos em aberto (P4, P20). Exceção: ao retomar um projeto suspenso, os membros que entretanto entraram noutro projeto em aberto ficam sinalizados e o gestor vê um aviso (P22) | v1 |
| R12 | Só o gestor altera a fase do projeto e define manualmente o seu estado; os estados "atenção" e "atrasado" são calculados (P5) | v1 |
| R13 | Não é aceite uma tarefa com prazo no passado na criação | v1 |
| R14 | O título tem no máximo 100 caracteres e a descrição 2000 (tarefas e projetos, P17) | v1 |
| R15 | A Lista mostra todas as tarefas do projeto, incluindo as de estado final; as tarefas em aberto aparecem antes das fechadas; tem o filtro "as minhas" | v1 |

## 5. Requisitos não funcionais

Só estão aqui os requisitos que foram definidos ou que decorrem de decisões já tomadas.

| ID | Requisito | Origem | Versão |
|---|---|---|---|
| NF01 | A interface está em inglês | Decisão | v1 |
| NF02 | A aplicação é usada em computador | Decisão | v1 |
| NF03 | Dimensão de referência: cerca de 30 utilizadores, 3 a 5 projetos em aberto em simultâneo, cada um com uma equipa de 5 a 8 pessoas | Pressuposto (P18) | v1 |
| NF04 | Controlo de acesso por papel e por projeto; na v1 o utilizador escolhe quem é num seletor (S7) e a autenticação chega com o login (F10) | Pressuposto (P15), regra R8 | v1 (acesso), v2 (autenticação) |
| NF05 | Rastreabilidade: ficam registados quem fez o quê e quando (criar, editar, mudar de estado, apagar) | Pressuposto (P14) | v2 |
| NF06 | Recuperabilidade: o que é apagado vai para uma reciclagem e pode ser recuperado pelo gestor | Decisão, regra R9 | v1 |
| NF07 | O cálculo da barra de progresso e dos estados "atenção" e "atrasado" segue exatamente as fórmulas definidas | Pressuposto (P5, P7) | v1 |

**Sem requisito definido (fora de âmbito):** desempenho, disponibilidade, compatibilidade com navegadores, acessibilidade, cópias de segurança. Ver [constraints.md](constraints.md#5-limitações-de-âmbito).

## 6. Âmbito do MVP (v1)

**Dentro do MVP:** F01 a F09, com Matriz e Lista como vistas.

**Critério de aceitação do MVP:** um gestor consegue criar projetos, formar a equipa, atribuir tarefas, e acompanhar o progresso de cada projeto e do conjunto; um membro consegue gerir as tarefas do seu projeto na Matriz e na Lista.

**Riscos**
- Na v1 não há login: o utilizador escolhe quem é num seletor (S7), por isso a visibilidade por papel não impede que alguém escolha o papel de gestor.
- Sem arquivo, auditoria nem lembretes na v1, não há registo de ações nem avisos de prazo.
- Não há requisitos de segurança avançada nem de proteção de dados, o que é um risco se a aplicação for usada fora do exercício.

## 7. Funcionalidades fora do âmbito inicial

**v2 (Should):** F10 a F14.

| ID | Funcionalidade | Estado |
|---|---|---|
| F10 | Login e contas criadas pelo gestor de projeto | v2 |
| F11 | Etiquetas por tipo de trabalho e tempo de execução | v2 |
| F12 | Lembretes e alertas de atraso | v2 |
| F13 | Arquivo de projetos fechados | v2 |
| F14 | Registo de auditoria | v2 |

**v3 (Could):** F15 a F21.

| ID | Funcionalidade | Estado |
|---|---|---|
| F15 | Vista Calendário | v3 |
| F16 | Anexos nas tarefas | v3 |
| F17 | Comentários nas tarefas | v3 |
| F18 | Avisos por email ou SMS | v3 |
| F19 | Importação e exportação de dados | Não definido ("talvez") |
| F20 | Registo de tempo, tarefas repetidas, modelos de projeto | "Para já não" |
| F21 | Quadro por estados | Descartado na primeira fase |

Excluído de forma consciente, sem funcionalidade associada: estado "adiantado" do projeto (P6) e utilizadores externos à empresa (P1).

## Anexo A. Perguntas de clarificação e respostas

| # | Tema | Pergunta | Resposta | Tipo |
|---|---|---|---|---|
| 1 | Contexto | Que problema tem hoje a empresa? | Não tem aplicação; precisa de uma | Pressuposto (P19) |
| 2 | Escala | Quantos utilizadores, equipas e projetos? | Pequena empresa, cerca de 30 utilizadores, 3 a 5 projetos em aberto em simultâneo, cada um com uma equipa de 5 a 8 pessoas | Pressuposto (P18) |
| 3 | Papéis | Que tipos de utilizador existem? | Membro da equipa e gestor de projeto | Decisão |
| 4 | Contas | Quem cria e gere as contas? | O gestor de projeto (com o login, na v2) | Decisão |
| 5 | Equipas e projetos | Como se relacionam? | O gestor cria o projeto e adiciona membros, e isso forma a equipa; o gestor gere vários projetos; um membro pertence a um projeto em aberto de cada vez | Decisão |
| 6 | Visibilidade | Quem vê o quê? | O membro só vê o que a sua equipa faz; o gestor vê tudo | Decisão |
| 7 | Projeto | Que informação tem? | Nome, descrição, datas, fase e estado | Decisão |
| 8 | Fases do projeto | Tem fases? | Iniciação, planeamento, execução, monitorização, avaliação | Decisão |
| 9 | Estados do projeto | Que estados tem? | Não iniciado, em andamento, atenção, atrasado, suspenso, concluído | Decisão |
| 10 | Tarefa | Que informação tem? | Título, descrição, estado, prazo obrigatório, urgência, importância, etiquetas, subtarefas, responsáveis | Decisão |
| 11 | Estados da tarefa | Que estados tem? | Por fazer, em curso, em revisão, bloqueado, concluído, cancelado, duplicado | Decisão |
| 12 | Responsáveis | Quantos por tarefa? | Mais do que um | Decisão |
| 13 | Prioridade | Como se define? | Urgência e importância, cada uma em 4 níveis | Decisão |
| 14 | Etiquetas | Que tipos? | Tipo de trabalho e tempo de execução | Decisão |
| 15 | Anexos | São necessários? | Sim, mas ficam para uma fase posterior | Decisão |
| 16 | Progresso | O que quer ver o gestor? | Se cada projeto, ou o conjunto, está dentro do prazo ou atrasado | Decisão |
| 17 | Medição | Como se mede o progresso? | Barra de progresso: percentagem de tarefas concluídas e data | Decisão |
| 18 | Vistas | Que vistas existem? | Matriz e Lista na primeira fase; Calendário depois | Decisão |
| 19 | Avisos | Como se avisa? | Lembretes e alertas de atraso configurados pelo utilizador, dentro da aplicação (v2) | Decisão |
| 20 | Apagar | Quem apaga e recupera? | Projetos: só o gestor. Tarefas: qualquer membro da equipa. Há reciclagem, e só o gestor recupera | Decisão |
| 21 | Arquivo | O que é arquivar? | Só se arquiva um projeto fechado, com todas as tarefas e o histórico (v2) | Decisão |
| 22 | Auditoria | É preciso registo de ações? | Sim (v2) | Decisão |
| 23 | Saída de utilizador | O que acontece? | Remove-se o utilizador e coloca-se outro no seu lugar | Decisão |
| 24 | Comentários | São necessários? | Ficam para mais tarde | Decisão |
| 25 | Registo de tempo e repetição | São necessários? | Não | Decisão |
| 26 | Plataforma e língua | Onde e em que língua? | Computador; interface em inglês | Decisão |
| 27 | Segurança e proteção de dados | Que exigências? | Sem RGPD nem segurança avançada | Fora de âmbito |
| 28 | Integrações | Importar, exportar, ligar a outros sistemas? | Talvez; não definido | Fora do MVP |
| 29 | Modelos de projeto | Copiar projeto como modelo? | Não, por agora | Fora de âmbito |
| 30 | Prazo no passado | Aceita-se na criação? | Não | Decisão |
| 31 | Tamanhos | Limite de título e descrição? | 100 e 2000 caracteres | Decisão |
| 32 | Nomes dos quadrantes | Como se chamam? | Do first, Schedule, Delegate, Later | Decisão |
| 33 | Estado "atenção" e "atrasado" | Como se calculam? | Por desvio entre tempo decorrido e progresso: 15 e 25 pontos percentuais; "atrasado" também com a data de fim ultrapassada | Decisão |
| 34 | Ordem de entrega | Como se organizam as versões? | v1 = Must, v2 = Should, v3 = Could | Decisão |
| 35 | Login | Entra no MVP? | Não: passa para a v2 | Decisão |
| 36 | Equipa | Há uma entidade equipa? | Não: o gestor cria o projeto e adiciona membros, e isso forma a equipa | Decisão |
| 37 | Membro já noutro projeto | Pode ser adicionado a um segundo projeto em aberto? | Não: a ação é recusada com um aviso | Decisão |
| 38 | Lista | Que tarefas mostra e por que ordem? | Todas, incluindo as de estado final, com o filtro "as minhas"; em aberto primeiro, depois fechadas | Decisão |
| 39 | Identificação na v1 | Como sabe a aplicação quem é o utilizador, sem login? | O utilizador escolhe quem é num seletor, sem palavra-passe | Decisão |
| 40 | Projeto suspenso | Conta como projeto em aberto? | Não: conta como fechado, porque não se vai trabalhar nele durante algum tempo; os membros são realocados a outro projeto | Decisão |
| 41 | Retoma de projeto suspenso | O que acontece à equipa? | A lista de membros mantém-se; os membros que entretanto entraram noutro projeto ficam com uma etiqueta vermelha e o gestor vê um aviso. Um membro continua a estar em um só projeto em aberto de cada vez | Decisão |
| 42 | Visibilidade e arquivo | Os membros libertados continuam a ver o projeto? A vista global mostra os suspensos? Arquiva-se um suspenso? | Sim; sim; não | Decisão |
| 43 | Vista global e projetos concluídos | A vista global mostra os projetos concluídos? | Não | Decisão |
