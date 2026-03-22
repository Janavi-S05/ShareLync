package com.project.filesharingapp.asset.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CacheEvictionService {

    @CacheEvict(value = "filesByUser", key = "#userId")
    public void evictFileListCache(String userId) {
        log.info("Evicted filesByUser cache for {}", userId);
    }
}
