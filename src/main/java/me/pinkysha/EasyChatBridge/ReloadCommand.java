package me.pinkysha.EasyChatBridge;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class ReloadCommand implements SimpleCommand {
    private final EasyChatBridge plugin;

    ReloadCommand(EasyChatBridge plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        plugin.reload();
        invocation.source().sendMessage(Component.text("easychatbridge: config reloaded", NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("easychat.velocity.reload");
    }
}
