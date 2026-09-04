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
import java.io.FileReader;
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
import org.bukkit.block.Block;
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

        // No argument keeps doing what it always did, so existing habits and
        // any scripts calling it are not broken by the new subcommands.
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
     *
     * The order matters and the check below enforces it: the plugin has to
     * already be running on MySQL before this is worth doing. Otherwise the
     * rows would be copied across and the server would carry straight on
     * writing new gates to SQLite, and the copy would be stale within minutes.
     */
    private static void doSqliteToMySQL(CommandSender sender) {
        if (!StargateDBConnector.isMySQL()) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "This server is still running on SQLite, so there is nothing to convert into.");
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "Set the database type to \u00a7emysql\u00a77 in config.yml, fill in the host, port, name,"
                    + " username and password, restart the server once so the tables are built, then run"
                    + " \u00a7e/wxconvertdb sqlite\u00a77.");
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
                        + "MySQL treats gate names case-insensitively where SQLite does not, so names that"
                        + " differ only by capitalisation clash. Rename them in SQLite and run this again.");
            }
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "The SQLite file has not been touched. Run \u00a7e/wxreload\u00a77 to load the gates from"
                    + " MySQL, and keep the old file as your backup until you are happy.");
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
        try (FileReader reader = new FileReader(nxtFile)) {
            gates = JsonParser.parseReader(reader).getAsJsonArray();
        } catch (Exception e) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "Failed to parse NovyXtreme JSON: " + e.getMessage());
            WXTLogger.prettyLog(Level.SEVERE, false, "[wxconvertdb] JSON parse error: " + e.getMessage());
            return;
        }

        StargateShape standardShape = StargateHelper.getStargateShape("Standard");
        if (standardShape == null) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "Standard gate shape not found. Make sure WormholeXTreme has loaded its shapes.");
            return;
        }

        int total = gates.size();
        int converted = 0;
        int skipped = 0;
        int ungenerated = 0;
        List<String> skippedNames = new ArrayList<>();
        List<String> ungeneratedNames = new ArrayList<>();

        if (noGenerate) {
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "\u00a77Running with \u00a7e" + FLAG_NO_GENERATE
                    + "\u00a77: gates in unexplored terrain will be listed and left for a later run.");
        }

        sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                + "Found " + total + " gate(s) to convert. Starting...");

        if (noGenerate) {
            // Nothing below this point runs in offline mode: that loop builds
            // and detects gates, which is exactly what the flag exists to
            // avoid. The batched job takes over instead.
            runOfflineConversion(sender, gates, standardShape);
            return;
        }

        for (int i = 0; i < total; i++) {
            JsonObject obj = gates.get(i).getAsJsonObject();

            String name        = obj.get("name").getAsString();
            String ownerRaw    = obj.get("owner").getAsString();
            String facingStr   = obj.get("facing").getAsString();
            JsonObject leverJson = obj.get("leverBlock").getAsJsonObject();
            int timesVisited   = obj.has("timesVisited") ? obj.get("timesVisited").getAsInt() : 0;

            if (StargateManager.isStargate(name)) {
                sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                        + "  §8[" + (i + 1) + "/" + total + "] §7Skipping '§e" + name
                        + "§7' — gate already exists in WXT.");
                skipped++;
                skippedNames.add(name);
                continue;
            }

            String ownerName = resolveOwnerName(ownerRaw);

            World world;
            double lx, ly, lz;
            try {
                String worldName = leverJson.get("world").getAsString();
                world = Bukkit.getWorld(worldName);
                if (world == null) {
                    sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                            + "  [" + (i + 1) + "/" + total + "] Skipping '" + name
                            + "' — world '" + worldName + "' is not loaded.");
                    skipped++;
                    skippedNames.add(name);
                    continue;
                }
                lx = leverJson.get("x").getAsDouble();
                ly = leverJson.get("y").getAsDouble();
                lz = leverJson.get("z").getAsDouble();
            } catch (Exception e) {
                sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                        + "  [" + (i + 1) + "/" + total + "] Skipping '" + name
                        + "' — bad lever block data: " + e.getMessage());
                skipped++;
                skippedNames.add(name);
                continue;
            }

            BlockFace facing;
            try {
                facing = BlockFace.valueOf(facingStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                        + "  [" + (i + 1) + "/" + total + "] Skipping '" + name
                        + "' — unknown facing '" + facingStr + "'.");
                skipped++;
                skippedNames.add(name);
                continue;
            }

            Location leverLoc = new Location(world, lx, ly, lz);

            // Everything past this point touches blocks, and touching a block
            // in ungenerated terrain makes the server generate the chunk right
            // there on the main thread. Across a couple of thousand gates that
            // is what turns this command into a watchdog kill, so the check
            // happens before the first getBlockAt rather than after.
            if (noGenerate && !isAreaGenerated(world, leverLoc)) {
                sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                        + "  [" + (i + 1) + "/" + total + "] Skipping '" + name
                        + "' \u2014 terrain there has never been generated.");
                ungenerated++;
                ungeneratedNames.add(name);
                continue;
            }

            buildNxtGateStructure(world, leverLoc, facing);

            Stargate s = StargateHelper.checkStargate(leverLoc.getBlock(), facing, standardShape);
            if (s == null) {
                sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                        + "  [" + (i + 1) + "/" + total + "] Skipping '" + name
                        + "' — shape not detectable after building. Check the area is clear.");
                skipped++;
                skippedNames.add(name);
                continue;
            }

            s.setGateName(name);
            s.setGateOwner(ownerName);
            s.setGateShape(standardShape);
            s.setVisitCount(timesVisited);
            s.setGateFacing(facing);
            StargateManager.addGateToNetwork(s, "Public");
            s.setGateNetwork(StargateManager.getStargateNetwork("Public"));

            StargateManager.addStargate(s, StargateCreatedEvent.Cause.IMPORTED);
            for (Location loc : s.getGateStructureBlocks()) {
                StargateManager.addBlockIndex(world.getBlockAt(loc), s);
            }
            for (Location loc : s.getGatePortalBlocks()) {
                StargateManager.addBlockIndex(world.getBlockAt(loc), s);
            }
            for (ArrayList<Location> layer : s.getGateLightBlocks()) {
                for (Location loc : layer) {
                    StargateManager.addBlockIndex(world.getBlockAt(loc), s);
                }
            }
            for (ArrayList<Location> layer : s.getGateWooshBlocks()) {
                for (Location loc : layer) {
                    StargateManager.addBlockIndex(world.getBlockAt(loc), s);
                }
            }

            s.toggleDialLeverState(true);
            s.setupGateSign(true);
            StargateDBManager.stargateToSQL(s);

            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "  §8[" + (i + 1) + "/" + total + "] §2Converted §7'§e" + name
                    + "§7' (owner: §b" + ownerName + "§7, visits: §a" + timesVisited + "§7)");
            converted++;
        }

        sender.sendMessage(ConfigManager.MessageStrings.normalHeader + "Conversion complete §3::");
        sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                + "§2Converted: " + converted + "  §8|  §eSkipped: " + skipped);
        if (ungenerated > 0) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader
                    + "Left in unexplored terrain: §8" + String.join("§7, §8", ungeneratedNames));
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "§7Visit those areas (or run without §e" + FLAG_NO_GENERATE
                    + "§7) and convert again — gates already done are skipped automatically.");
        }
        if (!skippedNames.isEmpty()) {
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "Skipped gates: §8" + String.join("§7, §8", skippedNames));
        }

        WXTLogger.prettyLog(Level.INFO, false,
                "[wxconvertdb] Conversion finished. Converted=" + converted + " Skipped=" + skipped
                + " Ungenerated=" + ungenerated);
    }

    /** How many gates are handled per tick. Small enough to stay invisible. */
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
            private int ungenerated = 0;
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
                        ungenerated++;
                        rejected.add(g.name + " (unexplored)");
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
                    report(sender, converted, skipped, ungenerated, pillarless, rejected, obstructed);
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

    private static void report(CommandSender sender, int converted, int skipped, int ungenerated,
            int pillarless, List<String> rejected, List<String> obstructed) {
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
            sender.sendMessage(ConfigManager.MessageStrings.normalHeader
                    + "\u00a77" + ungenerated + " gate(s) sit in unexplored terrain and were left alone.");
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

    /**
     * Whether every chunk the gate structure could touch already exists on
     * disk. The structure reaches a few blocks either side of the lever, so
     * the corners of a 16-block box around it are checked rather than just the
     * lever's own chunk.
     *
     * isChunkGenerated does not load or create anything, which is the whole
     * point: asking the question has to be cheaper than the work it avoids.
     */
    private static boolean isAreaGenerated(World world, Location leverLoc) {
        int x = leverLoc.getBlockX();
        int z = leverLoc.getBlockZ();
        for (int dx = -16; dx <= 16; dx += 16) {
            for (int dz = -16; dz <= 16; dz += 16) {
                if (!world.isChunkGenerated((x + dx) >> 4, (z + dz) >> 4)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void buildNxtGateStructure(World world, Location leverLoc, BlockFace facing) {
        int lx = leverLoc.getBlockX();
        int ly = leverLoc.getBlockY();
        int lz = leverLoc.getBlockZ();

        for (int row = 0; row < NXT_SHAPE.length; row++) {
            for (int col = 0; col < NXT_SHAPE[row].length; col++) {
                Material mat = NXT_SHAPE[row][col] ? Material.OBSIDIAN : Material.AIR;
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
                    world.getBlockAt(wx, wy, wz).setType(mat);
                }
            }
        }

        leverLoc.getBlock().setType(Material.LEVER);
    }

}