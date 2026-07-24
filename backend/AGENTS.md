# backend/AGENTS.md

These instructions apply to all work under `backend/`.

## Architecture

Use package-by-feature under `com.saigonplantravel.backend`.

Expected feature layout:

```text
feature/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
│   ├── request/
│   └── response/
└── mapper/
```

Avoid generic horizontal root packages unless code is truly shared.

## Persistence

- Flyway is migration-first.
- Never rely on Hibernate to create or alter production schema.
- Keep `spring.jpa.hibernate.ddl-auto=validate`.
- Do not edit an already-applied shared migration; create a new migration.
- Repositories normally extend `JpaRepository`.
- Do not create `RepositoryImpl` for ordinary CRUD.
- Use custom repository implementations only when justified.
- Use `BigDecimal` for monetary fields.
- Consider N+1 queries and lazy-loading behavior.

## API

- Base path: `/api/v1`.
- Controllers return DTOs, never entities.
- Use Jakarta Bean Validation.
- Keep controllers thin.
- Business rules belong in services or domain methods.
- Use consistent errors.
- Read methods normally use `@Transactional(readOnly = true)`.

## Dependency injection

- Prefer constructor injection.
- `@RequiredArgsConstructor` is allowed.
- Avoid field injection.

## Lombok

Entity:

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
```

Avoid on entities:

- `@Data`
- class-level `@Setter`
- uncontrolled `@ToString`
- naive `@EqualsAndHashCode`

DTO:

- Prefer Java records.

## Testing

Add the smallest useful tests:

- service unit tests for business rules;
- repository tests for non-trivial queries;
- controller/integration tests for API contracts.

Run:

```bash
./mvnw test
```

Report tests that could not run and the exact reason.
