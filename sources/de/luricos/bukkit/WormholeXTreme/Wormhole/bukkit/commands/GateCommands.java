package de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.commands.Command;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/bukkit/commands/GateCommands.class */
public class GateCommands extends WormholeCommand {
    @Command(name = "wormhole", syntax = "gate info [GateName]", description = "Print gate info for selected Wormhole", permission = "")
    public void printGateInfo(Plugin plugin, CommandSender sender, Map<String, String> args) {
        sender.sendMessage("print gate info command executed");
    }
}
