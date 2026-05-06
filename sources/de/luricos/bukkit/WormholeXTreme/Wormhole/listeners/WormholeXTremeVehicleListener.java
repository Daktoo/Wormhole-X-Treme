package de.luricos.bukkit.WormholeXTreme.Wormhole.listeners;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.events.StargateMinecartTeleportEvent;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.StargateRestrictions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/listeners/WormholeXTremeVehicleListener.class */
public class WormholeXTremeVehicleListener implements Listener {
    private static final Vector nospeed = new Vector();

    private static boolean handleStargateMinecartTeleportEvent(VehicleMoveEvent event) {
        String gatenetwork;
        Location l = event.getTo();
        Block ch = l.getWorld().getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
        Stargate st = StargateManager.getGateFromBlock(ch);
        if (st == null || !st.isGateActive() || st.getGateTarget() == null) {
            return false;
        }
        if (ch.getType() == (st.isGateCustom() ? st.getGateCustomPortalMaterial() : st.getGateShape() != null ? st.getGateShape().getShapePortalMaterial() : Material.WATER)) {
            if (st.getGateNetwork() != null) {
                gatenetwork = st.getGateNetwork().getNetworkName();
            } else {
                gatenetwork = "Public";
            }
            Location target = st.getGateTarget().getGateMinecartTeleportLocation() != null ? st.getGateTarget().getGateMinecartTeleportLocation() : st.getGateTarget().getGatePlayerTeleportLocation();
            Minecart veh = (Minecart) event.getVehicle();
            Vector v = veh.getVelocity();
            veh.setVelocity(nospeed);
            final Player passenger = (veh.getPassenger() instanceof Player) ? (Player) veh.getPassenger() : null;
            if (passenger != null && (passenger instanceof Player)) {
                Player p = passenger;
                WXTLogger.prettyLog(Level.FINE, false, "Minecart Player in gate:" + st.getGateName() + " gate Active: " + st.isGateActive() + " Target Gate: " + st.getGateTarget().getGateName() + " Network: " + gatenetwork);
                if (ConfigManager.getWormholeUseIsTeleport() && ((st.isGateSignPowered() && !WXPermissions.checkPermission(p, st, WXPermissions.PermissionType.SIGN)) || (!st.isGateSignPowered() && !WXPermissions.checkPermission(p, st, WXPermissions.PermissionType.DIALER)))) {
                    p.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
                    return false;
                }
                if (st.getGateTarget().isGateIrisActive()) {
                    p.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Remote Iris is locked!");
                    veh.teleport(st.getGateMinecartTeleportLocation() != null ? st.getGateMinecartTeleportLocation() : st.getGatePlayerTeleportLocation());
                    if (ConfigManager.getTimeoutShutdown() == 0) {
                        st.shutdownStargate(true);
                        return false;
                    }
                    return false;
                }
                if (ConfigManager.isUseCooldownEnabled()) {
                    if (StargateRestrictions.isPlayerUseCooldown(p)) {
                        p.sendMessage(ConfigManager.MessageStrings.playerUseCooldownRestricted.toString());
                        p.sendMessage(ConfigManager.MessageStrings.playerUseCooldownWaitTime.toString() + StargateRestrictions.checkPlayerUseCooldownRemaining(p));
                        return false;
                    }
                    StargateRestrictions.addPlayerUseCooldown(p);
                }
            } else if (st.getGateTarget().isGateIrisActive()) {
                WXTLogger.prettyLog(Level.FINE, false, "Minecart in gate:" + st.getGateName() + " gate Active: " + st.isGateActive() + " Target Gate: " + st.getGateTarget().getGateName() + " Network: " + gatenetwork);
                veh.teleport(st.getGateMinecartTeleportLocation() != null ? st.getGateMinecartTeleportLocation() : st.getGatePlayerTeleportLocation());
                if (ConfigManager.getTimeoutShutdown() == 0) {
                    st.shutdownStargate(true);
                    return false;
                }
                return false;
            }
            double speed = v.length();
            final Vector new_speed = new Vector();
            switch (AnonymousClass2.$SwitchMap$org$bukkit$block$BlockFace[st.getGateTarget().getGateFacing().ordinal()]) {
                case 1:
                    new_speed.setX(-1);
                    break;
                case 2:
                    new_speed.setX(1);
                    break;
                case 3:
                    new_speed.setZ(-1);
                    break;
                case 4:
                    new_speed.setZ(1);
                    break;
            }
            new_speed.multiply(speed * 5.0d);
            if (st.getGateTarget().isGateIrisActive()) {
                veh.teleport(st.getGateMinecartTeleportLocation() != null ? st.getGateMinecartTeleportLocation() : st.getGatePlayerTeleportLocation());
                veh.setVelocity(new_speed);
            } else if (passenger != null) {
                WXTLogger.prettyLog(Level.FINE, false, "Removing player from cart and doing some teleport hackery");
                veh.eject();
                veh.remove();
                final Minecart newveh = target.getWorld().spawn(target, Minecart.class);
                Event teleportevent = new StargateMinecartTeleportEvent(veh, newveh);
                WormholeXTreme.getThisPlugin().getServer().getPluginManager().callEvent(teleportevent);
                passenger.teleport(target);
                WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new Runnable() { // from class: de.luricos.bukkit.WormholeXTreme.Wormhole.listeners.WormholeXTremeVehicleListener.1
                    @Override // java.lang.Runnable
                    public void run() {
                        newveh.setPassenger(passenger);
                        newveh.setVelocity(new_speed);
                        newveh.setFireTicks(0);
                    }
                }, 5L);
            } else {
                veh.teleport(target);
                veh.setVelocity(new_speed);
            }
            if (ConfigManager.getTimeoutShutdown() == 0) {
                st.shutdownStargate(true);
                return true;
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: renamed from: de.luricos.bukkit.WormholeXTreme.Wormhole.listeners.WormholeXTremeVehicleListener$2, reason: invalid class name */
    /* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/listeners/WormholeXTremeVehicleListener$2.class */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$org$bukkit$block$BlockFace = new int[BlockFace.values().length];

        static {
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.NORTH.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.SOUTH.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.EAST.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.WEST.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (event.getVehicle() instanceof Minecart) {
            handleStargateMinecartTeleportEvent(event);
        }
    }
}
