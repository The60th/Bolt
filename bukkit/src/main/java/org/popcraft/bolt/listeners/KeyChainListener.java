package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltItems;
import org.popcraft.bolt.util.KeyChainHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KeyChainListener implements Listener {
    private final BoltPlugin plugin;
    private final Map<UUID, Integer> openGuis = new HashMap<>();

    public KeyChainListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final ItemStack item = e.getItem();
        if (!BoltItems.isKeyChain(item)) {
            return;
        }
        e.setCancelled(true);
        final Player player = e.getPlayer();
        final int slot = player.getInventory().getHeldItemSlot();
        openGuis.put(player.getUniqueId(), slot);

        final Inventory inv = Bukkit.createInventory(new KeyChainHolder(slot), 27, Component.text("Key Chain"));

        final List<String> entries = BoltItems.getKeyChainKeys(item);
        int slotIndex = 0;
        for (final String entry : entries) {
            if (slotIndex >= 27) break;
            final String[] parts = entry.split(":");
            if (parts.length != 2) continue;
            try {
                final UUID lockId = UUID.fromString(parts[0]);
                final ItemStack keyItem = "M".equals(parts[1])
                        ? BoltItems.createMasterKey(lockId)
                        : BoltItems.createCopyKey(lockId);
                inv.setItem(slotIndex++, keyItem);
            } catch (IllegalArgumentException ignored) {
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof KeyChainHolder)) {
            return;
        }
        final Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null) return;

        final boolean clickedTop = clickedInv.getHolder() instanceof KeyChainHolder;
        final InventoryAction action = e.getAction();

        // Determine which item (if any) would enter the top inventory
        final ItemStack entering;
        if (clickedTop) {
            entering = switch (action) {
                case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> e.getCursor();
                case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                    final int hotbar = e.getHotbarButton();
                    yield hotbar >= 0
                            ? ((Player) e.getWhoClicked()).getInventory().getItem(hotbar)
                            : ((Player) e.getWhoClicked()).getInventory().getItemInOffHand();
                }
                default -> null;
            };
        } else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            entering = e.getCurrentItem();
        } else {
            entering = null;
        }

        if (entering != null && entering.getType() != Material.AIR) {
            if (!BoltItems.isMasterKey(entering) && !BoltItems.isCopyKey(entering)) {
                e.setCancelled(true);
                sendNotAKeyMessage(e.getWhoClicked());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof KeyChainHolder holder)) {
            return;
        }
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }
        openGuis.remove(player.getUniqueId());

        // Rebuild key chain from items remaining in the GUI
        final List<String> entries = new ArrayList<>();
        for (final ItemStack item : e.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            final UUID lockId = BoltItems.getKeyId(item);
            if (lockId == null) continue;
            final String type = BoltItems.isMasterKey(item) ? "M" : "C";
            entries.add(lockId + ":" + type);
        }

        final int slot = holder.getSlot();
        final ItemStack keyChainItem = player.getInventory().getItem(slot);
        if (keyChainItem != null && BoltItems.isKeyChain(keyChainItem)) {
            BoltItems.setKeyChainKeys(keyChainItem, entries);
            player.getInventory().setItem(slot, keyChainItem);
        }
    }

    private void sendNotAKeyMessage(final HumanEntity entity) {
        if (entity instanceof Player player) {
            BoltComponents.sendMessage(player, Translation.KEYCHAIN_NOT_A_KEY);
        }
    }
}
