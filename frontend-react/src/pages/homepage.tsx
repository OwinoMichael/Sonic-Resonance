import React, { useEffect, useState } from 'react';
import { Mic, Library, TrendingUp, Music2 } from 'lucide-react';
import AppLayout from '../components/AppLayout';
import { AudioRecorderService, type FingerprintResult } from '@/services/AudioRecorderService';

interface HomePageProps {
  navigate: (path: string) => void;
}


const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/audio';

export default function HomePage({ navigate }: HomePageProps) {
  const [recorder] = useState(() => new AudioRecorderService(WS_URL, 10000)); // 10s recording
  const [isStarting, setIsStarting] = useState(false);

  useEffect(() => {
    // Set up callbacks for the recorder
    recorder.setCallbacks({
      onConnected: () => {
        //console.log('✓ Connected to audio server');
        setIsStarting(false);
      },
      onRecording: (timeRemaining) => {
        sessionStorage.setItem('timeRemaining', String(timeRemaining));
        sessionStorage.setItem('status', 'Listening...');
        
        // Navigate when recording starts (timeRemaining will be 10)
        if (timeRemaining === 10) { 
          navigate('/listening');
        }
      },

      onProcessing: () => {
        sessionStorage.setItem('status', 'Analyzing audio...');
      },
      onResult: (result: FingerprintResult) => {
        //console.log(result)
        if (result.matches && result.matches.length > 0) {
          const formatted = result.matches.map((m) => ({
            title: m.title,
            artist: m.artist,
            album: m.album,
            confidence: Math.round(m.confidence * 100),
            coverArtUrl: m.coverArtUrl,        // ← add
            year: m.releaseDate ? new Date(m.releaseDate).getFullYear() : undefined,
            duration: m.durationMs ? formatDuration(m.durationMs) : undefined,
            links: m.links,                    // ← add
          }));
          sessionStorage.setItem('matchResults', JSON.stringify(formatted));
          navigate('/matches');
        } else {
          navigate('/no-match');
        }
      },
      onError: (error) => {
        //console.error('Recording error:', error);
        alert(`Error: ${error}`);
        setIsStarting(false);
        navigate('/');
      },
      onComplete: () => {
        //console.log('Recording complete');
        setIsStarting(false);
      }
    });

    // Cleanup on unmount - but don't destroy if recording
    return () => {
      //console.log('HomePage unmounting...');
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
      //console.error('Failed to start recording:', error);
      console.error('', error);
      setIsStarting(false);
      alert('Failed to start recording. Please check microphone permissions.');
    }
  };

  const formatDuration = (ms: number): string => {
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
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