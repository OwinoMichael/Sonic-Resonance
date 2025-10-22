export interface RecordingCallbacks {
  onConnected?: () => void;
  onRecording?: (timeRemaining: number) => void;
  onProcessing?: () => void;
  onResult?: (result: FingerprintResult) => void;
  onError?: (error: string) => void;
  onComplete?: () => void;
}

export interface FingerprintResult {
  type: 'result' | 'no-match';
  matches?: Array<{
    trackId: string;
    title: string;
    artist: string;
    album?: string;
    confidence: number;
    duration?: string;
    year?: number;
    links?: {
      youtube?: string;
      spotify?: string;
      apple?: string;
      soundcloud?: string;
    };
  }>;
  message?: string;
}

export class AudioRecorderService {
  private websocketUrl: string;
  private recordingDuration: number;
  private websocket: WebSocket | null = null;
  private mediaRecorder: MediaRecorder | null = null;
  private audioStream: MediaStream | null = null;
  private recordingTimer: NodeJS.Timeout | null = null;
  private countdownTimer: NodeJS.Timeout | null = null;
  private isRecording: boolean = false;
  private callbacks: RecordingCallbacks = {};

  constructor(websocketUrl: string, recordingDuration: number = 10000) {
    this.websocketUrl = websocketUrl;
    this.recordingDuration = recordingDuration;
  }

  /**
   * Register callbacks for recording events
   */
  public setCallbacks(callbacks: RecordingCallbacks): void {
    this.callbacks = callbacks;
  }

  /**
   * Start recording audio
   */
  public async startRecording(): Promise<void> {
    if (this.isRecording) {
      console.warn('Already recording');
      return;
    }

    try {
      // Step 1: Connect WebSocket
      await this.connectWebSocket();

      // Step 2: Request microphone access
      this.audioStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          sampleRate: 44100,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });

      // Step 3: Create MediaRecorder
      const mimeType = this.getSupportedMimeType();
      if (!mimeType) {
        throw new Error('No supported audio format found');
      }

      this.mediaRecorder = new MediaRecorder(this.audioStream, {
        mimeType,
        audioBitsPerSecond: 128000,
      });

      // Step 4: Handle audio data chunks
      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0 && this.websocket?.readyState === WebSocket.OPEN) {
          this.websocket.send(event.data);
        }
      };

      this.mediaRecorder.onstop = () => {
        this.handleRecordingComplete();
      };

      // Step 5: Start recording
      this.mediaRecorder.start(250); // Send chunks every 250ms
      this.isRecording = true;

      // Step 6: Start countdown timer
      this.startCountdown();

      // Step 7: Auto-stop after duration
      this.recordingTimer = setTimeout(() => {
        this.stopRecording();
      }, this.recordingDuration);

    } catch (error) {
      console.error('Error starting recording:', error);
      this.cleanup();
      this.callbacks.onError?.(`Failed to start recording: ${error}`);
      throw error;
    }
  }

  /**
   * Stop recording manually
   */
  public stopRecording(): void {
    if (!this.isRecording) {
      return;
    }

    if (this.recordingTimer) {
      clearTimeout(this.recordingTimer);
      this.recordingTimer = null;
    }

    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = null;
    }

    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop();
    }

    this.isRecording = false;
  }

  /**
   * Connect to WebSocket server
   */
  private connectWebSocket(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.websocket = new WebSocket(this.websocketUrl);
        this.websocket.binaryType = 'blob';

        this.websocket.onopen = () => {
          console.log('✓ WebSocket connected');
          resolve();
        };

        this.websocket.onmessage = (event) => {
          this.handleWebSocketMessage(event.data);
        };

        this.websocket.onerror = (error) => {
          console.error('WebSocket error:', error);
          reject(new Error('WebSocket connection failed'));
        };

        this.websocket.onclose = (event) => {
          console.log('WebSocket closed:', event.code, event.reason);
          this.cleanup();
        };

        // Timeout if connection takes too long
        setTimeout(() => {
          if (this.websocket?.readyState !== WebSocket.OPEN) {
            reject(new Error('WebSocket connection timeout'));
          }
        }, 5000);

      } catch (error) {
        reject(error);
      }
    });
  }

  /**
   * Handle WebSocket messages from server
   */
  private handleWebSocketMessage(data: string): void {
    try {
      const message = JSON.parse(data);

      switch (message.type) {
        case 'connected':
          console.log('Server ready:', message.sessionId);
          this.callbacks.onConnected?.();
          break;

        case 'ack':
          // Server acknowledged audio chunk
          console.log(`Received ${message.bytes} bytes`);
          break;

        case 'processing':
          console.log('Server processing audio...');
          this.callbacks.onProcessing?.();
          break;

        case 'result':
          console.log('✅ Received result:', message);
          this.callbacks.onResult?.(message as FingerprintResult);
          this.callbacks.onComplete?.();
          // Cleanup after result received
          setTimeout(() => this.cleanup(), 100);
          break;

        case 'no-match':
          console.log('⚠️  No match found');
          this.callbacks.onResult?.({
            type: 'no-match',
            message: message.message || 'No match found',
          });
          this.callbacks.onComplete?.();
          // Cleanup after result received
          setTimeout(() => this.cleanup(), 100);
          break;

        case 'error':
          console.error('❌ Server error:', message.message);
          this.callbacks.onError?.(message.message);
          this.cleanup();
          break;

        default:
          console.warn('Unknown message type:', message.type);
      }
    } catch (error) {
      console.error('Error parsing WebSocket message:', error);
    }
  }

  /**
   * Handle recording completion
   */
  private handleRecordingComplete(): void {
    console.log('🎵 Recording complete, sending "done" signal');

    // Send "done" message to server
    if (this.websocket?.readyState === WebSocket.OPEN) {
      const doneMessage = JSON.stringify({ type: 'done' });
      console.log('📤 Sending:', doneMessage);
      this.websocket.send(doneMessage);
    } else {
      console.error('❌ WebSocket not open when trying to send "done" signal');
      console.error('WebSocket state:', this.websocket?.readyState);
      this.callbacks.onError?.('Connection lost during recording');
    }

    // Stop audio stream
    if (this.audioStream) {
      this.audioStream.getTracks().forEach(track => {
        track.stop();
        console.log('🎤 Stopped audio track');
      });
      this.audioStream = null;
    }
  }

  /**
   * Start countdown timer
   */
  private startCountdown(): void {
    let timeRemaining = Math.floor(this.recordingDuration / 1000);
    
    this.callbacks.onRecording?.(timeRemaining);

    this.countdownTimer = setInterval(() => {
      timeRemaining -= 1;
      this.callbacks.onRecording?.(timeRemaining);

      if (timeRemaining <= 0 && this.countdownTimer) {
        clearInterval(this.countdownTimer);
        this.countdownTimer = null;
      }
    }, 1000);
  }

  /**
   * Get supported MIME type for MediaRecorder
   */
  private getSupportedMimeType(): string | null {
    const types = [
      'audio/webm;codecs=opus',
      'audio/webm',
      'audio/ogg;codecs=opus',
      'audio/mp4',
    ];

    for (const type of types) {
      if (MediaRecorder.isTypeSupported(type)) {
        console.log('Using MIME type:', type);
        return type;
      }
    }

    return null;
  }

  /**
   * Clean up resources
   */
  private cleanup(): void {
    console.log('🧹 Cleanup called');
    
    if (this.recordingTimer) {
      clearTimeout(this.recordingTimer);
      this.recordingTimer = null;
      console.log('   - Cleared recording timer');
    }

    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = null;
      console.log('   - Cleared countdown timer');
    }

    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop();
      console.log('   - Stopped MediaRecorder');
    }
    this.mediaRecorder = null;

    if (this.audioStream) {
      this.audioStream.getTracks().forEach(track => track.stop());
      this.audioStream = null;
      console.log('   - Stopped audio stream');
    }

    // IMPORTANT: Only close WebSocket if we're done processing
    // Don't close during recording or processing
    if (this.websocket && !this.isRecording) {
      if (this.websocket.readyState === WebSocket.OPEN || 
          this.websocket.readyState === WebSocket.CONNECTING) {
        console.log('   - Closing WebSocket');
        this.websocket.close();
      }
      this.websocket = null;
    } else if (this.websocket && this.isRecording) {
      console.log('   - Keeping WebSocket open (still recording)');
    }

    this.isRecording = false;
    console.log('✅ Cleanup complete');
  }

  /**
   * Check if currently recording
   */
  public getIsRecording(): boolean {
    return this.isRecording;
  }

  /**
   * Force cleanup (call on unmount)
   */
  public destroy(): void {
    this.cleanup();
  }
}