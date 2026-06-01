package zx.campusking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campusking.asset")
public class AssetProperties {

    private String baseUrl = "";
    private String localRoot = "webui/public";
    private int maxResponseBytes = 1024 * 1024;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    public String getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(String localRoot) {
        this.localRoot = localRoot == null || localRoot.isBlank() ? "webui/public" : localRoot.trim();
    }

    public int getMaxResponseBytes() {
        return maxResponseBytes;
    }

    public void setMaxResponseBytes(int maxResponseBytes) {
        this.maxResponseBytes = maxResponseBytes > 0 ? maxResponseBytes : 1024 * 1024;
    }
}
