package com.websocket.websocketdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
//websocket要做的配置类,作用是扫描endpoint的@ServerEndpoint注解
public class WebSocketConfig {
    //注入一个bean对象,自动注册使用了@ServerEndpoint注解的bean
    @Bean
    public ServerEndpointExporter serverEndpointExporter(){
        return new ServerEndpointExporter();
    }
}
