package com.sonicres.demo.features.audio;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WebSocket handler for receiving audio streams from clients.
 *
 * Flow:
 * 1. Client connects → Create SessionAudioBuffer
 * 2. Client sends binary audio chunks → Append to buffer's temp file
 * 3. Client disconnects → Trigger AudioProcessingTask
 * 4. AudioProcessingTask decodes audio with FFmpeg → Fingerprints → Sends result
 */

@Component
public class AudioSocketConnectionHandler extends BinaryWebSocketHandler {

    //Map sessionId -> SessionAudioManager
    private final ConcurrentMap<String, SessionAudioBuffer> sessions = new ConcurrentHashMap<>();

    //Executor for CPU/IO-heavy tasks (decoding & fingerprinting)
    private final ExecutorService processingPool = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors())
    );

    private final FingerprintService fingerprintService;

    public AudioSocketConnectionHandler(FingerprintService fingerprintService){
        this.fingerprintService = fingerprintService;
    }


    /**
     * Called when a new WebSocket connection is established.
     * Creates a buffer to accumulate audio data for this session.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), new SessionAudioBuffer(session));
        System.out.println("Connected session " + session.getId());

        // Notify client that connection is ready
        if (session.isOpen()) {
            session.sendMessage(new TextMessage("{\"type\":\"connected\",\"sessionId\":\"" + session.getId() + "\"}"));
        }
    }

    /**
     * Called when client sends binary audio data.
     * Appends the audio chunk to the session's temp file.
     *
     * The client (browser) typically sends audio in small chunks as it's recorded.
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        SessionAudioBuffer buffer = sessions.get(session.getId());

        if (buffer == null) {
            // Defensive: create buffer if somehow missing
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

    /**
     * Called when WebSocket connection is closed (client stops recording).
     * This triggers the audio processing pipeline:
     * 1. Close the buffer (finalize temp file)
     * 2. Submit AudioProcessingTask to background thread pool
     * 3. AudioProcessingTask decodes with FFmpeg and fingerprints
     * 4. Result is sent back to client (if still connected)
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SessionAudioBuffer buffer = sessions.remove(session.getId());


        if (buffer != null) {
            buffer.closeForProcessing();
            // Submit background fingerprinting job
            AudioProcessingTask task = new AudioProcessingTask(
                    buffer,
                    fingerprintService

            );

            processingPool.submit(task);
        }
        System.out.println("Closed session " + session.getId());
    }
}
