# Enterprise Knowledge Base & Intelligent Operations Agent Platform

A secure Spring Boot backend for enterprise knowledge management and intelligent operations workflows. The platform currently provides identity and access control, tenant-scoped knowledge bases, document ingestion, chat persistence, and operational safeguards. Its roadmap extends the current backend into a complete RAG and tool-calling agent platform.

## Current Status

Stage 4, Day 1 is complete. The current release includes:

- Stateless authentication with Spring Security and signed JWTs
- `USER` and `ADMIN` role authorization
- Resource ownership derived exclusively from the authenticated JWT subject
- Redis-backed token revocation and Lua-based fixed-window rate limiting
- Knowledge base, document, chunk, chat session, and chat message persistence
- Secure UTF-8 TXT and Markdown uploads
- Configurable local file storage with generated paths and SHA-256 checksums
- Document parsing, text cleanup, processing status, failure tracking, and retry support
- OpenAPI documentation and automated unit, web, security, persistence, and integration tests

PDF parsing, automatic chunking, vector storage, retrieval-augmented generation, model integration, and operations tools are planned for later milestones.

## Architecture

```text
HTTP request
  -> Spring Security filter chain
  -> JWT validation and Redis revocation check
  -> Controller and request validation
  -> Resource ownership checks
  -> Application service and transaction boundary
  -> Document ingestion / domain service
  -> Storage, parser, and text-cleaning strategies
  -> MyBatis mapper
  -> MySQL
  -> Standard Result or PageResult response
```

Long-lived business data is stored in MySQL. Redis contains short-lived operational state such as revoked token identifiers and rate-limit counters. Uploaded source files are stored beneath a configurable root and are never addressed using the original filename.

## Technology Stack

- Java 26
- Spring Boot 4.1.0
- Spring Security 7.1.0
- OAuth2 Resource Server and Nimbus JWT
- MyBatis Spring Boot Starter 4.0.1
- MySQL 8+
- Redis 7+
- springdoc-openapi 3.0.3
- Maven
- JUnit 5, Mockito, and MockMvc

## Requirements

- JDK 26+
- Maven 3.9+
- MySQL 8+
- Redis 7+
- Docker Desktop is optional and can be used to run Redis locally

## Quick Start

### 1. Create the database

From the repository root, create a local database and apply the schema:

```powershell
$projectPath = (Get-Location).Path.Replace('\', '/')

mysql -u root -p --execute="CREATE DATABASE IF NOT EXISTS enterprise_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
mysql -u root -p --database=enterprise_agent --execute="source $projectPath/sql/001_init_schema.sql"
mysql -u root -p --database=enterprise_agent --execute="source $projectPath/sql/002_add_document_title_unique.sql"
mysql -u root -p --database=enterprise_agent --execute="source $projectPath/sql/003_complete_chat_schema.sql"
mysql -u root -p --database=enterprise_agent --execute="source $projectPath/sql/004_align_user_schema.sql"
mysql -u root -p --database=enterprise_agent --execute="source $projectPath/sql/005_stage4_document_ingestion.sql"
```

The migrations are ordered and designed to preserve existing compatible data. Migration `004` normalizes user identifiers and status types. Migration `005` adds document file metadata and independent parsing and chunking states.

### 2. Start Redis

Use an existing local Redis instance or create a development container:

```powershell
docker run --name enterprise-agent-redis -p 6379:6379 -d redis:7
docker exec enterprise-agent-redis redis-cli PING
```

The expected response is `PONG`. On later runs, start the existing container with:

```powershell
docker start enterprise-agent-redis
```

### 3. Configure the application

The application does not store database passwords or JWT secrets in tracked configuration files. Set the required values in the current shell:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-local-database-password"
$env:DB_URL = "jdbc:mysql://localhost:3306/enterprise_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
```

The development launcher validates the JWT configuration and generates a process-local 32-byte random secret when `JWT_SECRET_BASE64` is missing or invalid. The generated value is neither written to disk nor printed:

```powershell
.\scripts\run-dev.ps1
```

To configure every value manually:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-local-database-password"
$env:DB_URL = "jdbc:mysql://localhost:3306/enterprise_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

$jwtKeyBytes = New-Object byte[] 32
$jwtRandom = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $jwtRandom.GetBytes($jwtKeyBytes) } finally { $jwtRandom.Dispose() }
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($jwtKeyBytes)
$env:JWT_ISSUER = "https://enterprise-agent-platform.local"
$env:JWT_ACCESS_TOKEN_TTL = "30m"

$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:REDIS_DATABASE = "0"

$env:DOCUMENT_STORAGE_ROOT = "./storage/uploads"
$env:DOCUMENT_MAX_FILE_SIZE = "10MB"
$env:DOCUMENT_MAX_REQUEST_SIZE = "11MB"
$env:DOCUMENT_ALLOWED_TYPES = "TXT,MARKDOWN"

mvn spring-boot:run
```

See [`.env.example`](.env.example) for the complete configuration reference. Spring Boot does not automatically load that file.

## Configuration Reference

| Variable | Required | Default | Purpose |
|---|---:|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes for local database access | None | Activates the `dev` datasource profile |
| `DB_URL` | Recommended | Local development database | JDBC connection URL |
| `DB_USERNAME` | Yes in `dev` | `root` | Database username |
| `DB_PASSWORD` | Yes in `dev` | None | Database password |
| `JWT_SECRET_BASE64` | Yes | None | Base64-encoded JWT signing key of at least 32 bytes |
| `JWT_ISSUER` | No | `https://enterprise-agent-platform.local` | Expected JWT issuer |
| `JWT_ACCESS_TOKEN_TTL` | No | `30m` | Access-token lifetime |
| `REDIS_HOST` | No | `localhost` | Redis host |
| `REDIS_PORT` | No | `6379` | Redis port |
| `REDIS_PASSWORD` | No | Empty | Redis password |
| `REDIS_DATABASE` | No | `0` | Redis logical database |
| `REDIS_CONNECT_TIMEOUT` | No | `2s` | Redis connection timeout |
| `REDIS_TIMEOUT` | No | `2s` | Redis command timeout |
| `RATE_LIMIT_CHAT_MESSAGE_LIMIT` | No | `20` | Messages allowed per window |
| `RATE_LIMIT_CHAT_MESSAGE_WINDOW` | No | `1m` | Rate-limit window |
| `RATE_LIMIT_KEY_TTL_BUFFER` | No | `5s` | Extra TTL for rate-limit keys |
| `DOCUMENT_STORAGE_ROOT` | No | `./storage/uploads` | Root directory for uploaded documents |
| `DOCUMENT_MAX_FILE_SIZE` | No | `10MB` | Maximum individual file size |
| `DOCUMENT_MAX_REQUEST_SIZE` | No | `11MB` | Maximum multipart request size |
| `DOCUMENT_ALLOWED_TYPES` | No | `TXT,MARKDOWN` | Accepted document types |

## Security Model

The authentication entry point is `POST /api/auth/login`. A successful login returns a Bearer JWT containing only these claims:

- `sub`: authenticated user ID
- `jti`: unique token identifier
- `role`: database-backed application role
- `iss`, `iat`, and `exp`: issuer and lifetime metadata

Passwords, password hashes, email addresses, signing keys, and other sensitive values are never included in the token.

Business endpoints do not trust a client-supplied owner ID. Controllers derive the current user from the authenticated principal, and services verify ownership before reading or changing a knowledge base, document, chunk, chat session, or message.

Logout stores only the token `jti` in Redis and uses the token's remaining lifetime as the entry TTL. Revocation checks fail closed with HTTP `503` if Redis is unavailable. The ordinary message rate limiter favors availability and temporarily allows a request if its Redis dependency fails.

## Document Ingestion

The current document pipeline is:

```text
JWT subject
  -> knowledge base ownership check
  -> filename, size, extension, MIME, and UTF-8 validation
  -> generated {userId}/{knowledgeBaseId}/{UUID}.{ext} storage path
  -> document metadata persisted with PENDING status
  -> parsing state changed to PROCESSING
  -> TXT or Markdown parser selected by ParserRegistry
  -> normalized by TextCleaner
  -> content stored and parsing state changed to SUCCESS
```

The original filename is retained only as display metadata. Physical paths are generated by the server, normalized, and checked against the configured storage root. API responses do not expose absolute file paths.

If database insertion fails after a file is stored, the file is deleted as compensation. A parsing failure records a safe error summary, keeps the source file for retry, and changes the parsing state to `FAILED`.

## API Overview

Every endpoint returns a standard envelope:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

Paginated endpoint data uses this shape:

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 10,
  "totalPages": 0
}
```

| Area | Method and path | Access | Purpose |
|---|---|---|---|
| Health | `GET /api/health` | Public | Application health check |
| Users | `POST /api/users/register` | Public | Register a standard user |
| Authentication | `POST /api/auth/login` | Public | Validate credentials and issue a JWT |
| Authentication | `POST /api/auth/logout` | Authenticated | Revoke the active JWT |
| Users | `GET /api/users/me` | Authenticated | Return the current user |
| Users | `GET /api/users` | `ADMIN` | List users |
| Users | `GET /api/users/{id}` | `ADMIN` | Get a user by ID |
| Knowledge bases | `POST /api/knowledge-bases` | Authenticated | Create a knowledge base |
| Knowledge bases | `GET /api/knowledge-bases` | Authenticated | List owned knowledge bases |
| Knowledge bases | `GET /api/knowledge-bases/{id}` | Authenticated | Get an owned knowledge base |
| Documents | `POST /api/documents` | Authenticated | Create a manual document |
| Documents | `GET /api/documents` | Authenticated | Search and paginate owned documents |
| Documents | `GET /api/documents/{id}` | Authenticated | Get an owned document |
| Documents | `DELETE /api/documents/{id}` | Authenticated | Delete a document and cascade its chunks |
| Documents | `GET /api/knowledge-bases/{id}/documents` | Authenticated | List documents in an owned knowledge base |
| Ingestion | `POST /api/knowledge-bases/{knowledgeBaseId}/documents/upload` | Authenticated owner | Upload a TXT or Markdown document |
| Ingestion | `POST /api/documents/{documentId}/parse` | Authenticated owner | Parse or retry a document |
| Ingestion | `GET /api/documents/{documentId}/processing-status` | Authenticated owner | Get parsing and chunking status |
| Chunks | `POST /api/documents/{id}/chunks` | Authenticated owner | Create a manual text chunk |
| Chunks | `GET /api/documents/{id}/chunks` | Authenticated owner | List document chunks in order |
| Chat | `POST /api/chat/sessions` | Authenticated | Create a chat session |
| Chat | `GET /api/chat/sessions` | Authenticated | List owned chat sessions |
| Chat | `POST /api/chat/sessions/{id}/messages` | Authenticated owner | Store a rate-limited message |
| Chat | `GET /api/chat/sessions/{id}/messages` | Authenticated owner | List messages in stable order |

Send the token with protected requests:

```http
Authorization: Bearer <access-token>
```

Document search supports pagination and optional filters:

```http
GET /api/documents?page=1&size=10&keyword=operations&knowledgeBaseId=1
```

The page size must be between 1 and 100.

## OpenAPI and Swagger UI

With the `dev` profile active, API documentation is available at:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Stage 4 Day 1 group: <http://localhost:8080/v3/api-docs/stage-4-day-1>

The OpenAPI document defines a `bearerAuth` JWT security scheme. Public endpoints do not require a token; protected endpoints continue to enforce the actual application security rules when called through Swagger UI.

## Testing

Run the default regression suite without connecting to the development database:

```powershell
mvn clean test
```

Run the real MySQL integration tests against an isolated test database:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-test-database-password"
$env:DB_URL = "jdbc:mysql://localhost:3306/enterprise_agent_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
mvn clean test
```

Enable the real Redis integration tests explicitly:

```powershell
docker start enterprise-agent-redis
$env:REDIS_INTEGRATION_TEST = "true"
mvn clean test
```

The test suite covers:

- Service rules and transaction boundaries with Mockito
- Request validation, HTTP status codes, and response envelopes with MockMvc
- Authentication, authorization, JWT validation, logout, and Redis failure behavior
- MyBatis mappings, dynamic SQL, constraints, and cascading deletes against MySQL
- Secure document upload, parsing, retry, state transitions, and ownership isolation
- Redis blacklist TTL behavior and atomic Lua rate limiting
- OpenAPI paths, security schemes, and Swagger UI availability

## Project Structure

```text
.
|-- docs/                   Design notes, milestone reports, and handoff guides
|-- http/                   HTTP request examples
|-- scripts/                Development utilities
|-- sql/                    Ordered database schema and migration scripts
|-- src/main/java/          Application source code
|-- src/main/resources/     Configuration and MyBatis mappings
|-- src/test/java/          Unit, web, security, and integration tests
|-- storage/uploads/        Local development uploads; ignored by Git
|-- .env.example            Environment-variable reference
|-- pom.xml                 Maven build configuration
`-- README.md
```

## Operational and Privacy Guidelines

- Never commit `.env` files, database passwords, JWT secrets, access tokens, source documents, or production logs.
- Do not log passwords, password hashes, authorization headers, complete JWTs, complete document content, chunks, or chat messages.
- Use `DOCUMENT_STORAGE_ROOT` to place production uploads outside the repository or replace local storage with an object-storage implementation.
- Use a dedicated database and authenticated Redis deployment in production.
- Rotate JWT signing keys through deployment secrets rather than source-control changes.

## Roadmap

- Stage 4 Day 2: PDF parsing, fixed-length overlapping chunks, batch persistence, processing logs, retries, and physical-file cleanup
- Stage 5: embeddings and vector storage
- Stage 6: retrieval-augmented generation with citations
- Stage 7: streaming responses and conversation orchestration
- Stage 8: operations tool calling, audit logs, and resilience controls
- Stage 9: containerized deployment, observability, and final acceptance testing

Implementation evidence and detailed test procedures are available in [`docs/stage4-day1-implementation-report.md`](docs/stage4-day1-implementation-report.md). Cross-machine setup notes are available in [`docs/remote-work-handoff.md`](docs/remote-work-handoff.md).
