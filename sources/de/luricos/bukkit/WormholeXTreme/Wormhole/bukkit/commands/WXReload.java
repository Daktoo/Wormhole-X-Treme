package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/commands/WXReload.class */
public class WXReload implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, WXPermissions.PermissionType.CONFIG)) {
            String[] a = CommandUtilities.commandEscaper(args);
            if (a.length > 4 || a.length == 0) {
                return false;
            }
            if (a[0].equalsIgnoreCase("n") || a[0].equalsIgnoreCase("now")) {
                if (WormholeXTreme.getThisPlugin().reloadPlugin()) {
                    sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Reloading complete");
                    return true;
                }
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Error during reload see console log");
                return true;
            }
            return true;
        }
        sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
        return true;
    }
}
