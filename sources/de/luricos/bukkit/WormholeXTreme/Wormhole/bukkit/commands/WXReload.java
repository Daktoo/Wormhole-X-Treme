package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WXReload implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, WXPermissions.PermissionType.CONFIG)) {
            // Allow both "/wxreload" (no args) and "/wxreload n" / "/wxreload now"
            if (args.length == 0 || args[0].equalsIgnoreCase("n") || args[0].equalsIgnoreCase("now")) {
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Reloading WormholeXTreme...");
                if (WormholeXTreme.getThisPlugin().reloadPlugin()) {
                    sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Reloading complete.");
                } else {
                    sender.sendMessage(ConfigManager.MessageStrings.errorHeader + "Error during reload, see console log.");
                }
                return true;
            }
            // Unknown subcommand — show usage
            return false;
        }
        sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
        return true;
    }
}
