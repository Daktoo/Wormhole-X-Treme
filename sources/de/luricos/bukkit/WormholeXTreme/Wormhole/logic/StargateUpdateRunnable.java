package de.luricos.bukkit.WormholeXTreme.Wormhole.logic;

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.StargateRestrictions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayerManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/logic/StargateUpdateRunnable.class */
public class StargateUpdateRunnable implements Runnable {
    private final Stargate stargate;
    private final ActionToTake action;
    private Action eventBlockAction;

    /* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/logic/StargateUpdateRunnable$ActionToTake.class */
    public enum ActionToTake {
        SHUTDOWN,
        ANIMATE_WOOSH,
        DEACTIVATE,
        AFTERSHUTDOWN,
        DIAL_SIGN_CLICK,
        LIGHTUP,
        COOLDOWN_REMOVE,
        DIAL_SIGN_RESET,
        ESTABLISH_WORMHOLE
    }

    public StargateUpdateRunnable(Stargate stargate, ActionToTake action) {
        this(stargate, action, null);
    }

    public StargateUpdateRunnable(Stargate stargate, ActionToTake action, Action eventBlockAction) {
        this.stargate = stargate;
        this.action = action;
        this.eventBlockAction = eventBlockAction;
    }

    private void runLogger(ActionToTake action) {
        switch (action) {
            case ESTABLISH_WORMHOLE:
            case ANIMATE_WOOSH:
            case LIGHTUP:
                WXTLogger.prettyLog(Level.FINER, false, "Run Action \"" + action.toString() + (this.stargate != null ? "\" Stargate \"" + this.stargate.getGateName() : "") + "\"");
                break;
            default:
                WXTLogger.prettyLog(Level.FINE, false, "Run Action \"" + action.toString() + ", ActionType: " + (this.eventBlockAction != null ? this.eventBlockAction.toString() : "NULL") + (this.stargate != null ? "\" Stargate \"" + this.stargate.getGateName() : "") + "\"");
                break;
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        runLogger(this.action);
        Player player = null;
        if (WormholePlayerManager.getRegisteredWormholePlayer(this.stargate.getLastUsedBy()) != null) {
            player = WormholePlayerManager.getRegisteredWormholePlayer(this.stargate.getLastUsedBy()).getPlayer();
        }
        switch (this.action) {
            case ESTABLISH_WORMHOLE:
                this.stargate.establishWormhole();
                break;
            case ANIMATE_WOOSH:
                this.stargate.animateOpening();
                break;
            case LIGHTUP:
                this.stargate.lightStargate(true);
                break;
            case SHUTDOWN:
                this.stargate.shutdownStargate(true);
                break;
            case DEACTIVATE:
                this.stargate.timeoutStargate();
                break;
            case AFTERSHUTDOWN:
                this.stargate.stopAfterShutdownTimer();
                break;
            case DIAL_SIGN_CLICK:
                this.stargate.dialSignClicked(this.eventBlockAction);
                if (player != null && this.stargate.getGateDialSignTarget() == null) {
                    player.sendMessage("No available target to set dialer to.");
                    break;
                }
                break;
            case DIAL_SIGN_RESET:
                this.stargate.resetSign(true);
                break;
            case COOLDOWN_REMOVE:
                StargateRestrictions.removePlayerUseCooldown(player);
                break;
        }
    }
}
