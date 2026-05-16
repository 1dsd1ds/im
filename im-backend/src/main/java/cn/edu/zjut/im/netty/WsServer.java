package cn.edu.zjut.im.netty;

import cn.edu.zjut.im.config.NettyConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsServer {

    private final NettyConfig nettyConfig;
    private final AuthHandler authHandler;
    private final WsMessageHandler messageHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @PostConstruct
    public void start() {
        log.info("正在启动 Netty WebSocket 服务...");
        new Thread(() -> {
            bossGroup = new NioEventLoopGroup(nettyConfig.getBossThreads());
            workerGroup = new NioEventLoopGroup(nettyConfig.getWorkerThreads());

            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                // HTTP 编解码
                                pipeline.addLast("http-codec", new HttpServerCodec());
                                // 大数据流支持
                                pipeline.addLast("chunked-writer", new ChunkedWriteHandler());
                                // HTTP 消息聚合
                                pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                                // 60秒读空闲检测
                                pipeline.addLast("idle-handler", new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                                // WebSocket 握手 + JWT 鉴权 + 帧编解码设置（握手后自移除）
                                pipeline.addLast("auth-handler", authHandler);
                                // 业务消息处理
                                pipeline.addLast("message-handler", messageHandler);
                            }
                        })
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true);

                ChannelFuture future = bootstrap.bind(nettyConfig.getPort()).sync();
                serverChannel = future.channel();
                log.info("Netty WebSocket 服务启动，端口: {}", nettyConfig.getPort());

                serverChannel.closeFuture().sync();
            } catch (Exception e) {
                log.error("Netty 启动失败", e);
            } finally {
                shutdown();
            }
        }, "netty-server").start();
    }

    @PreDestroy
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        shutdown();
    }

    private void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Netty 服务已关闭");
    }
}
