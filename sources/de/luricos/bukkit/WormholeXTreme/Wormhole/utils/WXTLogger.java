package de.luricos.bukkit.WormholeXTreme.Wormhole.utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/utils/WXTLogger.class */
public class WXTLogger {
    private static Level logLevel = Level.INFO;
    private static Logger logger = null;
    private static String logPluginName = null;
    private static String logPluginVersion = null;

    public static void initLogger(String pluginName, String pluginVersion, Level logLevel2) {
        if (logger == null) {
            Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(pluginName);
            if (plugin != null) {
                logger = Logger.getLogger(plugin.getServer().getLogger().getName() + "." + pluginName);
            }
            logLevel = logLevel2;
            logger.setLevel(logLevel2);
            logPluginName = pluginName;
            logPluginVersion = pluginVersion;
        }
    }

    public static void setLogLevel(Level logLevel2) {
        logLevel = logLevel2;
        logger.setLevel(logLevel2);
    }

    public static void prettyLog(Level logLevel2, boolean version, String message) {
        String prettyName = "[" + getName() + "]";
        String prettyVersion = "[v" + getVersion() + "]";
        String prettyLogLine = prettyName;
        if (version) {
            prettyLogLine = prettyLogLine + prettyVersion;
        }
        logger.log(logLevel2, prettyLogLine + " " + message);
    }

    public static Level getLogLevel() {
        return logLevel;
    }

    public static String getVersion() {
        return logPluginVersion;
    }

    public static String getName() {
        return logPluginName;
    }

    public static void info(String message) {
        prettyLog(Level.INFO, false, message);
    }

    public static void info(String message, boolean version) {
        prettyLog(Level.INFO, version, message);
    }

    public static void warn(String message) {
        prettyLog(Level.WARNING, false, message);
    }

    public static void severe(String message) {
        prettyLog(Level.SEVERE, false, message);
    }
}
