# JobTracker

Projeto organizado em duas aplicações independentes:

- `backend/`: API Java 21 + Spring Boot, testes, Docker e coleção Postman.
- `frontend/`: aplicação Angular com autenticação JWT e consumo da API.

## Backend

Com Java 21 instalado (o Maven 3.9.9 é baixado pelo Maven Wrapper):

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS, use `./mvnw` no lugar de `.\mvnw.cmd`.

API em `http://localhost:8080`. Instruções e contratos completos em [backend/README.md](backend/README.md).

Para subir API e PostgreSQL com Docker, execute **na raiz**:

```powershell
docker compose up -d --build
```

O Compose da raiz inclui a configuração do backend e preserva o nome do projeto e o volume existente `jobtracker_data`. Requer Docker Compose 2.20+ com suporte a `include`. Mantenha o `.env` na raiz quando executar por ela.

## Frontend

Em outro terminal, com Node.js 24.6+ e npm:

```powershell
cd frontend
npm ci
npm start
```

Abra `http://localhost:4200`. Cadastre uma conta ou faça login. A interface permite consultar o dashboard, cadastrar empresas e etapas e criar e mover candidaturas.

O proxy em `frontend/proxy.conf.json` encaminha `/api/**` para `http://127.0.0.1:8080`, sem mudanças de CORS ou segurança na API. Altere o destino se executar o backend em outra porta.

```powershell
cd frontend
npm run build
```

O build estático fica em `frontend/dist/jobtracker-frontend/browser`. Em produção, configure o servidor web para servir a SPA e encaminhar `/api` ao backend; o proxy do Angular funciona somente no desenvolvimento.

## Verificação do backend

```powershell
cd backend
.\mvnw.cmd test
```

Para rodar também os testes PostgreSQL (`*IT.java`), que exigem o Docker em execução:

```powershell
.\mvnw.cmd verify
```

Detalhes em [backend/README.md](backend/README.md#verificações-e-testes).

## Formatação

O frontend usa Prettier com dois espaços de indentação. Na pasta `frontend`,
execute `npm run format` para formatar ou `npm run format:check` para conferir.

O backend usa Spotless com Google Java Format no estilo AOSP, com quatro espaços
de indentação. Na pasta `backend`, execute `.\mvnw.cmd spotless:apply` para formatar ou
`.\mvnw.cmd spotless:check` para conferir. A primeira execução baixa o formatador pelo Maven.

As configurações ficam em `.editorconfig`, `frontend/.prettierrc` e `backend/pom.xml`.
Os arquivos de texto usam fim de linha LF no repositório, definido em `.gitattributes`.

## Integração contínua

O workflow `.github/workflows/ci.yml` roda em pull requests e na branch `main`, com dois jobs:

- `frontend`: Prettier (`npm run format:check`) e build Angular.
- `backend`: Spotless (`spotless:check`) e testes Java (`verify`, com H2 e PostgreSQL).

Os relatórios de teste ficam nos artefatos da execução. Versões fixadas: Node 24.6.0 com npm 11.5.1,
Temurin 21.0.13, Maven 3.9.9 pelo Maven Wrapper e PostgreSQL 16.4 nos testes.

Para que a falha bloqueie a aprovação, o administrador do repositório precisa criar uma regra de proteção
para `main` em **Settings > Branches**, com "Require status checks to pass before merging" e os checks
`frontend` e `backend` marcados. Essa configuração fica no GitHub e não é versionada no repositório.
