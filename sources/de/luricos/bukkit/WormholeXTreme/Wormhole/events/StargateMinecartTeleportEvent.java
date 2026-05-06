package de.luricos.bukkit.WormholeXTreme.Wormhole.events;

import org.bukkit.entity.Minecart;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/events/StargateMinecartTeleportEvent.class */
public class StargateMinecartTeleportEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Minecart oldMinecart;
    private Minecart newMinecart;

    public StargateMinecartTeleportEvent(Minecart oldMinecart, Minecart newMinecart) {
        this.oldMinecart = oldMinecart;
        this.newMinecart = newMinecart;
    }

    public Minecart getNewMinecart() {
        return this.newMinecart;
    }

    public Minecart getOldMinecart() {
        return this.oldMinecart;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
