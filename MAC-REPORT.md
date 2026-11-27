# Mac test report: QA AI bootcamp release R0 (coach 0.25.0)

Run on 27 Nov 2026 by Claude Code on the Mac, following `MAC-HANDOVER.md` Part B. No file inside `qa-ai-coach/` was changed. Nothing was committed, pushed, published or sent. No passwords, secrets or keys are in this report or in the `evidence/` folder.

**Overall: the coach works on the Mac. 2 Mac problems would hit testers. One of them, week 2/3 Python install on Intel Macs, is a blocker for Intel Mac testers.**

## 1. Versions (step 0)

| Item | Value |
|---|---|
| macOS | 15.7.9 |
| CPU | **x86_64 (Intel Mac)**, not Apple Silicon |
| Node.js | **v20.20.2 installed (too old)** → test run on **v22.23.5**, portable (see note) |
| npm | 10.8.2 (system) / 10.9.9 (with Node 22.23.5) |
| git | 2.33.1 |
| Claude Code | 2.1.332 |
| Python | `python3.12` = 3.12.13 (Homebrew); `python3` = **3.9.0** |
| Playwright | 1.64.1, Chrome Headless Shell 156.0.8078.4 |

**Node note:** the Mac's Node was v20.20.2 (Homebrew `node@20`; `/usr/local/bin/node` is even older, v14.15.1). The programme manager chose a portable copy: the official Node 22.23.5 tarball from nodejs.org, SHA-256 checked, unpacked to `~/Downloads/bootcamp-mac-test/.node` and put first on PATH for this run only. Nothing system-wide was installed.

**Path note:** the test folder was `~/Downloads/bootcamp-mac-test`, not `~/bootcamp-mac-test` as the handover says. All commands used the real path, and the env variables were set as instructed (`QA_BOOTCAMP_HOME=$HOME/.qa-ai-bootcamp-mac-test`, `QA_COACH_NO_OPEN=1`, `PW_TEST_HTML_REPORT_OPEN=never`).

## 2. Summary

| Step | Result | Evidence | Time |
|---|---|---|---|
| 0 Versions | PASS (after Node fix) | Node 20.20.2 found; ran on portable Node 22.23.5 | – |
| 1 Unit and hook tests | **FAIL (3)** | 96 tests: 85 pass, 3 fail, 8 skip (Python). Fails: #6 and #81 come from the test setup; **#52 is a Windows-only case not marked as such** (see §3) | 3 s |
| 1b Unit tests again, with Python venv | FAIL (4) | 91 pass, 4 fail (the 3 above, plus #7: `parcelly-support/tests` is not in the bundle) | 4 s |
| 2 Plugin check | PASS | `✔ Validation passed` | 2 s |
| 3 Workspace and set-up check | PASS (1 Mac issue) | `INIT_OK new`; `"os": "mac"`; `TODAY 2026-11-27 Fri 27 Nov` first; `CHECK 6 of 6`; no PowerShell or Windows paths. **Python 3.12 shown as missing although `python3.12` is installed** (see §4) | 2 s |
| 4 Conduit practice app | PASS | `APP ready http://127.0.0.1:4310 mfa=off\|totp captcha=off\|on`; `/api/environment` returns JSON; note file written | ~6 s |
| 5 Starter and Playwright | PASS | `STARTER_OK`; all setup lines ✔; `.mcp.json` runs `npx … run-test-mcp-server --headless` (no `cmd /c`); 3 agents; `.env` deny rules; `4 passed` (both role logins and both seeds) | 35 s setup, 6 s tests |
| 6 Second role | PASS | `member-denied` in `chromium`, `mod.role2` in `chromium-role2`: `4 passed` | 4 s |
| 7 Stability and secrets | PASS | `✔ Stable: all tests passed in both runs` (14/14 ×2); clean; planted leak → `tests/leak.spec.ts:1  a value from your .env file`, value not printed | ~30 s |
| 8 Claude's browser and agents | PASS | `specs/home.md` saved (2 scenarios); "Yes, the planner's browser was signed in" | **42 s** |
| 9a Guard: show .env | PASS | Refused: "I can't open the `.env` file…", no values | 10 s |
| 9b Guard: password in prompt | PASS | `Stopped: this message holds a value from your .env file. It was not sent to Claude.` | 5 s |
| 9c Guard: output (`cat ../conduit-practice-note.txt`) | PASS (different path) | Value not shown, but the **tool guard** blocked the command ("one of the coach's own files"), so `[removed]` never appeared. See 9d | 10 s |
| 9d Output guard, extra test | PASS | `cat` of a Playwright `error-context.md` that held the real password → Claude saw `textbox "Password": [removed]` | 19 s |
| 10 `/bootcamp help` | PASS | Full command list; no Windows/PowerShell wording or paths (it names no paths at all, so `~/qa-ai-bootcamp` doesn't appear) | 12 s |
| 11a Week 2 evals venv (as shipped) | **FAIL** | `ERROR: Failed building wheel for cryptography`: **no Intel-Mac wheel for cryptography 50.0.2** | 110 s |
| 11a′ Week 2 with workaround | PASS* | With `cryptography==48.0.1`: `ready`; pytest `13 passed` | 43 s |
| 11b Practice agent and trace | PASS* | `AGENT_RUN r-20261127-092024-tu48`, sensible reply; `TRACE …/week-2/traces/….md` readable. Finds `evals/.venv` without `QA_BOOTCAMP_PYTHON` too | 9 s |
| 11c Week 3 redteam venv (as shipped) | **FAIL** | Same `cryptography` wheel build failure | 53 s |
| 11c′ Week 3 with workaround | PASS* | `10 passed, 1 deselected` | 45 s |
| 11d Chat page | PASS | `CHAT ready http://127.0.0.1:4370` / `CHAT stopped` | 1 s |
| 12 Clean-up | PASS | `APP stopped`; nothing listening on 4310/4370 | 1 s |

\* passes only with a changed copy of `requirements.txt` kept outside the plugin (`cryptography==48.0.1`). The shipped file fails on Intel Macs.

## 3. Failures in detail

### F1: Week 2 and week 3 Python install fails on Intel Macs (BLOCKER for Intel Mac testers)
- **Command** (in `qa-ai-bootcamp/evals`, and the same in `redteam`):
  `python3.12 -m venv .venv && .venv/bin/python -m pip install -r requirements.txt`
- **Error (end of output):**
  ```
  xcode-select: Failed to locate 'clang', requesting installation of command line developer tools.
  error: could not compile `cryptography-key-parsing` (build script) due to 1 previous error; 1 warning emitted
  💥 maturin failed
  ERROR: Failed building wheel for cryptography
  error: failed-wheel-build-for-install
  × Failed to build installable wheels for some pyproject.toml based projects
  ╰─> cryptography
  ```
- **Cause:** both `skills/bootcamp/starter/evals/requirements.txt` and `skills/bootcamp/starter/redteam/requirements.txt` pin `cryptography==50.0.2`. PyPI has no macOS x86_64 or universal2 wheel for 50.0.0, 50.0.1, 50.0.2 or 49.0.0, only arm64. The last release with an Intel-Mac wheel is **48.0.1** (`cp311-abi3-macosx_10_9_universal2`). On an Intel Mac pip therefore compiles from source, which needs Rust and a working Xcode/clang. Testers won't have those, and this Mac's Xcode is broken anyway. Every other native package pinned (numpy, scipy, scikit-learn, grpcio, pydantic_core, jiter, rpds-py, aiohttp, cffi) does have Intel wheels.
- **Proven:** with only that one line changed to `48.0.1`, both installs work, all imports load and all tests pass (11a′, 11c′). Whether deepeval/deepteam behave the same with 48.0.1 in a real eval or scan run was not tested.
- **Apple Silicon Macs are not affected.**

### F2: Unit test #52 "tool guard: shell commands cannot reach the coach's files" fails on Mac
- **Command:** `node --test "qa-ai-coach/tests/bootcamp-*.test.mjs"`
- **Error:** `should deny: type ..\journal.md` (`tests/bootcamp-guards.test.mjs:338`)
- **Cause (likely):** the test case uses a Windows backslash path. On Mac the guard doesn't treat `\` as a path separator, so it isn't seen as `../journal.md`. In zsh `type` is a shell builtin that doesn't print files, and `..\journal.md` becomes `..journal.md`, so there is **no real leak on Mac**. The test needs to be Windows-only (like test #23), or the guard needs to normalise `\` on every OS. The other six DENY cases in the test pass on Mac.

### F3: Unit test #81 "setup: --dry-run lists the steps and changes nothing" fails on Mac
- **Error:** `The input did not match the regular expression /npm install/. Input: ''` (`tests/bootcamp-starter.test.mjs:174`)
- **Cause (confirmed):** the test copies the starter into `os.tmpdir()`, which on macOS is `/var/folders/…`. `/var` is a symlink to `/private/var`. The `isMain` check in `starter/smoke-tests/scripts/setup.mjs:112` compares `path.resolve(process.argv[1])` (the symlinked path) with `fileURLToPath(import.meta.url)` (the resolved real path), so they differ and the script does nothing and prints nothing. Run from a real path, `setup.mjs --dry-run` prints every expected line (checked). **Tester impact:** small, but if a tester's folder sits under a symlink, `npm run setup` would silently do nothing. Comparing `fs.realpathSync` of both sides would fix it.

### F4: Unit tests #6 and #7: bundle and test-environment only (not Mac problems)
- #6: `ENOENT … /Users/levincharles/Downloads/scripts/release/build_coach_repo.py`: the test reads a release script three folders above the plugin, which is not in the zip.
- #7 (only when a Python venv is given): `ImportError: Start directory is not importable: 'tests'`. `agents-practice/parcelly-support/tests/` is not in the bundle.
- These would fail on Windows too with this zip. The handover's "0 failures" expectation assumed the full repo.

## 4. Mac-specific things a tester would trip over

1. **Node 22 is not on a typical Mac.** This Mac had Node 20 (Homebrew) and an even older `/usr/local/bin/node` (v14). Telling testers to use the nodejs.org installer is right. Testers who use Homebrew `node@20` will be caught by `check.mjs`, but the hooks run plain `node` from PATH, so the PATH order matters.
2. **`check.mjs` looks for Python only as `python3`** (`scripts/bootcamp/check.mjs:64`). On this Mac `python3` is 3.9 while `python3.12` is installed, so the check says `LATER Python 3.12 or later … install` and sends the tester off to install something they already have. Meanwhile `starter-redteam.mjs` prints `NEXT python3.12 -m venv .venv`, and `agent.mjs` falls back to `python3` when there is no venv. These disagree. Suggest also trying `python3.12` / `python3.13` in the check.
3. **Intel Macs + cryptography 50.0.2**: see F1. Ask Mac testers for "Intel or Apple chip" before the bootcamp.
4. **Untrusted folder warning in every `claude -p` run**: `Ignoring 3 (or 10) permissions.allow entries from .claude/settings.json: this workspace has not been trusted.` Interactive testers accept the trust dialog the first time, but it shows up in each new folder (workspace, `smoke-tests`). If a tester says No, the bootcamp allow rules are silently ignored. The deny rules and hooks still worked.
5. **Playwright writes typed passwords into `test-results/…/error-context.md`** (page snapshot, `textbox "Password": <value>`) when a login test fails. The output guard turns it into `[removed]` for Claude (proven in 9d), but the value is on disk in plain text, and so is the HTML report and the trace. Claude itself pointed this out. Worth telling testers, or deleting `test-results/` after a failed login.
6. Handover step 9c no longer reaches the output guard: the tool guard blocks `cat ../conduit-practice-note.txt` first ("coach's own file"). That is safe, but the expected `[removed]` result needs a different test (9d above works).
7. Small: the `.env` parser in my run had to strip the "(a member: your main test account)" text after `TEST_USER=` in `conduit-practice-note.txt`. A tester copying the whole line into `.env` gets `email or password is invalid`. Put the hint on its own line.
8. Unpacking: the handover assumes `~/bootcamp-mac-test`. Double-clicking the zip in Downloads leaves it in `~/Downloads/`. All scripts worked from there.
9. No browser window opened at any point; everything ran headless.
10. Disk: `check.mjs` reports 8.8 GB free on this Mac. The Playwright browsers (~150 MB) and two Python venvs (~1 GB together) fit, but tight disks are common on older Intel Macs.

## 5. What is in the hand-over zip
- `MAC-REPORT.md`: this file.
- `MAC-HANDOVER.md`: the instructions this run followed.
- `evidence/`: raw output of every step (`step1…step12`). Every file was scanned for the practice password and user values; where a value appeared it is replaced by `[REDACTED-BY-TESTER]`.
- `artifacts/`: what the run produced: `bootcamp.json`, plan/check output, `specs/home.md`, the two role-2 spec files, `.mcp.json`, `.claude/settings.json`, the trace file, and the Intel workaround `requirements` diff.
- Not included: `.env`, `conduit-practice-note.txt`, `playwright/.auth/`, `node_modules/`, `.venv/`, the portable Node.
