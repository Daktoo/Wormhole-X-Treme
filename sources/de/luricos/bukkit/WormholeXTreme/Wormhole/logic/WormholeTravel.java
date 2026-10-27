package de.luricos.bukkit.WormholeXTreme.Wormhole.logic;
import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class WormholeTravel implements Runnable {
    private static final int MOB_CHECK_INTERVAL = 5;
    private static final double LOOKOUT = 5.0d;
    private static int taskId = -1;
    private final Map<UUID, Sighting> lastSeen = new HashMap<>();
    private int tick = 0;
    private WormholeTravel() {
    }
    private static final class Sighting {
        final World world;
        final Vector position;
        final Vector velocity;
        Sighting(World world, Vector position, Vector velocity) {
            this.world = world;
            this.position = position;
            this.velocity = velocity;
        }
    }
    private static final class Horizon {
        final World world;
        final List<BoundingBox> boxes;
        final BoundingBox area;
        final Material material;
        private Horizon(World world, List<BoundingBox> boxes, BoundingBox area, Material material) {
            this.world = world;
            this.boxes = boxes;
            this.area = area;
            this.material = material;
        }
        static Horizon of(Stargate gate) {
            World world = gate.getGateWorld();
            ArrayList<Location> portal = gate.getGatePortalBlocks();
            if (world == null || portal == null || portal.isEmpty()) {
                return null;
            }
            Location first = portal.get(0);
            if (!world.isChunkLoaded(first.getBlockX() >> 4, first.getBlockZ() >> 4)) {
                return null;
            }
            Material material = portalMaterial(gate);
            List<BoundingBox> boxes = new ArrayList<>(portal.size());
            BoundingBox area = null;
            for (Location l : portal) {
                Block block = world.getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
                if (block.getType() != material) {
                    continue;
                }
                BoundingBox box = BoundingBox.of(block);
                boxes.add(box);
                area = (area == null) ? box.clone() : area.union(box);
            }
            return area == null ? null : new Horizon(world, boxes, area, material);
        }
        boolean contains(Vector point) {
            for (BoundingBox box : this.boxes) {
                if (box.contains(point)) {
                    return true;
                }
            }
            return false;
        }
        Vector crossing(Vector from, Vector to) {
            Vector path = to.clone().subtract(from);
            double length = path.length();
            if (length < 1.0E-6d) {
                return null;
            }
            Vector direction = path.multiply(1.0d / length);
            Vector nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (BoundingBox box : this.boxes) {
                RayTraceResult hit = box.rayTrace(from, direction, length);
                if (hit != null) {
                    double distance = hit.getHitPosition().distanceSquared(from);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = hit.getHitPosition();
                    }
                }
            }
            return nearest;
        }
    }
    public static void start() {
        if (taskId != -1) {
            WormholeXTreme.getScheduler().cancelTask(taskId);
        }
        taskId = WormholeXTreme.getScheduler().scheduleSyncRepeatingTask(WormholeXTreme.getThisPlugin(), new WormholeTravel(), 1L, 1L);
        if (taskId == -1) {
            WXTLogger.prettyLog(Level.WARNING, false, "Could not schedule wormhole travel. Mobs, items, and arrows will not use stargates");
        }
    }
    @Override
    public void run() {
        this.tick++;
        boolean mobs = ConfigManager.isMobTravelEnabled() && this.tick % MOB_CHECK_INTERVAL == 0;
        boolean arrows = ConfigManager.isArrowTravelEnabled();
        boolean items = ConfigManager.isItemTravelEnabled();
        if (!arrows && !items) {
            this.lastSeen.clear();
            if (!mobs) {
                return;
            }
        }
        Set<UUID> watched = new HashSet<>();
        Set<UUID> sent = new HashSet<>();
        for (Stargate gate : StargateManager.getAllGates()) {
            if (!gate.isGateActive() || gate.getGateTarget() == null) {
                continue;
            }
            Horizon horizon = Horizon.of(gate);
            if (horizon == null) {
                continue;
            }
            if (mobs) {
                checkMobs(gate, horizon);
            }
            if (arrows || items) {
                checkMovers(gate, horizon, arrows, items, watched, sent);
            }
        }
        this.lastSeen.keySet().retainAll(watched);
    }
    private static void checkMobs(Stargate gate, Horizon horizon) {
        for (Entity entity : horizon.world.getNearbyEntities(horizon.area, e -> e instanceof Mob)) {
            Mob mob = (Mob) entity;
            if (mob.isDead() || !mob.isValid() || mob.isInsideVehicle() || !mob.getPassengers().isEmpty()) {
                continue;
            }
            Block feet = mob.getLocation().getBlock();
            if (StargateManager.getGateFromBlock(feet) != gate || feet.getType() != horizon.material) {
                continue;
            }
            sendMob(mob, gate);
        }
    }
    private static void sendMob(Mob mob, Stargate gate) {
        Stargate target = gate.getGateTarget();
        if (target == null) {
            return;
        }
        if (target.isGateIrisActive()) {
            if (WormholeHazards.killCreatureAtIris(mob, gate)) {
                closeIfSingleUse(gate);
                return;
            }
            Location back = outsideGate(gate);
            if (back != null) {
                mob.teleport(back);
            }
            return;
        }
        Location destination = target.getGatePlayerTeleportLocation();
        if (destination == null) {
            return;
        }
        mob.setFallDistance(0.0f);
        mob.setNoDamageTicks(5);
        if (mob.teleport(destination)) {
            WXTLogger.prettyLog(Level.FINE, false, mob.getType() + " travelled through '" + gate.getGateName() + "' to '" + target.getGateName() + "'.");
            closeIfSingleUse(gate);
        }
    }
    private void checkMovers(Stargate gate, Horizon horizon, boolean arrows, boolean items, Set<UUID> watched, Set<UUID> sent) {
        BoundingBox lookout = horizon.area.clone().expand(LOOKOUT);
        for (Entity entity : horizon.world.getNearbyEntities(lookout, e -> (arrows && e instanceof AbstractArrow) || (items && e instanceof Item))) {
            UUID id = entity.getUniqueId();
            if (sent.contains(id) || entity.isDead() || !entity.isValid() || entity.isInsideVehicle() || !entity.getPassengers().isEmpty()) {
                continue;
            }
            if (entity instanceof AbstractArrow && ((AbstractArrow) entity).isInBlock()) {
                continue;
            }
            Vector now = entity.getBoundingBox().getCenter();
            Vector velocity = entity.getVelocity();
            Sighting before = this.lastSeen.get(id);
            boolean seenHereBefore = before != null && before.world.equals(horizon.world);
            Vector previous = seenHereBefore ? before.position : now.clone().subtract(velocity);
            Vector momentum = seenHereBefore ? before.velocity : velocity;
            Vector crossing = horizon.contains(now) ? now : horizon.crossing(previous, now);
            if (crossing != null && enteringFromFront(gate, momentum)) {
                sent.add(id);
                sendMover(entity, gate, crossing, momentum);
                continue;
            }
            watched.add(id);
            this.lastSeen.put(id, new Sighting(horizon.world, now, velocity));
        }
    }
    private static boolean enteringFromFront(Stargate gate, Vector momentum) {
        Vector front = facingVector(gate.getGateFacing());
        return front == null || momentum.dot(front) <= 0.01d;
    }
        private static void sendMover(Entity entity, Stargate gate, Vector crossing, Vector momentum) {
        Stargate target = gate.getGateTarget();
        if (target == null) {
            return;
        }
        if (target.isGateIrisActive()) {
            if (WormholeHazards.destroyAtIris(entity, gate)) {
                closeIfSingleUse(gate);
                return;
            }
            Location back = outsideGate(gate);
            if (back != null && entity.teleport(back)) {
                entity.setVelocity(new Vector());
            }
            return;
        }
        Vector out = turnThrough(momentum, gate.getGateFacing(), target.getGateFacing());
        Location exit = exitPoint(gate, target, crossing);
        if (exit == null) {
            return;
        }
        if (out.lengthSquared() > 1.0E-6d) {
            exit.setDirection(out);
        }
        boolean otherWorld = exit.getWorld() != null && !exit.getWorld().equals(entity.getWorld());
        if (!entity.teleport(exit)) {
            return;
        }
        entity.setVelocity(out);
        if (otherWorld) {
            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), () -> {
                if (entity.isValid()) {
                    entity.setVelocity(out);
                }
            }, 1L);
        }
        WXTLogger.prettyLog(Level.FINE, false, entity.getType() + " travelled through '" + gate.getGateName() + "' to '" + target.getGateName() + "'.");
        closeIfSingleUse(gate);
    }
    private static Location exitPoint(Stargate gate, Stargate target, Vector crossing) {
        Location targetBase = target.getGatePlayerTeleportLocation();
        if (targetBase == null) {
            return null;
        }
        Vector sourceFront = facingVector(gate.getGateFacing());
        Vector targetFront = facingVector(target.getGateFacing());
        Location sourceBase = gate.getGatePlayerTeleportLocation();

        Vector offset = new Vector(0.0d, 1.0d, 0.0d);
        if (sourceBase != null && sourceFront != null) {
            Vector relative = crossing.clone().subtract(sourceBase.toVector());
            relative.subtract(sourceFront.clone().multiply(relative.dot(sourceFront)));
            offset = turnThrough(relative, gate.getGateFacing(), target.getGateFacing());
        }
        Vector forward = targetFront != null ? targetFront : new Vector();
        Location exit = targetBase.clone().add(offset).add(forward);
        if (!exit.getBlock().isPassable()) {
            exit = targetBase.clone().add(0.0d, 1.0d, 0.0d).add(forward);
        }
        return exit;
    }
    private static Vector turnThrough(Vector vector, BlockFace sourceFacing, BlockFace targetFacing) {
        Vector into = facingVector(sourceFacing);
        Vector outOf = facingVector(targetFacing);
        if (into == null || outOf == null) {
            return vector.clone();
        }
        into.multiply(-1.0);
        double cos = Math.max(-1.0d, Math.min(1.0d, into.dot(outOf)));
        if ( cos > 0.9999d) {
            return vector.clone();
        }
        if (cos < -0.9999d) {
            Vector axis = Math.abs(into.getY()) < 0.9d ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
            return vector.clone().rotateAroundAxis(axis, Math.PI);
        }
        Vector axis = into.getCrossProduct(outOf).normalize();
        return vector.clone().rotateAroundAxis(axis, Math.acos(cos));
    private static Vector facingVector(BlockFace facing) {
        if (facing == null) {
            return null;
        }
        Vector axis = into.getCrossProduct(outOf).normalize();
        return vector.clone().rotateAroundAxis(axis, Math.acos(cos));
    }
    private static Vector facingVector(BlockFace facing) {
        if (ConfigManager.getTimeoutShutdown() == 0) {
            gate.shutdownStargate(true);
        }
    }
    private static Location outsideGate(Stargate gate) {
        Location base = gate.getGatePlayerTeleportLocation();
        if (base == null) {
            return null;
        }
        BlockFace facing = gate.getGateFacing();
        if (facing == null) {
            return null;
        }
        for (int distance = 1; distance <= 3; distance++) {
            Location candidate = base.clone().add(facing.getModX() * distance, 0, facing.getModZ() * distance);
            Block feet = candidate.getBlock();
            if (StargateManager.getGateFromBlock(feet) == null && feet.isPassable() && feet.getRelative(BlockFace.UP).isPassable()) {
                return candidate;
            }
        }
        return base.clone().add(facing.getModX() * 2, 0, facing.getModZ() * 2);
    }
    private static Material portalMaterial(Stargate gate) {
        if (gate.isGateCustom()) {
            return gate.getGateCustomPortalMaterial();
        }
        return gate.getGateShape() != null ? gate.getGateShape().getShapePortalMaterial() : Material.WATER;
    }
}