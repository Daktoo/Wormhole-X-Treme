package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.events.WormholeSystemEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/PermissionManager.class */
public class PermissionManager {
    protected PermissionBackend backend = null;
    protected ConfigManager configManager;

    public PermissionManager(ConfigManager configManager) {
        this.configManager = configManager;
        initBackend();
    }

    private void initBackend() {
        String backendName = ConfigManager.getConfigurations().get(ConfigManager.ConfigKeys.PERMISSIONS_BACKEND).getStringValue();
        if (backendName == null || backendName.isEmpty()) {
            backendName = "bukkit";
            ConfigManager.setPermissionBackend(backendName);
        }
        setBackend(backendName);
    }

    public PermissionBackend getBackend() {
        return this.backend;
    }

    public void setBackend(String backendName) {
        synchronized (this) {
            this.backend = PermissionBackend.getBackend(backendName, this, this.configManager);
            this.backend.initialize();
        }
        callEvent(WormholeSystemEvent.Action.PERMISSION_BACKEND_CHANGED);
    }

    protected void callEvent(WormholeSystemEvent event) {
        Bukkit.getServer().getPluginManager().callEvent(event);
    }

    protected void callEvent(WormholeSystemEvent.Action action) {
        callEvent(new WormholeSystemEvent(action));
    }

    public void reset() {
        if (this.backend != null) {
            this.backend.reload();
        }
        callEvent(WormholeSystemEvent.Action.RELOADED);
    }

    public void end() {
        reset();
    }

    public boolean has(Player player, String permissionString) {
        return this.backend.has(player, permissionString);
    }

    public boolean hasPermission(Player player, String permissionString) {
        return this.backend.hasPermission(player, permissionString);
    }
}
