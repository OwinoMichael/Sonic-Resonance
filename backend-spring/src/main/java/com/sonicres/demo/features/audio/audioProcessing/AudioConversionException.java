package com.sonicres.demo.features.audio.audioProcessing;

public class AudioConversionException extends Exception {
    public AudioConversionException(String message) {
        super(message);
    }

    public AudioConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
