package biz.thonbecker.personal.content.infrastructure.configuration;

import biz.thonbecker.personal.content.infrastructure.client.BibleVerseHttpClient;
import biz.thonbecker.personal.content.infrastructure.client.DadJokeHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configuration for HTTP Interface clients using Spring's declarative HTTP client.
 * Creates proxy beans for external API clients backed by WebClient.
 */
@Configuration
public class HttpInterfaceConfig {

    @Bean
    public DadJokeHttpClient dadJokeHttpClient() {
        WebClient webClient =
                WebClient.builder().baseUrl("https://icanhazdadjoke.com").build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient))
                .build();

        return factory.createClient(DadJokeHttpClient.class);
    }

    @Bean
    public BibleVerseHttpClient bibleVerseHttpClient(@Value("${bible.verse.api.url}") String bibleVerseApiUrl) {
        WebClient webClient = WebClient.builder().baseUrl(bibleVerseApiUrl).build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient))
                .build();

        return factory.createClient(BibleVerseHttpClient.class);
    }
}
