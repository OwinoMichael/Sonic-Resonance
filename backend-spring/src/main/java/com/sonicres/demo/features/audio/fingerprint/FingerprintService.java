package com.sonicres.demo.features.audio.fingerprint;

import java.io.File;

public interface FingerprintService {

    FingerprintResult fingerprintAndMatch(File wavFile) throws Exception;
}
