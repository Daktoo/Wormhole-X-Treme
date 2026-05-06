package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import java.util.ArrayList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/commands/WXList.class */
public class WXList implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, WXPermissions.PermissionType.LIST)) {
            ArrayList<Stargate> gates = StargateManager.getAllGates();
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Available gates §3::");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < gates.size(); i++) {
                sb.append("§7").append(gates.get(i).getGateName());
                if (i != gates.size() - 1) {
                    sb.append("§8, ");
                }
                if (sb.toString().length() >= 75) {
                    sender.sendMessage(sb.toString());
                    sb = new StringBuilder();
                }
            }
            if (!sb.toString().equals("")) {
                sender.sendMessage(sb.toString());
                return true;
            }
            return true;
        }
        sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
        return true;
    }
}
