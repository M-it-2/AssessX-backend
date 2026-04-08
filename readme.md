# AssessX Backend

AssessX is a backend platform for conducting programming assessments — multiple-choice tests and code practice tasks with automated grading. Built with Spring Boot and designed to integrate with a JavaFX desktop client.

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Configuration](#environment-configuration)
  - [Running with Docker Compose](#running-with-docker-compose)
  - [Running Locally](#running-locally)
- [Authentication](#authentication)
- [API Reference](#api-reference)
  - [Auth](#auth)
  - [Users](#users)
  - [Groups](#groups)
  - [Tests](#tests)
  - [Code Practices](#code-practices)
  - [Assignments](#assignments)
  - [Results](#results)
- [Database Schema](#database-schema)
- [Code Execution Sandbox](#code-execution-sandbox)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

AssessX supports two user roles: **Teacher** and **Student**. Teachers create tests and coding tasks, assign them to groups, and view gradebook results. Students complete assigned work within a time limit, with code submissions running inside an isolated Docker container.

Key capabilities:

- GitHub OAuth 2.0 login with JWT-based API authentication
- Multiple-choice tests with JSONB storage and automatic grading
- Code practice tasks with configurable unit tests, executed in a sandboxed Docker container
- Assignment scheduling with deadlines and group-level targeting
- Gradebook API for tracking results per student and per group

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.4 |
| Build | Maven (./mvnw wrapper) |
| Database | PostgreSQL 15+ |
| ORM | Spring Data JPA (Hibernate) |
| Authentication | GitHub OAuth 2.0 (Spring Security OAuth2 Client) |
| Token | JWT — Spring Security OAuth2 Resource Server (HS256) |
| Code isolation | Docker (openjdk:21-slim container per submission) |
| Testing | JUnit 5, Mockito, Spring MockMvc |

---

## Architecture

```
JavaFX Client  <--HTTP/REST-->  Spring Boot API  <-->  PostgreSQL
                                       |
                              Docker sandbox
                              (Java code runner)
                                       |
                              GitHub OAuth 2.0
```

The API server is stateless. All session state is carried in JWT tokens. Code submissions spin up a fresh, network-isolated Docker container per request and are cleaned up immediately after execution.

---

## Getting Started

### Prerequisites

- Docker and Docker Compose
- A GitHub OAuth App ([create one here](https://github.com/settings/developers))
  - Set the callback URL to `http://localhost:8080/auth/github/callback`
- Java 21 and Maven (only needed for local runs without Docker)

### Environment Configuration

Copy the example file and fill in your values:

```bash
cp .env.example .env
```

Required variables:

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL host (e.g. `localhost` or `db` in Docker) |
| `DB_PORT` | PostgreSQL port (default `5432`) |
| `DB_NAME` | Database name |
| `DB_USER` | Database user |
| `DB_PASS` | Database password |
| `GITHUB_CLIENT_ID` | GitHub OAuth App client ID |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth App client secret |
| `JWT_SECRET` | Secret key for signing JWT tokens (min 32 chars) |
| `PORT` | Application port (optional, defaults to `8080`) |

### Running with Docker Compose

**Development** (includes PostgreSQL):

```bash
docker compose -f docker-compose.dev.yml up --build
```

This starts:
- `db` — PostgreSQL 15 with a health check, data persisted in a named volume
- `backend` — Spring Boot application, depends on `db` being healthy

The backend mounts the `assessx_sandbox` volume used by the code execution service.

**Production** (external PostgreSQL):

```bash
docker compose up --build
```

Starts only the backend service. Database must be provisioned separately. Initialize the schema from `db/schema.sql`.

### Running Locally

```bash
./mvnw spring-boot:run -f AssessX-backend/pom.xml
```

Requires a running PostgreSQL instance and a populated `.env` file at the project root. Also requires Docker to be running locally for code submission execution.

---

## Authentication

The API uses a two-step flow:

1. **OAuth login** — the client redirects the user to `GET /auth/github`. After GitHub authentication, the server issues a JWT and returns it as JSON: `{ "token": "..." }`.

2. **API access** — all `/api/**` endpoints require the token in the `Authorization` header:
   ```
   Authorization: Bearer <token>
   ```

3. **Registration completion** — new users have no name or group assigned. After receiving the token, clients must call `POST /auth/complete-registration` to provide this information before accessing other resources.

JWT tokens expire after 24 hours. The token encodes the user's ID, GitHub login, and role (`STUDENT` or `TEACHER`).

---

## API Reference

All API endpoints return errors in a consistent format:

```json
{
  "error": "Description of what went wrong",
  "status": 404
}
```

### Auth

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/auth/github` | Public | Initiates GitHub OAuth flow |
| GET | `/auth/github/callback` | Public | OAuth callback — returns JWT |
| GET | `/auth/me` | Authenticated | Returns current user info |
| POST | `/auth/complete-registration` | Authenticated | Sets user name, role, and group |

**POST /auth/complete-registration** — request body:

```json
{
  "name": "Jane Doe",
  "role": "STUDENT",
  "groupId": 1
}
```

### Users

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/users` | TEACHER | List all users |
| GET | `/api/users/{id}` | Authenticated | Get user by ID |

**User response:**

```json
{
  "id": 1,
  "githubLogin": "janedoe",
  "name": "Jane Doe",
  "role": "STUDENT",
  "groupIds": [2]
}
```

### Groups

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/groups` | Authenticated | List all groups |
| POST | `/api/groups` | TEACHER | Create a group |
| GET | `/api/groups/{id}/students` | TEACHER | List students in a group |
| POST | `/api/groups/{id}/students?userId=` | TEACHER | Add student to a group |
| DELETE | `/api/groups/{id}/students/{userId}` | TEACHER | Remove student from a group |

**POST /api/groups** — request body:

```json
{
  "name": "CS-101"
}
```

### Tests

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/tests` | Authenticated | List all tests |
| GET | `/api/tests/{id}` | Authenticated | Get test (answers hidden for STUDENT) |
| POST | `/api/tests` | TEACHER | Create a test |
| PUT | `/api/tests/{id}` | TEACHER | Update a test |
| DELETE | `/api/tests/{id}` | TEACHER | Delete a test |
| POST | `/api/tests/{id}/submit` | Authenticated | Submit answers |

**POST /api/tests** — request body:

```json
{
  "title": "Java Basics",
  "questions": {
    "q1": "What does JVM stand for?",
    "q2": "Which keyword declares a constant?"
  },
  "answers": {
    "q1": "Java Virtual Machine",
    "q2": "final"
  },
  "points": 10,
  "timeLimitSec": 600
}
```

**POST /api/tests/{id}/submit** — request body:

```json
{
  "assignmentId": 3,
  "answers": {
    "q1": "Java Virtual Machine",
    "q2": "final"
  }
}
```

Response:

```json
{
  "earnedPoints": 10,
  "maxPoints": 10,
  "correctAnswers": 2,
  "totalQuestions": 2
}
```

### Code Practices

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/practices` | Authenticated | List all practices |
| GET | `/api/practices/{id}` | Authenticated | Get practice details |
| POST | `/api/practices` | TEACHER | Create a practice |
| PUT | `/api/practices/{id}` | TEACHER | Update a practice |
| DELETE | `/api/practices/{id}` | TEACHER | Delete a practice |
| POST | `/api/practices/{id}/submit` | Authenticated | Submit a code solution |

**POST /api/practices** — request body:

```json
{
  "title": "Fibonacci",
  "description": "Implement a method that returns the nth Fibonacci number.",
  "unitTests": [
    "assert Solution.fib(0) == 0;",
    "assert Solution.fib(1) == 1;",
    "assert Solution.fib(10) == 55;"
  ],
  "points": 20,
  "timeLimitSec": 30
}
```

**POST /api/practices/{id}/submit** — request body:

```json
{
  "assignmentId": 5,
  "code": "public class Solution { public static int fib(int n) { ... } }"
}
```

Response:

```json
{
  "passedTests": 3,
  "totalTests": 3,
  "output": "All tests passed."
}
```

### Assignments

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/assignments` | TEACHER | List all assignments |
| GET | `/api/assignments/my` | STUDENT | List assignments for the current user's groups |
| POST | `/api/assignments` | TEACHER | Create an assignment |
| DELETE | `/api/assignments/{id}` | TEACHER | Delete an assignment |

**POST /api/assignments** — request body (exactly one of `testId` or `practiceId` must be set):

```json
{
  "groupId": 2,
  "testId": 1,
  "practiceId": null,
  "deadline": "2026-05-01T23:59:00"
}
```

### Results

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/results/my` | Authenticated | Get current user's results |
| GET | `/api/results/group/{groupId}` | TEACHER | Get all results for a group |
| GET | `/api/results/{id}` | Authenticated | Get a single result by ID |

---

## Database Schema

The schema is defined in `db/schema.sql`. Apply it to a fresh database before running in production with `ddl-auto: validate`.

**Tables:**

| Table | Purpose |
|---|---|
| `users` | GitHub-authenticated users with STUDENT or TEACHER role |
| `groups` | Named student groups |
| `user_groups` | Many-to-many join between users and groups |
| `tests` | Multiple-choice tests; `questions` and `answers` stored as JSONB |
| `code_practices` | Coding tasks with metadata |
| `practice_unit_tests` | Unit test code snippets linked to a practice (one-to-many) |
| `assignments` | Links a group to a test or practice with a deadline |
| `results` | Submission record per user per assignment attempt |
| `code_submissions` | Full code and execution output for a practice result |

A database-level `CHECK` constraint on `assignments` and `results` enforces that exactly one of `test_id` or `practice_id` is set.

---

## Code Execution Sandbox

Student code runs inside a Docker container with no network access, limited CPU, and capped memory:

```
docker run --network none --memory 128m --cpus 0.5 openjdk:21-slim
```

The service:
1. Writes `Solution.java` (student code) and `Runner.java` (generated unit test runner) to a temporary directory under the `/sandbox` volume
2. Compiles and runs the code inside the container
3. Waits for completion with a hard timeout of `timeLimitSec + 5` seconds
4. Parses stdout to count passed/total tests
5. Cleans up the temporary directory

The sandbox volume (`assessx_sandbox`) must be shared between the host Docker daemon and the backend container. In development, this is configured automatically in `docker-compose.dev.yml`.

---

## Testing

The project has 110 unit and MockMvc tests covering all service methods and three controllers.

Test layout:

| Layer | Type | Count |
|---|---|---|
| Service | Unit (JUnit 5 + Mockito) | ~80 |
| Controller | MockMvc (standaloneSetup) | ~31 |
| Application | Context load (requires DB) | 1 |

The context load test (`MititApplicationTests`) requires a running PostgreSQL instance with valid environment variables. All other tests are fully isolated and run without a database.

---

## Contributing

See [contribution.md](contribution.md) for guidelines and [commit.md](commit.md) for the commit message convention used in this project.

Commit format:

```
<type>: <short description>
```

Types: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `ci`, `build`, `perf`, `style`, `revert`, `hotfix`, `release`

---

## License

This project is licensed under the Mozilla Public License 2.0. See [license.md](license.md) for the full text.
