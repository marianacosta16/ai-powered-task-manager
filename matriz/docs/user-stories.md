# Matriz — User Stories e Critérios de Aceitação

> Converte os requisitos do MVP de [requirements.md](requirements.md) em especificações verificáveis.
> Não propõe tecnologias nem contém código.

## 1. Como ler este documento

- **Papéis:** gestor de projeto e membro da equipa ([requirements.md](requirements.md#2-tipos-de-utilizadores)).
- **Formato das stories:** *Como [tipo de utilizador], quero [funcionalidade], para [benefício].*
- **"Identificado":** um utilizador "identificado" é o que iniciou sessão com uma conta de demonstração ([F10](requirements.md#3-requisitos-funcionais), [S5](constraints.md#3-requisitos-de-segurança-e-privacidade)).
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
| US07 | Membro da equipa ou gestor de projeto | recuperar tarefas apagadas dos meus projetos e, sendo gestor, também projetos apagados | corrigir remoções acidentais | [F09](requirements.md#3-requisitos-funcionais), [R9](requirements.md#4-regras-de-negócio), [R20](requirements.md#4-regras-de-negócio) |
| US08 | Membro da equipa | ver apenas os projetos de que sou membro e as suas tarefas | trabalhar sem ver informação de outras equipas | [F06](requirements.md#3-requisitos-funcionais), [R8](requirements.md#4-regras-de-negócio) |
| US09 | Membro da equipa | ver todas as tarefas de um projeto numa lista, com as abertas primeiro e o filtro "as minhas" | ter uma visão completa do trabalho do projeto | [F05](requirements.md#3-requisitos-funcionais), [R15](requirements.md#4-regras-de-negócio), [R18](requirements.md#4-regras-de-negócio) |
| US10 | Membro da equipa ou gestor de projeto | iniciar e terminar sessão e consultar quem sou | aceder à aplicação com a minha conta | [F10](requirements.md#3-requisitos-funcionais), [S5](constraints.md#3-requisitos-de-segurança-e-privacidade), [P21](constraints.md#2-pressupostos-assumidos) |
| US11 | Membro da equipa | adicionar, editar e apagar os meus comentários numa tarefa e ler os dos colegas | discutir o trabalho com a equipa | [F17](requirements.md#3-requisitos-funcionais), [R24](requirements.md#4-regras-de-negócio), [P28](constraints.md#2-pressupostos-assumidos) |
| US12 | Membro da equipa ou gestor de projeto | pesquisar projetos pelo nome e filtrar as tarefas por estado e por quadrante | encontrar depressa o que procuro | [F22](requirements.md#3-requisitos-funcionais), [R25](requirements.md#4-regras-de-negócio), [P29](constraints.md#2-pressupostos-assumidos) |
| US13 | Membro da equipa ou gestor de projeto | ver ao entrar os meus projetos com as contagens de tarefas por estado e ir ao detalhe | saber onde está o trabalho | [F07](requirements.md#3-requisitos-funcionais), [R8](requirements.md#4-regras-de-negócio), [R18](requirements.md#4-regras-de-negócio), [P27](constraints.md#2-pressupostos-assumidos) |

### Prioridades

A prioridade de cada story é a da funcionalidade principal em que se baseia (MoSCoW). Todas as stories atuais são da v1 (MVP).

| Prioridade | Versão | Stories | Funcionalidades |
|---|---|---|---|
| **Must** | v1 | US01 a US13 | F01 a F10, F17 e F22 |

Todas as funcionalidades da v1 têm pelo menos uma user story: F01 (US01), F02 e F03 (US02), F04 (US03), F05 (US09), F06 (US08), F07 (US06 e US13), F08 (US04 e US05), F09 (US07), F10 (US10), F17 (US11) e F22 (US12).

As funcionalidades da v2 (Should) ainda não têm user story: F11 (etiquetas), F12 (lembretes), F13 (arquivo), F14 (auditoria) e F23 (criação de contas). Ficam para a ronda seguinte.

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
- **Given** a data atual 2026-01-01 e um gestor de projeto identificado
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

**Cenário 1.13: remoção sem substituto**
- **Given** a membro "Ana" na equipa do projeto "Website", com uma tarefa "por fazer" atribuída nesse projeto
- **When** o gestor remove a Ana sem indicar um substituto para a tarefa
- **Then** a remoção não é concluída, é mostrado um aviso a pedir um substituto, e a Ana continua na equipa com a tarefa.

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

**Cenário 1.18: "atenção" e "atrasado" não são definidos pelo gestor**
- **Given** um gestor de projeto identificado e um projeto "em andamento"
- **When** o gestor abre as opções de estado do projeto
- **Then** os estados "atenção" e "atrasado" não estão disponíveis para escolha.

**Cenário 1.19: o criador indica o gestor do projeto**
- **Given** os gestores "Marta" e "Pedro"
- **When** a Marta cria o projeto "Website" e indica o Pedro como gestor
- **Then** o Pedro é o gestor do projeto "Website", o projeto aparece na lista do Pedro e não aparece na lista da Marta.

**Cenário 1.20: fuso horário por defeito**
- **Given** um gestor de projeto identificado
- **When** cria um projeto sem indicar o fuso horário
- **Then** o projeto fica com o fuso horário de Lisboa.

**Cenário 1.21: um projeto concluído fica congelado**
- **Given** um gestor de projeto identificado e um projeto no estado "concluído", com membros e tarefas
- **When** o gestor abre o projeto
- **Then** não estão disponíveis as ações de editar os dados do projeto, mudar o estado, adicionar ou remover membros, nem apagar o projeto, e tudo continua visível como estava.
- **When** é pedida uma dessas ações por outra via
- **Then** a ação é recusada, com uma mensagem a explicar que um projeto concluído não pode ser alterado nem reaberto, e nada muda.

**Cenário 1.22: confirmação antes de concluir um projeto**
- **Given** um gestor de projeto identificado e um projeto "em andamento"
- **When** o gestor escolhe o estado "concluído"
- **Then** é pedida uma confirmação, a avisar que o projeto deixa de poder ser alterado ou reaberto.
- **When** o gestor cancela
- **Then** o projeto mantém o estado "em andamento".
- **When** o gestor volta a escolher "concluído" e confirma
- **Then** o projeto passa a "concluído".

**Cenário 1.23: data de início não está no passado**
- **Given** a data atual 2026-01-10 e um gestor de projeto identificado
- **When** tenta criar um projeto com data de início 2026-01-09
- **Then** o projeto não é criado e é indicado que a data de início não pode estar no passado.
- **When** cria um projeto com data de início 2026-01-10
- **Then** o projeto é criado.

**Cenário 1.24: substituto já no projeto**
- **Given** a membro "Ana" com 2 tarefas em aberto no projeto "Website", e o membro "Rui", também do projeto, com 1 tarefa em aberto
- **When** o gestor remove a Ana e passa as 2 tarefas dela para o Rui
- **Then** a Ana deixa de fazer parte da equipa e o Rui fica com 3 tarefas em aberto.

**Cenário 1.25: substituto com mais de 5 tarefas em aberto**
- **Given** a membro "Ana" com 3 tarefas em aberto no projeto "Website", e o membro "Rui", também do projeto, com 3 tarefas em aberto
- **When** o gestor tenta remover a Ana e passar as 3 tarefas dela para o Rui
- **Then** a remoção é recusada, com uma mensagem a explicar que o Rui ficaria com mais de 5 tarefas em aberto, e a Ana continua na equipa com as suas tarefas.

**Cenário 1.26: repartir as tarefas por vários substitutos**
- **Given** a membro "Ana" com 4 tarefas em aberto no projeto "Website", e os membros "Rui" e "Sofia", também do projeto, cada um com 3 tarefas em aberto
- **When** o gestor remove a Ana e passa 2 tarefas dela para o Rui e 2 para a Sofia
- **Then** a Ana deixa de fazer parte da equipa e o Rui e a Sofia ficam com 5 tarefas em aberto cada.

**Cenário 1.27: tarefa partilhada com outro responsável**
- **Given** a membro "Ana" e o membro "Rui", ambos responsáveis pela tarefa "Escrever relatório" do projeto "Website", e a membro "Sofia", também do projeto
- **When** o gestor tenta remover a Ana sem indicar um substituto para essa tarefa
- **Then** a remoção não é concluída e é mostrado um aviso a pedir um substituto.
- **When** o gestor remove a Ana e indica a Sofia como substituta
- **Then** a tarefa fica com o Rui e a Sofia como responsáveis.

**Cenário 1.28: as subtarefas contam para o limite**
- **Given** a membro "Ana" com 1 tarefa em aberto no projeto "Website", e o membro "Rui", também do projeto, com 4 tarefas e 1 subtarefa em aberto
- **When** o gestor tenta remover a Ana e passar a tarefa dela para o Rui
- **Then** a remoção é recusada, com uma mensagem a explicar que o Rui ficaria com mais de 5 tarefas em aberto, e a Ana continua na equipa.

**Cenário 1.29: adicionar um membro a um projeto suspenso**
- **Given** o projeto "Website" no estado "suspenso" e a membro "Ana" na equipa do projeto "Intranet", que está em aberto
- **When** o gestor adiciona a Ana ao projeto "Website"
- **Then** a Ana é adicionada, porque o projeto "Website" não está em aberto.
- **When** o gestor retoma o projeto "Website" (estado "em andamento")
- **Then** a Ana fica com a etiqueta vermelha e o gestor vê um aviso, como no cenário 1.8.

**Cenário 1.30: remover um membro de um projeto suspenso**
- **Given** o projeto "Website" no estado "suspenso" e a membro "Ana" na sua equipa, sem tarefas por concluir
- **When** o gestor remove a Ana
- **Then** a Ana deixa de fazer parte da equipa.

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

**Cenário 2.10: criar tarefa num projeto concluído ou suspenso**
- **Given** um projeto "concluído" e um projeto "suspenso", dos quais um membro da equipa é membro
- **When** o membro abre o projeto concluído, ou o projeto suspenso
- **Then** não aparece o controlo para criar uma tarefa nem para criar uma subtarefa.
- **When** é pedida a criação de uma tarefa, ou de uma subtarefa de uma tarefa existente, num desses projetos por outra via (por exemplo, por ligação direta)
- **Then** a criação é recusada, com uma mensagem a explicar que não se criam tarefas num projeto concluído ou suspenso, e o projeto fica sem a nova tarefa.
- **When** o gestor retoma o projeto suspenso (estado "em andamento") e o membro o abre
- **Then** o controlo para criar tarefas aparece e a tarefa pode ser criada.

**Cenário 2.11: remover responsáveis**
- **Given** uma tarefa com os responsáveis Ana e Rui
- **When** o membro remove a atribuição da Ana
- **Then** a tarefa fica só com o Rui como responsável.
- **When** o membro tenta remover a atribuição do Rui, que é o último responsável
- **Then** a remoção é recusada, com uma mensagem a pedir que se atribua primeiro outra pessoa, e o Rui continua responsável.

**Cenário 2.12: tamanho do motivo de bloqueio**
- **Given** uma tarefa "em curso"
- **When** o membro a passa para "bloqueado" com um motivo de 100 caracteres
- **Then** a mudança é aceite.
- **When** o membro tenta passá-la para "bloqueado" com um motivo de 101 caracteres
- **Then** a mudança é recusada, com indicação do limite, e a tarefa mantém o estado anterior.

**Cenário 2.13: alterar tarefas num projeto suspenso ou concluído**
- **Given** um projeto "suspenso" e um projeto "concluído", com tarefas, dos quais um membro da equipa é membro
- **When** o membro edita o título de uma tarefa, muda o estado de outra e apaga uma terceira no projeto suspenso
- **Then** as três ações são aceites.
- **When** o membro abre uma tarefa do projeto concluído
- **Then** vê os dados da tarefa, mas não estão disponíveis as ações de editar, mudar de estado, atribuir ou remover responsáveis, nem apagar.
- **When** é pedida uma dessas ações por outra via
- **Then** a ação é recusada, com uma mensagem a explicar que o projeto está concluído, e a tarefa não muda.

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

**Cenário 3.6: ordem das tarefas no quadrante**
- **Given** no quadrante "urgente e importante" as tarefas A (prazo 2026-12-20, urgência 4, importância 4), B (prazo 2026-12-10, urgência 3, importância 3), C (prazo 2026-12-10, urgência 4, importância 3) e D (prazo 2026-12-10, urgência 4, importância 4)
- **When** o membro abre a Matriz do projeto
- **Then** as tarefas aparecem pela ordem D, C, B, A: primeiro as de prazo mais próximo e, com o mesmo prazo, a de urgência mais alta e depois a de importância mais alta.

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
- **When** o gestor abre o painel inicial
- **Then** a barra de progresso desse projeto mostra 50% (4 de 8, sem contar canceladas e duplicadas).

**Cenário 6.2: projeto sem tarefas contáveis**
- **Given** um projeto sem tarefas, ou só com tarefas canceladas e duplicadas
- **When** o gestor abre o painel inicial
- **Then** o projeto mostra a etiqueta "sem tarefas" em vez de uma percentagem, e não ocorre erro.

**Cenário 6.3: estado "atenção" (desvio de 15 pontos percentuais)**
- **Given** um projeto "em andamento" com data de início 2026-01-01 e data de fim 2026-01-31, ambas sem hora (31 dias no total), a 2026-01-16 às 12:00 (50% do tempo decorrido) e com 35% de progresso (desvio de 15 pontos percentuais)
- **When** o gestor abre o painel inicial
- **Then** o projeto aparece com o estado "atenção".

**Cenário 6.4: desvio abaixo do limite**
- **Given** o mesmo projeto, a 2026-01-16 às 12:00, com 36% de progresso (desvio de 14 pontos percentuais)
- **When** o gestor abre o painel inicial
- **Then** o projeto mantém o estado "em andamento".

**Cenário 6.5: estado "atrasado" por desvio (25 pontos percentuais)**
- **Given** o mesmo projeto, a 2026-01-16 às 12:00, com 25% de progresso (desvio de 25 pontos percentuais)
- **When** o gestor abre o painel inicial
- **Then** o projeto aparece com o estado "atrasado".

**Cenário 6.6: estado "atrasado" por data de fim ultrapassada**
- **Given** um projeto "em andamento" com data de fim 2026-01-31, a 2026-02-02, com 80% de progresso
- **When** o gestor abre o painel inicial
- **Then** o projeto aparece com o estado "atrasado".

**Cenário 6.7: membro não vê a barra de progresso nem o estado calculado**
- **Given** um membro da equipa identificado, num projeto com tarefas
- **When** abre o painel inicial
- **Then** vê o projeto com as contagens de tarefas por estado, mas sem a barra de progresso nem o estado calculado.

**Cenário 6.8: projeto suspenso no painel inicial**
- **Given** um projeto no estado "suspenso"
- **When** o gestor abre o painel inicial
- **Then** o projeto aparece com o estado "suspenso" e a sua barra de progresso, e os estados "atenção" e "atrasado" não lhe são atribuídos por cálculo.

**Cenário 6.9: projeto concluído fora do painel inicial**
- **Given** um projeto no estado "concluído"
- **When** o gestor, ou um membro do projeto, abre o painel inicial
- **Then** o projeto não aparece.

**Cenário 6.10: projeto sem tarefas depois da data de fim**
- **Given** um projeto "em andamento" sem tarefas, com data de fim 2026-01-31, a 2026-02-02
- **When** o gestor abre o painel inicial
- **Then** o projeto mostra a etiqueta "sem tarefas" e não lhe é atribuído o estado "atrasado" por cálculo.

**Cenário 6.11: as subtarefas não contam no progresso**
- **Given** um projeto com 2 tarefas principais: uma "concluído" e outra "em curso", com 3 subtarefas das quais 2 estão "concluído"
- **When** o gestor abre o painel inicial
- **Then** a barra de progresso mostra 50% (1 de 2 tarefas principais).

**Cenário 6.12: arredondamento do progresso**
- **Given** um projeto com 3 tarefas principais, uma delas "concluído"
- **When** o gestor abre o painel inicial
- **Then** a barra de progresso mostra 33,33%.

**Cenário 6.13: projeto não iniciado fora do prazo**
- **Given** um projeto "não iniciado" com data de fim 2026-01-31, a 2026-02-02
- **When** o gestor abre o painel inicial
- **Then** o projeto mantém o estado "não iniciado".

**Cenário 6.14: data de fim com hora**
- **Given** um projeto "em andamento", com 90% de progresso e a data de fim 2026-01-31 às 18:00 no fuso horário do projeto
- **When** o gestor abre o painel inicial às 19:00 de 2026-01-31, no fuso horário do projeto
- **Then** o projeto aparece com o estado "atrasado".

**Cenário 6.15: data de fim sem hora**
- **Given** um projeto "em andamento", com 90% de progresso e a data de fim 2026-01-31 sem hora
- **When** o gestor abre o painel inicial às 23:00 de 2026-01-31, no fuso horário do projeto
- **Then** o projeto não aparece como "atrasado" por data de fim.
- **When** o gestor abre o painel inicial às 00:00 de 2026-02-01, no fuso horário do projeto
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

**Cenário 7.3: o que cada papel recupera**
- **Given** a gestora "Marta" dos projetos "Website" e "Intranet", a membro "Ana" (membro do "Website" e não do "Intranet"), a tarefa T1 apagada do projeto "Website", e o projeto "Intranet" apagado, ambos na reciclagem
- **When** a Ana abre a reciclagem, pelo painel inicial
- **Then** vê a tarefa T1, que pode recuperar, e não vê o projeto "Intranet".
- **When** a Ana recupera a T1
- **Then** a T1 volta ao projeto "Website".
- **When** a Marta abre a reciclagem
- **Then** vê o projeto "Intranet" e pode recuperá-lo.

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

**Cenário 7.7: tarefa apagada antes do projeto**
- **Given** um projeto com as tarefas T1, T2 e T3, em que a T1 foi apagada por um membro e está na reciclagem
- **When** o gestor apaga o projeto e depois o recupera
- **Then** o projeto volta com as tarefas T2 e T3, e a T1 continua na reciclagem, onde pode ser recuperada.

**Cenário 7.8: subtarefa cuja tarefa-mãe foi eliminada de vez**
- **Given** uma tarefa-mãe com uma subtarefa que foi apagada antes dela, e a tarefa-mãe apagada há 31 dias (eliminada definitivamente)
- **When** o gestor tenta recuperar a subtarefa
- **Then** a subtarefa não pode ser recuperada, porque a tarefa-mãe já não existe.
- **Given** a mesma situação, mas com a tarefa-mãe apagada há 5 dias
- **When** o gestor recupera a tarefa-mãe
- **Then** a tarefa-mãe volta sem essa subtarefa, que continua na reciclagem e pode ser recuperada.

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

**Cenário 9.5: ordem das tarefas na Lista**
- **Given** um projeto com as tarefas em aberto T1 (prazo 2026-12-20) e T2 (prazo 2026-12-05), e as tarefas fechadas T3 "concluído" (prazo 2026-11-20) e T4 "cancelado" (prazo 2026-11-01)
- **When** o membro abre a Lista do projeto
- **Then** as tarefas aparecem pela ordem T2, T1, T4, T3: primeiro as em aberto e depois as fechadas, e dentro de cada grupo pelo prazo mais próximo.

### US10 — Iniciar e terminar sessão

**Cenário 10.1: início de sessão com sucesso**
- **Given** a conta de demonstração da Ana, membro da equipa, e o ecrã de início de sessão
- **When** a Ana inicia sessão com as credenciais corretas
- **Then** entra na aplicação e vê o painel inicial com o seu nome e o papel "membro da equipa".

**Cenário 10.2: credenciais incorretas**
- **Given** o ecrã de início de sessão
- **When** alguém tenta iniciar sessão com uma palavra-passe incorreta
- **Then** não entra e é mostrada uma mensagem a dizer que as credenciais não estão corretas, sem indicar qual dos dois dados falhou.

**Cenário 10.3: consultar o utilizador autenticado**
- **Given** a Ana com sessão iniciada
- **When** consulta o utilizador autenticado
- **Then** vê o seu nome e o seu papel.

**Cenário 10.4: terminar sessão**
- **Given** a Ana com sessão iniciada
- **When** termina a sessão
- **Then** volta ao ecrã de início de sessão e já não consegue abrir projetos.

**Cenário 10.5: acesso sem sessão**
- **Given** ninguém com sessão iniciada
- **When** é aberta uma ligação direta para um projeto
- **Then** é pedido o início de sessão e o projeto não é mostrado.

**Cenário 10.6: sessão sem atividade**
- **Given** a Ana com sessão iniciada, que deixou a aplicação sem qualquer atividade durante 3 horas
- **When** tenta abrir um projeto
- **Then** a sessão já terminou, é pedido o início de sessão, com uma mensagem a dizer que a sessão terminou (P33), e o projeto não é mostrado.

### US11 — Comentários nas tarefas

**Cenário 11.1: adicionar um comentário**
- **Given** a Ana, membro de um projeto, e a tarefa "Escrever relatório" desse projeto
- **When** a Ana adiciona o comentário "Falta rever o capítulo 2"
- **Then** o comentário fica na tarefa, com a Ana como autora e a data e hora em que foi criado.

**Cenário 11.2: listar os comentários**
- **Given** uma tarefa com três comentários, escritos às 09:00, às 10:00 e às 11:00 por pessoas diferentes
- **When** um membro do projeto abre os comentários da tarefa
- **Then** aparecem do das 09:00 para o das 11:00, cada um com autor e data.

**Cenário 11.3: só quem tem acesso ao projeto**
- **Given** um utilizador "Luís" que não é membro do projeto
- **When** tenta ver ou adicionar um comentário a uma tarefa desse projeto, por ligação direta
- **Then** o acesso é recusado e o comentário não é criado.

**Cenário 11.4: texto mostrado literalmente**
- **Given** um membro a adicionar um comentário
- **When** guarda o comentário `<b>x</b>`
- **Then** o comentário aparece exatamente como `<b>x</b>`, sem formatação nem execução de código.

**Cenário 11.5: tamanho máximo**
- **Given** um membro a adicionar um comentário
- **When** guarda um comentário com 2000 caracteres
- **Then** o comentário é criado.
- **When** tenta guardar um comentário com 2001 caracteres
- **Then** o comentário não é criado e é indicado o limite excedido.

**Cenário 11.6: editar e apagar o próprio comentário**
- **Given** uma tarefa com um comentário da Ana e um comentário do Rui
- **When** a Ana edita o seu comentário para "Capítulo 2 revisto"
- **Then** o comentário passa a mostrar esse texto e mantém a Ana como autora e a data de criação original.
- **When** a Ana tenta guardar uma edição com 2001 caracteres
- **Then** a edição é recusada, com indicação do limite, e o texto anterior mantém-se.
- **When** a Ana abre o comentário do Rui
- **Then** não estão disponíveis as ações de editar nem de apagar.
- **When** a Ana apaga o seu comentário
- **Then** o comentário deixa de aparecer na tarefa e não se recupera.

**Cenário 11.7: os comentários seguem a tarefa**
- **Given** uma tarefa com dois comentários
- **When** a tarefa é apagada e, depois, recuperada da reciclagem pelo gestor
- **Then** os comentários deixam de aparecer quando a tarefa é apagada e voltam a aparecer quando é recuperada.

**Cenário 11.8: comentários em subtarefas**
- **Given** uma tarefa com uma subtarefa "Enviar convites", num projeto em aberto
- **When** a Ana adiciona o comentário "Enviado" à subtarefa
- **Then** o comentário fica na subtarefa, com as mesmas regras dos comentários das tarefas (autor, data, edição e eliminação pelo autor).

**Cenário 11.9: comentários num projeto suspenso**
- **Given** um projeto "suspenso" com uma tarefa
- **When** a Ana adiciona um comentário, edita o seu comentário e apaga outro comentário seu
- **Then** as três ações são aceites.

**Cenário 11.10: comentários num projeto concluído**
- **Given** um projeto "concluído" com uma tarefa que já tem um comentário da Ana
- **When** a Ana abre a tarefa
- **Then** vê o comentário, mas não estão disponíveis as ações de adicionar, editar nem apagar comentários.
- **When** é pedida a criação, a edição ou a eliminação de um comentário desse projeto por outra via
- **Then** a ação é recusada, com uma mensagem a explicar que o projeto está concluído, e o comentário não muda.

### US12 — Pesquisa e filtros

**Cenário 12.1: pesquisar projetos pelo nome**
- **Given** a Ana, membro dos projetos "Website" e "Intranet"
- **When** pesquisa "web"
- **Then** aparece o projeto "Website" e não aparece o "Intranet".

**Cenário 12.2: a pesquisa só devolve projetos acessíveis**
- **Given** um projeto "Website Novo" de que o Luís não é membro
- **When** o Luís pesquisa "website"
- **Then** o projeto "Website Novo" não aparece.

**Cenário 12.3: pesquisa sem resultados**
- **Given** a Ana a pesquisar projetos
- **When** pesquisa um texto que não consta do nome de nenhum dos seus projetos
- **Then** não aparece nenhum projeto e é indicado que não há resultados.

**Cenário 12.4: filtrar tarefas por estado**
- **Given** a Lista de um projeto com as tarefas T1 "por fazer", T2 "em curso" e T3 "por fazer"
- **When** o membro filtra por "por fazer"
- **Then** aparecem T1 e T3 e não aparece T2.

**Cenário 12.5: filtrar tarefas por quadrante**
- **Given** a Lista de um projeto com as tarefas A (urgência 4, importância 4), B (urgência 1, importância 3) e C (urgência 3, importância 2)
- **When** o membro filtra pelo quadrante "Do first"
- **Then** aparece só a tarefa A.

**Cenário 12.6: combinar filtros**
- **Given** a Lista de um projeto com as tarefas T1 (Ana, "por fazer", urgência 4, importância 4), T2 (Ana, "em curso", urgência 4, importância 4) e T3 (Rui, "por fazer", urgência 4, importância 4)
- **When** a Ana ativa "as minhas", filtra por "por fazer" e pelo quadrante "Do first"
- **Then** aparece só a tarefa T1.

### US13 — Painel inicial

**Cenário 13.1: contagens por estado**
- **Given** a Ana, membro do projeto "Website", com 5 tarefas principais: 2 "por fazer", 1 "em curso", 1 "concluído" e 1 "cancelado"
- **When** abre o painel inicial
- **Then** vê o projeto "Website" com as contagens: por fazer 2, em curso 1, em revisão 0, bloqueado 0, concluído 1, cancelado 1 e duplicado 0.

**Cenário 13.2: só os projetos acessíveis**
- **Given** a Ana, membro do projeto "Website" e não membro do projeto "Intranet"
- **When** abre o painel inicial
- **Then** vê o projeto "Website" e não vê o projeto "Intranet".

**Cenário 13.3: navegar para o detalhe**
- **Given** o painel inicial da Ana com o projeto "Website"
- **When** a Ana seleciona o projeto "Website"
- **Then** abre o detalhe desse projeto, na vista Matriz.

**Cenário 13.4: subtarefas não contam**
- **Given** um projeto com 2 tarefas principais, uma delas com 3 subtarefas
- **When** um membro abre o painel inicial
- **Then** as contagens por estado somam 2 tarefas.

**Cenário 13.5: projeto concluído fora do painel**
- **Given** o projeto "Website" no estado "concluído", a gestora "Marta" e a membro "Ana", ambas com acesso ao projeto
- **When** a Marta abre o painel inicial
- **Then** o projeto "Website" não aparece.
- **When** a Ana abre o painel inicial
- **Then** o projeto "Website" não aparece.
- **When** a Marta, ou a Ana, pesquisa "Website" em Projetos
- **Then** o projeto "Website" aparece, com o estado "concluído".

### Cenário de aceitação do MVP

**Cenário MVP-1: do projeto ao progresso**
- **Given** a data atual 2026-01-01, as contas de demonstração da gestora "Marta" e da membro "Ana", que não está em nenhum projeto em aberto
- **When** a Marta inicia sessão, cria o projeto "Website" (início 2026-01-01, fim 2026-01-31, fase "iniciação", estado "não iniciado"), adiciona a Ana e passa o estado para "em andamento"
- **And** a Ana inicia sessão e cria a tarefa "Escrever relatório" (prazo 2026-01-20, urgência 4, importância 4) e a tarefa "Rever texto" (prazo 2026-01-25, urgência 1, importância 3)
- **And** a 2026-01-10 a Ana passa a primeira para "concluído"
- **And** a Ana adiciona à tarefa "Rever texto" o comentário "Falta o capítulo 2"
- **Then** a Matriz mostra "Rever texto" no quadrante "Schedule" e não mostra "Escrever relatório"
- **And** a Lista mostra as duas tarefas, com "Rever texto" primeiro, e ao filtrar a Lista pelo estado "por fazer" aparece só "Rever texto"
- **And** a pesquisa de projetos pelo nome "web" mostra o projeto "Website"
- **And** a Marta vê no painel inicial o projeto "Website" com 50% de progresso e o estado "em andamento" (cerca de 30% do tempo decorrido)
- **And** a Ana vê no painel inicial o projeto "Website" com as contagens por estado: por fazer 1, em curso 0, em revisão 0, bloqueado 0, concluído 1, cancelado 0 e duplicado 0
- **And** a tarefa "Rever texto" mostra o comentário "Falta o capítulo 2", com a Ana como autora e a data de criação.

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
| EC15 | Remover um membro que ainda tem tarefas ou subtarefas por concluir atribuídas, mesmo partilhadas | A remoção só é concluída se todas passarem para substitutos (cenários 1.12, 1.13 e 1.24 a 1.27) | [R17](requirements.md#4-regras-de-negócio), [P24](constraints.md#2-pressupostos-assumidos) |
| EC16 | Projeto sem tarefas com a data de fim ultrapassada | Mantém a etiqueta "sem tarefas", sem estado calculado (cenário 6.10) | [P16](constraints.md#2-pressupostos-assumidos) |
| EC17 | Criar um projeto com data de fim igual ou anterior à data de início | Projeto não criado, com indicação (cenário 1.10) | [R16](requirements.md#4-regras-de-negócio), [P23](constraints.md#2-pressupostos-assumidos) |
| EC18 | O substituto novo de uma remoção já está noutro projeto em aberto | Ação recusada; a remoção não é concluída (cenário 1.14) | [R11](requirements.md#4-regras-de-negócio), [P24](constraints.md#2-pressupostos-assumidos) |
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
| EC29 | Criar uma tarefa ou subtarefa num projeto concluído ou suspenso | O controlo de criar não aparece; um pedido por outra via é recusado, com mensagem (cenário 2.10) | [R22](requirements.md#4-regras-de-negócio) |
| EC30 | Remover o último responsável de uma tarefa | Recusado, com mensagem a pedir que se atribua primeiro outra pessoa (cenário 2.11) | [R26](requirements.md#4-regras-de-negócio) |
| EC31 | Abrir uma ligação direta sem ter iniciado sessão | É pedido o início de sessão e o conteúdo não é mostrado (cenário 10.5) | [F10](requirements.md#3-requisitos-funcionais), [S5](constraints.md#3-requisitos-de-segurança-e-privacidade) |
| EC32 | Comentário com mais de 2000 caracteres | Não é criado, com indicação do limite (cenário 11.5) | [R24](requirements.md#4-regras-de-negócio), [P28](constraints.md#2-pressupostos-assumidos) |
| EC33 | Pesquisar um projeto cujo nome não corresponde a nenhum acessível | Sem resultados, com indicação (cenários 12.2 e 12.3) | [R25](requirements.md#4-regras-de-negócio) |
| EC34 | Adicionar, editar ou apagar um comentário num projeto concluído | Os controlos não aparecem; um pedido por outra via é recusado, com mensagem (cenário 11.10) | [R24](requirements.md#4-regras-de-negócio), [P28](constraints.md#2-pressupostos-assumidos) |
| EC35 | Editar, mudar de estado ou apagar uma tarefa num projeto concluído | Os controlos não aparecem; um pedido por outra via é recusado, com mensagem. Num projeto suspenso estas ações são aceites (cenário 2.13) | [R27](requirements.md#4-regras-de-negócio) |
| EC36 | Um membro tenta recuperar um projeto apagado | A reciclagem não lhe mostra o projeto; só o gestor o recupera (cenário 7.3) | [R9](requirements.md#4-regras-de-negócio) |
| EC37 | Alterar, reabrir ou apagar um projeto concluído (dados, membros, estado) | Os controlos não aparecem; um pedido por outra via é recusado, com mensagem (cenário 1.21) | [R28](requirements.md#4-regras-de-negócio) |
| EC38 | Sessão sem atividade durante 3 horas | A sessão termina e é pedido o início de sessão, com uma mensagem a dizer que a sessão terminou (cenário 10.6, P33) | [S5](constraints.md#3-requisitos-de-segurança-e-privacidade) |
| EC39 | Marcar um projeto como concluído sem querer | É pedida uma confirmação antes; ao cancelar, o estado mantém-se (cenário 1.22) | [R28](requirements.md#4-regras-de-negócio) |
| EC40 | Criar ou alterar um projeto com data de início no passado | Recusado, com mensagem; o dia de hoje é aceite (cenário 1.23) | [R29](requirements.md#4-regras-de-negócio) |
| EC41 | Um substituto ficaria com mais de 5 tarefas e subtarefas em aberto | Ação recusada, com mensagem; a remoção não é concluída (cenários 1.25 e 1.28) | [R17](requirements.md#4-regras-de-negócio), [P24](constraints.md#2-pressupostos-assumidos) |
| EC42 | Adicionar a um projeto suspenso um membro que está noutro projeto em aberto | Aceite; ao retomar o projeto aplica-se a etiqueta vermelha (cenário 1.29) | [R11](requirements.md#4-regras-de-negócio), [P22](constraints.md#2-pressupostos-assumidos) |
| EC43 | Duas pessoas editam a mesma tarefa e gravam uma depois da outra | A segunda gravação é recusada, com uma mensagem a pedir para recarregar a tarefa e tentar outra vez; a primeira mantém-se (a verificar no incremento 3) | [R30](requirements.md#4-regras-de-negócio) |

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
Nenhum cenário da v1 depende de pressupostos por validar. Os pressupostos P21, P23, P26, P27, P28 e P29, de que alguns cenários dependem, foram confirmados pela autora.

O [P14](constraints.md#2-pressupostos-assumidos) (conteúdo da auditoria) também está por validar, mas nenhum cenário da v1 depende dele.

### Adiado por decisão da autora
- Regras de palavra-passe, recuperação de acesso e registo público: não se estipulam para já (extensões opcionais).
- Conteúdo, antecedência e repetição dos lembretes e alertas ([F12](requirements.md#3-requisitos-funcionais)): ficam para a user story da v2.
- Conteúdo do registo de auditoria ([F14](requirements.md#3-requisitos-funcionais)), além de [P14](constraints.md#2-pressupostos-assumidos): fica para a user story da v2.
- Criação de contas, etiquetas e arquivo (F23, F11 e F13): ficam para a v2.
- Anexos, Calendário e avisos por email ou SMS: v3.

### Por definir
- O nível 2 chama-se "medium", mas conta como "baixo" na Matriz: incoerência de nomenclatura a confirmar com o cliente.

A lista completa de questões em aberto está em [constraints.md](constraints.md#4-decisões-técnicas-pendentes).
