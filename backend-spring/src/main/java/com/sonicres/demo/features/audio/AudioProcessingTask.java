package com.sonicres.demo.features.audio;

import org.springframework.web.socket.TextMessage;

import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

public class AudioProcessingTask implements Runnable {

    private final SessionAudioBuffer buffer;
    private final FingerprintService fingerprintService;
    // Optional: for Jave2 approach

    public AudioProcessingTask(SessionAudioBuffer buffer,
                               FingerprintService fingerprintService) {
        this.buffer = buffer;
        this.fingerprintService = fingerprintService;

    }

    @Override
    public void run() {
        File rawFile = buffer.getTempFile();
        File wavFile = null;

        try {
            buffer.closeForProcessing();

            // Create temp WAV file
            wavFile = File.createTempFile("audio-wav-" + UUID.randomUUID(), ".wav");

            // OPTION 1: Use FFmpeg (your current approach - improved)
            boolean success = decodeWithFFmpeg(rawFile, wavFile);

            if (!success) {
                System.err.println("FFmpeg conversion failed");
                sendErrorToClient("Audio decoding failed");
                return;
            }

            // Call fingerprinting on wavFile
            FingerprintResult result = fingerprintService.fingerprintAndMatch(wavFile);

            // Send result back to client if session still open
            sendResultToClient(result);

        } catch (Exception e) {
            System.err.println("Error processing audio: " + e.getMessage());
            e.printStackTrace();
            sendErrorToClient("Audio processing error: " + e.getMessage());
        } finally {
            // Cleanup temp files
            cleanup(rawFile, wavFile);
        }
    }

    /**
     * FFmpeg via ProcessBuilder
     * This is your current approach, just refactored and improved
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
                    // Optionally log progress
                    // System.out.println(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("FFmpeg failed with exit code: " + exitCode);
                System.err.println("FFmpeg output:\n" + output);
                return false;
            }

            // Verify output file was created and has content
            if (!outputFile.exists() || outputFile.length() == 0) {
                System.err.println("FFmpeg produced no output file");
                return false;
            }

            return true;

        } catch (IOException | InterruptedException e) {
            System.err.println("Error running FFmpeg: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Send successful result to client
     */
    private void sendResultToClient(FingerprintResult result) {
        if (buffer.getSession() != null && buffer.getSession().isOpen()) {
            try {
                buffer.getSession().sendMessage(new TextMessage(result.toJSON()));
            } catch (IOException e) {
                System.err.println("Failed to send result to client: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Send error message to client
     */
    private void sendErrorToClient(String errorMessage) {
        if (buffer.getSession() != null && buffer.getSession().isOpen()) {
            try {
                String errorJson = String.format(
                        "{\"error\": true, \"message\": \"%s\"}",
                        errorMessage.replace("\"", "\\\"")
                );
                buffer.getSession().sendMessage(new TextMessage(errorJson));
            } catch (IOException e) {
                System.err.println("Failed to send error to client: " + e.getMessage());
                e.printStackTrace();
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
            } catch (IOException e) {
                System.err.println("Failed to delete raw file: " + e.getMessage());
            }
        }

        if (wavFile != null) {
            try {
                Files.deleteIfExists(wavFile.toPath());
            } catch (IOException e) {
                System.err.println("Failed to delete WAV file: " + e.getMessage());
            }
        }
    }
}