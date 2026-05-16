package cn.edu.zjut.im.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret = "im-system-secret-key-2024";
    private Long expiration = 86400000L;
    private String tokenHeader = "Authorization";
    private String tokenPrefix = "Bearer ";
    private Long refreshExpiration = 604800000L;
    private Boolean enableRefreshToken = true;
    private Boolean allowMultiDeviceLogin = true;
    private String issuer = "im-system";
    private String audience = "im-client";
}
