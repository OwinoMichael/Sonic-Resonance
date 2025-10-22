package com.sonicres.demo.features.audio;

import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class SimpleFingerprintService implements FingerprintService {

    @Override
    public FingerprintResult fingerprintAndMatch(File wavFile) throws Exception {
        // TODO: Replace with real fingerprinting (Chromaprint/AcoustID, Dejavu, etc.)
        // For demo, return a fake result
        FingerprintResult r = new FingerprintResult();
        r.setTrackName("Demo Song");
        r.setArtist("Demo Artist");
        r.setConfidence(0.85);

        return r;
    }
}