# Wallet Backend

Spring Boot backend for a digital wallet with:
- JWT authentication
- admin wallet top-up
- user to user transfers
- user top-up requests with admin approval
- PostgreSQL + Flyway
- Swagger UI

## Stack

- Java 17+
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
| `JWT_SECRET` |  | `kYmNZ/GsmJ0BD5lo5UbhcOJzHRycHbehXwS82XH5J4U=` |
| `JWT_EXPIRATION` | `86400000` |
| `SERVER_PORT` | `8080` |
| `JPA_SHOW_SQL` | `true` |

## Run with Docker

From the `backend` directory:

Default: let Docker Compose read values from `.env`:

```bash
cp .env.example .env
docker compose up --build
```

Optional: run without creating `.env` by passing inline environment variables:

Unix shells (`zsh`, `bash`, Git Bash, WSL):

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


API will be available at:
- `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI definition: `http://localhost:8080/v3/api-docs`


Stop containers:

```bash
docker compose down
```

Stop and remove database volume too:

```bash
docker compose down -v
```

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

From the `backend` directory:

Run the full test suite:

```bash
./mvnw test
```

Run a single test class:

```bash
./mvnw -Dtest=FlowTest test
```

## Postman

Postman files are included in `backend/postman`:

- `Wallet-Backend.postman_collection.json`
- `Wallet-Backend.local.postman_environment.json`

Suggested order when testing:

1. import the collection and environment
2. run `Login Admin` and `Login User 1`
3. run user or admin flows with the saved tokens
4. update wallet IDs or request IDs in the environment if your local data differs

## Database structure

The database tables:

- `users`: stores account details, role, and status
- `wallets`: stores each user's wallet, current balance, currency, and wallet status
- `transfers`: stores money movements between wallets
- `top_up_requests`: stores user-submitted top-up requests for admin review
- `wallet_transactions`: stores the wallet ledger for top-ups and transfers with balance before and after each operation

Purpose of the structure:

- keep user identity separate from wallet balances
- track every balance-changing operation with a reference and audit trail
- support both direct admin top-ups and approval-based top-up requests
- make transfer history and wallet transaction history easy to query

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
