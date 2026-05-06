package de.luricos.bukkit.WormholeXTreme.Wormhole.listeners;

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/listeners/WormholeXTremeEntityListener.class */
public class WormholeXTremeEntityListener implements Listener {
    private static boolean handleEntityExplodeEvent(List<Block> explodeBlocks) {
        for (Block explodeBlock : explodeBlocks) {
            if (StargateManager.isBlockInGate(explodeBlock)) {
                Stargate s = StargateManager.getGateFromBlock(explodeBlock);
                WXTLogger.prettyLog(Level.FINE, false, "Blocked Creeper Explosion on Stargate: \"" + s.getGateName() + "\"");
                return true;
            }
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:45:0x00da  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static boolean handlePlayerDamageEvent(org.bukkit.event.entity.EntityDamageEvent r6) {
        /*
            Method dump skipped, instruction units count: 311
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: de.luricos.bukkit.WormholeXTreme.Wormhole.listeners.WormholeXTremeEntityListener.handlePlayerDamageEvent(org.bukkit.event.entity.EntityDamageEvent):boolean");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!event.isCancelled()) {
            if ((event.getCause().equals(EntityDamageEvent.DamageCause.FIRE) || event.getCause().equals(EntityDamageEvent.DamageCause.FIRE_TICK) || event.getCause().equals(EntityDamageEvent.DamageCause.LAVA)) && (event.getEntity() instanceof Player) && handlePlayerDamageEvent(event)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!event.isCancelled()) {
            List<Block> explodeBlocks = event.blockList();
            if (handleEntityExplodeEvent(explodeBlocks)) {
                event.setCancelled(true);
            }
        }
    }
}
