package de.luricos.bukkit.WormholeXTreme.Wormhole.model;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholeDialSignException;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateUpdateRunnable;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayer;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayerManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WorldUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/model/Stargate.class */
public class Stargate {
    private static final int MAX_SIGN_LINE_LENGTH = 15;
    private StargateNetwork gateNetwork;
    private StargateShape gateShape;
    private World gateWorld;
    private BlockFace gateFacing;
    private boolean gateSignPowered;
    private boolean gateRedstonePowered;
    private Stargate gateDialSignTarget;
    private Sign gateDialSign;
    private Location gatePlayerTeleportLocation;
    private Location gateMinecartTeleportLocation;
    private Block gateDialLeverBlock;
    private Block gateIrisLeverBlock;
    private boolean gateDialSwitchUsesButton = false;
    private Block gateDialSignBlock;
    private Block gateRedstoneDialActivationBlock;
    private Block gateRedstoneSignActivationBlock;
    private Block gateRedstoneGateActivatedBlock;
    private Block gateNameBlockHolder;
    private int gateActivateTaskId;
    private int gateEstablishWormholeTaskId;
    private int gateShutdownTaskId;
    private int gateAfterShutdownTaskId;
    private byte loadedVersion = -1;
    private long gateId = -1;
    private String gateName = "";
    private String gateSourceName = null;
    private String gateOwner = null;
    private String lastUsedBy = null;
    private int visitCount = 0;
    private boolean gateActive = false;
    private boolean gateRecentlyActive = false;
    private boolean gateLightsActive = false;
    private Stargate gateTarget = null;
    private long gateTempSignTarget = -1;
    private int gateDialSignIndex = 0;
    private long gateTempTargetId = -1;
    private String gateIrisDeactivationCode = "";
    private boolean gateIrisActive = false;
    private boolean gateIrisDefaultActive = false;
    private int gateAnimationStep3D = 1;
    private int gateAnimationStep2D = 0;
    private boolean gateAnimationRemoving = false;
    private int gateLightingCurrentIteration = 0;
    private final ArrayList<Location> gateStructureBlocks = new ArrayList<>();
    private final ArrayList<Location> gatePortalBlocks = new ArrayList<>();
    private final ArrayList<ArrayList<Location>> gateLightBlocks = new ArrayList<>();
    private final ArrayList<ArrayList<Location>> gateWooshBlocks = new ArrayList<>();
    private final ArrayList<Block> gateAnimatedBlocks = new ArrayList<>();
    private final HashMap<Integer, Stargate> gateSignOrder = new HashMap<>();
    private boolean gateCustom = false;
    private Material gateCustomStructureMaterial = null;
    private Material gateCustomPortalMaterial = null;
    private Material gateCustomLightMaterial = null;
    private Material gateCustomIrisMaterial = null;
    private int gateCustomWooshTicks = -1;
    private int gateCustomLightTicks = -1;
    private int gateCustomWooshDepth = -1;
    private int gateCustomWooshDepthSquared = -1;
    private boolean gateChevronsLocked = false;
    private boolean gateEstablishedWormhole = false;
    private boolean stargateIsValid = true;

    public void animateOpening() {
        if (!isGateLightsActive()) {
            clearWooshAnimation();
            return;
        }
        Material wooshMaterial = isGateCustom() ? getGateCustomPortalMaterial() : getGateShape() != null ? getGateShape().getShapePortalMaterial() : Material.WATER;
        int wooshDepth = isGateCustom() ? getGateCustomWooshDepth() : getGateShape() != null ? getGateShape().getShapeWooshDepth() : 0;
        if (getGateWooshBlocks() != null && getGateWooshBlocks().size() > 0) {
            ArrayList<Location> wooshBlockStep = getGateWooshBlocks().get(getGateAnimationStep3D());
            if (!isGateAnimationRemoving()) {
                if (wooshBlockStep != null) {
                    for (Location l : wooshBlockStep) {
                        Block b = getGateWorld().getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
                        getGateAnimatedBlocks().add(b);
                        StargateManager.getOpeningAnimationBlocks().put(l, b);
                        b.setType(wooshMaterial);
                        
                        if (wooshMaterial == Material.NETHER_PORTAL) {
                            org.bukkit.block.data.BlockData blockData = b.getBlockData();
                            if (blockData instanceof org.bukkit.block.data.Orientable) {
                                org.bukkit.block.data.Orientable orientable = (org.bukkit.block.data.Orientable) blockData;
                                if (getGateFacing() == org.bukkit.block.BlockFace.NORTH || getGateFacing() == org.bukkit.block.BlockFace.SOUTH) {
                                    orientable.setAxis(org.bukkit.Axis.X);
                                } else {
                                    orientable.setAxis(org.bukkit.Axis.Z);
                                }
                                b.setBlockData(orientable);
                            }
                        }
                    }
                    WXTLogger.prettyLog(Level.FINER, false, getGateName() + " Woosh Adding: " + getGateAnimationStep3D() + " Woosh Block Size: " + wooshBlockStep.size());
                }
                if (getGateWooshBlocks().size() == getGateAnimationStep3D() + 1) {
                    setGateAnimationRemoving(true);
                } else {
                    setGateAnimationStep3D(getGateAnimationStep3D() + 1);
                }
                WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.ANIMATE_WOOSH), isGateCustom() ? getGateCustomWooshTicks() : getGateShape() != null ? getGateShape().getShapeWooshTicks() : 2L);
                return;
            }
            if (wooshBlockStep != null) {
                for (Location loc : wooshBlockStep) {
                    Block b2 = getGateWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                    StargateManager.getOpeningAnimationBlocks().remove(loc);
                    getGateAnimatedBlocks().remove(b2);
                    if (!StargateManager.isBlockInGate(b2)) {
                        b2.setType(Material.AIR);
                    }
                }
                WXTLogger.prettyLog(Level.FINER, false, getGateName() + " Woosh Removing: " + getGateAnimationStep3D() + " Woosh Block Size: " + wooshBlockStep.size());
            }
            if (getGateAnimationStep3D() == 1) {
                setGateAnimationRemoving(false);
                if (isGateLightsActive() && isGateActive()) {
                    fillGateInterior(wooshMaterial);
                    return;
                }
                return;
            }
            setGateAnimationStep3D(getGateAnimationStep3D() - 1);
            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.ANIMATE_WOOSH), isGateCustom() ? getGateCustomWooshTicks() : getGateShape() != null ? getGateShape().getShapeWooshTicks() : 2L);
            return;
        }
        if (getGateAnimationStep2D() == 0 && wooshDepth > 0) {
            for (Location block : getGatePortalBlocks()) {
                Block r = getGateWorld().getBlockAt(block.getBlockX(), block.getBlockY(), block.getBlockZ()).getRelative(getGateFacing());
                r.setType(wooshMaterial);
                
                if (wooshMaterial == Material.NETHER_PORTAL) {
                    org.bukkit.block.data.BlockData blockData = r.getBlockData();
                    if (blockData instanceof org.bukkit.block.data.Orientable) {
                        org.bukkit.block.data.Orientable orientable = (org.bukkit.block.data.Orientable) blockData;
                        if (getGateFacing() == org.bukkit.block.BlockFace.NORTH || getGateFacing() == org.bukkit.block.BlockFace.SOUTH) {
                            orientable.setAxis(org.bukkit.Axis.X);
                        } else {
                            orientable.setAxis(org.bukkit.Axis.Z);
                        }
                        r.setBlockData(orientable);
                    }
                }
                
                getGateAnimatedBlocks().add(r);
                StargateManager.getOpeningAnimationBlocks().put(r.getLocation(), r);
            }
            setGateAnimationStep2D(getGateAnimationStep2D() + 1);
            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.ANIMATE_WOOSH), 4L);
            return;
        }
        if (getGateAnimationStep2D() < wooshDepth) {
            int size = getGateAnimatedBlocks().size();
            int start = getGatePortalBlocks().size();
            for (int i = size - start; i < size; i++) {
                Block r2 = getGateAnimatedBlocks().get(i).getRelative(getGateFacing());
                r2.setType(wooshMaterial);
                
                if (wooshMaterial == Material.NETHER_PORTAL) {
                    org.bukkit.block.data.BlockData blockData = r2.getBlockData();
                    if (blockData instanceof org.bukkit.block.data.Orientable) {
                        org.bukkit.block.data.Orientable orientable = (org.bukkit.block.data.Orientable) blockData;
                        if (getGateFacing() == org.bukkit.block.BlockFace.NORTH || getGateFacing() == org.bukkit.block.BlockFace.SOUTH) {
                            orientable.setAxis(org.bukkit.Axis.X);
                        } else {
                            orientable.setAxis(org.bukkit.Axis.Z);
                        }
                        r2.setBlockData(orientable);
                    }
                }
                
                getGateAnimatedBlocks().add(r2);
                StargateManager.getOpeningAnimationBlocks().put(r2.getLocation(), r2);
            }
            setGateAnimationStep2D(getGateAnimationStep2D() + 1);
            if (getGateAnimationStep2D() == wooshDepth) {
                WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.ANIMATE_WOOSH), 8L);
                return;
            } else {
                WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.ANIMATE_WOOSH), 4L);
                return;
            }
        }
        if (getGateAnimationStep2D() >= wooshDepth) {
            for (int i2 = 0; i2 < getGatePortalBlocks().size(); i2++) {
                int index = getGateAnimatedBlocks().size() - 1;
                if (index >= 0) {
                    Block b3 = getGateAnimatedBlocks().get(index);
                    b3.setType(Material.AIR);
                    getGateAnimatedBlocks().remove(index);
                    StargateManager.getOpeningAnimationBlocks().remove(b3.getLocation());
                }
            }
            if (getGateAnimationStep2D() < (wooshDepth * 2) - 1) {
                setGateAnimationStep2D(getGateAnimationStep2D() + 1);
                WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.ANIMATE_WOOSH), 3L);
            } else {
                setGateAnimationStep2D(0);
                if (isGateActive()) {
                    fillGateInterior(wooshMaterial);
                }
            }
        }
    }

    void completeGate(String name, String idc) {
        setGateName(name);
        if (getGateNameBlockHolder() != null) {
            setupGateSign(true);
        }
        setIrisDeactivationCode(idc);
        if (isGateRedstonePowered()) {
            setupRedstoneGateActivatedLever(true);
            if (isGateSignPowered()) {
                setupRedstoneDialWire(true);
                setupRedstoneSignDialWire(true);
            }
        }
    }

    public void deleteGateBlocks() {
        for (Location bc : getGateStructureBlocks()) {
            Block b = getGateWorld().getBlockAt(bc.getBlockX(), bc.getBlockY(), bc.getBlockZ());
            b.setType(Material.AIR);
        }
    }

    public void deletePortalBlocks() {
        for (Location bc : getGatePortalBlocks()) {
            Block b = getGateWorld().getBlockAt(bc.getBlockX(), bc.getBlockY(), bc.getBlockZ());
            b.setType(Material.AIR);
        }
    }

    public void deleteTeleportSign() {
        if (getGateDialSignBlock() != null && getGateDialSign() != null) {
            Block teleportSign = getGateDialSignBlock().getRelative(getGateFacing());
            teleportSign.setType(Material.AIR);
        }
    }

    private boolean dialStargate() {
        WorldUtils.scheduleChunkLoad(getGatePlayerTeleportLocation().getBlock());
        if (getGateShutdownTaskId() > 0) {
            WormholeXTreme.getScheduler().cancelTask(getGateShutdownTaskId());
        }
        if (getGateAfterShutdownTaskId() > 0) {
            WormholeXTreme.getScheduler().cancelTask(getGateAfterShutdownTaskId());
        }
        int timeout = ConfigManager.getTimeoutShutdown() * 20;
        if (timeout > 0) {
            setGateShutdownTaskId(WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.SHUTDOWN), timeout));
            WXTLogger.prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" ShutdownTaskID \"" + getGateShutdownTaskId() + "\" created.");
            if (getGateShutdownTaskId() == -1) {
                shutdownStargate(true);
                WXTLogger.prettyLog(Level.SEVERE, false, "Failed to schedule wormhole shutdown timeout: " + timeout + " Received task id of -1. Wormhole forced closed NOW.");
            }
        }
        if (getGateShutdownTaskId() > 0 || timeout == 0) {
            if (!isGateActive()) {
                setGateActive(true);
                toggleDialLeverState(false);
                toggleRedstoneGateActivatedPower();
                setGateRecentlyActive(false);
            }
            if (!isGateLightsActive()) {
                lightStargate(true);
                return true;
            }
            WXTLogger.prettyLog(Level.FINE, false, "Chevrons locked at gate: '" + getGateName() + "'");
            setGateChevronsLocked(true);
            return true;
        }
        WXTLogger.prettyLog(Level.WARNING, false, "No wormhole. No visual events.");
        return false;
    }

    public boolean dialStargate(Stargate target, boolean force) {
        WXTLogger.prettyLog(Level.FINER, false, "Dialing Stargate: '" + target.getGateName() + "'; force:='" + force + "'");
        if (getGateActivateTaskId() > 0) {
            WXTLogger.prettyLog(Level.FINER, false, "Cancelling ActivateTaskID: " + getGateActivateTaskId() + " for gate '" + target.getGateName() + "'");
            WormholeXTreme.getScheduler().cancelTask(getGateActivateTaskId());
        }
        if (!target.isGateLightsActive() || force) {
            setGateTarget(target);
            if (getGateTarget() == null) {
                WXTLogger.prettyLog(Level.WARNING, false, "Target lost! Closing local wormhole for safety percussions.");
                shutdownStargate(true);
                return false;
            }
            dialStargate();
            getGateTarget().dialStargate();
            getGateTarget().setSourceGateName(getGateName());
            establishWormhole();
            if (isGateActive() && getGateTarget().isGateActive()) {
                return true;
            }
            if (isGateActive() && !getGateTarget().isGateActive()) {
                shutdownStargate(true);
                WXTLogger.prettyLog(Level.WARNING, false, "Far wormhole failed to open. Closing local wormhole for safety sake.");
                return false;
            }
            if (!isGateActive() && getGateTarget().isGateActive()) {
                target.shutdownStargate(true);
                WXTLogger.prettyLog(Level.WARNING, false, "Local wormhole failed to open. Closing far end wormhole for safety sake.");
                return false;
            }
            return false;
        }
        return false;
    }

    public void establishWormhole() {
        if (getGateEstablishWormholeTaskId() > 0) {
            WXTLogger.prettyLog(Level.FINER, false, "Wormhole \"" + getGateName() + "\" EstablishWormholeTaskIdID \"" + getGateEstablishWormholeTaskId() + "\" cancelled.");
            WormholeXTreme.getScheduler().cancelTask(getGateEstablishWormholeTaskId());
        }
        if (getGateTarget() == null || !isGateActive()) {
            return;
        }
        WXTLogger.prettyLog(Level.FINER, false, "Trying to establish link between '" + getGateName() + "' and '" + getGateTarget().getGateName() + "'");
        if (getGateTarget().getGateChevronsLocked()) {
            setWormholeEstablished(true);
            getGateTarget().setWormholeEstablished(true);
            WXTLogger.prettyLog(Level.FINER, false, "Chevrons locked on both sides. Starting thread ANIMATE_WOOSH.");
            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.ANIMATE_WOOSH), 1L);
            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(getGateTarget(), StargateUpdateRunnable.ActionToTake.ANIMATE_WOOSH), 1L);
            return;
        }
        WXTLogger.prettyLog(Level.FINER, false, "Chevrons where not locked on both sides. Restarting thread.");
        setGateEstablishWormholeTaskId(WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.ESTABLISH_WORMHOLE), 1L));
    }

    public void fillGateInterior(Material material) {
        for (Location loc : getGatePortalBlocks()) {
            Block blk = getGateWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            blk.setType(material);
            
       
            if (material == Material.NETHER_PORTAL) {
                org.bukkit.block.data.BlockData blockData = blk.getBlockData();
                if (blockData instanceof org.bukkit.block.data.Orientable) {
                    org.bukkit.block.data.Orientable orientable = (org.bukkit.block.data.Orientable) blockData;
                    if (getGateFacing() == org.bukkit.block.BlockFace.NORTH || getGateFacing() == org.bukkit.block.BlockFace.SOUTH) {
                        orientable.setAxis(org.bukkit.Axis.X); 
                    } else {
                        orientable.setAxis(org.bukkit.Axis.Z); 
                    }
                    blk.setBlockData(orientable);
                }
            }
            
            blk.getState().update();
        }
    }

    private int getGateEstablishWormholeTaskId() {
        return this.gateEstablishWormholeTaskId;
    }

    private int getGateActivateTaskId() {
        return this.gateActivateTaskId;
    }

    private int getGateAfterShutdownTaskId() {
        return this.gateAfterShutdownTaskId;
    }

    private ArrayList<Block> getGateAnimatedBlocks() {
        return this.gateAnimatedBlocks;
    }

    public void clearWooshAnimation() {
        for (Block b : getGateAnimatedBlocks()) {
            if (b != null) {
                b.setType(Material.AIR);
                StargateManager.getOpeningAnimationBlocks().remove(b.getLocation());
            }
        }
        getGateAnimatedBlocks().clear();
        setGateAnimationStep2D(0);
        setGateAnimationStep3D(1);
        setGateAnimationRemoving(false);
    }

    public int getGateAnimationStep2D() {
        return this.gateAnimationStep2D;
    }

    private int getGateAnimationStep3D() {
        return this.gateAnimationStep3D;
    }

    public Material getGateCustomIrisMaterial() {
        return this.gateCustomIrisMaterial;
    }

    public Material getGateCustomLightMaterial() {
        return this.gateCustomLightMaterial;
    }

    public int getGateCustomLightTicks() {
        return this.gateCustomLightTicks;
    }

    public Material getGateCustomPortalMaterial() {
        return this.gateCustomPortalMaterial;
    }

    public Material getGateCustomStructureMaterial() {
        return this.gateCustomStructureMaterial;
    }

    public int getGateCustomWooshDepth() {
        return this.gateCustomWooshDepth;
    }

    public int getGateCustomWooshDepthSquared() {
        return this.gateCustomWooshDepthSquared;
    }

    public int getGateCustomWooshTicks() {
        return this.gateCustomWooshTicks;
    }

    public Block getGateDialLeverBlock() {
        return this.gateDialLeverBlock;
    }

    public boolean isGateDialSwitchUsesButton() {
        return this.gateDialSwitchUsesButton;
    }

    private String fitSignLine(String text, String prefix, String suffix) {
        if (text == null) {
            text = "";
        }
        int availableLength = MAX_SIGN_LINE_LENGTH - prefix.length() - suffix.length();
        if (availableLength < 0) {
            availableLength = 0;
        }
        if (text.length() > availableLength) {
            text = text.substring(0, availableLength);
        }
        return prefix + text + suffix;
    }

    public synchronized Sign getGateDialSign() {
        return this.gateDialSign;
    }

    public synchronized Block getGateDialSignBlock() {
        return this.gateDialSignBlock;
    }

    public synchronized int getGateDialSignIndex() {
        return this.gateDialSignIndex;
    }

    public Stargate getGateDialSignTarget() {
        return this.gateDialSignTarget;
    }

    public BlockFace getGateFacing() {
        return this.gateFacing;
    }

    public long getGateId() {
        return this.gateId;
    }

    public String getGateIrisDeactivationCode() {
        return this.gateIrisDeactivationCode;
    }

    public Block getGateIrisLeverBlock() {
        return this.gateIrisLeverBlock;
    }

    public ArrayList<ArrayList<Location>> getGateLightBlocks() {
        return this.gateLightBlocks;
    }

    private int getGateLightingCurrentIteration() {
        return this.gateLightingCurrentIteration;
    }

    public Location getGateMinecartTeleportLocation() {
        return this.gateMinecartTeleportLocation;
    }

    public String getGateName() {
        return this.gateName;
    }

    public Block getGateNameBlockHolder() {
        return this.gateNameBlockHolder;
    }

    public StargateNetwork getGateNetwork() {
        return this.gateNetwork;
    }

    public String getGateOwner() {
        return this.gateOwner;
    }

    public Location getGatePlayerTeleportLocation() {
        return this.gatePlayerTeleportLocation;
    }

    public ArrayList<Location> getGatePortalBlocks() {
        return this.gatePortalBlocks;
    }

    public Block getGateRedstoneDialActivationBlock() {
        return this.gateRedstoneDialActivationBlock;
    }

    public Block getGateRedstoneGateActivatedBlock() {
        return this.gateRedstoneGateActivatedBlock;
    }

    public Block getGateRedstoneSignActivationBlock() {
        return this.gateRedstoneSignActivationBlock;
    }

    public StargateShape getGateShape() {
        return this.gateShape;
    }

    private int getGateShutdownTaskId() {
        return this.gateShutdownTaskId;
    }

    private HashMap<Integer, Stargate> getGateSignOrder() {
        return this.gateSignOrder;
    }

    public ArrayList<Location> getGateStructureBlocks() {
        return this.gateStructureBlocks;
    }

    public Stargate getGateTarget() {
        return this.gateTarget;
    }

    long getGateTempSignTarget() {
        return this.gateTempSignTarget;
    }

    long getGateTempTargetId() {
        return this.gateTempTargetId;
    }

    public ArrayList<ArrayList<Location>> getGateWooshBlocks() {
        return this.gateWooshBlocks;
    }

    public World getGateWorld() {
        return this.gateWorld;
    }

    public byte getLoadedVersion() {
        return this.loadedVersion;
    }

    public boolean isGateActive() {
        return this.gateActive;
    }

    public boolean getGateChevronsLocked() {
        return this.gateChevronsLocked;
    }

    private boolean isGateAnimationRemoving() {
        return this.gateAnimationRemoving;
    }

    public boolean isGateCustom() {
        return this.gateCustom;
    }

    public boolean isGateIrisActive() {
        return this.gateIrisActive;
    }

    private boolean isGateIrisDefaultActive() {
        return this.gateIrisDefaultActive;
    }

    public boolean isGateLightsActive() {
        return this.gateLightsActive;
    }

    public boolean isGateRecentlyActive() {
        return this.gateRecentlyActive;
    }

    public boolean isGateRedstonePowered() {
        return this.gateRedstonePowered;
    }

    public boolean isGateSignPowered() {
        return this.gateSignPowered;
    }

    public boolean isWormholeEstablished() {
        return this.gateEstablishedWormhole;
    }

    public void setWormholeEstablished(boolean established) {
        this.gateEstablishedWormhole = established;
    }

    public void lightStargate(boolean on) {
        WXTLogger.prettyLog(Level.FINE, false, "Lighting up '" + getGateName() + "'");
        if (on) {
            WXTLogger.prettyLog(Level.FINER, false, "Lighting up Order: " + getGateLightingCurrentIteration());
            if (getGateLightingCurrentIteration() == 0) {
                setGateLightsActive(true);
                setGateChevronsLocked(false);
            } else if (!isGateLightsActive()) {
                lightStargate(false);
                setGateLightingCurrentIteration(0);
                return;
            }
            setGateLightingCurrentIteration(getGateLightingCurrentIteration() + 1);
            if (getGateLightBlocks() != null) {
                if (getGateLightBlocks().size() > 0 && getGateLightBlocks().get(getGateLightingCurrentIteration()) != null) {
                    for (Location l : getGateLightBlocks().get(getGateLightingCurrentIteration())) {
                        Block b = getGateWorld().getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
                        b.setType(isGateCustom() ? getGateCustomLightMaterial() : getGateShape() != null ? getGateShape().getShapeLightMaterial() : Material.GLOWSTONE);
                    }
                }
                if (getGateLightingCurrentIteration() >= getGateLightBlocks().size() - 1) {
                    setGateLightingCurrentIteration(0);
                    setGateChevronsLocked(true);
                    WXTLogger.prettyLog(Level.FINE, false, "Locked Chevrons for gate '" + getGateName() + "'");
                    return;
                }
                WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.LIGHTUP), isGateCustom() ? getGateCustomLightTicks() : getGateShape() != null ? getGateShape().getShapeLightTicks() : 2L);
                return;
            }
            return;
        }
        WXTLogger.prettyLog(Level.FINE, false, "Cleanup lighting process for gate: '" + getGateName() + "'");
        setGateLightsActive(false);
        setGateChevronsLocked(false);
        if (getGateLightBlocks() != null) {
            for (int i = 0; i < getGateLightBlocks().size(); i++) {
                if (getGateLightBlocks().get(i) != null) {
                    for (Location l2 : getGateLightBlocks().get(i)) {
                        Block b2 = getGateWorld().getBlockAt(l2.getBlockX(), l2.getBlockY(), l2.getBlockZ());
                        b2.setType(isGateCustom() ? getGateCustomStructureMaterial() : getGateShape() != null ? getGateShape().getShapeStructureMaterial() : Material.OBSIDIAN);
                    }
                }
            }
        }
    }

    public void resetSign(boolean teleportSign) {
        if (!teleportSign) {
            return;
        }
        getGateDialSignBlock().setType(Material.OAK_WALL_SIGN); { org.bukkit.block.data.type.WallSign _wsd = (org.bukkit.block.data.type.WallSign) getGateDialSignBlock().getBlockData(); _wsd.setFacing(getGateFacing()); getGateDialSignBlock().setBlockData(_wsd); }
        setGateDialSign((Sign) getGateDialSignBlock().getState());
        getGateDialSign().setLine(0, fitSignLine(getGateName(), "", ""));
        if (getGateNetwork() != null) {
            getGateDialSign().setLine(1, getGateNetwork().getNetworkName());
        } else {
            getGateDialSign().setLine(1, "");
        }
        getGateDialSign().setLine(2, "");
        getGateDialSign().setLine(3, "");
        getGateDialSign().update(true);
    }

    public void resetTeleportSign() {
        if (getGateDialSignBlock() != null && getGateDialSign() != null) {
            getGateDialSignBlock().setType(Material.AIR);
            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.DIAL_SIGN_RESET), 2L);
        }
    }

    private void setGateActivateTaskId(int gateActivateTaskId) {
        this.gateActivateTaskId = gateActivateTaskId;
    }

    private void setGateEstablishWormholeTaskId(int gateEstablishWormholeTaskId) {
        this.gateEstablishWormholeTaskId = gateEstablishWormholeTaskId;
    }

    public void setGateActive(boolean gateActive) {
        this.gateActive = gateActive;
    }

    private void setGateAfterShutdownTaskId(int gateAfterShutdownTaskId) {
        this.gateAfterShutdownTaskId = gateAfterShutdownTaskId;
    }

    private void setGateAnimationRemoving(boolean gateAnimationRemoving) {
        this.gateAnimationRemoving = gateAnimationRemoving;
    }

    public void setGateAnimationStep2D(int gateAnimationStep2D) {
        this.gateAnimationStep2D = gateAnimationStep2D;
    }

    private void setGateAnimationStep3D(int gateAnimationStep3D) {
        this.gateAnimationStep3D = gateAnimationStep3D;
    }

    public void setGateCustom(boolean gateCustom) {
        this.gateCustom = gateCustom;
    }

    public void setGateCustomIrisMaterial(Material gateCustomIrisMaterial) {
        this.gateCustomIrisMaterial = gateCustomIrisMaterial;
    }

    public void setGateCustomLightMaterial(Material gateCustomLightMaterial) {
        this.gateCustomLightMaterial = gateCustomLightMaterial;
    }

    public void setGateCustomLightTicks(int gateCustomLightTicks) {
        this.gateCustomLightTicks = gateCustomLightTicks;
    }

    public void setGateCustomPortalMaterial(Material gateCustomPortalMaterial) {
        this.gateCustomPortalMaterial = gateCustomPortalMaterial;
    }

    public void setGateCustomStructureMaterial(Material gateCustomStructureMaterial) {
        this.gateCustomStructureMaterial = gateCustomStructureMaterial;
    }

    public void setGateCustomWooshDepth(int gateCustomWooshDepth) {
        this.gateCustomWooshDepth = gateCustomWooshDepth;
    }

    public void setGateCustomWooshDepthSquared(int gateCustomWooshDepthSquared) {
        this.gateCustomWooshDepthSquared = gateCustomWooshDepthSquared;
    }

    public void setGateCustomWooshTicks(int gateCustomWooshTicks) {
        this.gateCustomWooshTicks = gateCustomWooshTicks;
    }

    public void setGateDialLeverBlock(Block gateDialLeverBlock) {
        this.gateDialLeverBlock = gateDialLeverBlock;
        this.gateDialSwitchUsesButton = gateDialLeverBlock != null && isButtonMaterial(gateDialLeverBlock.getType());
    }

    public synchronized void setGateDialSign(Sign gateDialSign) {
        this.gateDialSign = gateDialSign;
    }

    public synchronized void setGateDialSignBlock(Block gateDialSignBlock) {
        this.gateDialSignBlock = gateDialSignBlock;
    }

    public synchronized void setGateDialSignIndex(int gateDialSignIndex) {
        this.gateDialSignIndex = gateDialSignIndex;
    }

    protected void setGateDialSignTarget(Stargate gateDialSignTarget) {
        this.gateDialSignTarget = gateDialSignTarget;
    }

    public void setGateFacing(BlockFace gateFacing) {
        this.gateFacing = gateFacing;
    }

    private boolean isButtonMaterial(Material material) {
        return material != null && material.name().endsWith("_BUTTON");
    }

    void setGateId(long gateId) {
        this.gateId = gateId;
    }

    public void setGateIrisActive(boolean gateIrisActive) {
        this.gateIrisActive = gateIrisActive;
    }

    public void setGateIrisDeactivationCode(String gateIrisDeactivationCode) {
        this.gateIrisDeactivationCode = gateIrisDeactivationCode;
    }

    public void setGateIrisDefaultActive(boolean gateIrisDefaultActive) {
        this.gateIrisDefaultActive = gateIrisDefaultActive;
    }

    public void setGateIrisLeverBlock(Block gateIrisLeverBlock) {
        this.gateIrisLeverBlock = gateIrisLeverBlock;
    }

    private void setGateLightingCurrentIteration(int gateLightingCurrentIteration) {
        this.gateLightingCurrentIteration = gateLightingCurrentIteration;
    }

    public void setGateLightsActive(boolean gateLightsActive) {
        this.gateLightsActive = gateLightsActive;
    }

    public void setGateMinecartTeleportLocation(Location gateMinecartTeleportLocation) {
        this.gateMinecartTeleportLocation = gateMinecartTeleportLocation;
    }

    public void setGateName(String gateName) {
        this.gateName = gateName;
    }

    public void setGateNameBlockHolder(Block gateNameBlockHolder) {
        this.gateNameBlockHolder = gateNameBlockHolder;
    }

    public void setGateNetwork(StargateNetwork gateNetwork) {
        this.gateNetwork = gateNetwork;
    }

    public void setGateOwner(String gateOwner) {
        this.gateOwner = gateOwner;
    }

    public void setGatePlayerTeleportLocation(Location gatePlayerTeleportLocation) {
        this.gatePlayerTeleportLocation = gatePlayerTeleportLocation;
    }

    private void setGateRecentlyActive(boolean gateRecentlyActive) {
        this.gateRecentlyActive = gateRecentlyActive;
    }

    public void setGateRedstoneDialActivationBlock(Block gateRedstoneDialActivationBlock) {
        this.gateRedstoneDialActivationBlock = gateRedstoneDialActivationBlock;
    }

    public void setGateRedstoneGateActivatedBlock(Block gateRedstoneGateActivatedBlock) {
        this.gateRedstoneGateActivatedBlock = gateRedstoneGateActivatedBlock;
    }

    public void setGateRedstonePowered(boolean gateRedstonePowered) {
        this.gateRedstonePowered = gateRedstonePowered;
    }

    public void setGateRedstoneSignActivationBlock(Block gateRedstoneSignActivationBlock) {
        this.gateRedstoneSignActivationBlock = gateRedstoneSignActivationBlock;
    }

    public void setGateShape(StargateShape gateShape) {
        this.gateShape = gateShape;
    }

    private void setGateShutdownTaskId(int gateShutdownTaskId) {
        this.gateShutdownTaskId = gateShutdownTaskId;
    }

    public void setGateSignPowered(boolean gateSignPowered) {
        this.gateSignPowered = gateSignPowered;
    }

    private void setGateTarget(Stargate gateTarget) {
        this.gateTarget = gateTarget;
    }

    public void setGateTempSignTarget(long gateTempSignTarget) {
        this.gateTempSignTarget = gateTempSignTarget;
    }

    public void setGateTempTargetId(long gateTempTargetId) {
        this.gateTempTargetId = gateTempTargetId;
    }

    public void setGateWorld(World gateWorld) {
        this.gateWorld = gateWorld;
    }

    public String getLastUsedBy() {
        return this.lastUsedBy;
    }

    public void setLastUsedBy(Player player) {
        setLastUsedBy(player.getName());
    }

    public void setLastUsedBy(String playerName) {
        this.lastUsedBy = playerName;
    }

    public int getVisitCount() {
        return this.visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    public void incrementVisitCount() {
        this.visitCount++;
    }

    public String getSourceGateName() {
        return this.gateSourceName;
    }

    public void setSourceGateName(String gateName) {
        this.gateSourceName = gateName;
    }

    public void setGateChevronsLocked(boolean locked) {
        this.gateChevronsLocked = locked;
    }

    public boolean gateChevronsLocked() {
        return this.gateChevronsLocked;
    }

    public void setIrisDeactivationCode(String idc) {
        if (idc != null && !idc.equals("")) {
            setGateIrisDeactivationCode(idc);
            setupIrisLever(true);
        } else {
            setIrisState(false);
            setupIrisLever(false);
            setGateIrisDeactivationCode("");
        }
    }

    private void setIrisState(boolean irisactive) {
        setGateIrisActive(irisactive);
        fillGateInterior(isGateIrisActive() ? isGateCustom() ? getGateCustomIrisMaterial() : getGateShape() != null ? getGateShape().getShapeIrisMaterial() : Material.STONE : isGateActive() ? isGateCustom() ? getGateCustomPortalMaterial() : getGateShape() != null ? getGateShape().getShapePortalMaterial() : Material.WATER : Material.AIR);
        if (getGateIrisLeverBlock() != null && getGateIrisLeverBlock().getType() == Material.LEVER) {
            { if (getGateIrisLeverBlock().getBlockData() instanceof org.bukkit.block.data.type.Switch) { org.bukkit.block.data.type.Switch _sw = (org.bukkit.block.data.type.Switch) getGateIrisLeverBlock().getBlockData(); _sw.setPowered(isGateIrisActive()); getGateIrisLeverBlock().setBlockData(_sw); } }
        }
    }

    public void setLoadedVersion(byte loadedVersion) {
        this.loadedVersion = loadedVersion;
    }

    public void setupGateSign(boolean create) {
        if (getGateNameBlockHolder() != null) {
            if (create) {
                Block nameSign = getGateNameBlockHolder().getRelative(getGateFacing());
                getGateStructureBlocks().add(nameSign.getLocation());
                nameSign.setType(Material.OAK_WALL_SIGN); { org.bukkit.block.data.type.WallSign _wsd2 = (org.bukkit.block.data.type.WallSign) nameSign.getBlockData(); _wsd2.setFacing(getGateFacing()); nameSign.setBlockData(_wsd2); }
                Sign sign = (Sign) nameSign.getState();
                sign.setLine(0, fitSignLine(getGateName(), "-", "-"));
                if (getGateNetwork() != null) {
                    sign.setLine(1, "N:" + getGateNetwork().getNetworkName());
                }
                if (getGateOwner() != null) {
                    sign.setLine(2, "O:" + getGateOwner());
                }
                sign.update(true);
                return;
            }
            Block nameSign2 = getGateNameBlockHolder().getRelative(getGateFacing());
            if (org.bukkit.Tag.WALL_SIGNS.isTagged(nameSign2.getType())) {
                getGateStructureBlocks().remove(nameSign2.getLocation());
                nameSign2.setType(Material.AIR);
            }
        }
    }

    public void setupIrisLever(boolean create) {
        if (getGateIrisLeverBlock() == null && getGateShape() != null && !(getGateShape() instanceof Stargate3DShape)) {
            setGateIrisLeverBlock(getGateDialLeverBlock().getRelative(BlockFace.DOWN));
        }
        if (getGateIrisLeverBlock() != null) {
            if (create) {
                getGateStructureBlocks().add(getGateIrisLeverBlock().getLocation());
                getGateIrisLeverBlock().setType(Material.LEVER); { org.bukkit.block.data.type.Switch _sw2 = (org.bukkit.block.data.type.Switch) getGateIrisLeverBlock().getBlockData(); _sw2.setFacing(getGateFacing()); _sw2.setFace(org.bukkit.block.data.type.Switch.Face.WALL); getGateIrisLeverBlock().setBlockData(_sw2); }
            } else if (getGateIrisLeverBlock().getType().equals(Material.LEVER)) {
                getGateStructureBlocks().remove(getGateIrisLeverBlock().getLocation());
                getGateIrisLeverBlock().setType(Material.AIR);
            }
        }
    }

    public void setupRedstone(boolean create) {
        if (isGateSignPowered()) {
            setupRedstoneDialWire(create);
            setupRedstoneSignDialWire(create);
        }
        setupRedstoneGateActivatedLever(create);
    }

    private void setupRedstoneDialWire(boolean create) {
        if (getGateRedstoneDialActivationBlock() != null) {
            if (create) {
                getGateStructureBlocks().add(getGateRedstoneDialActivationBlock().getLocation());
                getGateRedstoneDialActivationBlock().setType(Material.REDSTONE_WIRE);
            } else if (getGateRedstoneGateActivatedBlock().getType() == Material.REDSTONE_WIRE) {
                getGateStructureBlocks().remove(getGateRedstoneDialActivationBlock().getLocation());
                getGateRedstoneDialActivationBlock().setType(Material.AIR);
            }
        }
    }

    private void setupRedstoneGateActivatedLever(boolean create) {
        if (getGateRedstoneGateActivatedBlock() != null) {
            if (create) {
                getGateStructureBlocks().add(getGateRedstoneGateActivatedBlock().getLocation());
                getGateRedstoneGateActivatedBlock().setType(Material.LEVER); { org.bukkit.block.data.type.Switch _sw3 = (org.bukkit.block.data.type.Switch) getGateRedstoneGateActivatedBlock().getBlockData(); _sw3.setFace(org.bukkit.block.data.type.Switch.Face.FLOOR); getGateRedstoneGateActivatedBlock().setBlockData(_sw3); }
            } else if (getGateRedstoneGateActivatedBlock().getType().equals(Material.LEVER)) {
                getGateStructureBlocks().remove(getGateRedstoneGateActivatedBlock().getLocation());
                getGateRedstoneGateActivatedBlock().setType(Material.AIR);
            }
        }
    }

    private void setupRedstoneSignDialWire(boolean create) {
        if (getGateRedstoneSignActivationBlock() != null) {
            if (create) {
                getGateStructureBlocks().add(getGateRedstoneSignActivationBlock().getLocation());
                getGateRedstoneSignActivationBlock().setType(Material.REDSTONE_WIRE);
            } else if (getGateRedstoneGateActivatedBlock().getType() == Material.REDSTONE_WIRE) {
                getGateStructureBlocks().remove(getGateRedstoneSignActivationBlock().getLocation());
                getGateRedstoneSignActivationBlock().setType(Material.AIR);
            }
        }
    }

    public void shutdownStargate(boolean timer) {
        if (getGateShutdownTaskId() > 0) {
            WXTLogger.prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" ShutdownTaskID \"" + getGateShutdownTaskId() + "\" cancelled.");
            WormholeXTreme.getScheduler().cancelTask(getGateShutdownTaskId());
            setGateShutdownTaskId(-1);
        }
        if (getGateTarget() != null) {
            getGateTarget().shutdownStargate(true);
            getGateTarget().setSourceGateName(null);
        }
        setGateTarget(null);
        if (timer) {
            setGateRecentlyActive(true);
        }
        setGateActive(false);
        lightStargate(false);
        setWormholeEstablished(false);
        setSourceGateName(null);
        toggleDialLeverState(false);
        toggleRedstoneGateActivatedPower();
        if (isGateIrisDefaultActive()) {
            setIrisState(isGateIrisDefaultActive());
        } else if (!isGateIrisActive()) {
            fillGateInterior(Material.AIR);
        }
        if (timer) {
            startAfterShutdownTimer();
        }
        WorldUtils.scheduleChunkUnload(getGatePlayerTeleportLocation().getBlock());
        StargateManager.removeActivatedStargate(getGateName());
        if (WormholePlayerManager.findPlayerByGateName(getGateName()) != null) {
            WormholePlayerManager.findPlayerByGateName(getGateName()).removeStargate(getGateName());
        }
    }

    public void startActivationTimer(Player p) {
        if (getGateActivateTaskId() > 0) {
            WormholeXTreme.getScheduler().cancelTask(getGateActivateTaskId());
        }
        int timeout = ConfigManager.getTimeoutActivate() * 20;
        setGateActivateTaskId(WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.DEACTIVATE), timeout));
        WXTLogger.prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" ActivateTaskID \"" + getGateActivateTaskId() + "\" created.");
    }

    private void startAfterShutdownTimer() {
        if (getGateAfterShutdownTaskId() > 0) {
            WormholeXTreme.getScheduler().cancelTask(getGateAfterShutdownTaskId());
        }
        setGateAfterShutdownTaskId(WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.AFTERSHUTDOWN), 60L));
        WXTLogger.prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" AfterShutdownTaskID \"" + getGateAfterShutdownTaskId() + "\" created.");
        if (getGateAfterShutdownTaskId() == -1) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Failed to schdule wormhole after shutdown, received task id of -1.");
            setGateRecentlyActive(false);
        }
    }

    public void stopActivationTimer() {
        if (getGateActivateTaskId() > 0) {
            WXTLogger.prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" ActivateTaskID \"" + getGateActivateTaskId() + "\" cancelled.");
            WormholeXTreme.getScheduler().cancelTask(getGateActivateTaskId());
            setGateActivateTaskId(-1);
        }
    }

    public void stopAfterShutdownTimer() {
        if (getGateAfterShutdownTaskId() > 0) {
            WXTLogger.prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" AfterShutdownTaskID \"" + getGateAfterShutdownTaskId() + "\" cancelled.");
            WormholeXTreme.getScheduler().cancelTask(getGateAfterShutdownTaskId());
            setGateAfterShutdownTaskId(-1);
        }
        setGateRecentlyActive(false);
    }

    public void dialSignClicked() {
        dialSignClicked(null);
    }

    public Location getLocation() {
        return getGateDialLeverBlock().getLocation();
    }

    public World getWorld() {
        return getGateDialLeverBlock().getWorld();
    }

    public boolean isValid() {
        return this.stargateIsValid;
    }

    public void invalidateGate() {
        this.stargateIsValid = false;
    }

    public void dialSignClicked(Action eventAction) {
        String line2;
        synchronized (getGateNetwork().getNetworkGateLock()) {
            getGateDialSignBlock().setType(Material.OAK_WALL_SIGN); { org.bukkit.block.data.type.WallSign _wsd3 = (org.bukkit.block.data.type.WallSign) getGateDialSignBlock().getBlockData(); _wsd3.setFacing(getGateFacing()); getGateDialSignBlock().setBlockData(_wsd3); }
            try {
                if (getGateDialSign() == null) {
                    setGateDialSign((Sign) getGateDialSignBlock().getState());
                }
                if (!getGateDialSign().getType().equals(Material.OAK_WALL_SIGN)) {
                    throw new WormholeDialSignException("Expected WALL_SIGN. Found '" + getGateDialSign().getType().name() + "' for gate '" + getGateName() + "' in world '" + getWorld().getName() + "'.");
                }
                getGateDialSign().setLine(0, "-" + getGateName() + "-");
                String lineMarkerS = ">" + ChatColor.GREEN;
                String lineMarkerE = ChatColor.BLACK + "<";
                if (getGateNetwork().getNetworkSignGateList().size() <= 1) {
                    getGateDialSign().setLine(1, "");
                    getGateDialSign().setLine(2, ChatColor.DARK_RED + "No Other Gates" + ChatColor.BLACK);
                    getGateDialSign().setLine(3, "");
                    getGateDialSign().update();
                    setGateDialSignTarget(null);
                    return;
                }
                int signCount = getGateNetwork().getNetworkSignGateList().size();
                int direction = 1;
                if (eventAction != null && eventAction.equals(Action.RIGHT_CLICK_BLOCK)) {
                    direction = -1;
                }
                ArrayList<Stargate> signGates = new ArrayList<>();
                for (Stargate gate : getGateNetwork().getNetworkSignGateList()) {
                    if (gate != null && gate.getGateName() != null && !gate.getGateName().equals(getGateName())) {
                        signGates.add(gate);
                    }
                }
                if (signGates.isEmpty()) {
                    getGateDialSign().setLine(1, "");
                    getGateDialSign().setLine(2, ChatColor.DARK_RED + "No Other Gates" + ChatColor.BLACK);
                    getGateDialSign().setLine(3, "");
                    getGateDialSign().update();
                    setGateDialSignTarget(null);
                    return;
                }
                int currentIndex = -1;
                Stargate currentTarget = getGateDialSignTarget();
                if (currentTarget != null) {
                    for (int i = 0; i < signGates.size(); i++) {
                        if (signGates.get(i).getGateId() == currentTarget.getGateId()
                                || (signGates.get(i).getGateName() != null && signGates.get(i).getGateName().equals(currentTarget.getGateName()))) {
                            currentIndex = i;
                            break;
                        }
                    }
                }
                if (currentIndex == -1) {
                    int savedIndex = getGateDialSignIndex();
                    if (savedIndex >= 0 && savedIndex < signGates.size()) {
                        currentIndex = savedIndex;
                    }
                }
                int nextIndex;
                if (currentIndex == -1) {
                    nextIndex = direction == 1 ? 0 : signGates.size() - 1;
                } else {
                    nextIndex = ((currentIndex + direction) % signGates.size() + signGates.size()) % signGates.size();
                }
                currentTarget = signGates.get(nextIndex);
                setGateDialSignTarget(currentTarget);
                setGateDialSignIndex(nextIndex);
                String line1 = "";
                String line3 = "";
                if (signGates.size() > 1) {
                    line1 = signGates.get((nextIndex - 1 + signGates.size()) % signGates.size()).getGateName();
                    line3 = signGates.get((nextIndex + 1) % signGates.size()).getGateName();
                }
                line2 = lineMarkerS + currentTarget.getGateName() + lineMarkerE;
                getGateDialSign().setLine(1, line1);
                getGateDialSign().setLine(2, line2);
                getGateDialSign().setLine(3, line3);
                getGateDialSign().update(true);
            } catch (WormholeDialSignException e) {
                WXTLogger.prettyLog(Level.WARNING, false, e.getMessage());
            } catch (ClassCastException e2) {
                throw new WormholeDialSignException("Could not set DialSign for gate '" + getGateName() + "' in world '" + getWorld().getName() + "'. Cast State to Sign failed for BlockType '" + getGateDialSignBlock().getType().name() + "'");
            }
        }
    }

    public void timeoutStargate() {
        if (getGateActivateTaskId() > 0) {
            WXTLogger.prettyLog(Level.FINE, false, "Wormhole \"" + getGateName() + "\" ActivateTaskID \"" + getGateActivateTaskId() + "\" timed out.");
            setGateActivateTaskId(-1);
        }
        WormholePlayer wormholePlayer = WormholePlayerManager.getRegisteredWormholePlayer(getLastUsedBy());
        if (isGateIrisDefaultActive()) {
            setIrisState(isGateIrisDefaultActive());
        }
        if (isGateLightsActive()) {
            lightStargate(false);
        }
        if (wormholePlayer != null) {
            wormholePlayer.sendMessage("Gate: " + getGateName() + " timed out and deactivated.");
            wormholePlayer.removeStargate(getGateName());
        }
        StargateManager.removeActivatedStargate(getGateName());
    }

    public void toggleDialLeverState(boolean regenerate) {
        if (getGateDialLeverBlock() != null) {
            if (isGateActive()) {
                WorldUtils.scheduleChunkLoad(getGateDialLeverBlock());
            }
            Material material = getGateDialLeverBlock().getType();
            boolean useButton = isButtonMaterial(material) || this.gateDialSwitchUsesButton;
            if (regenerate) {
                if (useButton) {
                    getGateDialLeverBlock().setType(Material.STONE_BUTTON);
                    if (getGateDialLeverBlock().getBlockData() instanceof org.bukkit.block.data.type.Switch) {
                        org.bukkit.block.data.type.Switch switchData = (org.bukkit.block.data.type.Switch) getGateDialLeverBlock().getBlockData();
                        switchData.setFacing(getGateFacing());
                        switchData.setFace(org.bukkit.block.data.type.Switch.Face.WALL);
                        getGateDialLeverBlock().setBlockData(switchData);
                    }
                } else {
                    getGateDialLeverBlock().setType(Material.LEVER);
                    if (getGateDialLeverBlock().getBlockData() instanceof org.bukkit.block.data.type.Switch) {
                        org.bukkit.block.data.type.Switch switchData = (org.bukkit.block.data.type.Switch) getGateDialLeverBlock().getBlockData();
                        switchData.setFacing(getGateFacing());
                        switchData.setFace(org.bukkit.block.data.type.Switch.Face.WALL);
                        getGateDialLeverBlock().setBlockData(switchData);
                    }
                }
                material = getGateDialLeverBlock().getType();
            }
            if (isButtonMaterial(material)) {
                if (getGateDialLeverBlock().getBlockData() instanceof org.bukkit.block.data.type.Switch) {
                    org.bukkit.block.data.type.Switch switchData = (org.bukkit.block.data.type.Switch) getGateDialLeverBlock().getBlockData();
                    switchData.setPowered(isGateActive());
                    getGateDialLeverBlock().setBlockData(switchData);
                }
            } else if (material == Material.LEVER) {
                if (getGateDialLeverBlock().getBlockData() instanceof org.bukkit.block.data.type.Switch) {
                    org.bukkit.block.data.type.Switch switchData = (org.bukkit.block.data.type.Switch) getGateDialLeverBlock().getBlockData();
                    switchData.setPowered(isGateActive());
                    getGateDialLeverBlock().setBlockData(switchData);
                }
            }
            if (!isGateActive()) {
                WorldUtils.scheduleChunkUnload(getGateDialLeverBlock());
            }
            WXTLogger.prettyLog(Level.FINE, false, "Dial Button Lever Gate: \"" + getGateName() + "\" Material: \"" + material.toString() + "\" State: \"" + (isGateActive() ? 1 : 0) + "\"");
        }
    }

    /* JADX INFO: renamed from: de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate$1, reason: invalid class name */
    /* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/model/Stargate$1.class */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$org$bukkit$Material = new int[Material.values().length];

        static {
            try {
                $SwitchMap$org$bukkit$Material[Material.STONE_BUTTON.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$bukkit$Material[Material.LEVER.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public void toggleIrisActive(boolean setDefault) {
        setGateIrisActive(!isGateIrisActive());
        setIrisState(isGateIrisActive());
        if (setDefault) {
            setGateIrisDefaultActive(isGateIrisActive());
        }
    }

    private void toggleRedstoneGateActivatedPower() {
        if (isGateRedstonePowered() && getGateRedstoneGateActivatedBlock() != null && getGateRedstoneGateActivatedBlock().getType() == Material.LEVER) {
            byte leverState = getGateRedstoneGateActivatedBlock().getData();
            { if (getGateRedstoneGateActivatedBlock().getBlockData() instanceof org.bukkit.block.data.type.Switch) { org.bukkit.block.data.type.Switch _sw8 = (org.bukkit.block.data.type.Switch) getGateRedstoneGateActivatedBlock().getBlockData(); _sw8.setPowered(isGateActive()); getGateRedstoneGateActivatedBlock().setBlockData(_sw8); } }
        }
    }

    public boolean tryClickTeleportSign(Block clickedBlock) {
        return tryClickTeleportSign(clickedBlock, Action.LEFT_CLICK_BLOCK, null);
    }

    public boolean tryClickTeleportSign(Block clickedBlock, Action eventAction) {
        return tryClickTeleportSign(clickedBlock, eventAction, null);
    }

    public boolean tryClickTeleportSign(Block clickedBlock, Action eventAction, String triggeredByPlayer) {
        if (getGateDialSign() == null && getGateDialSignBlock() != null) {
            if (org.bukkit.Tag.WALL_SIGNS.isTagged(getGateDialSignBlock().getType())) {
                if (eventAction == null) {
                    getGateDialSignBlock().setType(Material.AIR);
                }
                WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.DIAL_SIGN_CLICK, eventAction));
                return true;
            }
            return false;
        }
        if (WorldUtils.isSameBlock(clickedBlock, getGateDialSignBlock())) {
            if (eventAction == null && getGateDialSignBlock() != null) {
                getGateDialSignBlock().setType(Material.AIR);
            }
            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(this, StargateUpdateRunnable.ActionToTake.DIAL_SIGN_CLICK, eventAction));
            return true;
        }
        return false;
    }
}