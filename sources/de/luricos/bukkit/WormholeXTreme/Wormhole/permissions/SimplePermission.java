package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/SimplePermission.class */
enum SimplePermission {
    USE("wormhole.simple.use"),
    BUILD("wormhole.simple.build"),
    REMOVE("wormhole.simple.remove"),
    CONFIG("wormhole.simple.config");

    private final String simplePermissionNode;
    private static final Map<String, SimplePermission> simplePermissionMap = new HashMap();

    static {
        for (SimplePermission simplePermission : EnumSet.allOf(SimplePermission.class)) {
            simplePermissionMap.put(simplePermission.simplePermissionNode, simplePermission);
        }
    }

    public static SimplePermission fromSimplePermissionNode(String simplePermissionNode) {
        return simplePermissionMap.get(simplePermissionNode);
    }

    SimplePermission(String simplePermissionNode) {
        this.simplePermissionNode = simplePermissionNode;
    }

    protected boolean checkPermission(Player player) {
        if (player != null && !ConfigManager.getPermissionsSupportDisable() && WormholeXTreme.getPermissionManager() != null && ConfigManager.getSimplePermissions()) {
            if (WormholeXTreme.getPermissionManager().has(player, this.simplePermissionNode)) {
                WXTLogger.prettyLog(Level.FINE, false, "Player: " + player.getName() + "\" granted simple \"" + toString() + "\" permissions.");
                return true;
            }
            WXTLogger.prettyLog(Level.FINE, false, "Player: " + player.getName() + "\" denied simple \"" + toString() + "\" permissions.");
            return false;
        }
        return false;
    }

    public String getSimplePermission() {
        return this.simplePermissionNode;
    }
}
