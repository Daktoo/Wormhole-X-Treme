package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.BonusGates;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.DestinyController;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.GateKind;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
public class WXLink implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Only players can link a controller.");
            return true;
        }
        Player player = (Player) sender;
        String[] a = CommandUtilities.commandEscaper(args);
        if (a.length < 1) {
            return false;
        }
        if (!BonusGates.isEnabled()) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + BonusGates.DISABLED_MESSAGE);
            return true;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!DestinyController.isController(held)) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Hold a " + DestinyController.NAME + " in your main hand to link it.");
            return true;
        }
        Stargate gate = StargateManager.getStargate(a[0]);
        if (gate == null) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Gate does not exist: " + a[0]);
            return true;
        }
        if (!GateKind.isDestiny(gate)) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + gate.getGateName() + " is not a Destiny gate.");
            return true;
        }
        if (!DestinyController.isInRange(player, gate)) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "You need to be within " + (int) DestinyController.RANGE + " blocks of " + gate.getGateName() + " to link it.");
            return true;
        }
        if (!WXPermissions.checkPermission(player, gate, WXPermissions.PermissionType.DIALER)) {
            player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            return true;
        }
        String idc = gate.getGateIrisDeactivationCode();
        if (idc != null && !idc.isEmpty()) {
            if (a.length < 2) {
                player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Gate " + gate.getGateName() + "'s Iris is locked.");
                return true;
            }
            if (!idc.equals(a[1])) {
                player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Incorrect IDC. Please try again.");
                return true;
            }
        }
        DestinyController.setLinkedGate(held, gate.getGateName());
        player.getInventory().setItemInMainHand(held);
        player.sendMessage(ConfigManager.MessageStrings.normalStrings.toString() + "Controller linked to " + gate.getGateName() + ". Right-click to active gate, left-click to toggle iris.");
        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !(sender instanceof Player)) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;
        String prefix = args[0].toLowerCase();
        List<String> names = new ArrayList<>();
        for (Stargate gate : StargateManager.getAllGates()) {
            if (GateKind.isDestiny(gate) && gate.getGateName().toLowerCase().startsWith(prefix) && DestinyController.isInRange(player, gate)) {
                names.add(gate.getGateName());
            }
        }
        Collections.sort(names);
        return names;
    }
}