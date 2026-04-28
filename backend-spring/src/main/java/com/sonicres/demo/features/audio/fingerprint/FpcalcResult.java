package com.sonicres.demo.features.audio.fingerprint;

public class FpcalcResult {
    public String fingerprint;
    public int duration;

    public FpcalcResult() {
    }

    public FpcalcResult(int duration, String fingerprint) {
        this.duration = duration;
        this.fingerprint = fingerprint;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}
