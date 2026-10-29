# Matriz — Restrições, Pressupostos e Decisões Pendentes

> Documento de referência das restrições e dos pressupostos da Matriz. Não propõe tecnologias nem contém código.
> Documentos relacionados: [requirements.md](requirements.md) e [user-stories.md](user-stories.md).

## 1. Restrições conhecidas

| ID | Restrição | Origem |
|---|---|---|
| C1 | O cliente não está disponível: as respostas foram dadas pela autora do projeto em nome do cliente, pelo que todas as decisões devem ser validadas com um cliente real | Contexto do exercício |
| C2 | A interface é em inglês | Decisão |
| C3 | A aplicação é usada em computador | Decisão |
| C4 | Existem apenas dois papéis: gestor de projeto e membro da equipa | Decisão |
| C5 | A IA é usada como apoio ao desenvolvimento; a aplicação não tem funcionalidades de IA | Decisão |
| C6 | Não há indicadores de sucesso, orçamento nem prazo definidos pelo cliente | Pressuposto (P19) |
| C7 | Os documentos estão em português; os termos da interface (por exemplo, os nomes dos quadrantes como "Do first") estão em inglês | Convenção |

## 2. Pressupostos assumidos

Propostas aceites para preencher lacunas. Devem ser validadas com o cliente real.

| ID | Pressuposto | Justificação |
|---|---|---|
| P1 | Existe uma única empresa, sem utilizadores externos | Não foi pedido nenhum acesso externo |
| P2 | O único papel de gestão é o gestor de projeto, que também forma as equipas (adicionando membros aos projetos) | Confirmado nas respostas |
| P3 | Fase e estado do projeto coexistem; só o gestor altera a fase e define manualmente o estado (os estados "atenção" e "atrasado" são calculados, ver P5) | A fase descreve o ciclo de vida e o estado, a situação atual |
| P4 | "Projeto em aberto" é qualquer projeto que não esteja concluído, suspenso nem arquivado | Um projeto suspenso não vai ser trabalhado durante algum tempo, por isso conta como fechado |
| P5 | Para projetos "em andamento", os estados "atenção" e "atrasado" são calculados. O progresso esperado é o tempo decorrido, (hoje − início) ÷ (fim − início), em dias; o desvio é o progresso esperado menos o progresso real, em pontos percentuais. **Atenção** quando o desvio é igual ou superior a 15 e inferior a 25; **atrasado** quando o desvio é igual ou superior a 25, ou quando a data de fim passou e o projeto não está concluído. Os estados "não iniciado", "suspenso" e "concluído" são definidos só pelo gestor | Permite ao gestor ver se está dentro do prazo; confirmado pela autora |
| P6 | O estado "adiantado" não existe | A autora decidiu ignorá-lo |
| P7 | Progresso = tarefas concluídas ÷ tarefas totais, sem contar canceladas e duplicadas | Evita distorcer a barra com tarefas que não vão ser feitas |
| P8 | Qualquer membro move uma tarefa entre por fazer, em curso, em revisão e bloqueado; concluído, cancelado e duplicado são finais; reabrir uma tarefa final só o gestor | Define quem pode fazer cada mudança de estado |
| P9 | A Matriz mostra todas as tarefas da equipa, com o filtro "as minhas" | Dá visão da equipa sem perder a visão pessoal |
| P10 | Tarefas em estado final ficam escondidas por defeito na Matriz | Mantém a Matriz focada no que falta fazer |
| P11 | Quando existirem lembretes (v2), os avisos aparecem só com a aplicação aberta | Consequência de não haver canais externos antes da v3 |
| P12 | Os projetos e tarefas apagados ficam numa reciclagem; arquivar é diferente de apagar | Separa remoção de encerramento |
| P13 | Quando um utilizador sai, as suas tarefas passam para quem o substitui | Evita tarefas sem responsável |
| P14 | O registo de auditoria guarda quem fez o quê e quando (criar, editar, mudar de estado, apagar) | A resposta foi só "sim" |
| P15 | A aplicação precisa de controlo de acesso por papel desde a v1; o login (F10) chega na v2 e, na v1, o utilizador é escolhido num seletor (S7) | Necessário para cumprir as regras de visibilidade e de apagar |
| P16 | Se não houver tarefas contáveis, o projeto mostra a etiqueta "sem tarefas" em vez de percentagem, e os estados "atenção" e "atrasado" por desvio não são calculados (o "atrasado" por data de fim ultrapassada mantém-se) | Evita confundir "sem tarefas" com "tarefas por fazer"; o caso não estava definido |
| P17 | Os limites de 100 caracteres (título) e 2000 (descrição) aplicam-se também ao nome e à descrição do projeto | A autora aceitou os limites para "títulos e descrições" sem distinguir tarefas e projetos |
| P18 | Dimensão de referência: cerca de 30 utilizadores, 3 a 5 projetos em aberto em simultâneo, cada um com uma equipa de 5 a 8 pessoas | Foi preciso um tamanho para o dimensionamento; aceite pela autora |
| P19 | A empresa não tem hoje nenhuma aplicação deste tipo | Premissa do exercício |
| P20 | Ao concluir ou suspender um projeto, os seus membros ficam livres para serem adicionados a outro; a lista de membros do projeto mantém-se e eles continuam a vê-lo | Num projeto suspenso não se vai trabalhar durante algum tempo, por isso os membros têm de ser realocados; sem isto a regra de um projeto em aberto por membro (R11) não seria aplicável |
| P21 | Na v1 os utilizadores disponíveis no seletor existem previamente (não são criados na aplicação); a criação de contas pelo gestor chega com o login (F10, v2) | Sem contas na v1, tem de haver uma lista de utilizadores de onde o gestor escolhe os membros |
| P22 | Ao retomar um projeto suspenso, os membros que entretanto entraram noutro projeto em aberto continuam na lista de membros, sinalizados com uma etiqueta vermelha, e o gestor vê um aviso; as suas tarefas mantêm-se atribuídas. É a única exceção à regra R11 | Evita perder a equipa original e obriga o gestor a decidir a realocação |

## 3. Requisitos de segurança e privacidade

### Definidos
| ID | Requisito | Origem | Versão |
|---|---|---|---|
| S1 | Todos os utilizadores se autenticam para aceder à aplicação | P15, F10 | v2 |
| S2 | As contas são criadas pelo gestor de projeto; não há registo livre | Decisão, F10 | v2 |
| S3 | Um membro só acede aos projetos de que é membro e às suas tarefas, também por ligação direta; o gestor acede a tudo | R8, F06 | v1 |
| S4 | Só o gestor apaga projetos, recupera apagados e reabre tarefas finais | R9, P8 | v1 |
| S5 | As ações relevantes ficam registadas (quem, o quê, quando) | P14, F14 | v2 |
| S6 | Os dados apagados ficam numa reciclagem recuperável | P12, F09 | v1 |
| S7 | Na v1 o utilizador escolhe quem é num seletor, sem palavra-passe. Isto não é autenticação: qualquer pessoa pode escolher o papel de gestor | Decisão, P15 | v1 |

### Excluídos ou adiados
- **RGPD e segurança avançada:** fora de âmbito por decisão da autora. É um risco se a aplicação for usada com dados reais.
- **Regras de palavra-passe e recuperação de acesso:** não se estipulam para já.
- **Conteúdo do registo de auditoria** (além de P14): fica para a user story da v2.

## 4. Decisões técnicas pendentes

Nenhuma tecnologia foi escolhida. Os pontos seguintes têm de ser decididos na fase de desenho.

| ID | Decisão pendente | Condiciona |
|---|---|---|
| T1 | Forma de execução da aplicação (por exemplo, acesso por navegador ou aplicação instalada) | NF02 |
| T2 | Onde e como se guardam os dados, e se há um local central partilhado pelas equipas | F10, F06, F09, F13 |
| T3 | Mecanismo de autenticação e de controlo de acesso por papel e por projeto | S1 a S4 |
| T4 | Como se disparam os lembretes e alertas de atraso dentro da aplicação | F12, P11 |
| T5 | Como se tratam as datas (fuso horário e cálculo do tempo decorrido em dias) | P5, cenários 6.3 a 6.6 |
| T6 | Como se guarda o registo de auditoria | F14, S5 |
| T7 | Tecnologias de interface, lógica e armazenamento | Todas |
| T8 | Ambiente onde a aplicação corre e como se publica | T1 |
| T9 | Estratégia de testes, incluindo como automatizar os cenários Given / When / Then | [user-stories.md](user-stories.md) |
| T10 | Navegadores ou sistemas suportados, e requisitos de acessibilidade | NF02 |
| T11 | Cópias de segurança e recuperação de dados | T2 |
| T12 | Como se implementa o seletor de utilizador da v1 e a lista de utilizadores disponíveis (decisão funcional já tomada: S7, P21) | F06, S3, S4 |

### Questões funcionais em aberto
- Valores permitidos para a antecedência do lembrete e para o intervalo de repetição do alerta, e até quando se repete (F12).
- Se uma tarefa duplicada tem de ser duplicada de uma tarefa do mesmo projeto.
- Se as tarefas ficam ligadas a uma fase do projeto.
- Ordem das tarefas dentro de cada grupo da Lista (abertas e fechadas).
- Que integrações, se alguma, o cliente quer (F19).
- Se um prazo igual à data atual é aceite na criação (R13).
- Se alterar o prazo de uma tarefa existente para o passado é aceite.
- O nível 2 chama-se "medium", mas conta como "baixo" na Matriz: confirmar a nomenclatura.

## 5. Limitações de âmbito

- Sem RGPD nem segurança avançada.
- Sem requisitos de desempenho, disponibilidade, compatibilidade, acessibilidade ou cópias de segurança.
- Sem indicadores de sucesso, orçamento ou prazo do cliente.
- Sem utilizadores externos à empresa.
- Sem estado "adiantado" do projeto.
- Sem funcionalidades de IA na aplicação.
- Fora do MVP, na v2: login e contas, etiquetas, lembretes e alertas de atraso, arquivo de projetos e registo de auditoria.
- Fora do MVP, na v3: Calendário, anexos, comentários, avisos por email ou SMS, importação e exportação, registo de tempo, tarefas repetidas, modelos de projeto e quadro por estados (ver [requirements.md](requirements.md#7-funcionalidades-fora-do-âmbito-inicial)).
- Os avisos só funcionam com a aplicação aberta (P11).
