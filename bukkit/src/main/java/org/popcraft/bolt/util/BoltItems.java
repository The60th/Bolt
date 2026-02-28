package org.popcraft.bolt.util;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class BoltItems {
    private static NamespacedKey LOCK_ID_KEY;
    private static NamespacedKey KEY_ID_KEY;
    private static NamespacedKey LOCKPICK_KEY;

    private BoltItems() {
    }

    public static void init(Plugin plugin) {
        LOCK_ID_KEY = new NamespacedKey(plugin, "lock_id");
        KEY_ID_KEY = new NamespacedKey(plugin, "key_id");
        LOCKPICK_KEY = new NamespacedKey(plugin, "lockpick");
    }

    public static UUID getLockId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        final PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        final String value = pdc.get(LOCK_ID_KEY, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static UUID getKeyId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        final PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        final String value = pdc.get(KEY_ID_KEY, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isLockpick(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        final PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(LOCKPICK_KEY);
    }

    public static boolean playerHasKey(Player player, UUID lockId) {
        if (lockId == null) {
            return false;
        }
        final PlayerInventory inventory = player.getInventory();
        for (final ItemStack item : inventory.getContents()) {
            final UUID keyId = getKeyId(item);
            if (lockId.equals(keyId)) {
                return true;
            }
        }
        return false;
    }

    public static UUID getHeldLockId(Player player) {
        return getLockId(player.getInventory().getItemInMainHand());
    }

    public static boolean isHoldingLockpick(Player player) {
        return isLockpick(player.getInventory().getItemInMainHand());
    }
}
