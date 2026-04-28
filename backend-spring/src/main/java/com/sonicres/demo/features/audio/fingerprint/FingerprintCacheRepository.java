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
            return Arrays.asList(objectMapper.readValue(json, FingerprintResult[].class));
        } catch (Exception e) {
            return null;
        }
    }

    public void save(String fingerprint, List<FingerprintResult> results) {
        try {
            String json = objectMapper.writeValueAsString(results);
            FingerprintResult top = results.get(0);

            jdbcTemplate.update("""
                INSERT INTO fingerprint_cache 
                    (fingerprint, result_json, track_name, artist, album, 
                     release_date, duration_ms, label, spotify_track_id, 
                     deezer_track_id, cover_art_url, confidence)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (fingerprint) DO UPDATE SET
                    result_json = EXCLUDED.result_json,
                    track_name = EXCLUDED.track_name,
                    artist = EXCLUDED.artist,
                    album = EXCLUDED.album,
                    release_date = EXCLUDED.release_date,
                    duration_ms = EXCLUDED.duration_ms,
                    label = EXCLUDED.label,
                    spotify_track_id = EXCLUDED.spotify_track_id,
                    deezer_track_id = EXCLUDED.deezer_track_id,
                    cover_art_url = EXCLUDED.cover_art_url,
                    confidence = EXCLUDED.confidence
                """,
                    fingerprint, json,
                    top.getTrackName(), top.getArtist(), top.getAlbum(),
                    top.getReleaseDate(), top.getDurationMs(), top.getLabel(),
                    top.getSpotifyTrackId(), top.getDeezerTrackId(),
                    top.getCoverArtUrl(), top.getConfidence()
            );

        } catch (Exception e) {
            // log but don't fail
        }
    }
}