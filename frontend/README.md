# JobTracker Frontend

Aplicação Angular 21 criada com Angular CLI 21.2.29.

## Desenvolvimento

Com Node.js 24.6+ e npm, nesta pasta:

```powershell
npm ci
npm start
```

Abra http://localhost:4200 e mantenha o backend em execução na porta 8080.
O arquivo `proxy.conf.json` encaminha `/api/**` para a API. O serviço em
`src/app/api.ts` centraliza HTTP, sessão e envio do JWT. A sessão fica no
sessionStorage da aba e é removida ao sair ou receber HTTP 401.

A interface inclui cadastro, login, resumo do dashboard, criação de empresas,
etapas e candidaturas, listagem e mudança de etapa. Os demais endpoints continuam
disponíveis na API e na documentação Swagger.

As telas usam rotas independentes: `/` para dashboard e candidaturas,
`/empresas` para cadastrar empresas e `/etapas` para cadastrar etapas.
Cada tela mantém sua listagem e um botão de cadastro que abre o formulário em
um modal: “Cadastrar candidatura”, “Cadastrar empresa” e “Cadastrar etapa”.

## Build

```powershell
npm run build
```

Saída: `dist/jobtracker-frontend/browser`. O servidor de produção deve servir
`index.html` para as rotas da SPA e encaminhar `/api` à API. O proxy do CLI
funciona apenas durante `npm start`.
