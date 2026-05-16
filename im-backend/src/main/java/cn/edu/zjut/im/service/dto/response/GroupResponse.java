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
public class GroupResponse {
    private Long id;
    private String name;
    private String avatarUrl;
    private Long ownerId;
    private String ownerName;
    private int memberCount;
    private LocalDateTime createdAt;
}
