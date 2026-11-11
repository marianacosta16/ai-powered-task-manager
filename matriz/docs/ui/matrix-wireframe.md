# Matriz — Wireframe do ecrã Matrix

> Wireframe de baixa fidelidade do ecrã principal da aplicação: a vista **Matrix** de um projeto (F04). Não define estilo visual nem tecnologia.
> Documentos relacionados: [requirements.md](../requirements.md), [user-stories.md](../user-stories.md) e [constraints.md](../constraints.md).

## 1. Ecrã escolhido

A **vista Matrix de um projeto** é a vista principal da Matriz ([requirements.md](../requirements.md#32-vistas)). É o ecrã onde o membro e o gestor passam mais tempo, e mostra de uma só vez a identificação do utilizador, o projeto, a navegação entre vistas e as tarefas por urgência e importância. O painel inicial (F07) fica para outro wireframe.

**Como foi gerado:** o wireframe foi desenhado com IA (Claude) a partir dos documentos deste repositório, em SVG de baixa fidelidade, sem cores nem estilo. Os rótulos estão em inglês, como a interface (NF01). Os nomes e as tarefas são fictícios (C8).

## 2. Wireframe

![Wireframe do ecrã Matrix](matrix-wireframe.svg)

Ficheiro: [matrix-wireframe.svg](matrix-wireframe.svg). Os números do desenho correspondem aos da tabela seguinte. Os elementos de contorno tracejado só aparecem ao gestor do projeto.

## 3. Elementos do ecrã

| # | Elemento | O que mostra | Origem |
|---|---|---|---|
| 1 | Seletor de projeto | Os projetos que o utilizador pode ver: o membro vê os de que é membro (incluindo concluídos e suspensos); o gestor vê os de que é gestor | F06, R8 |
| 2 | Fase e estado do projeto | Fase (*Execution*) e estado (*In progress*), só para leitura neste ecrã. Os estados *Attention* e *Late* são calculados | F01, P3, P5, R12 |
| 3 | Utilizador autenticado e *Sign out* | Nome e papel de quem tem a sessão iniciada, e o botão para terminar a sessão | F10, S5 |
| 4 | Separadores *Matrix*, *List* e *Dashboard* | *Matrix* e *List* alternam entre as vistas do projeto (*Matrix* está ativo); *Dashboard* volta ao painel inicial | F04, F05, F07 |
| 5 | Itens só do gestor | *Recycle bin* (reciclagem), *Team* (membros) e *Edit project* | F09, F01, R9, R17 |
| 6 | Filtro *My tasks* | Mostra a tarefa principal se o utilizador for responsável por ela ou por alguma das suas subtarefas | P25 |
| 7 | Botão *+ New task* | Abre a criação de tarefa. Num projeto concluído ou suspenso o botão não aparece (cenário 2.10) | F02, US02, R22 |
| 8 | Eixos | Urgência na horizontal e importância na vertical, cada uma dividida em alta (níveis 3 e 4) e baixa (níveis 1 e 2) | F03, secção 3.2 de requirements |
| 9 | Quadrantes | *Do first*, *Schedule*, *Delegate* e *Later* | F04 |
| 10 | Cartão de tarefa | Título, prazo, responsáveis, estado e número de subtarefas. Só tarefas principais; as de estado final estão escondidas | F02, F08, R18, P10 |

## 4. Navegação

| De | Ação | Para |
|---|---|---|
| Matrix | Escolher outro projeto no seletor (1) | Matrix desse projeto |
| Matrix | Separador *List* (4) | Lista do projeto (US09) |
| Matrix | Clicar num cartão (10) | Detalhe da tarefa, com subtarefas e comentários (US04, US05, US11) |
| Início de sessão | Credenciais corretas | Painel inicial (US10, US13) |
| Painel inicial | Escolher ou pesquisar um projeto | Matrix desse projeto (US12, US13) |
| Matrix | Separador *Dashboard* (4) | Painel inicial (US13) |
| Matrix | *+ New task* (7) | Formulário de nova tarefa (US02) |
| Matrix (gestor) | *Recycle bin* (5) | Reciclagem (F09, US07) |
| Matrix (gestor) | *Team* ou *Edit project* (5) | Equipa e dados do projeto (US01) |
| Qualquer ecrã | *Sign out* (3) | Ecrã de início de sessão (US10) |

## 5. Validação face ao MVP

### 5.1 Funcionalidades da v1

| Funcionalidade | Presente neste ecrã? |
|---|---|
| F01 Projetos e equipa | Em parte: fase e estado (2); *Team* e *Edit project* para o gestor (5). A criação do projeto é outro ecrã |
| F02 Tarefas | Sim: cartões (10) e *+ New task* (7) |
| F03 Urgência e importância | Sim: eixos (8) e posição do cartão |
| F04 Vista Matriz | Sim: é o ecrã |
| F05 Vista Lista | Sim, como separador (4) |
| F06 Visibilidade por papel e projeto | Sim: seletor de projeto (1), utilizador autenticado (3) e itens só do gestor (5) |
| F07 Painel inicial | Só a ligação *Dashboard* (4); o painel é outro ecrã |
| F08 Subtarefas | Sim: número de subtarefas no cartão; o detalhe fica no ecrã da tarefa |
| F09 Reciclagem | Só a ligação *Recycle bin* (5), para o gestor (R9) |
| F10 Autenticação | Sim: utilizador autenticado e *Sign out* (3); o início de sessão é outro ecrã |
| F17 Comentários | Não neste ecrã: ficam no detalhe da tarefa |
| F22 Pesquisa e filtros | Não neste ecrã: a pesquisa fica no painel inicial e os filtros na Lista |

### 5.2 Conteúdo fora do MVP

O ecrã não mostra nada da v2 nem da v3: sem criação de contas (F23), etiquetas (F11), lembretes ou alertas (F12), arquivo (F13), auditoria (F14), Calendário (F15), anexos (F16) ou avisos externos (F18). Também não tem pesquisa de projetos (fica no painel inicial), notificações nem outros elementos não documentados.

### 5.3 Resultado

O ecrã corresponde às funcionalidades do MVP e as ligações de navegação cobrem as user stories US01 a US13. Os pontos abaixo são escolhas do wireframe que a documentação não define.

## 6. Pontos de decisão

O código **W** é local a este documento. W1 a W5 foram confirmados pela autora do projeto.

| ID | Ponto | Proposta usada no wireframe | Estado |
|---|---|---|---|
| W1 | O que mostra um cartão de tarefa | Título, prazo, responsáveis, estado e número de subtarefas. A documentação não define o conteúdo do cartão | Confirmado |
| W2 | Fase e estado do projeto na Matrix | Mostrados no cabeçalho, só para leitura, e o estado inclui os calculados (*Attention*, *Late*). Não está documentado que a Matrix os mostre | Confirmado |
| W3 | Onde ficam os itens do gestor | Como separadores e botões no mesmo ecrã. A posição é uma proposta; o painel inicial (F07) e a reciclagem (F09) são ecrãs próprios | Confirmado |
| W4 | Ordem dos cartões dentro de cada quadrante | Por prazo, do mais próximo para o mais distante; com o mesmo prazo, por urgência e depois por importância (R23, cenário 3.6). A mesma regra vale para a Lista | Decisão |
| W5 | *+ New task* num projeto concluído ou suspenso | O botão não aparece (R22, cenário 2.10). Um pedido de criação por outra via é recusado com mensagem | Decisão |

**Dependem de pressupostos por validar:** P21 (contas de demonstração, elemento 3).

## 7. Fora deste wireframe

Outros ecrãs do MVP que ainda não têm wireframe: Painel inicial, início de sessão, Lista, criação e detalhe de tarefa (com comentários), criação e edição de projeto, equipa e reciclagem.
