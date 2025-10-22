package com.sonicres.demo.features.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
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

            buffer.closeForProcessing();

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

            // Convert to WAV - FFmpeg is now installed in the same container
            System.out.println("🔄 Converting audio to WAV using local FFmpeg...");
            boolean success = decodeWithLocalFFmpeg(rawFile, wavFile);

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

            closeSession(session);

        } catch (Exception e) {
            System.err.println("❌ Error processing audio: " + e.getMessage());
            e.printStackTrace();
            sendErrorToClient("Audio processing error: " + e.getMessage());
            closeSession(session);
        } finally {
            cleanup(rawFile, wavFile);
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
            System.err.println("⚠️  Could not determine if running in Docker: " + e.getMessage());
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

            System.out.println("🐳 Using Docker FFmpeg container: " + ffmpegContainer);

            // Copy input file to shared volume
            File sharedDir = new File("/tmp/audio");
            if (!sharedDir.exists()) {
                sharedDir.mkdirs();
            }

            File sharedInput = new File(sharedDir, "input-" + UUID.randomUUID() + ".webm");
            File sharedOutput = new File(sharedDir, "output-" + UUID.randomUUID() + ".wav");

            System.out.println("📋 Copying to shared volume: " + sharedInput.getName());
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

            System.out.println("🎬 Executing: " + String.join(" ", pb.command()));

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("  FFmpeg: " + line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("📊 FFmpeg exit code: " + exitCode);

            if (exitCode != 0) {
                System.err.println("❌ FFmpeg failed with exit code: " + exitCode);
                System.err.println("FFmpeg output:\n" + output);
                return false;
            }

            // Copy output back
            System.out.println("📋 Copying from shared volume: " + sharedOutput.getName());
            Files.copy(sharedOutput.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Cleanup shared files
            sharedInput.delete();
            sharedOutput.delete();

            if (!outputFile.exists() || outputFile.length() == 0) {
                System.err.println("❌ FFmpeg produced no output file");
                return false;
            }

            return true;

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Error running Docker FFmpeg: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Convert audio using local FFmpeg installation (for development)
     */
    private boolean decodeWithLocalFFmpeg(File inputFile, File outputFile) {
        try {
            System.out.println("💻 Using local FFmpeg installation");

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
                System.err.println("❌ FFmpeg failed with exit code: " + exitCode);
                System.err.println("FFmpeg output:\n" + output);
                return false;
            }

            if (!outputFile.exists() || outputFile.length() == 0) {
                System.err.println("❌ FFmpeg produced no output file");
                return false;
            }

            return true;

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Error running local FFmpeg: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void sendResultToClient(FingerprintResult result) {
        WebSocketSession session = buffer.getSession();

        if (session != null && session.isOpen()) {
            try {
                String resultJson = result.toJSON();
                System.out.println("✅ Sending result to client: " + session.getId());
                session.sendMessage(new TextMessage(resultJson));
            } catch (IOException e) {
                System.err.println("❌ Failed to send result to client: " + e.getMessage());
                e.printStackTrace();
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
                System.err.println("❌ Sending error to client: " + errorJson);
                session.sendMessage(new TextMessage(errorJson));
            } catch (IOException e) {
                System.err.println("❌ Failed to send error to client: " + e.getMessage());
            }
        }
    }

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

    private void cleanup(File rawFile, File wavFile) {
        if (rawFile != null) {
            try {
                Files.deleteIfExists(rawFile.toPath());
                System.out.println("🗑️  Deleted raw file");
            } catch (IOException e) {
                System.err.println("⚠️  Failed to delete raw file");
            }
        }

        if (wavFile != null) {
            try {
                Files.deleteIfExists(wavFile.toPath());
                System.out.println("🗑️  Deleted WAV file");
            } catch (IOException e) {
                System.err.println("⚠️  Failed to delete WAV file");
            }
        }
    }
}


// ==================== IMPORTANT: Install Docker CLI in Spring Container ====================
// The Spring container needs the Docker CLI to execute `docker exec`
// 
// Option 1: Mount Docker socket (RECOMMENDED for dev)
// Add to docker-compose.local.yml spring service:
//
//   volumes:
//     - /var/run/docker.sock:/var/run/docker.sock
//
// Option 2: Install Docker CLI in the container
// This happens automatically with the volume mount above