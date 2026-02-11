 # Doc Helper Backend

Spring Boot backend for the Doc Helper project. It powers user authentication, document handling, chat, embeddings + search, usage tracking, and billing workflows.

**What it does**
- Auth: Firebase token auth + email verification and password reset flows
- Documents: upload, store, list, delete, and track document metadata
- Chat: create threads, persist chat messages, and return summaries
- Embeddings + search: generate embeddings and store vectors in pgvector for similarity search
- Usage + limits: record token usage, report daily usage, and enforce plan limits
- Billing: products, prices, subscriptions, and Razorpay webhooks
- Configurable UI: backend-driven UI configuration and feature toggles
- User activity: record and aggregate activity events
- Ops: structured logging, cache, and actuator health endpoints

**Tech stack**
- Java 21, Spring Boot 3.5
- Spring Data JPA + PostgreSQL (pgvector)
- MongoDB (chat metadata and history)
- Redis (cache + buffers)
- Spring AI (chat + embeddings)
- Spring Security, Spring Mail, SpringDoc OpenAPI

**Project structure**
- `src/main/java/com/ayushsingh/doc_helper/core` — shared config (security, cache, logging, AI, OpenAPI)
- `src/main/java/com/ayushsingh/doc_helper/features` — feature modules
- `src/main/resources` — app config and SQL

**Folder structure (overview)**
```text
doc_helper_backend/
  src/
    main/
      java/
        com/ayushsingh/doc_helper/
          core/
            ai/                - chat + embedding config, advisors, tools
            caching/           - Redis cache config + keys
            exception_handling/- error model + global handlers
            firebase/          - Firebase Admin bootstrap
            logging/           - structured logging + MDC
            openapi/           - SpringDoc config
            security/          - auth filters, user context, security config
          features/
            auth/              - auth endpoints + email flows
            chat/              - chat threads, messages, summaries
            doc_summary/       - document summarizer
            doc_util/          - embeddings + local storage helpers
            payments/          - Razorpay webhook + payment records
            product_features/  - dynamic product creation and backend driven ui binding with versions
            ui_components/     - ui components contract (to be used with features)
            usage_monitoring/  - quotas, usage reports, schedulers
            user/              - user + role management
            user_activity/     - activity recording + aggregation
            user_doc/          - document metadata + storage
            user_plan/         - plans, products, prices, subscriptions
      resources/
        application*.yml       - environment config
        db/queries.sql         - SQL helpers
        secrets/               - Firebase service account JSON
  docker-compose.yaml          - local Postgres (pgvector) + Redis
  pom.xml                      - Maven dependencies + build
```

**Getting started**
1. Prerequisites: Java 21, Maven, PostgreSQL with pgvector, Redis, MongoDB.
2. Start local services (Postgres + Redis):

```bash
docker compose up -d
```

3. Create a `.env` file (loaded in `application-dev.yml`) with the required variables:

```bash
DB_CONNECTION_URL=
DB_USERNAME=
DB_PASSWORD=
MONGO_URL=
EMAIL_USERNAME=
EMAIL_PASSWORD=
OPENAI_API_KEY=
OPENAI_BASE_URL=
OPENAI_CHAT_MODEL=
OPENAI_EMBEDDING_MODEL=
WEB_SEARCH_BASE_URL=
WEB_SEARCH_API_KEY=
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
SUBSCRIPTION_EVENT_WEBHOOK_SECRET=
```

4. Add Firebase service account JSON at:
`src/main/resources/secrets/firebase-admin-sdk-service-account-config.json`

5. Run the app with the dev profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Default port is `8086`.

**API docs**
- Swagger UI: `http://localhost:8086/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8086/v3/api-docs`

**Notes**
- Uploads are stored under `uploads/` by default.
- Max upload size is 5MB per file and 10MB per request (see `application-dev.yml`).
- Actuator endpoints are enabled in dev; adjust exposure for production.
