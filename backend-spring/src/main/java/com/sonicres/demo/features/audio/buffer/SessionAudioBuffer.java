package com.sonicres.demo.features.audio.buffer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;


public class SessionAudioBuffer {

    private final WebSocketSession session;
    private final File tempFile;
    private final OutputStream outputStream;
    private volatile boolean closed = false;
    private long totalBytesWritten = 0;

    private static final Logger Log = LoggerFactory.getLogger(SessionAudioBuffer.class);

    public SessionAudioBuffer(WebSocketSession session) throws IOException {
        this.session = session;
        this.tempFile = File.createTempFile("audio-stream-" + session.getId() + "-", ".raw");
        this.outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));


    }

    public synchronized void append(java.nio.ByteBuffer buffer) throws IOException {
        if (closed) {
            throw new IOException("Buffer already closed for processing");
        }

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        outputStream.write(bytes);
        totalBytesWritten += bytes.length;

        // Flush periodically to prevent memory issues
        if (totalBytesWritten % 50000 == 0) {
            outputStream.flush();
        }
    }

    public File getTempFile() {
        return tempFile;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public long getTotalBytes() {
        return totalBytesWritten;
    }

    // Finalize and provide file for processing
    public synchronized void closeForProcessing() throws IOException {
        if (!closed) {
            outputStream.flush(); // Write memory buffer to disk
            outputStream.close(); // Release file handle
            closed = true;
            Log.info("🔒 Buffer closed for processing. Total bytes: " + totalBytesWritten);
        }
    }

    public void closeSilently() {
        try {
            if (!closed) {
                outputStream.close();
                closed = true;
            }
        } catch (Exception ignored) {}

        if (tempFile != null && tempFile.exists()) {
            boolean deleted = tempFile.delete();
            if (deleted) {
                Log.info("🗑️  Deleted temp file: " + tempFile.getName());
            }
        }
    }
}
