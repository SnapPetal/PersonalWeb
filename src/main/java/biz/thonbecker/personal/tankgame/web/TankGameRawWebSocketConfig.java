package biz.thonbecker.personal.tankgame.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
class TankGameRawWebSocketConfig implements WebSocketConfigurer {

    private final TankGameRawWebSocketHandler handler;

    TankGameRawWebSocketConfig() {
        handler = new TankGameRawWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/tankgame-ws").setAllowedOriginPatterns("*");
    }
}
