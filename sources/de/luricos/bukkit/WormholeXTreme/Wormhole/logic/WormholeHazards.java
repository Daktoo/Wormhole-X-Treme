package de.luricos.bukkit.WormholeXTreme.Wormhole.logic;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.BoundingBox;
public class WormholeHazards implements Listener {
    private static final double IRIS_IMPACT_HEARING_RADIUS = 16.0d;
    private static final Map<UUID, String> pendingDeathMessages = new HashMap<>();
    public static void strikeWithKawoosh(Stargate gate, Collection<Block> wooshBlocks) {
        if (!ConfigManager.isDangerousKawooshEnabled() || gate == null || wooshBlocks == null || wooshBlocks.isEmpty()) {
            return;
        }
        double damage = ConfigManager.getKawooshDamage();
        if (damage <= 0.0d) {
            return;
        }
        World world = gate.getGateWorld();
        if (world == null) {
            return;
        }
        List<BoundingBox> blockBoxes = new ArrayList<>(wooshBlocks.size());
        BoundingBox area = null;
        for (Block block : wooshBlocks) {
            if (block == null) {
                continue;
            }
            BoundingBox box = BoundingBox.of(block);
            blockBoxes.add(box);
            area = (area == null) ? box.clone() : area.union(box);
        }
        if (area == null) {
            return;
        }
        Set<UUID> alreadyStruck = gate.getKawooshVictims();
        for (Entity entity : world.getNearbyEntities(area, e -> e instanceof LivingEntity)) {
            LivingEntity victim = (LivingEntity) entity;
            if (alreadyStruck.contains(victim.getUniqueId()) || !isKawooshTarget(victim)) {
                continue;
            }
            if (!overlapsAny(victim.getBoundingBox(), blockBoxes)) {
                continue;
            }
            if (victim.getNoDamageTicks() > victim.getMaximumNoDamageTicks() / 2) {
                continue;
            }
            alreadyStruck.add(victim.getUniqueId());
            if (victim instanceof Player) {
                ((Player) victim).sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "You were caught in an unstable vortex!");
            }
            String deathMessage = victim instanceof Player
                ? victim.getName() + " was caught in an unstable vortex" /* + " of gate " + gate.getGateName() <- add this if you want gates to show in death message */
                : null;
            damage(victim, damage, deathMessage);
            WXTLogger.prettyLog(Level.FINE, false, "Kawoosh of gate '" + gate.getGateName() + "' struck " + describe(victim) + " for " + damage + " damage.");
        }
    }
    private static boolean isKawooshTarget(LivingEntity entity) {
        if (entity.isDead() || !entity.isValid() || entity.isInvulnerable() || entity instanceof ArmorStand) {
            return false;
        }
        if (entity instanceof Player) {
            GameMode mode = ((Player) entity).getGameMode();
            return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
        }
        return true;
    }
    private static boolean overlapsAny(BoundingBox box, List<BoundingBox> boxes) {
        for (BoundingBox other : boxes) {
            if (box.overlaps(other)) {
                return true;
            }
        }
        return false;
    }
    public static boolean shouldIrisKill(Player player) {
        if (!ConfigManager.isDeadlyIrisEnabled() || player == null || player.isDead()) {
            return false;
        }
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }
    public static void killAtIris(Player player, Stargate sourceGate) {
        Stargate target = sourceGate.getGateTarget();
        String targetName = target != null ? target.getGateName() : sourceGate.getGateName();
        player.sendMessage(ConfigManager.MessageStrings.errorHeader.toString() + "You were not reintegrated due to the remote iris being closed.");
        notifyIrisImpact(target, player);
        kill(player, player.getName() + " was killed upon hitting an iris");
        WXTLogger.prettyLog(Level.INFO, false, player.getName() + " was killed by the closed iris of '" + targetName + "', travelling from '" + sourceGate.getGateName() + "'.");
    }
    public static boolean killPassengersAtIris(Entity vehicle, Stargate sourceGate) {
        if (!ConfigManager.isDeadlyIrisEnabled() || vehicle == null) {
            return false;
        }
        boolean killedAny = false;
        for (Entity passenger : new ArrayList<>(vehicle.getPassengers())) {
            if (passenger instanceof Player) {
                Player player = (Player) passenger;
                if (shouldIrisKill(player)) {
                    killAtIris(player, sourceGate);
                    killedAny = true;
                }
            } else if (passenger instanceof LivingEntity) {
                if (killCreatureAtIris((LivingEntity) passenger, sourceGate)) {
                    killedAny = true;
                }
            }
        }
        return killedAny;
    }
    public static boolean destroyAtIris(Entity entity, Stargate sourceGate) {
        if (!ConfigManager.isDeadlyIrisEnabled() || entity == null || entity.isDead()) {
            return false;
        }
        notifyIrisImpact(sourceGate.getGateTarget(), null);
        entity.remove();
        WXTLogger.prettyLog(Level.FINE, false, entity.getType() + " was destroyed by the closed iris of '" + (sourceGate.getGateTarget() != null ? sourceGate.getGateTarget().getGateName() : "?") + "'.");
        return true;
    }
    public static boolean killCreatureAtIris(LivingEntity creature, Stargate sourceGate) {
        if (!ConfigManager.isDeadlyIrisEnabled() || creature == null || creature instanceof Player || creature instanceof ArmorStand) {
            return false;
        }
        if (creature.isDead() || creature.isInvulnerable()) {
            return false;
        }
        notifyIrisImpact(sourceGate.getGateTarget(), null);
        creature.setHealth(0.0d);
        WXTLogger.prettyLog(Level.FINE, false, describe(creature) + " was killed by the closed iris of '" + (sourceGate.getGateTarget() != null ? sourceGate.getGateTarget().getGateName() : "?") + "'.");
        return true;
    }

    private static void notifyIrisImpact(Stargate target, Player traveller) {
        if (target == null || target.getGatePlayerTeleportLocation() == null) {
            return;
        }
        Location irisLocation = target.getGatePlayerTeleportLocation();
        World world = irisLocation.getWorld();
        if (world == null) {
            return;
        }
        double radiusSquared = IRIS_IMPACT_HEARING_RADIUS * IRIS_IMPACT_HEARING_RADIUS;
        for (Player listener : world.getPlayers()) {
            if (listener.equals(traveller)) {
                continue;
            }
            if (listener.getLocation().distanceSquared(irisLocation) <= radiusSquared) {
                listener.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Something struck the iris of gate " + target.getGateName() + ".");
            }
        }
    }
    private static void damage(LivingEntity victim, double amount, String deathMessage) {
        if (deathMessage == null) {
            victim.damage(amount);
            return;
        }
        pendingDeathMessages.put(victim.getUniqueId(), deathMessage);
        try {
            victim.damage(amount);
        } finally {
            pendingDeathMessages.remove(victim.getUniqueId());
        }
    }
    private static void kill(Player player, String deathMessage) {
        pendingDeathMessages.put(player.getUniqueId(), deathMessage);
        try {
            player.setHealth(0.0d);
        } finally {
            pendingDeathMessages.remove(player.getUniqueId());
        }
    }
    private static String describe(LivingEntity entity) {
        return entity instanceof Player ? "player '" + entity.getName() + "'" : entity.getType().toString();
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        String message = pendingDeathMessages.get(event.getEntity().getUniqueId());
        if (message != null) {
            event.setDeathMessage(message);
        }
    }
}