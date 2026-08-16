package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateNetwork;
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
            List<String> opts = new ArrayList<>(Arrays.asList("all", "self", "network", "player"));
            List<String> result = new ArrayList<>();
            for (String opt : opts) {
                if (opt.startsWith(prefix)) result.add(opt);
            }
            return result;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            List<String> result = new ArrayList<>();
            if (sub.equals("network")) {
                // Only offer networks to somebody who is allowed to list them,
                // so completion cannot be used to enumerate networks they
                // cannot see.
                if (!canList(sender, WXPermissions.PermissionType.LIST_NETWORK)) {
                    return Collections.emptyList();
                }
                for (String name : StargateManager.getAllNetworkNames()) {
                    if (name.toLowerCase().startsWith(prefix)) result.add(name);
                }
            } else if (sub.equals("player")) {
                if (!canList(sender, WXPermissions.PermissionType.LIST_PLAYER)) {
                    return Collections.emptyList();
                }
                for (String name : StargateManager.getAllGateOwnerNames()) {
                    if (name.toLowerCase().startsWith(prefix)) result.add(name);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * Console is unrestricted, matching the rest of /wxlist. Players are checked
     * against the supplied permission type.
     */
    private static boolean canList(CommandSender sender, WXPermissions.PermissionType type) {
        if (!CommandUtilities.playerCheck(sender)) {
            return true;
        }
        return WXPermissions.checkPermission((Player) sender, type);
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

        if (sub.equals("network")) {
            if (a.length < 2) {
                sender.sendMessage(ConfigManager.MessageStrings.errorHeader + "Usage: /wxlist network <network>");
                return true;
            }
            if (!canList(sender, WXPermissions.PermissionType.LIST_NETWORK)) {
                sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
                return true;
            }
            return listNetwork(sender, a[1]);
        }

        if (sub.equals("player")) {
            if (a.length < 2) {
                sender.sendMessage(ConfigManager.MessageStrings.errorHeader + "Usage: /wxlist player <player>");
                return true;
            }
            if (!canList(sender, WXPermissions.PermissionType.LIST_PLAYER)) {
                sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
                return true;
            }
            return listPlayer(sender, a[1]);
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
        sendGateList(sender, gates, true);
        return true;
    }

    private static boolean listSelf(Player player) {
        ArrayList<Stargate> owned = StargateManager.getGatesOwnedBy(player.getName());
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "Your stargates §3:: §7(" + owned.size() + " total)");
        if (owned.isEmpty()) {
            player.sendMessage(ConfigManager.MessageStrings.normalHeader + "§8You do not own any stargates.");
            return true;
        }
        // The owner is the player reading the list, so repeating it on every
        // line is noise.
        sendGateList(player, owned, false);
        return true;
    }

    private static boolean listNetwork(CommandSender sender, String networkName) {
        StargateNetwork network = StargateManager.getStargateNetworkIgnoreCase(networkName);
        if (network == null) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader + "No such network: " + networkName);
            return true;
        }
        String resolvedName = network.getNetworkName() != null ? network.getNetworkName() : networkName;
        ArrayList<Stargate> gates = StargateManager.getGatesInNetwork(resolvedName);
        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Gates on network '" + resolvedName + "' §3:: §7(" + gates.size() + " total)");
        if (gates.isEmpty()) {
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "§8No stargates found on this network.");
            return true;
        }
        sendGateList(sender, gates, true);
        return true;
    }

    private static boolean listPlayer(CommandSender sender, String playerName) {
        ArrayList<Stargate> gates = StargateManager.getGatesOwnedBy(playerName);
        // Show the owner's name as it is actually stored on the gates, rather
        // than however the requester happened to type it.
        String resolvedName = playerName;
        if (!gates.isEmpty() && gates.get(0).getGateOwner() != null) {
            resolvedName = gates.get(0).getGateOwner();
        }
        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Gates owned by '" + resolvedName + "' §3:: §7(" + gates.size() + " total)");
        if (gates.isEmpty()) {
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "§8No stargates found for this player.");
            return true;
        }
        // Every gate in this list has the same owner, so the per-gate owner tag
        // would just repeat the header.
        sendGateList(sender, gates, false);
        return true;
    }

    private static void sendGateList(CommandSender sender, List<Stargate> gates, boolean showOwner) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < gates.size(); i++) {
            Stargate gate = gates.get(i);
            String ownerSuffix = showOwner && gate.getGateOwner() != null ? " §8[" + gate.getGateOwner() + "]" : "";
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
