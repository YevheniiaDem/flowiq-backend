# Flowiq Backend - AI Business Operator

Spring Boot backend для Flowiq - AI-powered платформи для управління бізнесом.

## 🚀 Tech Stack

- **Java:** 17
- **Spring Boot:** 3.5.14
- **Database:** PostgreSQL
- **Build Tool:** Maven
- **Security:** Spring Security + JWT
- **ORM:** Spring Data JPA (Hibernate)

## 📦 Dependencies

### Core
- Spring Boot Starter Web (REST API)
- Spring Boot Starter Data JPA (Database)
- Spring Boot Starter Security (Authentication)
- Spring Boot Starter Validation (DTO validation)

### Database
- PostgreSQL Driver
- Flyway (schema migrations)
- Docker Compose Support

### API Documentation
- springdoc-openapi (Swagger UI + OpenAPI 3)

### Development
- Spring Boot DevTools (Hot reload)
- Lombok (Code generation)

### Testing
- Spring Boot Starter Test
- Spring Security Test

## 🏗️ Project Structure

```
src/main/java/com/flowiq/
├── config/              # Configuration (Security, CORS)
├── controller/          # REST Controllers
├── service/             # Business Logic
├── repository/          # JPA Repositories
├── entity/              # JPA Entities
├── dto/                 # Data Transfer Objects
├── security/            # JWT & Auth
└── exception/           # Exception Handling

src/main/resources/
├── application.properties
└── db/migration/        # Flyway migrations
```

## ⚙️ Configuration

### application.properties

```properties
spring.application.name=flowiq-backend
spring.docker.compose.enabled=false
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

## 🔧 Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 15+ (или Docker)

### Installation

```bash
# Clone repository
git clone https://github.com/YevheniiaDem/flowiq-backend.git
cd flowiq-backend

# Build project
./mvnw clean install

# Run application
./mvnw spring-boot:run
```

### With Docker Compose

```bash
# Start PostgreSQL
docker-compose up -d

# Run application
./mvnw spring-boot:run
```

## 📡 API Endpoints (Planned)

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `POST /api/auth/refresh` - Refresh JWT token
- `GET /api/auth/me` - Get current user

### Dashboard
- `GET /api/dashboard/stats` - Dashboard statistics
- `GET /api/dashboard/health` - Business health score
- `GET /api/dashboard/summary` - AI summary

### Transactions
- `GET /api/transactions` - List transactions
- `POST /api/transactions` - Create transaction
- `PUT /api/transactions/{id}` - Update transaction
- `DELETE /api/transactions/{id}` - Delete transaction

### Analytics
- `GET /api/analytics/revenue` - Revenue analytics
- `GET /api/analytics/expenses` - Expense analytics
- `GET /api/analytics/profit-margin` - Profit margin

### AI Features
- `GET /api/insights` - AI business insights
- `POST /api/insights/generate` - Generate new insights
- `POST /api/chat/message` - AI chat
- `GET /api/forecasts/revenue` - Revenue forecast

### Imports (bank statements)
- `POST /api/imports/upload` - Upload bank CSV/XLSX statement
- `GET /api/imports` - List import jobs
- `GET /api/imports/{id}` - Get import job details

### Bank Integrations (planned — not implemented)

See [Bank Integrations Roadmap](docs/roadmap/BANK_INTEGRATIONS_ROADMAP.md). Endpoints below are **future** only:

- `GET /api/integrations` - List integrations
- `POST /api/integrations/{provider}/connect` - Connect integration
- `POST /api/integrations/{provider}/sync` - Sync data

## 🔐 Security

- JWT-based authentication
- BCrypt password encryption
- CORS configuration
- Role-based access control (ADMIN, USER, VIEWER)
- Request validation

## API Documentation

Interactive API documentation is available via Swagger UI (springdoc-openapi).

### Open Swagger UI

Start the application, then open:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Alternative URL:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

API groups:

| Group | Endpoints |
|-------|-----------|
| **Public APIs** | `/api/health`, `/api/auth/register`, `/api/auth/login` |
| **Protected APIs** | `/api/dashboard`, `/api/analytics`, `/api/transactions`, `/api/imports`, `/api/reports`, `/api/ai-accountant`, `/api/notifications` |

Group-specific OpenAPI JSON:

- Public: `http://localhost:8080/v3/api-docs/public`
- Protected: `http://localhost:8080/v3/api-docs/protected`

### Use JWT token in Swagger

1. Call **POST /api/auth/login** or **POST /api/auth/register** (no auth required).
2. Copy the `token` value from the response.
3. Click the **Authorize** button (lock icon) at the top of Swagger UI.
4. Enter: `Bearer <your-token>` or just paste the token (Swagger adds the `Bearer` prefix automatically).
5. Click **Authorize**, then **Close**.
6. All protected endpoints will now include the JWT in requests.

### Test endpoints

1. Expand any endpoint in Swagger UI.
2. Click **Try it out**.
3. Fill in parameters or request body.
4. Click **Execute** and inspect the response.

For file upload endpoints (e.g. CSV import), use the file picker in the request form.

## Database Migrations

Schema changes are managed with [Flyway](https://flywaydb.org/). Hibernate is configured with `ddl-auto=validate` and does not modify the database at runtime.

Migration files live in `src/main/resources/db/migration/` and follow the naming pattern `V{version}__{description}.sql` (for example, `V3__add_notifications_table.sql`).

### Add a new migration

1. Create the next versioned SQL file in `src/main/resources/db/migration/`.
2. Use a descriptive name after the double underscore, for example `V3__add_column_to_users.sql`.
3. Write idempotent-safe DDL only in that file (one logical change per migration).
4. Start the application or run Flyway manually — pending migrations are applied automatically on startup.

### Run migrations

Migrations run automatically when the application starts and PostgreSQL is available:

```bash
docker-compose up -d
./mvnw spring-boot:run
```

To apply migrations without starting the full app (requires a running database):

```bash
./mvnw flyway:migrate
```

Add the Flyway Maven plugin to `pom.xml` if you want to use CLI migration commands regularly.

### Check Flyway state

**From application logs** — on startup you should see lines such as:

```
Flyway Community Edition ...
Successfully applied N migration(s)
```

**From PostgreSQL** — inspect the history table:

```sql
SELECT installed_rank, version, description, type, script, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
```

**Reset local database** (development only):

```bash
docker-compose down -v
docker-compose up -d
./mvnw spring-boot:run
```

If you migrate an existing database that was previously managed by Hibernate `ddl-auto=update`, either reset the volume or baseline Flyway before the first run (see [Flyway baseline](https://documentation.red-gate.com/fd/baseline-version-184127456.html)).

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

## 📊 Database Schema (Planned)

### Core Tables
- `users` - User accounts
- `companies` - Multi-tenant companies
- `transactions` - Revenue/Expense transactions
- `categories` - Transaction categories
- `insights` - AI-generated insights
- `forecasts` - AI predictions
- `integrations` - Third-party integrations (**planned**, see `docs/roadmap/BANK_INTEGRATIONS_ROADMAP.md`)
- `chat_conversations` - AI chat history
- `chat_messages` - Chat messages

## 🚧 Development Status

### ✅ Completed
- [x] Project setup
- [x] Spring Boot configuration
- [x] Dependencies management
- [x] Git repository initialization

### 🚧 In Progress
- [ ] Database schema & migrations
- [ ] JWT authentication
- [ ] REST API structure
- [ ] Entity models
- [ ] Service layer

### 📋 Planned
- [ ] AI integration (OpenAI/Claude)
- [ ] Third-party integrations (Stripe, QuickBooks)
- [ ] Real-time features (WebSocket)
- [ ] Report generation
- [ ] Email notifications
- [ ] Admin panel

## 🤝 Contributing

This is a private project. For questions, contact the repository owner.

## 📄 License

Proprietary - All Rights Reserved

## 🔗 Related Projects

- [Flowiq Frontend](https://github.com/YevheniiaDem/flowiq-frontend) - Next.js frontend application

## 👨‍💻 Author

Yevheniia Demchuk

---

**Status:** 🚧 In Development  
**Version:** 0.0.1-SNAPSHOT  
**Last Updated:** June 9, 2026
