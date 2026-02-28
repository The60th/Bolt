package org.popcraft.bolt.command.impl;

import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.util.BoltComponents;

import java.util.Collections;
import java.util.List;

public class AdminConvertCommand extends BoltCommand {
    public AdminConvertCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, "Migration from other plugins is no longer supported in this version.");
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, "Migration from other plugins is no longer supported.");
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, "Migration from other plugins is no longer supported.");
    }
}
