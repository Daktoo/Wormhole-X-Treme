package de.luricos.bukkit.WormholeXTreme.Wormhole.plugin;

import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.logging.Level;

public class WormholeWorldsSupport {
    public static void disableWormholeWorlds() {
        WXTLogger.prettyLog(Level.INFO, false, "WormholeWorlds support is not available.");
    }

    public static void enableWormholeWorlds() {
        WXTLogger.prettyLog(Level.INFO, false, "WormholeWorlds support is not available.");
    }

    public static void enableWormholeWorlds(boolean reload) {
        WXTLogger.prettyLog(Level.INFO, false, "WormholeWorlds support is not available.");
    }

    public static boolean isEnabled() {
        return false;
    }

    public static boolean isSupportedVersion(String verIn) {
        return false;
    }

    public static boolean isSupportedVersion(String verIn, Double checkVer) {
        return false;
    }
}
