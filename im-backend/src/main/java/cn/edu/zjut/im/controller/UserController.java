package cn.edu.zjut.im.controller;

import cn.edu.zjut.im.entity.User;
import cn.edu.zjut.im.repository.UserRepository;
import cn.edu.zjut.im.security.JwtUtil;
import cn.edu.zjut.im.service.UserCacheService;
import cn.edu.zjut.im.service.dto.request.UpdateProfileRequest;
import cn.edu.zjut.im.service.dto.response.ApiResponse;
import cn.edu.zjut.im.service.dto.response.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户", description = "用户搜索和信息")
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserCacheService userCacheService;

    @GetMapping("/search")
    @Operation(summary = "搜索用户（按昵称或用户名）")
    public ResponseEntity<ApiResponse<List<UserInfoResponse>>> searchUsers(@RequestParam String keyword) {
        List<User> users = userRepository.findByNicknameContainingOrUsernameContaining(keyword, keyword);
        List<UserInfoResponse> result = users.stream()
                .map(u -> UserInfoResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .nickname(u.getNickname())
                        .avatarUrl(u.getAvatarUrl())
                        .status(u.getStatus())
                        .lastOnlineAt(u.getLastOnlineAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.<List<UserInfoResponse>>builder()
                .success(true).message("查询成功").data(result).build());
    }

    @PutMapping("/me")
    @Transactional
    @Operation(summary = "更新个人信息")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        String token = jwtUtil.extractToken(authHeader);
        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.<UserInfoResponse>builder()
                    .success(false).message("用户不存在").build());
        }
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        userRepository.save(user);
        userCacheService.evictUser(userId);

        return ResponseEntity.ok(ApiResponse.<UserInfoResponse>builder()
                .success(true).message("更新成功").data(toUserInfo(user)).build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户信息")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.<UserInfoResponse>builder()
                    .success(false).message("用户不存在").build());
        }
        UserInfoResponse info = UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .lastOnlineAt(user.getLastOnlineAt())
                .build();
        return ResponseEntity.ok(ApiResponse.<UserInfoResponse>builder()
                .success(true).message("查询成功").data(info).build());
    }

    private UserInfoResponse toUserInfo(User u) {
        return UserInfoResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .avatarUrl(u.getAvatarUrl())
                .status(u.getStatus())
                .lastOnlineAt(u.getLastOnlineAt())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
