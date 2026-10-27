package de.luricos.bukkit.WormholeXTreme.listeners;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.BonusGates;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.DestinyController;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.GateKind;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.WXPermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
public class DestinyControllerListener implements Listener {
    private static final long COOLDOWN_MS = 500L;
    private final Map<UUID, Long> lastUse = new HashMap<>();
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() == Action.PHYSICAL) {
            return;
        }
        ItemStack item = event.getItem();
        if (!DestinyController.isController(item)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long last = this.lastUse.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) {
            return;
        }
        this.lastUse.put(player.getUniqueId(), now);
        if (!BonusGates.isEnabled()) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + BonusGates.DISABLED_MESSAGE);
            return;
        }
        String gateName = DestinyController.getLinkedGate(item);
        if (gateName == null) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "Controller is not linked.");
            return;
        }
        Stargate gate = StargateManager.getStargate(gateName);
        if (gate == null || !GateKind.isDestiny(gate)) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "The linked gate '" + gateName + "' no longer exists.");
            return;
        }
        if (!DestinyController.isInRange(player, gate)) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "You need to be within " + (int) DestinyController.RANGE + " blocks of " + gate.getGateName() + ".");
            return;
        }
        if (!WXPermissions.checkPermission(player, gate, WXPermissions.PermissionType.DIALER)) {
            player.sendMessage(ConfigManager.MessageStrings.permissionNo.toString());
            return;
        }
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            WormholeXTremePlayerListener.toggleGateFromController(player, gate);
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            toggleIris(player, gate);
        }
    }
    private static void toggleIris(Player player, Stargate gate) {
        String idc = gate.getGateIrisDeactivationCode();
        if (idc == null || idc.isEmpty()) {
            player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + gate.getGateName() + " has no iris.");
            return;
        }
        gate.toggleIrisActive(true);
        player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Iris of " + gate.getGateName() + (gate.isGateIrisActive() ? " closed." : " opened."));
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.lastUse.remove(event.getPlayer().getUniqueId());
    }
}