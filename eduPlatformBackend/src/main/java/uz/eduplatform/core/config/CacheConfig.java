package uz.eduplatform.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(1000));

        // Register per-cache configurations
        Map<String, Caffeine<Object, Object>> cacheBuilders = Map.of(
                "subjects", caffeineBuilder(Duration.ofHours(1), 500),
                "topics", caffeineBuilder(Duration.ofHours(1), 500),
                "questions", caffeineBuilder(Duration.ofMinutes(15), 2000),
                "dashboard_stats", caffeineBuilder(Duration.ofMinutes(5), 100),
                "available_questions", caffeineBuilder(Duration.ofMinutes(10), 500),
                "content_stats", caffeineBuilder(Duration.ofMinutes(10), 200),
                "system_info", caffeineBuilder(Duration.ofMinutes(2), 50),
                "activePlans", caffeineBuilder(Duration.ofHours(1), 50),
                "plans", caffeineBuilder(Duration.ofHours(1), 100)
        );

        cacheBuilders.forEach((name, builder) ->
                cacheManager.registerCustomCache(name,
                        builder.build()));

        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineBuilder(Duration ttl, long maxSize) {
        return Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize);
    }
}
