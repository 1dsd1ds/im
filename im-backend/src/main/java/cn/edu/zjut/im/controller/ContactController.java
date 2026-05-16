package cn.edu.zjut.im.controller;

import cn.edu.zjut.im.security.JwtUtil;
import cn.edu.zjut.im.service.ContactService;
import cn.edu.zjut.im.service.dto.response.ApiResponse;
import cn.edu.zjut.im.service.dto.response.PendingRequestResponse;
import cn.edu.zjut.im.service.dto.response.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Tag(name = "联系人", description = "好友管理")
public class ContactController {

    private final ContactService contactService;
    private final JwtUtil jwtUtil;

    private Long getUserId(String authHeader) {
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping("/add")
    @Operation(summary = "添加好友（发送申请）")
    public ResponseEntity<ApiResponse<Void>> addContact(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Long> body) {
        Long userId = getUserId(authHeader);
        String result = contactService.addContact(userId, body.get("contactId"));
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(!result.contains("失败")).message(result).build());
    }

    @PostMapping("/accept/{requestId}")
    @Operation(summary = "同意好友申请")
    public ResponseEntity<ApiResponse<Void>> acceptContact(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long requestId) {
        Long userId = getUserId(authHeader);
        String result = contactService.acceptContact(userId, requestId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message(result).build());
    }

    @PostMapping("/reject/{requestId}")
    @Operation(summary = "拒绝好友申请")
    public ResponseEntity<ApiResponse<Void>> rejectContact(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long requestId) {
        Long userId = getUserId(authHeader);
        String result = contactService.rejectContact(userId, requestId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message(result).build());
    }

    @DeleteMapping("/{contactId}")
    @Operation(summary = "删除好友")
    public ResponseEntity<ApiResponse<Void>> removeContact(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long contactId) {
        Long userId = getUserId(authHeader);
        String result = contactService.removeContact(userId, contactId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message(result).build());
    }

    @GetMapping
    @Operation(summary = "获取好友列表")
    public ResponseEntity<ApiResponse<List<UserInfoResponse>>> getContacts(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        List<UserInfoResponse> contacts = contactService.getContacts(userId);
        return ResponseEntity.ok(ApiResponse.<List<UserInfoResponse>>builder()
                .success(true).message("查询成功").data(contacts).build());
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待处理的好友申请")
    public ResponseEntity<ApiResponse<List<PendingRequestResponse>>> getPendingRequests(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        List<PendingRequestResponse> requests = contactService.getPendingRequests(userId);
        return ResponseEntity.ok(ApiResponse.<List<PendingRequestResponse>>builder()
                .success(true).message("查询成功").data(requests).build());
    }
}
