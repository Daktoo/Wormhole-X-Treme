package de.luricos.bukkit.WormholeXTreme.Wormhole.config;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionsManager;
import java.util.logging.Level;
import org.bukkit.Material;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/config/Setting.class */
public class Setting {
    private final ConfigManager.ConfigKeys name;
    private final String desc;
    private Object value;
    private final String plugin;

    protected Setting(ConfigManager.ConfigKeys name, Object value, String desc, String plugin) {
        this.name = name;
        this.desc = desc;
        this.value = value;
        this.plugin = plugin;
    }

    public boolean getBooleanValue() {
        return ((Boolean) this.value).booleanValue();
    }

    public String getDescription() {
        return this.desc;
    }

    public double getDoubleValue() {
        return ((Double) this.value).doubleValue();
    }

    public int getIntValue() {
        return ((Integer) this.value).intValue();
    }

    public Level getLevel() {
        return Level.parse((String) this.value);
    }

    public Material getMaterialValue() {
        return (Material) this.value;
    }

    public ConfigManager.ConfigKeys getName() {
        return this.name;
    }

    public PermissionsManager.PermissionLevel getPermissionLevel() {
        return (PermissionsManager.PermissionLevel) this.value;
    }

    public String getPluginName() {
        return this.plugin;
    }

    public String getStringValue() {
        return (String) this.value;
    }

    public Object getValue() {
        return this.value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
