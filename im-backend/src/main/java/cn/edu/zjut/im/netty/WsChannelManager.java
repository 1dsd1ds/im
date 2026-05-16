package cn.edu.zjut.im.netty;

import cn.edu.zjut.im.entity.Contact;
import cn.edu.zjut.im.netty.protocol.WsMessage;
import cn.edu.zjut.im.repository.ContactRepository;
import cn.edu.zjut.im.service.OnlineStatusService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsChannelManager {

    private final OnlineStatusService onlineStatusService;
    private final ContactRepository contactRepository;

    // userId -> Channel
    private final ConcurrentHashMap<Long, Channel> userChannelMap = new ConcurrentHashMap<>();

    // ChannelId -> userId
    private final ConcurrentHashMap<ChannelId, Long> channelUserMap = new ConcurrentHashMap<>();

    public void register(Long userId, Channel channel) {
        Channel oldChannel = userChannelMap.put(userId, channel);
        if (oldChannel != null && oldChannel != channel) {
            oldChannel.close();
        }
        channelUserMap.put(channel.id(), userId);
        // Redis 标记上线 + 异步同步 DB
        onlineStatusService.markOnline(userId);
        log.info("用户上线: userId={}, 当前在线: {}", userId, userChannelMap.size());
        // 通知所有好友自己上线了
        notifyFriendsStatus(userId, "USER_ONLINE");
    }

    public void unregister(Channel channel) {
        Long userId = channelUserMap.remove(channel.id());
        if (userId != null) {
            // 仅当 channel 仍是当前活跃连接时才移除，避免误删新连接
            boolean removed = userChannelMap.remove(userId, channel);
            if (removed) {
                // Redis 标记下线 + 异步同步 DB
                onlineStatusService.markOffline(userId);
                log.info("用户下线: userId={}, 当前在线: {}", userId, userChannelMap.size());
                // 通知所有好友自己下线了
                notifyFriendsStatus(userId, "USER_OFFLINE");
            } else {
                log.info("用户旧连接关闭(已被新连接替换，保持在线): userId={}", userId);
            }
        }
    }

    private void notifyFriendsStatus(Long userId, String statusType) {
        try {
            List<Contact> contacts = contactRepository.findByContactIdAndStatus(userId, "ACCEPTED");
            for (Contact contact : contacts) {
                Long friendId = contact.getUserId();
                WsMessage statusMsg = WsMessage.builder()
                        .type(statusType)
                        .data(Map.of("userId", userId, "timestamp", System.currentTimeMillis()))
                        .build();
                TextWebSocketFrame frame = MessageCodec.encode(statusMsg);
                if (frame != null) {
                    sendMessage(friendId, frame);
                }
            }
        } catch (Exception e) {
            log.warn("通知好友状态变更失败: userId={}", userId, e);
        }
    }

    public Channel getChannel(Long userId) {
        return userChannelMap.get(userId);
    }

    public boolean isOnline(Long userId) {
        return userChannelMap.containsKey(userId) && userChannelMap.get(userId).isActive();
    }

    public boolean sendMessage(Long userId, TextWebSocketFrame frame) {
        Channel channel = userChannelMap.get(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(frame).addListener(f -> {
                if (!f.isSuccess()) {
                    log.error("消息推送失败: userId={}", userId, f.cause());
                }
            });
            return true;
        }
        return false;
    }

    public Set<Long> getOnlineUsers() {
        return userChannelMap.keySet();
    }

    public int getOnlineCount() {
        return userChannelMap.size();
    }

    public Long getUserId(Channel channel) {
        return channelUserMap.get(channel.id());
    }

    public boolean kickUser(Long userId) {
        Channel channel = userChannelMap.remove(userId);
        if (channel != null) {
            channelUserMap.remove(channel.id());
            channel.close();
            return true;
        }
        return false;
    }
}
