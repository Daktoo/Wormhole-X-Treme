package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/commands/CommandUtilities.class */
public class CommandUtilities {
    /**
     * Gate names to offer as tab completions, scoped by the same permissions
     * that govern /wxlist.
     *
     * A player with LIST_ALL sees every gate, matching /wxlist all. A player
     * without it sees only gates they own, matching the /wxlist fallback, so tab
     * completion cannot be used to enumerate gates they are not allowed to list.
     * Console is unrestricted, as it is for /wxlist.
     */
    public static ArrayList<String> getGateNameSuggestions(CommandSender sender, String prefix) {
        ArrayList<String> names = new ArrayList<>();
        String lowerPrefix = prefix == null ? "" : prefix.toLowerCase();
        boolean ownedOnly = false;
        String requesterName = null;
        if (playerCheck(sender)) {
            Player player = (Player) sender;
            requesterName = player.getName();
            if (!WXPermissions.checkPermission(player, WXPermissions.PermissionType.LIST_ALL)) {
                if (!WXPermissions.checkPermission(player, WXPermissions.PermissionType.LIST_SELF)) {
                    return names;
                }
                ownedOnly = true;
            }
        }
        for (Stargate gate : StargateManager.getAllGates()) {
            String gateName = gate.getGateName();
            if (gateName == null || !gateName.toLowerCase().startsWith(lowerPrefix)) {
                continue;
            }
            if (ownedOnly) {
                String owner = gate.getGateOwner();
                if (owner == null || !owner.equalsIgnoreCase(requesterName)) {
                    continue;
                }
            }
            names.add(gateName);
        }
        return names;
    }

    public static void closeGate(Stargate stargate, boolean iris) {
        if (stargate != null) {
            if (stargate.isGateActive()) {
                stargate.shutdownStargate(true);
                if (stargate.isGateActive()) {
                    stargate.setGateActive(false);
                }
            }
            if (stargate.isGateLightsActive()) {
                stargate.lightStargate(false);
                stargate.stopActivationTimer();
            }
            if (iris && stargate.isGateIrisActive()) {
                stargate.toggleIrisActive(false);
            }
        }
    }

    public static String[] commandEscaper(String[] args) {
        StringBuilder tempString = new StringBuilder();
        boolean startQuoteFound = false;
        boolean endQuoteFound = false;
        ArrayList<String> argsPartsList = new ArrayList<>();
        for (String part : args) {
            if (part.contains("\"") && !startQuoteFound) {
                if (!part.replaceFirst("\"", "").contains("\"")) {
                    startQuoteFound = true;
                }
            } else if (part.contains("\"") && startQuoteFound) {
                endQuoteFound = true;
            }
            if (!startQuoteFound) {
                argsPartsList.add(part);
            }
            if (startQuoteFound) {
                tempString.append(part.replace("\"", ""));
                if (endQuoteFound) {
                    argsPartsList.add(tempString.toString());
                    startQuoteFound = false;
                    endQuoteFound = false;
                    tempString = new StringBuilder();
                } else {
                    tempString.append(" ");
                }
            }
        }
        return (String[]) argsPartsList.toArray(new String[argsPartsList.size()]);
    }

    public static void gateRemove(Stargate stargate, boolean destroy) {
        stargate.setupGateSign(false);
        if (!destroy) {
            stargate.resetTeleportSign();
        }
        if (!stargate.getGateIrisDeactivationCode().equals("")) {
            if (stargate.isGateIrisActive()) {
                stargate.toggleIrisActive(false);
            }
            stargate.setupIrisLever(false);
        }
        if (stargate.isGateRedstonePowered()) {
            stargate.setupRedstone(false);
        }
        if (destroy) {
            stargate.deleteGateBlocks();
            stargate.deletePortalBlocks();
            stargate.deleteTeleportSign();
        }
        StargateManager.removeStargate(stargate);
    }

    public static String getGateNetwork(Stargate stargate) {
        if (stargate != null && stargate.getGateNetwork() != null) {
            return stargate.getGateNetwork().getNetworkName();
        }
        return "Public";
    }

    public static boolean isBoolean(String booleanString) {
        return booleanString.equalsIgnoreCase("true") || booleanString.equalsIgnoreCase("false");
    }

    public static boolean playerCheck(CommandSender sender) {
        return sender instanceof Player;
    }
}
