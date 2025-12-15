package biz.thonbecker.personal.shared.infrastructure.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        final var cacheManager = new CaffeineCacheManager("bibleVerses", "jokeAudio");
        cacheManager.setCaffeine(
                Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).maximumSize(100));
        return cacheManager;
    }
}
