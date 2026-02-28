# Bolt Codebase Documentation

## Overview

Bolt is a modern Minecraft protection plugin for individual blocks and entities. It provides a lightweight, flexible system for players to lock blocks (chests, doors, etc.) and entities (armor stands, item frames, etc.) with granular permission-based access control.

**Authors:** pop4959, rymiel
**Platforms:** Bukkit, Spigot, Paper, Folia
**Java Version:** 21
**Minecraft Versions:** 1.18.2 - 1.21.4+

---

## Project Structure

```
Bolt/
├── common/           Platform-agnostic core logic (~31 classes)
├── bukkit/           Bukkit/Paper platform implementation (~140 classes)
├── folia/            Folia scheduler compatibility (2 classes)
├── paper/            Paper-specific wrapper (thin module)
├── docs/             Documentation
├── build.gradle.kts  Root Gradle build (Kotlin DSL)
└── settings.gradle.kts
```

### Build System

- **Gradle Kotlin DSL** with multi-module setup
- Shadow plugin for JAR shading (relocates kyori event, bstats, chunky-nbt)
- Modrinth publishing support
- Output: `Bolt-{version}.jar` (shaded), `Bolt-{version}-noshade.jar`, sources JAR

### Key Dependencies

| Dependency | Purpose |
|-----------|---------|
| Paper API 1.21.9 | Server API (compileOnly) |
| net.kyori:event-api:3.0.0 | Custom event bus |
| org.bstats:bstats-bukkit:3.0.2 | Server metrics |
| org.popcraft:chunky-nbt:1.3.127 | NBT utilities |

---

## Architecture

### Module Breakdown

#### `common/` - Core Logic (Platform-agnostic)

| Package | Key Classes | Purpose |
|---------|-------------|---------|
| `protection` | `Protection`, `BlockProtection`, `EntityProtection` | Core data models (sealed hierarchy) |
| `access` | `Access`, `AccessList`, `AccessRegistry` | Access type definitions and player access lists |
| `data` | `Store`, `MemoryStore`, `Profile`, `ProfileCache` | Storage interface and in-memory implementation |
| `source` | `Source`, `SourceType`, `SourceTypeRegistry` | Source resolution (who can access a lock) |
| `util` | `BoltPlayer`, `BlockLocation`, `Group`, `Mode`, `Action`, `Permission` | Session state, coordinates, enums |
| `lang` | `Translator`, `Translation`, `Strings` | i18n (16 languages) |

#### `bukkit/` - Platform Implementation

| Package | Key Classes | Purpose |
|---------|-------------|---------|
| `BoltPlugin` | `BoltPlugin.java` | Main plugin class, lifecycle, service wiring |
| `BoltAPI` | `BoltAPI.java` | Public API for other plugins |
| `command/impl` | `LockCommand`, `UnlockCommand`, `InfoCommand`, etc. | All player and admin commands |
| `data` | `SQLStore`, `SimpleProtectionCache`, `SimpleProfileCache` | SQL persistence + caching layer |
| `data/migration` | `LWCMigration`, `LocketteMigration` | Migration from other lock plugins |
| `listeners` | `BlockListener`, `EntityListener`, `InventoryListener`, `PlayerListener` | Event handling (~60+ events) |
| `matcher/block` | 62+ matchers (ChestMatcher, DoorMatcher, etc.) | Related block detection |
| `source` | `PlayerSourceTransformer`, `GroupSourceTransformer`, `PasswordSourceTransformer` | Source identifier transformation |
| `util` | `BoltComponents`, `Doors`, `Profiles`, `Protections` | Messaging, door logic, display helpers |

#### `folia/` - Folia Compatibility

| Class | Purpose |
|-------|---------|
| `FoliaUtil` | Detect Folia server |
| `SchedulerUtil` | Region-based scheduler abstraction |

---

## Core Data Models

### Protection Hierarchy

```
Protection (abstract, sealed)
├── id: UUID                          Unique protection identifier
├── owner: UUID                       Player who owns the lock
├── type: String                      Protection type ("private", "public", etc.)
├── created: long                     Creation timestamp
├── accessed: long                    Last access timestamp
├── access: Map<String, String>       Source-to-access-type mappings
│
├── BlockProtection
│   ├── world: String
│   ├── x, y, z: int
│   └── block: String                Material name
│
└── EntityProtection
    └── entity: String                Entity type name
```

### Source System

A `Source` represents "who can access a protection":

| Source Type | Identifier | Example |
|------------|------------|---------|
| `PLAYER` | UUID | `player:550e8400-...` |
| `PASSWORD` | SHA-1 hash | `password:a94a8fe5...` |
| `GROUP` | Group name | `group:my_friends` |
| `PERMISSION` | Permission node | `permission:bolt.vip` |
| `REDSTONE` | (none) | System source for redstone signals |
| `BLOCK` | (none) | System source for block activation |
| `DOOR` | (none) | System source for door mechanisms |

### Access Types (from config.yml)

| Type | Default Permissions |
|------|-------------------|
| `private` | Owner only, allows redstone |
| `display` | interact, open, redstone |
| `deposit` | deposit, open |
| `withdrawal` | withdraw, open |
| `public` | All permissions |

### Permission Constants

```
INTERACT          - Interact with block (open door, press button)
OPEN              - Open container inventory
DEPOSIT           - Place items into container
WITHDRAW          - Take items from container
MODIFY            - Modify protection settings
MOUNT             - Mount entity/vehicle
EDIT              - Edit access list
DESTROY           - Break block or kill entity
REDSTONE          - Trigger via redstone signal
ENTITY_INTERACT   - Non-player entity interaction
ENTITY_BREAK_DOOR - Non-player door breaking
AUTO_CLOSE        - Auto-close doors
```

---

## Storage Layer

### Store Interface

```java
// Block protections
loadBlockProtection(BlockLocation) -> CompletableFuture<BlockProtection>
loadBlockProtections()             -> CompletableFuture<Collection<BlockProtection>>
saveBlockProtection(BlockProtection)
removeBlockProtection(BlockProtection)

// Entity protections
loadEntityProtection(UUID)         -> CompletableFuture<EntityProtection>
saveEntityProtection(EntityProtection)
removeEntityProtection(EntityProtection)

// Groups & Access Lists
loadGroup(String), saveGroup(Group), removeGroup(Group)
loadAccessList(UUID), saveAccessList(AccessList), removeAccessList(AccessList)

// Persistence
pendingSave() -> long
flush()       -> CompletableFuture<Void>
```

### SQLStore (Primary Backend)

- Supports **SQLite** (default) and **MySQL**
- 4 tables: `blocks`, `entities`, `groups`, `access`
- Write-ahead queue pattern: saves are batched and flushed every 30 seconds
- Complex fields (access maps, member lists) serialized as JSON via GSON
- Async CompletableFuture-based loading

### SimpleProtectionCache (Caching Layer)

Wraps any Store with in-memory caching:
- Three-tier block cache: `BlockLocation -> UUID -> BlockProtection`
- Single-tier caches for entities, groups, access lists
- All data loaded into memory at startup for synchronous lookups
- Delegates writes to backing store

---

## Plugin Lifecycle

### onEnable()

1. Save default config
2. Create `SQLStore` from config (database type, credentials)
3. Create `Bolt` instance with `SimpleProtectionCache` wrapping `SQLStore`
4. Call `reload()` to load protection types, access types, protectable blocks/entities
5. Initialize `BoltComponents` (MiniMessage for chat formatting)
6. Register event listeners (Block, Entity, Inventory, Player)
7. Register commands
8. Create `CallbackManager` for clickable chat callbacks
9. Create `EventBus` for custom events
10. Load profile cache asynchronously
11. Initialize metrics (bStats)
12. Run data migrations if configured
13. Register `BoltAPI` via Bukkit Services Manager

### onDisable()

1. Disable BoltComponents
2. Unregister event handlers
3. Flush pending database changes
4. Unregister API service

---

## Command System

### Command Architecture

```java
abstract class BoltCommand {
    execute(CommandSender, String[] args)
    suggestions(CommandSender, String[] args) -> List<String>
    shortHelp(CommandSender, String[] args) -> String
    longHelp(CommandSender, String[] args) -> String
    hidden() -> boolean
}
```

### Player Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/bolt lock [type]` | `bolt.command.lock` | Activate lock mode, click block to lock |
| `/bolt unlock` | `bolt.command.unlock` | Activate unlock mode, click to remove lock |
| `/bolt info` | `bolt.command.info` | Click block to see protection details |
| `/bolt edit add\|remove <player>` | `bolt.command.edit` | Add/remove player from next clicked lock |
| `/bolt modify add\|remove <access> <source> [ids]` | `bolt.command.modify` | Advanced access modification |
| `/bolt trust add\|remove <source> <id> [access]` | `bolt.command.trust` | Global player-level trust (all owned locks) |
| `/bolt transfer <player>` | `bolt.command.transfer` | Transfer ownership of next clicked lock |
| `/bolt password [pass]` | `bolt.command.password` | Enter password for password-protected locks |
| `/bolt group create\|delete\|add\|remove\|list` | `bolt.command.group` | Manage player groups |
| `/bolt mode <PERSIST\|NOLOCK\|NOSPAM>` | `bolt.command.mode` | Toggle player modes |
| `/bolt help [cmd]` | `bolt.command` | Show help |

### Admin Commands (`/bolt admin <subcommand>`)

| Subcommand | Description |
|-----------|-------------|
| `cleanup` | Remove orphaned protections |
| `convert` | Migrate from LWC/Lockette |
| `debug` | Show raw protection data on click |
| `expire` | Remove inactive locks |
| `find <player>` | Find locks owned by player |
| `flush` | Force database flush |
| `nearby` | Show nearby protections |
| `purge <player>` | Remove all locks by player |
| `reload` | Reload configuration |
| `report` | Generate server protection report |
| `storage` | Show database statistics |
| `transfer` | Admin-level ownership transfer |
| `trust` | Admin-level trust management |

---

## Event Listeners

### BlockListener (~30 events)

Handles all block-related protection checks:

- **PlayerInteract**: Check INTERACT permission on right-click, trigger pending actions
- **BlockPlace**: Prevent placement on protected blocks; auto-protect new blocks
- **BlockBreak**: Check DESTROY permission; remove protection on break; handle double chests
- **BlockRedstone**: Check REDSTONE permission for signal propagation
- **BlockPistonExtend/Retract**: Prevent moving protected blocks
- **BlockExplode/EntityExplode**: Remove protected blocks from explosion lists
- **BlockFromTo**: Prevent liquid flow into protected blocks
- **BlockFade/Form/Spread**: Prevent natural changes to protected blocks
- **BlockDispense**: Check REDSTONE for dispensers; handle shulker box auto-protect
- **LeavesDecay**: Prevent decay on protected leaf blocks
- **SignChange**: Require INTERACT to edit signs
- **SpongeAbsorb**: Prevent absorption of protected blocks
- **BucketEmpty/Fill**: Check INTERACT + DEPOSIT/WITHDRAW for cauldrons

### EntityListener (~25 events)

- **EntityPlace/HangingPlace**: Auto-protect placed entities
- **EntityDeath**: Remove protection on death
- **EntityDamageByEntity**: Check DESTROY for attacks, WITHDRAW for item frames
- **PlayerInteractEntity/AtEntity**: Check INTERACT, DEPOSIT, WITHDRAW
- **PlayerArmorStandManipulate**: Check INTERACT + DEPOSIT/WITHDRAW
- **EntityMount/VehicleEnter**: Check MOUNT permission
- **EntityTransform**: Block transformation of protected entities
- **ExplosionPrime**: Block explosions of protected entities
- **ProjectileHit**: Check DESTROY for projectile shooter
- **EntityChangeBlock**: Handle special block changes (copper golems, end portal)

### InventoryListener

- **InventoryOpen**: Check OPEN permission
- **InventoryClick**: Fine-grained DEPOSIT/WITHDRAW based on click action type
- **InventoryDrag**: Check DEPOSIT for drag operations
- **InventoryMoveItem**: Check hopper/dropper transfers between protections

### PlayerListener

- **PlayerJoin**: Load player modes from `players/<uuid>.yml`, update profile cache
- **PlayerQuit**: Clean up BoltPlayer session state

---

## Access Control Flow

### Permission Check: `canAccess(protection, sourceResolver, permissions...)`

```
1. No protection → ALLOW
2. Is source the OWNER? → Check against OWNER permission set → ALLOW if match
3. Is source a MOD (bolt.mod)? → Check against DISPLAY set → ALLOW if match
4. Is source an ADMIN (bolt.admin)? → Check against OWNER set → ALLOW if match
5. Check protection TYPE permissions (e.g., "public" allows INTERACT)
6. For each entry in protection.access map:
   a. Does source match the entry's source? (via SourceResolver)
   b. Does the entry's access type include requested permission?
   c. If yes → remove from unresolved set
7. For each entry in owner's AccessList (global trust):
   a. Same source + permission matching
8. Check server permission fallback (bolt.permission.<perm>)
9. ALLOW if all permissions resolved, DENY otherwise
```

### Locking Flow

```
1. Player runs `/bolt lock [type]`
2. LockCommand sets Action.LOCK on BoltPlayer
3. Player clicks a block
4. BlockListener.onPlayerInteract fires
5. triggerAction() called:
   a. Check bolt.protection.lock.<block> permission
   b. If restricted type, check bolt.type.protection.<type>
   c. If already locked: change type (if has EDIT permission)
   d. If not locked: create BlockProtection, post LockBlockEvent
   e. Save to Store
6. Confirmation message sent
```

### Auto-Protection Flow

```
1. Player places a block
2. BlockListener.onBlockPlaceMonitor fires
3. Check: NOLOCK mode disabled? Block is protectable? Has auto-protect permission?
4. Create BlockProtection with player as owner
5. Post LockBlockEvent (cancellable)
6. Save to Store
```

---

## Matcher System

Block matchers detect related blocks that should share protection:

```java
interface Matcher<T> {
    boolean canMatch(T target);
    Collection<T> findMatch(T target);
    boolean enabled();
}
```

**62+ block matchers** handle: chests (double), doors (double), beds, bells, cakes, signs, banners, ladders, rails, redstone components, crops, coral, candles, decorated pots, and more.

Example: `ChestMatcher` finds the adjacent half of a double chest so both halves share one protection.

---

## Player Session State (BoltPlayer)

```
BoltPlayer (per-player, in-memory, not persisted)
├── action: Action              Pending action (LOCK, UNLOCK, INFO, EDIT, TRANSFER, DEBUG)
├── modifications: Map          Pending access modifications for /edit
├── sources: Set<Source>        Player's resolved access sources
├── modes: Set<Mode>            Active modes (PERSIST, NOLOCK, NOSPAM)
├── interacted: boolean         Interaction cooldown flag (cleared after 1 tick)
├── lockNil: boolean            Lock with NIL_UUID (global/admin lock)
└── passwords: List<String>     Entered passwords this session
```

---

## Configuration (config.yml)

```yaml
database:
  type: sqlite | mysql          # Storage backend
  path: ./Bolt/bolt.db          # SQLite file path
  hostname: ...                 # MySQL connection
  database: ...
  username: ...
  password: ...

protections:                    # Protection type definitions
  private:
    default: true
    allows: [redstone]
  public:
    allows: [interact, open, deposit, withdraw, ...]

access:                         # Access type definitions
  normal:
    default: true
  admin:
    require-permission: true

blocks:                         # Per-block protectable config
  CHEST:
    autoProtect: private
  OAK_DOOR:
    autoProtect: false

entities:                       # Per-entity protectable config
  ARMOR_STAND:
    autoProtect: private
```

---

## Public API (BoltAPI)

Exposed via Bukkit Services Manager:

```java
BoltAPI bolt = Bukkit.getServicesManager().load(BoltAPI.class);

// Check protection
bolt.isProtectable(block);
bolt.isProtected(block);
bolt.findProtection(block);

// Create/modify/remove
bolt.createProtection(block, player);
bolt.saveProtection(protection);
bolt.removeProtection(protection);

// Access control
bolt.canAccess(protection, player, "interact", "open");

// Extension points
bolt.registerPlayerSourceResolver(resolver);
bolt.registerSourceTransformer(type, transformer);
bolt.registerListener(LockBlockEvent.class, event -> { ... });
```

---

## Key Design Patterns

1. **Sealed Class Hierarchy** - Type-safe Protection variants (Block/Entity)
2. **Store Interface + Decorator** - Pluggable storage with caching wrapper
3. **Action Pattern** - Set action on player, execute on next interaction
4. **Source/Resolver** - Flexible, extensible access subject system
5. **Matcher Pattern** - Related block detection for shared protections
6. **Async Persistence** - Write-ahead queue with scheduled batch flush
7. **Constructor Injection** - Services wired via constructors, no DI framework
8. **Event Bus** - Kyori EventBus for cancellable custom events
