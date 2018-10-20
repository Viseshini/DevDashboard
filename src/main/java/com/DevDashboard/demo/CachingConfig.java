package com.DevDashboard.demo;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration class for the caching done by Spring
 * @author ViseshiniReddy
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CachingConfig {

    /**
     * Sets cache up before the application starts running
     * @return a cacheManager for devListCache
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("devListCache");
    }

    /**
     * Erases the cache in 1 hour, fixedDelay is in milliseconds
     * Rate Limit is reset after an hour
     */
    @CacheEvict(allEntries = true, value = "devListCache")
    @Scheduled(fixedDelay = 1 * 60 * 60 * 1000)
    public void reportCacheEvict() {
        //FOR TESTING
        //System.out.println("devListCache flushed");
    }
}
