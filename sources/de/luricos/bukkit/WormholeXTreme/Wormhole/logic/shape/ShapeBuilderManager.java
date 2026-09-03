package de.luricos.bukkit.WormholeXTreme.Wormhole.logic.shape;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateHelper;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Drives the /wxshape wizard: holds one session per player, renders the
 * clickable chat interface, validates the result, and reads and writes .Shape
 * files in the same format the loader already understands.
 *
 * Nothing here touches the world. A shape only becomes real once it passes
 * validation and is written into the GateShapes folder, at which point it is
 * handed to StargateHelper to load like any other shape file.
 */
public class ShapeBuilderManager {

    private static final Map<String, ShapeBuilderSession> sessions = new HashMap<String, ShapeBuilderSession>();

    private static final String HEADER = ConfigManager.MessageStrings.normalHeader.toString();
    private static final String ERROR = ConfigManager.MessageStrings.errorHeader.toString();

    private static final List<String> PORTAL_SUGGESTIONS = Arrays.asList("WATER", "LAVA", "NETHER_PORTAL", "AIR");
    private static final List<String> IRIS_SUGGESTIONS = Arrays.asList("BEDROCK", "STONE", "IRON_BLOCK", "GLASS", "DIAMOND_BLOCK", "OBSIDIAN");
    private static final List<String> STRUCTURE_SUGGESTIONS = Arrays.asList("OBSIDIAN", "STONE", "IRON_BLOCK", "GLASS", "DIAMOND_BLOCK", "LAPIS_BLOCK");
    private static final List<String> ACTIVE_SUGGESTIONS = Arrays.asList("GLOWSTONE", "REDSTONE_ORE", "SEA_LANTERN");

    public static ShapeBuilderSession getSession(Player player) {
        return sessions.get(player.getName());
    }

    public static boolean hasSession(Player player) {
        return sessions.containsKey(player.getName());
    }

    public static void endSession(Player player) {
        sessions.remove(player.getName());
    }

    // ------------------------------------------------------------------
    // Entry points
    // ------------------------------------------------------------------

    public static void startCreate(Player player) {
        ShapeBuilderSession session = new ShapeBuilderSession(player.getName());
        sessions.put(player.getName(), session);
        player.sendMessage(HEADER + "Gate shape builder §3::");
        player.sendMessage(HEADER + "§8Type §7cancel §8at any point to abandon the shape.");
        promptName(player);
    }

    /**
     * Loads an existing shape file back into a session so it can be reworked.
     * The file on disk is left alone until the edit is saved.
     */
    public static boolean startEdit(Player player, String shapeName) {
        File file = findShapeFile(shapeName);
        if (file == null) {
            player.sendMessage(ERROR + "No shape file found for: " + shapeName);
            return true;
        }
        ShapeBuilderSession session = new ShapeBuilderSession(player.getName());
        try {
            parseIntoSession(file, session);
        } catch (Exception e) {
            player.sendMessage(ERROR + "Could not read " + file.getName() + ": " + e.getMessage());
            WXTLogger.prettyLog(Level.WARNING, false, "Shape edit parse failed for " + file.getName() + ": " + e.getMessage());
            return true;
        }
        session.setEditing(true);
        session.setStage(ShapeBuilderSession.Stage.GRID);
        sessions.put(player.getName(), session);
        player.sendMessage(HEADER + "Editing shape '" + session.getShapeName() + "' §3::");
        player.sendMessage(HEADER + "§8Type §7cancel §8to leave the file untouched.");
        renderGrid(player, session);
        return true;
    }

    public static void cancel(Player player) {
        endSession(player);
        player.sendMessage(HEADER + "Shape builder cancelled. Nothing was saved.");
    }

    // ------------------------------------------------------------------
    // Typed input
    // ------------------------------------------------------------------

    /**
     * Handles a line the player typed while a session is waiting for text.
     * Returns true when the input was consumed, so the chat listener knows to
     * cancel the message rather than broadcasting it.
     */
    public static boolean handleInput(Player player, String rawInput) {
        ShapeBuilderSession session = getSession(player);
        if (session == null) {
            return false;
        }
        String input = rawInput.trim();
        if (input.equalsIgnoreCase("cancel")) {
            cancel(player);
            return true;
        }
        switch (session.getStage()) {
            case NAME:
                return handleNameInput(player, session, input);
            case DIMENSIONS:
                return handleDimensionsInput(player, session, input);
            case WOOSH_TICKS:
                return handleTickInput(player, session, input, true);
            case LIGHT_TICKS:
                return handleTickInput(player, session, input, false);
            case PORTAL_MATERIAL:
            case IRIS_MATERIAL:
            case STRUCTURE_MATERIAL:
            case ACTIVE_MATERIAL:
                return handleMaterialInput(player, session, input);
            default:
                // The grid and redstone stages are click driven; anything typed
                // there is ordinary chat and should pass through untouched.
                return false;
        }
    }

    private static boolean handleNameInput(Player player, ShapeBuilderSession session, String input) {
        String name = input.replaceAll("[^A-Za-z0-9_-]", "");
        if (name.isEmpty()) {
            player.sendMessage(ERROR + "Shape names may only contain letters, numbers, hyphens and underscores.");
            promptName(player);
            return true;
        }
        if (!session.isEditing() && StargateHelper.isStargateShape(name)) {
            player.sendMessage(ERROR + "A shape called '" + name + "' already exists. Pick another name, or use /wxshape edit " + name + ".");
            promptName(player);
            return true;
        }
        session.setShapeName(name);
        session.setStage(ShapeBuilderSession.Stage.DIMENSIONS);
        promptDimensions(player);
        return true;
    }

    private static boolean handleDimensionsInput(Player player, ShapeBuilderSession session, String input) {
        String[] parts = input.toLowerCase().replace("x", " ").split("\\s+");
        Integer width = null;
        Integer height = null;
        if (parts.length == 1) {
            width = parseInt(parts[0]);
            height = width;
        } else if (parts.length >= 2) {
            width = parseInt(parts[0]);
            height = parseInt(parts[1]);
        }
        if (width == null || height == null) {
            player.sendMessage(ERROR + "Give the size as a single number, or width then height. For example: 7 or 7x7.");
            promptDimensions(player);
            return true;
        }
        if (width.intValue() < ShapeBuilderSession.MIN_DIMENSION || height.intValue() < ShapeBuilderSession.MIN_DIMENSION
                || width.intValue() > ShapeBuilderSession.MAX_DIMENSION || height.intValue() > ShapeBuilderSession.MAX_DIMENSION) {
            player.sendMessage(ERROR + "Size must be between " + ShapeBuilderSession.MIN_DIMENSION + " and " + ShapeBuilderSession.MAX_DIMENSION + " blocks each way.");
            promptDimensions(player);
            return true;
        }
        session.setDimensions(width.intValue(), height.intValue());
        session.setStage(ShapeBuilderSession.Stage.GRID);
        player.sendMessage(HEADER + "Created a " + width + " by " + height + " grid. Click a cell to change it.");
        renderGrid(player, session);
        return true;
    }

    private static boolean handleTickInput(Player player, ShapeBuilderSession session, String input, boolean woosh) {
        Integer ticks = parseInt(input);
        if (ticks == null || ticks.intValue() < 0 || ticks.intValue() > 200) {
            player.sendMessage(ERROR + "Give a whole number of ticks between 0 and 200.");
            repromptCurrentStage(player, session);
            return true;
        }
        if (woosh) {
            session.setWooshTicks(ticks.intValue());
            session.setStage(ShapeBuilderSession.Stage.LIGHT_TICKS);
        } else {
            session.setLightTicks(ticks.intValue());
            session.setStage(ShapeBuilderSession.Stage.PORTAL_MATERIAL);
        }
        repromptCurrentStage(player, session);
        return true;
    }

    private static boolean handleMaterialInput(Player player, ShapeBuilderSession session, String input) {
        String name = input.trim().toUpperCase().replace(' ', '_');
        if (Material.matchMaterial(name) == null) {
            player.sendMessage(ERROR + "'" + input + "' is not a material this server recognises.");
            repromptCurrentStage(player, session);
            return true;
        }
        applyMaterial(session, name);
        repromptCurrentStage(player, session);
        return true;
    }

    /** Stores the chosen material and advances to whatever comes next. */
    private static void applyMaterial(ShapeBuilderSession session, String material) {
        switch (session.getStage()) {
            case PORTAL_MATERIAL:
                session.setPortalMaterial(material);
                session.setStage(ShapeBuilderSession.Stage.IRIS_MATERIAL);
                break;
            case IRIS_MATERIAL:
                session.setIrisMaterial(material);
                session.setStage(ShapeBuilderSession.Stage.STRUCTURE_MATERIAL);
                break;
            case STRUCTURE_MATERIAL:
                session.setStructureMaterial(material);
                session.setStage(ShapeBuilderSession.Stage.ACTIVE_MATERIAL);
                break;
            case ACTIVE_MATERIAL:
                session.setActiveMaterial(material);
                session.setStage(ShapeBuilderSession.Stage.REDSTONE);
                break;
            default:
                break;
        }
    }

    /** Click-driven material choice, routed through the same code as typing. */
    public static void chooseMaterial(Player player, String material) {
        ShapeBuilderSession session = getSession(player);
        if (session == null) {
            return;
        }
        applyMaterial(session, material.toUpperCase());
        repromptCurrentStage(player, session);
    }

    // ------------------------------------------------------------------
    // Prompts
    // ------------------------------------------------------------------

    /**
     * Re-renders whatever question the session is currently on. Used after a
     * click answers one question and moves the wizard to the next.
     */
    public static void repromptCurrentStage(Player player, ShapeBuilderSession session) {
        switch (session.getStage()) {
            case NAME:
                promptName(player);
                break;
            case DIMENSIONS:
                promptDimensions(player);
                break;
            case GRID:
                renderGrid(player, session);
                break;
            case WOOSH_TICKS:
                promptWooshTicks(player);
                break;
            case LIGHT_TICKS:
                promptLightTicks(player);
                break;
            case PORTAL_MATERIAL:
            case IRIS_MATERIAL:
            case STRUCTURE_MATERIAL:
            case ACTIVE_MATERIAL:
            case REDSTONE:
                promptMaterial(player, session);
                break;
            default:
                break;
        }
    }

    private static void promptName(Player player) {
        player.sendMessage(HEADER + "Type a name for the new shape.");
    }

    private static void promptDimensions(Player player) {
        player.sendMessage(HEADER + "Type the gate size, e.g. \u00a777\u00a78 for square or \u00a777 9\u00a78 for width then height.");
        player.sendMessage(HEADER + "\u00a78Between " + ShapeBuilderSession.MIN_DIMENSION + " and " + ShapeBuilderSession.MAX_DIMENSION + " blocks each way.");
    }

    private static void promptWooshTicks(Player player) {
        player.sendMessage(HEADER + "Ticks between each woosh step. Type a number, or:");
        TextComponent line = new TextComponent("");
        line.addExtra(button(" [Use default: 3] ", "\u00a7a", "/wxshape ticks woosh 3", "1 tick is about 50ms."));
        line.addExtra(button(" [Cancel] ", "\u00a7c", "/wxshape cancel", "Abandon this shape."));
        player.spigot().sendMessage(line);
    }

    private static void promptLightTicks(Player player) {
        player.sendMessage(HEADER + "Ticks between each chevron lighting. Type a number, or:");
        TextComponent line = new TextComponent("");
        line.addExtra(button(" [Use default: 2] ", "\u00a7a", "/wxshape ticks light 2", "1 tick is about 50ms."));
        line.addExtra(button(" [Cancel] ", "\u00a7c", "/wxshape cancel", "Abandon this shape."));
        player.spigot().sendMessage(line);
    }

    /** Renders whichever material question the session has reached. */
    private static void promptMaterial(Player player, ShapeBuilderSession session) {
        List<String> suggestions;
        String question;
        String current;
        switch (session.getStage()) {
            case PORTAL_MATERIAL:
                question = "Portal material - what the [P] blocks become when the gate is open.";
                suggestions = PORTAL_SUGGESTIONS;
                current = session.getPortalMaterial();
                break;
            case IRIS_MATERIAL:
                question = "Iris material - what the [P] blocks become when the iris is closed.";
                suggestions = IRIS_SUGGESTIONS;
                current = session.getIrisMaterial();
                break;
            case STRUCTURE_MATERIAL:
                question = "Stargate material - what the frame is built from.";
                suggestions = STRUCTURE_SUGGESTIONS;
                current = session.getStructureMaterial();
                break;
            case ACTIVE_MATERIAL:
                question = "Active material - what the chevron lights become when the gate is open.";
                suggestions = ACTIVE_SUGGESTIONS;
                current = session.getActiveMaterial();
                break;
            case REDSTONE:
                promptRedstone(player);
                return;
            default:
                return;
        }
        player.sendMessage(HEADER + question);
        player.sendMessage(HEADER + "\u00a78Default is \u00a77" + current + "\u00a78. Click one, or type any material name.");
        TextComponent line = new TextComponent("");
        for (String suggestion : suggestions) {
            line.addExtra(button(" [" + suggestion + "] ", suggestion.equals(current) ? "\u00a7a" : "\u00a77",
                    "/wxshape material " + suggestion, "Use " + suggestion + "."));
        }
        line.addExtra(button(" [Cancel] ", "\u00a7c", "/wxshape cancel", "Abandon this shape."));
        player.spigot().sendMessage(line);
    }

    private static void promptRedstone(Player player) {
        player.sendMessage(HEADER + "Should this shape allow redstone to and from its redstone blocks?");
        TextComponent line = new TextComponent("");
        line.addExtra(button(" [Yes] ", "\u00a7a", "/wxshape redstone true", "Redstone activation enabled."));
        line.addExtra(button(" [No - default] ", "\u00a77", "/wxshape redstone false", "Redstone activation disabled."));
        line.addExtra(button(" [Cancel] ", "\u00a7c", "/wxshape cancel", "Abandon this shape."));
        player.spigot().sendMessage(line);
    }

    // ------------------------------------------------------------------
    // Grid rendering
    // ------------------------------------------------------------------

    public static void renderGrid(Player player, ShapeBuilderSession session) {
        player.sendMessage(HEADER + "Shape '" + session.getShapeName() + "' \u00a73:: \u00a77Layer " + session.getLayerNumber(session.getCurrentLayer())
                + " \u00a78(" + (session.getCurrentLayer() + 1) + " of " + session.getLayerCount() + ")");
        String[][] grid = session.getLayers().get(session.getCurrentLayer());
        for (int row = 0; row < grid.length; row++) {
            TextComponent line = new TextComponent("");
            for (int column = 0; column < grid[row].length; column++) {
                String cell = grid[row][column];
                String tooltip = "\u00a77[" + cell + "]\n\u00a7f" + ShapePalette.describe(cell) + "\n\u00a78Click to change this block.";
                line.addExtra(button("[" + ShapePalette.labelOf(cell) + "]", ShapePalette.colourOf(cell),
                        "/wxshape cell " + row + " " + column, tooltip));
            }
            player.spigot().sendMessage(line);
        }
        renderGridControls(player, session);
    }

    private static void renderGridControls(Player player, ShapeBuilderSession session) {
        TextComponent line = new TextComponent("");
        if (session.getCurrentLayer() > 0) {
            line.addExtra(button(" [< Layer] ", "\u00a77", "/wxshape layer " + (session.getCurrentLayer() - 1), "Go to the previous layer."));
        }
        if (session.getCurrentLayer() < session.getLayerCount() - 1) {
            line.addExtra(button(" [Layer >] ", "\u00a77", "/wxshape layer " + (session.getCurrentLayer() + 1), "Go to the next layer."));
        }
        if (session.getLayerCount() < ShapeBuilderSession.MAX_LAYERS) {
            line.addExtra(button(" [+ Layer] ", "\u00a7a", "/wxshape addlayer",
                    "Add another layer at the same size.\n\u00a78Layers stack outwards from the gate face."));
        }
        if (session.getLayerCount() > 1) {
            line.addExtra(button(" [- Layer] ", "\u00a76", "/wxshape droplayer", "Delete the layer you are looking at."));
        }
        line.addExtra(button(" [Done] ", "\u00a7a", "/wxshape done", "Check the shape and move on to the settings."));
        line.addExtra(button(" [Cancel] ", "\u00a7c", "/wxshape cancel", "Abandon this shape. Nothing is saved."));
        player.spigot().sendMessage(line);
    }

    /**
     * Shows the editor for one cell: the base blocks it can be, and the roles
     * that can be toggled on top. Every entry carries a tooltip explaining what
     * that letter means.
     */
    public static void renderCellPalette(Player player, ShapeBuilderSession session, int row, int column) {
        String current = session.getCell(session.getCurrentLayer(), row, column);
        if (current == null) {
            player.sendMessage(ERROR + "That cell is not part of the grid.");
            return;
        }
        player.sendMessage(HEADER + "Row " + (row + 1) + ", column " + (column + 1) + " \u00a78currently \u00a77[" + current + "]");

        TextComponent bases = new TextComponent("\u00a78Block: ");
        for (ShapePalette.Base base : ShapePalette.getBases()) {
            boolean selected = base.getToken().equalsIgnoreCase(ShapePalette.baseOf(current));
            String tooltip = "\u00a77" + base.getToken() + "\n\u00a7f" + base.getDescription();
            bases.addExtra(button(" [" + base.getLabel() + "] ", selected ? "\u00a7a" : base.getColour(),
                    "/wxshape set " + row + " " + column + " " + base.getToken(), tooltip));
        }
        player.spigot().sendMessage(bases);

        TextComponent mods = new TextComponent("\u00a78Roles: ");
        for (ShapePalette.Modifier modifier : ShapePalette.getModifiers()) {
            boolean on = ShapePalette.hasModifier(current, modifier.getToken());
            String label = modifier.getLabel();
            String tooltip;
            String command;
            if (modifier.isOrdered()) {
                // Ordered roles open a picker, so the firing order can be chosen
                // by hand rather than only being assigned on placement.
                int order = ShapePalette.orderOf(current, modifier.getToken());
                if (on && order > 0) {
                    label = label + order;
                }
                tooltip = "\u00a77:" + modifier.getToken() + "\n\u00a7f" + modifier.getDescription()
                        + "\n\u00a78Click to " + (on ? "change or remove its number." : "pick its number.");
                command = "/wxshape order " + row + " " + column + " " + modifier.getToken();
            } else {
                tooltip = "\u00a77:" + modifier.getToken() + "\n\u00a7f" + modifier.getDescription()
                        + "\n\u00a78Click to " + (on ? "remove it from" : "add it to") + " this block.";
                command = "/wxshape mod " + row + " " + column + " " + modifier.getToken();
            }
            mods.addExtra(button(" [" + label + "] ", on ? "\u00a7a" : "\u00a78", command, tooltip));
        }
        player.spigot().sendMessage(mods);

        TextComponent back = new TextComponent("");
        back.addExtra(button(" [Back to grid] ", "\u00a77", "/wxshape grid", "Leave this cell as it is."));
        player.spigot().sendMessage(back);
    }

    /**
     * Lets the player pick the firing order for an ordered role on one cell.
     *
     * Blocks sharing a number fire together, so numbers already in use are
     * highlighted rather than hidden - stacking several blocks on one step is a
     * normal thing to want.
     */
    public static void renderOrderPicker(Player player, ShapeBuilderSession session, int row, int column, String modifier) {
        ShapePalette.Modifier definition = ShapePalette.getModifier(modifier);
        if (definition == null || !definition.isOrdered()) {
            player.sendMessage(ERROR + "That role does not have a firing order.");
            return;
        }
        String current = session.getCell(session.getCurrentLayer(), row, column);
        if (current == null) {
            player.sendMessage(ERROR + "That cell is not part of the grid.");
            return;
        }
        int currentOrder = ShapePalette.orderOf(current, modifier);
        String what = definition.getToken().equals(ShapePalette.MOD_WOOSH) ? "Woosh" : "Light";

        player.sendMessage(HEADER + what + " order for row " + (row + 1) + ", column " + (column + 1)
                + (currentOrder > 0 ? " \u00a78currently \u00a77#" + currentOrder : " \u00a78not set yet"));

        java.util.Set<Integer> used = session.usedOrders(modifier);
        TextComponent line = new TextComponent("");
        for (int order = 1; order <= ShapePalette.MAX_ORDER; order++) {
            boolean isCurrent = order == currentOrder;
            int others = session.countAtOrder(modifier, order) - (isCurrent ? 1 : 0);
            String colour = isCurrent ? "\u00a7a" : (used.contains(Integer.valueOf(order)) ? "\u00a7b" : "\u00a78");
            String tooltip = "\u00a77" + definition.getToken() + "#" + order + "\n\u00a7f"
                    + (others > 0
                        ? others + " other block(s) already fire on step " + order + ". They all fire together."
                        : "Nothing else is on step " + order + " yet.")
                    + "\n\u00a78Steps run in ascending order.";
            line.addExtra(button(" [" + definition.getLabel() + order + "] ", colour,
                    "/wxshape setorder " + row + " " + column + " " + modifier + " " + order, tooltip));
        }
        player.spigot().sendMessage(line);

        TextComponent controls = new TextComponent("");
        if (currentOrder > 0) {
            controls.addExtra(button(" [Remove] ", "\u00a7c", "/wxshape mod " + row + " " + column + " " + modifier,
                    "Take this role off the block entirely."));
        }
        controls.addExtra(button(" [Back to cell] ", "\u00a77", "/wxshape cell " + row + " " + column,
                "Leave the order as it is."));
        player.spigot().sendMessage(controls);
    }

    /** Applies a hand-picked firing order and returns to the cell editor. */
    public static void setModifierOrder(Player player, ShapeBuilderSession session, int row, int column, String modifier, int order) {
        ShapePalette.Modifier definition = ShapePalette.getModifier(modifier);
        if (definition == null || !definition.isOrdered()) {
            player.sendMessage(ERROR + "That role does not have a firing order.");
            return;
        }
        if (order < 1 || order > ShapePalette.MAX_ORDER) {
            player.sendMessage(ERROR + "Pick an order between 1 and " + ShapePalette.MAX_ORDER + ".");
            return;
        }
        String current = session.getCell(session.getCurrentLayer(), row, column);
        if (current == null) {
            return;
        }
        session.setCell(session.getCurrentLayer(), row, column, ShapePalette.setModifierOrder(current, modifier, order));
        renderCellPalette(player, session, row, column);
    }

    /** Swaps a cell's base block, keeping whatever roles it already had. */
    public static void setCellBase(Player player, ShapeBuilderSession session, int row, int column, String base) {
        if (ShapePalette.getBase(base) == null) {
            player.sendMessage(ERROR + "Unknown block type: " + base);
            return;
        }
        String current = session.getCell(session.getCurrentLayer(), row, column);
        if (current == null) {
            return;
        }
        session.setCell(session.getCurrentLayer(), row, column, ShapePalette.withBase(current, base));
        renderCellPalette(player, session, row, column);
    }

    /** Adds or removes one role on a cell. */
    public static void toggleCellModifier(Player player, ShapeBuilderSession session, int row, int column, String modifier) {
        ShapePalette.Modifier definition = ShapePalette.getModifier(modifier);
        if (definition == null) {
            player.sendMessage(ERROR + "Unknown role: " + modifier);
            return;
        }
        String current = session.getCell(session.getCurrentLayer(), row, column);
        if (current == null) {
            return;
        }
        boolean adding = !ShapePalette.hasModifier(current, modifier);
        if (adding && definition.isUnique() && session.countModifier(modifier) > 0) {
            player.sendMessage(ERROR + "This shape already has a [" + definition.getLabel() + "] block. Remove that one first.");
            return;
        }
        int order = definition.isOrdered() ? session.nextOrderFor(modifier) : 0;
        session.setCell(session.getCurrentLayer(), row, column, ShapePalette.toggleModifier(current, modifier, order));
        renderCellPalette(player, session, row, column);
    }

    // ------------------------------------------------------------------
    // Validation and saving
    // ------------------------------------------------------------------

    /**
     * Checks the grid makes a gate the loader and builder can actually use.
     * Every problem is reported at once rather than one per attempt.
     */
    public static List<String> validate(ShapeBuilderSession session) {
        List<String> problems = new ArrayList<String>();

        int activation = session.countModifier(ShapePalette.MOD_ACTIVATION);
        if (activation == 0) {
            problems.add("No activation block. Every gate needs exactly one [A].");
        } else if (activation > 1) {
            problems.add("Found " + activation + " activation blocks. There can only be one [A].");
        }

        for (ShapePalette.Modifier modifier : ShapePalette.getModifiers()) {
            if (!modifier.isUnique() || modifier.getToken().equals(ShapePalette.MOD_ACTIVATION)) {
                continue;
            }
            int count = session.countModifier(modifier.getToken());
            if (count > 1) {
                problems.add("Found " + count + " of the [" + modifier.getLabel() + "] block. There can only be one.");
            }
        }

        if (session.countBase(ShapePalette.PORTAL) == 0) {
            problems.add("No portal blocks. Place some [P] for players to walk through.");
        }
        if (session.countBase(ShapePalette.STARGATE) == 0) {
            problems.add("No frame blocks. Place some [S] to build the gate out of.");
        }
        if (session.countModifier(ShapePalette.MOD_ENTER_PLAYER) == 0) {
            problems.add("No player arrival point. Add the [E] role to a block or nobody can arrive through the gate.");
        }

        boolean hasDialler = session.countModifier(ShapePalette.MOD_DIALER_SIGN) > 0;
        if (!hasDialler && countRedstone(session, ShapePalette.REDSTONE_DIAL) > 0) {
            problems.add("The redstone dial block [>] needs a sign dialler [D] to target.");
        }
        if (!hasDialler && countRedstone(session, ShapePalette.REDSTONE_SIGN) > 0) {
            problems.add("The redstone sign block [~] needs a sign dialler [D] to cycle.");
        }
        String[] redstoneTokens = {ShapePalette.REDSTONE_DIAL, ShapePalette.REDSTONE_SIGN, ShapePalette.REDSTONE_ACTIVE};
        for (String token : redstoneTokens) {
            if (countRedstone(session, token) > 1) {
                problems.add("There can only be one [" + ShapePalette.getBase(token).getLabel() + "] redstone block.");
            }
        }

        checkFacingClear(session, problems, ShapePalette.MOD_ACTIVATION, "activation block [A]");
        checkFacingClear(session, problems, ShapePalette.MOD_DIALER_SIGN, "sign dialler [D]");

        return problems;
    }

    /**
     * Redstone tokens count whether they are the whole block or a role on one,
     * since the game accepts both spellings.
     */
    private static int countRedstone(ShapeBuilderSession session, String token) {
        return session.countBase(token) + session.countModifier(token);
    }

    /**
     * The switch and the sign both need clear space in front of them, which in
     * shape terms means the same position one layer further out must be Ignored.
     */
    private static void checkFacingClear(ShapeBuilderSession session, List<String> problems, String modifier, String label) {
        for (int layer = 0; layer < session.getLayerCount(); layer++) {
            String[][] grid = session.getLayers().get(layer);
            for (int row = 0; row < grid.length; row++) {
                for (int column = 0; column < grid[row].length; column++) {
                    if (!ShapePalette.hasModifier(grid[row][column], modifier)) {
                        continue;
                    }
                    if (layer + 1 >= session.getLayerCount()) {
                        continue;
                    }
                    String facing = session.getCell(layer + 1, row, column);
                    if (!ShapePalette.IGNORED.equalsIgnoreCase(ShapePalette.baseOf(facing))
                            || !ShapePalette.rawModifiers(facing).isEmpty()) {
                        problems.add("The " + label + " on layer " + session.getLayerNumber(layer)
                                + " is blocked - the same spot on the next layer must be Ignored.");
                    }
                }
            }
        }
    }

    /**
     * Writes the shape file and hands it to the loader.
     * Returns null on success, or a message describing what went wrong.
     */
    public static String save(ShapeBuilderSession session) {
        File directory = StargateHelper.getShapesDirectory();
        if (!directory.exists() && !directory.mkdirs()) {
            return "Could not create the GateShapes folder.";
        }
        File target = new File(directory, session.getShapeName() + ".shape");
        // Editing a shape must not quietly switch it back on. Remember where
        // the flag stood, and put it back once the new file is loaded.
        boolean wasKnown = StargateHelper.isStargateShape(session.getShapeName());
        boolean wasEnabled = !wasKnown || StargateHelper.isShapeEnabled(session.getShapeName());
        try {
            Files.write(target.toPath(), serialize(session).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            WXTLogger.prettyLog(Level.WARNING, false, "Failed to write shape " + target.getName() + ": " + e.getMessage());
            return "Could not write " + target.getName() + ": " + e.getMessage();
        }
        // Replacing an edited shape means dropping the old one first, otherwise
        // a rename would leave the previous version loaded alongside it.
        StargateHelper.unloadShape(session.getShapeName());
        if (!StargateHelper.loadShapeFile(target)) {
            return "The file was written but the loader rejected it. Check the server log.";
        }
        if (wasKnown && StargateHelper.isShapeEnabled(session.getShapeName()) != wasEnabled) {
            StargateHelper.setShapeEnabled(session.getShapeName(), wasEnabled);
        }
        return null;
    }

    /** Renders the session as a .Shape file in the documented v2 format. */
    public static String serialize(ShapeBuilderSession session) {
        StringBuilder out = new StringBuilder();
        out.append("# The name for this shape\n");
        out.append("Name=").append(session.getShapeName()).append("\n");
        out.append("# Version 2 of shape files allows for 3D gates, woosh and light order etc.\n");
        out.append("Version=2\n\n");
        out.append("# Built with /wxshape.\n");
        out.append("#    [I] = Ignored\n");
        out.append("#    [S] = Stargate Material\n");
        out.append("#    [P] = Air blocks that will turn into the portal material when activated.\n");
        out.append("#    :N = name sign, :EP = player arrival, :EM = minecart arrival\n");
        out.append("#    :A = activation switch, :D = sign dialler, :IA = iris switch\n");
        out.append("#    :L#n = chevron light order, :W#n = woosh order\n");
        out.append("#    [RD] / [RS] / [RA] = redstone dial in, sign cycle, gate active out\n");
        out.append("# See Standard.shape for the full documentation.\n\n");
        out.append("GateShape=\n\n");

        for (int layer = 0; layer < session.getLayerCount(); layer++) {
            out.append("Layer#").append(session.getLayerNumber(layer)).append("=\n");
            String[][] grid = session.getLayers().get(layer);
            for (String[] row : grid) {
                for (String cell : row) {
                    out.append('[').append(cell).append(']');
                }
                out.append('\n');
            }
            out.append('\n');
        }

        out.append("# Number of ticks to wait before activating each # of the woosh. 1 tick = ~50ms\n");
        out.append("WOOSH_TICKS = ").append(session.getWooshTicks()).append(";\n");
        out.append("# Number of ticks to wait before activating each # of the lights. 1 tick = ~50ms\n");
        out.append("LIGHT_TICKS = ").append(session.getLightTicks()).append(";\n\n");
        out.append("PORTAL_MATERIAL=").append(session.getPortalMaterial()).append("\n");
        out.append("IRIS_MATERIAL=").append(session.getIrisMaterial()).append("\n");
        out.append("STARGATE_MATERIAL=").append(session.getStructureMaterial()).append("\n");
        out.append("ACTIVE_MATERIAL=").append(session.getActiveMaterial()).append("\n\n");
        out.append("# Redstone activated is the parameter to allow redstone to/from redstone locations.\n");
        out.append("REDSTONE_ACTIVATED=").append(session.isRedstoneActivated() ? "TRUE" : "FALSE").append("\n");
        return out.toString();
    }

    // ------------------------------------------------------------------
    // Reading an existing shape back in
    // ------------------------------------------------------------------

    /**
     * Parses a .Shape file into a session for editing.
     *
     * Layer numbers are preserved exactly, because the loader treats Layer#n as
     * the layer's depth and hand-written shapes do skip numbers. Short rows and
     * narrow layers are padded out with Ignored, which places and checks
     * nothing, so the built gate is unchanged.
     */
    public static void parseIntoSession(File file, ShapeBuilderSession session) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        List<List<String[]>> layers = new ArrayList<List<String[]>>();
        List<Integer> numbers = new ArrayList<Integer>();
        List<String[]> current = null;
        String name = file.getName().replaceAll("\\.shape$", "");

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("Name=")) {
                name = line.substring(5).trim();
            } else if (line.startsWith("Layer#")) {
                current = new ArrayList<String[]>();
                layers.add(current);
                numbers.add(Integer.valueOf(parseLayerNumber(line, layers.size())));
            } else if (line.startsWith("[") && current != null) {
                current.add(parseRow(line));
            } else if (line.startsWith("WOOSH_TICKS")) {
                Integer value = parseInt(afterEquals(line).replace(";", "").trim());
                if (value != null) {
                    session.setWooshTicks(value.intValue());
                }
            } else if (line.startsWith("LIGHT_TICKS")) {
                Integer value = parseInt(afterEquals(line).replace(";", "").trim());
                if (value != null) {
                    session.setLightTicks(value.intValue());
                }
            } else if (line.startsWith("PORTAL_MATERIAL")) {
                session.setPortalMaterial(afterEquals(line).trim());
            } else if (line.startsWith("IRIS_MATERIAL")) {
                session.setIrisMaterial(afterEquals(line).trim());
            } else if (line.startsWith("STARGATE_MATERIAL")) {
                session.setStructureMaterial(afterEquals(line).trim());
            } else if (line.startsWith("ACTIVE_MATERIAL")) {
                session.setActiveMaterial(afterEquals(line).trim());
            } else if (line.startsWith("REDSTONE_ACTIVATED")) {
                session.setRedstoneActivated(afterEquals(line).trim().equalsIgnoreCase("TRUE"));
            }
        }

        if (layers.isEmpty() || layers.get(0).isEmpty()) {
            throw new IOException("no gate layers found in the file");
        }

        // The builder works on one rectangle shared by every layer. Some
        // hand-written shapes have layers that overhang the first one; padding
        // those in would be fine, but truncating them would silently delete
        // real blocks, so refuse the edit instead of quietly damaging the file.
        int gridHeight = layers.get(0).size();
        int gridWidth = layers.get(0).get(0).length;
        for (int index = 0; index < layers.size(); index++) {
            List<String[]> rows = layers.get(index);
            if (rows.size() > gridHeight) {
                throw new IOException("layer " + (index + 1) + " has " + rows.size()
                        + " rows but the gate is " + gridHeight + " tall - edit this shape by hand");
            }
            for (String[] row : rows) {
                if (row.length > gridWidth) {
                    throw new IOException("layer " + (index + 1) + " has a row of " + row.length
                            + " blocks but the gate is " + gridWidth + " wide - edit this shape by hand");
                }
            }
        }

        // Width and height come from the first layer, matching how the game's
        // own shape parser reads a file.
        int height = gridHeight;
        int width = gridWidth;
        session.setShapeName(name);
        session.setDimensions(width, height);
        session.getLayers().clear();
        session.getLayerNumbers().clear();

        for (int index = 0; index < layers.size(); index++) {
            List<String[]> rows = layers.get(index);
            String[][] grid = session.blankLayer();
            for (int row = 0; row < rows.size() && row < height; row++) {
                String[] cells = rows.get(row);
                for (int column = 0; column < cells.length && column < width; column++) {
                    String token = cells[column];
                    grid[row][column] = ShapePalette.isRecognised(token) ? token : ShapePalette.IGNORED;
                }
            }
            session.addLayer(grid, index < numbers.size() ? numbers.get(index).intValue() : index + 1);
        }
        session.setCurrentLayer(0);
    }

    /** Reads the n out of "Layer#n=", falling back to the layer's position. */
    private static int parseLayerNumber(String line, int fallback) {
        try {
            String digits = line.substring(line.indexOf('#') + 1).replaceAll("[^0-9].*$", "");
            return digits.isEmpty() ? fallback : Integer.parseInt(digits);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static String[] parseRow(String line) {
        List<String> cells = new ArrayList<String>();
        int index = 0;
        while (index < line.length()) {
            int open = line.indexOf('[', index);
            if (open < 0) {
                break;
            }
            int close = line.indexOf(']', open);
            if (close < 0) {
                break;
            }
            cells.add(line.substring(open + 1, close).trim());
            index = close + 1;
        }
        return cells.toArray(new String[0]);
    }

    private static String afterEquals(String line) {
        int index = line.indexOf('=');
        return index < 0 ? "" : line.substring(index + 1);
    }

    /** Finds a shape file by name, ignoring case, in the GateShapes folder. */
    public static File findShapeFile(String shapeName) {
        File directory = StargateHelper.getShapesDirectory();
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (!file.getName().toLowerCase().endsWith(".shape")) {
                continue;
            }
            String bare = file.getName().substring(0, file.getName().length() - ".shape".length());
            if (bare.equalsIgnoreCase(shapeName)) {
                return file;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Chat component helpers
    // ------------------------------------------------------------------

    private static TextComponent button(String label, String colour, String command, String tooltip) {
        TextComponent component = new TextComponent(colour + label);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(tooltip)));
        return component;
    }

    private static TextComponent cellButton(String label, String colour, String command, String tooltip) {
        return button(label, colour, command, tooltip);
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
