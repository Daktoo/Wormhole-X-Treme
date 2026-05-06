package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateUpdateRunnable;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayerManager;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/StargateRestrictions.class */
public class StargateRestrictions {
    private static final ConcurrentHashMap<Player, Long> playerUseCooldownStart = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Player, RestrictionGroup> playerUseCooldownGroup = new ConcurrentHashMap<>();

    /* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/StargateRestrictions$RestrictionGroup.class */
    private enum RestrictionGroup {
        CD_GROUP_ONE(ConfigManager.getUseCooldownGroupOne()),
        CD_GROUP_TWO(ConfigManager.getUseCooldownGroupTwo()),
        CD_GROUP_THREE(ConfigManager.getUseCooldownGroupThree()),
        BR_GROUP_ONE(ConfigManager.getBuildRestrictionGroupOne()),
        BR_GROUP_TWO(ConfigManager.getBuildRestrictionGroupTwo()),
        BR_GROUP_THREE(ConfigManager.getBuildRestrictionGroupThree());

        private final long restrictionGroupNode;

        RestrictionGroup(long restrictionGroupNode) {
            this.restrictionGroupNode = restrictionGroupNode;
        }

        public long getGroupValue() {
            return this.restrictionGroupNode;
        }
    }

    public static void addPlayerUseCooldown(Player player) {
        RestrictionGroup cooldownGroup = null;
        if (WXPermissions.checkPermission(player, WXPermissions.PermissionType.USE_COOLDOWN_GROUP_ONE)) {
            cooldownGroup = RestrictionGroup.CD_GROUP_ONE;
        } else if (WXPermissions.checkPermission(player, WXPermissions.PermissionType.USE_COOLDOWN_GROUP_TWO)) {
            cooldownGroup = RestrictionGroup.CD_GROUP_TWO;
        } else if (WXPermissions.checkPermission(player, WXPermissions.PermissionType.USE_COOLDOWN_GROUP_THREE)) {
            cooldownGroup = RestrictionGroup.CD_GROUP_THREE;
        }
        if (cooldownGroup != null) {
            getPlayerUseCooldownStart().put(player, Long.valueOf(System.nanoTime()));
            getPlayerUseCooldownGroup().put(player, cooldownGroup);
            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(WormholePlayerManager.getRegisteredWormholePlayer(player).getStargate(), StargateUpdateRunnable.ActionToTake.COOLDOWN_REMOVE), cooldownGroup.getGroupValue() * 20);
        }
    }

    public static long checkPlayerUseCooldownRemaining(Player player) {
        if (getPlayerUseCooldownStart().containsKey(player) && getPlayerUseCooldownGroup().containsKey(player)) {
            long startTime = getPlayerUseCooldownStart().get(player).longValue();
            long currentTime = System.nanoTime();
            long elapsedTime = (currentTime - startTime) / 1000000000;
            return getPlayerUseCooldownGroup().get(player).getGroupValue() >= elapsedTime ? getPlayerUseCooldownGroup().get(player).getGroupValue() - elapsedTime : removePlayerUseCooldown(player);
        }
        return -1L;
    }

    private static ConcurrentHashMap<Player, RestrictionGroup> getPlayerUseCooldownGroup() {
        return playerUseCooldownGroup;
    }

    private static ConcurrentHashMap<Player, Long> getPlayerUseCooldownStart() {
        return playerUseCooldownStart;
    }

    public static boolean isPlayerBuildRestricted(Player player) {
        if (ConfigManager.isBuildRestrictionEnabled()) {
            RestrictionGroup restrictionGroup = null;
            if (WXPermissions.checkPermission(player, WXPermissions.PermissionType.BUILD_RESTRICTION_GROUP_ONE)) {
                restrictionGroup = RestrictionGroup.BR_GROUP_ONE;
            } else if (WXPermissions.checkPermission(player, WXPermissions.PermissionType.BUILD_RESTRICTION_GROUP_TWO)) {
                restrictionGroup = RestrictionGroup.BR_GROUP_TWO;
            } else if (WXPermissions.checkPermission(player, WXPermissions.PermissionType.BUILD_RESTRICTION_GROUP_THREE)) {
                restrictionGroup = RestrictionGroup.BR_GROUP_THREE;
            }
            int gateCount = 0;
            for (Stargate stargate : StargateManager.getAllGates()) {
                if (stargate.getGateOwner() != null && stargate.getGateOwner().equalsIgnoreCase(player.getName())) {
                    gateCount++;
                }
            }
            return (restrictionGroup == null || gateCount == 0 || ((long) gateCount) < restrictionGroup.getGroupValue()) ? false : true;
        }
        return false;
    }

    public static boolean isPlayerUseCooldown(Player player) {
        return getPlayerUseCooldownStart().containsKey(player) && getPlayerUseCooldownGroup().containsKey(player);
    }

    public static int removePlayerUseCooldown(Player player) {
        if (getPlayerUseCooldownStart().containsKey(player)) {
            getPlayerUseCooldownStart().remove(player);
        }
        if (getPlayerUseCooldownGroup().containsKey(player)) {
            getPlayerUseCooldownGroup().remove(player);
            return 0;
        }
        return 0;
    }
}
