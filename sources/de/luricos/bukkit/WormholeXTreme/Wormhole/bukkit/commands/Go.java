package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/commands/Go.class */
public class Go implements CommandExecutor {
    private static boolean doGo(Player player, String[] args) {
        if (WXPermissions.checkPermission(player, WXPermissions.PermissionType.GO)) {
            if (args.length == 1) {
                String goGate = args[0].trim().replace("\n", "").replace("\r", "");
                Stargate s = StargateManager.getStargate(goGate);
                if (s != null) {
                    player.teleport(s.getGatePlayerTeleportLocation());
                    return true;
                }
                player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Gate does not exist: " + args[0]);
                return true;
            }
            return false;
        }
        player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
        return true;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] arguments = CommandUtilities.commandEscaper(args);
        if (arguments.length >= 3 || arguments.length <= 0) {
            return false;
        }
        return !CommandUtilities.playerCheck(sender) || doGo((Player) sender, arguments);
    }
}
