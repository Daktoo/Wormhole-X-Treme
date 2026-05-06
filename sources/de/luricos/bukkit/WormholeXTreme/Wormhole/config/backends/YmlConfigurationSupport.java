package de.luricos.bukkit.WormholeXTreme.Wormhole.config.backends;

import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationOptions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/config/backends/YmlConfigurationSupport.class */
public class YmlConfigurationSupport extends ConfigurationBackend {
    private FileConfiguration fileConfiguration;

    public YmlConfigurationSupport(ConfigurationManager manager) {
        super(manager);
        this.fileConfiguration = (FileConfiguration) this.manager.getConfig();
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public void initialize() {
        this.fileConfiguration.options().copyDefaults(true);
        WormholeXTreme.getPlugin().saveConfig();
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public void reload() {
        end();
        initialize();
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public void end() {
        WormholeXTreme.getPlugin().saveConfig();
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public Set<String> getKeys(boolean deep) {
        return this.fileConfiguration.getKeys(deep);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public Map<String, Object> getValues(boolean deep) {
        return this.fileConfiguration.getValues(deep);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean contains(String path) {
        return this.fileConfiguration.contains(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isSet(String path) {
        return this.fileConfiguration.isSet(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public String getCurrentPath() {
        return this.fileConfiguration.getCurrentPath();
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public String getName() {
        return this.fileConfiguration.getName();
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public Configuration getRoot() {
        return this.fileConfiguration.getRoot();
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public ConfigurationSection getParent() {
        return this.fileConfiguration.getParent();
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public Object get(String path) {
        return this.fileConfiguration.get(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public Object get(String path, Object def) {
        return this.fileConfiguration.get(path, def);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public void set(String path, Object value) {
        this.fileConfiguration.set(path, value);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public ConfigurationSection createSection(String path) {
        return this.fileConfiguration.createSection(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public ConfigurationSection createSection(String path, Map<?, ?> map) {
        return this.fileConfiguration.createSection(path, map);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public String getString(String path) {
        return this.fileConfiguration.getString(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public String getString(String path, String def) {
        return this.fileConfiguration.getString(path, def);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isString(String path) {
        return this.fileConfiguration.isString(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public int getInt(String path) {
        return this.fileConfiguration.getInt(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public int getInt(String path, int def) {
        return this.fileConfiguration.getInt(path, def);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isInt(String path) {
        return this.fileConfiguration.isInt(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean getBoolean(String path) {
        return this.fileConfiguration.getBoolean(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean getBoolean(String path, boolean def) {
        return this.fileConfiguration.getBoolean(path, def);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isBoolean(String path) {
        return this.fileConfiguration.isBoolean(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public double getDouble(String path) {
        return this.fileConfiguration.getDouble(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public double getDouble(String path, double def) {
        return this.fileConfiguration.getDouble(path, def);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isDouble(String path) {
        return this.fileConfiguration.isDouble(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public long getLong(String path) {
        return this.fileConfiguration.getLong(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public long getLong(String path, long def) {
        return this.fileConfiguration.getLong(path, def);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isLong(String path) {
        return this.fileConfiguration.isLong(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<?> getList(String path) {
        return this.fileConfiguration.getList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<?> getList(String path, List<?> def) {
        return this.fileConfiguration.getList(path, def);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isList(String path) {
        return this.fileConfiguration.isList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<String> getStringList(String path) {
        return this.fileConfiguration.getStringList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<Integer> getIntegerList(String path) {
        return this.fileConfiguration.getIntegerList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<Boolean> getBooleanList(String path) {
        return this.fileConfiguration.getBooleanList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<Double> getDoubleList(String path) {
        return this.fileConfiguration.getDoubleList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<Float> getFloatList(String path) {
        return this.fileConfiguration.getFloatList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<Long> getLongList(String path) {
        return this.fileConfiguration.getLongList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<Byte> getByteList(String path) {
        return this.fileConfiguration.getByteList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<Character> getCharacterList(String path) {
        return this.fileConfiguration.getCharacterList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<Short> getShortList(String path) {
        return this.fileConfiguration.getShortList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public List<Map<?, ?>> getMapList(String path) {
        return this.fileConfiguration.getMapList(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public Vector getVector(String path) {
        return this.fileConfiguration.getVector(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public Vector getVector(String path, Vector def) {
        return this.fileConfiguration.getVector(path, def);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isVector(String path) {
        return this.fileConfiguration.isVector(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public OfflinePlayer getOfflinePlayer(String path) {
        return this.fileConfiguration.getOfflinePlayer(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public OfflinePlayer getOfflinePlayer(String path, OfflinePlayer def) {
        return this.fileConfiguration.getOfflinePlayer(path, def);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isOfflinePlayer(String path) {
        return this.fileConfiguration.isOfflinePlayer(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public ItemStack getItemStack(String path) {
        return this.fileConfiguration.getItemStack(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public ItemStack getItemStack(String path, ItemStack def) {
        return this.fileConfiguration.getItemStack(path, def);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isItemStack(String path) {
        return this.fileConfiguration.isItemStack(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public Color getColor(String path) {
        return this.fileConfiguration.getColor(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public Color getColor(String path, Color def) {
        return this.fileConfiguration.getColor(path, def);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isColor(String path) {
        return this.fileConfiguration.isColor(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public ConfigurationSection getConfigurationSection(String path) {
        return this.fileConfiguration.getConfigurationSection(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public boolean isConfigurationSection(String path) {
        return this.fileConfiguration.isConfigurationSection(path);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public ConfigurationSection getDefaultSection() {
        return this.fileConfiguration.getDefaultSection();
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public void addDefault(String path, Object value) {
        this.fileConfiguration.addDefault(path, value);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public void addDefaults(Map<String, Object> defaults) {
        this.fileConfiguration.addDefaults(defaults);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public void addDefaults(Configuration defaults) {
        this.fileConfiguration.addDefaults(defaults);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public void setDefaults(Configuration defaults) {
        this.fileConfiguration.setDefaults(defaults);
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public Configuration getDefaults() {
        return this.fileConfiguration.getDefaults();
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigurationBackend
    public ConfigurationOptions options() {
        return this.fileConfiguration.options();
    }
}
