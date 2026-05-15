# registro-ponto-backend

Backend para registro de ponto — Player Contabilidade.

### Requisitos

- Java 21 (LTS)
- Maven 3.8+
- MySQL 5.7+ (recomendado 8.0+; 5.7 funciona, mas o Hibernate emite um warning cosmético sobre o dialect)

### Configuração do ambiente

1. Instale o JDK 21 (ex.: Oracle JDK ou Adoptium Temurin).
2. Configure `JAVA_HOME` apontando para o JDK 21 e adicione `%JAVA_HOME%\bin` ao `PATH`.
3. Crie o banco no MySQL (ou deixe `createDatabaseIfNotExist=true` cuidar disso na primeira execução).

### Variáveis de ambiente

| Variável            | Default                              | Descrição                          |
|---------------------|--------------------------------------|------------------------------------|
| `DB_HOST`           | `localhost`                          | Host do MySQL                      |
| `DB_PORT`           | `3306`                               | Porta do MySQL                     |
| `DB_NAME`           | `registro_ponto`                     | Nome do schema                     |
| `DB_USER`           | `root`                               | Usuário do banco                   |
| `DB_PASSWORD`       | `root`                               | Senha do banco                     |
| `SERVER_PORT`       | `8080`                               | Porta da aplicação                 |
| `JWT_SECRET`        | (valor placeholder — troque em prod) | Segredo HS256 (>= 256 bits)        |
| `JWT_EXPIRATION_MS` | `86400000`                           | Validade do token (ms)             |
| `APP_TIME_ZONE`     | `America/Sao_Paulo`                  | Fuso da aplicação (API, Jackson e `AppTimeService`) |

Datas na API são convertidas via `AppTimeService` (injete em qualquer service). O banco continua em UTC (`Instant`); na resposta JSON o horário sai com offset do fuso configurado (ex.: `-03:00`).

### Execução do backend

Caso o seu MySQL local não esteja na configuração padrão (`localhost:3306`, `root/root`), exporte as variáveis correspondentes na sessão antes de subir a aplicação. Exemplo (PowerShell, MySQL local em `3370` com `admin/admin`):

```powershell
$env:DB_PORT="3370"; $env:DB_USER="admin"; $env:DB_PASSWORD="admin"
mvn spring-boot:run
```

Em ambientes que casam com os defaults, basta:

```bash
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health check: http://localhost:8080/health

### Credenciais de teste

Dois usuários são criados automaticamente no startup pelo `DataSeeder`, caso ainda não existam no banco:

| Username      | Senha       | Role           |
|---------------|-------------|----------------|
| `colaborador` | `12345678`  | `COLLABORATOR` |
| `gerente`     | `87654321`  | `MANAGER`      |

A senha é armazenada como hash BCrypt; o seed é idempotente (não duplica usuário nem colaborador se já existirem). Para cada usuário de teste é criada uma linha em `colaborators` com `first_name` (`Natanael` / `Gerente`) quando ainda não existir.

### Fluxo da aplicação

A autenticação é **stateless via JWT** (HS256, biblioteca `jjwt`):

1. Cliente envia `POST /auth/login` com `{ "username", "password" }`.
2. `AuthenticationManager` valida a senha contra o hash BCrypt do banco através do `CustomUserDetailsService`.
3. Em caso de sucesso, o `JwtService` gera um token com claims `sub`, `roles`, `iat`, `exp`.
4. Em requests subsequentes, o cliente envia o token no header `Authorization: Bearer <token>`.
5. O `JwtAuthenticationFilter` valida o token a cada request e popula o `SecurityContext` com o `UserDetails`.

Endpoints públicos (sem token): `POST /auth/login`, `/health`, Swagger UI/JSON. Qualquer outro endpoint exige token JWT válido — sem token ou com token inválido retorna **401**; com role insuficiente retorna **403**.

### Endpoints de autenticação

| Método | Endpoint        | Auth   | Descrição                                                         |
|--------|-----------------|--------|-------------------------------------------------------------------|
| POST   | `/auth/login`   | nenhum | Recebe credenciais e devolve `{ token, tokenType }`               |
| GET    | `/auth/me`      | Bearer | Devolve `{ username, roles }` do usuário identificado pelo token  |
| GET    | `/v1/colaborator` | Bearer | Devolve `{ user_id, first_name }` do registro em `colaborators` ligado ao usuário do token |

O token JWT continua identificando o usuário pelo **username** (claim `sub`); o endpoint consulta a tabela `colaborators` pelo `user_id` correspondente. Se o usuário existir mas não houver linha de colaborador, a API responde **404** (`ProblemDetail`).

Validações de entrada em `POST /auth/login`:

- `username`: obrigatório, apenas letras (`^[A-Za-z]+$`)
- `password`: obrigatório, exatamente 8 dígitos numéricos (`^\d{8}$`)

Violações de formato retornam **400**; credenciais inválidas retornam **401**.

**Exemplo de login:**

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"colaborador","password":"12345678"}'
```

**Exemplo de uso do token em endpoint protegido:**

```bash
curl http://localhost:8080/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Exemplo de perfil do colaborador autenticado:**

```bash
curl http://localhost:8080/v1/colaborator \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada (campos em *snake_case*): `{"user_id":1,"first_name":"Natanael"}` (valores conforme o banco e o seed).

### Endpoints de atividades planejadas

Permitem ao colaborador informar o que pretende fazer **antes de iniciar a jornada**. Cada registro fica na tabela `planned_activities` (`id`, `collaborator_id`, `description`, `created_at`, `updated_at`), sempre vinculado ao colaborador do token JWT.

| Método | Endpoint                         | Auth   | Descrição                                              |
|--------|----------------------------------|--------|--------------------------------------------------------|
| POST   | `/v1/activities/planned`         | Bearer | Cria atividade: body `{ "description" }` → `{ id, description, created_at }` (201) |
| GET    | `/v1/activities/planned`         | Bearer | Lista atividades do colaborador autenticado: `[ { id, description, created_at }, ... ]` |
| DELETE | `/v1/activities/planned/{id}`    | Bearer | Remove atividade do colaborador → `{ id, description, created_at }` |

O `collaborator_id` é resolvido internamente pelo username do token; o cliente não envia esse campo. Só é possível listar ou excluir atividades do próprio colaborador. Atividade inexistente ou de outro colaborador retorna **404** (`ProblemDetail`).

Validações em `POST /v1/activities/planned`:

- `description`: obrigatório, não vazio, até 500 caracteres

**Exemplo — criar atividade planejada:**

```bash
curl -X POST http://localhost:8080/v1/activities/planned \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"description":"Revisar relatórios contábeis"}'
```

Resposta esperada (201): `{"id":1,"description":"Revisar relatórios contábeis","created_at":"2026-05-15T11:30:00-03:00"}` (`created_at` em ISO-8601, fuso `America/Sao_Paulo`)

**Exemplo — listar:**

```bash
curl http://localhost:8080/v1/activities/planned \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada: `[{"id":1,"description":"Revisar relatórios contábeis","created_at":"2026-05-15T11:30:00-03:00"}]`

**Exemplo — remover:**

```bash
curl -X DELETE http://localhost:8080/v1/activities/planned/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada: `{"id":1,"description":"Revisar relatórios contábeis","created_at":"2026-05-15T11:30:00-03:00"}`

### Testes

A suíte usa **H2 em memória** (modo MySQL) para os testes que precisam de banco. Para rodar:

```powershell
mvn test
```

Para rodar uma classe específica:

```powershell
mvn test -Dtest=AuthControllerTest
mvn test -Dtest=ColaboratorControllerTest
mvn test -Dtest=PlannedActivityControllerTest
```
