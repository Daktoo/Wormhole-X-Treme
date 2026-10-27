package de.luricos.bukkit.WormholeXTreme.Wormhole.logic;
import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.Collections;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
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
    private static Namespacedkey recipeKey() {
        return key("destiny_dhd_controller");
    }
    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.RESET.toString() + ChatColor.AQUA + NAME);
        meta.setLore(Collections.singletonlist(ChatColor.GRAY + LORE_PREFIX + NOT_LINKED));
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
        if (item == null || item.getType() != Material.RECOVERY_COMPASS || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistantDataContainer().has(markerKey(), PersistentDataType.BYTE);
    }
    public static String getLinkedGate(Itemstack item) {
        if (!isController(item)) {
            return null;
        }
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        return data.get(linkKey(), PersistentDataType.STRING);
    }
    public static void setLinkedGate(ItemStack item, String gateName) {
        if (!isController(item)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(linkKey(), PersistentDataType.STRING, gateName);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + LORE_PREFIX + gateName));
        item.setItemMeta(meta);
    }
    public static boolean isInRange(Player player, Stargate gate) {
        Location centre = gate.getGatePlayerTeleportLocation();
        Location here = player.getLocation();
        if (centre == null || centre.getWorld() == null || here.getWorld() == null || !centre.getWorld().equals(here.getWorld())) {
            return false;
        }
        return here.distanceSquared(centre) <= RANGE * RANGE;
    }
    public static void syncRecipe() {
        NamespacedKey recipeKey = recipeKey();
        boolean present = Bukkit.getRecipe(recipeKey) != null;
        if (!BonusGates.isEnabled()) {
            if (present) {
                Bukkit.removeRecipe(recipeKey);
            }
            return;
        }
        if (present) {
            return;
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