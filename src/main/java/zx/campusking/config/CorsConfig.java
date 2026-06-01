package zx.campusking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final WebuiConfigFile.CorsDefaults DEFAULTS = WebuiConfigFile.loadCorsDefaults();

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(DEFAULTS.allowedOriginPatterns().toArray(String[]::new))
                .allowedMethods(DEFAULTS.allowedMethods().toArray(String[]::new))
                .allowedHeaders(DEFAULTS.allowedHeaders().toArray(String[]::new))
                .allowCredentials(DEFAULTS.allowCredentials())
                .maxAge(DEFAULTS.maxAge());
    }
}
