package de.luricos.bukkit.WormholeXTreme.Wormhole.listeners;

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WorldUtils;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/listeners/WormholeXTremeBlockListener.class */
public class WormholeXTremeBlockListener implements Listener {
    private static boolean handleBlockBreak(Player player, Stargate stargate, Block block) {
        boolean allowed = WXPermissions.checkPermission(player, stargate, WXPermissions.PermissionType.DAMAGE);
        if (allowed) {
            if (!WorldUtils.isSameBlock(stargate.getGateDialLeverBlock(), block)) {
                if (stargate.getGateDialSignBlock() != null && WorldUtils.isSameBlock(stargate.getGateDialSignBlock(), block)) {
                    player.sendMessage("Destroyed DHD Sign. You will be unable to change dialing target from this gate.");
                    player.sendMessage("You can rebuild it later.");
                    stargate.setGateDialSign(null);
                    return false;
                }
                if (block.getType() == (stargate.isGateCustom() ? stargate.getGateCustomIrisMaterial() : stargate.getGateShape() != null ? stargate.getGateShape().getShapeIrisMaterial() : Material.STONE)) {
                    return true;
                }
                if (stargate.isGateActive()) {
                    stargate.setGateActive(false);
                    stargate.fillGateInterior(Material.AIR);
                }
                if (stargate.isGateLightsActive()) {
                    stargate.lightStargate(false);
                    stargate.stopActivationTimer();
                    StargateManager.removeActivatedStargate(stargate.getGateName());
                }
                stargate.resetTeleportSign();
                stargate.setupGateSign(false);
                if (!stargate.getGateIrisDeactivationCode().equals("")) {
                    stargate.setupIrisLever(false);
                }
                if (stargate.isGateRedstonePowered()) {
                    stargate.setupRedstone(false);
                }
                StargateManager.removeStargate(stargate);
                player.sendMessage("Stargate Destroyed: " + stargate.getGateName());
                return false;
            }
            player.sendMessage("Destroyed DHD. You will be unable to dial out from this gate.");
            player.sendMessage("You can rebuild it later.");
            return false;
        }
        if (player != null) {
            WXTLogger.prettyLog(Level.FINE, false, "Player: " + player.getName() + " denied block destroy on: " + stargate.getGateName());
            return true;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isCancelled()) {
            Block block = event.getBlock();
            Stargate stargate = StargateManager.getGateFromBlock(block);
            Player player = event.getPlayer();
            if (stargate != null && handleBlockBreak(player, stargate, block)) {
                event.setCancelled(true);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:36:0x00a1  */
        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.NORMAL)
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent r7) {
        if (!r7.isCancelled() && StargateManager.isBlockInGate(r7.getBlock())) {
            r7.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockDamage(BlockDamageEvent event) {
        if (!event.isCancelled()) {
            Stargate stargate = StargateManager.getGateFromBlock(event.getBlock());
            Player player = event.getPlayer();
            if (stargate != null && player != null && !WXPermissions.checkPermission(player, stargate, WXPermissions.PermissionType.DAMAGE)) {
                event.setCancelled(true);
                WXTLogger.prettyLog(Level.FINE, false, "Player: " + player.getName() + " denied damage on: " + stargate.getGateName());
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!event.isCancelled()) {
            if (StargateManager.isBlockInGate(event.getToBlock()) || StargateManager.isBlockInGate(event.getBlock())) {
                event.setCancelled(true);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:33:0x0096  */
        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.NORMAL)
    public void onBlockIgnite(org.bukkit.event.block.BlockIgniteEvent r7) {
        if (!r7.isCancelled() && StargateManager.isBlockInGate(r7.getBlock())) {
            r7.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (!event.isCancelled()) {
            Block block = event.getBlock();
            if (StargateManager.isBlockInGate(block) && !block.getType().equals(Material.REDSTONE_WIRE)) {
                event.setCancelled(true);
            }
        }
    }
}
