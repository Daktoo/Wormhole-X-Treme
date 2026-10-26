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
            for (Location 1 : portal) {
                Block block = world.getBlockAt(1.getBlockX(), 1.getBlockY(), 1.getBlockZ());
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
            
        }
    }
}