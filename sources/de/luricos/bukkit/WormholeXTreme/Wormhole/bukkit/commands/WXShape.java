package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateHelper;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.shape.ShapeBuilderManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.shape.ShapeBuilderSession;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * /wxshape - build and manage gate shapes from inside the game.
 *
 * The player-facing subcommands are create, edit, load, unload and remove. The
 * remaining subcommands (cell, set, grid, layer, addlayer, droplayer, done,
 * ticks, material, redstone, cancel) are what the clickable chat interface
 * runs behind the scenes; they are deliberately left off the tab completion
 * list because nobody needs to type them.
 */
public class WXShape implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("create", "edit", "load", "unload", "remove");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("edit") || sub.equals("unload") || sub.equals("remove")) {
                return filterPrefix(StargateHelper.getShapeNames(), args[1]);
            }
            if (sub.equals("load")) {
                return filterPrefix(listShapeFileNames(), args[1]);
            }
        }
        return Collections.emptyList();
    }

    private static List<String> filterPrefix(List<String> options, String prefix) {
        List<String> result = new ArrayList<String>();
        String lower = prefix == null ? "" : prefix.toLowerCase();
        for (String option : options) {
            if (option != null && option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    private static List<String> listShapeFileNames() {
        List<String> names = new ArrayList<String>();
        File[] files = StargateHelper.getShapesDirectory().listFiles();
        if (files == null) {
            return names;
        }
        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".shape")) {
                names.add(file.getName().substring(0, file.getName().length() - ".shape".length()));
            }
        }
        return names;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtilities.playerCheck(sender)) {
            sender.sendMessage(ConfigManager.MessageStrings.errorHeader + "/wxshape is an in-game interface and cannot be used from the console.");
            return true;
        }
        Player player = (Player) sender;
        if (!WXPermissions.checkPermission(player, WXPermissions.PermissionType.SHAPE)) {
            sender.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            return true;
        }

        String[] a = CommandUtilities.commandEscaper(args);
        if (a.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = a[0].toLowerCase();
        ShapeBuilderSession session = ShapeBuilderManager.getSession(player);

        // Wizard controls first: these only mean anything with a live session.
        switch (sub) {
            case "cancel":
                if (session == null) {
                    player.sendMessage(ConfigManager.MessageStrings.errorHeader + "You have no shape in progress.");
                    return true;
                }
                ShapeBuilderManager.cancel(player);
                return true;
            case "grid":
                return requireSession(player, session) && renderGrid(player, session);
            case "cell":
                return requireSession(player, session) && doCell(player, session, a);
            case "set":
                return requireSession(player, session) && doSet(player, session, a);
            case "mod":
                return requireSession(player, session) && doMod(player, session, a);
            case "order":
                return requireSession(player, session) && doOrder(player, session, a);
            case "setorder":
                return requireSession(player, session) && doSetOrder(player, session, a);
            case "layer":
                return requireSession(player, session) && doLayer(player, session, a);
            case "addlayer":
                return requireSession(player, session) && doAddLayer(player, session);
            case "droplayer":
                return requireSession(player, session) && doDropLayer(player, session);
            case "done":
                return requireSession(player, session) && doDone(player, session);
            case "ticks":
                return requireSession(player, session) && doTicks(player, session, a);
            case "material":
                return requireSession(player, session) && doMaterial(player, session, a);
            case "redstone":
                return requireSession(player, session) && doRedstone(player, session, a);
            default:
                break;
        }

        switch (sub) {
            case "create":
                if (session != null) {
                    player.sendMessage(ConfigManager.MessageStrings.errorHeader + "You already have a shape in progress. Finish it, or run /wxshape cancel.");
                    return true;
                }
                ShapeBuilderManager.startCreate(player);
                return true;
            case "edit":
                if (session != null) {
                    player.sendMessage(ConfigManager.MessageStrings.errorHeader + "You already have a shape in progress. Finish it, or run /wxshape cancel.");
                    return true;
                }
                if (a.length < 2) {
                    player.sendMessage(ConfigManager.MessageStrings.errorHeader + "Usage: /wxshape edit <shape>");
                    return true;
                }
                return ShapeBuilderManager.startEdit(player, a[1]);
            case "load":
                return doLoad(player, a);
            case "unload":
                return doUnload(player, a);
            case "remove":
                return doRemove(player, a);
            default:
                sendHelp(player);
                return true;
        }
    }

    private static boolean requireSession(Player player, ShapeBuilderSession session) {
        if (session == null) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + "You have no shape in progress. Start one with /wxshape create.");
            return false;
        }
        return true;
    }

    private static boolean renderGrid(Player player, ShapeBuilderSession session) {
        ShapeBuilderManager.renderGrid(player, session);
        return true;
    }

    private static boolean doCell(Player player, ShapeBuilderSession session, String[] a) {
        if (a.length < 3) {
            return true;
        }
        Integer row = parseInt(a[1]);
        Integer column = parseInt(a[2]);
        if (row == null || column == null) {
            return true;
        }
        ShapeBuilderManager.renderCellPalette(player, session, row.intValue(), column.intValue());
        return true;
    }

    private static boolean doSet(Player player, ShapeBuilderSession session, String[] a) {
        if (a.length < 4) {
            return true;
        }
        Integer row = parseInt(a[1]);
        Integer column = parseInt(a[2]);
        if (row == null || column == null) {
            return true;
        }
        ShapeBuilderManager.setCellBase(player, session, row.intValue(), column.intValue(), a[3]);
        return true;
    }

    private static boolean doMod(Player player, ShapeBuilderSession session, String[] a) {
        if (a.length < 4) {
            return true;
        }
        Integer row = parseInt(a[1]);
        Integer column = parseInt(a[2]);
        if (row == null || column == null) {
            return true;
        }
        ShapeBuilderManager.toggleCellModifier(player, session, row.intValue(), column.intValue(), a[3]);
        return true;
    }

    /** Opens the firing-order picker for an ordered role on one cell. */
    private static boolean doOrder(Player player, ShapeBuilderSession session, String[] a) {
        if (a.length < 4) {
            return true;
        }
        Integer row = parseInt(a[1]);
        Integer column = parseInt(a[2]);
        if (row == null || column == null) {
            return true;
        }
        ShapeBuilderManager.renderOrderPicker(player, session, row.intValue(), column.intValue(), a[3]);
        return true;
    }

    /** Applies a number picked from that picker. */
    private static boolean doSetOrder(Player player, ShapeBuilderSession session, String[] a) {
        if (a.length < 5) {
            return true;
        }
        Integer row = parseInt(a[1]);
        Integer column = parseInt(a[2]);
        Integer order = parseInt(a[4]);
        if (row == null || column == null || order == null) {
            return true;
        }
        ShapeBuilderManager.setModifierOrder(player, session, row.intValue(), column.intValue(), a[3], order.intValue());
        return true;
    }

    private static boolean doLayer(Player player, ShapeBuilderSession session, String[] a) {
        if (a.length < 2) {
            return true;
        }
        Integer layer = parseInt(a[1]);
        if (layer == null) {
            return true;
        }
        session.setCurrentLayer(layer.intValue());
        ShapeBuilderManager.renderGrid(player, session);
        return true;
    }

    private static boolean doAddLayer(Player player, ShapeBuilderSession session) {
        if (!session.addLayer()) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + "A shape can have at most " + ShapeBuilderSession.MAX_LAYERS + " layers.");
            return true;
        }
        ShapeBuilderManager.renderGrid(player, session);
        return true;
    }

    private static boolean doDropLayer(Player player, ShapeBuilderSession session) {
        if (!session.removeCurrentLayer()) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + "The first layer is the gate itself and cannot be removed.");
            return true;
        }
        ShapeBuilderManager.renderGrid(player, session);
        return true;
    }

    private static boolean doDone(Player player, ShapeBuilderSession session) {
        List<String> problems = ShapeBuilderManager.validate(session);
        if (!problems.isEmpty()) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + "This shape is not usable yet:");
            for (String problem : problems) {
                player.sendMessage(ConfigManager.MessageStrings.normalHeader + "§8- §7" + problem);
            }
            player.sendMessage(ConfigManager.MessageStrings.normalHeader + "§8Fix those and press Done again.");
            return true;
        }
        session.setStage(ShapeBuilderSession.Stage.WOOSH_TICKS);
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "Shape looks good. Now the timings and materials.");
        ShapeBuilderManager.repromptCurrentStage(player, session);
        return true;
    }

    private static boolean doTicks(Player player, ShapeBuilderSession session, String[] a) {
        if (a.length < 3) {
            return true;
        }
        Integer value = parseInt(a[2]);
        if (value == null) {
            return true;
        }
        if (a[1].equalsIgnoreCase("woosh")) {
            session.setWooshTicks(value.intValue());
            session.setStage(ShapeBuilderSession.Stage.LIGHT_TICKS);
        } else {
            session.setLightTicks(value.intValue());
            session.setStage(ShapeBuilderSession.Stage.PORTAL_MATERIAL);
        }
        ShapeBuilderManager.repromptCurrentStage(player, session);
        return true;
    }

    private static boolean doMaterial(Player player, ShapeBuilderSession session, String[] a) {
        if (a.length < 2) {
            return true;
        }
        ShapeBuilderManager.chooseMaterial(player, a[1]);
        return true;
    }

    private static boolean doRedstone(Player player, ShapeBuilderSession session, String[] a) {
        if (a.length < 2) {
            return true;
        }
        session.setRedstoneActivated(Boolean.parseBoolean(a[1]));
        String failure = ShapeBuilderManager.save(session);
        if (failure != null) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + failure);
            return true;
        }
        String name = session.getShapeName();
        ShapeBuilderManager.endSession(player);
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "Saved shape '" + name + "' and loaded it.");
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "§8Build one with §7/wxbuild " + name);
        return true;
    }

    private static boolean doLoad(Player player, String[] a) {
        if (a.length < 2) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + "Usage: /wxshape load <shape>");
            return true;
        }
        File file = ShapeBuilderManager.findShapeFile(a[1]);
        if (file == null) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + "No .shape file called '" + a[1] + "' in the GateShapes folder.");
            return true;
        }
        if (!StargateHelper.loadShapeFile(file)) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + "Could not load " + file.getName() + ". Check the server log.");
            return true;
        }
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "Loaded shape from " + file.getName() + ".");
        return true;
    }

    private static boolean doUnload(Player player, String[] a) {
        if (a.length < 2) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + "Usage: /wxshape unload <shape>");
            return true;
        }
        if (!StargateHelper.unloadShape(a[1])) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + "No loaded shape called '" + a[1] + "'.");
            return true;
        }
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "Unloaded shape '" + a[1] + "'. The file is still on disk.");
        return true;
    }

    private static boolean doRemove(Player player, String[] a) {
        if (a.length < 2) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + "Usage: /wxshape remove <shape>");
            return true;
        }
        File file = ShapeBuilderManager.findShapeFile(a[1]);
        StargateHelper.unloadShape(a[1]);
        if (file == null) {
            player.sendMessage(ConfigManager.MessageStrings.normalHeader + "No file to delete; the shape is unloaded.");
            return true;
        }
        if (!file.delete()) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader + "Unloaded the shape but could not delete " + file.getName() + ".");
            return true;
        }
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "Deleted " + file.getName() + " and unloaded the shape.");
        return true;
    }

    private static void sendHelp(Player player) {
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "Gate shape commands §3::");
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "§7/wxshape create §8- build a new shape step by step");
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "§7/wxshape edit §8<shape> - rework an existing shape");
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "§7/wxshape load §8<shape> - load a .shape file from disk");
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "§7/wxshape unload §8<shape> - unload without deleting the file");
        player.sendMessage(ConfigManager.MessageStrings.normalHeader + "§7/wxshape remove §8<shape> - delete the file entirely");
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
