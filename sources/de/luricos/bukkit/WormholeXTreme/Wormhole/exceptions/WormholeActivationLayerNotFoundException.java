package de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/exceptions/WormholeActivationLayerNotFoundException.class */
public class WormholeActivationLayerNotFoundException extends WormholeXTremeException {
    private String message;

    public WormholeActivationLayerNotFoundException(String message) {
        this.message = message;
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        return this.message;
    }
}
