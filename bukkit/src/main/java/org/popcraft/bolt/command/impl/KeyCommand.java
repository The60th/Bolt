package org.popcraft.bolt.command.impl;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltItems;

import java.util.ArrayList;
import java.util.List;

public class KeyCommand extends BoltCommand {
    public KeyCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        final String sub = arguments.next();
        if (!"note".equalsIgnoreCase(sub)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_INVALID);
            return;
        }
        // Collect remaining words as the note text
        final List<String> words = new ArrayList<>();
        String word;
        while ((word = arguments.next()) != null) {
            words.add(word);
        }
        if (words.isEmpty()) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_NOT_ENOUGH_ARGS);
            return;
        }
        final ItemStack item = player.getInventory().getItemInMainHand();
        if (!BoltItems.isMasterKey(item) && !BoltItems.isCopyKey(item)) {
            BoltComponents.sendMessage(sender, Translation.KEY_NEED_KEY_MAIN_HAND);
            return;
        }
        BoltItems.setKeyNote(item, String.join(" ", words));
        BoltComponents.sendMessage(sender, Translation.KEY_NOTE_SET);
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() <= 1) {
            return List.of("note");
        }
        return List.of();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_SHORT_KEY);
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_KEY);
    }
}
