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
- Docker Compose Support

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

### Integrations
- `GET /api/integrations` - List integrations
- `POST /api/integrations/{provider}/connect` - Connect integration
- `POST /api/integrations/{provider}/sync` - Sync data

## 🔐 Security

- JWT-based authentication
- BCrypt password encryption
- CORS configuration
- Role-based access control (ADMIN, USER, VIEWER)
- Request validation

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
- `integrations` - Third-party integrations
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
