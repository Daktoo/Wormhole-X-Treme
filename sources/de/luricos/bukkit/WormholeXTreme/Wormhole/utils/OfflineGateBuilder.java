package de.luricos.bukkit.WormholeXTreme.Wormhole.utils;

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate3DShape;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateShapeLayer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * Rebuilds a Stargate from a lever position and a facing without generating
 * terrain or writing a single block.
 *
 * The normal path, StargateHelper.checkStargate3D, does three things at once:
 * it works out where every block of the gate should be, it optionally places
 * those blocks, and it verifies what it finds. That is right for a player
 * building a gate and wrong for importing gates that already exist, where
 * placing blocks would overwrite real builds and the geometry is already known.
 *
 * This class keeps the arithmetic and drops the construction. Positions are
 * computed as Locations, which touch nothing; blocks are only read, and only
 * to confirm what is already there.
 *
 * The rule on missing blocks matches how the server is actually built: the
 * ring must be complete or the gate is rejected, while the DHD pillar is
 * optional, because players remove those deliberately. Recording only the
 * blocks that genuinely exist also stops the lighting code putting a removed
 * pillar back the first time the gate fires.
 */
public final class OfflineGateBuilder {

    /** The outcome for one gate, so the caller can report it. */
    public static final class Result {

        public final Stargate gate;
        public final String rejection;
        public final int ringBlocksMissing;
        public final int pillarBlocksMissing;
        public final boolean portalObstructed;

        private Result(Stargate gate, String rejection, int ringBlocksMissing,
                int pillarBlocksMissing, boolean portalObstructed) {
            this.gate = gate;
            this.rejection = rejection;
            this.ringBlocksMissing = ringBlocksMissing;
            this.pillarBlocksMissing = pillarBlocksMissing;
            this.portalObstructed = portalObstructed;
        }

        public boolean isAccepted() {
            return gate != null;
        }

        static Result rejected(String why) {
            return new Result(null, why, 0, 0, false);
        }
    }

    /** The layer holding the ring. Every block in it has to be present. */
    private static final int RING_LAYER = 0;

    private OfflineGateBuilder() {
    }

    /**
     * @param world     the gate's world, already known to be loaded.
     * @param leverX/Y/Z the recorded dial lever position. It does not need to
     *                  hold an actual lever: on a gate whose pillar has been
     *                  removed there will be nothing there, and that is fine.
     * @param facing    the direction the gate faces.
     * @param shape     the shape to rebuild against.
     */
    public static Result build(World world, int leverX, int leverY, int leverZ,
            BlockFace facing, Stargate3DShape shape) {

        if (shape == null || shape.getShapeActivationLayer() == -1) {
            return Result.rejected("shape has no activation layer");
        }

        Stargate s = new Stargate();
        s.setGateWorld(world);
        s.setGateShape(shape);
        s.setGateFacing(facing);

        // The one unavoidable world touch. Stargate stores the lever as a
        // Block and reads its type on assignment to tell a lever from a
        // button, so the chunk is loaded here. Nothing is written, and on a
        // gate with no pillar this simply reads air and leaves the gate
        // dial-only until someone rebuilds it.
        Block leverBlock = world.getBlockAt(leverX, leverY, leverZ);
        s.setGateDialLeverBlock(leverBlock);
        s.getGateStructureBlocks().add(leverBlock.getLocation());

        int[] facingVector = facingVector(facing);
        if (facingVector == null) {
            return Result.rejected("unsupported facing " + facing.name());
        }

        int[] ref = shape.getShapeReferenceVector();
        int[] dir = new int[3];
        dir[0] = (facingVector[1] * ref[2]) - (facingVector[2] * ref[1]);
        dir[1] = (facingVector[2] * ref[0]) - (facingVector[0] * ref[2]);
        dir[2] = (facingVector[0] * ref[1]) - (facingVector[1] * ref[0]);

        // The activation block sits one step behind the lever, and the shape
        // is anchored from it exactly as checkStargate3D does.
        BlockFace opposite = inverse(facing);
        int ax = leverX + opposite.getModX();
        int ay = leverY + opposite.getModY();
        int az = leverZ + opposite.getModZ();

        StargateShapeLayer actLayer = shape.getShapeLayers().get(shape.getShapeActivationLayer());
        int[] start = new int[3];
        start[0] = ax - (dir[0] * actLayer.getLayerActivationPosition()[2]);
        start[1] = ay - actLayer.getLayerActivationPosition()[1];
        start[2] = az - (dir[2] * actLayer.getLayerActivationPosition()[2]);

        Material structureMaterial = shape.getShapeStructureMaterial();
        int ringMissing = 0;
        int pillarMissing = 0;
        boolean portalObstructed = false;

        for (int i = 0; i < shape.getShapeLayers().size(); i++) {
            StargateShapeLayer layer = shape.getShapeLayers().get(i);
            if (layer == null) {
                continue;
            }
            int layerOffset = shape.getShapeActivationLayer() - i;
            int[] corner = {start[0] - (facingVector[0] * layerOffset), start[1],
                            start[2] - (facingVector[2] * layerOffset)};

            boolean isRing = (i == RING_LAYER);

            for (Integer[] pos : layer.getLayerBlockPositions()) {
                Location loc = locationFromVector(pos, dir, corner, world);
                if (world.getBlockAt(loc).getType() == structureMaterial) {
                    s.getGateStructureBlocks().add(loc);
                } else if (isRing) {
                    ringMissing++;
                } else {
                    // A missing pillar block is expected, not an error. It is
                    // left out of the structure list so nothing rebuilds it.
                    pillarMissing++;
                }
            }

            for (Integer[] pos : layer.getLayerPortalPositions()) {
                Location loc = locationFromVector(pos, dir, corner, world);
                Material found = world.getBlockAt(loc).getType();
                if (found == Material.AIR || found == Material.WATER) {
                    s.getGatePortalBlocks().add(loc);
                } else {
                    portalObstructed = true;
                    s.getGatePortalBlocks().add(loc);
                }
            }

            if (layer.getLayerPlayerExitPosition().length > 0) {
                s.setGatePlayerTeleportLocation(exitLocation(
                        layer.getLayerPlayerExitPosition(), dir, corner, world, facing));
            }
            if (layer.getLayerMinecartExitPosition().length > 0) {
                s.setGateMinecartTeleportLocation(exitLocation(
                        layer.getLayerMinecartExitPosition(), dir, corner, world, facing));
            }

            collectGrouped(layer.getLayerWooshPositions(), s.getGateWooshBlocks(), dir, corner, world);
            collectGrouped(layer.getLayerLightPositions(), s.getGateLightBlocks(), dir, corner, world);
        }

        if (ringMissing > 0) {
            return Result.rejected(ringMissing + " ring block"
                    + (ringMissing == 1 ? "" : "s") + " missing");
        }

        if (shape.getShapeSignPosition().length > 0) {
            int[] sp = shape.getShapeSignPosition();
            int[] blockLocation = {sp[2] * dir[0], sp[1], sp[2] * dir[2]};
            s.setGateNameBlockHolder(world.getBlockAt(blockLocation[0] + start[0],
                    blockLocation[1] + start[1], blockLocation[2] + start[2]));
        }
        if (shape.isShapeRedstoneActivated()) {
            s.setGateRedstonePowered(true);
        }

        return new Result(s, null, ringMissing, pillarMissing, portalObstructed);
    }

    /**
     * Where a player lands. The live code walks upward from the exit block
     * until it finds air; on a uniform shape the exit block is solid and the
     * space above it is clear, so one step up gives the same answer without
     * reading a column of blocks.
     */
    private static Location exitLocation(int[] pos, int[] dir, int[] corner, World world,
            BlockFace facing) {
        int[] blockLocation = {pos[2] * dir[0], pos[1], pos[2] * dir[2]};
        Location loc = new Location(world,
                blockLocation[0] + corner[0] + 0.5d,
                blockLocation[1] + corner[1] + 1.0d,
                blockLocation[2] + corner[2] + 0.5d);
        loc.setYaw(WorldUtils.getDegreesFromBlockFace(facing).floatValue());
        loc.setPitch(0.0f);
        return loc;
    }

    private static void collectGrouped(List<ArrayList<Integer[]>> source,
            List<ArrayList<Location>> target, int[] dir, int[] corner, World world) {
        for (int i = 0; i < source.size(); i++) {
            while (target.size() < i + 1) {
                target.add(new ArrayList<Location>());
            }
            if (source.get(i) == null) {
                continue;
            }
            for (Integer[] pos : source.get(i)) {
                target.get(i).add(locationFromVector(pos, dir, corner, world));
            }
        }
    }

    /** Mirrors StargateHelper.getBlockFromVector, but as a Location. */
    private static Location locationFromVector(Integer[] bVect, int[] dir, int[] corner, World w) {
        return new Location(w,
                (bVect[2].intValue() * dir[0]) + corner[0],
                bVect[1].intValue() + corner[1],
                (bVect[2].intValue() * dir[2]) + corner[2]);
    }

    private static int[] facingVector(BlockFace facing) {
        switch (facing) {
            case NORTH: return new int[] {0, 0, -1};
            case SOUTH: return new int[] {0, 0, 1};
            case EAST:  return new int[] {1, 0, 0};
            case WEST:  return new int[] {-1, 0, 0};
            case UP:    return new int[] {0, 1, 0};
            case DOWN:  return new int[] {0, -1, 0};
            default:    return null;
        }
    }

    private static BlockFace inverse(BlockFace facing) {
        switch (facing) {
            case NORTH: return BlockFace.SOUTH;
            case SOUTH: return BlockFace.NORTH;
            case EAST:  return BlockFace.WEST;
            case WEST:  return BlockFace.EAST;
            case UP:    return BlockFace.DOWN;
            case DOWN:  return BlockFace.UP;
            default:    return BlockFace.SELF;
        }
    }
}
