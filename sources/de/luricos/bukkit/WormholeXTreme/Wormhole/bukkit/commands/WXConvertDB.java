package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.events.StargateCreatedEvent;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateHelper;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateDBManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateDBConnector;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate3DShape;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateShape;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.OfflineGateBuilder;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.SqliteToMySqlImporter;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class WXConvertDB implements CommandExecutor, TabCompleter {

    private static final String NXT_FILE_NAME = "stargatesList.json";
    private static final String DB_DIR = "plugins" + File.separator
            + "WormholeXTreme" + File.separator + "WormholeXTremeDB";

    private static final String SUB_NXT = "nxt";
    private static final String SUB_SQLITE = "sqlite";
    private static final String FLAG_NO_GENERATE = "nogenerate";

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> subs = new ArrayList<>();
            for (String sub : Arrays.asList(SUB_NXT, SUB_SQLITE)) {
                if (sub.startsWith(prefix)) {
                    subs.add(sub);
                }
            }
            return subs;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase(SUB_NXT)
                || args[0].equalsIgnoreCase("json") || args[0].equalsIgnoreCase("novyxtreme"))
                && FLAG_NO_GENERATE.startsWith(args[1].toLowerCase())) {
            return Collections.singletonList(FLAG_NO_GENERATE);
        }
        return Collections.emptyList();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (CommandUtilities.playerCheck(sender)
                && !WXPermissions.checkPermission((Player) sender, WXPermissions.PermissionType.CONFIG)) {
            sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            return true;
        }

        String sub = args.length == 0 ? SUB_NXT : args[0].toLowerCase();

        if (sub.equals(SUB_NXT) || sub.equals("json") || sub.equals("novyxtreme")) {
            boolean noGenerate = false;
            for (int i = 1; i < args.length; i++) {
                String flag = args[i].toLowerCase();
                if (flag.equals(FLAG_NO_GENERATE) || flag.equals("nogen") || flag.equals("-n")) {
                    noGenerate = true;
                } else {
                    sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                            + "Unknown option '" + args[i] + "'. The only flag here is "
                            + FLAG_NO_GENERATE + ".");
                    return true;
                }
            }
            doConvert(sender, noGenerate);
            return true;
        }
        if (sub.equals(SUB_SQLITE) || sub.equals("mysql") || sub.equals("mariadb")) {
            doSqliteToMySQL(sender);
            return true;
        }

        sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                + "Unknown option '" + args[0] + "'. Use /" + label + " nxt or /" + label + " sqlite.");
        return true;
    }

    /**
     * Copies the old SQLite database into the configured MySQL/MariaDB one.
     */
    private static void doSqliteToMySQL(CommandSender sender) {
        if (!StargateDBConnector.isMySQL()) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "This server is still running on SQLite.");
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "Change the database type to MySQL in config.yml, then retry.");
            return;
        }

        if (!SqliteToMySqlImporter.hasSqliteDatabase()) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "No SQLite database found at " + SqliteToMySqlImporter.sqliteFile().getPath() + ".");
            return;
        }

        sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                + "Reading " + SqliteToMySqlImporter.sqliteFile().getPath() + "...");
        sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                + "Writing into " + StargateDBConnector.describe() + ".");

        Connection target = null;
        try {
            target = StargateDBConnector.openMySQLDirect();
            SqliteToMySqlImporter.Result result = SqliteToMySqlImporter.importInto(target);

            sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Conversion complete \u00a73::");
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "\u00a72Gates copied: " + result.gatesCopied
                    + "  \u00a78|  \u00a7eGates already present: " + result.gatesSkipped);
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "\u00a77Player permissions: \u00a7b" + result.individualPermissions
                    + "\u00a77, group permissions: \u00a7b" + result.groupPermissions
                    + "\u00a77, settings rows: \u00a7b" + result.configurations);
            if (!result.collidedGateNames.isEmpty()) {
                sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                        + "Left behind (name already taken in MySQL): \u00a78"
                        + String.join("\u00a77, \u00a78", result.collidedGateNames));
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                        + "MySQL's gate names are case-insensitive, rename them in SQLite and run this again.");
            }
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "Run \u00a7e/wxreload\u00a77 to load the gates from MySQL");
        } catch (SqliteToMySqlImporter.ImportException e) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader + e.getMessage());
            WXTLogger.prettyLog(Level.SEVERE, false, "[wxconvertdb] SQLite import failed: " + e.getMessage());
        } catch (SQLException e) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "Could not connect to MySQL/MariaDB: " + e.getMessage());
            WXTLogger.prettyLog(Level.SEVERE, false, "[wxconvertdb] MySQL connection failed: " + e.getMessage());
        } finally {
            if (target != null) {
                try {
                    target.close();
                } catch (SQLException e) {
                    WXTLogger.prettyLog(Level.FINE, false, e.getMessage());
                }
            }
        }
    }

    /**
     * @param noGenerate when true, a gate sitting in terrain that has never
     *                   been generated is reported and skipped instead of
     *                   forcing the server to generate that chunk mid-command.
     */
    private static void doConvert(CommandSender sender, boolean noGenerate) {
        File nxtFile = new File(DB_DIR + File.separator + NXT_FILE_NAME);
        if (!nxtFile.exists()) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "NovyXtreme database file not found. Place '"
                    + NXT_FILE_NAME + "' inside plugins/WormholeXTreme/WormholeXTremeDB/ and try again.");
            return;
        }

        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Reading NovyXtreme database...");

        JsonArray gates;
        try (Reader reader = Files.newBufferedReader(nxtFile.toPath(), StandardCharsets.UTF_8)) {
            gates = JsonParser.parseReader(reader).getAsJsonArray();
        } catch (Exception e) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "Failed to parse NovyXtreme JSON: " + e.getMessage());
            WXTLogger.prettyLog(Level.SEVERE, false, "[wxconvertdb] JSON parse error: " + e.getMessage());
            return;
        }

        StargateShape standardShape = StargateHelper.getStargateShape("Standard");
        if (standardShape == null) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader + "Standard gate shape not found. Make sure WormholeXTreme has loaded its shapes.");
            return;
        }

        sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                + "Found " + gates.size() + " gate(s) to convert. Starting...");

        if (noGenerate) {
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "\u00A77Running with gates in unexplored terrain will be listed and left for a later run.");
            runOfflineConversion(sender, gates, standardShape);
            return;
        }

        runOnlineConversion(sender, gates, standardShape);
    }

    private static void runOnlineConversion(CommandSender sender, JsonArray gates,
            StargateShape standardShape) {
        final List<PendingGate> pending = new ArrayList<>();
        final List<String> unreadable = new ArrayList<>();

        for (int i = 0; i < gates.size(); i++) {
            JsonObject obj = gates.get(i).getAsJsonObject();
            PendingGate g = new PendingGate();
            try {
                g.name = obj.get("name").getAsString();
                g.owner = resolveOwnerName(obj.get("owner").getAsString());
                g.facing = BlockFace.valueOf(obj.get("facing").getAsString().toUpperCase());
                JsonObject lever = obj.get("leverBlock").getAsJsonObject();
                g.world = Bukkit.getWorld(lever.get("world").getAsString());
                g.x = (int) Math.floor(lever.get("x").getAsDouble());
                g.y = (int) Math.floor(lever.get("y").getAsDouble());
                g.z = (int) Math.floor(lever.get("z").getAsDouble());
                g.visits = obj.has("timesVisited") ? obj.get("timesVisited").getAsInt() : 0;
            } catch (Exception e) {
                unreadable.add(obj.has("name") ? obj.get("name").getAsString() : "(unnamed)");
                continue;
            }
            if (g.world == null) {
                unreadable.add(g.name);
                continue;
            }
            g.chunkKey = (((long) (g.x >> 4)) << 32) ^ ((g.z >> 4) & 0xffffffffL);
            pending.add(g);
        }
        pending.sort((a, b) -> Long.compare(a.chunkKey, b.chunkKey));
        if (!unreadable.isEmpty()) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader + "Unreadable entries: \u00a78" + String.join("\u00a77, \u00a78", unreadable));
        }
        final int total = pending.size();
        new BukkitRunnable() {
            private int index = 0;
            private int converted = 0;
            private int skipped = 0;
            private final List<String> skippedNames = new ArrayList<>();
            @Override
            public void run() {
                int handled = 0;
                while (index < total && handled < ONLINE_BATCH_SIZE) {
                    PendingGate g = pending.get(index++);
                    handled++;
                    if (StargateManager.isStargate(g.name)) {
                        skipped++;
                        skippedNames.add(g.name + " (already in WXT)");
                        continue;
                    }
                    Location leverLoc = new Location(g.world, g.x, g.y, g.z);
                    buildNxtGateStructure(g.world, leverLoc, g.facing);
                    Stargate s = StargateHelper.checkStargate(leverLoc.getBlock(), g.facing, standardShape);
                    if (s == null) {
                        skipped++;
                        skippedNames.add(g.name + " (shape not detectable)");
                        continue;
                    }
                    s.setGateName(g.name);
                    s.setGateOwner(g.owner);
                    s.setGateShape(standardShape);
                    s.setVisitCount(g.visits);
                    s.setGateFacing(g.facing);
                    StargateManager.addGateToNetwork(s, "Public");
                    s.setGateNetwork(StargateManager.getStargateNetwork("Public"));
                    StargateManager.addStargate(s, StargateCreatedEvent.Cause.IMPORTED);
                    indexGateBlocks(s, g.world);
                    s.toggleDialLeverState(true);
                    s.setupGateSign(true);
                    StargateDBManager.stargateToSQL(s);
                    converted++;
                    if (converted % OFFLINE_PROGRESS_EVERY == 0) {
                        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "  \u00a78" + index + "/" + total + "  \u00a77processed, \u00a72" + converted + "\u00a77 converted.");
                    }
                }
                if (index >= total) {
                    sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Conversion complete \u00a73::");
                    sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "\u00a72Converted: " + converted + "  \u00a78|  \u00a7eSkipped: " + skipped);
                    if (!skippedNames.isEmpty()) {
                        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Skipped gates: \u00a78" + String.join("\u00a77, \u00a78", skippedNames));
                    }
                    WXTLogger.prettyLog(Level.INFO, false,
                                        "[wxconvertdb] Conversion finished. Converted=" + converted + " Skipped=" + skipped + " Unreadable=" + unreadable.size());
                    cancel();
                }
            }
        }.runTaskTimer(WormholeXTreme.getThisPlugin(), 1L, 1L);
    }

    /** How many gates are handled per tick. Small enough to stay invisible. */
    private static final int ONLINE_BATCH_SIZE = 4;
    private static final int OFFLINE_BATCH_SIZE = 10;
    /** How often to report progress, in gates. */
    private static final int OFFLINE_PROGRESS_EVERY = 50;

    /** One parsed entry, resolved enough to sort before any world access. */
    private static final class PendingGate {
        String name;
        String owner;
        BlockFace facing;
        World world;
        int x;
        int y;
        int z;
        int visits;
        long chunkKey;
    }

    /**
     * Records gates that already exist in the world, spread across ticks.
     *
     * Nothing is built and no terrain is generated. Gates are sorted by chunk
     * first so that stacked and neighbouring gates share a load instead of
     * pulling the same region file back off disk repeatedly, and the work runs
     * a few gates per tick so the server stays responsive throughout.
     */
    private static void runOfflineConversion(CommandSender sender, JsonArray gates,
            StargateShape shape) {

        if (!(shape instanceof Stargate3DShape)) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "The Standard shape is not a layered (Version 2) shape, so offline conversion"
                    + " cannot work out its geometry.");
            return;
        }
        final Stargate3DShape shape3D = (Stargate3DShape) shape;

        final List<PendingGate> pending = new ArrayList<>();
        final List<String> unreadable = new ArrayList<>();

        for (int i = 0; i < gates.size(); i++) {
            JsonObject obj = gates.get(i).getAsJsonObject();
            PendingGate g = new PendingGate();
            try {
                g.name = obj.get("name").getAsString();
                g.owner = resolveOwnerName(obj.get("owner").getAsString());
                g.facing = BlockFace.valueOf(obj.get("facing").getAsString().toUpperCase());
                JsonObject lever = obj.get("leverBlock").getAsJsonObject();
                g.world = Bukkit.getWorld(lever.get("world").getAsString());
                g.x = (int) Math.floor(lever.get("x").getAsDouble());
                g.y = (int) Math.floor(lever.get("y").getAsDouble());
                g.z = (int) Math.floor(lever.get("z").getAsDouble());
                g.visits = obj.has("timesVisited") ? obj.get("timesVisited").getAsInt() : 0;
            } catch (Exception e) {
                unreadable.add(obj.has("name") ? obj.get("name").getAsString() : "(unnamed)");
                continue;
            }
            if (g.world == null) {
                unreadable.add(g.name);
                continue;
            }
            g.chunkKey = (((long) (g.x >> 4)) << 32) ^ ((g.z >> 4) & 0xffffffffL);
            pending.add(g);
        }

        pending.sort((a, b) -> Long.compare(a.chunkKey, b.chunkKey));

        if (!unreadable.isEmpty()) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "Unreadable entries (bad data or world not loaded): \u00a78"
                    + String.join("\u00a77, \u00a78", unreadable));
        }

        sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                + "\u00a77Recording \u00a7b" + pending.size() + "\u00a77 gate(s) without touching the world."
                + " The server stays up while this runs.");

        new BukkitRunnable() {
            private int index = 0;
            private int converted = 0;
            private int skipped = 0;
            private final List<String> ungeneratedNames = new ArrayList<>();
            private int pillarless = 0;
            private final List<String> rejected = new ArrayList<>();
            private final List<String> obstructed = new ArrayList<>();

            @Override
            public void run() {
                int handled = 0;
                while (index < pending.size() && handled < OFFLINE_BATCH_SIZE) {
                    PendingGate g = pending.get(index++);
                    handled++;

                    if (StargateManager.isStargate(g.name)) {
                        skipped++;
                        continue;
                    }
                    if (!g.world.isChunkGenerated(g.x >> 4, g.z >> 4)) {
                        ungeneratedNames.add(g.name);
                        continue;
                    }

                    OfflineGateBuilder.Result r = OfflineGateBuilder.build(
                            g.world, g.x, g.y, g.z, g.facing, shape3D);

                    if (!r.isAccepted()) {
                        rejected.add(g.name + " (" + r.rejection + ")");
                        continue;
                    }
                    if (r.portalObstructed) {
                        obstructed.add(g.name);
                    }
                    if (r.pillarBlocksMissing > 0) {
                        pillarless++;
                    }

                    Stargate s = r.gate;
                    s.setGateName(g.name);
                    s.setGateOwner(g.owner);
                    s.setVisitCount(g.visits);
                    StargateManager.addGateToNetwork(s, "Public");
                    s.setGateNetwork(StargateManager.getStargateNetwork("Public"));
                    StargateManager.addStargate(s, StargateCreatedEvent.Cause.IMPORTED);
                    indexGateBlocks(s, g.world);
                    StargateDBManager.stargateToSQL(s);
                    converted++;

                    if (converted % OFFLINE_PROGRESS_EVERY == 0) {
                        sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                                + "  \u00a78" + index + "/" + pending.size()
                                + " \u00a77processed, \u00a72" + converted + "\u00a77 recorded.");
                    }
                }

                if (index >= pending.size()) {
                    report(sender, converted, skipped, ungeneratedNames, pillarless, rejected, obstructed);
                    cancel();
                }
            }
        }.runTaskTimer(WormholeXTreme.getThisPlugin(), 1L, 1L);
    }

    private static void indexGateBlocks(Stargate s, World world) {
        for (Location loc : s.getGateStructureBlocks()) {
            StargateManager.addBlockIndex(world.getBlockAt(loc), s);
        }
        for (Location loc : s.getGatePortalBlocks()) {
            StargateManager.addBlockIndex(world.getBlockAt(loc), s);
        }
        for (List<Location> layer : s.getGateLightBlocks()) {
            for (Location loc : layer) {
                StargateManager.addBlockIndex(world.getBlockAt(loc), s);
            }
        }
        for (List<Location> layer : s.getGateWooshBlocks()) {
            for (Location loc : layer) {
                StargateManager.addBlockIndex(world.getBlockAt(loc), s);
            }
        }
    }

    private static void report(CommandSender sender, int converted, int skipped,
                              List<String> ungeneratedNames, int pillarless, List<String> rejected,
                              List<String> obstructed) {
        int ungenerated = ungeneratedNames.size();
        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Offline conversion complete \u00a73::");
        sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                + "\u00a72Recorded: " + converted
                + "  \u00a78|  \u00a7eAlready present: " + skipped
                + "  \u00a78|  \u00a7cRejected: " + rejected.size());
        if (pillarless > 0) {
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "\u00a77" + pillarless + " gate(s) had missing DHD pillar blocks. Recorded as-is;"
                    + " nothing will rebuild them.");
        }
        if (ungenerated > 0) {
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "\u00a77" + ungenerated + " gate(s) sit in unexplored terrain and were left alone: \u00a78" + String.join("\u00a77, \u00a78", ungeneratedNames));
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "\u00a77Visit those areas and convert again.");
        }
        if (!obstructed.isEmpty()) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "Blocked portal interior: \u00a78" + String.join("\u00a77, \u00a78", obstructed));
        }
        if (!rejected.isEmpty()) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "Rejected: \u00a78" + String.join("\u00a77, \u00a78", rejected));
        }
        sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                + "Run \u00a7e/wxreload\u00a77 when you are happy. Re-running skips gates already recorded.");
        WXTLogger.prettyLog(Level.INFO, false, "[wxconvertdb] Offline conversion finished. Recorded="
                + converted + " Skipped=" + skipped + " Rejected=" + rejected.size()
                + " Ungenerated=" + ungenerated);
    }

    private static String resolveOwnerName(String raw) {
        if (raw == null || raw.isEmpty()) return "unknown";
        if (raw.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
            try {
                UUID uuid = UUID.fromString(raw);
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op.getName() != null) return op.getName();
            } catch (Exception ignored) {}
            return raw;
        }
        return raw;
    }

    private static final boolean[][] NXT_SHAPE = {
        {false, false, true,  true,  true,  false, false},
        {false, true,  false, false, false, true,  false},
        {true,  false, false, false, false, false, true },
        {true,  false, false, false, false, false, true },
        {true,  false, false, false, false, false, true },
        {false, true,  false, false, false, true,  false},
        {false, false, true,  true,  true,  false, false}
    };
    private static void buildNxtGateStructure(World world, Location leverLoc, BlockFace facing) {
        int lx = leverLoc.getBlockX();
        int ly = leverLoc.getBlockY();
        int lz = leverLoc.getBlockZ();

        for (int row = 0; row < NXT_SHAPE.length; row++) {
            for (int col = 0; col < NXT_SHAPE[row].length; col++) {
                int wx, wy, wz;
                wy = ly - 1 + row;
                switch (facing) {
                    case NORTH:
                        wx = lx + 5 + col * -1;
                        wz = lz + 4;
                        break;
                    case SOUTH:
                        wx = lx - 5 + col;
                        wz = lz - 4;
                        break;
                    case EAST:
                        wx = lx - 4;
                        wz = lz + 5 + col * -1;
                        break;
                    case WEST:
                        wx = lx + 4;
                        wz = lz - 5 + col;
                        break;
                    default:
                        continue;
                }
                if (NXT_SHAPE[row][col]) {
                    world.getBlockAt(wx, wy, wz).setType(Material.OBSIDIAN);
                }
            }
        }

        leverLoc.getBlock().setType(Material.LEVER);
    }

}
