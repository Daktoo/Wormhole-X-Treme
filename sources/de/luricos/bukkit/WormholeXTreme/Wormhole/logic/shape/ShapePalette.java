package de.luricos.bukkit.WormholeXTreme.Wormhole.logic.shape;

import java.util.ArrayList;
import java.util.List;

/**
 * The vocabulary of a .Shape cell.
 *
 * A cell is a base block plus any number of modifiers, written
 * {@code [BASE:MOD:MOD]} - for example {@code [S:L#1:EP]} is a frame block that
 * is both the first chevron light and the player arrival point. Modelling a
 * cell as a single flat token would quietly discard the extra modifiers when a
 * shape was edited, so base and modifiers are kept separate throughout.
 *
 * Token text is written to disk verbatim, so it follows the format documented
 * in GateShapes/Standard.shape.
 */
public class ShapePalette {

    public static final String IGNORED = "I";
    public static final String STARGATE = "S";
    public static final String PORTAL = "P";
    public static final String REDSTONE_DIAL = "RD";
    public static final String REDSTONE_SIGN = "RS";
    public static final String REDSTONE_ACTIVE = "RA";

    public static final String MOD_NAME_SIGN = "N";
    public static final String MOD_ENTER_PLAYER = "EP";
    public static final String MOD_ENTER_MINECART = "EM";
    public static final String MOD_ACTIVATION = "A";
    public static final String MOD_DIALER_SIGN = "D";
    public static final String MOD_IRIS_ACTIVATION = "IA";
    public static final String MOD_LIGHT = "L";
    public static final String MOD_WOOSH = "W";
    /**
     * The redstone tokens are accepted both as a standalone base - [RD] - and
     * as a modifier on another block - [S:RS] - because the game's own layer
     * parser matches them the same way in either position, and the bundled
     * shapes use both forms.
     */
    public static final String MOD_REDSTONE_DIAL = "RD";
    public static final String MOD_REDSTONE_SIGN = "RS";
    public static final String MOD_REDSTONE_ACTIVE = "RA";

    /** A base block type - what the cell is made of. */
    public static class Base {
        private final String token;
        private final String label;
        private final String colour;
        private final String description;

        private Base(String token, String label, String colour, String description) {
            this.token = token;
            this.label = label;
            this.colour = colour;
            this.description = description;
        }

        public String getToken() {
            return this.token;
        }

        public String getLabel() {
            return this.label;
        }

        public String getColour() {
            return this.colour;
        }

        public String getDescription() {
            return this.description;
        }
    }

    /** A role a cell plays on top of its base block. */
    public static class Modifier {
        private final String token;
        private final String label;
        private final String colour;
        private final String description;
        private final boolean ordered;
        private final boolean unique;

        private Modifier(String token, String label, String colour, String description, boolean ordered, boolean unique) {
            this.token = token;
            this.label = label;
            this.colour = colour;
            this.description = description;
            this.ordered = ordered;
            this.unique = unique;
        }

        public String getToken() {
            return this.token;
        }

        public String getLabel() {
            return this.label;
        }

        public String getColour() {
            return this.colour;
        }

        public String getDescription() {
            return this.description;
        }

        /** True for L and W, which carry a #number firing order. */
        public boolean isOrdered() {
            return this.ordered;
        }

        /** True when a gate may only have one cell carrying this modifier. */
        public boolean isUnique() {
            return this.unique;
        }
    }

    private static final List<Base> BASES = new ArrayList<Base>();
    private static final List<Modifier> MODIFIERS = new ArrayList<Modifier>();

    static {
        BASES.add(new Base(IGNORED, "I", "\u00a78", "Ignored - nothing is placed or checked here."));
        BASES.add(new Base(STARGATE, "S", "\u00a77", "Stargate material - part of the gate frame."));
        BASES.add(new Base(PORTAL, "P", "\u00a79", "Portal block - air until the gate activates, then portal material."));
        BASES.add(new Base(REDSTONE_DIAL, ">", "\u00a74", "Redstone dial activation. A charge next to this block dials the gate. Needs a sign dialler, and should sit on a frame block."));
        BASES.add(new Base(REDSTONE_SIGN, "~", "\u00a74", "Redstone sign cycle. A charge next to this block cycles the sign target. Needs a sign dialler."));
        BASES.add(new Base(REDSTONE_ACTIVE, "<", "\u00a74", "Redstone output. Emits a charge while the gate is active. Should sit on a frame block."));

        // Ordered by how much they matter when picking the grid label.
        MODIFIERS.add(new Modifier(MOD_ACTIVATION, "A", "\u00a7c", "Activation switch attaches here. Required, exactly one per gate. The block facing it must be Ignored.", false, true));
        MODIFIERS.add(new Modifier(MOD_DIALER_SIGN, "D", "\u00a7d", "Sign dialler hangs here. Optional, one per gate - without it the gate can only be dialled with /dial. The block facing it must be Ignored.", false, true));
        MODIFIERS.add(new Modifier(MOD_NAME_SIGN, "N", "\u00a7e", "Name sign holder. Optional, one per gate.", false, true));
        MODIFIERS.add(new Modifier(MOD_IRIS_ACTIVATION, "R", "\u00a76", "Iris activation switch. Optional, one per gate - needed if the gate should support an iris.", false, true));
        MODIFIERS.add(new Modifier(MOD_ENTER_PLAYER, "E", "\u00a7a", "Player arrival point. Feet land on this block.", false, true));
        MODIFIERS.add(new Modifier(MOD_ENTER_MINECART, "M", "\u00a7a", "Minecart arrival point. Wheels land on this block.", false, true));
        MODIFIERS.add(new Modifier(MOD_LIGHT, "L", "\u00a7b", "Chevron light - lights up when the gate activates. Numbered in the order you place them.", true, false));
        MODIFIERS.add(new Modifier(MOD_WOOSH, "W", "\u00a73", "Woosh block - part of the opening animation. Numbered in the order you place them.", true, false));
        MODIFIERS.add(new Modifier(MOD_REDSTONE_DIAL, ">", "\u00a74", "Redstone dial activation. A charge next to this block dials the gate. Needs a sign dialler.", false, true));
        MODIFIERS.add(new Modifier(MOD_REDSTONE_SIGN, "~", "\u00a74", "Redstone sign cycle. A charge next to this block cycles the sign target. Needs a sign dialler.", false, true));
        MODIFIERS.add(new Modifier(MOD_REDSTONE_ACTIVE, "<", "\u00a74", "Redstone output. Emits a charge while the gate is active.", false, true));
    }

    public static List<Base> getBases() {
        return BASES;
    }

    public static List<Modifier> getModifiers() {
        return MODIFIERS;
    }

    public static Base getBase(String token) {
        for (Base base : BASES) {
            if (base.getToken().equalsIgnoreCase(token)) {
                return base;
            }
        }
        return null;
    }

    public static Modifier getModifier(String token) {
        for (Modifier modifier : MODIFIERS) {
            if (modifier.getToken().equalsIgnoreCase(token)) {
                return modifier;
            }
        }
        return null;
    }

    /** Splits "S:L#1:EP" into its colon-separated segments. */
    public static String[] segments(String cell) {
        if (cell == null || cell.isEmpty()) {
            return new String[]{IGNORED};
        }
        return cell.split(":");
    }

    public static String baseOf(String cell) {
        String[] parts = segments(cell);
        return parts.length == 0 ? IGNORED : parts[0];
    }

    /** Modifier segments with their #number suffixes intact. */
    public static List<String> rawModifiers(String cell) {
        List<String> mods = new ArrayList<String>();
        String[] parts = segments(cell);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                mods.add(parts[i]);
            }
        }
        return mods;
    }

    /** Strips any #number, so "L#3" becomes "L". */
    public static String stripOrder(String modifier) {
        int hash = modifier.indexOf('#');
        return hash < 0 ? modifier : modifier.substring(0, hash);
    }

    public static boolean hasModifier(String cell, String modifier) {
        for (String raw : rawModifiers(cell)) {
            if (stripOrder(raw).equalsIgnoreCase(modifier)) {
                return true;
            }
        }
        return false;
    }

    /** Firing order of the given modifier on this cell, or 0 when unnumbered. */
    public static int orderOf(String cell, String modifier) {
        for (String raw : rawModifiers(cell)) {
            if (!stripOrder(raw).equalsIgnoreCase(modifier)) {
                continue;
            }
            int hash = raw.indexOf('#');
            if (hash < 0) {
                return 0;
            }
            try {
                return Integer.parseInt(raw.substring(hash + 1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /** Rebuilds a cell token from a base and its modifier segments. */
    public static String build(String base, List<String> modifiers) {
        StringBuilder out = new StringBuilder(base);
        for (String modifier : modifiers) {
            out.append(':').append(modifier);
        }
        return out.toString();
    }

    /** Swaps the base block, leaving every modifier in place. */
    public static String withBase(String cell, String base) {
        return build(base, rawModifiers(cell));
    }

    /**
     * Adds the modifier if absent, removes it if present.
     * The order value is used only when adding an ordered modifier.
     */
    public static String toggleModifier(String cell, String modifier, int order) {
        List<String> result = new ArrayList<String>();
        boolean removed = false;
        for (String raw : rawModifiers(cell)) {
            if (stripOrder(raw).equalsIgnoreCase(modifier)) {
                removed = true;
                continue;
            }
            result.add(raw);
        }
        if (!removed) {
            Modifier definition = getModifier(modifier);
            result.add(definition != null && definition.isOrdered() ? modifier + "#" + order : modifier);
        }
        return build(baseOf(cell), result);
    }

    /**
     * The grid label: the most significant modifier if the cell has one,
     * otherwise the base block's own letter.
     */
    public static String labelOf(String cell) {
        for (Modifier modifier : MODIFIERS) {
            if (!hasModifier(cell, modifier.getToken())) {
                continue;
            }
            if (modifier.isOrdered()) {
                int order = orderOf(cell, modifier.getToken());
                return order > 0 ? modifier.getLabel() + order : modifier.getLabel();
            }
            return modifier.getLabel();
        }
        Base base = getBase(baseOf(cell));
        return base == null ? "?" : base.getLabel();
    }

    public static String colourOf(String cell) {
        for (Modifier modifier : MODIFIERS) {
            if (hasModifier(cell, modifier.getToken())) {
                return modifier.getColour();
            }
        }
        Base base = getBase(baseOf(cell));
        return base == null ? "\u00a7f" : base.getColour();
    }

    /** True when every segment of the cell is something the palette knows. */
    public static boolean isRecognised(String cell) {
        if (getBase(baseOf(cell)) == null) {
            return false;
        }
        for (String raw : rawModifiers(cell)) {
            if (getModifier(stripOrder(raw)) == null) {
                return false;
            }
        }
        return true;
    }

    /** Human-readable summary of everything a cell is doing. */
    public static String describe(String cell) {
        StringBuilder out = new StringBuilder();
        Base base = getBase(baseOf(cell));
        out.append(base == null ? "Unrecognised block." : base.getDescription());
        for (String raw : rawModifiers(cell)) {
            Modifier modifier = getModifier(stripOrder(raw));
            if (modifier != null) {
                out.append("\n\u00a7f").append(modifier.getDescription());
            }
        }
        return out.toString();
    }
}
