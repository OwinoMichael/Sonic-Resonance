package com.sonicres.demo.features.audio;

import org.springframework.web.socket.WebSocketSession;

import java.io.*;

public class SessionAudioBuffer {

    private final WebSocketSession session;
    private final File tempFile;
    private final OutputStream outputStream;



    public SessionAudioBuffer(WebSocketSession session) throws IOException {
        this.session = session;
        this.tempFile = File.createTempFile("audio-stream-" + session.getId() + "-", ".raw");
        this.outputStream = new BufferedOutputStream(new FileOutputStream(tempFile)); // Write on Memory, when it gets large, flush to Disk
    }

    public synchronized void append(java.nio.ByteBuffer buffer) throws IOException {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        outputStream.write(bytes);
    }

    public File getTempFile(){
        return tempFile;
    }

    public WebSocketSession getSession(){
        return session;
    }

    //finalize and provide file for processing
    public synchronized void closeForProcessing() throws IOException {
        outputStream.flush(); //writes memory on the buffer to the file on the disk
        outputStream.close(); //flushes and releases file being handled. no more writting done
    }

    public void closeSilently() {
        try { outputStream.close(); } catch (Exception ignored) {}
        if (tempFile.exists()) tempFile.delete();
    }
}
