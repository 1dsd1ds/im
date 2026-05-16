package cn.edu.zjut.im.netty;

import cn.edu.zjut.im.netty.protocol.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static WsMessage decode(TextWebSocketFrame frame) {
        try {
            return MAPPER.readValue(frame.text(), WsMessage.class);
        } catch (Exception e) {
            log.error("消息解码失败: {}", frame.text(), e);
            return null;
        }
    }

    public static TextWebSocketFrame encode(WsMessage message) {
        try {
            if (message.getTimestamp() == null) {
                message.setTimestamp(System.currentTimeMillis());
            }
            String json = MAPPER.writeValueAsString(message);
            return new TextWebSocketFrame(json);
        } catch (Exception e) {
            log.error("消息编码失败", e);
            return null;
        }
    }
}
