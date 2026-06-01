package zx.campusking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campusking.asset")
public class AssetProperties {

    private static final WebuiConfigFile.AssetDefaults DEFAULTS = WebuiConfigFile.loadAssetDefaults();

    private String baseUrl = DEFAULTS.baseUrl();
    private String publicBaseUrl = DEFAULTS.publicBaseUrl();
    private String localRoot = DEFAULTS.localRoot();
    private int maxResponseBytes = DEFAULTS.maxResponseBytes();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? DEFAULTS.baseUrl() : baseUrl.trim();
    }

    public String getLocalRoot() {
        return localRoot;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setLocalRoot(String localRoot) {
        this.localRoot = localRoot == null || localRoot.isBlank() ? DEFAULTS.localRoot() : localRoot.trim();
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl == null || publicBaseUrl.isBlank() ? DEFAULTS.publicBaseUrl() : publicBaseUrl.trim();
    }

    public int getMaxResponseBytes() {
        return maxResponseBytes;
    }

    public void setMaxResponseBytes(int maxResponseBytes) {
        this.maxResponseBytes = maxResponseBytes > 0 ? maxResponseBytes : DEFAULTS.maxResponseBytes();
    }
}
