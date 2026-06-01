package zx.campusking.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WebuiConfigFile {

    private static final String DEFAULT_CONFIG_PATH = "config/config.yaml";
    private static final int DEFAULT_MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final String DEFAULT_CARDS_PACKAGE = "zx.campusking.cards";
    private static final int DEFAULT_BACKEND_PORT = 8080;

    private WebuiConfigFile() {
    }

    public static AssetDefaults loadAssetDefaults() {
        ParsedConfig config = loadConfig();
        if (config.isEmpty()) {
            return defaultAssetDefaults();
        }

        String publicBaseUrl = config.string("frontend.baseUrl", config.string("baseUrl", ""));
        String assetBaseUrl = config.string("asset.baseUrl", config.string("assetBaseUrl", ""));
        String assetLocalRoot = config.string("asset.localRoot", config.string("assetLocalRoot", "webui/public"));
        int assetMaxResponseBytes = parseIntOrDefault(
                config.string("asset.maxResponseBytes", config.string("assetMaxResponseBytes", "")),
                DEFAULT_MAX_RESPONSE_BYTES
        );

        return new AssetDefaults(publicBaseUrl, assetBaseUrl, assetLocalRoot, assetMaxResponseBytes);
    }

    public static ServerDefaults loadServerDefaults() {
        ParsedConfig config = loadConfig();
        if (config.isEmpty()) {
            return defaultServerDefaults();
        }

        int backendPort = parsePortOrDefault(
                config.string("server.backendPort", config.string("backendPort", "")),
                DEFAULT_BACKEND_PORT
        );

        return new ServerDefaults(backendPort);
    }

    public static CardDefaults loadCardDefaults() {
        ParsedConfig config = loadConfig();
        if (config.isEmpty()) {
            return defaultCardDefaults();
        }

        String cardsPackage = config.string("cards.package", config.string("cardsPackage", DEFAULT_CARDS_PACKAGE));

        return new CardDefaults(cardsPackage);
    }

    public static CorsDefaults loadCorsDefaults() {
        ParsedConfig config = loadConfig();
        if (config.isEmpty()) {
            return defaultCorsDefaults();
        }

        List<String> allowedOriginPatterns = config.list("cors.allowedOriginPatterns", List.of("*"));
        List<String> allowedMethods = config.list("cors.allowedMethods", List.of("GET", "POST", "OPTIONS"));
        List<String> allowedHeaders = config.list("cors.allowedHeaders", List.of("*"));
        boolean allowCredentials = parseBooleanOrDefault(config.string("cors.allowCredentials", ""), false);
        long maxAge = parseLongOrDefault(config.string("cors.maxAge", ""), 3600);

        return new CorsDefaults(allowedOriginPatterns, allowedMethods, allowedHeaders, allowCredentials, maxAge);
    }

    public static WebSocketDefaults loadWebSocketDefaults() {
        ParsedConfig config = loadConfig();
        if (config.isEmpty()) {
            return defaultWebSocketDefaults();
        }

        String gamePath = config.string("websocket.gamePath", "/ws/game");
        List<String> allowedOrigins = config.list("websocket.allowedOrigins", List.of("*"));
        return new WebSocketDefaults(gamePath, allowedOrigins);
    }

    private static ParsedConfig loadConfig() {
        Path configPath = resolveConfigPath();
        if (!Files.exists(configPath)) {
            return ParsedConfig.empty();
        }

        try {
            return parseConfig(Files.readAllLines(configPath, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            return ParsedConfig.empty();
        }
    }

    private static ParsedConfig parseConfig(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        Map<String, List<String>> lists = new HashMap<>();
        String section = "";
        String listKey = "";

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.startsWith("-")) {
                if (!listKey.isBlank()) {
                    lists.computeIfAbsent(listKey, ignored -> new ArrayList<>())
                            .add(normalizeValue(trimmed.substring(1)));
                }
                continue;
            }

            int separatorIndex = line.indexOf(':');
            if (separatorIndex <= 0) {
                continue;
            }

            String key = line.substring(0, separatorIndex).trim();
            if (key.isEmpty() || key.contains(" ")) {
                continue;
            }

            boolean topLevel = leadingSpaces(line) == 0;
            String value = normalizeValue(line.substring(separatorIndex + 1));
            String fullKey = topLevel || section.isBlank() ? key : section + "." + key;

            if (topLevel && value.isBlank()) {
                section = key;
                listKey = "";
                continue;
            }

            if (value.isBlank()) {
                listKey = fullKey;
                lists.putIfAbsent(fullKey, new ArrayList<>());
                continue;
            }

            values.put(fullKey, value);
            listKey = "";
            if (topLevel) {
                section = "";
            }
        }

        return new ParsedConfig(values, lists);
    }

    private static Path resolveConfigPath() {
        String overridePath = System.getenv("CAMPUSKING_WEBUI_CONFIG");
        if (overridePath != null && !overridePath.isBlank()) {
            return Path.of(overridePath).toAbsolutePath().normalize();
        }
        return Path.of(DEFAULT_CONFIG_PATH).toAbsolutePath().normalize();
    }

    private static AssetDefaults defaultAssetDefaults() {
        return new AssetDefaults("", "", "webui/public", DEFAULT_MAX_RESPONSE_BYTES);
    }

    private static CardDefaults defaultCardDefaults() {
        return new CardDefaults(DEFAULT_CARDS_PACKAGE);
    }

    private static ServerDefaults defaultServerDefaults() {
        return new ServerDefaults(DEFAULT_BACKEND_PORT);
    }

    private static CorsDefaults defaultCorsDefaults() {
        return new CorsDefaults(List.of("*"), List.of("GET", "POST", "OPTIONS"), List.of("*"), false, 3600);
    }

    private static WebSocketDefaults defaultWebSocketDefaults() {
        return new WebSocketDefaults("/ws/game", List.of("*"));
    }

    private static int leadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count += 1;
        }
        return count;
    }

    private static String normalizeValue(String rawValue) {
        StringBuilder builder = new StringBuilder();
        char quote = 0;
        for (int index = 0; index < rawValue.length(); index += 1) {
            char current = rawValue.charAt(index);
            if ((current == '"' || current == '\'') && (index == 0 || rawValue.charAt(index - 1) != '\\')) {
                quote = quote == current ? 0 : (quote == 0 ? current : quote);
                builder.append(current);
                continue;
            }
            if (current == '#' && quote == 0) {
                break;
            }
            builder.append(current);
        }

        String normalized = builder.toString().trim();
        if (normalized.length() >= 2) {
            char first = normalized.charAt(0);
            char last = normalized.charAt(normalized.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return normalized.substring(1, normalized.length() - 1);
            }
        }
        return normalized;
    }

    private static int parseIntOrDefault(String rawValue, int defaultValue) {
        try {
            int parsed = Integer.parseInt(rawValue);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static int parsePortOrDefault(String rawValue, int defaultValue) {
        int parsed = parseIntOrDefault(rawValue, defaultValue);
        return parsed <= 65535 ? parsed : defaultValue;
    }

    private static long parseLongOrDefault(String rawValue, long defaultValue) {
        try {
            long parsed = Long.parseLong(rawValue);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static boolean parseBooleanOrDefault(String rawValue, boolean defaultValue) {
        if ("true".equalsIgnoreCase(rawValue)) {
            return true;
        }
        if ("false".equalsIgnoreCase(rawValue)) {
            return false;
        }
        return defaultValue;
    }

    private record ParsedConfig(Map<String, String> values, Map<String, List<String>> lists) {

        private static ParsedConfig empty() {
            return new ParsedConfig(Map.of(), Map.of());
        }

        private boolean isEmpty() {
            return values.isEmpty() && lists.isEmpty();
        }

        private String string(String key, String defaultValue) {
            String value = values.get(key);
            return value == null || value.isBlank() ? defaultValue : value;
        }

        private List<String> list(String key, List<String> defaultValue) {
            List<String> value = lists.get(key);
            return value == null || value.isEmpty() ? defaultValue : List.copyOf(value);
        }
    }

    public record AssetDefaults(String publicBaseUrl, String baseUrl, String localRoot, int maxResponseBytes) {
    }

    public record CardDefaults(String cardsPackage) {
    }

    public record ServerDefaults(int backendPort) {
    }

    public record CorsDefaults(
            List<String> allowedOriginPatterns,
            List<String> allowedMethods,
            List<String> allowedHeaders,
            boolean allowCredentials,
            long maxAge
    ) {
    }

    public record WebSocketDefaults(String gamePath, List<String> allowedOrigins) {
    }
}
