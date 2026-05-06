package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateHelper;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/commands/BuildList.class */
public class BuildList implements CommandExecutor {
    private static boolean listBuilds(Player player, String[] args) {
        if (!WXPermissions.checkPermission(player, WXPermissions.PermissionType.CONFIG)) {
            return false;
        }
        int gateID = 1;
        StringBuilder shapeNames = new StringBuilder();
        for (String shapeName : StargateHelper.getShapeNames()) {
            shapeNames.append(ChatColor.GREEN + "(" + gateID + ")").append(ChatColor.GRAY + shapeName).append(", ");
            gateID++;
        }
        if (shapeNames.length() >= 2) {
            shapeNames.delete(shapeNames.length() - 2, shapeNames.length());
        }
        if (shapeNames.length() == 0) {
            player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "No shapes loaded.");
        } else {
            player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Available Shapes: " + ((Object) shapeNames));
        }
        return true;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (CommandUtilities.playerCheck(sender)) {
            String[] arguments = CommandUtilities.commandEscaper(args);
            Player player = (Player) sender;
            return listBuilds(player, arguments);
        }
        return true;
    }
}
