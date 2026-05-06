package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.backends;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/backends/BukkitPermissionsSupport.class */
public class BukkitPermissionsSupport extends PermissionBackend {
    public BukkitPermissionsSupport(PermissionManager manager, ConfigManager configManager, String providerName) {
        super(manager, configManager, providerName);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend
    public void initialize() {
        WXTLogger.info("Attached to Bukkit");
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend
    public void reload() {
        end();
        initialize();
    }

    public void end() {
        WXTLogger.info("Detached from BukkitPermissionsSupport");
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend
    public boolean hasPermission(Player player, String permissionString) {
        return player.hasPermission(permissionString);
    }
}
