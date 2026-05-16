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
public class MessageResponse {
    private String id;
    private Long fromUserId;
    private String fromNickname;
    private String fromAvatar;
    private Long toUserId;
    private Long groupId;
    private String msgType;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
