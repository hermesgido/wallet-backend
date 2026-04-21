# Wallet Backend

Small Spring Boot backend for a digital wallet with:
- JWT authentication
- admin wallet top-up
- user to user transfers
- user top-up requests with admin approval
- PostgreSQL + Flyway
- Swagger UI

## Stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Docker / Docker Compose

## Default demo users

When running with the `dev` profile, the app seeds:
- `admin@demo.com` / `Password@123`
- `user1@demo.com` / `Password@123`
- `user2@demo.com` / `Password@123`

## Configuration

The app reads configuration from environment variables.

| Variable | Default |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/walletdb` |
| `DB_USERNAME` | `walletdbuser` |
| `DB_PASSWORD` | `walletdbpass` |
| `JWT_SECRET` | built-in dev default |
| `JWT_EXPIRATION` | `86400000` |
| `SERVER_PORT` | `8080` |
| `JPA_SHOW_SQL` | `true` |

## Run locally

Start PostgreSQL first, then run:

```bash
./mvnw spring-boot:run
```

Or with explicit env vars:

```bash
DB_URL=jdbc:postgresql://localhost:5432/walletdb \
DB_USERNAME=walletdbuser \
DB_PASSWORD=walletdbpass \
JWT_SECRET=ZwHb76tYhO6UCtde6fdjaMkYS5SeLfruZ0LOB6WyOuY= \
./mvnw spring-boot:run
```

## Run tests

```bash
./mvnw test
```

## Run with Docker

From the `backend` directory:

Option 1: create a local env file first:

```bash
cp .env.example .env
```

Update `JWT_SECRET` in `.env` before starting the app.

```bash
docker compose up --build
```

Option 2: run with inline environment variables:

```bash
POSTGRES_DB=walletdb \
POSTGRES_USER=walletdbuser \
POSTGRES_PASSWORD=walletdbpass \
DB_URL=jdbc:postgresql://db:5432/walletdb \
DB_USERNAME=walletdbuser \
DB_PASSWORD=walletdbpass \
JWT_SECRET=kYmNZ/GsmJ0BD5lo5UbhcOJzHRycHbehXwS82XH5J4U= \
JWT_EXPIRATION=86400000 \
SPRING_PROFILES_ACTIVE=dev \
SERVER_PORT=8080 \
JPA_SHOW_SQL=false \
docker compose up --build
```

Generate a sample JWT secret with:

```bash
openssl rand -base64 32
```

API will be available at:
- `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Stop containers:

```bash
docker compose down
```

Stop and remove database volume too:

```bash
docker compose down -v
```

## Main endpoints

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`

### User

- `POST /api/transfers`
- `POST /api/top-up-requests`
- `GET /api/top-up-requests/my`

### Admin

- `POST /api/admin/wallets/{walletId}/top-up`
- `GET /api/admin/top-up-requests`
- `POST /api/admin/top-up-requests/{id}/approve`
- `POST /api/admin/top-up-requests/{id}/reject`

## Assumptions

- one wallet per user
- single currency: `TZS`
- users can only transfer from their own wallet
- users can only request top-up for their own wallet
- admin top-up is immediate
- top-up request approval credits the wallet and records a wallet transaction

## Notes

- Flyway migrations run automatically on startup
- `ddl-auto` is set to `validate`
- Docker setup uses PostgreSQL 16 and starts the backend after the database is healthy
- committed config now uses `.env.example`; real values should stay in local `.env`
