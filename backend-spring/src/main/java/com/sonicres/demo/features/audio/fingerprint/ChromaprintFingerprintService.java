package com.sonicres.demo.features.audio.fingerprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChromaprintFingerprintService implements FingerprintService {

    private static final Logger log = LoggerFactory.getLogger(ChromaprintFingerprintService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FingerprintCacheRepository cacheRepository;
    private final AcoustIdClient acoustIdClient;

    public ChromaprintFingerprintService(FingerprintCacheRepository cacheRepository, AcoustIdClient acoustIdClient) {
        this.cacheRepository = cacheRepository;
        this.acoustIdClient = acoustIdClient;
    }

    @Override
    public FingerprintResult fingerprintAndMatch(File wavFile) throws Exception {

        FingerprintData data = generateFingerprintData(wavFile);

        String fingerprint = data.fingerprint;
        int duration = data.duration;

        // ✅ normalize ONLY for caching
        String cacheKey = fingerprint.substring(0, Math.min(100, fingerprint.length()));

        // 1. Check cache
        List<FingerprintResult> cached = cacheRepository.find(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached.get(0);
        }

        // 2. Call AcoustID (FULL fingerprint!)
        List<FingerprintResult> matches =
                acoustIdClient.lookup(fingerprint, duration);

        // 3. Rank
        List<FingerprintResult> topMatches = matches.stream()
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(10)
                .toList();

        // 4. Save using normalized key
        if (!topMatches.isEmpty()) {
            cacheRepository.save(cacheKey, topMatches);
        }

        return topMatches.isEmpty() ? new FingerprintResult() : topMatches.get(0);
    }

    private FingerprintData generateFingerprintData(File wavFile) throws Exception {

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
            throw new RuntimeException("fpcalc failed: " + output);
        }

        Map<String, Object> json =
                new ObjectMapper().readValue(output, Map.class);

        String fingerprint = (String) json.get("fingerprint");
        int duration = ((Number) json.get("duration")).intValue();

        return new FingerprintData(fingerprint, duration);
    }

    private static class FingerprintData {
        String fingerprint;
        int duration;

        FingerprintData(String fingerprint, int duration) {
            this.fingerprint = fingerprint;
            this.duration = duration;
        }
    }
}


