package de.luricos.bukkit.WormholeXTreme.Wormhole.plugin;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class PermissionsSupport {
    public static void disablePermissions() {
        WXTLogger.info("Detached from Permissions plugin.");
    }

    public static void enablePermissions() {
        if (ConfigManager.getPermissionsSupportDisable()) {
            WXTLogger.info("Permission Plugin support disabled via settings.txt.");
            return;
        }
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp != null) {
            WXTLogger.info("Attached to Vault permissions (" + rsp.getProvider().getName() + ")");
            if (ConfigManager.getSimplePermissions()) {
                WXTLogger.info("Simple Permissions Enabled");
            } else {
                WXTLogger.info("Complex Permissions Enabled");
            }
        } else {
            WXTLogger.info("Vault permissions not available. Defaulting to built-in Bukkit permissions.");
        }
    }
}
