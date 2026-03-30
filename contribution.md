# Contributing
 
## Git hooks (Lefthook)
This project uses [Lefthook](https://github.com/evilmartians/lefthook) to enforce branch naming conventions.
Every `git push` automatically validates that the branch name matches the required pattern.
### Allowed branch name formats
```
build/my-build
chore/my-chore
ci/ci-pipe
docs/docs-update
feat/my-feat
fix/my-fix
perf/my-perf
refactor/my-refactor
revert/my-revert
style/my-style
test/test-case
hotfix/some-update
release/new-release
update/new-update
```
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
# via Homebrew
brew install lefthook
```
#### Linux
```bash
# via Snap
sudo snap install lefthook --classic
# if the command is not found after snap install, add a symlink
sudo ln -s /snap/lefthook/current/lefthook /usr/local/bin/lefthook
# or download the binary directly
curl -L https://github.com/evilmartians/lefthook/releases/latest/download/lefthook_linux_amd64 \
  -o /usr/local/bin/lefthook
chmod +x /usr/local/bin/lefthook
```
---
### Setup
After installing, run this once in the project root:
```bash
lefthook install
```
This registers the hooks in `.git/hooks/`. From now on every `git push` will validate your branch name automatically.
### Verify installation
```bash
lefthook --version
```
---
### Bypassing the hook (use sparingly)
If you need to push without validation (e.g. directly to `main`):
```bash
git push origin main --no-verify
```

---

## Environment variables

The project uses a `.env` file for configuration. It is **not committed to the repository**.

Copy the example file and fill in your values:
```bash
cp .env.example .env
```

`.env.example` contains all required keys with empty values:
```env
DB_USER=
DB_PASS=

GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=

JWT_SECRET=
```

### Variable reference

| Variable | Description |
|---|---|
| `DB_USER` | PostgreSQL username |
| `DB_PASS` | PostgreSQL password |
| `GITHUB_CLIENT_ID` | GitHub OAuth App client ID |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth App client secret |
| `JWT_SECRET` | Secret key for signing JWT tokens |

> GitHub OAuth credentials can be obtained at https://github.com/settings/developers

> For `JWT_SECRET` use a long random string, e.g. generated with `openssl rand -base64 32`

---

## Running the project

### Prerequisites
Make sure you have the following installed:
- Java 21+
- Docker
- Docker Compose

### Quick start

The project includes a `build.sh` script inside `AssesX-backend/` that automatically checks and installs missing dependencies, builds the JAR, and starts all services via Docker Compose.
```bash
cd AssesX-backend
chmod +x build.sh
./build.sh
```

This will:
1. Check and configure `JAVA_HOME`
2. Check and install Docker / Docker Compose if missing
3. Build the Spring Boot JAR via Maven
4. Start the backend and PostgreSQL via `docker compose up --build`

---

### Manual start

If you prefer to run steps manually:
```bash
# 1. Build the JAR
cd AssesX-backend
./mvnw clean package -DskipTests

# 2. Start services
cd ..
docker compose up --build
```

---

### Stopping services
```bash
docker compose down
```

To also remove the database volume:
```bash
docker compose down -v
```