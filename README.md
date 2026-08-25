# squell

Yet another SQL query builder for Java — small and fluent, like plenty of others out there. What's different here: it's held to actual [Elegant Objects](https://www.elegantobjects.org/) rules, not just "OOP-flavored." No static entry points. No `null` as an internal sentinel — the Null Object pattern is used throughout. No mutable builders or setters — every step returns a new, immutable step. Constructor injection everywhere instead of the framework deciding what gets instantiated.

## Install

```gradle
dependencies {
    implementation 'io.github.happyduke96:squell:0.1.0'
}
```

## Testing

Two tiers:

- `./gradlew test` — fast, no external dependencies, runs entirely against embedded H2.
- `./gradlew integrationTest` — against real Postgres and MySQL (via [Testcontainers](https://testcontainers.com/), needs Docker) and real SQLite (via `sqlite-jdbc`, no container). Covers what H2 can't: `RETURNING` / `ON CONFLICT`, and the Postgres/MySQL-only converters through their actual column types instead of hand-built wire objects.

