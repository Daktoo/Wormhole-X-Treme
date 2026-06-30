package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateHelper;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTStringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class Build implements CommandExecutor, TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            int index = 1;
            for (String shapeName : StargateHelper.getShapeNames()) {
                // offer both the shape name and its numeric index
                if (shapeName.toLowerCase().startsWith(prefix)) {
                    opts.add(shapeName);
                }
                if (String.valueOf(index).startsWith(prefix)) {
                    opts.add(String.valueOf(index));
                }
                index++;
            }
            return opts;
        }
        return Collections.emptyList();
    }

    private static boolean doBuild(Player player, String[] args) {
        if (args.length == 1) {
            if (!WXPermissions.checkPermission(player, WXPermissions.PermissionType.BUILD)) {
                player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
                return true;
            }
            String shapeName = args[0];
            if (WXTStringUtils.isIntNumber(shapeName)) {
                int sCount = 1;
                int sCEnd = Integer.parseInt(shapeName);
                Iterator<String> it = StargateHelper.getShapeNames().iterator();
                while (true) {
                    if (!it.hasNext()) { break; }
                    String sName = it.next();
                    if (sCount >= sCEnd) { shapeName = sName; break; }
                    sCount++;
                }
            }
            if (StargateHelper.isStargateShape(shapeName)) {
                StargateManager.addPlayerBuilderShape(player.getName(), StargateHelper.getStargateShape(shapeName));
                player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Press Activation button on new DHD to autobuild Stargate in the shape of: " + StargateHelper.getStargateShapeName(shapeName));
                return true;
            }
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Invalid shape: " + shapeName);
            return true;
        }
        return false;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (CommandUtilities.playerCheck(sender)) {
            String[] arguments = CommandUtilities.commandEscaper(args);
            if (arguments.length < 3 && arguments.length > 0) {
                Player player = (Player) sender;
                return doBuild(player, arguments);
            }
            return false;
        }
        return true;
    }
}