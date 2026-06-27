package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayer;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayerManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class Dial implements CommandExecutor, TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // arg 1: target gate name; arg 2: IDC code (free-form, no suggestions)
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Stargate gate : StargateManager.getAllGates()) {
                if (gate.getGateName().toLowerCase().startsWith(prefix)) {
                    names.add(gate.getGateName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }

    private static boolean doDial(Player player, String[] args) {
        WormholePlayer wormholePlayer = WormholePlayerManager.getRegisteredWormholePlayer(player);
        Stargate sourceGate = wormholePlayer.getStargate();
        if (sourceGate != null && sourceGate.isGateLightsActive()) {
            if (WXPermissions.checkPermission(player, sourceGate, WXPermissions.PermissionType.DIALER)) {
                String startnetwork = CommandUtilities.getGateNetwork(sourceGate);
                if (!sourceGate.getGateName().equals(args[0])) {
                    Stargate target = StargateManager.getStargate(args[0]);
                    if (target == null) {
                        CommandUtilities.closeGate(sourceGate, false);
                        wormholePlayer.removeStargate(sourceGate);
                        player.sendMessage(ConfigManager.MessageStrings.targetInvalid.toString());
                        return true;
                    }
                    String targetnetwork = CommandUtilities.getGateNetwork(target);
                    WXTLogger.prettyLog(Level.FINE, false, "Dial Target - Gate: \"" + target.getGateName() + "\" Network: \"" + targetnetwork + "\"");
                    if (!startnetwork.equals(targetnetwork)) {
                        CommandUtilities.closeGate(sourceGate, false);
                        wormholePlayer.removeStargate(sourceGate);
                        player.sendMessage(ConfigManager.MessageStrings.targetInvalid.toString() + " Not on same network.");
                        return true;
                    }
                    if (sourceGate.isGateIrisActive()) {
                        sourceGate.toggleIrisActive(false);
                    }
                    if (!target.getGateIrisDeactivationCode().equals("") && target.isGateIrisActive() && args.length >= 2 && target.getGateIrisDeactivationCode().equals(args[1]) && target.isGateIrisActive()) {
                        target.toggleIrisActive(false);
                        player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "IDC accepted. Iris has been deactivated.");
                    }
                    if (sourceGate.dialStargate(target, false)) {
                        target.setLastUsedBy(player);
                        player.sendMessage(ConfigManager.MessageStrings.gateConnected.toString());
                        return true;
                    }
                    player.sendMessage(String.format(ConfigManager.MessageStrings.targetIsInUseBy.toString(), target.getGateName(), target.getLastUsedBy()));
                    CommandUtilities.closeGate(sourceGate, false);
                    wormholePlayer.removeStargate(sourceGate);
                    return true;
                }
                player.sendMessage(ConfigManager.MessageStrings.targetIsSelf.toString());
                CommandUtilities.closeGate(sourceGate, false);
                wormholePlayer.removeStargate(sourceGate);
                return true;
            }
            player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            wormholePlayer.removeStargate(sourceGate);
            return true;
        }
        player.sendMessage(ConfigManager.MessageStrings.gateNotActive.toString());
        return true;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] arguments = CommandUtilities.commandEscaper(args);
        if (arguments.length >= 3 || arguments.length <= 0) {
            return false;
        }
        return !CommandUtilities.playerCheck(sender) || doDial((Player) sender, arguments);
    }
}
