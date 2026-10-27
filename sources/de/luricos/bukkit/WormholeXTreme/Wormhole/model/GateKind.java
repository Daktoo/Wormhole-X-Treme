package de.luricos.bukkit.WormholeXTreme.Wormhole.model;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.BuiltInShapes;
public enum GateKind {
    MILKY_WAY("Milky Way"),
    PEGASUS("Pegasus"),
    DESTINY("Destiny");
    private final String displayName;
    GateKind(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return this.displayName;
    }
    public boolean isBonus() {
        return this != MILKY_WAY;
    }
    public static GateKind of(Stargate gate) {
        if (gate == null || gate.getGateShape() == null || gate.getGateShape().getShapeName() == null) {
            return MILKY_WAY;
        }
        String shapeName = gate.getGateShape().getShapeName();
        if (shapeName.equalsIgnoreCase(BuiltInShapes.PEGASUS)) {
            return PEGASUS;
        }
        if (shapeName.equalsIgnoreCase(BuiltInShapes.DESTINY)) {
            return DESTINY;
        }
        return MILKY_WAY;
    }
    public static boolean isDestiny(Stargate gate) {
        return of(gate) == DESTINY;
    }
}