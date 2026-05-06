package de.luricos.bukkit.WormholeXTreme.Wormhole.model;

import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/model/StargateShapeLayer.class */
public class StargateShapeLayer {
    private ArrayList<Integer[]> layerBlockPositions = new ArrayList<>();
    private int[] layerNameSignPosition = null;
    private int[] layerPlayerExitPosition = null;
    private int[] layerMinecartExitPosition = null;
    private int[] layerActivationPosition = null;
    private int[] layerIrisActivationPosition = null;
    private int[] layerDialSignPosition = null;
    private int[] layerRedstoneDialActivationPosition = null;
    private int[] layerRedstoneSignActivationPosition = null;
    private int[] layerRedstoneGateActivatedPosition = null;
    private ArrayList<ArrayList<Integer[]>> layerLightPositions = new ArrayList<>();
    private ArrayList<ArrayList<Integer[]>> layerWooshPositions = new ArrayList<>();
    private ArrayList<Integer[]> layerPortalPositions = new ArrayList<>();

    protected StargateShapeLayer(String[] layerLines, int height, int width) {
        int numBlocks = 0;
        for (int i = 0; i < layerLines.length; i++) {
            if (Pattern.compile("\\[(.+?)\\]") == null) {
                WXTLogger.prettyLog(Level.CONFIG, false, "Stargate Sign Position: \"" + Arrays.toString(getLayerNameSignPosition()) + "\"");
            }
            Matcher m = Pattern.compile("\\[(.+?)\\]").matcher(layerLines[i]);
            int j = 0;
            while (m.find()) {
                Integer[] point = {0, Integer.valueOf((height - 1) - i), Integer.valueOf((width - 1) - j)};
                String[] arr$ = m.group(1).split(":");
                for (String mod : arr$) {
                    if (mod.equalsIgnoreCase("S")) {
                        numBlocks++;
                        getLayerBlockPositions().add(point);
                    } else if (mod.equalsIgnoreCase("P")) {
                        getLayerPortalPositions().add(point);
                    } else if (mod.equalsIgnoreCase("N") || mod.equalsIgnoreCase("EP") || mod.equalsIgnoreCase("EM") || mod.equalsIgnoreCase("A") || mod.equalsIgnoreCase("D") || mod.equalsIgnoreCase("IA") || mod.equalsIgnoreCase("RA") || mod.equalsIgnoreCase("RD") || mod.equalsIgnoreCase("RS")) {
                        int[] pointI = new int[3];
                        for (int k = 0; k < 3; k++) {
                            pointI[k] = point[k].intValue();
                        }
                        if (mod.equalsIgnoreCase("N")) {
                            setLayerNameSignPosition(pointI);
                        }
                        if (mod.equalsIgnoreCase("EP")) {
                            setLayerPlayerExitPosition(pointI);
                        }
                        if (mod.equalsIgnoreCase("EM")) {
                            setLayerMinecartExitPosition(pointI);
                        }
                        if (mod.equalsIgnoreCase("A")) {
                            setLayerActivationPosition(pointI);
                        }
                        if (mod.equalsIgnoreCase("D")) {
                            setLayerDialSignPosition(pointI);
                        }
                        if (mod.equalsIgnoreCase("IA")) {
                            setLayerIrisActivationPosition(pointI);
                        }
                        if (mod.equalsIgnoreCase("RA")) {
                            setLayerRedstoneGateActivatedPosition(pointI);
                        }
                        if (mod.equalsIgnoreCase("RD")) {
                            setLayerRedstoneDialActivationPosition(pointI);
                        }
                        if (mod.equalsIgnoreCase("RS")) {
                            setLayerRedstoneSignActivationPosition(pointI);
                        }
                    } else if (mod.contains("L") || mod.contains("l")) {
                        int light_iteration = mod.contains("#") ? Integer.parseInt(mod.split("#")[1]) : 1;
                        while (getLayerLightPositions().size() <= light_iteration) {
                            getLayerLightPositions().add(null);
                        }
                        if (getLayerLightPositions().get(light_iteration) == null) {
                            ArrayList<Integer[]> new_it = new ArrayList<>();
                            getLayerLightPositions().set(light_iteration, new_it);
                        }
                        getLayerLightPositions().get(light_iteration).add(point);
                        WXTLogger.prettyLog(Level.CONFIG, false, "Light Material Position (Order:" + light_iteration + " Position:" + Arrays.toString(point) + ")");
                    } else if (mod.contains("W") || mod.contains("w")) {
                        int w_iteration = mod.contains("#") ? Integer.parseInt(mod.split("#")[1]) : 1;
                        while (getLayerWooshPositions().size() <= w_iteration) {
                            getLayerWooshPositions().add(null);
                        }
                        if (getLayerWooshPositions().get(w_iteration) == null) {
                            ArrayList<Integer[]> new_it2 = new ArrayList<>();
                            getLayerWooshPositions().set(w_iteration, new_it2);
                        }
                        getLayerWooshPositions().get(w_iteration).add(point);
                        WXTLogger.prettyLog(Level.CONFIG, false, "Woosh Position (Order:" + w_iteration + " Position:" + Arrays.toString(point) + ")");
                    }
                }
                j++;
            }
        }
        WXTLogger.prettyLog(Level.CONFIG, false, "Stargate Sign Position: \"" + Arrays.toString(getLayerNameSignPosition()) + "\"");
        WXTLogger.prettyLog(Level.CONFIG, false, "Stargate Player Exit Position: \"" + Arrays.toString(getLayerPlayerExitPosition()) + "\"");
        WXTLogger.prettyLog(Level.CONFIG, false, "Stargate Minecart Exit Position: \"" + Arrays.toString(getLayerMinecartExitPosition()) + "\"");
        WXTLogger.prettyLog(Level.CONFIG, false, "Stargate Activation Position: \"" + Arrays.toString(getLayerActivationPosition()) + "\"");
        WXTLogger.prettyLog(Level.CONFIG, false, "Stargate Iris Activation Position: \"" + Arrays.toString(getLayerIrisActivationPosition()) + "\"");
        WXTLogger.prettyLog(Level.CONFIG, false, "Stargate Dial Sign Position: \"" + Arrays.toString(getLayerDialSignPosition()) + "\"");
        WXTLogger.prettyLog(Level.CONFIG, false, "Stargate Redstone Dial Activation Position: \"" + Arrays.toString(getLayerRedstoneDialActivationPosition()) + "\"");
        WXTLogger.prettyLog(Level.CONFIG, false, "Stargate Redstone Sign Activation Position: \"" + Arrays.toString(getLayerRedstoneSignActivationPosition()) + "\"");
        WXTLogger.prettyLog(Level.CONFIG, false, "Stargate Redstone Gate Activated Position: \"" + Arrays.toString(getLayerRedstoneGateActivatedPosition()) + "\"");
    }

    public int[] getLayerActivationPosition() {
        return this.layerActivationPosition != null ? (int[]) this.layerActivationPosition.clone() : new int[0];
    }

    public ArrayList<Integer[]> getLayerBlockPositions() {
        return this.layerBlockPositions;
    }

    public int[] getLayerDialSignPosition() {
        return this.layerDialSignPosition != null ? (int[]) this.layerDialSignPosition.clone() : new int[0];
    }

    public int[] getLayerIrisActivationPosition() {
        return this.layerIrisActivationPosition != null ? (int[]) this.layerIrisActivationPosition.clone() : new int[0];
    }

    public ArrayList<ArrayList<Integer[]>> getLayerLightPositions() {
        return this.layerLightPositions;
    }

    public int[] getLayerMinecartExitPosition() {
        return this.layerMinecartExitPosition != null ? (int[]) this.layerMinecartExitPosition.clone() : new int[0];
    }

    public int[] getLayerNameSignPosition() {
        return this.layerNameSignPosition != null ? (int[]) this.layerNameSignPosition.clone() : new int[0];
    }

    public int[] getLayerPlayerExitPosition() {
        return this.layerPlayerExitPosition != null ? (int[]) this.layerPlayerExitPosition.clone() : new int[0];
    }

    public ArrayList<Integer[]> getLayerPortalPositions() {
        return this.layerPortalPositions;
    }

    public int[] getLayerRedstoneDialActivationPosition() {
        return this.layerRedstoneDialActivationPosition != null ? (int[]) this.layerRedstoneDialActivationPosition.clone() : new int[0];
    }

    public int[] getLayerRedstoneGateActivatedPosition() {
        return this.layerRedstoneGateActivatedPosition != null ? (int[]) this.layerRedstoneGateActivatedPosition.clone() : new int[0];
    }

    public int[] getLayerRedstoneSignActivationPosition() {
        return this.layerRedstoneSignActivationPosition != null ? (int[]) this.layerRedstoneSignActivationPosition.clone() : new int[0];
    }

    public ArrayList<ArrayList<Integer[]>> getLayerWooshPositions() {
        return this.layerWooshPositions;
    }

    public void setLayerActivationPosition(int[] layerActivationPosition) {
        this.layerActivationPosition = (int[]) layerActivationPosition.clone();
    }

    public void setLayerBlockPositions(ArrayList<Integer[]> layerBlockPositions) {
        this.layerBlockPositions = layerBlockPositions;
    }

    public void setLayerDialSignPosition(int[] layerDialSignPosition) {
        this.layerDialSignPosition = (int[]) layerDialSignPosition.clone();
    }

    public void setLayerIrisActivationPosition(int[] layerIrisActivationPosition) {
        this.layerIrisActivationPosition = (int[]) layerIrisActivationPosition.clone();
    }

    public void setLayerLightPositions(ArrayList<ArrayList<Integer[]>> layerLightPositions) {
        this.layerLightPositions = layerLightPositions;
    }

    public void setLayerMinecartExitPosition(int[] layerMinecartExitPosition) {
        this.layerMinecartExitPosition = (int[]) layerMinecartExitPosition.clone();
    }

    public void setLayerNameSignPosition(int[] layerNameSignPosition) {
        this.layerNameSignPosition = (int[]) layerNameSignPosition.clone();
    }

    public void setLayerPlayerExitPosition(int[] layerPlayerExitPosition) {
        this.layerPlayerExitPosition = (int[]) layerPlayerExitPosition.clone();
    }

    public void setLayerPortalPositions(ArrayList<Integer[]> layerPortalPositions) {
        this.layerPortalPositions = layerPortalPositions;
    }

    public void setLayerRedstoneDialActivationPosition(int[] layerRedstoneDialActivationPosition) {
        this.layerRedstoneDialActivationPosition = (int[]) layerRedstoneDialActivationPosition.clone();
    }

    public void setLayerRedstoneGateActivatedPosition(int[] layerRedstoneGateActivatedPosition) {
        this.layerRedstoneGateActivatedPosition = (int[]) layerRedstoneGateActivatedPosition.clone();
    }

    public void setLayerRedstoneSignActivationPosition(int[] layerRedstoneSignActivationPosition) {
        this.layerRedstoneSignActivationPosition = (int[]) layerRedstoneSignActivationPosition.clone();
    }

    public void setLayerWooshPositions(ArrayList<ArrayList<Integer[]>> layerWooshPositions) {
        this.layerWooshPositions = layerWooshPositions;
    }
}
