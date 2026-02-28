package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.event.Cancellable;
import org.popcraft.bolt.event.LockBlockEvent;
import org.popcraft.bolt.event.LockEntityEvent;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltItems;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.ProtectableConfig;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.SchedulerUtil;
import org.popcraft.bolt.util.Time;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

abstract class InteractionListener {
    protected final BoltPlugin plugin;

    protected InteractionListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    protected boolean triggerAction(final Player player, final Protection protection, final Block block) {
        final boolean protectable = plugin.isProtectable(block);
        final ProtectableConfig config = plugin.getProtectableConfig(block);
        final Component displayName = Protections.displayType(block, player);
        final String lockPermission = "bolt.protection.lock.%s".formatted(block.getType().name().toLowerCase());
        return triggerAction(player, protection, block, protectable, config, displayName, lockPermission);
    }

    protected boolean triggerAction(final Player player, final Protection protection, final Entity entity) {
        final boolean protectable = plugin.isProtectable(entity);
        final ProtectableConfig config = plugin.getProtectableConfig(entity);
        final Component displayName = Protections.displayType(entity, player);
        final String lockPermission = "bolt.protection.lock.%s".formatted(entity.getType().name().toLowerCase());
        return triggerAction(player, protection, entity, protectable, config, displayName, lockPermission);
    }

    private boolean triggerAction(final Player player, final Protection protection, final Object object, final boolean protectable, final ProtectableConfig config, final Component displayName, final String lockPermission) {
        final BoltPlayer boltPlayer = plugin.player(player);
        final Action action = boltPlayer.getAction();
        if (action == null) {
            return false;
        }
        if (!player.hasPermission(action.getPermission())) {
            BoltComponents.sendMessage(player, Translation.COMMAND_NO_PERMISSION);
            return false;
        }
        final Action.Type actionType = action.getType();
        switch (actionType) {
            case LOCK -> {
                final UUID lockId = BoltItems.getHeldLockId(player);
                if (lockId == null && !action.isAdmin()) {
                    BoltComponents.sendMessage(
                            player,
                            Translation.HOLD_LOCK_ITEM,
                            plugin.isUseActionBar()
                    );
                    break;
                }
                if (protection != null) {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_LOCKED_ALREADY,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
                    );
                    break;
                }
                final boolean requiresLockPermission = config != null && config.lockPermission();
                if ((protectable || action.isAdmin()) && (!requiresLockPermission || player.hasPermission(lockPermission))) {
                    final Cancellable event;
                    if (object instanceof final Block block) {
                        event = new LockBlockEvent(player, block, false);
                    } else if (object instanceof final Entity entity) {
                        event = new LockEntityEvent(player, entity, false);
                    } else {
                        throw new IllegalStateException("Protection is not a block or entity");
                    }
                    plugin.getEventBus().post(event);
                    if (event.isCancelled()) {
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_LOCKED_CANCELLED,
                                plugin.isUseActionBar(),
                                Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
                        );
                        break;
                    }
                    final UUID effectiveLockId = lockId != null ? lockId : UUID.randomUUID();
                    final Protection newProtection;
                    if (object instanceof final Block block) {
                        newProtection = plugin.createProtection(block, effectiveLockId);
                    } else if (object instanceof final Entity entity) {
                        newProtection = plugin.createProtection(entity, effectiveLockId);
                    } else {
                        throw new IllegalStateException("Protection is not a block or entity");
                    }
                    plugin.saveProtection(newProtection);
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
                    );
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKABLE,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
                    );
                }
            }
            case UNLOCK -> {
                if (protection != null) {
                    if (plugin.canAccess(protection, player)) {
                        plugin.removeProtection(protection);
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_UNLOCKED,
                                plugin.isUseActionBar(),
                                Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
                        );
                    } else {
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_UNLOCKED_NO_PERMISSION,
                                plugin.isUseActionBar()
                        );
                    }
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
                    );
                }
            }
            case INFO -> {
                if (protection != null) {
                    BoltComponents.sendMessage(
                            player,
                            Translation.INFO,
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player)),
                            Placeholder.component(Translation.Placeholder.LOCK_ID, Component.text(protection.getLockId().toString())),
                            Placeholder.component(Translation.Placeholder.CREATED_TIME, Time.relativeTimestamp(protection.getCreated(), player)),
                            Placeholder.component(Translation.Placeholder.ACCESSED_TIME, Time.relativeTimestamp(protection.getAccessed(), player))
                    );
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
                    );
                }
            }
            case LOCKPICK -> {
                if (protection == null) {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
                    );
                    break;
                }
                if (!BoltItems.isHoldingLockpick(player)) {
                    BoltComponents.sendMessage(
                            player,
                            Translation.HOLD_LOCKPICK,
                            plugin.isUseActionBar()
                    );
                    break;
                }
                if (plugin.isJammed(protection)) {
                    BoltComponents.sendMessage(
                            player,
                            Translation.LOCK_JAMMED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
                    );
                    break;
                }
                final int outcome = ThreadLocalRandom.current().nextInt(3);
                if (outcome == 0) {
                    // Success: remove the protection
                    plugin.removeProtection(protection);
                    BoltComponents.sendMessage(
                            player,
                            Translation.LOCKPICK_SUCCESS,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
                    );
                } else if (outcome == 1) {
                    // Jam: set jammed until
                    final long jamUntil = System.currentTimeMillis() + (plugin.getLockpickJamDuration() * 1000L);
                    protection.setJammedUntil(jamUntil);
                    plugin.saveProtection(protection);
                    BoltComponents.sendMessage(
                            player,
                            Translation.LOCKPICK_JAM,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
                    );
                } else {
                    // Fail: message only
                    BoltComponents.sendMessage(
                            player,
                            Translation.LOCKPICK_FAIL,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
                    );
                }
            }
            case DEBUG -> BoltComponents.sendMessage(
                    player,
                    Optional.ofNullable(protection).map(Protection::toString).toString()
            );
        }
        boltPlayer.clearAction();
        return true;
    }
}
