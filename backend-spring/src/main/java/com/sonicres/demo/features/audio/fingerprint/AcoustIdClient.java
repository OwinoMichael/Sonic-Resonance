//package com.sonicres.demo.features.audio.fingerprint;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.net.URI;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class AcoustIdClient {
//
//    private static final Logger log = LoggerFactory.getLogger(AcoustIdClient.class);
//
//    private final WebClient webClient;
//    private final ObjectMapper objectMapper;
//
//    @Value("${acousticid.api.key}")
//    private String apiKey;
//
//    public AcoustIdClient(WebClient webClient, ObjectMapper objectMapper) {
//        this.webClient = webClient;
//        this.objectMapper = objectMapper;
//    }
//
//    public List<FingerprintResult> lookup(String fingerprint, int duration) {
//
//        log.info("fingerprint = {}", fingerprint);
//        log.info("duration = {}", duration);
//        log.info("API KEY = {}", apiKey);
//        log.info("fp length = {}", fingerprint.length());
//
//        String response = webClient.get()
//                .uri(uriBuilder -> {
//
//                    URI uri = uriBuilder
//                            .scheme("https")
//                            .host("api.acoustid.org")
//                            .path("/v2/lookup")
//                            .queryParam("client", apiKey)
//                            .queryParam("format", "json")
//                            .queryParam("meta", "recordings")
//                            .queryParam("fingerprint", fingerprint)
//                            .queryParam("duration", duration)
//                            .build();
//
//                    log.info("FINAL AcoustID URI = {}", uri);
//
//                    return uri;
//                })
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//
//
//        return parseResults(response);
//    }
//
//    private List<FingerprintResult> parseResults(String json) {
//        try {
//            Map<String, Object> root = objectMapper.readValue(json, Map.class);
//
//            List<Map<String, Object>> results =
//                    (List<Map<String, Object>>) root.get("results");
//
//            List<FingerprintResult> matches = new ArrayList<>();
//
//            if (results == null) return matches;
//
//            for (Map<String, Object> r : results) {
//
//                double score = ((Number) r.getOrDefault("score", 0)).doubleValue();
//
//                List<Map<String, Object>> recordings =
//                        (List<Map<String, Object>>) r.get("recordings");
//
//                if (recordings == null || recordings.isEmpty()) continue;
//
//                Map<String, Object> rec = recordings.get(0);
//
//                FingerprintResult fr = new FingerprintResult();
//                fr.setTrackName((String) rec.getOrDefault("title", "Unknown"));
//                fr.setArtist(extractArtist(rec));
//                fr.setConfidence(score);
//
//                matches.add(fr);
//            }
//
//            return matches;
//
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to parse AcoustID response", e);
//        }
//    }
//
//    private String extractArtist(Map<String, Object> rec) {
//        List<Map<String, Object>> artists =
//                (List<Map<String, Object>>) rec.get("artists");
//
//        if (artists != null && !artists.isEmpty()) {
//            return (String) artists.get(0).getOrDefault("name", "Unknown");
//        }
//        return "Unknown";
//    }
//}