# Database migrations (Flyway)

The application uses two schema strategies, chosen by Spring profile:

- **default / `local`** — Hibernate manages the schema (`ddl-auto=update` on Postgres,
  `create-drop` on H2). Zero setup; ideal for development and the demo.
- **`prod`** — Hibernate only **validates** the schema; **Flyway owns it**. This is the
  enterprise path: schema as versioned, reviewed, repeatable SQL.

## Why no checked-in baseline yet

The baseline must exactly match the JPA model. The reliable, drift-free way to produce it is
to generate it from a known-good schema rather than hand-write it. Generate `V1__baseline.sql`
once, review it, commit it, then run with `SPRING_PROFILES_ACTIVE=prod`.

## Generate the baseline (one time)

1. Start a throwaway Postgres and run the app in the default profile so Hibernate builds the schema:
   ```bash
   SPRING_PROFILES_ACTIVE=default JPA_DDL_AUTO=create \
   DB_URL=jdbc:postgresql://localhost:5432/sentinel mvn spring-boot:run
   ```
2. Dump the schema (no data) into the migrations folder:
   ```bash
   pg_dump --schema-only --no-owner --no-privileges \
     -d "postgresql://sentinel:sentinel@localhost:5432/sentinel" \
     > src/main/resources/db/migration/V1__baseline.sql
   ```
3. Review the SQL, then run in prod mode (Flyway applies it, Hibernate validates):
   ```bash
   SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
   ```

## Day-2

Every schema change after the baseline is a new, immutable, forward-only file:
`V2__add_x.sql`, `V3__index_y.sql`, … Never edit an applied migration. CI should run
`flyway validate` against a fresh database on every PR.
