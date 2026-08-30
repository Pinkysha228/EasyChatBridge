# EasyChatBridge

A Velocity plugin for the [EasyChat](https://github.com/Pinkysha228/EasyChat) network. It receives a notification (raw bytes; the payload format is not inspected) from one backend server on the network and relays it to every other connected backend server.

This role used to be handled by Velocity's built-in BungeeCord forwarding (`bungeecord:main` → `Forward`/`ALL`). That mechanism still works, but it is entirely opaque — if a message fails to arrive, there is no way to tell why. EasyChatBridge performs the same job through a dedicated channel and a real `PluginMessageEvent` on the proxy, which makes the relay observable, lets you exclude specific servers from delivery, and removes the dependency on undocumented proxy behavior.

## How it works

A backend server sends bytes on the `easychat:notify` channel, the same way it would send any other plugin message from a player. The plugin listens for this on the proxy, identifies the originating server, and forwards the same bytes to every other registered server except the origin and any server on the exclusion list.

**Known limitation:** this is a limitation of Velocity's plugin messaging in general, not of this plugin. A plugin message can only be delivered to a backend server through an already-established connection belonging to some player on that server. If the target server has no players online at the moment of delivery, the packet has nothing to travel through and is silently dropped. This applies to any proxy-to-backend plugin messaging, not just this plugin.

## Installation

Build with `mvn clean package`, drop the resulting `.jar` into your Velocity `plugins/` folder, and restart the proxy. Every backend server on the network needs a plugin capable of sending and receiving data on the same channel (for example, EasyChat with a patched `NetworkBridge`).

## Configuration

Generated on first run at `plugins/easychatbridge/config.properties`.

```properties
# Namespaced plugin messaging channel. Must match the channel used by the
# sending plugin on the backend server (format must be "namespace:key")
channel=easychat:notify

# Comma-separated list of server IDs (as defined in velocity.toml) that
# should NOT receive relayed notifications, e.g.: lobby,build
excluded-servers=

# Whether to log a line to the proxy console for every relayed message
log-forwards=true
```

## Commands

| Command | Description | Permission |
|---|---|---|
| `/easychatbridge reload` | Reloads the configuration without restarting the proxy | `easychat.velocity.reload` |

## Requirements

- Velocity 3.4.0 or newer
- Java 17 or newer

## License

MIT
