package me.pinkysha.EasyChatBridge;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Uses java.util.Properties instead of YAML or TOML to avoid introducing
 * an additional dependency for a configuration containing only a few settings.
 */
final class RelayConfig {
    private static final String FILE_NAME = "config.properties";

    private final String channel;
    private final Set<String> excludedServers;
    private final boolean logForwards;

    private RelayConfig(String channel, Set<String> excludedServers, boolean logForwards) {
        this.channel = channel;
        this.excludedServers = excludedServers;
        this.logForwards = logForwards;
    }

    static RelayConfig loadOrCreate(Path dataDirectory, Logger logger) {
        Path path = dataDirectory.resolve(FILE_NAME);
        Properties props = new Properties();
        try {
            Files.createDirectories(dataDirectory);
            if (Files.exists(path)) {
                try (var in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    props.load(in);
                }
            } else {
                writeDefaults(path);
                props.setProperty("channel", "easychat:notify");
                props.setProperty("excluded-servers", "");
                props.setProperty("log-forwards", "true");
            }
        } catch (IOException e) {
            logger.warn("Failed to read or create {}, using default values: {}",
                    FILE_NAME, e.getMessage());
        }

        String channel = props.getProperty("channel", "easychat:notify").trim();
        boolean log = Boolean.parseBoolean(props.getProperty("log-forwards", "true").trim());
        Set<String> excluded = Arrays.stream(props.getProperty("excluded-servers", "").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        return new RelayConfig(channel, excluded, log);
    }

    private static void writeDefaults(Path path) throws IOException {
        String content = """
                # Namespaced plugin messaging channel. This value must match the channel
                # used by the sending plugin on the backend server (format: "namespace:key").
                channel=easychat:notify

                # Server IDs (as defined in velocity.toml) that should NOT receive notifications.
                # Specify multiple server IDs as a comma-separated list, for example: lobby,build
                excluded-servers=

                # Whether to log one entry to the proxy console for each forwarded message.
                log-forwards=true
                """;
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    String channel() {
        return channel;
    }

    boolean isExcluded(String serverName) {
        return excludedServers.contains(serverName);
    }

    boolean logForwards() {
        return logForwards;
    }
}