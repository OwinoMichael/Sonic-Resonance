import React, { useEffect, useState } from 'react';
import { Mic, Library, TrendingUp, Music2 } from 'lucide-react';
import AppLayout from '../components/AppLayout';
import { AudioRecorderService, type FingerprintResult } from '@/services/AudioRecorderService';

interface HomePageProps {
  navigate: (path: string) => void;
}

// WebSocket URL - adjust based on your environment
const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/audio';

export default function HomePage({ navigate }: HomePageProps) {
  const [recorder] = useState(() => new AudioRecorderService(WS_URL, 10000));
  const [isStarting, setIsStarting] = useState(false);

  useEffect(() => {
    // Set up callbacks for the recorder
    recorder.setCallbacks({
      onConnected: () => {
        console.log('✓ Connected to audio server');
        setIsStarting(false);
      },
      onRecording: (timeRemaining) => {
        console.log(`Recording... ${timeRemaining}s remaining`);
        // Only navigate on first recording callback
        if (timeRemaining === 10) {
          navigate('/listening');
        }
      },
      onProcessing: () => {
        console.log('Processing audio...');
        // Stay on listening page during processing
      },
      onResult: (result: FingerprintResult) => {
        console.log('Received result from server:', result);
        if (result.type === 'result' && result.matches && result.matches.length > 0) {
          // Store matches in sessionStorage for MatchesPage to use
          sessionStorage.setItem('matchResults', JSON.stringify(result.matches));
          navigate('/matches');
        } else {
          navigate('/no-match');
        }
      },
      onError: (error) => {
        console.error('Recording error:', error);
        alert(`Error: ${error}`);
        setIsStarting(false);
        navigate('/');
      },
      onComplete: () => {
        console.log('Recording complete');
        setIsStarting(false);
      }
    });

    // Cleanup on unmount - but don't destroy if recording
    return () => {
      console.log('HomePage unmounting...');
      // Only destroy if not recording
      if (!recorder.getIsRecording()) {
        recorder.destroy();
      }
    };
  }, [recorder, navigate]);

  const handleListenClick = async () => {
    if (isStarting) return;

    setIsStarting(true);
    
    try {
      await recorder.startRecording();
    } catch (error) {
      console.error('Failed to start recording:', error);
      setIsStarting(false);
      alert('Failed to start recording. Please check microphone permissions.');
    }
  };

  return (
    <AppLayout currentRoute="/" navigate={navigate}>
      <main className="main-content">
        <div className="content-header">
          <h2 className="main-title">Discover Any Song</h2>
          <p className="main-subtitle">Tap the button to identify music instantly</p>
        </div>

        <div className="button-container">
          <div className="button-container">
            <button 
              onClick={handleListenClick} 
              className="listen-button"
              disabled={isStarting}
            >
              <div className="glass-highlight"></div>
              <div className="button-icon">
                {isStarting ? (
                  <Music2 className="w-20 h-20 text-yellow-400 animate-pulse" strokeWidth={1.5} />
                ) : (
                  <Mic className="w-20 h-20 text-yellow-400 icon-mic" strokeWidth={1.5} />
                )}
              </div>
              <div className="glass-reflection"></div>
              <div className="edge-glow"></div>
            </button>

          <div className="glass-particle particle-1"></div>
          <div className="glass-particle particle-2"></div>
          <div className="glass-particle particle-3"></div>
          </div>
        </div>

        <div className="activity-card" onClick={() => navigate('/library')}>
          <div className="activity-icon">
            <Library className="w-6 h-6 text-sky-400" />
          </div>
          <div className="activity-content">
            <p className="activity-title">Your Library</p>
            <p className="activity-subtitle">View all identified songs</p>
          </div>
          <TrendingUp className="w-5 h-5 text-yellow-400" />
        </div>
      </main>
    </AppLayout>
  );
}