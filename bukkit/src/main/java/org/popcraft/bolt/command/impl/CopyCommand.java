package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltItems;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CopyCommand extends BoltCommand {
    public CopyCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        final ItemStack mainHand = player.getInventory().getItemInMainHand();
        final ItemStack offHand = player.getInventory().getItemInOffHand();
        if (!BoltItems.isMasterKey(mainHand)) {
            BoltComponents.sendMessage(player, Translation.COPY_NEED_MASTER_KEY_MAIN_HAND, plugin.isUseActionBar());
            return;
        }
        if (!BoltItems.isKeyBlank(offHand)) {
            BoltComponents.sendMessage(player, Translation.COPY_NEED_KEY_BLANK_OFF_HAND, plugin.isUseActionBar());
            return;
        }
        final UUID lockId = BoltItems.getKeyId(mainHand);
        BoltItems.copyKeyToBlank(offHand, lockId);
        BoltComponents.sendMessage(player, Translation.COPY_SUCCESS, plugin.isUseActionBar(),
                Placeholder.component(Translation.Placeholder.LOCK_ID, Component.text(lockId.toString())));
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_COPY,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt copy"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_COPY);
    }
}
