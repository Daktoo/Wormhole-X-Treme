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
        if (!CommandUtilities.playerCheck(sender)) {
            return true;
        }
        Player player = (Player) sender;
        String[] a = CommandUtilities.commandEscaper(args);
        if (a.length < 1 || a.length > 3) {
            return false;
        }
        if (!BonusGates.isEnabled()) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + BonusGates.DISABLED_MESSAGE);
            return true;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!DestinyController.isController(held)) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Hold a " + DestinyController.NAME + " in your main hand to use /wxlink.");
            return true;
        }
        String name = a[0].trim().replace("\n", "").replace("\r", "");
        String idc = "";
        String network = "Public";
        for (int i = 1; i < a.length; i++) {
            String[] keyValue = a[i].split("=", 2);
            if (keyValue.length == 2 && keyValue[0].equalsIgnoreCase("idc")) {
                idc = keyValue[1];
            } else if (keyValue.length == 2 && keyValue[0].equalsIgnoreCase("net")) {
                network = keyValue[1];
            } else if (keyValue.length == 1 && idc.isEmpty()) {
                idc = keyValue[0];
            }
        }
        Stargate existing = StargateManager.getStargate(name);
        if (existing != null) {
            if (!GateKind.isDestiny(existing)) {
                player.sendMessage(ConfigManager.MessageStrings.constructNameTaken.toString() + "\"" + name + "\"");
                return true;
            }
            return linkExisting(player, held, existing, idc);
        }
        return completeAndLink(player, held, name, idc, network);
    }
    private static boolean completeAndLink(Player player, ItemStack held, String name, String idc, String network) {
        Stargate gate = DestinyController.findUnbuiltGate(player);
        if (gate == null) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "There is no new Destiny gate within " + (int) DestinyController.RANGE + " blocks.");
            player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "To link this controller to a Destiny gate, use /wxlink.");
            return true;
        }
        if (gate.getGateDialLeverBlock() != null) {
            gate.getGateStructureBlocks().remove(gate.getGateDialLeverBlock().getLocation());
        }
        Stargate previous = StargateManager.getIncompleteStargate(player);
        StargateManager.addIncompleteStargate(player.getName(), gate);
        List<String> completeArgs = new ArrayList<>();
        completeArgs.add(name);
        if (!idc.isEmpty()) {
            completeArgs.add("idc=" + idc);
        }
        completeArgs.add("net=" + network);
        Complete.doComplete(player, completeArgs.toArray(new String[0]));
        Stargate completed = StargateManager.getStargate(name);
        if (completed != gate) {
            if (previous != null && previous != gate) {
                StargateManager.addIncompleteStargate(player.getName(), previous);
            }
            return true;
        }
        link(player, held, completed);
        return true;
    }
    private static boolean linkExisting(Player player, ItemStack held, Stargate gate, String idc) {
        if (!DestinyController.isInRange(player, gate)) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "You must be within " + (int) DestinyController.RANGE + " blocks of " + gate.getGateName() + " to link it.");
            return true;
        }
        if (!WXPermissions.checkPermission(player, gate, WXPermissions.PermissionType.DIALER)) {
            player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            return true;
        }
        String gateIdc = gate.getGateIrisDeactivationCode();
        if (gateIdc != null && !gateIdc.isEmpty()) {
            if (idc.isEmpty()) {
                player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Gate " + gate.getGateName() + "'s Iris is locked.");
                return true;
            }
            if (!gateIdc.equals(idc)) {
                player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Incorrect IDC. Please try again.");
                return true;
            }
        }
        link(player, held, gate);
        return true;
    }
    private static void link(Player player, ItemStack held, Stargate gate) {
        DestinyController.setLinkedGate(held, gate.getGateName());
        player.getInventory().setItemInMainHand(held);
        player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Controller linked to " + gate.getGateName() + ". Right-click to activate gate, left-click to toggle iris.");
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
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
        if (args.length == 2 || args.length == 3) {
            List<String> options = new ArrayList<>();
            for (String option : new String[] {"idc=", "net="}) {
                if (option.startsWith(args[args.length - 1].toLowerCase())) {
                    options.add(option);
                }
            }
            return options;
        }
        return Collections.emptyList();
    }
}