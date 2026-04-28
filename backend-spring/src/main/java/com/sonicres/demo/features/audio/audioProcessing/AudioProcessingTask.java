package com.sonicres.demo.features.audio.audioProcessing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonicres.demo.features.audio.fingerprint.*;
import com.sonicres.demo.features.audio.buffer.SessionAudioBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AudioProcessingTask implements Runnable {

    private static final Logger Log = LoggerFactory.getLogger(AudioProcessingTask.class);

    private final SessionAudioBuffer buffer;
    private final FingerprintService fingerprintService;
    private final AudioConversionService conversionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    //private final FingerprintCacheRepository cacheRepository;

    public AudioProcessingTask(SessionAudioBuffer buffer,
                               FingerprintService fingerprintService,
                               AudioConversionService conversionService) {
        this.buffer = buffer;
        this.fingerprintService = fingerprintService;
        this.conversionService = conversionService;
    }

    @Override
    public void run() {
        WebSocketSession session = buffer.getSession();
        File rawFile = null;
        File wavFile = null;

        try {
            Log.info("🎵 Processing audio for session: {}", session.getId());
            buffer.closeForProcessing();

            rawFile = buffer.getTempFile();

            if (rawFile.length() == 0) {
                sendErrorAndClose(session, "No audio data received");
                return;
            }

            // Let the service handle conversion
            wavFile = conversionService.convertToWav(rawFile);
            Log.info("🔍 DEBUG WAV: {}", wavFile.getAbsolutePath());

            // Process and send result
            List<FingerprintResult> results = fingerprintService.fingerprintAndMatch(wavFile);
            sendResultToClient(session, results);

            closeSession(session);

        } catch (Exception e) {
            Log.error("Processing failed for session {}: {}",
                    session.getId(), e.getMessage(), e);
            sendErrorAndClose(session, "Processing failed: " + e.getMessage());
        } finally {
            cleanup(rawFile, wavFile);
        }
    }

    private void sendResultToClient(WebSocketSession session, List<FingerprintResult> results) {
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> payload = new HashMap<>();

                if (results != null && !results.isEmpty()) {
                    List<Map<String, Object>> matchList = results.stream()
                            .map(m -> {
                                Map<String, Object> map = new HashMap<>();
                                map.put("title", m.getTrackName());
                                map.put("artist", m.getArtist());
                                map.put("album", m.getAlbum());
                                map.put("confidence", m.getConfidence());
                                map.put("releaseDate", m.getReleaseDate());
                                map.put("durationMs", m.getDurationMs());
                                map.put("coverArtUrl", m.getCoverArtUrl());
                                map.put("links", Map.of(
                                        "spotify", m.getSpotifyUrl() != null ? m.getSpotifyUrl() : "",
                                        "deezer", m.getDeezerUrl() != null ? m.getDeezerUrl() : "",
                                        "youtube", m.getYouTubeSearchUrl() != null ? m.getYouTubeSearchUrl() : ""
                                ));
                                return map;
                            })
                            .toList();

                    payload.put("type", "result");
                    payload.put("matches", matchList);
                } else {
                    payload.put("type", "no-match");
                    payload.put("matches", List.of());
                    payload.put("message", "No match found");
                }

                String json = new ObjectMapper().writeValueAsString(payload);
                session.sendMessage(new TextMessage(json));
                Log.info("✅ Sent result to client: {}", session.getId());

            } catch (IOException e) {
                Log.error("❌ Failed to send result: {}", e.getMessage(), e);
            }
        }
    }


    private void sendErrorToClient(WebSocketSession session, String errorMessage) {
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> errorMap = Map.of(
                        "type", "error",
                        "message", errorMessage
                );
                String errorJson = objectMapper.writeValueAsString(errorMap);
                session.sendMessage(new TextMessage(errorJson));
                Log.error("❌ Sent error to client {}: {}", session.getId(), errorMessage);
            } catch (IOException e) {
                Log.error("❌ Failed to send error: {}", e.getMessage(), e);
            }
        }
    }

    private void sendErrorAndClose(WebSocketSession session, String errorMessage) {
        sendErrorToClient(session, errorMessage);
        closeSession(session);
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

    private void cleanup(File... files) {
        for (File file : files) {
            if (file != null) {
                try {
                    Files.deleteIfExists(file.toPath());
                    Log.info("🗑️ Deleted temp file: {}", file.getName());
//                Log.info("🔍 DEBUG: Kept temp file at: {}", file.getAbsolutePath());
                } catch (IOException e) {
                    Log.error("⚠️ Failed to delete temp file {}: {}",
                            file.getName(), e.getMessage(), e);
                }
            }
        }
    }
}

//    /**
//     * Check if running inside Docker container
//     */
//    private boolean isRunningInDocker() {
//        try {
//            // Check for .dockerenv file
//            File dockerEnv = new File("/.dockerenv");
//            if (dockerEnv.exists()) {
//                return true;
//            }
//
//            // Check cgroup
//            File cgroup = new File("/proc/1/cgroup");
//            if (cgroup.exists()) {
//                try (BufferedReader reader = new BufferedReader(new FileReader(cgroup))) {
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        if (line.contains("docker") || line.contains("kubepods")) {
//                            return true;
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            Log.error("⚠️  Could not determine if running in Docker: {}", e.getMessage(), e);
//        }
//
//        // Fallback: check environment variable
//        return System.getenv("FFMPEG_CONTAINER") != null;
//    }
//
//    /**
//     * Convert audio using FFmpeg in Docker container (via docker exec)
//     */
//    private boolean decodeWithDockerFFmpeg(File inputFile, File outputFile) {
//        try {
//            String ffmpegContainer = System.getenv("FFMPEG_CONTAINER");
//            if (ffmpegContainer == null) {
//                ffmpegContainer = "ffmpeg-service-local";
//            }
//
//            Log.info("🐳 Using Docker FFmpeg container: {}", ffmpegContainer);
//
//            // Copy input file to shared volume
//            File sharedDir = new File("/tmp/audio");
//            if (!sharedDir.exists()) {
//                sharedDir.mkdirs();
//            }
//
//            File sharedInput = new File(sharedDir, "input-" + UUID.randomUUID() + ".webm");
//            File sharedOutput = new File(sharedDir, "output-" + UUID.randomUUID() + ".wav");
//
//            Log.info("📋 Copying to shared volume: {}", sharedInput.getName());
//            Files.copy(inputFile.toPath(), sharedInput.toPath(), StandardCopyOption.REPLACE_EXISTING);
//
//            // Execute FFmpeg via docker exec
//            ProcessBuilder pb = new ProcessBuilder(
//                    "docker", "exec", ffmpegContainer,
//                    "ffmpeg",
//                    "-y",
//                    "-i", "/tmp/audio/" + sharedInput.getName(),
//                    "-ac", "1",
//                    "-ar", "44100",
//                    "-acodec", "pcm_s16le",
//                    "-f", "wav",
//                    "/tmp/audio/" + sharedOutput.getName()
//            );
//
//            Log.info("🎬 Executing: {}", String.join(" ", pb.command()));
//
//            pb.redirectErrorStream(true);
//            Process process = pb.start();
//
//            StringBuilder output = new StringBuilder();
//            try (BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(process.getInputStream()))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    output.append(line).append("\n");
//                    Log.info("  FFmpeg: {}", line);
//                }
//            }
//
//            int exitCode = process.waitFor();
//            Log.info("📊 FFmpeg exit code: {}", exitCode);
//
//            if (exitCode != 0) {
//                Log.error("❌ FFmpeg failed with exit code: {}", exitCode);
//                Log.error("FFmpeg output:\n{}", output);
//                return false;
//            }
//
//            // Copy output back
//            Log.info("📋 Copying from shared volume: {}", sharedOutput.getName());
//            Files.copy(sharedOutput.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//
//            // Cleanup shared files
//            sharedInput.delete();
//            sharedOutput.delete();
//
//            if (!outputFile.exists() || outputFile.length() == 0) {
//                Log.error("❌ FFmpeg produced no output file");
//                return false;
//            }
//
//            return true;
//
//        } catch (IOException | InterruptedException e) {
//            Log.error("❌ Error running Docker FFmpeg: {}", e.getMessage(), e);
//            return false;
//        }
//    }



