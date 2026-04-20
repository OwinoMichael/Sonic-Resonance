package com.sonicres.demo.features.audio.fingerprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChromaprintFingerprintService implements FingerprintService {

    private static final Logger log = LoggerFactory.getLogger(ChromaprintFingerprintService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public FingerprintResult fingerprintAndMatch(File wavFile) throws Exception {

        log.info("🔍 Generating fingerprint for file: {}", wavFile.getName());

        ProcessBuilder pb = new ProcessBuilder(
                "fpcalc",
                "-json",
                wavFile.getAbsolutePath()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            output = reader.lines().collect(Collectors.joining("\n"));
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            log.error("❌ fpcalc failed. Output: {}", output);
            throw new RuntimeException("Fingerprint generation failed");
        }

        log.info("📄 fpcalc output: {}", output);

        // Parse JSON
        Map<String, Object> json = objectMapper.readValue(output, Map.class);

        String fingerprint = (String) json.get("fingerprint");
        Integer duration = (Integer) json.get("duration");

        if (fingerprint == null || fingerprint.isEmpty()) {
            throw new RuntimeException("Fingerprint is empty");
        }

        // For now: return raw fingerprint info (no matching yet)
        FingerprintResult result = new FingerprintResult();
        result.setTrackName("Unknown");
        result.setArtist("Unknown");
        result.setConfidence(0.0);
        result.setFingerprint(fingerprint);

        log.info("✅ Fingerprint generated. Duration: {}s", duration);

        return result;
    }
}
