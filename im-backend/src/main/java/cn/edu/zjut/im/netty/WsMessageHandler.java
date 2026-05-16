package cn.edu.zjut.im.netty;

import cn.edu.zjut.im.entity.GroupMember;
import cn.edu.zjut.im.entity.Message;
import cn.edu.zjut.im.netty.protocol.WsMessage;
import cn.edu.zjut.im.repository.MessageRepository;
import cn.edu.zjut.im.service.GroupService;
import cn.edu.zjut.im.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class WsMessageHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final WsChannelManager channelManager;
    private final MessageRepository messageRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final GroupService groupService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.info("Channel异常, 即将关闭: channelId={}, cause={}", ctx.channel().id(), cause.getMessage());
        // 先标记下线再关闭，确保 channelInactive 拿到正确的 userId
        updateUserOffline(ctx);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.info("心跳超时，关闭连接: {}", ctx.channel().id());
                ctx.close();
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        updateUserOffline(ctx);
        channelManager.unregister(ctx.channel());
        try {
            super.channelInactive(ctx);
        } catch (Exception ignored) {
        }
    }

    private void updateUserOffline(ChannelHandlerContext ctx) {
        // 在线状态的 Redis + DB 更新统一由 WsChannelManager.unregister() 处理
        Long userId = ctx.channel().attr(ChannelAttributes.USER_ID).get();
        if (userId == null) {
            userId = channelManager.getUserId(ctx.channel());
        }
        if (userId != null) {
            log.info("连接断开: userId={}", userId);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        WsMessage msg = MessageCodec.decode(frame);
        if (msg == null) return;

        Long userId = ctx.channel().attr(ChannelAttributes.USER_ID).get();
        String username = ctx.channel().attr(ChannelAttributes.USERNAME).get();

        String type = msg.getType();
        if ("CHAT_MSG".equals(type)) {
            handleChatMessage(ctx, msg, userId);
        } else if ("MSG_READ".equals(type)) {
            handleReadReceipt(msg);
        } else if ("GROUP_MSG".equals(type)) {
            handleGroupMessage(ctx, msg, userId);
        } else if ("HEARTBEAT".equals(type)) {
            handleHeartbeat(ctx);
        } else {
            sendError(ctx, "未知消息类型: " + type);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleChatMessage(ChannelHandlerContext ctx, WsMessage msg, Long fromUserId) {
        Map<String, Object> data = (Map<String, Object>) msg.getData();
        Long toUserId = toLong(data.get("toId"));
        String content = (String) data.get("content");
        String msgType = (String) data.getOrDefault("msgType", "TEXT");

        if (toUserId == null || content == null) {
            sendError(ctx, "消息格式错误");
            return;
        }

        // 保存消息
        Message message = new Message();
        message.setId(idGenerator.nextId());
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setMsgType(msgType);

        boolean isOnline = channelManager.isOnline(toUserId);
        message.setStatus("UNREAD");
        messageRepository.save(message);

        if (!isOnline) {
            log.info("目标用户离线，消息存库: toUserId={}, content={}", toUserId, content);
        }

        // 如果对方在线，直接推送
        if (isOnline) {
            WsMessage pushMsg = WsMessage.builder()
                    .type("CHAT_MSG")
                    .data(Map.of(
                            "msgId", String.valueOf(message.getId()),
                            "fromId", fromUserId,
                            "toId", toUserId,
                            "content", content,
                            "msgType", msgType,
                            "timestamp", System.currentTimeMillis()
                    ))
                    .build();
            TextWebSocketFrame frame = MessageCodec.encode(pushMsg);
            if (frame != null) {
                boolean sent = channelManager.sendMessage(toUserId, frame);
                log.info("推送消息给在线用户: toUserId={}, sent={}", toUserId, sent);
            }
        }

        // 回执给发送者
        WsMessage ack = WsMessage.builder()
                .type("CHAT_ACK")
                .data(Map.of(
                        "msgId", String.valueOf(message.getId()),
                        "status", message.getStatus(),
                        "timestamp", System.currentTimeMillis()
                ))
                .build();
        ctx.writeAndFlush(MessageCodec.encode(ack));
    }

    @SuppressWarnings("unchecked")
    private void handleGroupMessage(ChannelHandlerContext ctx, WsMessage msg, Long fromUserId) {
        Map<String, Object> data = (Map<String, Object>) msg.getData();
        Long groupId = toLong(data.get("groupId"));
        String content = (String) data.get("content");
        String msgType = (String) data.getOrDefault("msgType", "TEXT");

        if (groupId == null || content == null) {
            sendError(ctx, "群聊消息格式错误");
            return;
        }

        Message message = new Message();
        message.setId(idGenerator.nextId());
        message.setFromUserId(fromUserId);
        message.setGroupId(groupId);
        message.setContent(content);
        message.setMsgType(msgType);
        message.setStatus("UNREAD");
        messageRepository.save(message);

        // 广播给所有在线群成员
        List<GroupMember> members = groupService.getGroupMembers(groupId);
        List<Long> onlineIds = new ArrayList<>();
        for (GroupMember member : members) {
            if (member.getUserId().equals(fromUserId)) continue;
            if (channelManager.isOnline(member.getUserId())) {
                onlineIds.add(member.getUserId());
            }
        }

        if (!onlineIds.isEmpty()) {
            WsMessage pushMsg = WsMessage.builder()
                    .type("GROUP_MSG")
                    .data(Map.of(
                            "msgId", String.valueOf(message.getId()),
                            "fromId", fromUserId,
                            "groupId", groupId,
                            "content", content,
                            "msgType", msgType,
                            "timestamp", System.currentTimeMillis()
                    ))
                    .build();
            TextWebSocketFrame frame = MessageCodec.encode(pushMsg);
            if (frame != null) {
                for (Long memberId : onlineIds) {
                    channelManager.sendMessage(memberId, frame);
                }
                log.info("群聊消息广播: groupId={}, fromUserId={}, 在线成员={}", groupId, fromUserId, onlineIds.size());
            }
        }

        WsMessage ack = WsMessage.builder()
                .type("GROUP_ACK")
                .data(Map.of(
                        "msgId", String.valueOf(message.getId()),
                        "status", "SENT",
                        "groupId", groupId,
                        "timestamp", System.currentTimeMillis()
                ))
                .build();
        ctx.writeAndFlush(MessageCodec.encode(ack));
    }

    @SuppressWarnings("unchecked")
    private void handleReadReceipt(WsMessage msg) {
        Map<String, Object> data = (Map<String, Object>) msg.getData();
        Long msgId = toLong(data.get("msgId"));
        Long readerId = toLong(data.get("readerId"));

        log.info("收到已读回执: msgId={}, readerId={}", msgId, readerId);

        if (msgId != null) {
            Message message = messageRepository.findById(msgId).orElse(null);
            if (message != null && !"READ".equals(message.getStatus())) {
                message.setStatus("READ");
                messageRepository.save(message);

                Long senderId = message.getFromUserId();
                log.info("消息已读，通知发送者: msgId={}, senderId={}", msgId, senderId);

                WsMessage readMsg = WsMessage.builder()
                        .type("MSG_READ")
                        .data(Map.of("msgId", String.valueOf(msgId), "readerId", String.valueOf(readerId)))
                        .build();
                TextWebSocketFrame frame = MessageCodec.encode(readMsg);
                if (frame != null) {
                    boolean sent = channelManager.sendMessage(senderId, frame);
                    if (!sent) {
                        log.warn("已读回执转发失败(发送者不在线): senderId={}, msgId={}", senderId, msgId);
                    }
                } else {
                    log.error("已读回执编码失败: msgId={}", msgId);
                }
            } else if (message == null) {
                log.warn("已读回执对应的消息不存在: msgId={}", msgId);
            } else {
                log.info("消息已是已读状态，跳过: msgId={}", msgId);
            }
        }
    }

    private void handleHeartbeat(ChannelHandlerContext ctx) {
        WsMessage ack = WsMessage.builder()
                .type("HEARTBEAT_ACK")
                .data(Map.of())
                .build();
        ctx.writeAndFlush(MessageCodec.encode(ack));
    }

    private void sendError(ChannelHandlerContext ctx, String message) {
        WsMessage error = WsMessage.builder()
                .type("ERROR")
                .data(Map.of("code", 400, "message", message))
                .build();
        ctx.writeAndFlush(MessageCodec.encode(error));
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Long) return (Long) value;
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
