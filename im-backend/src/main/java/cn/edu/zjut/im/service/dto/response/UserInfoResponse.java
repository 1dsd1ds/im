package cn.edu.zjut.im.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String status;
    private LocalDateTime lastOnlineAt;
    private LocalDateTime createdAt;
}
