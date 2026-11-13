# Matriz — Arquitetura

> Decisão sobre o estilo de arquitetura, identificação dos componentes principais e a stack. As decisões técnicas por tomar continuam em [constraints.md](constraints.md#4-decisões-técnicas-pendentes).
> Documentos relacionados: [requirements.md](requirements.md), [user-stories.md](user-stories.md) e [constraints.md](constraints.md).

## 1. Decisão

| Campo | Valor |
|---|---|
| **Decisão** | A Matriz é um **monólito modular**: uma só aplicação e uma só base de dados, organizada em módulos com responsabilidades e fronteiras claras |
| **Estado** | Aprovada pela autora do projeto (2026-11-05) |
| **Âmbito** | MVP (v1), com a evolução prevista para a v2 e v3 |
| **Componentes** | Proposta inicial (secção 4), a rever no desenho detalhado |

## 2. Alternativas consideradas

| Alternativa | Porque não foi escolhida |
|---|---|
| **Monólito simples** | Funcionaria no MVP, mas a v2 e a v3 acrescentam criação de contas, lembretes, arquivo, auditoria, anexos e avisos externos. Sem fronteiras, a aplicação mistura-se à medida que cresce |
| **Microservices** | O ganho principal (escalar partes separadas, equipas independentes) não existe aqui. O custo cai sobre regras que precisam de consistência forte entre entidades (R3, R4, R11, R17 e R20) |

## 3. Justificação

1. **Dimensão pequena.** A dimensão da v1 é de 10 utilizadores e os projetos que os gestores criarem (NF03), sem requisitos de desempenho nem de disponibilidade. Uma só aplicação chega.
2. **Regras que cruzam entidades.** Projetos, membros, tarefas e subtarefas têm regras que têm de ser atómicas: um membro só está num projeto em aberto (R11), a remoção com substituto (R17), tarefa-mãe e subtarefas (R3, R4) e a reciclagem em cascata (R9, R20). Com uma só base de dados, cada operação é uma transação. Com serviços separados seriam precisos mecanismos distribuídos, sem benefício aqui.
3. **Cenários testáveis.** Os cenários Given / When / Then ([user-stories.md](user-stories.md)) correspondem a operações completas. Numa só aplicação testam-se sem levantar vários serviços.
4. **Evolução prevista.** A v2 e a v3 entram como módulos novos ou como extensões de módulos existentes (secção 6), sem reestruturar o que já existe.
5. **Uma só equipa e um só deploy.** O projeto é desenvolvido por uma pessoa, com a IA como apoio (C5). A simplicidade operacional pesa mais do que a independência entre partes.

### Consequências
- **Positivas:** consistência simples, um só deploy, menos código de infraestrutura, evolução por módulos.
- **Negativas:** as fronteiras entre módulos têm de ser mantidas à mão; um módulo não pode ser escalado sozinho.
- **Risco principal:** as fronteiras erodirem-se com o tempo. Mitigação: as regras da secção 5.

### Quando reavaliar
- Se um módulo precisar de escalar de forma independente (por exemplo, as Notificações na v3).
- Se a utilização real passar muito além da dimensão de referência (NF03).
- Se aparecerem equipas diferentes a evoluir módulos diferentes.

## 4. Componentes e responsabilidades

### 4.1 Módulos de negócio (v1)

| Módulo | Responsabilidades | Funcionalidades e regras |
|---|---|---|
| **Projetos e Equipas** | Guarda os projetos (nome, descrição, datas, fuso, fase, estado manual) e o seu gestor. Gere os membros de cada projeto: adicionar, remover e a regra de um projeto em aberto por membro. Valida as datas do projeto | F01; R11, R12 (parte manual), R16, R17, R19 |
| **Tarefas** | Guarda tarefas e subtarefas: estado, prazo, urgência, importância, responsáveis e comentários. Valida os prazos (R1, R2, R3, R13), os fechos (R4, R5), os motivos de bloqueio e a tarefa original de um duplicado (R6), os limites de texto (R14) que os responsáveis são membros do projeto (R21) e que há sempre pelo menos um (R26), e os limites e a ordem dos comentários (R24) | F02, F03, F08, F17; R1 a R6, R13, R14, R21, R24, R26; P8, P28 |
| **Vistas e Progresso** | Responde a consultas de leitura: a Matriz por projeto, a Lista (com "as minhas" e os filtros por estado e quadrante), o painel inicial (contagens de tarefas por estado e, para o gestor, o progresso) e a pesquisa de projetos. Calcula o progresso e os estados "atenção" e "atrasado" | F04, F05, F07, F22; R15, R18, R23, R25; P5, P7, P16, P25, P26, P27, P29 |
| **Acesso e Visibilidade** | Autentica o utilizador (iniciar e terminar sessão, com contas de demonstração por seed) e sabe o seu papel. Decide o que cada pessoa pode ver e fazer. Fornece esta resposta aos outros componentes | F06, F10; R8, R9 (permissões), R19 (visibilidade do gestor); S1, S2, S5 |
| **Reciclagem** | Guarda projetos, tarefas e subtarefas apagados, deixa recuperá-los a quem tem acesso, conforme o papel (R9), e elimina-os definitivamente ao fim de 30 dias | F09; R9, R20 |

### 4.2 Componentes transversais

| Componente | Responsabilidades |
|---|---|
| **Casos de uso** | Coordenam as operações que atravessam mais do que um módulo, para que cada uma seja uma só transação: remover um membro com substituto (R17), retomar um projeto suspenso (P22), apagar um projeto com as suas tarefas (R9, R20), atribuir uma tarefa só a quem é membro (R21), criar tarefas só num projeto em aberto (R22) e limitar as ações sobre tarefas conforme o estado do projeto (R27). Verificam as permissões junto do módulo Acesso e Visibilidade |
| **Núcleo partilhado** | Funcionalidades usadas por todos os módulos: datas, horas e fuso horário (P26), validação de texto e mensagens de recusa (NF08, NF09), termos da interface em inglês ([requirements.md](requirements.md#34-termos-da-interface)) |
| **Interface** | Os ecrãs em inglês (NF01), para computador (NF02). Mostra os dados, recolhe as ações e apresenta as mensagens. Não contém regras de negócio |
| **Persistência** | O acesso aos dados, numa só base de dados partilhada pela aplicação. Cada módulo só acede aos seus próprios dados (T2) |

## 5. Regras de fronteira

Estas regras mantêm o monólito modular e não um monólito comum.

1. **A interface só fala com os casos de uso e com as consultas de leitura**, nunca diretamente com os dados.
2. **Um módulo não lê nem escreve nos dados de outro.** Quando precisa de informação de outro módulo, usa a sua interface pública.
3. **As dependências vão num só sentido.** Os módulos de negócio não dependem uns dos outros em círculo; quando duas regras cruzam módulos (por exemplo, o R17 envolve membros e tarefas), a coordenação fica nos **casos de uso**.
4. **As regras de negócio vivem nos módulos**, não na interface nem nos casos de uso, que só as combinam.
5. **A decisão sobre o que o utilizador vê ou pode fazer vem sempre do módulo Acesso e Visibilidade**, para uma mudança futura na autenticação (por exemplo, a criação de contas na v2) mudar só esse módulo. Para decidir, o módulo Acesso lê `manager_id` e a associação de membros do projeto ([database.md](database.md#61-o-que-o-modelo-garante-e-o-que-a-aplicação-valida)): é a única exceção à regra 2, porque é ele que sabe quem tem acesso. Os dados do projeto continuam a ser lidos só pelo módulo Projetos, e os casos de uso juntam as duas respostas (404 se o projeto não existe, 403 se não há acesso).

> **Nota (revisão de código do PR 1, 2026-11-13):** `listProjects` faz várias consultas (os projetos a que o utilizador tem acesso, os projetos e os gestores) em vez de uma só com junções, de propósito. Uma consulta única juntaria dados de módulos diferentes (acesso, projetos e utilizadores), contra a regra 2. Com a dimensão da v1 (NF03) o custo é desprezável, e a coordenação fica no caso de uso. Não juntar numa só consulta sem rever esta regra.

### Dependências entre componentes

```
Interface
   │
   ▼
Casos de uso ──► Acesso e Visibilidade
   │
   ├──► Projetos e Equipas
   ├──► Tarefas
   ├──► Reciclagem
   └──► Vistas e Progresso ──(só leitura)──► Projetos e Equipas, Tarefas

Todos os módulos ──► Núcleo partilhado
Todos os módulos ──► Persistência (cada um só aos seus dados)
```

## 6. Evolução prevista

| Versão | Funcionalidade | Onde entra |
|---|---|---|
| v2 | Criação de contas pelo gestor (F23) | Módulo **Acesso e Visibilidade**, que já autentica na v1 (F10); passa a criar contas em vez de as receber por seed |
| v2 | Etiquetas (F11) | Módulo **Tarefas** |
| v2 | Lembretes e alertas de atraso (F12) | Novo módulo **Notificações** |
| v2 | Arquivo de projetos (F13) | Módulo **Projetos e Equipas**, ou um módulo próprio se crescer |
| v2 | Registo de auditoria (F14) | Novo módulo **Auditoria**, alimentado pelos casos de uso |
| v3 | Calendário (F15) | Módulo **Vistas e Progresso** |
| v3 | Anexos (F16) | Módulo **Tarefas**, ou um módulo próprio. Os comentários (F17) já estão no módulo Tarefas desde a v1 |
| v3 | Avisos por email ou SMS (F18) | Módulo **Notificações** (candidato a extração, ver secção 3) |
| v3 | Importação e exportação (F19) | Novo módulo |

## 7. O que este documento não decide
- A tecnologia da interface do utilizador, o ambiente onde a aplicação corre, e as cópias de segurança: continuam em T1, T8, T10 e T11 ([constraints.md](constraints.md#4-decisões-técnicas-pendentes)).
- A estrutura interna de cada módulo e os nomes das suas interfaces: ficam para o desenho detalhado.

## 8. Stack e decisões técnicas

Escolhida em 2026-11-12 com a autora do projeto, por ser a mais simples que serve a arquitetura e o contrato. Antes não estava registada em nenhum documento.

| Decisão | Escolha | Estado | Porquê |
|---|---|---|---|
| Linguagem e framework | **Java 21 com Spring Boot** | Decidido | Uma só aplicação executável, com módulos como pacotes; validação, segurança e geração do contrato OpenAPI maduras |
| Base de dados | **PostgreSQL** | Decidido | O modelo de dados é relacional, com chaves e transações que as regras R11, R17 e R20 precisam ([database.md](database.md)) |
| Forma de execução e interface | **API REST primeiro**; a interface escolhe-se depois da API (T1) | Decidido | A API testa-se contra o [openapi.yaml](../openapi.yaml) sem depender da interface; os wireframes já existem |
| Autenticação | **Sessão no servidor, identificada por um cookie**, que termina ao terminar sessão ou após 3 horas sem atividade; controlo de acesso por papel e por projeto feito no servidor | Decidido | Expira e termina com facilidade; a S1 exige recusar o acesso também por ligação direta |
| Build | Maven | Adotado (incremento 0) | É o arranque habitual do Spring Boot |
| Acesso aos dados e migrações | Flyway para o esquema; consultas e escritas por JDBC (`JdbcTemplate`); o Spring Data JPA não se usa e foi retirado das dependências | Adotado: Flyway e JDBC; o JPA não se usa (decidido em 2026-11-13) | Esquema versionado (T12). O seed corre ao arrancar e não numa migração, porque precisa do hash da palavra-passe. Até agora o JDBC bastou; decide-se se se usa JPA quando houver escritas mais complexas (incremento 2) |
| Testes | JUnit 5 e Spring Boot Test, com um PostgreSQL de teste (`matriz_test`); ArchUnit para as fronteiras; swagger-parser para validar o contrato | Adotado (incremento 0) | Cada cenário Given / When / Then passa a ser um teste (T9) |
| Datas e horas | `java.time` com o fuso do projeto | Proposta | Cumpre o P26 e o cálculo do tempo decorrido (T5) |
| Segurança | Spring Security, com um serviço de utilizadores sobre `app_user`, hash BCrypt e filtros que devolvem 401 e 403 em JSON | Adotado (incremento 1) | Entrou só no incremento 1, para não gerar uma palavra-passe aleatória nos registos do incremento 0 |
| Proteção CSRF e sessões | CSRF desligado (API só JSON e cookie `SameSite=Lax`); sessões em memória | Pressuposto (P31, P32) | Simples para a v1; rever com a interface (T1) |

**Como a stack respeita a arquitetura**
- Cada módulo da secção 4 é um pacote da aplicação, e os casos de uso são serviços que coordenam os módulos numa só transação.
- Os controladores REST fazem o papel da **Interface**: só falam com os casos de uso e com as consultas de leitura (regra 1 da secção 5).
- Um teste automático verifica as dependências entre pacotes (regras 2 e 3 da secção 5).

As propostas confirmam-se no incremento 0 do [plano de implementação](implementation-plan.md).
