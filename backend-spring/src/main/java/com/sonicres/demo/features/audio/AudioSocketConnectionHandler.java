package com.sonicres.demo.features.audio;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AudioSocketConnectionHandler extends BinaryWebSocketHandler {

    //Map sessionId -> SessionAudioManager
    private final ConcurrentMap<String, SessionAudioBuffer> sessionAudioBufferConcurrentMap = new ConcurrentMap<String, SessionAudioBuffer>();

    //Executor for CPU/IO-heavy tasks (decoding & fingerprinting)
    private final ExecutorService processingPool = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors())
    );

    private final FingerprintService fingerprintService;

    public AudioSocketConnectionHandler(FingerprintService fingerprintService){
        this.fingerprintService = fingerprintService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), new SessionAudioBuffer(session));
        System.out.println("Connected session " + session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        SessionAudioBuffer buffer = sessions.get(session.getId());
        if (buffer == null) {
            // unexpected - create buffer defensively
            buffer = new SessionAudioBuffer(session);
            sessions.put(session.getId(), buffer);
        }

        ByteBuffer payload = message.getPayload();
        // Write to buffer (non-blocking)
        buffer.append(payload);

        // Optionally send ack/heartbeat to client
        if (session.isOpen()) {
            session.sendMessage(new TextMessage("{\"type\":\"ack\",\"bytes\":" + payload.remaining() + "}"));
        }
    }
}
