package de.luricos.bukkit.WormholeXTreme.Wormhole.logic;
import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
public final class DestinyChevrons {
    private static final int LOCK_PAUSE_TICKS = 3;
    private static final Map<String, Spin> running = new HashMap<>();
    private DestinyChevrons() {
    }
    public static boolean isSpinning(Stargate gate) {
        return gate != null && running.containsKey(gate.getGateName());
    }
    public static void start(Stargate gate) {
        if (isSpinning(gate)) {
            return;
        }
        gate.setGateLightsActive(true);
        gate.setGateChevronsLocked(false);
        if (!gate.hasStructureBlocksPresent()) {
            gate.setGateChevronsLocked(true);
            return;
        }
        Ring ring = Ring.of(gate);
        if (ring == null || ring.chevrons.isEmpty() || gate.getSourceGateName() != null) {
            lightAllChevrons(gate);
            gate.setGateChevronsLocked(true);
            return;
        }
        Spin spin = new Spin(gate, ring);
        running.put(gate.getGateName(), spin);
        spin.taskId = WormholeXTreme.getScheduler().scheduleSyncRepeatingTask(WormholeXTreme.getThisPlugin(), spin, 1L, 1L);
        if (spin.taskId == -1) {
            running.remove(gate.getGateName());
            lightAllChevrons(gate);
            gate.setGateChevronsLocked(true);
        }
    }
    public static void stop(Stargate gate) {
        Spin spin = gate == null ? null : running.remove(gate.getGateName());
        if (spin == null) {
            return;
        }
        WormholeXTreme.getScheduler().cancelTask(spin.taskId);
        spin.restoreRing();
    }
    private static void lightAllChevrons(Stargate gate) {
        if (gate.getGateLightBlocks() == null || gate.getGateWorld() == null) {
            return;
        }
        for (ArrayList<Location> step : gate.getGateLightBlocks()) {
            if (step == null) {
                continue;
            }
            for (Location l : step) {
                setLit(gate, gate.getGateWorld().getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ()), true);
            }
        }
    }
    private static void setLit(Stargate gate, Block block, boolean lit) {
        Material frame = gate.getEffectiveStructureMaterial();
        Material light = gate.getEffectiveLightMaterial();
        Material type = block.getType();
        if (type != frame && type != light) {
            return;
        }
        Material wanted = lit ? light : frame;
        if (type != wanted) {
            block.setType(wanted);
        }
    }
    private static final class Ring {
        final List<Block> blocks;
        final Set<Integer> chevrons = new HashSet<>();
        final List<Integer> lockOrder = new ArrayList<>();
        final int top;
        private Ring(List<Block> blocks, int top) {
            this.blocks = blocks;
            this.top = top;
        }
        static Ring of(Stargate gate) {
            World world = gate.getGateWorld();
            BlockFace facing = gate.getGateFacing();
            ArrayList<Location> portal = gate.getGatePortalBlocks();
            if (world == null || facing == null || portal == null || portal.isEmpty()) {
                return null;
            }
            Vector front = new Vector(facing.getModX(), facing.getModY(), facing.getModZ());
            if (front.lengthSquared() == 0.0d || front.getY() != 0.0d) {
                return null;
            }
            Vector up = new Vector(0, 1, 0);
            Vector right = front.clone().multiply(-1.0d).getCrossProduct(up);
            Vector centre = new Vector();
            for (Location l : portal) {
                centre.add(new Vector(l.getBlockX() + 0.5d, l.getBlockY() + 0.5d, l.getBlockZ() + 0.5d));
            }
            centre.multiply(1.0d / portal.size());
            double plane = centre.dot(front);
            Set<Location> portalSet = new HashSet<>();
            for (Location l : portal) {
                portalSet.add(new Location(world, l.getBlockX(), l.getBlockY(), l.getBlockZ()));
            }
            Set<Location> lights = new HashSet<>();
            if (gate.getGateLightBlocks() != null) {
                for (ArrayList<Location> step : gate.getGateLightBlocks()) {
                    if (step != null) {
                        for (Location l : step) {
                            lights.add(new Location(world, l.getBlockX(), l.getBlockY(), l.getBlockZ()));
                        }
                    }
                }
            }
            Material frame = gate.getEffectiveStructureMaterial();
            Material light = gate.getEffectiveLightMaterial();
            Map<Block, Double> angles = new HashMap<>();
            Set<Location> seen = new HashSet<>();
            for (Location l : gate.getGateStructureBlocks()) {
                Location key = new Location(world, l.getBlockX(), l.getBlockY(), l.getBlockZ());
                if (!seen.add(key) || portalSet.contains(key)) {
                    continue;
                }
                Vector c = new Vector(key.getBlockX() + 0.5d, key.getBlockY() + 0.5d, key.getBlockZ() + 0.5d);
                if (Math.abs(c.dot(front) - plane) > 0.5d) {
                    continue;
                }
                Block block = world.getBlockAt(key);
                if (block.getType() != frame && block.getType() != light) {
                    continue;
                }
                Vector offset = c.clone().subtract(centre);
                double angle = Math.toDegrees(Math.atan2(offset.dot(right), offset.dot(up)));
                angles.put(block, angle < 0.0d ? angle + 360.0d : angle);
            }
            if (angles.size() < 3) {
                return null;
            }
            List<Block> ordered = new ArrayList<>(angles.keySet());
            ordered.sort(Comparator.comparingDouble(angles::get));
            int top = 0;
            double bestTop = Double.MAX_VALUE;
            List<Integer> chevronIndexes = new ArrayList<>();
            for (int i = 0; i < ordered.size(); i++) {
                Block block = ordered.get(i);
                double angle = angles.get(block);
                double fromTop = Math.min(angle, 360.0d - angle);
                if (lights.contains(block.getLocation())) {
                    chevronIndexes.add(i);
                    if (fromTop < bestTop) {
                        bestTop = fromTop;
                        top = i;
                    }
                }
            }
            Ring ring = new Ring(ordered, top);
            ring.chevrons.addAll(chevronIndexes);
            final int topIndex = top;
            chevronIndexes.sort(Comparator.comparingDouble(i -> i == topIndex ? 720.0d : angles.get(ordered.get(i))));
            ring.lockOrder.addAll(chevronIndexes);
            return ring;
        }
    }
    private static final class Spin implements Runnable {
        final Stargate gate;
        final Ring ring;
        final Set<Integer> locked = new HashSet<>();
        int taskId = -1;
        int position;
        int direction = 1;
        int next = 0;
        int stepsLeft;
        int pause = 0;
        Spin(Stargate gate, Ring ring) {
            this.gate = gate;
            this.ring = ring;
            this.position = ring.top;
            this.stepsLeft = stepsTo(ring.lockOrder.get(0));
        }
        int stepsTo(int target) {
            int n = this.ring.blocks.size();
            int steps = Math.floorMod((target - this.position) * this.direction, n);
            if (steps < 4) {
                steps += n;
            }
            return steps;
        }
        @Override
        public void run() {
            if (!this.gate.isGateLightsActive()) {
                stop(this.gate);
                return;
            }
            if (this.pause > 0) {
                this.pause--;
                return;
            }
            int n = this.ring.blocks.size();
            if (!this.locked.contains(this.position)) {
                setLit(this.gate, this.ring.blocks.get(this.position), false);
            }
            this.position = Math.floorMod(this.position + this.direction, n);
            setLit(this.gate, this.ring.blocks.get(this.position), true);
            this.stepsLeft--;
            if (this.stepsLeft > 0) {
                return;
            }
            this.locked.add(this.position);
            this.next++;
            if (this.next >= this.ring.lockOrder.size()) {
                running.remove(this.gate.getGateName());
                WormholeXTreme.getScheduler().cancelTask(this.taskId);
                this.gate.setGateChevronsLocked(true);
                WXTLogger.prettyLog(Level.FINE, false, "Destiny gate '" + this.gate.getGateName() + "' locked all chevrons.");
                return;
            }
            this.direction = -this.direction;
            this.pause = LOCK_PAUSE_TICKS;
            this.stepsLeft = stepsTo(this.ring.lockOrder.get(this.next));
        }
        void restoreRing() {
            for (int i = 0; i < this.ring.blocks.size(); i++) {
                if (!this.ring.chevrons.contains(i)) {
                    setLit(this.gate, this.ring.blocks.get(i), false);
                }
            }
        }
    }
}