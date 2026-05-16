package cn.edu.zjut.im.netty.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {
    private String type;
    private Object data;
    private Long timestamp;
    private String requestId;
}
