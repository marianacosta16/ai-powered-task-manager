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
| C6 | Não há indicadores de sucesso, orçamento nem prazo definidos pelo cliente, que é fictício | Contexto do exercício (C1) |
| C7 | Os documentos estão em português; os termos da interface (por exemplo, os nomes dos quadrantes como "Do first") estão em inglês | Convenção |
| C8 | Na v1 usam-se só dados fictícios, num ambiente controlado, porque o seletor não autentica ninguém (S4) | Decisão |

## 2. Pressupostos assumidos

Propostas aceites para preencher lacunas. Devem ser validadas com o cliente real. Quando uma decisão depender de um pressuposto, deve ser questionada à autora do projeto.

| ID | Pressuposto | Justificação |
|---|---|---|
| P1 | Existe uma única empresa, sem utilizadores externos | Não foi pedido nenhum acesso externo |
| P2 | O único papel de gestão é o gestor de projeto, que também forma as equipas (adicionando membros aos projetos) | Confirmado nas respostas |
| P3 | Fase e estado do projeto coexistem; só o gestor altera a fase e define o estado, exceto "atenção" e "atrasado", que são só calculados (ver P5) | A fase descreve o ciclo de vida e o estado, a situação atual |
| P4 | "Projeto em aberto" é qualquer projeto que não esteja concluído, suspenso nem arquivado | Um projeto suspenso não vai ser trabalhado durante algum tempo, por isso não conta como projeto em aberto |
| P5 | Para projetos "em andamento", os estados "atenção" e "atrasado" são calculados. O progresso esperado é o tempo decorrido, (agora − início) ÷ (fim − início), medido no fuso horário do projeto (P26); o desvio é o progresso esperado menos o progresso real, em pontos percentuais. **Atenção** quando o desvio é igual ou superior a 15 e inferior a 25; **atrasado** quando o desvio é igual ou superior a 25, ou quando a data de fim passou e o projeto não está concluído. Os estados "não iniciado", "suspenso" e "concluído" são definidos só pelo gestor. Um projeto "não iniciado" nunca é marcado como "atenção" nem "atrasado", mesmo depois da data de fim. Os valores calculados (progresso e desvio) são arredondados à segunda casa decimal | Permite ao gestor ver se está dentro do prazo; confirmado pela autora |
| P6 | O estado "adiantado" não existe | A autora decidiu ignorá-lo |
| P7 | Progresso = tarefas principais concluídas ÷ tarefas principais totais (as subtarefas não contam, R18), sem contar canceladas e duplicadas | Evita distorcer a barra com tarefas que não vão ser feitas |
| P8 | Qualquer membro move uma tarefa ou subtarefa entre todos os estados, incluindo os finais (concluído, cancelado e duplicado); reabrir uma tarefa final só o gestor, que escolhe o estado para onde a reabre | Define quem pode fazer cada mudança de estado; confirmado pela autora |
| P9 | A Matriz mostra todas as tarefas da equipa, com o filtro "as minhas" | Dá visão da equipa sem perder a visão pessoal |
| P10 | Tarefas em estado final ficam escondidas por defeito na Matriz | Mantém a Matriz focada no que falta fazer |
| P11 | Quando existirem lembretes (v2), os avisos aparecem só com a aplicação aberta | Consequência de não haver canais externos antes da v3 |
| P12 | Os projetos e tarefas apagados ficam numa reciclagem; arquivar é diferente de apagar | Separa remoção de encerramento |
| P13 | (v2, com a gestão de contas) Quando um utilizador sai, as suas tarefas em aberto passam para quem o substitui; o nome mantém-se no histórico e nas tarefas arquivadas ou em estado final | Evita tarefas sem responsável e preserva o histórico |
| P14 | O registo de auditoria guarda quem fez o quê e quando (criar, editar, mudar de estado, apagar) | A resposta foi só "sim" |
| P15 | A aplicação precisa de controlo de acesso por papel desde a v1; o login (F10) chega na v2 e, na v1, o utilizador é escolhido num seletor (S4) | Necessário para cumprir as regras de visibilidade e de apagar |
| P16 | Se não houver tarefas contáveis, o projeto mostra a etiqueta "sem tarefas" em vez de percentagem, mesmo que a data de fim tenha passado, e os estados "atenção" e "atrasado" não lhe são atribuídos por cálculo | Evita confundir "sem tarefas" com "tarefas por fazer"; confirmado pela autora |
| P17 | Os limites de 100 caracteres (título) e 2000 (descrição) aplicam-se também ao nome e à descrição do projeto | A autora aceitou os limites para "títulos e descrições" e confirmou que valem para tarefas e projetos |
| P18 | Dimensão de referência: cerca de 30 utilizadores, 3 a 5 projetos em aberto em simultâneo, cada um com uma equipa de 5 a 8 pessoas | Foi preciso um tamanho para o dimensionamento; aceite pela autora |
| P19 | A empresa não tem hoje nenhuma aplicação deste tipo | Premissa do exercício |
| P20 | Ao concluir ou suspender um projeto, os seus membros ficam livres para serem adicionados a outro; a lista de membros do projeto mantém-se e eles continuam a vê-lo | Num projeto suspenso não se vai trabalhar durante algum tempo, por isso os membros têm de ser realocados; sem isto a regra de um projeto em aberto por membro (R11) não seria aplicável |
| P21 | Na v1 os utilizadores disponíveis no seletor existem previamente (não são criados na aplicação) e o seletor mostra apenas os nomes; a criação de contas pelo gestor chega com o login (F10, v2) | Sem contas na v1, tem de haver uma lista de utilizadores de onde o gestor escolhe os membros |
| P22 | Ao retomar um projeto suspenso, os membros que entretanto entraram noutro projeto em aberto continuam na lista de membros, sinalizados com uma etiqueta vermelha, e o gestor vê um aviso; as suas tarefas mantêm-se atribuídas. A etiqueta desaparece quando o membro passa a estar em um só projeto em aberto. É a única exceção à regra R11 | Evita perder a equipa original e obriga o gestor a decidir a realocação |
| P23 | A data de início e a de fim do projeto são obrigatórias e o instante de fim é posterior ao de início. Se hoje é anterior à data de início, o tempo decorrido é 0% | O P5 divide por (fim − início) e precisa de datas válidas |
| P24 | Ao remover um membro com tarefas por concluir (R17), o novo membro fica com todas essas tarefas, tem de ser um membro livre (R11) e não pode já estar no projeto. "Por concluir" são as tarefas que não estão em estado final | Completa a regra de remoção decidida pela autora |
| P25 | O filtro "as minhas" mostra a tarefa principal se o membro for responsável por ela ou por alguma das suas subtarefas | Interpretação de "o filtro considera as subtarefas", dado que as subtarefas não aparecem sozinhas (R18) |
| P26 | O fuso horário é definido ao criar o projeto e aplica-se a todas as datas e horas do projeto, incluindo os prazos das tarefas. A data de fim pode ter hora; sem hora, o dia de fim conta inteiro (até ao fim desse dia). A data de início sem hora conta a partir das 00:00. O tempo decorrido mede-se entre o instante de início e o de fim | O fuso e a hora de fim são decisão da autora; o dia inteiro e as 00:00 são interpretações minhas |
## 3. Requisitos de segurança e privacidade

### Definidos
| ID | Requisito | Origem | Versão |
|---|---|---|---|
| S1 | Um membro só acede aos projetos de que é membro e às suas tarefas, também por ligação direta; o gestor acede só aos projetos de que é gestor | R8, F06 | v1 |
| S2 | Só o gestor apaga projetos, recupera apagados e reabre tarefas finais | R9, P8 | v1 |
| S3 | Os dados apagados ficam numa reciclagem recuperável | P12, F09 | v1 |
| S4 | Na v1 o utilizador escolhe quem é num seletor, sem palavra-passe. Isto não é autenticação: qualquer pessoa pode escolher o papel de gestor | Decisão, P15 | v1 |
| S5 | Todos os utilizadores se autenticam para aceder à aplicação | P15, F10 | v2 |
| S6 | As contas são criadas pelo gestor de projeto; não há registo livre | Decisão, F10 | v2 |
| S7 | As ações relevantes ficam registadas (quem, o quê, quando) | P14, F14 | v2 |

### Riscos aceites de forma consciente na v1
- Qualquer pessoa pode escolher o papel de gestor no seletor e apagar ou recuperar projetos (S4).
- Sem registo de auditoria na v1, não fica registado quem apagou o quê.
- O seletor mostra a todos a lista de utilizadores (P21).

Mitigações decididas: dados fictícios num ambiente controlado (C8) e um seletor que mostra apenas nomes (P21). Se a aplicação passar a usar dados reais, o login (F10) deve ser antecipado.

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
| T3 | Mecanismo de autenticação e de controlo de acesso por papel e por projeto | S1, S2, S5 e S6 |
| T4 | Como se disparam os lembretes e alertas de atraso dentro da aplicação | F12, P11 |
| T5 | Como se implementam as datas e horas (fuso horário do projeto, P26, e cálculo do tempo decorrido) | P5, cenários 6.3 a 6.6, 6.14 e 6.15 |
| T6 | Como se guarda o registo de auditoria | F14, S7 |
| T7 | Tecnologias de interface, lógica e armazenamento | Todas |
| T8 | Ambiente onde a aplicação corre e como se publica | T1 |
| T9 | Estratégia de testes, incluindo como automatizar os cenários Given / When / Then | [user-stories.md](user-stories.md) |
| T10 | Navegadores ou sistemas suportados, e requisitos de acessibilidade | NF02 |
| T11 | Cópias de segurança e recuperação de dados | T2 |
| T12 | Como se implementa o seletor de utilizador da v1 e a lista de utilizadores disponíveis (decisão funcional já tomada: S4, P21) | F06, S1, S2 |
| T13 | Como se tratam as edições simultâneas da mesma tarefa por vários utilizadores | F02, F08 |

### Questões funcionais em aberto
- Valores permitidos para a antecedência do lembrete e para o intervalo de repetição do alerta, e até quando se repete (F12).
- O que veem os responsáveis de uma tarefa que não são membros do projeto (R8, R21).
- Se o gestor que cria um projeto e indica outro gestor continua a vê-lo.
- Que fuso horário se usa se não for indicado ao criar o projeto.
- Se uma subtarefa pode ser recuperada da reciclagem quando a tarefa-mãe já foi eliminada definitivamente.
- Se as tarefas ficam ligadas a uma fase do projeto.
- Ordem das tarefas dentro de cada grupo da Lista (abertas e fechadas).
- Que integrações, se alguma, o cliente quer (F19).
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
