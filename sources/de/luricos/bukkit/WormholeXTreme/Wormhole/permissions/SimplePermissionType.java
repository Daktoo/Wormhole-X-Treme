package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/SimplePermissionType.class */
enum SimplePermissionType {
    USE("wormhole.simple.use"),
    BUILD("wormhole.simple.build"),
    REMOVE("wormhole.simple.remove"),
    CONFIG("wormhole.simple.config");

    private final String simplePermissionNode;
    private static final Map<String, SimplePermissionType> simplePermissionMap = new HashMap();

    static {
        for (SimplePermissionType simplePermissionType : EnumSet.allOf(SimplePermissionType.class)) {
            simplePermissionMap.put(simplePermissionType.simplePermissionNode, simplePermissionType);
        }
    }

    public static SimplePermissionType fromSimplePermissionNode(String simplePermissionNode) {
        return simplePermissionMap.get(simplePermissionNode);
    }

    SimplePermissionType(String simplePermissionNode) {
        this.simplePermissionNode = simplePermissionNode;
    }

    public String getString() {
        return this.simplePermissionNode;
    }

    public boolean checkPermission(Player player) {
        return WormholeXTreme.getPermissionManager().has(player, getString());
    }
}
