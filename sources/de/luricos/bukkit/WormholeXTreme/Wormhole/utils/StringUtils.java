package de.luricos.bukkit.WormholeXTreme.Wormhole.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/utils/StringUtils.class */
public class StringUtils {
    public static String implode(String[] array, String separator) {
        if (array.length == 0) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        for (String str : array) {
            buffer.append(separator);
            buffer.append(str);
        }
        return buffer.substring(separator.length()).trim();
    }

    public static String implode(List<?> list, String separator) {
        if (list.isEmpty()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        int lastElement = list.size() - 1;
        for (int i = 0; i < list.size(); i++) {
            buffer.append(list.get(i).toString());
            if (i < lastElement) {
                buffer.append(separator);
            }
        }
        return buffer.toString();
    }

    public static String readStream(InputStream is) throws IOException {
        if (is != null) {
            StringBuilder builder = new StringBuilder();
            try {
                Reader reader = new InputStreamReader(is, "UTF-8");
                char[] buffer = new char[128];
                while (true) {
                    int read = reader.read(buffer);
                    if (read > 0) {
                        builder.append(buffer, 0, read);
                    } else {
                        return builder.toString();
                    }
                }
            } finally {
                is.close();
            }
        } else {
            return null;
        }
    }

    public static String repeat(String str, int times) {
        StringBuilder buffer = new StringBuilder(times * str.length());
        for (int i = 0; i < times; i++) {
            buffer.append(str);
        }
        return buffer.toString();
    }

    public static int toInteger(String value, int defaultValue) {
        int ret = defaultValue;
        try {
            ret = Integer.parseInt(value);
        } catch (NumberFormatException e) {
        }
        return ret;
    }
}
