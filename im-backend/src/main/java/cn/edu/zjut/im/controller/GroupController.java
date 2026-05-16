package cn.edu.zjut.im.controller;

import cn.edu.zjut.im.entity.Group;
import cn.edu.zjut.im.entity.GroupMember;
import cn.edu.zjut.im.entity.User;
import cn.edu.zjut.im.security.JwtUtil;
import cn.edu.zjut.im.service.GroupService;
import cn.edu.zjut.im.service.UserCacheService;
import cn.edu.zjut.im.service.OnlineStatusService;
import cn.edu.zjut.im.service.dto.request.CreateGroupRequest;
import cn.edu.zjut.im.service.dto.response.ApiResponse;
import cn.edu.zjut.im.service.dto.response.GroupMemberResponse;
import cn.edu.zjut.im.service.dto.response.GroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "群组", description = "群组管理")
public class GroupController {

    private final GroupService groupService;
    private final UserCacheService userCacheService;
    private final OnlineStatusService onlineStatusService;
    private final JwtUtil jwtUtil;

    private Long getUserId(String authHeader) {
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping
    @Operation(summary = "创建群组")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateGroupRequest request) {
        Long userId = getUserId(authHeader);
        Group group = groupService.createGroup(request.getName(), userId, request.getMemberIds());
        User owner = userCacheService.getUser(userId);
        GroupResponse response = GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .avatarUrl(group.getAvatarUrl())
                .ownerId(group.getOwnerId())
                .ownerName(owner != null ? owner.getNickname() : "")
                .memberCount(groupService.getGroupMemberCount(group.getId()))
                .createdAt(group.getCreatedAt())
                .build();
        return ResponseEntity.ok(ApiResponse.<GroupResponse>builder()
                .success(true).message("创建成功").data(response).build());
    }

    @PostMapping("/{groupId}/join")
    @Operation(summary = "加入群组")
    public ResponseEntity<ApiResponse<Void>> joinGroup(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long groupId) {
        Long userId = getUserId(authHeader);
        String result = groupService.joinGroup(groupId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success("加入成功".equals(result)).message(result).build());
    }

    @PostMapping("/{groupId}/leave")
    @Operation(summary = "退出群组")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long groupId) {
        Long userId = getUserId(authHeader);
        String result = groupService.leaveGroup(groupId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success("已退出群组".equals(result)).message(result).build());
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @Operation(summary = "群主踢出成员")
    public ResponseEntity<ApiResponse<Void>> kickMember(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long groupId,
            @PathVariable Long userId) {
        Long ownerId = getUserId(authHeader);
        String result = groupService.kickMember(groupId, ownerId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success("已踢出群组".equals(result)).message(result).build());
    }

    @PostMapping("/{groupId}/dismiss")
    @Operation(summary = "解散群组")
    public ResponseEntity<ApiResponse<Void>> dismissGroup(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long groupId) {
        Long userId = getUserId(authHeader);
        String result = groupService.dismissGroup(groupId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success("群组已解散".equals(result)).message(result).build());
    }

    @GetMapping
    @Operation(summary = "获取我的群组列表")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getMyGroups(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        List<Group> groups = groupService.getUserGroups(userId);

        Set<Long> ownerIds = groups.stream().map(Group::getOwnerId).collect(Collectors.toSet());
        Map<Long, User> userMap = userCacheService.getUsersBatch(ownerIds);

        List<GroupResponse> result = groups.stream().map(g -> GroupResponse.builder()
                .id(g.getId())
                .name(g.getName())
                .avatarUrl(g.getAvatarUrl())
                .ownerId(g.getOwnerId())
                .ownerName(userMap.containsKey(g.getOwnerId()) ? userMap.get(g.getOwnerId()).getNickname() : "")
                .memberCount(groupService.getGroupMemberCount(g.getId()))
                .createdAt(g.getCreatedAt())
                .build()).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<GroupResponse>>builder()
                .success(true).message("查询成功").data(result).build());
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "获取群成员列表")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getMembers(@PathVariable Long groupId) {
        List<GroupMember> members = groupService.getGroupMembers(groupId);

        Set<Long> userIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userCacheService.getUsersBatch(userIds);
        Set<Long> onlineIds = onlineStatusService.filterOnlineUsers(userIds);

        List<GroupMemberResponse> result = members.stream().map(m -> {
            User u = userMap.get(m.getUserId());
            return GroupMemberResponse.builder()
                    .id(m.getId())
                    .userId(m.getUserId())
                    .username(u != null ? u.getUsername() : "")
                    .nickname(u != null ? u.getNickname() : "")
                    .avatarUrl(u != null ? u.getAvatarUrl() : null)
                    .role(m.getRole())
                    .status(onlineIds.contains(m.getUserId()) ? "ONLINE" : "OFFLINE")
                    .joinedAt(m.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<GroupMemberResponse>>builder()
                .success(true).message("查询成功").data(result).build());
    }
}
