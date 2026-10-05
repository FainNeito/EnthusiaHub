# Hub PvP restoration - SPEAR record

## Spec
- HUB-PVP-01: When pvp_sword.enabled is true, the hub shall restore the configured sword hotbar slot, permission, selection countdown, armor/enchantments/trims and death effects.
- HUB-PVP-02: The hub shall allow melee and player projectile combat only between opted-in players; cancelled protection events and deluxehub.bypass.pvp minigame behavior shall remain intact.
- HUB-PVP-03: The hub shall mark generated PvP swords and armor unbreakable and prevent durability damage to player gear in enabled hub worlds. PvP item protection shall remain active even if general durability protection is disabled.
- HUB-PVP-04: The hub shall cancel countdowns and clear PvP state on quit/world exit/disable, preserve death/respawn settings, and respect disabled worlds and build mode.
- HUB-PVP-05: Existing MiniMessage and barrel/shelf protections from the local hub fixes shall be retained in the canonical source.

## Prove
Observed current master 641891b: PvPSwordModule/PvPSwordItem absent; live config pvp_sword.enabled=true and pvp_mode.enabled=false. Live general durability protection=true, but canonical source lacks it. Discord report requests sword toggle restoration. Old implementations recovered from origin/backup/master-pre-strafbefehl; these use FoliaLib and legacy NBT and require current Paper/PDC adaptation.

No project EARS validator/state helpers/requirements files exist on this base. This record tracks requirements/tasks/evidence manually. Do not claim historical red/green tests.

## Tasks and boundaries
1. Restore/adapt historical module and integrate current hotbar/combat/build-mode lifecycle.
2. Protect all gear durability and retain existing deployed fixes in source.
3. Run focused regressions and clean Gradle build on Java 21 target/Paper 1.21.11.
4. Open reviewable PR against master and inspect exact-head checks/review.
5. Production activation requires merged clean source and explicit operational authorization. No production mutations in this task before those gates.

## Architecture
The existing Bukkit module/hotbar infrastructure owns player/scheduler events. Use current Bukkit scheduler and PDC contract; no Folia claim/dependency. Preserve legacy pvp_mode settings and public hotbar options. Standalone Gradle repository owns build; existing release workflow incorrectly invokes Maven and lacks a PR check, so add Gradle verification for this PR. No monorepo/submodule owner is identified by this repository.

## Refine/evidence
Implemented the historical sword toggle using the current Paper scheduler/PDC contracts. Sword and all generated armor receive unbreakable metadata; world durability events are cancelled by default, with an independent active-PvP safeguard. Melee/projectile opt-in checks preserve minigame bypass and already-cancelled events. Lifecycle cleanup, configurable countdowns, death/respawn and armor/death effects remain supported. Both historical string sword enchantments and current YAML maps load without modifying saved configuration.

Retained the previously deployed MiniMessage and container/durability fixes in canonical source. The unrelated dirty BuildMode permission patch remains untouched in the original checkout.

Local verification: `gradle clean test shadowJar --console=plain`, Gradle 8.13/JDK 23, Java bytecode target 21, Paper API 1.21.11. Regression coverage includes 1000 durability attempts, excluded worlds, PvP protection when general durability is off, melee/projectile membership combinations and minigame bypass, sword/armor metadata, switching-away cancellation and shutdown/world-exit cleanup, both enchantment formats, packaged defaults/messages, and existing MiniMessage/barrel/shelf paths. Early new tests exposed incomplete Paper plugin namespace mocks; corrected the fixtures, then reran successfully. This is current-run regression evidence, not historical red/green evidence.

CI workflow now runs the same Gradle build for PRs and master. Existing manual Maven/all-branches release workflow remains incompatible with this Gradle repository; do not use it for production. The documented canonical local build is the Gradle command above from a clean reviewed merged commit. PR exact-head checks/review results are recorded on GitHub.

Delivery: https://github.com/BadgersMC/EnthusiaHub/pull/1 targets canonical master from FainNeito's fork because the connected account has pull/read permission only on BadgersMC/EnthusiaHub. All 12 local regression tests pass. Initial exact-head CI (9d3a0ca3a2623ba027c676897bd666002389895d, https://github.com/BadgersMC/EnthusiaHub/actions/runs/37267201592) returned `action_required`: a repository maintainer must approve the fork workflow before it can run. CodeRabbit reported a successful status but explicitly skipped reviewing this repository; that is not review approval. No human reviews are present. CI/review/merge remain pending.

Production has not been modified or restarted. No live/client acceptance is claimed. After review/merge and deployment authorization: build from clean merged master, record commit/version/hash/checks, back up the active artifact/configuration, deploy and restart the hub, then test the configured slot/countdown, two-player combat, death/respawn, gear durability, and barrel/shelf denial with actual clients.
