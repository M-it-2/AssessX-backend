#!/usr/bin/env bash
# ─────────────────────────────────────────────
# AssessX — dev startup script
# Runs: PostgreSQL (native) → Spring Boot
# Usage: bash start.sh
# ─────────────────────────────────────────────

set -euo pipefail

PGDIR="$HOME/postgresql/pgsql"
PGDATA="$HOME/postgresql/data"
PGPORT=5432

export PATH="$PGDIR/bin:$PATH"
export JAVA_HOME="$HOME/.jdks/corretto-24.0.2"
export PATH="$JAVA_HOME/bin:$PATH"

# ── 1. Load .env ─────────────────────────────
if [ -f ".env" ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
  echo "[env] Loaded .env"
else
  echo "[env] WARNING: .env not found, using defaults"
fi

# ── 2. Start PostgreSQL if not running ───────
if pg_ctl -D "$PGDATA" status > /dev/null 2>&1; then
  echo "[db]  PostgreSQL already running"
else
  echo "[db]  Starting PostgreSQL..."
  pg_ctl -D "$PGDATA" -l "$PGDATA/postgres.log" start -w
  echo "[db]  PostgreSQL started"
fi

# ── 3. Run Spring Boot ────────────────────────
echo "[app] Starting Spring Boot on :8080..."
cd AssesX-backend
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Duser.timezone=UTC"
