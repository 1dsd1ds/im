package cn.edu.zjut.im.service;

import cn.edu.zjut.im.entity.User;
import cn.edu.zjut.im.repository.UserRepository;
import cn.edu.zjut.im.security.JwtUtil;
import cn.edu.zjut.im.service.dto.request.LoginRequest;
import cn.edu.zjut.im.service.dto.request.RegisterRequest;
import cn.edu.zjut.im.service.dto.response.LoginResponse;
import cn.edu.zjut.im.service.dto.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (user == null) {
            return LoginResponse.builder().success(false).message("用户名或密码错误").build();
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return LoginResponse.builder().success(false).message("用户名或密码错误").build();
        }

        // 在线状态由 WebSocket 连接/断开统一管理，HTTP 登录不修改状态
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        log.info("用户登录成功: {}", user.getUsername());

        return LoginResponse.builder()
                .success(true)
                .message("登录成功")
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return LoginResponse.builder().success(false).message("用户名已存在").build();
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus("OFFLINE");
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        log.info("用户注册成功: {}", user.getUsername());

        return LoginResponse.builder()
                .success(true)
                .message("注册成功")
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .lastOnlineAt(user.getLastOnlineAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public void logout(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setStatus("OFFLINE");
            user.setLastOnlineAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("用户登出: {}", user.getUsername());
        }
    }

    public String changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "用户不存在";
        }
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return "原密码错误";
        }
        if (newPassword == null || newPassword.length() < 6) {
            return "新密码至少6位";
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("用户 {} 修改了密码", user.getUsername());
        return "密码修改成功";
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public Long getUserIdFromToken(String token) {
        return jwtUtil.getUserIdFromToken(token);
    }
}
