# Contributing

## Git Hooks (Lefthook)

This project uses [Lefthook](https://github.com/evilmartians/lefthook) to enforce branch naming conventions. Every `git push` automatically validates that the branch name matches the required pattern.

### Allowed branch name formats

| Prefix | Example |
|---|---|
| `feat/` | `feat/user-auth` |
| `fix/` | `fix/login-crash` |
| `hotfix/` | `hotfix/null-pointer` |
| `docs/` | `docs/api-reference` |
| `chore/` | `chore/update-deps` |
| `ci/` | `ci/add-pipeline` |
| `build/` | `build/optimize-jar` |
| `perf/` | `perf/query-speed` |
| `refactor/` | `refactor/auth-module` |
| `revert/` | `revert/bad-commit` |
| `style/` | `style/format-code` |
| `test/` | `test/user-service` |
| `release/` | `release/v1.2.0` |
| `update/` | `update/dependencies` |

---

### Installing Lefthook

#### Windows
```powershell
# via Scoop
scoop install lefthook

# or via Winget
winget install evilmartians.lefthook
```

#### macOS
```bash
brew install lefthook
```

#### Linux
```bash
# via Snap
sudo snap install lefthook --classic

# If the command is not found after snap install, add a symlink:
sudo ln -s /snap/lefthook/current/lefthook /usr/local/bin/lefthook

# Alternatively, download the binary directly:
curl -L https://github.com/evilmartians/lefthook/releases/latest/download/lefthook_linux_amd64 \
  -o /usr/local/bin/lefthook
chmod +x /usr/local/bin/lefthook
```

---

### Setup

Run this once in the project root after installing:

```bash
lefthook install
```

This registers the hooks in `.git/hooks/`. From this point on, every `git push` will validate your branch name automatically.

### Verify installation
```bash
lefthook --version
```

---

### Bypassing the hook

If you need to skip validation (e.g. an emergency push directly to `main`), use `--no-verify`. **Use sparingly.**

```bash
git push origin main --no-verify
```

---

## Environment Variables

The project uses a `.env` file for configuration. It is **not committed to the repository**.

Copy the example file and fill in your values:
```bash
cp .env.example .env
```

`.env.example` contains all required keys with empty values:
```env
DB_URL=
DB_PORT=
DB_NAME=
DB_USER=
DB_PASS=
GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=
JWT_SECRET=
PORT=8080
```

### Variable reference

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL host (e.g. `localhost` or a remote IP) |
| `DB_PORT` | PostgreSQL port (default: `5432`) |
| `DB_NAME` | Name of the database |
| `DB_USER` | PostgreSQL username |
| `DB_PASS` | PostgreSQL password |
| `GITHUB_CLIENT_ID` | GitHub OAuth App client ID |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth App client secret |
| `JWT_SECRET` | Secret key for signing JWT tokens |
| `PORT` | Port the server listens on (default: `8080`) |

> **GitHub OAuth credentials** — create or manage at [github.com/settings/developers](https://github.com/settings/developers)
>
> **JWT_SECRET** — use a long random string. Generate one with:
> ```bash
> openssl rand -base64 32
> ```

---

## Running the Project

### Prerequisites

- Java 21+
- Docker
- Docker Compose

---

### Docker Compose overview

The project uses two Compose files that work together:

| File | Purpose |
|---|---|
| `docker-compose.yml` | Base config — used in production. Runs the backend only, connects to an external database. |
| `docker-compose.dev.yml` | Dev override — automatically merged by Docker Compose when present. Adds a local PostgreSQL container and overrides DB connection settings to use it. |

When you run `docker compose` without specifying a file, Docker Compose automatically merges both files. This means locally you always get the backend **and** a local Postgres — no extra flags needed.

---

### Local development

```bash
# 1. Build the JAR (skip tests for speed)
cd AssessX-backend
./mvnw clean package -DskipTests
cd ..

# 2. Start the backend + local PostgreSQL
docker compose up --build
```

The backend will be available at `http://localhost:8080`.

On subsequent runs you can skip `--build` if you haven't changed the code:
```bash
docker compose up
```

---

### Production (external DB)

Pass only the base file to skip the dev override and its local Postgres:

```bash
docker compose -f docker-compose.yml up --build
```

Make sure all `DB_*` variables in `.env` point to your external database before deploying.

---

### Managing containers

| Action | Command |
|---|---|
| Stop containers | `docker compose down` |
| Stop and remove local DB volume | `docker compose down -v` |
| View running containers | `docker compose ps` |
| Follow logs | `docker compose logs -f` |
| Restart a single service | `docker compose restart backend` |

> **`docker compose down -v`** permanently deletes the local database volume and all its data. Only use this when you want a clean slate.