package de.luricos.bukkit.WormholeXTreme.Wormhole.listeners;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class WormholeXTremeServerListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        String pluginName = event.getPlugin().getDescription().getName();
        // Shut down permission manager if Vault disappears
        if (pluginName.equals("Vault") && !ConfigManager.getPermissionsSupportDisable()) {
            if (WormholeXTreme.getPermissionManager() != null) {
                WormholeXTreme.getPermissionManager().end();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
    }
}
