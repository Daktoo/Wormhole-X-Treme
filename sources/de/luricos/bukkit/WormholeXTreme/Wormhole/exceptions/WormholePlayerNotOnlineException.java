package de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/exceptions/WormholePlayerNotOnlineException.class */
public class WormholePlayerNotOnlineException extends WormholeXTremeException {
    private String message;

    public WormholePlayerNotOnlineException(String message) {
        this.message = message;
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        return this.message;
    }
}
