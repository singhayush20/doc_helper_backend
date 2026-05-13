# Doc Helper Backend

Spring Boot backend for Doc Helper. It handles authentication, document upload and summarization, chat over documents, embeddings and vector search, usage tracking, subscriptions, payments, and backend-driven feature configuration.

## What it does

- Firebase-authenticated API with email verification and password reset flows
- Document upload, storage, metadata management, and deletion
- Document summarization with chunking and LLM aggregation
- Chat endpoints with persisted thread/message history and SSE streaming
- Embeddings stored in PostgreSQL with `pgvector`
- Usage monitoring, quotas, daily summaries, and billing-related scheduling
- Billing products, prices, subscriptions, and Razorpay webhook handling
- Backend-driven feature flags and UI component configuration
- User activity recording and aggregation

## Tech stack

- Java 21
- Spring Boot 3.5.x
- Maven
- PostgreSQL + `pgvector`
- MongoDB
- Redis
- Spring AI
- Spring Security
- Firebase Admin SDK
- Flyway
- SpringDoc OpenAPI

## Repository layout

```text
doc_helper_backend/
  src/
    main/
      java/com/ayushsingh/doc_helper/
        DocHelperApplication.java
        core/
          ai/                   Chat and embedding configuration, advisors, web-search tools
          caching/              Redis cache configuration
          constants/            Shared auth constants
          exception_handling/   Global error model and exception handlers
          firebase/             Firebase Admin bootstrap
          logging/              MDC/logging filters
          openapi/              Swagger/OpenAPI configuration
          security/             Firebase auth filters, provider, request user context
        features/
          auth/                 Signup and email/password flows
          chat/                 Chat endpoints, persistence, prompts, citations
          doc_summary/          Documents, chunks, summaries
          doc_util/             Parsing, embeddings, local file storage
          email/                Email sending support
          feature_workflow/     Workflow entities/config
          payments/             Razorpay integration and webhook processing
          product_features/     Feature catalog and product-to-feature mapping
          ui_components/        Backend UI component models/registry
          usage_monitoring/     Quotas, usage reporting, scheduled billing jobs
          user/                 User and role management
          user_activity/        Activity capture and batch flushing
          user_doc/             User-uploaded document metadata APIs
          user_plan/            Billing products, prices, subscriptions
      resources/
        application.yml
        application-dev.yml
        db/migration/           Flyway migrations
        logback-spring.xml
        plantuml/               Diagrams and workflow artifacts
        secrets/                Firebase service account JSON (not committed)
    test/
      java/com/ayushsingh/doc_helper/
        DocHelperApplicationTests.java
  docker-compose.yaml
  pom.xml
  mvnw
  mvnw.cmd
```

## Main entry points

- Application bootstrap: `src/main/java/com/ayushsingh/doc_helper/DocHelperApplication.java`
- Swagger UI: `http://localhost:8086/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8086/v3/api-docs`

Primary API groups:

- `/api/v1/auth`
- `/api/v1/chatbot`
- `/api/v1/documents`
- `/api/v1/summarizer`
- `/api/v1/user-docs`
- `/api/v1/usage`
- `/api/v1/user-activities`
- `/api/v1/billing`
- `/api/v1/billing/razorpay`
- `/api/v1/features`
- `/api/admin/features`

## Local development

### Prerequisites

- Java 21
- Docker
- MongoDB running locally or remotely
- Firebase service account credentials
- API credentials for the external providers you intend to use

### Start local infrastructure

The checked-in Docker Compose file starts PostgreSQL with `pgvector` and Redis:

```bash
docker compose up -d
```

`docker-compose.yaml` does not start MongoDB, so you need to run MongoDB separately and point `MONGO_URL` at it.

### Environment variables

The `dev` profile imports `.env` from the repo root. A minimal `.env` looks like:

```bash
DB_CONNECTION_URL=
DB_USERNAME=
DB_PASSWORD=
MONGO_URL=
EMAIL_USERNAME=
EMAIL_PASSWORD=
OPENAI_API_KEY=
WEB_SEARCH_BASE_URL=
WEB_SEARCH_API_KEY=
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
SUBSCRIPTION_EVENT_WEBHOOK_SECRET=
```

Optional overrides supported by `application-dev.yml`:

```bash
OPENAI_BASE_URL=
OPENAI_CHAT_API_KEY=
OPENAI_CHAT_BASE_URL=
OPENAI_CHAT_DEFAULT_MODEL=
OPENAI_EMBEDDING_API_KEY=
OPENAI_EMBEDDING_BASE_URL=
OPENAI_EMBEDDING_MODEL=
OPENAI_EMBEDDING_DIMENSIONS=
OLLAMA_API_KEY=
OLLAMA_BASE_URL=
```

### Firebase credentials

Place the Firebase Admin SDK JSON at:

```text
src/main/resources/secrets/firebase-admin-sdk-service-account-config.json
```

The current code loads that exact filesystem path at startup, so the file must exist there when running locally.

### Run the application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The app runs on port `8086` by default.

## Build and test

```bash
./mvnw test
./mvnw package
```

Current automated test coverage is minimal. The repository currently contains a single Spring context load test.

## Runtime notes

- Uploads are stored under `uploads/`
- Multipart upload limits are 5 MB per file and 10 MB per request
- Flyway is enabled in the `dev` profile
- JPA schema mode is `validate`, so the database schema must already match the entities/migrations
- Actuator endpoints are exposed in the current config; tighten that for production
