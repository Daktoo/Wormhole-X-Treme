package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateDBManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.plugin.WormholeWorldsSupport;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class WXStatus implements CommandExecutor, TabCompleter {

    private static final String[] PLUGIN_COMMANDS = {
        "dial", "wormhole", "wxlist", "wxbuild", "wxbuildlist",
        "wxremove", "wxcompass", "wxcomplete", "wxidc", "wxforce",
        "wxgo", "wxreload", "wxstatus"
    };

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if ("all".startsWith(prefix)) {
                return Arrays.asList("all");
            }
        }
        return Collections.emptyList();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, WXPermissions.PermissionType.CONFIG)) {
            // Allow both "/wxstatus" (no args) and "/wxstatus a" / "/wxstatus all"
            if (args.length == 0 || args[0].equalsIgnoreCase("a") || args[0].equalsIgnoreCase("all")) {
                printStatus(sender);
                return true;
            }
            // Unknown subcommand — show usage
            return false;
        }
        sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
        return true;
    }

    private void printStatus(CommandSender sender) {
        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "WormholeXTreme System Status: §3::");

        boolean dbOk = StargateDBManager.isConnected();
        boolean wwOk = WormholeWorldsSupport.isEnabled();
        boolean pluginOk = WormholeXTreme.isPluginAvailable();

        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Plugin:        " + status(pluginOk));
        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "DB Connection: " + status(dbOk));
        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "WXW Link:      " + status(wwOk));

        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Commands: §3::");

        WormholeXTreme plugin = WormholeXTreme.getThisPlugin();
        for (String cmd : PLUGIN_COMMANDS) {
            boolean ok = false;
            try {
                org.bukkit.command.PluginCommand pc = plugin.getCommand(cmd);
                ok = (pc != null) && (pc.getExecutor() != null);
            } catch (Exception e) {
                ok = false;
            }
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "  /" + cmd + ": " + status(ok));
        }

    }

    private static String status(boolean ok) {
        return ok ? "§2OK" : "§4FAILED";
    }
}
