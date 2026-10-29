# Matriz — User Stories e Critérios de Aceitação

> Converte os requisitos do MVP de [requirements.md](requirements.md) em especificações verificáveis.
> Não propõe tecnologias nem contém código.

## 1. Como ler este documento

- **Papéis:** gestor de projeto e membro da equipa ([requirements.md](requirements.md#2-tipos-de-utilizadores)).
- **Formato das stories:** *Como [tipo de utilizador], quero [funcionalidade], para [benefício].*
- **"Identificado":** na v1 não há login; um utilizador "identificado" é o que foi escolhido no seletor de utilizador ([S7](constraints.md#3-requisitos-de-segurança-e-privacidade)).
- **Critérios de aceitação:** formato *Given / When / Then*, com valores concretos para poderem ser verificados sem interpretação.
- **Rastreabilidade:** cada story indica as funcionalidades (F) e regras (R) de [requirements.md](requirements.md) e os pressupostos (P) de [constraints.md](constraints.md) em que se baseia; os códigos são ligações para a secção onde estão definidos.
- **Níveis de urgência e importância:** cada tarefa guarda os 4 níveis (1 = low, 2 = medium, 3 = high, 4 = critical). A Matriz tem dois eixos, e cada eixo é dividido em duas metades para formar 4 quadrantes: os níveis 1 e 2 ficam na metade "baixo" e os níveis 3 e 4 na metade "alto". Os 4 níveis continuam visíveis em cada tarefa.

## 2. User Stories do MVP

| ID | Como... | Quero... | Para... | Base |
|---|---|---|---|---|
| US01 | Gestor de projeto | criar um projeto com nome, descrição, datas, fase e estado, e adicionar-lhe membros | organizar o trabalho da empresa e formar a equipa | [F01](requirements.md#3-requisitos-funcionais), [R12](requirements.md#4-regras-de-negócio) |
| US02 | Membro da equipa | criar uma tarefa com prazo, urgência e importância | registar o trabalho que a equipa tem de fazer | [F02](requirements.md#3-requisitos-funcionais), [F03](requirements.md#3-requisitos-funcionais), [R1](requirements.md#4-regras-de-negócio) |
| US03 | Membro da equipa | ver as tarefas de um projeto numa Matriz urgente/importante, com filtro "as minhas" | perceber o que fazer primeiro | [F04](requirements.md#3-requisitos-funcionais), [P9](constraints.md#2-pressupostos-assumidos), [P10](constraints.md#2-pressupostos-assumidos) |
| US04 | Membro da equipa | dividir uma tarefa em subtarefas com prazo próprio | acompanhar trabalho grande por partes | [F08](requirements.md#3-requisitos-funcionais), [R2](requirements.md#4-regras-de-negócio), [R3](requirements.md#4-regras-de-negócio) |
| US05 | Membro da equipa | concluir uma tarefa-mãe só quando as subtarefas estiverem fechadas | garantir que nada fica por fazer | [F08](requirements.md#3-requisitos-funcionais), [R4](requirements.md#4-regras-de-negócio), [R5](requirements.md#4-regras-de-negócio) |
| US06 | Gestor de projeto | ver uma barra de progresso por projeto e o respetivo estado calculado | saber se cada projeto está dentro do prazo | [F07](requirements.md#3-requisitos-funcionais), [P5](constraints.md#2-pressupostos-assumidos), [P7](constraints.md#2-pressupostos-assumidos) |
| US07 | Gestor de projeto | recuperar projetos e tarefas apagados | corrigir remoções acidentais | [F09](requirements.md#3-requisitos-funcionais), [R9](requirements.md#4-regras-de-negócio) |
| US08 | Membro da equipa | ver apenas os projetos de que sou membro e as suas tarefas | trabalhar sem ver informação de outras equipas | [F06](requirements.md#3-requisitos-funcionais), [R8](requirements.md#4-regras-de-negócio) |
| US09 | Membro da equipa | ver todas as tarefas de um projeto numa lista, com as abertas primeiro e o filtro "as minhas" | ter uma visão completa do trabalho do projeto | [F05](requirements.md#3-requisitos-funcionais), [R15](requirements.md#4-regras-de-negócio) |

### Prioridades

A prioridade de cada story é a da funcionalidade principal em que se baseia (MoSCoW). Todas as stories atuais são da v1 (MVP).

| Prioridade | Versão | Stories | Funcionalidades |
|---|---|---|---|
| **Must** | v1 | US01 a US09 | F01 a F09 |

Todas as funcionalidades da v1 têm pelo menos uma user story: F01 (US01), F02 e F03 (US02), F04 (US03), F05 (US09), F06 (US08), F07 (US06), F08 (US04 e US05) e F09 (US07).

As funcionalidades da v2 (Should) ainda não têm user story: F10 (login), F11 (etiquetas), F12 (lembretes), F13 (arquivo) e F14 (auditoria). Ficam para a ronda seguinte.

## 3. Critérios de aceitação

### US01 — Criar projeto

**Cenário 1.1: criação com sucesso**
- **Given** um gestor de projeto identificado
- **When** cria um projeto com nome "Website", descrição, data de início e data de fim, fase "iniciação" e estado "não iniciado"
- **Then** o projeto aparece na lista de projetos em aberto com esses valores.

**Cenário 1.2: membro não pode criar projetos**
- **Given** um membro da equipa identificado
- **When** tenta criar um projeto
- **Then** a ação é recusada e nenhum projeto é criado.

**Cenário 1.3: só o gestor altera fase e estado**
- **Given** um projeto na fase "planeamento"
- **When** um membro da equipa tenta alterar a fase ou o estado
- **Then** a alteração é recusada e a fase e o estado mantêm-se.

**Cenário 1.4: adicionar um membro ao projeto**
- **Given** um gestor de projeto identificado, um projeto "Website" e um membro "Ana" que não está em nenhum projeto em aberto
- **When** o gestor adiciona a Ana ao projeto "Website"
- **Then** a Ana passa a fazer parte da equipa do projeto e passa a ver o projeto.

**Cenário 1.5: membro já está noutro projeto em aberto**
- **Given** a membro "Ana" na equipa do projeto "Website", que está em aberto, e um projeto "Intranet"
- **When** o gestor tenta adicionar a Ana ao projeto "Intranet"
- **Then** a ação é recusada, é mostrado um aviso a indicar que a Ana já está num projeto, e a equipa do projeto "Intranet" não muda.

**Cenário 1.6: só o gestor adiciona membros**
- **Given** um membro da equipa identificado
- **When** tenta adicionar um membro a um projeto
- **Then** a ação é recusada.

**Cenário 1.7: projeto suspenso liberta os membros**
- **Given** a membro "Ana" na equipa do projeto "Website", que o gestor passa para o estado "suspenso", e um projeto "Intranet" em aberto
- **When** o gestor adiciona a Ana ao projeto "Intranet"
- **Then** a Ana é adicionada à equipa do projeto "Intranet".

**Cenário 1.8: retomar um projeto com membros noutro projeto**
- **Given** o projeto "Website" suspenso, com a Ana na lista de membros, e a Ana entretanto adicionada ao projeto "Intranet", que está em aberto
- **When** o gestor retoma o projeto "Website" (estado "em andamento")
- **Then** o projeto "Website" é retomado, a Ana continua na lista de membros com uma etiqueta vermelha, e o gestor vê um aviso a indicar que a Ana está noutro projeto.

### US02 — Criar tarefa

**Cenário 2.1: criação com sucesso**
- **Given** um membro da equipa identificado, num projeto de que é membro
- **When** cria a tarefa "Escrever relatório" com prazo 2026-12-15, urgência 3 e importância 4
- **Then** a tarefa fica registada no projeto com estado "por fazer" e esses valores.

**Cenário 2.2: prazo obrigatório**
- **Given** um membro da equipa a criar uma tarefa
- **When** tenta guardar sem prazo
- **Then** a tarefa não é criada e é indicado que o prazo é obrigatório.

**Cenário 2.3: vários responsáveis**
- **Given** uma tarefa nova
- **When** o membro atribui a tarefa a dois membros da equipa
- **Then** a tarefa mostra os dois responsáveis.

**Cenário 2.4: prazo no passado**
- **Given** a data atual 2026-11-10
- **When** o membro tenta criar uma tarefa com prazo 2026-11-09
- **Then** a tarefa não é criada e é indicado que o prazo não pode estar no passado.

**Cenário 2.5: tamanho máximo de título e descrição**
- **Given** um membro da equipa a criar uma tarefa
- **When** guarda uma tarefa com um título de 100 caracteres e uma descrição de 2000 caracteres
- **Then** a tarefa é criada.
- **When** tenta guardar uma tarefa com um título de 101 caracteres ou uma descrição de 2001 caracteres
- **Then** a tarefa não é criada e é indicado o limite excedido.

### US03 — Matriz do projeto

**Cenário 3.1: posição na matriz**
- **Given** um projeto com três tarefas: A (urgência 4, importância 4), B (urgência 1, importância 3) e C (urgência 3, importância 2)
- **When** o membro abre a Matriz do projeto
- **Then** A está no quadrante "urgente e importante", B em "não urgente e importante" e C em "urgente e não importante".

**Cenário 3.2: estados finais escondidos**
- **Given** uma tarefa em estado "concluído", "cancelado" ou "duplicado"
- **When** o membro abre a Matriz
- **Then** essa tarefa não aparece.

**Cenário 3.3: filtro "as minhas"**
- **Given** uma Matriz com tarefas de vários membros da equipa
- **When** o membro ativa o filtro "as minhas"
- **Then** só aparecem as tarefas em que o membro é responsável; ao desativar, voltam a aparecer todas as da equipa.

### US04 — Subtarefas e prazos

**Cenário 4.1: prazo dentro do limite**
- **Given** uma tarefa-mãe com prazo 2026-12-15
- **When** o membro cria uma subtarefa com prazo 2026-12-10
- **Then** a subtarefa é criada.

**Cenário 4.2: subtarefa com prazo posterior ao da mãe**
- **Given** uma tarefa-mãe com prazo 2026-12-15
- **When** o membro cria uma subtarefa com prazo 2026-12-20
- **Then** a subtarefa não é criada e é indicado que o prazo excede o da tarefa-mãe.

**Cenário 4.3: antecipar o prazo da mãe**
- **Given** uma tarefa-mãe com prazo 2026-12-15 e uma subtarefa com prazo 2026-12-12
- **When** o membro altera o prazo da mãe para 2026-12-10
- **Then** a alteração é recusada, o prazo mantém-se em 2026-12-15 e é indicado que existe uma subtarefa com prazo posterior.

**Cenário 4.4: estado e prioridades próprios da subtarefa**
- **Given** uma tarefa-mãe "por fazer" com urgência 2 e importância 2
- **When** o membro cria uma subtarefa com urgência 4 e importância 3 e a passa para "em curso"
- **Then** a subtarefa mostra estado "em curso", urgência 4 e importância 3, e a tarefa-mãe mantém o estado "por fazer", urgência 2 e importância 2.

### US05 — Concluir tarefa-mãe

**Cenário 5.1: subtarefa em aberto**
- **Given** uma tarefa-mãe com duas subtarefas, uma "concluído" e outra "em curso"
- **When** o membro tenta passar a mãe para "concluído"
- **Then** a mudança é recusada e a mãe mantém o estado anterior.

**Cenário 5.2: todas as subtarefas fechadas**
- **Given** uma tarefa-mãe com três subtarefas nos estados "concluído", "cancelado" e "duplicado"
- **When** o membro passa a mãe para "concluído"
- **Then** a mudança é aceite.

**Cenário 5.3: cancelar a mãe**
- **Given** uma tarefa-mãe com uma subtarefa "por fazer"
- **When** o membro tenta passar a mãe para "cancelado"
- **Then** a mudança é recusada.

### US06 — Progresso e estado do projeto

Regras usadas (ver [P5](constraints.md#2-pressupostos-assumidos) e [P16](constraints.md#2-pressupostos-assumidos)): o progresso esperado é o tempo decorrido, (hoje − início) ÷ (fim − início), em dias; o desvio é o progresso esperado menos o progresso real, em pontos percentuais; o estado é calculado só para projetos "em andamento".

**Cenário 6.1: cálculo da barra**
- **Given** um projeto com 10 tarefas: 4 concluídas, 2 em curso, 2 por fazer, 1 cancelada e 1 duplicada
- **When** o gestor abre a vista global
- **Then** a barra de progresso desse projeto mostra 50% (4 de 8, sem contar canceladas e duplicadas).

**Cenário 6.2: projeto sem tarefas contáveis**
- **Given** um projeto sem tarefas, ou só com tarefas canceladas e duplicadas
- **When** o gestor abre a vista global
- **Then** o projeto mostra a etiqueta "sem tarefas" em vez de uma percentagem, e não ocorre erro.

**Cenário 6.3: estado "atenção" (desvio de 15 pontos percentuais)**
- **Given** um projeto "em andamento" com data de início 2026-01-01 e data de fim 2026-01-31, a 2026-01-16 (50% do tempo decorrido) e com 35% de progresso (desvio de 15 pontos percentuais)
- **When** o gestor abre a vista global
- **Then** o projeto aparece com o estado "atenção".

**Cenário 6.4: desvio abaixo do limite**
- **Given** o mesmo projeto, a 2026-01-16, com 36% de progresso (desvio de 14 pontos percentuais)
- **When** o gestor abre a vista global
- **Then** o projeto mantém o estado "em andamento".

**Cenário 6.5: estado "atrasado" por desvio (25 pontos percentuais)**
- **Given** o mesmo projeto, a 2026-01-16, com 25% de progresso (desvio de 25 pontos percentuais)
- **When** o gestor abre a vista global
- **Then** o projeto aparece com o estado "atrasado".

**Cenário 6.6: estado "atrasado" por data de fim ultrapassada**
- **Given** um projeto "em andamento" com data de fim 2026-01-31, a 2026-02-02, com 80% de progresso
- **When** o gestor abre a vista global
- **Then** o projeto aparece com o estado "atrasado".

**Cenário 6.7: membro não vê a vista global**
- **Given** um membro da equipa identificado
- **When** abre a aplicação
- **Then** a vista global com os projetos de todas as equipas não está disponível.

**Cenário 6.8: projeto suspenso na vista global**
- **Given** um projeto no estado "suspenso"
- **When** o gestor abre a vista global
- **Then** o projeto aparece com o estado "suspenso" e a sua barra de progresso, e os estados "atenção" e "atrasado" não lhe são atribuídos por cálculo.

**Cenário 6.9: projeto concluído fora da vista global**
- **Given** um projeto no estado "concluído"
- **When** o gestor abre a vista global
- **Then** o projeto não aparece.

### US07 — Recuperar apagados

**Cenário 7.1: apagar vai para a reciclagem**
- **Given** um gestor de projeto e um projeto com 5 tarefas
- **When** apaga o projeto
- **Then** o projeto desaparece da lista de projetos e fica na reciclagem com as 5 tarefas.

**Cenário 7.2: recuperar**
- **Given** um projeto na reciclagem
- **When** o gestor o recupera
- **Then** o projeto volta à lista, com as 5 tarefas e os estados que tinham.

**Cenário 7.3: membro não recupera**
- **Given** uma tarefa apagada por um membro e presente na reciclagem
- **When** um membro tenta recuperá-la
- **Then** a ação é recusada; só o gestor a pode recuperar.

### US08 — Visibilidade por equipa

**Cenário 8.1: só os projetos de que é membro**
- **Given** dois projetos, A (com a membro Ana) e B (com o membro Rui)
- **When** a Ana abre a aplicação
- **Then** só vê o projeto A e as suas tarefas.

**Cenário 8.2: acesso direto recusado**
- **Given** a membro Ana, que não faz parte do projeto B
- **When** tenta abrir o projeto B
- **Then** o acesso é recusado e nenhum dado do projeto é mostrado.

**Cenário 8.3: gestor vê tudo**
- **Given** um gestor de projeto identificado
- **When** abre a lista de projetos
- **Then** vê os projetos de todas as equipas.

### US09 — Lista de tarefas do projeto

**Cenário 9.1: tarefas do projeto**
- **Given** um projeto com 3 tarefas abertas e 2 tarefas concluídas, e um membro desse projeto
- **When** o membro abre a Lista do projeto
- **Then** aparecem as 5 tarefas, cada uma com título, estado, prazo, urgência, importância e responsáveis.

**Cenário 9.2: abertas antes das fechadas**
- **Given** um projeto com as tarefas T1 "concluído", T2 "por fazer", T3 "cancelado" e T4 "em curso"
- **When** o membro abre a Lista
- **Then** T2 e T4 aparecem antes de T1 e T3.

**Cenário 9.3: filtro "as minhas"**
- **Given** uma Lista com tarefas da Ana e do Rui, incluindo tarefas da Ana em estado "concluído"
- **When** a Ana ativa o filtro "as minhas"
- **Then** só aparecem as tarefas em que a Ana é responsável, incluindo as concluídas; ao desativar o filtro, voltam a aparecer todas as do projeto.

## 4. Situações excecionais e casos-limite

| ID | Situação | Resultado esperado | Base |
|---|---|---|---|
| EC1 | Projeto sem tarefas, ou só com tarefas canceladas e duplicadas (denominador do progresso = 0) | Mostra a etiqueta "sem tarefas" e não ocorre erro (cenário 6.2) | [P7](constraints.md#2-pressupostos-assumidos), [P16](constraints.md#2-pressupostos-assumidos) |
| EC2 | Antecipar o prazo da mãe para antes de uma subtarefa | Alteração recusada com aviso (cenário 4.3) | [R3](requirements.md#4-regras-de-negócio) |
| EC3 | Concluir uma mãe cujas subtarefas estão todas canceladas ou duplicadas | Aceite, porque contam como fechadas (cenário 5.2) | [R5](requirements.md#4-regras-de-negócio) |
| EC4 | Marcar uma tarefa como "bloqueado" sem motivo | Mudança recusada até ser indicado o motivo | [R6](requirements.md#4-regras-de-negócio) |
| EC5 | Marcar uma tarefa como "duplicado" sem indicar de qual | Mudança recusada até ser indicada a tarefa | [R6](requirements.md#4-regras-de-negócio) |
| EC6 | Um membro sai da empresa | O membro é removido e as suas tarefas passam para quem o substitui | [P13](constraints.md#2-pressupostos-assumidos) |
| EC7 | Apagar um projeto que tem tarefas | Projeto e tarefas vão juntos para a reciclagem (cenário 7.1) | [R9](requirements.md#4-regras-de-negócio), [P12](constraints.md#2-pressupostos-assumidos) |
| EC8 | Um membro abre uma ligação direta para um projeto de que não é membro | Acesso recusado (cenário 8.2) | [R8](requirements.md#4-regras-de-negócio) |
| EC9 | Um membro tenta reabrir uma tarefa em estado final | Ação recusada; só o gestor pode reabrir | [P8](constraints.md#2-pressupostos-assumidos) |
| EC10 | Criar uma tarefa com prazo no passado | Tarefa não criada, com mensagem (cenário 2.4) | [R13](requirements.md#4-regras-de-negócio) |
| EC11 | Desvio exatamente igual a 15 ou a 25 pontos percentuais | 15 passa a "atenção" (cenário 6.3) e 25 passa a "atrasado" (cenário 6.5) | [P5](constraints.md#2-pressupostos-assumidos) |
| EC12 | Adicionar a um projeto um membro que já está noutro projeto em aberto | Ação recusada com aviso (cenário 1.5) | [R11](requirements.md#4-regras-de-negócio) |
| EC13 | Adicionar a um projeto um membro cujo projeto anterior foi concluído ou suspenso | Aceite, porque esses projetos não estão em aberto (cenário 1.7) | [R11](requirements.md#4-regras-de-negócio), [P20](constraints.md#2-pressupostos-assumidos) |
| EC14 | Retomar um projeto suspenso cujos membros já estão noutro projeto em aberto | Retoma aceite, com etiqueta vermelha nesses membros e aviso ao gestor (cenário 1.8) | [P22](constraints.md#2-pressupostos-assumidos) |

## 5. Revisão: ambiguidades e pontos em aberto

### Ambiguidades resolvidas
| # | Ambiguidade | Como foi resolvida |
|---|---|---|
| A1 | "Baixo" e "alto" nas escalas de 4 níveis | Por números: 1-2 baixo, 3-4 alto, só para posicionar na Matriz (confirmado pela autora) |
| A2 | "Atrasado" e "atenção" sem números | Cenários 6.3 a 6.6 usam percentagens e datas concretas ([P5](constraints.md#2-pressupostos-assumidos)) |
| A3 | "Progresso" | Cálculo explícito no cenário 6.1 ([P7](constraints.md#2-pressupostos-assumidos)) |
| A4 | "Aviso" quando se antecipa o prazo da mãe | Passou a ser uma recusa com mensagem ([R3](requirements.md#4-regras-de-negócio)) |
| A5 | "Fechada" para subtarefas | Concluída, cancelada ou duplicada ([R5](requirements.md#4-regras-de-negócio)) |
| A6 | Prazo no passado | Não é aceite na criação ([R13](requirements.md#4-regras-de-negócio)) |
| A7 | Progresso sem tarefas contáveis | Etiqueta "sem tarefas" em vez de 0% ([P16](constraints.md#2-pressupostos-assumidos)) |
| A8 | Tamanho de título e descrição | 100 e 2000 caracteres ([R14](requirements.md#4-regras-de-negócio), [P17](constraints.md#2-pressupostos-assumidos)) |

### Adiado por decisão da autora
- Regras de palavra-passe e recuperação de acesso: não se estipulam para já.
- Conteúdo, antecedência e repetição dos lembretes e alertas ([F12](requirements.md#3-requisitos-funcionais)): ficam para a user story da v2.
- Conteúdo do registo de auditoria ([F14](requirements.md#3-requisitos-funcionais)), além de [P14](constraints.md#2-pressupostos-assumidos): fica para a user story da v2.
- Login, etiquetas e arquivo (F10, F11 e F13): ficam para a v2.
- Anexos, comentários, Calendário e avisos por email ou SMS: v3.

### Por definir
- Prazo igual à data atual: decidir se é aceite na criação.
- Alterar o prazo de uma tarefa existente para o passado: decidir se é aceite.
- Ordem das tarefas dentro de cada grupo da Lista (abertas e fechadas): por definir.- O nível 2 chama-se "medium", mas conta como "baixo" na Matriz: incoerência de nomenclatura a confirmar com o cliente.

A lista completa de questões em aberto está em [constraints.md](constraints.md#4-decisões-técnicas-pendentes).
