package de.luricos.bukkit.WormholeXTreme.Wormhole.utils;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import java.util.logging.Level;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/utils/WorldUtils.class */
public class WorldUtils {

    /* JADX INFO: renamed from: de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WorldUtils$1, reason: invalid class name */
    /* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/utils/WorldUtils$1.class */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$org$bukkit$block$BlockFace = new int[BlockFace.values().length];

        static {
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.NORTH.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.EAST.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.SOUTH.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.WEST.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.UP.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.DOWN.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.NORTH_EAST.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.SOUTH_WEST.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.NORTH_WEST.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.SOUTH_EAST.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
        }
    }

    public static Float getDegreesFromBlockFace(BlockFace blockFace) {
        switch (AnonymousClass1.$SwitchMap$org$bukkit$block$BlockFace[blockFace.ordinal()]) {
            case 1:
                return Float.valueOf(180.0f);
            case 2:
                return Float.valueOf(270.0f);
            case 3:
                return Float.valueOf(0.0f);
            case 4:
                return Float.valueOf(90.0f);
            default:
                return Float.valueOf(0.0f);
        }
    }

    public static BlockFace getInverseDirection(BlockFace blockFace) {
        return blockFace.getOppositeFace();
    }

    public static BlockFace getPerpendicularRightDirection(BlockFace blockFace) {
        switch (AnonymousClass1.$SwitchMap$org$bukkit$block$BlockFace[blockFace.ordinal()]) {
            case 1:
            case 5:
                return BlockFace.EAST;
            case 2:
                return BlockFace.SOUTH;
            case 3:
            case 6:
                return BlockFace.WEST;
            case 4:
                return BlockFace.NORTH;
            case 7:
                return BlockFace.SOUTH_EAST;
            case 8:
                return BlockFace.NORTH_WEST;
            case 9:
                return BlockFace.NORTH_EAST;
            case 10:
                return BlockFace.SOUTH_WEST;
            default:
                return blockFace;
        }
    }

    public static BlockFace getPerpendicularLeftDirection(BlockFace blockFace) {
        switch (AnonymousClass1.$SwitchMap$org$bukkit$block$BlockFace[blockFace.ordinal()]) {
            case 1:
            case 5:
                return BlockFace.WEST;
            case 2:
                return BlockFace.NORTH;
            case 3:
            case 6:
                return BlockFace.EAST;
            case 4:
                return BlockFace.SOUTH;
            case 7:
                return BlockFace.NORTH_WEST;
            case 8:
                return BlockFace.SOUTH_EAST;
            case 9:
                return BlockFace.SOUTH_WEST;
            case 10:
                return BlockFace.NORTH_EAST;
            default:
                return blockFace;
        }
    }

    public static boolean isSameBlock(Block b1, Block b2) {
        return b1 != null && b2 != null && b1.getX() == b2.getX() && b1.getY() == b2.getY() && b1.getZ() == b2.getZ();
    }

    public static void scheduleChunkLoad(Block b) {
        World w = b.getWorld();
        Chunk c = b.getChunk();
        if (WormholeXTreme.getWorldHandler() != null) {
            WormholeXTreme.getWorldHandler().addStickyChunk(c, "WormholeXTreme");
            return;
        }
        int cX = c.getX();
        int cZ = c.getZ();
        if (!w.isChunkLoaded(cX, cZ)) {
            WXTLogger.prettyLog(Level.FINE, false, "Loading chunk: " + c.toString() + " on: " + w.getName());
            w.loadChunk(cX, cZ);
        }
    }

    public static void scheduleChunkUnload(Block b) {
        World w = b.getWorld();
        Chunk c = b.getChunk();
        if (WormholeXTreme.getWorldHandler() != null) {
            WormholeXTreme.getWorldHandler().removeStickyChunk(c, "WormholeXTreme");
            return;
        }
        int cX = c.getX();
        int cZ = c.getZ();
        if (w.isChunkLoaded(cX, cZ)) {
            WXTLogger.prettyLog(Level.FINE, false, "Scheduling chunk unload: " + c.toString() + " on: " + w.getName());
            w.unloadChunkRequest(cX, cZ);
        }
    }
}
