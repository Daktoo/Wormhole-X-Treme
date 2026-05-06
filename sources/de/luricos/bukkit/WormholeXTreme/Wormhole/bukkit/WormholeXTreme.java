package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit;

import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.GateCommands;
import de.luricos.bukkit.WormholeXTreme.Wormhole.commands.CommandManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.backends.YmlConfigurationSupport;
import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholeNotAvailable;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.backends.BukkitPermissionsSupport;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.backends.VaultSupport;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/WormholeXTreme.class */
public class WormholeXTreme extends JavaPlugin {
    private ConfigurationManager configManager;
    private CommandManager commandManager;
    private PermissionManager permissionManager;

    public void onLoad() {
        WXTLogger.initLogger(getDescription().getName(), getDescription().getVersion(), Level.INFO);
        WXTLogger.info("Loading WormholeXTreme ...", true);
        ConfigurationBackend.registerBackendAlias("bukkit", YmlConfigurationSupport.class);
        WXTLogger.info("Load complete", true);
    }

    public boolean reloadPlugin() {
        WXTLogger.info("Reload in progress...", true);
        return true;
    }

    public void onEnable() {
        try {
            if (this.configManager == null) {
                this.configManager = new ConfigurationManager(getConfig());
            }
            WXTLogger.setLogLevel(this.configManager.getLogLevel());
            PermissionBackend.registerBackendAlias("vault", VaultSupport.class);
            PermissionBackend.registerBackendAlias("bukkit", BukkitPermissionsSupport.class);
            PermissionBackend.resolvePermissionBackends();
            if (this.permissionManager == null) {
                this.permissionManager = new PermissionManager(null);
            }
            if (this.commandManager == null) {
                this.commandManager = new CommandManager(this);
            }
            this.commandManager.register(new GateCommands());
        } catch (Exception e) {
            WXTLogger.severe(String.format("Caught Exception while trying to load support plugins. {%s}", e.getMessage()));
            e.printStackTrace();
        }
        WXTLogger.info("Boot sequence completed", true);
    }

    public void onDisable() {
        if (this.permissionManager != null) {
            this.permissionManager.end();
        }
        if (this.configManager != null) {
            this.configManager.end();
        }
    }

    private void initConfiguration() {
        if (this.configManager == null) {
            this.configManager = new ConfigurationManager(getConfig());
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        try {
            PluginDescriptionFile pdf = getDescription();
            if (args.length > 0) {
                return this.commandManager.execute(sender, command, args);
            }
            if (sender instanceof Player) {
                sender.sendMessage("[" + ChatColor.RED + "WormholeXTreme" + ChatColor.WHITE + "] version [" + ChatColor.BLUE + pdf.getVersion() + ChatColor.WHITE + "]");
                return !this.permissionManager.has((Player) sender, "wormhole.manage");
            }
            sender.sendMessage("[WormholeXTreme] version [" + pdf.getVersion() + "]");
            return false;
        } catch (Throwable th) {
            return true;
        }
    }

    public static PermissionManager getPermissionManager() {
        try {
            if (!isPluginAvailable() && WXTLogger.getLogLevel().intValue() < Level.WARNING.intValue()) {
                throw new WormholeNotAvailable("This plugin is not ready yet." + (!getPlugin().isEnabled() ? " Loading sequence is still in progress." : ""));
            }
        } catch (WormholeNotAvailable e) {
            WXTLogger.warn(e.getMessage());
        }
        return getPlugin().permissionManager;
    }

    public static ConfigurationManager getConfigManager() {
        try {
            if (!isPluginAvailable() && WXTLogger.getLogLevel().intValue() < Level.WARNING.intValue()) {
                throw new WormholeNotAvailable("This plugin is not ready yet." + (!getPlugin().isEnabled() ? " Loading sequence is still in progress." : ""));
            }
        } catch (WormholeNotAvailable e) {
            WXTLogger.warn(e.getMessage());
        }
        return getPlugin().configManager;
    }

    public static WormholeXTreme getPlugin() {
        org.bukkit.plugin.Plugin _rawPlugin = Bukkit.getServer().getPluginManager().getPlugin("WormholeXTreme");
        WormholeXTreme plugin = (_rawPlugin instanceof WormholeXTreme) ? (WormholeXTreme) _rawPlugin : null;
        if (plugin == null) {
            throw new RuntimeException("'WormholeXTreme' not found. 'WormholeXTreme' plugin disabled?");
        }
        return plugin;
    }

    public static void registerCommands() {
    }

    public void registerEvents(boolean critical) {
    }

    public static boolean isPluginAvailable() {
        WormholeXTreme plugin = getPlugin();
        if (plugin != null) {
            return plugin.permissionManager != null;
        }
        WXTLogger.severe("Cound not fetch plugin instance!");
        return false;
    }
}
