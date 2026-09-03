package de.luricos.bukkit.WormholeXTreme.Wormhole.model;

import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.shape.ShapeEnabledFile;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Material;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/model/Stargate3DShape.class */
public class Stargate3DShape extends StargateShape {
    private final ArrayList<StargateShapeLayer> shapeLayers = new ArrayList<>();
    private int shapeActivationLayer = -1;
    private int shapeSignLayer = -1;
    private boolean shapeRedstoneActivated = false;

    public Stargate3DShape(String[] fileLines) {
        setShapeSignPosition(new int[0]);
        setShapeEnterPosition(new int[0]);
        int height = 0;
        int width = 0;
        int wooshDepth = 0;
        int i = 0;
        while (i < fileLines.length) {
            String line = fileLines[i];
            // Settings are matched against a whitespace-free copy: the shape
            // writer emits "LIGHT_TICKS = 2;" and the old exact-match on
            // "LIGHT_TICKS=" quietly missed it. Name= is read from the raw
            // line, since a shape name may contain spaces.
            String setting = line.trim();
            int semicolon = setting.indexOf(';');
            if (semicolon >= 0) {
                setting = setting.substring(0, semicolon);
            }
            setting = setting.replace(" ", "").replace("\t", "");
            if (!line.startsWith("#")) {
                if (line.contains("Name=")) {
                    setShapeName(line.split("=")[1]);
                    WXTLogger.prettyLog(Level.FINE, false, "Begin parsing shape: \"" + getShapeName() + "\"");
                } else if (line.equals("GateShape=")) {
                    int index = i;
                    while (!fileLines[index].startsWith("[")) {
                        index++;
                    }
                    while (fileLines[index].startsWith("[")) {
                        if (width <= 0) {
                            Pattern p = Pattern.compile("(\\[.*?\\])");
                            Matcher m = p.matcher(fileLines[index]);
                            while (m.find()) {
                                width++;
                            }
                        }
                        height++;
                        index++;
                    }
                    if (height <= 0 || width <= 0) {
                        WXTLogger.prettyLog(Level.SEVERE, false, "Unable to parse custom gate due to incorrect height or width: \"" + getShapeName() + "\"");
                        throw new IllegalArgumentException("Unable to parse custom gate due to incorrect height or width: \"" + getShapeName() + "\"");
                    }
                    WXTLogger.prettyLog(Level.FINE, false, "Shape: \"" + getShapeName() + "\" Height: \"" + Integer.toString(height) + "\" Width: \"" + Integer.toString(width) + "\"");
                } else if (line.startsWith("Layer")) {
                    int layer = Integer.valueOf(line.trim().split("[#=]")[1]).intValue();
                    i++;
                    String[] layerLines = new String[height];
                    int line_index = 0;
                    while (true) {
                        if (!fileLines[i].startsWith("[") && !fileLines[i].startsWith("#")) {
                            break;
                        }
                        WXTLogger.prettyLog(Level.FINE, false, "Layer=" + layer + " i=" + i + " line_index=" + line_index + " Line=" + fileLines[i]);
                        layerLines[line_index] = fileLines[i];
                        i++;
                        if (!fileLines[i].startsWith("#")) {
                            line_index++;
                        }
                    }
                    StargateShapeLayer ssl = new StargateShapeLayer(layerLines, height, width);
                    while (getShapeLayers().size() <= layer) {
                        getShapeLayers().add(null);
                    }
                    getShapeLayers().set(layer, ssl);
                    if (ssl.getLayerActivationPosition().length > 0) {
                        setShapeActivationLayer(layer);
                    }
                    if (ssl.getLayerDialSignPosition().length > 0) {
                        setShapeSignLayer(layer);
                    }
                    if (ssl.getLayerPlayerExitPosition() != null && ssl.getLayerPlayerExitPosition().length == 3) {
                        setShapeEnterPosition(ssl.getLayerPlayerExitPosition());
                    }
                    if (ssl.getLayerWooshPositions().size() > 0) {
                        wooshDepth++;
                    }
                } else if (setting.contains("PORTAL_MATERIAL=") && setting.split("=").length > 1) {
                    setShapePortalMaterial(Material.valueOf(setting.split("=")[1]));
                } else if (setting.contains("IRIS_MATERIAL=") && setting.split("=").length > 1) {
                    setShapeIrisMaterial(Material.valueOf(setting.split("=")[1]));
                } else if (setting.contains("STARGATE_MATERIAL=") && setting.split("=").length > 1) {
                    setShapeStructureMaterial(Material.valueOf(setting.split("=")[1]));
                } else if (setting.contains("ACTIVE_MATERIAL=") && setting.split("=").length > 1) {
                    setShapeLightMaterial(Material.valueOf(setting.split("=")[1]));
                } else if (setting.contains("LIGHT_TICKS=") && setting.split("=").length > 1) {
                    setShapeLightTicks(Integer.valueOf(setting.split("=")[1]).intValue());
                } else if (setting.contains("WOOSH_TICKS=") && setting.split("=").length > 1) {
                    setShapeWooshTicks(Integer.valueOf(setting.split("=")[1]).intValue());
                } else if (setting.startsWith("REDSTONE_ACTIVATED=") && setting.split("=").length > 1) {
                    setShapeRedstoneActivated(Boolean.valueOf(setting.split("=")[1]).booleanValue());
                } else if (setting.toUpperCase().startsWith("ENABLED=") && setting.split("=").length > 1) {
                    setShapeEnabled(ShapeEnabledFile.parse(setting.split("=")[1]));
                }
            }
            i++;
        }
        setShapeWooshDepth(wooshDepth > 0 ? wooshDepth : 0);
        setShapeWooshDepthSquared(getShapeWooshDepth() * getShapeWooshDepth());
        if (getShapeEnterPosition().length != 3) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Shape: \"" + getShapeName() + "\" does not have an enterance/exit point for players to teleport in. This will cause errors.");
            throw new IllegalArgumentException("Shape: \"" + getShapeName() + "\" does not have an enterance point for players to teleport in. This will cause errors.");
        }
        WXTLogger.prettyLog(Level.FINE, false, "Finished parsing shape: \"" + getShapeName() + "\"");
    }

    public int getShapeActivationLayer() {
        return this.shapeActivationLayer;
    }

    public ArrayList<StargateShapeLayer> getShapeLayers() {
        return this.shapeLayers;
    }

    public int getShapeSignLayer() {
        return this.shapeSignLayer;
    }

    public boolean isShapeRedstoneActivated() {
        return this.shapeRedstoneActivated;
    }

    private void setShapeActivationLayer(int shapeActivationLayer) {
        this.shapeActivationLayer = shapeActivationLayer;
    }

    private void setShapeRedstoneActivated(boolean shapeRedstoneActivated) {
        this.shapeRedstoneActivated = shapeRedstoneActivated;
    }

    private void setShapeSignLayer(int shapeSignLayer) {
        this.shapeSignLayer = shapeSignLayer;
    }
}
