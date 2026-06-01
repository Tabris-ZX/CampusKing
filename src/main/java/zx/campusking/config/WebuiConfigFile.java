package zx.campusking.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class WebuiConfigFile {

    private static final String DEFAULT_CONFIG_PATH = "config/config.yaml";
    private static final int DEFAULT_MAX_RESPONSE_BYTES = 1024 * 1024;

    private WebuiConfigFile() {
    }

    public static AssetDefaults loadAssetDefaults() {
        Path configPath = resolveConfigPath();
        if (!Files.exists(configPath)) {
            return defaultAssetDefaults();
        }

        String assetBaseUrl = "";
        String assetLocalRoot = "webui/public";
        int assetMaxResponseBytes = DEFAULT_MAX_RESPONSE_BYTES;

        try {
            List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
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

                String value = normalizeValue(line.substring(separatorIndex + 1));
                switch (key) {
                    case "assetBaseUrl" -> assetBaseUrl = value;
                    case "assetLocalRoot" -> assetLocalRoot = value.isBlank() ? "webui/public" : value;
                    case "assetMaxResponseBytes" -> assetMaxResponseBytes = parseIntOrDefault(value, DEFAULT_MAX_RESPONSE_BYTES);
                    default -> {
                        // Ignore unrelated keys so one file can serve both frontend and backend.
                    }
                }
            }
        } catch (IOException ignored) {
            return defaultAssetDefaults();
        }

        return new AssetDefaults(assetBaseUrl, assetLocalRoot, assetMaxResponseBytes);
    }

    private static Path resolveConfigPath() {
        String overridePath = System.getenv("CAMPUSKING_WEBUI_CONFIG");
        if (overridePath != null && !overridePath.isBlank()) {
            return Path.of(overridePath).toAbsolutePath().normalize();
        }
        return Path.of(DEFAULT_CONFIG_PATH).toAbsolutePath().normalize();
    }

    private static AssetDefaults defaultAssetDefaults() {
        return new AssetDefaults("", "webui/public", DEFAULT_MAX_RESPONSE_BYTES);
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

    public record AssetDefaults(String baseUrl, String localRoot, int maxResponseBytes) {
    }
}
