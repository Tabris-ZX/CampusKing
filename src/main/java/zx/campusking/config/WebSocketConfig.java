package zx.campusking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import zx.campusking.websocket.GameWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final WebuiConfigFile.WebSocketDefaults DEFAULTS = WebuiConfigFile.loadWebSocketDefaults();

    private final GameWebSocketHandler gameWebSocketHandler;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, DEFAULTS.gamePath())
                .setAllowedOrigins(DEFAULTS.allowedOrigins().toArray(String[]::new));
    }
}
