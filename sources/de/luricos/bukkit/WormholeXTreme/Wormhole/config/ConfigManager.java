package de.luricos.bukkit.WormholeXTreme.Wormhole.config;

import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionsManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.plugin.PluginDescriptionFile;

public class ConfigManager {
    private static final ConcurrentHashMap<ConfigKeys, Setting> configurations = new ConcurrentHashMap<>();


    public enum ConfigKeys {
        BUILT_IN_PERMISSIONS_ENABLED,
        BUILT_IN_DEFAULT_PERMISSION_LEVEL,
        PERMISSIONS_SUPPORT_DISABLE,
        SIMPLE_PERMISSIONS,
        WORMHOLE_USE_IS_TELEPORT,
        TIMEOUT_ACTIVATE,
        TIMEOUT_SHUTDOWN,
        BUILD_RESTRICTION_ENABLED,
        BUILD_RESTRICTION_GROUP_ONE,
        BUILD_RESTRICTION_GROUP_TWO,
        BUILD_RESTRICTION_GROUP_THREE,
        USE_COOLDOWN_ENABLED,
        USE_COOLDOWN_GROUP_ONE,
        USE_COOLDOWN_GROUP_TWO,
        USE_COOLDOWN_GROUP_THREE,
        HELP_SUPPORT_DISABLE,
        WORLDS_SUPPORT_ENABLED,
        LOG_LEVEL,
        SHOW_GATE_WELCOME_MESSAGE,
        USE_EVENT_OR_TP_TRANSPORT,
        WORMHOLE_KICKBACK_BLOCK_COUNT,
        PERMISSIONS_BACKEND,
        ECONOMY_ENABLED
    }


    public enum MessageStrings {
        messageColor("§7"),
        errorHeader("§3:: §5error §3:: §7"),
        normalHeader("§3:: §7"),
        permissionNo(errorHeader + "You lack the permissions to do this."),
        targetIsSelf(errorHeader + "Can't dial own gate without solar flare"),
        targetInvalid(errorHeader + "Invalid gate target."),
        targetIsActive(errorHeader + "Target gate %sis currently active."),
        targetIsInUseBy(errorHeader + "Target gate %s is currently in use by %s."),
        gateNotActive(errorHeader + "No gate activated to dial."),
        gateRemoteActive(errorHeader + "Gate %sremotely activated%s."),
        gateShutdown(normalHeader + "Gate %ssuccessfully shutdown."),
        gateActivated(normalHeader + "Gate %ssuccessfully activated."),
        gateDeactivated(normalHeader + "Gate %ssuccessfully deactivated."),
        gateConnected(normalHeader + "Stargates connected."),
        gateIsInvalid(errorHeader + "Stargate has not a valid setup. Please check your log for errors."),
        gateWithInvalidShape(errorHeader + "No valid Stargate shape was found."),
        gateWithInvalidShapeAssistance(normalHeader + "Type /wxbuild for build assistance."),
        constructSuccess(normalHeader + "Gate successfully constructed."),
        constructNameInvalid(errorHeader + "Gate name invalid: "),
        constructNameTooLong(errorHeader + "Gate name too long: "),
        constructNameTaken(errorHeader + "Gate name already taken: "),
        requestInvalid(errorHeader + "Invalid Request"),
        gateNotSpecified(errorHeader + "No gate name specified."),
        playerBuildCountRestricted(errorHeader + "You are at your max number of built gates."),
        playerUseCooldownRestricted(errorHeader + "You must wait longer before using a stargate."),
        playerUseCooldownWaitTime(errorHeader + "Current Wait (in seconds): "),
        playerUsedStargate(normalHeader + "Welcome at %s%s");

        private final String m;

        MessageStrings(String message) {
            this.m = message;
        }

        @Override
        public String toString() {
            return this.m;
        }
    }

    public static String getPermissionBackend() {
        return isConfigurationKey(ConfigKeys.PERMISSIONS_BACKEND) ? getSetting(ConfigKeys.PERMISSIONS_BACKEND).getStringValue() : "bukkit";
    }

    public static int getBuildRestrictionGroupOne() {
        if (isConfigurationKey(ConfigKeys.BUILD_RESTRICTION_GROUP_ONE)) {
            return getSetting(ConfigKeys.BUILD_RESTRICTION_GROUP_ONE).getIntValue();
        }
        return 1;
    }

    public static int getBuildRestrictionGroupThree() {
        if (isConfigurationKey(ConfigKeys.BUILD_RESTRICTION_GROUP_THREE)) {
            return getSetting(ConfigKeys.BUILD_RESTRICTION_GROUP_THREE).getIntValue();
        }
        return 3;
    }

    public static int getBuildRestrictionGroupTwo() {
        if (isConfigurationKey(ConfigKeys.BUILD_RESTRICTION_GROUP_TWO)) {
            return getSetting(ConfigKeys.BUILD_RESTRICTION_GROUP_TWO).getIntValue();
        }
        return 2;
    }

    public static PermissionsManager.PermissionLevel getBuiltInDefaultPermissionLevel() {
        Setting bidpl = getConfigurations().get(ConfigKeys.BUILT_IN_DEFAULT_PERMISSION_LEVEL);
        if (bidpl != null) {
            return bidpl.getPermissionLevel();
        }
        return PermissionsManager.PermissionLevel.WORMHOLE_USE_PERMISSION;
    }

    public static boolean getBuiltInPermissionsEnabled() {
        Setting bipe = getConfigurations().get(ConfigKeys.BUILT_IN_PERMISSIONS_ENABLED);
        if (bipe != null) {
            return bipe.getBooleanValue();
        }
        return false;
    }

    public static ConcurrentHashMap<ConfigKeys, Setting> getConfigurations() {
        return configurations;
    }

    public static boolean getHelpSupportDisable() {
        Setting hsd = getConfigurations().get(ConfigKeys.HELP_SUPPORT_DISABLE);
        return hsd != null && hsd.getBooleanValue();
    }

    public static Level getLogLevel() {
        Setting ll = getConfigurations().get(ConfigKeys.LOG_LEVEL);
        if (ll != null) {
            return ll.getLevel();
        }
        return Level.INFO;
    }

    public static boolean isGateArrivalWelcomeMessageEnabled() {
        Setting wme = getConfigurations().get(ConfigKeys.SHOW_GATE_WELCOME_MESSAGE);
        if (wme != null) {
            return wme.getBooleanValue();
        }
        return true;
    }

    public static void setShowGWM(boolean g) {
        setConfigValue(ConfigKeys.SHOW_GATE_WELCOME_MESSAGE, Boolean.valueOf(g));
    }

    public static boolean getGateTransportMethod() {
        Setting tm = getConfigurations().get(ConfigKeys.USE_EVENT_OR_TP_TRANSPORT);
        if (tm != null) {
            return tm.getBooleanValue();
        }
        return true;
    }

    public static void setGateTransportMethod(boolean tm) {
        setConfigValue(ConfigKeys.USE_EVENT_OR_TP_TRANSPORT, Boolean.valueOf(tm));
    }

    public static int getWormholeKickbackBlockCount() {
        if (isConfigurationKey(ConfigKeys.WORMHOLE_KICKBACK_BLOCK_COUNT)) {
            return getSetting(ConfigKeys.WORMHOLE_KICKBACK_BLOCK_COUNT).getIntValue();
        }
        return 2;
    }

    public static void setWormholeKickbackBlockCount(int wkbCount) {
        setConfigValue(ConfigKeys.WORMHOLE_KICKBACK_BLOCK_COUNT, Integer.valueOf(wkbCount));
    }

    public static boolean getPermissionsSupportDisable() {
        Setting psd = getConfigurations().get(ConfigKeys.PERMISSIONS_SUPPORT_DISABLE);
        return psd != null && psd.getBooleanValue();
    }

    private static Setting getSetting(ConfigKeys configKey) {
        return getConfigurations().get(configKey);
    }

    public static boolean getSimplePermissions() {
        Setting sp = getConfigurations().get(ConfigKeys.SIMPLE_PERMISSIONS);
        if (sp != null) {
            return sp.getBooleanValue();
        }
        return false;
    }

    public static int getTimeoutActivate() {
        Setting ta = getConfigurations().get(ConfigKeys.TIMEOUT_ACTIVATE);
        if (ta != null) {
            return ta.getIntValue();
        }
        return 30;
    }

    public static int getTimeoutShutdown() {
        Setting ts = getConfigurations().get(ConfigKeys.TIMEOUT_SHUTDOWN);
        if (ts != null) {
            return ts.getIntValue();
        }
        return 38;
    }

    public static int getUseCooldownGroupOne() {
        if (isConfigurationKey(ConfigKeys.USE_COOLDOWN_GROUP_ONE)) {
            return getSetting(ConfigKeys.USE_COOLDOWN_GROUP_ONE).getIntValue();
        }
        return 120;
    }

    public static int getUseCooldownGroupThree() {
        if (isConfigurationKey(ConfigKeys.USE_COOLDOWN_GROUP_THREE)) {
            return getSetting(ConfigKeys.USE_COOLDOWN_GROUP_THREE).getIntValue();
        }
        return 60;
    }

    public static int getUseCooldownGroupTwo() {
        if (isConfigurationKey(ConfigKeys.USE_COOLDOWN_GROUP_TWO)) {
            return getSetting(ConfigKeys.USE_COOLDOWN_GROUP_TWO).getIntValue();
        }
        return 30;
    }

    public static boolean getWormholeUseIsTeleport() {
        Setting bipe = getConfigurations().get(ConfigKeys.WORMHOLE_USE_IS_TELEPORT);
        if (bipe != null) {
            return bipe.getBooleanValue();
        }
        return false;
    }

    public static boolean isBuildRestrictionEnabled() {
        return getConfigurations().get(ConfigKeys.BUILD_RESTRICTION_ENABLED) != null && getConfigurations().get(ConfigKeys.BUILD_RESTRICTION_ENABLED).getBooleanValue();
    }

    private static boolean isConfigurationKey(ConfigKeys configKey) {
        return getConfigurations().containsKey(configKey);
    }

    public static boolean isUseCooldownEnabled() {
        return getConfigurations().get(ConfigKeys.USE_COOLDOWN_ENABLED) != null && getConfigurations().get(ConfigKeys.USE_COOLDOWN_ENABLED).getBooleanValue();
    }

    public static boolean isWormholeWorldsSupportEnabled() {
        Setting wsd = getConfigurations().get(ConfigKeys.WORLDS_SUPPORT_ENABLED);
        if (wsd != null) {
            return wsd.getBooleanValue();
        }
        return false;
    }

    public static void setBuildRestrictionEnabled(boolean b) {
        setConfigValue(ConfigKeys.BUILD_RESTRICTION_ENABLED, Boolean.valueOf(b));
    }

    public static void setBuildRestrictionGroupOne(int count) {
        setConfigValue(ConfigKeys.BUILD_RESTRICTION_GROUP_ONE, Integer.valueOf(count));
    }

    public static void setBuildRestrictionGroupThree(int count) {
        setConfigValue(ConfigKeys.BUILD_RESTRICTION_GROUP_THREE, Integer.valueOf(count));
    }

    public static void setBuildRestrictionGroupTwo(int count) {
        setConfigValue(ConfigKeys.BUILD_RESTRICTION_GROUP_TWO, Integer.valueOf(count));
    }

    public static void setConfigValue(ConfigKeys key, Object value) {
        if (key != null && isConfigurationKey(key) && value != null) {
            getConfigurations().get(key).setValue(value);
        }
    }

    public static void setSimplePermissions(boolean b) {
        setConfigValue(ConfigKeys.SIMPLE_PERMISSIONS, Boolean.valueOf(b));
    }

    public static void setTimeoutActivate(int i) {
        setConfigValue(ConfigKeys.TIMEOUT_ACTIVATE, Integer.valueOf(i));
    }

    public static void setTimeoutShutdown(int i) {
        setConfigValue(ConfigKeys.TIMEOUT_SHUTDOWN, Integer.valueOf(i));
    }

    public static void setupConfigs(PluginDescriptionFile pdf) {
        Configuration.loadConfiguration(pdf);
    }

    public static void setUseCooldownEnabled(boolean b) {
        setConfigValue(ConfigKeys.USE_COOLDOWN_ENABLED, Boolean.valueOf(b));
    }

    public static void setUseCooldownGroupOne(int time) {
        setConfigValue(ConfigKeys.USE_COOLDOWN_GROUP_ONE, Integer.valueOf(time));
    }

    public static void setUseCooldownGroupThree(int time) {
        setConfigValue(ConfigKeys.USE_COOLDOWN_GROUP_THREE, Integer.valueOf(time));
    }

    public static void setUseCooldownGroupTwo(int time) {
        setConfigValue(ConfigKeys.USE_COOLDOWN_GROUP_TWO, Integer.valueOf(time));
    }

    public static boolean isEconomyEnabled() {
        Setting s = getConfigurations().get(ConfigKeys.ECONOMY_ENABLED);
        return s != null && s.getBooleanValue();
    }

    public static void setEconomyEnabled(boolean b) {
        setConfigValue(ConfigKeys.ECONOMY_ENABLED, Boolean.valueOf(b));
    }

    public static void setDebugLevel(String level) {
        setConfigValue(ConfigKeys.LOG_LEVEL, level.toUpperCase());
    }

    public static void setPermissionBackend(String backendName) {
        setConfigValue(ConfigKeys.PERMISSIONS_BACKEND, backendName);
    }
}