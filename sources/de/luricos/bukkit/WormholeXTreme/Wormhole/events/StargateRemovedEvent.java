package de.luricos.bukkit.WormholeXTreme.Wormhole.events;

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import org.bukkit.event.HandlerList;

/**
 * Fired once a stargate has been taken out of the manager's gate list.
 *
 * Like its counterpart this is a notification rather than a veto. By the time
 * listeners run the gate is already gone from the index and the database, so
 * the Stargate object handed over should be treated as read-only and used
 * mainly for its name, owner, network and last known location.
 */
public class StargateRemovedEvent extends WormholeEvent {

    private static final HandlerList handlers = new HandlerList();

    private final Stargate stargate;
    private final String gateName;

    public StargateRemovedEvent(Stargate stargate, String gateName) {
        super("StargateRemovedEvent");
        this.stargate = stargate;
        this.gateName = gateName;
    }

    public Stargate getStargate() {
        return this.stargate;
    }

    /**
     * The gate's name as it was at removal time, captured separately because
     * teardown can clear fields on the Stargate itself.
     */
    public String getGateName() {
        return this.gateName;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
