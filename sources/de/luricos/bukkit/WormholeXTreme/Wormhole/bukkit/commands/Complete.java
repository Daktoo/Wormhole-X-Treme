package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.economy.EconomyManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateShape;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.StargateRestrictions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Complete implements CommandExecutor {

    private static boolean doComplete(Player player, String[] args) {
        String name = args[0].trim().replace("\n", "").replace("\r", "");
        if (name.length() < 12) {
            String idc = "";
            String network = "Public";
            for (int i = 1; i < args.length; i++) {
                String[] key_value_string = args[i].split("=");
                if (key_value_string[0].equals("idc")) {
                    idc = key_value_string[1];
                } else if (key_value_string[0].equals("net")) {
                    network = key_value_string[1];
                }
            }

            if (WXPermissions.checkPermission(player, network, WXPermissions.PermissionType.BUILD)) {
                if (!StargateRestrictions.isPlayerBuildRestricted(player)) {
                    if (StargateManager.getStargate(name) == null) {

                        if (ConfigManager.isEconomyEnabled() && EconomyManager.isEconomyEnabled()) {
                            Stargate incomplete = StargateManager.getIncompleteStargate(player);
                            StargateShape shape = incomplete != null ? incomplete.getGateShape() : null;
                            String shapeName = shape != null ? shape.getShapeName() : null;

                            WXTLogger.prettyLog(Level.FINE, false,
                                    "[Economy] wxcomplete economy check: shape=" + shapeName
                                    + " player=" + player.getName());

                            if (shapeName != null) {
                                if (!EconomyManager.canAffordAndCharge(player, shapeName)) {
                                    return true;
                                }
                            }
                        }

                        if (StargateManager.completeStargate(player, name, idc, network)) {
                            player.sendMessage(ConfigManager.MessageStrings.constructSuccess.toString());
                            return true;
                        }
                        player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Construction Failed!?");
                        return true;
                    }
                    player.sendMessage(ConfigManager.MessageStrings.constructNameTaken.toString() + "\"" + name + "\"");
                    return true;
                }
                player.sendMessage(ConfigManager.MessageStrings.playerBuildCountRestricted.toString());
                return true;
            }
            player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            return true;
        }
        player.sendMessage(ConfigManager.MessageStrings.constructNameTooLong.toString() + "\"" + name + "\"");
        return true;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] arguments = CommandUtilities.commandEscaper(args);
        if (arguments.length > 3 || arguments.length <= 0) {
            return false;
        }
        return !CommandUtilities.playerCheck(sender) || doComplete((Player) sender, arguments);
    }
}