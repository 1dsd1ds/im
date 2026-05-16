package cn.edu.zjut.im.service;

import cn.edu.zjut.im.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    public void blacklist(String token) {
        try {
            long ttlSeconds = jwtUtil.getRemainingTtlSeconds(token);
            if (ttlSeconds <= 0) return;
            String hash = DigestUtils.md5DigestAsHex(token.getBytes(StandardCharsets.UTF_8));
            stringRedisTemplate.opsForValue()
                    .set(BLACKLIST_PREFIX + hash, "", Duration.ofSeconds(ttlSeconds));
            log.info("Token 已加入黑名单, TTL={}s", ttlSeconds);
        } catch (Exception e) {
            log.warn("Token 加入黑名单失败", e);
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            String hash = DigestUtils.md5DigestAsHex(token.getBytes(StandardCharsets.UTF_8));
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + hash));
        } catch (Exception e) {
            log.warn("Redis 不可用，Token 黑名单检查跳过");
            return false; // fail-open
        }
    }
}
