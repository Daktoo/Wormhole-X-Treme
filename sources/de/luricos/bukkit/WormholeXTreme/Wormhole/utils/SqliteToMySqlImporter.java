package de.luricos.bukkit.WormholeXTreme.Wormhole.utils;

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateDBConnector;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

/**
 * Copies the contents of the old WormholeXTreme.sqlite file into a
 * MySQL/MariaDB database.
 *
 * This is the same idea as {@link HsqlDbImporter}, one generation later: the
 * data is readable, it is simply sitting in the wrong backend. Rows whose key
 * already exists in the target are left alone, so the import is safe to run
 * more than once and can never overwrite something already there.
 *
 * The SQLite file is never written to. Nothing is deleted or renamed either,
 * so if the move turns out badly the original is still exactly where it was.
 */
public final class SqliteToMySqlImporter {

    /** What came across, so the caller can report it without re-counting. */
    public static final class Result {

        public final int gatesCopied;
        public final int gatesSkipped;
        public final int individualPermissions;
        public final int groupPermissions;
        public final int configurations;
        /** Gates left behind because their name collided in the target. */
        public final List<String> collidedGateNames;

        private Result(int gatesCopied, int gatesSkipped, int individualPermissions,
                int groupPermissions, int configurations, List<String> collidedGateNames) {
            this.gatesCopied = gatesCopied;
            this.gatesSkipped = gatesSkipped;
            this.individualPermissions = individualPermissions;
            this.groupPermissions = groupPermissions;
            this.configurations = configurations;
            this.collidedGateNames = collidedGateNames;
        }
    }

    /** Raised with a message that is fit to show a player as-is. */
    public static final class ImportException extends Exception {

        private static final long serialVersionUID = 1L;

        public ImportException(String message) {
            super(message);
        }

        public ImportException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private SqliteToMySqlImporter() {
    }

    /** True when there is an old SQLite database to read. */
    public static boolean hasSqliteDatabase() {
        return sqliteFile().isFile();
    }

    public static File sqliteFile() {
        return new File(StargateDBConnector.SQLITE_FILE);
    }

    /**
     * Reads every table out of the SQLite file and writes it into the
     * configured MySQL/MariaDB database.
     *
     * @param target an open connection to the MySQL/MariaDB database. The
     *               caller owns it and is responsible for closing it.
     */
    public static Result importInto(Connection target) throws ImportException {
        File source = sqliteFile();
        if (!source.isFile()) {
            throw new ImportException("No SQLite database found at " + source.getPath() + ".");
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ImportException("No SQLite JDBC driver on the classpath, so the old database cannot be read.", e);
        }

        requireTable(target, "Stargates");

        Connection sqlite = null;
        try {
            // Only SELECTs are issued below. setReadOnly is deliberately not
            // called: the SQLite driver refuses it once a connection is open.
            sqlite = DriverManager.getConnection("jdbc:sqlite:" + source.getPath());

            boolean restoreAutoCommit = target.getAutoCommit();
            target.setAutoCommit(false);
            try {
                List<String> collided = new ArrayList<String>();
                int[] gates = copyStargates(sqlite, target, collided);
                int indv = copyTwoColumnTable(sqlite, target,
                        "StargateIndividualPermissions", "PlayerName", "Permission");
                int group = copyTwoColumnTable(sqlite, target,
                        "StargateGroupPermissions", "GroupName", "Permission");
                int config = copyConfigurations(sqlite, target);
                target.commit();

                Result result = new Result(gates[0], gates[1], indv, group, config, collided);
                WXTLogger.prettyLog(Level.INFO, false, "[wxconvertdb] SQLite to MySQL import finished. Gates copied="
                        + result.gatesCopied + " skipped=" + result.gatesSkipped
                        + " individualPerms=" + result.individualPermissions
                        + " groupPerms=" + result.groupPermissions
                        + " configurations=" + result.configurations);
                return result;
            } catch (SQLException e) {
                safeRollback(target);
                throw new ImportException("Import failed and was rolled back: " + e.getMessage(), e);
            } finally {
                try {
                    target.setAutoCommit(restoreAutoCommit);
                } catch (SQLException e) {
                    WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new ImportException("Could not read the SQLite database: " + e.getMessage(), e);
        } finally {
            if (sqlite != null) {
                try {
                    sqlite.close();
                } catch (SQLException e) {
                    WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
                }
            }
        }
    }

    /**
     * The MySQL schema is built at startup, so a missing table means the
     * plugin has not yet been started against this database.
     */
    private static void requireTable(Connection target, String table) throws ImportException {
        try {
            target.prepareStatement("SELECT 1 FROM " + table + " LIMIT 1;").executeQuery().close();
        } catch (SQLException e) {
            throw new ImportException("The MySQL database has no " + table + " table yet. Set database type to"
                    + " mysql in config.yml and restart the server once so the tables are created, then run this"
                    + " again.");
        }
    }

    private static void safeRollback(Connection target) {
        try {
            target.rollback();
        } catch (SQLException e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "[wxconvertdb] Rollback failed: " + e.getMessage());
        }
    }

    /**
     * @param collided filled with the names of gates the target would not
     *                 accept, so the caller can list them for the admin.
     * @return {copied, skipped}
     */
    private static int[] copyStargates(Connection sqlite, Connection target, List<String> collided)
            throws SQLException {
        // Exact matches only. Whether two names that differ by case collide is
        // the target's business, not ours: it depends on the collation of the
        // Name column, which changed at schema version 8. Guessing here would
        // wrongly skip gates on a case-sensitive column, so the savepoint below
        // asks the database instead of second-guessing it.
        Set<String> existing = new HashSet<String>();
        ResultSet have = target.prepareStatement("SELECT Name FROM Stargates;").executeQuery();
        while (have.next()) {
            existing.add(have.getString("Name"));
        }
        have.close();

        ResultSet rows;
        try {
            rows = sqlite.prepareStatement("SELECT * FROM Stargates;").executeQuery();
        } catch (SQLException e) {
            WXTLogger.prettyLog(Level.WARNING, false, "[wxconvertdb] The SQLite database has no Stargates table: "
                    + e.getMessage());
            return new int[] {0, 0};
        }
        Set<String> columns = columnsOf(rows);

        // Gate ids are not just row numbers. Dial targets and sign targets are
        // stored inside GateData as the id of the gate they point at, so
        // letting AUTO_INCREMENT hand out fresh ones silently breaks every
        // linked sign on the server. Ids are carried across verbatim whenever
        // the target is empty, which is the case for a first-time migration.
        boolean preserveIds = existing.isEmpty() && columns.contains("id");
        if (!preserveIds && columns.contains("id")) {
            WXTLogger.prettyLog(Level.WARNING, false, "[wxconvertdb] Target already holds gates, so original"
                    + " gate ids cannot be preserved. Dial signs on imported gates may need re-pointing.");
        }

        PreparedStatement insert = target.prepareStatement(preserveIds
                ? "INSERT INTO Stargates (Id, Name, GateData, Network, World, WorldName, WorldEnvironment,"
                        + " Owner, GateShape, VisitCount) VALUES ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? );"
                : "INSERT INTO Stargates (Name, GateData, Network, World, WorldName, WorldEnvironment, Owner,"
                        + " GateShape, VisitCount) VALUES ( ? , ? , ? , ? , ? , ? , ? , ? , ? );");
        int copied = 0;
        int skipped = 0;
        try {
            while (rows.next()) {
                String name = rows.getString("Name");
                if (name == null) {
                    skipped++;
                    continue;
                }
                if (existing.contains(name)) {
                    WXTLogger.prettyLog(Level.FINE, false, "[wxconvertdb] Gate '" + name
                            + "' is already in the target database, leaving it alone.");
                    skipped++;
                    continue;
                }
                int c = 0;
                if (preserveIds) {
                    insert.setInt(++c, rows.getInt("Id"));
                }
                insert.setString(++c, name);
                insert.setBytes(++c, rows.getBytes("GateData"));
                insert.setString(++c, columns.contains("network") ? rows.getString("Network") : "");
                insert.setLong(++c, columns.contains("world") ? rows.getLong("World") : 0L);
                insert.setString(++c, columns.contains("worldname") ? rows.getString("WorldName") : "");
                insert.setString(++c, columns.contains("worldenvironment")
                        ? rows.getString("WorldEnvironment") : "");
                insert.setString(++c, columns.contains("owner") ? rows.getString("Owner") : null);
                insert.setString(++c, columns.contains("gateshape") ? rows.getString("GateShape") : "");
                insert.setInt(++c, columns.contains("visitcount") ? rows.getInt("VisitCount") : 0);

                // A savepoint per row, so any constraint we did not anticipate
                // costs one gate rather than the whole import.
                Savepoint point = target.setSavepoint();
                try {
                    insert.executeUpdate();
                    target.releaseSavepoint(point);
                    existing.add(name);
                    copied++;
                } catch (SQLIntegrityConstraintViolationException e) {
                    target.rollback(point);
                    WXTLogger.prettyLog(Level.WARNING, false, "[wxconvertdb] Target refused gate '" + name
                            + "': " + e.getMessage());
                    collided.add(name);
                    skipped++;
                }
            }
        } finally {
            rows.close();
            insert.close();
        }
        return new int[] {copied, skipped};
    }

    /**
     * The two permission tables have the same shape, so one routine covers
     * both. A missing table in the source is not an error: older databases
     * simply may not have it.
     */
    private static int copyTwoColumnTable(Connection sqlite, Connection target, String table,
            String keyColumn, String valueColumn) throws SQLException {
        if (!tableReadable(target, table)) {
            WXTLogger.prettyLog(Level.WARNING, false, "[wxconvertdb] Target has no " + table
                    + " table, skipping it.");
            return 0;
        }

        Set<String> existing = new HashSet<String>();
        ResultSet have = target.prepareStatement("SELECT " + keyColumn + " FROM " + table + ";").executeQuery();
        while (have.next()) {
            existing.add(have.getString(keyColumn));
        }
        have.close();

        ResultSet rows;
        try {
            rows = sqlite.prepareStatement("SELECT " + keyColumn + ", " + valueColumn
                    + " FROM " + table + ";").executeQuery();
        } catch (SQLException e) {
            return 0;
        }

        PreparedStatement insert = target.prepareStatement("INSERT INTO " + table
                + " ( " + keyColumn + ", " + valueColumn + " ) VALUES ( ? , ? );");
        int copied = 0;
        try {
            while (rows.next()) {
                String key = rows.getString(keyColumn);
                if (key == null || existing.contains(key)) {
                    continue;
                }
                insert.setString(1, key);
                insert.setString(2, rows.getString(valueColumn));

                Savepoint point = target.setSavepoint();
                try {
                    insert.executeUpdate();
                    target.releaseSavepoint(point);
                    existing.add(key);
                    copied++;
                } catch (SQLIntegrityConstraintViolationException e) {
                    target.rollback(point);
                    WXTLogger.prettyLog(Level.WARNING, false, "[wxconvertdb] Target refused " + table
                            + " row '" + key + "': " + e.getMessage());
                }
            }
        } finally {
            rows.close();
            insert.close();
        }
        return copied;
    }

    /** Key is a reserved word in MySQL, so it has to be quoted on that side. */
    private static int copyConfigurations(Connection sqlite, Connection target) throws SQLException {
        if (!tableReadable(target, "Configurations")) {
            return 0;
        }

        Set<String> existing = new HashSet<String>();
        ResultSet have = target.prepareStatement("SELECT `Key` FROM Configurations;").executeQuery();
        while (have.next()) {
            existing.add(have.getString(1));
        }
        have.close();

        ResultSet rows;
        try {
            rows = sqlite.prepareStatement("SELECT \"Key\", Value FROM Configurations;").executeQuery();
        } catch (SQLException e) {
            return 0;
        }

        PreparedStatement insert = target.prepareStatement(
                "INSERT INTO Configurations ( `Key`, Value ) VALUES ( ? , ? );");
        int copied = 0;
        try {
            while (rows.next()) {
                String key = rows.getString(1);
                if (key == null || existing.contains(key)) {
                    continue;
                }
                insert.setString(1, key);
                insert.setString(2, rows.getString(2));
                insert.executeUpdate();
                copied++;
            }
        } finally {
            rows.close();
            insert.close();
        }
        return copied;
    }

    private static boolean tableReadable(Connection connection, String table) {
        try {
            connection.prepareStatement("SELECT 1 FROM " + table + " LIMIT 1;").executeQuery().close();
            return true;
        } catch (SQLException e) {
            return false;
        }
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
}
