package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateDBManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.plugin.WormholeWorldsSupport;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/commands/WXStatus.class */
public class WXStatus implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, WXPermissions.PermissionType.CONFIG)) {
            String[] a = CommandUtilities.commandEscaper(args);
            if (a.length > 4 || a.length == 0) {
                return false;
            }
            if (args[0].equalsIgnoreCase("a") || args[0].equalsIgnoreCase("all")) {
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "§6----------------------------");
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "System status");
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "§6----------------------------");
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "DBConnection: " + (StargateDBManager.isConnected() ? "§2ready" : "§4failed"));
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Wxw-link: " + (WormholeWorldsSupport.isEnabled() ? "§2ready" : "§4failed"));
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader.toString());
                return true;
            }
            return true;
        }
        sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
        return true;
    }
}
