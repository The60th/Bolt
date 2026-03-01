package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.lockpicking.LockDifficulty;
import org.popcraft.bolt.lockpicking.RakeSetGui;
import org.popcraft.bolt.lockpicking.TensionSweepGui;
import org.popcraft.bolt.util.BoltComponents;

import java.util.ArrayList;
import java.util.List;

public class LockpickTestCommand extends BoltCommand {
    private static final List<String> TYPES = List.of("tension", "rake");
    private static final List<String> DIFFICULTIES = List.of("easy", "medium", "hard");

    public LockpickTestCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        final String type = arguments.next();
        if (type == null) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_NOT_ENOUGH_ARGS);
            return;
        }
        final String difficultyArg = arguments.next();
        final String difficultyName = difficultyArg != null ? difficultyArg : "medium";
        final LockDifficulty difficulty = LockDifficulty.fromString(difficultyName);
        if (difficulty == null) {
            BoltComponents.sendMessage(sender, Translation.LOCKPICK_TEST_INVALID_DIFFICULTY);
            return;
        }
        switch (type.toLowerCase()) {
            case "tension" -> {
                BoltComponents.sendMessage(sender, Translation.LOCKPICK_TEST_STARTING,
                        Placeholder.component(Translation.Placeholder.MODE, Component.text("Tension & Sweep")),
                        Placeholder.component(Translation.Placeholder.TIER, Component.text(difficulty.getDisplayName())));
                new TensionSweepGui(plugin, player, difficulty).open();
            }
            case "rake" -> {
                BoltComponents.sendMessage(sender, Translation.LOCKPICK_TEST_STARTING,
                        Placeholder.component(Translation.Placeholder.MODE, Component.text("Rake & Set")),
                        Placeholder.component(Translation.Placeholder.TIER, Component.text(difficulty.getDisplayName())));
                new RakeSetGui(plugin, player, difficulty).open();
            }
            default -> BoltComponents.sendMessage(sender, Translation.COMMAND_INVALID);
        }
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        final Arguments copy = arguments.copy();
        final String type = copy.next();
        if (arguments.remaining() <= 1) {
            return new ArrayList<>(TYPES);
        }
        if ("tension".equalsIgnoreCase(type) || "rake".equalsIgnoreCase(type)) {
            return new ArrayList<>(DIFFICULTIES);
        }
        return List.of();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_LOCKPICK_TEST,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt lockpicktest"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_LOCKPICK_TEST);
    }
}
