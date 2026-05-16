package cn.edu.zjut.im.service;

import cn.edu.zjut.im.entity.Contact;
import cn.edu.zjut.im.entity.Group;
import cn.edu.zjut.im.entity.Message;
import cn.edu.zjut.im.entity.User;
import cn.edu.zjut.im.repository.ContactRepository;
import cn.edu.zjut.im.repository.MessageRepository;
import cn.edu.zjut.im.service.dto.response.ConversationResponse;
import cn.edu.zjut.im.service.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserCacheService userCacheService;
    private final ContactRepository contactRepository;
    private final OnlineStatusService onlineStatusService;
    private final GroupService groupService;

    public List<MessageResponse> pullOfflineMessages(Long userId) {
        List<Message> messages = messageRepository.findByToUserIdAndStatusOrderByCreatedAtAsc(userId, "UNREAD");
        return messages.stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    public Page<MessageResponse> getChatHistory(Long userId1, Long userId2, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Message> messages = messageRepository.findChatHistory(userId1, userId2, pageRequest);
        return messages.map(this::toMessageResponse);
    }

    public Page<MessageResponse> getGroupHistory(Long groupId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Message> messages = messageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageRequest);
        return messages.map(this::toMessageResponse);
    }

    @Transactional
    public void markAsRead(Long fromUserId, Long toUserId) {
        messageRepository.markAsRead(fromUserId, toUserId);
    }

    public List<ConversationResponse> getConversations(Long userId) {
        List<Contact> contacts = contactRepository.findByUserIdAndStatus(userId, "ACCEPTED");

        Map<Long, User> userMap = userCacheService.getUsersBatch(
                contacts.stream().map(Contact::getContactId).collect(Collectors.toSet())
        );

        java.util.Set<Long> contactIds = contacts.stream().map(Contact::getContactId).collect(Collectors.toSet());
        java.util.Set<Long> onlineIds = onlineStatusService.filterOnlineUsers(contactIds);

        List<ConversationResponse> result = new ArrayList<>();

        // 单聊会话
        for (Contact contact : contacts) {
            User contactUser = userMap.get(contact.getContactId());
            if (contactUser == null) continue;

            int unread = messageRepository.countUnread(contact.getContactId(), userId);

            PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Message> lastMsgs = messageRepository.findChatHistory(userId, contact.getContactId(), pageRequest);
            String lastContent = null;
            String lastType = null;
            java.time.LocalDateTime lastTime = null;
            if (!lastMsgs.isEmpty()) {
                Message lastMsg = lastMsgs.getContent().get(0);
                lastContent = lastMsg.getContent();
                lastType = lastMsg.getMsgType();
                lastTime = lastMsg.getCreatedAt();
            }

            result.add(ConversationResponse.builder()
                    .type("user")
                    .contactId(contactUser.getId())
                    .contactName(contact.getRemark() != null ? contact.getRemark() : contactUser.getNickname())
                    .contactAvatar(contactUser.getAvatarUrl())
                    .contactStatus(onlineIds.contains(contactUser.getId()) ? "ONLINE" : "OFFLINE")
                    .lastMsgContent(lastContent)
                    .lastMsgType(lastType)
                    .lastMsgTime(lastTime)
                    .unreadCount(unread)
                    .build());
        }

        // 群聊会话
        List<Group> groups = groupService.getUserGroups(userId);
        for (Group group : groups) {
            PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Message> lastMsgs = messageRepository.findByGroupIdOrderByCreatedAtDesc(group.getId(), pageRequest);
            String lastContent = null;
            String lastType = null;
            java.time.LocalDateTime lastTime = null;
            if (!lastMsgs.isEmpty()) {
                Message lastMsg = lastMsgs.getContent().get(0);
                lastContent = lastMsg.getContent();
                lastType = lastMsg.getMsgType();
                lastTime = lastMsg.getCreatedAt();
            }

            result.add(ConversationResponse.builder()
                    .type("group")
                    .groupId(group.getId())
                    .contactId(group.getId())
                    .contactName(group.getName())
                    .contactAvatar(group.getAvatarUrl())
                    .contactStatus(groupService.getGroupMemberCount(group.getId()) + "人")
                    .lastMsgContent(lastContent)
                    .lastMsgType(lastType)
                    .lastMsgTime(lastTime)
                    .unreadCount(0)
                    .build());
        }

        result.sort((a, b) -> {
            if (a.getLastMsgTime() == null && b.getLastMsgTime() == null) return 0;
            if (a.getLastMsgTime() == null) return 1;
            if (b.getLastMsgTime() == null) return -1;
            return b.getLastMsgTime().compareTo(a.getLastMsgTime());
        });

        return result;
    }

    public int getUnreadCount(Long userId) {
        return messageRepository.countTotalUnread(userId);
    }

    public Map<Long, String> getMessageStatuses(List<Long> ids) {
        List<Message> messages = messageRepository.findAllById(ids);
        return messages.stream().collect(Collectors.toMap(Message::getId, Message::getStatus));
    }

    private MessageResponse toMessageResponse(Message message) {
        User fromUser = userCacheService.getUser(message.getFromUserId());
        return MessageResponse.builder()
                .id(String.valueOf(message.getId()))
                .fromUserId(message.getFromUserId())
                .fromNickname(fromUser != null ? fromUser.getNickname() : "未知用户")
                .fromAvatar(fromUser != null ? fromUser.getAvatarUrl() : null)
                .toUserId(message.getToUserId())
                .groupId(message.getGroupId())
                .msgType(message.getMsgType())
                .content(message.getContent())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
