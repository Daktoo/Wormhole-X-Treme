package de.luricos.bukkit.WormholeXTreme.Wormhole.events;

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import org.bukkit.event.HandlerList;

/**
 * Fired once a stargate has become real and is in the manager's gate list.
 *
 * This is a notification, not a veto: the gate already exists by the time
 * listeners see it, so the event is not cancellable. Anything that wants to
 * refuse a build should do so earlier, in the completion path.
 */
public class StargateCreatedEvent extends WormholeEvent {

    /** Why the gate appeared, so listeners can tell a fresh build from a restart. */
    public enum Cause {
        /** A player finished building it, via sign or /wxcomplete. */
        BUILT,
        /** It was read back out of the database on plugin enable. */
        LOADED,
        /** It arrived through /wxconvertdb or another bulk import. */
        IMPORTED
    }

    private static final HandlerList handlers = new HandlerList();

    private final Stargate stargate;
    private final Cause cause;

    public StargateCreatedEvent(Stargate stargate, Cause cause) {
        super("StargateCreatedEvent");
        this.stargate = stargate;
        this.cause = cause == null ? Cause.BUILT : cause;
    }

    public Stargate getStargate() {
        return this.stargate;
    }

    public Cause getCause() {
        return this.cause;
    }

    public String getGateName() {
        return this.stargate == null ? null : this.stargate.getGateName();
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
