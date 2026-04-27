package com.sonicres.demo.features.audio.fingerprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class FingerprintCacheRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FingerprintCacheRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FingerprintResult> find(String fingerprint) {
        try {
            String json = jdbcTemplate.queryForObject(
                    "SELECT result_json FROM fingerprint_cache WHERE fingerprint = ?",
                    String.class,
                    fingerprint
            );

            return Arrays.asList(
                    objectMapper.readValue(json, FingerprintResult[].class)
            );

        } catch (Exception e) {
            return null;
        }
    }

    public void save(String fingerprint, List<FingerprintResult> results) {
        try {
            String json = objectMapper.writeValueAsString(results);

            jdbcTemplate.update(
                    "INSERT INTO fingerprint_cache (fingerprint, result_json) VALUES (?, ?)",
                    fingerprint, json
            );

        } catch (Exception ignored) {}
    }
}
