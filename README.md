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

### Migrações de banco (Flyway)

O schema é versionado em `src/main/resources/db/migration/`. Na subida da aplicação o Flyway aplica as migrações pendentes automaticamente — **não é necessário rodar SQL manual em produção**.

| Arquivo | Conteúdo |
|---------|----------|
| `V1__init_schema.sql` | Schema completo para banco **novo** |
| `V2__journey_checkout_and_planned_activity_snapshot.sql` | Evolução idempotente: `duration_seconds`, `summary`, `snapshot_planned_activity_id`, `planned_activity_id` nullable |

Configuração (`application.yml`):

- `spring.jpa.hibernate.ddl-auto: validate` — o Hibernate **não** altera mais tabelas em runtime; só valida o mapeamento.
- `spring.flyway.baseline-on-migrate: true` e `baseline-version: 1` — banco **já existente** (criado antes pelo Hibernate) recebe baseline na V1 e executa apenas **V2+**, preservando dados.

**Produção (primeiro deploy com Flyway):** suba a aplicação normalmente; o Flyway registra o baseline e aplica `V2`. Confira em `flyway_schema_history`.

**Ambiente novo:** executa `V1` (cria tabelas) e `V2` (ajustes idempotentes).

**Novas alterações:** crie sempre `V3__descricao.sql`, `V4__...` (nunca edite migrações já aplicadas em produção).

Os testes automatizados desabilitam o Flyway e usam H2 com `ddl-auto: create-drop` (`src/test/resources/application.properties`).

### Credenciais de teste

Dois usuários são criados automaticamente no startup pelo `DataSeeder`, caso ainda não existam no banco:

| Username      | Senha       | Role           |
|---------------|-------------|----------------|
| `colaborador` | `12345678`  | `COLLABORATOR` |
| `gerente`     | `87654321`  | `MANAGER`      |

A senha é armazenada como hash BCrypt; o seed é idempotente (não duplica usuário nem colaborador se já existirem). Para cada usuário de teste é criada uma linha em `colaborators` com `first_name` (`Natanael` / `Gerente`) quando ainda não existir.

### Fluxo da aplicação

A autenticação é **stateless via JWT** (HS256, biblioteca `jjwt`):

1. Cliente envia `POST /v1/auth/login` com `{ "username", "password" }`.
2. `AuthenticationManager` valida a senha contra o hash BCrypt do banco através do `CustomUserDetailsService`.
3. Em caso de sucesso, o `JwtService` gera um token com claims `sub`, `roles`, `iat`, `exp`.
4. Em requests subsequentes, o cliente envia o token no header `Authorization: Bearer <token>`.
5. O `JwtAuthenticationFilter` valida o token a cada request e popula o `SecurityContext` com o `UserDetails`.

Endpoints públicos (sem token): `POST /v1/auth/login`, `/health`, Swagger UI/JSON. Qualquer outro endpoint exige token JWT válido — sem token ou com token inválido retorna **401**; com role insuficiente retorna **403**.

### Endpoints de autenticação

| Método | Endpoint        | Auth   | Descrição                                                         |
|--------|-----------------|--------|-------------------------------------------------------------------|
| POST   | `/v1/auth/login`   | nenhum | Recebe credenciais e devolve `{ token, tokenType }`               |
| GET    | `/v1/collaborator` | Bearer | Devolve `{ user_id, first_name }` do registro em `colaborators` ligado ao usuário do token |

O token JWT continua identificando o usuário pelo **username** (claim `sub`); o endpoint consulta a tabela `colaborators` pelo `user_id` correspondente. Se o usuário existir mas não houver linha de colaborador, a API responde **404** (`ProblemDetail`).

Validações de entrada em `POST /v1/auth/login`:

- `username`: obrigatório, apenas letras (`^[A-Za-z]+$`)
- `password`: obrigatório, exatamente 8 dígitos numéricos (`^\d{8}$`)

Violações de formato retornam **400**; credenciais inválidas retornam **401**.

**Exemplo de login:**

```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"colaborador","password":"12345678"}'
```

**Exemplo de perfil do colaborador autenticado:**

```bash
curl http://localhost:8080/v1/collaborator \
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

### Endpoints de jornadas

Registram o **check-in** (início da jornada) do colaborador autenticado. O horário de entrada (`started_at`) é definido automaticamente no servidor. As atividades do backlog (`planned_activities` do colaborador) são vinculadas à jornada na tabela de associação `journey_planned_activities`, com cópia da `description` e `is_checked` inicial `false` no JSON da API (coluna interna `checked`).

| Método | Endpoint                       | Auth   | Descrição                                                                 |
|--------|--------------------------------|--------|---------------------------------------------------------------------------|
| GET    | `/v1/journeys/current`         | Bearer | Retorna a jornada `in_progress` do colaborador (200)                      |
| POST   | `/v1/journeys/start`           | Bearer | Check-in: inicia jornada; body `{}` → JSON da jornada (201)               |
| POST   | `/v1/journeys/current/end`     | Bearer | Check-out: encerra a jornada em andamento; body `{ "summary" }` opcional → mesmo JSON de jornada do check-in (200) |
| POST   | `/v1/journeys/{id}/activities/unplanned` ou `.../unplanned/` | Bearer | Adiciona atividade não planejada à jornada `{id}`: body `{ "description" }` → `{ id, journey_id, description, created_at }` (201) |
| DELETE | `/v1/journeys/activities/unplanned/{id}` | Bearer | Remove a atividade não planejada pelo `id` do registro → mesmo corpo que no POST (200) |
| PUT    | `/v1/journeys/activities/planned/{id}` | Bearer | Marca/desmarca item: URL com o `id` do vínculo; body `{ "is_checked" }` → mesmo formato de um elemento de `journey_planned_activities` (200) |

Modelo de dados:

- `journeys`: `id`, `collaborator_id`, `started_at`, `ended_at` (null enquanto em andamento), `duration_seconds` (definido no encerramento), `summary` (opcional, texto livre no check-out), `status` (`in_progress` \| `completed`), `created_at`, `updated_at`
- `journey_planned_activities`: vínculo jornada ↔ atividade planejada, com `description` (snapshot), coluna `checked` (mapeada no JSON como `is_checked`)
- `unplanned_activities`: `id`, `journey_id`, `description`, `created_at` (JSON em ISO-8601 com o fuso da aplicação)

Regras:

- Só pode existir **uma** jornada `in_progress` por colaborador; segundo `POST /v1/journeys/start` retorna **409** (`ProblemDetail`, título `Jornada em andamento`).
- `GET /v1/journeys/current` sem jornada ativa retorna **404** (`ProblemDetail`, título `Jornada não encontrada`).
- O backlog em `/v1/activities/planned` **não é removido** no check-in; o CRUD de atividades planejadas permanece igual.
- **Encerramento (check-out)** com `POST /v1/journeys/current/end`: só é permitido com jornada **`in_progress`**. O servidor grava `ended_at`, calcula `duration_seconds` entre `started_at` e o encerramento, aplica `status` `completed` e persiste o `summary` se enviado (opcional). Se não houver jornada em andamento, retorna **404** (mesmo título que `GET /current` sem jornada).
- Após `completed`, **nenhuma alteração** é permitida na jornada (regras abaixo reforçam isso para atividades e marcações).
- **Atividades não planejadas** (`unplanned_activities` no JSON da jornada): só podem ser **incluídas ou removidas** enquanto a jornada estiver **em andamento** (`status` `in_progress` e `ended_at` nulo). Se a jornada estiver finalizada (`completed` ou `ended_at` preenchido), `POST` e `DELETE` retornam **409** (`ProblemDetail`, título `Jornada não pode ser alterada`). Jornada inexistente, de outro colaborador ou não pertencente ao token no `POST` retorna **404** (`Jornada não encontrada`). Atividade não planejada inexistente ou de outro colaborador no `DELETE` retorna **404** (`Atividade não planejada não encontrada`).
- Para marcar ou desmarcar um item vinculado à jornada em andamento, use `PUT /v1/journeys/activities/planned/{id}` com o `id` de cada elemento em `journey_planned_activities` na URL e body `{ "is_checked": true|false }`.

Em jornada em andamento, `ended_at`, `duration_seconds` e `summary` vêm como `null` no JSON. Após o encerramento, passam a vir preenchidos conforme o check-out.

Resposta esperada no check-in (201):

```json
{
  "id": 1,
  "collaborator_id": 1,
  "started_at": "2026-05-15T08:00:00-03:00",
  "ended_at": null,
  "duration_seconds": null,
  "summary": null,
  "status": "in_progress",
  "journey_planned_activities": [
    {
      "id": 1,
      "planned_activity_id": 3,
      "description": "Revisar relatórios contábeis",
      "is_checked": false
    }
  ],
  "unplanned_activities": [],
  "created_at": "2026-05-15T08:00:00-03:00",
  "updated_at": "2026-05-15T08:00:00-03:00"
}
```

**Exemplo — consultar jornada em andamento:**

```bash
curl http://localhost:8080/v1/journeys/current \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Exemplo — iniciar jornada (check-in):**

```bash
curl -X POST http://localhost:8080/v1/journeys/start \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Exemplo — encerrar jornada (check-out) com resumo opcional:**

```bash
curl -X POST http://localhost:8080/v1/journeys/current/end \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"summary":"Fechei todas as pendências; reunião com cliente no período da tarde."}'
```

Resposta esperada (200): mesmo formato do check-in (`JourneyResponse`), com `status` `completed`, `ended_at` preenchido, `duration_seconds` calculado pelo servidor e `summary` com o texto enviado (ou `null` se usar `{}` ou `{"summary":null}`).

Validações em `POST /v1/journeys/current/end`:

- `summary`: opcional; se presente, até 2000 caracteres

Validações em `POST /v1/journeys/{id}/activities/unplanned` (mesmo formato de `POST /v1/activities/planned`):

- `description`: obrigatório, não vazio, até 500 caracteres

**Exemplo — adicionar atividade não planejada:**

```bash
curl -X POST http://localhost:8080/v1/journeys/1/activities/unplanned \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"description":"Atendimento emergencial"}'
```

Resposta esperada (201): `{"id":1,"journey_id":1,"description":"Atendimento emergencial","created_at":"2026-05-15T10:00:00-03:00"}`

**Exemplo — remover atividade não planejada:**

```bash
curl -X DELETE http://localhost:8080/v1/journeys/activities/unplanned/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada (200): mesmo JSON do item criado (último estado antes da exclusão).

O segmento `{id}` na URL do `PUT /v1/journeys/activities/planned/{id}` é o `id` retornado em `journey_planned_activities` em `GET`/`POST /v1/journeys/start`. Se não existir, não pertencer ao colaborador do token ou a jornada não estiver em andamento (`ended_at` nulo e `status` `in_progress`), a API responde **404** (`ProblemDetail`, título `Atividade da jornada não encontrada`).

Validações do body de marcação:

- `is_checked`: obrigatório (boolean)

**Exemplo — marcar atividade como feita:**

```bash
curl -X PUT http://localhost:8080/v1/journeys/activities/planned/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"is_checked":true}'
```

Resposta esperada (200): `{"id":1,"planned_activity_id":3,"description":"Revisar relatórios contábeis","is_checked":true}`

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
mvn test -Dtest=JourneyControllerTest
mvn test -Dtest=JourneyPlannedActivityControllerTest
mvn test -Dtest=JourneyUnplannedActivityControllerTest
```
