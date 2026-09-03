package de.luricos.bukkit.WormholeXTreme.Wormhole.model;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Decides which database the plugin talks to and opens connections to it.
 *
 * Everything that needs a connection goes through here, so the choice of
 * backend is made in one place rather than being spelled out as a literal JDBC
 * URL wherever a connection happens to be needed.
 */
public final class StargateDBConnector {

    public enum Dialect {
        SQLITE,
        MYSQL
    }

    /** Where the SQLite file lives. Unchanged from before, so nothing moves. */
    public static final String SQLITE_FILE =
            "./plugins/WormholeXTreme/WormholeXTremeDB/WormholeXTreme.sqlite";

    private static Dialect resolvedDialect = null;

    private StargateDBConnector() {
    }

    /** The configured backend, defaulting to SQLite for anything unrecognised. */
    public static Dialect getDialect() {
        if (resolvedDialect != null) {
            return resolvedDialect;
        }
        String configured = ConfigManager.getDatabaseType();
        if (configured != null) {
            String value = configured.trim().toLowerCase();
            if (value.equals("mysql") || value.equals("mariadb")) {
                resolvedDialect = Dialect.MYSQL;
                return resolvedDialect;
            }
            if (!value.isEmpty() && !value.equals("sqlite")) {
                WXTLogger.prettyLog(Level.WARNING, false, "Unknown database type '" + configured
                        + "'. Known values are sqlite, mysql and mariadb. Using sqlite.");
            }
        }
        resolvedDialect = Dialect.SQLITE;
        return resolvedDialect;
    }

    /** Forgets the cached choice, so a config reload can change backend. */
    public static void resetDialect() {
        resolvedDialect = null;
    }

    public static boolean isMySQL() {
        return getDialect() == Dialect.MYSQL;
    }

    /**
     * Which set of schema files to run. The two dialects need different DDL:
     * IDENTITY and untyped BINARY are not MySQL syntax, and Key is reserved
     * there.
     */
    public static String getSchemaDirectory() {
        return isMySQL() ? "sql_commands/mysql" : "sql_commands";
    }

    public static Connection open() throws SQLException {
        return isMySQL() ? openMySQL() : openSQLite();
    }

    private static Connection openSQLite() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No SQLite JDBC driver on the classpath.", e);
        }
        File file = new File(SQLITE_FILE);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SQLITE_FILE);
        connection.setAutoCommit(true);
        return connection;
    }

    private static Connection openMySQL() throws SQLException {
        String scheme = loadMySQLDriver();
        String url = "jdbc:" + scheme + "://" + ConfigManager.getDatabaseHost()
                + ":" + ConfigManager.getDatabasePort()
                + "/" + ConfigManager.getDatabaseName();
        Properties properties = new Properties();
        properties.setProperty("user", ConfigManager.getDatabaseUsername());
        properties.setProperty("password", ConfigManager.getDatabasePassword());
        properties.setProperty("useUnicode", "true");
        properties.setProperty("characterEncoding", "UTF-8");
        Connection connection = DriverManager.getConnection(url, properties);
        connection.setAutoCommit(true);
        return connection;
    }

    /**
     * Loads whichever driver is present and returns the URL scheme that goes
     * with it. MariaDB's driver is preferred because it is the one bundled.
     */
    private static String loadMySQLDriver() throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            return "mariadb";
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return "mysql";
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return "mysql";
        } catch (ClassNotFoundException ignored) {
        }
        throw new SQLException("No MariaDB or MySQL JDBC driver on the classpath.");
    }

    /** A short description for the log, with no password in it. */
    public static String describe() {
        if (isMySQL()) {
            return "MySQL/MariaDB " + ConfigManager.getDatabaseHost() + ":"
                    + ConfigManager.getDatabasePort() + "/" + ConfigManager.getDatabaseName();
        }
        return "SQLite " + SQLITE_FILE;
    }
}
