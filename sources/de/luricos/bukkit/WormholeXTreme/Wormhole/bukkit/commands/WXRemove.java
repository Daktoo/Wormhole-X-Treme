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

public class WXRemove implements CommandExecutor, TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
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
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            if ("-all".startsWith(prefix)) {
                return Arrays.asList("-all");
            }
        }
        return Collections.emptyList();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] a = CommandUtilities.commandEscaper(args);
        if (a.length < 1 || a.length > 2 || a[0].equalsIgnoreCase("-all")) {
            return false;
        }
        Stargate s = StargateManager.getStargate(a[0]);
        if (s == null) {
            // Not in the main registry, but an orphaned entry may still be sitting
            // in a network list and showing up on other gates' dialer signs.
            Stargate orphan = StargateManager.findOrphanedGate(a[0]);
            if (orphan != null && StargateManager.purgeOrphanedGate(a[0])) {
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Cleared orphaned wormhole entry: " + a[0]);
                return true;
            }
        }
        if (s != null) {
            if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, s, WXPermissions.PermissionType.REMOVE)) {
                boolean destroy = a.length == 2 && a[1].equalsIgnoreCase("-all");
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
