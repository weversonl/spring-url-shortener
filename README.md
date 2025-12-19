# Spring URL Shortener

A URL shortening application built with Spring Boot, Apache Cassandra (persistence), and Redis (cache).

Purpose
I built this application as an exercise to integrate with a multi-node NoSQL Cassandra and a Redis cluster so I could develop a system with low downtime and high availability that can scale.

Quick summary
- Project: spring-url-shortener
- Language: Java 17
- Build: Maven
- Primary database: Apache Cassandra
- Cache: Redis (cluster)
- Profiles: `dev` (local) and `prod` (for Docker Compose)

What this README contains
- [x] Prerequisites
- [x] How to build (local)
- [x] How to run locally (jar / mvn)
- [x] How to run with Docker Compose (infrastructure + API)
- [x] API endpoints and request/response examples
- [x] Environment variables and profiles
- [x] Tests

Prerequisites
- JDK 17 (matches the `java.version` in the project)
- Maven (or use the wrapper `./mvnw`)
- Docker and docker-compose (only required for running via Docker Compose)

Important structure
- `src/main/java` - source code
- `src/main/resources/application.yaml` - configuration (profiles `dev` and `prod`)
- `docker/docker-compose.yaml` - orchestrates Cassandra, Redis and two API instances
- `docker/Dockerfile.dev` - Dockerfile used by `docker-compose` to build the API image
- `docker/cassandra-init.cql` and `docker/cassandra-init.sh` - Cassandra keyspace/table initialization scripts
- `docker/redis-cluster-init.sh` - script to create the Redis cluster

Build (local)
1. Produce the JAR:

```bash
./mvnw -DskipTests clean package
```

The artifact will be created at `target/app.jar` (the `finalName` in `pom.xml` is `app`).

Running locally
- Using Maven (development mode):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- Using the generated JAR:

```bash
java -jar target/app.jar --spring.profiles.active=dev
```

By default the `dev` profile expects Cassandra and Redis running on `127.0.0.1` (see `application.yaml`).

Running with Docker Compose (infrastructure + API)
- The compose file in the `docker/` directory orchestrates:
  - Cassandra (3 nodes + initializer)
  - Redis (6 nodes forming a cluster + initializer)
  - Two API instances (api1 and api2)

It is recommended to have at least 4GB of free RAM available to run the whole stack.

To bring up the full stack (from the project root):

```bash
docker-compose -f docker/docker-compose.yaml up --build
```

Important notes:
- The init containers (`cassandra-init`, `redis-cluster-init`) run scripts that create the `shortener` keyspace in Cassandra and initialize the Redis cluster.
- The API containers run with `SPRING_PROFILES_ACTIVE=prod` in the compose file and expect services to be reachable by hostname (e.g. `cassandra1`, `redis1`, ...).
- The `docker/Dockerfile.dev` expects `target/app.jar` to exist when building the image — run the build before starting Docker Compose.

Useful environment variables
- SPRING_PROFILES_ACTIVE: Spring profile to use (`dev` or `prod`).
- SHORTENER_NODE_ID: Node ID used by the application to generate codes (e.g. `1` or `2`).
- SERVER_PORT: Server port (e.g. `8080`).

Configuration (`application.yaml`)
- `dev` profile (activated with `--spring.profiles.active=dev`):
  - Cassandra at `127.0.0.1:9042` (datacenter `dc1`, keyspace `shortener`)
  - Redis cluster at `127.0.0.1:6379`
  - `shortener.node-id` set to `1` in the YAML

- `prod` profile (for Docker Compose):
  - Cassandra at `cassandra1:9042`, `cassandra2:9042`, `cassandra3:9042`
  - Redis cluster at `redis1..redis6:6379`
  - `shortener.node-id` taken from the `SHORTENER_NODE_ID` environment variable

API (endpoints)
- POST /api/shortener
  - Description: creates a shortcode for a long URL.
  - Request (JSON):
    {
      "url": "https://example.com/some/page"
    }
  - Response: 201 Created
    - Body (application/json):
      {
        "shortCode": "abc123",
        "shortUrl": "http://host:port/api/shortener/abc123"
      }
    - The `Location` header points to the created resource (e.g. `/api/shortener/abc123`).
  - Validation: the `url` field is required (message: "url é obrigatória").

- GET /api/shortener/{shortcode}
  - Description: redirects to the full URL corresponding to the shortcode.
  - Behavior: returns 302 FOUND (Location = full URL) if found; otherwise returns 404.

Quick curl examples
- Create a shortcode:

```bash
curl -X POST http://localhost:8080/api/shortener \
  -H "Content-Type: application/json" \
  -d '{"url":"https://spring.io"}' -v
```

- Access a shortcode (follow redirect):

```bash
curl -v http://localhost:8080/api/shortener/{shortcode}
```

Tests
- Run all unit tests:

```bash
./mvnw test
```

Help / Troubleshooting
- Connection errors to Cassandra/Redis when using the `dev` profile: verify Cassandra and Redis are running locally on ports 9042 and 6379.
- If using Docker Compose and you get out-of-memory errors, increase Docker resources (memory/CPU) or start services separately.

License
- `Spring URL Shortener` is [MIT licensed](LICENSE).
