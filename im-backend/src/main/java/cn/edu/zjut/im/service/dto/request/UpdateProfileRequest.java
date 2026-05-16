package cn.edu.zjut.im.service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min = 1, max = 50, message = "昵称长度1-50位")
    private String nickname;

    private String avatarUrl;
}
