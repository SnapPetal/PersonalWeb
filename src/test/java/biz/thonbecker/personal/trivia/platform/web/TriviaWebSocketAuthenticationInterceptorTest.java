package biz.thonbecker.personal.trivia.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class TriviaWebSocketAuthenticationInterceptorTest {

    private final TriviaWebSocketAuthenticationInterceptor interceptor = new TriviaWebSocketAuthenticationInterceptor();

    @Test
    void rejectsUnauthenticatedQuizCommand() {
        final var message = quizMessage(null);

        assertThat(interceptor.preSend(message, messageChannel())).isNull();
    }

    @Test
    void acceptsAuthenticatedQuizCommand() {
        final var message = quizMessage(new UsernamePasswordAuthenticationToken("user-1", null));

        assertThat(interceptor.preSend(message, messageChannel())).isSameAs(message);
    }

    private Message<String> quizMessage(final java.security.Principal principal) {
        final var accessor = SimpMessageHeaderAccessor.create();
        accessor.setDestination("/app/quiz/join");
        accessor.setUser(principal);
        return MessageBuilder.createMessage("{}", accessor.getMessageHeaders());
    }

    private org.springframework.messaging.MessageChannel messageChannel() {
        return (message, timeout) -> true;
    }
}
