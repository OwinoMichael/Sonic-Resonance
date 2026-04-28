package com.sonicres.demo.features.audio.fingerprint;

import java.io.File;
import java.util.List;

public interface FingerprintService {

    List<FingerprintResult> fingerprintAndMatch(File wavFile) throws Exception;
}
