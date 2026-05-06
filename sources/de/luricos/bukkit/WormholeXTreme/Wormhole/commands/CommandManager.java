package de.luricos.bukkit.WormholeXTreme.Wormhole.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.commands.exceptions.AutoCompleteChoicesException;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.StringUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/commands/CommandManager.class */
public class CommandManager {
    protected static final Logger logger = Bukkit.getLogger();
    protected Map<String, Map<CommandSyntax, CommandBinding>> listeners = new LinkedHashMap();
    protected Plugin plugin;

    public CommandManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(CommandListener listener) {
        Method[] arr$ = listener.getClass().getMethods();
        for (Method method : arr$) {
            if (method.isAnnotationPresent(Command.class)) {
                Command cmdAnnotation = (Command) method.getAnnotation(Command.class);
                Map<CommandSyntax, CommandBinding> commandListeners = this.listeners.get(cmdAnnotation.name());
                if (commandListeners == null) {
                    commandListeners = new LinkedHashMap();
                    this.listeners.put(cmdAnnotation.name(), commandListeners);
                }
                commandListeners.put(new CommandSyntax(cmdAnnotation.syntax()), new CommandBinding(listener, method));
            }
        }
        listener.onRegistered(this);
    }

    public boolean execute(CommandSender sender, org.bukkit.command.Command command, String[] args) {
        Map<CommandSyntax, CommandBinding> callMap = this.listeners.get(command.getName());
        if (callMap == null) {
            return false;
        }
        CommandBinding selectedBinding = null;
        String arguments = StringUtils.implode(args, " ");
        for (Map.Entry<CommandSyntax, CommandBinding> entry : callMap.entrySet()) {
            CommandSyntax syntax = entry.getKey();
            if (syntax.isMatch(arguments) && (selectedBinding == null || syntax.getRegexp().length() >= 0)) {
                CommandBinding binding = entry.getValue();
                binding.setParams(syntax.getMatchedArguments(arguments));
                selectedBinding = binding;
            }
        }
        if (selectedBinding == null) {
            sender.sendMessage(ChatColor.RED + "Error in command syntax. Check command help.");
            return true;
        }
        if ((sender instanceof Player) && !selectedBinding.checkPermissions((Player) sender)) {
            logger.warning("User " + ((Player) sender).getName() + " tried to access chat command \"" + command.getName() + " " + arguments + "\", but doesn't have permission to do this.");
            sender.sendMessage(ChatColor.RED + "Sorry, you don't have enough permissions.");
            return true;
        }
        try {
            selectedBinding.call(this.plugin, sender, selectedBinding.getParams());
            return true;
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof AutoCompleteChoicesException) {
                AutoCompleteChoicesException autocomplete = (AutoCompleteChoicesException) e.getTargetException();
                sender.sendMessage("Autocomplete for <" + autocomplete.getArgName() + ">:");
                sender.sendMessage("    " + StringUtils.implode(autocomplete.getChoices(), "   "));
                return true;
            }
            throw new RuntimeException(e.getTargetException());
        } catch (Exception e2) {
            logger.severe("Found bogus command handler for " + command.getName() + " command. (Is plugin is update?)");
            if (e2.getCause() != null) {
                e2.getCause().printStackTrace();
                return true;
            }
            e2.printStackTrace();
            return true;
        }
    }

    public List<CommandBinding> getCommands() {
        List<CommandBinding> commands = new LinkedList<>();
        for (Map<CommandSyntax, CommandBinding> map : this.listeners.values()) {
            commands.addAll(map.values());
        }
        return commands;
    }
}
