package de.luricos.bukkit.WormholeXTreme.Wormhole.player;

import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/player/LocalPlayer.class */
public abstract class LocalPlayer {
    protected Player player;

    public abstract String getName();

    public abstract String getDisplayName();

    protected LocalPlayer(Player player) {
        this.player = null;
        this.player = player;
    }

    public boolean isOnline() {
        return this.player.isOnline();
    }

    public PlayerOrientation getCardinalDirection() {
        double rotation = (this.player.getLocation().getYaw() - 90.0f) % 360.0f;
        if (rotation < 0.0d) {
            rotation += 360.0d;
        }
        return getDirection(rotation);
    }

    private PlayerOrientation getDirection(double rotation) {
        if (0.0d <= rotation && rotation < 22.5d) {
            return PlayerOrientation.NORTH;
        }
        if (22.5d <= rotation && rotation < 67.5d) {
            return PlayerOrientation.NORTH_EAST;
        }
        if (67.5d <= rotation && rotation < 112.5d) {
            return PlayerOrientation.EAST;
        }
        if (112.5d <= rotation && rotation < 157.5d) {
            return PlayerOrientation.SOUTH_EAST;
        }
        if (157.5d <= rotation && rotation < 202.5d) {
            return PlayerOrientation.SOUTH;
        }
        if (202.5d <= rotation && rotation < 247.5d) {
            return PlayerOrientation.SOUTH_WEST;
        }
        if (247.5d <= rotation && rotation < 292.5d) {
            return PlayerOrientation.WEST;
        }
        if (292.5d <= rotation && rotation < 337.5d) {
            return PlayerOrientation.NORTH_WEST;
        }
        if (337.5d <= rotation && rotation < 360.0d) {
            return PlayerOrientation.NORTH;
        }
        return null;
    }
}
