package de.luricos.bukkit.WormholeXTreme.Wormhole.model;

import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Material;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/model/StargateShape.class */
public class StargateShape {
    private String shapeName;
    private int[][] shapeStructurePositions;
    private int[] shapeSignPosition;
    private int[] shapeEnterPosition;
    private int[] shapeLightPositions;
    private int[][] shapePortalPositions;
    private int[] shapeReferenceVector;
    private int[] shapeToGateCorner;
    private int shapeWooshDepth;
    private int shapeWooshDepthSquared;
    private Material shapePortalMaterial;
    private Material shapeIrisMaterial;
    private Material shapeStructureMaterial;
    private Material shapeLightMaterial;
    private int shapeWooshTicks;
    private int shapeLightTicks;
    /** A disabled shape stays loaded but cannot be used to build gates. */
    private boolean shapeEnabled = true;

    /* JADX WARN: Type inference failed for: r1v10, types: [int[], int[][]] */
    /* JADX WARN: Type inference failed for: r1v2, types: [int[], int[][]] */
    public StargateShape() {
        this.shapeName = "Standard";
        this.shapeStructurePositions = new int[][]{{0, 2, 0}, {0, 3, 0}, {0, 4, 0}, {0, 1, 1}, {0, 5, 1}, {0, 0, 2}, {0, 6, 2}, {0, 6, 3}, {0, 0, 3}, {0, 0, 4}, {0, 6, 4}, {0, 5, 5}, {0, 1, 5}, {0, 2, 6}, {0, 3, 6}, {0, 4, 6}};
        this.shapeSignPosition = new int[]{0, 3, 6};
        this.shapeEnterPosition = new int[]{0, 0, 3};
        this.shapeLightPositions = new int[]{3, 4, 11, 12};
        this.shapePortalPositions = new int[][]{{0, 2, 1}, {0, 3, 1}, {0, 4, 1}, {0, 1, 2}, {0, 2, 2}, {0, 3, 2}, {0, 4, 2}, {0, 5, 2}, {0, 1, 3}, {0, 2, 3}, {0, 3, 3}, {0, 4, 3}, {0, 5, 3}, {0, 1, 4}, {0, 2, 4}, {0, 3, 4}, {0, 4, 4}, {0, 5, 4}, {0, 2, 5}, {0, 3, 5}, {0, 4, 5}};
        this.shapeReferenceVector = new int[]{0, 1, 0};
        this.shapeToGateCorner = new int[]{1, -1, 4};
        this.shapeWooshDepth = 0;
        this.shapeWooshDepthSquared = 0;
        this.shapePortalMaterial = Material.WATER;
        this.shapeIrisMaterial = Material.STONE;
        this.shapeStructureMaterial = Material.OBSIDIAN;
        this.shapeLightMaterial = Material.GLOWSTONE;
        this.shapeWooshTicks = 3;
        this.shapeLightTicks = 3;
        this.shapeEnabled = true;
    }

    /* JADX WARN: Type inference failed for: r1v10, types: [int[], int[][]] */
    /* JADX WARN: Type inference failed for: r1v2, types: [int[], int[][]] */
    public StargateShape(String[] file_data) {
        this.shapeName = "Standard";
        this.shapeStructurePositions = new int[][]{{0, 2, 0}, {0, 3, 0}, {0, 4, 0}, {0, 1, 1}, {0, 5, 1}, {0, 0, 2}, {0, 6, 2}, {0, 6, 3}, {0, 0, 3}, {0, 0, 4}, {0, 6, 4}, {0, 5, 5}, {0, 1, 5}, {0, 2, 6}, {0, 3, 6}, {0, 4, 6}};
        this.shapeSignPosition = new int[]{0, 3, 6};
        this.shapeEnterPosition = new int[]{0, 0, 3};
        this.shapeLightPositions = new int[]{3, 4, 11, 12};
        this.shapePortalPositions = new int[][]{{0, 2, 1}, {0, 3, 1}, {0, 4, 1}, {0, 1, 2}, {0, 2, 2}, {0, 3, 2}, {0, 4, 2}, {0, 5, 2}, {0, 1, 3}, {0, 2, 3}, {0, 3, 3}, {0, 4, 3}, {0, 5, 3}, {0, 1, 4}, {0, 2, 4}, {0, 3, 4}, {0, 4, 4}, {0, 5, 4}, {0, 2, 5}, {0, 3, 5}, {0, 4, 5}};
        this.shapeReferenceVector = new int[]{0, 1, 0};
        this.shapeToGateCorner = new int[]{1, -1, 4};
        this.shapeWooshDepth = 0;
        this.shapeWooshDepthSquared = 0;
        this.shapePortalMaterial = Material.WATER;
        this.shapeIrisMaterial = Material.STONE;
        this.shapeStructureMaterial = Material.OBSIDIAN;
        this.shapeLightMaterial = Material.GLOWSTONE;
        this.shapeWooshTicks = 3;
        this.shapeLightTicks = 3;
        this.shapeEnabled = true;
        setShapeSignPosition(new int[0]);
        setShapeEnterPosition(new int[0]);
        ArrayList<Integer[]> blockPositions = new ArrayList<>();
        ArrayList<Integer[]> portalPositions = new ArrayList<>();
        ArrayList<Integer> lightPositions = new ArrayList<>();
        int numBlocks = 0;
        int curWooshDepth = 0;
        int height = 0;
        int width = 0;
        for (int i = 0; i < file_data.length; i++) {
            String line = file_data[i];
            if (line.contains("Name=")) {
                this.shapeName = line.split("=")[1];
                WXTLogger.prettyLog(Level.FINE, false, "Begin parsing shape: \"" + this.shapeName + "\"");
            } else if (line.equals("GateShape=")) {
                for (int index = i + 1; index < file_data.length && file_data[index].startsWith("["); index++) {
                    if (width <= 0) {
                        Pattern p = Pattern.compile("(\\[.*?\\])");
                        Matcher pm = p.matcher(file_data[index]);
                        while (pm.find()) {
                            width++;
                        }
                    }
                    height++;
                }
                if (height <= 0 || width <= 0) {
                    WXTLogger.prettyLog(Level.SEVERE, false, "Unable to parse custom gate due to incorrect height or width: \"" + this.shapeName + "\"");
                    throw new IllegalArgumentException("Unable to parse custom gate due to incorrect height or width: \"" + this.shapeName + "\"");
                }
                WXTLogger.prettyLog(Level.FINE, false, "Shape: \"" + this.shapeName + "\" Height: \"" + Integer.toString(height) + "\" Width: \"" + Integer.toString(width) + "\"");
                for (int index2 = i + 1; index2 < file_data.length && file_data[index2].startsWith("["); index2++) {
                    Pattern p2 = Pattern.compile("(\\[.*?\\])");
                    Matcher m = p2.matcher(file_data[index2]);
                    int j = 0;
                    while (m.find()) {
                        String block = m.group(0);
                        Integer[] point = {0, Integer.valueOf((height - 1) - ((index2 - i) - 1)), Integer.valueOf((width - 1) - j)};
                        if (block.contains("O")) {
                            numBlocks++;
                            blockPositions.add(point);
                        } else if (block.contains("P")) {
                            portalPositions.add(point);
                        }
                        if (block.contains("S") || block.contains("E")) {
                            int[] pointI = new int[3];
                            for (int k = 0; k < 3; k++) {
                                pointI[k] = point[k].intValue();
                            }
                            if (block.contains("S")) {
                                setShapeSignPosition(pointI);
                            }
                            if (block.contains("E")) {
                                setShapeEnterPosition(pointI);
                            }
                        }
                        if (block.contains("L") && block.contains("O")) {
                            lightPositions.add(Integer.valueOf(numBlocks - 1));
                        }
                        j++;
                    }
                }
            } else if (line.contains("BUTTON_UP")) {
                getShapeToGateCorner()[1] = Integer.parseInt(line.split("=")[1]);
            } else if (line.contains("BUTTON_RIGHT")) {
                getShapeToGateCorner()[0] = Integer.parseInt(line.split("=")[1]);
            } else if (line.contains("BUTTON_AWAY")) {
                getShapeToGateCorner()[2] = Integer.parseInt(line.split("=")[1]);
            } else if (line.contains("WOOSH_DEPTH")) {
                curWooshDepth = Integer.parseInt(line.split("=")[1]);
            } else if (line.contains("PORTAL_MATERIAL")) {
                setShapePortalMaterial(Material.valueOf(line.split("=")[1]));
            } else if (line.contains("IRIS_MATERIAL")) {
                setShapeIrisMaterial(Material.valueOf(line.split("=")[1]));
            } else if (line.contains("STARGATE_MATERIAL")) {
                setShapeStructureMaterial(Material.valueOf(line.split("=")[1]));
            } else if (line.contains("ACTIVE_MATERIAL")) {
                setShapeLightMaterial(Material.valueOf(line.split("=")[1]));
            } else if (line.replace(" ", "").toUpperCase().startsWith("ENABLED=") && line.split("=").length > 1) {
                setShapeEnabled(de.luricos.bukkit.WormholeXTreme.Wormhole.logic.shape.ShapeEnabledFile.parse(line.split("=")[1]));
            }
        }
        WXTLogger.prettyLog(Level.FINE, false, "Stargate Sign Position: \"" + Arrays.toString(getShapeSignPosition()) + "\"");
        WXTLogger.prettyLog(Level.FINE, false, "Stargate Enter Position: \"" + Arrays.toString(getShapeEnterPosition()) + "\"");
        WXTLogger.prettyLog(Level.FINE, false, "Stargate Button Position [Left/Right,Up/Down,Forward/Back]: \"" + Arrays.toString(getShapeToGateCorner()) + "\"");
        int[][] tempPortalPositions = new int[portalPositions.size()][3];
        for (int i2 = 0; i2 < portalPositions.size(); i2++) {
            int[] point2 = new int[3];
            for (int j2 = 0; j2 < 3; j2++) {
                point2[j2] = portalPositions.get(i2)[j2].intValue();
            }
            tempPortalPositions[i2] = point2;
        }
        setShapePortalPositions(tempPortalPositions);
        WXTLogger.prettyLog(Level.FINE, false, "Stargate Portal Positions: \"" + Arrays.deepToString(getShapePortalPositions()) + "\"");
        int[] tempLightPositions = new int[lightPositions.size()];
        for (int i3 = 0; i3 < lightPositions.size(); i3++) {
            tempLightPositions[i3] = lightPositions.get(i3).intValue();
        }
        setShapeLightPositions(tempLightPositions);
        WXTLogger.prettyLog(Level.FINE, false, "Light Material Positions: \"" + Arrays.toString(getShapeLightPositions()) + "\"");
        int[][] tempStructurePositions = new int[blockPositions.size()][3];
        for (int i4 = 0; i4 < blockPositions.size(); i4++) {
            int[] point3 = new int[3];
            for (int j3 = 0; j3 < 3; j3++) {
                point3[j3] = blockPositions.get(i4)[j3].intValue();
            }
            tempStructurePositions[i4] = point3;
        }
        setShapeStructurePositions(tempStructurePositions);
        WXTLogger.prettyLog(Level.FINE, false, "Stargate Material Positions: \"" + Arrays.deepToString(getShapeStructurePositions()) + "\"");
        WXTLogger.prettyLog(Level.FINE, false, "Finished parsing shape: \"" + this.shapeName + "\"");
        setShapeWooshDepth(curWooshDepth);
        setShapeWooshDepthSquared(curWooshDepth * curWooshDepth);
    }

    public final int[] getShapeEnterPosition() {
        return (int[]) this.shapeEnterPosition.clone();
    }

    public Material getShapeIrisMaterial() {
        return this.shapeIrisMaterial;
    }

    public Material getShapeLightMaterial() {
        return this.shapeLightMaterial;
    }

    public final int[] getShapeLightPositions() {
        return (int[]) this.shapeLightPositions.clone();
    }

    public int getShapeLightTicks() {
        return this.shapeLightTicks;
    }

    /** False when this shape has been switched off with /wxshape disable. */
    public boolean isShapeEnabled() {
        return this.shapeEnabled;
    }

    public void setShapeEnabled(boolean shapeEnabled) {
        this.shapeEnabled = shapeEnabled;
    }

    public String getShapeName() {
        return this.shapeName;
    }

    public String getShapeNameKey() {
        return this.shapeName.toLowerCase();
    }

    public Material getShapePortalMaterial() {
        return this.shapePortalMaterial;
    }

    public final int[][] getShapePortalPositions() {
        return (int[][]) this.shapePortalPositions.clone();
    }

    public int[] getShapeReferenceVector() {
        return (int[]) this.shapeReferenceVector.clone();
    }

    public final int[] getShapeSignPosition() {
        return (int[]) this.shapeSignPosition.clone();
    }

    public Material getShapeStructureMaterial() {
        return this.shapeStructureMaterial;
    }

    public int[][] getShapeStructurePositions() {
        return (int[][]) this.shapeStructurePositions.clone();
    }

    public final int[] getShapeToGateCorner() {
        return (int[]) this.shapeToGateCorner.clone();
    }

    public int getShapeWooshDepth() {
        return this.shapeWooshDepth;
    }

    public int getShapeWooshDepthSquared() {
        return this.shapeWooshDepthSquared;
    }

    public int getShapeWooshTicks() {
        return this.shapeWooshTicks;
    }

    public final void setShapeEnterPosition(int[] shapeEnterPosition) {
        this.shapeEnterPosition = (int[]) shapeEnterPosition.clone();
    }

    public final void setShapeIrisMaterial(Material shapeIrisMaterial) {
        this.shapeIrisMaterial = shapeIrisMaterial;
    }

    public final void setShapeLightMaterial(Material shapeLightMaterial) {
        this.shapeLightMaterial = shapeLightMaterial;
    }

    public final void setShapeLightPositions(int[] shapeLightPositions) {
        this.shapeLightPositions = (int[]) shapeLightPositions.clone();
    }

    public void setShapeLightTicks(int shapeLightTicks) {
        this.shapeLightTicks = shapeLightTicks;
    }

    public void setShapeName(String shapeName) {
        this.shapeName = shapeName;
    }

    public final void setShapePortalMaterial(Material shapePortalMaterial) {
        this.shapePortalMaterial = shapePortalMaterial;
    }

    public final void setShapePortalPositions(int[][] shapePortalPositions) {
        this.shapePortalPositions = (int[][]) shapePortalPositions.clone();
    }

    public void setShapeReferenceVector(int[] shapeReferenceVector) {
        this.shapeReferenceVector = (int[]) shapeReferenceVector.clone();
    }

    public final void setShapeSignPosition(int[] shapeSignPosition) {
        this.shapeSignPosition = (int[]) shapeSignPosition.clone();
    }

    public final void setShapeStructureMaterial(Material shapeStructureMaterial) {
        this.shapeStructureMaterial = shapeStructureMaterial;
    }

    public final void setShapeStructurePositions(int[][] shapeStructurePositions) {
        this.shapeStructurePositions = (int[][]) shapeStructurePositions.clone();
    }

    public void setShapeToGateCorner(int[] shapeToGateCorner) {
        this.shapeToGateCorner = (int[]) shapeToGateCorner.clone();
    }

    public final void setShapeWooshDepth(int shapeWooshDepth) {
        this.shapeWooshDepth = shapeWooshDepth;
    }

    public final void setShapeWooshDepthSquared(int shapeWooshDepthSquared) {
        this.shapeWooshDepthSquared = shapeWooshDepthSquared;
    }

    public void setShapeWooshTicks(int shapeWooshTicks) {
        this.shapeWooshTicks = shapeWooshTicks;
    }
}
