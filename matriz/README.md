# Matriz

Aplicação de gestão de projetos e tarefas para equipas de trabalho, com uma **matriz urgente/importante** como vista principal para decidir o que fazer primeiro.

> Projeto de aprendizagem. Esta fase contém apenas a documentação de requisitos; ainda não há código.

## O problema

Uma empresa quer uma ferramenta, do género Jira, Azure DevOps ou Trello, onde as equipas possam organizar projetos, gerir tarefas e acompanhar o progresso do trabalho. Não tem hoje nenhuma aplicação deste tipo e ainda não definiu funcionalidades, regras ou prioridades. A Matriz parte desse pedido e transforma-o numa especificação clara e verificável.

## Objetivos principais

1. **Organizar projetos:** criar projetos com fases e estados e formar a equipa de cada um.
2. **Gerir tarefas:** criar, atribuir e priorizar tarefas (com subtarefas) por urgência e importância.
3. **Acompanhar o progresso:** permitir ao gestor de projeto ver, por projeto e no conjunto, se o trabalho está dentro do prazo.

## Utilizadores

| Papel | Resumo |
|---|---|
| **Membro da equipa** | Gere as tarefas dos projetos de que faz parte |
| **Gestor de projeto** | Cria projetos e forma as equipas, gere as tarefas e acompanha o progresso dos seus projetos |

## Versões

A entrega segue a prioridade MoSCoW: **v1 (MVP) = Must**, **v2 = Should**, **v3 = Could**.

### Funcionalidades previstas para o MVP (v1)

- Projetos (nome, descrição, datas, fase e estado) e respetiva equipa: o gestor cria o projeto e adiciona membros.
- Tarefas com prazo obrigatório, vários responsáveis, urgência e importância em 4 níveis, e subtarefas com as suas regras de prazo e fecho.
- Vista **Matriz** por projeto (quadrantes *Do first*, *Schedule*, *Delegate* e *Later*) e vista **Lista**.
- **Painel inicial** com os projetos acessíveis e as contagens de tarefas por estado; para o gestor, também a barra de progresso e o estado calculado por projeto.
- Visibilidade por papel: o membro só vê os projetos de que faz parte; o gestor vê só os projetos de que é gestor. O utilizador inicia e termina sessão com uma conta de demonstração criada por seed.
- Reciclagem para recuperar projetos e tarefas apagados.
- Comentários nas tarefas (adicionar e listar, com autor e data).
- Pesquisa de projetos pelo nome e filtros de tarefas por estado e por quadrante.

### Versões seguintes
- **v2 (Should):** criação de contas pelo gestor, etiquetas, lembretes e alertas de atraso, arquivo de projetos e registo de auditoria.
- **v3 (Could):** Calendário, anexos, avisos por email ou SMS, importação e exportação, registo de tempo, tarefas repetidas, modelos de projeto e quadro por estados.

A lista completa, com justificações, está em [docs/requirements.md](docs/requirements.md).

## Organização da documentação

| Ficheiro | Conteúdo |
|---|---|
| [docs/requirements.md](docs/requirements.md) | Visão e objetivos, tipos de utilizadores, requisitos funcionais e não funcionais, regras de negócio, âmbito do MVP e funcionalidades fora do âmbito inicial |
| [docs/user-stories.md](docs/user-stories.md) | User stories do MVP, prioridades, critérios de aceitação (Given / When / Then) e casos excecionais |
| [docs/constraints.md](docs/constraints.md) | Restrições, pressupostos, requisitos de segurança e privacidade, decisões técnicas pendentes e limitações de âmbito |
| [docs/architecture.md](docs/architecture.md) | Decisão de arquitetura (monólito modular), justificação, componentes e responsabilidades |
| [docs/database.md](docs/database.md) | Modelo de dados da v1: entidades, atributos, relacionamentos, cardinalidades e diagrama ER (Mermaid) |
| [openapi.yaml](openapi.yaml) | Contrato OpenAPI 3.1 simplificado: criar tarefa e listar as tarefas de um projeto |
| [docs/ui/matrix-wireframe.md](docs/ui/matrix-wireframe.md) | Wireframe de baixa fidelidade do ecrã Matrix (SVG), elementos, navegação e validação face ao MVP |
| [docs/plan.md](docs/plan.md) | Análise de lacunas e decisões pendentes, e plano incremental de implementação (proposta) |
| [AGENTS.md](AGENTS.md) | Instruções persistentes para agentes de desenvolvimento que trabalhem neste repositório |

### Convenções
- Os códigos identificam os itens em todos os documentos: **F** (funcionalidade), **R** (regra de negócio), **NF** (requisito não funcional), **C** (restrição), **P** (pressuposto), **S** (segurança), **T** (decisão técnica pendente), **US** (user story) e **EC** (caso excecional).
- Cada item está marcado como **Decisão** ou **Pressuposto**. As decisões foram tomadas pela autora do projeto em nome do cliente, que não está disponível, por isso os pressupostos devem ser validados com um cliente real.
- Os documentos estão em português; a interface da aplicação é em inglês.
- Estes documentos servem de contexto para as fases seguintes. Quando se usar IA para apoiar a escrita ou o desenvolvimento, convém rever a consistência entre eles.

## Estado atual

| Fase | Estado |
|---|---|
| Descrição do problema e perguntas de clarificação | Concluído |
| Análise de requisitos e âmbito do MVP | Concluído (só o P14, da v2, por validar) |
| User stories e critérios de aceitação da v1 | Concluído (13 stories, cobrem F01 a F10, F17 e F22) |
| User stories da v2 | Por fazer (F11 a F14 e F23) |
| Conciliação com o novo âmbito funcional | Registada em [docs/requirements.md](docs/requirements.md#8-conciliação-com-o-novo-âmbito-funcional); pressupostos P27 a P29 confirmados |
| Base de conhecimento (esta documentação) | Concluído |
| Decisões técnicas e desenho | Em curso: arquitetura decidida (monólito modular, ver [docs/architecture.md](docs/architecture.md)); modelo de dados da v1 proposto, com pontos por confirmar (ver [docs/database.md](docs/database.md)); contrato OpenAPI com duas operações (ver [openapi.yaml](openapi.yaml)); tecnologias por decidir (ver T1 a T13 em [docs/constraints.md](docs/constraints.md#4-decisões-técnicas-pendentes)) |
| Implementação | Por iniciar |

Não foi escolhida nenhuma tecnologia.
