package de.luricos.bukkit.WormholeXTreme.Wormhole.model;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateHelper;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionsManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/model/StargateDBManager.class */
public class StargateDBManager {
    private static volatile PreparedStatement storeStatement;
    private static volatile PreparedStatement updateGateStatement;
    private static volatile PreparedStatement getGateStatement;
    private static volatile PreparedStatement removeStatement;
    private static volatile PreparedStatement incrementVisitStatement = null;
    private static Connection wormholeSQLConnection = null;
    private static volatile PreparedStatement updateIndvPermStatement = null;
    private static volatile PreparedStatement storeIndvPermStatement = null;
    private static volatile PreparedStatement getIndvPermStatement = null;
    private static volatile PreparedStatement getAllIndvPermStatement = null;

    private static void connectDB() {
        try {
            Class.forName("org.sqlite.JDBC");
            try {
                if (wormholeSQLConnection == null || wormholeSQLConnection.isClosed()) {
                    setWormholeSQLConnection(DriverManager.getConnection("jdbc:sqlite:./plugins/WormholeXTreme/WormholeXTremeDB/WormholeXTreme.sqlite", "sa", ""));
                    wormholeSQLConnection.setAutoCommit(true);
                } else {
                    WXTLogger.prettyLog(Level.SEVERE, false, "WormholeDB already connected.");
                }
            } catch (SQLException e) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Failed to intialized internal DB. Stargates will not be saved: " + e.getMessage());
            }
        } catch (Exception e2) {
            WXTLogger.prettyLog(Level.SEVERE, false, "ERROR: failed to load SQLITE JDBC driver.");
            e2.printStackTrace();
        }
    }

    public static boolean isConnected() {
        if (wormholeSQLConnection != null) {
            try {
                if (wormholeSQLConnection.isClosed()) {
                    return false;
                }
                return true;
            } catch (SQLException e) {
                WXTLogger.prettyLog(Level.FINE, false, "DBLink not available.");
                return false;
            }
        }
        return false;
    }

    public static ConcurrentHashMap<String, PermissionsManager.PermissionLevel> getAllIndividualPermissions() {
        ConcurrentHashMap<String, PermissionsManager.PermissionLevel> perms = new ConcurrentHashMap<>();
        if (!isConnected()) {
            connectDB();
        }
        ResultSet perm = null;
        try {
            try {
                if (wormholeSQLConnection.isClosed()) {
                    connectDB();
                }
                if (getAllIndvPermStatement == null) {
                    getAllIndvPermStatement = wormholeSQLConnection.prepareStatement("SELECT PlayerName, Permission FROM StargateIndividualPermissions;");
                }
                perm = getAllIndvPermStatement.executeQuery();
                while (perm.next()) {
                    perms.put(perm.getString("PlayerName"), PermissionsManager.PermissionLevel.valueOf(perm.getString("Permission")));
                }
                try {
                    perm.close();
                } catch (SQLException e) {
                    WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
                }
            } catch (Throwable th) {
                try {
                    perm.close();
                } catch (SQLException e2) {
                    WXTLogger.prettyLog(Level.FINE, false, e2.getMessage());
                }
                throw th;
            }
        } catch (SQLException e3) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Error GetAllIndividualPermissions: " + e3.getMessage());
            e3.printStackTrace();
            try {
                perm.close();
            } catch (SQLException e4) {
                WXTLogger.prettyLog(Level.FINE, false, e4.getMessage());
            }
        }
        return perms;
    }

    public static void loadStargates(Server server) {
        if (!isConnected()) {
            connectDB();
        }
        PreparedStatement stmt = null;
        ResultSet gatesData = null;
        try {
            try {
                if (wormholeSQLConnection.isClosed()) {
                    connectDB();
                }
                stmt = wormholeSQLConnection.prepareStatement("SELECT * FROM Stargates;");
                gatesData = stmt.executeQuery();
                while (gatesData.next()) {
                    String networkName = gatesData.getString("Network");
                    StargateNetwork sn = null;
                    if (networkName != null) {
                        sn = StargateManager.getStargateNetwork(networkName);
                        if (sn == null && !networkName.equals("")) {
                            sn = StargateManager.addStargateNetwork(networkName);
                        }
                    }
                    String worldName = gatesData.getString("WorldName");
                    String worldEnvironment = gatesData.getString("WorldEnvironment");
                    if (!ConfigManager.isWormholeWorldsSupportEnabled()) {
                        server.createWorld(new WorldCreator(worldName).environment(World.Environment.valueOf(worldEnvironment)));
                    } else if (WormholeXTreme.getWorldHandler() != null && WormholeXTreme.getWorldHandler().loadWorld(worldName) == null) {
                        WXTLogger.prettyLog(Level.WARNING, true, "World: " + worldName + " is not a Wormhole World, the suggested action is to add it as one. Otherwise disregard this warning.");
                    }
                    World w = server.getWorld(worldName);
                    Stargate s = StargateHelper.parseVersionedData(gatesData.getBytes("GateData"), w, gatesData.getString("Name"), sn);
                    if (s != null) {
                        s.setGateId(gatesData.getInt("Id"));
                        s.setGateOwner(gatesData.getString("Owner"));
                        String gateShapeName = gatesData.getString("GateShape");
                        if (gateShapeName == null) {
                            gateShapeName = "Standard";
                        }
                        s.setGateShape(StargateHelper.getStargateShape(gateShapeName));
                        try {
                            s.setVisitCount(gatesData.getInt("VisitCount"));
                        } catch (SQLException e) {
                            s.setVisitCount(0);
                        }
                        if (sn != null) {
                            sn.getNetworkGateList().add(s);
                            if (s.isGateSignPowered()) {
                                sn.getNetworkSignGateList().add(s);
                                if (s.getGateDialSign() != null && s.getGateDialSignBlock() != null) {
                                    s.tryClickTeleportSign(s.getGateDialSignBlock());
                                }
                            }
                        }
                        StargateManager.addStargate(s);
                        WXTLogger.prettyLog(Level.FINE, false, "Loading Stargate: '" + s.getGateName() + "', GateFace: '" + s.getGateFacing().name() + "' from DB");
                    } else {
                        WXTLogger.prettyLog(Level.WARNING, true, "Failed to load Stargate '" + sn + "' from DB.");
                    }
                }
                gatesData.close();
                stmt.close();
                ArrayList<Stargate> gateList = StargateManager.getAllGates();
                for (Stargate s2 : gateList) {
                    if (s2.isGateLightsActive() && !s2.isGateActive()) {
                        s2.lightStargate(false);
                    }
                    if (s2.getGateTempTargetId() >= 0) {
                        Iterator<Stargate> it = gateList.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            Stargate t = it.next();
                            if (t.getGateId() == s2.getGateTempTargetId()) {
                                s2.dialStargate(t, true);
                                break;
                            }
                        }
                    }
                    if (s2.getGateTempSignTarget() >= 0) {
                        Iterator<Stargate> it2 = gateList.iterator();
                        while (true) {
                            if (it2.hasNext()) {
                                Stargate t2 = it2.next();
                                if (t2.getGateId() == s2.getGateTempSignTarget()) {
                                    s2.setGateDialSignTarget(t2);
                                    break;
                                }
                            }
                        }
                    }
                }
                WXTLogger.prettyLog(Level.INFO, false, gateList.size() + " Wormholes loaded from WormholeDB.");
                try {
                    gatesData.close();
                } catch (SQLException e) {
                    WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
                }
                try {
                    stmt.close();
                } catch (SQLException e2) {
                    WXTLogger.prettyLog(Level.FINE, false, e2.getMessage());
                }
            } catch (Throwable th) {
                try {
                    gatesData.close();
                } catch (SQLException e3) {
                    WXTLogger.prettyLog(Level.FINE, false, e3.getMessage());
                }
                try {
                    stmt.close();
                } catch (SQLException e4) {
                    WXTLogger.prettyLog(Level.FINE, false, e4.getMessage());
                }
                throw th;
            }
        } catch (SQLException e5) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Error loading stargates from DB: " + e5.getMessage());
            try {
                gatesData.close();
            } catch (SQLException e6) {
                WXTLogger.prettyLog(Level.FINE, false, e6.getMessage());
            }
            try {
                stmt.close();
            } catch (SQLException e7) {
                WXTLogger.prettyLog(Level.FINE, false, e7.getMessage());
            }
        }
    }

    protected static void removeStargateFromSQL(Stargate s) {
        if (!isConnected()) {
            connectDB();
        }
        try {
            if (wormholeSQLConnection.isClosed()) {
                connectDB();
            }
            if (removeStatement == null) {
                removeStatement = wormholeSQLConnection.prepareStatement("DELETE FROM Stargates WHERE name = ?;");
            }
            removeStatement.setString(1, s.getGateName());
            removeStatement.executeUpdate();
        } catch (SQLException e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Error storing stargate to DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void setWormholeSQLConnection(Connection connection) {
        wormholeSQLConnection = connection;
    }

    public static void shutdown() {
        try {
            try {
                if (wormholeSQLConnection != null && !wormholeSQLConnection.isClosed()) {
                    wormholeSQLConnection.close();
                    wormholeSQLConnection = null;
                    WXTLogger.prettyLog(Level.INFO, false, "WormholeDB shutdown successful.");
                }
                if (wormholeSQLConnection == null) {
                    wormholeSQLConnection = null;
                    storeStatement = null;
                    updateGateStatement = null;
                    getGateStatement = null;
                    removeStatement = null;
                    updateIndvPermStatement = null;
                    storeIndvPermStatement = null;
                    getIndvPermStatement = null;
                    getAllIndvPermStatement = null;
                }
            } catch (SQLException e) {
                WXTLogger.prettyLog(Level.SEVERE, false, " Failed to shutdown:" + e.getMessage());
                if (wormholeSQLConnection == null) {
                    wormholeSQLConnection = null;
                    storeStatement = null;
                    updateGateStatement = null;
                    getGateStatement = null;
                    removeStatement = null;
                    updateIndvPermStatement = null;
                    storeIndvPermStatement = null;
                    getIndvPermStatement = null;
                    getAllIndvPermStatement = null;
                }
            }
        } catch (Throwable th) {
            if (wormholeSQLConnection == null) {
                wormholeSQLConnection = null;
                storeStatement = null;
                updateGateStatement = null;
                getGateStatement = null;
                removeStatement = null;
                updateIndvPermStatement = null;
                storeIndvPermStatement = null;
                getIndvPermStatement = null;
                getAllIndvPermStatement = null;
            }
            throw th;
        }
    }

    public static void stargateToSQL(Stargate s) {
        if (!isConnected()) {
            connectDB();
        }
        ResultSet gatesData = null;
        try {
            try {
                if (wormholeSQLConnection.isClosed()) {
                    connectDB();
                }
                if (getGateStatement == null) {
                    getGateStatement = wormholeSQLConnection.prepareStatement("SELECT * FROM Stargates WHERE Name = ?");
                }
                getGateStatement.setString(1, s.getGateName());
                ResultSet gatesData2 = getGateStatement.executeQuery();
                if (gatesData2.next()) {
                    gatesData2.close();
                    if (updateGateStatement == null) {
                        updateGateStatement = wormholeSQLConnection.prepareStatement("UPDATE Stargates SET GateData = ?, Network = ?, World = ?, WorldName = ?, WorldEnvironment = ?, Owner = ?, GateShape = ?, VisitCount = ? WHERE Name = ?");
                    }
                    byte[] data = StargateHelper.stargatetoBinary(s);
                    updateGateStatement.setBytes(1, data);
                    if (s.getGateNetwork() != null) {
                        updateGateStatement.setString(2, s.getGateNetwork().getNetworkName());
                    } else {
                        updateGateStatement.setString(2, "");
                    }
                    updateGateStatement.setLong(3, s.getGateWorld().getUID().getMostSignificantBits());
                    updateGateStatement.setString(4, s.getGateWorld().getName());
                    updateGateStatement.setString(5, s.getGateWorld().getEnvironment().toString());
                    updateGateStatement.setString(6, s.getGateOwner());
                    if (s.getGateShape() == null) {
                        updateGateStatement.setString(7, "Standard");
                    } else {
                        updateGateStatement.setString(7, s.getGateShape().getShapeName());
                    }
                    updateGateStatement.setInt(8, s.getVisitCount());
                    updateGateStatement.setString(9, s.getGateName());
                    updateGateStatement.executeUpdate();
                    WXTLogger.prettyLog(Level.FINE, false, "Saved gate '" + s.getGateName() + "', GateFace: '" + s.getGateFacing().name() + "' to DB");
                } else {
                    gatesData2.close();
                    if (storeStatement == null) {
                        storeStatement = wormholeSQLConnection.prepareStatement("INSERT INTO Stargates(Name, GateData, Network, World, WorldName, WorldEnvironment, Owner, GateShape, VisitCount) VALUES ( ? , ? , ? , ? , ? , ?, ?, ?, ? );");
                    }
                    storeStatement.setString(1, s.getGateName());
                    byte[] data2 = StargateHelper.stargatetoBinary(s);
                    storeStatement.setBytes(2, data2);
                    if (s.getGateNetwork() != null) {
                        storeStatement.setString(3, s.getGateNetwork().getNetworkName());
                    } else {
                        storeStatement.setString(3, "");
                    }
                    storeStatement.setLong(4, s.getGateWorld().getUID().getMostSignificantBits());
                    storeStatement.setString(5, s.getGateWorld().getName());
                    storeStatement.setString(6, s.getGateWorld().getEnvironment().toString());
                    storeStatement.setString(7, s.getGateOwner());
                    storeStatement.setString(8, s.getGateShape().getShapeName());
                    storeStatement.setInt(9, s.getVisitCount());
                    storeStatement.executeUpdate();
                    getGateStatement.setString(1, s.getGateName());
                    gatesData2 = getGateStatement.executeQuery();
                    if (gatesData2.next()) {
                        s.setGateId(gatesData2.getInt("Id"));
                    }
                }
                try {
                    gatesData2.close();
                } catch (SQLException e) {
                    WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
                }
            } catch (Throwable th) {
                try {
                    gatesData.close();
                } catch (SQLException e2) {
                    WXTLogger.prettyLog(Level.FINE, false, e2.getMessage());
                }
                throw th;
            }
        } catch (SQLException e3) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Error storing stargate to DB: " + e3.getMessage());
            e3.printStackTrace();
            try {
                gatesData.close();
            } catch (SQLException e4) {
                WXTLogger.prettyLog(Level.FINE, false, e4.getMessage());
            }
        }
    }

    public static void incrementVisitCount(Stargate s) {
        if (s == null) return;
        s.incrementVisitCount();
        if (!isConnected()) {
            connectDB();
        }
        try {
            if (wormholeSQLConnection.isClosed()) {
                connectDB();
            }
            if (incrementVisitStatement == null) {
                incrementVisitStatement = wormholeSQLConnection.prepareStatement(
                        "UPDATE Stargates SET VisitCount = VisitCount + 1 WHERE Name = ?");
            }
            incrementVisitStatement.setString(1, s.getGateName());
            incrementVisitStatement.executeUpdate();
            WXTLogger.prettyLog(Level.FINE, false,
                    "Incremented visit count for gate '" + s.getGateName()
                    + "' to " + s.getVisitCount());
        } catch (SQLException e) {
            WXTLogger.prettyLog(Level.WARNING, false,
                    "Failed to increment visit count for gate '" + s.getGateName()
                    + "': " + e.getMessage());
        }
    }

    public static void storeIndividualPermissionInDB(String player, PermissionsManager.PermissionLevel pl) {
        if (!isConnected()) {
            connectDB();
        }
        ResultSet perm = null;
        try {
            try {
                if (wormholeSQLConnection.isClosed()) {
                    connectDB();
                }
                if (getIndvPermStatement == null) {
                    getIndvPermStatement = wormholeSQLConnection.prepareStatement("SELECT Permission FROM StargateIndividualPermissions WHERE PlayerName = ?;");
                }
                getIndvPermStatement.setString(1, player);
                perm = getIndvPermStatement.executeQuery();
                if (perm.next()) {
                    if (updateIndvPermStatement == null) {
                        updateIndvPermStatement = wormholeSQLConnection.prepareStatement("UPDATE StargateIndividualPermissions SET Permission = ? WHERE PlayerName = ?;");
                    }
                    updateIndvPermStatement.setString(2, player);
                    updateIndvPermStatement.setString(1, pl.toString());
                    int modified = updateIndvPermStatement.executeUpdate();
                    if (modified != 1) {
                        WXTLogger.prettyLog(Level.SEVERE, false, "Failed to update " + player + " permissions in DB.");
                    }
                } else {
                    if (storeIndvPermStatement == null) {
                        storeIndvPermStatement = wormholeSQLConnection.prepareStatement("INSERT INTO StargateIndividualPermissions ( PlayerName, Permission ) VALUES ( ? , ? );");
                    }
                    storeIndvPermStatement.setString(1, player);
                    storeIndvPermStatement.setString(2, pl.toString());
                    storeIndvPermStatement.executeUpdate();
                }
            } catch (SQLException e) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Error StoreIndividualPermissionInDB : " + e.getMessage());
                e.printStackTrace();
                try {
                    perm.close();
                } catch (SQLException e2) {
                    WXTLogger.prettyLog(Level.FINE, false, e2.getMessage());
                }
            }
        } finally {
            try {
                perm.close();
            } catch (SQLException e3) {
                WXTLogger.prettyLog(Level.FINE, false, e3.getMessage());
            }
        }
    }
}