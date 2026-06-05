package zx.campusking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final WebuiConfigFile.CorsDefaults DEFAULTS = WebuiConfigFile.loadCorsDefaults();

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        addMapping(registry, "/game/**");
        addMapping(registry, "/assets/**");
    }

    private void addMapping(CorsRegistry registry, String pathPattern) {
        registry.addMapping(pathPattern)
          .allowedOriginPatterns(DEFAULTS.allowedOriginPatterns().toArray(String[]::new))
          .allowedMethods(DEFAULTS.allowedMethods().toArray(String[]::new))
          .allowedHeaders(DEFAULTS.allowedHeaders().toArray(String[]::new))
          .allowCredentials(DEFAULTS.allowCredentials())
          .maxAge(DEFAULTS.maxAge());
    }
}
