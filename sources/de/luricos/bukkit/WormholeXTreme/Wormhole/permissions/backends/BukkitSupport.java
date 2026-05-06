package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.backends;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.logging.Level;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/backends/BukkitSupport.class */
public class BukkitSupport extends PermissionBackend {
    public BukkitSupport(PermissionManager manager, ConfigManager config, String providerName) {
        super(manager, config, providerName);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend
    public void initialize() {
        WXTLogger.prettyLog(Level.INFO, false, "Attached to Bukkit");
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend
    public void reload() {
        WXTLogger.prettyLog(Level.INFO, false, "Detached from BukkitSupport");
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend
    public boolean hasPermission(Player player, String permissionString) {
        return player.hasPermission(permissionString);
    }
}
