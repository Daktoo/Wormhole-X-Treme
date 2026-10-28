package de.luricos.bukkit.WormholeXTreme.Wormhole.logic;
import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.GateKind;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateShape;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.Collections;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
public final class DestinyController {
    public static final double RANGE = 10.0d;
    public static final String NAME = "Destiny DHD Controller";
    private static final String LORE_PREFIX = "Connected gate: ";
    private static final String NOT_LINKED = "None";
    private DestinyController() {
    }
    private static NamespacedKey key(String name) {
        return new NamespacedKey(WormholeXTreme.getThisPlugin(), name);
    }
    private static NamespacedKey markerKey() {
        return key("destiny_controller");
    }
    private static NamespacedKey linkKey() {
        return key("linked_gate");
    }
    private static NamespacedKey recipeKey() {
        return key("destiny_dhd_controller");
    }
    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.RESET.toString() + ChatColor.AQUA + NAME);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + LORE_PREFIX + NOT_LINKED));
        Enchantment glint = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (glint != null) {
            meta.addEnchant(glint, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(markerKey(), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
    public static boolean isController(ItemStack item) {
        if (item == null || (item.getType() != Material.COMPASS && item.getType() != Material.RECOVERY_COMPASS) || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(markerKey(), PersistentDataType.BYTE);
    }
    public static String getLinkedGate(ItemStack item) {
        if (!isController(item)) {
            return null;
        }
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        return data.get(linkKey(), PersistentDataType.STRING);
    }
    public static ItemStack setLinkedGate(ItemStack item, String gateName) {
        if (!isController(item)) {
            return item;
        }
        if (item.getType() != Material.COMPASS) {
            ItemStack compass = new ItemStack(Material.COMPASS, item.getAmount());
            compass.setItemMeta(Bukkit.getItemFactory().asMetaFor(item.getItemMeta(), Material.COMPASS));
            item = compass;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(linkKey(), PersistentDataType.STRING, gateName);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + LORE_PREFIX + gateName));
        Stargate gate = StargateManager.getStargate(gateName);
        if (meta instanceof CompassMeta && gate != null && gate.getGatePlayerTeleportLocation() != null) {
            CompassMeta compass = (CompassMeta) meta;
            compass.setLodestone(gate.getGatePlayerTeleportLocation());
            compass.setLodestoneTracked(false);
        }
        item.setItemMeta(meta);
        return item;
    }
    public static boolean isInRange(Player player, Stargate gate) {
        Location centre = gate.getGatePlayerTeleportLocation();
        Location here = player.getLocation();
        if (centre == null || centre.getWorld() == null || here.getWorld() == null || !centre.getWorld().equals(here.getWorld())) {
            return false;
        }
        return here.distanceSquared(centre) <= RANGE * RANGE;
    }
    private static final BlockFace[] FACINGS = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    public static Stargate findUnbuiltGate(Player player) {
        Stargate pending = StargateManager.getIncompleteStargate(player);
        if (GateKind.isDestiny(pending) && isInRange(player, pending)) {
            return pending;
        }
        StargateShape shape = StargateHelper.getStargateShape(BuiltInShapes.DESTINY);
        Location here = player.getLocation();
        World world = here.getWorld();
        if (shape == null || world == null) {
            return null;
        }
        Material frame = shape.getShapeStructureMaterial();
        int reach = (int) Math.ceil(RANGE) + 4;
        int minY = Math.max(world.getMinHeight(), here.getBlockY() - reach);
        int maxY = Math.min(world.getMaxHeight() - 1, here.getBlockY() + reach);
        Stargate best = null;
        boolean bestFacesPlayer = false;
        double bestDistance = Double.MAX_VALUE;
        for (int x = here.getBlockX() - reach; x <= here.getBlockX() + reach; x++) {
            for (int z = here.getBlockZ() - reach; z <= here.getBlockZ() + reach; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != frame || StargateManager.isBlockInGate(block)) {
                        continue;
                    }
                    for (BlockFace facing : FACINGS) {
                        Stargate found = StargateHelper.findStargate(block.getRelative(facing), facing, shape);
                        if (found == null || !isInRange(player, found)) {
                            continue;
                        }
                        boolean facesPlayer = (here.getX() - (x + 0.5d)) * facing.getModX() + (here.getZ() - (z + 0.5d)) * facing.getModZ() > 0.0d;
                        double distance = here.distanceSquared(found.getGatePlayerTeleportLocation());
                        if (best == null || (facesPlayer && !bestFacesPlayer) || (facesPlayer == bestFacesPlayer && distance < bestDistance)) {
                            best = found;
                            bestFacesPlayer = facesPlayer;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }
        return best;
    }
    public static void syncRecipe() {
        NamespacedKey recipeKey = recipeKey();
        Recipe existing = Bukkit.getRecipe(recipeKey);
        if (!BonusGates.isEnabled()) {
            if (existing != null) {
                Bukkit.removeRecipe(recipeKey);
            }
            return;
        }
        if (existing != null) {
            if (existing.getResult().getType() == Material.COMPASS) {
                return;
            }
            Bukkit.removeRecipe(recipeKey);
        }
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, create());
        recipe.shape("GPG", "RER", "GOG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('P', Material.GLASS_PANE);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('O', Material.OBSIDIAN);
        try {
            Bukkit.addRecipe(recipe);
        } catch (IllegalStateException e) {
            WXTLogger.prettyLog(Level.WARNING, false, "Could not add the Destiny DHD Controller recipe: " + e.getMessage());
        }
    }
}