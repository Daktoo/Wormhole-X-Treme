package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateDBManager;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/PermissionsManager.class */
public class PermissionsManager {
    private static ConcurrentHashMap<String, PermissionLevel> player_general_permission = new ConcurrentHashMap<>();

    /* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/PermissionsManager$PermissionLevel.class */
    public enum PermissionLevel {
        NO_PERMISSION_SET,
        WORMHOLE_FULL_PERMISSION,
        WORMHOLE_CREATE_PERMISSION,
        WORMHOLE_USE_PERMISSION,
        WORMHOLE_NO_PERMISSION
    }

    private static PermissionLevel getIndividualPermissionLevel(String player) {
        String pl_lower = player.toLowerCase();
        if (player_general_permission.containsKey(pl_lower)) {
            return player_general_permission.get(pl_lower);
        }
        return PermissionLevel.NO_PERMISSION_SET;
    }

    protected static PermissionLevel getPermissionLevel(Player p, Stargate s) {
        if (!ConfigManager.getBuiltInPermissionsEnabled()) {
            return PermissionLevel.WORMHOLE_FULL_PERMISSION;
        }
        if (s != null) {
        }
        PermissionLevel lvl = getIndividualPermissionLevel(p.getName());
        if (lvl != PermissionLevel.NO_PERMISSION_SET) {
            return lvl;
        }
        if (s != null) {
        }
        if (s != null) {
        }
        if (p.isOp()) {
            return PermissionLevel.WORMHOLE_FULL_PERMISSION;
        }
        return ConfigManager.getBuiltInDefaultPermissionLevel();
    }

    public static void handlePermissionRequest(Player p, String[] message_parts) {
        p.sendMessage("This system is currently under development and thus disabled");
    }

    public static void loadPermissions() {
        player_general_permission = StargateDBManager.getAllIndividualPermissions();
    }

    private static void setIndividualPermissionLevel(String player, PermissionLevel lvl) {
        String pl_lower = player.toLowerCase();
        player_general_permission.put(pl_lower, lvl);
        StargateDBManager.storeIndividualPermissionInDB(pl_lower, lvl);
    }
}
