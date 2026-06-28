package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WXTop implements CommandExecutor {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, WXPermissions.PermissionType.TOP)) {
            int limit = DEFAULT_LIMIT;
            if (args.length > 0) {
                try {
                    limit = Math.min(Math.max(1, Integer.parseInt(args[0])), MAX_LIMIT);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ConfigManager.MessageStrings.errorHeader + "Usage: /wxtop [count] (max " + MAX_LIMIT + ")");
                    return true;
                }
            }

            ArrayList<Stargate> all = StargateManager.getAllGates();
            if (all.isEmpty()) {
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "No stargates found.");
                return true;
            }

            List<Stargate> sorted = new ArrayList<>(all);
            Collections.sort(sorted, new Comparator<Stargate>() {
                @Override
                public int compare(Stargate a, Stargate b) {
                    return Integer.compare(b.getVisitCount(), a.getVisitCount());
                }
            });

            int count = Math.min(limit, sorted.size());
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Top " + count + " Most Visited Stargates §3::");

            int longest = 0;
            for (int i = 0; i < count; i++) {
                int len = sorted.get(i).getGateName().length();
                if (len > longest) longest = len;
            }

            for (int i = 0; i < count; i++) {
                Stargate gate = sorted.get(i);
                String rank = String.format("%2d", i + 1);
                String name = gate.getGateName();
                String owner = gate.getGateOwner() != null ? gate.getGateOwner() : "unknown";
                int visits = gate.getVisitCount();

                String pad = visits > 0 ? "§2" : "§8";
                String visitStr = visits == 1 ? "1 visit" : visits + " visits";

                sender.sendMessage(
                    ConfigManager.MessageStrings.normalHeader
                    + "§8#" + rank + " §7" + name
                    + " §8[§3" + owner + "§8] "
                    + pad + visitStr
                );
            }

            return true;
        }
        sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
        return true;
    }
}