package de.luricos.bukkit.WormholeXTreme.Wormhole.config;

import de.luricos.bukkit.WormholeXTreme.Wormhole.events.WormholeSystemEvent;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/config/ConfigurationManager.class */
public class ConfigurationManager {
    protected ConfigurationBackend backend = null;
    private org.bukkit.configuration.Configuration config;

    public ConfigurationManager(org.bukkit.configuration.Configuration config) {
        this.config = config;
        initBackend();
    }

    private void initBackend() {
        String backendName = this.config.getString("configuration.backend");
        if (backendName == null || backendName.isEmpty()) {
            backendName = ConfigurationBackend.defaultBackend;
            this.config.set("configuration.backend", backendName);
        }
        setBackend(backendName);
    }

    public ConfigurationBackend getBackend() {
        return this.backend;
    }

    public String getBackendName() {
        return this.backend.getName();
    }

    public void setBackend(String backendName) {
        synchronized (this) {
            this.backend = ConfigurationBackend.getBackend(backendName, this.config);
            this.backend.initialize();
        }
        callEvent(WormholeSystemEvent.Action.PERMISSION_BACKEND_CHANGED);
    }

    public ConfigurationSection getConfig() {
        return this.config;
    }

    protected void callEvent(WormholeSystemEvent event) {
        Bukkit.getServer().getPluginManager().callEvent(event);
    }

    protected void callEvent(WormholeSystemEvent.Action action) {
        callEvent(new WormholeSystemEvent(action));
    }

    public void reset() {
        if (this.backend != null) {
            this.backend.reload();
        }
        callEvent(WormholeSystemEvent.Action.RELOADED);
    }

    public void end() {
        if (this.backend != null) {
            this.backend.end();
        }
    }

    public Level getLogLevel() {
        return Level.parse(this.config.getString("logger.level", "INFO"));
    }
}
