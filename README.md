# registro-ponto-backend

Backend para registro de ponto — Player Contabilidade (API REST, Spring Boot, MySQL).

### Repositórios do sistema

| Repositório | Descrição |
|-------------|-----------|
| **registro-ponto-backend** (este) | API, regras de negócio, persistência |
| [registro-ponto-frontend](https://github.com/Sistema-de-Registro-de-Ponto/registro-ponto-frontend) | Flutter Web — colaborador e gestão |
| [registro-ponto-rpa](https://github.com/Sistema-de-Registro-de-Ponto/registro-ponto-rpa) | Robô Python — importação do Portal Ponto Ágil |

### Execução integrada (demo completa com RPA)

Ordem sugerida para validar o fluxo ponta a ponta:

1. **MySQL** acessível com as variáveis `DB_*`.
2. **Backend** (esta pasta) — com `RPA_API_KEY` definida (ex.: `dev-rpa-key-change-me`, igual ao `.env` do RPA):

   ```powershell
   $env:RPA_API_KEY="dev-rpa-key-change-me"
   mvn spring-boot:run
   ```

3. **Portal mock** — pasta `registro-ponto-rpa`: `py serve_mock.py` (porta `5500`).
4. **RPA** — pasta `registro-ponto-rpa`: `py main.py` (importa batidas em `rpa_records`).
5. **Frontend** — pasta `registro-ponto-frontend`: `flutter run -d chrome` → login `gerente` / `87654321` → aba **RPA**.

Detalhes do robô e do portal mock: README do `registro-ponto-rpa`.

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
| `RPA_API_KEY`       | `troque-esta-chave-rpa-em-producao`  | Chave do robô RPA no header `X-Rpa-Api-Key`. Em dev local, use o mesmo valor do `.env` do `registro-ponto-rpa` (ex.: `dev-rpa-key-change-me`) |

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
| `V3__managers.sql` | Tabela `managers` |
| `V4__rpa_records.sql` | Tabela `rpa_records` (importações do portal externo via RPA; equiv. `registros_rpa` do PDF) |

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

Com o perfil `local` (`application-local.yml`) ou `APP_SEED_COLLABORATOR_DEMO=true`, o `CollaboratorDemoDataSeeder` cria colaboradores adicionais com jornadas `completed` de demonstração:

| Username | Senha      | Nome  | Jornadas                                                                 |
|----------|------------|-------|--------------------------------------------------------------------------|
| `natan`  | `12345678` | Natan | 60 dias úteis retroativos a partir de `anchor-date` (padrão local: 16/05/2026) |
| `thais`  | `12345678` | Thais | Dias úteis do mês corrente até hoje (`month-only: true`)                 |

O seed é idempotente por colaborador: se já existirem jornadas `completed` suficientes, não recria. Senha padrão configurável em `app.seed.collaborator-demo.default-password`.

### Fluxo da aplicação

A autenticação é **stateless via JWT** (HS256, biblioteca `jjwt`):

1. Cliente envia `POST /v1/auth/login` com `{ "username", "password" }`.
2. `AuthenticationManager` valida a senha contra o hash BCrypt do banco através do `CustomUserDetailsService`.
3. Em caso de sucesso, o `JwtService` gera um token com claims `sub`, `roles`, `iat`, `exp`.
4. Em requests subsequentes, o cliente envia o token no header `Authorization: Bearer <token>`.
5. O `JwtAuthenticationFilter` valida o token a cada request e popula o `SecurityContext` com o `UserDetails`.

Endpoints públicos (sem JWT): `POST /v1/auth/login`, `POST /v1/rpa/imports` (com header `X-Rpa-Api-Key`), `/health`, Swagger UI/JSON. Demais endpoints exigem token JWT válido — sem token ou com token inválido retorna **401**; com role insuficiente retorna **403**.

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

### Endpoints do gerente

Área de gestão para usuários com role `MANAGER` (credencial de teste: `gerente` / `87654321`). O token JWT identifica o usuário pelo **username**; o perfil consulta a tabela `managers` pelo `user_id` correspondente.

| Método | Endpoint                              | Auth   | Role     | Descrição                                                                 |
|--------|---------------------------------------|--------|----------|---------------------------------------------------------------------------|
| GET    | `/v1/manager`                         | Bearer | qualquer | Perfil do gerente autenticado → `{ user_id, first_name }` (200)           |
| GET    | `/v1/manager/overview`                | Bearer | qualquer | Indicadores agregados do dashboard no período (200)                       |
| GET    | `/v1/manager/collaborators`           | Bearer | MANAGER  | Lista colaboradores (`COLLABORATOR`) com métricas do dia, paginada (200)  |
| GET    | `/v1/manager/collaborators/{id}`      | Bearer | MANAGER  | Detalhe do colaborador e jornada em andamento, se houver (200)            |
| GET    | `/v1/manager/journeys`                | Bearer | MANAGER  | Lista jornadas no período, com filtro opcional por nome (200)             |
| GET    | `/v1/manager/journeys/{id}`           | Bearer | MANAGER  | Detalhe da jornada com atividades (200)                                   |
| GET    | `/v1/manager/reports/consolidated`    | Bearer | MANAGER  | Relatório consolidado: indicadores globais + tabela por colaborador (200) |
| GET    | `/v1/manager/rpa/records`             | Bearer | MANAGER  | Lista registros importados via RPA, paginada (200)                        |

Usuário sem linha em `managers` em `GET /v1/manager` retorna **404** (`ProblemDetail`, título `Gerente não encontrado`). Endpoints de colaboradores e jornadas com token de role `COLLABORATOR` retornam **403**.

#### Perfil do gerente

```bash
curl http://localhost:8080/v1/manager \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada (200): `{"user_id":2,"first_name":"Gerente"}`

#### Visão geral (dashboard)

Agrega métricas de **todas** as jornadas cujo `started_at` cai no intervalo informado (inclusive nos extremos, fuso `APP_TIME_ZONE`). Sem `start_date` e `end_date`, usa o **dia atual**.

| Query         | Obrigatório | Descrição                                      |
|---------------|-------------|------------------------------------------------|
| `start_date`  | não         | Início do período (`YYYY-MM-DD`)               |
| `end_date`    | não         | Fim do período (`YYYY-MM-DD`)                  |

`start_date` posterior a `end_date` retorna **400** (`ProblemDetail`, título `Requisição inválida`).

```bash
curl "http://localhost:8080/v1/manager/overview?start_date=2026-05-01&end_date=2026-05-18" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada (200):

```json
{
  "duration_seconds": 12600,
  "journeys_progress": 1,
  "average_adherence_percentage": 75,
  "activities_completed": 3,
  "unplanned_activities": 2
}
```

| Campo                           | Descrição                                                                 |
|---------------------------------|---------------------------------------------------------------------------|
| `duration_seconds`              | Soma de `duration_seconds` das jornadas `completed` no período            |
| `journeys_progress`             | Quantidade de jornadas `in_progress` com `started_at` no período        |
| `average_adherence_percentage`  | Média inteira da aderência por jornada com atividades planejadas (0 se nenhuma) |
| `activities_completed`          | Atividades planejadas da jornada marcadas (`is_checked` true) no período |
| `unplanned_activities`          | Total de atividades não planejadas registradas no período               |

#### Relatório consolidado

Indicadores globais (`summary`) e tabela paginada por colaborador (`collaborators`) no período informado. Sem datas, usa o **dia atual** (`APP_TIME_ZONE`).

Apenas colaboradores com **ao menos uma jornada** (`started_at` no intervalo) entram na tabela. O `summary` reflete o mesmo conjunto após o filtro `search`.

| Query         | Obrigatório | Default | Descrição                                      |
|---------------|-------------|---------|------------------------------------------------|
| `start_date`  | não         | hoje    | Início do período (`YYYY-MM-DD`)               |
| `end_date`    | não         | hoje    | Fim do período (`YYYY-MM-DD`)                  |
| `search`      | não         | —       | Filtro parcial por `first_name` (case insensitive) |
| `page`        | não         | `0`     | Página da tabela (0-based)                     |
| `size`        | não         | `20`    | Itens por página                               |

`start_date` posterior a `end_date` retorna **400** (`ProblemDetail`, título `Requisição inválida`).

```bash
curl "http://localhost:8080/v1/manager/reports/consolidated?start_date=2026-05-01&end_date=2026-05-18&search=nat&page=0&size=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada (200):

```json
{
  "period": {
    "start_date": "2026-05-01",
    "end_date": "2026-05-18"
  },
  "summary": {
    "duration_seconds": 45000,
    "planned_activities": 42,
    "activities_completed": 30,
    "unplanned_activities": 8,
    "average_adherence_percentage": 71
  },
  "collaborators": {
    "content": [
      {
        "id": 1,
        "first_name": "Natanael",
        "duration_seconds": 12600,
        "planned_activities": 10,
        "activities_completed": 7,
        "unplanned_activities": 2,
        "adherence_percentage": 70
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "size": 20,
    "number": 0,
    "numberOfElements": 1,
    "first": true,
    "empty": false
  }
}
```

| Campo (`summary` e item em `collaborators.content`) | Descrição                                                                 |
|-----------------------------------------------------|---------------------------------------------------------------------------|
| `duration_seconds`                                  | Soma de `duration_seconds` das jornadas **completed** no período          |
| `planned_activities`                                | Total de itens em `journey_planned_activities` no período               |
| `activities_completed`                              | Itens planejados com `is_checked` true no período                         |
| `unplanned_activities`                              | Total em `unplanned_activities` no período                                |
| `average_adherence_percentage` (`summary`)            | Média inteira da aderência por jornada com planejadas (0 se nenhuma)      |
| `adherence_percentage` (linha)                      | Mesma regra, média das jornadas do colaborador no período                 |

#### Listagem de colaboradores

Retorna apenas usuários com role `COLLABORATOR`. Métricas referem-se ao **dia atual** (`APP_TIME_ZONE`).

| Query    | Obrigatório | Default | Descrição                                      |
|----------|-------------|---------|------------------------------------------------|
| `search` | não         | —       | Filtro parcial por `first_name` (case insensitive) |
| `page`   | não         | `0`     | Página (0-based)                               |
| `size`   | não         | `20`    | Itens por página                               |

Paginação no formato Spring `Page` (`content`, `totalElements`, `totalPages`, `last`, `number`, etc.), igual a `GET /v1/manager/journeys`.

```bash
curl "http://localhost:8080/v1/manager/collaborators?search=nat&page=0&size=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada (200):

```json
{
  "content": [
    {
      "id": 1,
      "first_name": "Natanael",
      "current_journey_status": "in_progress",
      "hours_today_seconds": 8100,
      "adherence_percentage": 50
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": { "sorted": true, "unsorted": false, "empty": false },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "empty": false,
  "sort": { "sorted": true, "unsorted": false, "empty": false }
}
```

| Campo (item em `content`)  | Descrição                                                                 |
|----------------------------|---------------------------------------------------------------------------|
| `id`                       | ID em `colaborators`                                                      |
| `first_name`               | Nome cadastrado do colaborador                                            |
| `current_journey_status`   | `in_progress` (jornada aberta hoje), `completed` (finalizou hoje, sem aberta) ou `none` |
| `hours_today_seconds`      | Soma das jornadas `completed` hoje + tempo decorrido da `in_progress`     |
| `adherence_percentage`     | % de atividades planejadas marcadas na jornada **em andamento**; `null` se não houver jornada aberta ou sem atividades planejadas |

| Campo (paginação)  | Descrição                                      |
|--------------------|------------------------------------------------|
| `totalElements`    | Total de colaboradores (com filtro `search`)   |
| `totalPages`       | Total de páginas                               |
| `number`           | Página atual (0-based)                         |
| `size`             | Tamanho da página                              |
| `numberOfElements` | Itens em `content` nesta página                 |
| `last` / `first`   | Última / primeira página                       |
| `empty`            | Sem colaboradores na consulta                  |

#### Detalhe do colaborador

```bash
curl http://localhost:8080/v1/manager/collaborators/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada (200):

```json
{
  "id": 1,
  "user_id": 1,
  "first_name": "Natanael",
  "hours_today_seconds": 8100,
  "adherence_percentage": 50,
  "current_journey": {
    "id": 42,
    "collaborator_id": 1,
    "started_at": "2026-05-18T08:00:00-03:00",
    "ended_at": null,
    "duration_seconds": null,
    "summary": null,
    "status": "in_progress",
    "journey_planned_activities": [],
    "unplanned_activities": [],
    "created_at": "2026-05-18T08:00:00-03:00",
    "updated_at": "2026-05-18T08:00:00-03:00"
  }
}
```

`current_journey` segue o mesmo JSON de `GET /v1/journeys/current` (`JourneyResponse`); é `null` quando não há jornada em andamento. Colaborador inexistente ou que não seja `COLLABORATOR` retorna **404** (`ProblemDetail`, título `Colaborador não encontrado`).

#### Consulta de jornadas

Lista e detalhe de jornadas de **todos** os colaboradores (`COLLABORATOR`). O filtro de período usa `started_at` no fuso `APP_TIME_ZONE` (inclusive nos extremos). Sem `start_date` e `end_date`, usa o **dia atual**. Não há busca livre por texto nem campo de aderência na listagem.

| Query               | Obrigatório | Default | Descrição                                              |
|---------------------|-------------|---------|--------------------------------------------------------|
| `start_date`        | não         | hoje    | Início do período (`YYYY-MM-DD`)                       |
| `end_date`          | não         | hoje    | Fim do período (`YYYY-MM-DD`)                          |
| `collaborator_name` | não         | —       | Filtro parcial por nome do colaborador (case insensitive) |
| `page`              | não         | `0`     | Página (0-based)                                       |
| `size`              | não         | `20`    | Itens por página                                       |

`start_date` posterior a `end_date` retorna **400** (`ProblemDetail`, título `Requisição inválida`).

**Exemplo — listagem (hoje):**

```bash
curl "http://localhost:8080/v1/manager/journeys?page=0&size=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Exemplo — período e filtro por nome:**

```bash
curl "http://localhost:8080/v1/manager/journeys?start_date=2025-05-01&end_date=2025-05-14&collaborator_name=Maria&page=0&size=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada (200) — envelope Spring `Page` (inclui total de registros):

```json
{
  "content": [
    {
      "id": 42,
      "journey_date": "2025-05-14",
      "collaborator_id": 1,
      "collaborator_first_name": "Maria Silva",
      "started_at": "2025-05-14T08:03:00-03:00",
      "ended_at": null,
      "duration_seconds": 8460,
      "status": "in_progress"
    },
    {
      "id": 41,
      "journey_date": "2025-05-13",
      "collaborator_id": 2,
      "collaborator_first_name": "João Santos",
      "started_at": "2025-05-13T08:10:00-03:00",
      "ended_at": "2025-05-13T18:12:00-03:00",
      "duration_seconds": 36120,
      "status": "completed"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": { "sorted": true, "unsorted": false, "empty": false },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 25,
  "totalPages": 3,
  "last": false,
  "first": true,
  "size": 10,
  "number": 0,
  "numberOfElements": 2,
  "empty": false,
  "sort": { "sorted": true, "unsorted": false, "empty": false }
}
```

| Campo (item em `content`) | Descrição                                                                 |
|---------------------------|---------------------------------------------------------------------------|
| `id`                      | ID da jornada (detalhe: `GET /v1/manager/journeys/{id}`)                  |
| `journey_date`            | Data da jornada (`YYYY-MM-DD`, derivada de `started_at`)                  |
| `collaborator_id`         | ID em `colaborators`                                                      |
| `collaborator_first_name` | Nome do colaborador                                                       |
| `started_at`              | Entrada (ISO-8601 com fuso da aplicação)                                  |
| `ended_at`                | Saída; `null` se em andamento                                             |
| `duration_seconds`        | Total em segundos; em andamento = tempo decorrido desde `started_at`      |
| `status`                  | `in_progress` ou `completed`                                              |

| Campo (paginação)   | Descrição                                                                 |
|---------------------|---------------------------------------------------------------------------|
| `content`           | Itens da página atual                                                     |
| `totalElements`     | Total de jornadas no período/filtro (ex.: “de 25”)                        |
| `totalPages`        | Total de páginas com o `size` informado                                   |
| `number`            | Página atual (0-based); próxima requisição: `page = number + 1`          |
| `size`              | Tamanho da página                                                         |
| `numberOfElements`  | Quantidade de itens em `content` nesta página                             |
| `first` / `last`    | Primeira / última página                                                  |
| `empty`             | `true` se não houver jornadas no período                                  |

Texto “1–7 de 25” no front: `início = number * size + 1`, `fim = number * size + numberOfElements`, `total = totalElements` (ajustar quando `empty`).

**Exemplo — detalhe da jornada:**

```bash
curl http://localhost:8080/v1/manager/journeys/42 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada (200) — em andamento (`ended_at`, `duration_seconds` e `summary` como `null`):

```json
{
  "id": 42,
  "collaborator_id": 1,
  "collaborator_first_name": "Maria Silva",
  "started_at": "2025-05-14T08:03:00-03:00",
  "ended_at": null,
  "duration_seconds": null,
  "summary": null,
  "status": "in_progress",
  "journey_planned_activities": [
    {
      "id": 101,
      "planned_activity_id": 5,
      "description": "Revisar relatórios",
      "is_checked": true
    }
  ],
  "unplanned_activities": [],
  "created_at": "2025-05-14T08:03:00-03:00",
  "updated_at": "2025-05-14T10:15:00-03:00"
}
```

Resposta esperada (200) — finalizada:

```json
{
  "id": 41,
  "collaborator_id": 2,
  "collaborator_first_name": "João Santos",
  "started_at": "2025-05-13T08:10:00-03:00",
  "ended_at": "2025-05-13T18:12:00-03:00",
  "duration_seconds": 36120,
  "summary": "Dia produtivo.",
  "status": "completed",
  "journey_planned_activities": [],
  "unplanned_activities": [],
  "created_at": "2025-05-13T08:10:00-03:00",
  "updated_at": "2025-05-13T18:12:00-03:00"
}
```

Jornada inexistente retorna **404** (`ProblemDetail`, título `Jornada não encontrada`).

**Exemplo de login do gerente:**

```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"gerente","password":"87654321"}'
```

#### Registros RPA (importação do portal externo)

Batidas de ponto coletadas pelo robô Python no **Portal Ponto Ágil** (ou mock) são persistidas em `rpa_records` — separadas das jornadas criadas no app (`journeys`). Equivalência com o requisito do PDF: tabela `registros_rpa`.

| Método | Endpoint                 | Auth                         | Role    | Descrição                                      |
|--------|--------------------------|------------------------------|---------|------------------------------------------------|
| POST   | `/v1/rpa/imports`        | `X-Rpa-Api-Key` (`RPA_API_KEY`) | —   | Importa lote de registros (201)                |
| GET    | `/v1/manager/rpa/records`| Bearer                       | MANAGER | Lista importações paginada no período (200)    |

**Importação (simula o robô RPA):**

```bash
curl -X POST http://localhost:8080/v1/rpa/imports \
  -H "Content-Type: application/json" \
  -H "X-Rpa-Api-Key: dev-rpa-key-change-me" \
  -d '{
    "source_system": "ponto_agil",
    "records": [
      {
        "external_employee_id": "001",
        "employee_name": "Natanael",
        "work_date": "2026-05-18",
        "check_in_at": "2026-05-18T08:00:00-03:00",
        "check_out_at": "2026-05-18T17:00:00-03:00",
        "raw_payload": { "portal_row": 1 }
      }
    ]
  }'
```

Resposta esperada (201): `{"imported_count":1,"ids":[1]}`

API key ausente ou inválida retorna **401**. Payload inválido retorna **400**; `check_out_at` anterior a `check_in_at` retorna **400**.

O backend calcula `worked_seconds` quando não informado (diferença entre entrada e saída). Tenta vincular `collaborator_id` pelo `employee_name` (match case-insensitive com `colaborators.first_name`).

**Listagem para o gestor:**

| Query         | Obrigatório | Default | Descrição                                      |
|---------------|-------------|---------|------------------------------------------------|
| `start_date`  | não         | hoje    | Início do período (`YYYY-MM-DD`)               |
| `end_date`    | não         | hoje    | Fim do período (`YYYY-MM-DD`)                  |
| `search`      | não         | —       | Filtro parcial por `employee_name`             |
| `page`        | não         | `0`     | Página (0-based)                               |
| `size`        | não         | `20`    | Itens por página                               |

```bash
curl "http://localhost:8080/v1/manager/rpa/records?start_date=2026-05-01&end_date=2026-05-18&page=0&size=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Resposta esperada (200) — item em `content`:

```json
{
  "id": 1,
  "source_system": "ponto_agil",
  "external_employee_id": "001",
  "employee_name": "Natanael",
  "work_date": "2026-05-18",
  "check_in_at": "2026-05-18T08:00:00-03:00",
  "check_out_at": "2026-05-18T17:00:00-03:00",
  "worked_seconds": 32400,
  "raw_payload": { "portal_row": 1 },
  "imported_at": "2026-05-18T14:30:00-03:00",
  "collaborator_id": 1,
  "collaborator_first_name": "Natanael"
}
```

`collaborator_id` e `collaborator_first_name` vêm `null` quando não há match com colaborador cadastrado.

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
| GET    | `/v1/journeys`                 | Bearer | Histórico de jornadas do colaborador no período, com paginação (200)      |
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

#### Histórico de jornadas

`GET /v1/journeys` lista as jornadas do colaborador autenticado cujo `started_at` cai no intervalo informado (inclusive nos dois extremos, no fuso `APP_TIME_ZONE`). Ordenação: mais recente primeiro (`started_at` decrescente).

Query params:

| Parâmetro     | Obrigatório | Default | Descrição |
|---------------|-------------|---------|-----------|
| `start_date`  | sim         | —       | Início do período (`YYYY-MM-DD`) |
| `end_date`    | sim         | —       | Fim do período (`YYYY-MM-DD`) |
| `page`        | não         | `0`     | Índice da página (zero-based) |
| `size`        | não         | `20`    | Quantidade de registros por página |

Resposta (200): envelope padrão Spring Data `Slice` — lista em `content` (cada item é um `JourneyResponse` completo).

```json
{
  "content": [
    {
      "id": 1,
      "collaborator_id": 1,
      "started_at": "2025-05-14T08:03:00-03:00",
      "ended_at": "2025-05-14T17:30:00-03:00",
      "duration_seconds": 34020,
      "summary": null,
      "journey_planned_activities": [],
      "unplanned_activities": [],
      "status": "completed",
      "created_at": "2025-05-14T08:03:00-03:00",
      "updated_at": "2025-05-14T17:30:00-03:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "unsorted": false, "empty": false },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "first": true,
  "last": false,
  "size": 20,
  "number": 0,
  "numberOfElements": 1,
  "empty": false,
  "sort": { "sorted": true, "unsorted": false, "empty": false }
}
```

Campos principais para o front:

- `content`: itens da página atual.
- `last`: `false` → há próxima página (“Carregar mais”); `true` → última página (ou lista vazia).
- `number`: índice da página atual (zero-based); próxima requisição: `page = number + 1`.
- `empty`: `true` se não houver jornadas no período.

`start_date` posterior a `end_date` retorna **400** (`ProblemDetail`, título `Requisição inválida`).

**Exemplo — primeira página do histórico:**

```bash
curl "http://localhost:8080/v1/journeys?start_date=2025-05-01&end_date=2025-05-14&page=0" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Exemplo — carregar mais (segunda página):**

```bash
curl "http://localhost:8080/v1/journeys?start_date=2025-05-01&end_date=2025-05-14&page=1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

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
mvn test -Dtest=ManagerControllerTest
mvn test -Dtest=RpaImportControllerTest
mvn test -Dtest=ManagerRpaControllerTest
```

### Compartilhamento (avaliação)

Compartilhe os três repositórios com o usuário GitHub **playercontabilidade** (backend, frontend e RPA).
