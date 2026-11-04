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
| **Gestor de projeto** | Cria projetos e forma as equipas, gere as tarefas e acompanha o progresso de todos os projetos |

## Versões

A entrega segue a prioridade MoSCoW: **v1 (MVP) = Must**, **v2 = Should**, **v3 = Could**.

### Funcionalidades previstas para o MVP (v1)

- Projetos (nome, descrição, datas, fase e estado) e respetiva equipa: o gestor cria o projeto e adiciona membros.
- Tarefas com prazo obrigatório, vários responsáveis, urgência e importância em 4 níveis, e subtarefas com as suas regras de prazo e fecho.
- Vista **Matriz** por projeto (quadrantes *Do first*, *Schedule*, *Delegate* e *Later*) e vista **Lista**.
- Vista global do gestor com barra de progresso e estado calculado por projeto.
- Visibilidade por papel: o membro só vê os projetos de que faz parte; o gestor vê tudo. Na v1 não há login: o utilizador escolhe quem é num seletor.
- Reciclagem para recuperar projetos e tarefas apagados.

### Versões seguintes
- **v2 (Should):** login e contas, etiquetas, lembretes e alertas de atraso, arquivo de projetos e registo de auditoria.
- **v3 (Could):** Calendário, anexos, comentários, avisos por email ou SMS, importação e exportação, registo de tempo, tarefas repetidas, modelos de projeto e quadro por estados.

A lista completa, com justificações, está em [docs/requirements.md](docs/requirements.md).

## Organização da documentação

| Ficheiro | Conteúdo |
|---|---|
| [docs/requirements.md](docs/requirements.md) | Visão e objetivos, tipos de utilizadores, requisitos funcionais e não funcionais, regras de negócio, âmbito do MVP e funcionalidades fora do âmbito inicial |
| [docs/user-stories.md](docs/user-stories.md) | User stories do MVP, prioridades, critérios de aceitação (Given / When / Then) e casos excecionais |
| [docs/constraints.md](docs/constraints.md) | Restrições, pressupostos, requisitos de segurança e privacidade, decisões técnicas pendentes e limitações de âmbito |
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
| Análise de requisitos e âmbito do MVP | Concluído |
| User stories e critérios de aceitação da v1 | Concluído (9 stories, cobrem F01 a F09) |
| User stories da v2 | Por fazer (F10 a F14) |
| Base de conhecimento (esta documentação) | Concluído |
| Decisões técnicas e desenho | Por iniciar (ver T1 a T12 em [docs/constraints.md](docs/constraints.md#4-decisões-técnicas-pendentes)) |
| Implementação | Por iniciar |

Não foi escolhida nenhuma tecnologia.
