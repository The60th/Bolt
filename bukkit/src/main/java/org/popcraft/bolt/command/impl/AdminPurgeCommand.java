package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AdminPurgeCommand extends BoltCommand {
    public AdminPurgeCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() < 1) {
            shortHelp(sender, arguments);
            return;
        }
        final String lockIdString = arguments.next();
        final UUID lockId;
        try {
            lockId = UUID.fromString(lockIdString);
        } catch (IllegalArgumentException e) {
            BoltComponents.sendMessage(
                    sender,
                    Translation.GENERIC_NOT_FOUND,
                    Placeholder.component(Translation.Placeholder.LOCK_ID, Component.text(lockIdString))
            );
            return;
        }
        final long count = plugin.loadProtections().stream()
                .filter(protection -> lockId.equals(protection.getLockId()))
                .peek(plugin::removeProtection)
                .count();
        BoltComponents.sendMessage(
                sender,
                Translation.PURGE,
                Placeholder.component(Translation.Placeholder.LOCK_ID, Component.text(lockIdString)),
                Placeholder.component(Translation.Placeholder.COUNT, Component.text(count))
        );
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_ADMIN_PURGE,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin purge"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_PURGE);
    }
}
