# squell

Yet another SQL query builder for Java — small and fluent, like plenty of others out there. What's different here: it's held to actual [Elegant Objects](https://www.elegantobjects.org/) rules, not just "OOP-flavored." No `null` as an internal sentinel — the Null Object pattern is used throughout. No mutable builders or setters — every step returns a new, immutable step, enforced structurally rather than by convention. Constructor injection everywhere instead of the framework deciding what gets instantiated.

## Install

```gradle
dependencies {
    implementation 'io.github.happyduke96:squell:0.3.0'
}
```

**Upgrading from 0.2.x:** `@GeneratedValue` accessors return the plain type (`long id()`), not `Optional<Long>` — `Optional<...>` no longer compiles. Drop the `Optional` and the `.orElseThrow()` calls; compare ids with `a.id() == b.id()`. Everything else in 0.3.0 is additive.

## Examples

Short snippets; for a full runnable project see [squell-example](https://github.com/happyDuke96/squell-example). `posts`, `accounts`, ... are instances of the generated `XxxTable` (`new PostTable()`), assumed created.

### Entity

```java
@Entity("posts")
public interface Post {
    @Id
    UUID id();
    UUID authorId();
    String title();
    Status status();

    enum Status { DRAFT, PUBLISHED }
}
```

The annotation processor generates `PostTable` at compile time — no reflection. `posts.title()` is a typed `Field<String>` for conditions and column references.

### Client

The constructor takes a plain `javax.sql.DataSource`, so any pool works:

```java
PostgresClient client = new PostgresClient(dataSource);

// fails fast at startup if a table or column is missing or mismatched
client.validateSchemaOrThrow(new PostTable(), new AccountTable());
```

### CRUD

```java
client.insert(posts)
        .values(posts.create(id, authorId, "Zero-reflection SQL", Post.Status.DRAFT))
        .execute();

List<Post> published = client.select(posts)
        .where(posts.status().eq(Post.Status.PUBLISHED))
        .orderBy(posts.title())
        .fetch();

client.update(posts)
        .set(posts.status(), Post.Status.PUBLISHED)
        .where(posts.id().eq(id))
        .execute();

client.delete(posts)
        .where(posts.id().eq(id))
        .execute();
```

### Generated values

```java
@Entity("tickets")
public interface Ticket {
    @Id
    @GeneratedValue
    long id();
    String label();
}

Ticket draft = tickets.create("first contact");   // no id argument; draft.id() == 0
Ticket saved = client.insert(tickets)
        .values(draft)
        .executeAndReturn();                      // saved.id() is database-assigned
```

`@GeneratedValue` marks any database-filled column — identity, default, stored or trigger-maintained. `create()` omits it; the accessor is the plain type, `0`/`false`/`null` until read back. `executeAndReturn()` needs `RETURNING`, so not on MySQL.

### Join

```java
AliasedTable<Author> a = authors.as("a");
AliasedTable<Post> p = posts.as("p");

List<Author> withPublished = client.select(a)
        .join(p)
        .on(a.field(authors.id()).eqField(p.field(posts.authorId())))
        .where(p.field(posts.status()).eq(Post.Status.PUBLISHED))
        .fetch(a);
```

`fetch(a)` returns the aliased side; pass a `RowMapper<R>` for a combined shape. `select`/`join` always take an `AliasedTable<?>`, so self-joins need no special case.

### Transactions

```java
client.transactionWithoutResult(IsolationLevel.SERIALIZABLE, tx -> {
    tx.update(accounts)
            .set(accounts.balance(), newBalance)
            .where(accounts.id().eq(id))
            .execute();

    // REQUIRES_NEW: own connection, commits even if the outer transaction rolls back
    tx.transactionWithoutResultRequiringNew(other -> {
        other.update(accounts)
                .set(accounts.pendingBalance(), 0)
                .where(accounts.id().eq(otherId))
                .execute();
    });

    // NESTED: a SAVEPOINT, rolls back alone
    tx.transactionWithoutResultNested(inner -> {
        inner.update(accounts)
                .set(accounts.pendingBalance(), 0)
                .where(accounts.id().eq(id))
                .execute();
    });
});

// retries on a deadlock or a forUpdateNoWait() conflict
int affected = client.transactionWithRetry(5, Duration.ofMillis(100), tx -> {
    return tx.update(accounts)
            .set(accounts.balance(), newBalance)
            .where(accounts.id().eq(id))
            .execute();
});
```

### Locking

```java
// SELECT ... FOR UPDATE NOWAIT: fails fast instead of blocking
List<Item> locked = client.transaction(tx -> {
    return tx.select(items)
            .where(items.id().eq(id))
            .forUpdateNoWait()
            .fetch();
});

// ON CONFLICT DO UPDATE SET quantity = quantity + ?: atomic counter, no read-then-write race
client.insert(items)
        .values(items.create(UUID.randomUUID(), "SKU-1", 1))
        .onConflict(items.sku())
        .doUpdateIncrementing(items.quantity(), 1);
```

`forUpdateSkipLocked()` and `forShare()` have the same shape. MySQL supports all four, SQLite none (its `select()` returns a plain `SelectStep<T>`, so they don't compile).

### Optimistic locking

```java
@Entity("documents")
public interface Document {
    @Id
    UUID id();
    @Version
    int version();
    String title();
}

Document loaded = client.select(documents)
        .where(documents.id().eq(id))
        .fetchOne()
        .orElseThrow();

// UPDATE ... SET title = ?, version = version + 1 WHERE id = ? AND version = ?
client.update(documents)
        .set(documents.title(), "final")
        .where(documents.id().eq(id))
        .whereVersion(documents.version(), loaded.version())
        .execute();
```

If the row changed since it was read, `execute()` throws `OptimisticLockException` instead of updating nothing. `@Version` is `int`/`long`/`Integer`/`Long`, one per entity, starts at `0`, and `whereVersion` needs a `set(...)`.

### Pagination

```java
// offset: content and total count in one call
Page<Post> page = client.select(posts)
        .orderBy(posts.title())
        .fetchPage(0, 20);

// keyset: no COUNT(*), no OFFSET
Slice<Post> slice = client.select(posts)
        .where(posts.id().gt(lastSeenId))
        .orderBy(posts.id())
        .fetchSlice(20);
```

### Related rows without N+1

```java
List<Post> loaded = client.select(posts)
        .fetch();

// one WHERE postId IN (...) query for all of them
Map<UUID, List<Comment>> byPost = client.fetchRelated(
        loaded.stream().map(Post::id).toList(),
        comments,
        comments.postId(),
        Comment::postId);
```

A post without comments is absent from the map; an empty id list skips the query.

### Per-transaction find cache

```java
client.transactionWithoutResult(tx -> {
    Document first = tx.findCached(documents, id).orElseThrow();
    Document second = tx.findCached(documents, id).orElseThrow();   // same instance, no second query

    tx.update(documents)                                            // evicts the cached row
            .set(documents.title(), "updated")
            .where(documents.id().eq(id))
            .execute();
});
```

Keyed by (table, id) for the transaction's lifetime, misses included; needs an `@Id`. `update`/`delete` on the same transaction evict; `insert` and raw `sql` don't, so call `tx.forget(table, id)` after them.

### Aggregates

```java
Optional<Integer> mostUpvoted = client.select(comments)
        .max(comments.upvotes());

Map<String, Double> avgByCategory = client.select(comments)
        .avgBy(comments.category(), comments.upvotes());
```

`min`/`max`/`sum`/`avg`/`countBy` run ungrouped; the `...By(field)` variants group, with an optional `Condition having` overload.

### Raw SQL

```java
List<Post> found = client.sql("SELECT * FROM posts WHERE id = ?", id)
        .result(posts);

int updated = client.sql("UPDATE posts SET status = ? WHERE id = ?", "PUBLISHED", id)
        .execute();
```

Params bind positionally to `?`; the SQL text isn't escaped, so build it only from literals.

### Row mappers

A `RowMapper<T>` is a functional interface, `ResultSet` in, `T` out. Raw SQL (`result`, `resultOne`) and joins (`fetch`, `fetchOne`) accept one, and you can write your own.

```java
// ready-made: a generated Table is a RowMapper, it reads whole rows by column name
List<Post> all = client.sql("SELECT * FROM posts")
        .result(posts);

// a lambda for any projection
List<String> titles = client.sql("SELECT title FROM posts")
        .result(row -> row.getString("title"));

// your own class, reusable and testable
record PostStats(UUID id, String title, long comments) {
    static final RowMapper<PostStats> MAPPER = row -> new PostStats(
            row.getObject("id", UUID.class),
            row.getString("title"),
            row.getLong("comments"));
}

List<PostStats> stats = client.sql("""
                SELECT p.id, p.title, count(c.id) AS comments
                FROM posts p LEFT JOIN comments c ON c.postId = p.id
                GROUP BY p.id, p.title
                """)
        .result(PostStats.MAPPER);

// compose: reuse the ready-made mapper inside your own
record PostWithAuthor(Post post, String authorName) {
}

RowMapper<PostWithAuthor> withAuthor = row -> new PostWithAuthor(
        posts.map(row),
        row.getString("author_name"));

Optional<PostWithAuthor> found = client.sql("""
                SELECT p.*, a.fullName AS author_name
                FROM posts p JOIN authors a ON a.id = p.authorId
                WHERE p.id = ?
                """, id)
        .resultOne(withAuthor);
```

`resultOne` returns empty for no rows and throws `IllegalStateException` for more than one. A join takes the same mappers: `fetch(a)` is the ready-made one for the aliased side, and `fetch(row -> ...)` reads columns by position in `SELECT *` order across both tables.

### ConditionBuilder

```java
Condition filter = new ConditionBuilder()
        .add(titleFilter, posts.title()::ilike)
        .add(statusFilter, posts.status()::eq)
        .add(minUpvotes, comments.upvotes()::ge)
        .buildAnd();

List<Post> found = client.select(posts)
        .where(filter)
        .fetch();
```

`add(value, fieldRef)` skips a `null`, blank, empty or non-positive value — no `if` branches for optional filters.

## Testing

- `./gradlew test` — fast, no external dependencies, embedded H2.
- `./gradlew integrationTest` — real Postgres and MySQL ([Testcontainers](https://testcontainers.com/), needs Docker) and real SQLite. Covers what H2 can't: `RETURNING` / `ON CONFLICT` and the Postgres/MySQL-only converters.
