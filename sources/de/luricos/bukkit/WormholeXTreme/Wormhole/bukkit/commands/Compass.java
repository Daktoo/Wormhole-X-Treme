package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/commands/Compass.class */
public class Compass implements CommandExecutor {
    private static boolean doCompass(Player player) {
        if (WXPermissions.checkPermission(player, WXPermissions.PermissionType.COMPASS)) {
            Stargate closest = StargateManager.findClosestStargate(player.getLocation());
            if (closest != null) {
                player.setCompassTarget(closest.getGatePlayerTeleportLocation());
                player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Compass set to wormhole: " + closest.getGateName());
                return true;
            }
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "No wormholes to track!");
            return true;
        }
        player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
        return true;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return !CommandUtilities.playerCheck(sender) || doCompass((Player) sender);
    }
}
