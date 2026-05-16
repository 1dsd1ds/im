package cn.edu.zjut.im.controller;

import cn.edu.zjut.im.security.JwtUtil;
import cn.edu.zjut.im.service.AuthService;
import cn.edu.zjut.im.service.TokenBlacklistService;
import cn.edu.zjut.im.service.dto.request.LoginRequest;
import cn.edu.zjut.im.service.dto.request.RegisterRequest;
import cn.edu.zjut.im.service.dto.response.ApiResponse;
import cn.edu.zjut.im.service.dto.response.LoginResponse;
import cn.edu.zjut.im.service.dto.response.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "登录/注册/Token验证")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        ApiResponse<LoginResponse> apiResponse = ApiResponse.<LoginResponse>builder()
                .success(response.isSuccess())
                .message(response.getMessage())
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        ApiResponse<LoginResponse> apiResponse = ApiResponse.<LoginResponse>builder()
                .success(response.isSuccess())
                .message(response.getMessage())
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/validate")
    @Operation(summary = "验证Token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Token无效").build());
        }
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true).message("Token有效")
                .data(Map.of("userId", jwtUtil.getUserIdFromToken(token),
                             "username", jwtUtil.getUsernameFromToken(token)))
                .build());
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        String token = jwtUtil.extractToken(authHeader);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.ok(ApiResponse.<UserInfoResponse>builder()
                    .success(false).message("未登录").build());
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        UserInfoResponse userInfo = authService.getUserInfo(userId);
        return ResponseEntity.ok(ApiResponse.<UserInfoResponse>builder()
                .success(true).message("获取成功").data(userInfo).build());
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = jwtUtil.extractToken(authHeader);
        if (token != null && jwtUtil.validateToken(token)) {
            authService.logout(jwtUtil.getUserIdFromToken(token));
            tokenBlacklistService.blacklist(token);
        }
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("已登出").build());
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        String token = jwtUtil.extractToken(authHeader);
        Long userId = jwtUtil.getUserIdFromToken(token);
        String result = authService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success("密码修改成功".equals(result)).message(result).build());
    }
}
