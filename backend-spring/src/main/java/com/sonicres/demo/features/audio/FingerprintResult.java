package com.sonicres.demo.features.audio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class FingerprintResult {
    private String trackName;
    private String artist;
    private Double confidence;

    public FingerprintResult() {}

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

    // ✅ Add these getters
    public String getTrackName() {
        return trackName;
    }

    public String getArtist() {
        return artist;
    }

    public Double getConfidence() {
        return confidence;
    }

    public String toJSON() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS, true);
            ObjectWriter ow = mapper.writerWithDefaultPrettyPrinter();
            return ow.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }
}


