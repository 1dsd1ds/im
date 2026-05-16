package cn.edu.zjut.im.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "netty")
public class NettyConfig {
    private int port = 8090;
    private int bossThreads = 1;
    private int workerThreads = 4;
}
