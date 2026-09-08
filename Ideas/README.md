Here are some solid Java & Spring Boot project ideas, grouped by complexity and what they showcase:

---

## 🟢 Beginner (Good for foundations)

1. **URL Shortener** — REST API that maps long URLs to short codes, with redirect tracking. Shows basic CRUD, JPA, and REST design.
2. **Expense Tracker API** — Manage income/expenses with category tagging and monthly summaries. Great for showcasing DTOs, validation, and JPA relationships.
3. **Library Management System** — Books, members, and borrowing records. Clean domain model with One-to-Many/Many-to-Many relationships.

---

## 🟡 Intermediate (Demonstrates real engineering)

4. **JWT Auth Boilerplate** — Full Spring Security setup with JWT login/refresh tokens, role-based access. Highly reusable and frequently starred.
5. **Multi-tenant SaaS Starter** — Schema-per-tenant or discriminator-based data isolation. Shows architectural thinking.
6. **Real-time Chat App** — Spring WebSocket + STOMP protocol. Add rooms, message history, and typing indicators.
7. **E-commerce Backend** — Products, cart, orders, payments (Stripe/Razorpay mock). Covers complex business logic, transactions, and event publishing.
8. **File Storage Service** — Upload/download files via REST, backed by S3 or MinIO. Covers streaming, multipart, and cloud integration.

---

## 🔴 Advanced (Makes you stand out)

9. **Microservices with Kafka** — Split a domain (e.g., orders + notifications + inventory) into services communicating via Kafka. Add API Gateway + service discovery (Eureka).
10. **Job Scheduling Platform** — Think a mini Quartz-based scheduler with a REST API to create/pause/trigger jobs, with execution history.
11. **Distributed Rate Limiter** — Redis-backed rate limiting library/service using token bucket or sliding window algorithm.
12. **Spring Batch ETL Pipeline** — Read large CSVs, transform, and write to DB with retry/skip logic. Great for data engineering credibility.
13. **Observability Demo App** — App instrumented with Micrometer + Prometheus + Grafana + distributed tracing (OpenTelemetry). Shows production-readiness awareness.

---

## 💡 Tips to Make Any Project Pop on GitHub

- Write a **clean README** with architecture diagrams, setup steps, and API docs (Swagger)
- Add a **`docker-compose.yml`** so anyone can run it instantly
- Include **integration tests** with Testcontainers
- Add a **GitHub Actions CI pipeline** (build + test on push)
- Use **conventional commits** and a proper `.gitignore`

---

Want me to scaffold any of these — like generating the project structure, `pom.xml`, folder layout, or a README template?


# Java & Spring Boot — 13 Expert Project Prompts
> For use with Antigravity. **Zero code comments. Production-grade. No placeholders.**

---

## GLOBAL RULES (Apply to ALL 13 Projects)

```
- Java 21 (LZTs, virtual threads, sealed classes where applicable)
- Spring Boot 3.3.x
- Maven with parent POM
- Zero comments anywhere in any source file
- No TODO, FIXME, placeholder, or stub methods
- All code must compile and run without modification
- Use records for DTOs and immutable value objects
- Use @Slf4j (Lombok) for logging — never System.out
- All endpoints documented via springdoc-openapi (Swagger UI at /swagger-ui.html)
- application.yml (never application.properties)
- Profiles: dev, test, prod
- .env.example at root listing all required environment variables
- docker-compose.yml at root that boots the full stack
- Flyway for all DB migrations (never hibernate ddl-auto=create)
- Global exception handler via @RestControllerAdvice
- Standard error envelope: { timestamp, status, error, message, path }
- All IDs are UUIDs
- All timestamps are Instant (UTC)
- Pagination via Pageable for all list endpoints
- Bean Validation (jakarta.validation) on all request bodies
- 80%+ test coverage: unit tests (Mockito) + integration tests (Testcontainers)
- @SpringBootTest integration tests use a real Postgres container via Testcontainers
- GitHub Actions CI: .github/workflows/ci.yml (build → test → docker build)
- Makefile at root: make build, make test, make run, make docker-up
```

---

## PROJECT 1 — URL Shortener

```
Build a production-ready URL Shortener REST API in Java 21 + Spring Boot 3.3 with the following complete specifications. Write zero comments in any source file.

### Tech Stack
- Spring Web, Spring Data JPA, Spring Cache, Spring Security (API key auth)
- PostgreSQL (via Testcontainers for tests)
- Redis (for caching and rate limiting)
- Flyway migrations
- Caffeine as local cache fallback
- springdoc-openapi

### Domain Model
Entity: ShortUrl
Fields:
  - id: UUID (PK)
  - originalUrl: String (NOT NULL, max 2048)
  - shortCode: String (NOT NULL, UNIQUE, 6–10 chars)
  - customAlias: String (NULLABLE, UNIQUE, 3–30 chars, alphanumeric + hyphens)
  - createdAt: Instant
  - expiresAt: Instant (NULLABLE)
  - clickCount: long (default 0, updated atomically)
  - active: boolean (default true)
  - ownerId: UUID (FK to api_keys.owner_id, NULLABLE for public links)

Entity: ClickEvent
Fields:
  - id: UUID (PK)
  - shortUrlId: UUID (FK)
  - clickedAt: Instant
  - ipAddress: String
  - userAgent: String
  - referer: String (NULLABLE)
  - country: String (NULLABLE, derived from IP via MaxMind GeoLite2)
  - deviceType: String (MOBILE | DESKTOP | TABLET | UNKNOWN)

Entity: ApiKey
Fields:
  - id: UUID (PK)
  - keyHash: String (bcrypt hash, NOT NULL)
  - ownerId: UUID (NOT NULL)
  - label: String
  - createdAt: Instant
  - lastUsedAt: Instant (NULLABLE)
  - active: boolean

### Flyway Migrations (src/main/resources/db/migration)
V1__create_api_keys.sql
V2__create_short_urls.sql
V3__create_click_events.sql
V4__add_indexes.sql (index on shortCode, customAlias, expiresAt, ownerId)

### Short Code Generation
- Default: base62 encoding of a random 6-byte value → always 8 chars
- Custom alias: validated against a reserved-words blocklist stored in a Set<String> loaded from reserved-words.txt in resources
- Collision retry: up to 5 attempts before throwing ShortCodeCollisionException

### API Endpoints

Public (no auth):
  GET  /{code}         → 301 redirect to originalUrl (or 302 if temporary flag set)
                         If expired → 410 GONE
                         If inactive → 404 NOT FOUND
                         Async click event recording via @Async thread pool

Authenticated (X-Api-Key header):
  POST   /api/v1/urls
    Body: { originalUrl, customAlias?, expiresAt?, temporary? }
    Response: { id, shortCode, shortUrl, originalUrl, expiresAt, createdAt }

  GET    /api/v1/urls               → paginated list of caller's short URLs
  GET    /api/v1/urls/{id}          → single short URL detail
  DELETE /api/v1/urls/{id}          → soft-delete (active=false)
  PATCH  /api/v1/urls/{id}/activate → re-activate

  GET    /api/v1/urls/{id}/stats
    Response: {
      totalClicks,
      uniqueIps,
      clicksByDay: [{ date, count }],  // last 30 days
      clicksByCountry: [{ country, count }],
      clicksByDevice: [{ deviceType, count }],
      topReferers: [{ referer, count }]
    }

Admin (X-Api-Key of admin role):
  GET    /api/v1/admin/urls         → all URLs across all owners (paginated)
  DELETE /api/v1/admin/urls/{id}    → hard delete

### Rate Limiting
- Redis-backed sliding window: 100 requests/minute per API key
- Unauthenticated redirect endpoint: 30 req/min per IP
- Return 429 Too Many Requests with Retry-After header on breach

### Caching Strategy
- Cache shortCode → ShortUrl in Redis with TTL = min(1 hour, timeToExpiry)
- On delete/deactivate, evict from Redis immediately
- Fallback to Caffeine if Redis is unavailable (circuit-breaker via Resilience4j)

### Validation Rules
- originalUrl: must be a valid HTTP or HTTPS URL (custom validator @ValidUrl)
- customAlias: ^[a-zA-Z0-9-]{3,30}$ (custom validator @ValidAlias)
- expiresAt: must be in the future if provided
- On POST, if customAlias already exists → 409 CONFLICT

### Configuration (application.yml)
app:
  base-url: ${APP_BASE_URL:http://localhost:8080}
  short-code-length: 8
  max-custom-alias-length: 30
  rate-limit:
    authenticated-rpm: 100
    unauthenticated-rpm: 30
  click-event:
    async-pool-size: 4
  cache:
    redis-ttl-seconds: 3600
    caffeine-max-size: 10000

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}

### Exception Hierarchy
UrlShortenerException (base, abstract)
  ├── ShortUrlNotFoundException (404)
  ├── ShortCodeCollisionException (500)
  ├── UrlExpiredException (410)
  ├── UrlInactiveException (404)
  ├── CustomAliasConflictException (409)
  ├── InvalidUrlException (400)
  └── RateLimitExceededException (429)

### Service Layer (interfaces + implementations)
UrlShortenerService: shorten, resolve, deactivate, activate, getStats
ClickEventService: record (async), aggregateStats
RateLimiterService: isAllowed(key, window, limit)
ApiKeyService: validateKey, createKey, rotateKey

### Repository Layer
ShortUrlRepository extends JpaRepository
  - Optional<ShortUrl> findByShortCode(String code)
  - Optional<ShortUrl> findByCustomAlias(String alias)
  - Page<ShortUrl> findByOwnerIdAndActiveTrue(UUID ownerId, Pageable p)

ClickEventRepository extends JpaRepository
  - List<ClickEvent> findByShortUrlIdAndClickedAtAfter(UUID id, Instant since)
  - @Query aggregate queries for stats

### Tests
- Unit: UrlShortenerServiceTest, ClickEventServiceTest, RateLimiterServiceTest (Mockito)
- Integration: UrlShortenerControllerIT, RedirectControllerIT (Testcontainers Postgres + Redis)
- Custom validator tests: ValidUrlTest, ValidAliasTest

### docker-compose.yml services
- app (Spring Boot jar, depends on postgres + redis)
- postgres:16-alpine (port 5432, volume for data)
- redis:7-alpine (port 6379, AOF persistence enabled)

### Project Structure
src/main/java/com/urlshortener/
  api/
    controller/   (RedirectController, UrlController, AdminController)
    dto/          (request/response records)
    validation/   (validators + annotations)
  domain/
    entity/       (ShortUrl, ClickEvent, ApiKey)
    repository/
    service/      (interfaces)
    service/impl/
    exception/
  infrastructure/
    cache/        (Redis config, Caffeine config)
    ratelimit/
    security/     (ApiKeyAuthFilter, SecurityConfig)
    async/        (AsyncConfig with executor config)
    config/       (AppProperties @ConfigurationProperties)
  Application.java
```

---

## PROJECT 2 — Expense Tracker API

```
Build a production-ready personal Expense Tracker REST API in Java 21 + Spring Boot 3.3. Zero comments in all source files.

### Tech Stack
- Spring Web, Spring Data JPA, Spring Security (JWT), Spring Validation
- PostgreSQL + Flyway
- springdoc-openapi
- Testcontainers (integration tests)
- MapStruct (DTO ↔ Entity mapping — no manual mapping code)
- Lombok

### Domain Model

Entity: User
  - id: UUID
  - email: String (UNIQUE, NOT NULL)
  - passwordHash: String (NOT NULL)
  - fullName: String (NOT NULL)
  - currency: String (3-char ISO 4217, default "USD")
  - createdAt: Instant
  - active: boolean

Entity: Category
  - id: UUID
  - name: String (NOT NULL)
  - color: String (hex color, e.g. "#FF5733")
  - icon: String (icon slug, e.g. "food", "transport")
  - type: CategoryType enum (INCOME | EXPENSE)
  - userId: UUID (NULL = system default, non-null = user-defined)
  - createdAt: Instant

Entity: Transaction
  - id: UUID
  - userId: UUID (FK)
  - categoryId: UUID (FK)
  - amount: BigDecimal (NOT NULL, precision=19 scale=4, must be > 0)
  - type: TransactionType enum (INCOME | EXPENSE | TRANSFER)
  - description: String (max 500)
  - note: String (max 2000, NULLABLE)
  - transactionDate: LocalDate (NOT NULL)
  - createdAt: Instant
  - updatedAt: Instant
  - tags: List<String> (stored as text[], Postgres array)
  - attachmentUrl: String (NULLABLE, URL to receipt image in S3)
  - recurring: boolean
  - recurringConfig: JSONB (NULLABLE, stores { frequency, endDate, dayOfMonth })

Entity: Budget
  - id: UUID
  - userId: UUID (FK)
  - categoryId: UUID (FK, NULLABLE = overall budget)
  - amount: BigDecimal (monthly budget cap)
  - month: YearMonth (stored as date — first day of month)
  - alertThreshold: int (percentage, e.g. 80 = alert at 80% spent)
  - createdAt: Instant

Entity: RecurringTransaction
  - id: UUID
  - templateTransactionId: UUID (FK to Transaction)
  - frequency: RecurrenceFrequency enum (DAILY | WEEKLY | MONTHLY | YEARLY)
  - dayOfMonth: Integer (NULLABLE, for MONTHLY)
  - dayOfWeek: String (NULLABLE, for WEEKLY)
  - nextRunDate: LocalDate
  - endDate: LocalDate (NULLABLE)
  - active: boolean

### Flyway Migrations
V1__users.sql
V2__categories.sql
V3__default_categories.sql  (INSERT system-level categories: Food, Transport, Housing, Entertainment, Healthcare, Education, Shopping, Salary, Investment, Other)
V4__transactions.sql
V5__budgets.sql
V6__recurring_transactions.sql
V7__indexes.sql

### Auth
- JWT access token (15 min TTL) + refresh token (30 days, stored in DB as hashed value)
- POST /api/v1/auth/register   → { fullName, email, password }
- POST /api/v1/auth/login      → { email, password } → { accessToken, refreshToken, expiresIn }
- POST /api/v1/auth/refresh    → { refreshToken } → { accessToken, refreshToken }
- POST /api/v1/auth/logout     → invalidates refresh token in DB

### API Endpoints (all under /api/v1, all require JWT)

Categories:
  GET    /categories         → system + user's custom categories
  POST   /categories         → create custom category (name, color, icon, type)
  PATCH  /categories/{id}    → update own category
  DELETE /categories/{id}    → delete own category (must have no transactions)

Transactions:
  GET    /transactions       → paginated, filterable by type, categoryId, dateFrom, dateTo, tags, minAmount, maxAmount
                               Sort by: transactionDate (default DESC), amount, createdAt
  POST   /transactions       → create
  GET    /transactions/{id}  → detail
  PUT    /transactions/{id}  → full update
  DELETE /transactions/{id}  → delete
  POST   /transactions/bulk  → create up to 100 at once (returns list of results with per-item success/failure)
  POST   /transactions/{id}/attach → multipart upload receipt → stores to S3 (or local FileSystem in dev profile)

Budgets:
  GET    /budgets            → current month's budgets with spent amount + remaining + progress %
  GET    /budgets?month=YYYY-MM → budgets for specific month
  POST   /budgets            → create or update for a category+month combo
  DELETE /budgets/{id}

Reports:
  GET /reports/summary?from=YYYY-MM-DD&to=YYYY-MM-DD
    Response: {
      totalIncome, totalExpense, netSavings,
      savingsRate (percentage),
      byCategory: [{ category, total, percentage, transactionCount }],
      byMonth: [{ month, income, expense, net }],
      topExpenseCategories: [{ category, total }] (top 5),
      averageDailyExpense
    }

  GET /reports/trends?months=12
    Response: monthly income vs expense for last N months

  GET /reports/cashflow?year=YYYY
    Response: monthly net cashflow for the year

Recurring:
  GET    /recurring                 → list active recurring rules
  POST   /recurring/{transactionId} → make a transaction recurring (body: frequency, dayOfMonth, endDate)
  DELETE /recurring/{id}            → deactivate (soft)

### Scheduled Jobs
- RecurringTransactionJob: runs daily at 06:00 UTC via @Scheduled
  - Finds all active RecurringTransactions where nextRunDate <= today
  - Creates new Transaction from template
  - Updates nextRunDate based on frequency
  - Sends budget alert event if budget threshold is breached

- BudgetAlertJob: runs daily at 08:00 UTC
  - For each user+budget, computes spent vs limit
  - If spent >= alertThreshold%, publishes BudgetAlertEvent
  - BudgetAlertEventListener sends an email (via JavaMailSender, configurable SMTP)

### Validation
- amount: must be > 0, max precision 4 decimal places
- transactionDate: not in the future for EXPENSE, any date for INCOME
- email: @Email + uniqueness check
- password: min 8 chars, at least 1 uppercase, 1 digit, 1 special char (custom @StrongPassword validator)
- tags: max 10 tags, each max 30 chars

### Exception Hierarchy
AppException (base)
  ├── UserNotFoundException (404)
  ├── CategoryNotFoundException (404)
  ├── TransactionNotFoundException (404)
  ├── CategoryInUseException (409)
  ├── DuplicateBudgetException (409)
  ├── UnauthorizedResourceException (403)
  ├── InvalidDateRangeException (400)
  └── BulkOperationException (207 — contains per-item errors)

### Configuration
app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-ttl: PT15M
    refresh-token-ttl: P30D
  storage:
    type: ${STORAGE_TYPE:local}   # local | s3
    local-path: ${LOCAL_UPLOAD_PATH:./uploads}
    s3-bucket: ${S3_BUCKET:}
    s3-region: ${S3_REGION:us-east-1}
  mail:
    from: ${MAIL_FROM:noreply@expensetracker.com}

### Tests
- Unit: TransactionServiceTest, BudgetServiceTest, ReportServiceTest, RecurringJobTest
- Integration: TransactionControllerIT, AuthControllerIT, BudgetControllerIT (Testcontainers)
- Mapper tests: TransactionMapperTest

### Project Structure
src/main/java/com/expensetracker/
  api/controller/
  api/dto/request/
  api/dto/response/
  domain/entity/
  domain/repository/
  domain/service/
  domain/service/impl/
  domain/event/
  domain/exception/
  domain/enums/
  infrastructure/
    security/   (JwtFilter, JwtService, SecurityConfig)
    storage/    (StorageService interface + LocalStorage + S3Storage impls)
    mail/
    scheduler/
    mapper/     (MapStruct mappers)
    config/
  Application.java
```

---

## PROJECT 3 — Library Management System

```
Build a production-ready Library Management System REST API in Java 21 + Spring Boot 3.3. Zero comments in any source file.

### Tech Stack
- Spring Web, Spring Data JPA, Spring Security (JWT + role-based)
- PostgreSQL + Flyway
- springdoc-openapi, Lombok, MapStruct
- Testcontainers, Resilience4j (for external ISBN lookup)

### Roles
ADMIN — full access
LIBRARIAN — manage books, members, transactions
MEMBER — search books, view own borrowing history, place holds

### Domain Model

Entity: Book
  - id: UUID
  - isbn: String (UNIQUE, 10 or 13 digit, validated)
  - title: String (NOT NULL, max 500)
  - authors: List<Author> (ManyToMany)
  - publisher: String
  - publishedYear: int
  - genre: Set<Genre> (ManyToMany)
  - description: String (max 5000)
  - coverImageUrl: String (NULLABLE)
  - language: String (ISO 639-1)
  - pageCount: int
  - totalCopies: int (NOT NULL, >= 1)
  - availableCopies: int (NOT NULL, >= 0, derived but persisted for query perf)
  - location: String (shelf location, e.g. "A-3-12")
  - active: boolean

Entity: BookCopy
  - id: UUID
  - bookId: UUID (FK)
  - copyNumber: int (sequential per book)
  - condition: CopyCondition enum (NEW | GOOD | FAIR | POOR | DAMAGED)
  - available: boolean
  - notes: String (NULLABLE)
  - acquiredAt: LocalDate

Entity: Author
  - id: UUID
  - name: String (NOT NULL)
  - biography: String (NULLABLE, max 5000)
  - birthDate: LocalDate (NULLABLE)
  - nationality: String (NULLABLE)

Entity: Genre
  - id: UUID
  - name: String (UNIQUE, NOT NULL)
  - description: String (NULLABLE)

Entity: Member
  - id: UUID
  - userId: UUID (FK to User)
  - membershipNumber: String (UNIQUE, auto-generated: LIB-YYYYMM-XXXXX)
  - firstName: String
  - lastName: String
  - email: String (UNIQUE)
  - phone: String
  - address: String
  - membershipType: MembershipType enum (BASIC | PREMIUM | STUDENT | SENIOR)
  - membershipExpiry: LocalDate
  - status: MemberStatus enum (ACTIVE | SUSPENDED | EXPIRED)
  - maxBorrowLimit: int (derived from membershipType: BASIC=3, PREMIUM=10, STUDENT=5, SENIOR=5)
  - createdAt: Instant

Entity: BorrowingRecord
  - id: UUID
  - memberId: UUID (FK)
  - copyId: UUID (FK to BookCopy)
  - borrowedAt: Instant
  - dueDate: LocalDate (borrowedAt + loan period based on membershipType)
  - returnedAt: Instant (NULLABLE)
  - renewalCount: int (default 0, max 2)
  - status: BorrowStatus enum (ACTIVE | RETURNED | OVERDUE | LOST)
  - fine: BigDecimal (NULLABLE, computed on return if overdue)
  - fineStatus: FineStatus enum (PENDING | PAID | WAIVED, NULLABLE)
  - issuedBy: UUID (librarian user id)

Entity: Hold
  - id: UUID
  - memberId: UUID (FK)
  - bookId: UUID (FK to Book, not copy)
  - requestedAt: Instant
  - expiresAt: Instant (hold valid for 7 days from when notified)
  - status: HoldStatus enum (WAITING | READY | FULFILLED | CANCELLED | EXPIRED)
  - queuePosition: int
  - notifiedAt: Instant (NULLABLE, when librarian marks a copy ready)

Entity: Fine
  - id: UUID
  - borrowingRecordId: UUID (FK)
  - memberId: UUID (FK)
  - amount: BigDecimal
  - reason: FineReason enum (OVERDUE | DAMAGED | LOST)
  - issuedAt: Instant
  - paidAt: Instant (NULLABLE)
  - status: FineStatus enum (PENDING | PAID | WAIVED)
  - waivedBy: UUID (NULLABLE, librarian who waived)

### Loan Duration Rules
BASIC:    14 days, fine ₹2/day overdue
PREMIUM:  28 days, fine ₹1/day overdue
STUDENT:  21 days, fine ₹1/day overdue
SENIOR:   21 days, fine ₹0.50/day overdue

### Flyway Migrations
V1__users.sql
V2__authors_genres.sql
V3__books.sql
V4__book_copies.sql
V5__members.sql
V6__borrowing_records.sql
V7__holds.sql
V8__fines.sql
V9__indexes.sql
V10__seed_genres.sql  (Fiction, Non-Fiction, Science, History, Biography, Technology, Self-Help, Children, Mystery, Fantasy, Romance)

### API Endpoints

Auth: POST /api/v1/auth/register, /login, /refresh, /logout (same JWT pattern as Project 2)

Books (ADMIN/LIBRARIAN manage, MEMBER read-only):
  GET    /api/v1/books                   → paginated, filter: genre, author, available, language, year, query (full-text search on title+author)
  GET    /api/v1/books/{id}              → full book detail with copies list (LIBRARIAN sees all copies; MEMBER sees count only)
  GET    /api/v1/books/isbn/{isbn}       → lookup by ISBN (fetches from OpenLibrary API if not in DB, caches result 24h)
  POST   /api/v1/books                   → ADMIN/LIBRARIAN create book + initial copies
  PUT    /api/v1/books/{id}              → ADMIN/LIBRARIAN update
  DELETE /api/v1/books/{id}             → ADMIN only, soft delete (cannot delete if copies are borrowed)
  POST   /api/v1/books/{id}/copies      → ADMIN/LIBRARIAN add more copies
  PATCH  /api/v1/books/{id}/copies/{copyId}/condition → update copy condition

Authors:
  GET  /api/v1/authors         → paginated search
  POST /api/v1/authors         → ADMIN/LIBRARIAN
  PUT  /api/v1/authors/{id}    → ADMIN/LIBRARIAN

Members:
  GET    /api/v1/members           → ADMIN/LIBRARIAN, paginated
  POST   /api/v1/members           → ADMIN/LIBRARIAN register new member
  GET    /api/v1/members/{id}      → ADMIN/LIBRARIAN or own profile
  PATCH  /api/v1/members/{id}      → update info
  POST   /api/v1/members/{id}/suspend   → ADMIN/LIBRARIAN
  POST   /api/v1/members/{id}/activate  → ADMIN/LIBRARIAN
  GET    /api/v1/members/{id}/borrowings → borrowing history (own = MEMBER, any = LIBRARIAN)
  GET    /api/v1/members/{id}/fines     → fine summary

Borrowing:
  POST   /api/v1/borrowings              → LIBRARIAN issues a book { memberId, copyId }
                                           Validates: member active, no unpaid fines, under borrow limit, copy available
  POST   /api/v1/borrowings/{id}/return → LIBRARIAN returns a copy, computes fine if overdue
  POST   /api/v1/borrowings/{id}/renew  → MEMBER or LIBRARIAN, max 2 renewals, extends dueDate
  POST   /api/v1/borrowings/{id}/lost   → LIBRARIAN marks copy as lost, raises fine equal to book replacement cost

Holds:
  POST   /api/v1/holds             → MEMBER places hold on a book
  DELETE /api/v1/holds/{id}       → MEMBER cancels own hold
  GET    /api/v1/holds/queue/{bookId} → LIBRARIAN sees hold queue with positions
  POST   /api/v1/holds/{id}/ready → LIBRARIAN marks hold ready (notifiedAt set, expiresAt = now + 7 days)
  POST   /api/v1/holds/{id}/fulfil → LIBRARIAN issues copy to member fulfilling hold

Fines:
  GET    /api/v1/fines              → ADMIN/LIBRARIAN all fines, filter by status
  POST   /api/v1/fines/{id}/pay     → LIBRARIAN marks as paid
  POST   /api/v1/fines/{id}/waive   → ADMIN/LIBRARIAN waive with reason

Reports (ADMIN/LIBRARIAN):
  GET /api/v1/reports/inventory     → total books, copies, available, borrowed, overdue
  GET /api/v1/reports/popular-books → top 20 most borrowed books (last 30/90/365 days)
  GET /api/v1/reports/overdue       → all active borrowings past due date
  GET /api/v1/reports/fines-summary → total outstanding fines, paid this month, waived this month

### Scheduled Jobs
- OverdueCheckJob: daily at midnight — marks ACTIVE borrows past due as OVERDUE, sends notification event
- HoldExpiryJob: daily at 01:00 — marks READY holds past expiresAt as EXPIRED, advances queue
- MembershipExpiryJob: daily at 02:00 — marks members with expired membership as EXPIRED status
- FineCalculationJob: daily at midnight — recalculates fine amounts for OVERDUE records (running fine accrual)

### External Integration
- OpenLibrary API (https://openlibrary.org/isbn/{isbn}.json) used on /books/isbn/{isbn}
- Wrapped in @Cacheable (24h) to avoid repeated calls
- Resilience4j CircuitBreaker: if OpenLibrary is down, return partial data (ISBN + 404-equivalent upstream message)

### Exception Hierarchy
LibraryException (base)
  ├── BookNotFoundException (404)
  ├── CopyNotFoundException (404)
  ├── MemberNotFoundException (404)
  ├── CopyNotAvailableException (409)
  ├── BorrowLimitExceededException (409)
  ├── UnpaidFinesException (409)
  ├── MemberNotActiveException (403)
  ├── RenewalLimitExceededException (409)
  ├── HoldNotFoundException (404)
  └── DuplicateHoldException (409)

### Tests
- Unit: BorrowingServiceTest, FineCalculationServiceTest, HoldQueueServiceTest
- Integration: BorrowingControllerIT, HoldControllerIT, IsbnLookupServiceIT (WireMock for OpenLibrary)

### docker-compose.yml services
- app
- postgres:16-alpine
- wiremock (for local OpenLibrary stub in dev)
```

---

## PROJECT 4 — JWT Auth Boilerplate

```
Build a reusable, production-grade JWT Authentication & Authorization boilerplate in Java 21 + Spring Boot 3.3. This must be the definitive reference implementation. Zero comments in any source file.

### Tech Stack
- Spring Security 6, Spring Data JPA, Spring Web
- PostgreSQL + Flyway
- JJWT 0.12.x (io.jsonwebtoken)
- springdoc-openapi
- Testcontainers
- Lombok, MapStruct
- Spring Mail (for email verification + password reset)
- Google Authenticator TOTP (dev.samstevens.totp) for 2FA

### Feature Checklist (all must be implemented)
[x] Registration with email verification (6-digit OTP, 15-min expiry)
[x] Login → access token (15 min) + refresh token (30 days, HTTP-only cookie option)
[x] Refresh token rotation (each refresh issues new pair, old refresh token invalidated)
[x] Logout (invalidates current refresh token)
[x] Logout all devices (invalidates ALL refresh tokens for user)
[x] Password reset via email (HMAC-signed one-time link, 30-min expiry)
[x] Change password (requires current password)
[x] Role-based access (ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN)
[x] Permission-based access (granular, e.g. USER_READ, USER_WRITE, REPORT_VIEW)
[x] 2FA via TOTP (enable, verify setup, use on login, disable with password)
[x] Account lockout after 5 failed login attempts (15-min lockout)
[x] Audit log of all auth events
[x] Device/session tracking (user can see and revoke individual sessions)

### Domain Model

Entity: User
  - id: UUID
  - email: String (UNIQUE)
  - passwordHash: String
  - firstName: String
  - lastName: String
  - status: UserStatus enum (PENDING_VERIFICATION | ACTIVE | LOCKED | DISABLED)
  - failedLoginAttempts: int (default 0)
  - lockedUntil: Instant (NULLABLE)
  - emailVerified: boolean
  - twoFactorEnabled: boolean
  - twoFactorSecret: String (NULLABLE, encrypted at rest using AES-256)
  - createdAt: Instant
  - updatedAt: Instant
  - lastLoginAt: Instant (NULLABLE)
  - lastLoginIp: String (NULLABLE)

Entity: Role
  - id: UUID
  - name: String (UNIQUE, e.g. ROLE_ADMIN)
  - description: String

Entity: Permission
  - id: UUID
  - name: String (UNIQUE, e.g. USER_READ)
  - resource: String
  - action: String

Role ↔ Permission: ManyToMany
User ↔ Role: ManyToMany

Entity: RefreshToken
  - id: UUID
  - tokenHash: String (SHA-256 hash of the actual token)
  - userId: UUID (FK)
  - deviceInfo: String (User-Agent)
  - ipAddress: String
  - issuedAt: Instant
  - expiresAt: Instant
  - lastUsedAt: Instant
  - revoked: boolean
  - revokedAt: Instant (NULLABLE)
  - family: UUID (token family for rotation attack detection)

Entity: EmailVerification
  - id: UUID
  - userId: UUID (FK)
  - otpHash: String (bcrypt hash of 6-digit code)
  - purpose: VerificationPurpose enum (REGISTRATION | PASSWORD_RESET | EMAIL_CHANGE)
  - expiresAt: Instant
  - used: boolean

Entity: AuthAuditLog
  - id: UUID
  - userId: UUID (NULLABLE, for failed login attempts on non-existent email)
  - email: String
  - event: AuthEvent enum (LOGIN_SUCCESS | LOGIN_FAILED | LOGOUT | TOKEN_REFRESHED | PASSWORD_CHANGED | PASSWORD_RESET | ACCOUNT_LOCKED | 2FA_ENABLED | 2FA_DISABLED | SESSION_REVOKED | ALL_SESSIONS_REVOKED)
  - ipAddress: String
  - userAgent: String
  - timestamp: Instant
  - metadata: JSONB (NULLABLE, extra context)

### Flyway Migrations
V1__users.sql
V2__roles_permissions.sql
V3__user_roles_permissions.sql
V4__refresh_tokens.sql
V5__email_verifications.sql
V6__auth_audit_log.sql
V7__seed_roles_permissions.sql (insert ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN + all permissions)
V8__indexes.sql

### API Endpoints

/api/v1/auth/register
  POST body: { firstName, lastName, email, password }
  → Creates user (status=PENDING_VERIFICATION), sends OTP email
  → 201 with { message: "Verification email sent" }

/api/v1/auth/verify-email
  POST body: { email, otp }
  → Verifies OTP, sets emailVerified=true, status=ACTIVE
  → Returns { accessToken, refreshToken, user }

/api/v1/auth/resend-verification
  POST body: { email }
  → Invalidates old OTP, sends new one (rate-limited: max 3 per hour per email)

/api/v1/auth/login
  POST body: { email, password, totpCode? }
  → On success without 2FA: returns { accessToken, refreshToken, tokenType:"Bearer", expiresIn:900, user }
  → On success with 2FA enabled but no totpCode: returns 200 with { requiresTwoFactor: true, tempToken }
  → tempToken valid for 5 min, used to complete 2FA step

/api/v1/auth/login/two-factor
  POST body: { tempToken, totpCode }
  → Validates TOTP, issues final token pair

/api/v1/auth/refresh
  POST body: { refreshToken }
  → Issues new access+refresh pair, invalidates old refresh token
  → If refresh token is already used (rotation attack): revoke entire token family, return 401

/api/v1/auth/logout
  POST (authenticated)
  → Revokes current refresh token (extracted from request body or cookie)

/api/v1/auth/logout-all
  POST (authenticated)
  → Revokes all refresh tokens for the current user

/api/v1/auth/forgot-password
  POST body: { email }
  → Always returns 200 (security: don't reveal if email exists)
  → If user exists, send reset link: {frontendUrl}/reset-password?token={signedToken}

/api/v1/auth/reset-password
  POST body: { token, newPassword }
  → Validates signed token, resets password, invalidates all refresh tokens

/api/v1/auth/change-password
  POST (authenticated) body: { currentPassword, newPassword }
  → Validates current password, updates hash, invalidates all OTHER refresh tokens

/api/v1/users/me
  GET (authenticated) → full profile
  PUT (authenticated) body: { firstName, lastName } → update profile

/api/v1/users/me/sessions
  GET (authenticated) → list active refresh tokens (id, deviceInfo, ip, lastUsedAt, issuedAt)
  DELETE /{sessionId} → revoke specific session

/api/v1/users/me/two-factor
  POST /enable     → generates TOTP secret, returns { secret, qrCodeUri } (base32 secret + otpauth URI for QR)
  POST /verify     → body: { totpCode } — confirms setup, enables 2FA
  POST /disable    → body: { password, totpCode } — disables 2FA

Admin (/api/v1/admin/** requires ROLE_ADMIN):
  GET    /users           → paginated user list
  GET    /users/{id}      → user detail with roles
  POST   /users/{id}/roles → assign roles body: { roles: ["ROLE_MODERATOR"] }
  DELETE /users/{id}/roles → remove roles
  POST   /users/{id}/lock → lock account
  POST   /users/{id}/unlock → unlock account
  GET    /audit-log       → paginated auth events, filter by userId, event, dateRange

### JWT Implementation Details
- Algorithm: RS256 (RSA-256, NOT HS256)
- Public/private key pair loaded from PEM files (configurable path in application.yml)
- Access token claims: sub (userId), email, roles, permissions, jti (unique token ID), iss, iat, exp
- Refresh token: opaque random 256-bit value (UUID-based), stored hashed in DB
- Token validation: signature, expiry, jti not in revoked set (check Redis for revoked jtis with TTL = remaining token lifetime)

### Security Config
- SecurityFilterChain with stateless session (STATELESS)
- JwtAuthenticationFilter extends OncePerRequestFilter
- CORS: configurable allowed origins
- CSRF: disabled (stateless API)
- Rate limiting on auth endpoints:
  /auth/login: 10 req/min per IP
  /auth/forgot-password: 3 req/10min per email
  /auth/resend-verification: 3 req/hour per email

### Encryption
- User's TOTP secret encrypted with AES-256-GCM at rest
- Encryption key from ${TOTP_ENCRYPTION_KEY} env var (base64-encoded 32-byte key)
- EncryptionService interface with AesGcmEncryptionService implementation

### Application Properties
app:
  jwt:
    private-key-path: ${JWT_PRIVATE_KEY_PATH:classpath:keys/private.pem}
    public-key-path: ${JWT_PUBLIC_KEY_PATH:classpath:keys/public.pem}
    access-token-ttl: PT15M
    refresh-token-ttl: P30D
    temp-token-ttl: PT5M
  auth:
    max-failed-attempts: 5
    lockout-duration: PT15M
    otp-expiry: PT15M
    password-reset-link-expiry: PT30M
    password-reset-secret: ${PASSWORD_RESET_SECRET}
  frontend-url: ${FRONTEND_URL:http://localhost:3000}
  encryption:
    totp-key: ${TOTP_ENCRYPTION_KEY}

### Tests
- Unit: JwtServiceTest, AuthServiceTest, TwoFactorServiceTest, EncryptionServiceTest
- Integration: AuthControllerIT (all auth flows end-to-end with Testcontainers)
  - Full registration → verification → login → refresh → logout flow
  - 2FA enable → login with TOTP flow
  - Token rotation attack detection test
  - Account lockout test

### Key tool dependencies (pom.xml)
- io.jsonwebtoken:jjwt-api:0.12.6
- io.jsonwebtoken:jjwt-impl:0.12.6
- io.jsonwebtoken:jjwt-jackson:0.12.6
- dev.samstevens.totp:totp-spring-boot-starter:1.7.1
- com.google.zxing:core:3.5.3 (QR code URI generation for TOTP setup)
```

---

## PROJECT 5 — Multi-Tenant SaaS Starter

```
Build a production-ready multi-tenant SaaS backend boilerplate in Java 21 + Spring Boot 3.3 using schema-per-tenant isolation strategy. Zero comments in source files.

### Tech Stack
- Spring Web, Spring Data JPA, Spring Security (JWT)
- PostgreSQL (multi-schema)
- Flyway (tenant-aware migrations)
- Redis (tenant-scoped caching, session)
- springdoc-openapi, Testcontainers, Lombok, MapStruct

### Multi-Tenancy Strategy
- Schema-per-tenant: each tenant gets its own Postgres schema (e.g., tenant_acme, tenant_globex)
- Public schema: contains tenant registry + platform admin data
- Tenant resolution order:
  1. JWT claim (tenantSlug)
  2. X-Tenant-ID HTTP header (for service-to-service)
  3. Subdomain extraction from Host header (acme.yoursaas.com → acme)
- TenantContext: ThreadLocal<String> holding current tenant slug
- TenantAwareDataSource: wraps HikariCP, sets search_path=tenant_{slug},public on connection checkout

### Public Schema Entities

Entity: Tenant
  - id: UUID
  - slug: String (UNIQUE, lowercase alphanum + hyphens, 3-50 chars, immutable after creation)
  - name: String
  - plan: TenantPlan enum (FREE | STARTER | PRO | ENTERPRISE)
  - status: TenantStatus enum (PROVISIONING | ACTIVE | SUSPENDED | DELETED)
  - schemaName: String (generated: tenant_ + slug)
  - createdAt: Instant
  - trialEndsAt: Instant (NULLABLE)
  - settings: JSONB (flexible tenant config: maxUsers, storageGb, features list)
  - billingEmail: String
  - timezone: String (e.g. "America/New_York")

Entity: TenantUser (cross-tenant identity, maps user_id to tenant)
  - id: UUID
  - userId: UUID
  - tenantId: UUID (FK → Tenant)
  - role: TenantRole enum (OWNER | ADMIN | MEMBER | VIEWER)
  - invitedBy: UUID (NULLABLE)
  - joinedAt: Instant
  - active: boolean

Entity: Invitation
  - id: UUID
  - tenantId: UUID (FK)
  - email: String
  - role: TenantRole
  - token: String (UNIQUE, 64-char random)
  - invitedBy: UUID
  - expiresAt: Instant
  - acceptedAt: Instant (NULLABLE)
  - status: InvitationStatus enum (PENDING | ACCEPTED | EXPIRED | REVOKED)

Entity: PlatformUser (in public schema — cross-tenant identity)
  - id: UUID
  - email: String (UNIQUE)
  - passwordHash: String
  - fullName: String
  - avatarUrl: String (NULLABLE)
  - status: UserStatus enum (ACTIVE | DISABLED)
  - createdAt: Instant

### Per-Tenant Schema Entities (in tenant_{slug} schema)

Entity: TenantProfile (mirrors PlatformUser info per tenant for customization)
  - userId: UUID (PK, references public.platform_users.id logically)
  - displayName: String
  - bio: String (NULLABLE)
  - preferences: JSONB
  - lastSeenAt: Instant

Entity: AuditLog
  - id: UUID
  - userId: UUID
  - action: String (e.g. "project.created", "member.invited")
  - resourceType: String
  - resourceId: UUID (NULLABLE)
  - metadata: JSONB
  - ipAddress: String
  - timestamp: Instant

Entity: FeatureFlag
  - id: UUID
  - key: String (UNIQUE per tenant)
  - enabled: boolean
  - rolloutPercentage: int (0-100)
  - enabledUserIds: UUID[] (NULLABLE, whitelist)
  - createdAt: Instant

### Flyway Migrations
Public schema:
  db/migration/public/V1__platform_users.sql
  db/migration/public/V2__tenants.sql
  db/migration/public/V3__tenant_users.sql
  db/migration/public/V4__invitations.sql

Per-tenant schema (run for each tenant on provisioning):
  db/migration/tenant/V1__tenant_profile.sql
  db/migration/tenant/V2__audit_log.sql
  db/migration/tenant/V3__feature_flags.sql

TenantMigrationService: runs tenant migrations on new tenant provisioning using Flyway.configure().schemas(schemaName).load()

### API Endpoints

Platform (no tenant context):
  POST /api/v1/platform/auth/register    → create PlatformUser
  POST /api/v1/platform/auth/login       → returns JWT with no tenantSlug claim
  POST /api/v1/platform/tenants          → provision new tenant (creates schema, runs migrations, creates OWNER record)
  GET  /api/v1/platform/tenants/mine     → list tenants the current user belongs to

Tenant-scoped (require tenant context in JWT or header):
  POST /api/v1/auth/login                → re-issues JWT with tenantSlug claim embedded
  GET  /api/v1/users                     → list members of current tenant (paginated), ADMIN+
  DELETE /api/v1/users/{userId}          → remove member from tenant, ADMIN+
  PATCH /api/v1/users/{userId}/role      → change member role, OWNER only

  POST /api/v1/invitations               → ADMIN+ invite member by email
  GET  /api/v1/invitations               → list pending invitations
  DELETE /api/v1/invitations/{id}        → revoke invitation
  POST /api/v1/invitations/accept        → body: { token } — accepts invite, joins tenant
  GET  /api/v1/invitations/validate/{token} → check token validity (public endpoint)

  GET  /api/v1/settings                  → get tenant settings (plan, features, limits)
  PATCH /api/v1/settings                 → OWNER update settings (timezone, billing email)

  GET  /api/v1/feature-flags             → list flags
  POST /api/v1/feature-flags             → ADMIN create flag
  PATCH /api/v1/feature-flags/{key}      → toggle, set rollout percentage
  GET  /api/v1/feature-flags/{key}/evaluate → evaluate flag for current user (considers rollout %)

  GET  /api/v1/audit-log                 → ADMIN paginated audit log, filter by userId, action, dateRange

Platform Admin (/api/v1/platform/admin/** — requires platform admin JWT, separate PLATFORM_ADMIN role):
  GET    /tenants               → all tenants, filter by status/plan
  POST   /tenants/{id}/suspend  → suspend tenant
  POST   /tenants/{id}/activate → reactivate
  DELETE /tenants/{id}          → soft delete (marks DELETED, does not drop schema)
  POST   /tenants/{id}/plan     → change plan

### Plan Limits Enforcement
- PlanLimitService checks on every relevant action
- Limits stored in TenantPlanLimits @ConfigurationProperties:
  FREE:       maxUsers=5,  storageGb=1,  apiCallsPerMonth=1000
  STARTER:    maxUsers=25, storageGb=10, apiCallsPerMonth=10000
  PRO:        maxUsers=100,storageGb=50, apiCallsPerMonth=100000
  ENTERPRISE: maxUsers=-1, storageGb=-1, apiCallsPerMonth=-1  (-1 = unlimited)
- On limit breach: throw PlanLimitExceededException (402 Payment Required)

### Tenant Provisioning Flow (transactional)
1. Validate slug uniqueness
2. Create Tenant record in public schema (status=PROVISIONING)
3. Create Postgres schema: CREATE SCHEMA IF NOT EXISTS tenant_{slug}
4. Run Flyway tenant migrations against the new schema
5. Create TenantUser record linking creator as OWNER
6. Set status=ACTIVE
7. Send welcome email

### TenantContext & Filter Chain
- TenantContextFilter (OncePerRequestFilter, before JwtAuthFilter):
  - Resolves tenant from subdomain/header/JWT
  - Sets TenantContext.setCurrentTenant(slug)
  - Sets search_path on the DataSource connection via AbstractRoutingDataSource

- TenantAwareJpaConfig: configures LocalContainerEntityManagerFactoryBean per-request schema

### Caching
- All Redis keys prefixed with tenant slug: {tenantSlug}:{key}
- @TenantCacheable custom annotation that auto-prefixes keys

### Tests
- Unit: TenantProvisioningServiceTest, PlanLimitServiceTest, FeatureFlagServiceTest, TenantContextFilterTest
- Integration: TenantProvisioningIT (Testcontainers — creates real schemas, runs migrations, tears down)
  - Multi-tenant isolation test: data written in tenant_a must not appear in tenant_b
  - Invitation flow IT
  - Plan limit enforcement IT

### docker-compose.yml
- app
- postgres:16-alpine (configured with max_connections=500)
- redis:7-alpine
```

---

## PROJECT 6 — Real-time Chat App

```
Build a production-ready real-time chat application backend in Java 21 + Spring Boot 3.3 using WebSocket + STOMP. Zero comments in any source file.

### Tech Stack
- Spring WebSocket, Spring Messaging (STOMP)
- Spring Security (JWT for HTTP + WebSocket handshake)
- Spring Data JPA, PostgreSQL, Flyway
- Redis (message broker relay, presence tracking, pub/sub)
- springdoc-openapi (for REST endpoints)
- Testcontainers
- Lombok, MapStruct

### Domain Model

Entity: User
  - id: UUID, email, passwordHash, username (UNIQUE, 3-30 chars), fullName, avatarUrl, status (ONLINE|AWAY|OFFLINE|DND), bio, createdAt

Entity: ChatRoom
  - id: UUID
  - name: String (NULLABLE, for groups)
  - type: RoomType enum (DIRECT | GROUP | CHANNEL)
  - description: String (NULLABLE)
  - avatarUrl: String (NULLABLE)
  - createdBy: UUID
  - createdAt: Instant
  - lastActivityAt: Instant (updated on each message)
  - maxMembers: int (50 for GROUP, unlimited for CHANNEL, 2 for DIRECT)
  - archived: boolean

Entity: RoomMembership
  - id: UUID
  - roomId: UUID (FK)
  - userId: UUID (FK)
  - role: MemberRole enum (OWNER | ADMIN | MEMBER)
  - joinedAt: Instant
  - lastReadAt: Instant (used for unread count calculation)
  - notificationsEnabled: boolean
  - nickname: String (NULLABLE, per-room display name override)
  - active: boolean (false = left the room)

Entity: Message
  - id: UUID
  - roomId: UUID (FK)
  - senderId: UUID (FK)
  - content: String (max 4000 chars, NULLABLE if attachments present)
  - type: MessageType enum (TEXT | IMAGE | FILE | AUDIO | SYSTEM | REACTION_SUMMARY)
  - parentId: UUID (NULLABLE, FK to Message for threads)
  - replyToId: UUID (NULLABLE, FK to Message for inline reply preview)
  - editedAt: Instant (NULLABLE)
  - deletedAt: Instant (NULLABLE, soft delete)
  - createdAt: Instant
  - metadata: JSONB (NULLABLE, e.g. link preview, file info)
  - isPinned: boolean

Entity: MessageAttachment
  - id: UUID
  - messageId: UUID (FK)
  - filename: String
  - mimeType: String
  - sizeBytes: long
  - storageKey: String (S3 key)
  - url: String (presigned or CDN)
  - width: Integer (NULLABLE, for images)
  - height: Integer (NULLABLE, for images)

Entity: Reaction
  - id: UUID
  - messageId: UUID (FK)
  - userId: UUID (FK)
  - emoji: String (e.g. "👍", "❤️", stored as unicode)
  - createdAt: Instant
  - UNIQUE constraint: (messageId, userId, emoji)

Entity: UserPresence (NOT persisted to DB — Redis only)
  - userId: UUID
  - status: PresenceStatus (ONLINE | AWAY | OFFLINE | DND)
  - lastSeen: Instant
  - activeRooms: Set<UUID>
  Redis key: presence:{userId} with TTL of 30 seconds (heartbeat refreshes it)

Entity: TypingIndicator (Redis only)
  Redis key: typing:{roomId}:{userId} with TTL of 3 seconds

### Flyway Migrations
V1__users.sql
V2__chat_rooms.sql
V3__room_memberships.sql
V4__messages.sql
V5__message_attachments.sql
V6__reactions.sql
V7__indexes.sql (index on roomId+createdAt for cursor pagination, userId+roomId for membership lookup)

### REST API Endpoints (/api/v1, JWT auth)

Auth: standard register/login/refresh (same pattern as Project 2)

Users:
  GET  /users/search?q=&limit=10   → search by username/fullName
  GET  /users/{id}                 → public profile
  PUT  /users/me                   → update profile
  POST /users/me/avatar            → multipart upload avatar

Rooms:
  GET    /rooms                    → list rooms the current user belongs to, sorted by lastActivityAt
  POST   /rooms/direct             → body: { targetUserId } — creates or returns existing DIRECT room
  POST   /rooms/group              → body: { name, memberIds, description? }
  GET    /rooms/{id}               → room detail
  PATCH  /rooms/{id}               → update name/description/avatar (ADMIN+)
  DELETE /rooms/{id}               → archive room (OWNER)
  POST   /rooms/{id}/members       → add members (ADMIN+)
  DELETE /rooms/{id}/members/{uid} → remove member or leave (own uid = leave)
  PATCH  /rooms/{id}/members/{uid}/role → change role (OWNER)
  GET    /rooms/{id}/members       → paginated member list

Messages (cursor-based pagination, not offset):
  GET  /rooms/{roomId}/messages?before={messageId}&limit=50 → messages older than cursor
  GET  /rooms/{roomId}/messages?after={messageId}&limit=50  → messages newer than cursor
  POST /rooms/{roomId}/messages    → send (also available via WebSocket)
  PUT  /messages/{id}              → edit (sender only, within 15 min of send)
  DELETE /messages/{id}            → soft delete (sender) or hard delete (ADMIN)
  POST /messages/{id}/pin          → ADMIN
  DELETE /messages/{id}/pin        → ADMIN
  GET  /rooms/{roomId}/pinned      → list pinned messages

Reactions:
  POST   /messages/{id}/reactions  → body: { emoji }
  DELETE /messages/{id}/reactions/{emoji} → remove own reaction

Search:
  GET /search?q=&roomId?=&limit=20 → full-text search in accessible rooms
  (Uses Postgres tsvector/GIN index on messages.content)

Threads:
  GET /messages/{id}/thread → paginated thread replies
  POST /messages/{id}/thread → reply in thread (also via WebSocket)

Files:
  POST /files/upload-url → returns presigned S3 PUT URL + storageKey (for direct-to-S3 client upload)
  POST /files/confirm    → body: { storageKey, messageId } — confirms upload, creates MessageAttachment

### WebSocket Events (STOMP over SockJS)

Connection:
  - Client connects to /ws with JWT as query param ?token=... (validated in WebSocket handshake interceptor)

Client → Server (app destinations):
  /app/chat.send          → payload: { roomId, content, type, replyToId?, parentId? }
  /app/chat.edit          → payload: { messageId, content }
  /app/chat.delete        → payload: { messageId }
  /app/chat.react         → payload: { messageId, emoji, action: ADD|REMOVE }
  /app/chat.typing        → payload: { roomId, isTyping }
  /app/chat.read          → payload: { roomId, lastReadMessageId }
  /app/presence.heartbeat → payload: {} (sent every 20s by client)
  /app/presence.status    → payload: { status: ONLINE|AWAY|DND }

Server → Client (topic subscriptions):
  /topic/room/{roomId}           → new messages, edits, deletes, reactions
  /topic/room/{roomId}/typing    → typing indicators { userId, username, isTyping }
  /topic/room/{roomId}/presence  → member presence changes
  /user/queue/notifications      → private: unread counts, room updates, direct messages
  /user/queue/errors             → private: delivery failures, permission errors

Event Payloads (all via STOMP):
  MessageEvent:    { type: NEW_MESSAGE|EDITED|DELETED, message: MessageDto }
  ReactionEvent:   { type: ADDED|REMOVED, messageId, emoji, userId, reactions: [{ emoji, count, userIds }] }
  TypingEvent:     { roomId, userId, username, isTyping }
  PresenceEvent:   { userId, status, lastSeen }
  ReadEvent:       { roomId, userId, lastReadMessageId }

### Redis Integration
- STOMP broker relay: Spring configured with Redis Pub/Sub as the external broker relay (using SimpleBroker in dev, external Redis in prod)
- Presence: HSET presence:{userId} status,lastSeen,activeRooms; EXPIRE 30s; refreshed by heartbeat
- Typing: SET typing:{roomId}:{userId} EX 3 → auto-expires = stop typing
- Online users in room: SMEMBERS online:{roomId}
- Unread counts: HASH unread:{userId} → { roomId: count } (incremented on new message, reset on /read)

### Message Delivery Guarantee
- On send via WebSocket: persist to DB first, then publish to Redis Pub/Sub
- If user is offline: increment unread counter in Redis; on next connection, push missed messages via /user/queue/notifications

### Scheduled Jobs
- PresenceCleanupJob: every 60s — scan for presence keys near expiry, mark users OFFLINE in Redis
- MessageRetentionJob: weekly — hard-delete soft-deleted messages older than 30 days

### Security
- WebSocket connections: JWT validation in HandshakeInterceptor
- STOMP messages: SimpMessageSendingOperations scoped to user's session rooms
- Rate limit on message send: 20 messages/minute per user (Redis counter)
- Message edit window: 15 minutes after send

### Exception Handling in WebSocket
- All WebSocket errors published to /user/queue/errors
- Error payload: { code, message, originalPayload }

### Tests
- Unit: MessageServiceTest, PresenceServiceTest, ReactionServiceTest
- Integration: ChatWebSocketIT (uses StompClient + Testcontainers Postgres + Redis)
  - Send message → verify received on subscriber
  - Typing indicator flow
  - Reaction add/remove
- REST: RoomControllerIT, MessageControllerIT

### docker-compose.yml
- app (WebSocket port 8080)
- postgres:16-alpine
- redis:7-alpine (used as both cache and broker relay)
- nginx (WebSocket proxy with upgrade headers, SSL termination in prod)
```

---

## PROJECT 7 — E-commerce Backend

```
Build a production-ready E-commerce REST API in Java 21 + Spring Boot 3.3. Full order lifecycle, inventory, payments, and notifications. Zero comments in any source file.

### Tech Stack
- Spring Web, Spring Data JPA, Spring Security (JWT)
- PostgreSQL + Flyway
- Redis (cart storage, session, rate limiting, idempotency keys)
- Spring Events / ApplicationEventPublisher (domain events)
- Stripe SDK (com.stripe:stripe-java:25.x) for payments
- Spring Mail (order confirmations)
- springdoc-openapi, Lombok, MapStruct, Testcontainers

### Domain Model

Entity: User (standard: id, email, passwordHash, fullName, phone, role: CUSTOMER|ADMIN, createdAt)

Entity: Address
  - id: UUID, userId, label (HOME|WORK|OTHER), line1, line2, city, state, postalCode, country (ISO 3166-1 alpha-2), isDefault, createdAt

Entity: Category
  - id: UUID, name, slug (UNIQUE), description, parentId (self-ref FK, NULLABLE), imageUrl, sortOrder, active

Entity: Product
  - id: UUID
  - name: String
  - slug: String (UNIQUE)
  - description: String (max 10000)
  - categoryId: UUID (FK)
  - brand: String
  - sku: String (UNIQUE, NOT NULL)
  - basePrice: BigDecimal (precision=19, scale=4)
  - compareAtPrice: BigDecimal (NULLABLE, for showing "was" price)
  - images: List<ProductImage>
  - attributes: JSONB (color, size, material, etc.)
  - tags: text[]
  - status: ProductStatus enum (DRAFT | ACTIVE | ARCHIVED)
  - featured: boolean
  - averageRating: BigDecimal (denormalized, updated on review)
  - reviewCount: int
  - createdAt: Instant
  - updatedAt: Instant

Entity: ProductVariant
  - id: UUID
  - productId: UUID (FK)
  - title: String (e.g. "Red / Large")
  - sku: String (UNIQUE)
  - price: BigDecimal (NULLABLE, overrides basePrice if set)
  - attributes: JSONB (e.g. { color: "Red", size: "L" })
  - stockQuantity: int (NOT NULL, >= 0)
  - reservedQuantity: int (locked during checkout, released on expire/cancel)
  - weight: BigDecimal (kg, NULLABLE)
  - active: boolean

Entity: InventoryReservation
  - id: UUID
  - variantId: UUID (FK)
  - orderId: UUID (NULLABLE, FK — set when order placed)
  - cartId: String (Redis cart ID)
  - quantity: int
  - expiresAt: Instant (15 min TTL)
  - status: ReservationStatus enum (PENDING | CONFIRMED | RELEASED | EXPIRED)

Entity: Cart (stored in Redis as JSON, not in DB)
  Key: cart:{cartId}
  Structure: { cartId, userId?, items: [{ variantId, productId, quantity, priceAtAdd, addedAt }], createdAt, updatedAt, appliedCoupon?, reservationIds: [] }
  TTL: 7 days for unauthenticated, 30 days for authenticated

Entity: Coupon
  - id: UUID
  - code: String (UNIQUE, uppercase alphanumeric)
  - type: DiscountType enum (PERCENTAGE | FIXED_AMOUNT | FREE_SHIPPING)
  - value: BigDecimal
  - minOrderAmount: BigDecimal (NULLABLE)
  - maxDiscountAmount: BigDecimal (NULLABLE, cap for PERCENTAGE)
  - usageLimit: int (NULLABLE, total uses)
  - perUserLimit: int (NULLABLE, uses per user)
  - usageCount: int
  - validFrom: Instant
  - validUntil: Instant (NULLABLE)
  - active: boolean

Entity: Order
  - id: UUID
  - orderNumber: String (UNIQUE, auto-generated: ORD-YYYYMMDD-XXXXX)
  - userId: UUID (FK, NULLABLE for guest checkout)
  - guestEmail: String (NULLABLE)
  - status: OrderStatus enum (PENDING_PAYMENT | PAYMENT_FAILED | CONFIRMED | PROCESSING | SHIPPED | DELIVERED | CANCELLED | REFUNDED | PARTIALLY_REFUNDED)
  - shippingAddress: JSONB (snapshot of address at order time)
  - billingAddress: JSONB
  - subtotal: BigDecimal
  - discountAmount: BigDecimal (default 0)
  - shippingAmount: BigDecimal
  - taxAmount: BigDecimal
  - totalAmount: BigDecimal
  - currency: String (ISO 4217)
  - couponCode: String (NULLABLE)
  - notes: String (NULLABLE)
  - stripePaymentIntentId: String (NULLABLE)
  - stripeChargeId: String (NULLABLE)
  - refundedAmount: BigDecimal (default 0)
  - createdAt: Instant
  - updatedAt: Instant
  - paidAt: Instant (NULLABLE)
  - cancelledAt: Instant (NULLABLE)
  - cancelReason: String (NULLABLE)

Entity: OrderItem
  - id: UUID
  - orderId: UUID (FK)
  - variantId: UUID (FK, NULLABLE if variant deleted)
  - productId: UUID (FK)
  - productName: String (snapshot)
  - variantTitle: String (snapshot)
  - sku: String (snapshot)
  - quantity: int
  - unitPrice: BigDecimal (snapshot at purchase time)
  - totalPrice: BigDecimal

Entity: Shipment
  - id: UUID
  - orderId: UUID (FK)
  - carrier: String (e.g. FedEx, UPS, Delhivery)
  - trackingNumber: String
  - trackingUrl: String (NULLABLE)
  - status: ShipmentStatus enum (CREATED | DISPATCHED | IN_TRANSIT | OUT_FOR_DELIVERY | DELIVERED | FAILED | RETURNED)
  - shippedAt: Instant (NULLABLE)
  - estimatedDelivery: LocalDate (NULLABLE)
  - deliveredAt: Instant (NULLABLE)
  - events: List<ShipmentEvent> (OneToMany)

Entity: ShipmentEvent
  - id: UUID, shipmentId, status, description, location, occurredAt: Instant

Entity: Review
  - id: UUID, productId, userId, orderId, rating (1-5), title, body, verified (bought product), helpfulCount, createdAt, status: ReviewStatus (PENDING | APPROVED | REJECTED)

Entity: Wishlist
  - id: UUID, userId, name, public: boolean, createdAt
  - items: Set<WishlistItem>

Entity: WishlistItem
  - id: UUID, wishlistId, productId, variantId (NULLABLE), addedAt

### Flyway Migrations
V1__users.sql
V2__addresses.sql
V3__categories.sql
V4__products.sql
V5__product_variants.sql
V6__inventory_reservations.sql
V7__coupons.sql
V8__orders.sql
V9__order_items.sql
V10__shipments.sql
V11__reviews.sql
V12__wishlists.sql
V13__indexes.sql
V14__seed_categories.sql

### API Endpoints (all /api/v1)

Auth: standard JWT register/login/refresh

Products (public read, ADMIN write):
  GET  /products                   → paginated, filter: category, brand, minPrice, maxPrice, rating, available, tags, q (search), sort
  GET  /products/{slug}            → detail with variants, images, average rating
  GET  /products/featured          → limit 12
  GET  /products/new-arrivals      → last 30 days, limit 12
  POST /products                   → ADMIN
  PUT  /products/{id}              → ADMIN
  PATCH /products/{id}/status      → ADMIN
  POST /products/{id}/images       → ADMIN multipart upload
  DELETE /products/{id}/images/{imageId} → ADMIN
  GET  /products/{id}/variants     → all variants
  POST /products/{id}/variants     → ADMIN
  PATCH /products/{id}/variants/{variantId}/stock → ADMIN adjust stock { quantity, reason }

Categories (public read):
  GET  /categories          → tree structure (nested)
  GET  /categories/{slug}   → with child categories
  POST /categories          → ADMIN
  PUT  /categories/{id}     → ADMIN

Cart (anonymous or authenticated):
  GET    /cart                → get or create cart (cartId from cookie or header)
  POST   /cart/items          → add item { variantId, quantity }; reserves inventory
  PATCH  /cart/items/{variantId} → update quantity
  DELETE /cart/items/{variantId} → remove item; releases reservation
  DELETE /cart                → clear cart
  POST   /cart/coupon         → apply coupon { code }
  DELETE /cart/coupon         → remove coupon
  POST   /cart/merge          → merge anonymous cart into authenticated user's cart on login
  GET    /cart/summary        → { subtotal, discount, shipping, tax, total, items }

Checkout:
  POST /checkout/initiate
    Body: { cartId, shippingAddressId or shippingAddress, billingAddressId or billingAddress, shippingMethod, notes? }
    → Validates cart, stock, coupon; calculates totals; creates Order (PENDING_PAYMENT); creates Stripe PaymentIntent
    → Returns: { orderId, orderNumber, clientSecret (for Stripe Elements), totalAmount }

  POST /checkout/confirm/{orderId}
    → Frontend confirms Stripe payment, calls this to verify Stripe PaymentIntent succeeded
    → Confirms inventory reservations, reduces stock, updates order to CONFIRMED
    → Publishes OrderConfirmedEvent (sends email, updates coupon usage)

  POST /checkout/guest
    → Same as initiate but requires guestEmail

Stripe Webhook (POST /webhooks/stripe, requires Stripe-Signature header verification):
  - payment_intent.succeeded → confirm order if not already done
  - payment_intent.payment_failed → update order to PAYMENT_FAILED, release reservations
  - charge.refunded → update refundedAmount, set REFUNDED or PARTIALLY_REFUNDED status

Orders:
  GET  /orders               → current user's orders (paginated)
  GET  /orders/{id}          → order detail with items, shipment
  GET  /orders/{orderNumber} → lookup by order number
  POST /orders/{id}/cancel   → user can cancel if PENDING_PAYMENT or CONFIRMED (before shipment)

Admin Orders:
  GET    /admin/orders         → all orders, filter by status, dateRange, userId
  POST   /admin/orders/{id}/process → move to PROCESSING
  POST   /admin/orders/{id}/ship    → create Shipment { carrier, trackingNumber, trackingUrl }
  POST   /admin/orders/{id}/refund  → body: { amount, reason } → calls Stripe Refund API
  PATCH  /admin/shipments/{id}/events → add ShipmentEvent

Reviews:
  GET  /products/{id}/reviews  → paginated, sort by recent/helpful/rating
  POST /products/{id}/reviews  → authenticated, must have delivered order containing product, one review per product per user
  POST /reviews/{id}/helpful   → vote helpful (idempotent)
  PATCH /admin/reviews/{id}/status → ADMIN approve/reject

Wishlists:
  GET    /wishlists            → user's wishlists
  POST   /wishlists            → create
  GET    /wishlists/{id}       → detail (public wishlists accessible without auth)
  DELETE /wishlists/{id}       → delete
  POST   /wishlists/{id}/items → add { productId, variantId? }
  DELETE /wishlists/{id}/items/{productId} → remove
  POST   /wishlists/{id}/add-to-cart → move all items to cart

### Idempotency
- All order mutation endpoints require Idempotency-Key header
- Store idempotency key → response hash in Redis with 24h TTL
- Duplicate requests return cached response without re-processing

### Inventory Reservation Flow
1. Add to cart → create InventoryReservation (15 min TTL), reduce availableStock
2. Checkout confirmed → convert PENDING → CONFIRMED reservation, release reservedQuantity field
3. If cart expires or user removes item → release reservation, restore availableStock
4. Scheduled ReservationExpiryJob runs every 5 min: release PENDING reservations past expiresAt

### Price Calculation
ShippingCalculator: { FREE_SHIPPING if order >= ₹999, else flat ₹99 }
TaxCalculator: 18% GST on (subtotal - discount)
Totals: subtotal - discountAmount + shippingAmount + taxAmount = totalAmount

### Domain Events (Spring ApplicationEventPublisher)
OrderConfirmedEvent → sends order confirmation email (HTML template via Thymeleaf + JavaMailSender)
OrderShippedEvent   → sends shipping notification email with tracking link
OrderDeliveredEvent → sends review request email
LowStockEvent       → published when stockQuantity <= 5; ADMIN notified via email

### Tests
- Unit: CartServiceTest, PriceCalculatorTest, CouponValidatorTest, InventoryServiceTest
- Integration: CheckoutControllerIT (Testcontainers + WireMock for Stripe), OrderControllerIT, WebhookControllerIT

### docker-compose.yml
- app
- postgres:16-alpine
- redis:7-alpine
- wiremock (Stripe API mock)
```

---

## PROJECT 8 — File Storage Service

```
Build a production-ready File Storage Service REST API in Java 21 + Spring Boot 3.3. Zero comments in any source file.

### Tech Stack
- Spring Web (streaming), Spring Data JPA
- Spring Security (JWT + API key)
- PostgreSQL + Flyway
- AWS SDK v2 (software.amazon.awssdk) for S3
- MinIO (S3-compatible, for local dev)
- Redis (presigned URL caching, quota tracking)
- Tika (Apache Tika — MIME type detection)
- Thumbnailator (image thumbnail generation)
- springdoc-openapi, Testcontainers, Lombok

### Domain Model

Entity: StorageUser
  - id: UUID, email, passwordHash, displayName, quotaBytes (default 5GB), usedBytes, plan: StoragePlan enum (FREE|PRO|ENTERPRISE), createdAt

Entity: Bucket
  - id: UUID
  - ownerId: UUID (FK)
  - name: String (UNIQUE globally, 3-63 chars, lowercase alphanum + hyphens)
  - displayName: String
  - region: String (e.g. "us-east-1")
  - access: BucketAccess enum (PRIVATE | PUBLIC_READ | PUBLIC_READ_WRITE)
  - versioning: boolean
  - lifecycleRules: JSONB (NULLABLE, e.g. delete after N days, transition to GLACIER)
  - storageClass: StorageClass enum (STANDARD | IA | GLACIER)
  - createdAt: Instant
  - s3BucketName: String (the actual bucket name in S3/MinIO)

Entity: StorageObject
  - id: UUID
  - bucketId: UUID (FK)
  - key: String (full path within bucket, e.g. "photos/2024/vacation.jpg")
  - originalFilename: String
  - contentType: String (Tika-detected)
  - sizeBytes: long
  - etag: String (MD5 hash from S3)
  - storageClass: StorageClass enum
  - versionId: String (NULLABLE, if versioning enabled)
  - metadata: JSONB (custom user-defined key-value pairs, max 10 entries, key max 50 chars, value max 200 chars)
  - tags: text[]
  - uploadedAt: Instant
  - lastModifiedAt: Instant
  - expiresAt: Instant (NULLABLE, auto-delete lifecycle)
  - deleted: boolean
  - deletedAt: Instant (NULLABLE)
  - uploadedBy: UUID

Entity: ObjectVersion (when bucket versioning enabled)
  - id: UUID, objectId, versionId, sizeBytes, etag, createdAt, isLatest: boolean

Entity: SharedLink
  - id: UUID
  - objectId: UUID (FK)
  - token: String (UNIQUE, 32-char random)
  - expiresAt: Instant (NULLABLE, permanent if null)
  - maxDownloads: int (NULLABLE, NULLABLE = unlimited)
  - downloadCount: int
  - password: String (NULLABLE, bcrypt hash)
  - createdBy: UUID
  - createdAt: Instant
  - active: boolean

Entity: BucketPolicy
  - id: UUID
  - bucketId: UUID (FK)
  - granteeType: GranteeType enum (USER | API_KEY | PUBLIC)
  - granteeId: UUID (NULLABLE, user or api key id)
  - permissions: Set<Permission> enum (READ | WRITE | DELETE | ADMIN)

Entity: ApiKey
  - id: UUID, userId, keyHash (SHA-256), name, scopedBucketId (NULLABLE, restrict to bucket), permissions: Set<Permission>, expiresAt (NULLABLE), createdAt, lastUsedAt, active

Entity: UploadSession (multipart uploads)
  - id: UUID
  - objectKey: String
  - bucketId: UUID
  - uploadedBy: UUID
  - s3UploadId: String
  - totalParts: int (NULLABLE, known after initiation)
  - uploadedParts: JSONB (list of {partNumber, etag})
  - status: UploadStatus enum (INITIATED | IN_PROGRESS | COMPLETED | ABORTED)
  - createdAt: Instant
  - expiresAt: Instant (24h, after which S3 upload is aborted)

### API Endpoints (/api/v1)

Auth: standard JWT + API key (X-Api-Key header) support on all endpoints

Buckets:
  GET    /buckets                → list user's buckets with usage stats
  POST   /buckets                → create bucket { name, region, access, versioning, storageClass }
  GET    /buckets/{name}         → bucket detail + stats (objectCount, totalSize)
  PATCH  /buckets/{name}         → update access, versioning, lifecycle
  DELETE /buckets/{name}         → delete (must be empty)
  GET    /buckets/{name}/policy  → get access policies
  POST   /buckets/{name}/policy  → grant access to user or api key
  DELETE /buckets/{name}/policy/{policyId} → revoke

Objects:
  GET    /buckets/{name}/objects → list objects (paginated with cursor), filter: prefix, contentType, tags
  POST   /buckets/{name}/objects → single-file upload (multipart/form-data, max 100MB via streaming)
    - Detects MIME via Tika
    - Generates thumbnails for images (200px, 600px) stored alongside original
    - Checks user quota before upload
    - Returns { objectId, key, url, contentType, sizeBytes, etag }
  GET    /buckets/{name}/objects/{key} → metadata only
  HEAD   /buckets/{name}/objects/{key} → headers only (like S3 HEAD)
  DELETE /buckets/{name}/objects/{key} → soft delete (or hard if no versioning)
  PATCH  /buckets/{name}/objects/{key}/metadata → update custom metadata
  PATCH  /buckets/{name}/objects/{key}/tags
  POST   /buckets/{name}/objects/{key}/copy → body: { destinationBucket, destinationKey }
  POST   /buckets/{name}/objects/{key}/move → rename/move within or across buckets

Download:
  GET /buckets/{name}/objects/{key}/download
    - Streams content directly from S3 with Range header support (resumable downloads)
    - Checks bucket access (PRIVATE requires auth, PUBLIC_READ = open)
    - Sets Content-Disposition: attachment; filename=...
    - Supports If-None-Match (ETag) and If-Modified-Since for browser caching

Presigned URLs:
  POST /buckets/{name}/objects/presign-upload
    Body: { key, contentType, sizeBytes, expiresInSeconds (max 3600), metadata? }
    Returns: { uploadUrl, uploadId, fields? (for POST upload), expiresAt }
  POST /buckets/{name}/objects/{key}/presign-download
    Body: { expiresInSeconds }
    Returns: { downloadUrl, expiresAt }

Multipart Upload (for files > 100MB):
  POST   /buckets/{name}/multipart/initiate    → body: { key, contentType, totalSize } → returns { uploadSessionId, s3UploadId, partSize }
  PUT    /buckets/{name}/multipart/{sessionId}/parts/{partNumber} → stream part bytes (5MB-5GB per part)
  POST   /buckets/{name}/multipart/{sessionId}/complete → body: { parts: [{partNumber, etag}] }
  DELETE /buckets/{name}/multipart/{sessionId} → abort

Versions:
  GET    /buckets/{name}/objects/{key}/versions → list versions (versioned buckets only)
  GET    /buckets/{name}/objects/{key}/versions/{versionId}/download → download specific version
  DELETE /buckets/{name}/objects/{key}/versions/{versionId} → delete specific version

Shared Links:
  POST   /objects/{id}/share        → body: { expiresAt?, maxDownloads?, password? }
  GET    /objects/{id}/shares       → list active shared links
  DELETE /shares/{token}            → revoke

  GET /s/{token}                    → public endpoint — validate link (password check), redirect to presigned download URL
    Body (if password protected): { password }

User Quota:
  GET /users/me/quota               → { usedBytes, quotaBytes, usedPercent, objectCount }

Admin:
  GET /admin/users                  → list with quota usage
  PATCH /admin/users/{id}/quota     → set custom quota
  GET /admin/storage-stats          → total storage used, object count, per-plan breakdown
  POST /admin/users/{id}/buckets/{name}/sync → re-sync usedBytes from S3 if drift detected

### Streaming Upload Implementation
- Do NOT buffer entire file in memory
- Use InputStream directly from HttpServletRequest or MultipartFile.getInputStream()
- Stream to S3 via AWS SDK v2 RequestBody.fromInputStream(inputStream, contentLength)
- Quota check via Redis atomic counter before accepting upload bytes
- On S3 upload failure: do NOT persist StorageObject; rollback quota increment

### Thumbnail Generation
- On image upload (JPEG, PNG, WEBP, GIF): generate 200x200 and 600x600 thumbnails via Thumbnailator
- Store thumbnails at: {key}_thumb_200.jpg and {key}_thumb_600.jpg
- Non-blocking: use @Async CompletableFuture

### Storage Abstraction
StorageClient interface:
  - upload(String bucket, String key, InputStream data, long size, String contentType, Map<String,String> metadata): CompletableFuture<UploadResult>
  - download(String bucket, String key): S3Object
  - downloadRange(String bucket, String key, long start, long end): InputStream
  - delete(String bucket, String key): void
  - copy(String sourceBucket, String sourceKey, String destBucket, String destKey): void
  - generatePresignedPutUrl(String bucket, String key, Duration ttl): String
  - generatePresignedGetUrl(String bucket, String key, Duration ttl): String
  - createMultipartUpload(...): String
  - uploadPart(...): String (etag)
  - completeMultipartUpload(...): void
  - abortMultipartUpload(...): void

Implementations:
  - S3StorageClient (AWS SDK v2, for prod profile)
  - MinioStorageClient (MinIO SDK, same interface, for dev profile — MinIO is S3-compatible so actually uses S3 client with endpoint override)

### Configuration
app:
  storage:
    provider: ${STORAGE_PROVIDER:minio}  # minio | s3
    s3:
      access-key: ${AWS_ACCESS_KEY_ID}
      secret-key: ${AWS_SECRET_ACCESS_KEY}
      region: ${AWS_REGION:us-east-1}
      endpoint: ${S3_ENDPOINT:}  # override for MinIO
    bucket-prefix: ${BUCKET_PREFIX:dev-}
  quota:
    default-bytes: 5368709120  # 5 GB
  upload:
    max-single-file-bytes: 104857600  # 100 MB
    multipart-threshold-bytes: 104857600
    part-size-bytes: 10485760  # 10 MB

### Scheduled Jobs
- MultipartCleanupJob: hourly — abort & delete S3 multipart uploads for sessions past expiresAt
- ObjectLifecycleJob: daily — delete objects past expiresAt
- QuotaSyncJob: daily — recompute usedBytes from DB for each user (drift correction)

### Tests
- Unit: QuotaServiceTest, ThumbnailServiceTest, SharedLinkServiceTest
- Integration: ObjectUploadIT, MultipartUploadIT, DownloadStreamIT, PresignedUrlIT (all with Testcontainers MinIO via testcontainers-minio)

### docker-compose.yml
- app
- postgres:16-alpine
- redis:7-alpine
- minio/minio:latest (MINIO_ROOT_USER, MINIO_ROOT_PASSWORD, port 9000 + 9001 console)
```

---

## PROJECT 9 — Microservices with Kafka

```
Build a production-ready microservices system in Java 21 + Spring Boot 3.3, consisting of 4 independent services communicating via Apache Kafka. Zero comments in any source file.

### Architecture Overview
Services:
  1. api-gateway        — Spring Cloud Gateway (routing, auth, rate limiting)
  2. order-service      — order placement and lifecycle
  3. inventory-service  — stock management
  4. notification-service — email + push notifications

Inter-service: Apache Kafka (event-driven)
Service discovery: Spring Cloud Eureka (eureka-server as separate app)
Config: Spring Cloud Config (optional: inline per-service for simplicity)
Each service has its own PostgreSQL schema/database.

### Common Library (shared-lib Maven module)
- Common DTOs used across services (avoidance of duplication)
- Kafka event classes (OrderCreatedEvent, InventoryReservedEvent, etc.)
- Common error envelope
- JWT utility class
- Base exception types
All shared-lib classes are records (immutable).

### api-gateway (Spring Cloud Gateway + Spring Security)

Routes (in application.yml):
  /api/orders/**     → order-service:8081
  /api/inventory/**  → inventory-service:8082
  /api/notifications/**  → notification-service:8083

Filters:
  - AuthenticationFilter: validates JWT on all routes except /api/auth/**
  - RateLimitingFilter: Redis-backed, 100 req/min per user
  - RequestLoggingFilter: logs method, path, userId, duration
  - CorrelationIdFilter: adds X-Correlation-ID header (generated if absent)

No business logic in gateway. Auth is validate-only (verifies JWT signature, extracts claims, forwards via X-User-Id, X-User-Roles headers).

Eureka client: registers as API-GATEWAY.

### eureka-server (Spring Cloud Netflix Eureka)
Standalone Spring Boot app.
No business logic. Single application.yml config.
Dashboard at /eureka/web.

### order-service (port 8081)

DB: orders_db (Postgres)

Entities:
  Order: id, userId, status (PENDING|RESERVED|CONFIRMED|SHIPPED|DELIVERED|CANCELLED|FAILED), items: List<OrderItem>, totalAmount, shippingAddress: JSONB, createdAt, updatedAt, correlationId
  OrderItem: id, orderId, productId, productName (snapshot), quantity, unitPrice
  OutboxEvent: id, aggregateId, aggregateType, eventType, payload: JSONB, createdAt, processed: boolean, processedAt

Kafka Topics Published:
  order.created       → OrderCreatedEvent { orderId, userId, items: [{productId, quantity}], correlationId }
  order.cancelled     → OrderCancelledEvent { orderId, items, reason, correlationId }
  order.confirmed     → OrderConfirmedEvent { orderId, userId, totalAmount, shippingAddress, correlationId }

Kafka Topics Consumed:
  inventory.reserved  → InventoryReservedEvent → update order status to RESERVED, publish order.confirmed
  inventory.failed    → InventoryFailedEvent → update order status to FAILED, publish order.cancelled
  
REST Endpoints:
  POST   /api/orders           → place order { items, shippingAddress }
  GET    /api/orders           → user's orders (paginated)
  GET    /api/orders/{id}      → order detail
  POST   /api/orders/{id}/cancel → user cancels (if PENDING or RESERVED)
  GET    /api/admin/orders     → ADMIN: all orders
  
Outbox Pattern:
  - On order create: persist Order + OutboxEvent in same DB transaction
  - OutboxPublisherJob: every 1s, poll unprocessed OutboxEvents, publish to Kafka, mark processed
  - Guarantees at-least-once delivery even if Kafka is temporarily unavailable

### inventory-service (port 8082)

DB: inventory_db (Postgres)

Entities:
  Product: id, name, sku, stockQuantity, reservedQuantity, version (optimistic locking @Version), updatedAt
  InventoryReservation: id, orderId, productId, quantity, status (PENDING|CONFIRMED|RELEASED), expiresAt, createdAt
  OutboxEvent: (same pattern as order-service)

Kafka Topics Consumed:
  order.created    → Reserve inventory for each item in order
  order.cancelled  → Release reservations for cancelled order
  order.confirmed  → Confirm reservations (deduct from actual stock)

Kafka Topics Published:
  inventory.reserved → all items reserved successfully
  inventory.failed   → one or more items insufficient stock (includes which items failed)
  inventory.low-stock → published when stockQuantity drops below threshold (10 units)

Reservation Logic:
  1. For each item in OrderCreatedEvent: acquire pessimistic write lock on Product row
  2. Check: stockQuantity - reservedQuantity >= requestedQuantity
  3. If all items pass: increment reservedQuantity, create InventoryReservation, publish inventory.reserved
  4. If any item fails: rollback all increments, publish inventory.failed
  5. ReservationExpiryJob: every 5min, release PENDING reservations past expiresAt → restore reservedQuantity

REST Endpoints:
  GET    /api/inventory/products          → paginated product list with stock
  GET    /api/inventory/products/{id}     → product stock detail
  PATCH  /api/inventory/products/{id}/stock → ADMIN adjust stock { delta, reason }
  POST   /api/inventory/products          → ADMIN create product
  GET    /api/inventory/reservations      → ADMIN list active reservations
  GET    /api/inventory/low-stock         → products below threshold

### notification-service (port 8083)

DB: notifications_db (Postgres)

Entities:
  NotificationLog: id, userId, type (EMAIL|PUSH|SMS), template, recipient, status (SENT|FAILED|SKIPPED), metadata: JSONB, sentAt, failureReason
  UserNotificationPreference: userId, emailEnabled, pushEnabled, smsEnabled, orderUpdates, promotions, lowStockAlerts

Kafka Topics Consumed:
  order.confirmed  → Send "Order Confirmed" email + push
  order.cancelled  → Send "Order Cancelled" email
  order.shipped    → Send "Your order has shipped" email with tracking (when shipment info present in event)
  inventory.low-stock → Send admin alert email

Email: Spring Mail + Thymeleaf HTML templates (one template per notification type, stored in resources/templates/email/)
Push: Firebase Cloud Messaging (com.google.firebase:firebase-admin) — fire-and-forget, failure logged not retried
Retry: Spring Retry on email send failure, max 3 attempts with exponential backoff

REST Endpoints:
  GET  /api/notifications        → user's notification history (paginated)
  PUT  /api/notifications/preferences → update notification preferences
  GET  /api/notifications/preferences → get preferences
  POST /api/admin/notifications/test  → ADMIN test-fire a notification template

### Kafka Configuration (all services)
- Topics: created programmatically via KafkaAdmin + NewTopic beans
- Consumer group IDs: order-service-group, inventory-service-group, notification-service-group
- Each topic: 3 partitions, replication factor 1 (dev), 3 (prod)
- Dead Letter Topics: {topic}.DLT for each consumer
- ErrorHandlingDeserializer2 with DeadLetterPublishingRecoverer

Topic list with retention:
  order.created:       retention.ms=604800000 (7 days)
  order.cancelled:     retention.ms=604800000
  order.confirmed:     retention.ms=604800000
  inventory.reserved:  retention.ms=86400000 (1 day)
  inventory.failed:    retention.ms=86400000
  inventory.low-stock: retention.ms=86400000
  *.DLT:               retention.ms=2592000000 (30 days)

Serialization: JsonDeserializer / JsonSerializer with trusted packages configured

### Correlation
- X-Correlation-ID header flows from gateway → services → Kafka event payload
- Each service logs correlation ID in every log statement via MDC

### Tests (per service)
- order-service: OrderServiceTest, OrderControllerIT, OutboxPublisherIT, KafkaConsumerIT (EmbeddedKafka)
- inventory-service: ReservationServiceTest, InventoryControllerIT, ReservationExpiryJobTest, KafkaConsumerIT
- notification-service: NotificationServiceTest, KafkaConsumerIT

### docker-compose.yml services
- eureka-server
- api-gateway
- order-service
- inventory-service
- notification-service
- postgres (one instance with 3 databases: orders_db, inventory_db, notifications_db)
- kafka:7.6.1 (Confluent platform image)
- zookeeper:7.6.1
- kafka-ui (provectus/kafka-ui for local topic browsing)
- redis:7-alpine (used by api-gateway for rate limiting)

### Maven Structure
pom.xml (parent)
  ├── shared-lib/
  ├── eureka-server/
  ├── api-gateway/
  ├── order-service/
  ├── inventory-service/
  └── notification-service/
```

---

## PROJECT 10 — Job Scheduling Platform

```
Build a production-ready Job Scheduling Platform REST API in Java 21 + Spring Boot 3.3. Users define, schedule, monitor, and manage background jobs via REST. Zero comments in source files.

### Tech Stack
- Spring Web, Spring Security (JWT)
- Quartz Scheduler (org.quartz-scheduler:quartz) with JDBC JobStore (Postgres)
- Spring Data JPA (for job metadata, logs — separate from Quartz tables)
- PostgreSQL + Flyway
- Redis (job result cache, lock for clustered scheduler)
- WebSocket (Spring WebSocket) for real-time job execution logs
- springdoc-openapi, Testcontainers, Lombok

### Core Concepts
- Job: a definition of what to execute and how (type, config, schedule)
- Trigger: when to execute (immediate, one-time, cron, interval)
- Execution: a single run of a Job
- JobType: HTTP (call an external URL), SQL (run a query), SHELL (run a command), CHAIN (run jobs sequentially)

### Domain Model (Application DB — separate from Quartz tables)

Entity: JobDefinition
  - id: UUID
  - name: String (UNIQUE per user)
  - description: String (NULLABLE)
  - ownerId: UUID (FK)
  - type: JobType enum (HTTP | SQL | SHELL | CHAIN)
  - config: JSONB (type-specific config — see below)
  - schedule: JSONB (trigger definition — see below)
  - status: JobStatus enum (ACTIVE | PAUSED | DISABLED | ARCHIVED)
  - timezone: String (e.g. "Asia/Kolkata")
  - maxRetries: int (default 3)
  - retryDelaySeconds: int (default 60)
  - timeoutSeconds: int (default 300)
  - notifyOnFailure: boolean
  - notifyEmail: String (NULLABLE)
  - tags: text[]
  - createdAt: Instant
  - updatedAt: Instant
  - nextFireTime: Instant (NULLABLE, synced from Quartz)
  - lastFireTime: Instant (NULLABLE)

JobType configs (in config JSONB):
  HTTP:  { url, method (GET|POST|PUT|PATCH|DELETE), headers: {}, body (NULLABLE), expectedStatusCode (default 200), followRedirects }
  SQL:   { dataSourceId (FK to configured data sources), query, params: [] }
  SHELL: { command, workingDirectory, envVars: {} }
  CHAIN: { jobIds: [UUID], failFast (stop chain if one fails) }

Schedule definition (in schedule JSONB):
  { type: IMMEDIATE | ONE_TIME | CRON | FIXED_RATE | FIXED_DELAY }
  IMMEDIATE: {}
  ONE_TIME:  { at: "ISO-8601 datetime" }
  CRON:      { expression: "0 0 9 * * MON-FRI", misfire: FIRE_ONCE | SKIP | IGNORE }
  FIXED_RATE: { intervalSeconds, startAt (NULLABLE) }
  FIXED_DELAY: { delaySeconds, startAt (NULLABLE) }

Entity: JobExecution
  - id: UUID
  - jobId: UUID (FK to JobDefinition)
  - quartzFireInstanceId: String (correlates with Quartz internal ID)
  - triggeredBy: TriggerSource enum (SCHEDULER | MANUAL | API | CHAIN)
  - triggeredByUserId: UUID (NULLABLE, for MANUAL)
  - status: ExecutionStatus enum (QUEUED | RUNNING | SUCCESS | FAILED | TIMED_OUT | SKIPPED | CANCELLED)
  - startedAt: Instant
  - completedAt: Instant (NULLABLE)
  - durationMs: long (NULLABLE)
  - attemptNumber: int (1 = first try, 2+ = retry)
  - output: String (max 50000 chars, truncated if longer)
  - errorMessage: String (NULLABLE)
  - exitCode: int (NULLABLE, for SHELL jobs)
  - httpStatusCode: int (NULLABLE, for HTTP jobs)
  - nextRetryAt: Instant (NULLABLE, set if failed and more retries remain)

Entity: DataSource
  - id: UUID, ownerId, name, type (POSTGRESQL|MYSQL), host, port, database, username, encryptedPassword, ssl: boolean, testOnSave: boolean, createdAt

Entity: ExecutionLog (streaming job output)
  - id: bigserial
  - executionId: UUID (FK)
  - timestamp: Instant
  - level: LogLevel enum (INFO | WARN | ERROR | DEBUG)
  - message: String (max 5000)
  - sequence: int (ordering within execution)

### Quartz Integration
- Store: JDBC JobStore (Quartz manages its own tables in Quartz schema)
- Clustered: true (isClustered=true, clusterCheckinInterval=10000ms)
- Quartz tables created via standard Quartz SQL init script (added to Flyway as V99__quartz_tables.sql)
- ThreadPool: SimpleThreadPool, threadCount=20
- All Quartz jobs extend AbstractJob (which looks up JobDefinition from DB and delegates to type-specific executor)
- Quartz JobDataMap contains: { jobDefinitionId: UUID }

### Job Executor Design
JobExecutor interface:
  ExecutionResult execute(JobDefinition job, JobExecution execution)

Implementations:
  HttpJobExecutor: uses java.net.http.HttpClient (Java 11+ built-in), streams response body to ExecutionLog
  SqlJobExecutor: connects to configured DataSource, executes query, logs result count
  ShellJobExecutor: ProcessBuilder, captures stdout/stderr line-by-line → ExecutionLog, enforces timeout via process.waitFor(timeout, SECONDS)
  ChainJobExecutor: executes each job in sequence, waits for each to complete before triggering next

### API Endpoints (/api/v1)

Auth: standard JWT

Jobs:
  GET    /jobs                     → paginated list (filter: status, type, tags, q)
  POST   /jobs                     → define + schedule job
  GET    /jobs/{id}                → detail with last 5 executions
  PUT    /jobs/{id}                → update definition + reschedule in Quartz
  DELETE /jobs/{id}                → unschedule from Quartz + archive
  POST   /jobs/{id}/pause          → pause Quartz trigger
  POST   /jobs/{id}/resume         → resume Quartz trigger
  POST   /jobs/{id}/trigger        → manual trigger (immediate, async) → returns { executionId }
  POST   /jobs/{id}/trigger/sync   → trigger and wait for result (timeout 60s) → returns ExecutionResult
  GET    /jobs/{id}/next-fire-times → list next 5 scheduled times (cron preview)
  POST   /jobs/{id}/duplicate      → clone job with new name

Executions:
  GET    /jobs/{id}/executions      → paginated execution history (filter: status, dateRange)
  GET    /executions/{id}           → execution detail
  GET    /executions/{id}/logs      → paginated execution logs (filter: level)
  DELETE /executions/{id}           → cancel if QUEUED or RUNNING (sends interrupt to thread)

WebSocket (real-time log streaming):
  WS /ws/executions/{executionId}/logs → streams ExecutionLog entries as they are written
  Client subscribes, server sends { timestamp, level, message, sequence } until execution completes
  Implemented via SimpMessagingTemplate; executor publishes to /topic/execution/{executionId}/logs

DataSources:
  GET    /datasources               → list
  POST   /datasources               → create + test connection { host, port, database, username, password, type }
  GET    /datasources/{id}          → detail (password masked)
  PUT    /datasources/{id}          → update (password optional — only updates if provided)
  DELETE /datasources/{id}          → delete (cannot delete if referenced by active jobs)
  POST   /datasources/{id}/test     → test current connection

Dashboard:
  GET /dashboard/stats
    Response: { totalJobs, activeJobs, pausedJobs, executions24h, successRate24h, failedLast24h, avgDurationMs, nextScheduledJobs: [top 5] }

Admin:
  GET /admin/executions            → all users' executions
  GET /admin/quartz/scheduler-info → thread pool usage, cluster nodes, total jobs in store
  POST /admin/quartz/standby       → pause entire scheduler
  POST /admin/quartz/start         → resume entire scheduler

### Retry Logic
On FAILED execution with attemptNumber < job.maxRetries:
  - Schedule a one-shot Quartz trigger at now() + retryDelay
  - Create new JobExecution with attemptNumber+1, status=QUEUED
  - Original execution status = FAILED

### Timeout Enforcement
AbstractJob wraps executor.execute() in a CompletableFuture with completeOnTimeout:
  If timeout → cancel future, update execution status to TIMED_OUT, send interrupt to executor

### Notification on Failure
On final failure (no more retries): publish JobFailedEvent → NotificationListener → send email via JavaMailSender

### Configuration
app:
  scheduler:
    thread-pool-size: 20
    cluster-check-interval-ms: 10000
  execution:
    log-retention-days: 90
    max-output-size-chars: 50000
  shell:
    allowed-commands: ${ALLOWED_SHELL_COMMANDS:echo,curl,python3,node}  # whitelist
  encryption:
    datasource-password-key: ${DATASOURCE_ENCRYPTION_KEY}

### Tests
- Unit: HttpJobExecutorTest, CronPreviewServiceTest, JobDefinitionValidatorTest
- Integration: JobSchedulingIT (real Quartz + Testcontainers Postgres), HttpJobExecutorIT (WireMock), ShellJobExecutorIT, WebSocketLogStreamingIT

### docker-compose.yml
- app (2 replicas to test Quartz clustering)
- postgres:16-alpine
- redis:7-alpine
```

---

## PROJECT 11 — Distributed Rate Limiter

```
Build a production-grade distributed rate limiting library AND a standalone rate limiting service in Java 21 + Spring Boot 3.3. Two deliverables: (1) a Maven library JAR, (2) a REST service that uses it. Zero comments in any source file.

### Tech Stack
- Spring Boot (service)
- Spring Data Redis (Lettuce client)
- Lua scripting in Redis (atomic operations)
- Spring AOP (for @RateLimited annotation)
- Testcontainers (Redis)
- Micrometer + Prometheus (metrics)

### Algorithms Implemented (all via Redis Lua scripts)
1. Fixed Window Counter    — simple, cheap; bucket resets every window
2. Sliding Window Log      — precise; stores each request timestamp in Redis sorted set
3. Sliding Window Counter  — memory-efficient hybrid approximation
4. Token Bucket            — smooth, allows bursting
5. Leaky Bucket            — strict output rate, no bursting

Each algorithm implements: RateLimitAlgorithm interface
  Result check(RateLimitKey key, RateLimitConfig config)

RateLimitKey: { identifier: String (userId/IP/apiKey), resource: String (endpoint name) }
RateLimitConfig: { limit: int, windowSeconds: int, algorithm: AlgorithmType }

Result: { allowed: boolean, limit: int, remaining: int, resetAt: Instant, retryAfter: Duration? }

### Redis Lua Scripts (one per algorithm, loaded on startup via RedisTemplate.execute(ScriptingCommands))

FixedWindowScript (fixed_window.lua):
  KEYS[1] = rate:{identifier}:{resource}:{windowStart}
  ARGV[1] = limit, ARGV[2] = window TTL seconds
  → INCR KEYS[1]; EXPIRE KEYS[1] ARGV[2]; return {count, limit}

SlidingWindowLogScript (sliding_log.lua):
  KEYS[1] = rate:{identifier}:{resource}
  ARGV[1] = now (epoch ms), ARGV[2] = window ms, ARGV[3] = limit
  → ZREMRANGEBYSCORE KEYS[1] 0 (now-window); count = ZCARD; if count < limit: ZADD now→now; EXPIRE

TokenBucketScript (token_bucket.lua):
  KEYS[1] = bucket:{identifier}:{resource}
  ARGV[1] = capacity, ARGV[2] = refillRate (tokens/sec), ARGV[3] = now (epoch ms), ARGV[4] = requested
  → fetch last tokens + lastRefill; compute new tokens based on elapsed time; check >= requested; update

LeakyBucketScript (leaky_bucket.lua):
  → Tracks last leak time and queue size; drains at fixed rate; accepts if queue not full

SlidingWindowCounterScript (sliding_counter.lua):
  → Uses two fixed-window counters (current + previous) weighted by how far into current window

### Library Module (rate-limiter-core)

Classes:
  RateLimiter (main entry point):
    Result check(RateLimitKey key, RateLimitConfig config)
    Result checkAndConsume(RateLimitKey key, RateLimitConfig config)  // atomic check+decrement
    void reset(RateLimitKey key, RateLimitConfig config)

  RateLimiterRegistry:
    registerPolicy(String name, RateLimitConfig config)
    Result checkByPolicy(String identifier, String policyName)

  @RateLimited annotation (for Spring AOP):
    String policy()       // policy name from registry
    String keyExpression() // SpEL: "#userId", "#request.remoteAddr", "@apiKeyExtractor.extract(#request)"
    AlgorithmType algorithm() default TOKEN_BUCKET
    int limit() default 100
    int windowSeconds() default 60

  RateLimitedAspect (AOP):
    @Around intercepts @RateLimited annotated methods
    Evaluates keyExpression via SpEL
    Calls RateLimiter.checkAndConsume
    On denied: throws RateLimitExceededException with Retry-After info

  RateLimitProperties (@ConfigurationProperties("rate-limiter")):
    Map<String, PolicyConfig> policies  (named policies)
    AlgorithmType defaultAlgorithm
    boolean metricsEnabled

  RateLimiterAutoConfiguration (@ConditionalOnClass(RedisTemplate.class)):
    Beans: RateLimiter, RateLimiterRegistry, RateLimitedAspect, RateLimiterMetrics

  RateLimiterMetrics (Micrometer):
    Counter: rate_limiter_requests_total{policy, result=allowed|denied}
    Histogram: rate_limiter_check_duration_seconds{policy}
    Gauge: rate_limiter_remaining_tokens{policy, identifier}

### Service Module (rate-limiter-service)
Standalone Spring Boot app exposing the library via REST.
Other services call this to check limits (sidecar pattern).

Entities:
  RateLimitPolicy (DB):
    - id: UUID, name (UNIQUE), description, algorithm: AlgorithmType, limit, windowSeconds, burstCapacity (NULLABLE, for token bucket), createdAt, updatedAt, active

  RateLimitViolation (DB):
    - id: UUID, policyName, identifier, resource, violatedAt, requestCount, limit

API Endpoints:

Policies:
  GET    /api/v1/policies           → list all policies
  POST   /api/v1/policies           → create policy
  GET    /api/v1/policies/{name}    → get policy
  PUT    /api/v1/policies/{name}    → update
  DELETE /api/v1/policies/{name}    → delete

Rate Check:
  POST /api/v1/check
    Body: { identifier, resource, policy? (policy name), config? (inline RateLimitConfig) }
    → Either policy name OR inline config must be provided
    Response: { allowed, limit, remaining, resetAt, retryAfterSeconds? }

  POST /api/v1/check/batch
    Body: { checks: [{ identifier, resource, policy }] } (max 100 per batch)
    → Returns array of results, same order

  POST /api/v1/reset
    Body: { identifier, resource, policy }
    → Admin only, resets counters

  POST /api/v1/block
    Body: { identifier, durationSeconds }
    → Admin: explicitly blocks an identifier for a duration (SET block:{identifier} EX durationSeconds)

Metrics:
  GET /api/v1/metrics/violations → paginated violation log (filter: policy, dateRange, identifier)
  GET /api/v1/metrics/top-violators?policy=&limit=20 → top identifiers by violation count

  GET /actuator/prometheus → Prometheus scrape endpoint

Spring Boot Admin integration: /actuator/health includes Redis connectivity + script load status

### Redis Key Design
  rate:fw:{id}:{resource}:{window}   → fixed window counter
  rate:sl:{id}:{resource}            → sliding log sorted set
  rate:sc:{id}:{resource}:{window}   → sliding counter hash
  rate:tb:{id}:{resource}            → token bucket hash {tokens, lastRefill}
  rate:lb:{id}:{resource}            → leaky bucket hash {queue, lastLeak}
  block:{id}                         → string (blocked identifier, TTL = block duration)

Key expiry: always set TTL to prevent stale key accumulation

### Testing
- Unit: Each algorithm tested in isolation with MockRedis (embedded Lettuce)
- Integration: AlgorithmIntegrationTest (Testcontainers Redis) — runs each algorithm through:
  - Allow under limit
  - Deny when limit reached
  - Allow after window reset
  - Concurrent requests test (100 threads, assert exactly {limit} allowed)
- AOP: @RateLimited annotation on test @Service, assert exception thrown on 101st call
- Service: RateLimitControllerIT, BatchCheckIT

### Configuration Example (for library users)
rate-limiter:
  default-algorithm: TOKEN_BUCKET
  metrics-enabled: true
  policies:
    login-api:
      algorithm: SLIDING_WINDOW_LOG
      limit: 5
      window-seconds: 300
    search-api:
      algorithm: TOKEN_BUCKET
      limit: 100
      window-seconds: 60
      burst-capacity: 150

### Maven Structure
pom.xml (parent)
  ├── rate-limiter-core/   (library, no spring-boot-starter-web dep)
  └── rate-limiter-service/ (Spring Boot app)

### docker-compose.yml
- rate-limiter-service
- postgres:16-alpine
- redis:7-alpine (use redis.conf with maxmemory-policy=noeviction)
- prometheus + grafana (pre-configured dashboard for rate limiter metrics)
```

---

## PROJECT 12 — Spring Batch ETL Pipeline

```
Build a production-ready ETL data pipeline platform using Spring Batch in Java 21 + Spring Boot 3.3. Users can upload CSV/JSON files, configure transformation rules, and monitor pipeline executions. Zero comments in source files.

### Tech Stack
- Spring Batch 5.x (aligned with Spring Boot 3.3)
- Spring Web, Spring Data JPA, Spring Security (JWT)
- PostgreSQL (application DB + Spring Batch JobRepository schema)
- Redis (job result cache, progress tracking)
- AWS S3 / MinIO (file storage for raw + processed files)
- Apache Commons CSV (org.apache.commons:commons-csv)
- OpenCSV (for writing)
- Jackson (JSON processing)
- Micrometer + Prometheus
- springdoc-openapi, Testcontainers, Lombok

### ETL Domain

PipelineDefinition:
  - id: UUID
  - name: String
  - ownerId: UUID
  - sourceType: SourceType enum (CSV | JSON | JDBC | S3)
  - destinationType: DestType enum (CSV | JSON | POSTGRESQL | S3)
  - sourceConfig: JSONB (delimiter, encoding, hasHeader, s3Key, jdbcQuery, dataSourceId)
  - destConfig: JSONB (tableName, s3Key, conflictStrategy: INSERT|UPSERT|REPLACE)
  - fieldMappings: JSONB (list of FieldMapping: { sourceField, destField, dataType, transformations: [] })
  - filterRules: JSONB (list of FilterRule: { field, operator: EQ|NEQ|GT|LT|CONTAINS|REGEX, value })
  - chunkSize: int (default 500)
  - skipLimit: int (max skippable errors, default 10)
  - retryLimit: int (per item, default 3)
  - notifyOnComplete: boolean
  - active: boolean
  - createdAt: Instant

Transformations (per-field, applied in order):
  TRIM, UPPERCASE, LOWERCASE, STRIP_SPECIAL_CHARS, PARSE_DATE(format), FORMAT_DATE(inputFormat, outputFormat), REGEX_EXTRACT(pattern, group), REGEX_REPLACE(pattern, replacement), DEFAULT_IF_EMPTY(value), LOOKUP_TABLE(dataSourceId, query), MASK(keepFirstN, keepLastN, maskChar)

FieldMapping also supports: required: boolean, validationRules: []
Validation rules: NOT_NULL, MIN_LENGTH(n), MAX_LENGTH(n), REGEX(pattern), NUMERIC, DATE(format), EMAIL, IN_SET(values)

PipelineRun:
  - id: UUID
  - pipelineId: UUID (FK)
  - springBatchJobInstanceId: long (FK to Spring Batch metadata)
  - springBatchJobExecutionId: long
  - triggerSource: TriggerSource enum (MANUAL | SCHEDULED | FILE_UPLOAD | API)
  - triggeredBy: UUID (NULLABLE)
  - status: RunStatus enum (STARTING | RUNNING | COMPLETED | FAILED | STOPPED | ABANDONED)
  - inputFileKey: String (S3 key of raw file)
  - outputFileKey: String (NULLABLE, S3 key of processed file)
  - startedAt: Instant
  - completedAt: Instant (NULLABLE)
  - readCount: long
  - writeCount: long
  - skipCount: long
  - errorCount: long
  - processRate: double (rows/sec)
  - errorSummary: JSONB (grouped errors: { errorType, count, sampleRows: [first 5] })
  - parameters: JSONB (runtime parameters passed to job)

SkippedRecord:
  - id: UUID, pipelineRunId, lineNumber, rawData (JSONB), errorType, errorMessage, timestamp

DataSourceConfig:
  (Same as Project 10's DataSource — reuse concept: id, name, type, host, port, db, username, encryptedPassword)

### Spring Batch Job Architecture

One Job per pipeline run: "etlPipeline-{pipelineId}-{runId}"

Job Parameters: { pipelineId, runId, inputFileKey, timestamp }

Steps:
  Step 1: validateInputStep (Tasklet)
    - Check file exists in S3
    - Detect and validate encoding
    - Count rows (for progress calculation)
    - Validate header row if CSV
    - On failure: update PipelineRun status, abort

  Step 2: etlStep (Chunk-oriented, chunkSize from PipelineDefinition)
    Reader: DynamicItemReader (delegates to CsvItemReader | JsonItemReader | JdbcCursorItemReader based on sourceType)
      - CsvItemReader: streams S3 object to FlatFileItemReader, uses LineMapper with DelimitedLineTokenizer
      - JsonItemReader: streams and parses JSON array with Jackson streaming API
      - JdbcCursorItemReader: executes JDBC query against configured DataSource

    Processor: PipelineItemProcessor (implements ItemProcessor<Map<String,Object>, Map<String,Object>>)
      - Apply filter rules (skip item if not matching)
      - Apply field mappings (rename fields)
      - Apply transformation functions per field
      - Apply validation rules (throw ValidationException with field details if fails — Batch will skip or fail based on skipLimit)

    Writer: DynamicItemWriter (delegates based on destType)
      - CsvItemWriter: FlatFileItemWriter, writes to temp file in S3
      - JsonItemWriter: Jackson streaming, writes JSON array to S3
      - JdbcBatchItemWriter: bulk INSERT or UPSERT using named parameters
      - S3ItemWriter: batches items as JSONL and uploads in multipart

    Listeners:
      - ProgressTrackingItemReadListener: increments Redis counter on each read; used for % progress
      - SkipListener: persists SkippedRecord on each skip
      - ChunkListener: flushes progress update to DB every 10 chunks
      - StepExecutionListener: updates PipelineRun.readCount, writeCount, skipCount on step complete

  Step 3: postProcessStep (Tasklet)
    - Move output file to final S3 destination
    - Generate summary: errorSummary JSONB
    - Update PipelineRun to COMPLETED
    - If notifyOnComplete: publish PipelineCompleteEvent

JobCompletionNotificationListener (JobExecutionListener):
  - On job completion (success or fail): update PipelineRun status + counts
  - On fail: send email with error summary

### API Endpoints (/api/v1)

Auth: standard JWT

Pipelines:
  GET    /pipelines                    → paginated list
  POST   /pipelines                    → define pipeline
  GET    /pipelines/{id}               → detail with last 5 runs
  PUT    /pipelines/{id}               → update (cannot update while a run is in progress)
  DELETE /pipelines/{id}               → soft delete

  POST   /pipelines/{id}/run           → trigger manual run (immediate)
    Body (optional): { parameters: {} }
    Response: { runId, jobExecutionId, status: STARTING }

  POST   /pipelines/{id}/run-with-file → multipart upload file + trigger
    File uploaded to S3 raw/ prefix first, then job started
    Response: { runId, inputFileKey }

  POST   /pipelines/{id}/preview       → dry run (processes first 100 rows only, no write, returns sample output)
  GET    /pipelines/{id}/validate-config → validates field mappings, transformation syntax, test DB connectivity

Runs:
  GET  /pipelines/{id}/runs            → paginated run history (filter: status, dateRange)
  GET  /runs/{id}                      → run detail
  GET  /runs/{id}/progress             → { percent, readCount, writeCount, skipCount, elapsedMs, estimatedRemainingMs }
  POST /runs/{id}/stop                 → request graceful stop (sets a stop flag, Batch checks between chunks)
  GET  /runs/{id}/skipped-records      → paginated skipped records
  GET  /runs/{id}/output-download-url  → presigned S3 URL for output file (if dest is file)

Schedules:
  GET    /pipelines/{id}/schedule      → get schedule
  PUT    /pipelines/{id}/schedule      → set schedule { cronExpression, timezone, enabled }
  DELETE /pipelines/{id}/schedule      → remove schedule

DataSources:
  GET  /datasources, POST, GET/{id}, PUT/{id}, DELETE/{id}, POST/{id}/test (same as Project 10)

Templates:
  GET  /templates                      → pre-built pipeline templates (CSV→PostgreSQL, JSON→S3, etc.)
  POST /pipelines/from-template/{templateId} → create pipeline from template with user overrides

Admin:
  GET /admin/runs                     → all users' runs
  GET /admin/batch/jobs               → Spring Batch JobRepository summary (requires ADMIN)
  POST /admin/runs/{id}/abandon       → mark run as ABANDONED (for stuck jobs)

### Progress Tracking (Redis)
- Key: progress:{runId} → HSET { readCount, writeCount, skipCount, totalRows, startedAt }
- Updated by ProgressTrackingItemReadListener every 100 items
- GET /runs/{id}/progress reads from Redis (falls back to DB if Redis miss)
- TTL: 24h after completion

### Scheduling
- PipelineScheduleService: manages Quartz triggers per pipeline
  - On PUT /schedule: create/update Quartz CronTrigger
  - Quartz job: PipelineSchedulerJob (Tasklet) → calls PipelineRunService.triggerRun()

### Retry & Skip Configuration (configured per PipelineDefinition)
Job:
  .faultTolerant()
  .skip(ValidationException.class).skipLimit(skipLimit)
  .retry(TransientDataAccessException.class).retryLimit(retryLimit)
  .retryPolicy(exponential backoff: 1s, 2s, 4s)

### Transformation Function Registry
TransformationFunctionRegistry: Map<TransformationType, TransformationFunction>
TransformationFunction: Function<Object, Object> (pure, no side effects)
All transformation functions registered as Spring beans, collected by registry.
LOOKUP_TABLE transformation caches query results for the duration of a step run.

### Tests
- Unit: PipelineItemProcessorTest (each transformation), FilterRuleEvaluatorTest, CsvItemReaderTest
- Integration: CsvToPostgresIT, JsonToS3IT, LargeFileIT (Testcontainers Postgres + MinIO, test with 100k row CSV)
- Skip behavior: SkipOnValidationErrorIT (10% bad rows, assert exactly skipLimit rows skipped, rest written)
- Concurrent runs: ConcurrentPipelineRunIT (2 pipelines simultaneously, assert no data mixing)

### docker-compose.yml
- app
- postgres:16-alpine
- redis:7-alpine
- minio:latest
- prometheus + grafana (batch metrics dashboard: rows/sec, skip rate, job duration)
```

---

## PROJECT 13 — Observability Demo App

```
Build a production-ready Spring Boot 3.3 application that demonstrates comprehensive observability: metrics, distributed tracing, structured logging, and alerting. This is a reference implementation for production-readiness patterns. Zero comments in source files.

### Tech Stack
- Spring Boot 3.3, Spring Web, Spring Data JPA
- Micrometer (metrics) + Prometheus (scraping) + Grafana (dashboards)
- Micrometer Tracing + OpenTelemetry (micrometer-tracing-bridge-otel)
- OpenTelemetry Java Agent (alternative: manual instrumentation)
- OTLP Exporter → Jaeger (distributed traces)
- Loki (log aggregation) + Promtail (log shipping)
- Logback (structured JSON logging via logstash-logback-encoder)
- Spring Security (JWT)
- PostgreSQL + Redis
- Testcontainers
- k6 (load testing scripts included)

### The Demo Application: Bookstore API
A functional bookstore REST API used as the vehicle for demonstrating observability.

Domain: Book (id, title, author, isbn, price, stock), Order (id, userId, items, total, status), Review (id, bookId, userId, rating, comment)
Standard CRUD + checkout flow — simple enough to understand quickly, complex enough to show distributed traces.

### Observability Requirements (ALL must be implemented — this is the point of the project)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. METRICS (Micrometer → Prometheus)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Auto-instrumented (via micrometer + actuator):
  - JVM: heap/non-heap memory, GC pause duration, thread count, class loading
  - HTTP Server: http_server_requests_seconds (histogram by uri, method, status)
  - DB: HikariCP pool (active, idle, pending, max), JDBC query time
  - Spring Batch: (if used in future)

Custom business metrics (all via MeterRegistry):
  Counter:   bookstore_orders_total{status=success|failed|cancelled}
  Counter:   bookstore_inventory_low_stock_events_total{bookId}
  Histogram: bookstore_checkout_duration_seconds (SLO buckets: 0.1, 0.5, 1, 5 seconds)
  Histogram: bookstore_search_duration_seconds
  Gauge:     bookstore_inventory_stock_level{bookId, title} (tracks all books with stock < 20)
  Gauge:     bookstore_active_users (based on Redis session count)
  Summary:   bookstore_order_value_summary (p50, p95, p99 of order totals)

Custom Infrastructure metrics:
  Counter:   bookstore_cache_hits_total{cache=books|inventory}
  Counter:   bookstore_cache_misses_total{cache=books|inventory}
  Timer:     bookstore_external_api_duration_seconds{service=openLibrary} (for ISBN lookup)
  Counter:   bookstore_external_api_errors_total{service, errorType}

MetricsService: centralized bean for recording all business metrics (never call MeterRegistry directly from controllers/services — always via MetricsService)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2. DISTRIBUTED TRACING (OpenTelemetry → Jaeger)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Context propagation: W3C TraceContext (traceparent, tracestate headers)

Auto-instrumented:
  - All Spring MVC requests (span per HTTP request)
  - JDBC queries (span per query via JDBC instrumentation)
  - Redis operations
  - @Async method calls
  - RestTemplate / WebClient outbound calls

Custom spans (via Tracer.nextSpan()):
  - BookService.searchBooks: span with attributes { query.term, result.count, cache.hit }
  - CheckoutService.processOrder: span with attributes { order.id, order.total, item.count }
  - InventoryService.reserveStock: child span per book
  - ExternalApiClient.lookupIsbn: span with attributes { isbn, upstream.status }

Baggage propagation:
  - userId (from JWT) added to baggage on every request → visible in all spans
  - requestId (X-Request-ID header) propagated as baggage

Sampling strategy:
  - Default: 10% head-based sampling (to reduce noise)
  - Always-sample: if X-Force-Sample: true header present (for debugging specific requests)
  - Error sampling: all traces with error spans are always sampled (tail-based via custom sampler)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
3. STRUCTURED LOGGING (JSON → Loki)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Logback configuration (logback-spring.xml):
  Dev profile: human-readable console appender
  Prod profile: JSON appender (logstash-logback-encoder) → stdout → Promtail → Loki

JSON log fields (mandatory on every log line):
  timestamp, level, logger, message, thread,
  traceId, spanId (auto-injected by Micrometer Tracing MDC),
  userId (from MDC, set in JwtAuthFilter),
  requestId (MDC, set in RequestIdFilter),
  environment (from spring.profiles.active),
  service (bookstore-api),
  version (from @project.version@ in pom.xml),
  any additional MDC fields

MDC Management:
  MdcFilter (OncePerRequestFilter, order=-1):
    - Extracts userId from JWT
    - Extracts or generates requestId (X-Request-ID header)
    - Sets MDC: userId, requestId, clientIp, userAgent
    - Clears MDC on response complete

Logging conventions:
  - Controller: log at INFO on entry (method, path, userId) and INFO on exit (status, duration)
  - Service: log at DEBUG for business logic steps, WARN for recoverable issues, ERROR for unexpected
  - Repository: no logging (covered by JDBC trace spans)
  - Sensitive fields NEVER logged: password, token, cardNumber (list enforced via custom SensitiveDataMaskingConverter)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
4. HEALTH CHECKS & READINESS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Spring Actuator endpoints exposed:
  /actuator/health        → liveness + readiness (for k8s)
  /actuator/health/liveness
  /actuator/health/readiness
  /actuator/metrics
  /actuator/prometheus
  /actuator/info
  /actuator/loggers       → runtime log level change
  /actuator/threaddump
  /actuator/heapdump      → ADMIN only (requires actuator API key)

Custom Health Indicators:
  ExternalApiHealthIndicator: calls OpenLibrary API with 2s timeout; reports UP/DOWN/DEGRADED
  DiskSpaceHealthIndicator: checks upload temp dir free space (threshold 500MB)
  CacheHealthIndicator: checks Redis ping; degraded = using local Caffeine fallback

/actuator/info output:
  { app: { name, version, description }, build: { time, artifact, group }, java: { version }, git: { commit, branch, buildTime } }
  (Populated from git.properties via git-commit-id-maven-plugin + build-info from Spring Boot Actuator)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
5. ALERTING RULES (Prometheus → Alertmanager)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Alert rules defined in prometheus/alert-rules.yml (checked into repo):
  - HighErrorRate: http_server_requests error rate > 5% over 5min → CRITICAL
  - SlowRequests: p99 latency > 2s over 10min → WARNING
  - HighMemoryUsage: JVM heap > 85% of max → WARNING
  - CheckoutSLOBreach: bookstore_checkout_duration_seconds p95 > 1s → WARNING
  - LowInventory: bookstore_inventory_stock_level < 5 → WARNING
  - DatabasePoolExhaustion: HikariCP pending connections > 0 for > 30s → CRITICAL
  - ServiceDown: up == 0 → CRITICAL (page immediately)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
6. GRAFANA DASHBOARDS (provisioned as JSON, checked into repo)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
dashboards/service-overview.json:
  - Request rate (RPS), error rate, p50/p95/p99 latency (last 1h)
  - Active users gauge
  - Orders per minute, success vs failed
  - Cache hit/miss ratio donut chart
  - Database pool usage

dashboards/jvm-details.json:
  - Heap + non-heap memory over time
  - GC pause duration (p50/p99)
  - Thread count breakdown
  - Class loading

dashboards/business-metrics.json:
  - Orders total by status (bar chart)
  - Revenue per hour (sum of order totals)
  - Inventory stock levels (table with low-stock highlighted)
  - Checkout duration distribution (heatmap)
  - External API latency + error rate

All dashboards provisioned automatically via grafana/provisioning/ (datasource + dashboard YAML).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
7. LOAD TESTING (k6 scripts)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
load-tests/smoke.js         → 5 VUs, 1 min — basic sanity
load-tests/average.js       → 50 VUs, 10 min — normal load
load-tests/stress.js        → ramp 0→200 VUs over 10 min, hold 5 min, ramp down
load-tests/spike.js         → sudden 500 VU spike for 30s then back to baseline
load-tests/checkout-flow.js → realistic scenario: browse → search → view → add to cart → checkout

Each script exports thresholds:
  http_req_failed: < 1%
  http_req_duration(p95): < 1000ms
  checks: > 99%

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
8. API (Bookstore REST, /api/v1)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Auth: standard JWT register/login/refresh

Books:
  GET  /books                 → search (paginated, full-text)
  GET  /books/{id}            → detail
  POST /books                 → ADMIN create
  PUT  /books/{id}            → ADMIN update
  PATCH /books/{id}/stock     → ADMIN adjust stock

Orders:
  POST /orders/checkout       → body: { items: [{bookId, quantity}] }
  GET  /orders                → user's orders
  GET  /orders/{id}           → order detail
  POST /orders/{id}/cancel    → cancel

Reviews:
  GET  /books/{id}/reviews    → paginated
  POST /books/{id}/reviews    → authenticated user, max 1 per book
  DELETE /reviews/{id}        → own review or ADMIN

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Configuration
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
management:
  endpoints.web.exposure.include: health,info,metrics,prometheus,loggers,threaddump
  tracing:
    sampling.probability: 0.1   # 10% sampling in prod
  otlp:
    metrics.export.url: ${OTEL_EXPORTER_OTLP_METRICS_ENDPOINT:http://localhost:4318/v1/metrics}
    tracing.endpoint: ${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT:http://localhost:4318/v1/traces}

logging:
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"

### Tests
- Unit: MetricsServiceTest (assert counters/timers increment correctly), MdcFilterTest
- Integration: ObservabilityIT (assert spans exported, assert metrics registered, assert MDC fields in log output via captured log appender)
- Load test: Makefile target make load-test-smoke (runs k6 smoke.js against local docker-compose stack)

### docker-compose.yml services (full observability stack)
- app (Spring Boot)
- postgres:16-alpine
- redis:7-alpine
- prometheus:v2.51 (with prometheus.yml + alert-rules.yml volume-mounted)
- alertmanager:v0.27 (alertmanager.yml volume-mounted)
- grafana:10.4 (provisioned dashboards + datasources)
- loki:2.9 (log aggregation)
- promtail:2.9 (ships container logs to Loki)
- jaeger:1.57 (all-in-one image, OTLP receiver on 4317/4318, UI on 16686)
- k6 (grafana/k6, for make load-test-* targets)

Grafana pre-configured datasources (via provisioning):
  - Prometheus (http://prometheus:9090)
  - Loki (http://loki:3100)
  - Jaeger (http://jaeger:16686)
```

---

*End of 13 Project Prompts*
































# ☕ Java Starter Kit — 120 Projects
### GitHub Pages Showcase · Pure Java · Minimal Files · No Build Tools

> **Every project** = plain `.java` files compiled with `javac` and run with `java`.
> **No Maven, no Gradle, no Spring** — just the JDK.
> Each project pairs with an `index.html` page in the GitHub Pages gallery that
> syntax-highlights the source, shows compile/run commands, and links to the README.
> One deliberate exception: Project #99 (Mini ORM) allows the SQLite JDBC JAR.

---

## 📁 GitHub Pages Scaffold

```
repo-root/
├── index.html                         ← Gallery: search/filter all 120 projects
├── assets/
│   ├── gallery.css
│   └── gallery.js                     ← Fetches src/*.java, injects into Prism blocks
├── _projects/
│   ├── bank-account/
│   │   ├── index.html                 ← Project page: badges, description, code viewer
│   │   ├── src/
│   │   │   ├── Main.java
│   │   │   └── BankAccount.java
│   │   └── README.md
│   ├── lru-cache/
│   │   ├── index.html
│   │   ├── src/
│   │   │   ├── Main.java
│   │   │   └── LRUCache.java
│   │   └── README.md
│   └── ...
└── README.md
```

### Per-project `index.html` template
```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>{{PROJECT_NAME}} — Java 120</title>
  <link rel="stylesheet" href="../../assets/gallery.css">
  <link rel="stylesheet"
    href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css">
</head>
<body>
  <nav><a href="../../index.html">← All Projects</a></nav>
  <header>
    <span class="badge java">Java</span>
    <span class="badge {{DIFFICULTY}}">{{DIFFICULTY}}</span>
    <h1>{{PROJECT_NAME}}</h1>
    <p class="tagline">{{ONE_LINE_DESCRIPTION}}</p>
  </header>
  <section class="meta">
    <h2>Core Concepts</h2><p>{{CONCEPTS}}</p>
    <h2>Files</h2><ul>{{FILE_LIST}}</ul>
    <h2>Compile &amp; Run</h2>
    <pre>cd _projects/{{SLUG}}/src
javac *.java
java Main</pre>
  </section>
  <section id="code-viewer">
    <!-- gallery.js fetches each .java file and renders it here -->
  </section>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js"></script>
  <script src="../../assets/gallery.js"></script>
</body>
</html>
```

### File count rule
| Files in `src/` | When to use |
|---|---|
| **1 file** (`Main.java`) | Fully self-contained — ~35% of projects |
| **2 files** | One clearly distinct class (data model, algorithm, or service) |
| **3 files** | Two separable concerns — data + logic + entry point |
| **4 files** | Large pipeline projects (lexer / parser / interpreter / main) |
| **Inner classes** | Prefer inner classes over extra files for small helpers |

---

## 📊 Summary Table — All 120 Java Projects

| # | Project Name | Slug | Category | Difficulty | Tag |
|---|---|---|---|---|---|
| 1 | Simple Bank Account | `bank-account` | OOP | Beginner | |
| 2 | Student Grade Tracker | `grade-tracker` | OOP / Collections | Beginner | |
| 3 | Number Guessing Game | `number-guessing` | CLI / Logic | Beginner | [CLASSIC] |
| 4 | ATM Machine Simulator | `atm-simulator` | OOP / Control Flow | Beginner | |
| 5 | Rock Paper Scissors | `rps-game` | CLI / Random | Beginner | [CLASSIC] |
| 6 | Basic Shape Calculator | `shape-calculator` | OOP / Interfaces | Beginner | |
| 7 | Library Book Tracker | `library-tracker` | Collections / OOP | Beginner | |
| 8 | Simple Inventory System | `inventory-system` | ArrayList / OOP | Beginner | |
| 9 | Caesar Cipher | `caesar-cipher` | String / Math | Beginner | |
| 10 | Roman Numeral Converter | `roman-numerals` | Math / String | Beginner | |
| 11 | Palindrome Checker | `palindrome` | String / Recursion | Beginner | |
| 12 | Fibonacci Generator | `fibonacci` | Iteration / Recursion | Beginner | [CLASSIC] |
| 13 | Prime Number Sieve | `prime-sieve` | Math / Arrays | Beginner | |
| 14 | Matrix Multiplier | `matrix-multiply` | 2D Arrays / Math | Beginner | |
| 15 | Generic Stack | `generic-stack` | Data Structures | Beginner | |
| 16 | Generic Queue (Circular) | `generic-queue` | Data Structures | Beginner | |
| 17 | Custom ArrayList | `custom-arraylist` | Generics / Arrays | Beginner | |
| 18 | Temperature Converter (Enum) | `temp-converter` | OOP / Enum | Beginner | |
| 19 | Employee Payroll System | `payroll` | OOP / Inheritance | Beginner | |
| 20 | Word Frequency Counter | `word-freq` | HashMap / File I/O | Beginner | |
| 21 | Linked List from Scratch | `linked-list` | Data Structures | Intermediate | |
| 22 | Binary Search Tree | `bst` | Data Structures | Intermediate | |
| 23 | Graph (BFS / DFS / Topo) | `graph` | Data Structures | Intermediate | |
| 24 | Dijkstra's Shortest Path | `dijkstra` | Algorithms | Intermediate | |
| 25 | LRU Cache (HashMap + DLL) | `lru-cache` | Data Structures | Intermediate | [CLASSIC] |
| 26 | Trie Autocomplete | `trie` | Data Structures | Intermediate | |
| 27 | Stack-based Calculator | `shunting-yard` | Algorithms / Parsing | Intermediate | |
| 28 | Local TCP Chat Server | `tcp-chat` | Sockets / Threads | Intermediate | [RARE] |
| 29 | Multi-threaded Downloader | `threaded-downloader` | Threads / I/O | Intermediate | [RARE] |
| 30 | Event Bus / Pub-Sub | `event-bus` | Design Patterns | Intermediate | [RARE] |
| 31 | Observer Pattern (Stock) | `observer-pattern` | Design Patterns | Intermediate | |
| 32 | Command Pattern (Undo/Redo) | `command-pattern` | Design Patterns | Intermediate | |
| 33 | Builder Pattern (Query API) | `builder-pattern` | Design Patterns | Intermediate | |
| 34 | Proxy Pattern (Lazy Load) | `proxy-pattern` | Design Patterns | Intermediate | |
| 35 | Strategy Pattern (Sorting) | `strategy-pattern` | Design Patterns | Intermediate | |
| 36 | Iterator Pattern (Tree) | `iterator-pattern` | Design Patterns | Intermediate | [RARE] |
| 37 | Maze Generator & Solver | `maze` | Algorithms | Intermediate | |
| 38 | Tic-Tac-Toe with Minimax | `tictactoe-minimax` | Algorithms / Games | Intermediate | |
| 39 | Memory Allocator Simulator | `memory-allocator` | Arrays / Algorithms | Intermediate | [RARE] |
| 40 | Thread Pool from Scratch | `thread-pool` | Concurrency | Intermediate | [RARE] |
| 41 | Producer-Consumer (blocking) | `producer-consumer` | Concurrency | Intermediate | |
| 42 | Read-Write Lock | `rw-lock` | Concurrency | Intermediate | [RARE] |
| 43 | Semaphore from Scratch | `semaphore` | Concurrency | Intermediate | [RARE] |
| 44 | External Merge Sort | `merge-sort-file` | File I/O / Algorithms | Intermediate | |
| 45 | CSV Parser & Reporter | `csv-reporter` | File I/O / Parsing | Intermediate | |
| 46 | JSON Serializer (no libs) | `json-serializer` | Parsing / Reflection | Intermediate | [RARE] |
| 47 | Huffman Encoder / Decoder | `huffman` | Algorithms / Trees | Intermediate | |
| 48 | Heap Implementation | `heap` | Data Structures | Intermediate | |
| 49 | AVL Tree | `avl-tree` | Data Structures | Intermediate | [CLASSIC] |
| 50 | Red-Black Tree | `red-black-tree` | Data Structures | Advanced | [RARE] |
| 51 | B-Tree | `btree` | Data Structures | Advanced | [RARE] |
| 52 | Skip List | `skip-list` | Data Structures | Intermediate | [RARE] |
| 53 | Bloom Filter | `bloom-filter` | Data Structures | Intermediate | [RARE] |
| 54 | LFU Cache | `lfu-cache` | Data Structures | Intermediate | [RARE] |
| 55 | Interval Tree | `interval-tree` | Data Structures | Intermediate | [RARE] |
| 56 | Segment Tree | `segment-tree` | Data Structures | Intermediate | [RARE] |
| 57 | Fenwick Tree (BIT) | `fenwick-tree` | Data Structures | Intermediate | [RARE] |
| 58 | Disjoint Set / Union-Find | `union-find` | Data Structures | Intermediate | |
| 59 | Kruskal's MST | `kruskals-mst` | Algorithms | Intermediate | |
| 60 | A* Pathfinding | `astar` | Algorithms | Intermediate | |
| 61 | Knuth-Morris-Pratt (KMP) | `kmp-search` | String Algorithms | Intermediate | [RARE] |
| 62 | Rabin-Karp Rolling Hash | `rabin-karp` | String Algorithms | Intermediate | [RARE] |
| 63 | Levenshtein Distance | `levenshtein` | Dynamic Programming | Intermediate | |
| 64 | Longest Common Subsequence | `lcs` | Dynamic Programming | Intermediate | |
| 65 | 0-1 Knapsack | `knapsack` | Dynamic Programming | Intermediate | |
| 66 | Matrix Chain Multiplication | `matrix-chain` | Dynamic Programming | Intermediate | [RARE] |
| 67 | Mini HTTP Server | `mini-http` | Networking | Intermediate | |
| 68 | Simple FTP Client | `ftp-client` | Networking | Intermediate | [RARE] |
| 69 | SMTP Client (raw socket) | `smtp-client` | Networking | Intermediate | [RARE] |
| 70 | Port Scanner | `port-scanner` | Networking | Intermediate | |
| 71 | Custom Exception Framework | `exception-framework` | OOP / Design | Intermediate | |
| 72 | Dependency Injector | `dep-injector` | Reflection | Advanced | [RARE] |
| 73 | Object Pool | `object-pool` | Design Patterns | Intermediate | [RARE] |
| 74 | Circuit Breaker | `circuit-breaker` | Design Patterns | Advanced | [RARE] |
| 75 | Rate Limiter (Token Bucket) | `rate-limiter` | Concurrency | Intermediate | [RARE] |
| 76 | Lock-Free Stack (CAS) | `lock-free-stack` | Concurrency | Advanced | [RARE] |
| 77 | Lock-Free Queue (M-S algo) | `lock-free-queue` | Concurrency | Advanced | [RARE] |
| 78 | Reactive Streams (mini) | `reactive-streams` | Concurrency | Advanced | [RARE] |
| 79 | Actor Model (mini) | `actor-model` | Concurrency | Advanced | [RARE] |
| 80 | Distributed KV Store | `dist-kv` | Networking / Distributed | Advanced | [RARE] |
| 81 | Raft Consensus (mini) | `raft` | Distributed Systems | Advanced | [RARE] |
| 82 | LSM-Tree Storage Engine | `lsm-tree` | Storage / Algorithms | Advanced | [RARE] |
| 83 | JVM Bytecode Reader | `jvm-reader` | JVM Internals | Advanced | [RARE] |
| 84 | Class File Disassembler | `class-dis` | JVM Internals | Advanced | [RARE] |
| 85 | Interpreter (tiny language) | `interpreter` | Compilers / PL | Advanced | [RARE] |
| 86 | Compiler (expr lang) | `mini-compiler` | Compilers | Advanced | [RARE] |
| 87 | RSA from Scratch | `rsa` | Cryptography | Advanced | [RARE] |
| 88 | AES Implementation | `aes` | Cryptography | Advanced | [RARE] |
| 89 | Consistent Hashing Ring | `consistent-hash` | Distributed Systems | Advanced | [RARE] |
| 90 | Ray Tracer (PPM output) | `ray-tracer` | Graphics / Math | Advanced | [RARE] |
| 91 | Conway's Game of Life | `game-of-life` | Simulation | Intermediate | [CLASSIC] |
| 92 | Sorting Visualizer (console) | `sort-visualizer` | Algorithms / console | Intermediate | [RARE] |
| 93 | N-Queens Solver | `n-queens` | Algorithms / Backtracking | Intermediate | |
| 94 | Sudoku Solver | `sudoku-solver` | Algorithms / Backtracking | Intermediate | |
| 95 | Blackjack Game | `blackjack` | Games / OOP | Intermediate | [CLASSIC] |
| 96 | Wordle Clone (console) | `wordle` | Games / String | Intermediate | |
| 97 | Battleship Game | `battleship` | Games / OOP | Intermediate | |
| 98 | Text-Based RPG | `text-rpg` | OOP / State | Intermediate | |
| 99 | Mini ORM (JDBC + SQLite) | `mini-orm` | Database / Reflection | Advanced | [RARE] |
| 100 | WebSocket Server (raw) | `websocket-server` | Networking | Advanced | [RARE] |
| 101 | Plugin System (ClassLoader) | `plugin-system` | Reflection | Advanced | [RARE] |
| 102 | Event Sourcing Engine | `event-sourcing` | Design Patterns | Advanced | [RARE] |
| 103 | CQRS Framework (mini) | `cqrs` | Design Patterns | Advanced | [RARE] |
| 104 | Fibonacci via Matrix Exp. | `matrix-exp-fib` | Math / Algorithms | Intermediate | [RARE] |
| 105 | Fast Fourier Transform | `fft` | Math / Algorithms | Advanced | [RARE] |
| 106 | Quadtree | `quadtree` | Spatial Data Structures | Intermediate | [RARE] |
| 107 | K-D Tree | `kd-tree` | Spatial Data Structures | Advanced | [RARE] |
| 108 | Steganography (LSB in BMP) | `steganography` | Cryptography | Advanced | [RARE] |
| 109 | Arithmetic Coding | `arithmetic-coding` | Compression | Advanced | [RARE] |
| 110 | Genetic Algorithm (TSP) | `genetic-algo` | Algorithms / AI | Advanced | [RARE] |
| 111 | Simulated Annealing | `sim-annealing` | Algorithms / Optimization | Advanced | [RARE] |
| 112 | Neural Network (no libs) | `neural-net` | AI / Math | Advanced | [RARE] |
| 113 | Monte Carlo Simulator | `monte-carlo` | Math / Probability | Intermediate | |
| 114 | Finite State Machine | `fsm` | Design Patterns | Intermediate | |
| 115 | CPU Scheduler Simulator | `cpu-scheduler` | OS Concepts | Intermediate | [RARE] |
| 116 | Virtual Memory Simulator | `virtual-memory` | OS Concepts | Advanced | [RARE] |
| 117 | Regex Engine (NFA) | `regex-engine` | Parsing / Automata | Advanced | [RARE] |
| 118 | Async HTTP Client (NIO) | `async-http` | Concurrency / NIO | Advanced | [RARE] |
| 119 | Merkle Tree | `merkle-tree` | Cryptography / Trees | Intermediate | [RARE] |
| 120 | P2P File Sharing | `p2p-sharing` | Networking | Advanced | [RARE] |

---

## 🟢 BEGINNER PROJECTS (1–20)

---

### 1. Simple Bank Account
**Slug:** `bank-account`
**Files:**
```
src/
├── Main.java
└── BankAccount.java
```
**Description:** OOP bank account with deposit, withdraw, transfer between accounts, full transaction history (type, amount, timestamp, running balance), and a custom `InsufficientFundsException` as a checked exception.
**Core Concepts:** Encapsulation, `ArrayList<Transaction>`, custom checked exceptions, `LocalDateTime`, `toString()` override
**Difficulty:** Beginner
**Unique Challenge:** Designing `InsufficientFundsException` as a *checked* exception (extends `Exception`) with the attempted amount and current balance in the message — forces callers to handle it at compile time.
**Compile & Run:**
```bash
cd _projects/bank-account/src
javac *.java
java Main
```

---

### 2. Student Grade Tracker
**Slug:** `grade-tracker`
**Files:**
```
src/
├── Main.java
└── Student.java
```
**Description:** Track a roster of students, each with per-subject grades and credit hours. Compute weighted GPA, assign letter grades, rank the class, and print a sorted report with column-aligned output.
**Core Concepts:** `ArrayList`, `HashMap<String, Double>`, `Comparator.thenComparing`, OOP, `String.format`
**Difficulty:** Beginner
**Unique Challenge:** Computing weighted GPA correctly — you cannot average per-subject averages; you must sum `(grade × credits)` across all subjects and divide by total credit hours, accounting for each subject's weight separately.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 3. Number Guessing Game [CLASSIC]
**Slug:** `number-guessing`
**Files:**
```
src/
└── Main.java
```
**Description:** Guess a secret random number in a configurable range with higher/lower hints. Tracks number of guesses per round, best score (fewest guesses) across the session, and offers a replay loop.
**Core Concepts:** `Random`, `Scanner`, `while` loop, conditional logic, session-scoped variable tracking
**Difficulty:** Beginner
**Unique Challenge:** Tracking the best score across multiple in-session games without any persistence — initialize `bestScore = Integer.MAX_VALUE` and update it only when a round completes successfully, not on quit.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main
```

---

### 4. ATM Machine Simulator
**Slug:** `atm-simulator`
**Files:**
```
src/
├── Main.java
├── Account.java
└── ATM.java
```
**Description:** Simulates an ATM — PIN authentication with lockout after 3 failed attempts, balance inquiry, deposit, withdrawal with denomination breakdown, mini-statement, and a simulated receipt printout.
**Core Concepts:** OOP, `switch` statement, input validation, state machine (locked/authenticated/idle)
**Difficulty:** Beginner
**Unique Challenge:** Implementing the PIN lockout correctly — once locked, the correct PIN must still be rejected until an admin unlocks the account. The locked state must persist across menu choices, not just the current input loop.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 5. Rock Paper Scissors [CLASSIC]
**Slug:** `rps-game`
**Files:**
```
src/
└── Main.java
```
**Description:** Rock Paper Scissors with a sneaky adaptive AI that biases its next pick toward the move that would beat the player's most frequently chosen move. Tracks wins/losses/ties over a configurable number of rounds.
**Core Concepts:** `Random`, `enum` with `beats()` method, `switch` expression (Java 14+), `HashMap<Move, Integer>` for frequency
**Difficulty:** Beginner
**Unique Challenge:** Implementing the adaptive AI using a `HashMap` of move frequencies — after each round, find the player's most common move, then return the move that beats it. Tie-break by choosing randomly among equally frequent moves.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main
```

---

### 6. Basic Shape Calculator
**Slug:** `shape-calculator`
**Files:**
```
src/
├── Main.java
└── Shape.java
```
**Description:** Abstract `Shape` hierarchy — `Circle`, `Rectangle`, `Triangle`, `RegularPolygon` — each implementing `area()` and `perimeter()`. Sort shapes by area, find the largest, and compare perimeter/area ratios.
**Core Concepts:** Abstract classes, interfaces (`Comparable<Shape>`), polymorphism, `List.sort`, `instanceof` pattern matching (Java 16+)
**Difficulty:** Beginner
**Unique Challenge:** Using Java 16+ `instanceof` pattern matching — `if (shape instanceof Circle c) { ... c.getRadius() ... }` — to cleanly dispatch on shape type without explicit casting.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 7. Library Book Tracker
**Slug:** `library-tracker`
**Files:**
```
src/
├── Main.java
├── Book.java
└── Library.java
```
**Description:** Catalog books with title, author, ISBN, genre, and copies available. Check out and return books, search by any field, flag overdue checkouts (using `LocalDate`), and generate a summary report.
**Core Concepts:** `ArrayList`, `HashMap`, `LocalDate`, OOP, multi-field search, `Comparator`
**Difficulty:** Beginner
**Unique Challenge:** Implementing multi-field search with a single query string — search title, author, and ISBN simultaneously using `String.contains(query.toLowerCase())` on each field, returning the union of all matches deduplicated by ISBN.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 8. Simple Inventory System
**Slug:** `inventory-system`
**Files:**
```
src/
├── Main.java
├── Product.java
└── Inventory.java
```
**Description:** Manage products with name, SKU, category, price, and stock quantity. Add, remove, restock, search, filter by category, flag low-stock items, and print a sorted report.
**Core Concepts:** `ArrayList`, `HashMap<String, Product>` by SKU, `Comparator.comparing().thenComparing()`, OOP
**Difficulty:** Beginner
**Unique Challenge:** Implementing a composite sort (category → stock level ascending → name) using `Comparator.comparing(Product::getCategory).thenComparingInt(Product::getStock).thenComparing(Product::getName)` as a single fluent chain.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 9. Caesar Cipher
**Slug:** `caesar-cipher`
**Files:**
```
src/
└── Main.java
```
**Description:** Encrypt and decrypt text with a Caesar shift cipher. Preserves case, keeps non-alpha characters unchanged, supports brute-force decryption (all 25 shifts), and batch-encrypts lines from a file.
**Core Concepts:** `char` arithmetic, modular arithmetic, `StringBuilder`, `args[]` parsing, file I/O with `BufferedReader`
**Difficulty:** Beginner
**Unique Challenge:** Handling both uppercase and lowercase independently with correct wraparound — `(char)((c - 'A' + shift) % 26 + 'A')` — and not accidentally shifting lowercase with the uppercase base or vice versa.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main encrypt "Hello World" 13
java -cp src Main brute "Uryyb Jbeyq"
```

---

### 10. Roman Numeral Converter
**Slug:** `roman-numerals`
**Files:**
```
src/
└── Main.java
```
**Description:** Convert integers (1–3999) to Roman numerals and Roman numeral strings back to integers. Validate Roman numeral strings, support batch conversion from a file, and explain each conversion step.
**Core Concepts:** `int[][]` value-symbol pairs, greedy subtraction, `String` parsing, validation via round-trip, `BufferedReader`
**Difficulty:** Beginner
**Unique Challenge:** Validating Roman numeral strings without regex — convert the string to an integer, convert that integer back to Roman, and assert the two strings match. Invalid inputs produce a different round-trip result.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main 2024
java -cp src Main MMXXIV
```

---

### 11. Palindrome Checker
**Slug:** `palindrome`
**Files:**
```
src/
└── Main.java
```
**Description:** Check strings, numbers, and phrases (ignoring spaces, punctuation, and case) for palindrome property. Find the longest palindromic substring using Manacher's O(n) algorithm. Benchmark both approaches.
**Core Concepts:** `StringBuilder.reverse()`, `Character.isLetterOrDigit`, two-pointer check, Manacher's algorithm with separator transform
**Difficulty:** Beginner
**Unique Challenge:** Implementing Manacher's algorithm in Java — insert `#` separators to unify odd/even-length palindromes, build the `P[]` radius array using the mirror property and the rightmost boundary `R`, keeping the entire algorithm O(n).
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main "A man a plan a canal Panama"
java -cp src Main --longest "bananas"
```

---

### 12. Fibonacci Generator [CLASSIC]
**Slug:** `fibonacci`
**Files:**
```
src/
└── Main.java
```
**Description:** Generate Fibonacci numbers via iterative, recursive, memoized, and matrix exponentiation approaches. Compare performance benchmarks. Use `BigInteger` for arbitrary-precision results beyond `long` range.
**Core Concepts:** `BigInteger`, `HashMap` memoization, 2×2 matrix multiply, `System.nanoTime` benchmarking, recursion
**Difficulty:** Beginner
**Unique Challenge:** Implementing O(log n) matrix exponentiation — represent Fibonacci recurrence as `[[1,1],[1,0]]^n`, implement matrix multiply for 2×2 `BigInteger` arrays, then apply fast-power (repeated squaring) to get Fib(n) in O(log n) multiplications.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main 1000
java -cp src Main --benchmark 50
```

---

### 13. Prime Number Sieve
**Slug:** `prime-sieve`
**Files:**
```
src/
└── Main.java
```
**Description:** Find all primes up to N using the Sieve of Eratosthenes, and primes in a large range (up to 10^9) using the segmented sieve. Also implement trial division for primality testing and full prime factorization.
**Core Concepts:** `boolean[]` sieve, segmented sieve with pre-computed small primes, `ArrayList<Integer>`, loop optimization
**Difficulty:** Beginner
**Unique Challenge:** Implementing the segmented sieve — pre-sieve small primes up to √N, then sieve fixed-size segments of the full range using those small primes. This avoids allocating a 10^9-element array while still finding all primes in the range.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main --sieve 1000000
java -cp src Main --segmented 1000000000
java -cp src Main --factorize 360
```

---

### 14. Matrix Multiplier
**Slug:** `matrix-multiply`
**Files:**
```
src/
└── Main.java
```
**Description:** Matrix addition, multiplication, transpose, determinant (via Gaussian elimination with partial pivoting), inverse, and power. Reads matrices from stdin and prints formatted results with aligned columns.
**Core Concepts:** 2D `double[][]` arrays, Gaussian elimination, `printf` formatting, LU decomposition concept
**Difficulty:** Beginner
**Unique Challenge:** Implementing Gaussian elimination with partial pivoting — for each column, find the row with the largest absolute value and swap it to the pivot position before eliminating. This avoids numerical blow-up from small pivots.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main
```

---

### 15. Generic Stack
**Slug:** `generic-stack`
**Files:**
```
src/
├── Main.java
└── Stack.java
```
**Description:** A generic stack backed by a dynamically resized array — `push`, `pop`, `peek`, `isEmpty`, `size`, `clear`, and an `iterator()`. Also demonstrates the stack's use for balanced-parentheses checking and infix-to-postfix conversion.
**Core Concepts:** Java generics (`<T>`), `@SuppressWarnings("unchecked")` for generic array, dynamic resizing (double on full, halve on quarter-full), `Iterable<T>`
**Difficulty:** Beginner
**Unique Challenge:** Creating a generic array in Java requires `(T[]) new Object[capacity]` plus `@SuppressWarnings("unchecked")` — the cast is safe because the only things stored are `T` instances, but the compiler can't verify that without the annotation.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 16. Generic Queue (Circular Buffer)
**Slug:** `generic-queue`
**Files:**
```
src/
├── Main.java
└── Queue.java
```
**Description:** Generic circular-buffer queue with `enqueue`, `dequeue`, `peek`, `isFull`, `isEmpty`, and dynamic resizing when full. A priority queue variant uses `Comparable` for ordering. Demonstrates the queue in a BFS implementation.
**Core Concepts:** Generics, circular array (`head`/`tail` pointers mod capacity), `Comparable<T>`, `Iterable<T>`
**Difficulty:** Beginner
**Unique Challenge:** Distinguishing full from empty in a circular buffer — both states have `head == tail`. Use a separate `size` counter rather than `(tail + 1) % capacity == head` so the buffer can use its full capacity without wasting one slot.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 17. Custom ArrayList
**Slug:** `custom-arraylist`
**Files:**
```
src/
├── Main.java
└── MyArrayList.java
```
**Description:** Generic dynamic array matching the `java.util.ArrayList` API — `add`, `remove(index)`, `remove(Object)`, `get`, `set`, `contains`, `indexOf`, `sort`, and `iterator`. Includes a `modCount` field that triggers `ConcurrentModificationException` in the iterator.
**Core Concepts:** Generics, dynamic array resizing, `Comparable`, `Iterator` as inner class, `modCount` for structural change detection
**Difficulty:** Beginner
**Unique Challenge:** Implementing `ConcurrentModificationException` — the inner `Iterator` captures `modCount` at construction time; each `next()` call checks if `MyArrayList.this.modCount != expectedModCount` and throws if the list was structurally modified during iteration.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 18. Temperature Converter (Enum-driven)
**Slug:** `temp-converter`
**Files:**
```
src/
└── Main.java
```
**Description:** Convert between Celsius, Fahrenheit, Kelvin, and Rankine using a `TemperatureUnit` enum where each constant implements an abstract `toKelvin()` method. All conversions go through Kelvin as the canonical unit.
**Core Concepts:** `enum` with abstract methods, constant-specific class bodies, `switch` expression, `EnumSet`, `Arrays.stream`
**Difficulty:** Beginner
**Unique Challenge:** Putting conversion logic *inside* the enum by declaring `abstract double toKelvin(double value)` at the enum level and overriding it in each constant's body — making the converter table-driven and extensible without any `if/switch` in the conversion method.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main 100 C F
java -cp src Main 373.15 K C
```

---

### 19. Employee Payroll System
**Slug:** `payroll`
**Files:**
```
src/
├── Main.java
└── Employee.java
```
**Description:** Abstract `Employee` hierarchy with `FullTimeEmployee`, `PartTimeEmployee`, and `Contractor`. Each overrides `calculatePay()` polymorphically. Generate a formatted payroll report sorted by department then pay.
**Core Concepts:** Abstract classes, polymorphism, `List<Employee>`, `Comparator`, `String.format` for alignment
**Difficulty:** Beginner
**Unique Challenge:** Using a sealed class hierarchy (Java 17+) — `sealed abstract class Employee permits FullTimeEmployee, PartTimeEmployee, Contractor` — so a `switch` expression over employee types is exhaustive at compile time without a `default` branch.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 20. Word Frequency Counter
**Slug:** `word-freq`
**Files:**
```
src/
└── Main.java
```
**Description:** Read a text file, tokenize words (strip punctuation, lowercase), count frequencies using a `HashMap`, load a stop-word set from a second file, and print the top-N most frequent non-stop words as a formatted ranked table.
**Core Concepts:** `HashMap<String, Integer>`, `BufferedReader`, `String.split` + `replaceAll`, `List.sort` with `Map.Entry` comparator
**Difficulty:** Beginner
**Unique Challenge:** Sorting `Map.Entry<String, Integer>` by value descending, then by key ascending for a stable tie-breaker — `Map.Entry.comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey())`.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main book.txt stopwords.txt 20
```

---



## 🟡 INTERMEDIATE PROJECTS (21–71)

---

### 21. Linked List from Scratch
**Slug:** `linked-list`
**Files:**
```
src/
├── Main.java
├── LinkedList.java
└── Node.java
```
**Description:** Generic doubly-linked list implementing `Iterable<T>` with a full `ListIterator`. Operations: `addFirst`, `addLast`, `addAt(index)`, `removeFirst`, `removeLast`, `removeAt(index)`, `get`, `contains`, `reverse`, and in-place merge sort.
**Core Concepts:** Generics, doubly-linked nodes, `ListIterator<T>`, `Iterable<T>`, in-place merge sort on linked nodes
**Difficulty:** Intermediate
**Unique Challenge:** Implementing `ListIterator.previous()` correctly — you need to track both `nextNode` (the node `next()` will return) and `lastReturned` (the node most recently returned by either `next()` or `previous()`), and update both on every call.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 22. Binary Search Tree
**Slug:** `bst`
**Files:**
```
src/
├── Main.java
└── BST.java
```
**Description:** Generic BST with insert, search, delete, min, max, floor, ceiling, rank, and all four traversal orders (in-order, pre-order, post-order, level-order). Includes an iterator for in-order traversal without recursion.
**Core Concepts:** Recursive BST, generics, `Comparable<K>`, `Queue<Node>` for BFS, stack-based in-order iterator
**Difficulty:** Intermediate
**Unique Challenge:** Correctly deleting a node with two children using Hibbard deletion — find the in-order successor (smallest node in the right subtree), copy its key to the node being deleted, then delete the successor from the right subtree.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 23. Graph (BFS / DFS / Topological Sort)
**Slug:** `graph`
**Files:**
```
src/
├── Main.java
└── Graph.java
```
**Description:** Adjacency-list directed and undirected graph supporting BFS (shortest path, level-order), DFS (pre/post-order, finish times), cycle detection, topological sort (Kahn's algorithm and DFS-based), connected components, and strongly connected components (Kosaraju's).
**Core Concepts:** `HashMap<Integer, List<Integer>>`, `ArrayDeque` as stack/queue, Kahn's algorithm, Kosaraju's two-pass DFS
**Difficulty:** Intermediate
**Unique Challenge:** Implementing Kosaraju's SCC algorithm — run DFS on the original graph, push nodes to a stack by finish time; then run DFS on the reversed graph in reverse finish-time order. Each DFS tree in the second pass is one SCC.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 24. Dijkstra's Shortest Path
**Slug:** `dijkstra`
**Files:**
```
src/
├── Main.java
├── Graph.java
└── Dijkstra.java
```
**Description:** Weighted directed graph with Dijkstra's shortest path (using a `PriorityQueue`), full path reconstruction via a `parent` map, negative-cycle detection (Bellman-Ford fallback), and an `all-pairs` mode.
**Core Concepts:** `PriorityQueue<int[]>` with custom comparator, `HashMap` for distances, parent-map path reconstruction, Bellman-Ford for negative edges
**Difficulty:** Intermediate
**Unique Challenge:** Reconstructing the full path from source to destination — after Dijkstra fills the `parent` map, trace from destination back to source using `parent.get(node)` and reverse the resulting list with `Collections.reverse`.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --source 0 --dest 5
```

---

### 25. LRU Cache (HashMap + DLL) [CLASSIC]
**Slug:** `lru-cache`
**Files:**
```
src/
├── Main.java
└── LRUCache.java
```
**Description:** O(1) get and put LRU cache using a `HashMap` backed by a custom doubly-linked list. Generic key-value pair. Configurable capacity. Cache statistics (hit rate, eviction count). Thread-safe variant using `ReentrantLock`.
**Core Concepts:** `HashMap<K, Node<K,V>>`, doubly-linked list with sentinel head/tail nodes, generics, `ReentrantLock`
**Difficulty:** Intermediate
**Unique Challenge:** Keeping the `HashMap` and the doubly-linked list perfectly synchronized on every operation — a `get` must move the accessed node to the head of the DLL *and* keep its `HashMap` entry pointing to the same node object. Any pointer mistake causes incorrect eviction.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 26. Trie Autocomplete
**Slug:** `trie`
**Files:**
```
src/
├── Main.java
└── Trie.java
```
**Description:** Prefix trie with `insert`, `search`, `startsWith`, `delete`, and `topKCompletions(prefix, k)` returning the k most frequent completions found via DFS. Each word stores its insertion frequency for ranking.
**Core Concepts:** `Map<Character, TrieNode>`, recursive DFS, `PriorityQueue` for top-k by frequency, frequency weighting
**Difficulty:** Intermediate
**Unique Challenge:** Returning top-k completions by frequency — use a bounded min-heap of size k during DFS. If the heap has k items and the current word's frequency exceeds the heap minimum, evict the minimum and add the current word. This avoids collecting all completions first.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 27. Stack-based Calculator (Shunting-Yard)
**Slug:** `shunting-yard`
**Files:**
```
src/
├── Main.java
└── Calculator.java
```
**Description:** Evaluate arbitrary infix math expressions — tokenize, convert to Reverse Polish Notation using the Shunting-Yard algorithm, then evaluate the RPN with a stack. Supports `+`, `-`, `*`, `/`, `^` (right-associative), unary minus, and parentheses.
**Core Concepts:** `ArrayDeque<String>` as stack, operator precedence table, associativity (left/right), tokenizer for multi-digit numbers
**Difficulty:** Intermediate
**Unique Challenge:** Detecting unary minus during tokenization — a `-` is unary if it follows an operator, a left parenthesis, or is the first token. Handling this correctly without a separate unary-minus token requires inserting a `0` before it or tagging it explicitly.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main "(3 + 4) * -2"
java -cp src Main "2^10"
```

---

### 28. Local TCP Chat Server [RARE]
**Slug:** `tcp-chat`
**Files:**
```
src/
├── Main.java
├── Server.java
└── Client.java
```
**Description:** Multi-client text chat over TCP. The server spawns a thread per client, maintains a `CopyOnWriteArrayList` of connected clients, and broadcasts every message. Clients support `/nick <name>`, `/quit`, `/whisper <user> <msg>`, and `/list` commands.
**Core Concepts:** `ServerSocket`, `Socket`, `Thread`, `PrintWriter`/`BufferedReader`, `CopyOnWriteArrayList`, command parsing
**Difficulty:** Intermediate
**Unique Challenge:** Using `CopyOnWriteArrayList` for the client handler list — broadcasting (reading the list) happens on multiple threads simultaneously; client disconnect (writing the list) can happen at any time. `CopyOnWriteArrayList` eliminates `ConcurrentModificationException` at the cost of slightly stale reads.
**Compile & Run:**
```bash
javac src/*.java
java -cp src Server 9999 &
java -cp src Client localhost 9999
```

---

### 29. Multi-threaded File Downloader [RARE]
**Slug:** `threaded-downloader`
**Files:**
```
src/
├── Main.java
└── Downloader.java
```
**Description:** Download a large file by splitting it into N equal byte-range chunks, fetching each chunk in a separate thread using HTTP `Range` headers, and writing each chunk to the correct offset of a pre-allocated `RandomAccessFile`. Reassembly is implicit.
**Core Concepts:** `Thread`, `HttpURLConnection` with `Range` header, `RandomAccessFile`, `CountDownLatch`, byte-offset calculation
**Difficulty:** Intermediate
**Unique Challenge:** Writing chunks to their exact byte offset in a `RandomAccessFile` simultaneously — each thread calls `file.seek(chunkStart)` then `file.write(buffer)`. Since threads operate on non-overlapping ranges, no synchronization is needed for the writes themselves, only for the `CountDownLatch`.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main https://example.com/bigfile.zip 8
```

---

### 30. Event Bus / Pub-Sub [RARE]
**Slug:** `event-bus`
**Files:**
```
src/
├── Main.java
└── EventBus.java
```
**Description:** Type-safe in-process publish-subscribe system. Subscribers register by event class: `bus.subscribe(UserEvent.class, handler)`. Publishing dispatches to all matching handlers synchronously or asynchronously via an `ExecutorService`. Supports once-only subscriptions.
**Core Concepts:** `ConcurrentHashMap<Class<?>, List<Consumer<?>>>`, generics, `FunctionalInterface`, `ExecutorService`, type-safe dispatch
**Difficulty:** Intermediate
**Unique Challenge:** Achieving type-safe subscribe/publish without unchecked casts at call sites — use `Class<T>` as the key and `Consumer<T>` as the handler; the `subscribe(Class<T>, Consumer<T>)` signature ensures the handler type matches the event class at compile time.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 31. Observer Pattern (Stock Ticker)
**Slug:** `observer-pattern`
**Files:**
```
src/
├── Main.java
├── Subject.java
└── Observer.java
```
**Description:** GoF Observer applied to a stock price feed — `StockMarket` is the subject, multiple `Display` panels are observers. When price changes, all observers update automatically. Uses `WeakReference<Observer>` to prevent memory leaks.
**Core Concepts:** `interface Observer`, `interface Subject`, `ArrayList`, `WeakReference<T>`, push vs. pull model comparison
**Difficulty:** Intermediate
**Unique Challenge:** Using `WeakReference<Observer>` in the subject's observer list — when an observer is GC'd (e.g., a display panel is closed), it's silently removed from the list during the next notification cycle rather than leaking memory indefinitely.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 32. Command Pattern (Undo / Redo)
**Slug:** `command-pattern`
**Files:**
```
src/
├── Main.java
├── Command.java
└── Editor.java
```
**Description:** Text editor with full undo/redo using the Command pattern. Commands — `InsertCommand`, `DeleteCommand`, `ReplaceCommand`, `PasteCommand` — each implement `execute()` and `undo()`. History stored in two `ArrayDeque` stacks.
**Core Concepts:** `interface Command` with `execute`/`undo`, `ArrayDeque<Command>` undo/redo stacks, immutable command snapshots
**Difficulty:** Intermediate
**Unique Challenge:** Branching undo history — after `undo()`, if the user types a new character, the redo stack must be *cleared* entirely (not branched). This matches real editor behavior: you can't redo after introducing new input.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 33. Builder Pattern (Fluent Query API)
**Slug:** `builder-pattern`
**Files:**
```
src/
├── Main.java
└── QueryBuilder.java
```
**Description:** SQL query builder using a fluent inner `Builder` — `QueryBuilder.select("name","age").from("users").where("age > 18").orderBy("name").limit(10).build()`. Each method returns the builder for chaining. The final `Query` object is immutable.
**Core Concepts:** Inner `static Builder` class, method chaining, immutable result object, `String.join`, `StringBuilder`
**Difficulty:** Intermediate
**Unique Challenge:** Making the builder truly immutable — each method call on the builder returns `this` (mutable builder), but the terminal `build()` constructs a new immutable `Query` record/object using Java 16+ `record` or a final-field class.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 34. Proxy Pattern (Lazy Load)
**Slug:** `proxy-pattern`
**Files:**
```
src/
├── Main.java
└── Image.java
```
**Description:** Virtual proxy for expensive image loading — `ProxyImage` implements the same `Image` interface as `RealImage`. The real image is only loaded on the first `display()` call; subsequent calls use the cached instance. Thread-safe via double-checked locking.
**Core Concepts:** `interface Image`, proxy class, lazy initialization, `volatile`, double-checked locking (DCL) pattern
**Difficulty:** Intermediate
**Unique Challenge:** Implementing DCL thread safety correctly — declare the `realImage` field as `volatile` so the JVM's memory model guarantees that the initialized object is fully visible to other threads before the reference is published.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 35. Strategy Pattern (Pluggable Sorting)
**Slug:** `strategy-pattern`
**Files:**
```
src/
├── Main.java
└── Sorter.java
```
**Description:** Pluggable sorting strategies — BubbleSort, InsertionSort, MergeSort, QuickSort, HeapSort — swappable at runtime via a `@FunctionalInterface`. Compare strategy performance on the same input data with `System.nanoTime` benchmarks.
**Core Concepts:** `@FunctionalInterface SortStrategy`, lambda expressions, generic `Comparator<T>`, `System.nanoTime`, runtime strategy swap
**Difficulty:** Intermediate
**Unique Challenge:** Making the strategy a `@FunctionalInterface` — `void sort(int[] arr)` — so any sorting lambda can be plugged in: `sorter.setStrategy((a) -> Arrays.sort(a))` vs. a custom implementation, making the benchmark trivially extensible.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --benchmark
```

---

### 36. Iterator Pattern from Scratch (Tree) [RARE]
**Slug:** `iterator-pattern`
**Files:**
```
src/
├── Main.java
└── Tree.java
```
**Description:** Binary tree with three separate `Iterator<T>` implementations — in-order (stack-based, non-recursive), pre-order, and level-order (queue-based) — all accessible via `tree.inorderIterator()` etc. Each works lazily with `hasNext`/`next`.
**Core Concepts:** `Iterator<T>`, `Iterable<T>`, `ArrayDeque<Node>` as stack/queue, lazy traversal, inner iterator class
**Difficulty:** Intermediate
**Unique Challenge:** Implementing a non-recursive in-order iterator using an explicit stack — on construction, push all left-spine nodes. On each `next()`, pop a node, record its value, then push the right child's entire left spine. State persists across `next()` calls.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 37. Maze Generator & Solver
**Slug:** `maze`
**Files:**
```
src/
├── Main.java
└── Maze.java
```
**Description:** Generate perfect mazes using recursive backtracking (DFS) and Prim's algorithm. Solve with BFS (shortest path), DFS (any path), and A* (optimal). Render as ASCII art with the solution path highlighted in a different character.
**Core Concepts:** `ArrayDeque` as stack/queue, `PriorityQueue` for A*, 2D `int[][]` with wall bits, `Random.shuffle` on direction arrays
**Difficulty:** Intermediate
**Unique Challenge:** Encoding the maze compactly — store walls as 4 bits within each cell integer (`N=1, E=2, S=4, W=8`) rather than a separate 2D wall array. Carving a passage removes the bit in cell A *and* the opposite bit in cell B simultaneously.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --width 40 --height 20
java -cp src Main --width 40 --height 20 --gen prims --solve astar
```

---

### 38. Tic-Tac-Toe with Minimax
**Slug:** `tictactoe-minimax`
**Files:**
```
src/
└── Main.java
```
**Description:** Unbeatable Tic-Tac-Toe AI using Minimax with alpha-beta pruning. Display the board after each move, show the AI's score for each candidate move in verbose mode, and measure how many fewer nodes alpha-beta explores vs. pure Minimax.
**Core Concepts:** Minimax recursion, alpha-beta pruning, game state as `char[]` (9-element flat array), terminal state detection, node-count tracking
**Difficulty:** Intermediate
**Unique Challenge:** Implementing alpha-beta pruning correctly — `alpha` is the best value the maximizer can guarantee so far; `beta` is the best the minimizer can guarantee. Prune when `beta <= alpha`. The result must be identical to pure Minimax; verify this with an assertion.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main
java -cp src Main --verbose
```

---

### 39. Memory Allocator Simulator [RARE]
**Slug:** `memory-allocator`
**Files:**
```
src/
├── Main.java
└── MemoryManager.java
```
**Description:** Simulate three memory allocation strategies — first-fit, best-fit, and worst-fit — on a fixed-size `byte[]`. Track fragmentation percentage, allocation success rate, and average search time. Includes free-block coalescing.
**Core Concepts:** Array-based free list, linked block headers (stored in the array itself), fragmentation calculation, coalescing adjacent free blocks
**Difficulty:** Intermediate
**Unique Challenge:** Coalescing adjacent free blocks after a `free()` call — scan the free list for blocks immediately before and after the newly freed block (by address), merge them into a single larger block to reduce external fragmentation.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 40. Thread Pool from Scratch [RARE]
**Slug:** `thread-pool`
**Files:**
```
src/
├── Main.java
└── ThreadPool.java
```
**Description:** Fixed-size thread pool with a `LinkedBlockingQueue` work queue, `Future<V>`-like result wrapper, graceful shutdown (`shutdown()` drains remaining tasks), `awaitTermination()`, and a `ScheduledThreadPool` variant with delay support.
**Core Concepts:** `Thread`, `BlockingQueue<Runnable>`, `volatile boolean shutdown`, `CountDownLatch`, `Callable<V>`, `ReentrantLock` + `Condition`
**Difficulty:** Intermediate
**Unique Challenge:** Implementing `awaitTermination()` correctly — block the calling thread until all worker threads have finished. Use a `CountDownLatch(poolSize)` where each worker calls `latch.countDown()` after processing the shutdown signal, then `latch.await()` in `awaitTermination()`.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 41. Producer-Consumer (Blocking Queue)
**Slug:** `producer-consumer`
**Files:**
```
src/
├── Main.java
└── BoundedBuffer.java
```
**Description:** Classic producer-consumer problem with a hand-rolled bounded blocking queue using `synchronized`, `wait()`, and `notifyAll()`. Demonstrates the difference between `notify()` (buggy with multiple producers) and `notifyAll()` (correct).
**Core Concepts:** `synchronized`, `wait()`, `notifyAll()`, bounded buffer, `Thread`, deadlock-free design
**Difficulty:** Intermediate
**Unique Challenge:** Explaining and demonstrating why `notify()` is wrong with multiple producers AND multiple consumers — a producer could wake another producer (who finds the buffer full and waits again), while a waiting consumer is never woken. Only `notifyAll()` guarantees the right thread proceeds.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --producers 3 --consumers 2 --capacity 10
```

---

### 42. Read-Write Lock [RARE]
**Slug:** `rw-lock`
**Files:**
```
src/
├── Main.java
└── ReadWriteLock.java
```
**Description:** Custom read-write lock allowing multiple concurrent readers or one exclusive writer — built from `synchronized`, `wait`, and `notifyAll`. Implements writer preference to prevent writer starvation, and demonstrates the lock with a concurrent shared-cache scenario.
**Core Concepts:** `synchronized`, `wait`/`notifyAll`, reader count, waiting-writer count, fairness policy
**Difficulty:** Intermediate
**Unique Challenge:** Implementing writer preference — a new reader must block if there are writers *waiting* (not just the current writer). Track `waitingWriters` separately; in `lockRead()`, loop `while (activeWriter || waitingWriters > 0) wait()`.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 43. Semaphore from Scratch [RARE]
**Slug:** `semaphore`
**Files:**
```
src/
├── Main.java
└── Semaphore.java
```
**Description:** Counting semaphore built from `synchronized` and `wait`/`notifyAll`. Implements `acquire()`, `release()`, `tryAcquire(timeout)`, and `availablePermits()`. Demonstrates with two classic problems: a bounded resource pool and the Dining Philosophers (5 philosophers, deadlock-free).
**Core Concepts:** `synchronized`, `wait`/`notifyAll`, `System.currentTimeMillis` for timeout, Dining Philosophers with asymmetric fork acquisition
**Difficulty:** Intermediate
**Unique Challenge:** Solving Dining Philosophers deadlock-free using asymmetric lock ordering — philosophers 0–3 pick up the left fork first; philosopher 4 picks up the right fork first. This breaks the circular wait condition without a global lock.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --demo philosophers
java -cp src Main --demo resource-pool
```

---

### 44. External Merge Sort
**Slug:** `merge-sort-file`
**Files:**
```
src/
├── Main.java
└── ExternalMerge.java
```
**Description:** Sort a file larger than available RAM using external merge sort — split the file into sorted temporary chunks (each fits in memory), then k-way merge all chunks into the final sorted output using a `PriorityQueue`.
**Core Concepts:** `BufferedReader`/`BufferedWriter`, `PriorityQueue<Line>` for k-way merge, temp file management, `File.deleteOnExit()`
**Difficulty:** Intermediate
**Unique Challenge:** Implementing k-way merge with a min-heap — each heap entry holds `(currentLine, sourceReader)`; after popping the minimum line, read the next line from that source reader and re-insert it. This keeps the heap at most k entries.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main bigfile.txt sorted.txt --chunk-size 100000
```

---

### 45. CSV Parser & Reporter
**Slug:** `csv-reporter`
**Files:**
```
src/
├── Main.java
├── CSVParser.java
└── Report.java
```
**Description:** RFC-4180 compliant CSV parser handling quoted fields (with embedded commas and newlines), escaped double-quotes (`""`), and BOM stripping. Generate pivot reports: group by a column, aggregate another with SUM/AVG/COUNT/MAX/MIN.
**Core Concepts:** State machine CSV parsing, `List<Map<String, String>>`, `HashMap` grouping, RFC-4180 quoted-field rules
**Difficulty:** Intermediate
**Unique Challenge:** Parsing RFC-4180 edge cases correctly — a quoted field can span multiple lines (the newline is literal data), and `""` inside a quoted field is an escaped quote, not the end of the field. A state machine (UNQUOTED / QUOTED / ESCAPED) handles all cases cleanly.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main data.csv --group region --agg sum --col sales
```

---

### 46. JSON Serializer (no libraries) [RARE]
**Slug:** `json-serializer`
**Files:**
```
src/
├── Main.java
├── JsonSerializer.java
└── JsonParser.java
```
**Description:** Serialize Java POJOs to JSON strings and parse JSON strings back to `Map<String, Object>` / `List<Object>` trees — no Jackson, no Gson. Reflection-based serializer handles nested objects, arrays, and all primitive types.
**Core Concepts:** `java.lang.reflect.Field`, `Field.setAccessible(true)`, `Field.get(obj)`, recursive serialization, recursive-descent JSON parser
**Difficulty:** Intermediate
**Unique Challenge:** Using reflection to iterate a POJO's declared fields and recursively serialize nested objects — checking `field.getType().isPrimitive()`, `isArray()`, `List.class.isAssignableFrom(type)`, and defaulting to recursive object serialization for anything else.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 47. Huffman Encoder / Decoder
**Slug:** `huffman`
**Files:**
```
src/
├── Main.java
└── HuffmanTree.java
```
**Description:** Build a Huffman coding tree from character frequencies using a `PriorityQueue`. Encode text to a compact `BitSet`-backed bitstream. Write a binary file with a serialized code table header. Decode back to the original text. Report compression ratio.
**Core Concepts:** `PriorityQueue<Node>`, `BitSet`, binary tree traversal for code generation, `DataOutputStream`/`DataInputStream` for file I/O
**Difficulty:** Intermediate
**Unique Challenge:** Serializing the Huffman tree to the output file header so the decoder can reconstruct it without re-analyzing the data — use a pre-order traversal encoding: write `0` + recurse for internal nodes, write `1` + the character byte for leaf nodes.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main compress input.txt output.huf
java -cp src Main decompress output.huf recovered.txt
```

---

### 48. Heap Implementation
**Slug:** `heap`
**Files:**
```
src/
├── Main.java
└── Heap.java
```
**Description:** Generic min-heap and max-heap backed by a `Comparable[]` array — `insert`, `extractMin`/`extractMax`, `peek`, `decreaseKey` (with an index map for O(log n) update), and `heapSort`. Demonstrates use in Dijkstra's algorithm.
**Core Concepts:** Generics, `Comparable<T>`, `siftUp`/`siftDown`, `HashMap<T, Integer>` index map for `decreaseKey`
**Difficulty:** Intermediate
**Unique Challenge:** Implementing `decreaseKey` efficiently — requires knowing a node's current array position in O(1). Maintain a `HashMap<T, Integer>` that maps each element to its array index; update this map on every swap during `siftUp`/`siftDown`.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 49. AVL Tree [CLASSIC]
**Slug:** `avl-tree`
**Files:**
```
src/
├── Main.java
└── AVLTree.java
```
**Description:** Self-balancing AVL tree with insert, delete, search, min, max, and in-order traversal. Each node stores its height. Single and double rotations (LL, RR, LR, RL cases) restore the balance factor (|left.height - right.height| ≤ 1) after every operation.
**Core Concepts:** Recursive BST, rotation operations, balance factor, height tracking, `Comparable<K>`
**Difficulty:** Intermediate
**Unique Challenge:** Correctly choosing between single and double rotations — after insert or delete, check the node's balance factor AND the child's balance factor. For example: balance = +2 and left child balance = -1 → LR double rotation (rotate left child left, then root right).
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 50. Red-Black Tree [RARE]
**Slug:** `red-black-tree`
**Files:**
```
src/
├── Main.java
└── RBTree.java
```
**Description:** Red-Black tree matching the standard `java.util.TreeMap` implementation — insert with recoloring and rotations (3 insert cases), delete with double-black resolution (6 delete cases), search, min, max, and in-order traversal. Uses a `NIL` sentinel node.
**Core Concepts:** Tree rotations, red/black node coloring, insert fixup (3 cases), delete fixup (6 cases), sentinel NIL node
**Difficulty:** Advanced
**Unique Challenge:** Handling all 6 deletion cases correctly — the double-black resolution involves checking sibling color, sibling's children colors, and parent color in a specific sequence. Case 2 (sibling black, both nephews black) propagates double-black upward; the other cases terminate in ≤ 3 rotations.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 51. B-Tree [RARE]
**Slug:** `btree`
**Files:**
```
src/
├── Main.java
├── BTree.java
└── BTreeNode.java
```
**Description:** B-Tree with configurable minimum degree `t` (each non-root node has t–1 to 2t–1 keys). Implements proactive splitting on insert (split full children before descending), search, delete with three cases (leaf key, internal key, underflow fix), and range scan.
**Core Concepts:** Balanced tree invariants, proactive split on insert, delete cases (predecessor/successor replacement, merge), in-order traversal
**Difficulty:** Advanced
**Unique Challenge:** The three-way delete decision — (1) key is in a leaf: remove directly; (2) key is in an internal node: replace with in-order predecessor or successor (from a child with ≥ t keys) then recursively delete; (3) neither child has ≥ t keys: merge children, then delete from merged node.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 52. Skip List [RARE]
**Slug:** `skip-list`
**Files:**
```
src/
├── Main.java
└── SkipList.java
```
**Description:** Generic probabilistic skip list with O(log n) expected search, insert, and delete. Each node has a random height determined by coin flips. Header has `MAX_LEVEL` forward pointers. Range queries return all keys in `[low, high]`. Used as an exercise comparing with a BST.
**Core Concepts:** Multi-level forward pointer arrays, `Random` for level selection, `update[]` predecessor array, generics, `Comparable<K>`
**Difficulty:** Intermediate
**Unique Challenge:** Maintaining the `update[]` array correctly during insert and delete — before any structural change, build `update[i]` = the rightmost node at level `i` whose `forward[i]` needs updating. Getting this traversal wrong corrupts every level above the changed node.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 53. Bloom Filter [RARE]
**Slug:** `bloom-filter`
**Files:**
```
src/
├── Main.java
└── BloomFilter.java
```
**Description:** Probabilistic set membership filter with configurable false-positive rate. Computes optimal `m` (bit count) and `k` (hash count) from target capacity `n` and FP rate `p`. Uses double hashing to simulate k independent hash functions from two `hashCode` seeds. Empirically verifies the FP rate.
**Core Concepts:** `BitSet`, double hashing, FP rate formula, `MessageDigest` (SHA-256) for hash seeds, `BitSet.set`/`BitSet.get`
**Difficulty:** Intermediate
**Unique Challenge:** Implementing double hashing — `h_i(x) = (h1(x) + i * h2(x)) % m` — to simulate k independent hash functions from just two. Use `Arrays.hashCode` and a seeded XOR-shift for `h1`/`h2`, then reduce each `h_i` to `[0, m)` with `Math.floorMod` (not `%`, which gives negative values).
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --capacity 100000 --fp-rate 0.01 --verify
```

---

### 54. LFU Cache [RARE]
**Slug:** `lfu-cache`
**Files:**
```
src/
├── Main.java
└── LFUCache.java
```
**Description:** O(1) get and put Least Frequently Used cache. Maintains three maps: `key → value`, `key → frequency`, and `frequency → LinkedHashSet<K>` (ordered by insertion within the same frequency). Tracks `minFreq` for O(1) eviction.
**Core Concepts:** `HashMap`, `LinkedHashSet` for LRU ordering within a frequency bucket, `minFreq` tracking, O(1) eviction
**Difficulty:** Intermediate
**Unique Challenge:** Maintaining the `minFreq` variable correctly — when a key's frequency increases from `f` to `f+1`, if `f == minFreq` AND the frequency-f bucket is now empty, increment `minFreq` to `f+1`. On a new insertion, always reset `minFreq = 1`.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 55. Interval Tree [RARE]
**Slug:** `interval-tree`
**Files:**
```
src/
├── Main.java
└── IntervalTree.java
```
**Description:** Augmented BST where each node stores an interval `[low, high]` and a `maxEnd` field (the maximum `high` value in its subtree). Supports insert, delete, point-containment query, and overlap query — all in O(log n) for balanced trees.
**Core Concepts:** Augmented BST, `maxEnd` invariant, overlap predicate `(a.low <= b.high && b.low <= a.high)`, in-order traversal
**Difficulty:** Intermediate
**Unique Challenge:** Maintaining the `maxEnd` augmentation after rotations — whenever a subtree is restructured, recompute `maxEnd` as `max(node.high, node.left.maxEnd, node.right.maxEnd)` for every affected node bottom-up. Forgetting to update `maxEnd` makes overlap queries return incorrect results.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 56. Segment Tree [RARE]
**Slug:** `segment-tree`
**Files:**
```
src/
├── Main.java
└── SegmentTree.java
```
**Description:** Array-based segment tree supporting range sum, range min, and range max queries in O(log n). Point update in O(log n). Lazy propagation for range update (add a value to all elements in [l, r]) in O(log n).
**Core Concepts:** Array-based binary tree (node `i` has children `2i` and `2i+1`), `build` in O(n), sift-down query, lazy propagation with pending-update tags
**Difficulty:** Intermediate
**Unique Challenge:** Implementing lazy propagation correctly — a pending update at a node must be pushed down to its children (with `pushDown()`) before any split of that node's range during a query or update. Forgetting `pushDown` before recursing yields stale results.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 57. Fenwick Tree (Binary Indexed Tree) [RARE]
**Slug:** `fenwick-tree`
**Files:**
```
src/
├── Main.java
└── FenwickTree.java
```
**Description:** Fenwick tree (BIT) supporting prefix sum, point update, and range sum query — all O(log n). Uses the `lowbit(i) = i & (-i)` trick. Demonstrates order-of-magnitude speedup over a naive prefix-sum array for mixed update/query workloads.
**Core Concepts:** `lowbit = i & (-i)`, prefix sum navigation, range query decomposition, benchmark vs. naive array
**Difficulty:** Intermediate
**Unique Challenge:** Understanding why `i += lowbit(i)` navigates upward during update while `i -= lowbit(i)` navigates downward during query — implementing both without mixing them up, and proving correctness with the overlap-free coverage property of the BIT.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 58. Disjoint Set / Union-Find
**Slug:** `union-find`
**Files:**
```
src/
├── Main.java
└── UnionFind.java
```
**Description:** Union-Find with path compression (iterative and recursive variants) and union by rank AND union by size — compare the two strategies' tree heights. Demonstrates use in Kruskal's MST and cycle detection. Achieves near-O(1) amortized operations.
**Core Concepts:** `parent[]`, `rank[]`/`size[]` arrays, path compression, inverse Ackermann function amortized complexity
**Difficulty:** Intermediate
**Unique Challenge:** Implementing iterative path compression (two-pass: find root, then update all nodes on the path) vs. one-pass path halving (every other node points to its grandparent). Both achieve near-O(1) but halving is simpler to implement correctly.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 59. Kruskal's MST
**Slug:** `kruskals-mst`
**Files:**
```
src/
├── Main.java
├── Graph.java
└── UnionFind.java
```
**Description:** Find the Minimum Spanning Tree (or Forest, for disconnected graphs) of a weighted undirected graph using Kruskal's algorithm with a Union-Find for cycle detection. Verify the MST weight against Prim's algorithm output.
**Core Concepts:** Edge sorting by weight, Union-Find for cycle detection, greedy algorithm, MST vs. MSF distinction
**Difficulty:** Intermediate
**Unique Challenge:** Handling disconnected graphs — Kruskal's produces a minimum spanning *forest*, not a tree. After processing all edges, check if the number of union operations equals `V - numComponents`; report each component's sub-tree separately.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main graph.txt
```

---

### 60. A* Pathfinding
**Slug:** `astar`
**Files:**
```
src/
├── Main.java
├── Grid.java
└── AStar.java
```
**Description:** A* pathfinding on a 2D obstacle grid with 4-directional and 8-directional (diagonal, cost √2) movement. Multiple heuristics: Manhattan, Euclidean, Chebyshev, Octile. Reads ASCII-art maps from files. Tie-breaking produces visually straight paths.
**Core Concepts:** `PriorityQueue<Node>`, `HashMap` for `gScore`/`parent`, admissible heuristics, diagonal movement cost, path reconstruction
**Difficulty:** Intermediate
**Unique Challenge:** Implementing tie-breaking — when `f = g + h` is equal for multiple nodes, prefer the node with the larger `g` (farther from start). Add a tiny `h * epsilon` tiebreaker to `f` during priority comparison, avoiding the visually ugly "explored everywhere equally" behavior.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main dungeon.txt 1,1 20,15
```

---

### 61. Knuth-Morris-Pratt (KMP) [RARE]
**Slug:** `kmp-search`
**Files:**
```
src/
├── Main.java
└── KMP.java
```
**Description:** Find all occurrences of a pattern in a text in O(n + m) using KMP with the failure function. Also implement the Z-algorithm as a comparison. Show step-by-step failure function construction. Benchmark against `String.indexOf` on large texts.
**Core Concepts:** Failure function (`lps[]` array), KMP state machine, Z-array, O(n+m) guarantee, `while (k > 0)` backoff loop
**Difficulty:** Intermediate
**Unique Challenge:** Building the KMP failure function correctly — the tricky `while (k > 0 && pattern[k] != pattern[i]) k = lps[k-1]` backoff loop is subtle. Without it, the function computes wrong values for patterns with overlapping prefixes like `"AABAAB"`.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main "abcabc" "abc"
java -cp src Main --file large_text.txt --pattern "the quick"
```

---

### 62. Rabin-Karp Rolling Hash [RARE]
**Slug:** `rabin-karp`
**Files:**
```
src/
├── Main.java
└── RabinKarp.java
```
**Description:** Multi-pattern string search using polynomial rolling hashing. Find all occurrences of a set of patterns in text in O(n + m) average. Demonstrates hash collision handling (always verify on hash match). Benchmark single-pattern vs. KMP vs. naive.
**Core Concepts:** Polynomial rolling hash, sliding window, `HashSet<Long>` for multi-pattern hashes, collision verification, modular arithmetic
**Difficulty:** Intermediate
**Unique Challenge:** Handling hash collisions correctly — when the rolling hash of a text window matches a pattern's hash, always verify the actual strings match before reporting. Without this, false positives corrupt the output. Double hashing (two independent hash functions) reduces collision probability to negligible.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main text.txt patterns.txt
```

---

### 63. Levenshtein Distance
**Slug:** `levenshtein`
**Files:**
```
src/
└── Main.java
```
**Description:** Compute edit distance between two strings with full DP table visualization, edit script reconstruction (sequence of insertions, deletions, substitutions) via traceback, space-optimized two-row variant, and a batch spell-check mode against a dictionary.
**Core Concepts:** `int[][]` DP table, traceback for edit script, space optimization (two rows), `BufferedReader` for batch mode
**Difficulty:** Intermediate
**Unique Challenge:** Reconstructing the edit script by tracing back diagonally through the DP table — at each cell, determine whether the cheapest operation was a match (diagonal, cost 0), substitution (diagonal, cost 1), insertion (left), or deletion (up), and output the corresponding edit symbol.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main "kitten" "sitting"
java -cp src Main --spell essay.txt dictionary.txt
```

---

### 64. Longest Common Subsequence
**Slug:** `lcs`
**Files:**
```
src/
└── Main.java
```
**Description:** Compute the LCS of two strings using DP, reconstruct the actual LCS string, generate a diff (unified-diff format with +/-/space lines), and apply the diff as a patch. Space-optimized to O(min(m,n)).
**Core Concepts:** `int[][]` DP, LCS reconstruction via traceback, diff generation, two-row space optimization
**Difficulty:** Intermediate
**Unique Challenge:** Generating a human-readable diff from the LCS traceback — label each character as `' '` (common, in LCS), `'-'` (only in string A, deleted), or `'+'` (only in string B, inserted). Group consecutive same-label characters into diff hunks.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main "ABCBDAB" "BDCAB"
java -cp src Main --diff file_a.txt file_b.txt
```

---

### 65. 0-1 Knapsack
**Slug:** `knapsack`
**Files:**
```
src/
└── Main.java
```
**Description:** 0-1 knapsack with full DP, item selection traceback (which items were chosen), space-optimized 1D DP, unbounded knapsack variant, fractional knapsack (greedy) comparison, and a benchmark showing when greedy fails for 0-1.
**Core Concepts:** `int[][]` DP, item traceback by iterating backward, 1D space optimization, greedy for fractional
**Difficulty:** Intermediate
**Unique Challenge:** Tracing back selected items from the 1D space-optimized DP — you can't trace back from a 1D array the same way as from a 2D table. Keep the 2D table only for the traceback phase, or re-run with the known optimal value to identify selected items in a second pass.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main items.json 50
java -cp src Main --fractional items.json 50
```

---

### 66. Matrix Chain Multiplication [RARE]
**Slug:** `matrix-chain`
**Files:**
```
src/
└── Main.java
```
**Description:** Find the optimal parenthesization of a matrix chain to minimize scalar multiplications — using interval DP. Print the minimum cost, the split table, and recursively format the optimal parenthesization as a human-readable expression.
**Core Concepts:** Interval DP, `int[][] m` (min cost), `int[][] s` (split point), recursive parenthesization printing
**Difficulty:** Intermediate
**Unique Challenge:** Printing the optimal parenthesization from the `s[][]` split table recursively — `print(s, i, j)` outputs `"(" + print(s, i, s[i][j]) + print(s, s[i][j]+1, j) + ")"`. Off-by-one errors in `s` indices cause malformed output that's hard to debug.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main 30 35 15 5 10 20 25
```

---

### 67. Mini HTTP Server
**Slug:** `mini-http`
**Files:**
```
src/
├── Main.java
└── RequestHandler.java
```
**Description:** HTTP/1.0 static file server on raw `ServerSocket`. Serves files from a root directory with correct MIME types (`URLConnection.guessContentTypeFromName`), handles `404`/`403`, generates directory listings, and handles concurrent connections with a thread-per-request model.
**Core Concepts:** `ServerSocket`, `Socket`, `InputStream`/`OutputStream`, HTTP request parsing, MIME type detection, threading
**Difficulty:** Intermediate
**Unique Challenge:** Parsing the HTTP request from a raw TCP stream that may arrive in multiple `read()` calls — buffer the input using a `BufferedReader` and read lines until the empty line (`\r\n`) that terminates the headers, then parse the request line and relevant headers.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main 8080 ./public
```

---

### 68. Simple FTP Client [RARE]
**Slug:** `ftp-client`
**Files:**
```
src/
├── Main.java
└── FTPClient.java
```
**Description:** FTP client implementing RFC 959 — connect to an FTP server, authenticate with USER/PASS, navigate with PWD/CWD, list files with LIST, download with RETR, upload with STOR, and delete with DELE. Implements passive mode (PASV) for data connections.
**Core Concepts:** `Socket` for control connection, PASV response parsing, data connection, FTP response codes, file streaming
**Difficulty:** Intermediate
**Unique Challenge:** Parsing the PASV response — `"227 Entering Passive Mode (h1,h2,h3,h4,p1,p2)"` — to compute the data connection host (`h1.h2.h3.h4`) and port (`p1 * 256 + p2`). The host part may differ from the control connection host on NAT'd servers.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main ftp.example.com
```

---

### 69. SMTP Client (raw socket) [RARE]
**Slug:** `smtp-client`
**Files:**
```
src/
├── Main.java
└── SMTPClient.java
```
**Description:** Send emails via a raw SMTP conversation — no `jakarta.mail`. Implements the full handshake: `EHLO`, `STARTTLS` upgrade (wrapping the socket in `SSLSocket`), `AUTH LOGIN` with Base64-encoded credentials, `MAIL FROM`, `RCPT TO`, `DATA`, and `QUIT`. Parses multi-line server responses (lines starting with `-`).
**Core Concepts:** `Socket`, `SSLSocket`, `javax.net.ssl.SSLSocketFactory`, SMTP RFC 5321, `Base64.getEncoder()`, multi-line response parsing
**Difficulty:** Intermediate
**Unique Challenge:** Wrapping the existing plaintext `Socket` with an `SSLSocket` after STARTTLS — call `SSLSocketFactory.createSocket(existingSocket, host, port, true)` to upgrade the same TCP connection rather than opening a new one.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --to user@example.com --from me@gmail.com --subject "Test"
```

---

### 70. Port Scanner
**Slug:** `port-scanner`
**Files:**
```
src/
└── Main.java
```
**Description:** Scan a host for open TCP ports across a configurable range with a bounded thread pool (`ExecutorService`). Banner-grab on open ports by sending a probe and reading the first response. Map well-known ports to service names via a built-in `HashMap`.
**Core Concepts:** `ExecutorService.newFixedThreadPool`, `Socket.connect` with `InetSocketAddress` and timeout, `ConcurrentLinkedQueue`, banner grabbing
**Difficulty:** Intermediate
**Unique Challenge:** Collecting results thread-safely from many concurrent scanner tasks — use a `ConcurrentLinkedQueue<ScanResult>` that all threads add to without synchronization, then drain and sort it on the main thread after `executor.awaitTermination`.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main 192.168.1.1 1 1024 --threads 200
java -cp src Main scanme.nmap.org 20 80 --banner
```

---

### 71. Custom Exception Framework
**Slug:** `exception-framework`
**Files:**
```
src/
├── Main.java
├── AppException.java
└── ExceptionHandler.java
```
**Description:** Application exception hierarchy with error codes (`enum ErrorCode`), severity levels (`FATAL`, `ERROR`, `WARN`), structured logging with context (key-value pairs), exception chaining, and a global `Thread.UncaughtExceptionHandler` that formats all unhandled exceptions consistently.
**Core Concepts:** Custom exception hierarchy, `enum ErrorCode`, `Thread.setDefaultUncaughtExceptionHandler`, `Map<String, Object>` context, chained exceptions
**Difficulty:** Intermediate
**Unique Challenge:** Installing a global `UncaughtExceptionHandler` via `Thread.setDefaultUncaughtExceptionHandler(handler)` that catches every unhandled exception across all threads — and demonstrating that it fires even for exceptions thrown in daemon threads.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---



## 🔴 ADVANCED PROJECTS (72–120)

---

### 72. Dependency Injector [RARE]
**Slug:** `dep-injector`
**Files:**
```
src/
├── Main.java
├── Container.java
└── Inject.java
```
**Description:** A lightweight IoC container using reflection to auto-wire `@Inject`-annotated constructors. Supports singleton and prototype scopes. Detects circular dependencies at wiring time and throws a descriptive error. Demonstrates wiring a three-layer service graph.
**Core Concepts:** `java.lang.reflect.Constructor`, `@interface Inject`, `Class<?>`, `Constructor.newInstance`, `Set<Class<?>>` for cycle detection
**Difficulty:** Advanced
**Unique Challenge:** Detecting circular dependencies during wiring — maintain a `Set<Class<?>> resolving` that tracks types currently being instantiated. If a type appears in `resolving` when `resolve(type)` is called recursively, throw a `CircularDependencyException` naming the cycle path.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 73. Object Pool [RARE]
**Slug:** `object-pool`
**Files:**
```
src/
├── Main.java
└── ObjectPool.java
```
**Description:** Generic thread-safe object pool with `borrow()`, `return()`, and `borrowWithTimeout(millis)`. A background eviction thread removes idle objects after a configurable TTL. Factory objects create new instances when the pool is exhausted up to `maxSize`. Tracks pool statistics.
**Core Concepts:** `BlockingQueue<T>`, generics, `Callable<T>` factory, `ScheduledExecutorService` for eviction, idle timestamp tracking
**Difficulty:** Intermediate
**Unique Challenge:** Implementing idle-timeout eviction using a `ScheduledExecutorService` sweeper — each pooled object is wrapped with its last-returned timestamp; the sweeper runs periodically, removes objects idle longer than TTL, and calls `factory.create()` to replenish if the pool falls below `minSize`.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 74. Circuit Breaker [RARE]
**Slug:** `circuit-breaker`
**Files:**
```
src/
├── Main.java
├── CircuitBreaker.java
└── CircuitState.java
```
**Description:** Resilience4j-style circuit breaker with three states — CLOSED (normal), OPEN (failing fast), HALF-OPEN (probing recovery). Configurable failure threshold, success threshold (for HALF-OPEN → CLOSED), and open timeout. Generic over return type with `Supplier<T>`.
**Core Concepts:** `AtomicInteger` failure count, `volatile CircuitState state`, `System.nanoTime` for open timeout, `Supplier<T>`, state machine transitions
**Difficulty:** Advanced
**Unique Challenge:** Implementing the HALF-OPEN state that allows exactly one probe request through at a time — use `AtomicInteger probeCount` and only increment past 0 if the compare-and-set succeeds, so concurrent calls don't all slip through simultaneously.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 75. Rate Limiter (Token Bucket) [RARE]
**Slug:** `rate-limiter`
**Files:**
```
src/
├── Main.java
├── TokenBucket.java
└── SlidingWindowLimiter.java
```
**Description:** Two thread-safe rate limiting implementations — Token Bucket (burst-capable, lazy refill) and Sliding Window Log (precise, O(log n) per request). Both exposed as `@FunctionalInterface`-compatible decorators. Demo: simulated HTTP endpoint under load.
**Core Concepts:** `ReentrantLock`, `System.nanoTime`, `AtomicLong` for lazy token refill, `ConcurrentSkipListMap` for sliding window log timestamps
**Difficulty:** Intermediate
**Unique Challenge:** Making token bucket refill lazy (compute available tokens on demand based on elapsed nanoseconds since last refill) rather than using a background thread — `tokensNow = min(capacity, stored + (elapsed * ratePerNs))` computed under lock.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 76. Lock-Free Stack (CAS) [RARE]
**Slug:** `lock-free-stack`
**Files:**
```
src/
├── Main.java
└── LockFreeStack.java
```
**Description:** Treiber's lock-free stack using `AtomicReference<Node>` and compare-and-set (CAS) — no `synchronized` anywhere. Implements `push` and `pop` with CAS retry loops. Demonstrates the ABA problem with `AtomicReference` and fixes it with `AtomicStampedReference`.
**Core Concepts:** `AtomicReference<Node<T>>`, `compareAndSet`, ABA problem, `AtomicStampedReference`, lock-freedom proof
**Difficulty:** Advanced
**Unique Challenge:** Demonstrating the ABA problem concretely — thread 1 reads `head = A`, thread 2 pops A, pushes B, pops B, pushes A again; thread 1's CAS on `head` from A to `A.next` succeeds but is wrong because `A.next` has changed. Fix: stamp each reference with an integer version.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --demo aba
```

---

### 77. Lock-Free Queue (Michael-Scott) [RARE]
**Slug:** `lock-free-queue`
**Files:**
```
src/
├── Main.java
└── LockFreeQueue.java
```
**Description:** Michael-Scott non-blocking FIFO queue — the algorithm underlying `java.util.concurrent.ConcurrentLinkedQueue`. Implements `enqueue` (two-phase: link node, advance tail) and `dequeue` (advance head, return old head's data). Includes a "helping" mechanism for partially-completed enqueues.
**Core Concepts:** `AtomicReference<Node<T>>` head and tail, two-phase enqueue, CAS retry, helping mechanism, `volatile` data field
**Difficulty:** Advanced
**Unique Challenge:** Implementing the helping mechanism — when `dequeue` observes that `tail` lags behind the actual last node (because an enqueue was preempted after linking but before advancing `tail`), it CAS-advances `tail` itself before dequeuing. Without this, concurrent dequeues can return stale data.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 78. Reactive Streams (mini) [RARE]
**Slug:** `reactive-streams`
**Files:**
```
src/
├── Main.java
├── Publisher.java
├── Subscriber.java
└── Operators.java
```
**Description:** A minimal reactive streams implementation modeled after `java.util.concurrent.Flow`. Implements `map`, `filter`, `flatMap`, `merge`, `zip`, `take`, and `onBackpressureBuffer`. A slow subscriber explicitly requests N items via `subscription.request(n)`.
**Core Concepts:** `java.util.concurrent.Flow.Publisher/Subscriber/Subscription`, generics, backpressure demand signaling, `AtomicLong requestedCount`, `BlockingQueue` buffer
**Difficulty:** Advanced
**Unique Challenge:** Implementing backpressure demand signaling correctly — the publisher must track `requestedCount` (an `AtomicLong`) and only emit items when `requestedCount > 0`, decrementing it per emission. The subscriber calls `subscription.request(n)` to increment it.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 79. Actor Model (mini) [RARE]
**Slug:** `actor-model`
**Files:**
```
src/
├── Main.java
├── Actor.java
└── ActorSystem.java
```
**Description:** Akka-inspired actor model — actors communicate only by message-passing via mailboxes (`LinkedBlockingQueue`), share no mutable state. `ActorSystem` creates actors, routes messages, and handles supervision (restart on failure). Demo: parallel word-count pipeline.
**Core Concepts:** `LinkedBlockingQueue<Object>` mailbox, `ExecutorService`, actor reference (opaque handle), supervision strategy, message immutability
**Difficulty:** Advanced
**Unique Challenge:** Implementing supervisor hierarchies — a parent actor receives a `ChildFailed(actorRef, cause)` message when a child throws. The supervisor decides to restart (create a new actor with the same behavior), stop it, or escalate to its own parent.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 80. Distributed Key-Value Store [RARE]
**Slug:** `dist-kv`
**Files:**
```
src/
├── Main.java
├── Node.java
├── Server.java
└── Client.java
```
**Description:** Multi-process distributed KV store with consistent hashing for key routing, replication factor of 3 (each key stored on 3 nodes), and quorum reads/writes (majority must respond). Nodes communicate via TCP sockets. A client library handles routing transparently.
**Core Concepts:** `ServerSocket`, `TreeMap` for consistent hash ring, replication fan-out, quorum protocol (`W + R > N`), `CountDownLatch` for quorum wait
**Difficulty:** Advanced
**Unique Challenge:** Implementing quorum writes — fan out the write to all 3 replica nodes in parallel threads, use a `CountDownLatch(2)` that decrements on each acknowledgment, and return success after 2 acks (majority of 3). If fewer than 2 ack within a timeout, return failure and roll back.
**Compile & Run:**
```bash
javac src/*.java
java -cp src Server 8001 &
java -cp src Server 8002 &
java -cp src Server 8003 &
java -cp src Client set mykey "hello"
```

---

### 81. Raft Consensus (mini) [RARE]
**Slug:** `raft`
**Files:**
```
src/
├── Main.java
├── RaftNode.java
├── LogEntry.java
└── RPC.java
```
**Description:** A 5-node Raft consensus simulation where nodes run as threads and communicate via `LinkedBlockingQueue` RPC channels. Implements leader election (randomized 150–300ms timeout), log replication (AppendEntries), and commit index advancement. Simulates node failures and leader re-election.
**Core Concepts:** `volatile int currentTerm`, `volatile int votedFor`, `LinkedBlockingQueue` RPC queues, randomized `scheduleTimeout`, `AtomicInteger` vote count
**Difficulty:** Advanced
**Unique Challenge:** Using randomized election timeouts to ensure exactly one node wins most elections — `ThreadLocalRandom.current().nextInt(150, 300)` milliseconds. When a split vote occurs (no majority), all candidates time out independently and start a new term, preventing livelock.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 82. LSM-Tree Storage Engine [RARE]
**Slug:** `lsm-tree`
**Files:**
```
src/
├── Main.java
├── LSMTree.java
├── SSTable.java
└── MemTable.java
```
**Description:** Log-Structured Merge-Tree with: in-memory `TreeMap` MemTable (sorted inserts), append-only WAL for crash recovery, immutable SSTable files (sorted key-value pairs written on MemTable flush), and a background thread that merges and compacts SSTables by sequence number.
**Core Concepts:** `TreeMap`, `RandomAccessFile`, WAL append, binary search on SSTable, merge-sort for compaction, tombstone deletes, sequence numbers
**Difficulty:** Advanced
**Unique Challenge:** Implementing compaction correctly — merge N sorted SSTable files using a k-way min-heap, de-duplicate keys by keeping only the entry with the highest sequence number, and drop tombstone entries (deletes) that have no older data to shadow.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 83. JVM Bytecode Reader [RARE]
**Slug:** `jvm-reader`
**Files:**
```
src/
├── Main.java
└── ClassFileReader.java
```
**Description:** Parse a Java `.class` file binary format — read the magic number (`0xCAFEBABE`), minor/major version, constant pool (all 18 tag types), access flags, class/superclass names, interfaces, fields, methods, and attributes (Code, LineNumberTable, SourceFile).
**Core Concepts:** `DataInputStream`, JVMS constant pool tags and structure, method descriptor parsing, attribute table traversal
**Difficulty:** Advanced
**Unique Challenge:** Parsing the constant pool correctly — it's 1-indexed, variable-width, and `LONG`/`DOUBLE` entries consume two slots (entry `i+1` is unusable). Missing the two-slot rule causes every subsequent constant pool index to be off by one, corrupting all references.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main HelloWorld.class
java -cp src Main Main.class
```

---

### 84. Class File Disassembler [RARE]
**Slug:** `class-dis`
**Files:**
```
src/
├── Main.java
├── Disassembler.java
└── ClassFileReader.java
```
**Description:** Read a `.class` file and disassemble its method bytecode to human-readable mnemonics — output similar to `javap -c`. Resolves constant pool references in `INVOKEVIRTUAL`, `GETFIELD`, `LDC`, `NEW` etc. to their symbolic names. Shows line number mappings from `LineNumberTable`.
**Core Concepts:** JVMS opcode table (`Opcode.java`), constant pool resolution, method descriptor formatting, `LineNumberTable` attribute parsing
**Difficulty:** Advanced
**Unique Challenge:** Resolving symbolic references — `INVOKEVIRTUAL #4` means: look up constant pool entry 4 (a `CONSTANT_Methodref`), which references a class index and a name-and-type index, each of which references UTF-8 entries. Chase this 3-level indirection to produce `"java/io/PrintStream.println:(Ljava/lang/String;)V"`.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main HelloWorld.class
```

---

### 85. Interpreter (tiny language) [RARE]
**Slug:** `interpreter`
**Files:**
```
src/
├── Main.java
├── Lexer.java
├── Parser.java
└── Interpreter.java
```
**Description:** A tree-walk interpreter for a small language with: `let` variables, arithmetic, comparisons, `if/else`, `while`, `print`, first-class functions, lexical closures, and a REPL. Error messages include the source line and column.
**Core Concepts:** Handwritten lexer (no regex), recursive-descent parser, AST node hierarchy (sealed interface, Java 17+), `Environment` chain for scope, closure objects capturing their definition environment
**Difficulty:** Advanced
**Unique Challenge:** Implementing lexical closures — a `Function` AST node must capture a *reference* to the `Environment` at its definition site, not a copy. When the function is called, create a new `Environment` that *extends* the captured one, so the closure sees variables from its enclosing scope.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main           # REPL
javac src/*.java && java -cp src Main prog.lang # Run file
```

---

### 86. Compiler (expression lang → bytecode) [RARE]
**Slug:** `mini-compiler`
**Files:**
```
src/
├── Main.java
├── Lexer.java
├── Parser.java
└── CodeGen.java
```
**Description:** Compile a simple expression language (variables, arithmetic, booleans, `if/else`, `while`, functions) to bytecode for Project #85's interpreter VM, or alternatively to JVM `.class` file bytecode encoded as a raw `byte[]` without ASM.
**Core Concepts:** Lexer, recursive-descent parser, AST, symbol table with scope levels, bytecode emission, function call convention (local variable slots)
**Difficulty:** Advanced
**Unique Challenge:** Building a multi-scope symbol table — each function definition opens a new scope level; `resolve(name)` walks up from the current scope to find a variable, emitting `LOAD_LOCAL` if in the current function frame or `LOAD_UPVALUE` if in an enclosing scope.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main program.lang
java -cp src Main --dis program.lang    # Disassemble without running
```

---

### 87. RSA from Scratch [RARE]
**Slug:** `rsa`
**Files:**
```
src/
├── Main.java
├── RSA.java
└── MathUtils.java
```
**Description:** Full RSA implementation: generate two 512-bit primes using Miller-Rabin primality test, compute `n = p*q`, choose `e`, compute `d` via extended Euclidean algorithm, encrypt and decrypt with `BigInteger.modPow`. Demonstrates why small `e` (e.g., `e=3`) is insecure without padding.
**Core Concepts:** `BigInteger`, `BigInteger.probablePrime` vs. custom Miller-Rabin, `modPow`, extended Euclidean for modular inverse
**Difficulty:** Advanced
**Unique Challenge:** Implementing Miller-Rabin with enough deterministic witnesses for 512-bit numbers — use the 7 witnesses `{2, 3, 5, 7, 11, 13, 17}` which guarantee correctness for all `n < 3.3 × 10²⁴`. For larger numbers, use 40 random witnesses. Java's `BigInteger.probablePrime` uses 100 rounds of Miller-Rabin.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main generate
java -cp src Main encrypt "Hello RSA" public.key
java -cp src Main decrypt ciphertext.bin private.key
```

---

### 88. AES Implementation [RARE]
**Slug:** `aes`
**Files:**
```
src/
├── Main.java
├── AES.java
└── GF256.java
```
**Description:** AES-128 and AES-256 from scratch — SubBytes (S-box lookup), ShiftRows, MixColumns (GF(2⁸) arithmetic), AddRoundKey, and KeyExpansion. Implements ECB and CBC modes. No `javax.crypto`. Verifies output against NIST test vectors.
**Core Concepts:** `byte[]` 4×4 state array, S-box construction via GF(2⁸) inverse + affine transform, GF(2⁸) multiply via `xtime`, key schedule
**Difficulty:** Advanced
**Unique Challenge:** Implementing GF(2⁸) multiplication for MixColumns — `xtime(b) = (b << 1) ^ (0x1b if b has bit 7 set)`. MixColumns multiplies each byte by 2 and 3 in GF(2⁸) using `xtime` and XOR. Getting the irreducible polynomial `0x11b` right (modulo reduction) is the critical step.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --test-vectors
java -cp src Main encrypt input.bin output.bin --key 0x2b7e151628aed2a6abf7158809cf4f3c
```

---

### 89. Consistent Hashing Ring [RARE]
**Slug:** `consistent-hash`
**Files:**
```
src/
├── Main.java
└── ConsistentHash.java
```
**Description:** Consistent hashing ring with configurable virtual node count. Add/remove servers and route keys. Demonstrates the core property: adding 1 server remaps approximately K/N keys (not all K). Logs before/after remapping statistics for each add/remove operation.
**Core Concepts:** `TreeMap<Long, String>` sorted ring, `MessageDigest` (MD5/SHA-1) for hashing, `floorKey`/`ceilingKey` for routing, virtual nodes for load balance
**Difficulty:** Advanced
**Unique Challenge:** Demonstrating the K/N remapping property empirically — hash 100k test keys before and after adding a server, compare the routing results, count how many keys moved, and show it's approximately 100k/N (within statistical variance), proving the algorithm's core advantage.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --servers 5 --vn 150 --keys 100000
```

---

### 90. Ray Tracer (PPM output) [RARE]
**Slug:** `ray-tracer`
**Files:**
```
src/
├── Main.java
├── Scene.java
└── Vec3.java
```
**Description:** CPU ray tracer writing PPM image files. Supports spheres, infinite planes, point lights, Phong shading (ambient + diffuse + specular), hard shadows (shadow rays), mirror reflections (recursive, depth-limited), and 4× SSAA anti-aliasing.
**Core Concepts:** `Vec3` with dot/cross/normalize, ray-sphere intersection (quadratic formula), ray-plane intersection, recursive reflection, PPM binary format via `BufferedWriter`
**Difficulty:** Advanced
**Unique Challenge:** Implementing SSAA anti-aliasing — for each pixel, shoot 4 rays at sub-pixel offsets `(-0.25, -0.25)`, `(0.25, -0.25)`, `(-0.25, 0.25)`, `(0.25, 0.25)` in normalized image coordinates, average the 4 returned colors. This eliminates jagged edges on sphere silhouettes.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --preset spheres output.ppm
javac src/*.java && java -cp src Main scene.json output.ppm
```

---

### 91. Conway's Game of Life [CLASSIC]
**Slug:** `game-of-life`
**Files:**
```
src/
└── Main.java
```
**Description:** Console Game of Life with infinite board — store live cells as `long`-encoded `(x,y)` pairs in a `HashSet`. Includes preset patterns (Glider, R-pentomino, Gosper Glider Gun, Pulsar), step/pause/reset controls via keyboard, and a generation counter.
**Core Concepts:** `HashSet<Long>` with `encode(x,y) = ((long)x << 32) | (y & 0xFFFFFFFFL)`, neighbor counting via set lookups, `Thread.sleep` timing
**Difficulty:** Intermediate
**Unique Challenge:** Encoding `(x, y)` cell coordinates as a single `long` for O(1) `HashSet` contains — `(long)x << 32 | (y & 0xFFFFFFFFL)`. The `& 0xFFFFFFFFL` mask prevents sign-extension from corrupting the y-bits when y is negative.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main --pattern gosper-glider-gun
```

---

### 92. Sorting Visualizer (console) [RARE]
**Slug:** `sort-visualizer`
**Files:**
```
src/
├── Main.java
└── SortVisualizer.java
```
**Description:** Watch Bubble, Insertion, Selection, Merge, Quick, Heap, and Radix sort animate as ANSI-colored horizontal bar charts in the console — updated in-place with `\033[{n}A` cursor-up escape codes. Counts comparisons and swaps per algorithm.
**Core Concepts:** ANSI escape codes (`\033[nA` cursor up, `\033[2K` erase line), `Thread.sleep`, instrumented sort arrays, callback-based swap notification
**Difficulty:** Intermediate
**Unique Challenge:** Rendering the bar chart in-place without clearing the screen — after drawing N rows for the array, print `\033[{N}A` to move the cursor back up N lines before drawing the next frame. The cursor-up count must match the exact number of lines drawn.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --algo quicksort --size 30 --delay 50
java -cp src Main --compare-all --size 25
```

---

### 93. N-Queens Solver
**Slug:** `n-queens`
**Files:**
```
src/
└── Main.java
```
**Description:** Solve N-Queens using backtracking with three bitmasks (columns, left-diagonals, right-diagonals) for O(1) conflict checking. Find one solution, count all solutions, or print all boards. Compare bitmask approach vs. naive 2D board approach with timing.
**Core Concepts:** Backtracking, bitmask conflict detection, `Integer.numberOfTrailingZeros` to pick next safe column, recursion
**Difficulty:** Intermediate
**Unique Challenge:** Using bitmasks for O(1) conflict detection — `available = ~(cols | leftDiag | rightDiag) & allOnes`. Iterate available positions using `pos = available & -available` (isolates lowest set bit), place there, then `available &= available - 1` to clear it. Diagonals shift left/right at each row.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main 8 --all
java -cp src Main 12 --count
```

---

### 94. Sudoku Solver
**Slug:** `sudoku-solver`
**Files:**
```
src/
└── Main.java
```
**Description:** Solve any valid Sudoku using backtracking with bitmask-based O(1) valid-move lookup per cell. Constraint propagation (naked singles, hidden singles) eliminates most backtracking for easy/medium puzzles. Reads 81-char puzzle strings.
**Core Concepts:** Backtracking, three `int[]` bitmask arrays (row/col/box), `Integer.numberOfTrailingZeros` for next candidate, naked-single propagation
**Difficulty:** Intermediate
**Unique Challenge:** Using a bitmask per row/column/box — each is a 9-bit int where bit `k` means digit `k+1` is still available. Valid moves for cell `(r,c)` are `rowBits[r] & colBits[c] & boxBits[box]`. Picking the next digit is `Integer.numberOfTrailingZeros(available)`.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
```

---

### 95. Blackjack Game [CLASSIC]
**Slug:** `blackjack`
**Files:**
```
src/
├── Main.java
└── BlackjackEngine.java
```
**Description:** Casino-rules Blackjack with split pairs, double down, insurance bet, configurable multi-deck shoe (1–8 decks), and a Hi-Lo card counting hint system that tracks running count and suggests optimal bet sizing. All card values use immutable `record` types.
**Core Concepts:** `enum Rank`/`Suit`, `record Card`, `Collections.shuffle`, state machine per hand, Ace soft/hard tracking, Hi-Lo count tracking
**Difficulty:** Intermediate
**Unique Challenge:** Computing hand value with multiple Aces — iterate through all Aces, count each as 11, then for each Ace check if the total exceeds 21 and drop that Ace to 1. The key insight: you only ever need at most one Ace to be 11 (a second 11 would bust even without other cards).
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
java -cp src Main --decks 6 --counting
```

---

### 96. Wordle Clone (console)
**Slug:** `wordle`
**Files:**
```
src/
└── Main.java
```
**Description:** Wordle in the console with ANSI color feedback — green (correct position), yellow (wrong position), gray (not in word). Handles duplicate-letter edge cases with a two-pass algorithm. Daily-word mode seeds from today's `LocalDate`. Accepts a custom word list file.
**Core Concepts:** `String`, `char[]`, `HashMap<Character, Integer>` for duplicate-letter tracking, `LocalDate` for daily seed, ANSI escape codes
**Difficulty:** Intermediate
**Unique Challenge:** Correct yellow/gray logic for duplicate letters — first pass: mark all greens and decrement the answer's letter frequency map. Second pass: a non-green letter is yellow only if the answer's remaining frequency for that letter is > 0, otherwise gray. This prevents over-awarding yellows.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main
java -cp src Main --daily
java -cp src Main --wordlist custom.txt
```

---

### 97. Battleship Game
**Slug:** `battleship`
**Files:**
```
src/
├── Main.java
├── Board.java
└── Player.java
```
**Description:** Two-player or vs-AI Battleship on a 10×10 grid. Ships: Carrier (5), Battleship (4), Cruiser (3), Submarine (3), Destroyer (2). AI uses a probability density map — after each hit, recalculates which cells most likely extend the sunk ship's axis.
**Core Concepts:** OOP, `enum ShipType`, 2D `char[][]` grid, probability density map (`int[][]`), hunt/target/destroy AI state machine
**Difficulty:** Intermediate
**Unique Challenge:** Building the AI probability heat map — for every unsunk ship, count how many valid placements (horizontal and vertical, non-overlapping with known misses and sinks) include each cell. The cell with the highest count is the best next shot.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --mode vs-ai
java -cp src Main --mode two-player
```

---

### 98. Text-Based RPG
**Slug:** `text-rpg`
**Files:**
```
src/
├── Main.java
├── GameEngine.java
└── Entity.java
```
**Description:** Dungeon crawler with a room graph, inventory system, turn-based combat (with armor, dodge, and critical hits), XP and leveling, branching dialogue trees, and save/load serialized to a plain-text format (no JSON library). All game entities use integer ID references.
**Core Concepts:** OOP, state machine, hand-written serialization (key=value format), `HashMap<Integer, Room>` world graph, ID-based entity references
**Difficulty:** Intermediate
**Unique Challenge:** Serializing the complete game state to a plain text format (no JSON library) — each entity is serialized as a block of `key=value` lines separated by blank lines, then deserialized by splitting on `=` and resolving ID references after all entities are loaded.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
java -cp src Main --load savegame.sav
```

---

### 99. Mini ORM (JDBC + SQLite) [RARE]
**Slug:** `mini-orm`
**Files:**
```
src/
├── Main.java
├── ORM.java
└── Field.java
```
**Description:** Annotation-driven ORM backed by JDBC and SQLite. Define models with `@Table`, `@Column`, `@Id`, `@ForeignKey`. Auto-generates `CREATE TABLE`, `INSERT`, `SELECT`, `UPDATE`, `DELETE` SQL. Maps `ResultSet` rows back to Java objects via reflection. NOTE: Requires `sqlite-jdbc.jar` on the classpath — the one allowed external JAR.
**Core Concepts:** `java.lang.reflect`, `@interface` annotations, `java.sql.Connection`/`PreparedStatement`/`ResultSet`, SQL generation, `Field.setAccessible(true)`
**Difficulty:** Advanced
**Unique Challenge:** Generically mapping `ResultSet` columns back to Java field types — use `field.getType()` to dispatch: `String` → `getString`, `int`/`Integer` → `getInt`, `long`/`Long` → `getLong`, `double`/`Double` → `getDouble`, `boolean`/`Boolean` → `getBoolean`. Handle null values by checking `wasNull()`.
**Compile & Run:**
```bash
javac -cp sqlite-jdbc.jar src/*.java
java -cp sqlite-jdbc.jar:src Main
```

---

### 100. WebSocket Server (raw) [RARE]
**Slug:** `websocket-server`
**Files:**
```
src/
├── Main.java
├── WebSocketServer.java
└── WebSocketFrame.java
```
**Description:** RFC 6455 WebSocket server from scratch. Handles the HTTP upgrade handshake (SHA-1 key hash + Base64 accept key), parses binary frames (7-bit / 16-bit / 64-bit payload length), unmasks client-to-server frames, encodes server frames, and implements ping/pong keepalive.
**Core Concepts:** `MessageDigest` SHA-1, `Base64.getEncoder`, `DataInputStream` for frame parsing, masking XOR with 4-byte key, `select`-equivalent via `Selector`/`SelectionKey`
**Difficulty:** Advanced
**Unique Challenge:** Parsing WebSocket frame length correctly — the first 7 bits of byte 2 encode length as: `0–125` (literal), `126` (next 2 bytes are uint16 length), `127` (next 8 bytes are int64 length). Each case requires a different `DataInputStream.readShort`/`readLong` call.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --port 8765
```

---

### 101. Plugin System (ClassLoader) [RARE]
**Slug:** `plugin-system`
**Files:**
```
src/
├── Main.java
├── PluginManager.java
└── Plugin.java
```
**Description:** Runtime plugin system using `URLClassLoader` — drop a `.jar` or `.java` (compiled) file into `plugins/`, and the manager auto-discovers and loads any class implementing the `Plugin` interface. Each plugin gets its own `URLClassLoader` for isolation. Supports hot-reload.
**Core Concepts:** `URLClassLoader`, `ServiceLoader`, `interface Plugin`, `Class.forName`, classloader isolation, `URLClassLoader.close()` for unloading
**Difficulty:** Advanced
**Unique Challenge:** Isolating each plugin in its own `URLClassLoader` so plugins cannot see each other's classes — and unloading a plugin by calling `URLClassLoader.close()` on its loader and nulling all references to it, allowing GC to collect its classes.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 102. Event Sourcing Engine [RARE]
**Slug:** `event-sourcing`
**Files:**
```
src/
├── Main.java
├── EventStore.java
└── Aggregate.java
```
**Description:** Event sourcing framework where all state changes are stored as immutable domain events in an append-only log. Aggregate state is rebuilt by replaying events from the log. Implements snapshot optimization — periodically checkpoint aggregate state to skip full replay.
**Core Concepts:** Append-only `RandomAccessFile` event log, event serialization (hand-written), aggregate replay, snapshot + offset tracking
**Difficulty:** Advanced
**Unique Challenge:** Implementing snapshot optimization — every N events, serialize the aggregate's current state and its event offset to a snapshot file. On startup, load the latest snapshot and replay only events after its offset, not from the beginning.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 103. CQRS Framework (mini) [RARE]
**Slug:** `cqrs`
**Files:**
```
src/
├── Main.java
├── CommandBus.java
├── QueryBus.java
└── Handler.java
```
**Description:** Command Query Responsibility Segregation — separate `CommandBus` and `QueryBus` dispatch requests to registered handlers. Auto-registration discovers handlers via reflection by scanning for classes implementing `CommandHandler<C>` or `QueryHandler<Q, R>` and extracting the generic type parameter `C`/`Q`.
**Core Concepts:** `HashMap<Class<?>, Object>` handler registry, `java.lang.reflect.ParameterizedType`, generic type extraction, mediator pattern
**Difficulty:** Advanced
**Unique Challenge:** Extracting the generic type parameter from a handler class at runtime — `((ParameterizedType) handler.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0]` gives the `Class<C>` to use as the `CommandBus` registry key. This is type erasure working against you.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 104. Fibonacci via Matrix Exponentiation [RARE]
**Slug:** `matrix-exp-fib`
**Files:**
```
src/
└── Main.java
```
**Description:** Compute Fibonacci(n) in O(log n) using `[[1,1],[1,0]]^n` matrix exponentiation with `BigInteger`. Benchmark against iterative, memoized, and naive-recursive approaches on Fib(10000). Display the number of digits in the result.
**Core Concepts:** `BigInteger`, 2×2 `BigInteger[][]` matrix multiply, fast power via repeated squaring, `System.nanoTime` benchmark
**Difficulty:** Intermediate
**Unique Challenge:** Implementing 2×2 `BigInteger` matrix multiply correctly — 4 multiplications and 2 additions per entry, being careful not to use intermediate results from the same matrix (compute into a new `result[][]` not in-place). Fast-power squares the matrix and multiplies by the base matrix on odd exponent bits.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main 10000
java -cp src Main --benchmark 100
```

---

### 105. Fast Fourier Transform [RARE]
**Slug:** `fft`
**Files:**
```
src/
├── Main.java
├── FFT.java
└── Complex.java
```
**Description:** Cooley-Tukey radix-2 DIT FFT and IFFT — compute DFT in O(n log n) instead of O(n²). Applications: polynomial multiplication, convolution, and signal frequency analysis. Includes a DFT implementation for correctness verification.
**Core Concepts:** `Complex` class with add/multiply, bit-reversal permutation, butterfly operation, twiddle factors (`e^(-2πi k/n)`), divide-and-conquer recursion
**Difficulty:** Advanced
**Unique Challenge:** Implementing the bit-reversal permutation that reorders input samples before the butterfly passes — `reversed = Integer.reverse(i) >>> (32 - log2(n))`. Off-by-one errors in the shift amount produce a permutation that looks correct for small n but fails for larger n.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --demo convolution
java -cp src Main --demo frequency-analysis
```

---

### 106. Quadtree [RARE]
**Slug:** `quadtree`
**Files:**
```
src/
├── Main.java
└── Quadtree.java
```
**Description:** Point-region Quadtree with insert, point lookup, range query (return all points in a rectangle), and nearest-neighbor search. Visualizes the tree structure as ASCII art showing each quadrant's boundaries. Benchmarks against brute-force for range queries.
**Core Concepts:** Recursive subdivision into NW/NE/SW/SE quadrants, axis-aligned bounding box, range query pruning, nearest-neighbor with backtracking
**Difficulty:** Intermediate
**Unique Challenge:** Implementing range query pruning — only recurse into quadrants whose bounding box overlaps the query rectangle. Use `AABB.intersects(queryRect)` before recursing, reducing average case from O(n) to O(√n + k) where k is the result count.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
```

---

### 107. K-D Tree [RARE]
**Slug:** `kd-tree`
**Files:**
```
src/
├── Main.java
└── KDTree.java
```
**Description:** k-dimensional tree for exact nearest-neighbor search and range queries on k-dimensional points. Alternates splitting dimension by depth (`depth % k`). Implements nearest-neighbor with hypersphere backtracking: after descending one branch, check if the other branch's bounding hypersphere could contain a closer point.
**Core Concepts:** Recursive BST on alternating dimensions, `PriorityQueue` for k-NN, hypersphere-hyperplane intersection test, `double[]` k-dimensional points
**Difficulty:** Advanced
**Unique Challenge:** The hypersphere backtracking test — after searching the near branch, only search the far branch if `Math.pow(splitAxisDistance, 2) < currentBestDistSq`. Without this check, nearest-neighbor is O(n) in the worst case; with it, it's O(log n) for balanced trees with uniform data.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --dims 3 --points 10000
```

---

### 108. Steganography (LSB in BMP) [RARE]
**Slug:** `steganography`
**Files:**
```
src/
├── Main.java
└── Steg.java
```
**Description:** Hide and extract secret messages in 24-bit and 32-bit BMP images by replacing the LSB of each color channel byte. Parse the BMP file header manually to find the pixel data offset. Include a message-length header (first 32 bits of the LSB stream) for clean extraction.
**Core Concepts:** `FileInputStream`/`RandomAccessFile`, `ByteBuffer`, BMP `BITMAPFILEHEADER`/`BITMAPINFOHEADER` parsing, bitwise `& 0xFE | bit`, LSB stream
**Difficulty:** Advanced
**Unique Challenge:** Parsing the BMP header to find `bfOffBits` — a `DWORD` at byte offset 10 in the file header — then seeking to that offset before reading pixel data. The offset differs between 24-bit BMPs (usually 54) and 32-bit BMPs (usually 122, with a color table). Using the offset from the header rather than hard-coding it is the correct approach.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main hide input.bmp "Secret message" output.bmp
java -cp src Main extract output.bmp
```

---

### 109. Arithmetic Coding [RARE]
**Slug:** `arithmetic-coding`
**Files:**
```
src/
├── Main.java
└── ArithmeticCodec.java
```
**Description:** Arithmetic coding encoder and decoder — compress a message to a single rational number in [0,1) using cumulative symbol probabilities. Implements integer renormalization (underflow handling) to avoid precision loss during long encodings. Compare compression ratio to Huffman.
**Core Concepts:** Cumulative frequency model, interval narrowing, integer renormalization (E1/E2/E3 scaling), `long` arithmetic for fixed-point intervals, bitwise output
**Difficulty:** Advanced
**Unique Challenge:** Implementing E3 (underflow) renormalization — when the interval straddles the midpoint (neither entirely in the lower nor upper half), scale the interval around the midpoint and defer one output bit. Tracking the number of pending E3 bits and flushing them correctly is the hardest part of arithmetic coding.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main compress input.txt output.ac
java -cp src Main decompress output.ac recovered.txt
```

---

### 110. Genetic Algorithm (TSP) [RARE]
**Slug:** `genetic-algo`
**Files:**
```
src/
├── Main.java
└── GeneticAlgorithm.java
```
**Description:** Solve the Travelling Salesman Problem with a Genetic Algorithm — tournament selection, Partially Mapped Crossover (PMX) for valid tour permutations, 2-opt mutation, and elitism. Print the best tour length each generation. Optionally load a city coordinates file.
**Core Concepts:** `int[]` permutation chromosomes, PMX crossover, 2-opt swap mutation, tournament selection, fitness = 1/tourLength, `ThreadLocalRandom`
**Difficulty:** Advanced
**Unique Challenge:** Implementing PMX crossover — select a crossover segment, copy it from parent 1 to offspring at the same positions; fill remaining positions from parent 2, but if a city is already in the offspring (from the segment), substitute its mapped partner from the position-mapping defined by the segment.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --cities 20 --pop 200 --generations 2000
java -cp src Main --file cities.csv --generations 5000
```

---

### 111. Simulated Annealing [RARE]
**Slug:** `sim-annealing`
**Files:**
```
src/
├── Main.java
└── SimulatedAnnealing.java
```
**Description:** Solve TSP and job scheduling via simulated annealing. Acceptance probability `P = e^(-ΔE/T)` occasionally accepts worse solutions to escape local optima. Compare geometric, linear, and logarithmic cooling schedules. Print the temperature-vs-best-cost curve.
**Core Concepts:** Acceptance probability formula, cooling schedule strategies, `Math.random()` for acceptance, 2-opt neighborhood, `double` temperature tracking
**Difficulty:** Advanced
**Unique Challenge:** Tuning the cooling schedule — the initial temperature must be high enough that the acceptance probability for the worst expected ΔE starts near 0.8, and the final temperature must be near 0. Setting `T_init = -ΔE_avg / Math.log(0.8)` using a few random moves to estimate `ΔE_avg` is a principled starting point.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --cities 30 --cooling geometric --alpha 0.995
```

---

### 112. Neural Network (no libraries) [RARE]
**Slug:** `neural-net`
**Files:**
```
src/
├── Main.java
├── NeuralNetwork.java
└── Layer.java
```
**Description:** Fully-connected neural network with configurable layer sizes, sigmoid/ReLU/tanh activations, He/Xavier initialization, mini-batch SGD, L2 regularization, and backpropagation — all using `double[][]` matrix operations implemented from scratch. Trains on XOR and a small dataset.
**Core Concepts:** `double[][]` matrix multiply, chain rule backprop, activation function derivatives, He initialization, `Math.exp`, `Math.tanh`
**Difficulty:** Advanced
**Unique Challenge:** Implementing the backward pass correctly across multiple hidden layers — the gradient at layer `l` is `dA[l] = W[l+1].T × dZ[l+1]`; then `dZ[l] = dA[l] * g'(Z[l])` element-wise. Transposing `W[l+1]` and getting the matrix dimension ordering right is the error-prone step.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --task xor
java -cp src Main --task spiral --layers 2,32,32,2
```

---

### 113. Monte Carlo Simulator
**Slug:** `monte-carlo`
**Files:**
```
src/
└── Main.java
```
**Description:** Three Monte Carlo experiments: (1) Estimate π using the circle-in-square method with antithetic variates variance reduction, (2) Simulate 1,000,000 Monty Hall games proving the 2/3 switching win rate, (3) Price European call options and compare to the Black-Scholes analytical result.
**Core Concepts:** `ThreadLocalRandom`, `Math`, convergence statistics, antithetic variates, Black-Scholes formula components
**Difficulty:** Intermediate
**Unique Challenge:** Implementing antithetic variates for π estimation — for each uniform random point `(u, v)`, also test the antithetic point `(1-u, 1-v)`. The two samples are negatively correlated, halving the estimator variance and achieving the same accuracy with √2 fewer samples.
**Compile & Run:**
```bash
javac src/Main.java && java -cp src Main --task pi --samples 10000000
java -cp src Main --task monty-hall --trials 1000000
java -cp src Main --task options --S 100 --K 105 --T 1 --r 0.05 --sigma 0.2
```

---

### 114. Finite State Machine
**Slug:** `fsm`
**Files:**
```
src/
├── Main.java
├── FSM.java
└── State.java
```
**Description:** Generic FSM with typed states and events, guard predicates (`Predicate<Context>`), entry/exit action callbacks (`Runnable`), and event dispatch. Three demo machines: traffic light, vending machine, and the TCP connection state diagram (SYN_SENT, ESTABLISHED, FIN_WAIT, etc.).
**Core Concepts:** `Map<State, Map<Event, Transition>>`, `Predicate<Context>`, `Runnable` for entry/exit actions, `enum` states/events, generic context object
**Difficulty:** Intermediate
**Unique Challenge:** Using `Predicate<Context>` for guard conditions — `transition.when(ctx -> ctx.getBalance() >= ctx.getPrice())` — so transitions are conditionally fired based on runtime context rather than hard-coded logic, making the FSM definition declarative and testable.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --demo vending
java -cp src Main --demo tcp
```

---

### 115. CPU Scheduler Simulator [RARE]
**Slug:** `cpu-scheduler`
**Files:**
```
src/
├── Main.java
├── Scheduler.java
└── Process.java
```
**Description:** Simulate five CPU scheduling algorithms: FCFS, SJF (non-preemptive), SRTF (preemptive SJF), Round Robin (configurable quantum), and MLFQ (Multi-Level Feedback Queue with aging). Output a Gantt chart and per-process metrics: waiting time, turnaround time, response time.
**Core Concepts:** `PriorityQueue<Process>`, `ArrayDeque<Process>[]` MLFQ queues, time-sliced simulation loop, process state machine, Gantt rendering as char array
**Difficulty:** Intermediate
**Unique Challenge:** Implementing MLFQ correctly — processes start in queue 0 (highest priority, quantum 2); using a full quantum demotes to queue 1 (quantum 4), then queue 2 (FCFS). An aging mechanism scans all lower queues every 20 time units and promotes processes that have waited too long, preventing starvation.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --algo mlfq --quantum 4 --processes processes.json
java -cp src Main --compare-all --processes processes.json
```

---

### 116. Virtual Memory Simulator [RARE]
**Slug:** `virtual-memory`
**Files:**
```
src/
├── Main.java
├── VirtualMemory.java
└── PageTable.java
```
**Description:** Simulate page tables, TLB (Translation Lookaside Buffer), and four page replacement policies: FIFO, LRU, Optimal (Bélády), and Clock (second-chance). Measure page fault rates and TLB hit rates for each policy on the same reference string.
**Core Concepts:** `int[]` page frame array, `LinkedHashMap<Integer, Integer>` for LRU (access-order mode), TLB as small cache, Optimal algorithm lookahead
**Difficulty:** Advanced
**Unique Challenge:** Implementing Bélády's Optimal algorithm — when a page fault occurs, evict the page whose next use is farthest in the future (or never used again). This requires scanning the remaining reference string to find each loaded page's next occurrence, which is O(n) per fault.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main --frames 4 --policy lru
java -cp src Main --frames 4 --compare-all --references 7,0,1,2,0,3,0,4,2,3
```

---

### 117. Regex Engine (NFA) [RARE]
**Slug:** `regex-engine`
**Files:**
```
src/
├── Main.java
└── RegexEngine.java
```
**Description:** Regex engine supporting `.`, `*`, `+`, `?`, `|`, `()`, character classes `[a-z]`, and anchors `^`/`$`. Uses Thompson's algorithm to construct an NFA from the regex, then simulates the NFA via epsilon-closure BFS — O(nm) total where n = text length, m = pattern length.
**Core Concepts:** Thompson's NFA construction, epsilon-closure via BFS (`Set<State>`), NFA simulation (set of active states per character), `|` as split state
**Difficulty:** Advanced
**Unique Challenge:** Computing epsilon-closures correctly — `epsilonClosure(states)` returns all states reachable from `states` via ε-transitions using BFS. This must be called after every character input (not just at the start) because new ε-reachable states emerge after each transition.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main "a(b|c)*d" "abcbd"
java -cp src Main --match "[a-z]+" "hello world"
```

---

### 118. Async HTTP Client (NIO) [RARE]
**Slug:** `async-http`
**Files:**
```
src/
├── Main.java
└── AsyncHTTPClient.java
```
**Description:** Non-blocking HTTP/1.1 client built on Java NIO — `SocketChannel`, `Selector`, and `ByteBuffer`. Uses `CompletableFuture<HttpResponse>` as the result type. Supports concurrent requests on a single selector thread, HTTP/1.1 keep-alive connection pooling, and `Content-Length` based response body parsing.
**Core Concepts:** `SocketChannel`, `Selector`, `SelectionKey`, `ByteBuffer`, `CompletableFuture`, NIO event loop, connection state machine
**Difficulty:** Advanced
**Unique Challenge:** Implementing the NIO connection state machine correctly — register for `OP_CONNECT`, in the connect-ready handler call `finishConnect()`, then re-register for `OP_WRITE` to send the request; in the write-ready handler, switch to `OP_READ` after the request is fully sent; in the read-ready handler, accumulate response bytes and complete the `CompletableFuture`.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main https://example.com
java -cp src Main --concurrent 10 https://httpbin.org/get
```

---

### 119. Merkle Tree [RARE]
**Slug:** `merkle-tree`
**Files:**
```
src/
├── Main.java
└── MerkleTree.java
```
**Description:** Merkle tree for data integrity verification — build from a list of data blocks using SHA-256, generate an audit proof (minimal list of sibling hashes needed to recompute the root), verify a proof without the full tree, and demonstrate how tampering with one block invalidates exactly one path to the root.
**Core Concepts:** `MessageDigest` SHA-256, binary tree, audit proof generation (sibling path), proof verification (rehash up to root), `byte[]` hash comparison
**Difficulty:** Intermediate
**Unique Challenge:** Generating the audit proof — given a leaf index, walk from the leaf to the root; at each level, include the *sibling* hash (left sibling if the current node is right, right sibling if left), along with a direction bit. The verifier rehashes the leaf with each sibling in sequence to arrive at the root.
**Compile & Run:**
```bash
javac src/*.java && java -cp src Main
java -cp src Main --verify --leaf 3 --data "transaction data"
```

---

### 120. P2P File Sharing [RARE]
**Slug:** `p2p-sharing`
**Files:**
```
src/
├── Main.java
├── Peer.java
├── Tracker.java
└── PeerClient.java
```
**Description:** Decentralized file sharing where peers register files with a central tracker (file SHA-256 hash, chunk count, host:port). Downloading peers query the tracker, receive a seeder list, then download non-overlapping chunks in parallel from multiple seeders. Each chunk is SHA-256 verified before `RandomAccessFile` assembly.
**Core Concepts:** `ServerSocket`, `Thread`, `RandomAccessFile`, `MessageDigest` SHA-256, chunk bitmap (downloaded/in-progress/needed), parallel chunk download with `CountDownLatch`
**Difficulty:** Advanced
**Unique Challenge:** Coordinating parallel chunk downloads from multiple peers — maintain an `AtomicIntegerArray` chunk bitmap (0=needed, 1=in-progress, 2=done); each downloader thread CAS-sets its chunk from 0 to 1 before fetching, preventing two threads from downloading the same chunk. On completion, set to 2 and verify the SHA-256.
**Compile & Run:**
```bash
javac src/*.java
java -cp src Tracker --port 9000 &
java -cp src Main --share bigfile.zip --tracker localhost:9000 &
java -cp src Main --download <sha256hash> --tracker localhost:9000
```

---

## 🏆 Top 10 Most Impressive Java Projects for a Portfolio

> These projects demonstrate deep CS fundamentals, JVM internals knowledge, and systems engineering. Each one will generate genuine interest from senior engineers and technical interviewers.

| Rank | Project | Slug | Why It Impresses |
|---|---|---|---|
| 🥇 1 | Interpreter (tiny language) | `interpreter` | Shows you understand how programming languages work at the implementation level — lexer, parser, closures, environments |
| 🥈 2 | JVM Bytecode Reader + Disassembler | `jvm-reader` + `class-dis` | Deep JVM internals most Java developers have never seen — the `.class` file binary format |
| 🥉 3 | LSM-Tree Storage Engine | `lsm-tree` | Database storage internals — the architecture of LevelDB, Cassandra, and RocksDB |
| 4 | Raft Consensus | `raft` | Distributed systems engineering — the algorithm that powers etcd and CockroachDB |
| 5 | Lock-Free Queue (Michael-Scott) | `lock-free-queue` | Concurrency at the hardware memory model level — CAS, helping, and why locks aren't always needed |
| 6 | RSA from Scratch | `rsa` | Number theory + cryptography — `BigInteger`, Miller-Rabin, and modular arithmetic without any crypto library |
| 7 | Regex Engine (NFA) | `regex-engine` | Automata theory made concrete — Thompson's construction + epsilon-closure simulation |
| 8 | AES Implementation | `aes` | Cryptography at the byte level — GF(2⁸) arithmetic and the Rijndael algorithm |
| 9 | Compiler (expr lang) | `mini-compiler` | Code generation — the final boss of CS fundamentals |
| 10 | Reactive Streams | `reactive-streams` | Async/backpressure — the design behind Project Reactor and RxJava, implemented from scratch |

---

## 🗂 Projects by Difficulty Count

| Difficulty | Count | Notes |
|---|---|---|
| Beginner | 20 | Projects 1–20 |
| Intermediate | 62 | Projects 21–71, plus 73, 75, 91–98, 104, 106, 113–115, 119 |
| Advanced | 38 | Projects 50–51, 72, 74, 76–90, 99–103, 105, 107–112, 116–118, 120 |

## 🏷 Tagged Projects

| Tag | Count |
|---|---|
| [CLASSIC] | 6 (Number Guessing, RPS, AVL Tree, Conway's Life, Blackjack, LRU Cache) |
| [RARE] | 57 (projects most Java developers have never attempted) |

---

## 📋 Quick Reference — File Counts

| File Count | Example Projects |
|---|---|
| **1 file** (`Main.java`) | Number Guessing, RPS, Caesar Cipher, Roman Numerals, Palindrome, Fibonacci, Prime Sieve, Matrix Multiply, Levenshtein, LCS, Knapsack, Matrix Chain, Port Scanner, Tic-Tac-Toe, N-Queens, Sudoku, Conway's Life, Wordle, Monte Carlo, Fibonacci Matrix Exp |
| **2 files** | Bank Account, Grade Tracker, ATM, Shape Calc, Stack, Queue, ArrayList, Temp Converter, Payroll, Word Freq, BST, Graph, Dijkstra, LRU, Trie, Shunting-Yard, Event Bus, Observer, Command, Builder, Proxy, Strategy, Maze, Memory Alloc, Thread Pool, Prod-Consumer, RW Lock, Semaphore, Merge Sort, Bloom, LFU, Interval, Segment, Fenwick, Union-Find, Kruskal, A*, KMP, Rabin-Karp, Mini HTTP, FTP, SMTP, Port Scanner, Exception, Object Pool, Rate Limiter, Game of Life, Sort Vis, Battleship, FSM, CPU Sched, Merkle |
| **3 files** | Library Tracker, Inventory, Linked List, AVL, Skip List, Heap, Producer-Consumer, JSON Ser, Huffman, Mini HTTP, TCP Chat, Downloader, Lock-Free Stack, Lock-Free Queue, Bloom Filter, Gen Algo, Sim Ann, Neural Net, RSA, Consistent Hash, Ray Tracer, Game of Life, Blackjack, RPG, Actor Model, Event Sourcing, CQRS, Quadtree, Steganography, Virtual Memory, Arithmetic Coding, P2P, WebSocket |
| **4 files** | B-Tree, Red-Black, Distributed KV, Raft, LSM-Tree, JVM Reader+Dis, Interpreter, Compiler, AES, P2P Sharing, Regex Engine, Async HTTP |

---

*Every project = plain Java + `javac` + `java`. No Maven. No Gradle. No Spring.*
*Build them, understand them, explain them — that's what makes the difference.*
