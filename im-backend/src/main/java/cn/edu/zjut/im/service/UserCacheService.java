package cn.edu.zjut.im.service;

import cn.edu.zjut.im.entity.User;
import cn.edu.zjut.im.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    private static final String USER_CACHE_PREFIX = "user:";
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public User getUser(Long userId) {
        String key = USER_CACHE_PREFIX + userId;
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                return MAPPER.readValue(json, User.class);
            }
        } catch (Exception e) {
            log.debug("Redis 读取用户缓存失败: userId={}", userId);
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            cacheUser(user);
        }
        return user;
    }

    public Map<Long, User> getUsersBatch(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();

        Map<Long, User> result = new HashMap<>();
        Set<String> keys = userIds.stream()
                .map(id -> USER_CACHE_PREFIX + id)
                .collect(Collectors.toSet());

        // 批量查 Redis
        try {
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            if (values != null) {
                for (String json : values) {
                    if (json != null) {
                        try {
                            User user = MAPPER.readValue(json, User.class);
                            result.put(user.getId(), user);
                        } catch (JsonProcessingException ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Redis 批量读取用户缓存失败");
        }

        // 未命中的从 DB 查
        Set<Long> missed = userIds.stream()
                .filter(id -> !result.containsKey(id))
                .collect(Collectors.toSet());

        if (!missed.isEmpty()) {
            List<User> dbUsers = userRepository.findAllById(missed);
            for (User user : dbUsers) {
                result.put(user.getId(), user);
                cacheUser(user);
            }
        }

        return result;
    }

    private void cacheUser(User user) {
        try {
            String key = USER_CACHE_PREFIX + user.getId();
            String json = MAPPER.writeValueAsString(user);
            stringRedisTemplate.opsForValue().set(key, json, USER_CACHE_TTL);
        } catch (Exception e) {
            log.debug("Redis 缓存用户失败: userId={}", user.getId());
        }
    }

    public void evictUser(Long userId) {
        try {
            stringRedisTemplate.delete(USER_CACHE_PREFIX + userId);
        } catch (Exception e) {
            log.debug("Redis 删除用户缓存失败: userId={}", userId);
        }
    }
}
