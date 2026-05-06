package de.luricos.bukkit.WormholeXTreme.Wormhole.utils;

import java.nio.ByteBuffer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/utils/DataUtils.class */
public class DataUtils {
    public static Block blockFromBytes(byte[] bytes, World w) {
        ByteBuffer b = ByteBuffer.wrap(bytes);
        return w.getBlockAt(b.getInt(), b.getInt(), b.getInt());
    }

    public static byte[] blockLocationToBytes(Location l) {
        ByteBuffer bb = ByteBuffer.allocate(12);
        bb.putInt(l.getBlockX());
        bb.putInt(l.getBlockY());
        bb.putInt(l.getBlockZ());
        return bb.array();
    }

    public static byte[] blockToBytes(Block b) {
        ByteBuffer bb = ByteBuffer.allocate(12);
        bb.putInt(b.getX());
        bb.putInt(b.getY());
        bb.putInt(b.getZ());
        return bb.array();
    }

    public static int byteArrayToInt(byte[] b, int index) {
        return (b[index] << 24) + ((b[index + 1] & 255) << 16) + ((b[index + 2] & 255) << 8) + (b[index + 3] & 255);
    }

    public static boolean byteToBoolean(byte b) {
        return b >= 1;
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    public static Location locationFromBytes(byte[] bytes, World w) {
        ByteBuffer b = ByteBuffer.wrap(bytes);
        return new Location(w, b.getDouble(), b.getDouble(), b.getDouble(), b.getFloat(), b.getFloat());
    }

    public static byte[] locationToBytes(Location l) {
        ByteBuffer b = ByteBuffer.allocate(32);
        b.putDouble(l.getX());
        b.putDouble(l.getY());
        b.putDouble(l.getZ());
        b.putFloat(l.getPitch());
        b.putFloat(l.getYaw());
        return b.array();
    }
}
