package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholePermissionBackendException;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/permissions/PermissionBackend.class */
public abstract class PermissionBackend {
    protected static final String defaultBackend = "bukkit";
    protected static Map<String, Class<? extends PermissionBackend>> registeredBackendAliases = new HashMap();
    protected PermissionManager manager;
    protected ConfigManager configManager;
    protected String providerName;

    public abstract void initialize();

    public abstract void reload();

    public abstract boolean hasPermission(Player player, String str);

    protected PermissionBackend(PermissionManager manager, ConfigManager configManager, String providerName) {
        this.manager = manager;
        this.configManager = configManager;
        this.providerName = providerName;
    }

    public String getProviderName() {
        return this.providerName;
    }

    public static String getBackendClassName(String alias) {
        if (registeredBackendAliases.containsKey(alias)) {
            return registeredBackendAliases.get(alias).getName();
        }
        return alias;
    }

    public static String getBackendPluginName(String alias) {
        String pluginName = getBackendClassName(alias);
        if (pluginName.lastIndexOf(46) > 0) {
            pluginName = pluginName.substring(pluginName.lastIndexOf(46));
        }
        return pluginName.substring(1, pluginName.length() - "Support".length());
    }

    public static Class<? extends PermissionBackend> getBackendClass(String alias) throws ClassNotFoundException {
        if (!registeredBackendAliases.containsKey(alias)) {
            return (Class<? extends PermissionBackend>) Class.forName(alias);
        }
        return registeredBackendAliases.get(alias);
    }

    public static void registerBackendAlias(String alias, Class<? extends PermissionBackend> backendClass) {
        if (!PermissionBackend.class.isAssignableFrom(backendClass)) {
            throw new WormholePermissionBackendException("Provided class should be subclass of PermissionBackend.class");
        }
        registeredBackendAliases.put(alias, backendClass);
        WXTLogger.prettyLog(Level.INFO, false, "PermissionAlias backend: '" + alias + "' registered!");
    }

    public static String getBackendAlias(Class<? extends PermissionBackend> backendClass) {
        if (registeredBackendAliases.containsValue(backendClass)) {
            for (String alias : registeredBackendAliases.keySet()) {
                if (registeredBackendAliases.get(alias).equals(backendClass)) {
                    return alias;
                }
            }
        }
        return backendClass.getName();
    }

    public static PermissionBackend getDefaultBackend() {
        return getBackend(null, WormholeXTreme.getPermissionManager(), null, defaultBackend);
    }

    public static PermissionBackend getBackend(String backendName, ConfigManager configManager) {
        return getBackend(backendName, WormholeXTreme.getPermissionManager(), configManager, defaultBackend);
    }

    public static PermissionBackend getBackend(String backendName, PermissionManager manager, ConfigManager configManager) {
        return getBackend(backendName, manager, configManager, defaultBackend);
    }

    public static PermissionBackend getBackend(String backendName, PermissionManager manager, ConfigManager configManager, String fallBackBackend) {
        if (backendName == null || backendName.isEmpty()) {
            backendName = defaultBackend;
        }
        String className = getBackendClassName(backendName);
        try {
            Class<? extends PermissionBackend> backendClass = getBackendClass(backendName);
            WXTLogger.prettyLog(Level.INFO, false, "Initializing " + backendName + " backend");
            Constructor<? extends PermissionBackend> constructor = backendClass.getConstructor(PermissionManager.class, ConfigManager.class, String.class);
            return constructor.newInstance(manager, configManager, getBackendPluginName(backendName));
        } catch (ClassNotFoundException e) {
            WXTLogger.prettyLog(Level.WARNING, false, "Backend \"" + backendName + "\" not found");
            if (fallBackBackend == null) {
                throw new WormholePermissionBackendException("Backend \"" + backendName + "\" not found: " + e.getMessage());
            }
            if (!className.equals(getBackendClassName(fallBackBackend))) {
                return getBackend(fallBackBackend, manager, configManager, null);
            }
            throw new RuntimeException(e);
        } catch (Exception e2) {
            throw new RuntimeException(e2);
        }
    }

    public static List<String> getRegisteredAliases() {
        return new ArrayList(registeredBackendAliases.keySet());
    }

    public static List<Class<? extends PermissionBackend>> getRegisteredClasses() {
        return new ArrayList(registeredBackendAliases.values());
    }

    public boolean has(Player player, String permissionString) {
        return hasPermission(player, permissionString);
    }

    public static void resolvePermissionBackends() {
        // No-op: backends are registered manually via registerBackendAlias
    }

}
