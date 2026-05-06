package de.luricos.bukkit.WormholeXTreme.Wormhole;

import de.luricos.bukkit.WormholeXTreme.Worlds.handler.WorldHandler;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.Build;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.BuildList;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.Compass;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.Complete;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.Dial;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.Force;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.Go;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.WXIDC;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.WXList;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.WXReload;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.WXRemove;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.WXStatus;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.Wormhole;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.Configuration;
import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholeNotAvailable;
import de.luricos.bukkit.WormholeXTreme.Wormhole.listeners.WormholeXTremeBlockListener;
import de.luricos.bukkit.WormholeXTreme.Wormhole.listeners.WormholeXTremeEntityListener;
import de.luricos.bukkit.WormholeXTreme.Wormhole.listeners.WormholeXTremePlayerListener;
import de.luricos.bukkit.WormholeXTreme.Wormhole.listeners.WormholeXTremeRedstoneListener;
import de.luricos.bukkit.WormholeXTreme.Wormhole.listeners.WormholeXTremeServerListener;
import de.luricos.bukkit.WormholeXTreme.Wormhole.listeners.WormholeXTremeVehicleListener;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateHelper;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateDBManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.backends.BukkitSupport;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.backends.VaultSupport;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayerManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.plugin.WormholeWorldsSupport;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.DBUpdateUtil;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/WormholeXTreme.class */
public class WormholeXTreme extends JavaPlugin {
    protected PermissionManager permissionManager;
    protected ConfigManager configManager;
    private boolean blockPluginExecution = false;
    private static final WormholeXTremePlayerListener playerListener = new WormholeXTremePlayerListener();
    private static final WormholeXTremeBlockListener blockListener = new WormholeXTremeBlockListener();
    private static final WormholeXTremeVehicleListener vehicleListener = new WormholeXTremeVehicleListener();
    private static final WormholeXTremeEntityListener entityListener = new WormholeXTremeEntityListener();
    private static final WormholeXTremeServerListener serverListener = new WormholeXTremeServerListener();
    private static final WormholeXTremeRedstoneListener redstoneListener = new WormholeXTremeRedstoneListener();
    private static WorldHandler worldHandler = null;
    private static BukkitScheduler scheduler = null;

    public void onLoad() {
        WXTLogger.initLogger(getDescription().getName(), getDescription().getVersion(), ConfigManager.getLogLevel());
        WXTLogger.prettyLog(Level.INFO, true, "Loading WormholeXTreme ...");
        setScheduler(getServer().getScheduler());
        ConfigManager.setupConfigs(getDescription());
        this.configManager = null;
        WXTLogger.setLogLevel(ConfigManager.getLogLevel());
        if (!DBUpdateUtil.updateDB()) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Something went wrong during DBUpdate. Please check your server.log for details. Disabling WXT for safety precautions.");
            this.blockPluginExecution = true;
        } else {
            StargateHelper.loadShapes();
            WXTLogger.prettyLog(Level.INFO, true, "Load complete");
        }
    }

    public boolean reloadPlugin() {
        WXTLogger.prettyLog(Level.INFO, true, "Reload in progress...");
        try {
            Configuration.writeFile(getDescription());
            ArrayList<Stargate> gates = StargateManager.getAllGates();
            for (Stargate gate : gates) {
                if (gate.isGateActive() || gate.isGateLightsActive()) {
                    gate.shutdownStargate(false);
                }
                StargateDBManager.stargateToSQL(gate);
            }
            WXTLogger.prettyLog(Level.INFO, true, "Configuration written and stargates saved.");
            StargateDBManager.shutdown();
            WormholePlayerManager.unregisterAllPlayers();
            WormholeWorldsSupport.disableWormholeWorlds();
            ConfigManager.setupConfigs(getDescription());
            WXTLogger.setLogLevel(ConfigManager.getLogLevel());
            StargateHelper.reloadShapes();
            if (!ConfigManager.isWormholeWorldsSupportEnabled()) {
                WXTLogger.prettyLog(Level.INFO, true, "Wormhole Worlds support disabled in settings.txt, loading stargates and worlds ourself.");
                StargateDBManager.loadStargates(getServer());
            }
            this.permissionManager.reset();
            WormholeWorldsSupport.enableWormholeWorlds(true);
            WormholePlayerManager.registerAllOnlinePlayers();
            WXTLogger.prettyLog(Level.INFO, true, "Reloading complete.");
            return true;
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Caught exception while reloading: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void onEnable() {
        if (this.blockPluginExecution) {
            WXTLogger.prettyLog(Level.INFO, true, "Startup is blocked because of a previous database error. Check your server.log");
            return;
        }
        WXTLogger.prettyLog(Level.INFO, true, "Boot sequence initiated...");
        if (!ConfigManager.isWormholeWorldsSupportEnabled()) {
            WXTLogger.prettyLog(Level.INFO, true, "Wormhole Worlds support disabled in settings.txt, loading stargates and worlds by our self.");
            StargateDBManager.loadStargates(getServer());
        }
        try {
            PermissionBackend.registerBackendAlias("vault", VaultSupport.class);
            PermissionBackend.registerBackendAlias("bukkit", BukkitSupport.class);
            resolvePermissionBackends();
            if (this.permissionManager == null) {
                this.permissionManager = new PermissionManager(this.configManager);
            }
            if (ConfigManager.isWormholeWorldsSupportEnabled()) {
                WormholeWorldsSupport.enableWormholeWorlds();
            }
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Caught Exception while trying to load support plugins. {" + e.getMessage() + "}");
            e.printStackTrace();
        }
        registerEvents(true);
        if (!ConfigManager.isWormholeWorldsSupportEnabled()) {
            registerEvents(false);
            registerCommands();
        }
        WormholePlayerManager.registerAllOnlinePlayers();
        WXTLogger.prettyLog(Level.INFO, true, "Boot sequence completed");
    }

    public void onDisable() {
        if (this.blockPluginExecution) {
            WXTLogger.prettyLog(Level.INFO, true, "Disable Functions skipped because of a previous error.");
            return;
        }
        WXTLogger.prettyLog(Level.INFO, true, "Shutdown sequence initiated...");
        try {
            Configuration.writeFile(getDescription());
            ArrayList<Stargate> gates = StargateManager.getAllGates();
            for (Stargate gate : gates) {
                if (gate.isGateActive() || gate.isGateLightsActive()) {
                    gate.shutdownStargate(false);
                }
                WXTLogger.prettyLog(Level.FINE, false, "Saving gate: '" + gate.getGateName() + "', GateFace: '" + gate.getGateFacing().name() + "'");
                StargateDBManager.stargateToSQL(gate);
            }
            StargateDBManager.shutdown();
            if (this.permissionManager != null) {
                this.permissionManager.end();
            }
            WormholePlayerManager.unregisterAllPlayers();
            WXTLogger.prettyLog(Level.INFO, true, "Successfully shutdown WXT.");
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Caught exception while shutting down: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static PermissionManager getPermissionManager() {
        try {
            if (!isPluginAvailable() && WXTLogger.getLogLevel().intValue() < Level.WARNING.intValue()) {
                throw new WormholeNotAvailable("This plugin is not ready yet." + (!getThisPlugin().isEnabled() ? " Loading sequence is still in progress." : ""));
            }
        } catch (WormholeNotAvailable e) {
            WXTLogger.prettyLog(Level.WARNING, false, e.getMessage());
        }
        return getThisPlugin().permissionManager;
    }

    private void resolvePermissionBackends() {
        for (String providerAlias : PermissionBackend.getRegisteredAliases()) {
            String pluginName = PermissionBackend.getBackendPluginName(providerAlias);
            WXTLogger.prettyLog(Level.INFO, false, "Attempting to use supported permissions plugin '" + pluginName + "'");
            Plugin permToLoad = Bukkit.getPluginManager().getPlugin(pluginName);
            if (pluginName.equals(PermissionBackend.getDefaultBackend().getProviderName()) || (permToLoad != null && permToLoad.isEnabled())) {
                ConfigManager.setPermissionBackend(providerAlias);
                WXTLogger.prettyLog(Level.INFO, false, "Config node PERMISSIONS_BACKEND changed to '" + providerAlias + "'");
                return;
            }
            WXTLogger.prettyLog(Level.FINE, false, "Permission backend '" + providerAlias + "' was not found as plugin or not enabled!");
        }
    }

    public static BukkitScheduler getScheduler() {
        return scheduler;
    }

    public static WormholeXTreme getThisPlugin() {
        org.bukkit.plugin.Plugin _rawPlugin = Bukkit.getServer().getPluginManager().getPlugin("WormholeXTreme");
        WormholeXTreme plugin = (_rawPlugin instanceof WormholeXTreme) ? (WormholeXTreme) _rawPlugin : null;
        if (plugin == null) {
            throw new RuntimeException("'WormholeXTreme' not found. 'WormholeXTreme' plugin disabled?");
        }
        return plugin;
    }

    public static WorldHandler getWorldHandler() {
        return worldHandler;
    }

    public static void registerCommands() {
        WormholeXTreme tp = getThisPlugin();
        tp.getCommand("wxforce").setExecutor(new Force());
        tp.getCommand("wxidc").setExecutor(new WXIDC());
        tp.getCommand("wxcompass").setExecutor(new Compass());
        tp.getCommand("wxcomplete").setExecutor(new Complete());
        tp.getCommand("wxremove").setExecutor(new WXRemove());
        tp.getCommand("wxlist").setExecutor(new WXList());
        tp.getCommand("wxgo").setExecutor(new Go());
        tp.getCommand("dial").setExecutor(new Dial());
        tp.getCommand("wxbuild").setExecutor(new Build());
        tp.getCommand("wxbuildlist").setExecutor(new BuildList());
        tp.getCommand("wormhole").setExecutor(new Wormhole());
        tp.getCommand("wxreload").setExecutor(new WXReload());
        tp.getCommand("wxstatus").setExecutor(new WXStatus());
    }

    public static void registerEvents(boolean critical) {
        WormholeXTreme wxt = getThisPlugin();
        if (critical) {
            Bukkit.getServer().getPluginManager().registerEvents(serverListener, wxt);
            return;
        }
        Bukkit.getServer().getPluginManager().registerEvents(blockListener, wxt);
        Bukkit.getServer().getPluginManager().registerEvents(playerListener, wxt);
        Bukkit.getServer().getPluginManager().registerEvents(redstoneListener, wxt);
        Bukkit.getServer().getPluginManager().registerEvents(vehicleListener, wxt);
        Bukkit.getServer().getPluginManager().registerEvents(entityListener, wxt);
    }

    protected static void setScheduler(BukkitScheduler scheduler2) {
        scheduler = scheduler2;
    }

    public static void setWorldHandler(WorldHandler worldHandler2) {
        worldHandler = worldHandler2;
    }

    public static boolean isPluginAvailable() {
        WormholeXTreme thisPlugin = getThisPlugin();
        return (thisPlugin instanceof WormholeXTreme) && thisPlugin.permissionManager != null;
    }
}
