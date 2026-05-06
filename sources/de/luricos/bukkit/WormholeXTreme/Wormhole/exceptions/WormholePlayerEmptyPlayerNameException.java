package de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/exceptions/WormholePlayerEmptyPlayerNameException.class */
public class WormholePlayerEmptyPlayerNameException extends WormholeXTremeException {
    private String message;

    public WormholePlayerEmptyPlayerNameException(String message) {
        this.message = message;
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        return this.message;
    }
}
