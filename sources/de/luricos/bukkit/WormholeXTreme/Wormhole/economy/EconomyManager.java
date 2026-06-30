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

public class EconomyManager {

    private static final String PRICES_FILE = "plugins" + File.separator
            + "WormholeXTreme" + File.separator + "GateShapePrices.txt";

    private static final String[] ECONOMY_PLUGINS = {
        "Vault", "iConomy", "Essentials", "BOSEconomy", "MineConomy", "EconomyPlus"
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
            Method getRegistration = Bukkit.getServicesManager().getClass()
                    .getMethod("getRegistration", Class.class);
            Object rsp = getRegistration.invoke(Bukkit.getServicesManager(), economyClass);
            if (rsp == null) {
                WXTLogger.prettyLog(Level.FINE, false,
                        "[Economy] Vault present but no Economy provider registered.");
                return false;
            }
            Method getProvider = rsp.getClass().getMethod("getProvider");
            vaultEconomy = getProvider.invoke(rsp);
            vaultHas = economyClass.getMethod("has", Player.class, double.class);
            vaultWithdraw = economyClass.getMethod("withdrawPlayer", Player.class, double.class);
            Class<?> responseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
            vaultIsSuccess = responseClass.getMethod("transactionSuccess");
            WXTLogger.prettyLog(Level.INFO, false,
                    "[Economy] Vault Economy provider: "
                    + vaultEconomy.getClass().getSimpleName());
            return true;
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.WARNING, false,
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

        ChargeResult staticResult = chargeViaEconomyStaticApi(player, shapeName, price);
        if (staticResult != ChargeResult.API_UNAVAILABLE) {
            return staticResult == ChargeResult.SUCCESS;
        }

        if (vaultEconomy != null && vaultHas != null && vaultWithdraw != null) {
            return chargeViaVaultApi(player, shapeName, price);
        }

        if (chargeViaPluginApi(player, shapeName, price)) {
            return true;
        }

        WXTLogger.prettyLog(Level.WARNING, false,
                "[Economy] No working economy API hook was available for "
                + detectedPlugin + "; build charge cancelled.");
        player.sendMessage("§3:: §5error §3:: §7Economy transaction failed. Please contact an admin.");
        return false;
    }

    private enum ChargeResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        API_UNAVAILABLE
    }

    private static boolean chargeViaPluginApi(Player player, String shapeName, double price) {
        if (detectedPlugin == null) {
            return false;
        }

        String pluginName = detectedPlugin.toLowerCase();
        if (pluginName.contains("essentials")) {
            return chargeViaEconomyPluginApi(player, shapeName, price);
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin(detectedPlugin);
        return chargeViaGenericEconomyApi(player, shapeName, price, plugin);
    }

    private static boolean chargeViaGenericEconomyApi(Player player, String shapeName, double price, Plugin plugin) {
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }

        Object user = getEconomyUser(plugin, player);
        if (user != null && chargeViaEconomyUser(player, shapeName, price, user, plugin.getName())) {
            return true;
        }

        if (chargeViaEconomyObject(player, shapeName, price, plugin, plugin.getName())) {
            return true;
        }

        return false;
    }

    private static Object getEconomyUser(Plugin plugin, Player player) {
        String[] userMethods = new String[]{"getUser", "getAccount", "getIConomyAccount", "getPlayer", "getPlayerAccount"};
        for (String methodName : userMethods) {
            try {
                Method method = plugin.getClass().getMethod(methodName, String.class);
                Object user = method.invoke(plugin, player.getName());
                if (user != null) {
                    return user;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {
            }
            try {
                Method method = plugin.getClass().getMethod(methodName, Player.class);
                Object user = method.invoke(plugin, player);
                if (user != null) {
                    return user;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static boolean chargeViaEconomyUser(Player player, String shapeName, double price, Object user, String source) {
        Boolean enough = invokeEconomyCheck(user, player, price);
        if (enough == null) {
            return false;
        }
        if (!enough) {
            player.sendMessage(
                    "§3:: §5error §3:: §7You do not have enough money to build this stargate. "
                    + "Cost: " + price);
            WXTLogger.prettyLog(Level.INFO, false,
                    "[Economy] " + player.getName() + " cannot afford " + price
                    + " for '" + shapeName + "'.");
            return false;
        }

        Boolean paid = invokeEconomyWithdraw(user, player, price);
        if (paid == null || !paid) {
            return false;
        }

        player.sendMessage(
                "§3:: §7" + price + " has been deducted from your balance "
                + "for building a " + shapeName + " stargate.");
        WXTLogger.prettyLog(Level.INFO, false,
                "[Economy] " + source + ": charged " + player.getName() + " "
                + price + " for '" + shapeName + "'.");
        return true;
    }

    private static boolean chargeViaEconomyObject(Player player, String shapeName, double price, Object target, String source) {
        Boolean enough = invokeEconomyCheck(target, player, price);
        if (enough == null) {
            return false;
        }
        if (!enough) {
            player.sendMessage(
                    "§3:: §5error §3:: §7You do not have enough money to build this stargate. "
                    + "Cost: " + price);
            WXTLogger.prettyLog(Level.INFO, false,
                    "[Economy] " + player.getName() + " cannot afford " + price
                    + " for '" + shapeName + "'.");
            return false;
        }

        Boolean paid = invokeEconomyWithdraw(target, player, price);
        if (paid == null || !paid) {
            return false;
        }

        player.sendMessage(
                "§3:: §7" + price + " has been deducted from your balance "
                + "for building a " + shapeName + " stargate.");
        WXTLogger.prettyLog(Level.INFO, false,
                "[Economy] " + source + ": charged " + player.getName() + " "
                + price + " for '" + shapeName + "'.");
        return true;
    }

    private static Boolean invokeEconomyCheck(Object target, org.bukkit.entity.Player player, double price) {
        String[] checkMethods = new String[]{"has", "hasEnough", "canAfford", "canPay", "hasMoney", "hasFunds", "canPay"};
        for (String methodName : checkMethods) {
            Method method = findFirstMethod(target.getClass(), new String[]{methodName}, double.class);
            if (method != null) {
                try {
                    return (Boolean) method.invoke(target, price);
                } catch (Exception ignored) {
                }
            }
            method = findFirstMethod(target.getClass(), new String[]{methodName}, org.bukkit.entity.Player.class, double.class);
            if (method != null) {
                try {
                    return (Boolean) method.invoke(target, player, price);
                } catch (Exception ignored) {
                }
            }
            method = findFirstMethod(target.getClass(), new String[]{methodName}, String.class, double.class);
            if (method != null) {
                try {
                    return (Boolean) method.invoke(target, player.getName(), price);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static Boolean invokeEconomyWithdraw(Object target, org.bukkit.entity.Player player, double price) {
        String[] withdrawMethods = new String[]{"withdraw", "subtract", "take", "remove", "pay", "charge", "debit"};
        for (String methodName : withdrawMethods) {
            Method method = findFirstMethod(target.getClass(), new String[]{methodName}, double.class);
            if (method != null) {
                try {
                    Object result = method.invoke(target, price);
                    return result == null || !(result instanceof Boolean) || (Boolean) result;
                } catch (Exception ignored) {
                }
            }
            method = findFirstMethod(target.getClass(), new String[]{methodName}, org.bukkit.entity.Player.class, double.class);
            if (method != null) {
                try {
                    Object result = method.invoke(target, player, price);
                    return result == null || !(result instanceof Boolean) || (Boolean) result;
                } catch (Exception ignored) {
                }
            }
            method = findFirstMethod(target.getClass(), new String[]{methodName}, String.class, double.class);
            if (method != null) {
                try {
                    Object result = method.invoke(target, player.getName(), price);
                    return result == null || !(result instanceof Boolean) || (Boolean) result;
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static boolean chargeViaEconomyPluginApi(Player player, String shapeName, double price) {
        Plugin economyPlugin = Bukkit.getPluginManager().getPlugin(detectedPlugin);
        if (economyPlugin == null || !economyPlugin.isEnabled()) {
            return false;
        }

        try {
            ChargeResult staticResult = chargeViaEconomyStaticApi(player, shapeName, price);
            if (staticResult != ChargeResult.API_UNAVAILABLE) {
                return staticResult == ChargeResult.SUCCESS;
            }

            Object economy = getEconomyPluginObject(economyPlugin);
            if (economy != null && chargeViaEconomyObject(player, shapeName, price, economy, detectedPlugin)) {
                return true;
            }

            Object user = getEconomyPluginUser(economyPlugin, player);
            if (user != null && chargeViaEconomyUser(player, shapeName, price, user, detectedPlugin)) {
                return true;
            }
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.WARNING, false,
                    "[Economy] Economy plugin API error for " + player.getName() + ": " + e.getMessage());
        }
        return false;
    }

    private static ChargeResult chargeViaEconomyStaticApi(Player player, String shapeName, double price) {
        try {
            Class<?> economyApiClass = Class.forName("com.earth2me.essentials.api.Economy");
            Method hasMethod = findMethod(economyApiClass, "has", String.class, double.class);
            if (hasMethod == null) {
                hasMethod = findMethod(economyApiClass, "hasEnough", String.class, double.class);
            }
            Method withdrawMethod = findMethod(economyApiClass, "withdraw", String.class, double.class);
            if (withdrawMethod == null) {
                withdrawMethod = findMethod(economyApiClass, "subtract", String.class, double.class);
            }
            if (hasMethod == null || withdrawMethod == null) {
                return ChargeResult.API_UNAVAILABLE;
            }

            boolean enough = (boolean) hasMethod.invoke(null, player.getName(), price);
            if (!enough) {
                player.sendMessage(
                        "§3:: §5error §3:: §7You do not have enough money to build this stargate. "
                        + "Cost: " + price);
                WXTLogger.prettyLog(Level.INFO, false,
                        "[Economy] " + player.getName() + " cannot afford " + price
                        + " for '" + shapeName + "'.");
                return ChargeResult.INSUFFICIENT_FUNDS;
            }

            Object result = withdrawMethod.invoke(null, player.getName(), price);
            if (result instanceof Boolean && !((Boolean) result)) {
                return ChargeResult.INSUFFICIENT_FUNDS;
            }
            player.sendMessage(
                    "§3:: §7" + price + " has been deducted from your balance "
                    + "for building a " + shapeName + " stargate.");
            WXTLogger.prettyLog(Level.INFO, false,
                    "[Economy] Essentials static API: charged " + player.getName() + " "
                    + price + " for '" + shapeName + "'.");
            return ChargeResult.SUCCESS;
        } catch (ClassNotFoundException ignored) {
            return ChargeResult.API_UNAVAILABLE;
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.WARNING, false,
                    "[Economy] Essentials static API error for " + player.getName() + ": " + e.getMessage());
            return ChargeResult.API_UNAVAILABLE;
        }
    }

    private static Object getEconomyPluginObject(Plugin economyPlugin) {
        for (String methodName : new String[]{"getEconomy", "getEconomyHandler", "getMoneyEconomy", "getAPI", "getApi", "getEconomyAPI", "getProvider", "getEconomyManager", "getEconomyPlus"}) {
            try {
                Method method = economyPlugin.getClass().getMethod(methodName);
                Object economy = method.invoke(economyPlugin);
                if (economy != null) {
                    return economy;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {
            }
        }
        // Try common field names that may hold an API object (plugin-specific)
        for (String fieldName : new String[]{"economy", "api", "economyApi", "economyManager"}) {
            try {
                java.lang.reflect.Field f = economyPlugin.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                Object val = f.get(economyPlugin);
                if (val != null) return val;
            } catch (NoSuchFieldException ignored) {
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static boolean chargeViaEconomyPluginObject(Player player, String shapeName, double price, Object economy) {
        Method hasMethod = findMethod(economy.getClass(), "has", String.class, double.class);
        if (hasMethod == null) {
            hasMethod = findMethod(economy.getClass(), "hasEnough", String.class, double.class);
        }
        Method withdrawMethod = findFirstMethod(economy.getClass(), new String[]{"withdraw", "subtract", "take"}, String.class, double.class);
        if (hasMethod == null || withdrawMethod == null) {
            return false;
        }

        try {
            boolean enough = (boolean) hasMethod.invoke(economy, player.getName(), price);
            if (!enough) {
                player.sendMessage(
                        "§3:: §5error §3:: §7You do not have enough money to build this stargate. "
                        + "Cost: " + price);
                WXTLogger.prettyLog(Level.INFO, false,
                        "[Economy] " + player.getName() + " cannot afford " + price
                        + " for '" + shapeName + "'.");
                return false;
            }
            Object result = withdrawMethod.invoke(economy, player.getName(), price);
            if (result instanceof Boolean && !((Boolean) result)) {
                return false;
            }
            player.sendMessage(
                    "§3:: §7" + price + " has been deducted from your balance "
                    + "for building a " + shapeName + " stargate.");
            WXTLogger.prettyLog(Level.INFO, false,
                    "[Economy] Economy plugin object: charged " + player.getName() + " "
                    + price + " for '" + shapeName + "'.");
            return true;
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.WARNING, false,
                    "[Economy] Economy plugin object error for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private static Object getEconomyPluginUser(Plugin economyPlugin, Player player) {
        try {
            Method getUserByName = findMethod(economyPlugin.getClass(), "getUser", String.class);
            if (getUserByName != null) {
                return getUserByName.invoke(economyPlugin, player.getName());
            }
        } catch (Exception ignored) {
        }
        try {
            Method getUserByPlayer = findMethod(economyPlugin.getClass(), "getUser", Player.class);
            if (getUserByPlayer != null) {
                return getUserByPlayer.invoke(economyPlugin, player);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean chargeViaEconomyPluginUser(Player player, String shapeName, double price, Object user) {
        Method hasMethod = findFirstMethod(user.getClass(), new String[]{"has", "hasEnough", "canAfford"}, double.class);
        Method withdrawMethod = findFirstMethod(user.getClass(), new String[]{"withdraw", "subtract", "take", "remove", "pay"}, double.class);
        if (hasMethod == null || withdrawMethod == null) {
            return false;
        }

        try {
            boolean enough = (boolean) hasMethod.invoke(user, price);
            if (!enough) {
                player.sendMessage(
                        "§3:: §5error §3:: §7You do not have enough money to build this stargate. "
                        + "Cost: " + price);
                WXTLogger.prettyLog(Level.INFO, false,
                        "[Economy] " + player.getName() + " cannot afford " + price
                        + " for '" + shapeName + "'.");
                return false;
            }
            Object result = withdrawMethod.invoke(user, price);
            if (result instanceof Boolean && !((Boolean) result)) {
                return false;
            }
            player.sendMessage(
                    "§3:: §7" + price + " has been deducted from your balance "
                    + "for building a " + shapeName + " stargate.");
            WXTLogger.prettyLog(Level.INFO, false,
                    "[Economy] Economy plugin user object: charged " + player.getName() + " "
                    + price + " for '" + shapeName + "'.");
            return true;
        } catch (Exception e) {
            WXTLogger.prettyLog(Level.WARNING, false,
                    "[Economy] Economy plugin user error for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private static Method findFirstMethod(Class<?> type, String[] methodNames, Class<?>... parameterTypes) {
        for (String methodName : methodNames) {
            Method method = findMethod(type, methodName, parameterTypes);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            return type.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
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
            return false;
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