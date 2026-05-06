package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/commands/WXIDC.class */
public class WXIDC implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] a = CommandUtilities.commandEscaper(args);
        if (a.length >= 1) {
            if (StargateManager.isStargate(a[0])) {
                Stargate s = StargateManager.getStargate(a[0]);
                if (!s.isGateSignPowered() && s.getGateIrisLeverBlock() != null) {
                    if (!CommandUtilities.playerCheck(sender) || WXPermissions.checkPermission((Player) sender, WXPermissions.PermissionType.CONFIG) || (s.getGateOwner() != null && s.getGateOwner().equals(((Player) sender).getName()))) {
                        if (a.length >= 2) {
                            if (a[1].equals("-clear")) {
                                StargateManager.removeBlockIndex(s.getGateIrisLeverBlock());
                                s.setIrisDeactivationCode("");
                            } else {
                                s.setIrisDeactivationCode(a[1]);
                                StargateManager.addBlockIndex(s.getGateIrisLeverBlock(), s);
                            }
                        }
                        sender.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "IDC for gate: " + s.getGateName() + " is: " + s.getGateIrisDeactivationCode());
                        return true;
                    }
                    sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
                    return true;
                }
                sender.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Iris not available for sign powered stargates or gates without an iris activation block.");
                return true;
            }
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Invalid Stargate: " + a[0]);
            return true;
        }
        return false;
    }
}
