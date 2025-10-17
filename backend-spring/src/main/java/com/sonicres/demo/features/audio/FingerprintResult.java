package com.sonicres.demo.features.audio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class FingerprintResult {

    private String trackName;
    private String artist;
    private Double confidence;

    public FingerprintResult() {
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String toJSON() {
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            return ow.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }

    @Override
    public String toString() {
        return "FingerprintResult{" +
                "trackName='" + trackName + '\'' +
                ", artist='" + artist + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
