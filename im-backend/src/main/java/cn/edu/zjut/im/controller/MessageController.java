package cn.edu.zjut.im.controller;

import cn.edu.zjut.im.security.JwtUtil;
import cn.edu.zjut.im.service.MessageService;
import cn.edu.zjut.im.service.dto.response.ApiResponse;
import cn.edu.zjut.im.service.dto.response.ConversationResponse;
import cn.edu.zjut.im.service.dto.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "消息", description = "离线消息/历史消息/会话列表")
public class MessageController {

    private final MessageService messageService;
    private final JwtUtil jwtUtil;

    private Long getUserId(String authHeader) {
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserIdFromToken(token);
    }

    @GetMapping("/offline")
    @Operation(summary = "拉取离线消息")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> pullOffline(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        List<MessageResponse> messages = messageService.pullOfflineMessages(userId);
        return ResponseEntity.ok(ApiResponse.<List<MessageResponse>>builder()
                .success(true).message("拉取成功").data(messages).build());
    }

    @GetMapping("/history/{contactId}")
    @Operation(summary = "获取与某联系人的历史消息（分页）")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getHistory(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long contactId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(authHeader);
        Page<MessageResponse> messages = messageService.getChatHistory(userId, contactId, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<MessageResponse>>builder()
                .success(true).message("查询成功").data(messages).build());
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "获取群聊历史消息（分页）")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getGroupHistory(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<MessageResponse> messages = messageService.getGroupHistory(groupId, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<MessageResponse>>builder()
                .success(true).message("查询成功").data(messages).build());
    }

    @PostMapping("/read")
    @Operation(summary = "标记某联系人的所有消息已读")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Long> body) {
        Long userId = getUserId(authHeader);
        Long contactId = body.get("contactId");
        messageService.markAsRead(contactId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("已标记已读").build());
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        List<ConversationResponse> conversations = messageService.getConversations(userId);
        return ResponseEntity.ok(ApiResponse.<List<ConversationResponse>>builder()
                .success(true).message("查询成功").data(conversations).build());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读消息总数")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        int count = messageService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .success(true).message("查询成功").data(count).build());
    }

    @GetMapping("/status")
    @Operation(summary = "批量查询消息状态")
    public ResponseEntity<ApiResponse<Map<Long, String>>> getMessageStatuses(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String ids) {
        List<Long> idList = java.util.Arrays.stream(ids.split(","))
                .map(Long::parseLong).collect(java.util.stream.Collectors.toList());
        Map<Long, String> statuses = messageService.getMessageStatuses(idList);
        return ResponseEntity.ok(ApiResponse.<Map<Long, String>>builder()
                .success(true).message("查询成功").data(statuses).build());
    }
}
