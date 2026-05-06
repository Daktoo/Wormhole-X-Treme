package de.luricos.bukkit.WormholeXTreme.Wormhole.player;

import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholePlayerEmptyStargateNameException;
import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholePlayerNullPointerException;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/player/WormholePlayer.class */
public class WormholePlayer extends LocalPlayer {
    private Map<String, WormholePlayerUsageProperties> usageProperties;
    private Map<String, Stargate> stargateMap;
    private String currentGateName;

    protected WormholePlayer(Player player) {
        super(player);
        this.usageProperties = new HashMap();
        this.stargateMap = new HashMap();
        this.currentGateName = "";
    }

    public void resetPlayer() {
        WXTLogger.prettyLog(Level.FINE, false, "Resetting player '" + getName() + "'");
        for (Stargate s : getStargates()) {
            removeStargate(s.getGateName());
            removeProperty(s.getGateName());
        }
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.player.LocalPlayer
    public String getName() {
        return this.player.getName();
    }

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.player.LocalPlayer
    public String getDisplayName() {
        return this.player.getDisplayName();
    }

    public Player getPlayer() {
        return this.player;
    }

    public WormholePlayerUsageProperties getProperties() {
        if (!"".equals(getCurrentGateName())) {
            return getProperties(getCurrentGateName());
        }
        return null;
    }

    public WormholePlayerUsageProperties getProperties(Stargate stargate) {
        return getProperties(stargate.getGateName());
    }

    public WormholePlayerUsageProperties getProperties(String gateName) {
        if (hasStargate(gateName)) {
            return this.usageProperties.get(gateName);
        }
        return new WormholePlayerUsageProperties();
    }

    public PlayerOrientation getKickBackDirection(BlockFace facing) {
        return getKickBackDirection(null, facing);
    }

    public PlayerOrientation getKickBackDirection(PlayerOrientation direction) {
        return getKickBackDirection(direction, null);
    }

    private PlayerOrientation getKickBackDirection(PlayerOrientation direction, BlockFace facing) {
        if (!isOnline()) {
            return null;
        }
        if (direction != null || facing != null) {
            WXTLogger.prettyLog(Level.FINE, false, "PlayerDirection: " + getCardinalDirection() + ", BlockFacing: " + facing);
            PlayerOrientation kickBack = null;
            if (direction != null) {
                kickBack = PlayerOrientation.byCaseInsensitiveName(direction.name());
            }
            if (facing != null) {
                kickBack = PlayerOrientation.byCaseInsensitiveName(facing.name());
            }
            switch (kickBack) {
                case NORTH:
                case NORTH_EAST:
                case NORTH_WEST:
                    WXTLogger.prettyLog(Level.FINE, false, "NORTH: kickback direction SOUTH");
                    return PlayerOrientation.SOUTH;
                case SOUTH:
                case SOUTH_EAST:
                case SOUTH_WEST:
                    WXTLogger.prettyLog(Level.FINE, false, "SOUTH: kickback direction NORTH");
                    return PlayerOrientation.NORTH;
                case EAST:
                    WXTLogger.prettyLog(Level.FINE, false, "EAST: kickback direction WEST");
                    return PlayerOrientation.WEST;
                case WEST:
                    WXTLogger.prettyLog(Level.FINE, false, "WEST: kickback direction EAST");
                    return PlayerOrientation.EAST;
                default:
                    WXTLogger.prettyLog(Level.FINE, false, "No kickback direction found");
                    return null;
            }
        }
        return null;
    }

    public Stargate getStargate() {
        if (!"".equalsIgnoreCase(getCurrentGateName())) {
            return getStargate(getCurrentGateName());
        }
        return null;
    }

    public List<Stargate> getStargates() {
        List<Stargate> stargates = new ArrayList<>();
        for (Stargate s : this.stargateMap.values()) {
            stargates.add(s);
        }
        return stargates;
    }

    public void setCurrentGateName(String gateName) {
        if (gateName == null) {
            gateName = "";
        }
        WXTLogger.prettyLog(Level.FINE, false, "Setting current used gateName to '" + gateName + "' for player '" + getName() + "'");
        this.currentGateName = gateName;
    }

    public String getCurrentGateName() {
        return this.currentGateName;
    }

    public Stargate getStargate(String gateName) {
        if (hasStargate(gateName)) {
            WXTLogger.prettyLog(Level.FINE, false, "Get stargate '" + gateName + "'");
            return this.stargateMap.get(gateName);
        }
        WXTLogger.prettyLog(Level.WARNING, false, "Could not get stargate '" + gateName + "' for player '" + getName() + "'");
        return new Stargate();
    }

    public boolean hasStargate(Stargate stargate) {
        return hasStargate(stargate.getGateName());
    }

    public boolean hasStargate(String gateName) {
        try {
            if (gateName != null) {
                return this.stargateMap.containsKey(gateName);
            }
            throw new WormholePlayerNullPointerException("hasStargate checked for null. Can't check for null gateNames!");
        } catch (WormholePlayerNullPointerException e) {
            WXTLogger.prettyLog(Level.SEVERE, true, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void addStargate(Stargate stargate) {
        if (hasStargate(stargate)) {
            WXTLogger.prettyLog(Level.FINE, false, "Stargate '" + stargate.getGateName() + "' was already added for player '" + getName() + "'");
            setCurrentGateName(stargate.getGateName());
        } else {
            WXTLogger.prettyLog(Level.FINE, false, "Adding Stargate '" + stargate.getGateName() + "' to player '" + getName() + "'");
            this.stargateMap.put(stargate.getGateName(), stargate);
            addProperties(stargate.getGateName());
            setCurrentGateName(stargate.getGateName());
        }
    }

    private void addProperties(String stargateName) {
        WXTLogger.prettyLog(Level.FINE, false, "Adding properties for gate '" + stargateName + "' to player '" + getName() + "'");
        this.usageProperties.put(stargateName, new WormholePlayerUsageProperties());
    }

    public void removeStargate(Stargate stargate) {
        try {
            if (stargate != null) {
                removeStargate(stargate.getGateName());
                return;
            }
            throw new WormholePlayerNullPointerException("Remove Stargate failed. Stargate name was null.");
        } catch (WormholePlayerNullPointerException e) {
            WXTLogger.prettyLog(Level.SEVERE, true, e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeStargate(String stargateName) {
        try {
            WXTLogger.prettyLog(Level.FINE, false, "Removing Stargate '" + stargateName + "' from player '" + getName() + "'");
            if (!"".equals(stargateName)) {
                if (this.stargateMap.remove(stargateName) == null) {
                    WXTLogger.prettyLog(Level.FINE, false, "Stargate '" + stargateName + "' wasn't attached to player '" + getName() + "'");
                } else {
                    WXTLogger.prettyLog(Level.FINE, false, "StargateMaps count is now: '" + getGateCount() + "'");
                }
                removeProperty(stargateName);
                return;
            }
            throw new WormholePlayerEmptyStargateNameException("Stargate name can't be empty. Probably a malfunction during execution.");
        } catch (WormholePlayerEmptyStargateNameException e) {
            WXTLogger.prettyLog(Level.SEVERE, true, e.getMessage());
        }
    }

    private void removeProperty(String gateName) {
        WXTLogger.prettyLog(Level.FINE, false, "Removing property for Stargate '" + gateName + "' from player '" + getName() + "'");
        this.usageProperties.remove(gateName);
    }

    public int getGateCount() {
        return this.stargateMap.keySet().size();
    }

    public void sendMessage(String message) {
        if (this.player.isOnline()) {
            getPlayer().sendMessage(message);
        }
    }
}
