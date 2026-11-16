# Matriz — interface web

A interface do Matriz: React com TypeScript e Vite, em inglês. Liga-se ao backend (`../matriz`) pela API REST do contrato [`openapi.yaml`](../matriz/openapi.yaml). O plano está em [`matriz/docs/implementation-plan.md`](../matriz/docs/implementation-plan.md) (incrementos 9 a 13).

## Como executar

Precisa de Node 20.19 ou mais recente (testado com 24) e do backend a correr (ver o [README do backend](../matriz/README.md)).

```bash
cd frontend
npm install
cp .env.example .env   # opcional: só é preciso para mudar o endereço do backend
npm run dev
```

A interface abre em `http://localhost:5173`. O backend aceita essa origem por omissão (`matriz.cors.allowed-origins`); para outra, indicar a origem em `CORS_ALLOWED_ORIGINS` no `.env` do backend, sem `*` e sem `/` no fim.

## Configuração

O endereço do backend está num só sítio, `src/config.ts`, e vem da variável `VITE_API_URL`. Em desenvolvimento (e nos testes), sem a variável, usa `http://localhost:8081`; num *build*, de qualquer modo (produção, *staging*...), a variável é obrigatória (`src/buildChecks.ts`). Tudo o que fala com a API passa por `src/api/client.ts`.

## Comandos

| Comando | O que faz |
|---|---|
| `npm run dev` | Servidor de desenvolvimento, em `http://localhost:5173` |
| `npm test` | Testes dos componentes (Vitest e Testing Library) |
| `npm run lint` | Verificação do código (oxlint) |
| `npm run build` | Verifica os tipos e gera `dist/`. Precisa de `VITE_API_URL` (o *build* falha sem ela, para a página nunca apontar por engano para `localhost`): `VITE_API_URL=https://... npm run build` |

## Organização

```
src/
  config.ts          endereço do backend
  api/               cliente da API, erros do contrato e as operações (auth)
  auth/              a sessão do utilizador (contexto, hook)
  components/        componentes comuns: botão, campo, aviso, carregamento
  layout/            a barra superior das páginas depois de iniciar sessão
  pages/             os ecrãs
  routes/            a proteção das rotas que precisam de sessão
  styles/            variáveis, estilos base, dos componentes e do layout (três larguras)
  test/              preparação e auxiliares dos testes
```

## Regras que valem para todos os ecrãs

Ver [as regras do plano](../matriz/docs/implementation-plan.md#a-interface-regras-para-os-incrementos-9-a-13): os estados de carregamento, vazio, sucesso e erro; formulários que guardam o que foi escrito; botões que não deixam repetir o pedido; um 401 leva ao início de sessão com a mensagem da API; e o layout adapta-se a computador, tablet e telemóvel.
