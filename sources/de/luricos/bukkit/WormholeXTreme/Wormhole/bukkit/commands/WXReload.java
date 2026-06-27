package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class WXReload implements CommandExecutor, TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if ("now".startsWith(prefix)) {
                return Arrays.asList("now");
            }
        }
        return Collections.emptyList();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, WXPermissions.PermissionType.CONFIG)) {
            // Allow both "/wxreload" (no args) and "/wxreload n" / "/wxreload now"
            if (args.length == 0 || args[0].equalsIgnoreCase("n") || args[0].equalsIgnoreCase("now")) {
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Reloading WormholeXTreme...");
                if (WormholeXTreme.getThisPlugin().reloadPlugin()) {
                    sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Reloading complete.");
                } else {
                    sender.sendMessage(ConfigManager.MessageStrings.errorHeader + "Error during reload, see console log.");
                }
                return true;
            }
            // Unknown subcommand — show usage
            return false;
        }
        sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
        return true;
    }
}
