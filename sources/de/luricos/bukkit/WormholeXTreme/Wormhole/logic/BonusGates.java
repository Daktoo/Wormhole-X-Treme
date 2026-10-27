package de.luricos.bukkit.WormholeXTreme.Wormhole.logic;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.GateKind;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateShape;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
public final class BonusGates {
    public static final String DISABLED_MESSAGE = "Pegasus and Destiny gates are disabled on this server.";
    private BonusGates() {
    }
    public static boolean isEnabled() {
        return ConfigManager.isBonusGatesEnabled();
    }
    public static boolean isUsable(Stargate gate) {
        return !GateKind.of(gate).isBonus() || isEnabled();
    }
    static void registerShapes(Map<String, StargateShape> shapes) {
        boolean enabled = isEnabled();
        register(shapes, BuiltInShapes.PEGASUS, BuiltInShapes.pegasus(), enabled);
        register(shapes, BuiltInShapes.DESTINY, BuiltInShapes.destiny(), enabled);
        WXTLogger.prettyLog(Level.INFO, false, "Pegasus and Destiny gates are " + (enabled ? "enabled." : "disabled. Enable them with /wxt bonusgates true."));
    }
    private static void register(Map<String, StargateShape> shapes, String name, String[] lines, boolean enabled) {
        try {
            StargateShape shape = StargateShapeFactory.createShapeFromFile(lines);
            shape.setShapeEnabled(enabled);
            if (shapes.containsKey(name.toLowerCase())) {
                WXTLogger.prettyLog(Level.WARNING, false, "A shape file named " + name + " was ignored: " + name + " is a built-in gate type. Rename the file to use it.");
            }
            shapes.put(name.toLowerCase(), shape);
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Could not register the built-in " + name + " shape: " + e.getMessage());
        }
    }
    public static void onGateCompleted(Player player, Stargate gate) {
        GateKind kind = GateKind.of(gate);
        if (kind == GateKind.DESTINY) {
            Block buildSwitch = gate.getGateDialLeverBlock();
            if (buildSwitch != null && (buildSwitch.getType() == Material.LEVER || Tag.BUTTONS.isTagged(buildSwitch.getType()))) {
                buildSwitch.setType(Material.AIR);
            }
        } else if (kind == GateKind.PEGASUS && player != null) {
            player.sendMessage(ConfigManager.MessageStrings.normalHeader.toString() + "Pegasus gate linked.");
        }
    }
}