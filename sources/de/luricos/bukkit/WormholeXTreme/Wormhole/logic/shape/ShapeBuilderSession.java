package de.luricos.bukkit.WormholeXTreme.Wormhole.logic.shape;

import java.util.ArrayList;
import java.util.List;

/**
 * One player's in-progress gate shape.
 *
 * Held only in memory by {@link ShapeBuilderManager} - nothing is written to
 * disk until the wizard reaches the end and the shape validates, so cancelling
 * or disconnecting simply drops the work, as specified.
 */
public class ShapeBuilderSession {

    /** Where the wizard currently is. Drives what typed input means. */
    public enum Stage {
        NAME,
        DIMENSIONS,
        GRID,
        WOOSH_TICKS,
        LIGHT_TICKS,
        PORTAL_MATERIAL,
        IRIS_MATERIAL,
        STRUCTURE_MATERIAL,
        ACTIVE_MATERIAL,
        REDSTONE
    }

    /** Upper bounds, purely so a typo cannot spam the chat window to death. */
    public static final int MAX_DIMENSION = 15;
    public static final int MIN_DIMENSION = 3;
    public static final int MAX_LAYERS = 8;

    private final String playerName;
    private Stage stage = Stage.NAME;

    private String shapeName;
    private int width;
    private int height;

    /** One entry per layer; each entry is [row][column] of palette tokens. */
    private final List<String[][]> layers = new ArrayList<String[][]>();
    /**
     * The Layer#n number each layer is declared with. The loader treats that
     * number as the layer's depth and tolerates gaps, so a hand-written shape
     * that skips a number must keep skipping it after an edit.
     */
    private final List<Integer> layerNumbers = new ArrayList<Integer>();
    private int currentLayer = 0;

    private int wooshTicks = 3;
    private int lightTicks = 2;
    private String portalMaterial = "WATER";
    private String irisMaterial = "BEDROCK";
    private String structureMaterial = "OBSIDIAN";
    private String activeMaterial = "GLOWSTONE";
    private boolean redstoneActivated = false;

    /**
     * Set when the session came from /wxshape edit. The original file is only
     * replaced once the edited shape validates and saves.
     */
    private boolean editing = false;

    public ShapeBuilderSession(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public Stage getStage() {
        return this.stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public String getShapeName() {
        return this.shapeName;
    }

    public void setShapeName(String shapeName) {
        this.shapeName = shapeName;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    /**
     * Fixes the grid size and seeds the first layer. Every layer added later
     * reuses these dimensions, as the spec requires.
     */
    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        this.layers.clear();
        this.layerNumbers.clear();
        this.layers.add(blankLayer());
        this.layerNumbers.add(Integer.valueOf(1));
        this.currentLayer = 0;
    }

    public String[][] blankLayer() {
        String[][] layer = new String[this.height][this.width];
        for (int row = 0; row < this.height; row++) {
            for (int column = 0; column < this.width; column++) {
                layer[row][column] = ShapePalette.IGNORED;
            }
        }
        return layer;
    }

    public List<String[][]> getLayers() {
        return this.layers;
    }

    public List<Integer> getLayerNumbers() {
        return this.layerNumbers;
    }

    /** The Layer#n number of a layer, defaulting to its position. */
    public int getLayerNumber(int index) {
        if (index < 0 || index >= this.layerNumbers.size()) {
            return index + 1;
        }
        return this.layerNumbers.get(index).intValue();
    }

    /** Adds a layer that already has a declared number, used when parsing. */
    public void addLayer(String[][] grid, int number) {
        this.layers.add(grid);
        this.layerNumbers.add(Integer.valueOf(number));
    }

    public int getLayerCount() {
        return this.layers.size();
    }

    public int getCurrentLayer() {
        return this.currentLayer;
    }

    public void setCurrentLayer(int currentLayer) {
        if (currentLayer < 0 || currentLayer >= this.layers.size()) {
            return;
        }
        this.currentLayer = currentLayer;
    }

    /** Adds an empty layer of the same dimensions and switches to it. */
    public boolean addLayer() {
        if (this.layers.size() >= MAX_LAYERS) {
            return false;
        }
        int highest = 0;
        for (Integer number : this.layerNumbers) {
            if (number.intValue() > highest) {
                highest = number.intValue();
            }
        }
        this.layers.add(blankLayer());
        this.layerNumbers.add(Integer.valueOf(highest + 1));
        this.currentLayer = this.layers.size() - 1;
        return true;
    }

    /**
     * Drops the current layer. The first layer is the gate itself and cannot be
     * removed, so there is always something to render.
     */
    public boolean removeCurrentLayer() {
        if (this.layers.size() <= 1) {
            return false;
        }
        this.layers.remove(this.currentLayer);
        if (this.currentLayer < this.layerNumbers.size()) {
            this.layerNumbers.remove(this.currentLayer);
        }
        if (this.currentLayer >= this.layers.size()) {
            this.currentLayer = this.layers.size() - 1;
        }
        return true;
    }

    public String getCell(int layer, int row, int column) {
        if (layer < 0 || layer >= this.layers.size()) {
            return null;
        }
        String[][] grid = this.layers.get(layer);
        if (row < 0 || row >= grid.length || column < 0 || column >= grid[row].length) {
            return null;
        }
        return grid[row][column];
    }

    public void setCell(int layer, int row, int column, String token) {
        if (getCell(layer, row, column) == null) {
            return;
        }
        this.layers.get(layer)[row][column] = token;
    }

    /**
     * Next free ordering number for the L or W modifier across every layer, so
     * lights and woosh rings are numbered in the order the player places them
     * rather than all landing on #1.
     */
    public int nextOrderFor(String modifier) {
        int highest = 0;
        for (String[][] grid : this.layers) {
            for (String[] row : grid) {
                for (String cell : row) {
                    int order = ShapePalette.orderOf(cell, modifier);
                    if (order > highest) {
                        highest = order;
                    }
                }
            }
        }
        return highest + 1;
    }

    /** How many cells across every layer carry the given modifier. */
    public int countModifier(String modifier) {
        int count = 0;
        for (String[][] grid : this.layers) {
            for (String[] row : grid) {
                for (String cell : row) {
                    if (ShapePalette.hasModifier(cell, modifier)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** How many cells across every layer use the given base block. */
    public int countBase(String base) {
        int count = 0;
        for (String[][] grid : this.layers) {
            for (String[] row : grid) {
                for (String cell : row) {
                    if (base.equalsIgnoreCase(ShapePalette.baseOf(cell))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public int getWooshTicks() {
        return this.wooshTicks;
    }

    public void setWooshTicks(int wooshTicks) {
        this.wooshTicks = wooshTicks;
    }

    public int getLightTicks() {
        return this.lightTicks;
    }

    public void setLightTicks(int lightTicks) {
        this.lightTicks = lightTicks;
    }

    public String getPortalMaterial() {
        return this.portalMaterial;
    }

    public void setPortalMaterial(String portalMaterial) {
        this.portalMaterial = portalMaterial;
    }

    public String getIrisMaterial() {
        return this.irisMaterial;
    }

    public void setIrisMaterial(String irisMaterial) {
        this.irisMaterial = irisMaterial;
    }

    public String getStructureMaterial() {
        return this.structureMaterial;
    }

    public void setStructureMaterial(String structureMaterial) {
        this.structureMaterial = structureMaterial;
    }

    public String getActiveMaterial() {
        return this.activeMaterial;
    }

    public void setActiveMaterial(String activeMaterial) {
        this.activeMaterial = activeMaterial;
    }

    public boolean isRedstoneActivated() {
        return this.redstoneActivated;
    }

    public void setRedstoneActivated(boolean redstoneActivated) {
        this.redstoneActivated = redstoneActivated;
    }

    public boolean isEditing() {
        return this.editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }
}
