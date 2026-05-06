package de.luricos.bukkit.WormholeXTreme.Wormhole.model;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/model/StargateKeyring.class */
public enum StargateKeyring {
    DIAL("DialStargate"),
    SIGN("SignStargate"),
    NONE("UndefinedStargate");

    private String name;
    private static final Map<String, StargateKeyring> mapping = new HashMap();

    static {
        for (StargateKeyring kr : EnumSet.allOf(StargateKeyring.class)) {
            mapping.put(kr.name, kr);
        }
    }

    StargateKeyring(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static StargateKeyring byName(String name) {
        return mapping.get(name);
    }
}
