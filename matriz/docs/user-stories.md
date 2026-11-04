# Matriz — User Stories e Critérios de Aceitação

> Converte os requisitos do MVP de [requirements.md](requirements.md) em especificações verificáveis.
> Não propõe tecnologias nem contém código.

## 1. Como ler este documento

- **Papéis:** gestor de projeto e membro da equipa ([requirements.md](requirements.md#2-tipos-de-utilizadores)).
- **Formato das stories:** *Como [tipo de utilizador], quero [funcionalidade], para [benefício].*
- **"Identificado":** na v1 não há login; um utilizador "identificado" é o que foi escolhido no seletor de utilizador ([S4](constraints.md#3-requisitos-de-segurança-e-privacidade)).
- **"Recusada" e "não disponível":** quando uma ação é recusada, é mostrada uma mensagem que explica o motivo e o que fazer ([NF08](requirements.md#5-requisitos-não-funcionais)). Quando o utilizador não tem acesso à ação, o controlo não aparece e não há mensagem.
- **Rótulos da interface:** os estados, níveis e rótulos entre aspas nos cenários estão em português; o rótulo real da interface, em inglês, está na [tabela de termos](requirements.md#34-termos-da-interface).
- **Critérios de aceitação:** formato *Given / When / Then*, com valores concretos para poderem ser verificados sem interpretação.
- **Rastreabilidade:** cada story indica as funcionalidades (F) e regras (R) de [requirements.md](requirements.md) e os pressupostos (P) de [constraints.md](constraints.md) em que se baseia; os códigos são ligações para a secção onde estão definidos.
- **Níveis de urgência e importância:** cada tarefa guarda os 4 níveis (1 = low, 2 = medium, 3 = high, 4 = critical). A Matriz tem dois eixos, e cada eixo é dividido em duas metades para formar 4 quadrantes: os níveis 1 e 2 ficam na metade "baixo" e os níveis 3 e 4 na metade "alto". Os 4 níveis continuam visíveis em cada tarefa.

## 2. User Stories do MVP

| ID | Como... | Quero... | Para... | Base |
|---|---|---|---|---|
| US01 | Gestor de projeto | criar um projeto com nome, descrição, datas, fase e estado, e adicionar-lhe ou remover-lhe membros | organizar o trabalho da empresa e formar a equipa | [F01](requirements.md#3-requisitos-funcionais), [R11](requirements.md#4-regras-de-negócio), [R12](requirements.md#4-regras-de-negócio), [R14](requirements.md#4-regras-de-negócio), [R16](requirements.md#4-regras-de-negócio), [R17](requirements.md#4-regras-de-negócio), [R19](requirements.md#4-regras-de-negócio) |
| US02 | Membro da equipa | criar uma tarefa com prazo, urgência e importância | registar o trabalho que a equipa tem de fazer | [F02](requirements.md#3-requisitos-funcionais), [F03](requirements.md#3-requisitos-funcionais), [R1](requirements.md#4-regras-de-negócio) |
| US03 | Membro da equipa | ver as tarefas de um projeto numa Matriz urgente/importante, com filtro "as minhas" | perceber o que fazer primeiro | [F04](requirements.md#3-requisitos-funcionais), [R18](requirements.md#4-regras-de-negócio), [P9](constraints.md#2-pressupostos-assumidos), [P10](constraints.md#2-pressupostos-assumidos) |
| US04 | Membro da equipa | dividir uma tarefa em subtarefas com prazo próprio | acompanhar trabalho grande por partes | [F08](requirements.md#3-requisitos-funcionais), [R2](requirements.md#4-regras-de-negócio), [R3](requirements.md#4-regras-de-negócio) |
| US05 | Membro da equipa | concluir uma tarefa-mãe só quando as subtarefas estiverem fechadas | garantir que nada fica por fazer | [F08](requirements.md#3-requisitos-funcionais), [R4](requirements.md#4-regras-de-negócio), [R5](requirements.md#4-regras-de-negócio) |
| US06 | Gestor de projeto | ver uma barra de progresso por projeto e o respetivo estado calculado | saber se cada projeto está dentro do prazo | [F07](requirements.md#3-requisitos-funcionais), [R18](requirements.md#4-regras-de-negócio), [P5](constraints.md#2-pressupostos-assumidos), [P7](constraints.md#2-pressupostos-assumidos) |
| US07 | Gestor de projeto | recuperar projetos e tarefas apagados | corrigir remoções acidentais | [F09](requirements.md#3-requisitos-funcionais), [R9](requirements.md#4-regras-de-negócio), [R20](requirements.md#4-regras-de-negócio) |
| US08 | Membro da equipa | ver apenas os projetos de que sou membro e as suas tarefas | trabalhar sem ver informação de outras equipas | [F06](requirements.md#3-requisitos-funcionais), [R8](requirements.md#4-regras-de-negócio) |
| US09 | Membro da equipa | ver todas as tarefas de um projeto numa lista, com as abertas primeiro e o filtro "as minhas" | ter uma visão completa do trabalho do projeto | [F05](requirements.md#3-requisitos-funcionais), [R15](requirements.md#4-regras-de-negócio), [R18](requirements.md#4-regras-de-negócio) |

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
- **Then** a ação de criar projeto não está disponível e nenhum projeto é criado.

**Cenário 1.3: só o gestor altera fase e estado**
- **Given** um projeto na fase "planeamento"
- **When** um membro da equipa tenta alterar a fase ou o estado
- **Then** a alteração da fase e do estado não está disponível e ambos mantêm-se.

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
- **Then** a ação de adicionar membros não está disponível.

**Cenário 1.7: projeto suspenso liberta os membros**
- **Given** a membro "Ana" na equipa do projeto "Website", que o gestor passa para o estado "suspenso", e um projeto "Intranet" em aberto
- **When** o gestor adiciona a Ana ao projeto "Intranet"
- **Then** a Ana é adicionada à equipa do projeto "Intranet".

**Cenário 1.8: retomar um projeto com membros noutro projeto**
- **Given** o projeto "Website" suspenso, com a Ana na lista de membros, e a Ana entretanto adicionada ao projeto "Intranet", que está em aberto
- **When** o gestor retoma o projeto "Website" (estado "em andamento")
- **Then** o projeto "Website" é retomado, a Ana continua na lista de membros com uma etiqueta vermelha, e o gestor vê um aviso a indicar que a Ana está noutro projeto.

**Cenário 1.9: datas obrigatórias**
- **Given** um gestor de projeto identificado
- **When** tenta criar um projeto sem data de início ou sem data de fim
- **Then** o projeto não é criado e é indicado que as datas são obrigatórias.

**Cenário 1.10: data de fim não anterior à de início**
- **Given** um gestor de projeto identificado
- **When** tenta criar um projeto com data de início 2026-01-31 e data de fim 2026-01-01
- **Then** o projeto não é criado e é indicado que a data de fim não pode ser anterior à de início.
- **When** cria um projeto com data de início 2026-01-31 e data de fim 2026-01-31, ambas sem hora
- **Then** o projeto é criado, porque o dia de fim conta inteiro.

**Cenário 1.11: remover um membro**
- **Given** a membro "Ana" na equipa do projeto "Website", que está em aberto, sem tarefas por concluir atribuídas nesse projeto
- **When** o gestor remove a Ana do projeto
- **Then** a Ana deixa de fazer parte da equipa e pode ser adicionada a outro projeto em aberto.

**Cenário 1.12: remover um membro com tarefas por concluir, com substituto**
- **Given** a membro "Ana" na equipa do projeto "Website", com uma tarefa "por fazer" atribuída nesse projeto, e o membro "Rui", que não está em nenhum projeto em aberto
- **When** o gestor remove a Ana e adiciona o Rui como novo membro
- **Then** a Ana deixa de fazer parte da equipa, o Rui passa a fazer parte dela e fica com a tarefa que era da Ana.

**Cenário 1.13: remoção sem novo membro**
- **Given** a membro "Ana" na equipa do projeto "Website", com uma tarefa "por fazer" atribuída nesse projeto
- **When** o gestor remove a Ana sem adicionar um novo membro
- **Then** a remoção não é concluída, é mostrado um aviso a pedir um novo membro, e a Ana continua na equipa com a tarefa.

**Cenário 1.14: novo membro já está noutro projeto**
- **Given** o cenário anterior, e o membro "Rui" na equipa do projeto "Intranet", que está em aberto
- **When** o gestor tenta adicionar o Rui como novo membro do projeto "Website"
- **Then** a ação é recusada, com uma mensagem a explicar que o Rui já está noutro projeto em aberto (R11), e a remoção da Ana não é concluída.

**Cenário 1.15: tamanho máximo do nome e da descrição do projeto**
- **Given** um gestor de projeto identificado
- **When** cria um projeto com um nome de 100 caracteres e uma descrição de 2000 caracteres
- **Then** o projeto é criado.
- **When** tenta criar um projeto com um nome de 101 caracteres ou uma descrição de 2001 caracteres
- **Then** o projeto não é criado e é indicado o limite excedido.

**Cenário 1.16: a etiqueta vermelha desaparece**
- **Given** o projeto "Website" retomado, com a Ana na lista de membros com etiqueta vermelha por estar também no projeto "Intranet", que está em aberto
- **When** o projeto "Intranet" é concluído
- **Then** a Ana passa a estar em um só projeto em aberto e a etiqueta vermelha desaparece.

**Cenário 1.17: gestor com tarefas em vários projetos**
- **Given** a gestora "Marta", gestora do projeto "Website" e responsável por uma tarefa desse projeto, e o projeto "Intranet", de outro gestor, que está em aberto
- **When** o gestor do projeto "Intranet" adiciona a Marta como membro e lhe atribui uma tarefa
- **Then** a atribuição é aceite, porque a regra de um projeto em aberto por membro não se aplica ao gestor.

**Cenário 1.19: o criador indica o gestor do projeto**
- **Given** os gestores "Marta" e "Pedro"
- **When** a Marta cria o projeto "Website" e indica o Pedro como gestor
- **Then** o Pedro é o gestor do projeto "Website", o projeto aparece na lista do Pedro e não aparece na lista da Marta.

**Cenário 1.20: fuso horário por defeito**
- **Given** um gestor de projeto identificado
- **When** cria um projeto sem indicar o fuso horário
- **Then** o projeto fica com o fuso horário de Lisboa.

**Cenário 1.18: "atenção" e "atrasado" não são definidos pelo gestor**
- **Given** um gestor de projeto identificado e um projeto "em andamento"
- **When** o gestor abre as opções de estado do projeto
- **Then** os estados "atenção" e "atrasado" não estão disponíveis para escolha.

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

**Cenário 2.5: prazo igual à data atual**
- **Given** a data atual 2026-11-10
- **When** o membro cria uma tarefa com prazo 2026-11-10
- **Then** a tarefa é criada.

**Cenário 2.6: tamanho máximo de título e descrição**
- **Given** um membro da equipa a criar uma tarefa
- **When** guarda uma tarefa com um título de 100 caracteres e uma descrição de 2000 caracteres
- **Then** a tarefa é criada.
- **When** tenta guardar uma tarefa com um título de 101 caracteres ou uma descrição de 2001 caracteres
- **Then** a tarefa não é criada e é indicado o limite excedido.

**Cenário 2.7: editar o prazo para o passado**
- **Given** a data atual 2026-11-10 e uma tarefa com prazo 2026-12-15
- **When** o membro altera o prazo da tarefa para 2026-11-09
- **Then** a alteração é recusada, com uma mensagem a indicar que o prazo não pode estar no passado, e o prazo mantém-se em 2026-12-15.

**Cenário 2.8: texto mostrado literalmente**
- **Given** um membro da equipa a criar uma tarefa
- **When** guarda a tarefa com o título `<b>x</b>`
- **Then** o título aparece exatamente como `<b>x</b>`, sem formatação nem execução de código.

**Cenário 2.9: atribuir a quem não é membro do projeto**
- **Given** uma tarefa nova num projeto e um utilizador "Luís" que não é membro desse projeto
- **When** o membro tenta atribuir a tarefa ao Luís
- **Then** a atribuição é recusada, com uma mensagem a pedir ao gestor que adicione primeiro o Luís ao projeto, e o Luís não fica como responsável.
- **When** o gestor adiciona o Luís ao projeto e a tarefa é depois atribuída a ele
- **Then** a atribuição é aceite e o Luís aparece como responsável da tarefa.

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

**Cenário 3.4: subtarefas dentro da tarefa-mãe**
- **Given** uma tarefa A com duas subtarefas, ambas "por fazer", num projeto
- **When** o membro abre a Matriz do projeto
- **Then** aparece a tarefa A e não aparecem as duas subtarefas; ao abrir a tarefa A, as duas subtarefas são visíveis dentro dela.

**Cenário 3.5: filtro "as minhas" e subtarefas**
- **Given** uma tarefa A com responsável Rui, e uma subtarefa a1 da tarefa A com responsável Ana
- **When** a Ana ativa o filtro "as minhas" na Matriz
- **Then** aparece a tarefa A, porque a Ana é responsável por uma das suas subtarefas.

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

**Cenário 4.5: editar o prazo da subtarefa**
- **Given** uma tarefa-mãe com prazo 2026-12-15 e uma subtarefa com prazo 2026-12-10
- **When** o membro altera o prazo da subtarefa para 2026-12-20
- **Then** a alteração é recusada, com uma mensagem a indicar que o prazo excede o da tarefa-mãe, e o prazo da subtarefa mantém-se em 2026-12-10.

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

**Cenário 5.4: qualquer membro passa tarefas e subtarefas a estados finais**
- **Given** uma subtarefa "em curso" e uma tarefa sem subtarefas "em revisão"
- **When** um membro da equipa passa a subtarefa para "concluído" e a tarefa para "cancelado"
- **Then** ambas as mudanças são aceites.

### US06 — Progresso e estado do projeto

Regras usadas (ver [P5](constraints.md#2-pressupostos-assumidos) e [P16](constraints.md#2-pressupostos-assumidos)): o progresso esperado é o tempo decorrido, (agora − início) ÷ (fim − início), no fuso horário do projeto ([P26](constraints.md#2-pressupostos-assumidos)); o desvio é o progresso esperado menos o progresso real, em pontos percentuais; o estado é calculado só para projetos "em andamento".

**Cenário 6.1: cálculo da barra**
- **Given** um projeto com 10 tarefas: 4 concluídas, 2 em curso, 2 por fazer, 1 cancelada e 1 duplicada
- **When** o gestor abre a vista global
- **Then** a barra de progresso desse projeto mostra 50% (4 de 8, sem contar canceladas e duplicadas).

**Cenário 6.2: projeto sem tarefas contáveis**
- **Given** um projeto sem tarefas, ou só com tarefas canceladas e duplicadas
- **When** o gestor abre a vista global
- **Then** o projeto mostra a etiqueta "sem tarefas" em vez de uma percentagem, e não ocorre erro.

**Cenário 6.3: estado "atenção" (desvio de 15 pontos percentuais)**
- **Given** um projeto "em andamento" com data de início 2026-01-01 e data de fim 2026-01-31, ambas sem hora (31 dias no total), a 2026-01-16 às 12:00 (50% do tempo decorrido) e com 35% de progresso (desvio de 15 pontos percentuais)
- **When** o gestor abre a vista global
- **Then** o projeto aparece com o estado "atenção".

**Cenário 6.4: desvio abaixo do limite**
- **Given** o mesmo projeto, a 2026-01-16 às 12:00, com 36% de progresso (desvio de 14 pontos percentuais)
- **When** o gestor abre a vista global
- **Then** o projeto mantém o estado "em andamento".

**Cenário 6.5: estado "atrasado" por desvio (25 pontos percentuais)**
- **Given** o mesmo projeto, a 2026-01-16 às 12:00, com 25% de progresso (desvio de 25 pontos percentuais)
- **When** o gestor abre a vista global
- **Then** o projeto aparece com o estado "atrasado".

**Cenário 6.6: estado "atrasado" por data de fim ultrapassada**
- **Given** um projeto "em andamento" com data de fim 2026-01-31, a 2026-02-02, com 80% de progresso
- **When** o gestor abre a vista global
- **Then** o projeto aparece com o estado "atrasado".

**Cenário 6.7: membro não vê a vista global**
- **Given** um membro da equipa identificado
- **When** abre a aplicação
- **Then** a vista global dos projetos não está disponível.

**Cenário 6.8: projeto suspenso na vista global**
- **Given** um projeto no estado "suspenso"
- **When** o gestor abre a vista global
- **Then** o projeto aparece com o estado "suspenso" e a sua barra de progresso, e os estados "atenção" e "atrasado" não lhe são atribuídos por cálculo.

**Cenário 6.9: projeto concluído fora da vista global**
- **Given** um projeto no estado "concluído"
- **When** o gestor abre a vista global
- **Then** o projeto não aparece.

**Cenário 6.10: projeto sem tarefas depois da data de fim**
- **Given** um projeto "em andamento" sem tarefas, com data de fim 2026-01-31, a 2026-02-02
- **When** o gestor abre a vista global
- **Then** o projeto mostra a etiqueta "sem tarefas" e não lhe é atribuído o estado "atrasado" por cálculo.

**Cenário 6.11: as subtarefas não contam no progresso**
- **Given** um projeto com 2 tarefas principais: uma "concluído" e outra "em curso", com 3 subtarefas das quais 2 estão "concluído"
- **When** o gestor abre a vista global
- **Then** a barra de progresso mostra 50% (1 de 2 tarefas principais).

**Cenário 6.12: arredondamento do progresso**
- **Given** um projeto com 3 tarefas principais, uma delas "concluído"
- **When** o gestor abre a vista global
- **Then** a barra de progresso mostra 33,33%.

**Cenário 6.13: projeto não iniciado fora do prazo**
- **Given** um projeto "não iniciado" com data de fim 2026-01-31, a 2026-02-02
- **When** o gestor abre a vista global
- **Then** o projeto mantém o estado "não iniciado".

**Cenário 6.14: data de fim com hora**
- **Given** um projeto "em andamento", com 90% de progresso e a data de fim 2026-01-31 às 18:00 no fuso horário do projeto
- **When** o gestor abre a vista global às 19:00 de 2026-01-31, no fuso horário do projeto
- **Then** o projeto aparece com o estado "atrasado".

**Cenário 6.15: data de fim sem hora**
- **Given** um projeto "em andamento", com 90% de progresso e a data de fim 2026-01-31 sem hora
- **When** o gestor abre a vista global às 23:00 de 2026-01-31, no fuso horário do projeto
- **Then** o projeto não aparece como "atrasado" por data de fim.
- **When** o gestor abre a vista global às 00:00 de 2026-02-01, no fuso horário do projeto
- **Then** o projeto aparece com o estado "atrasado".

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
- **Then** a ação de recuperar não está disponível; só o gestor a pode recuperar.

**Cenário 7.4: apagar uma tarefa-mãe com subtarefas em aberto**
- **Given** uma tarefa-mãe com uma subtarefa "em curso"
- **When** o membro tenta apagar a tarefa-mãe
- **Then** a ação é recusada, com uma mensagem a pedir que as subtarefas em aberto sejam fechadas ou apagadas primeiro, e a tarefa-mãe mantém-se.

**Cenário 7.5: apagar uma subtarefa**
- **Given** uma tarefa-mãe com uma subtarefa
- **When** o membro apaga a subtarefa
- **Then** a subtarefa desaparece da tarefa-mãe e fica na reciclagem.

**Cenário 7.6: prazo de 30 dias na reciclagem**
- **Given** um projeto na reciclagem há 29 dias
- **When** o gestor abre a reciclagem
- **Then** o projeto aparece e pode ser recuperado.
- **Given** um projeto na reciclagem há 31 dias
- **When** o gestor abre a reciclagem
- **Then** o projeto já não aparece e não pode ser recuperado.

### US08 — Visibilidade por projeto

**Cenário 8.1: só os projetos de que é membro**
- **Given** dois projetos, A (com a membro Ana) e B (com o membro Rui)
- **When** a Ana abre a aplicação
- **Then** só vê o projeto A e as suas tarefas.

**Cenário 8.2: acesso direto recusado**
- **Given** a membro Ana, que não faz parte do projeto B
- **When** tenta abrir o projeto B
- **Then** o acesso é recusado, com uma mensagem a indicar que não tem acesso a esse projeto, e nenhum dado do projeto é mostrado.

**Cenário 8.3: o gestor vê só os seus projetos**
- **Given** os gestores "Marta", do projeto "Website", e "Pedro", do projeto "Intranet"
- **When** a Marta abre a lista de projetos
- **Then** vê o projeto "Website" e não vê o projeto "Intranet".

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

**Cenário 9.4: só tarefas principais**
- **Given** um projeto com 2 tarefas principais, uma delas com 3 subtarefas
- **When** o membro abre a Lista do projeto
- **Then** aparecem as 2 tarefas principais e não aparecem as 3 subtarefas.

### Cenário de aceitação do MVP

**Cenário MVP-1: do projeto ao progresso**
- **Given** a data atual 2026-01-10, a gestora "Marta" e a membro "Ana", que não está em nenhum projeto em aberto
- **When** a Marta cria o projeto "Website" (início 2026-01-01, fim 2026-01-31, fase "iniciação", estado "não iniciado"), adiciona a Ana e passa o estado para "em andamento"
- **And** a Ana cria a tarefa "Escrever relatório" (prazo 2026-01-20, urgência 4, importância 4) e a tarefa "Rever texto" (prazo 2026-01-25, urgência 1, importância 3), e passa a primeira para "concluído"
- **Then** a Matriz mostra "Rever texto" no quadrante "Schedule" e não mostra "Escrever relatório"
- **And** a Lista mostra as duas tarefas, com "Rever texto" primeiro
- **And** a Marta vê na vista global o projeto "Website" com 50% de progresso e o estado "em andamento" (cerca de 30% do tempo decorrido).

## 4. Situações excecionais e casos-limite

| ID | Situação | Resultado esperado | Base |
|---|---|---|---|
| EC1 | Projeto sem tarefas, ou só com tarefas canceladas e duplicadas (denominador do progresso = 0) | Mostra a etiqueta "sem tarefas" e não ocorre erro (cenário 6.2) | [P7](constraints.md#2-pressupostos-assumidos), [P16](constraints.md#2-pressupostos-assumidos) |
| EC2 | Antecipar o prazo da mãe para antes de uma subtarefa | Alteração recusada com aviso (cenário 4.3) | [R3](requirements.md#4-regras-de-negócio) |
| EC3 | Concluir uma mãe cujas subtarefas estão todas canceladas ou duplicadas | Aceite, porque contam como fechadas (cenário 5.2) | [R5](requirements.md#4-regras-de-negócio) |
| EC4 | Marcar uma tarefa como "bloqueado" sem motivo | Mudança recusada até ser indicado o motivo | [R6](requirements.md#4-regras-de-negócio) |
| EC5 | Marcar uma tarefa como "duplicado" sem indicar de qual | Mudança recusada até ser indicada a tarefa | [R6](requirements.md#4-regras-de-negócio) |
| EC6 | (v2) Um utilizador sai da empresa | O membro é removido e as suas tarefas passam para quem o substitui | [P13](constraints.md#2-pressupostos-assumidos) |
| EC7 | Apagar um projeto que tem tarefas | Projeto e tarefas vão juntos para a reciclagem (cenário 7.1) | [R9](requirements.md#4-regras-de-negócio), [P12](constraints.md#2-pressupostos-assumidos) |
| EC8 | Um membro abre uma ligação direta para um projeto de que não é membro | Acesso recusado (cenário 8.2) | [R8](requirements.md#4-regras-de-negócio) |
| EC9 | Um membro tenta reabrir uma tarefa em estado final | A ação de reabrir não está disponível para o membro; o gestor reabre a tarefa e escolhe o estado para onde vai | [P8](constraints.md#2-pressupostos-assumidos) |
| EC10 | Criar uma tarefa com prazo no passado | Tarefa não criada, com mensagem (cenário 2.4) | [R13](requirements.md#4-regras-de-negócio) |
| EC11 | Desvio exatamente igual a 15 ou a 25 pontos percentuais | 15 passa a "atenção" (cenário 6.3) e 25 passa a "atrasado" (cenário 6.5) | [P5](constraints.md#2-pressupostos-assumidos) |
| EC12 | Adicionar a um projeto um membro que já está noutro projeto em aberto | Ação recusada com aviso (cenário 1.5) | [R11](requirements.md#4-regras-de-negócio) |
| EC13 | Adicionar a um projeto um membro cujo projeto anterior foi concluído ou suspenso | Aceite, porque esses projetos não estão em aberto (cenário 1.7) | [R11](requirements.md#4-regras-de-negócio), [P20](constraints.md#2-pressupostos-assumidos) |
| EC14 | Retomar um projeto suspenso cujos membros já estão noutro projeto em aberto | Retoma aceite, com etiqueta vermelha nesses membros e aviso ao gestor (cenário 1.8) | [P22](constraints.md#2-pressupostos-assumidos) |
| EC15 | Remover um membro que ainda tem tarefas por concluir atribuídas | A remoção só é concluída se o gestor adicionar um novo membro, que fica com as tarefas (cenários 1.12 e 1.13) | [R17](requirements.md#4-regras-de-negócio), [P24](constraints.md#2-pressupostos-assumidos) |
| EC16 | Projeto sem tarefas com a data de fim ultrapassada | Mantém a etiqueta "sem tarefas", sem estado calculado (cenário 6.10) | [P16](constraints.md#2-pressupostos-assumidos) |
| EC17 | Criar um projeto com data de fim igual ou anterior à data de início | Projeto não criado, com indicação (cenário 1.10) | [R16](requirements.md#4-regras-de-negócio), [P23](constraints.md#2-pressupostos-assumidos) |
| EC18 | O novo membro de uma remoção já está noutro projeto em aberto, ou já está no projeto | Ação recusada; a remoção não é concluída (cenário 1.14) | [R11](requirements.md#4-regras-de-negócio), [P24](constraints.md#2-pressupostos-assumidos) |
| EC19 | Marcar uma tarefa como "duplicado" indicando um original de outro projeto, a própria tarefa ou uma tarefa já "duplicado" | Mudança recusada até ser indicado um original válido | [R6](requirements.md#4-regras-de-negócio) |
| EC20 | Projeto cujas tarefas principais têm subtarefas concluídas | As subtarefas não contam no progresso (cenário 6.11) | [R18](requirements.md#4-regras-de-negócio) |
| EC21 | Apagar uma tarefa-mãe com subtarefas em aberto | Ação recusada, com mensagem (cenário 7.4) | [R9](requirements.md#4-regras-de-negócio) |
| EC22 | Editar o prazo de uma tarefa para o passado, ou o de uma subtarefa para depois do da mãe | Alteração recusada, com mensagem (cenários 2.7 e 4.5) | [R2](requirements.md#4-regras-de-negócio), [R13](requirements.md#4-regras-de-negócio) |
| EC23 | Item na reciclagem há mais de 30 dias | Eliminado definitivamente e já não se recupera (cenário 7.6) | [R20](requirements.md#4-regras-de-negócio) |
| EC24 | Projeto "não iniciado" com a data de fim ultrapassada | Mantém o estado "não iniciado" (cenário 6.13) | [P5](constraints.md#2-pressupostos-assumidos) |
| EC25 | Gestor com tarefas em vários projetos em aberto | Aceite (cenário 1.17) | [R11](requirements.md#4-regras-de-negócio), [R19](requirements.md#4-regras-de-negócio) |
| EC26 | Título ou descrição com texto que parece código ou formatação | Mostrado literalmente (cenário 2.8) | [NF09](requirements.md#5-requisitos-não-funcionais) |
| EC27 | O gestor tenta definir o estado "atenção" ou "atrasado" | Não está disponível (cenário 1.18) | [R12](requirements.md#4-regras-de-negócio) |
| EC28 | Atribuir uma tarefa a alguém que não é membro do projeto | Recusado, com mensagem a pedir que a pessoa seja primeiro adicionada ao projeto (cenário 2.9) | [R21](requirements.md#4-regras-de-negócio) |

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
| A9 | "Recusada" e "não disponível" | Definidos na secção 1 ([NF08](requirements.md#5-requisitos-não-funcionais)) |
| A10 | Quando se aplicam R2 e R13 | Sempre que o prazo é definido ou alterado (cenários 2.7 e 4.5) |
| A11 | Apagar tarefas-mãe, estados finais e subtarefas apagadas | [R9](requirements.md#4-regras-de-negócio), [P8](constraints.md#2-pressupostos-assumidos) |
| A12 | Arredondamento do progresso | Duas casas decimais (cenário 6.12) |

### Cenários que dependem de pressupostos por validar
| Cenários | Pressuposto | O que depende dele |
|---|---|---|
| 1.9, 1.10 | [P23](constraints.md#2-pressupostos-assumidos) e [P26](constraints.md#2-pressupostos-assumidos) | Datas obrigatórias; um projeto de um só dia, sem hora, é válido |
| 6.3, 6.4, 6.5 | [P26](constraints.md#2-pressupostos-assumidos) | O dia de fim sem hora conta inteiro, o que dá 31 dias a 1-31 de janeiro |
| 6.15 | [P26](constraints.md#2-pressupostos-assumidos) | O dia de fim sem hora conta até ao fim desse dia |
| MVP-1 | [P26](constraints.md#2-pressupostos-assumidos) | O tempo decorrido é cerca de 30% |
| 1.4 a 1.8, 1.11 a 1.14, 1.17, 1.19 | [P21](constraints.md#2-pressupostos-assumidos) | Os utilizadores a adicionar já existem previamente (v1) |

O [P14](constraints.md#2-pressupostos-assumidos) (conteúdo da auditoria) também está por validar, mas nenhum cenário da v1 depende dele.

### Adiado por decisão da autora
- Regras de palavra-passe e recuperação de acesso: não se estipulam para já.
- Conteúdo, antecedência e repetição dos lembretes e alertas ([F12](requirements.md#3-requisitos-funcionais)): ficam para a user story da v2.
- Conteúdo do registo de auditoria ([F14](requirements.md#3-requisitos-funcionais)), além de [P14](constraints.md#2-pressupostos-assumidos): fica para a user story da v2.
- Login, etiquetas e arquivo (F10, F11 e F13): ficam para a v2.
- Anexos, comentários, Calendário e avisos por email ou SMS: v3.

### Por definir
- Ordem das tarefas dentro de cada grupo da Lista (abertas e fechadas): por definir.
- O nível 2 chama-se "medium", mas conta como "baixo" na Matriz: incoerência de nomenclatura a confirmar com o cliente.

A lista completa de questões em aberto está em [constraints.md](constraints.md#4-decisões-técnicas-pendentes).
