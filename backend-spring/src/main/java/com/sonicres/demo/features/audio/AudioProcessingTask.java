package com.sonicres.demo.features.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AudioProcessingTask implements Runnable {

    private final SessionAudioBuffer buffer;
    private final FingerprintService fingerprintService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AudioProcessingTask(SessionAudioBuffer buffer,
                               FingerprintService fingerprintService) {
        this.buffer = buffer;
        this.fingerprintService = fingerprintService;
    }

    @Override
    public void run() {
        File rawFile = buffer.getTempFile();
        File wavFile = null;
        WebSocketSession session = buffer.getSession();

        try {
            System.out.println("🎵 Starting audio processing for session: " + session.getId());

            // Close the buffer for processing
            buffer.closeForProcessing();

            // Check if we received any data
            long fileSize = rawFile.length();
            System.out.println("📁 Raw audio file size: " + fileSize + " bytes");

            if (fileSize == 0) {
                System.err.println("❌ No audio data received!");
                sendErrorToClient("No audio data received");
                closeSession(session);
                return;
            }

            // Create temp WAV file
            wavFile = File.createTempFile("audio-wav-" + UUID.randomUUID(), ".wav");
            System.out.println("📝 Created temp WAV file: " + wavFile.getName());

            // Convert to WAV using FFmpeg
            System.out.println("🔄 Converting audio to WAV...");
            boolean success = decodeWithFFmpeg(rawFile, wavFile);

            if (!success) {
                System.err.println("❌ FFmpeg conversion failed");
                sendErrorToClient("Audio decoding failed");
                closeSession(session);
                return;
            }

            System.out.println("✅ Audio converted to WAV: " + wavFile.length() + " bytes");

            // Call fingerprinting service
            System.out.println("🔍 Starting fingerprint matching...");
            FingerprintResult result = fingerprintService.fingerprintAndMatch(wavFile);

            // Send result back to client
            sendResultToClient(result);

            // Close session after sending result
            closeSession(session);

        } catch (Exception e) {
            System.err.println("❌ Error processing audio: " + e.getMessage());
            e.printStackTrace();
            sendErrorToClient("Audio processing error: " + e.getMessage());
            closeSession(session);
        } finally {
            // Cleanup temp files
            cleanup(rawFile, wavFile);
        }
    }

    /**
     * Convert audio to WAV using FFmpeg
     */
    private boolean decodeWithFFmpeg(File inputFile, File outputFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y",                                    // Overwrite output
                    "-i", inputFile.getAbsolutePath(),       // Input file
                    "-ac", "1",                              // Mono audio
                    "-ar", "44100",                          // 44.1kHz sample rate
                    "-acodec", "pcm_s16le",                  // PCM 16-bit LE codec
                    "-f", "wav",                             // Force WAV format
                    outputFile.getAbsolutePath()             // Output file
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture output for logging/debugging
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("❌ FFmpeg failed with exit code: " + exitCode);
                System.err.println("FFmpeg output:\n" + output);
                return false;
            }

            // Verify output file was created and has content
            if (!outputFile.exists() || outputFile.length() == 0) {
                System.err.println("❌ FFmpeg produced no output file");
                return false;
            }

            return true;

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Error running FFmpeg: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send successful result to client
     */
    private void sendResultToClient(FingerprintResult result) {
        WebSocketSession session = buffer.getSession();

        if (session != null && session.isOpen()) {
            try {
                String payload = String.format(
                        "{\"type\":\"result\",\"data\":%s}",
                        result.toJSON()
                );

                System.out.println("✅ Sending result to client: " + session.getId());
                System.out.println("Payload: " + payload);

                session.sendMessage(new TextMessage(payload));

            } catch (IOException e) {
                System.err.println("❌ Failed to send result to client: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("⚠️  Cannot send result - session is closed or null");
        }
    }


    /**
     * Send error message to client
     */
    private void sendErrorToClient(String errorMessage) {
        WebSocketSession session = buffer.getSession();

        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("type", "error");
                errorMap.put("message", errorMessage);

                String errorJson = objectMapper.writeValueAsString(errorMap);
                System.err.println("❌ Sending error to client: " + errorJson);
                session.sendMessage(new TextMessage(errorJson));
            } catch (IOException e) {
                System.err.println("❌ Failed to send error to client: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Close WebSocket session gracefully
     */
    private void closeSession(WebSocketSession session) {
        if (session != null && session.isOpen()) {
            try {
                System.out.println("🔌 Closing session: " + session.getId());
                session.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                System.err.println("❌ Error closing session: " + e.getMessage());
            }
        }
    }

    /**
     * Clean up temporary files
     */
    private void cleanup(File rawFile, File wavFile) {
        if (rawFile != null) {
            try {
                Files.deleteIfExists(rawFile.toPath());
                System.out.println("🗑️  Deleted raw file: " + rawFile.getName());
            } catch (IOException e) {
                System.err.println("⚠️  Failed to delete raw file: " + e.getMessage());
            }
        }

        if (wavFile != null) {
            try {
                Files.deleteIfExists(wavFile.toPath());
                System.out.println("🗑️  Deleted WAV file: " + wavFile.getName());
            } catch (IOException e) {
                System.err.println("⚠️  Failed to delete WAV file: " + e.getMessage());
            }
        }
    }
}