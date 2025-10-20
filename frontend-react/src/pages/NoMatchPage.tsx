import React from 'react';
import { Music2, X, Mic } from 'lucide-react';
import AppLayout from '../components/AppLayout';

interface NoMatchPageProps {
  navigate: (path: string) => void;
}

export default function NoMatchPage({ navigate }: NoMatchPageProps) {
  return (
    <AppLayout currentRoute="/" navigate={navigate}>
      <main className="no-match-page">
        <div className="no-match-content">
          <div className="no-match-icon">
            <Music2 className="w-24 h-24 text-gray-500" strokeWidth={1} />
            <X className="w-12 h-12 text-red-500 no-match-x" strokeWidth={3} />
          </div>
          
          <h2 className="no-match-title">No Match Found</h2>
          <p className="no-match-subtitle">
            We couldn't identify this song. Try again in a quieter environment.
          </p>

          <div className="no-match-tips">
            <h3 className="tips-title">Tips for better results:</h3>
            <ul className="tips-list">
              <li>Hold your device closer to the audio source</li>
              <li>Reduce background noise</li>
              <li>Make sure the music is playing at a good volume</li>
              <li>Try listening for at least 10 seconds</li>
            </ul>
          </div>

          <button onClick={() => navigate('/')} className="try-again-btn">
            <Mic className="w-5 h-5" />
            Try Again
          </button>
        </div>
      </main>
    </AppLayout>
  );
}