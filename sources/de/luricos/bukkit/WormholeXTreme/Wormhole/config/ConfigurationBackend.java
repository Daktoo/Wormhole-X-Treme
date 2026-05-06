package de.luricos.bukkit.WormholeXTreme.Wormhole.config;

import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholePermissionBackendException;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationOptions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/config/ConfigurationBackend.class */
public abstract class ConfigurationBackend {
    public static final String defaultBackend = "xml";
    private static final Map<String, Class<? extends ConfigurationBackend>> REGISTERED_BACKENDS = new HashMap();
    protected ConfigurationManager manager;

    public abstract void initialize();

    public abstract void reload();

    public abstract void end();

    public abstract Set<String> getKeys(boolean z);

    public abstract Map<String, Object> getValues(boolean z);

    public abstract boolean contains(String str);

    public abstract boolean isSet(String str);

    public abstract String getCurrentPath();

    public abstract String getName();

    public abstract org.bukkit.configuration.Configuration getRoot();

    public abstract ConfigurationSection getParent();

    public abstract Object get(String str);

    public abstract Object get(String str, Object obj);

    public abstract void set(String str, Object obj);

    public abstract ConfigurationSection createSection(String str);

    public abstract ConfigurationSection createSection(String str, Map<?, ?> map);

    public abstract String getString(String str);

    public abstract String getString(String str, String str2);

    public abstract boolean isString(String str);

    public abstract int getInt(String str);

    public abstract int getInt(String str, int i);

    public abstract boolean isInt(String str);

    public abstract boolean getBoolean(String str);

    public abstract boolean getBoolean(String str, boolean z);

    public abstract boolean isBoolean(String str);

    public abstract double getDouble(String str);

    public abstract double getDouble(String str, double d);

    public abstract boolean isDouble(String str);

    public abstract long getLong(String str);

    public abstract long getLong(String str, long j);

    public abstract boolean isLong(String str);

    public abstract List<?> getList(String str);

    public abstract List<?> getList(String str, List<?> list);

    public abstract boolean isList(String str);

    public abstract List<String> getStringList(String str);

    public abstract List<Integer> getIntegerList(String str);

    public abstract List<Boolean> getBooleanList(String str);

    public abstract List<Double> getDoubleList(String str);

    public abstract List<Float> getFloatList(String str);

    public abstract List<Long> getLongList(String str);

    public abstract List<Byte> getByteList(String str);

    public abstract List<Character> getCharacterList(String str);

    public abstract List<Short> getShortList(String str);

    public abstract List<Map<?, ?>> getMapList(String str);

    public abstract Vector getVector(String str);

    public abstract Vector getVector(String str, Vector vector);

    public abstract boolean isVector(String str);

    public abstract OfflinePlayer getOfflinePlayer(String str);

    public abstract OfflinePlayer getOfflinePlayer(String str, OfflinePlayer offlinePlayer);

    public abstract boolean isOfflinePlayer(String str);

    public abstract ItemStack getItemStack(String str);

    public abstract ItemStack getItemStack(String str, ItemStack itemStack);

    public abstract boolean isItemStack(String str);

    public abstract Color getColor(String str);

    public abstract Color getColor(String str, Color color);

    public abstract boolean isColor(String str);

    public abstract ConfigurationSection getConfigurationSection(String str);

    public abstract boolean isConfigurationSection(String str);

    public abstract ConfigurationSection getDefaultSection();

    public abstract void addDefault(String str, Object obj);

    public abstract void addDefaults(Map<String, Object> map);

    public abstract void addDefaults(org.bukkit.configuration.Configuration configuration);

    public abstract void setDefaults(org.bukkit.configuration.Configuration configuration);

    public abstract org.bukkit.configuration.Configuration getDefaults();

    public abstract ConfigurationOptions options();

    protected ConfigurationBackend(ConfigurationManager manager) {
        this.manager = manager;
    }

    public static String getBackendClassName(String alias) {
        if (REGISTERED_BACKENDS.containsKey(alias)) {
            return REGISTERED_BACKENDS.get(alias).getName();
        }
        return alias;
    }

    public static Class<? extends ConfigurationBackend> getBackendClass(String alias) throws ClassNotFoundException {
        if (!REGISTERED_BACKENDS.containsKey(alias)) {
            Class<?> clazz = Class.forName(alias);
            if (!ConfigurationBackend.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Provided class " + alias + " is not a subclass of ConfigurationBackend!");
            }
            return clazz.asSubclass(ConfigurationBackend.class);
        }
        return REGISTERED_BACKENDS.get(alias);
    }

    public static void registerBackendAlias(String alias, Class<? extends ConfigurationBackend> backendClass) {
        if (!ConfigurationBackend.class.isAssignableFrom(backendClass)) {
            throw new IllegalArgumentException("Provided class should be subclass of ConfigurationBackend");
        }
        REGISTERED_BACKENDS.put(alias, backendClass);
        WXTLogger.info(String.format("ConfigurationBackend: '%s' registered!", alias));
    }

    public static String getBackendAlias(Class<? extends ConfigurationBackend> backendClass) {
        if (REGISTERED_BACKENDS.containsValue(backendClass)) {
            for (String alias : REGISTERED_BACKENDS.keySet()) {
                if (REGISTERED_BACKENDS.get(alias).equals(backendClass)) {
                    return alias;
                }
            }
        }
        return backendClass.getName();
    }

    public static ConfigurationBackend getBackend(String backendName, org.bukkit.configuration.Configuration config) throws WormholePermissionBackendException {
        return getBackend(backendName, WormholeXTreme.getPermissionManager(), config, defaultBackend);
    }

    public static ConfigurationBackend getBackend(String backendName, PermissionManager manager, ConfigurationSection config, String fallBackBackend) throws WormholePermissionBackendException {
        if (backendName == null || backendName.isEmpty()) {
            backendName = defaultBackend;
        }
        String className = getBackendClassName(backendName);
        try {
            Class<? extends ConfigurationBackend> backendClass = getBackendClass(backendName);
            WXTLogger.info("Initializing " + backendName + " backend");
            Constructor<? extends ConfigurationBackend> constructor = backendClass.getConstructor(PermissionManager.class, ConfigurationSection.class);
            return constructor.newInstance(manager, config);
        } catch (ClassNotFoundException e) {
            WXTLogger.warn("Backend \"" + backendName + "\" is unknown.");
            if (fallBackBackend == null) {
                throw new RuntimeException(e);
            }
            if (!className.equals(getBackendClassName(fallBackBackend))) {
                return getBackend(fallBackBackend, manager, config, null);
            }
            throw new RuntimeException(e);
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = e.getCause();
                if (e instanceof WormholePermissionBackendException) {
                    throw ((WormholePermissionBackendException) e);
                }
            }
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        return getClass().getSimpleName() + "{config=" + getConfig().getName() + "}";
    }

    protected final ConfigurationSection getConfig() {
        return this.manager.getConfig();
    }
}
