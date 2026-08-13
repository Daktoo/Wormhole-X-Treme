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
        if (gate == null || network == null) {
            return;
        }
        if (!getStargateNetworks().containsKey(network)) {
            addStargateNetwork(network);
        }
        StargateNetwork net = getStargateNetworks().get(network);
        if (net != null) {
            synchronized (net.getNetworkGateLock()) {
                registerInNetworkList(net.getNetworkGateList(), gate);
                if (gate.isGateSignPowered()) {
                    registerInNetworkList(net.getNetworkSignGateList(), gate);
                }
            }
        }
    }

    /**
     * Add a gate to a network list without ever creating a duplicate entry.
     *
     * A plain add() let the same gate be registered repeatedly, and let an
     * abandoned build candidate keep a slot in the rotation alongside the real
     * gate of the same name. If an entry already carries this gate's name it is
     * replaced, so a live gate always supersedes a stale instance.
     */
    private static void registerInNetworkList(ArrayList<Stargate> list, Stargate gate) {
        for (int i = 0; i < list.size(); i++) {
            Stargate other = list.get(i);
            if (other == gate) {
                return;
            }
            if (isSameGateName(other, gate)) {
                list.set(i, gate);
                return;
            }
        }
        list.add(gate);
    }

    /** True when both gates carry the same non-empty name. */
    private static boolean isSameGateName(Stargate first, Stargate second) {
        return first != null && second != null
                && first.getGateName() != null && !first.getGateName().isEmpty()
                && first.getGateName().equals(second.getGateName());
    }

    /** Drop every entry naming this gate, whatever instance is holding the slot. */
    private static void purgeFromNetworkList(ArrayList<Stargate> list, Stargate gate, String gateName) {
        list.removeIf(entry -> entry == gate
                || (entry != null && gateName != null && gateName.equals(entry.getGateName())));
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
        // Network registration happens here, once the gate is real, rather than
        // during shape detection when it is still only a build candidate.
        if (s.getGateNetwork() != null) {
            addGateToNetwork(s, s.getGateNetwork().getNetworkName());
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
        if (findGateTooClose(stargate) != null) {
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
        // Peek before removing: if the gate is rejected the player keeps their
        // pending build and can move it rather than having to start over.
        Stargate pending = getIncompleteStargates().get(playerName);
        if (pending != null && findGateTooClose(pending) != null) {
            return false;
        }
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

    /**
     * Find an existing stargate that sits closer to this candidate than the
     * configured minimum gate distance allows.
     *
     * Distance is measured from the candidate's centre to the nearest block of
     * each existing gate, in the same world only. A configured distance of 0
     * disables the check entirely.
     *
     * This is only ever called when a gate is being built. Gates already standing
     * closer together than the configured minimum are left alone - the rule
     * applies to new construction, not to what is already on the map.
     *
     * @return the offending gate, or null if the candidate has enough room.
     */
    public static Stargate findGateTooClose(Stargate candidate) {
        int minimumDistance = ConfigManager.getWormholeMinimumGateDistance();
        if (minimumDistance <= 0 || candidate == null) {
            return null;
        }
        Location candidateCentre = candidate.getGatePlayerTeleportLocation();
        if (candidateCentre == null && candidate.getGateDialLeverBlock() != null) {
            candidateCentre = candidate.getGateDialLeverBlock().getLocation();
        }
        if (candidateCentre == null || candidateCentre.getWorld() == null) {
            return null;
        }
        double minimumDistanceSquared = (double) minimumDistance * (double) minimumDistance;
        for (Stargate existing : getAllGates()) {
            if (existing == candidate || existing.getGateName() == null
                    || existing.getGateName().equals(candidate.getGateName())) {
                continue;
            }
            if (existing.getGateWorld() == null
                    || !existing.getGateWorld().equals(candidateCentre.getWorld())) {
                continue;
            }
            if (distanceSquaredToClosestGateBlock(candidateCentre, existing) < minimumDistanceSquared) {
                return existing;
            }
        }
        return null;
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

    /**
     * Find a gate that is present in a network list but missing from the main
     * registry. These orphans cannot be reached by name through getStargate(),
     * so /wxremove had no way to clear one.
     */
    public static Stargate findOrphanedGate(String gateName) {
        if (gateName == null || getStargateList().containsKey(gateName)) {
            return null;
        }
        for (StargateNetwork net : getStargateNetworks().values()) {
            synchronized (net.getNetworkGateLock()) {
                for (Stargate gate : net.getNetworkGateList()) {
                    if (gate != null && gateName.equals(gate.getGateName())) {
                        return gate;
                    }
                }
                for (Stargate gate : net.getNetworkSignGateList()) {
                    if (gate != null && gateName.equals(gate.getGateName())) {
                        return gate;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Drop every trace of an orphaned gate from the network lists and the block
     * index. Returns true if anything was actually removed.
     */
    public static boolean purgeOrphanedGate(String gateName) {
        if (gateName == null || getStargateList().containsKey(gateName)) {
            return false;
        }
        boolean removed = false;
        for (StargateNetwork net : getStargateNetworks().values()) {
            synchronized (net.getNetworkGateLock()) {
                removed |= net.getNetworkGateList().removeIf(gate -> gate != null && gateName.equals(gate.getGateName()));
                removed |= net.getNetworkSignGateList().removeIf(gate -> gate != null && gateName.equals(gate.getGateName()));
                for (Stargate other : net.getNetworkSignGateList()) {
                    Stargate signTarget = other.getGateDialSignTarget();
                    if (signTarget != null && gateName.equals(signTarget.getGateName())) {
                        other.setGateDialSignTarget(null);
                        other.setGateDialSignIndex(0);
                        WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(other, StargateUpdateRunnable.ActionToTake.DIAL_SIGN_CLICK));
                    }
                }
            }
        }
        removed |= getAllGateBlocks().values().removeIf(indexed -> indexed != null && gateName.equals(indexed.getGateName()));
        if (removed) {
            WXTLogger.prettyLog(Level.INFO, false, "Purged orphaned stargate entry '" + gateName + "'.");
        }
        return removed;
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
        if (s == null) {
            return;
        }
        String gateName = s.getGateName();
        // Any removal path - /wxremove, a broken block, an admin command - must
        // close an open wormhole first, or the gate at the far end is left active
        // with a portal leading to a stargate that no longer exists.
        if (s.isGateActive() || s.isGateLightsActive() || s.isWormholeEstablished()
                || s.getGateTarget() != null || s.getSourceGateName() != null) {
            s.shutdownStargate(false);
        }
        getStargateList().remove(gateName);
        if (WormholePlayerManager.findPlayerByGateName(gateName) != null) {
            WormholePlayerManager.findPlayerByGateName(gateName).removeStargate(s);
        }
        StargateDBManager.removeStargateFromSQL(s);
        if (s.getGateNetwork() != null) {
            synchronized (s.getGateNetwork().getNetworkGateLock()) {
                // Purge by name, not by object identity: any stale instance left
                // over from an abandoned build carries the same name and would
                // otherwise survive removal and keep haunting the dialer lists.
                purgeFromNetworkList(s.getGateNetwork().getNetworkGateList(), s, gateName);
                purgeFromNetworkList(s.getGateNetwork().getNetworkSignGateList(), s, gateName);
                for (Stargate s2 : s.getGateNetwork().getNetworkSignGateList()) {
                    Stargate signTarget = s2.getGateDialSignTarget();
                    boolean targetsRemovedGate = signTarget != null
                            && (signTarget == s
                                || isSameGateName(signTarget, s)
                                || (s.getGateId() >= 0 && signTarget.getGateId() == s.getGateId()));
                    if (targetsRemovedGate && s2.isGateSignPowered()) {
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
        // Block-index sweep. gateRemove() calls setupGateSign(false) first, which
        // drops the name sign location out of the structure block list, so the two
        // loops above cannot clear it. Any surviving entry points at a dead gate
        // and hands it back out to the next player who clicks that block.
        getAllGateBlocks().values().removeIf(indexed -> indexed == s
                || (indexed != null && gateName != null && gateName.equals(indexed.getGateName())));
    }

    public static Stargate getStargateByPlayer(Player player) {
        return getStargateByPlayer(player.getName());
    }

    public static Stargate getStargateByPlayer(String playerName) {
        return getActivatedStargates().get(playerName);
    }
}
