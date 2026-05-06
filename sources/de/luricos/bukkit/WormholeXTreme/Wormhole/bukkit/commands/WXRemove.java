package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/commands/WXRemove.class */
public class WXRemove implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] a = CommandUtilities.commandEscaper(args);
        if (a.length < 1 || a.length > 2 || a[0].equalsIgnoreCase("-all")) {
            return false;
        }
        Stargate s = StargateManager.getStargate(a[0]);
        if (s != null) {
            if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, s, WXPermissions.PermissionType.REMOVE)) {
                boolean destroy = false;
                if (a.length == 2 && a[1].equalsIgnoreCase("-all")) {
                    destroy = true;
                }
                CommandUtilities.gateRemove(s, destroy);
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Wormhole Removed: " + s.getGateName());
                return true;
            }
            sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            return true;
        }
        sender.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Gate does not exist: " + a[0] + ". Remember proper capitalization.");
        return true;
    }
}
