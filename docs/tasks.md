# Start Survival tasks

- [x] SPEC: fresh isolated branch from canonical BadgersMC/EnthusiaHub master 641891bccf946867dec722acd64cd3afb6177d1c. Existing dirty builder checkout preserved. No local AGENTS, requirements, SPEAR validator/state helper found. Existing SPEAR brownfield skill read.
- [x] PROVE: current source has only generic configurable selector, no dedicated direct survival entry. Read-only production selector hotbar occupies slot 0; proposed direct item uses separate slot 4. Source absence evidence, not historical red tests.
- [x] ENGINE: optional direct entry using existing proxy/hotbar adapter; default disabled, invalid/empty target fail closed.
- [x] ARCH: inventory/slot/build/world boundaries verified locally; production `[PROXY] SMP` target read-only verified. Existing numeric version parser retained. Production-only patches require reconciliation before release.
- [x] REFINE-local: five focused tests and Gradle clean test/shadowJar pass; git diff --check passed. See verification.md.
- [ ] REVIEW: publish PR and inspect exact-head checks/actionable findings; hosted CI and source review still pending.
- [ ] RUNTIME: merged-source release/monorepo ownership, authorized staging/activation and Java/Bedrock fresh-player journey acceptance.
