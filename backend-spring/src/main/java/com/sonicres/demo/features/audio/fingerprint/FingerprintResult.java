package com.sonicres.demo.features.audio.fingerprint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.HashMap;
import java.util.Map;

public class FingerprintResult {
    private String trackName;
    private String artist;
    private String album;        // ← add this
    private Double confidence;
    private String fingerprint;

    public FingerprintResult() {}

    public FingerprintResult(String trackName, String artist, Double confidence) {
        this.trackName = trackName;
        this.artist = artist;
        this.confidence = confidence;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setConfidence(Double confidence) {
        if (confidence == null || confidence.isNaN() || confidence.isInfinite()) {
            confidence = 0.0;
        }
        this.confidence = confidence;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getTrackName() {
        return trackName;
    }

    public String getArtist() {
        return artist;
    }

    public Double getConfidence() {
        return confidence;
    }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

}


