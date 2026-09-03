package de.luricos.bukkit.WormholeXTreme.Wormhole.logic.shape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads and writes the ENABLED setting in a .shape file.
 *
 * A disabled shape has to stay disabled across a restart, so the flag lives in
 * the shape file itself rather than in memory or in a side config. That also
 * means an admin can flip it with a text editor and get the same result as
 * running the command.
 *
 * Nothing here touches Bukkit, so it can be exercised on its own.
 */
public final class ShapeEnabledFile {

    /**
     * Shapes that ship switched off. This only decides what happens the first
     * time a file is read without an ENABLED line; once the line is written,
     * the file is the only thing that matters.
     */
    private static final Set<String> DEFAULT_DISABLED = new HashSet<String>(Arrays.asList(
            "large", "largesigndial",
            "minimal", "minimalsigndial",
            "small", "smallsigndial"));

    private static final String KEY = "ENABLED";

    /** The comment written above the setting when it is first added. */
    public static final String COMMENT =
            "# FALSE stops this shape being used to build gates. Survives restarts.";

    private ShapeEnabledFile() {
    }

    /** Whether a shape of this name is on by default when its file is silent. */
    public static boolean defaultFor(String shapeName) {
        return shapeName == null || !DEFAULT_DISABLED.contains(shapeName.toLowerCase());
    }

    /**
     * The flag as written in the file, or null when the file does not mention
     * it at all. Comments are skipped, and keys that merely contain the word
     * (REDSTONE_ACTIVATED, say) are not mistaken for it.
     */
    public static Boolean readFlag(List<String> lines) {
        if (lines == null) {
            return null;
        }
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String value = valueFor(line, KEY);
            if (value != null) {
                return Boolean.valueOf(parse(value));
            }
        }
        return null;
    }

    /**
     * Reads a written value. Anything that is not recognisably a "no" counts
     * as enabled, so a typo leaves a shape usable rather than silently
     * removing it from the server.
     */
    public static boolean parse(String value) {
        String v = value == null ? "" : value.replace(";", "").trim();
        return !(v.equalsIgnoreCase("false") || v.equalsIgnoreCase("no")
                || v.equalsIgnoreCase("off") || v.equals("0"));
    }

    /**
     * Returns the file contents with the flag set.
     *
     * An existing ENABLED line is replaced where it stands. Otherwise the
     * setting is inserted just below Version=, or below Name= when there is no
     * version line, so it sits with the other top level settings instead of
     * being buried after the grid.
     */
    public static List<String> withFlag(List<String> lines, boolean enabled) {
        List<String> out = lines == null
                ? new ArrayList<String>()
                : new ArrayList<String>(lines);
        String setting = KEY + "=" + (enabled ? "TRUE" : "FALSE");

        for (int i = 0; i < out.size(); i++) {
            String line = out.get(i) == null ? "" : out.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (valueFor(line, KEY) != null) {
                out.set(i, setting);
                return out;
            }
        }

        int at = insertionPoint(out);
        out.add(at, setting);
        out.add(at, COMMENT);
        return out;
    }

    /** Where a new setting should go: after Version=, else after Name=, else the top. */
    private static int insertionPoint(List<String> lines) {
        int afterName = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i) == null ? "" : lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (valueFor(line, "Version") != null) {
                return i + 1;
            }
            if (afterName < 0 && valueFor(line, "Name") != null) {
                afterName = i + 1;
            }
        }
        return afterName >= 0 ? afterName : 0;
    }

    /** The value of key=value on this line, or null when the key does not match. */
    private static String valueFor(String line, String key) {
        int eq = line.indexOf('=');
        if (eq <= 0) {
            return null;
        }
        if (!line.substring(0, eq).trim().equalsIgnoreCase(key)) {
            return null;
        }
        return line.substring(eq + 1);
    }
}
