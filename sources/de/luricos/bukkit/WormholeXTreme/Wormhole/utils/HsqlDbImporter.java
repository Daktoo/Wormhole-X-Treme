package de.luricos.bukkit.WormholeXTreme.Wormhole.utils;

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateDBConnector;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

/**
 * Moves an old HSQLDB database into whichever database is configured now.
 *
 * Older versions of the plugin kept stargates in an HSQLDB file alongside the
 * plugin, as WormholeXTremeDB.script and friends. Nothing reads that format any
 * more, so if those files are still sitting there the data in them is
 * invisible. This finds them, copies what they hold into the current database
 * and then renames them out of the way so it only ever happens once.
 *
 * It is a no-op on a server that has no such files, which is the normal case.
 */
public final class HsqlDbImporter {

    private static final String LEGACY_BASE_NAME = "WormholeXTremeDB";
    private static final String[] LEGACY_SUFFIXES =
            {".script", ".properties", ".data", ".log", ".backup", ".lobs"};

    private HsqlDbImporter() {
    }

    private static File legacyDirectory() {
        return new File("plugins/WormholeXTreme/WormholeXTremeDB");
    }

    /** True when an old HSQLDB database is sitting in the plugin folder. */
    public static boolean hasLegacyDatabase() {
        File dir = legacyDirectory();
        return new File(dir, LEGACY_BASE_NAME + ".script").isFile()
                || new File(dir, LEGACY_BASE_NAME + ".properties").isFile();
    }

    /**
     * Copies the old database into the given connection. Rows whose gate name
     * is already present are left alone, so running twice cannot duplicate or
     * overwrite anything.
     *
     * @return true when the import finished and the old files were retired.
     */
    public static boolean importInto(Connection target) {
        if (!hasLegacyDatabase()) {
            return false;
        }
        WXTLogger.prettyLog(Level.INFO, false, "Old HSQLDB database found. Converting it into "
                + StargateDBConnector.describe() + ".");

        if (!loadDriver()) {
            WXTLogger.prettyLog(Level.SEVERE, false, "The HSQLDB driver is not on the classpath, so the old "
                    + "database cannot be read. Leaving it untouched.");
            return false;
        }

        // Work on a copy. These files were written by HSQLDB 1.8 and a modern
        // driver upgrades that format in place as it opens it, so opening the
        // originals directly would rewrite the only copy of the data before we
        // have read a single row.
        File workingCopy = copyToTemporaryDirectory();
        if (workingCopy == null) {
            return false;
        }

        String url = "jdbc:hsqldb:file:" + new File(workingCopy, LEGACY_BASE_NAME).getPath()
                + ";shutdown=true";

        Connection legacy = null;
        try {
            legacy = DriverManager.getConnection(url, "sa", "");
            int gates = copyStargates(legacy, target);
            int perms = copyPermissions(legacy, target);
            WXTLogger.prettyLog(Level.INFO, false, "Converted " + gates + " stargate(s) and "
                    + perms + " permission row(s) from the old database.");
        } catch (SQLException e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Could not read the old HSQLDB database: " + e.getMessage()
                    + ". It has been left in place so nothing is lost.");
            return false;
        } finally {
            if (legacy != null) {
                try {
                    legacy.close();
                } catch (SQLException e) {
                    WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
                }
            }
            deleteRecursively(workingCopy);
        }

        retireLegacyFiles();
        return true;
    }

    /** 2.x renamed the driver class; 1.8 databases may still be paired with the old one. */
    private static boolean loadDriver() {
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    /** Copies every legacy file into a scratch directory, leaving the originals alone. */
    private static File copyToTemporaryDirectory() {
        try {
            File temp = File.createTempFile("wxt-hsqldb", "");
            if (!temp.delete() || !temp.mkdirs()) {
                throw new java.io.IOException("could not create a working directory");
            }
            File dir = legacyDirectory();
            for (String suffix : LEGACY_SUFFIXES) {
                File source = new File(dir, LEGACY_BASE_NAME + suffix);
                if (source.isFile()) {
                    java.nio.file.Files.copy(source.toPath(),
                            new File(temp, LEGACY_BASE_NAME + suffix).toPath());
                }
            }
            return temp;
        } catch (java.io.IOException e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Could not copy the old database aside for conversion: "
                    + e.getMessage() + ". Leaving it untouched.");
            return null;
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static int copyStargates(Connection legacy, Connection target) throws SQLException {
        Set<String> existing = new HashSet<String>();
        ResultSet have = target.prepareStatement("SELECT Name FROM Stargates;").executeQuery();
        while (have.next()) {
            existing.add(have.getString("Name"));
        }
        have.close();

        ResultSet rows;
        try {
            rows = legacy.prepareStatement("SELECT * FROM Stargates;").executeQuery();
        } catch (SQLException e) {
            WXTLogger.prettyLog(Level.WARNING, false, "The old database has no Stargates table: " + e.getMessage());
            return 0;
        }
        Set<String> columns = columnsOf(rows);

        PreparedStatement insert = target.prepareStatement(
                "INSERT INTO Stargates (Name, GateData, Network, World, WorldName, WorldEnvironment, Owner, GateShape, VisitCount)"
                + " VALUES ( ? , ? , ? , ? , ? , ? , ? , ? , ? );");
        int copied = 0;
        while (rows.next()) {
            String name = rows.getString("Name");
            if (name == null || existing.contains(name)) {
                continue;
            }
            insert.setString(1, name);
            insert.setBytes(2, rows.getBytes("GateData"));
            insert.setString(3, columns.contains("network") ? rows.getString("Network") : "");
            insert.setLong(4, columns.contains("world") ? rows.getLong("World") : 0L);
            insert.setString(5, columns.contains("worldname") ? rows.getString("WorldName") : "");
            insert.setString(6, columns.contains("worldenvironment") ? rows.getString("WorldEnvironment") : "");
            insert.setString(7, columns.contains("owner") ? rows.getString("Owner") : null);
            insert.setString(8, columns.contains("gateshape") ? rows.getString("GateShape") : "");
            insert.setInt(9, columns.contains("visitcount") ? rows.getInt("VisitCount") : 0);
            insert.executeUpdate();
            copied++;
        }
        rows.close();
        insert.close();
        return copied;
    }

    private static int copyPermissions(Connection legacy, Connection target) throws SQLException {
        Set<String> existing = new HashSet<String>();
        ResultSet have = target.prepareStatement("SELECT PlayerName FROM StargateIndividualPermissions;").executeQuery();
        while (have.next()) {
            existing.add(have.getString("PlayerName"));
        }
        have.close();

        ResultSet rows;
        try {
            rows = legacy.prepareStatement("SELECT PlayerName, Permission FROM StargateIndividualPermissions;").executeQuery();
        } catch (SQLException e) {
            return 0;
        }
        PreparedStatement insert = target.prepareStatement(
                "INSERT INTO StargateIndividualPermissions ( PlayerName, Permission ) VALUES ( ? , ? );");
        int copied = 0;
        while (rows.next()) {
            String player = rows.getString("PlayerName");
            if (player == null || existing.contains(player)) {
                continue;
            }
            insert.setString(1, player);
            insert.setString(2, rows.getString("Permission"));
            insert.executeUpdate();
            copied++;
        }
        rows.close();
        insert.close();
        return copied;
    }

    /** Lower-cased column names, so an older schema missing columns is tolerated. */
    private static Set<String> columnsOf(ResultSet rows) throws SQLException {
        Set<String> names = new HashSet<String>();
        ResultSetMetaData meta = rows.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            names.add(meta.getColumnLabel(i).toLowerCase(Locale.ROOT));
        }
        return names;
    }

    /**
     * Renames rather than deletes. If the conversion turns out to have gone
     * wrong, the original is still there to go back to.
     */
    private static void retireLegacyFiles() {
        File dir = legacyDirectory();
        for (String suffix : LEGACY_SUFFIXES) {
            File file = new File(dir, LEGACY_BASE_NAME + suffix);
            if (!file.isFile()) {
                continue;
            }
            File retired = new File(dir, LEGACY_BASE_NAME + suffix + ".converted");
            if (!file.renameTo(retired)) {
                WXTLogger.prettyLog(Level.WARNING, false, "Converted the old database but could not rename "
                        + file.getName() + ". Move it by hand or it will be converted again next start.");
            }
        }
        WXTLogger.prettyLog(Level.INFO, false, "The old database files have been renamed with a .converted suffix. "
                + "Delete them once you are happy everything came across.");
    }
}
