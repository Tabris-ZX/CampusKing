package zx.campusking.config;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * 后端服务端口配置。
 * 端口统一从 config/config.yaml 的 backendPort 读取，避免后端端口和前端代理端口分开维护。
 */
@Configuration
public class BackendServerConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private static final WebuiConfigFile.ServerDefaults DEFAULTS = WebuiConfigFile.loadServerDefaults();

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        factory.setPort(DEFAULTS.backendPort());
        if (!DEFAULTS.contextPath().isBlank()) {
            factory.setContextPath(DEFAULTS.contextPath());
        }
    }
}
