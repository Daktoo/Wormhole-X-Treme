package de.luricos.bukkit.WormholeXTreme.Wormhole.economy;

import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private static final String PRICES_FILE = "plugins" + File.separator
            + "WormholeXTreme" + File.separator + "GateShapePrices.txt";

    private static final String[] ECONOMY_PLUGINS = {
        "Vault", "iConomy", "Essentials", "BOSEconomy", "MineConomy"
    };

    private static boolean economyEnabled = false;
    private static String detectedPlugin = null;

    private static Object vaultEconomy = null;
    private static Method vaultHas = null;
    private static Method vaultWithdraw = null;
    private static Method vaultIsSuccess = null;

    private static final Map<String, Double> shapePrices = new LinkedHashMap<>();

    public static void initialise(Collection<String> knownShapeNames) {
        economyEnabled = false;
        detectedPlugin = null;
        vaultEconomy = null;
        vaultHas = null;
        vaultWithdraw = null;
        vaultIsSuccess = null;

        if (tryHookVault()) {
            detectedPlugin = "Vault";
            economyEnabled = true;
            WXTLogger.prettyLog(Level.INFO, false, "[Economy] Hooked into Vault economy API.");
        } else {
            for (String name : ECONOMY_PLUGINS) {
                if (name.equals("Vault")) continue;
                Plugin p = Bukkit.getPluginManager().getPlugin(name);
                if (p != null && p.isEnabled()) {
                    detectedPlugin = name;
                    economyEnabled = true;
                    WXTLogger.prettyLog(Level.INFO, false,
                            "[Economy] Hooked into economy plugin: '" + name + "' (command mode).");
                    break;
                }
            }
        }

        if (!economyEnabled) {
            WXTLogger.prettyLog(Level.INFO, false,
                    "[Economy] No economy plugin found - economy features disabled.");
        }

        loadShapePrices(knownShapeNames);
    }

    private static boolean tryHookVault() {
        Plugin vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");
        if (vaultPlugin == null || !vaultPlugin.isEnabled()) {
            return false;
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            @SuppressWarnings("unchecked")
            RegisteredServiceProvider<?> rsp =
                    Bukkit.getServicesManager().getRegistration(
                            (Class<Object>) economyClass);
            if (rsp == null) {
                WXTLogger.prettyLog(Level.FINE, false,
                        "[Economy] Vault present but no Economy provider registered.");
                return false;
            }
            vaultEconomy = rsp.getProvider();
            vaultHas = economyClass.getMethod("has", Player.class, double.class);
            vaultWithdraw = economyClass.getMethod("withdrawPlayer", Player.class, double.class);
            Class<?> responseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
            vaultIsSuccess = responseClass.getMethod("transactionSuccess");
            return true;
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.FINE, false,
                    "[Economy] Vault hook failed: " + e.getMessage());
            return false;
        }
    }

    public static void loadShapePrices(Collection<String> knownShapeNames) {
        shapePrices.clear();
        File file = new File(PRICES_FILE);

        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        try {
                            double price = Double.parseDouble(parts[1].trim());
                            shapePrices.put(parts[0].trim().toLowerCase(), price);
                        } catch (NumberFormatException e) {
                            WXTLogger.prettyLog(Level.WARNING, false,
                                    "[Economy] Invalid price for shape '"
                                    + parts[0].trim() + "': " + parts[1].trim());
                        }
                    }
                }
            } catch (IOException e) {
                WXTLogger.prettyLog(Level.WARNING, false,
                        "[Economy] Failed to read GateShapePrices.txt: " + e.getMessage());
            }
        }

        boolean dirty = !file.exists();
        for (String shapeName : knownShapeNames) {
            if (!shapePrices.containsKey(shapeName.toLowerCase())) {
                shapePrices.put(shapeName.toLowerCase(), 0.0);
                WXTLogger.prettyLog(Level.INFO, false,
                        "[Economy] New shape '" + shapeName
                        + "' added to price config with default price 0.");
                dirty = true;
            }
        }

        if (dirty) {
            saveShapePrices();
        }

        WXTLogger.prettyLog(Level.INFO, false,
                "[Economy] Loaded " + shapePrices.size()
                + " shape price(s) from GateShapePrices.txt.");
    }

    public static void saveShapePrices() {
        File file = new File(PRICES_FILE);
        try {
            file.getParentFile().mkdirs();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write("# WormholeXTreme Gate Shape Build Prices");
                bw.newLine();
                bw.write("# Format: ShapeName = price");
                bw.newLine();
                bw.write("# Price is deducted from the player's balance when /wxcomplete is run.");
                bw.newLine();
                bw.write("# Set to 0 to make a shape free to build.");
                bw.newLine();
                bw.write("# Economy must be enabled (ECONOMY_ENABLED = true) in Settings.txt.");
                bw.newLine();
                bw.newLine();
                for (Map.Entry<String, Double> entry : shapePrices.entrySet()) {
                    bw.write(entry.getKey() + " = " + entry.getValue());
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            WXTLogger.prettyLog(Level.WARNING, false,
                    "[Economy] Failed to write GateShapePrices.txt: " + e.getMessage());
        }
    }

    public static boolean isEconomyEnabled() {
        return economyEnabled;
    }

    public static String getDetectedPlugin() {
        return detectedPlugin;
    }

    public static double getPriceForShape(String shapeName) {
        if (shapeName == null) return 0.0;
        Double price = shapePrices.get(shapeName.toLowerCase());
        return price != null ? price : 0.0;
    }

    public static boolean canAffordAndCharge(Player player, String shapeName) {
        if (!economyEnabled) return true;

        double price = getPriceForShape(shapeName);
        if (price <= 0.0) return true;

        WXTLogger.prettyLog(Level.INFO, false,
                "[Economy] Charging " + player.getName() + " " + price
                + " for shape '" + shapeName + "' via " + detectedPlugin + ".");

        if (vaultEconomy != null && vaultHas != null && vaultWithdraw != null) {
            return chargeViaVaultApi(player, shapeName, price);
        }

        return chargeViaCommand(player, shapeName, price);
    }

    private static boolean chargeViaVaultApi(Player player, String shapeName, double price) {
        try {
            boolean hasEnough = (boolean) vaultHas.invoke(vaultEconomy, player, price);
            if (!hasEnough) {
                player.sendMessage(
                        "§3:: §5error §3:: §7You do not have enough money to build this stargate. "
                        + "Cost: " + price);
                WXTLogger.prettyLog(Level.INFO, false,
                        "[Economy] " + player.getName() + " cannot afford " + price
                        + " for '" + shapeName + "'.");
                return false;
            }
            Object response = vaultWithdraw.invoke(vaultEconomy, player, price);
            boolean success = (boolean) vaultIsSuccess.invoke(response);
            if (success) {
                player.sendMessage(
                        "§3:: §7" + price + " has been deducted from your balance "
                        + "for building a " + shapeName + " stargate.");
                WXTLogger.prettyLog(Level.INFO, false,
                        "[Economy] Vault: charged " + player.getName() + " "
                        + price + " for '" + shapeName + "'.");
                return true;
            }
            player.sendMessage(
                    "§3:: §5error §3:: §7Economy transaction failed. Please contact an admin.");
            WXTLogger.prettyLog(Level.WARNING, false,
                    "[Economy] Vault withdrawal failed for " + player.getName()
                    + " amount=" + price);
            return false;
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.WARNING, false,
                    "[Economy] Vault API error for " + player.getName() + ": " + e.getMessage());
            return true;
        }
    }

    private static boolean chargeViaCommand(Player player, String shapeName, double price) {
        String priceStr = price == Math.floor(price)
                ? String.valueOf((long) price)
                : String.valueOf(price);
        String cmd = "eco take " + player.getName() + " " + priceStr;
        try {
            boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            if (dispatched) {
                player.sendMessage(
                        "§3:: §7" + price + " has been deducted from your balance "
                        + "for building a " + shapeName + " stargate.");
                WXTLogger.prettyLog(Level.INFO, false,
                        "[Economy] Command '" + cmd + "' executed for " + player.getName() + ".");
                return true;
            }
            WXTLogger.prettyLog(Level.WARNING, false,
                    "[Economy] Command '" + cmd + "' returned false. "
                    + "Economy plugin may not support /eco take.");
            return false;
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.WARNING, false,
                    "[Economy] Exception dispatching '" + cmd + "': " + e.getMessage());
            return true;
        }
    }
}