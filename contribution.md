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