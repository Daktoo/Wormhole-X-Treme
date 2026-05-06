package de.luricos.bukkit.WormholeXTreme.Wormhole.listeners;

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/listeners/WormholeXTremeRedstoneListener.class */
public class WormholeXTremeRedstoneListener implements Listener {
    private static boolean isCurrentNew(int oldCurrent, int newCurrent) {
        return (oldCurrent == 0 && newCurrent > 0) || (oldCurrent > 0 && newCurrent == 0);
    }

    private static boolean isCurrentOn(int oldCurrent, int newCurrent) {
        return newCurrent > 0 && oldCurrent == 0;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if (StargateManager.isBlockInGate(block)) {
            WXTLogger.prettyLog(Level.FINEST, false, "Caught redstone event on block: " + block.toString() + " oldCurrent: " + event.getOldCurrent() + " newCurrent: " + event.getNewCurrent());
            Stargate stargate = StargateManager.getGateFromBlock(event.getBlock());
            if (stargate.isGateSignPowered() && stargate.isGateRedstonePowered() && block.getType().equals(Material.REDSTONE_WIRE) && isCurrentNew(event.getOldCurrent(), event.getNewCurrent()) && !stargate.isGateActive()) {
                if (stargate.getGateRedstoneSignActivationBlock() != null && block.equals(stargate.getGateRedstoneSignActivationBlock()) && isCurrentOn(event.getOldCurrent(), event.getNewCurrent())) {
                    stargate.tryClickTeleportSign(stargate.getGateDialSignBlock(), Action.PHYSICAL);
                    WXTLogger.prettyLog(Level.FINE, false, "Caught redstone sign event on gate: " + stargate.getGateName() + " block: " + block.toString());
                    return;
                }
                if (stargate.getGateRedstoneDialActivationBlock() != null && block.equals(stargate.getGateRedstoneDialActivationBlock()) && isCurrentOn(event.getOldCurrent(), event.getNewCurrent())) {
                    if (stargate.isGateActive() && stargate.getGateTarget() != null) {
                        stargate.shutdownStargate(true);
                        WXTLogger.prettyLog(Level.FINE, false, "Caught redstone shutdown event on gate: " + stargate.getGateName() + " block: " + block.toString());
                    }
                    if (!stargate.isGateActive() && stargate.getGateDialSignTarget() != null && !stargate.isGateRecentlyActive()) {
                        stargate.dialStargate(stargate.getGateDialSignTarget(), false);
                        WXTLogger.prettyLog(Level.FINE, false, "Caught redstone dial event on gate: " + stargate.getGateName() + " block: " + block.toString());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockFromToEvent(BlockFromToEvent event) {
        WXTLogger.prettyLog(Level.FINE, false, "We got a BlockFromToEvent here: " + event.getToBlock());
    }
}
