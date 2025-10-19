import React from 'react';
import { Mic, Library, TrendingUp } from 'lucide-react';
import AppLayout from '../components/AppLayout';

interface HomePageProps {
  navigate: (path: string) => void;
}

export default function HomePage({ navigate }: HomePageProps) {
  const handleListenClick = () => {
    navigate('/listening');
    setTimeout(() => {
      navigate(Math.random() > 0.3 ? '/matches' : '/no-match');
    }, 3000);
  };

  return (
    <AppLayout currentRoute="/" navigate={navigate}>
      <main className="main-content">
        <div className="content-header">
          <h2 className="main-title">Discover Any Song</h2>
          <p className="main-subtitle">Tap the button to identify music instantly</p>
        </div>

        <div className="button-container">
          <button onClick={handleListenClick} className="listen-button">
            <div className="glass-highlight"></div>
            <div className="button-icon">
              <Mic className="w-20 h-20 text-yellow-400 icon-mic" strokeWidth={1.5} />
            </div>
            <div className="glass-reflection"></div>
            <div className="edge-glow"></div>
          </button>

          <div className="glass-particle particle-1"></div>
          <div className="glass-particle particle-2"></div>
          <div className="glass-particle particle-3"></div>
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