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
public class PendingRequestResponse {
    private Long id;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private LocalDateTime createdAt;
}
