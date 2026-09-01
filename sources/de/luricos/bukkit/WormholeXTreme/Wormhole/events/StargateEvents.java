package de.luricos.bukkit.WormholeXTreme.Wormhole.events;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

/**
 * One place to dispatch Wormhole X-Treme's own events from.
 *
 * Gate creation currently only happens on the main thread, but the database
 * loader is the sort of thing that gets moved off it later, and a synchronous
 * event dispatched from an async thread throws. So this checks first and hops
 * back to the main thread if it has to. A misbehaving listener should also
 * never be able to abort a gate build, hence the catch.
 */
public final class StargateEvents {

    private StargateEvents() {
    }

    public static void fire(Event event) {
        if (event == null) {
            return;
        }
        try {
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getPluginManager().callEvent(event);
                return;
            }
            Plugin plugin = WormholeXTreme.getThisPlugin();
            if (plugin != null && plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
            }
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[WormholeXTreme] Listener threw while handling "
                    + event.getEventName() + ": " + t);
        }
    }
}
