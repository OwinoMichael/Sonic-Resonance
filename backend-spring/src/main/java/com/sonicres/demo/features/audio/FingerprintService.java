package com.sonicres.demo.features.audio;

import java.io.File;

public interface FingerprintService {

    FingerprintResult fingerprintAndMatch(File wavFile) throws Exception;
}
