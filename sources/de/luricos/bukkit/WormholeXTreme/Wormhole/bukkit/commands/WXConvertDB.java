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

import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateShape;

import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;

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

            doConvert(sender);

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



    private static void doConvert(CommandSender sender) {

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

        List<String> skippedNames = new ArrayList<>();



        sender.sendMessage(ConfigManager.MessageStrings.normalHeader

                + "Found " + total + " gate(s) to convert. Starting...");



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

        if (!skippedNames.isEmpty()) {

            sender.sendMessage(ConfigManager.MessageStrings.normalHeader

                    + "Skipped gates: §8" + String.join("§7, §8", skippedNames));

        }



        WXTLogger.prettyLog(Level.INFO, false,

                "[wxconvertdb] Conversion finished. Converted=" + converted + " Skipped=" + skipped);

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

