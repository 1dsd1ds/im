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
public class ConversationResponse {
    private String type;
    private Long contactId;
    private Long groupId;
    private String contactName;
    private String contactAvatar;
    private String contactStatus;
    private String lastMsgContent;
    private String lastMsgType;
    private LocalDateTime lastMsgTime;
    private int unreadCount;
}
