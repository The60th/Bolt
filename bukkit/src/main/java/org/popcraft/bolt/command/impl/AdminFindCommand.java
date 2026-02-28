package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Pagination;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class AdminFindCommand extends BoltCommand {

    public AdminFindCommand(BoltPlugin plugin) {
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
        final List<Protection> protectionsWithLockId = plugin.loadProtections().stream()
                .filter(protection -> lockId.equals(protection.getLockId()))
                .sorted(Comparator.comparingLong(Protection::getCreated).reversed())
                .toList();
        if (protectionsWithLockId.isEmpty()) {
            BoltComponents.sendMessage(sender, Translation.FIND_NONE);
            return;
        }
        Pagination.runPage(plugin, sender, protectionsWithLockId, 0);
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_ADMIN_FIND,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin find"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_FIND);
    }
}
