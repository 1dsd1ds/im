package cn.edu.zjut.im.service;

import cn.edu.zjut.im.entity.User;
import cn.edu.zjut.im.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineStatusService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    private static final String ONLINE_USERS_KEY = "online_users";

    public void markOnline(Long userId) {
        try {
            stringRedisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());
        } catch (Exception e) {
            log.warn("Redis 标记上线失败: userId={}", userId, e);
        }
        // 异步同步 DB
        CompletableFuture.runAsync(() -> {
            try {
                userRepository.findById(userId).ifPresent(user -> {
                    user.setStatus("ONLINE");
                    user.setLastOnlineAt(LocalDateTime.now());
                    userRepository.save(user);
                });
            } catch (Exception e) {
                log.error("DB 同步上线状态失败: userId={}", userId, e);
            }
        });
    }

    public void markOffline(Long userId) {
        try {
            stringRedisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.toString());
        } catch (Exception e) {
            log.warn("Redis 标记下线失败: userId={}", userId, e);
        }
        // 异步同步 DB
        CompletableFuture.runAsync(() -> {
            try {
                userRepository.findById(userId).ifPresent(user -> {
                    user.setStatus("OFFLINE");
                    user.setLastOnlineAt(LocalDateTime.now());
                    userRepository.save(user);
                });
            } catch (Exception e) {
                log.error("DB 同步下线状态失败: userId={}", userId, e);
            }
        });
    }

    public boolean isOnline(Long userId) {
        try {
            Boolean member = stringRedisTemplate.opsForSet()
                    .isMember(ONLINE_USERS_KEY, userId.toString());
            return Boolean.TRUE.equals(member);
        } catch (Exception e) {
            log.warn("Redis 不可用，回退到 DB 查在线状态: userId={}", userId);
        }
        return userRepository.findById(userId)
                .map(u -> "ONLINE".equals(u.getStatus()))
                .orElse(false);
    }

    public Set<Long> filterOnlineUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptySet();
        try {
            Set<String> onlineStrs = stringRedisTemplate.opsForSet().members(ONLINE_USERS_KEY);
            if (onlineStrs != null) {
                Set<Long> online = onlineStrs.stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toSet());
                online.retainAll(userIds);
                return online;
            }
            return Collections.emptySet();
        } catch (Exception e) {
            log.warn("Redis 不可用，回退到 DB 批量查在线状态");
        }
        // Redis 不可用时回退 DB
        return userIds.stream()
                .filter(id -> userRepository.findById(id)
                        .map(u -> "ONLINE".equals(u.getStatus()))
                        .orElse(false))
                .collect(Collectors.toSet());
    }

    @Scheduled(fixedDelay = 60000)
    public void syncOnlineUsersToDb() {
        try {
            Set<String> onlineIds = stringRedisTemplate.opsForSet().members(ONLINE_USERS_KEY);
            if (onlineIds == null || onlineIds.isEmpty()) return;
            for (String id : onlineIds) {
                try {
                    Long userId = Long.parseLong(id);
                    userRepository.findById(userId).ifPresent(user -> {
                        if (!"ONLINE".equals(user.getStatus())) {
                            user.setStatus("ONLINE");
                            userRepository.save(user);
                        }
                    });
                } catch (Exception e) {
                    log.warn("定时同步在线状态失败: userId={}", id, e);
                }
            }
        } catch (Exception e) {
            log.debug("Redis 不可用，跳过定时同步");
        }
    }
}
