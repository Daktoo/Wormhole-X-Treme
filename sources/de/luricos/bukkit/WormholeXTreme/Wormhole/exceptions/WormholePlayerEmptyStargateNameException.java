package de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/exceptions/WormholePlayerEmptyStargateNameException.class */
public class WormholePlayerEmptyStargateNameException extends WormholeXTremeException {
    private String message;

    public WormholePlayerEmptyStargateNameException(String message) {
        this.message = message;
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        return this.message;
    }
}
