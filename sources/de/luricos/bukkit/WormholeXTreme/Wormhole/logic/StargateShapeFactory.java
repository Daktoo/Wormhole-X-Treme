package de.luricos.bukkit.WormholeXTreme.Wormhole.logic;

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate3DShape;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateShape;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/logic/StargateShapeFactory.class */
public class StargateShapeFactory {
    private static StargateShape create2DShape(String[] fileLines) {
        return new StargateShape(fileLines);
    }

    private static Stargate3DShape create3DShape(String[] fileLines) {
        return new Stargate3DShape(fileLines);
    }

    protected static StargateShape createShapeFromFile(String[] fileLines) {
        for (String line : fileLines) {
            if (line.startsWith("Version=2")) {
                return create3DShape(fileLines);
            }
        }
        return create2DShape(fileLines);
    }
}
