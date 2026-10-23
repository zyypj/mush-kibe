**English** | [Português](README.pt-BR.md)

# Process

How work is planned, built and delivered. Muxe follows agile practices adapted to a small, open-source team: short iterations, small increments, continuous integration and working software over big plans.

## Principles

- **Small, vertical slices.** Each change delivers something a player, staff member or operator can observe, end to end, instead of a layer at a time.
- **Working software is the measure of progress.** A feature is done when it runs, is tested and is documented, not when the code is written.
- **Feedback early.** Pull requests stay small so they are reviewed the same day. Homologation servers are used to validate with real players before production.
- **Sustainable pace and quality.** No "temporary" shortcuts without an issue that tracks them. Tests and documentation are part of the work, not extra work.
- **Simplicity.** Build what the current iteration needs, behind interfaces that keep the next step cheap.

## Work items

Work is tracked as GitHub issues, using the templates in [.github/ISSUE_TEMPLATE](../../.github/ISSUE_TEMPLATE/).

### User stories

New behavior is described as a user story with acceptance criteria:

```text
As a <player | staff member | operator>
I want <capability>
So that <benefit>

Acceptance criteria
- Given <context>, when <action>, then <observable result>.
```

Acceptance criteria are observable and testable. They become tests and the "Acceptance criteria" section of the feature documentation.

### Bugs

Bugs describe the expected behavior, the actual behavior and how to reproduce them. The fix starts with a failing test.

### Technical tasks

Refactors, upgrades and infrastructure work explain the problem they solve and how the result will be verified.

## Iterations

- Work is organized in **two-week iterations**.
- **Planning:** the team picks the stories that are *ready* (see below), most valuable first, and splits any that would not fit in a few days of work.
- **Daily sync:** short and asynchronous is fine: what was done, what is next, what is blocked.
- **Review:** at the end of the iteration, what was delivered is shown running on the homologation servers.
- **Retrospective:** what to keep, what to change and one concrete action for the next iteration.

## Definition of Ready

A story can enter an iteration when:

- [ ] It has a clear user, capability and benefit.
- [ ] Acceptance criteria are written and testable.
- [ ] Dependencies on other stories or infrastructure are known.
- [ ] It fits in a few days of work; otherwise it is split.
- [ ] Packets, data or configuration that change are identified.

## Definition of Done

A change is done when:

- [ ] Every acceptance criterion is met and covered by a test.
- [ ] `./gradlew build` passes locally and in CI.
- [ ] New behavior that depends on PostgreSQL or Redis has an integration test.
- [ ] Metrics exist for new packets, commands and slow operations.
- [ ] Feature documentation is created or updated, in English and Portuguese.
- [ ] Architecture decisions that changed are recorded as an [ADR](../decisions/README.md).
- [ ] The pull request was reviewed and merged into `development`.
- [ ] It was validated on the homologation servers before reaching production.

## Releases

- `development` is always buildable. `homologation` is what the test servers run. `production` is what players use.
- A release is a pull request from `homologation` to `production`, tagged `vMAJOR.MINOR.PATCH` following [Semantic Versioning](https://semver.org/).
- The project version lives in `gradle.properties`.

## Architecture decisions

Decisions that are hard to reverse, or that future contributors will ask "why?" about, are written as Architecture Decision Records in [docs/decisions](../decisions/README.md). An ADR is short: context, decision, consequences.
