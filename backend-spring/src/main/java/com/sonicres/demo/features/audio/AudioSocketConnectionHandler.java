package com.sonicres.demo.features.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonicres.demo.features.audio.audioProcessing.AudioConversionService;
import com.sonicres.demo.features.audio.audioProcessing.AudioProcessingTask;
import com.sonicres.demo.features.audio.buffer.SessionAudioBuffer;
import com.sonicres.demo.features.audio.fingerprint.AcoustIdClient;
import com.sonicres.demo.features.audio.fingerprint.FingerprintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger Log = LoggerFactory.getLogger(AudioSocketConnectionHandler.class);

    private final ConcurrentMap<String, SessionAudioBuffer> sessions = new ConcurrentHashMap<>();
    private final ExecutorService processingPool = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors())
    );
    private final FingerprintService fingerprintService;
    private final AudioConversionService conversionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AcoustIdClient acoustIdClient;

    public AudioSocketConnectionHandler(FingerprintService fingerprintService,
                                        AudioConversionService conversionService, AcoustIdClient acoustIdClient) {
        this.fingerprintService = fingerprintService;
        this.conversionService = conversionService;
        this.acoustIdClient = acoustIdClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Log.info("=== WebSocket Connection Established ===");
        Log.info("Session ID: {}", session.getId());
        Log.info("Remote Address: {}", session.getRemoteAddress());
        Log.info("Is Open: {}", session.isOpen());

        try {
            SessionAudioBuffer buffer = new SessionAudioBuffer(session);
            sessions.put(session.getId(), buffer);
            Log.info("✓ Created and stored buffer for session: {}", session.getId());

            // Notify client that connection is ready
            if (session.isOpen()) {
                String message = objectMapper.writeValueAsString(Map.of(
                        "type", "connected",
                        "sessionId", session.getId(),
                        "message", "Ready to receive audio"
                ));
                session.sendMessage(new TextMessage(message));
                Log.info("✓ Sent 'connected' message to client");
            }

            Log.info("✓ Connection fully established. Active sessions: {}", sessions.size());

        } catch (Exception e) {
            Log.error("❌ Error in afterConnectionEstablished: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        SessionAudioBuffer buffer = sessions.get(session.getId());

        if (buffer == null) {
            Log.error("⚠️  No buffer for session {}, creating new one", session.getId());
            buffer = new SessionAudioBuffer(session);
            sessions.put(session.getId(), buffer);
        }

        ByteBuffer payload = message.getPayload();
        int bytesReceived = payload.remaining();

        Log.info("📦 Received {} bytes from session: {}", bytesReceived, session.getId());

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
        Log.info("📨 Received text message from {}: {}", session.getId(), payload);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(payload, Map.class);
            String type = (String) json.get("type");

            if ("done".equals(type)) {
                Log.info("🎵 Client finished recording: {}", session.getId());
                handleRecordingComplete(session);
            } else if ("ping".equals(type)) {
                // Keep-alive ping
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            Map.of("type", "pong")
                    )));
                }
            } else {
                Log.error("⚠️  Unknown message type: {}", type);
            }

        } catch (Exception e) {
            Log.error("❌ Error parsing text message: {}", e.getMessage(), e);
            sendError(session, "Invalid message format");
        }
    }

    private void handleRecordingComplete(WebSocketSession session) {
        SessionAudioBuffer buffer = sessions.get(session.getId());

        if (buffer == null) {
            Log.error("⚠️  No buffer found for session: {}", session.getId());
            sendError(session, "No audio data received");
            return;
        }

        Log.info("🎵 Starting audio processing for session: {}", session.getId());
        Log.info("Total bytes received: {}", buffer.getTotalBytes());

        // Send "processing" status to client
        try {
            String processingMsg = objectMapper.writeValueAsString(Map.of(
                    "type", "processing",
                    "message", "Analyzing audio..."
            ));
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(processingMsg));
                Log.info("✓ Sent 'processing' message to client");
            }
        } catch (Exception e) {
            Log.error("❌ Error sending processing message: {}", e.getMessage(), e);
        }

        // Submit processing task
        AudioProcessingTask task = new AudioProcessingTask(
                buffer, fingerprintService, conversionService, acoustIdClient
        );
        processingPool.submit(task);
        Log.info("✓ Submitted processing task to thread pool");
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            if (session != null && session.isOpen()) {
                String error = objectMapper.writeValueAsString(Map.of(
                        "type", "error",
                        "message", errorMessage
                ));
                session.sendMessage(new TextMessage(error));
                Log.error("❌ Sent error to session {}: {}", session.getId(), errorMessage);
            }
        } catch (Exception e) {
            Log.error("❌ Failed to send error message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SessionAudioBuffer buffer = sessions.remove(session.getId());

        if (buffer != null) {
            buffer.closeSilently();
            Log.info("✗ Session closed: {} - Status: {} - Reason: {}",
                    session.getId(), status.getCode(),
                    status.getReason() != null ? status.getReason() : "N/A");
        } else {
            Log.info("✗ Session closed (no buffer): {} - Status: {}",
                    session.getId(), status.getCode());
        }

        Log.info("Remaining active sessions: {}", sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Log.error("❌ Transport error for session {}: {}", session.getId(), exception.getMessage(), exception);

        SessionAudioBuffer buffer = sessions.remove(session.getId());
        if (buffer != null) {
            buffer.closeSilently();
        }
    }
}