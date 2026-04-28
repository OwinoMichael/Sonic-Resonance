package com.sonicres.demo.features.audio.fingerprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChromaprintFingerprintService implements FingerprintService {

    private static final Logger log = LoggerFactory.getLogger(ChromaprintFingerprintService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FingerprintCacheRepository cacheRepository;
    //private final AcoustIdClient acoustIdClient;
    private final ArcCloudClient acrCloudClient;

    public ChromaprintFingerprintService(FingerprintCacheRepository cacheRepository, ArcCloudClient acrCloudClient) {
        this.cacheRepository = cacheRepository;
        this.acrCloudClient = acrCloudClient;
    }


    @Override
    public List<FingerprintResult> fingerprintAndMatch(File wavFile) throws Exception {
        String cacheKey = wavFile.getName().substring(0, Math.min(100, wavFile.getName().length()));

        List<FingerprintResult> cached = cacheRepository.find(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<FingerprintResult> matches = acrCloudClient.identify(wavFile);

        if (matches == null || matches.isEmpty()) {
            return List.of();
        }

        cacheRepository.save(cacheKey, matches);
        return matches;
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

    private FpcalcResult runFpcalc(File wavFile) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "fpcalc",
                "-json",
                wavFile.getAbsolutePath()
        );

        Process process = pb.start();

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining());
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(output, FpcalcResult.class);
    }
}


