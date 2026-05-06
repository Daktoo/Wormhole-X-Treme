package de.luricos.bukkit.WormholeXTreme.Wormhole.commands;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/commands/CommandSyntax.class */
public class CommandSyntax {
    protected String originalSyntax;
    protected String regexp;
    protected List<String> arguments = new LinkedList();

    public CommandSyntax(String syntax) {
        this.originalSyntax = syntax;
        this.regexp = prepareSyntaxRegexp(syntax);
    }

    public String getRegexp() {
        return this.regexp;
    }

    private String prepareSyntaxRegexp(String syntax) {
        String strReplace;
        String expression = syntax;
        Matcher argMatcher = Pattern.compile("(?:[\\s]+)?((\\<|\\[)([^\\>\\]]+)(?:\\>|\\]))").matcher(expression);
        int index = 0;
        while (argMatcher.find()) {
            if (argMatcher.group(2).equals("[")) {
                strReplace = expression.replace(argMatcher.group(0), "(?:(?:[\\s]+)(\"[^\"]+\"|[^\\s]+))?");
            } else {
                strReplace = expression.replace(argMatcher.group(1), "(\"[^\"]+\"|[\\S]+)");
            }
            expression = strReplace;
            int i = index;
            index++;
            this.arguments.add(i, argMatcher.group(3));
        }
        return expression;
    }

    public boolean isMatch(String str) {
        return str.matches(this.regexp);
    }

    public Map<String, String> getMatchedArguments(String str) {
        Map<String, String> matchedArguments = new HashMap<>(this.arguments.size());
        if (this.arguments.size() > 0) {
            Matcher argMatcher = Pattern.compile(this.regexp).matcher(str);
            if (argMatcher.find()) {
                for (int index = 1; index <= argMatcher.groupCount(); index++) {
                    String argumentValue = argMatcher.group(index);
                    if (argumentValue != null && !argumentValue.isEmpty()) {
                        if (argumentValue.startsWith("\"") && argumentValue.endsWith("\"")) {
                            argumentValue = argumentValue.substring(1, argumentValue.length() - 1);
                        }
                        matchedArguments.put(this.arguments.get(index - 1), argumentValue);
                    }
                }
            }
        }
        return matchedArguments;
    }
}
