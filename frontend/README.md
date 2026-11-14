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

O ícone do usuário no canto superior direito abre as opções de editar a imagem
de perfil e sair. O editor aceita PNG, JPEG ou WebP de até 5 MB, mostra uma prévia
do recorte central e salva uma versão de 256 × 256 pixels vinculada à conta.
A imagem permanece disponível nos próximos acessos.

A interface inclui cadastro, login, resumo do dashboard, criação, edição e
exclusão lógica de empresas, etapas e candidaturas, além da visualização de
candidaturas e da mudança de etapa.

As telas usam rotas independentes: `/` para dashboard e candidaturas,
`/empresas` para cadastrar empresas e `/etapas` para cadastrar etapas.
Cada tela mantém sua listagem e um botão de cadastro que abre o formulário em
um modal: “Cadastrar candidatura”, “Cadastrar empresa” e “Cadastrar etapa”.

Empresas usam modal para editar e oferecem um botão para acessar o site, quando
preenchido com um endereço válido. Etapas usam modal para editar. Candidaturas
possuem páginas próprias: `/candidaturas/:id` para visualizar e
`/candidaturas/:id/editar` para editar o registro selecionado. Após salvar,
a edição retorna à lista de candidaturas.

A exclusão solicita confirmação e preserva os dados no banco. Empresas e
candidaturas usam `deleted_at`; etapas são desativadas com `is_active = false`.
Registros excluídos saem das listas e candidaturas excluídas deixam de compor
os totais. Os vínculos, as anotações e o histórico existentes são preservados.
Uma candidatura pode manter sua empresa excluída ou etapa desativada durante
a edição, mas esses registros não ficam disponíveis para novos vínculos.

Somente o criador pode editar ou excluir uma empresa, e empresas aprovadas
permanecem protegidas contra edição e exclusão direta, conforme as regras da API.

## Build

```powershell
npm run build
```

Saída: `dist/jobtracker-frontend/browser`. O servidor de produção deve servir
`index.html` para as rotas da SPA e encaminhar `/api` à API. O proxy do CLI
funciona apenas durante `npm start`.
