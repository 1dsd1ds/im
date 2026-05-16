package cn.edu.zjut.im.netty;

import cn.edu.zjut.im.security.JwtUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class AuthHandler extends ChannelInboundHandlerAdapter {

    private final JwtUtil jwtUtil;
    private final WsChannelManager channelManager;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            URI uri = URI.create(request.uri());
            String query = uri.getQuery();

            String token = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && "token".equals(kv[0])) {
                        token = kv[1];
                        break;
                    }
                }
            }

            if (token == null || !jwtUtil.validateToken(token)) {
                log.warn("WebSocket 认证失败: token无效");
                ctx.close();
                return;
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            log.info("WebSocket 认证成功: userId={}, username={}", userId, username);

            ctx.channel().attr(ChannelAttributes.USER_ID).set(userId);
            ctx.channel().attr(ChannelAttributes.USERNAME).set(username);

            // 执行 WebSocket 握手
            WebSocketServerHandshakerFactory factory =
                    new WebSocketServerHandshakerFactory(
                            getWebSocketUrl(request), null, false
                    );
            WebSocketServerHandshaker handshaker = factory.newHandshaker(request);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                return;
            }

            handshaker.handshake(ctx.channel(), request).addListener(future -> {
                if (future.isSuccess()) {
                    // handshake() 内部已移除 HttpServerCodec 并添加 WebSocket 帧编解码器
                    // 这里只需清理可能残留的 HTTP 聚合器和分块处理器
                    safeRemove(ctx, "http-aggregator");
                    safeRemove(ctx, "chunked-writer");

                    // 注册 Channel（在线状态由 WsChannelManager → OnlineStatusService 统一管理）
                    channelManager.register(userId, ctx.channel());
                    log.info("WebSocket 握手成功: userId={}", userId);
                } else {
                    log.error("WebSocket 握手失败: userId={}", userId);
                    ctx.close();
                }
            });

            // 移除 AuthHandler 自身
            ctx.pipeline().remove(this);
            return;
        }
        super.channelRead(ctx, msg);
    }

    private String getWebSocketUrl(FullHttpRequest request) {
        String host = request.headers().get(HttpHeaderNames.HOST);
        return "ws://" + host + request.uri();
    }

    private void safeRemove(ChannelHandlerContext ctx, String name) {
        try {
            if (ctx.pipeline().get(name) != null) {
                ctx.pipeline().remove(name);
            }
        } catch (Exception ignored) {
        }
    }
}
