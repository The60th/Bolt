package gg.valar.valarCore.features.lockpicking;

import gg.valar.valarCore.utility.MessageUtil;
import gg.valar.valarCore.utility.Permissions;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;

/**
 * Demo/testing commands for the lockpicking minigames.
 * <p>
 * Usage:
 *   /lockpick tension [easy|medium|hard]
 *   /lockpick rake [easy|medium|hard]
 */
public class LockpickCommands {

    @Command("lockpick tension [difficulty]")
    @CommandDescription("Open the Tension & Sweep lockpicking minigame")
    @Permission(Permissions.LOCKPICK_ADMIN)
    public void tensionSweep(
            Player sender,
            @Default("medium") @Argument("difficulty") String difficultyName
    ) {
        LockDifficulty difficulty = LockDifficulty.fromString(difficultyName);
        if (difficulty == null) {
            MessageUtil.sendError(sender, "Unknown difficulty: " + difficultyName);
            MessageUtil.sendInfo(sender, "Options: easy, medium, hard");
            return;
        }

        MessageUtil.sendInfo(sender, "Starting Tension & Sweep (" + difficulty.getDisplayName() + "): "
                + difficulty.getTensionPins() + " pins, sweet spot " + difficulty.getTensionSweetSpotWidth()
                + " wide, decay every " + difficulty.getTensionDecayInterval() + "t");

        new TensionSweepGui(sender, difficulty).open();
    }

    @Command("lockpick rake [difficulty]")
    @CommandDescription("Open the Rake & Set lockpicking minigame")
    @Permission(Permissions.LOCKPICK_ADMIN)
    public void rakeSet(
            Player sender,
            @Default("medium") @Argument("difficulty") String difficultyName
    ) {
        LockDifficulty difficulty = LockDifficulty.fromString(difficultyName);
        if (difficulty == null) {
            MessageUtil.sendError(sender, "Unknown difficulty: " + difficultyName);
            MessageUtil.sendInfo(sender, "Options: easy, medium, hard");
            return;
        }

        MessageUtil.sendInfo(sender, "Starting Rake & Set (" + difficulty.getDisplayName() + "): "
                + difficulty.getRakePins() + " pins, " + difficulty.getRakeStartingPicks() + " picks, "
                + difficulty.getRakePopWindow() + "t window");

        new RakeSetGui(sender, difficulty).open();
    }
}
