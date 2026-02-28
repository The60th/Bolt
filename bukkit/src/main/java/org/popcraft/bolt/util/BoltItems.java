package org.popcraft.bolt.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

public final class BoltItems {
    private static NamespacedKey LOCK_ID_KEY;
    private static NamespacedKey KEY_ID_KEY;
    private static NamespacedKey LOCKPICK_KEY;
    private static NamespacedKey LOCK_TIER_KEY;
    private static NamespacedKey KEY_MASTER_KEY;
    private static NamespacedKey KEY_BLANK_KEY;
    private static NamespacedKey LOCKPICK_TIER_KEY;

    private BoltItems() {
    }

    public static void init(Plugin plugin) {
        LOCK_ID_KEY = new NamespacedKey(plugin, "lock_id");
        KEY_ID_KEY = new NamespacedKey(plugin, "key_id");
        LOCKPICK_KEY = new NamespacedKey(plugin, "lockpick");
        LOCK_TIER_KEY = new NamespacedKey(plugin, "lock_tier");
        KEY_MASTER_KEY = new NamespacedKey(plugin, "key_master");
        KEY_BLANK_KEY = new NamespacedKey(plugin, "key_blank");
        LOCKPICK_TIER_KEY = new NamespacedKey(plugin, "lockpick_tier");
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

    // --- Tier readers ---

    public static int getLockTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        final PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        final Integer value = pdc.get(LOCK_TIER_KEY, PersistentDataType.INTEGER);
        return value != null ? value : 0;
    }

    public static int getHeldLockTier(Player player) {
        return getLockTier(player.getInventory().getItemInMainHand());
    }

    public static boolean isKeyBlank(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        final PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(KEY_BLANK_KEY);
    }

    public static boolean isMasterKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        final PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(KEY_MASTER_KEY) && pdc.has(KEY_ID_KEY);
    }

    public static boolean isCopyKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        final PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(KEY_ID_KEY) && !pdc.has(KEY_MASTER_KEY);
    }

    public static int getLockpickTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        final PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        final Integer value = pdc.get(LOCKPICK_TIER_KEY, PersistentDataType.INTEGER);
        return value != null ? value : 0;
    }

    public static int getHeldLockpickTier(Player player) {
        return getLockpickTier(player.getInventory().getItemInMainHand());
    }

    // --- Item creation methods ---

    public static ItemStack createLockItem(int tier) {
        final ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        final ItemMeta meta = item.getItemMeta();
        final UUID lockId = UUID.randomUUID();
        meta.getPersistentDataContainer().set(LOCK_ID_KEY, PersistentDataType.STRING, lockId.toString());
        meta.getPersistentDataContainer().set(LOCK_TIER_KEY, PersistentDataType.INTEGER, tier);
        meta.displayName(noItalic(tierColor(tier)).append(Component.text(tierName(tier) + " Lock")));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createKeyBlank() {
        final ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        final ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_BLANK_KEY, PersistentDataType.BYTE, (byte) 1);
        meta.displayName(noItalic(NamedTextColor.GRAY).append(Component.text("Key Blank")));
        meta.lore(List.of(
                lore("Pair with a lock to create a master key")
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createMasterKey(UUID lockId) {
        final ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        final ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_ID_KEY, PersistentDataType.STRING, lockId.toString());
        meta.getPersistentDataContainer().set(KEY_MASTER_KEY, PersistentDataType.BYTE, (byte) 1);
        applyMasterKeyStyle(meta);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCopyKey(UUID lockId) {
        final ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        final ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_ID_KEY, PersistentDataType.STRING, lockId.toString());
        applyCopyKeyStyle(meta);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createLockpick(int tier) {
        final ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        final ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(LOCKPICK_KEY, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(LOCKPICK_TIER_KEY, PersistentDataType.INTEGER, tier);
        meta.displayName(noItalic(tierColor(tier)).append(Component.text(tierName(tier) + " Lockpick")));
        item.setItemMeta(meta);
        return item;
    }

    // --- In-place mutation methods ---

    public static void pairKeyBlankToLock(ItemStack item, UUID lockId) {
        final ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(KEY_BLANK_KEY);
        meta.getPersistentDataContainer().set(KEY_ID_KEY, PersistentDataType.STRING, lockId.toString());
        meta.getPersistentDataContainer().set(KEY_MASTER_KEY, PersistentDataType.BYTE, (byte) 1);
        applyMasterKeyStyle(meta);
        item.setItemMeta(meta);
    }

    public static void copyKeyToBlank(ItemStack item, UUID lockId) {
        final ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(KEY_BLANK_KEY);
        meta.getPersistentDataContainer().set(KEY_ID_KEY, PersistentDataType.STRING, lockId.toString());
        applyCopyKeyStyle(meta);
        item.setItemMeta(meta);
    }

    // --- Styling helpers ---

    private static void applyMasterKeyStyle(ItemMeta meta) {
        meta.displayName(noItalic(NamedTextColor.GOLD).append(Component.text("Master Key")));
        meta.lore(List.of(
                lore("Can be copied")
        ));
    }

    private static void applyCopyKeyStyle(ItemMeta meta) {
        meta.displayName(noItalic(NamedTextColor.YELLOW).append(Component.text("Copy Key")));
        meta.lore(List.of(
                lore("Cannot be copied")
        ));
    }

    private static Component noItalic(NamedTextColor color) {
        return Component.empty().color(color).decoration(TextDecoration.ITALIC, false);
    }

    private static NamedTextColor tierColor(int tier) {
        return switch (tier) {
            case 1 -> NamedTextColor.WHITE;
            case 2 -> NamedTextColor.AQUA;
            case 3 -> NamedTextColor.LIGHT_PURPLE;
            default -> NamedTextColor.GRAY;
        };
    }

    private static Component lore(String text) {
        return Component.text(text).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    // --- Utility ---

    public static String tierName(int tier) {
        return switch (tier) {
            case 1 -> "Basic";
            case 2 -> "Reinforced";
            case 3 -> "Fortified";
            default -> "Unknown";
        };
    }
}
