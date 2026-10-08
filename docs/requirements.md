# Survival entry requirements

## HUB-RET-002
WHEN the optional welcome is enabled and a first-time Hub player receives the owned Start Survival item THE SYSTEM SHALL send concise configurable chat explaining Enthusia and how to use the item to enter SMP.

Acceptance: disabled by default; no notice for returning/offline players, occupied slots without the owned item, build mode or disabled worlds. Empty messages suppress chat. Welcome never transfers, grants rewards or changes generic join actions/title; edition-neutral use-item wording remains readable on Java/Bedrock. Only the join handler sends it, not reload/item regrant. Runtime/client acceptance remains pending.

## HUB-RET-001
WHEN the optional Start Survival feature is enabled with a valid configured proxy target and an unoccupied configured hotbar slot THE SYSTEM SHALL offer a named Start Survival item that connects through the existing proxy action.

Acceptance: one direct interaction for Java and Bedrock, normal selector retained; no forced transfer or menu; disabled by default. Preserve player inventory, configured items, hider, PvP/bow slots, disabled worlds and build mode. Invalid target/slot or configured slot collision disables this item with an operator warning. Join selection only selects an actually granted owned item. Existing reload/quit cleanup applies.
