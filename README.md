# registro-ponto-backend

Backend para registro de ponto — Player Contabilidade.

### Requisitos

- Java 25 (LTS)
- Maven 3.8+
- MySQL 5.7+ (recomendado 8.0+; 5.7 funciona, mas o Hibernate emite um warning cosmético sobre o dialect)

### Configuração do ambiente

1. Instale o JDK 25 (ex.: Oracle JDK ou Adoptium Temurin).
2. Configure `JAVA_HOME` apontando para o JDK 25 e adicione `%JAVA_HOME%\bin` ao `PATH`.
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

_(a definir após implementação de autenticação)_

### Fluxo da aplicação

_(a definir)_
