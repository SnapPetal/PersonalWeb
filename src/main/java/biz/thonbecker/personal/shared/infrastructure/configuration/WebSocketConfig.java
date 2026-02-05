package biz.thonbecker.personal.shared.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to send messages to clients
        config.enableSimpleBroker("/topic");
        // Messages with /app prefix will be routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(5 * 1024 * 1024); // 5MB for image frame payloads
        registration.setSendBufferSizeLimit(5 * 1024 * 1024);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the /quiz-websocket endpoint with SockJS fallback
        registry.addEndpoint("/quiz-websocket").setAllowedOriginPatterns("*").withSockJS();
        registry.addEndpoint("/skatetricks-websocket")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
