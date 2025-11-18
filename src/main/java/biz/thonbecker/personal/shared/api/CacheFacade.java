package biz.thonbecker.personal.shared.api;

import java.util.Optional;
import org.springframework.modulith.NamedInterface;

/**
 * Public API for cache operations across modules.
 * This facade provides a consistent caching interface for all modules.
 */
@NamedInterface("CacheOperations")
public interface CacheFacade {

    /**
     * Retrieve a value from the cache.
     *
     * @param key the cache key
     * @param type the expected type of the cached value
     * @param <T> the type parameter
     * @return the cached value if present
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Store a value in the cache.
     *
     * @param key the cache key
     * @param value the value to cache
     */
    void put(String key, Object value);

    /**
     * Invalidate a cache entry.
     *
     * @param key the cache key to invalidate
     */
    void invalidate(String key);

    /**
     * Invalidate all cache entries matching a pattern.
     *
     * @param pattern the key pattern (supports wildcards)
     */
    void invalidatePattern(String pattern);

    /**
     * Clear all cache entries.
     */
    void clear();
}
