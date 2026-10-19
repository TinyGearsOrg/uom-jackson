# AGENTS.md

Operating rules for AI coding agents working in this repository: uom-jackson, the Jackson modules for uom quantities
and units. `uom-jackson2` is the module for Jackson 2 and `uom-jackson3` the one for Jackson 3; both serve the native
API (`uom`) and the JSR-385 API (`uom-jsr`) of [uom](https://github.com/TinyGearsOrg/uom), which are optional
dependencies. Claude Code loads this file through `CLAUDE.md`, a symbolic link to it; other agents read it directly.

This is the short, must-follow contract. The format the modules implement is [docs/JSON.md](docs/JSON.md), and the
build and the baselines are in the parent [pom.xml](pom.xml). If a rule here conflicts with an explicit request from the
user, ask before overriding it.

## Non-negotiables

- **Every new `.java` file starts with the license header** of the existing files (Apache-2.0, "Copyright (c) 2026
  TinyGears.org"); copy it, nothing writes it for you. YAML files carry `# SPDX-License-Identifier: Apache-2.0` first.
- **A change with no matching test update is incomplete**, unless it has no testable surface (docs only, build
  configuration); say so explicitly rather than skipping silently. A bug fix comes with a test that fails without the
  fix. Tests assert exact values, not just "not null" or "contains".
- **`mvn -q -B clean verify` passes before you report a change as done.** It runs the tests of both modules on three
  class paths. Report failures with their output.
- **Never commit unless the user asks**, never push or open a pull request unless asked, and stage only the files you
  changed (`git add <path>`).

## The two modules and the two APIs

- **The modules behave alike.** A change to `uom-jackson2` is made to `uom-jackson3` in the same change, and the other
  way round, with the same tests. They differ only where Jackson does: `ValueSerializer`, `ValueDeserializer` and
  `SerializationContext` in place of `JsonSerializer`, `JsonDeserializer` and `SerializerProvider`; `DatabindException`
  and unchecked exceptions; `PROPERTY_NAME`, `getString()`, `writeName()` and `writeStringProperty()`; mappers that are
  built, not configured (the `Mappers` helper of the Jackson 3 tests); `JsonNode.asString()`; and the default of
  `FAIL_ON_UNKNOWN_PROPERTIES`, which is on in Jackson 2 and off in Jackson 3. A real difference is written down in
  [docs/JSON.md](docs/JSON.md) and tested.
- **The JSON is the same for both uom APIs.** The quantities of uom-jsr are read and written through the native code
  with `Jsr.toJsr` and `Jsr.fromJsr`; do not add a second implementation of the format.
- **uom and uom-jsr are optional.** The module serves what is on the class path, and creating it without either throws
  an `IllegalStateException`. Nothing that mentions a type of uom may be loaded before `UomModule` has checked that it
  is there: `UomModule` mentions no uom type itself, the classes that do (`Options`, `Builder`) are reached only after
  the check, the native classes are installed only by `NativeSupport` and the JSR classes only by `JsrSupport`.
- **The tests are chosen by file name**, one surefire execution per class path (the parent `pom.xml`): `Native*Test`
  runs with and without uom-jsr, so it must not mention a type of uom-jsr; `NativeOnly*Test` runs without uom-jsr;
  `Neither*Test` runs without uom; every other test (`Jsr*Test`, `PresenceTest`, `VersionTest`, `ModulePathTest`) runs on
  the full class path. A test class that mentions an excluded type cannot be loaded there, so its name must fit its
  execution. `ModulePathTest` builds its own module paths from the jars of the class path, compiles a consumer module
  and runs it on `--module-path`, with the exploded `target/classes` as the module under test.

## Pinned outputs to keep in step

- [docs/JSON.md](docs/JSON.md) is the specification. A change of behaviour changes it, and the other way round.
- `vectors/quantities.json` is read by the native and the JSR tests of both modules. `read` and `write` are exact JSON
  text; a new behaviour of the format gets a vector, and a changed one changes it for every module and API.
- The README states the baselines (Jackson 2, Jackson 3, uom) and what is supported; they must match the properties of
  the parent `pom.xml`.
- [CHANGELOG.md](CHANGELOG.md): a change a user can see gets a line under `## [Unreleased]`.

## Workflow and code quality

- Read files in full before wide-ranging changes, before editing files you have not inspected, and when
  investigating. Do not rely on search snippets.
- The baseline is Java 17 (`maven.compiler.release`): no APIs or language features of later releases.
- The dependencies are `jackson-databind` of the module's line, `uom` and `uom-jsr` (both optional) and JUnit for tests;
  do not add another.
- **The baselines are not raised by the way.** `jackson2.version`, `jackson3.version` and `uom.version` are the oldest
  versions the modules support, and Dependabot ignores them. Raising one is a decision for the user. The weekly
  `latest` job of the CI workflow builds against the newest releases.
- The build compiles with `-Xlint:all -Werror`: fix a warning, or suppress it on the smallest element with a comment
  that says why.
- Keep code comments short (1 to 3 lines): state only the non-obvious constraint or rationale, never narrate what the
  code does. Do not recount what a line used to do; the history belongs in the commit message.
- Ask before removing functionality or code that appears intentional. Nothing is released yet, so backward
  compatibility is not required unless the user asks for it; from 1.0, semantic versioning applies.
- A design decision is written down in [docs/JSON.md](docs/JSON.md) or the README, with the reason. Open work is tracked
  in the GitHub issues of `TinyGearsOrg/uom-jackson`; a gap in uom itself is an issue of `TinyGearsOrg/uom`.
- GitHub Actions are pinned to a commit with the version in a comment. After a change to a workflow, lint it with
  `zizmor --min-severity low .github/workflows` where it is installed.
- Documentation is concise and technical, and every statement must match the code and the commands. Check a claim
  (run it, read the code) before writing it down.

## Conventions

- Commit subjects use a conventional-commit prefix: `feat:`, `fix:`, `perf:`, `refactor:`, `chore:`, `docs:`,
  `test:`, `style:`, `build:`, or `ci:`.
- Keep the commit body short: state what changed and why, not a walkthrough of how you got there or what an earlier
  draft did.
- No emojis in commits, pull requests, issues, or code. Keep prose concise, direct, and technical, with no cheerful
  filler.
- Answer a user's question before making edits or running implementation commands.
- When responding to feedback or an analysis, say whether you agree or disagree before describing what you changed.

## Git

Multiple sessions may be running in this working tree at the same time, each modifying different files. Git
operations that touch unstaged, staged, or untracked files outside your own changes will stomp on other sessions'
work. Follow these rules:

Committing:

- Only commit files you changed in this session.
- Stage explicit paths (`git add <path1> <path2>`); never `git add -A` / `git add .`.
- Before committing, run `git status` and verify you are only staging your files.

Never run (destroys other agents' work or bypasses checks):

- `git reset --hard`, `git checkout .`, `git clean -fd`, `git stash`, `git add -A`, `git add .`,
  `git commit --no-verify`.

If rebase conflicts occur:

- Resolve conflicts only in files you modified.
- If a conflict is in a file you did not modify, abort and ask the user.
- Never force push.
