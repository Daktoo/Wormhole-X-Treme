package de.luricos.bukkit.WormholeXTreme.Wormhole.model;

import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionsManager;
import java.util.ArrayList;
import java.util.HashMap;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/model/StargateNetwork.class */
public class StargateNetwork {
    private String networkName;
    private final ArrayList<Stargate> networkGateList = new ArrayList<>();
    private final ArrayList<Stargate> networkSignGateList = new ArrayList<>();
    private Object networkGateLock = new Object();
    private final HashMap<String, PermissionsManager.PermissionLevel> networkIndividualPermissions = new HashMap<>();

    public ArrayList<Stargate> getNetworkGateList() {
        return this.networkGateList;
    }

    public Object getNetworkGateLock() {
        return this.networkGateLock;
    }

    public HashMap<String, PermissionsManager.PermissionLevel> getNetworkIndividualPermissions() {
        return this.networkIndividualPermissions;
    }

    public String getNetworkName() {
        return this.networkName;
    }

    public ArrayList<Stargate> getNetworkSignGateList() {
        return this.networkSignGateList;
    }

    public void setNetworkGateLock(Object networkGateLock) {
        this.networkGateLock = networkGateLock;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }
}
