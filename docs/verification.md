# Start Survival evidence

Canonical repository BadgersMC/EnthusiaHub; isolated branch codex/start-survival-onboarding from fetched master 641891bccf946867dec722acd64cd3afb6177d1c. Original dirty builder checkout preserved. No project-local AGENTS/EARS/state helper exists; existing SPEAR skill read and requirements/tasks maintained.

Read-only production Hub inventory: EnthusiaHub-3.7.3-hub-fix.2.jar. Existing selector custom item in slot 0, pvp_sword slot 2; survival menu action `[PROXY] SMP`. New feature is optional/default disabled, target empty until explicitly configured. This source branch starts from authoritative master, not the deployed local patch; other production-only fixes and the separate PvP restoration PR must be reconciled before any release. No deployment of this PR by itself is approved.

Implementation uses the existing HotbarItem lifecycle/owned tag and ProxyAction wire format. Validated configuration prevents collisions with custom items, hider, PvP mode/sword, bow and arrow slot. Owned-item check prevents replacing player inventory. Grant/use reject build mode and disabled worlds. Join selection occurs only if the actual owned entry was granted; feature does not auto-transfer or force a form. Java and Bedrock use the same interaction path; no Floodgate API change.

Gradle 8.13/JDK 23, compiler release 21, Paper API 1.21.11. Initial compile/shadowJar succeeded before tests existed (NO-SOURCE is not a test pass). Five focused tests now pass with clean test shadowJar: config default/valid target, invalid target/slot, configured reservation collisions including arrows, inventory preservation/build-mode grant guard and actual proxy wire format/build/disabled-world interaction guards. No fabricated historical red-green evidence. A first fixture run encountered Material.isAir's server registry dependency; direct AIR/CAVE_AIR/VOID_AIR comparisons avoid that unnecessary registry lookup and tests pass. git diff --check passed.

Version 3.7.4 uses the repository's numeric major.minor.patch format because its Version.parse does not accept a suffix. Unmerged local test artifact only: build/libs/EnthusiaHub-3.7.4.jar, SHA-256 D62232F1E9864F7E7FE2DA0FFA4B1C565C70592B41CD07909EA5A0726D659120. Earlier development artifact is obsolete. No upload/activation/production config write performed.

Added Gradle PR verification workflow; existing manually triggered Maven release workflow is not a valid check for this Gradle checkout. Hosted CI and review are separate pending gates. Checked monorepo .gitmodules has Teleport but no Hub pin; verify actual Hub release ownership/integration before activation. No pin edits performed.

After both source PRs are reviewed/merged and release provenance reconciled, isolated Java/Bedrock acceptance must verify selected visible item, use-item transfer to actual SMP, selector/PvP preservation, occupied-slot behavior, build/disabled-world guards, reload/quit/world-change cleanup, first SMP Start Survival action, valid RTP outside protected regions and voluntary named-home guidance. Unit tests establish neither client rendering nor retention improvement.

Proposed production values after the separate authorization gates:

```yaml
start_survival:
  enabled: true
  server: SMP
  slot: 4
  select_on_join: true
```

Keep existing settings; generic source default selector occupies slot 4, so a fresh isolated default installation must choose a free entry slot or deliberately move its selector. The feature refuses collisions. Rollback disables start_survival through the normal approved reload path, restoring ordinary held-slot behavior and removing owned items; no player data is reset.
