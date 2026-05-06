package de.luricos.bukkit.WormholeXTreme.Wormhole.commands;

import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionManager;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/commands/CommandBinding.class */
public class CommandBinding {
    protected Object object;
    protected Method method;
    protected Map<String, String> params = new HashMap();

    public CommandBinding(Object object, Method method) {
        this.object = object;
        this.method = method;
    }

    public Command getMethodAnnotation() {
        return (Command) this.method.getAnnotation(Command.class);
    }

    public Map<String, String> getParams() {
        return this.params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public boolean checkPermissions(Player player) {
        PermissionManager manager = WormholeXTreme.getPermissionManager();
        String permission = getMethodAnnotation().permission();
        if (permission.contains("<")) {
            for (Map.Entry<String, String> entry : getParams().entrySet()) {
                if (entry.getValue() != null) {
                    permission = permission.replace("<" + entry.getKey() + ">", entry.getValue().toLowerCase());
                }
            }
        }
        return manager.has(player, permission);
    }

    public void call(Object... args) throws Exception {
        this.method.invoke(this.object, args);
    }
}
