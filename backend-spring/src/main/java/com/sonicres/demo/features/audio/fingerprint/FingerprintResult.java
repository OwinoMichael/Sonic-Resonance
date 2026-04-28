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
    private String releaseDate;
    private Integer durationMs;
    private String label;
    private String spotifyTrackId;
    private String deezerTrackId;
    private String coverArtUrl;

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

    public String getSpotifyUrl() {
        return spotifyTrackId != null
                ? "https://open.spotify.com/track/" + spotifyTrackId
                : null;
    }

    public String getDeezerUrl() {
        return deezerTrackId != null
                ? "https://www.deezer.com/track/" + deezerTrackId
                : null;
    }

    public String getYouTubeSearchUrl() {
        if (artist != null && trackName != null) {
            String query = (artist + " " + trackName)
                    .replace(" ", "+");
            return "https://www.youtube.com/results?search_query=" + query;
        }
        return null;
    }

    public String getDeezerTrackId() {
        return deezerTrackId;
    }

    public void setDeezerTrackId(String deezerTrackId) {
        this.deezerTrackId = deezerTrackId;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getSpotifyTrackId() {
        return spotifyTrackId;
    }

    public void setSpotifyTrackId(String spotifyTrackId) {
        this.spotifyTrackId = spotifyTrackId;
    }

    public String getCoverArtUrl() { return coverArtUrl; }
    public void setCoverArtUrl(String coverArtUrl) { this.coverArtUrl = coverArtUrl; }
}


