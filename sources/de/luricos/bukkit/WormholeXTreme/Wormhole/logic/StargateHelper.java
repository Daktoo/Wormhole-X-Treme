package de.luricos.bukkit.WormholeXTreme.Wormhole.logic;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholeActivationLayerNotFoundException;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateUpdateRunnable;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate3DShape;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateNetwork;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateShape;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateShapeLayer;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.DataUtils;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WorldUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/logic/StargateHelper.class */
public class StargateHelper {

    private static Material getMaterialById(int id) {
        for (Material m : Material.values()) {
            try {
                if (!m.isLegacy() && m.getId() == id) return m;
            } catch (Exception ignored) {}
        }
        return null;
    }


    private static final byte StargateSaveVersion = 9;
    private static final ConcurrentHashMap<String, StargateShape> stargateShapes = new ConcurrentHashMap<>();
    private static final byte[] emptyBlock = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public static Stargate checkStargate(Block buttonBlock, BlockFace facing) {
        Stargate bestMatch = null;
        StargateShape bestShape = null;
        Iterator<String> it = getStargateShapes().keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            StargateShape shape = getStargateShapes().get(key);
            if (shape == null) {
                continue;
            }
            Stargate s = shape instanceof Stargate3DShape
                    ? checkStargate3D(buttonBlock, facing, (Stargate3DShape) shape, false)
                    : checkStargate(buttonBlock, facing, shape, false);
            if (s != null) {
                WXTLogger.prettyLog(Level.FINE, false, "Shape: " + shape.getShapeName() + " matched.");
                // Prefer the most specific match: when multiple shapes match the
                // same physical structure (e.g. Standard vs StandardSignDial,
                // where SignDial is a structural superset requiring an extra
                // dial-sign holder block), pick the one with the most required
                // structure blocks, since that is the more specific/correct shape.
                if (bestMatch == null || s.getGateStructureBlocks().size() > bestMatch.getGateStructureBlocks().size()) {
                    bestMatch = s;
                    bestShape = shape;
                }
            }
        }
        if (bestShape != null) {
            WXTLogger.prettyLog(Level.FINE, false, "Shape: " + bestShape.getShapeName() + " was selected as the best match!");
        }
        return bestMatch;
    }

    public static Stargate checkStargate(Block buttonBlock, BlockFace facing, StargateShape shape) {
        if (shape instanceof Stargate3DShape) {
            return checkStargate3D(buttonBlock, facing, (Stargate3DShape) shape, true);
        }
        return checkStargate(buttonBlock, facing, shape, true);
    }

    private static Stargate checkStargate(Block buttonBlock, BlockFace facing, StargateShape shape, boolean create) {
        Block bLoc;
        BlockFace opposite = WorldUtils.getInverseDirection(facing);
        Block holdingBlock = buttonBlock.getRelative(opposite);
        if (isStargateMaterial(holdingBlock, shape)) {
            Stargate tempGate = new Stargate();
            tempGate.setGateWorld(buttonBlock.getWorld());
            tempGate.setGateName("");
            tempGate.setGateDialLeverBlock(buttonBlock);
            tempGate.setGateFacing(facing);
            tempGate.getGateStructureBlocks().add(buttonBlock.getLocation());
            tempGate.setGateShape(shape);
            if (!isStargateMaterial(holdingBlock.getRelative(BlockFace.DOWN), tempGate.getGateShape())) {
                return null;
            }
            Block possibleSignHolder = holdingBlock.getRelative(WorldUtils.getPerpendicularRightDirection(opposite));
            if (isStargateMaterial(possibleSignHolder, tempGate.getGateShape())) {
                Block signBlock = possibleSignHolder.getRelative(tempGate.getGateFacing());
                if (!tryCreateGateSign(signBlock, tempGate) && tempGate.isGateSignPowered()) {
                    return tempGate;
                }
            }
            int[] facingVector = {0, 0, 0};
            World w = buttonBlock.getWorld();
            switch (AnonymousClass2.$SwitchMap$org$bukkit$block$BlockFace[facing.ordinal()]) {
                case 1:
                    facingVector[0] = 1;
                    break;
                case 2:
                    facingVector[0] = -1;
                    break;
                case 3:
                    facingVector[2] = 1;
                    break;
                case 4:
                    facingVector[2] = -1;
                    break;
                case 5:
                    facingVector[1] = -1;
                    break;
                case 6:
                    facingVector[1] = 1;
                    break;
            }
            int[] directionVector = {0, 0, 0};
            int[] startingPosition = {0, 0, 0};
            directionVector[0] = (facingVector[1] * shape.getShapeReferenceVector()[2]) - (facingVector[2] * shape.getShapeReferenceVector()[1]);
            directionVector[1] = (facingVector[2] * shape.getShapeReferenceVector()[0]) - (facingVector[0] * shape.getShapeReferenceVector()[2]);
            directionVector[2] = (facingVector[0] * shape.getShapeReferenceVector()[1]) - (facingVector[1] * shape.getShapeReferenceVector()[0]);
            startingPosition[0] = buttonBlock.getX() + (facingVector[0] * shape.getShapeToGateCorner()[2]) + (directionVector[0] * shape.getShapeToGateCorner()[0]);
            startingPosition[1] = buttonBlock.getY() + shape.getShapeToGateCorner()[1];
            startingPosition[2] = buttonBlock.getZ() + (facingVector[2] * shape.getShapeToGateCorner()[2]) + (directionVector[2] * shape.getShapeToGateCorner()[0]);
            for (int i = 0; i < shape.getShapeStructurePositions().length; i++) {
                int[] bVect = shape.getShapeStructurePositions()[i];
                int[] blockLocation = {bVect[2] * directionVector[0] * (-1), bVect[1], bVect[2] * directionVector[2] * (-1)};
                Block maybeBlock = w.getBlockAt(blockLocation[0] + startingPosition[0], blockLocation[1] + startingPosition[1], blockLocation[2] + startingPosition[2]);
                if (create) {
                    maybeBlock.setType(tempGate.getGateShape().getShapeStructureMaterial());
                }
                if (isStargateMaterial(maybeBlock, tempGate.getGateShape())) {
                    tempGate.getGateStructureBlocks().add(maybeBlock.getLocation());
                    int[] arr$ = shape.getShapeLightPositions();
                    for (int lightPosition : arr$) {
                        if (lightPosition == i) {
                            while (tempGate.getGateLightBlocks().size() < 2) {
                                tempGate.getGateLightBlocks().add(new ArrayList<>());
                            }
                            tempGate.getGateLightBlocks().get(1).add(maybeBlock.getLocation());
                        }
                    }
                } else {
                    if (tempGate.getGateNetwork() != null) {
                        tempGate.getGateNetwork().getNetworkGateList().remove(tempGate);
                        if (tempGate.isGateSignPowered()) {
                            tempGate.getGateNetwork().getNetworkSignGateList().remove(tempGate);
                            return null;
                        }
                        return null;
                    }
                    return null;
                }
            }
            if (shape.getShapeSignPosition().length > 0) {
                int[] signLocationArray = {shape.getShapeSignPosition()[2] * directionVector[0] * (-1), shape.getShapeSignPosition()[1], shape.getShapeSignPosition()[2] * directionVector[2] * (-1)};
                Block nameBlock = w.getBlockAt(signLocationArray[0] + startingPosition[0], signLocationArray[1] + startingPosition[1], signLocationArray[2] + startingPosition[2]);
                tempGate.setGateNameBlockHolder(nameBlock);
            }
            int[] teleportLocArray = {shape.getShapeEnterPosition()[2] * directionVector[0] * (-1), shape.getShapeEnterPosition()[1], shape.getShapeEnterPosition()[2] * directionVector[2] * (-1)};
            Block teleBlock = w.getBlockAt(teleportLocArray[0] + startingPosition[0], teleportLocArray[1] + startingPosition[1], teleportLocArray[2] + startingPosition[2]);
            Block relative = teleBlock.getRelative(facing);
            while (true) {
                bLoc = relative;
                if (bLoc.getType() == Material.AIR || bLoc.getType() == Material.WATER) {
                    break;
                }
                relative = bLoc.getRelative(BlockFace.UP);
            }
            Location teleLoc = bLoc.getLocation();
            teleLoc.setYaw(WorldUtils.getDegreesFromBlockFace(facing).floatValue());
            teleLoc.setPitch(0.0f);
            teleLoc.setX(teleLoc.getX() + 0.5d);
            teleLoc.setY(teleLoc.getY() + 0.66d);
            teleLoc.setZ(teleLoc.getZ() + 0.5d);
            tempGate.setGatePlayerTeleportLocation(teleLoc);
            int[][] arr$2 = shape.getShapePortalPositions();
            for (int[] bVect2 : arr$2) {
                int[] blockLocation2 = {bVect2[2] * directionVector[0] * (-1), bVect2[1], bVect2[2] * directionVector[2] * (-1)};
                Block maybeBlock2 = w.getBlockAt(blockLocation2[0] + startingPosition[0], blockLocation2[1] + startingPosition[1], blockLocation2[2] + startingPosition[2]);
                if (maybeBlock2.getType() == Material.AIR) {
                    tempGate.getGatePortalBlocks().add(maybeBlock2.getLocation());
                } else {
                    if (tempGate.getGateNetwork() != null) {
                        tempGate.getGateNetwork().getNetworkGateList().remove(tempGate);
                        return null;
                    }
                    return null;
                }
            }
            setupSignGateNetwork(tempGate);
            return tempGate;
        }
        return null;
    }

    /* JADX INFO: renamed from: de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateHelper$2, reason: invalid class name */
    /* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/logic/StargateHelper$2.class */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$org$bukkit$block$BlockFace = new int[BlockFace.values().length];

        static {
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.NORTH.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.SOUTH.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.EAST.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.WEST.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.UP.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$org$bukkit$block$BlockFace[BlockFace.DOWN.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    private static Stargate checkStargate3D(Block buttonBlock, BlockFace facing, Stargate3DShape shape, boolean create) {
        try {
            Stargate s = new Stargate();
            if (shape.getShapeActivationLayer() == -1) {
                throw new WormholeActivationLayerNotFoundException("Shape '" + shape.getShapeName() + "' is invalid. No ActivationLayer found!");
            }
            BlockFace opposite = WorldUtils.getInverseDirection(facing);
            Block activationBlock = buttonBlock.getRelative(opposite);
            StargateShapeLayer act_layer = shape.getShapeLayers().get(shape.getShapeActivationLayer());
            s.setGateWorld(buttonBlock.getWorld());
            s.setGateDialLeverBlock(buttonBlock);
            s.getGateStructureBlocks().add(s.getGateDialLeverBlock().getLocation());
            s.setGateShape(shape);
            s.setGateFacing(facing);
            int[] facingVector = {0, 0, 0};
            switch (AnonymousClass2.$SwitchMap$org$bukkit$block$BlockFace[facing.ordinal()]) {
                case 1:
                    facingVector[2] = -1;
                    break;
                case 2:
                    facingVector[2] = 1;
                    break;
                case 3:
                    facingVector[0] = 1;
                    break;
                case 4:
                    facingVector[0] = -1;
                    break;
                case 5:
                    facingVector[1] = 1;
                    break;
                case 6:
                    facingVector[1] = -1;
                    break;
            }
            int[] directionVector = {0, 0, 0};
            int[] startingPosition = {0, 0, 0};
            directionVector[0] = (facingVector[1] * shape.getShapeReferenceVector()[2]) - (facingVector[2] * shape.getShapeReferenceVector()[1]);
            directionVector[1] = (facingVector[2] * shape.getShapeReferenceVector()[0]) - (facingVector[0] * shape.getShapeReferenceVector()[2]);
            directionVector[2] = (facingVector[0] * shape.getShapeReferenceVector()[1]) - (facingVector[1] * shape.getShapeReferenceVector()[0]);
            startingPosition[0] = activationBlock.getX() - (directionVector[0] * act_layer.getLayerActivationPosition()[2]);
            startingPosition[1] = activationBlock.getY() - act_layer.getLayerActivationPosition()[1];
            startingPosition[2] = activationBlock.getZ() - (directionVector[2] * act_layer.getLayerActivationPosition()[2]);
            for (int i = 0; i < shape.getShapeLayers().size(); i++) {
                if (shape.getShapeLayers().size() > i && shape.getShapeLayers().get(i) != null) {
                    int layerOffset = shape.getShapeActivationLayer() - i;
                    int[] layerStarter = {startingPosition[0] - (facingVector[0] * layerOffset), startingPosition[1], startingPosition[2] - (facingVector[2] * layerOffset)};
                    if (!checkStargateLayer(shape.getShapeLayers().get(i), layerStarter, directionVector, s, create)) {
                        if (s.getGateNetwork() != null) {
                            s.getGateNetwork().getNetworkGateList().remove(s);
                            if (s.isGateSignPowered()) {
                                s.getGateNetwork().getNetworkSignGateList().remove(s);
                                return null;
                            }
                            return null;
                        }
                        return null;
                    }
                }
            }
            if (shape.getShapeSignPosition().length > 0) {
                int[] signLocationArray = {shape.getShapeSignPosition()[2] * directionVector[0] * (-1), shape.getShapeSignPosition()[1], shape.getShapeSignPosition()[2] * directionVector[2] * (-1)};
                Block nameBlock = s.getGateWorld().getBlockAt(signLocationArray[0] + startingPosition[0], signLocationArray[1] + startingPosition[1], signLocationArray[2] + startingPosition[2]);
                s.setGateNameBlockHolder(nameBlock);
            }
            if (shape.isShapeRedstoneActivated()) {
                s.setGateRedstonePowered(true);
            }
            setupSignGateNetwork(s);
            return s;
        } catch (WormholeActivationLayerNotFoundException e) {
            return null;
        }
    }

    private static boolean checkStargateLayer(StargateShapeLayer layer, int[] lowerCorner, int[] directionVector, Stargate tempGate, boolean create) {
        Block teleBlock;
        Block teleBlock2;
        World w = tempGate.getGateWorld();
        for (int i = 0; i < layer.getLayerBlockPositions().size(); i++) {
            Block maybeBlock = getBlockFromVector(layer.getLayerBlockPositions().get(i), directionVector, lowerCorner, w);
            if (create) {
                maybeBlock.setType(tempGate.getGateShape().getShapeStructureMaterial());
            }
            if (isStargateMaterial(maybeBlock, tempGate.getGateShape())) {
                tempGate.getGateStructureBlocks().add(maybeBlock.getLocation());
            } else {
                return false;
            }
        }
        for (int i2 = 0; i2 < layer.getLayerPortalPositions().size(); i2++) {
            Block maybeBlock2 = getBlockFromVector(layer.getLayerPortalPositions().get(i2), directionVector, lowerCorner, w);
            if (create) {
                maybeBlock2.setType(Material.AIR);
            }
            if (maybeBlock2.getType() == Material.AIR) {
                tempGate.getGatePortalBlocks().add(maybeBlock2.getLocation());
            } else {
                return false;
            }
        }
        if (layer.getLayerPlayerExitPosition().length > 0) {
            Block blockFromVector = getBlockFromVector(layer.getLayerPlayerExitPosition(), directionVector, lowerCorner, w);
            while (true) {
                teleBlock2 = blockFromVector;
                if (teleBlock2.getType() == Material.AIR || teleBlock2.getType() == Material.WATER) {
                    break;
                }
                blockFromVector = teleBlock2.getRelative(BlockFace.UP);
            }
            Location teleLoc = teleBlock2.getLocation();
            teleLoc.setYaw(WorldUtils.getDegreesFromBlockFace(tempGate.getGateFacing()).floatValue());
            teleLoc.setPitch(0.0f);
            teleLoc.setX(teleLoc.getX() + 0.5d);
            teleLoc.setZ(teleLoc.getZ() + 0.5d);
            tempGate.setGatePlayerTeleportLocation(teleLoc);
        }
        if (layer.getLayerMinecartExitPosition().length > 0) {
            Block blockFromVector2 = getBlockFromVector(layer.getLayerMinecartExitPosition(), directionVector, lowerCorner, w);
            while (true) {
                teleBlock = blockFromVector2;
                if (teleBlock.getType().equals(Material.AIR) || teleBlock.getType().equals(Material.WATER)) {
                    break;
                }
                blockFromVector2 = teleBlock.getRelative(BlockFace.UP);
            }
            Location teleLoc2 = teleBlock.getLocation();
            teleLoc2.setYaw(WorldUtils.getDegreesFromBlockFace(tempGate.getGateFacing()).floatValue());
            teleLoc2.setPitch(0.0f);
            teleLoc2.setX(teleLoc2.getX() + 0.5d);
            teleLoc2.setZ(teleLoc2.getZ() + 0.5d);
            tempGate.setGateMinecartTeleportLocation(teleLoc2);
        }
        for (int i3 = 0; i3 < layer.getLayerWooshPositions().size(); i3++) {
            if (tempGate.getGateWooshBlocks().size() < i3 + 1) {
                tempGate.getGateWooshBlocks().add(new ArrayList<>());
            }
            if (layer.getLayerWooshPositions().get(i3) != null) {
                for (Integer[] position : layer.getLayerWooshPositions().get(i3)) {
                    Block wooshBlock = getBlockFromVector(position, directionVector, lowerCorner, w);
                    tempGate.getGateWooshBlocks().get(i3).add(wooshBlock.getLocation());
                }
            }
        }
        for (int i4 = 0; i4 < layer.getLayerLightPositions().size(); i4++) {
            if (tempGate.getGateLightBlocks().size() < i4 + 1) {
                tempGate.getGateLightBlocks().add(new ArrayList<>());
            }
            if (layer.getLayerLightPositions().get(i4) != null) {
                for (Integer[] position2 : layer.getLayerLightPositions().get(i4)) {
                    Block lightBlock = getBlockFromVector(position2, directionVector, lowerCorner, w);
                    tempGate.getGateLightBlocks().get(i4).add(lightBlock.getLocation());
                }
            }
        }
        if (layer.getLayerDialSignPosition().length > 0) {
            Block signBlockHolder = getBlockFromVector(layer.getLayerDialSignPosition(), directionVector, lowerCorner, w);
            // The :D position must actually be built out of the gate's structure
            // material for this to be a genuine SignDial-capable gate. Without
            // this check, a regular (non-dial) shape whose template marks this
            // coordinate as "ignored" would also match a physically-built
            // SignDial gate, causing the wrong shape to be detected.
            if (create) {
                signBlockHolder.setType(tempGate.getGateShape().getShapeStructureMaterial());
            } else if (!isStargateMaterial(signBlockHolder, tempGate.getGateShape())) {
                return false;
            }
            tempGate.getGateStructureBlocks().add(signBlockHolder.getLocation());
            Block signBlock = signBlockHolder.getRelative(tempGate.getGateFacing());
            if (!tryCreateGateSign(signBlock, tempGate) && tempGate.isGateSignPowered()) {
                return false;
            }
            if (tempGate.isGateSignPowered()) {
                tempGate.getGateStructureBlocks().add(signBlock.getLocation());
            }
        }
        if (layer.getLayerNameSignPosition().length > 0) {
            tempGate.setGateNameBlockHolder(getBlockFromVector(layer.getLayerNameSignPosition(), directionVector, lowerCorner, w));
        }
        if (layer.getLayerRedstoneDialActivationPosition().length > 0) {
            tempGate.setGateRedstoneDialActivationBlock(getBlockFromVector(layer.getLayerRedstoneDialActivationPosition(), directionVector, lowerCorner, w));
        }
        if (layer.getLayerRedstoneSignActivationPosition().length > 0) {
            tempGate.setGateRedstoneSignActivationBlock(getBlockFromVector(layer.getLayerRedstoneSignActivationPosition(), directionVector, lowerCorner, w));
        }
        if (layer.getLayerRedstoneGateActivatedPosition().length > 0) {
            tempGate.setGateRedstoneGateActivatedBlock(getBlockFromVector(layer.getLayerRedstoneGateActivatedPosition(), directionVector, lowerCorner, w));
        }
        if (layer.getLayerIrisActivationPosition().length > 0) {
            tempGate.setGateIrisLeverBlock(getBlockFromVector(layer.getLayerIrisActivationPosition(), directionVector, lowerCorner, w).getRelative(tempGate.getGateFacing()));
            tempGate.getGateStructureBlocks().add(tempGate.getGateIrisLeverBlock().getLocation());
            return true;
        }
        return true;
    }

    private static Block getBlockFromVector(int[] bVect, int[] directionVector, int[] lowerCorner, World w) {
        int[] blockLocation = {bVect[2] * directionVector[0], bVect[1], bVect[2] * directionVector[2]};
        return w.getBlockAt(blockLocation[0] + lowerCorner[0], blockLocation[1] + lowerCorner[1], blockLocation[2] + lowerCorner[2]);
    }

    private static Block getBlockFromVector(Integer[] bVect, int[] directionVector, int[] lowerCorner, World w) {
        int[] blockLocation = {bVect[2].intValue() * directionVector[0], bVect[1].intValue(), bVect[2].intValue() * directionVector[2]};
        return w.getBlockAt(blockLocation[0] + lowerCorner[0], blockLocation[1] + lowerCorner[1], blockLocation[2] + lowerCorner[2]);
    }

    public static StargateShape getStargateShape(String shapeName) {
        String shapeName2 = shapeName.toLowerCase();
        if (!getStargateShapes().containsKey(shapeName2)) {
            return null;
        }
        return getStargateShapes().get(shapeName2);
    }

    public static String getStargateShapeName(String shapeName) {
        String shapeName2 = shapeName.toLowerCase();
        if (!getStargateShapes().containsKey(shapeName2)) {
            return null;
        }
        return getStargateShapes().get(shapeName2).getShapeName();
    }

    private static ConcurrentHashMap<String, StargateShape> getStargateShapes() {
        return stargateShapes;
    }

    public static List<String> getShapeNames() {
        List<String> shapeNames = new ArrayList<>();
        for (String shapeName : getStargateShapes().keySet()) {
            shapeNames.add(getStargateShapeName(shapeName));
        }
        return shapeNames;
    }

    private static boolean isStargateMaterial(Block b, StargateShape s) {
        return b.getType() == s.getShapeStructureMaterial();
    }

    public static boolean isStargateShape(String name) {
        return getStargateShapes().containsKey(name.toLowerCase());
    }

    /** The folder shape files live in. */
    public static java.io.File getShapesDirectory() {
        return new java.io.File("plugins/WormholeXTreme/GateShapes/");
    }

    /**
     * Loads (or reloads) a single shape file into the in-memory shape list.
     * Returns false and logs if the file could not be parsed, leaving whatever
     * was already loaded untouched.
     */
    public static boolean loadShapeFile(java.io.File shapeFile) {
        if (shapeFile == null || !shapeFile.exists()) {
            return false;
        }
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(shapeFile.toPath());
            StargateShape shape = StargateShapeFactory.createShapeFromFile(lines.toArray(new String[0]));
            if (shape == null || shape.getShapeName() == null) {
                WXTLogger.prettyLog(java.util.logging.Level.WARNING, false, "Shape file has no usable name: " + shapeFile.getName());
                return false;
            }
            stargateShapes.put(shape.getShapeName().toLowerCase(), shape);
            WXTLogger.prettyLog(java.util.logging.Level.INFO, false, "Loaded shape: " + shape.getShapeName());
            return true;
        } catch (Exception e) {
            WXTLogger.prettyLog(java.util.logging.Level.WARNING, false, "Failed to load shape " + shapeFile.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Drops a shape from memory without touching its file. Gates already built
     * from it keep working; the shape simply stops being offered to builders.
     */
    public static boolean unloadShape(String shapeName) {
        if (shapeName == null) {
            return false;
        }
        return stargateShapes.remove(shapeName.toLowerCase()) != null;
    }

    public static void reloadShapes() {
        stargateShapes.clear();
        loadShapes();
    }

    /* JADX WARN: Removed duplicated region for block: B:124:0x0451  */
    /* JADX WARN: Removed duplicated region for block: B:134:0x0229 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:144:0x0260 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:146:0x017f A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:150:0x01d4 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:162:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:29:0x0183  */
    /* JADX WARN: Removed duplicated region for block: B:43:0x01d8  */
    /* JADX WARN: Removed duplicated region for block: B:57:0x022d  */
    /* JADX WARN: Removed duplicated region for block: B:70:0x0264  */
    /* JADX WARN: Removed duplicated region for block: B:78:0x028e  */
    public static void loadShapes() {
        java.io.File externalDir = new java.io.File("plugins/WormholeXTreme/GateShapes/");


        if (!externalDir.exists()) {
            externalDir.mkdirs();
        }


        String[] bundled = {"Horizontal.shape", "HorizontalSignDial.shape",
                            "Large.shape", "LargeSignDial.shape",
                            "Minimal.shape", "MinimalSignDial.shape",
                            "Small.shape", "SmallSignDial.shape",
                            "Standard.shape", "StandardSignDial.shape"};
        for (String name : bundled) {
            java.io.File dest = new java.io.File(externalDir, name);
            if (!dest.exists()) {
                try {
                    java.io.InputStream is = WormholeXTreme.class.getResourceAsStream("/GateShapes/" + name);
                    if (is == null) {
                        WXTLogger.prettyLog(java.util.logging.Level.WARNING, false, "Bundled shape not found in jar: " + name);
                        continue;
                    }
                    java.nio.file.Files.copy(is, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    is.close();
                    WXTLogger.prettyLog(java.util.logging.Level.INFO, false, "Extracted shape: " + name);
                } catch (Exception e) {
                    WXTLogger.prettyLog(java.util.logging.Level.WARNING, false, "Failed to extract shape " + name + ": " + e.getMessage());
                }
            }
        }

        java.io.File[] shapeFiles = externalDir.listFiles((dir, name) -> name.endsWith(".shape"));
        if (shapeFiles != null) {
            for (java.io.File shapeFile : shapeFiles) {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(shapeFile.toPath());
                    String[] fileLines = lines.toArray(new String[0]);
                    StargateShape shape = StargateShapeFactory.createShapeFromFile(fileLines);
                    if (shape != null && shape.getShapeName() != null) {
                        stargateShapes.put(shape.getShapeName().toLowerCase(), shape);
                        WXTLogger.prettyLog(java.util.logging.Level.FINE, false, "Loaded shape: " + shape.getShapeName());
                    }
                } catch (Exception e) {
                    WXTLogger.prettyLog(java.util.logging.Level.WARNING, false, "Failed to load shape " + shapeFile.getName() + ": " + e.getMessage());
                }
            }
        }
        WXTLogger.prettyLog(java.util.logging.Level.INFO, false, "Loaded " + stargateShapes.size() + " gate shape(s) from " + externalDir.getPath());
    }

    public static Stargate parseVersionedData(byte[] gate_data, World w, String name, StargateNetwork network) {
        Stargate s = new Stargate();
        s.setGateName(name);
        s.setGateNetwork(network);
        ByteBuffer byteBuff = ByteBuffer.wrap(gate_data);
        s.setLoadedVersion(byteBuff.get());
        s.setGateWorld(w);
        switch (s.getLoadedVersion()) {
            case 3:
                WXTLogger.prettyLog(Level.FINE, false, "Parsing version data: Using parser Version 3 for '" + name + '\"');
                return parseVersionedDataV3(w, s, byteBuff);
            case 4:
                WXTLogger.prettyLog(Level.FINE, false, "Parsing version data: Using parser Version 4 for '" + name + '\"');
                return parseVersionedDataV4(w, s, byteBuff);
            case 5:
                WXTLogger.prettyLog(Level.FINE, false, "Parsing version data: Using parser Version 5 for '" + name + '\"');
                return parseVersionedDataV5(w, s, byteBuff);
            case 6:
                WXTLogger.prettyLog(Level.FINE, false, "Parsing version data: Using parser Version 6 for '" + name + '\"');
                return parseVersionedDataV6(w, s, byteBuff);
            case 7:
                WXTLogger.prettyLog(Level.FINE, false, "Parsing version data: Using parser Version 7 for '" + name + '\"');
                return parseVersionedDataV7(w, s, byteBuff);
            case 8:
                WXTLogger.prettyLog(Level.FINE, false, "Parsing version data: Using parser Version 8 for '" + name + '\"');
                return parseVersionedDataV8(w, s, byteBuff);
            case StargateSaveVersion /* 9 */:
                WXTLogger.prettyLog(Level.FINE, false, "Parsing version data: Using parser Version 9 for '" + name + '\"');
                return parseVersionedDataV9(w, s, byteBuff);
            default:
                return null;
        }
    }

    private static Stargate parseVersionedDataV3(World w, Stargate s, ByteBuffer byteBuff) {
        byte[] locArray = new byte[32];
        byte[] blocArray = new byte[12];
        byteBuff.get(blocArray);
        s.setGateDialLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateIrisLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateNameBlockHolder(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(locArray);
        s.setGatePlayerTeleportLocation(DataUtils.locationFromBytes(locArray, w));
        s.setGateSignPowered(DataUtils.byteToBoolean(byteBuff.get()));
        byteBuff.get(blocArray);
        s.setGateDialSignIndex(byteBuff.getInt());
        s.setGateTempSignTarget(byteBuff.getInt());
        if (s.isGateSignPowered()) {
            s.setGateDialSignBlock(DataUtils.blockFromBytes(blocArray, w));
            if (w.isChunkLoaded(s.getGateDialSignBlock().getChunk())) {
                try {
                    s.setGateDialSign((Sign) s.getGateDialSignBlock().getState());
                } catch (Exception e) {
                    WXTLogger.prettyLog(Level.WARNING, false, "[V3] Unable to get sign for stargate: " + s.getGateName() + " and will be unable to change dial target.");
                    WXTLogger.prettyLog(Level.FINE, false, "[V3] Stacktrace: " + e.getMessage());
                }
            }
        }
        s.setGateActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateTempTargetId(byteBuff.getInt());
        int facingSize = byteBuff.getInt();
        byte[] strBytes = new byte[facingSize];
        byteBuff.get(strBytes);
        String faceStr = new String(strBytes);
        s.setGateFacing(BlockFace.valueOf(faceStr));
        s.getGatePlayerTeleportLocation().setYaw(WorldUtils.getDegreesFromBlockFace(s.getGateFacing()).floatValue());
        s.getGatePlayerTeleportLocation().setPitch(0.0f);
        int idcLen = byteBuff.getInt();
        byte[] idcBytes = new byte[idcLen];
        byteBuff.get(idcBytes);
        s.setGateIrisDeactivationCode(new String(idcBytes));
        s.setGateIrisActive(DataUtils.byteToBoolean(byteBuff.get()));
        int numBlocks = byteBuff.getInt();
        for (int i = 0; i < numBlocks; i++) {
            byteBuff.get(blocArray);
            Block bl = DataUtils.blockFromBytes(blocArray, w);
            s.getGateStructureBlocks().add(bl.getLocation());
        }
        int numBlocks2 = byteBuff.getInt();
        for (int i2 = 0; i2 < numBlocks2; i2++) {
            byteBuff.get(blocArray);
            Block bl2 = DataUtils.blockFromBytes(blocArray, w);
            s.getGatePortalBlocks().add(bl2.getLocation());
        }
        return s;
    }

    private static Stargate parseVersionedDataV4(World w, Stargate s, ByteBuffer byteBuff) {
        byte[] locArray = new byte[32];
        byte[] blocArray = new byte[12];
        byteBuff.get(blocArray);
        s.setGateDialLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateIrisLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateNameBlockHolder(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(locArray);
        s.setGatePlayerTeleportLocation(DataUtils.locationFromBytes(locArray, w));
        s.setGateSignPowered(DataUtils.byteToBoolean(byteBuff.get()));
        byteBuff.get(blocArray);
        s.setGateDialSignIndex(byteBuff.getInt());
        s.setGateTempSignTarget(byteBuff.getLong());
        if (s.isGateSignPowered()) {
            s.setGateDialSignBlock(DataUtils.blockFromBytes(blocArray, w));
            if (w.isChunkLoaded(s.getGateDialSignBlock().getChunk())) {
                try {
                    s.setGateDialSign((Sign) s.getGateDialSignBlock().getState());
                } catch (Exception e) {
                    WXTLogger.prettyLog(Level.WARNING, false, "[V4] Unable to get sign for stargate: " + s.getGateName() + " and will be unable to change dial target.");
                    WXTLogger.prettyLog(Level.FINE, false, "[V4] Stacktrace: " + e.getMessage());
                }
            }
        }
        s.setGateActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateTempTargetId(byteBuff.getLong());
        int facingSize = byteBuff.getInt();
        byte[] strBytes = new byte[facingSize];
        byteBuff.get(strBytes);
        String faceStr = new String(strBytes);
        s.setGateFacing(BlockFace.valueOf(faceStr));
        s.getGatePlayerTeleportLocation().setYaw(WorldUtils.getDegreesFromBlockFace(s.getGateFacing()).floatValue());
        s.getGatePlayerTeleportLocation().setPitch(0.0f);
        int idcLen = byteBuff.getInt();
        byte[] idcBytes = new byte[idcLen];
        byteBuff.get(idcBytes);
        s.setGateIrisDeactivationCode(new String(idcBytes));
        s.setGateIrisActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateIrisDefaultActive(s.isGateIrisActive());
        int numBlocks = byteBuff.getInt();
        for (int i = 0; i < numBlocks; i++) {
            byteBuff.get(blocArray);
            Block bl = DataUtils.blockFromBytes(blocArray, w);
            s.getGateStructureBlocks().add(bl.getLocation());
        }
        int numBlocks2 = byteBuff.getInt();
        for (int i2 = 0; i2 < numBlocks2; i2++) {
            byteBuff.get(blocArray);
            Block bl2 = DataUtils.blockFromBytes(blocArray, w);
            s.getGatePortalBlocks().add(bl2.getLocation());
        }
        return s;
    }

    private static Stargate parseVersionedDataV5(World w, Stargate s, ByteBuffer byteBuff) {
        byte[] locArray = new byte[32];
        byte[] blocArray = new byte[12];
        byteBuff.get(blocArray);
        s.setGateDialLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateIrisLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateNameBlockHolder(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(locArray);
        s.setGatePlayerTeleportLocation(DataUtils.locationFromBytes(locArray, w));
        s.setGateSignPowered(DataUtils.byteToBoolean(byteBuff.get()));
        byteBuff.get(blocArray);
        s.setGateDialSignIndex(byteBuff.getInt());
        s.setGateTempSignTarget(byteBuff.getLong());
        if (s.isGateSignPowered()) {
            s.setGateDialSignBlock(DataUtils.blockFromBytes(blocArray, w));
            if (w.isChunkLoaded(s.getGateDialSignBlock().getChunk())) {
                try {
                    s.setGateDialSign((Sign) s.getGateDialSignBlock().getState());
                } catch (Exception e) {
                    WXTLogger.prettyLog(Level.WARNING, false, "[V5] Unable to get sign for stargate: " + s.getGateName() + " and will be unable to change dial target.");
                    WXTLogger.prettyLog(Level.FINE, false, "[V5] Stacktrace: " + e.getMessage());
                }
            }
        }
        s.setGateActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateTempTargetId(byteBuff.getLong());
        int facingSize = byteBuff.getInt();
        byte[] strBytes = new byte[facingSize];
        byteBuff.get(strBytes);
        String faceStr = new String(strBytes);
        s.setGateFacing(BlockFace.valueOf(faceStr));
        s.getGatePlayerTeleportLocation().setYaw(WorldUtils.getDegreesFromBlockFace(s.getGateFacing()).floatValue());
        s.getGatePlayerTeleportLocation().setPitch(0.0f);
        int idcLen = byteBuff.getInt();
        byte[] idcBytes = new byte[idcLen];
        byteBuff.get(idcBytes);
        s.setGateIrisDeactivationCode(new String(idcBytes));
        s.setGateIrisActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateIrisDefaultActive(s.isGateIrisActive());
        s.setGateLightsActive(DataUtils.byteToBoolean(byteBuff.get()));
        int numBlocks = byteBuff.getInt();
        for (int i = 0; i < numBlocks; i++) {
            byteBuff.get(blocArray);
            Block bl = DataUtils.blockFromBytes(blocArray, w);
            s.getGateStructureBlocks().add(bl.getLocation());
        }
        int numBlocks2 = byteBuff.getInt();
        for (int i2 = 0; i2 < numBlocks2; i2++) {
            byteBuff.get(blocArray);
            Block bl2 = DataUtils.blockFromBytes(blocArray, w);
            s.getGatePortalBlocks().add(bl2.getLocation());
        }
        while (s.getGateLightBlocks().size() < 2) {
            s.getGateLightBlocks().add(null);
        }
        s.getGateLightBlocks().set(1, new ArrayList<>());
        int numBlocks3 = byteBuff.getInt();
        for (int i3 = 0; i3 < numBlocks3; i3++) {
            byteBuff.get(blocArray);
            Block bl3 = DataUtils.blockFromBytes(blocArray, w);
            s.getGateLightBlocks().get(1).add(bl3.getLocation());
        }
        return s;
    }

    private static Stargate parseVersionedDataV6(World w, Stargate s, ByteBuffer byteBuff) {
        byte[] locArray = new byte[32];
        byte[] blocArray = new byte[12];
        byteBuff.get(blocArray);
        s.setGateDialLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateIrisLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateNameBlockHolder(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(locArray);
        s.setGatePlayerTeleportLocation(DataUtils.locationFromBytes(locArray, w));
        s.setGateSignPowered(DataUtils.byteToBoolean(byteBuff.get()));
        byteBuff.get(blocArray);
        s.setGateDialSignIndex(byteBuff.getInt());
        s.setGateTempSignTarget(byteBuff.getLong());
        if (s.isGateSignPowered()) {
            s.setGateDialSignBlock(DataUtils.blockFromBytes(blocArray, w));
            if (w.isChunkLoaded(s.getGateDialSignBlock().getChunk())) {
                try {
                    s.setGateDialSign((Sign) s.getGateDialSignBlock().getState());
                } catch (Exception e) {
                    WXTLogger.prettyLog(Level.WARNING, false, "[V6] Unable to get sign for stargate: " + s.getGateName() + " and will be unable to change dial target.");
                    WXTLogger.prettyLog(Level.FINE, false, "[V6] Stacktrace: " + e.getMessage());
                }
            }
        }
        s.setGateActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateTempTargetId(byteBuff.getLong());
        int facingSize = byteBuff.getInt();
        byte[] strBytes = new byte[facingSize];
        byteBuff.get(strBytes);
        String faceStr = new String(strBytes);
        s.setGateFacing(BlockFace.valueOf(faceStr));
        s.getGatePlayerTeleportLocation().setYaw(WorldUtils.getDegreesFromBlockFace(s.getGateFacing()).floatValue());
        s.getGatePlayerTeleportLocation().setPitch(0.0f);
        int idcLen = byteBuff.getInt();
        byte[] idcBytes = new byte[idcLen];
        byteBuff.get(idcBytes);
        s.setGateIrisDeactivationCode(new String(idcBytes));
        s.setGateIrisActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateIrisDefaultActive(s.isGateIrisActive());
        s.setGateLightsActive(DataUtils.byteToBoolean(byteBuff.get()));
        boolean isRedstone = DataUtils.byteToBoolean(byteBuff.get());
        byteBuff.get(blocArray);
        if (isRedstone) {
            s.setGateRedstoneDialActivationBlock(DataUtils.blockFromBytes(blocArray, w));
        }
        boolean isRedstone2 = DataUtils.byteToBoolean(byteBuff.get());
        byteBuff.get(blocArray);
        if (isRedstone2) {
            s.setGateRedstoneSignActivationBlock(DataUtils.blockFromBytes(blocArray, w));
        }
        int numBlocks = byteBuff.getInt();
        for (int i = 0; i < numBlocks; i++) {
            byteBuff.get(blocArray);
            Block bl = DataUtils.blockFromBytes(blocArray, w);
            s.getGateStructureBlocks().add(bl.getLocation());
        }
        int numBlocks2 = byteBuff.getInt();
        for (int i2 = 0; i2 < numBlocks2; i2++) {
            byteBuff.get(blocArray);
            Block bl2 = DataUtils.blockFromBytes(blocArray, w);
            s.getGatePortalBlocks().add(bl2.getLocation());
        }
        int numLayers = byteBuff.getInt();
        while (s.getGateLightBlocks().size() < numLayers) {
            s.getGateLightBlocks().add(new ArrayList<>());
        }
        for (int i3 = 0; i3 < numLayers; i3++) {
            int numBlocks3 = byteBuff.getInt();
            for (int j = 0; j < numBlocks3; j++) {
                byteBuff.get(blocArray);
                Block bl3 = DataUtils.blockFromBytes(blocArray, w);
                s.getGateLightBlocks().get(i3).add(bl3.getLocation());
            }
        }
        int numLayers2 = byteBuff.getInt();
        while (s.getGateWooshBlocks().size() < numLayers2) {
            s.getGateWooshBlocks().add(new ArrayList<>());
        }
        for (int i4 = 0; i4 < numLayers2; i4++) {
            int numBlocks4 = byteBuff.getInt();
            for (int j2 = 0; j2 < numBlocks4; j2++) {
                byteBuff.get(blocArray);
                Block bl4 = DataUtils.blockFromBytes(blocArray, w);
                s.getGateWooshBlocks().get(i4).add(bl4.getLocation());
            }
        }
        if (byteBuff.remaining() > 0) {
            WXTLogger.prettyLog(Level.WARNING, false, "While loading gate, not all byte data was read. This could be bad: " + byteBuff.remaining());
        }
        return s;
    }

    private static Stargate parseVersionedDataV7(World w, Stargate s, ByteBuffer byteBuff) {
        byte[] locArray = new byte[32];
        byte[] blocArray = new byte[12];
        byteBuff.get(blocArray);
        s.setGateDialLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateIrisLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateNameBlockHolder(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(locArray);
        s.setGatePlayerTeleportLocation(DataUtils.locationFromBytes(locArray, w));
        byteBuff.get(locArray);
        s.setGateMinecartTeleportLocation(DataUtils.locationFromBytes(locArray, w));
        s.setGateSignPowered(DataUtils.byteToBoolean(byteBuff.get()));
        byteBuff.get(blocArray);
        s.setGateDialSignIndex(byteBuff.getInt());
        s.setGateTempSignTarget(byteBuff.getLong());
        if (s.isGateSignPowered()) {
            s.setGateDialSignBlock(DataUtils.blockFromBytes(blocArray, w));
            if (w.isChunkLoaded(s.getGateDialSignBlock().getChunk())) {
                try {
                    s.setGateDialSign((Sign) s.getGateDialSignBlock().getState());
                } catch (Exception e) {
                    WXTLogger.prettyLog(Level.WARNING, false, "[V7] Unable to get sign for stargate: " + s.getGateName() + " and will be unable to change dial target.");
                    WXTLogger.prettyLog(Level.FINE, false, "[V7] Stacktrace: " + e.getMessage());
                }
            }
        }
        s.setGateActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateTempTargetId(byteBuff.getLong());
        int facingSize = byteBuff.getInt();
        byte[] strBytes = new byte[facingSize];
        byteBuff.get(strBytes);
        String faceStr = new String(strBytes);
        s.setGateFacing(BlockFace.valueOf(faceStr));
        s.getGatePlayerTeleportLocation().setYaw(WorldUtils.getDegreesFromBlockFace(s.getGateFacing()).floatValue());
        s.getGatePlayerTeleportLocation().setPitch(0.0f);
        int idcLen = byteBuff.getInt();
        byte[] idcBytes = new byte[idcLen];
        byteBuff.get(idcBytes);
        s.setGateIrisDeactivationCode(new String(idcBytes));
        s.setGateIrisActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateIrisDefaultActive(s.isGateIrisActive());
        s.setGateLightsActive(DataUtils.byteToBoolean(byteBuff.get()));
        boolean isRedstone = DataUtils.byteToBoolean(byteBuff.get());
        byteBuff.get(blocArray);
        if (isRedstone) {
            s.setGateRedstoneDialActivationBlock(DataUtils.blockFromBytes(blocArray, w));
        }
        boolean isRedstone2 = DataUtils.byteToBoolean(byteBuff.get());
        byteBuff.get(blocArray);
        if (isRedstone2) {
            s.setGateRedstoneSignActivationBlock(DataUtils.blockFromBytes(blocArray, w));
        }
        int numBlocks = byteBuff.getInt();
        for (int i = 0; i < numBlocks; i++) {
            byteBuff.get(blocArray);
            Block bl = DataUtils.blockFromBytes(blocArray, w);
            s.getGateStructureBlocks().add(bl.getLocation());
        }
        int numBlocks2 = byteBuff.getInt();
        for (int i2 = 0; i2 < numBlocks2; i2++) {
            byteBuff.get(blocArray);
            Block bl2 = DataUtils.blockFromBytes(blocArray, w);
            s.getGatePortalBlocks().add(bl2.getLocation());
        }
        int numLayers = byteBuff.getInt();
        while (s.getGateLightBlocks().size() < numLayers) {
            s.getGateLightBlocks().add(new ArrayList<>());
        }
        for (int i3 = 0; i3 < numLayers; i3++) {
            int numBlocks3 = byteBuff.getInt();
            for (int j = 0; j < numBlocks3; j++) {
                byteBuff.get(blocArray);
                Block bl3 = DataUtils.blockFromBytes(blocArray, w);
                s.getGateLightBlocks().get(i3).add(bl3.getLocation());
            }
        }
        int numLayers2 = byteBuff.getInt();
        while (s.getGateWooshBlocks().size() < numLayers2) {
            s.getGateWooshBlocks().add(new ArrayList<>());
        }
        for (int i4 = 0; i4 < numLayers2; i4++) {
            int numBlocks4 = byteBuff.getInt();
            for (int j2 = 0; j2 < numBlocks4; j2++) {
                byteBuff.get(blocArray);
                Block bl4 = DataUtils.blockFromBytes(blocArray, w);
                s.getGateWooshBlocks().get(i4).add(bl4.getLocation());
            }
        }
        if (byteBuff.remaining() > 0) {
            WXTLogger.prettyLog(Level.WARNING, false, "While loading gate, not all byte data was read. This could be bad: " + byteBuff.remaining());
        }
        return s;
    }

    private static Stargate parseVersionedDataV8(World w, Stargate s, ByteBuffer byteBuff) {
        byte[] locArray = new byte[32];
        byte[] blocArray = new byte[12];
        byteBuff.get(blocArray);
        s.setGateDialLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateIrisLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateNameBlockHolder(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(locArray);
        s.setGatePlayerTeleportLocation(DataUtils.locationFromBytes(locArray, w));
        byteBuff.get(locArray);
        s.setGateMinecartTeleportLocation(DataUtils.locationFromBytes(locArray, w));
        s.setGateSignPowered(DataUtils.byteToBoolean(byteBuff.get()));
        byteBuff.get(blocArray);
        s.setGateDialSignIndex(byteBuff.getInt());
        s.setGateTempSignTarget(byteBuff.getLong());
        if (s.isGateSignPowered()) {
            s.setGateDialSignBlock(DataUtils.blockFromBytes(blocArray, w));
            if (w.isChunkLoaded(s.getGateDialSignBlock().getChunk())) {
                try {
                    s.setGateDialSign((Sign) s.getGateDialSignBlock().getState());
                } catch (Exception e) {
                    WXTLogger.prettyLog(Level.WARNING, false, "[V8] Unable to get sign for stargate: " + s.getGateName() + " and will be unable to change dial target.");
                    WXTLogger.prettyLog(Level.FINE, false, "[V8] Stacktrace: " + e.getMessage());
                }
            }
        }
        s.setGateActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateTempTargetId(byteBuff.getLong());
        
        if (byteBuff.remaining() >= 4) {
            int facingSize = byteBuff.getInt();
            if (facingSize < 0 || facingSize > byteBuff.remaining()) {
                WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow: invalid facing string size (" + facingSize + ") for: " + s.getGateName());
                s.setGateFacing(BlockFace.NORTH); 
            } else {
                byte[] strBytes = new byte[facingSize];
                byteBuff.get(strBytes);
                String faceStr = new String(strBytes);
                s.setGateFacing(BlockFace.valueOf(faceStr));
            }
        } else {
            WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow: insufficient data for facing size in: " + s.getGateName());
            s.setGateFacing(BlockFace.NORTH);
        }
        
        s.getGatePlayerTeleportLocation().setYaw(WorldUtils.getDegreesFromBlockFace(s.getGateFacing()).floatValue());
        s.getGatePlayerTeleportLocation().setPitch(0.0f);
        s.getGateMinecartTeleportLocation().setYaw(WorldUtils.getDegreesFromBlockFace(s.getGateFacing()).floatValue());
        s.getGateMinecartTeleportLocation().setPitch(0.0f);
        
        if (byteBuff.remaining() >= 4) {
            int idcLen = byteBuff.getInt();
            if (idcLen < 0 || idcLen > byteBuff.remaining()) {
                WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow: invalid iris deactivation code size (" + idcLen + ") for: " + s.getGateName());
                s.setGateIrisDeactivationCode("");
            } else {
                byte[] idcBytes = new byte[idcLen];
                byteBuff.get(idcBytes);
                s.setGateIrisDeactivationCode(new String(idcBytes));
            }
        } else {
            WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow: insufficient data for iris deactivation code size in: " + s.getGateName());
            s.setGateIrisDeactivationCode("");
        }
        
        s.setGateIrisActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateIrisDefaultActive(s.isGateIrisActive());
        s.setGateLightsActive(DataUtils.byteToBoolean(byteBuff.get()));
        boolean isRedstoneDA = DataUtils.byteToBoolean(byteBuff.get());
        byteBuff.get(blocArray);
        if (isRedstoneDA) {
            s.setGateRedstoneDialActivationBlock(DataUtils.blockFromBytes(blocArray, w));
        }
        boolean isRedstoneSA = DataUtils.byteToBoolean(byteBuff.get());
        byteBuff.get(blocArray);
        if (isRedstoneSA) {
            s.setGateRedstoneSignActivationBlock(DataUtils.blockFromBytes(blocArray, w));
        }
        boolean isRedstoneGA = DataUtils.byteToBoolean(byteBuff.get());
        byteBuff.get(blocArray);
        if (isRedstoneGA) {
            s.setGateRedstoneGateActivatedBlock(DataUtils.blockFromBytes(blocArray, w));
        }
        s.setGateRedstonePowered(DataUtils.byteToBoolean(byteBuff.get()));

        if (byteBuff.remaining() < 1) {
            return s;
        }
        s.setGateCustom(DataUtils.byteToBoolean(byteBuff.get()));

        s.setGateCustomStructureMaterial(readMaterialField(byteBuff, s.getGateName(), "structure"));
        s.setGateCustomPortalMaterial(readMaterialField(byteBuff, s.getGateName(), "portal"));
        s.setGateCustomLightMaterial(readMaterialField(byteBuff, s.getGateName(), "light"));
        s.setGateCustomIrisMaterial(readMaterialField(byteBuff, s.getGateName(), "iris"));

        s.setGateCustomWooshTicks(byteBuff.getInt());
        s.setGateCustomLightTicks(byteBuff.getInt());
        s.setGateCustomWooshDepth(byteBuff.getInt());
        s.setGateCustomWooshDepthSquared(s.getGateCustomWooshDepth() >= 0 ? s.getGateCustomWooshDepth() * s.getGateCustomWooshDepth() : -1);
        

        if (byteBuff.remaining() >= 4) {
            int numStructureBlocks = byteBuff.getInt();
            for (int i = 0; i < numStructureBlocks; i++) {
                if (byteBuff.remaining() < blocArray.length) {
                    WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow while reading structure blocks for: " + s.getGateName());
                    break;
                }
                byteBuff.get(blocArray);
                Block bl = DataUtils.blockFromBytes(blocArray, w);
                s.getGateStructureBlocks().add(bl.getLocation());
            }
        } else {
            WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow: insufficient data for structure block count in: " + s.getGateName());
        }
        

        if (byteBuff.remaining() >= 4) {
            int numPortalBlocks = byteBuff.getInt();
            for (int i2 = 0; i2 < numPortalBlocks; i2++) {
                if (byteBuff.remaining() < blocArray.length) {
                    WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow while reading portal blocks for: " + s.getGateName());
                    break;
                }
                byteBuff.get(blocArray);
                Block bl2 = DataUtils.blockFromBytes(blocArray, w);
                s.getGatePortalBlocks().add(bl2.getLocation());
            }
        } else {
            WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow: insufficient data for portal block count in: " + s.getGateName());
        }
        

        if (byteBuff.remaining() >= 4) {
            int numLightLayers = byteBuff.getInt();
            while (s.getGateLightBlocks().size() < numLightLayers) {
                s.getGateLightBlocks().add(new ArrayList<>());
            }
            for (int i3 = 0; i3 < numLightLayers; i3++) {
                if (byteBuff.remaining() < 4) {
                    WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow while reading light layer count for: " + s.getGateName());
                    break;
                }
                int numLightBlocks = byteBuff.getInt();
                for (int j = 0; j < numLightBlocks; j++) {
                    if (byteBuff.remaining() < blocArray.length) {
                        WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow while reading light blocks for: " + s.getGateName());
                        break;
                    }
                    byteBuff.get(blocArray);
                    Block bl3 = DataUtils.blockFromBytes(blocArray, w);
                    s.getGateLightBlocks().get(i3).add(bl3.getLocation());
                }
            }
        } else {
            WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow: insufficient data for light layer count in: " + s.getGateName());
        }
        

        if (byteBuff.remaining() >= 4) {
            int numWooshLayers = byteBuff.getInt();
            while (s.getGateWooshBlocks().size() < numWooshLayers) {
                s.getGateWooshBlocks().add(new ArrayList<>());
            }
            for (int i4 = 0; i4 < numWooshLayers; i4++) {
                if (byteBuff.remaining() < 4) {
                    WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow while reading woosh layer count for: " + s.getGateName());
                    break;
                }
                int numWooshBlocks = byteBuff.getInt();
                for (int j2 = 0; j2 < numWooshBlocks; j2++) {
                    if (byteBuff.remaining() < blocArray.length) {
                        WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow while reading woosh blocks for: " + s.getGateName());
                        break;
                    }
                    byteBuff.get(blocArray);
                    Block bl4 = DataUtils.blockFromBytes(blocArray, w);
                    s.getGateWooshBlocks().get(i4).add(bl4.getLocation());
                }
            }
        } else {
            WXTLogger.prettyLog(Level.WARNING, false, "[V8] Buffer underflow: insufficient data for woosh layer count in: " + s.getGateName());
        }
        
        if (byteBuff.remaining() > 0) {
            WXTLogger.prettyLog(Level.WARNING, false, "While loading gate, not all byte data was read. This could be bad: " + byteBuff.remaining());
        }
        return s;
    }

    private static void setupSignGateNetwork(Stargate stargate) {
        if (stargate.getGateName() != null && stargate.getGateName().length() > 0) {
            String networkName = "Public";
            if (stargate.getGateDialSign() != null && !stargate.getGateDialSign().getLine(1).equals("")) {
                networkName = stargate.getGateDialSign().getLine(1);
            }
            StargateNetwork net = StargateManager.getStargateNetwork(networkName);
            if (net == null) {
                net = StargateManager.addStargateNetwork(networkName);
            }
            // The gate is still only a build candidate here - the player may have
            // no permission, may pick a name already in use, may fail the economy
            // charge, or may simply walk away. Assign the network so completion
            // knows where the gate belongs, but do not put it in the network lists;
            // StargateManager.addStargate() does that once the gate is real.
            // Registering candidates left orphaned entries with a null owner that
            // no command could reach and no click could refresh.
            stargate.setGateNetwork(net);
            stargate.setGateDialSignIndex(-1);
            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(stargate, StargateUpdateRunnable.ActionToTake.DIAL_SIGN_CLICK));
        }
    }

    private static Material readMaterialField(ByteBuffer buf, String gateName, String fieldName) {
        if (buf.remaining() < 4) {
            WXTLogger.prettyLog(Level.WARNING, false,
                    "[V8] Buffer underflow reading " + fieldName + " material for: " + gateName);
            return null;
        }
        int value = buf.getInt();
        if (value < 0) {
            return null;
        }
        if (value > buf.remaining()) {
            return getMaterialById(value);
        }
        byte[] nameBytes = new byte[value];
        buf.get(nameBytes);
        String name = new String(nameBytes);
        if (name.isEmpty() || name.equals("null")) {
            return null;
        }
        Material m = parseMaterialName(name);
        if (m != null) {
            return m;
        }
        try {
            return getMaterialById(Integer.parseInt(name));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Material parseMaterialName(String name) {
        if (name == null || name.isEmpty() || name.equals("null")) return null;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            WXTLogger.prettyLog(Level.WARNING, false, "Unknown material name in save data: " + name);
            return null;
        }
    }

    private static Stargate parseVersionedDataV9(World w, Stargate s, ByteBuffer byteBuff) {
        byte[] locArray = new byte[32];
        byte[] blocArray = new byte[12];
        byteBuff.get(blocArray);
        s.setGateDialLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateIrisLeverBlock(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(blocArray);
        s.setGateNameBlockHolder(DataUtils.blockFromBytes(blocArray, w));
        byteBuff.get(locArray);
        s.setGatePlayerTeleportLocation(DataUtils.locationFromBytes(locArray, w));
        byteBuff.get(locArray);
        s.setGateMinecartTeleportLocation(DataUtils.locationFromBytes(locArray, w));
        s.setGateSignPowered(DataUtils.byteToBoolean(byteBuff.get()));
        byteBuff.get(blocArray);
        s.setGateDialSignIndex(byteBuff.getInt());
        s.setGateTempSignTarget(byteBuff.getLong());
        if (s.isGateSignPowered()) {
            s.setGateDialSignBlock(DataUtils.blockFromBytes(blocArray, w));
            if (w.isChunkLoaded(s.getGateDialSignBlock().getChunk())) {
                try {
                    s.setGateDialSign((Sign) s.getGateDialSignBlock().getState());
                } catch (Exception e) {
                    WXTLogger.prettyLog(Level.WARNING, false, "[V9] Unable to get sign for stargate: " + s.getGateName() + " and will be unable to change dial target.");
                    WXTLogger.prettyLog(Level.FINE, false, "[V9] Stacktrace: " + e.getMessage());
                }
            }
        }
        s.setGateActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateTempTargetId(byteBuff.getLong());
        int facingSize = byteBuff.getInt();
        byte[] strBytes = new byte[facingSize];
        byteBuff.get(strBytes);
        String faceStr = new String(strBytes);
        s.setGateFacing(BlockFace.valueOf(faceStr));
        s.getGatePlayerTeleportLocation().setYaw(WorldUtils.getDegreesFromBlockFace(s.getGateFacing()).floatValue());
        s.getGatePlayerTeleportLocation().setPitch(0.0f);
        s.getGateMinecartTeleportLocation().setYaw(WorldUtils.getDegreesFromBlockFace(s.getGateFacing()).floatValue());
        s.getGateMinecartTeleportLocation().setPitch(0.0f);
        int idcLen = byteBuff.getInt();
        byte[] idcBytes = new byte[idcLen];
        byteBuff.get(idcBytes);
        s.setGateIrisDeactivationCode(new String(idcBytes));
        s.setGateIrisActive(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateIrisDefaultActive(s.isGateIrisActive());
        s.setGateLightsActive(DataUtils.byteToBoolean(byteBuff.get()));
        boolean isRedstoneDA = DataUtils.byteToBoolean(byteBuff.get());
        byteBuff.get(blocArray);
        if (isRedstoneDA) {
            s.setGateRedstoneDialActivationBlock(DataUtils.blockFromBytes(blocArray, w));
        }
        boolean isRedstoneSA = DataUtils.byteToBoolean(byteBuff.get());
        byteBuff.get(blocArray);
        if (isRedstoneSA) {
            s.setGateRedstoneSignActivationBlock(DataUtils.blockFromBytes(blocArray, w));
        }
        boolean isRedstoneGA = DataUtils.byteToBoolean(byteBuff.get());
        byteBuff.get(blocArray);
        if (isRedstoneGA) {
            s.setGateRedstoneGateActivatedBlock(DataUtils.blockFromBytes(blocArray, w));
        }
        s.setGateRedstonePowered(DataUtils.byteToBoolean(byteBuff.get()));
        s.setGateCustom(DataUtils.byteToBoolean(byteBuff.get()));
        int structMatLen = byteBuff.getInt();
        byte[] structMatBytes = new byte[structMatLen]; byteBuff.get(structMatBytes);
        s.setGateCustomStructureMaterial(parseMaterialName(new String(structMatBytes)));
        int portalMatLen = byteBuff.getInt();
        byte[] portalMatBytes = new byte[portalMatLen]; byteBuff.get(portalMatBytes);
        s.setGateCustomPortalMaterial(parseMaterialName(new String(portalMatBytes)));
        int lightMatLen = byteBuff.getInt();
        byte[] lightMatBytes = new byte[lightMatLen]; byteBuff.get(lightMatBytes);
        s.setGateCustomLightMaterial(parseMaterialName(new String(lightMatBytes)));
        int irisMatLen = byteBuff.getInt();
        byte[] irisMatBytes = new byte[irisMatLen]; byteBuff.get(irisMatBytes);
        s.setGateCustomIrisMaterial(parseMaterialName(new String(irisMatBytes)));
        s.setGateCustomWooshTicks(byteBuff.getInt());
        s.setGateCustomLightTicks(byteBuff.getInt());
        s.setGateCustomWooshDepth(byteBuff.getInt());
        s.setGateCustomWooshDepthSquared(s.getGateCustomWooshDepth() >= 0 ? s.getGateCustomWooshDepth() * s.getGateCustomWooshDepth() : -1);
        int numStructureBlocks = byteBuff.getInt();
        for (int i = 0; i < numStructureBlocks; i++) {
            byteBuff.get(blocArray);
            Block bl = DataUtils.blockFromBytes(blocArray, w);
            s.getGateStructureBlocks().add(bl.getLocation());
        }
        int numPortalBlocks = byteBuff.getInt();
        for (int i2 = 0; i2 < numPortalBlocks; i2++) {
            byteBuff.get(blocArray);
            Block bl2 = DataUtils.blockFromBytes(blocArray, w);
            s.getGatePortalBlocks().add(bl2.getLocation());
        }
        int numLightLayers = byteBuff.getInt();
        while (s.getGateLightBlocks().size() < numLightLayers) {
            s.getGateLightBlocks().add(new ArrayList<>());
        }
        for (int i3 = 0; i3 < numLightLayers; i3++) {
            int numLightBlocks = byteBuff.getInt();
            for (int j = 0; j < numLightBlocks; j++) {
                byteBuff.get(blocArray);
                Block bl3 = DataUtils.blockFromBytes(blocArray, w);
                s.getGateLightBlocks().get(i3).add(bl3.getLocation());
            }
        }
        int numWooshLayers = byteBuff.getInt();
        while (s.getGateWooshBlocks().size() < numWooshLayers) {
            s.getGateWooshBlocks().add(new ArrayList<>());
        }
        for (int i4 = 0; i4 < numWooshLayers; i4++) {
            int numWooshBlocks = byteBuff.getInt();
            for (int j2 = 0; j2 < numWooshBlocks; j2++) {
                byteBuff.get(blocArray);
                Block bl4 = DataUtils.blockFromBytes(blocArray, w);
                s.getGateWooshBlocks().get(i4).add(bl4.getLocation());
            }
        }
        if (byteBuff.remaining() > 0) {
            WXTLogger.prettyLog(Level.WARNING, false, "While loading gate, not all byte data was read. This could be bad: " + byteBuff.remaining());
        }
        return s;
    }

    private static byte[] materialNameBytes(Material m) {
        if (m == null) return "null".getBytes();
        return m.name().getBytes();
    }

    public static byte[] stargatetoBinary(Stargate s) {
        try {
            byte[] utfFaceBytes = s.getGateFacing().toString().getBytes("UTF8");
            byte[] utfIdcBytes = s.getGateIrisDeactivationCode().getBytes("UTF8");
            byte[] structMatBytes = materialNameBytes(s.getGateCustomStructureMaterial());
            byte[] portalMatBytes = materialNameBytes(s.getGateCustomPortalMaterial());
            byte[] lightMatBytes  = materialNameBytes(s.getGateCustomLightMaterial());
            byte[] irisMatBytes   = materialNameBytes(s.getGateCustomIrisMaterial());
            int size = 230 + structMatBytes.length + portalMatBytes.length + lightMatBytes.length + irisMatBytes.length
                     + (s.getGateStructureBlocks().size() * 12) + (s.getGatePortalBlocks().size() * 12);
            for (int i = 0; i < s.getGateLightBlocks().size(); i++) {
                size += 4; 
                if (s.getGateLightBlocks().get(i) != null) {
                    size += s.getGateLightBlocks().get(i).size() * 12;
                }
            }
            for (int i2 = 0; i2 < s.getGateWooshBlocks().size(); i2++) {
                size += 4; 
                if (s.getGateWooshBlocks().get(i2) != null) {
                    size += s.getGateWooshBlocks().get(i2).size() * 12;
                }
            }
            ByteBuffer dataArr = ByteBuffer.allocate(size + utfFaceBytes.length + utfIdcBytes.length);
            dataArr.put((byte) 8);
            dataArr.put(DataUtils.blockToBytes(s.getGateDialLeverBlock()));
            dataArr.put(s.getGateIrisLeverBlock() != null ? DataUtils.blockToBytes(s.getGateIrisLeverBlock()) : emptyBlock);
            dataArr.put(s.getGateNameBlockHolder() != null ? DataUtils.blockToBytes(s.getGateNameBlockHolder()) : emptyBlock);
            dataArr.put(DataUtils.locationToBytes(s.getGatePlayerTeleportLocation()));
            dataArr.put(s.getGateMinecartTeleportLocation() != null ? DataUtils.locationToBytes(s.getGateMinecartTeleportLocation()) : DataUtils.locationToBytes(s.getGatePlayerTeleportLocation()));
            if (s.isGateSignPowered()) {
                dataArr.put((byte) 1);
                dataArr.put(DataUtils.blockToBytes(s.getGateDialSignBlock()));
                dataArr.putInt(s.getGateDialSignIndex());
                dataArr.putLong(s.getGateDialSignTarget() != null ? s.getGateDialSignTarget().getGateId() : -1L);
            } else {
                dataArr.put((byte) 0);
                dataArr.put(emptyBlock);
                dataArr.putInt(-1);
                dataArr.putLong(-1L);
            }
            if (s.isGateActive() && s.getGateTarget() != null) {
                dataArr.put((byte) 1);
                dataArr.putLong(s.getGateTarget().getGateId());
            } else {
                dataArr.put((byte) 0);
                dataArr.putLong(-1L);
            }
            dataArr.putInt(utfFaceBytes.length);
            dataArr.put(utfFaceBytes);
            dataArr.putInt(utfIdcBytes.length);
            dataArr.put(utfIdcBytes);
            dataArr.put(s.isGateIrisActive() ? (byte) 1 : (byte) 0);
            dataArr.put(s.isGateLightsActive() ? (byte) 1 : (byte) 0);
            if (s.getGateRedstoneDialActivationBlock() != null) {
                dataArr.put((byte) 1);
                dataArr.put(DataUtils.blockToBytes(s.getGateRedstoneDialActivationBlock()));
            } else {
                dataArr.put((byte) 0);
                dataArr.put(emptyBlock);
            }
            if (s.getGateRedstoneSignActivationBlock() != null) {
                dataArr.put((byte) 1);
                dataArr.put(DataUtils.blockToBytes(s.getGateRedstoneSignActivationBlock()));
            } else {
                dataArr.put((byte) 0);
                dataArr.put(emptyBlock);
            }
            if (s.getGateRedstoneGateActivatedBlock() != null) {
                dataArr.put((byte) 1);
                dataArr.put(DataUtils.blockToBytes(s.getGateRedstoneGateActivatedBlock()));
            } else {
                dataArr.put((byte) 0);
                dataArr.put(emptyBlock);
            }
            dataArr.put(s.isGateRedstonePowered() ? (byte) 1 : (byte) 0);
            dataArr.put(s.isGateCustom() ? (byte) 1 : (byte) 0);
            dataArr.putInt(structMatBytes.length); dataArr.put(structMatBytes);
            dataArr.putInt(portalMatBytes.length); dataArr.put(portalMatBytes);
            dataArr.putInt(lightMatBytes.length);  dataArr.put(lightMatBytes);
            dataArr.putInt(irisMatBytes.length);   dataArr.put(irisMatBytes);
            dataArr.putInt(s.getGateCustomWooshTicks());
            dataArr.putInt(s.getGateCustomLightTicks());
            dataArr.putInt(s.getGateCustomWooshDepth());
            dataArr.putInt(s.getGateStructureBlocks().size());
            for (int i3 = 0; i3 < s.getGateStructureBlocks().size(); i3++) {
                dataArr.put(DataUtils.blockLocationToBytes(s.getGateStructureBlocks().get(i3)));
            }
            dataArr.putInt(s.getGatePortalBlocks().size());
            for (int i4 = 0; i4 < s.getGatePortalBlocks().size(); i4++) {
                dataArr.put(DataUtils.blockLocationToBytes(s.getGatePortalBlocks().get(i4)));
            }
            dataArr.putInt(s.getGateLightBlocks().size());
            for (int i5 = 0; i5 < s.getGateLightBlocks().size(); i5++) {
                if (s.getGateLightBlocks().get(i5) != null) {
                    dataArr.putInt(s.getGateLightBlocks().get(i5).size());
                    for (int j = 0; j < s.getGateLightBlocks().get(i5).size(); j++) {
                        dataArr.put(DataUtils.blockLocationToBytes(s.getGateLightBlocks().get(i5).get(j)));
                    }
                } else {
                    dataArr.putInt(0);
                }
            }
            dataArr.putInt(s.getGateWooshBlocks().size());
            for (int i6 = 0; i6 < s.getGateWooshBlocks().size(); i6++) {
                if (s.getGateWooshBlocks().get(i6) != null) {
                    dataArr.putInt(s.getGateWooshBlocks().get(i6).size());
                    for (int j2 = 0; j2 < s.getGateWooshBlocks().get(i6).size(); j2++) {
                        dataArr.put(DataUtils.blockLocationToBytes(s.getGateWooshBlocks().get(i6).get(j2)));
                    }
                } else {
                    dataArr.putInt(0);
                }
            }
            if (dataArr.remaining() > 0) {
                WXTLogger.prettyLog(Level.WARNING, false, "Gate data not filling whole byte array. This could be bad:" + dataArr.remaining());
            }
            return dataArr.array();
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Unable to store gate in DB, byte encoding failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static boolean tryCreateGateSign(Block signBlock, Stargate tempGate) {
        WXTLogger.prettyLog(Level.FINE, false, "Trying to create GateSign for gate '" + tempGate.getGateName() + "' in '" + tempGate.getGateWorld().getName() + "'");
        if (org.bukkit.Tag.WALL_SIGNS.isTagged(signBlock.getType())) {
            tempGate.setGateSignPowered(true);
            tempGate.setGateDialSignBlock(signBlock);
            tempGate.setGateDialSign((Sign) signBlock.getState());
            tempGate.getGateStructureBlocks().add(signBlock.getLocation());
            String name = tempGate.getGateDialSign().getLine(0);
            if (StargateManager.getStargate(name) != null) {
                tempGate.setGateName("");
                return false;
            }
            String filteredName = name;
            if (name.startsWith("-") && name.endsWith("-")) {
                for (int i = 0; i < name.length(); i++) {
                    if (name.startsWith("-") && name.endsWith("-")) {
                        filteredName = name.substring(1, name.length() - 1);
                    }
                }
            }
            if (filteredName.length() > 2) {
                tempGate.setGateName(filteredName);
                return true;
            }
            return true;
        }
        return false;
    }
}
