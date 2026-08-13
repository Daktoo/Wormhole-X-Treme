package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class WXList implements CommandExecutor, TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> opts = new ArrayList<>(Arrays.asList("all", "self"));
            List<String> result = new ArrayList<>();
            for (String opt : opts) {
                if (opt.startsWith(prefix)) result.add(opt);
            }
            return result;
        }
        return Collections.emptyList();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] a = CommandUtilities.commandEscaper(args);
        String sub = a.length > 0 ? a[0].toLowerCase() : "all";

        if (sub.equals("self")) {
            if (!CommandUtilities.playerCheck(sender)) {
                sender.sendMessage(ConfigManager.MessageStrings.errorHeader + "/wxlist self can only be used by a player.");
                return true;
            }
            Player player = (Player) sender;
            if (!WXPermissions.checkPermission(player, WXPermissions.PermissionType.LIST_SELF)) {
                sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
                return true;
            }
            return listSelf(player);
        }

        boolean explicitAll = a.length > 0 && sub.equals("all");

        if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, WXPermissions.PermissionType.LIST_ALL)) {
            return listAll(sender);
        }
        Player player = (Player) sender;
        if (!explicitAll && WXPermissions.checkPermission(player, WXPermissions.PermissionType.LIST_SELF)) {
            // No argument given and no permission to list every gate: show the
            // player their own gates rather than refusing outright.
            return listSelf(player);
        }
        sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
        return true;
    }

    private static boolean listAll(CommandSender sender) {
        ArrayList<Stargate> gates = StargateManager.getAllGates();
        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "All gates §3:: §7(" + gates.size() + " total)");
        if (gates.isEmpty()) {
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "§8No stargates found.");
            return true;
        }
        sendGateList(sender, gates);
        return true;
    }

    private static boolean listSelf(Player player) {
        ArrayList<Stargate> all = StargateManager.getAllGates();
        ArrayList<Stargate> owned = new ArrayList<>();
        for (Stargate gate : all) {
            String owner = gate.getGateOwner();
            if (owner != null && owner.equalsIgnoreCase(player.getName())) {
                owned.add(gate);
            }
        }
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "Your stargates §3:: §7(" + owned.size() + " total)");
        if (owned.isEmpty()) {
            player.sendMessage(ConfigManager.MessageStrings.normalHeader + "§8You do not own any stargates.");
            return true;
        }
        sendGateList(player, owned);
        return true;
    }

    private static void sendGateList(CommandSender sender, List<Stargate> gates) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < gates.size(); i++) {
            Stargate gate = gates.get(i);
            String ownerSuffix = gate.getGateOwner() != null ? " §8[" + gate.getGateOwner() + "]" : "";
            sb.append("§7").append(gate.getGateName()).append(ownerSuffix);
            if (i != gates.size() - 1) {
                sb.append("§8, ");
            }
            if (sb.length() >= 200) {
                sender.sendMessage(sb.toString());
                sb = new StringBuilder();
            }
        }
        if (sb.length() > 0) {
            sender.sendMessage(sb.toString());
        }
    }
}
