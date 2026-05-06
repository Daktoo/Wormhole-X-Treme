package de.luricos.bukkit.WormholeXTreme.Wormhole.player;

import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholePlayerEmptyPlayerNameException;
import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholePlayerNotFoundException;
import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholePlayerNotOnlineException;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/player/WormholePlayerManager.class */
public class WormholePlayerManager {
    private static Map<String, WormholePlayer> wormholePlayers = new HashMap();

    public static void registerPlayer(String playerName) {
        try {
            Player player = Bukkit.getServer().getPlayer(playerName);
            if (player == null) {
                throw new WormholePlayerNotFoundException("Player '" + playerName + "' not found");
            }
            if (player != null && !player.isOnline()) {
                throw new WormholePlayerNotOnlineException("Player '" + playerName + "' is not online");
            }
            registerPlayer(player);
        } catch (WormholePlayerNotFoundException e) {
            WXTLogger.prettyLog(Level.SEVERE, false, e.getMessage());
        } catch (WormholePlayerNotOnlineException e2) {
            WXTLogger.prettyLog(Level.WARNING, false, e2.getMessage());
        }
    }

    public static void registerPlayer(Player player) {
        if (!isRegistered(player.getName())) {
            WXTLogger.prettyLog(Level.FINE, false, "Registering player '" + player.getName() + "' as WormholePlayer");
            wormholePlayers.put(player.getName(), new WormholePlayer(player));
        }
    }

    public static boolean isRegistered(Player player) {
        return isRegistered(player.getName());
    }

    public static boolean isRegistered(String playerName) {
        try {
            if ("".equals(playerName)) {
                throw new WormholePlayerEmptyPlayerNameException("playerName can't be empty.");
            }
            if (!wormholePlayers.containsKey(playerName)) {
                WXTLogger.prettyLog(Level.FINE, false, "'" + playerName + "' was not registered");
                return false;
            }
            return true;
        } catch (WormholePlayerEmptyPlayerNameException e) {
            WXTLogger.prettyLog(Level.SEVERE, true, e.getMessage());
            return true;
        }
    }

    public static void unregisterPlayer(Player player) {
        unregisterPlayer(player.getName());
    }

    public static void unregisterPlayer(String playerName) {
        if (!isRegistered(playerName)) {
            return;
        }
        WXTLogger.prettyLog(Level.FINE, false, "Unregistering WormholePlayer '" + playerName + "'");
        wormholePlayers.get(playerName).resetPlayer();
        wormholePlayers.remove(playerName);
    }

    public static void unregisterAllPlayers() {
        WXTLogger.prettyLog(Level.FINE, false, "Unregistering all WormholePlayers.");
        wormholePlayers.clear();
    }

    public static void registerAllOnlinePlayers() {
        WXTLogger.prettyLog(Level.FINE, false, "Registering all online players as WormholePlayers.");
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            registerPlayer(player);
        }
    }

    public static HashMap<String, WormholePlayer> getAllRegisteredPlayers() {
        return (HashMap) wormholePlayers;
    }

    public static WormholePlayer getRegisteredWormholePlayer(Player player) {
        return getRegisteredWormholePlayer(player.getName());
    }

    public static WormholePlayer getRegisteredWormholePlayer(String playerName) {
        if (isRegistered(playerName)) {
            return wormholePlayers.get(playerName);
        }
        return null;
    }

    public static WormholePlayer findPlayerByGateName(String gateName) {
        for (String pl : wormholePlayers.keySet()) {
            for (Stargate s : wormholePlayers.get(pl).getStargates()) {
                if (s.getGateName().equalsIgnoreCase(gateName)) {
                    return wormholePlayers.get(pl);
                }
            }
        }
        return null;
    }
}
