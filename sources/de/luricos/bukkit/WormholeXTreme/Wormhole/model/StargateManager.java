package de.luricos.bukkit.WormholeXTreme.Wormhole.model;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.economy.EconomyManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateUpdateRunnable;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayerManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/model/StargateManager.class */
public class StargateManager {
    private static Map<Location, Stargate> allGateBlocks = new HashMap();
    private static Map<String, Stargate> stargateList = new HashMap();
    private static Map<String, Stargate> incompleteStargates = new HashMap();
    private static Map<String, Stargate> activatedStargates = new HashMap();
    private static Map<String, StargateNetwork> stargateNetworks = new HashMap();
    private static Map<String, StargateShape> playerBuilders = new HashMap();
    private static Map<Location, Block> openingAnimationBlocks = new HashMap();

    public static void addActivatedStargate(String gateName, Stargate s) {
        if (!hasActivatedStargate(gateName)) {
            getActivatedStargates().put(gateName, s);
        }
    }

    public static void addActivatedStargate(Stargate s) {
        addActivatedStargate(s.getGateName(), s);
    }

    public static boolean hasActivatedStargate(String gateName) {
        return getActivatedStargates().containsKey(gateName);
    }

    public static boolean hasActivatedStargate(Stargate s) {
        return hasActivatedStargate(s.getGateName());
    }

    public static void addBlockIndex(Block b, Stargate s) {
        if (b != null && s != null) {
            getAllGateBlocks().put(b.getLocation(), s);
        }
    }

    public static void addGateToNetwork(Stargate gate, String network) {
        if (!getStargateNetworks().containsKey(network)) {
            addStargateNetwork(network);
        }
        StargateNetwork net = getStargateNetworks().get(network);
        if (net != null) {
            synchronized (net.getNetworkGateLock()) {
                net.getNetworkGateList().add(gate);
                if (gate.isGateSignPowered()) {
                    net.getNetworkSignGateList().add(gate);
                }
            }
        }
    }

    public static void addIncompleteStargate(String playerName, Stargate stargate) {
        getIncompleteStargates().put(playerName, stargate);
    }

    public static void addPlayerBuilderShape(String playerName, StargateShape shape) {
        getPlayerBuilders().put(playerName, shape);
    }

    public static void removePlayerBuilderShape(String playerName) {
        getPlayerBuilders().remove(playerName);
    }

    public static void addStargate(Stargate s) {
        getStargateList().put(s.getGateName(), s);
        for (Location b : s.getGateStructureBlocks()) {
            getAllGateBlocks().put(b, s);
        }
        for (Location b2 : s.getGatePortalBlocks()) {
            getAllGateBlocks().put(b2, s);
        }
    }

    public static StargateNetwork addStargateNetwork(String networkName) {
        if (getStargateNetworks().containsKey(networkName)) {
            return getStargateNetworks().get(networkName);
        }
        StargateNetwork sn = new StargateNetwork();
        sn.setNetworkName(networkName);
        getStargateNetworks().put(networkName, sn);
        return sn;
    }

    public static boolean completeStargate(String playerName, Stargate stargate) {
        Stargate posDupe = getStargate(stargate.getGateName());
        if (posDupe != null) {
            return false;
        }
        stargate.setGateOwner(playerName);
        stargate.completeGate(stargate.getGateName(), "");
        WXTLogger.prettyLog(Level.INFO, false, "Player: " + playerName + " completed a wormhole: " + stargate.getGateName());
        addStargate(stargate);
        StargateDBManager.stargateToSQL(stargate);
        return true;
    }

    public static boolean completeStargate(Player player, Stargate stargate) {
        if (player == null || stargate == null) {
            return false;
        }
        if (!chargePlayerForCompletedGate(player, stargate)) {
            return false;
        }
        return completeStargate(player.getName(), stargate);
    }

    private static boolean chargePlayerForCompletedGate(Player player, Stargate stargate) {
        if (!ConfigManager.isEconomyEnabled() || !EconomyManager.isEconomyEnabled()) {
            return true;
        }
        if (stargate.getGateShape() == null) {
            WXTLogger.prettyLog(Level.FINE, false,
                    "[Economy] Skipping build charge for '" + stargate.getGateName()
                    + "' because it has no shape.");
            return true;
        }

        String shapeName = stargate.getGateShape().getShapeName();
        WXTLogger.prettyLog(Level.FINE, false,
                "[Economy] auto-complete economy check: shape=" + shapeName
                + " player=" + player.getName());
        return EconomyManager.canAffordAndCharge(player, shapeName);
    }

    public static boolean completeStargate(Player player, String gateName, String idc, String network) {
        return completeStargate(player.getName(), gateName, idc, network);
    }

    public static boolean completeStargate(String playerName, String gateName, String idc, String network) {
        Stargate complete = getIncompleteStargates().remove(playerName);
        if (complete != null) {
            if (!network.equals("")) {
                StargateNetwork net = getStargateNetwork(network);
                if (net == null) {
                    net = addStargateNetwork(network);
                }
                addGateToNetwork(complete, network);
                complete.setGateNetwork(net);
            }
            complete.setGateOwner(playerName);
            complete.completeGate(gateName, idc);
            WXTLogger.prettyLog(Level.INFO, false, "Player: " + playerName + " completed a wormhole: " + complete.getGateName());
            addStargate(complete);
            StargateDBManager.stargateToSQL(complete);
            return true;
        }
        return false;
    }

    public static double distanceSquaredToClosestGateBlock(Location self, Stargate stargate) {
        double distance = Double.MAX_VALUE;
        if (stargate != null && self != null) {
            ArrayList<Location> gateblocks = stargate.getGateStructureBlocks();
            for (Location l : gateblocks) {
                double blockdistance = getSquaredDistance(self, l);
                if (blockdistance < distance) {
                    distance = blockdistance;
                }
            }
        }
        return distance;
    }

    public static Stargate findClosestStargate(Location self) {
        Stargate stargate = null;
        if (self != null) {
            ArrayList<Stargate> gates = getAllGates();
            double man = Double.MAX_VALUE;
            for (Stargate s : gates) {
                Location t = s.getGatePlayerTeleportLocation();
                double distance = getSquaredDistance(self, t);
                if (distance < man) {
                    man = distance;
                    stargate = s;
                }
            }
        }
        return stargate;
    }

    private static HashMap<String, Stargate> getActivatedStargates() {
        return (HashMap) activatedStargates;
    }

    private static HashMap<Location, Stargate> getAllGateBlocks() {
        return (HashMap) allGateBlocks;
    }

    public static ArrayList<Stargate> getAllGates() {
        ArrayList<Stargate> gates = new ArrayList<>();
        for (Stargate s : getStargateList().values()) {
            gates.add(s);
        }
        return gates;
    }

    public static Stargate getGateFromBlock(Block block) {
        if (getAllGateBlocks().containsKey(block.getLocation())) {
            return getAllGateBlocks().get(block.getLocation());
        }
        return null;
    }

    private static HashMap<String, Stargate> getIncompleteStargates() {
        return (HashMap) incompleteStargates;
    }

    public static Stargate getIncompleteStargate(String playerName) {
        return getIncompleteStargates().get(playerName);
    }

    public static Stargate getIncompleteStargate(Player player) {
        return getIncompleteStargate(player.getName());
    }

    protected static HashMap<Location, Block> getOpeningAnimationBlocks() {
        return (HashMap) openingAnimationBlocks;
    }

    private static HashMap<String, StargateShape> getPlayerBuilders() {
        return (HashMap) playerBuilders;
    }

    public static StargateShape getPlayerBuilderShape(Player player) {
        return getPlayerBuilderShape(player.getName());
    }

    public static StargateShape getPlayerBuilderShape(String playerName) {
        if (getPlayerBuilders().containsKey(playerName)) {
            return getPlayerBuilders().remove(playerName);
        }
        return null;
    }

    private static double getSquaredDistance(Location self, Location target) {
        double distance = Double.MAX_VALUE;
        if (self != null && target != null) {
            distance = Math.pow(self.getX() - target.getX(), 2.0d) + Math.pow(self.getY() - target.getY(), 2.0d) + Math.pow(self.getZ() - target.getZ(), 2.0d);
        }
        return distance;
    }

    public static Stargate getStargate(String gateName) {
        if (getStargateList().containsKey(gateName)) {
            return getStargateList().get(gateName);
        }
        return null;
    }

    private static HashMap<String, Stargate> getStargateList() {
        return (HashMap) stargateList;
    }

    public static StargateNetwork getStargateNetwork(String name) {
        if (getStargateNetworks().containsKey(name)) {
            return getStargateNetworks().get(name);
        }
        return null;
    }

    private static HashMap<String, StargateNetwork> getStargateNetworks() {
        return (HashMap) stargateNetworks;
    }

    public static boolean isBlockInGate(Block block) {
        return isLocationInGate(block.getLocation());
    }

    public static boolean isLocationInGate(Location loc) {
        return getAllGateBlocks().containsKey(loc) || getOpeningAnimationBlocks().containsKey(loc);
    }

    public static boolean isStargate(String gateName) {
        return getStargateList().containsKey(gateName);
    }

    public static Stargate removeActivatedStargate(String gateName) {
        return getActivatedStargates().remove(gateName);
    }

    public static void removeBlockIndex(Block block) {
        if (block != null) {
            getAllGateBlocks().remove(block.getLocation());
        }
    }

    public static void removeIncompleteStargate(Player player) {
        removeIncompleteStargate(player.getName());
    }

    public static void removeIncompleteStargate(String playerName) {
        getIncompleteStargates().remove(playerName);
    }

    public static void removeStargate(Stargate s) {
        getStargateList().remove(s.getGateName());
        if (WormholePlayerManager.findPlayerByGateName(s.getGateName()) != null) {
            WormholePlayerManager.findPlayerByGateName(s.getGateName()).removeStargate(s);
        }
        StargateDBManager.removeStargateFromSQL(s);
        if (s.getGateNetwork() != null) {
            synchronized (s.getGateNetwork().getNetworkGateLock()) {
                s.getGateNetwork().getNetworkGateList().remove(s);
                if (s.isGateSignPowered()) {
                    s.getGateNetwork().getNetworkSignGateList().remove(s);
                }
                for (Stargate s2 : s.getGateNetwork().getNetworkSignGateList()) {
                    if (s2.getGateDialSignTarget() != null && s2.getGateDialSignTarget().getGateId() == s.getGateId() && s2.isGateSignPowered()) {
                        s2.setGateDialSignTarget(null);
                        if (s.getGateNetwork().getNetworkSignGateList().size() > 1) {
                            s2.setGateDialSignIndex(0);
                            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(s2, StargateUpdateRunnable.ActionToTake.DIAL_SIGN_CLICK));
                        }
                    }
                }
            }
        }
        for (Location b : s.getGateStructureBlocks()) {
            getAllGateBlocks().remove(b);
        }
        for (Location b2 : s.getGatePortalBlocks()) {
            getAllGateBlocks().remove(b2);
        }
    }

    public static Stargate getStargateByPlayer(Player player) {
        return getStargateByPlayer(player.getName());
    }

    public static Stargate getStargateByPlayer(String playerName) {
        return getActivatedStargates().get(playerName);
    }
}package de.luricos.bukkit.WormholeXTreme.Wormhole.model;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.economy.EconomyManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateUpdateRunnable;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayerManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/model/StargateManager.class */
public class StargateManager {
    private static Map<Location, Stargate> allGateBlocks = new HashMap();
    private static Map<String, Stargate> stargateList = new HashMap();
    private static Map<String, Stargate> incompleteStargates = new HashMap();
    private static Map<String, Stargate> activatedStargates = new HashMap();
    private static Map<String, StargateNetwork> stargateNetworks = new HashMap();
    private static Map<String, StargateShape> playerBuilders = new HashMap();
    private static Map<Location, Block> openingAnimationBlocks = new HashMap();

    public static void addActivatedStargate(String gateName, Stargate s) {
        if (!hasActivatedStargate(gateName)) {
            getActivatedStargates().put(gateName, s);
        }
    }

    public static void addActivatedStargate(Stargate s) {
        addActivatedStargate(s.getGateName(), s);
    }

    public static boolean hasActivatedStargate(String gateName) {
        return getActivatedStargates().containsKey(gateName);
    }

    public static boolean hasActivatedStargate(Stargate s) {
        return hasActivatedStargate(s.getGateName());
    }

    public static void addBlockIndex(Block b, Stargate s) {
        if (b != null && s != null) {
            getAllGateBlocks().put(b.getLocation(), s);
        }
    }

    public static void addGateToNetwork(Stargate gate, String network) {
        if (!getStargateNetworks().containsKey(network)) {
            addStargateNetwork(network);
        }
        StargateNetwork net = getStargateNetworks().get(network);
        if (net != null) {
            synchronized (net.getNetworkGateLock()) {
                net.getNetworkGateList().add(gate);
                if (gate.isGateSignPowered()) {
                    net.getNetworkSignGateList().add(gate);
                }
            }
        }
    }

    public static void addIncompleteStargate(String playerName, Stargate stargate) {
        getIncompleteStargates().put(playerName, stargate);
    }

    public static void addPlayerBuilderShape(String playerName, StargateShape shape) {
        getPlayerBuilders().put(playerName, shape);
    }

    public static void removePlayerBuilderShape(String playerName) {
        getPlayerBuilders().remove(playerName);
    }

    public static void addStargate(Stargate s) {
        getStargateList().put(s.getGateName(), s);
        for (Location b : s.getGateStructureBlocks()) {
            getAllGateBlocks().put(b, s);
        }
        for (Location b2 : s.getGatePortalBlocks()) {
            getAllGateBlocks().put(b2, s);
        }
    }

    public static StargateNetwork addStargateNetwork(String networkName) {
        if (getStargateNetworks().containsKey(networkName)) {
            return getStargateNetworks().get(networkName);
        }
        StargateNetwork sn = new StargateNetwork();
        sn.setNetworkName(networkName);
        getStargateNetworks().put(networkName, sn);
        return sn;
    }

    public static boolean completeStargate(String playerName, Stargate stargate) {
        Stargate posDupe = getStargate(stargate.getGateName());
        if (posDupe != null) {
            return false;
        }
        stargate.setGateOwner(playerName);
        stargate.completeGate(stargate.getGateName(), "");
        WXTLogger.prettyLog(Level.INFO, false, "Player: " + playerName + " completed a wormhole: " + stargate.getGateName());
        addStargate(stargate);
        StargateDBManager.stargateToSQL(stargate);
        return true;
    }

    public static boolean completeStargate(Player player, Stargate stargate) {
        if (player == null || stargate == null) {
            return false;
        }
        if (!chargePlayerForCompletedGate(player, stargate)) {
            return false;
        }
        return completeStargate(player.getName(), stargate);
    }

    private static boolean chargePlayerForCompletedGate(Player player, Stargate stargate) {
        if (!ConfigManager.isEconomyEnabled() || !EconomyManager.isEconomyEnabled()) {
            return true;
        }
        if (stargate.getGateShape() == null) {
            WXTLogger.prettyLog(Level.FINE, false,
                    "[Economy] Skipping build charge for '" + stargate.getGateName()
                    + "' because it has no shape.");
            return true;
        }

        String shapeName = stargate.getGateShape().getShapeName();
        WXTLogger.prettyLog(Level.FINE, false,
                "[Economy] auto-complete economy check: shape=" + shapeName
                + " player=" + player.getName());
        return EconomyManager.canAffordAndCharge(player, shapeName);
    }

    public static boolean completeStargate(Player player, String gateName, String idc, String network) {
        return completeStargate(player.getName(), gateName, idc, network);
    }

    public static boolean completeStargate(String playerName, String gateName, String idc, String network) {
        Stargate complete = getIncompleteStargates().remove(playerName);
        if (complete != null) {
            if (!network.equals("")) {
                StargateNetwork net = getStargateNetwork(network);
                if (net == null) {
                    net = addStargateNetwork(network);
                }
                addGateToNetwork(complete, network);
                complete.setGateNetwork(net);
            }
            complete.setGateOwner(playerName);
            complete.completeGate(gateName, idc);
            WXTLogger.prettyLog(Level.INFO, false, "Player: " + playerName + " completed a wormhole: " + complete.getGateName());
            addStargate(complete);
            StargateDBManager.stargateToSQL(complete);
            return true;
        }
        return false;
    }

    public static double distanceSquaredToClosestGateBlock(Location self, Stargate stargate) {
        double distance = Double.MAX_VALUE;
        if (stargate != null && self != null) {
            ArrayList<Location> gateblocks = stargate.getGateStructureBlocks();
            for (Location l : gateblocks) {
                double blockdistance = getSquaredDistance(self, l);
                if (blockdistance < distance) {
                    distance = blockdistance;
                }
            }
        }
        return distance;
    }

    public static Stargate findClosestStargate(Location self) {
        Stargate stargate = null;
        if (self != null) {
            ArrayList<Stargate> gates = getAllGates();
            double man = Double.MAX_VALUE;
            for (Stargate s : gates) {
                Location t = s.getGatePlayerTeleportLocation();
                double distance = getSquaredDistance(self, t);
                if (distance < man) {
                    man = distance;
                    stargate = s;
                }
            }
        }
        return stargate;
    }

    private static HashMap<String, Stargate> getActivatedStargates() {
        return (HashMap) activatedStargates;
    }

    private static HashMap<Location, Stargate> getAllGateBlocks() {
        return (HashMap) allGateBlocks;
    }

    public static ArrayList<Stargate> getAllGates() {
        ArrayList<Stargate> gates = new ArrayList<>();
        for (Stargate s : getStargateList().values()) {
            gates.add(s);
        }
        return gates;
    }

    public static Stargate getGateFromBlock(Block block) {
        if (getAllGateBlocks().containsKey(block.getLocation())) {
            return getAllGateBlocks().get(block.getLocation());
        }
        return null;
    }

    private static HashMap<String, Stargate> getIncompleteStargates() {
        return (HashMap) incompleteStargates;
    }

    public static Stargate getIncompleteStargate(String playerName) {
        return getIncompleteStargates().get(playerName);
    }

    public static Stargate getIncompleteStargate(Player player) {
        return getIncompleteStargate(player.getName());
    }

    protected static HashMap<Location, Block> getOpeningAnimationBlocks() {
        return (HashMap) openingAnimationBlocks;
    }

    private static HashMap<String, StargateShape> getPlayerBuilders() {
        return (HashMap) playerBuilders;
    }

    public static StargateShape getPlayerBuilderShape(Player player) {
        return getPlayerBuilderShape(player.getName());
    }

    public static StargateShape getPlayerBuilderShape(String playerName) {
        if (getPlayerBuilders().containsKey(playerName)) {
            return getPlayerBuilders().remove(playerName);
        }
        return null;
    }

    private static double getSquaredDistance(Location self, Location target) {
        double distance = Double.MAX_VALUE;
        if (self != null && target != null) {
            distance = Math.pow(self.getX() - target.getX(), 2.0d) + Math.pow(self.getY() - target.getY(), 2.0d) + Math.pow(self.getZ() - target.getZ(), 2.0d);
        }
        return distance;
    }

    public static Stargate getStargate(String gateName) {
        if (getStargateList().containsKey(gateName)) {
            return getStargateList().get(gateName);
        }
        return null;
    }

    private static HashMap<String, Stargate> getStargateList() {
        return (HashMap) stargateList;
    }

    public static StargateNetwork getStargateNetwork(String name) {
        if (getStargateNetworks().containsKey(name)) {
            return getStargateNetworks().get(name);
        }
        return null;
    }

    private static HashMap<String, StargateNetwork> getStargateNetworks() {
        return (HashMap) stargateNetworks;
    }

    public static boolean isBlockInGate(Block block) {
        return isLocationInGate(block.getLocation());
    }

    public static boolean isLocationInGate(Location loc) {
        return getAllGateBlocks().containsKey(loc) || getOpeningAnimationBlocks().containsKey(loc);
    }

    public static boolean isStargate(String gateName) {
        return getStargateList().containsKey(gateName);
    }

    public static Stargate removeActivatedStargate(String gateName) {
        return getActivatedStargates().remove(gateName);
    }

    public static void removeBlockIndex(Block block) {
        if (block != null) {
            getAllGateBlocks().remove(block.getLocation());
        }
    }

    public static void removeIncompleteStargate(Player player) {
        removeIncompleteStargate(player.getName());
    }

    public static void removeIncompleteStargate(String playerName) {
        getIncompleteStargates().remove(playerName);
    }

    public static void removeStargate(Stargate s) {
        getStargateList().remove(s.getGateName());
        if (WormholePlayerManager.findPlayerByGateName(s.getGateName()) != null) {
            WormholePlayerManager.findPlayerByGateName(s.getGateName()).removeStargate(s);
        }
        StargateDBManager.removeStargateFromSQL(s);
        if (s.getGateNetwork() != null) {
            synchronized (s.getGateNetwork().getNetworkGateLock()) {
                s.getGateNetwork().getNetworkGateList().remove(s);
                if (s.isGateSignPowered()) {
                    s.getGateNetwork().getNetworkSignGateList().remove(s);
                }
                for (Stargate s2 : s.getGateNetwork().getNetworkSignGateList()) {
                    if (s2.getGateDialSignTarget() != null && s2.getGateDialSignTarget().getGateId() == s.getGateId() && s2.isGateSignPowered()) {
                        s2.setGateDialSignTarget(null);
                        if (s.getGateNetwork().getNetworkSignGateList().size() > 1) {
                            s2.setGateDialSignIndex(0);
                            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(s2, StargateUpdateRunnable.ActionToTake.DIAL_SIGN_CLICK));
                        }
                    }
                }
            }
        }
        for (Location b : s.getGateStructureBlocks()) {
            getAllGateBlocks().remove(b);
        }
        for (Location b2 : s.getGatePortalBlocks()) {
            getAllGateBlocks().remove(b2);
        }
    }

    public static Stargate getStargateByPlayer(Player player) {
        return getStargateByPlayer(player.getName());
    }

    public static Stargate getStargateByPlayer(String playerName) {
        return getActivatedStargates().get(playerName);
    }
}