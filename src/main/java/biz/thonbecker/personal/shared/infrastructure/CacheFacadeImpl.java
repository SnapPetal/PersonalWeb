package biz.thonbecker.personal.shared.infrastructure;

import biz.thonbecker.personal.shared.api.CacheFacade;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
class CacheFacadeImpl implements CacheFacade {

    private static final String DEFAULT_CACHE_NAME = "default";
    private final CacheManager cacheManager;

    public CacheFacadeImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        Cache cache = cacheManager.getCache(DEFAULT_CACHE_NAME);
        if (cache == null) {
            return Optional.empty();
        }

        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper == null) {
            return Optional.empty();
        }

        Object value = wrapper.get();
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }

        return Optional.empty();
    }

    @Override
    public void put(String key, Object value) {
        Cache cache = cacheManager.getCache(DEFAULT_CACHE_NAME);
        if (cache != null) {
            cache.put(key, value);
            log.debug("Cached value for key: {}", key);
        }
    }

    @Override
    public void invalidate(String key) {
        Cache cache = cacheManager.getCache(DEFAULT_CACHE_NAME);
        if (cache != null) {
            cache.evict(key);
            log.debug("Invalidated cache for key: {}", key);
        }
    }

    @Override
    public void invalidatePattern(String pattern) {
        Cache cache = cacheManager.getCache(DEFAULT_CACHE_NAME);
        if (cache != null) {
            // Caffeine doesn't support pattern eviction directly
            // For production, consider using Redis with pattern support
            cache.clear();
            log.warn("Pattern invalidation not fully supported, cleared entire cache");
        }
    }

    @Override
    public void clear() {
        Cache cache = cacheManager.getCache(DEFAULT_CACHE_NAME);
        if (cache != null) {
            cache.clear();
            log.info("Cache cleared");
        }
    }
}
