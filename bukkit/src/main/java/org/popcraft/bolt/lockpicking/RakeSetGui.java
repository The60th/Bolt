package org.popcraft.bolt.lockpicking;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rake & Set lockpicking minigame GUI (4x9 chest).
 * <p>
 * Pins randomly pop up. The player must click them within a short window to set
 * them. Clicking empty slots or traps costs picks. Features combo rewards,
 * jammed pins requiring two clicks, and progressive speed-up.
 * <p>
 * Layout:
 * Row 0: [Picks info] . . [Status] . . . [Combo] [Cancel]
 * Row 1: Pin area     [9 slots: gray=empty, iron bars=popped, yellow glass=jammed, red glass=trap, emerald=set]
 * Row 2: Border
 * Row 3: Pick display  [tripwire hooks showing remaining picks]
 */
public class RakeSetGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final int SLOTS = 9;
    private static final int GAME_TICK_INTERVAL = 2; // 2 ticks = 100ms (10 Hz)

    private final Plugin plugin;
    private final Player player;
    private final LockDifficulty difficulty;
    private final Runnable onSuccess;
    private final Runnable onFailure;

    // Difficulty params
    private final int totalPins;
    private final int startingPicks;
    private final int popWindow;
    private final int maxSimultaneous;
    private final double trapChance;
    private final double jamChance;
    private final int basePopInterval;
    private final int comboThreshold;
    private final double speedRamp;

    // Game state
    private int pinsSet = 0;
    private int picksRemaining;
    private boolean running = false;
    private int tickCounter = 0;
    private int combo = 0;

    // Per-slot state
    private enum SlotState { EMPTY, POPPED, JAMMED, TRAP, SET }
    private final SlotState[] slotStates = new SlotState[SLOTS];
    private final int[] popTicksRemaining = new int[SLOTS];

    // InvUI components
    private Gui gui;
    private Window window;
    private PicksInfoItem picksInfoItem;
    private StatusItem statusItem;
    private ComboItem comboItem;
    private final PinSlotItem[] pinSlotItems = new PinSlotItem[SLOTS];
    private final PickDisplayItem[] pickDisplayItems = new PickDisplayItem[SLOTS];

    // Scheduler
    private BukkitTask gameTask;

    public RakeSetGui(Plugin plugin, Player player, LockDifficulty difficulty) {
        this(plugin, player, difficulty, null, null);
    }

    public RakeSetGui(Plugin plugin, Player player, LockDifficulty difficulty, Runnable onSuccess, Runnable onFailure) {
        this.plugin = plugin;
        this.player = player;
        this.difficulty = difficulty;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
        this.totalPins = difficulty.getRakePins();
        this.startingPicks = difficulty.getRakeStartingPicks();
        this.popWindow = difficulty.getRakePopWindow();
        this.maxSimultaneous = difficulty.getRakeMaxSimultaneous();
        this.trapChance = difficulty.getRakeTrapChance();
        this.jamChance = difficulty.getRakeJamChance();
        this.basePopInterval = difficulty.getRakePopInterval();
        this.comboThreshold = difficulty.getRakeComboThreshold();
        this.speedRamp = difficulty.getRakeSpeedRamp();
        this.picksRemaining = startingPicks;

        for (int i = 0; i < SLOTS; i++) {
            slotStates[i] = SlotState.EMPTY;
            popTicksRemaining[i] = 0;
        }
    }

    private static String toLegacy(Component component) {
        return LEGACY.serialize(component);
    }

    public void open() {
        picksInfoItem = new PicksInfoItem();
        statusItem = new StatusItem();
        comboItem = new ComboItem();

        for (int i = 0; i < SLOTS; i++) {
            pinSlotItems[i] = new PinSlotItem(i);
            pickDisplayItems[i] = new PickDisplayItem(i);
        }

        gui = Gui.normal()
                .setStructure(
                        "P . . S . . . C X",
                        "0 1 2 3 4 5 6 7 8",
                        ". . . . . . . . .",
                        "a b c d e f g h i"
                )
                .addIngredient('.', createBorderItem())
                .addIngredient('P', picksInfoItem)
                .addIngredient('S', statusItem)
                .addIngredient('C', comboItem)
                .addIngredient('X', createCancelItem())
                .addIngredient('0', pinSlotItems[0])
                .addIngredient('1', pinSlotItems[1])
                .addIngredient('2', pinSlotItems[2])
                .addIngredient('3', pinSlotItems[3])
                .addIngredient('4', pinSlotItems[4])
                .addIngredient('5', pinSlotItems[5])
                .addIngredient('6', pinSlotItems[6])
                .addIngredient('7', pinSlotItems[7])
                .addIngredient('8', pinSlotItems[8])
                .addIngredient('a', pickDisplayItems[0])
                .addIngredient('b', pickDisplayItems[1])
                .addIngredient('c', pickDisplayItems[2])
                .addIngredient('d', pickDisplayItems[3])
                .addIngredient('e', pickDisplayItems[4])
                .addIngredient('f', pickDisplayItems[5])
                .addIngredient('g', pickDisplayItems[6])
                .addIngredient('h', pickDisplayItems[7])
                .addIngredient('i', pickDisplayItems[8])
                .build();

        window = Window.single()
                .setViewer(player)
                .setTitle(toLegacy(Component.text("Lockpick: Rake & Set (" + difficulty.getDisplayName() + ")").color(NamedTextColor.GOLD)))
                .setGui(gui)
                .addCloseHandler(this::onClose)
                .build();

        window.open();
        startGame();
    }

    /** Returns the current effective pop interval, accounting for progressive speed-up. */
    private int currentPopInterval() {
        if (pinsSet == 0) return basePopInterval;
        double factor = Math.pow(speedRamp, pinsSet);
        return Math.max(GAME_TICK_INTERVAL * 2, (int) (basePopInterval * factor));
    }

    private void startGame() {
        running = true;

        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!running || !player.isOnline()) {
                stopGame(false);
                return;
            }

            tickCounter++;

            // Tick down pop timers and retract expired pops
            for (int i = 0; i < SLOTS; i++) {
                if (slotStates[i] == SlotState.POPPED || slotStates[i] == SlotState.TRAP || slotStates[i] == SlotState.JAMMED) {
                    popTicksRemaining[i]--;
                    if (popTicksRemaining[i] <= 0) {
                        slotStates[i] = SlotState.EMPTY;
                        player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.4f, 1.0f);
                        combo = 0;
                    }
                }
            }

            // Spawn new pops at the current (possibly accelerated) interval
            int interval = currentPopInterval();
            if (tickCounter * GAME_TICK_INTERVAL % interval == 0) {
                spawnPops();
            }

            refreshAll();
        }, GAME_TICK_INTERVAL, GAME_TICK_INTERVAL);
    }

    private void spawnPops() {
        int activePops = 0;
        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            if (slotStates[i] == SlotState.POPPED || slotStates[i] == SlotState.TRAP || slotStates[i] == SlotState.JAMMED) {
                activePops++;
            } else if (slotStates[i] == SlotState.EMPTY) {
                availableSlots.add(i);
            }
        }

        int toSpawn = Math.min(maxSimultaneous - activePops, availableSlots.size());
        if (toSpawn <= 0) return;

        Collections.shuffle(availableSlots);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < toSpawn; i++) {
            int slot = availableSlots.get(i);
            double roll = random.nextDouble();
            if (roll < trapChance) {
                slotStates[slot] = SlotState.TRAP;
            } else if (roll < trapChance + jamChance) {
                slotStates[slot] = SlotState.JAMMED;
            } else {
                slotStates[slot] = SlotState.POPPED;
            }
            popTicksRemaining[slot] = popWindow;
        }
    }

    private void refreshAll() {
        picksInfoItem.notifyWindows();
        statusItem.notifyWindows();
        comboItem.notifyWindows();
        for (int i = 0; i < SLOTS; i++) {
            pinSlotItems[i].notifyWindows();
            pickDisplayItems[i].notifyWindows();
        }
    }

    private void onPinSet() {
        pinsSet++;
        combo++;

        if (combo >= comboThreshold) {
            picksRemaining = Math.min(startingPicks, picksRemaining + 1);
            combo = 0;
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
        } else {
            float pitch = 1.0f + (combo * 0.3f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
        }
    }

    private void onMistake() {
        combo = 0;
    }

    private void stopGame(boolean success) {
        if (!running) return;
        running = false;

        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }

        if (player.isOnline()) {
            player.closeInventory();
            if (success) {
                player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1.0f, 1.2f);
                player.sendMessage(Component.text("Lock picked successfully!").color(NamedTextColor.GREEN));
                if (onSuccess != null) onSuccess.run();
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 0.3f);
                player.sendMessage(Component.text("Lockpick failed - out of picks!").color(NamedTextColor.RED));
                if (onFailure != null) onFailure.run();
            }
        }
    }

    private void onClose() {
        if (running) {
            stopGame(false);
        }
    }

    // ========== Item Implementations ==========

    private Item createBorderItem() {
        return new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setDisplayName(" "));
    }

    private Item createCancelItem() {
        return new SimpleItem(
                new ItemBuilder(Material.BARRIER)
                        .setDisplayName(toLegacy(Component.text("Cancel")
                                .color(NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false))),
                click -> stopGame(false)
        );
    }

    /** Top-left: shows remaining picks count. */
    private class PicksInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.TRIPWIRE_HOOK)
                    .setDisplayName(toLegacy(Component.text("Picks: " + picksRemaining)
                            .color(picksRemaining <= 2 ? NamedTextColor.RED : NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)))
                    .addLoreLines(
                            toLegacy(Component.text("Don't waste them!")
                                    .color(NamedTextColor.GRAY))
                    );
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
        }
    }

    /** Top-center: shows game status. */
    private class StatusItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.IRON_INGOT)
                    .setDisplayName(toLegacy(Component.text("Pins: " + pinsSet + "/" + totalPins)
                            .color(NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false)))
                    .addLoreLines(
                            toLegacy(Component.text("Difficulty: " + difficulty.getDisplayName())
                                    .color(NamedTextColor.GRAY)),
                            "",
                            toLegacy(Component.text("Click popped pins to set them")
                                    .color(NamedTextColor.WHITE)),
                            toLegacy(Component.text("Jammed pins need 2 clicks!")
                                    .color(NamedTextColor.GOLD))
                    );
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
        }
    }

    /** Top-right (next to cancel): shows combo streak. */
    private class ComboItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            boolean active = combo > 0;
            Material mat = active ? Material.BLAZE_POWDER : Material.GUNPOWDER;

            ItemBuilder builder = new ItemBuilder(mat)
                    .setDisplayName(toLegacy(Component.text("Combo: " + combo + "/" + comboThreshold)
                            .color(active ? NamedTextColor.YELLOW : NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));

            if (active) {
                builder.addLoreLines(
                        toLegacy(Component.text((comboThreshold - combo) + " more for a bonus pick!")
                                .color(NamedTextColor.WHITE))
                );
            } else {
                builder.addLoreLines(
                        toLegacy(Component.text("Set " + comboThreshold + " pins in a row")
                                .color(NamedTextColor.GRAY)),
                        toLegacy(Component.text("to earn a bonus pick")
                                .color(NamedTextColor.GRAY))
                );
            }

            return builder;
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
        }
    }

    /** Row 1: pin area slots. */
    private class PinSlotItem extends AbstractItem {
        private final int slot;

        PinSlotItem(int slot) {
            this.slot = slot;
        }

        @Override
        public ItemProvider getItemProvider() {
            return switch (slotStates[slot]) {
                case SET -> new ItemBuilder(Material.EMERALD)
                        .setDisplayName(toLegacy(Component.text("Pin Set \u2714")
                                .color(NamedTextColor.GREEN)
                                .decoration(TextDecoration.ITALIC, false)));
                case POPPED -> new ItemBuilder(Material.IRON_BARS)
                        .setDisplayName(toLegacy(Component.text("Pin Popped!")
                                .color(NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                                .decoration(TextDecoration.BOLD, true)))
                        .addLoreLines(
                                toLegacy(Component.text("Click to set!")
                                        .color(NamedTextColor.WHITE))
                        );
                case JAMMED -> new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE)
                        .setDisplayName(toLegacy(Component.text("Jammed!")
                                .color(NamedTextColor.GOLD)
                                .decoration(TextDecoration.ITALIC, false)
                                .decoration(TextDecoration.BOLD, true)))
                        .addLoreLines(
                                toLegacy(Component.text("Click to loosen first!")
                                        .color(NamedTextColor.WHITE))
                        );
                case TRAP -> new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                        .setDisplayName(toLegacy(Component.text("Pin Popped!")
                                .color(NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                                .decoration(TextDecoration.BOLD, true)))
                        .addLoreLines(
                                toLegacy(Component.text("Click to set!")
                                        .color(NamedTextColor.WHITE))
                        );
                case EMPTY -> new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .setDisplayName(toLegacy(Component.text("Empty")
                                .color(NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false)));
            };
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            if (!running) return;
            if (clickType != ClickType.LEFT) return;

            switch (slotStates[slot]) {
                case POPPED -> {
                    slotStates[slot] = SlotState.SET;
                    onPinSet();

                    if (pinsSet >= totalPins) {
                        stopGame(true);
                        return;
                    }
                }
                case JAMMED -> {
                    slotStates[slot] = SlotState.POPPED;
                    player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_STEP, 0.8f, 1.5f);
                }
                case TRAP -> {
                    slotStates[slot] = SlotState.EMPTY;
                    picksRemaining--;
                    onMistake();
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.8f, 0.3f);

                    List<Integer> setPins = new ArrayList<>();
                    for (int i = 0; i < SLOTS; i++) {
                        if (slotStates[i] == SlotState.SET) {
                            setPins.add(i);
                        }
                    }
                    if (!setPins.isEmpty()) {
                        int resetSlot = setPins.get(ThreadLocalRandom.current().nextInt(setPins.size()));
                        slotStates[resetSlot] = SlotState.EMPTY;
                        pinsSet--;
                    }

                    if (picksRemaining <= 0) {
                        stopGame(false);
                        return;
                    }
                }
                case EMPTY -> {
                    picksRemaining--;
                    onMistake();
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.6f, 0.5f);

                    if (picksRemaining <= 0) {
                        stopGame(false);
                        return;
                    }
                }
                case SET -> {
                    // Already set, do nothing
                }
            }
            refreshAll();
        }
    }

    /** Row 3: visual display of remaining picks. */
    private class PickDisplayItem extends AbstractItem {
        private final int slot;

        PickDisplayItem(int slot) {
            this.slot = slot;
        }

        @Override
        public ItemProvider getItemProvider() {
            if (slot < picksRemaining) {
                return new ItemBuilder(Material.TRIPWIRE_HOOK)
                        .setDisplayName(toLegacy(Component.text("Pick " + (slot + 1))
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)));
            }

            return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setDisplayName(toLegacy(Component.text("Broken")
                            .color(NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
        }
    }
}
