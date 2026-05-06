package de.luricos.bukkit.WormholeXTreme.Wormhole.listeners;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateHelper;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateShape;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.StargateRestrictions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.PlayerOrientation;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayer;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayerManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WorldUtils;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.material.Button;
import org.bukkit.material.Lever;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/listeners/WormholeXTremePlayerListener.class */
public class WormholeXTremePlayerListener implements Listener {
    protected static boolean buttonLeverHit(Player player, Block clickedBlock, BlockFace direction) {
        Stargate newGate;
        Stargate stargate = StargateManager.getGateFromBlock(clickedBlock);
        if (stargate != null) {
            if (!WorldUtils.isSameBlock(stargate.getGateDialLeverBlock(), clickedBlock) || (!(stargate.isGateSignPowered() && WXPermissions.checkPermission(player, stargate, WXPermissions.PermissionType.SIGN)) && (stargate.isGateSignPowered() || !WXPermissions.checkPermission(player, stargate, WXPermissions.PermissionType.DIALER)))) {
                if (WorldUtils.isSameBlock(stargate.getGateIrisLeverBlock(), clickedBlock) && !stargate.isGateSignPowered() && WXPermissions.checkPermission(player, stargate, WXPermissions.PermissionType.DIALER)) {
                    Lever lever = new Lever(clickedBlock.getType(), clickedBlock.getData());
                    WXTLogger.prettyLog(Level.FINE, false, "Player '" + player.getName() + "' has triggered the iris lever of gate '" + stargate.getGateName() + "' status is now " + (!lever.isPowered()));
                    stargate.toggleIrisActive(true);
                    return true;
                }
                if (WorldUtils.isSameBlock(stargate.getGateIrisLeverBlock(), clickedBlock) || WorldUtils.isSameBlock(stargate.getGateDialLeverBlock(), clickedBlock)) {
                    player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
                    WormholePlayerManager.getRegisteredWormholePlayer(player).removeStargate(stargate);
                    return true;
                }
                return true;
            }
            WormholePlayerManager.getRegisteredWormholePlayer(player).addStargate(stargate);
            WormholePlayer wormholePlayer = handleGateActivationSwitch(player);
            if (wormholePlayer == null) {
                return true;
            }
            if (!wormholePlayer.getProperties().hasReceivedRemoteActiveMessage() && wormholePlayer.getProperties().hasActivatedStargate()) {
                if (wormholePlayer.getStargate().getGateName() != null ? !wormholePlayer.getStargate().getGateName().equals(stargate.getGateName()) : stargate.getGateName() != null) {
                    if (!wormholePlayer.getStargate().isGateActive()) {
                        WXTLogger.prettyLog(Level.FINE, false, "Gate '" + wormholePlayer.getStargate().getGateName() + "' didnt lightened up.");
                    }
                    WXTLogger.prettyLog(Level.FINE, false, "New gate for player '" + player.getName() + "' was set to stargate '" + wormholePlayer.getStargate().getGateName() + "'");
                }
            } else {
                WXTLogger.prettyLog(Level.FINE, false, "Gate '" + stargate.getGateName() + "' was remote active for player '" + player.getName() + "': no permission, invalid gate target");
                wormholePlayer.getProperties().setHasReceivedRemoteActiveMessage(true);
            }
            if (wormholePlayer.getProperties().hasShutdownGate() || wormholePlayer.getProperties().hasReceivedIrisLockMessage() || !wormholePlayer.getProperties().hasActivatedStargate() || wormholePlayer.getProperties().hasReceivedRemoteActiveMessage()) {
                wormholePlayer.removeStargate(stargate);
                return true;
            }
            return true;
        }
        if (direction == null) {
            Button directionButton = new Button(Material.STONE_BUTTON);
            directionButton.setData(clickedBlock.getData());
            direction = directionButton.getFacing();
            if (direction == null) {
                return false;
            }
        }
        StargateShape shape = StargateManager.getPlayerBuilderShape(player);
        if (shape != null) {
            newGate = StargateHelper.checkStargate(clickedBlock, direction, shape);
        } else {
            WXTLogger.prettyLog(Level.FINEST, false, "Attempting to find any gate shapes!");
            newGate = StargateHelper.checkStargate(clickedBlock, direction);
        }
        if (newGate != null) {
            if (WXPermissions.checkPermission(player, newGate, WXPermissions.PermissionType.BUILD) && !StargateRestrictions.isPlayerBuildRestricted(player)) {
                if (newGate.isGateSignPowered()) {
                    player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Stargate Design Valid with Sign Nav.");
                    if (newGate.getGateName().equals("")) {
                        player.sendMessage(ConfigManager.MessageStrings.constructNameInvalid.toString() + "\"\"");
                        return true;
                    }
                    boolean success = StargateManager.completeStargate(player, newGate);
                    if (success) {
                        player.sendMessage(ConfigManager.MessageStrings.constructSuccess.toString());
                        newGate.getGateDialSign().setLine(0, "-" + newGate.getGateName() + "-");
                        newGate.getGateDialSign().setData(newGate.getGateDialSign().getData());
                        newGate.getGateDialSign().update();
                        return true;
                    }
                    player.sendMessage("Stargate constrution failed!?");
                    return true;
                }
                player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Valid Stargate Design! §3:: §B<required> §6[optional]");
                player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Type '§F/wxcomplete §B<name> §6[idc=IDC] [net=NET]§7' to complete.");
                StargateManager.addIncompleteStargate(player.getName(), newGate);
                return true;
            }
            if (newGate.isGateSignPowered()) {
                newGate.resetTeleportSign();
            }
            StargateManager.removeIncompleteStargate(player);
            if (StargateRestrictions.isPlayerBuildRestricted(player)) {
                player.sendMessage(ConfigManager.MessageStrings.playerBuildCountRestricted.toString());
            }
            player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            return true;
        }
        WXTLogger.prettyLog(Level.FINE, false, player.getName() + " has pressed a button or lever but we did not find a valid gate shape");
        return false;
    }

    protected static WormholePlayer handleGateActivationSwitch(Player player) {
        WormholePlayer wormholePlayer = WormholePlayerManager.getRegisteredWormholePlayer(player);
        if (wormholePlayer == null || wormholePlayer.getStargate() == null) {
            return null;
        }
        Stargate currentGate = wormholePlayer.getStargate();
        if (currentGate.isGateActive() || currentGate.isGateLightsActive()) {
            if (currentGate.getGateTarget() != null) {
                currentGate.shutdownStargate(true);
                player.sendMessage(String.format(ConfigManager.MessageStrings.gateShutdown.toString(), currentGate.getGateName() + " "));
                wormholePlayer.getProperties().setHasShutdownGate(true);
                return wormholePlayer;
            }
            if (currentGate.getSourceGateName() == null && currentGate.isGateActive()) {
                currentGate.stopActivationTimer();
                currentGate.setGateActive(false);
                currentGate.toggleDialLeverState(false);
                currentGate.lightStargate(false);
                wormholePlayer.getProperties().setHasActivatedStargate(false);
                currentGate.setLastUsedBy(wormholePlayer.getPlayer());
                player.sendMessage(String.format(ConfigManager.MessageStrings.gateDeactivated.toString(), currentGate.getGateName() + " "));
                return wormholePlayer;
            }
            if (currentGate.isGateLightsActive() && !currentGate.isGateActive() && currentGate.getLastUsedBy() != wormholePlayer.getName()) {
                player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Gate has been activated by '" + currentGate.getLastUsedBy() + "' already.");
                wormholePlayer.getProperties().setHasReceivedWasActivatedOther(true);
            } else if (currentGate.isGateLightsActive() && !currentGate.isGateActive() && currentGate.getLastUsedBy().equals(wormholePlayer.getName())) {
                currentGate.stopActivationTimer();
                currentGate.setGateActive(false);
                currentGate.toggleDialLeverState(false);
                currentGate.lightStargate(false);
                wormholePlayer.getProperties().setHasActivatedStargate(false);
                currentGate.setLastUsedBy(wormholePlayer.getPlayer());
                player.sendMessage(String.format(ConfigManager.MessageStrings.gateDeactivated.toString(), currentGate.getGateName() + " "));
            } else {
                wormholePlayer.getProperties().setHasReceivedRemoteActiveMessage(true);
                Stargate sourceGate = StargateManager.getStargate(currentGate.getSourceGateName());
                if (sourceGate != null) {
                    player.sendMessage(String.format(ConfigManager.MessageStrings.gateRemoteActive.toString(), currentGate.getGateName() + " ", " by " + sourceGate.getLastUsedBy()));
                    player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Detected Wormhole source " + currentGate.getSourceGateName());
                }
            }
            return wormholePlayer;
        }
        if (currentGate.isGateSignPowered()) {
            if (WXPermissions.checkPermission(player, currentGate, WXPermissions.PermissionType.SIGN)) {
                if (currentGate.getGateDialSign() == null && currentGate.getGateDialSignBlock() != null) {
                    currentGate.tryClickTeleportSign(currentGate.getGateDialSignBlock());
                }
                if (currentGate.getGateDialSignTarget() != null) {
                    if (currentGate.dialStargate(currentGate.getGateDialSignTarget(), false)) {
                        player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Stargates connected!");
                        wormholePlayer.getProperties().setHasActivatedStargate(true);
                        currentGate.setLastUsedBy(player.getName());
                        WXTLogger.prettyLog(Level.FINE, false, "Player '" + currentGate.getLastUsedBy() + "' has activated gate '" + wormholePlayer.getStargate().getGateName() + "'");
                        return wormholePlayer;
                    }
                    Stargate targetGate = StargateManager.getStargate(currentGate.getGateDialSignTarget().getGateName());
                    player.sendMessage(String.format(ConfigManager.MessageStrings.gateRemoteActive.toString(), targetGate.getGateName() + " ", " by " + StargateManager.getStargate(targetGate.getSourceGateName()).getLastUsedBy()));
                    player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Detected Wormhole source " + targetGate.getSourceGateName());
                    wormholePlayer.getProperties().setHasReceivedRemoteActiveMessage(true);
                    return wormholePlayer;
                }
                player.sendMessage(ConfigManager.MessageStrings.targetInvalid.toString());
                wormholePlayer.getProperties().setHasReceivedInvalidTargetMessage(true);
                return wormholePlayer;
            }
            player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            wormholePlayer.getProperties().setHasReceivedNoPermissionMessage(true);
            return wormholePlayer;
        }
        player.sendMessage(String.format(ConfigManager.MessageStrings.gateActivated.toString(), currentGate.getGateName() + " "));
        player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Chevrons locked! §3:: §B<required> §6[optional]");
        player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Type '§F/dial §B<GateName> §6[idc]§7'");
        StargateManager.addActivatedStargate(currentGate);
        currentGate.startActivationTimer(player);
        currentGate.lightStargate(true);
        wormholePlayer.getProperties().setHasActivatedStargate(true);
        currentGate.setLastUsedBy(player.getName());
        return wormholePlayer;
    }

    protected static boolean handlePlayerInteractEvent(PlayerInteractEvent event) {
        Stargate stargate;
        Block clickedBlock = event.getClickedBlock();
        Action clickedBlockAction = event.getAction();
        Player player = event.getPlayer();
        if (clickedBlock != null && (clickedBlock.getType().equals(Material.STONE_BUTTON) || clickedBlock.getType().equals(Material.LEVER))) {
            if (clickedBlock.getType().equals(Material.LEVER) && !WXPermissions.checkPermission(player, WXPermissions.PermissionType.USE)) {
                return false;
            }
            if ((!clickedBlock.getType().equals(Material.STONE_BUTTON) || WXPermissions.checkPermission(player, WXPermissions.PermissionType.BUILD)) && buttonLeverHit(player, clickedBlock, null)) {
                return true;
            }
            return false;
        }
        if (clickedBlock != null && clickedBlock.getType().equals(Material.OAK_WALL_SIGN) && (stargate = StargateManager.getGateFromBlock(clickedBlock)) != null) {
            if (!WXPermissions.checkPermission(player, stargate, WXPermissions.PermissionType.SIGN)) {
                player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
                return true;
            }
            stargate.setLastUsedBy(player.getName());
            if (stargate.tryClickTeleportSign(clickedBlock, clickedBlockAction)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean hasChangedBlockCoordinates(Location fromLoc, Location toLoc) {
        return (fromLoc.getWorld().equals(toLoc.getWorld()) && fromLoc.getBlockX() == toLoc.getBlockX() && fromLoc.getBlockY() == toLoc.getBlockY() && fromLoc.getBlockZ() == toLoc.getBlockZ()) ? false : true;
    }

    protected WormholePlayer handlePlayerMoveEvent(PlayerMoveEvent event) {
        String gateNetwork;
        Player player = event.getPlayer();
        Location toLocFinal = event.getTo();
        Block gateBlockFinal = toLocFinal.getWorld().getBlockAt(toLocFinal.getBlockX(), toLocFinal.getBlockY(), toLocFinal.getBlockZ());
        Stargate stargate = StargateManager.getGateFromBlock(gateBlockFinal);
        if (stargate != null && stargate.isGateActive() && stargate.getGateTarget() != null) {
            if (gateBlockFinal.getType() == (stargate.isGateCustom() ? stargate.getGateCustomPortalMaterial() : stargate.getGateShape() != null ? stargate.getGateShape().getShapePortalMaterial() : Material.WATER) && !WormholePlayerManager.getRegisteredWormholePlayer(player).getProperties(stargate).hasUsedStargate()) {
                WormholePlayer wormholePlayer = WormholePlayerManager.getRegisteredWormholePlayer(player);
                wormholePlayer.addStargate(stargate);
                wormholePlayer.setCurrentGateName(stargate.getGateName());
                if (stargate.getGateNetwork() != null) {
                    gateNetwork = stargate.getGateNetwork().getNetworkName();
                } else {
                    gateNetwork = "Public";
                }
                WXTLogger.prettyLog(Level.FINE, false, "Player in gate: " + stargate.getGateName() + " gate Active: " + stargate.isGateActive() + " Target Gate: " + stargate.getGateTarget().getGateName() + " Network: " + gateNetwork);
                if (!wormholePlayer.getProperties().hasReceivedIrisLockMessage()) {
                    if (ConfigManager.getWormholeUseIsTeleport() && ((stargate.isGateSignPowered() && !WXPermissions.checkPermission(player, stargate, WXPermissions.PermissionType.SIGN)) || (!stargate.isGateSignPowered() && !WXPermissions.checkPermission(player, stargate, WXPermissions.PermissionType.DIALER)))) {
                        player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
                        wormholePlayer.getProperties().setHasPermission(false);
                        return wormholePlayer;
                    }
                    if (ConfigManager.isUseCooldownEnabled()) {
                        if (StargateRestrictions.isPlayerUseCooldown(player)) {
                            player.sendMessage(ConfigManager.MessageStrings.playerUseCooldownRestricted.toString());
                            player.sendMessage(ConfigManager.MessageStrings.playerUseCooldownWaitTime.toString() + StargateRestrictions.checkPlayerUseCooldownRemaining(player));
                            wormholePlayer.getProperties().setHasUseCooldown(true);
                            return wormholePlayer;
                        }
                        StargateRestrictions.addPlayerUseCooldown(player);
                    }
                    if (stargate.getGateTarget().isGateIrisActive() && !wormholePlayer.getProperties().hasReceivedIrisLockMessage()) {
                        player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Remote Iris is locked!");
                        wormholePlayer.getProperties().setHasReceivedIrisLockMessage(true);
                        int wkbCount = ConfigManager.getWormholeKickbackBlockCount();
                        if (wkbCount > 0) {
                            player.setNoDamageTicks(5);
                            PlayerOrientation direction = wormholePlayer.getKickBackDirection(wormholePlayer.getStargate().getGateFacing().getOppositeFace());
                            double pLocX = wormholePlayer.getPlayer().getLocation().getX();
                            double pLocY = wormholePlayer.getPlayer().getLocation().getY();
                            double pLocZ = wormholePlayer.getPlayer().getLocation().getZ();
                            WXTLogger.prettyLog(Level.FINE, false, "PlayerOrientation: " + direction.getName());
                            WXTLogger.prettyLog(Level.FINE, false, "old X:" + pLocX + ", Y:" + pLocY + ", Z:" + pLocZ);
                            switch (direction) {
                                case NORTH:
                                    WXTLogger.prettyLog(Level.FINE, false, "NORTH: " + pLocZ + " - 2 = " + (pLocX - 2.0d));
                                    pLocZ -= (double) wkbCount;
                                    break;
                                case SOUTH:
                                    WXTLogger.prettyLog(Level.FINE, false, "SOUTH: " + pLocZ + " + 2 = " + (pLocX + 2.0d));
                                    pLocZ += (double) wkbCount;
                                    break;
                                case EAST:
                                    WXTLogger.prettyLog(Level.FINE, false, "EAST: " + pLocX + " + 2 = " + (pLocZ - 2.0d));
                                    pLocX += (double) wkbCount;
                                    break;
                                case WEST:
                                    WXTLogger.prettyLog(Level.FINE, false, "WEST: " + pLocX + " - 2 = " + (pLocZ + 2.0d));
                                    pLocX -= (double) wkbCount;
                                    break;
                            }
                            Location newLoc = new Location(player.getWorld(), pLocX, pLocY, pLocZ, player.getLocation().getYaw(), player.getLocation().getPitch());
                            double pLocY2 = player.getWorld().getHighestBlockYAt(newLoc);
                            if (ConfigManager.getGateTransportMethod()) {
                                event.setTo(newLoc);
                                WXTLogger.prettyLog(Level.FINE, false, "Player was kicked back via event");
                            } else {
                                player.teleport(newLoc);
                                WXTLogger.prettyLog(Level.FINE, false, "Player was kicked back via teleport");
                            }
                            WXTLogger.prettyLog(Level.FINE, false, "new X:" + pLocX + ", Y:" + pLocY2 + ", Z:" + pLocZ);
                        }
                        return wormholePlayer;
                    }
                    Location target = stargate.getGateTarget().getGatePlayerTeleportLocation();
                    player.setNoDamageTicks(5);
                    if (ConfigManager.getGateTransportMethod()) {
                        event.setTo(target);
                        WXTLogger.prettyLog(Level.FINE, false, "Player was transported via event");
                    } else {
                        player.teleport(target);
                        WXTLogger.prettyLog(Level.FINE, false, "Player was transported via teleport");
                    }
                    if (target != stargate.getGatePlayerTeleportLocation() && !wormholePlayer.getProperties().hasUsedStargate()) {
                        WXTLogger.prettyLog(Level.INFO, false, player.getName() + " used wormhole: " + stargate.getGateName() + " to go to: " + stargate.getGateTarget().getGateName());
                        wormholePlayer.getProperties().setHasUsedStargate(true);
                    }
                    if (ConfigManager.getTimeoutShutdown() == 0) {
                        stargate.shutdownStargate(true);
                    }
                } else {
                    WXTLogger.prettyLog(Level.FINE, false, "Player '" + player.getName() + "' has received IRISLOCK_MESSASGE unlocking player.");
                    wormholePlayer.removeStargate(stargate);
                }
                return wormholePlayer;
            }
        }
        if (stargate != null && WormholePlayerManager.getRegisteredWormholePlayer(player).getProperties(stargate).hasReachedDestination()) {
            WXTLogger.prettyLog(Level.FINE, false, "Player '" + player.getName() + "' has safely reached destination.");
            WormholePlayerManager.getRegisteredWormholePlayer(player).removeStargate(stargate);
            return null;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!event.isCancelled()) {
            Stargate stargate = StargateManager.getGateFromBlock(event.getBlockClicked());
            if (stargate != null || StargateManager.isBlockInGate(event.getBlockClicked())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (!event.isCancelled()) {
            Stargate stargate = StargateManager.getGateFromBlock(event.getBlockClicked());
            if (stargate != null || StargateManager.isBlockInGate(event.getBlockClicked())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            WXTLogger.prettyLog(Level.FINE, false, "InteractEvent was canceled");
            return;
        }
        if (event.getClickedBlock() != null) {
            WXTLogger.prettyLog(Level.FINE, false, "Caught Player: \"" + event.getPlayer().getName() + "\" Event Name: \"" + event.getEventName() + "\" Action Type: \"" + event.getAction().toString() + "\" Event Block Type: \"" + event.getClickedBlock().getType().toString() + "\" Event World: \"" + event.getClickedBlock().getWorld().toString() + "\" Event Block: " + event.getClickedBlock().toString() + "\"");
            if (handlePlayerInteractEvent(event)) {
                event.setCancelled(true);
                WXTLogger.prettyLog(Level.FINE, false, "Cancelled Player: \"" + event.getPlayer().getName() + "\" Event Name: \"" + event.getEventName() + "\" Action Type: \"" + event.getAction().toString() + "\" Event Block Type: \"" + event.getClickedBlock().getType().toString() + "\" Event World: \"" + event.getClickedBlock().getWorld().toString() + "\" Event Block: " + event.getClickedBlock().toString() + "\"");
                return;
            }
            return;
        }
        WXTLogger.prettyLog(Level.FINE, false, "Caught and ignored Player: \"" + event.getPlayer().getName() + "\" Event type: \"" + event.getEventName() + "\"");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!hasChangedBlockCoordinates(event.getFrom(), event.getTo())) {
            return;
        }
        WormholePlayer wormholePlayer = handlePlayerMoveEvent(event);
        Player player = event.getPlayer();
        if (WormholePlayerManager.isRegistered(player) && wormholePlayer != null && wormholePlayer.getProperties() != null && wormholePlayer.getProperties().hasUsedStargate()) {
            if (ConfigManager.isGateArrivalWelcomeMessageEnabled()) {
                player.sendMessage(String.format(ConfigManager.MessageStrings.playerUsedStargate.toString(), "Gate " + wormholePlayer.getStargate().getGateTarget().getGateName(), " - created by " + wormholePlayer.getStargate().getGateTarget().getGateOwner()));
                WXTLogger.prettyLog(Level.FINE, false, "has received SHOW_GATE_WELCOME_MESSAGE");
            } else {
                WXTLogger.prettyLog(Level.FINE, false, "has disabled SHOW_GATE_WELCOME_MESSAGE");
            }
            wormholePlayer.getProperties().setHasReachedDestination(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        WXTLogger.prettyLog(Level.FINE, false, "Player '" + player.getName() + "' joined the server. Adding player to keyring.");
        WormholePlayerManager.registerPlayer(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        WXTLogger.prettyLog(Level.FINE, false, "Player '" + player.getName() + "' has quit. Removing player from keyring.");
        WormholePlayerManager.unregisterPlayer(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        WXTLogger.prettyLog(Level.FINE, false, "Player '" + player.getName() + "' was kicked. Removing player from keyring.");
        WormholePlayerManager.unregisterPlayer(player);
    }
}
