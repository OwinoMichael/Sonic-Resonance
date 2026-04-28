package com.sonicres.demo.features.audio.audioProcessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AudioConversionService {

    private static final Logger Log = LoggerFactory.getLogger(AudioConversionService.class);

    public File convertToWav(File inputFile) throws AudioConversionException {
        try {
            File outputFile = createTempFile(".wav");
            Log.info("Converting {} to WAV: {}", inputFile.getName(), outputFile.getName());

            ProcessResult result = executeFFmpeg(inputFile, outputFile);

            if (!result.isSuccess()) {
                throw new AudioConversionException(
                        "FFmpeg failed with exit code: " + result.exitCode +
                                "\nOutput: " + result.output
                );
            }

            validateOutput(outputFile);
            Log.info("✅ Conversion successful. Size: {} bytes", outputFile.length());
            return outputFile;

        } catch (IOException | InterruptedException e) {
            throw new AudioConversionException("Failed to convert audio", e);
        }
    }

    private File createTempFile(String suffix) throws IOException {
        return File.createTempFile("audio-" + UUID.randomUUID(), suffix);
    }

    private ProcessResult executeFFmpeg(File input, File output)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y", "-i", input.getAbsolutePath(),
                "-ac", "1",
                "-ar", "44100",
                "-acodec", "pcm_s16le",
                "-f", "wav",
                output.getAbsolutePath()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        String outputLog = captureOutput(process);
        int exitCode = process.waitFor();

        return new ProcessResult(exitCode, outputLog);
    }

    private String captureOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private void validateOutput(File file) throws AudioConversionException {
        if (!file.exists() || file.length() == 0) {
            throw new AudioConversionException("FFmpeg produced empty or missing output file");
        }
    }

    private record ProcessResult(int exitCode, String output) {
        boolean isSuccess() {
            return exitCode == 0;
        }
    }
}

