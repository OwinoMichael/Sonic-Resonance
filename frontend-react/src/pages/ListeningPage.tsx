import React from 'react';
import { Music2 } from 'lucide-react';
import AppLayout from '../components/AppLayout';

interface ListeningPageProps {
  navigate: (path: string) => void;
}

export default function ListeningPage({ navigate }: ListeningPageProps) {
  return (
    <AppLayout currentRoute="/" navigate={navigate}>
      <main className="main-content">
        <div className="content-header">
          <h2 className="main-title">Listening to Music</h2>
          <p className="main-subtitle">Hold your device near the audio source</p>
        </div>

        <div className="button-container">
          <div className="glow-ring glow-ring-1"></div>
          <div className="glow-ring glow-ring-2"></div>
          
          <button className="listen-button listening" disabled>
            <div className="glass-highlight"></div>
            <div className="button-icon">
              <div className="icon-wrapper">
                <Music2 className="w-20 h-20 text-yellow-400 animate-pulse" strokeWidth={1.5} />
                <div className="spinner-wrapper">
                  <div className="spinner"></div>
                </div>
              </div>
            </div>
            <div className="glass-reflection"></div>
            <div className="edge-glow"></div>
          </button>

          <div className="glass-particle particle-1"></div>
          <div className="glass-particle particle-2"></div>
          <div className="glass-particle particle-3"></div>
        </div>
      </main>
    </AppLayout>
  );
}