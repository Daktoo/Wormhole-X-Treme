package de.luricos.bukkit.WormholeXTreme.Wormhole.listeners;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.shape.ShapeBuilderManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.shape.ShapeBuilderSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Feeds ordinary chat into the /wxshape wizard while a player is answering one
 * of its questions, so they can type a name or a size rather than remembering
 * a command for each step.
 *
 * Chat fires off the main thread, so the input is handed back to the scheduler
 * before it touches any session state.
 */
public class WormholeXTremeShapeListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        ShapeBuilderSession session = ShapeBuilderManager.getSession(player);
        if (session == null) {
            return;
        }
        // Only swallow chat during the stages that actually want typed input.
        // The grid is click driven, so the player can still talk while building.
        if (!wantsTypedInput(session)) {
            return;
        }
        final String message = event.getMessage();
        event.setCancelled(true);
        WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new Runnable() {
            @Override
            public void run() {
                ShapeBuilderManager.handleInput(player, message);
            }
        });
    }

    /**
     * Sessions are in-memory only, so a player who logs out mid-build loses the
     * work rather than leaving a stale session behind.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        ShapeBuilderManager.endSession(event.getPlayer());
    }

    private static boolean wantsTypedInput(ShapeBuilderSession session) {
        switch (session.getStage()) {
            case NAME:
            case DIMENSIONS:
            case WOOSH_TICKS:
            case LIGHT_TICKS:
            case PORTAL_MATERIAL:
            case IRIS_MATERIAL:
            case STRUCTURE_MATERIAL:
            case ACTIVE_MATERIAL:
                return true;
            default:
                return false;
        }
    }
}
