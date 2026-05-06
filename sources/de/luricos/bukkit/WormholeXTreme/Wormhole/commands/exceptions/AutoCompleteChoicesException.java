package de.luricos.bukkit.WormholeXTreme.Wormhole.commands.exceptions;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/commands/exceptions/AutoCompleteChoicesException.class */
public class AutoCompleteChoicesException extends RuntimeException {
    protected String[] choices;
    protected String argName;

    public AutoCompleteChoicesException(String[] choices, String argName) {
        this.choices = choices;
        this.argName = argName;
    }

    public String getArgName() {
        return this.argName;
    }

    public String[] getChoices() {
        return this.choices;
    }
}
