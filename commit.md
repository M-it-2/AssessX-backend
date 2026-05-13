# Commit Message Convention

This project follows a structured commit message format based on [Conventional Commits](https://www.conventionalcommits.org/).

## Format

```
<type>: <short description>
```

- **type** — one of the prefixes listed below
- **description** — short, imperative, lowercase (e.g. `add`, `fix`, `remove`, not `added`, `fixed`)

---

## Types

| Type | When to use |
|---|---|
| `feat` | A new feature visible to the user |
| `fix` | A bug fix |
| `hotfix` | An urgent fix applied directly to production |
| `perf` | A change that improves performance |
| `refactor` | Code restructuring with no behavior change |
| `style` | Formatting, whitespace, missing semicolons — no logic change |
| `test` | Adding or updating tests |
| `docs` | Documentation only changes |
| `build` | Changes to build system or external dependencies |
| `ci` | Changes to CI/CD configuration or scripts |
| `chore` | Routine tasks, dependency updates, tooling |
| `revert` | Reverts a previous commit |
| `release` | Marks a new release version |
| `update` | General updates that don't fit other types |

---

## Examples

```
feat: add GitHub OAuth login
fix: resolve null pointer on empty user list
hotfix: patch token expiration bypass
perf: cache database query results
refactor: extract auth logic into separate service
style: apply formatter to user module
test: add unit tests for JWT validation
docs: update Docker setup instructions
build: upgrade Spring Boot to 3.3.0
ci: add GitHub Actions workflow for PR checks
chore: remove unused dependencies
revert: revert "feat: add dark mode toggle"
release: v1.4.0
update: refresh .env.example with new variables
```
