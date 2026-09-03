package de.luricos.bukkit.WormholeXTreme.Wormhole.utils;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateDBConnector;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.CodeSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/utils/DBUpdateUtil.class */
public class DBUpdateUtil {
    private static Connection sql_con;

    private static int getCountDBFiles() {
        CodeSource src = WormholeXTreme.class.getProtectionDomain().getCodeSource();
        URL jar = src.getLocation();
        int count = 0;
        ZipInputStream zis = null;
        try {
            try {
                zis = new ZipInputStream(jar.openStream());
                while (true) {
                    ZipEntry entry = zis.getNextEntry();
                    if (entry == null) {
                        break;
                    }
                    if (entry.getName().contains(StargateDBConnector.getSchemaDirectory() + "/db_create_")) {
                        count++;
                    }
                }
                zis.close();
                try {
                    zis.close();
                } catch (IOException e) {
                    WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
                }
            } catch (Throwable th) {
                try {
                    zis.close();
                } catch (IOException e2) {
                    WXTLogger.prettyLog(Level.FINE, false, e2.getMessage());
                }
                throw th;
            }
        } catch (IOException e3) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Unable to open jar file to read SQL Update commands: " + e3.getMessage());
            try {
                zis.close();
            } catch (IOException e4) {
                WXTLogger.prettyLog(Level.FINE, false, e4.getMessage());
            }
        }
        return count;
    }

    private static int getCurrentVersion() {
        try {
            java.sql.ResultSet rs = sql_con.prepareStatement(
                "SELECT MAX(Version) FROM VersionInfo;").executeQuery();
            if (rs.next()) {
                int ver = rs.getInt(1);
                rs.close();
                if (ver > 0) {
                    if (ver >= 7 && !columnExists("Stargates", "VisitCount")) {
                        WXTLogger.prettyLog(Level.WARNING, false,
                                "VersionInfo says " + ver
                                + " but VisitCount column is missing — forcing migration from 6.");
                        return 6;
                    }
                    return ver;
                }
            }
            rs.close();
        } catch (Exception ignored) {}
        try {
            sql_con.prepareStatement("SELECT 1 FROM Stargates LIMIT 1;").executeQuery().close();
            if (!columnExists("Stargates", "VisitCount")) {
                WXTLogger.prettyLog(Level.WARNING, false,
                        "Stargates table found but VisitCount column missing — returning version 6.");
                return 6;
            }
            return getCountDBFiles();
        } catch (Exception ignored) {}
        return 0;
    }

    private static boolean columnExists(String table, String column) {
        try {
            java.sql.ResultSet rs = sql_con.prepareStatement(
                    "SELECT " + column + " FROM " + table + " LIMIT 1;").executeQuery();
            rs.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static ArrayList<String> readTextFromJar(String s) {
        InputStream is = null;
        BufferedReader br = null;
        ArrayList<String> list = new ArrayList<>();
        try {
            try {
                is = WormholeXTreme.class.getResourceAsStream(s);
                br = new BufferedReader(new InputStreamReader(is));
                while (true) {
                    String line = br.readLine();
                    if (null == line) {
                        break;
                    }
                    list.add(line);
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (is != null) {
                    is.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                }
                if (is != null) {
                    is.close();
                }
            }
            return list;
        } catch (Throwable th) {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                    throw new RuntimeException(th);
                }
            }
            if (is != null) {
                try { is.close(); } catch (IOException e5) { e5.printStackTrace(); }
            }
            throw new RuntimeException(th);
        }
    }

    public static boolean updateDB() {
        File dir = new File("plugins/WormholeXTremeDB/");
        File dest_dir = new File("plugins/WormholeXTreme/WormholeXTremeDB/");
        File oldFileName = new File(dest_dir.getPath() + File.separator + "WormholeXTremeDB");
        File newFileName = new File(dest_dir.getPath() + File.separator + "WormholeXTreme.sqlite");
        if (!dest_dir.exists()) {
            try {
                dest_dir.mkdir();
            } catch (Exception e) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Unable to make directory: " + e.getMessage());
            }
        }
        if (dir.exists() && dir.isDirectory()) {
            WXTLogger.prettyLog(Level.WARNING, false, "Old Database found, moving directory.");
            File[] files = dir.listFiles();
            for (File f : files) {
                try {
                    f.renameTo(new File(dest_dir, f.getName()));
                } catch (Exception e2) {
                    WXTLogger.prettyLog(Level.SEVERE, false, "Unable to rename files: " + e2.getMessage());
                }
            }
            try {
                dir.delete();
            } catch (Exception e3) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Unable to delete directory: " + e3.getMessage());
                return false;
            }
        }
        if (oldFileName.isFile()) {
            WXTLogger.prettyLog(Level.WARNING, false, "Old Database File found. Performing Update after failsafe check.");
            if (newFileName.isFile()) {
                WXTLogger.prettyLog(Level.SEVERE, false, oldFileName.getName() + " and " + newFileName.getName() + " found both! Deleting failed during update. Please remove the correct file by hand (should be 0 KB).");
                return false;
            }
            try {
                if (!oldFileName.renameTo(newFileName)) {
                    throw new Exception("Check your database directory!");
                }
                WXTLogger.prettyLog(Level.INFO, false, "Successfully moved old Database to new Database.");
            } catch (Exception e4) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Unable to rename or delete oldFile. This is a serious problem! " + e4.getMessage());
                return false;
            }
        }
        if (!StargateDBConnector.isMySQL()) {
            File dbFile = new File(StargateDBConnector.SQLITE_FILE);
            if (dbFile.exists() && dbFile.length() > 0) {
                WXTLogger.prettyLog(Level.INFO, false, "Database already exists; checking for schema updates.");
            }
        }
        try {
            sql_con = StargateDBConnector.open();
            if (!StargateDBConnector.isMySQL()) {
                sql_con.prepareStatement("PRAGMA journal_mode = TRUNCATE;").executeQuery().close();
            }
            int version = getCurrentVersion();
            int count = getCountDBFiles();
            updateDB(version, count);
            return true;
        } catch (SQLException e5) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Could not prepare " + StargateDBConnector.describe()
                    + ": " + e5.getMessage());
            return false;
        }
    }

    private static void updateDB(int version, int count) {
        if (count <= version) {
            WXTLogger.prettyLog(Level.FINE, false, "Database is already up to date.");
            return;
        }
        boolean success = true;
        Statement stmt = null;
        try {
            try {
                stmt = sql_con.createStatement();
                for (int i = version + 1; i <= count; i++) {
                    StringBuilder sb = new StringBuilder();
                    ArrayList<String> lines = readTextFromJar("/" + StargateDBConnector.getSchemaDirectory() + "/db_create_" + i);
                    Iterator<String> it = lines.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        if (it.hasNext()) {
                            String line = it.next();
                            if (!line.startsWith("#") && !line.startsWith("--")) {
                                sb.append(line);
                            }
                            if (line.endsWith(";") && !line.startsWith("#")) {
                                try {
                                    stmt.executeUpdate(sb.toString());
                                } catch (SQLException sql_e) {
                                    int code = sql_e.getErrorCode();
                                    if (code == 1 || code == -27 || code == -21) {
                                        WXTLogger.prettyLog(Level.WARNING, false, "(" + code + ")Continuing after Error:" + sql_e);
                                    } else {
                                        WXTLogger.prettyLog(Level.SEVERE, false, "(" + code + ")Failure On:" + sql_e);
                                        success = false;
                                        try { Thread.sleep(250L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                                    }
                                }
                                sb = new StringBuilder();
                            }
                        }
                    }
                }
                try {
                    stmt.close();
                    sql_con.close();
                } catch (Exception _ignored) {}
                try {
                    stmt.close();
                } catch (SQLException e) {
                    WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
                }
            } catch (Exception e2) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Failed to update db:" + e2);
                try {
                    stmt.close();
                } catch (SQLException e3) {
                    WXTLogger.prettyLog(Level.FINE, false, e3.getMessage());
                }
            }
            if (success) {
                WXTLogger.prettyLog(Level.INFO, false, "Successfully updated database.");
            } else {
                WXTLogger.prettyLog(Level.SEVERE, false, "Failed to update DB.");
            }
        } catch (Throwable th) {
            try {
                stmt.close();
            } catch (SQLException e4) {
                WXTLogger.prettyLog(Level.FINE, false, e4.getMessage());
            }
            throw th;
        }
    }
}