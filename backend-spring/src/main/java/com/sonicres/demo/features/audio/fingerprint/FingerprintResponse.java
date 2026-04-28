package com.sonicres.demo.features.audio.fingerprint;

import java.util.List;

public class FingerprintResponse {
    private List<FingerprintResult> matches;

    public FingerprintResponse(List<FingerprintResult> matches) {
        this.matches = matches;
    }

    public List<FingerprintResult> getMatches() {
        return matches;
    }
}
