package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/ComplexPermissionType.class */
enum ComplexPermissionType {
    USE_SIGN("wormhole.use.sign"),
    USE_DIALER("wormhole.use.dialer"),
    USE_COMPASS("wormhole.use.compass"),
    USE_COOLDOWN_GROUP_ONE("wormhole.cooldown.groupone"),
    USE_COOLDOWN_GROUP_TWO("wormhole.cooldown.grouptwo"),
    USE_COOLDOWN_GROUP_THREE("wormhole.cooldown.groupthree"),
    REMOVE_OWN("wormhole.remove.own"),
    REMOVE_ALL("wormhole.remove.all"),
    BUILD("wormhole.build"),
    BUILD_RESTRICTION_GROUP_ONE("wormhole.build.groupone"),
    BUILD_RESTRICTION_GROUP_TWO("wormhole.build.grouptwo"),
    BUILD_RESTRICTION_GROUP_THREE("wormhole.build.groupthree"),
    CONFIG("wormhole.config"),
    LIST("wormhole.list"),
    NETWORK_USE("wormhole.network.use."),
    NETWORK_BUILD("wormhole.network.build."),
    GO("wormhole.go");

    private final String complexPermissionNode;
    private static final Map<String, ComplexPermissionType> complexPermissionMap = new HashMap();

    static {
        for (ComplexPermissionType type : EnumSet.allOf(ComplexPermissionType.class)) {
            complexPermissionMap.put(type.complexPermissionNode, type);
        }
    }

    public static ComplexPermissionType fromComplexPermissionNode(String complexPermissionNode) {
        return complexPermissionMap.get(complexPermissionNode);
    }

    ComplexPermissionType(String complexPermissionNode) {
        this.complexPermissionNode = complexPermissionNode;
    }

    protected boolean checkPermission(Player player) {
        return checkPermission(player, null, null);
    }

    protected boolean checkPermission(Player player, Stargate stargate) {
        return checkPermission(player, stargate, null);
    }

    public boolean checkPermission(Player player, Stargate stargate, String networkName) {
        boolean allowed;
        if (player != null && WormholeXTreme.getPermissionManager() != null && !ConfigManager.getSimplePermissions()) {
            switch (this) {
                case NETWORK_USE:
                case NETWORK_BUILD:
                    allowed = networkName != null && WormholeXTreme.getPermissionManager().has(player, new StringBuilder().append(getString()).append(networkName).toString());
                    break;
                case REMOVE_OWN:
                    allowed = stargate != null && stargate.getGateOwner() != null && stargate.getGateOwner().equals(player.getName()) && WormholeXTreme.getPermissionManager().has(player, this.complexPermissionNode);
                    break;
                default:
                    allowed = WormholeXTreme.getPermissionManager().has(player, getString());
                    break;
            }
            if (allowed) {
                WXTLogger.prettyLog(Level.FINE, false, "Player: '" + player.getName() + "' granted complex \"" + toString() + "\" permission" + (networkName != null ? " on network \"" + networkName + "\"" : "") + ".");
                return true;
            }
            WXTLogger.prettyLog(Level.FINE, false, "Player: '" + player.getName() + "' denied complex \"" + toString() + "\" permission" + (networkName != null ? " on network \"" + networkName + "\"" : "") + ".");
            return false;
        }
        return false;
    }

    protected boolean checkPermission(Player player, String networkName) {
        return checkPermission(player, null, networkName);
    }

    public String getString() {
        return this.complexPermissionNode;
    }
}
