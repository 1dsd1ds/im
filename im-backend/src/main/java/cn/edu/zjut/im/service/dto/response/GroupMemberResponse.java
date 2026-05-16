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
public class GroupMemberResponse {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
}
