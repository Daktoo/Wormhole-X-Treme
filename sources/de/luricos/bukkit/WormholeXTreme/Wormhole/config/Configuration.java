package de.luricos.bukkit.WormholeXTreme.Wormhole.config;

import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionsManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.plugin.PluginDescriptionFile;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/config/Configuration.class */
public class Configuration {
    private static File options = null;

    private static boolean invalidFile(File file, PluginDescriptionFile desc) {
        String s;
        BufferedReader bufferedreader = null;
        try {
            bufferedreader = new BufferedReader(new FileReader(file));
            do {
                s = bufferedreader.readLine();
                if (s == null) {
                    if (bufferedreader != null) {
                        try {
                            bufferedreader.close();
                        } catch (IOException e) {
                            WXTLogger.prettyLog(Level.WARNING, false, "Failure to close stream: " + e.getMessage());
                            return true;
                        }
                    }
                    return true;
                }
            } while (s.indexOf(desc.getVersion()) <= -1);
            if (bufferedreader != null) {
                try {
                    bufferedreader.close();
                } catch (IOException e2) {
                    WXTLogger.prettyLog(Level.WARNING, false, "Failure to close stream: " + e2.getMessage());
                }
            }
            return false;
        } catch (IOException e3) {
            if (bufferedreader != null) {
                try {
                    bufferedreader.close();
                } catch (IOException e4) {
                    WXTLogger.prettyLog(Level.WARNING, false, "Failure to close stream: " + e4.getMessage());
                    return true;
                }
            }
            return true;
        } catch (Throwable th) {
            if (bufferedreader != null) {
                try {
                    bufferedreader.close();
                } catch (IOException e5) {
                    WXTLogger.prettyLog(Level.WARNING, false, "Failure to close stream: " + e5.getMessage());
                    throw th;
                }
            }
            throw th;
        }
    }

    protected static void loadConfiguration(PluginDescriptionFile desc) {
        readFile(desc);
    }

    private static void readFile(File file, PluginDescriptionFile desc) throws IOException {
        Setting s;
        Setting[] arr$ = DefaultSettings.config;
        for (Setting element : arr$) {
            String value = ConfigurationFlatFile.getValueFromSetting(file, element.getName(), element.getValue().toString());
            if (value.toLowerCase().contains("true") || value.toLowerCase().contains("false")) {
                Setting s2 = new Setting(element.getName(), Boolean.valueOf(Boolean.parseBoolean(value)), element.getDescription(), "WormholeXTreme");
                ConfigManager.getConfigurations().put(s2.getName(), s2);
            } else {
                try {
                    Setting s3 = new Setting(element.getName(), Integer.valueOf(Integer.parseInt(value)), element.getDescription(), "WormholeXTreme");
                    ConfigManager.getConfigurations().put(s3.getName(), s3);
                } catch (NumberFormatException e) {
                    try {
                        s = new Setting(element.getName(), Double.valueOf(Double.parseDouble(value)), element.getDescription(), "WormholeXTreme");
                    } catch (NumberFormatException e2) {
                        if (element.getName() == ConfigManager.ConfigKeys.BUILT_IN_DEFAULT_PERMISSION_LEVEL) {
                            s = new Setting(element.getName(), PermissionsManager.PermissionLevel.valueOf(value), element.getDescription(), "WormholeXTreme");
                        } else {
                            s = new Setting(element.getName(), value, element.getDescription(), "WormholeXTreme");
                        }
                    }
                    ConfigManager.getConfigurations().put(s.getName(), s);
                }
            }
        }
    }

    private static void readFile(PluginDescriptionFile desc) {
        File directory = new File("plugins" + File.separator + desc.getName() + File.separator);
        if (!directory.exists()) {
            try {
                directory.mkdir();
            } catch (Exception e) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Unable to make directory: " + e.getMessage());
            }
        }
        String input = directory.getPath() + File.separator + "Settings.txt";
        options = new File(input);
        if (!options.exists()) {
            writeFile(options, desc, DefaultSettings.config);
        }
        try {
            readFile(options, desc);
        } catch (IOException e2) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Failed to read fiele: " + e2.getMessage());
        }
        if (invalidFile(options, desc)) {
            writeFile(desc);
        }
    }

    private static void writeFile(File file, PluginDescriptionFile desc, Setting[] config) {
        try {
            try {
                file.createNewFile();
            } catch (Exception e) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Unable to Create File: " + e.getMessage());
            }
            BufferedWriter bufferedwriter = new BufferedWriter(new FileWriter(file));
            ConfigurationFlatFile.createNewHeader(bufferedwriter, desc.getName() + " " + desc.getVersion(), desc.getName() + " Config Settings", true);
            for (Setting element : config) {
                ConfigurationFlatFile.createNewSetting(bufferedwriter, element.getName(), element.getValue().toString(), element.getDescription());
            }
            bufferedwriter.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void writeFile(PluginDescriptionFile desc) {
        try {
            try {
                options.createNewFile();
            } catch (Exception e) {
                WXTLogger.prettyLog(Level.SEVERE, false, "Unable to create new file: " + e.getMessage());
            }
            BufferedWriter bufferedwriter = new BufferedWriter(new FileWriter(options));
            ConfigurationFlatFile.createNewHeader(bufferedwriter, desc.getName() + " " + desc.getVersion(), desc.getName() + " Config Settings", true);
            Set<ConfigManager.ConfigKeys> keys = ConfigManager.getConfigurations().keySet();
            ArrayList<ConfigManager.ConfigKeys> list = new ArrayList<>(keys);
            Collections.sort(list);
            for (ConfigManager.ConfigKeys key : list) {
                Setting s = ConfigManager.getConfigurations().get(key);
                if (s != null) {
                    ConfigurationFlatFile.createNewSetting(bufferedwriter, s.getName(), s.getValue().toString(), s.getDescription());
                }
            }
            bufferedwriter.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
