package com.sonicres.demo.features.audio;

import org.springframework.web.socket.WebSocketSession;

import java.io.*;

public class SessionAudioBuffer {

    private final WebSocketSession session;
    private final File tempFile;
    private final OutputStream outputStream;

    public SessionAudioBuffer(WebSocketSession session, File tempFile, OutputStream outputStream) throws IOException {
        this.session = session;
        this.tempFile = File.createTempFile("audio-stream-" + session.getId() + "-", ".raw");
        this.outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));;
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
        outputStream.flush();
        outputStream.close();
    }

    public void closeSilently() {
        try { outputStream.close(); } catch (Exception ignored) {}
        if (tempFile.exists()) tempFile.delete();
    }
}
