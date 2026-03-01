package gg.valar.valarCore.features.lockpicking;

import gg.valar.valarCore.ValarCore;
import gg.valar.valarCore.utility.Colors;
import gg.valar.valarCore.utility.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitTask;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Tension & Sweep lockpicking minigame GUI (4x9 chest).
 * <p>
 * The player probes a hidden sweet spot by left-clicking slots (pitch indicates
 * proximity). Shift-left-click attempts to set the pin at that position.
 * <p>
 * Layout:
 * Row 0: Pin progress    [emerald=set, iron bars=unset, black glass=unused]
 * Row 1: Sweep zone      [9 clickable gray glass slots]
 * Row 2: Border
 * Row 3: Tension bar     [9 iron bars, depleting right-to-left]
 */
public class TensionSweepGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final int SLOTS = 9;
    private static final int GAME_TICK_INTERVAL = 5; // 5 ticks = 250ms (8 Hz)

    private final Player player;
    private final LockDifficulty difficulty;

    // Difficulty params
    private final int totalPins;
    private final int sweetSpotWidth;
    private final int decayInterval;
    private final int wrongPenalty;
    private final boolean sweetSpotShifts;
    private final int shiftInterval;

    // Game state
    private int pinsSet = 0;
    private int tensionBars = 9;
    private int sweetSpotStart;
    private boolean running = false;
    private int tickCounter = 0;

    // Visual state
    private int lastProbedSlot = -1;
    private int probeHighlightRemaining = 0; // iterations remaining for yellow highlight
    private int flashSlot = -1;
    private int flashRemaining = 0; // iterations remaining for red flash

    // InvUI components
    private Gui gui;
    private Window window;
    private final PinItem[] pinItems = new PinItem[SLOTS];
    private final SweepSlotItem[] sweepItems = new SweepSlotItem[SLOTS];
    private final TensionBarItem[] tensionItems = new TensionBarItem[SLOTS];

    // Scheduler
    private BukkitTask gameTask;

    public TensionSweepGui(Player player, LockDifficulty difficulty) {
        this.player = player;
        this.difficulty = difficulty;
        this.totalPins = difficulty.getTensionPins();
        this.sweetSpotWidth = difficulty.getTensionSweetSpotWidth();
        this.decayInterval = difficulty.getTensionDecayInterval();
        this.wrongPenalty = difficulty.getTensionWrongPenalty();
        this.sweetSpotShifts = difficulty.getTensionSweetSpotShifts();
        this.shiftInterval = difficulty.getTensionShiftInterval();
        this.sweetSpotStart = randomSweetSpot();
    }

    private static String toLegacy(Component component) {
        return LEGACY.serialize(component);
    }

    public void open() {
        for (int i = 0; i < SLOTS; i++) {
            pinItems[i] = new PinItem(i);
            sweepItems[i] = new SweepSlotItem(i);
            tensionItems[i] = new TensionBarItem(i);
        }

        gui = Gui.normal()
                .setStructure(
                        "0 1 2 3 4 5 6 7 8",
                        "a b c d e f g h i",
                        ". . . . . . . . .",
                        "A B C D E F G H I"
                )
                .addIngredient('.', createBorderItem())
                .addIngredient('0', pinItems[0])
                .addIngredient('1', pinItems[1])
                .addIngredient('2', pinItems[2])
                .addIngredient('3', pinItems[3])
                .addIngredient('4', pinItems[4])
                .addIngredient('5', pinItems[5])
                .addIngredient('6', pinItems[6])
                .addIngredient('7', pinItems[7])
                .addIngredient('8', pinItems[8])
                .addIngredient('a', sweepItems[0])
                .addIngredient('b', sweepItems[1])
                .addIngredient('c', sweepItems[2])
                .addIngredient('d', sweepItems[3])
                .addIngredient('e', sweepItems[4])
                .addIngredient('f', sweepItems[5])
                .addIngredient('g', sweepItems[6])
                .addIngredient('h', sweepItems[7])
                .addIngredient('i', sweepItems[8])
                .addIngredient('A', tensionItems[0])
                .addIngredient('B', tensionItems[1])
                .addIngredient('C', tensionItems[2])
                .addIngredient('D', tensionItems[3])
                .addIngredient('E', tensionItems[4])
                .addIngredient('F', tensionItems[5])
                .addIngredient('G', tensionItems[6])
                .addIngredient('H', tensionItems[7])
                .addIngredient('I', tensionItems[8])
                .build();

        window = Window.single()
                .setViewer(player)
                .setTitle(toLegacy(Component.text("Lockpick: Tension & Sweep (" + difficulty.getDisplayName() + ")").color(Colors.PRIMARY)))
                .setGui(gui)
                .addCloseHandler(this::onClose)
                .build();

        window.open();
        startGame();
    }

    private void startGame() {
        running = true;

        gameTask = Bukkit.getScheduler().runTaskTimer(ValarCore.getInstance(), () -> {
            if (!running || !player.isOnline()) {
                stopGame(false);
                return;
            }

            tickCounter++;

            // Decay tension at interval (counter is in game ticks of 5 server ticks each)
            if (tickCounter * GAME_TICK_INTERVAL % decayInterval == 0) {
                tensionBars = Math.max(0, tensionBars - 1);
                if (tensionBars <= 0) {
                    stopGame(false);
                    return;
                }
            }

            // Sweet spot shifts on hard mode
            if (sweetSpotShifts && tickCounter * GAME_TICK_INTERVAL % shiftInterval == 0) {
                sweetSpotStart = randomSweetSpot();
            }

            // Tick down highlight timers
            if (probeHighlightRemaining > 0) {
                probeHighlightRemaining--;
                if (probeHighlightRemaining <= 0) {
                    lastProbedSlot = -1;
                }
            }
            if (flashRemaining > 0) {
                flashRemaining--;
                if (flashRemaining <= 0) {
                    flashSlot = -1;
                }
            }

            refreshAll();
        }, GAME_TICK_INTERVAL, GAME_TICK_INTERVAL);
    }

    private int randomSweetSpot() {
        return ThreadLocalRandom.current().nextInt(0, SLOTS - sweetSpotWidth + 1);
    }

    private boolean isInSweetSpot(int slot) {
        return slot >= sweetSpotStart && slot < sweetSpotStart + sweetSpotWidth;
    }

    private int distanceToSweetSpot(int slot) {
        if (isInSweetSpot(slot)) return 0;
        int distStart = Math.abs(slot - sweetSpotStart);
        int distEnd = Math.abs(slot - (sweetSpotStart + sweetSpotWidth - 1));
        return Math.min(distStart, distEnd);
    }

    private void refreshAll() {
        for (int i = 0; i < SLOTS; i++) {
            pinItems[i].notifyWindows();
            sweepItems[i].notifyWindows();
            tensionItems[i].notifyWindows();
        }
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
                MessageUtil.sendSuccess(player, "Lock picked successfully!");
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 0.3f);
                MessageUtil.sendError(player, "Lockpick failed - tension lost!");
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

    /** Row 0: shows pin progress (emerald=set, iron bars=unset, black glass=unused). */
    private class PinItem extends AbstractItem {
        private final int slot;

        PinItem(int slot) {
            this.slot = slot;
        }

        @Override
        public ItemProvider getItemProvider() {
            if (slot < totalPins) {
                if (slot < pinsSet) {
                    // Pin already set
                    return new ItemBuilder(Material.EMERALD)
                            .setDisplayName(toLegacy(Component.text("Pin " + (slot + 1) + " \u2714")
                                    .color(Colors.SUCCESS)
                                    .decoration(TextDecoration.ITALIC, false)));
                } else {
                    // Pin not yet set
                    return new ItemBuilder(Material.IRON_BARS)
                            .setDisplayName(toLegacy(Component.text("Pin " + (slot + 1))
                                    .color(Colors.MUTED)
                                    .decoration(TextDecoration.ITALIC, false)));
                }
            }
            // Unused slot
            return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setDisplayName(" ");
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
        }
    }

    /** Row 1: sweep zone - clickable probe/set slots. */
    private class SweepSlotItem extends AbstractItem {
        private final int slot;

        SweepSlotItem(int slot) {
            this.slot = slot;
        }

        @Override
        public ItemProvider getItemProvider() {
            // Red flash for wrong attempt
            if (slot == flashSlot && flashRemaining > 0) {
                return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                        .setDisplayName(toLegacy(Component.text("Wrong!")
                                .color(Colors.ERROR)
                                .decoration(TextDecoration.ITALIC, false)));
            }

            // Yellow highlight for last probed slot
            if (slot == lastProbedSlot && probeHighlightRemaining > 0) {
                return new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE)
                        .setDisplayName(toLegacy(Component.text("Probing...")
                                .color(Colors.SECONDARY)
                                .decoration(TextDecoration.ITALIC, false)))
                        .addLoreLines(
                                toLegacy(Component.text("Shift-click to set pin")
                                        .color(Colors.MUTED))
                        );
            }

            return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .setDisplayName(toLegacy(Component.text("Slot " + (slot + 1))
                            .color(Colors.LIGHT)
                            .decoration(TextDecoration.ITALIC, false)))
                    .addLoreLines(
                            toLegacy(Component.text("Click to probe")
                                    .color(Colors.MUTED)),
                            toLegacy(Component.text("Shift-click to set pin")
                                    .color(Colors.MUTED))
                    );
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            if (!running) return;

            if (clickType == ClickType.SHIFT_LEFT) {
                // Attempt to set pin
                if (isInSweetSpot(slot)) {
                    // Success!
                    pinsSet++;
                    tensionBars = Math.min(9, tensionBars + 2); // Reward: restore some tension
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);

                    if (pinsSet >= totalPins) {
                        stopGame(true);
                        return;
                    }

                    // New sweet spot for next pin
                    sweetSpotStart = randomSweetSpot();
                    lastProbedSlot = -1;
                    probeHighlightRemaining = 0;
                } else {
                    // Wrong - penalty
                    tensionBars = Math.max(0, tensionBars - wrongPenalty);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.8f, 0.5f);

                    flashSlot = slot;
                    flashRemaining = 4;

                    if (tensionBars <= 0) {
                        stopGame(false);
                        return;
                    }
                }
                refreshAll();
            } else if (clickType == ClickType.LEFT) {
                // Probe - play pitch based on distance
                int distance = distanceToSweetSpot(slot);
                float pitch = switch (distance) {
                    case 0 -> 2.0f;
                    case 1 -> 1.6f;
                    case 2 -> 1.2f;
                    case 3 -> 0.9f;
                    default -> 0.5f;
                };
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, pitch);

                lastProbedSlot = slot;
                probeHighlightRemaining = 3; // ~750ms at 8Hz
                refreshAll();
            }
        }
    }

    /** Row 3: tension bar (iron bars depleting right-to-left). */
    private class TensionBarItem extends AbstractItem {
        private final int slot;

        TensionBarItem(int slot) {
            this.slot = slot;
        }

        @Override
        public ItemProvider getItemProvider() {
            if (slot < tensionBars) {
                // Active bar
                Material mat;
                if (tensionBars <= 3) {
                    mat = Material.RED_STAINED_GLASS_PANE;
                } else if (tensionBars <= 5) {
                    mat = Material.ORANGE_STAINED_GLASS_PANE;
                } else {
                    mat = Material.WHITE_STAINED_GLASS_PANE;
                }
                return new ItemBuilder(mat)
                        .setDisplayName(toLegacy(Component.text("Tension")
                                .color(tensionBars <= 3 ? Colors.ERROR : Colors.LIGHT)
                                .decoration(TextDecoration.ITALIC, false)))
                        .addLoreLines(
                                toLegacy(Component.text(tensionBars + "/9 remaining")
                                        .color(Colors.MUTED))
                        );
            }

            // Depleted bar
            return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setDisplayName(toLegacy(Component.text("Empty")
                            .color(Colors.MUTED)
                            .decoration(TextDecoration.ITALIC, false)));
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
        }
    }
}
