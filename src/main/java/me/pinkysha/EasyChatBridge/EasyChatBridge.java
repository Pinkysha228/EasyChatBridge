package me.pinkysha.EasyChatBridge;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Receives a notification (arbitrary byte data; the format is defined by the
 * backend plugin sending the message) from one server in the network through
 * a custom plugin messaging channel and forwards it as-is to all other
 * connected backend servers without inspecting or modifying its contents.
 *
 * Unlike the standard bungeecord:main "Forward ALL" mechanism, which can also
 * distribute packets to all servers but is transparently proxied by Velocity
 * itself, this implementation allows the packet to be intercepted through
 * PluginMessageEvent and handled explicitly by the proxy plugin. This makes it
 * possible to apply filtering, logging, and server exclusion rules.
 */
@Plugin(
        id = "easychatbridge",
        name = "EasyChatBridge",
        version = "1.0.0",
        description = "Forwards notifications from one backend server to all other servers in the network",
        authors = {"Pinkysha"}
)
public final class EasyChatBridge {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private RelayConfig config;
    private ChannelIdentifier channel;

    @Inject
    public EasyChatBridge(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        reload();
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("notifyrelay").plugin(this).build(),
                new ReloadCommand(this)
        );
        logger.info("NotifyRelay enabled, channel: {}", config.channel());
    }

    void reload() {
        config = RelayConfig.loadOrCreate(dataDirectory, logger);

        // Re-register the channel if its name has been changed in the configuration.
        if (channel != null) {
            server.getChannelRegistrar().unregister(channel);
        }
        channel = MinecraftChannelIdentifier.from(config.channel());
        server.getChannelRegistrar().register(channel);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channel)) {
            return; // Ignore messages sent through channels not managed by this plugin.
        }
        if (!(event.getSource() instanceof ServerConnection origin)) {
            return; // Ignore messages originating from clients or other non-backend sources.
        }

        // Mark the event as handled to prevent Velocity from forwarding the message
        // to the player using its default forwarding mechanism. This is an
        // inter-server message and is not intended to be delivered to the client.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        RegisteredServer originServer = origin.getServer();
        byte[] data = event.getData();
        int sent = 0;

        for (RegisteredServer target : server.getAllServers()) {
            if (target.equals(originServer)) continue;
            if (config.isExcluded(target.getServerInfo().getName())) continue;

            boolean ok = target.sendPluginMessage(channel, data);
            if (ok) {
                sent++;
            } else if (config.logForwards()) {
                logger.warn("[notify] {} -> {}: failed to deliver packet; no players are currently online on the target server",
                        originServer.getServerInfo().getName(), target.getServerInfo().getName());
            }
        }

        if (config.logForwards()) {
            logger.info("[notify] {} -> forwarded to {} server(s) ({} bytes)",
                    originServer.getServerInfo().getName(), sent, data.length);
        }
    }
}