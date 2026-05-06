package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.backends;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.logging.Level;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultSupport extends PermissionBackend {
    protected Permission vaultPermission;

    public VaultSupport(PermissionManager manager, ConfigManager config, String providerName) {
        super(manager, config, providerName);
        this.vaultPermission = null;
    }

    @Override
    public void initialize() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp != null) {
            this.vaultPermission = rsp.getProvider();
            WXTLogger.prettyLog(Level.INFO, false, "Attached to Vault permissions (" + this.vaultPermission.getName() + ")");
        } else {
            WXTLogger.prettyLog(Level.WARNING, false, "Vault permissions not available, falling back to Bukkit permissions.");
        }
    }

    @Override
    public void reload() {
        this.vaultPermission = null;
        WXTLogger.prettyLog(Level.INFO, false, "Detached from Vault permissions.");
    }

    @Override
    public boolean hasPermission(Player player, String permissionString) {
        if (this.vaultPermission != null) {
            return this.vaultPermission.playerHas(player.getWorld().getName(), player.getName(), permissionString);
        }
        // fallback to Bukkit (still works with LuckPerms)
        return player.hasPermission(permissionString);
    }
}
