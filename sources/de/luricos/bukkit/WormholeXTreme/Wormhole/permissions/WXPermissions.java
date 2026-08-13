package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionsManager;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/WXPermissions.class */
public class WXPermissions {

    /* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/WXPermissions$PermissionType.class */
    public enum PermissionType {
        DAMAGE,
        SIGN,
        DIALER,
        BUILD,
        REMOVE,
        USE,
        LIST,
        LIST_ALL,
        LIST_SELF,
        TOP,
        CONFIG,
        GO,
        COMPASS,
        USE_COOLDOWN_GROUP_ONE,
        USE_COOLDOWN_GROUP_TWO,
        USE_COOLDOWN_GROUP_THREE,
        BUILD_RESTRICTION_GROUP_ONE,
        BUILD_RESTRICTION_GROUP_TWO,
        BUILD_RESTRICTION_GROUP_THREE
    }

    public static boolean checkPermission(Player player, PermissionType permissiontype) {
        return checkPermission(player, null, null, permissiontype);
    }

    public static boolean checkPermission(Player player, Stargate stargate, PermissionType permissionType) {
        return checkPermission(player, stargate, null, permissionType);
    }

    private static boolean checkPermission(Player player, Stargate stargate, String network, PermissionType permissionType) {
        if (player == null) {
            return false;
        }
        if (player.isOp()) {
            switch (permissionType) {
                case DAMAGE:
                case REMOVE:
                case CONFIG:
                case GO:
                case SIGN:
                case DIALER:
                case USE:
                case LIST:
                case LIST_ALL:
                case LIST_SELF:
                case TOP:
                case COMPASS:
                case BUILD:
                    return true;
                default:
                    return false;
            }
        }
        if (!ConfigManager.getPermissionsSupportDisable() && WormholeXTreme.getPermissionManager() != null) {
            if (ConfigManager.getSimplePermissions()) {
                switch (permissionType) {
                    case DAMAGE:
                    case REMOVE:
                        return SimplePermissionType.REMOVE.checkPermission(player) || SimplePermissionType.CONFIG.checkPermission(player);
                    case CONFIG:
                    case GO:
                        return SimplePermissionType.CONFIG.checkPermission(player);
                    case SIGN:
                    case DIALER:
                    case USE:
                    case COMPASS:
                        return SimplePermissionType.USE.checkPermission(player);
                    case LIST:
                    case LIST_ALL:
                    case TOP:
                        return SimplePermissionType.CONFIG.checkPermission(player) || SimplePermissionType.USE.checkPermission(player);
                    case LIST_SELF:
                        return SimplePermissionType.USE.checkPermission(player) || SimplePermissionType.BUILD.checkPermission(player) || SimplePermissionType.CONFIG.checkPermission(player);
                    case BUILD:
                        return SimplePermissionType.BUILD.checkPermission(player);
                    default:
                        return false;
                }
            }
            String networkName = "Public";
            switch (permissionType) {
                case DAMAGE:
                case REMOVE:
                    return ComplexPermissionType.CONFIG.checkPermission(player) || ComplexPermissionType.REMOVE_ALL.checkPermission(player) || ComplexPermissionType.REMOVE_OWN.checkPermission(player, stargate);
                case CONFIG:
                    return ComplexPermissionType.CONFIG.checkPermission(player);
                case GO:
                    return ComplexPermissionType.GO.checkPermission(player);
                case SIGN:
                    if (stargate != null && stargate.getGateNetwork() != null) {
                        networkName = stargate.getGateNetwork().getNetworkName();
                    }
                    return ComplexPermissionType.USE_SIGN.checkPermission(player) && (networkName.equals("Public") || (!networkName.equals("Public") && ComplexPermissionType.NETWORK_USE.checkPermission(player, networkName)));
                case DIALER:
                    if (stargate != null && stargate.getGateNetwork() != null) {
                        networkName = stargate.getGateNetwork().getNetworkName();
                    }
                    return ComplexPermissionType.USE_DIALER.checkPermission(player) && (networkName.equals("Public") || (!networkName.equals("Public") && ComplexPermissionType.NETWORK_USE.checkPermission(player, networkName)));
                case USE:
                    if (stargate != null && stargate.getGateNetwork() != null) {
                        networkName = stargate.getGateNetwork().getNetworkName();
                    }
                    return (ComplexPermissionType.USE_SIGN.checkPermission(player) && (networkName.equals("Public") || (!networkName.equals("Public") && ComplexPermissionType.NETWORK_USE.checkPermission(player, networkName)))) || (ComplexPermissionType.USE_DIALER.checkPermission(player) && (networkName.equals("Public") || (!networkName.equals("Public") && ComplexPermissionType.NETWORK_USE.checkPermission(player, networkName))));
                case LIST:
                case LIST_ALL:
                case TOP:
                    return ComplexPermissionType.LIST_ALL.checkPermission(player) || ComplexPermissionType.CONFIG.checkPermission(player);
                case LIST_SELF:
                    return ComplexPermissionType.LIST_SELF.checkPermission(player) || ComplexPermissionType.LIST_ALL.checkPermission(player) || ComplexPermissionType.CONFIG.checkPermission(player);
                case COMPASS:
                    return ComplexPermissionType.USE_COMPASS.checkPermission(player);
                case BUILD:
                    if (stargate != null) {
                        if (stargate.getGateNetwork() != null) {
                            networkName = stargate.getGateNetwork().getNetworkName();
                        }
                    } else if (network != null) {
                        networkName = network;
                    }
                    if (ComplexPermissionType.BUILD_ALL.checkPermission(player)) {
                        return true;
                    }
                    return ComplexPermissionType.BUILD.checkPermission(player) && (networkName.equals("Public") || (!networkName.equals("Public") && ComplexPermissionType.NETWORK_BUILD.checkPermission(player, networkName)));
                case USE_COOLDOWN_GROUP_ONE:
                    return ComplexPermissionType.USE_COOLDOWN_GROUP_ONE.checkPermission(player);
                case USE_COOLDOWN_GROUP_TWO:
                    return ComplexPermissionType.USE_COOLDOWN_GROUP_TWO.checkPermission(player);
                case USE_COOLDOWN_GROUP_THREE:
                    return ComplexPermissionType.USE_COOLDOWN_GROUP_THREE.checkPermission(player);
                case BUILD_RESTRICTION_GROUP_ONE:
                    return ComplexPermissionType.BUILD_RESTRICTION_GROUP_ONE.checkPermission(player);
                case BUILD_RESTRICTION_GROUP_TWO:
                    return ComplexPermissionType.BUILD_RESTRICTION_GROUP_TWO.checkPermission(player);
                case BUILD_RESTRICTION_GROUP_THREE:
                    return ComplexPermissionType.BUILD_RESTRICTION_GROUP_THREE.checkPermission(player);
                default:
                    return false;
            }
        }
        // No permissions plugin available (or support disabled). Ops were already
        // granted above, so anything reaching here is a non-op. The gate-scoped
        // fallback below needs a stargate to reason about, which the list
        // commands never have, so they failed outright and left /wxlist unusable
        // for everyone but ops.
        if (stargate == null) {
            switch (permissionType) {
                case LIST_SELF:
                    // Listing your own gates exposes nothing you did not build.
                    return true;
                case LIST:
                case LIST_ALL:
                case TOP:
                    // Listing every gate on the server stays op-only.
                    return false;
                default:
                    return false;
            }
        }
        if (stargate != null) {
            switch (permissionType) {
                case DAMAGE:
                case REMOVE:
                case CONFIG:
                case GO:
                    return PermissionsManager.getPermissionLevel(player, stargate) == PermissionsManager.PermissionLevel.WORMHOLE_FULL_PERMISSION;
                case SIGN:
                case DIALER:
                case USE:
                case LIST:
                case COMPASS:
                    PermissionsManager.PermissionLevel lvl = PermissionsManager.getPermissionLevel(player, stargate);
                    return lvl == PermissionsManager.PermissionLevel.WORMHOLE_CREATE_PERMISSION || lvl == PermissionsManager.PermissionLevel.WORMHOLE_USE_PERMISSION || lvl == PermissionsManager.PermissionLevel.WORMHOLE_FULL_PERMISSION;
                case BUILD:
                    PermissionsManager.PermissionLevel lvl2 = PermissionsManager.getPermissionLevel(player, stargate);
                    return lvl2 == PermissionsManager.PermissionLevel.WORMHOLE_CREATE_PERMISSION || lvl2 == PermissionsManager.PermissionLevel.WORMHOLE_FULL_PERMISSION;
                default:
                    return false;
            }
        }
        return false;
    }

    public static boolean checkPermission(Player player, String network, PermissionType permissiontype) {
        return checkPermission(player, null, network, permissiontype);
    }
}
