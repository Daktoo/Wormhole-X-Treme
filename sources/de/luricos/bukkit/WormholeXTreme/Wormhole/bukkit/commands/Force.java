package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class Force implements CommandExecutor, TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            opts.add("-all");
            String prefix = args[0].toLowerCase();
            for (Stargate gate : StargateManager.getAllGates()) {
                if (gate.getGateName().toLowerCase().startsWith(prefix)) {
                    opts.add(gate.getGateName());
                }
            }
            if ("-all".startsWith(prefix)) {
                // already added above
            }
            return opts;
        }
        return Collections.emptyList();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] a = CommandUtilities.commandEscaper(args);
        if (a.length == 1) {
            if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, WXPermissions.PermissionType.CONFIG)) {
                if (a[0].equalsIgnoreCase("-all")) {
                    for (Stargate gate : StargateManager.getAllGates()) {
                        CommandUtilities.closeGate(gate, true);
                    }
                    sender.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "All gates have been deactivated, darkened, and have had their iris (if any) opened.");
                } else if (StargateManager.isStargate(a[0])) {
                    CommandUtilities.closeGate(StargateManager.getStargate(a[0]), true);
                    sender.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + a[0] + " has been closed, darkened, and has had its iris (if any) opened.");
                } else {
                    sender.sendMessage(ConfigManager.MessageStrings.targetInvalid.toString());
                    return false;
                }
                if (CommandUtilities.playerCheck(sender)) {
                    WXTLogger.prettyLog(Level.INFO, false, "Player: \"" + sender.getName() + "\" ran wxforce: " + Arrays.toString(a));
                }
                return true;
            }
            sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            return true;
        }
        return false;
    }
}
