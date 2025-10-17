package com.sonicres.demo.features.audio;

import org.springframework.stereotype.Service;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service for decoding Opus/WebM audio files using Jave2 (FFmpeg wrapper)
 * This is an alternative to calling FFmpeg directly via ProcessBuilder
 */
@Service
public class AudioDecoderService {

    /**
     * Decode Opus/WebM audio to PCM WAV format
     * Optimized for fingerprinting (mono, 44.1kHz)
     *
     * @param opusData byte array containing the Opus audio data
     * @return byte array containing decoded WAV audio
     * @throws IOException if file operations fail
     * @throws EncoderException if decoding fails
     */
    public byte[] decodeOpusToWav(byte[] opusData) throws IOException, EncoderException {
        Path tempInput = null;
        Path tempOutput = null;

        try {
            // Create temporary files
            tempInput = Files.createTempFile("opus_decode_input", ".webm");
            tempOutput = Files.createTempFile("opus_decode_output", ".wav");

            // Write input data
            Files.write(tempInput, opusData);

            // Configure audio output for fingerprinting
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("pcm_s16le");        // PCM 16-bit little-endian
            audio.setChannels(1);               // Mono
            audio.setSamplingRate(44100);       // 44.1kHz
            audio.setBitRate(705600);           // 44100 * 16 * 1

            // Configure encoding
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("wav");
            attrs.setAudioAttributes(audio);

            // Decode the audio
            MultimediaObject source = new MultimediaObject(tempInput.toFile());
            Encoder encoder = new Encoder();
            encoder.encode(source, tempOutput.toFile(), attrs);

            // Read and return the decoded data
            return Files.readAllBytes(tempOutput);

        } finally {
            // Clean up temporary files
            if (tempInput != null) {
                try { Files.deleteIfExists(tempInput); } catch (IOException ignored) {}
            }
            if (tempOutput != null) {
                try { Files.deleteIfExists(tempOutput); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Decode Opus/WebM audio file directly to WAV file
     *
     * @param inputFile input Opus/WebM file
     * @param outputFile output WAV file
     * @throws IOException if file operations fail
     * @throws EncoderException if decoding fails
     */
    public void decodeOpusFileToWav(File inputFile, File outputFile)
            throws IOException, EncoderException {

        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("pcm_s16le");
        audio.setChannels(1);
        audio.setSamplingRate(44100);

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("wav");
        attrs.setAudioAttributes(audio);

        MultimediaObject source = new MultimediaObject(inputFile);
        Encoder encoder = new Encoder();
        encoder.encode(source, outputFile, attrs);
    }

    /**
     * Decode with custom audio parameters
     *
     * @param inputFile input audio file
     * @param outputFile output WAV file
     * @param channels number of audio channels (1=mono, 2=stereo)
     * @param sampleRate sample rate in Hz
     * @throws IOException if file operations fail
     * @throws EncoderException if decoding fails
     */
    public void decodeWithCustomParams(File inputFile, File outputFile,
                                       int channels, int sampleRate)
            throws IOException, EncoderException {

        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("pcm_s16le");
        audio.setChannels(channels);
        audio.setSamplingRate(sampleRate);

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("wav");
        attrs.setAudioAttributes(audio);

        MultimediaObject source = new MultimediaObject(inputFile);
        Encoder encoder = new Encoder();
        encoder.encode(source, outputFile, attrs);
    }
}