package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class Go implements CommandExecutor, TabCompleter {

    /** How far in front of the gate we are willing to search for open space. */
    private static final int MAX_ARRIVAL_OFFSET = 3;

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Scoped by list permission: a player who cannot run /wxlist all
            // only gets their own gates suggested.
            return new ArrayList<String>(CommandUtilities.getGateNameSuggestions(sender, args[0]));
        }
        // Second argument is an IDC. Never suggest values for it.
        return Collections.emptyList();
    }

    private static boolean doGo(Player player, String[] args) {
        if (!WXPermissions.checkPermission(player, WXPermissions.PermissionType.GO)) {
            player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            return true;
        }
        if (args.length < 1) {
            return false;
        }
        String goGate = args[0].trim().replace("\n", "").replace("\r", "");
        Stargate s = StargateManager.getStargate(goGate);
        if (s == null) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Gate does not exist: " + args[0]);
            return true;
        }

        String idc = s.getGateIrisDeactivationCode();
        boolean idcLocked = idc != null && !idc.isEmpty();
        if (idcLocked) {
            // Warping straight to an IDC locked gate would sidestep the lock
            // entirely, so the code is required here just as it is when dialling.
            if (args.length < 2) {
                player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Gate '" + s.getGateName() + "' is IDC locked. Usage: /wxgo " + s.getGateName() + " <IDC>");
                return true;
            }
            String supplied = args[1].trim().replace("\n", "").replace("\r", "");
            if (!idc.equals(supplied)) {
                player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Incorrect IDC for gate: " + s.getGateName());
                return true;
            }
        }

        Location destination = getSafeArrivalLocation(s);
        if (destination == null) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Gate has no valid teleport location: " + s.getGateName());
            return true;
        }
        player.teleport(destination);
        return true;
    }

    /**
     * The stored teleport location sits inside the gate, which is filled with
     * the iris material while the iris is closed. Arriving there buries the
     * player in the iris block, so when the gate is shut we step out along the
     * gate's facing until there is room to stand.
     *
     * Falls back to the stored location if nothing suitable is found, which
     * keeps the old behaviour rather than dropping the player somewhere odd.
     */
    private static Location getSafeArrivalLocation(Stargate stargate) {
        Location base = stargate.getGatePlayerTeleportLocation();
        if (base == null || !stargate.isGateIrisActive()) {
            return base;
        }
        BlockFace facing = stargate.getGateFacing();
        if (facing == null) {
            return base;
        }
        for (int distance = 1; distance <= MAX_ARRIVAL_OFFSET; distance++) {
            Location candidate = base.clone().add(facing.getModX() * distance, 0, facing.getModZ() * distance);
            if (hasStandingRoom(candidate)) {
                candidate.setYaw(base.getYaw());
                candidate.setPitch(base.getPitch());
                return candidate;
            }
        }
        return base;
    }

    /** Feet and head space clear, and something solid to stand on. */
    private static boolean hasStandingRoom(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        return feet.isPassable() && head.isPassable();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] arguments = CommandUtilities.commandEscaper(args);
        if (arguments.length > 2 || arguments.length <= 0) {
            return false;
        }
        return !CommandUtilities.playerCheck(sender) || doGo((Player) sender, arguments);
    }
}
