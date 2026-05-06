package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.commands.CommandListener;
import de.luricos.bukkit.WormholeXTreme.Wormhole.commands.CommandManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.commands.exceptions.AutoCompleteChoicesException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/commands/WormholeCommand.class */
public abstract class WormholeCommand implements CommandListener {
    protected static final Logger logger = Bukkit.getLogger();
    protected CommandManager manager;

    @Override // de.luricos.bukkit.WormholeXTreme.Wormhole.commands.CommandListener
    public void onRegistered(CommandManager manager) {
        this.manager = manager;
    }

    protected void informPlayer(Plugin plugin, String playerName, String message) {
        Player player = Bukkit.getServer().getPlayer(playerName);
        if (player == null) {
            return;
        }
        player.sendMessage(ChatColor.BLUE + "[WormholeXTreme] " + ChatColor.WHITE + message);
    }

    protected String autoCompletePlayerName(String playerName) {
        return autoCompletePlayerName(playerName, "user");
    }

    protected String autoCompletePlayerName(String playerName, String argName) {
        if (playerName == null) {
            return null;
        }
        if (playerName.startsWith("#")) {
            return playerName.substring(1);
        }
        List<String> players = new LinkedList<>();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                return player.getName();
            }
            if (player.getName().toLowerCase().startsWith(playerName.toLowerCase()) && !players.contains(player.getName())) {
                players.add(player.getName());
            }
        }
        if (players.size() > 1) {
            throw new AutoCompleteChoicesException((String[]) players.toArray(new String[0]), argName);
        }
        if (players.size() == 1) {
            return players.get(0);
        }
        return playerName;
    }

    protected String getSenderName(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getName();
        }
        return "console";
    }

    protected String autoCompleteWorldName(String worldName) {
        return autoCompleteWorldName(worldName, "world");
    }

    protected String autoCompleteWorldName(String worldName, String argName) {
        if (worldName == null || worldName.isEmpty() || "*".equals(worldName)) {
            return null;
        }
        List<String> worlds = new LinkedList<>();
        for (World world : Bukkit.getServer().getWorlds()) {
            if (world.getName().equalsIgnoreCase(worldName)) {
                return world.getName();
            }
            if (world.getName().toLowerCase().startsWith(worldName.toLowerCase()) && !worlds.contains(world.getName())) {
                worlds.add(world.getName());
            }
        }
        if (worlds.size() > 1) {
            throw new AutoCompleteChoicesException((String[]) worlds.toArray(new String[0]), argName);
        }
        if (worlds.size() == 1) {
            return worlds.get(0);
        }
        return worldName;
    }

    protected String getSafeWorldName(String worldName, String userName) {
        if (worldName == null) {
            Player player = Bukkit.getServer().getPlayer(userName);
            if (player != null) {
                worldName = player.getWorld().getName();
            } else {
                worldName = ((World) Bukkit.getServer().getWorlds().get(0)).getName();
            }
        }
        return worldName;
    }

    protected int getPosition(String permission, String[] permissions) {
        try {
            int position = Integer.parseInt(permission) - 1;
            if (position < 0 || position >= permissions.length) {
                throw new RuntimeException("Wrong permission index specified!");
            }
            return position;
        } catch (NumberFormatException e) {
            for (int i = 0; i < permissions.length; i++) {
                if (permission.equalsIgnoreCase(permissions[i])) {
                    return i;
                }
            }
            throw new RuntimeException("Specified permission not found");
        }
    }

    protected Object parseValue(String value) {
        if (value == null) {
            return null;
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.valueOf(Boolean.parseBoolean(value));
        }
        try {
            return Integer.valueOf(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            try {
                return Double.valueOf(Double.parseDouble(value));
            } catch (NumberFormatException e2) {
                return value;
            }
        }
    }

    protected void sendMessage(CommandSender sender, String message) {
        String[] arr$ = message.split("\n");
        for (String messagePart : arr$) {
            sender.sendMessage(messagePart);
        }
    }
}
