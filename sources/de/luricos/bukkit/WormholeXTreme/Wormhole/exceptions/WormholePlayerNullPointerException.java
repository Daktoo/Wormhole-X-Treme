package de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/exceptions/WormholePlayerNullPointerException.class */
public class WormholePlayerNullPointerException extends WormholeXTremeException {
    private String message;

    public WormholePlayerNullPointerException(String message) {
        this.message = message;
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        return this.message;
    }
}
