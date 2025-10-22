import React from 'react';
import { X, Clock, ExternalLink } from 'lucide-react';
import AppLayout from '../components/AppLayout';

interface LibraryPageProps {
  navigate: (path: string) => void;
}

export default function LibraryPage({ navigate }: LibraryPageProps) {
  const library = [
    { id: 1, title: "Shape of You", artist: "Ed Sheeran", date: "Today" },
    { id: 2, title: "Starboy", artist: "The Weeknd", date: "Yesterday" },
    { id: 3, title: "Levitating", artist: "Dua Lipa", date: "2 days ago" }
  ];

  return (
    <AppLayout currentRoute="/library" navigate={navigate}>
      <main className="library-page">
        <div className="page-header">
          <button onClick={() => navigate('/')} className="back-btn">
            <X className="w-6 h-6" />
          </button>
          <h2 className="page-title">My Library</h2>
          <div className="w-6"></div>
        </div>

        <div className="library-container">
          <p className="library-subtitle">{library.length} songs identified</p>
          
          <div className="library-list">
            {library.map((song) => (
              <div key={song.id} className="library-item">
                <div className="library-cover">
                  <div style={{width: '64px', height: '64px', borderRadius: '0.75rem', background: 'linear-gradient(135deg, #fdb924, #4a8ca8)'}}></div>
                </div>

                <div className="library-info">
                  <h3 className="library-title">{song.title}</h3>
                  <p className="library-artist">{song.artist}</p>
                  <div className="library-time">
                    <Clock className="w-3 h-3" />
                    <span>{song.date}</span>
                  </div>
                </div>

                <button className="library-action">
                  <ExternalLink className="w-5 h-5" />
                </button>
              </div>
            ))}
          </div>
        </div>
      </main>
    </AppLayout>
  );
}