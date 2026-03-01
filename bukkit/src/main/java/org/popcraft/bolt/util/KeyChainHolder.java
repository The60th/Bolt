package org.popcraft.bolt.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class KeyChainHolder implements InventoryHolder {
    private final int slot;

    public KeyChainHolder(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
