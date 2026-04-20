package com.sonicres.demo.features.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AudioProcessingTask implements Runnable {

    private static final Logger Log = LoggerFactory.getLogger(AudioProcessingTask.class);

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
            Log.info("🎵 Starting audio processing for session: {}", session.getId());

            buffer.closeForProcessing();

            long fileSize = rawFile.length();
            Log.info("📁 Raw audio file size: {} bytes", fileSize);

            if (fileSize == 0) {
                Log.error("❌ No audio data received!");
                sendErrorToClient("No audio data received");
                closeSession(session);
                return;
            }

            // Create temp WAV file
            wavFile = File.createTempFile("audio-wav-" + UUID.randomUUID(), ".wav");
            Log.info("📝 Created temp WAV file: {}", wavFile.getName());

            // Convert to WAV - FFmpeg is now installed in the same container
            Log.info("🔄 Converting audio to WAV using local FFmpeg...");
            boolean success = decodeWithLocalFFmpeg(rawFile, wavFile);

            if (!success) {
                Log.error("❌ FFmpeg conversion failed");
                sendErrorToClient("Audio decoding failed");
                closeSession(session);
                return;
            }

            Log.info("✅ Audio converted to WAV: {} bytes", wavFile.length());

            // Call fingerprinting service
            Log.info("🔍 Starting fingerprint matching...");
            FingerprintResult result = fingerprintService.fingerprintAndMatch(wavFile);

            // Send result back to client
            sendResultToClient(result);

            closeSession(session);

        } catch (Exception e) {
            Log.error("❌ Error processing audio: {}", e.getMessage(), e);
            sendErrorToClient("Audio processing error: " + e.getMessage());
            closeSession(session);
        } finally {
            cleanup(rawFile, wavFile);
        }
    }

    /**
     * Convert audio using local FFmpeg installation (for development)
     */
    private boolean decodeWithLocalFFmpeg(File inputFile, File outputFile) {
        try {
            Log.info("💻 Using local FFmpeg installation");

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-i", inputFile.getAbsolutePath(),
                    "-ac", "1",
                    "-ar", "44100",
                    "-acodec", "pcm_s16le",
                    "-f", "wav",
                    outputFile.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

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
                Log.error("❌ FFmpeg failed with exit code: {}", exitCode);
                Log.error("FFmpeg output:\n{}", output);
                return false;
            }

            if (!outputFile.exists() || outputFile.length() == 0) {
                Log.error("❌ FFmpeg produced no output file");
                return false;
            }

            return true;

        } catch (IOException | InterruptedException e) {
            Log.error("❌ Error running local FFmpeg: {}", e.getMessage(), e);
            return false;
        }
    }

    private void sendResultToClient(FingerprintResult result) {
        WebSocketSession session = buffer.getSession();

        if (session != null && session.isOpen()) {
            try {
                String resultJson = result.toJSON();
                Log.info("✅ Sending result to client: {}", session.getId());
                session.sendMessage(new TextMessage(resultJson));
            } catch (IOException e) {
                Log.error("❌ Failed to send result to client: {}", e.getMessage(), e);
            }
        }
    }

    private void sendErrorToClient(String errorMessage) {
        WebSocketSession session = buffer.getSession();

        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("type", "error");
                errorMap.put("message", errorMessage);

                String errorJson = objectMapper.writeValueAsString(errorMap);
                Log.error("❌ Sending error to client: {}", errorJson);
                session.sendMessage(new TextMessage(errorJson));
            } catch (IOException e) {
                Log.error("❌ Failed to send error to client: {}", e.getMessage(), e);
            }
        }
    }

    private void closeSession(WebSocketSession session) {
        if (session != null && session.isOpen()) {
            try {
                Log.info("🔌 Closing session: {}", session.getId());
                session.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                Log.error("❌ Error closing session: {}", e.getMessage(), e);
            }
        }
    }

    private void cleanup(File rawFile, File wavFile) {
        if (rawFile != null) {
            try {
                Files.deleteIfExists(rawFile.toPath());
                Log.info("🗑️  Deleted raw file");
            } catch (IOException e) {
                Log.error("⚠️  Failed to delete raw file: {}", e.getMessage(), e);
            }
        }

        if (wavFile != null) {
            try {
                Files.deleteIfExists(wavFile.toPath());
                Log.info("🗑️  Deleted WAV file");
            } catch (IOException e) {
                Log.error("⚠️  Failed to delete WAV file: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Check if running inside Docker container
     */
    private boolean isRunningInDocker() {
        try {
            // Check for .dockerenv file
            File dockerEnv = new File("/.dockerenv");
            if (dockerEnv.exists()) {
                return true;
            }

            // Check cgroup
            File cgroup = new File("/proc/1/cgroup");
            if (cgroup.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(cgroup))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("docker") || line.contains("kubepods")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error("⚠️  Could not determine if running in Docker: {}", e.getMessage(), e);
        }

        // Fallback: check environment variable
        return System.getenv("FFMPEG_CONTAINER") != null;
    }

    /**
     * Convert audio using FFmpeg in Docker container (via docker exec)
     */
    private boolean decodeWithDockerFFmpeg(File inputFile, File outputFile) {
        try {
            String ffmpegContainer = System.getenv("FFMPEG_CONTAINER");
            if (ffmpegContainer == null) {
                ffmpegContainer = "ffmpeg-service-local";
            }

            Log.info("🐳 Using Docker FFmpeg container: {}", ffmpegContainer);

            // Copy input file to shared volume
            File sharedDir = new File("/tmp/audio");
            if (!sharedDir.exists()) {
                sharedDir.mkdirs();
            }

            File sharedInput = new File(sharedDir, "input-" + UUID.randomUUID() + ".webm");
            File sharedOutput = new File(sharedDir, "output-" + UUID.randomUUID() + ".wav");

            Log.info("📋 Copying to shared volume: {}", sharedInput.getName());
            Files.copy(inputFile.toPath(), sharedInput.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Execute FFmpeg via docker exec
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", ffmpegContainer,
                    "ffmpeg",
                    "-y",
                    "-i", "/tmp/audio/" + sharedInput.getName(),
                    "-ac", "1",
                    "-ar", "44100",
                    "-acodec", "pcm_s16le",
                    "-f", "wav",
                    "/tmp/audio/" + sharedOutput.getName()
            );

            Log.info("🎬 Executing: {}", String.join(" ", pb.command()));

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    Log.info("  FFmpeg: {}", line);
                }
            }

            int exitCode = process.waitFor();
            Log.info("📊 FFmpeg exit code: {}", exitCode);

            if (exitCode != 0) {
                Log.error("❌ FFmpeg failed with exit code: {}", exitCode);
                Log.error("FFmpeg output:\n{}", output);
                return false;
            }

            // Copy output back
            Log.info("📋 Copying from shared volume: {}", sharedOutput.getName());
            Files.copy(sharedOutput.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Cleanup shared files
            sharedInput.delete();
            sharedOutput.delete();

            if (!outputFile.exists() || outputFile.length() == 0) {
                Log.error("❌ FFmpeg produced no output file");
                return false;
            }

            return true;

        } catch (IOException | InterruptedException e) {
            Log.error("❌ Error running Docker FFmpeg: {}", e.getMessage(), e);
            return false;
        }
    }
}


