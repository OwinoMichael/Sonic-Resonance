package com.sonicres.demo.features.audio;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class WebSocketHandshakeInterceptor implements HandshakeInterceptor{

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        System.out.println("🤝 WebSocket Handshake - Before");
        System.out.println("URI: " + request.getURI());
        System.out.println("Headers: " + request.getHeaders());
        System.out.println("Remote Address: " + request.getRemoteAddress());

        return true;  // Allow handshake
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {

        System.out.println("🤝 WebSocket Handshake - After");
        if (exception != null) {
            System.err.println("❌ Handshake error: " + exception.getMessage());
            exception.printStackTrace();
        } else {
            System.out.println("✅ Handshake successful");
        }
    }
}
