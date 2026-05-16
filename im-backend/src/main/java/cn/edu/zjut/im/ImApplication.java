package cn.edu.zjut.im;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ImApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImApplication.class, args);
        System.out.println("即时通讯系统启动成功！");
        System.out.println("HTTP 服务: http://localhost:8080");
        System.out.println("WebSocket 服务: ws://localhost:8090/ws");
    }
}
