package com.sonicres.demo.features.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WebSocket handler for Shazam-like audio fingerprinting.
 *
 * Flow:
 * 1. Client connects → Create buffer
 * 2. Client streams audio chunks → Append to buffer
 * 3. Client sends "done" message → Trigger processing (connection stays open!)
 * 4. Server processes → Sends result back → Closes connection
 */
@Component
public class AudioSocketConnectionHandler extends BinaryWebSocketHandler {

    private final ConcurrentMap<String, SessionAudioBuffer> sessions = new ConcurrentHashMap<>();
    private final ExecutorService processingPool = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors())
    );
    private final FingerprintService fingerprintService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AudioSocketConnectionHandler(FingerprintService fingerprintService) {
        this.fingerprintService = fingerprintService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("=== WebSocket Connection Established ===");
        System.out.println("Session ID: " + session.getId());
        System.out.println("Remote Address: " + session.getRemoteAddress());
        System.out.println("Is Open: " + session.isOpen());

        try {
            SessionAudioBuffer buffer = new SessionAudioBuffer(session);
            sessions.put(session.getId(), buffer);
            System.out.println("✓ Created and stored buffer for session: " + session.getId());

            // Notify client that connection is ready
            if (session.isOpen()) {
                String message = objectMapper.writeValueAsString(Map.of(
                        "type", "connected",
                        "sessionId", session.getId(),
                        "message", "Ready to receive audio"
                ));
                session.sendMessage(new TextMessage(message));
                System.out.println("✓ Sent 'connected' message to client");
            }

            System.out.println("✓ Connection fully established. Active sessions: " + sessions.size());

        } catch (Exception e) {
            System.err.println("❌ Error in afterConnectionEstablished: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        SessionAudioBuffer buffer = sessions.get(session.getId());

        if (buffer == null) {
            System.err.println("⚠️  No buffer for session " + session.getId() + ", creating new one");
            buffer = new SessionAudioBuffer(session);
            sessions.put(session.getId(), buffer);
        }

        ByteBuffer payload = message.getPayload();
        int bytesReceived = payload.remaining();

        System.out.println("📦 Received " + bytesReceived + " bytes from session: " + session.getId());

        // Append audio chunk to buffer
        buffer.append(payload);

        // Send acknowledgment
        if (session.isOpen()) {
            String ack = objectMapper.writeValueAsString(Map.of(
                    "type", "ack",
                    "bytes", bytesReceived,
                    "totalBytes", buffer.getTotalBytes()
            ));
            session.sendMessage(new TextMessage(ack));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        System.out.println("📨 Received text message from " + session.getId() + ": " + payload);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(payload, Map.class);
            String type = (String) json.get("type");

            if ("done".equals(type)) {
                System.out.println("🎵 Client finished recording: " + session.getId());
                handleRecordingComplete(session);
            } else if ("ping".equals(type)) {
                // Keep-alive ping
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            Map.of("type", "pong")
                    )));
                }
            } else {
                System.err.println("⚠️  Unknown message type: " + type);
            }

        } catch (Exception e) {
            System.err.println("❌ Error parsing text message: " + e.getMessage());
            e.printStackTrace();
            sendError(session, "Invalid message format");
        }
    }

    private void handleRecordingComplete(WebSocketSession session) {
        SessionAudioBuffer buffer = sessions.get(session.getId());

        if (buffer == null) {
            System.err.println("⚠️  No buffer found for session: " + session.getId());
            sendError(session, "No audio data received");
            return;
        }

        System.out.println("🎵 Starting audio processing for session: " + session.getId());
        System.out.println("Total bytes received: " + buffer.getTotalBytes());

        // Send "processing" status to client
        try {
            String processingMsg = objectMapper.writeValueAsString(Map.of(
                    "type", "processing",
                    "message", "Analyzing audio..."
            ));
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(processingMsg));
                System.out.println("✓ Sent 'processing' message to client");
            }
        } catch (Exception e) {
            System.err.println("❌ Error sending processing message: " + e.getMessage());
        }

        // Submit processing task
        AudioProcessingTask task = new AudioProcessingTask(buffer, fingerprintService);
        processingPool.submit(task);
        System.out.println("✓ Submitted processing task to thread pool");
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            if (session != null && session.isOpen()) {
                String error = objectMapper.writeValueAsString(Map.of(
                        "type", "error",
                        "message", errorMessage
                ));
                session.sendMessage(new TextMessage(error));
                System.err.println("❌ Sent error to session " + session.getId() + ": " + errorMessage);
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send error message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SessionAudioBuffer buffer = sessions.remove(session.getId());

        if (buffer != null) {
            buffer.closeSilently();
            System.out.println("✗ Session closed: " + session.getId() +
                    " - Status: " + status.getCode() +
                    " - Reason: " + (status.getReason() != null ? status.getReason() : "N/A"));
        } else {
            System.out.println("✗ Session closed (no buffer): " + session.getId() +
                    " - Status: " + status.getCode());
        }

        System.out.println("Remaining active sessions: " + sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("❌ Transport error for session " + session.getId() + ": " + exception.getMessage());
        exception.printStackTrace();

        SessionAudioBuffer buffer = sessions.remove(session.getId());
        if (buffer != null) {
            buffer.closeSilently();
        }
    }
}