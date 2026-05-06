package de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/exceptions/WormholePermissionBackendException.class */
public class WormholePermissionBackendException extends WormholeXTremeException {
    private String message;

    public WormholePermissionBackendException(String message) {
        this.message = message;
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        return this.message;
    }
}
