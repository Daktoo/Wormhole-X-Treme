package de.luricos.bukkit.WormholeXTreme.Wormhole.config;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/config/ConfigurationFlatFile.class */
public class ConfigurationFlatFile {
    protected static void createNewHeader(BufferedWriter output, String title, String subtitle, boolean firstHeader) throws IOException {
        if (!firstHeader) {
            output.write("---------------");
            output.newLine();
            output.newLine();
            output.write("-------------------------------");
            output.newLine();
        }
        output.write(title);
        output.newLine();
        output.write(subtitle);
        output.newLine();
        output.write("-------------------------------");
        output.newLine();
        output.newLine();
    }

    protected static void createNewSetting(BufferedWriter output, ConfigManager.ConfigKeys name, String value, String description) throws IOException {
        output.append("---------------");
        output.newLine();
        output.write("Setting: " + name);
        output.newLine();
        output.write("Value: " + value);
        output.newLine();
        output.write("Description:");
        ArrayList<String> desc = new ArrayList<>();
        desc.add(0, "");
        String[] words = description.split(" ");
        int lineNumber = 0;
        for (String word : words) {
            if (desc.get(lineNumber).length() + word.length() < 80) {
                desc.set(lineNumber, desc.get(lineNumber) + " " + word);
            } else {
                lineNumber++;
                desc.add(lineNumber, "             " + word);
            }
        }
        for (String s : desc) {
            output.write(s);
            output.newLine();
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:18:0x00a1, code lost:
    
        r9.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x00a6, code lost:
    
        r9.close();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    protected static java.lang.String getValueFromSetting(java.io.File file, de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager.ConfigKeys key, java.lang.String defaultValue) throws java.io.IOException {
        if (file == null || !file.exists()) {
            return defaultValue;
        }
        java.io.BufferedReader reader = null;
        try {
            reader = new java.io.BufferedReader(new java.io.FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("Setting: " + key.toString())) {
                    String valueLine = reader.readLine();
                    if (valueLine != null && valueLine.startsWith("Value: ")) {
                        return valueLine.substring("Value: ".length()).trim();
                    }
                }
            }
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (java.io.IOException e) { e.printStackTrace(); }
            }
        }
        return defaultValue;
    }
}
