# Countertale v0.7.1 Changelog

## Scope

This release focused on build stability, runtime cleanup, API compatibility, localization, and low-risk internal refactoring. Gameplay rules were intentionally kept unchanged.

The main goal was to remove avoidable operational risk: fragile dependencies, deprecated Hytale APIs, leaked scheduled tasks, duplicated timer state, hard-coded player-facing text, and slow or unsafe in-memory lookups.

### Why this approach

The project should depend on the Hytale server API directly instead of an outdated parent artifact. This keeps the dependency graph explicit, makes Maven resolution predictable, and avoids Gradle-era assumptions leaking into the Maven build.

## Deprecated Inventory API Removal

- Removed usage of deprecated `com.hypixel.hytale.server.core.inventory.Inventory`.
- Replaced legacy inventory access with `InventoryComponent` containers:
  - `Hotbar`
  - `Storage`
  - `Armor`
- Updated lobby loot restoration to use `LootManager.getLobbyLoot(...)` instead of `player.getInventory()`.

### Benefit

The project no longer relies on an API marked for removal. That matters because inventory code is part of core gameplay: once the deprecated API disappears, old code would fail at compile time or runtime.

## Runtime State and Performance Refactor

- Reworked player stats lookup from list scanning to UUID-indexed access.
- Added `ConcurrentHashMap<UUID, PlayerStats>` for O(1) player stat resolution.
- Kept the existing list view where ordering or iteration is still needed.
- Switched several runtime collections to concurrency-safe structures where scheduled tasks and game events can touch them.
- Added runtime cleanup hooks for:
  - player health tracking
  - HUD tickers
  - queue HUDs
  - match flows
  - party state
  - shop data
  - player stats and slots
- Improved shutdown cleanup so scheduled tasks and static state are not left behind after plugin reload.

### Why this approach

The previous list-only model was acceptable for small tests, but it scales poorly and creates correctness issues under async callbacks. A UUID map matches the domain: player identity is stable, lookups are frequent, and removal must be deterministic.

## Match and Queue Optimizations

- Added direct player-to-match indexing in `MatchManager`.
- Reduced repeated scans when checking whether a player is already queued or in a match.
- Preserved active match ordering through a list while using a map for membership lookup.
- Optimized queue HUD updates by sharing a ticker instead of creating one scheduled task per HUD.
- Optimized game HUD updates through a shared ticker.

### Benefit

This reduces scheduler pressure and makes queue/match membership checks cheaper. It also makes plugin reload and match cleanup less likely to leave stale tasks running.

## Instance Pool and World Lifecycle

- Adjusted idle instance behavior so preloaded worlds remain lightweight until activated.
- Disabled ticking/block ticking while worlds are waiting in the pool.
- Activated ticking only when players are teleported into the instance.
- Added stronger cleanup paths for pooled and candidate instances.

### Why this approach

Preloading worlds is useful, but ticking idle worlds wastes server budget. Keeping worlds cold until use gives most of the latency benefit without paying the full runtime cost.

## HUD Timer Refactor

- Replaced duplicated timer fields in `GameHUD`:
  - `timerActive`
  - `lastTimerText`
  - `shopTimerActive`
  - `lastShopTimerText`
  - `invTimerActive`
  - `lastInvTimerText`
- Introduced a small private `HudTimer` state holder.
- Centralized:
  - active/inactive state
  - remaining seconds
  - last rendered text
  - "render only if changed" logic
- Removed unnecessary `remainingSeconds` fields from DM and FVF scoreboard HUDs where the value was only local to the scheduled tick.

### Why not a record

This state is mutable by design. A Java `record` would communicate value-object semantics, which would be misleading here. A small private class is a better fit because it owns mutable lifecycle state but does not leak outside `GameHUD`.

## Localization

- Added `Tenzinn.Core.Localization.Lang`.
- Reworked `MessageListeners` to use Hytale native translation keys instead of an external `messages.json`.
- Added English translations to:
  - `src/main/resources/Server/Languages/en-US/server.lang`
- Moved player-facing messages into `server.countertale.*` keys.
- Added parameterized translations for dynamic messages such as:
  - player names
  - money amounts
  - weapon names
  - map names
  - queue state
  - party state
  - wall/admin commands
- Kept raw messages only where they are data, not copy:
  - timers
  - numbers
  - currency values
  - blank UI fields
  - debug packet output
  - generated list rows

### Why this approach

Hytale already provides a localization pipeline. Using `Message.translation(...)` keeps UI/chat text in the same system as other server language assets. It is better than a custom JSON loader because it avoids a parallel localization mechanism and works with Hytale's expected language file structure.

For holograms/nameplates that currently require strings, `%server.countertale.key` tokens are still used. Normal chat and UI text use `Message.translation(...)`.

## Loot and Shop Improvements

- Reduced starter kit allocation churn by centralizing starter kit data.
- Categorized shop/loot data more predictably.
- Localized buy-phase, insufficient-money, loadout, and shop messages.
- Preserved current gameplay behavior.

### Benefit

The shop and loot paths are called frequently during match setup, respawn, and buying. Keeping allocations and branching simpler reduces avoidable overhead without changing balance.

## Remaining Weak Spots

### 1. Match flows are still too static

`MatchDeathmatch` and especially `MatchFVF` hold match state in static fields. This makes multiple simultaneous matches of the same mode unsafe or impossible to reason about. The current code assumes one active FVF flow and one active DM flow.

Recommended direction: move mode runtime into per-match controller objects, e.g. `DeathmatchController` and `FiveVsFiveController`, owned by `GameMatch`.

### 2. `RefactorTool` is doing too much

`RefactorTool` currently mixes player stat storage, loot selection, teleport/respawn, HUD updates, sound playback, scoring, and end-game page routing. That is not a utility class anymore; it is a service locator with gameplay logic.

Recommended split:

- `PlayerStatsService`
- `LoadoutService`
- `RespawnService`
- `HudUpdateService`
- `SoundService`
- `MatchEndService`

This should be done incrementally. Do not rewrite it in one pass.

### 3. `assert` is used for runtime validation

There are many `assert` calls around player refs, worlds, stores, and components. Java assertions are often disabled in production, so these checks may disappear exactly where they are needed most.

Recommended direction: replace runtime-critical assertions with explicit guards and logged exits.

Example:

```java
if (playerRef == null) {
    logger.warning("Cannot continue: playerRef is null");
    return;
}
```

### 4. Broad exception handling hides failure modes

Several systems catch `Exception`, call `printStackTrace()`, and continue. This makes failures noisy in development and weak in production: logs are inconsistent, errors are not classified, and callers often cannot react.

Recommended direction:

- use plugin logger consistently
- catch specific exception types where possible
- return explicit failure values from command/config/storage paths
- avoid `throw new RuntimeException(e)` in scheduled or command paths unless the server should actually fail that operation hard

### 5. Party state should be keyed by UUID

`PartyManager` still uses lists and index/id lookups for parties and invitations. It is better than before because collections are safer, but the model remains fragile:

- party id and list index are easy to confuse
- player comparison sometimes uses reference identity
- invitation lookup is linear

Recommended direction:

- `Map<UUID, PartyObject> partyByPlayer`
- `Map<Integer, PartyObject> partyById`
- `Map<UUID, InvitationParty> invitationByPlayer`

### 6. Configuration IO is duplicated and weakly typed

`MapListeners` and `RevenuesConfig` parse JSON manually and return null or throw runtime exceptions inconsistently.

Recommended direction:

- create a small config/storage layer
- return typed result objects or `Optional`
- validate required fields with useful errors
- keep file path resolution in one place

### 7. Hard-coded map names remain

FVF flow still uses hard-coded map names such as `Dust2` for temporal walls. This is a correctness risk once multiple maps or selected map voting are involved.

Recommended direction: use `myMatch.getMapId()` everywhere match-specific map data is needed.

### 8. HUD pages still duplicate scoreboard rendering

DM and FVF scoreboards have similar timer and table rendering logic. This is lower risk than match state, but it is still duplication.

Recommended direction: extract small helpers for:

- formatting match timer
- normalizing world/map display name
- clearing unused scoreboard slots

Do this after match flow state is cleaned up.

## Future Refactoring Plan

### Phase 1: Safety pass

- Replace production-critical `assert` usage with guard clauses.
- Replace `printStackTrace()` and `System.out.println()` with plugin logger usage.
- Remove hard-coded `Dust2` references from FVF flow.
- Add null-safe command argument handling for admin/economy/wall commands.

### Phase 2: Match runtime ownership

- Introduce a `MatchController` interface.
- Move DM and FVF timers out of static global fields.
- Store the controller inside `GameMatch`.
- Make `getTimer()`, `stopTimer()`, and round transitions instance-based.

### Phase 3: Split `RefactorTool`

- Move player stat storage into a dedicated service.
- Move loot/loadout logic into a dedicated service.
- Move respawn/teleport behavior into a dedicated service.
- Keep compatibility wrappers temporarily so call sites can migrate gradually.

### Phase 4: Party model cleanup

- Replace party/invitation linear scans with UUID-indexed maps.
- Stop exposing mutable party lists directly.
- Make leader transfer explicit and deterministic.

### Phase 5: Config and data validation

- Centralize plugin data paths.
- Add typed config loading for maps, revenues, shops, and holograms.
- Validate map spawn counts per mode before matches can start.
- Surface config errors clearly during plugin startup.

### Phase 6: UI rendering cleanup

- Extract scoreboard slot rendering helpers.
- Extract map display-name formatting.
- Consolidate timer formatting across HUDs.
- Keep UI-specific code in UI classes, but remove repeated formatting and fallback loops.
