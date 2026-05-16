package cn.edu.zjut.im.service;

import cn.edu.zjut.im.entity.Contact;
import cn.edu.zjut.im.entity.User;
import cn.edu.zjut.im.netty.MessageCodec;
import cn.edu.zjut.im.netty.WsChannelManager;
import cn.edu.zjut.im.netty.protocol.WsMessage;
import cn.edu.zjut.im.repository.ContactRepository;
import cn.edu.zjut.im.service.dto.response.PendingRequestResponse;
import cn.edu.zjut.im.service.dto.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserCacheService userCacheService;
    private final OnlineStatusService onlineStatusService;
    private final WsChannelManager wsChannelManager;

    public String addContact(Long userId, Long contactId) {
        if (userId.equals(contactId)) {
            return "不能添加自己为好友";
        }
        if (contactRepository.existsByUserIdAndContactId(userId, contactId)) {
            Contact existing = contactRepository.findByUserIdAndStatus(userId, "PENDING")
                    .stream().filter(c -> c.getContactId().equals(contactId)).findFirst().orElse(null);
            if (existing != null) {
                return "已发送好友申请，请等待对方同意";
            }
            return "已是好友";
        }

        // 创建申请记录
        Contact contact = new Contact();
        contact.setUserId(userId);
        contact.setContactId(contactId);
        contact.setStatus("PENDING");
        contactRepository.save(contact);

        log.info("用户 {} 申请添加用户 {} 为好友", userId, contactId);

        // 实时通知目标用户有新的好友申请
        User requester = userCacheService.getUser(userId);
        WsMessage notify = WsMessage.builder()
                .type("FRIEND_REQUEST")
                .data(Map.of(
                        "requestId", contact.getId(),
                        "userId", userId,
                        "nickname", requester != null ? requester.getNickname() : "未知用户",
                        "avatarUrl", requester != null ? requester.getAvatarUrl() : "",
                        "timestamp", System.currentTimeMillis()
                ))
                .build();
        io.netty.handler.codec.http.websocketx.TextWebSocketFrame frame = MessageCodec.encode(notify);
        if (frame != null) {
            wsChannelManager.sendMessage(contactId, frame);
        }

        return "好友申请已发送";
    }

    @Transactional
    public String acceptContact(Long userId, Long requestId) {
        Contact request = contactRepository.findById(requestId).orElse(null);
        if (request == null || !request.getContactId().equals(userId)) {
            return "申请不存在";
        }
        if (!"PENDING".equals(request.getStatus())) {
            return "申请已处理";
        }

        // 通过申请
        request.setStatus("ACCEPTED");
        contactRepository.save(request);

        // 创建反向好友关系（如果不存在）
        if (!contactRepository.existsByUserIdAndContactId(request.getContactId(), request.getUserId())) {
            Contact reverse = new Contact();
            reverse.setUserId(request.getContactId());
            reverse.setContactId(request.getUserId());
            reverse.setStatus("ACCEPTED");
            contactRepository.save(reverse);
        }

        log.info("用户 {} 同意了用户 {} 的好友申请", userId, request.getUserId());
        return "已添加为好友";
    }

    public String rejectContact(Long userId, Long requestId) {
        Contact request = contactRepository.findById(requestId).orElse(null);
        if (request == null || !request.getContactId().equals(userId)) {
            return "申请不存在";
        }
        request.setStatus("BLOCKED");
        contactRepository.save(request);
        return "已拒绝好友申请";
    }

    @Transactional
    public String removeContact(Long userId, Long contactId) {
        List<Contact> contacts = contactRepository.findBidirectional(userId, contactId);
        contactRepository.deleteAll(contacts);
        log.info("用户 {} 删除了好友 {}", userId, contactId);
        return "已删除好友";
    }

    public List<UserInfoResponse> getContacts(Long userId) {
        List<Contact> contacts = contactRepository.findByUserIdAndStatus(userId, "ACCEPTED");
        Set<Long> contactIds = contacts.stream().map(Contact::getContactId).collect(Collectors.toSet());
        Set<Long> onlineIds = onlineStatusService.filterOnlineUsers(contactIds);
        Map<Long, User> userMap = userCacheService.getUsersBatch(contactIds);

        return contacts.stream().map(c -> {
            User user = userMap.get(c.getContactId());
            if (user == null) return null;
            return UserInfoResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .nickname(c.getRemark() != null ? c.getRemark() : user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .status(onlineIds.contains(user.getId()) ? "ONLINE" : "OFFLINE")
                    .lastOnlineAt(user.getLastOnlineAt())
                    .build();
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<PendingRequestResponse> getPendingRequests(Long userId) {
        List<Contact> requests = contactRepository.findByContactIdAndStatus(userId, "PENDING");
        Set<Long> requesterIds = requests.stream().map(Contact::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userCacheService.getUsersBatch(requesterIds);

        return requests.stream().map(r -> {
            User requester = userMap.get(r.getUserId());
            return PendingRequestResponse.builder()
                    .id(r.getId())
                    .userId(r.getUserId())
                    .nickname(requester != null ? requester.getNickname() : "未知用户")
                    .avatarUrl(requester != null ? requester.getAvatarUrl() : null)
                    .createdAt(r.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }
}
