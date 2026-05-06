package de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/exceptions/UnknownOrientationException.class */
public class UnknownOrientationException extends WormholeXTremeException {
    private String orientation;

    public UnknownOrientationException(String orientation) {
        this.orientation = orientation;
    }

    public String getOrientation() {
        return this.orientation;
    }
}
