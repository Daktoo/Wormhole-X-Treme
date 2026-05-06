package de.luricos.bukkit.WormholeXTreme.Wormhole.utils;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/utils/WXTStringUtils.class */
public class WXTStringUtils {
    public static boolean isIntNumber(String num) {
        try {
            Integer.parseInt(num);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
