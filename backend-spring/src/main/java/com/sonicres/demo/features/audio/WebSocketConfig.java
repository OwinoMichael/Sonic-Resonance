package com.sonicres.demo.features.audio;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AudioSocketConnectionHandler audioSocketConnectionHandler;

    public WebSocketConfig(AudioSocketConnectionHandler audioSocketConnectionHandler) {
        this.audioSocketConnectionHandler = audioSocketConnectionHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(audioSocketConnectionHandler, "/ws/audio")
                .setAllowedOrigins("*") // !!!!!! Not safe for prod !!!!!!
                .addInterceptors(new WebSocketHandshakeInterceptor());  // Add interceptor
    }
}
