package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;

import java.util.Collections;
import java.util.List;

public class LockpickCommand extends BoltCommand {
    public LockpickCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (sender instanceof final Player player) {
            if (!plugin.isLockpickingEnabled()) {
                BoltComponents.sendMessage(sender, Translation.COMMAND_NO_PERMISSION);
                return;
            }
            plugin.player(player).setAction(new Action(Action.Type.LOCKPICK, "bolt.command.lockpick"));
            BoltComponents.sendMessage(
                    player,
                    Translation.CLICK_ACTION,
                    plugin.isUseActionBar(),
                    Placeholder.component(Translation.Placeholder.ACTION, BoltComponents.resolveTranslation(Translation.LOCKPICK, player))
            );
        } else {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
        }
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_LOCKPICK,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt lockpick"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_LOCKPICK);
    }
}
