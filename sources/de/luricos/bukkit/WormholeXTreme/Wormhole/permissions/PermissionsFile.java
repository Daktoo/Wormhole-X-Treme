package de.luricos.bukkit.WormholeXTreme.Wormhole.permissions;

import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * plugins/WormholeXTreme/Permissions.txt - a flat permission list for servers
 * running without an external permissions plugin.
 *
 * Each line is {@code node=opped} or {@code node=deopped}, answering "which
 * group should be able to use this permission?". {@code opped} means operators
 * only; {@code deopped} means everybody.
 *
 * The file is only consulted when no permissions plugin is driving the
 * decision. Ops are granted earlier in {@link WXPermissions} regardless, so in
 * practice this file decides what non-ops may do.
 *
 * Gate ownership still applies on top of it. Marking a gate-scoped node
 * {@code deopped} lets the existing ownership rules decide, it does not hand
 * every player rights over gates they did not build.
 */
public class PermissionsFile {

    private static final String FILE_NAME = "Permissions.txt";
    private static final String FOLDER = "plugins/WormholeXTreme/";

    private static final String OPPED = "opped";
    private static final String DEOPPED = "deopped";

    /** Node to default value. Insertion order is the order written to disk. */
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<String, String>();

    /** Node to the value actually loaded from disk. */
    private static final Map<String, String> values = new LinkedHashMap<String, String>();

    private static boolean loaded = false;

    static {
        // Defaults mirror plugin.yml, so switching a server from a permissions
        // plugin to this file does not silently change who can do what.
        DEFAULTS.put("wormhole.use", DEOPPED);
        DEFAULTS.put("wormhole.use.sign", DEOPPED);
        DEFAULTS.put("wormhole.use.dialer", DEOPPED);
        DEFAULTS.put("wormhole.use.compass", DEOPPED);
        DEFAULTS.put("wormhole.build", OPPED);
        DEFAULTS.put("wormhole.build.all", OPPED);
        DEFAULTS.put("wormhole.remove.own", OPPED);
        DEFAULTS.put("wormhole.remove.all", OPPED);
        DEFAULTS.put("wormhole.config", OPPED);
        DEFAULTS.put("wormhole.go", OPPED);
        DEFAULTS.put("wormhole.list", OPPED);
        DEFAULTS.put("wormhole.list.all", OPPED);
        DEFAULTS.put("wormhole.list.self", DEOPPED);
        DEFAULTS.put("wormhole.list.network", OPPED);
        DEFAULTS.put("wormhole.list.player", OPPED);
        DEFAULTS.put("wormhole.top", OPPED);
        DEFAULTS.put("wormhole.shape", OPPED);
        DEFAULTS.put("wormhole.cooldown.groupone", OPPED);
        DEFAULTS.put("wormhole.cooldown.grouptwo", OPPED);
        DEFAULTS.put("wormhole.cooldown.groupthree", OPPED);
        DEFAULTS.put("wormhole.build.groupone", OPPED);
        DEFAULTS.put("wormhole.build.grouptwo", OPPED);
        DEFAULTS.put("wormhole.build.groupthree", OPPED);
    }

    public static File getPermissionsFile() {
        return new File(FOLDER, FILE_NAME);
    }

    /**
     * Reads the file, creating it if it is not there and topping it up if the
     * plugin has gained nodes since it was written.
     *
     * The folder almost always exists already - the config and database are
     * written into it long before this runs - so creating the file must not be
     * conditional on having just created the folder. Existing installs get the
     * file on their next start.
     */
    public static void load() {
        values.clear();
        values.putAll(DEFAULTS);
        loaded = true;

        File folder = new File(FOLDER);
        if (!folder.exists() && !folder.mkdirs()) {
            WXTLogger.prettyLog(Level.WARNING, false, "Could not create " + FOLDER + " - falling back to built-in permission defaults.");
            return;
        }

        File file = getPermissionsFile();
        if (!file.exists()) {
            if (write(file, DEFAULTS, new ArrayList<String>())) {
                WXTLogger.prettyLog(Level.INFO, false, "Created " + file.getPath() + " with default permissions.");
            }
            return;
        }

        List<String> unknown = new ArrayList<String>();
        List<String> missing = new ArrayList<String>();
        try {
            for (String raw : Files.readAllLines(file.toPath())) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int equals = line.indexOf('=');
                if (equals < 0) {
                    WXTLogger.prettyLog(Level.WARNING, false, "Ignoring malformed line in " + FILE_NAME + ": " + line);
                    continue;
                }
                String node = line.substring(0, equals).trim().toLowerCase();
                String value = line.substring(equals + 1).trim().toLowerCase();
                if (!value.equals(OPPED) && !value.equals(DEOPPED)) {
                    WXTLogger.prettyLog(Level.WARNING, false, "Ignoring '" + node + "' in " + FILE_NAME
                            + ": expected " + OPPED + " or " + DEOPPED + ", found '" + value + "'.");
                    continue;
                }
                if (!DEFAULTS.containsKey(node)) {
                    // Kept rather than dropped, so a downgrade or a typo does
                    // not silently erase somebody's edit on rewrite.
                    unknown.add(node + "=" + value);
                }
                values.put(node, value);
            }
        } catch (IOException e) {
            WXTLogger.prettyLog(Level.WARNING, false, "Could not read " + file.getPath() + ": " + e.getMessage()
                    + " - falling back to built-in permission defaults.");
            return;
        }

        for (String node : DEFAULTS.keySet()) {
            if (!containsNode(file, node)) {
                missing.add(node);
            }
        }
        if (!missing.isEmpty()) {
            // Rewritten rather than appended so the file stays grouped and
            // commented, with every existing choice carried across.
            if (write(file, values, unknown)) {
                WXTLogger.prettyLog(Level.INFO, false, "Added " + missing.size() + " new permission node(s) to " + FILE_NAME + ".");
            }
        }
    }

    /** True when the file already mentions this node, comments aside. */
    private static boolean containsNode(File file, String node) {
        try {
            for (String raw : Files.readAllLines(file.toPath())) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int equals = line.indexOf('=');
                if (equals > 0 && line.substring(0, equals).trim().equalsIgnoreCase(node)) {
                    return true;
                }
            }
        } catch (IOException e) {
            return true;
        }
        return false;
    }

    private static boolean write(File file, Map<String, String> contents, List<String> extras) {
        StringBuilder out = new StringBuilder();
        out.append("# WormholeXTreme permissions\n");
        out.append("#\n");
        out.append("# Used when no permissions plugin is installed, or when permission\n");
        out.append("# support is switched off in the config.\n");
        out.append("#\n");
        out.append("#   node=opped     only server operators may use it\n");
        out.append("#   node=deopped   everybody may use it\n");
        out.append("#\n");
        out.append("# Operators always pass every check, so these lines really decide what\n");
        out.append("# everyone else can do. Gate ownership still applies on top: marking a\n");
        out.append("# gate permission deopped lets the normal owner rules decide, it does\n");
        out.append("# not give players rights over gates they did not build.\n");
        out.append("#\n");
        out.append("# Unknown nodes are left alone, so it is safe to keep your own notes\n");
        out.append("# and entries here.\n");
        out.append("\n");
        for (Map.Entry<String, String> entry : contents.entrySet()) {
            out.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        if (!extras.isEmpty()) {
            out.append("\n# Nodes this version does not recognise, preserved as written.\n");
            for (String extra : extras) {
                out.append(extra).append('\n');
            }
        }
        try {
            Files.write(file.toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            WXTLogger.prettyLog(Level.WARNING, false, "Could not write " + file.getPath() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Whether non-operators may use a node.
     * Returns null when the node is not covered, leaving the caller's own
     * fallback in charge.
     */
    public static Boolean allowsNonOp(String node) {
        if (!loaded) {
            load();
        }
        String value = values.get(node);
        if (value == null) {
            return null;
        }
        return Boolean.valueOf(value.equals(DEOPPED));
    }

    /** The node a permission type maps onto, or null if it has no entry. */
    public static String nodeFor(WXPermissions.PermissionType type) {
        switch (type) {
            case DAMAGE:
                return "wormhole.remove.all";
            case REMOVE:
                return "wormhole.remove.own";
            case CONFIG:
                return "wormhole.config";
            case GO:
                return "wormhole.go";
            case SIGN:
                return "wormhole.use.sign";
            case DIALER:
                return "wormhole.use.dialer";
            case USE:
                return "wormhole.use";
            case COMPASS:
                return "wormhole.use.compass";
            case BUILD:
                return "wormhole.build";
            case LIST:
                return "wormhole.list";
            case LIST_ALL:
                return "wormhole.list.all";
            case LIST_SELF:
                return "wormhole.list.self";
            case LIST_NETWORK:
                return "wormhole.list.network";
            case LIST_PLAYER:
                return "wormhole.list.player";
            case TOP:
                return "wormhole.top";
            case SHAPE:
                return "wormhole.shape";
            case USE_COOLDOWN_GROUP_ONE:
                return "wormhole.cooldown.groupone";
            case USE_COOLDOWN_GROUP_TWO:
                return "wormhole.cooldown.grouptwo";
            case USE_COOLDOWN_GROUP_THREE:
                return "wormhole.cooldown.groupthree";
            case BUILD_RESTRICTION_GROUP_ONE:
                return "wormhole.build.groupone";
            case BUILD_RESTRICTION_GROUP_TWO:
                return "wormhole.build.grouptwo";
            case BUILD_RESTRICTION_GROUP_THREE:
                return "wormhole.build.groupthree";
            default:
                return null;
        }
    }

    /** Convenience wrapper over {@link #nodeFor} and {@link #allowsNonOp}. */
    public static Boolean allowsNonOp(WXPermissions.PermissionType type) {
        String node = nodeFor(type);
        return node == null ? null : allowsNonOp(node);
    }

    /** Re-reads the file, for /wxreload. */
    public static void reload() {
        loaded = false;
        load();
    }
}
