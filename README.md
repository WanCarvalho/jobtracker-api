# JobTracker

Projeto organizado em duas aplicações independentes:

- `backend/`: API Java 21 + Spring Boot, testes, Docker e coleção Postman.
- `frontend/`: aplicação Angular com autenticação JWT e consumo da API.

## Backend

Com Java 21 e Maven 3.9+:

```powershell
cd backend
mvn spring-boot:run
```

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
mvn test
```
