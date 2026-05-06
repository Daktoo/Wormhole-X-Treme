package de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/exceptions/WormholeNotAvailable.class */
public class WormholeNotAvailable extends WormholeXTremeException {
    private String message;

    public WormholeNotAvailable(String message) {
        this.message = message;
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        return this.message;
    }
}
