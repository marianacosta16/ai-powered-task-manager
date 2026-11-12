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
| **Decisão** | Definição tomada pela autora do projeto em nome do cliente |
| **Pressuposto** | Proposta aceite para preencher uma lacuna; deve ser validada com o cliente real (ver [constraints.md](constraints.md#2-pressupostos-assumidos)) |
| **Fora de âmbito** | Excluído conscientemente (ver [constraints.md](constraints.md#5-limitações-de-âmbito)) |
| **Fora do MVP** | Funcionalidade prevista para a v2 ou a v3, não para a v1 |
| **Premissa** | Condição do contexto do exercício que não se decide nem se valida (por exemplo, o cliente ser fictício) |

Os códigos F (funcionalidade), R (regra), NF (requisito não funcional), C (restrição), P (pressuposto), S (segurança) e T (decisão técnica pendente) são usados em todos os documentos. Os códigos US e EC identificam user stories e casos excecionais em [user-stories.md](user-stories.md).

### Versões
A entrega segue a prioridade MoSCoW: **v1 (MVP) = Must**, **v2 = Should**, **v3 = Could**.

## 2. Tipos de utilizadores

Existem apenas dois papéis (decisão).

| Papel | Descrição | Pode |
|---|---|---|
| **Membro da equipa** | Executa o trabalho | Ver os projetos de que é membro e as suas tarefas; criar, editar e apagar tarefas; mudar estados (ver P8) |
| **Gestor de projeto** | Planeia e acompanha | Tudo o que o membro pode, mais: criar projetos e adicionar-lhes ou remover-lhes membros (formar a equipa); definir a fase e o estado do projeto; ver os projetos de que é gestor e o seu progresso global; apagar e recuperar projetos. Na v2: gerir contas e arquivar projetos |

O gestor de projeto também forma as equipas; não existe um papel separado de "gestor de equipas" (P2).

## 3. Requisitos funcionais

### 3.1 Informação gerida

**Utilizador:** nome e papel (membro da equipa ou gestor de projeto). O gestor também pode ter tarefas atribuídas. Os utilizadores existem previamente, como contas de demonstração criadas por seed (P21), e iniciam e terminam sessão (F10). A criação de contas pelo gestor chega na v2 (F23).

**Projeto:** nome, descrição, datas de início e de fim (obrigatórias, R16; a de fim pode ter hora), fuso horário (definido ao criar o projeto; por defeito, o de Lisboa, P26), fase, estado, gestor (um só, indicado por quem cria o projeto, R19) e membros.
- Fases: iniciação, planeamento, execução, monitorização, avaliação.
- Estados: não iniciado, em andamento, atenção, atrasado, suspenso, concluído. "Atenção" e "atrasado" são só calculados (P5) e o gestor não os define; os restantes são definidos pelo gestor.

**Equipa:** são os membros que o gestor adiciona ao projeto. Um membro só pode estar num projeto em aberto de cada vez (R11).

**Tarefa:** título, descrição, estado, prazo (obrigatório), urgência, importância, subtarefas e um ou mais responsáveis. Na v2 passa a ter também etiquetas (tipo de trabalho e tempo de execução) e um lembrete.
- Estados: por fazer, em curso, em revisão, bloqueado, concluído, cancelado, duplicado. Os três últimos são finais.
- Urgência e importância têm 4 níveis cada: 1 = low, 2 = medium, 3 = high, 4 = critical.

**Subtarefa:** tem estado, prazo, prioridades (urgência e importância) e responsáveis próprios. Aparece dentro da tarefa-mãe e não conta para o progresso do projeto (R18).

**Comentário:** texto escrito por um utilizador numa tarefa, com o autor e a data de criação (F17, R24).

**Termos usados**
- *Tarefa em estado final (ou fechada):* concluída, cancelada ou duplicada. Uma tarefa *em aberto* é a que não está em estado final.
- *Projeto em aberto:* não está concluído, suspenso nem arquivado (P4).
- *Projeto concluído:* é o único que se pode arquivar (R10).

### 3.2 Vistas
- **Matriz** (vista principal, por projeto): 4 quadrantes. Cada eixo é dividido em duas metades: níveis 1 e 2 são "baixo" e níveis 3 e 4 são "alto". Quadrantes: *Do first* (urgente e importante), *Schedule* (importante, não urgente), *Delegate* (urgente, não importante) e *Later* (nem urgente nem importante). Esconde as tarefas em estado final (P10) e mostra só tarefas principais (R18).
- **Lista** (por projeto): todas as tarefas do projeto, incluindo as de estado final; as tarefas em aberto aparecem antes das fechadas; tem o filtro "as minhas" e os filtros por estado e por quadrante (F22, R25), e mostra só tarefas principais (R15, R18).
- **Painel inicial** (ecrã de entrada, para todos os utilizadores, F07): os projetos acessíveis ao utilizador, cada um com a contagem das suas tarefas principais por estado, e navegação para o detalhe do projeto. O painel do gestor mostra os seus projetos em aberto e suspensos, com barra de progresso e estado calculado, e não mostra os concluídos (P27); o do membro mostra todos os seus projetos (R8).

### 3.3 Funcionalidades
Prioridade segundo MoSCoW: **Must** (essencial), **Should** (importante), **Could** (desejável).

| ID | Funcionalidade | Justificação | Prioridade | Versão |
|---|---|---|---|---|
| F01 | Gestão de projetos (nome, descrição, datas, fase, estado) e da sua equipa: o gestor cria o projeto e adiciona ou remove membros | Responde a "organizar projetos"; adicionar membros forma a equipa | Must | v1 |
| F02 | Gestão de tarefas (título, descrição, prazo, estado, vários responsáveis) | Responde a "gerir tarefas" | Must | v1 |
| F03 | Urgência e importância em 4 níveis | Sem elas a Matriz não existe | Must | v1 |
| F04 | Vista Matriz por projeto | Vista principal da aplicação | Must | v1 |
| F05 | Vista Lista por projeto | Dá uma visão completa e filtrável das tarefas | Must | v1 |
| F06 | Visibilidade por papel e por projeto | Protege o que cada utilizador deve ver | Must | v1 |
| F07 | Painel inicial: projetos acessíveis com contagens de tarefas por estado e navegação para o detalhe; para o gestor, também a barra de progresso e o estado calculado por projeto | Responde a "acompanhar o progresso" e dá o ponto de entrada da aplicação | Must | v1 |
| F08 | Subtarefas com estado e prioridades próprios, e regras de prazo e fecho | Permite dividir trabalho grande | Must | v1 |
| F09 | Reciclagem (recuperar projetos e tarefas apagados) | Evita perdas acidentais | Must | v1 |
| F10 | Autenticação: iniciar e terminar sessão e consultar o utilizador autenticado, com contas de demonstração criadas por seed | Identifica os utilizadores e protege o acesso; substitui o seletor de utilizador previsto antes | Must | v1 |
| F11 | Etiquetas por tipo de trabalho e tempo de execução | Ajuda a organizar e filtrar | Should | v2 |
| F12 | Lembretes e alertas de atraso, configuráveis | Evita falhar prazos | Should | v2 |
| F13 | Arquivo de projetos concluídos, com histórico | Mantém o histórico sem poluir os projetos ativos | Should | v2 |
| F14 | Registo de auditoria | Rastreia ações no trabalho partilhado | Should | v2 |
| F15 | Vista Calendário | Mostra prazos no tempo | Could | v3 |
| F16 | Anexos nas tarefas | Útil, mas aumenta o âmbito | Could | v3 |
| F17 | Comentários nas tarefas: adicionar e listar, com autor e data | Colaboração entre membros | Must | v1 |
| F18 | Avisos por email ou SMS | Avisar com a aplicação fechada | Could | v3 |
| F19 | Importação e exportação de dados | Interesse apontado como "talvez" | Could | v3 |
| F20 | Registo de tempo; tarefas repetidas; modelos de projeto | Funcionalidades comuns em ferramentas semelhantes, pedidas como "para já não" | Could | v3 |
| F21 | Quadro por estados | Descartado na primeira fase | Could | v3 |
| F22 | Pesquisa de projetos pelo nome e filtros de tarefas por estado e por quadrante (prioridade) | Pedida no novo âmbito funcional; ajuda a encontrar projetos e tarefas | Must | v1 |
| F23 | Criação de contas pelo gestor de projeto | Era parte do antigo F10; só faz falta quando as contas deixarem de vir de seed | Should | v2 |

### 3.4 Termos da interface

A interface está em inglês (NF01). Nos outros documentos os termos aparecem em português; o rótulo real da interface é o seguinte.

| Conceito | Rótulo na interface |
|---|---|
| Estados da tarefa | To do, In progress, In review, Blocked, Done, Cancelled, Duplicate |
| Estados do projeto | Not started, In progress, Attention, Late, Suspended, Completed |
| Fases do projeto | Initiation, Planning, Execution, Monitoring, Evaluation |
| Níveis de urgência e importância | Low, Medium, High, Critical |
| Quadrantes da Matriz | Do first, Schedule, Delegate, Later |
| Vistas | Matrix, List |
| Filtro "as minhas" | My tasks |
| Projeto sem tarefas | No tasks |
| Papéis | Team member, Project manager |
| Painel inicial | Dashboard |
| Iniciar e terminar sessão | Sign in, Sign out |
| Comentários | Comments |
| Pesquisa de projetos | Search projects |

## 4. Regras de negócio

| ID | Regra | Versão | Origem |
|---|---|---|---|
| R1 | O prazo de cada tarefa e subtarefa é obrigatório | v1 | Decisão |
| R2 | O prazo de uma subtarefa não pode ser posterior ao da tarefa-mãe, ao criar ou ao editar o prazo de qualquer das duas | v1 | Decisão |
| R3 | Antecipar o prazo da mãe para antes do de uma subtarefa é bloqueado, com aviso para rever primeiro as subtarefas | v1 | Decisão |
| R4 | Uma tarefa-mãe não pode ser concluída nem cancelada com subtarefas em aberto | v1 | Decisão |
| R5 | Uma subtarefa cancelada ou duplicada conta como fechada (em estado final) | v1 | Decisão |
| R6 | Uma tarefa bloqueada exige motivo. Uma tarefa duplicada exige indicar a tarefa original, escolhida manualmente: tem de ser do mesmo projeto, diferente da própria e não pode estar em estado "duplicado" | v1 | Decisão |
| R7 | Cada tarefa tem um lembrete, com antecedência definida por tarefa; o alerta de atraso repete-se segundo um intervalo definido pelo utilizador | v2 | Decisão |
| R8 | Um membro só vê os projetos de que é membro, incluindo os concluídos e suspensos; o gestor vê só os projetos de que é gestor | v1 | Decisão, detalhes em P20 (confirmado) |
| R9 | Só o gestor apaga projetos e recupera apagados; qualquer membro da equipa apaga tarefas e subtarefas. Não se apaga uma tarefa-mãe com subtarefas em aberto. As subtarefas apagadas vão para a reciclagem, tal como as tarefas. Um projeto recupera-se como estava no momento em que foi apagado: voltam as tarefas apagadas com ele, e as tarefas apagadas antes continuam na reciclagem. A mesma regra vale para tarefas e subtarefas: recuperar uma tarefa devolve as subtarefas apagadas com ela, as apagadas antes continuam na reciclagem, e uma tarefa ou subtarefa só se recupera enquanto o projeto ou a tarefa-mãe a que pertence não tiver sido eliminado de vez | v1 | Decisão |
| R10 | Só se arquiva um projeto concluído (um projeto suspenso não se arquiva); o arquivo guarda tarefas e histórico | v2 | Decisão |
| R11 | Um membro pertence a um projeto em aberto de cada vez; adicionar a um projeto um membro que já está noutro projeto em aberto é recusado, com um aviso. Projetos concluídos e suspensos não são projetos em aberto (P4, P20). A regra não se aplica ao gestor. Exceção: ao retomar um projeto suspenso, os membros que entretanto entraram noutro projeto em aberto ficam sinalizados e o gestor vê um aviso (P22) | v1 | Decisão, detalhes em P20 e P22 (confirmado) |
| R12 | Só o gestor altera a fase do projeto e define o seu estado, exceto "atenção" e "atrasado", que são só calculados e que o gestor não define (P5) | v1 | Decisão, detalhes em P5 (confirmado) |
| R13 | Não é aceite um prazo no passado ao criar ou ao editar o prazo de uma tarefa ou subtarefa; o prazo igual à data atual é aceite | v1 | Decisão |
| R14 | O título tem no máximo 100 caracteres e a descrição 2000 (tarefas e projetos, P17); o motivo de bloqueio de uma tarefa tem no máximo 100. Contam como caracteres as letras, a pontuação, os espaços e os símbolos | v1 | Decisão, detalhes em P17 (confirmado) |
| R15 | A Lista mostra todas as tarefas do projeto, incluindo as de estado final; as tarefas em aberto aparecem antes das fechadas; tem o filtro "as minhas" | v1 | Decisão |
| R16 | A data de início e a data de fim do projeto são obrigatórias, e o instante de fim é posterior ao de início (P23, P26) | v1 | Decisão, detalhes em P23 e P26 (confirmado) |
| R17 | O gestor pode remover um membro de um projeto em aberto, ficando o membro livre para outro projeto. Se o membro tiver tarefas por concluir nesse projeto, o gestor tem de adicionar um novo membro, que fica com essas tarefas; sem o novo membro, a remoção não é concluída (P24) | v1 | Decisão, detalhes em P24 (confirmado) |
| R18 | A Matriz, a Lista e o cálculo do progresso consideram só as tarefas principais; as subtarefas aparecem dentro da tarefa-mãe. O filtro "as minhas" mostra a tarefa principal se o membro for responsável por ela ou por alguma das suas subtarefas (P25) | v1 | Decisão, detalhes em P25 (confirmado) |
| R19 | Cada projeto tem um só gestor, indicado por quem cria o projeto. A empresa pode ter vários gestores, e cada um vê só os seus projetos. O gestor que cria um projeto e indica outro gestor deixa de o ver. Um gestor pode ser responsável por tarefas do seu projeto e, depois de adicionado como membro (a regra R11 não se lhe aplica), por tarefas de outros projetos | v1 | Decisão |
| R20 | Os projetos, tarefas e subtarefas apagados ficam 30 dias na reciclagem e são depois eliminados definitivamente | v1 | Decisão |
| R21 | Os responsáveis de uma tarefa ou subtarefa têm de ser membros do projeto (ou o seu gestor). Para atribuir uma tarefa a alguém de fora, essa pessoa é primeiro adicionada ao projeto | v1 | Decisão |
| R22 | Só se criam tarefas e subtarefas num projeto em aberto (P4): num projeto concluído ou suspenso o controlo de criar não aparece e, se a criação for pedida por outra via, é recusada com uma mensagem a explicar o motivo (NF08) | v1 | Decisão |
| R23 | Dentro de cada quadrante da Matriz, e dentro de cada grupo da Lista (em aberto e fechadas), as tarefas ordenam-se por prazo, do mais próximo (mais antigo) para o mais distante. Em caso de prazo igual, ordenam-se pela urgência e depois pela importância, do nível mais alto para o mais baixo | v1 | Decisão |
| R24 | Os membros do projeto e o seu gestor adicionam comentários às tarefas e subtarefas a que têm acesso e vêem os comentários dessas tarefas. Cada comentário guarda o autor e a data e hora de criação, atribuídos pela aplicação. O texto é simples (NF09) e tem no máximo 2000 caracteres. Só o autor edita e apaga o seu comentário, e apagar elimina-o de vez (a reciclagem cobre só projetos e tarefas). Pode comentar-se, editar e apagar em projetos em aberto e suspensos; num projeto concluído não se adiciona, edita nem apaga, e os comentários continuam visíveis. Os comentários seguem a tarefa: ao apagá-la ou recuperá-la, acontece o mesmo aos comentários (R9, R20). São listados do mais antigo para o mais recente | v1 | Decisão, detalhes em P28 (confirmado) |
| R25 | A pesquisa de projetos faz-se pelo nome e só devolve projetos acessíveis ao utilizador (R8), incluindo os concluídos. A Lista filtra as tarefas por estado e por quadrante (a "prioridade" do novo âmbito, calculada a partir da urgência e da importância); os filtros combinam-se com o filtro "as minhas" | v1 | Decisão, detalhes em P29 (confirmado) |
| R26 | Uma tarefa ou subtarefa tem sempre pelo menos um responsável. Podem associar-se vários responsáveis, que têm de ser elegíveis (R21), e remover-se atribuições; remover o último responsável é recusado, com uma mensagem a pedir que se atribua primeiro outra pessoa | v1 | Decisão |
| R27 | Num projeto suspenso, as tarefas e subtarefas editam-se, mudam de estado e apagam-se, mas não se criam novas (R22). Num projeto concluído não se cria, edita, muda de estado nem apaga nada nas tarefas, subtarefas, responsáveis e comentários (R24), e os dados continuam visíveis. Os controlos não aparecem e, se a ação for pedida por outra via, é recusada com uma mensagem a explicar o motivo (NF08) | v1 | Decisão |

**Origem das regras:** *Decisão* quer dizer que a autora decidiu tudo o que a regra diz. *Decisão, detalhes em Pn (confirmado)* quer dizer que a ideia é da autora e o pressuposto que a completa já foi aceite. *Decisão, detalhes em Pn (por validar)* quer dizer que a ideia é da autora e o pressuposto que a completa ainda não foi aceite. *Pressuposto Pn (por validar)* quer dizer que a regra depende de algo que ainda não foi confirmado (ver [constraints.md](constraints.md#2-pressupostos-assumidos)).

## 5. Requisitos não funcionais

Só estão aqui os requisitos que foram definidos ou que decorrem de decisões já tomadas.

| ID | Requisito | Origem | Versão |
|---|---|---|---|
| NF01 | A interface está em inglês | Decisão | v1 |
| NF02 | A aplicação é usada em computador | Decisão | v1 |
| NF03 | Dimensão de referência: cerca de 30 utilizadores, 3 a 5 projetos em aberto em simultâneo, cada um com uma equipa de 5 a 8 pessoas | Pressuposto (P18) | v1 |
| NF04 | Controlo de acesso por papel e por projeto, com o utilizador autenticado por sessão (F10, S5) | Pressuposto (P15), regra R8 | v1 |
| NF05 | Rastreabilidade: ficam registados quem fez o quê e quando (criar, editar, mudar de estado, apagar) | Pressuposto (P14) | v2 |
| NF06 | Recuperabilidade: o que é apagado vai para uma reciclagem, durante 30 dias, e pode ser recuperado pelo gestor | Decisão, regras R9 e R20 | v1 |
| NF07 | O cálculo da barra de progresso e dos estados "atenção" e "atrasado" segue exatamente as fórmulas definidas | Pressuposto (P5, P7) | v1 |
| NF08 | Sempre que uma ação é recusada, é mostrada uma mensagem que explica o motivo e o que fazer. Quando o utilizador não tem acesso a uma ação, o controlo simplesmente não aparece | Decisão | v1 |
| NF09 | O texto introduzido pelos utilizadores (nomes, títulos, descrições, motivos) é sempre mostrado como texto simples, nunca interpretado como código nem como formatação | Decisão | v1 |

**Como se verificam:** NF06 pelos cenários 7.1 a 7.6; NF07 pelos cenários 6.1 a 6.13; NF08 pelos cenários 1.5, 2.7 e 7.4; NF09 pelo cenário 2.8. O NF03 é uma dimensão de referência e não tem critério de aceitação.

**Sem requisito definido (fora de âmbito):** desempenho, disponibilidade, compatibilidade com navegadores, acessibilidade, cópias de segurança. Ver [constraints.md](constraints.md#5-limitações-de-âmbito).

## 6. Âmbito do MVP (v1)

**Dentro do MVP:** F01 a F10, F17 e F22, com Matriz e Lista como vistas e o Painel inicial como ecrã de entrada (conciliação com o novo âmbito na [secção 8](#8-conciliação-com-o-novo-âmbito-funcional)).

**Critério de aceitação do MVP:** um gestor consegue criar projetos, formar a equipa, atribuir tarefas, e acompanhar o progresso de cada projeto e do conjunto; um membro consegue gerir as tarefas do seu projeto na Matriz e na Lista; ambos iniciam sessão, comentam tarefas, pesquisam projetos, filtram tarefas e vêem o painel inicial com as contagens por estado. Verifica-se pelo [cenário de aceitação do MVP](user-stories.md#cenário-de-aceitação-do-mvp).

**Riscos**
- Na v1 as contas são de demonstração, criadas por seed, e não há regras de palavra-passe nem segurança avançada (S5, C8): só se usam dados fictícios.
- Sem arquivo, auditoria nem lembretes na v1, não há registo de ações nem avisos de prazo.
- Não há requisitos de segurança avançada nem de proteção de dados, o que é um risco se a aplicação for usada fora do exercício.

## 7. Funcionalidades fora do âmbito inicial

**v2 (Should):** F11 a F14 e F23.

| ID | Funcionalidade | Estado |
|---|---|---|
| F23 | Criação de contas pelo gestor de projeto | v2 |
| F11 | Etiquetas por tipo de trabalho e tempo de execução | v2 |
| F12 | Lembretes e alertas de atraso | v2 |
| F13 | Arquivo de projetos concluídos | v2 |
| F14 | Registo de auditoria | v2 |

**v3 (Could):** F15, F16 e F18 a F21.

| ID | Funcionalidade | Estado |
|---|---|---|
| F15 | Vista Calendário | v3 |
| F16 | Anexos nas tarefas | v3 |
| F18 | Avisos por email ou SMS | v3 |
| F19 | Importação e exportação de dados | Não definido ("talvez") |
| F20 | Registo de tempo, tarefas repetidas, modelos de projeto | "Para já não" |
| F21 | Quadro por estados | Descartado na primeira fase |

Excluído de forma consciente, sem funcionalidade associada: estado "adiantado" do projeto (P6) e utilizadores externos à empresa (P1).

Extensões opcionais do novo âmbito, fora do MVP: registo público de utilizadores e recuperação de palavra-passe.

## 8. Conciliação com o novo âmbito funcional

Em 2026-11-11 o exercício acrescentou um comportamento mínimo para a aplicação: autenticação, projetos, tarefas, responsáveis, comentários, pesquisa e filtros, e um painel inicial. Esta secção regista as diferenças face ao MVP aprovado e como foram resolvidas, antes de qualquer implementação.

| Área | MVP aprovado | Novo âmbito | Resolução |
|---|---|---|---|
| **Autenticação** | Sem login na v1: seletor de utilizador (S4, P21); login na v2 (F10) | Iniciar e terminar sessão e consultar o utilizador autenticado; contas de demonstração criadas por seed | **F10 sobe para a v1** (Must). O seletor (S4) é substituído pela sessão (S5). A criação de contas pelo gestor passa a **F23** (v2). Registo público e recuperação de palavra-passe ficam fora (extensões opcionais) |
| **Projetos** | F01, F06, F09 | Criar, listar, consultar, editar e eliminar os autorizados | Alinhado. "Eliminar" é enviar para a reciclagem (R9, R20), decisão da autora |
| **Tarefas** | F02, F03: urgência e importância, 4 níveis cada | Criar, consultar, editar e eliminar, com estado e prioridade | Alinhado. A "prioridade" é o **quadrante** da Matriz, calculado a partir da urgência e da importância, que se mantêm (R25), decisão da autora. "Eliminar" como nos projetos |
| **Responsáveis** | F02, R21 (só membros do projeto) | Associar vários utilizadores elegíveis e remover atribuições | Alinhado. "Elegíveis" são os membros do projeto e o seu gestor (R21). Remover atribuições é permitido, mas fica sempre pelo menos um responsável (**R26**), decisão da autora |
| **Comentários** | F17, v3 (Could) | Adicionar e listar, com autor e data | **F17 sobe para a v1** (Must), adicionar e listar, com o autor a poder editar e apagar os seus (**R24**, P28) |
| **Pesquisa e filtros** | Só o filtro "as minhas" | Pesquisar projetos pelo nome; filtrar tarefas por estado e prioridade | **Funcionalidade nova, F22** (Must, v1), **R25**, P29 |
| **Painel inicial** | F07: vista global só do gestor, com barra de progresso e estado, sem projetos concluídos | Projetos acessíveis com contagens de tarefas por estado e navegação para o detalhe | Um só painel para os dois papéis. **F07 reescrito**: contagens por estado para todos; o gestor vê também a barra de progresso e o estado. P27 resolve o que fazer com os concluídos |

**Pressupostos novos, todos confirmados:** P27 (painel), P28 (comentários) e P29 (pesquisa e filtros), em [constraints.md](constraints.md#2-pressupostos-assumidos).

**Impacto nos outros documentos:** S4 a S6, C8, P15, P21 e T12 em constraints; user stories US10 a US13 e o cenário 2.11; módulos, modelo de dados (entidade de comentários e credenciais), contrato OpenAPI e wireframe.

## Anexo A. Perguntas de clarificação e respostas

| # | Tema | Pergunta | Resposta | Tipo |
|---|---|---|---|---|
| 1 | Contexto | Que problema tem hoje a empresa? | Não tem aplicação; precisa de uma | Pressuposto (P19) |
| 2 | Escala | Quantos utilizadores, equipas e projetos? | Pequena empresa, cerca de 30 utilizadores, 3 a 5 projetos em aberto em simultâneo, cada um com uma equipa de 5 a 8 pessoas | Pressuposto (P18) |
| 3 | Papéis | Que tipos de utilizador existem? | Membro da equipa e gestor de projeto | Decisão |
| 4 | Contas | Quem cria e gere as contas? | O gestor de projeto (criação de contas na v2, F23) | Decisão |
| 5 | Equipas e projetos | Como se relacionam? | O gestor cria o projeto e adiciona membros, e isso forma a equipa; o gestor gere vários projetos; um membro pertence a um projeto em aberto de cada vez | Decisão |
| 6 | Visibilidade | Quem vê o quê? | O membro só vê o que a sua equipa faz; o gestor vê os projetos de que é gestor | Decisão |
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
| 21 | Arquivo | O que é arquivar? | Só se arquiva um projeto concluído, com todas as tarefas e o histórico (v2) | Decisão |
| 22 | Auditoria | É preciso registo de ações? | Sim (v2) | Decisão |
| 23 | Saída de utilizador | O que acontece? | Remove-se o utilizador e coloca-se outro no seu lugar | Decisão |
| 24 | Comentários | São necessários? | Ficam para mais tarde (revisto: sobem para a v1, secção 8) | Decisão |
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
| 35 | Login | Entra no MVP? | Não: passa para a v2 (revisto: sobe para a v1, secção 8) | Decisão |
| 36 | Equipa | Há uma entidade equipa? | Não: o gestor cria o projeto e adiciona membros, e isso forma a equipa | Decisão |
| 37 | Membro já noutro projeto | Pode ser adicionado a um segundo projeto em aberto? | Não: a ação é recusada com um aviso | Decisão |
| 38 | Lista | Que tarefas mostra e por que ordem? | Todas, incluindo as de estado final, com o filtro "as minhas"; em aberto primeiro, depois fechadas; dentro de cada grupo, pelo prazo mais próximo (R23) | Decisão |
| 39 | Identificação na v1 | Como sabe a aplicação quem é o utilizador, sem login? | O utilizador escolhe quem é num seletor, sem palavra-passe (substituído pelo login na v1, secção 8) | Decisão |
| 40 | Projeto suspenso | Conta como projeto em aberto? | Não: um projeto suspenso não conta como em aberto, porque não se vai trabalhar nele durante algum tempo; os membros são realocados a outro projeto | Decisão |
| 41 | Retoma de projeto suspenso | O que acontece à equipa? | A lista de membros mantém-se; os membros que entretanto entraram noutro projeto ficam com uma etiqueta vermelha e o gestor vê um aviso. Um membro continua a estar em um só projeto em aberto de cada vez | Decisão |
| 42 | Visibilidade e arquivo | Os membros libertados continuam a ver o projeto? A vista global mostra os suspensos? Arquiva-se um suspenso? | Sim; sim; não | Decisão |
| 43 | Vista global e projetos concluídos | A vista global mostra os projetos concluídos? | Não (no painel inicial do gestor, P27) | Decisão |
| 44 | Datas do projeto | São obrigatórias? | Sim; o instante de fim é posterior ao de início | Decisão (detalhes em P23) |
| 45 | Remover membro | Como se remove um membro de um projeto em aberto? | O gestor remove; se o membro tiver tarefas por concluir, tem de adicionar um novo membro que fica com elas, senão a remoção não é concluída | Decisão (detalhes em P24) |
| 46 | Prazo igual à data atual | Aceita-se na criação? | Sim | Decisão |
| 47 | Projeto sem tarefas | Que etiqueta mostra? | "Sem tarefas", mesmo depois da data de fim | Decisão |
| 48 | Subtarefas nas vistas e no progresso | Aparecem na Matriz e na Lista? Contam no progresso? | Não: aparecem dentro da tarefa-mãe e só as tarefas principais contam | Decisão |
| 49 | Tarefa duplicada | Como se sabe de qual é duplicada? | Marcação manual, escolhendo o original: do mesmo projeto, diferente da própria e não duplicado | Decisão |
| 50 | Projeto não iniciado | Fica atrasado se passar a data de fim? | Não: só os projetos "em andamento" recebem estados calculados | Decisão |
| 51 | Gestor | Quantos gestores por projeto? Pode ter tarefas? | Um só; pode ter tarefas atribuídas e a regra de um projeto em aberto por membro não se lhe aplica | Decisão |
| 52 | Apagar e estados finais | Que limites há? | Não se apaga uma tarefa-mãe com subtarefas em aberto; os membros movem tarefas e subtarefas para estados finais; as subtarefas apagadas vão para a reciclagem | Decisão |
| 53 | Prazos ao longo da vida | R2 e R13 aplicam-se quando? | Sempre que o prazo é definido ou alterado | Decisão |
| 54 | Etiqueta vermelha e reabrir | Quando desaparece? Para que estado se reabre? | Desaparece quando o membro está em um só projeto em aberto; ao reabrir, o gestor escolhe o estado | Decisão |
| 55 | Subtarefas | Têm responsáveis? O filtro "as minhas" considera-as? | Sim; sim | Decisão |
| 56 | Mensagens | O que se mostra quando uma ação é recusada ou não está disponível? | Recusada: mensagem com o motivo e o que fazer. Não disponível: o controlo não aparece | Decisão |
| 57 | Arredondamento e fuso horário | Como se arredondam os valores e que fuso tem o dia de fim? | Duas casas decimais; o dia de fim tem fuso horário (a definir em T5) | Decisão |
| 58 | Caracteres | O que conta nos limites? | Letras, pontuação, espaços e símbolos | Decisão |
| 59 | Reciclagem | Quanto tempo guarda? | 30 dias | Decisão |
| 60 | Seletor sem autenticação | Aceita-se que qualquer pessoa assuma o papel de gestor e veja a lista de utilizadores? | Sim, é uma decisão consciente na v1 (substituída pelo login na v1, secção 8) | Decisão |
| 61 | Estados "atenção" e "atrasado" | O gestor pode defini-los à mão? | Não: são só calculados | Decisão |
| 62 | Responsáveis | Têm de ser membros do projeto? | Sim; para atribuir uma tarefa a alguém de fora, adiciona-se primeiro essa pessoa ao projeto | Decisão |
| 63 | Mitigação do seletor | Como se reduz o risco do seletor sem autenticação? | Dados fictícios num ambiente controlado e o seletor só mostra nomes (o seletor foi substituído pelo login na v1, secção 8; os dados fictícios mantêm-se, C8) | Decisão |
| 64 | Vários gestores | A empresa pode ter vários gestores? Quem vê o quê? | Vários, mas um só por projeto; cada gestor vê só os seus projetos | Decisão |
| 65 | Escolha do gestor | Como se define o gestor de um projeto? | Quem cria o projeto indica o gestor; quem cria e indica outro gestor deixa de ver o projeto | Decisão |
| 66 | Fuso horário e hora de fim | Como se tratam? | O fuso horário é definido ao criar o projeto (por defeito, o de Lisboa) e a data de fim pode ter hora; sem hora, o dia de fim conta inteiro (P26) | Decisão |
