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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GiveCommand extends BoltCommand {
    private static final List<String> TYPES = List.of("lock", "key-blank", "lockpick", "master-key", "copy-key", "key-chain");
    private static final List<String> TIERS = List.of("basic", "reinforced", "fortified");

    public GiveCommand(BoltPlugin plugin) {
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
        switch (type.toLowerCase()) {
            case "lock" -> {
                final String tierArg = arguments.next();
                final int tier = parseTier(tierArg);
                if (tier < 1) {
                    BoltComponents.sendMessage(sender, Translation.GIVE_INVALID_TIER);
                    return;
                }
                final ItemStack item = BoltItems.createLockItem(tier);
                player.getInventory().addItem(item);
                BoltComponents.sendMessage(sender, Translation.GIVE_LOCK,
                        Placeholder.component(Translation.Placeholder.TIER, Component.text(BoltItems.tierName(tier))));
            }
            case "key-blank" -> {
                final ItemStack item = BoltItems.createKeyBlank();
                player.getInventory().addItem(item);
                BoltComponents.sendMessage(sender, Translation.GIVE_KEY_BLANK);
            }
            case "lockpick" -> {
                final String tierArg = arguments.next();
                final int tier = parseTier(tierArg);
                if (tier < 1) {
                    BoltComponents.sendMessage(sender, Translation.GIVE_INVALID_TIER);
                    return;
                }
                final ItemStack item = BoltItems.createLockpick(tier);
                player.getInventory().addItem(item);
                BoltComponents.sendMessage(sender, Translation.GIVE_LOCKPICK,
                        Placeholder.component(Translation.Placeholder.TIER, Component.text(BoltItems.tierName(tier))));
            }
            case "master-key" -> {
                final String uuidArg = arguments.next();
                if (uuidArg == null) {
                    BoltComponents.sendMessage(sender, Translation.COMMAND_NOT_ENOUGH_ARGS);
                    return;
                }
                final UUID lockId;
                try {
                    lockId = UUID.fromString(uuidArg);
                } catch (IllegalArgumentException e) {
                    BoltComponents.sendMessage(sender, Translation.GIVE_INVALID_UUID);
                    return;
                }
                final ItemStack item = BoltItems.createMasterKey(lockId);
                player.getInventory().addItem(item);
                BoltComponents.sendMessage(sender, Translation.GIVE_MASTER_KEY,
                        Placeholder.component(Translation.Placeholder.LOCK_ID, Component.text(lockId.toString())));
            }
            case "copy-key" -> {
                final String uuidArg = arguments.next();
                if (uuidArg == null) {
                    BoltComponents.sendMessage(sender, Translation.COMMAND_NOT_ENOUGH_ARGS);
                    return;
                }
                final UUID lockId;
                try {
                    lockId = UUID.fromString(uuidArg);
                } catch (IllegalArgumentException e) {
                    BoltComponents.sendMessage(sender, Translation.GIVE_INVALID_UUID);
                    return;
                }
                final ItemStack item = BoltItems.createCopyKey(lockId);
                player.getInventory().addItem(item);
                BoltComponents.sendMessage(sender, Translation.GIVE_COPY_KEY,
                        Placeholder.component(Translation.Placeholder.LOCK_ID, Component.text(lockId.toString())));
            }
            case "key-chain" -> {
                final ItemStack item = BoltItems.createKeyChain();
                player.getInventory().addItem(item);
                BoltComponents.sendMessage(sender, Translation.GIVE_KEY_CHAIN);
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
        if ("lock".equalsIgnoreCase(type) || "lockpick".equalsIgnoreCase(type)) {
            return new ArrayList<>(TIERS);
        }
        return List.of();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_GIVE,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt give"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_GIVE);
    }

    private static int parseTier(String arg) {
        if (arg == null) {
            return -1;
        }
        return switch (arg.toLowerCase()) {
            case "basic", "1" -> 1;
            case "reinforced", "2" -> 2;
            case "fortified", "3" -> 3;
            default -> -1;
        };
    }
}
