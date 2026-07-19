package biz.thonbecker.personal.trivia.platform.web;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
class TriviaWebSocketAuthenticationInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final var accessor = SimpMessageHeaderAccessor.wrap(message);
        final var destination = accessor.getDestination();
        if (destination != null && destination.startsWith("/app/quiz/") && accessor.getUser() == null) {
            return null;
        }
        return message;
    }
}
