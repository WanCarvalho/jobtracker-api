# JobTracker API

API REST para registrar e acompanhar candidaturas de emprego. Backend em Java 21 + Spring Boot 3.4.5.

## O que está implementado

- Cadastro e login com JWT.
- Perfil `USER` e `SUPER_ADMIN`.
- Empresas colaborativas com moderação (`PENDING`, `APPROVED`, `REJECTED`).
- Etapas personalizadas por usuário.
- CRUD de candidaturas e movimentação de etapa.
- Histórico de movimentações e observações.
- Dashboard com resumo e distribuição por etapa.
- Flyway, H2 para execução rápida e PostgreSQL via Docker.
- Swagger/OpenAPI.

## Requisitos

- JDK 21
- Docker Desktop (opcional, para PostgreSQL)
- Maven 3.9+ (o Maven Wrapper não foi incluído neste pacote).

## Executar rapidamente com H2

```powershell
.\mvnw.cmd spring-boot:run
```

Execute `mvn spring-boot:run` (ou gere um wrapper com `mvn wrapper:wrapper` e depois use `./mvnw spring-boot:run`).

A aplicação inicia em `http://localhost:8080`. A documentação fica em `http://localhost:8080/swagger-ui.html`.

Banco H2 em memória: `jdbc:h2:mem:jobtracker`. Console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:jobtracker`, usuário `sa`, senha vazia).

## Executar backend e PostgreSQL em containers

```powershell
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

Requer Docker Compose v2. O build compila e executa os testes com H2 dentro de uma imagem Maven/JDK 21. A imagem final contém apenas o JRE e a aplicação, executada sem root. Não é necessário instalar Java ou Maven no host para este fluxo.

A API fica em `http://localhost:8080`, Swagger em `http://localhost:8080/swagger-ui.html` e saúde em `http://localhost:8080/actuator/health`. O backend aguarda o healthcheck do PostgreSQL e conecta em `postgres:5432` pela rede do Compose. Para o DBeaver, use `127.0.0.1:5433`. Pare a API executada pelo Maven antes de subir o container, para liberar a porta 8080.

As credenciais padrão são para desenvolvimento. Para sobrescrevê-las, configure `DATABASE_PASSWORD`, `JWT_SECRET`, `SUPERADMIN_EMAIL` e `SUPERADMIN_PASSWORD` em um arquivo `.env` local, ignorado pelo Git e pelo build Docker. Em um volume existente, mudar `DATABASE_PASSWORD` não altera a senha do usuário PostgreSQL; ela precisa ser atualizada no banco. O bootstrap também não deve ser usado como mecanismo de troca da senha de um administrador existente.

O volume `jobtracker_data` é reutilizado. `docker compose down` mantém os dados; `docker compose down -v` os apaga. Volume persistente não substitui backup.

Esta composição atende ao desenvolvimento e à execução em um único host. Para produção, use imagens versionadas publicadas por CI, gestão de segredos, backups e monitoramento. Coloque um proxy com HTTPS diante da API; para várias réplicas, substitua a publicação fixa de 8080 por balanceamento no proxy/orquestrador, dimensione o pool de conexões e execute migrações como etapa controlada do deploy. Considere PostgreSQL gerenciado para disponibilidade e recuperação. Um frontend pode ser outro serviço; se for uma SPA estática, também pode ser publicado em CDN. O navegador deve acessar uma URL pública da API ou `/api` pelo proxy, pois o nome `backend` só resolve dentro da rede Docker.

## Executar a API localmente com PostgreSQL no Docker

```powershell
docker compose up -d postgres
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DATABASE_URL="jdbc:postgresql://127.0.0.1:5433/jobtracker"
mvn spring-boot:run
```

O PostgreSQL do Docker usa `127.0.0.1:5433` no Windows e a porta `5432` dentro do container, permitindo manter outra instalação local na porta `5432`. No DBeaver, use host `127.0.0.1`, porta `5433` e banco, usuário e senha `jobtracker`. O volume `jobtracker_data` mantém os dados ao recriar o container; não use `docker compose down -v` para preservar os dados.

Credenciais locais do compose: banco `jobtracker`, usuário `jobtracker`, senha `jobtracker`.

## Superadmin inicial

Por padrão, o sistema cria automaticamente:

- E-mail: `admin@jobtracker.local`
- Senha: `Admin123!`

Altere `SUPERADMIN_EMAIL`, `SUPERADMIN_PASSWORD` e `JWT_SECRET` em ambientes reais. Nunca use os valores padrão em produção.

## Autenticação

1. `POST /api/auth/register`
2. `POST /api/auth/login`
3. Envie o token recebido em `Authorization: Bearer <token>`.

Exemplo de cadastro:

```json
{
  "name": "Maria",
  "email": "maria@example.com",
  "password": "SenhaForte123"
}
```

## Fluxo sugerido para testar

1. Faça login como superadmin e guarde o token.
2. Cadastre um usuário em `/api/auth/register`.
3. Faça login como usuário.
4. Crie uma etapa em `POST /api/stages`:
```json
{"name":"Inscrição realizada","position":0,"color":"#64748b","isFinal":false}
```
5. Crie uma empresa em `POST /api/companies`:
```json
{"name":"Empresa Exemplo","cnpj":"12345678000199","website":"https://example.com","description":"Empresa de demonstração"}
```
6. A empresa ficará `PENDING` e visível ao criador e ao superadmin. O criador já pode usá-la em sua candidatura.
7. Como superadmin, consulte `GET /api/admin/companies/pending` e aprove com `PATCH /api/admin/companies/{id}/approve`.
8. Como outro usuário, a empresa aprovada aparecerá em `GET /api/companies`.
9. Crie candidatura em `POST /api/applications`:
```json
{
  "companyId": 1,
  "stageId": 1,
  "jobTitle": "Desenvolvedor Java",
  "salary": 5000.00,
  "jobUrl": "https://example.com/vaga",
  "description": "Vaga backend",
  "appliedAt": "2026-11-05"
}
```
10. Mova a candidatura com `PATCH /api/applications/{id}/stage`:
```json
{"stageId":2,"notes":"Entrevista técnica agendada."}
```
11. Consulte `GET /api/applications/{id}/history`, `/notes`, `/api/dashboard/summary` e `/api/dashboard/status-distribution`.

## Rotas

### Públicas
- `POST /api/auth/register`
- `POST /api/auth/login`

### Autenticadas
- `GET /api/auth/me`
- `/api/companies` (GET, POST)
- `/api/companies/{id}` (GET, PUT, DELETE)
- `/api/stages` (GET, POST)
- `/api/stages/{id}` (PUT)
- `/api/stages/{id}/deactivate` (PATCH)
- `/api/stages/reorder` (PATCH)
- `/api/applications` (GET, POST)
- `/api/applications/{id}` (GET, PUT, DELETE)
- `/api/applications/{id}/stage` (PATCH)
- `/api/applications/{id}/history` (GET)
- `/api/applications/{id}/notes` (GET, POST)
- `/api/dashboard/summary`
- `/api/dashboard/status-distribution`

### Somente SUPER_ADMIN
- `GET /api/admin/companies`
- `GET /api/admin/companies/pending`
- `PATCH /api/admin/companies/{id}/approve`
- `PATCH /api/admin/companies/{id}/reject`

## Observações e limitações desta primeira versão

- A lista de candidaturas está pronta para receber filtros/paginação em uma próxima iteração; nesta entrega retorna a lista do usuário.
- A normalização de nome e CNPJ evita duplicatas já aprovadas, mas não substitui uma rotina robusta de deduplicação/fuzzy matching.
- As etapas são pessoais por usuário; cada usuário configura seu próprio Kanban.
- CNPJ é opcional. A validação de dígitos verificadores e unicidade global pode ser adicionada em seguida.
- Para produção, revisar CORS, segredo JWT, política de senha, rate limiting, logs e estratégia de backup.
- A edição de empresa rejeitada a reenvia para `PENDING`; empresa aprovada não pode ser editada diretamente.

## Estrutura de pacotes

O projeto usa uma estrutura por camada, diretamente em `com.jobtracker`, sem
subpacotes por dominio:

```text
com.jobtracker
|-- controller  # Todos os controllers REST
|-- service     # Servicos de negocio, JWT e carregamento de usuarios
|-- repository  # Todos os repositorios Spring Data
|-- dto         # Contratos de entrada e saida, um record por arquivo
|-- entity      # Todas as entidades JPA
|-- enums       # Perfis e status
|-- config      # Configuracao de seguranca e bootstrap
|-- security    # Filtro de autenticacao JWT
|-- exception   # Tratamento global de excecoes
`-- JobTrackerApplication.java
```

Os controllers delegam as regras de negocio e consultas aos services. Os DTOs
preservam seus campos JSON e validacoes. A reorganizacao de pacotes preserva
rotas, metodos HTTP, transacoes e regras de seguranca, sem alterar o banco.

Os testes de regressao em `ApiRegressionTests` cobrem JWT, permissoes administrativas,
isolamento por usuario, validacao, candidaturas, historico, notas e dashboard.
Execute com `mvn test`.
