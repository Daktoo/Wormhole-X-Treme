package de.luricos.bukkit.WormholeXTreme.Wormhole.events;

import org.bukkit.event.HandlerList;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/events/WormholeSystemEvent.class */
public class WormholeSystemEvent extends WormholeEvent {
    protected Action action;
    private static final HandlerList handlers = new HandlerList();

    /* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/events/WormholeSystemEvent$Action.class */
    public enum Action {
        PERMISSION_BACKEND_CHANGED,
        RELOADED
    }

    public WormholeSystemEvent(Action action) {
        super(action.toString());
        this.action = action;
    }

    public Action getAction() {
        return this.action;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
