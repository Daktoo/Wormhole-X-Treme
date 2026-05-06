package de.luricos.bukkit.WormholeXTreme.Wormhole.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* JADX INFO: loaded from: WormholeXTreme.jar:de/luricos/bukkit/WormholeXTreme/Wormhole/commands/Command.class */
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    String name();

    String syntax();

    String description();

    String permission() default "";

    boolean isPrimary() default false;
}
