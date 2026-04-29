import React, { useEffect, useState } from 'react';
import { X, ChevronRight } from 'lucide-react';
import AppLayout from '../components/AppLayout';

interface MatchesPageProps {
  navigate: (path: string) => void;
}

interface Match {
  trackId?: string;
  title: string;
  artist: string;
  album?: string;
  confidence: number;
  duration?: string;
  year?: number;
  links?: {
    youtube?: string;
    spotify?: string;
    deezer?: string;
    soundcloud?: string;
  };
  coverArtUrl?: string;
}

export default function MatchesPage({ navigate }: MatchesPageProps) {
  const [matches, setMatches] = useState<Match[]>([]);

  useEffect(() => {
    const storedMatches = sessionStorage.getItem('matchResults');
    if (storedMatches) {
      try {
        const parsed = JSON.parse(storedMatches);
        setMatches(parsed);
      } catch {
        setMatches(getDemoMatches());
      }
    } else {
      setMatches(getDemoMatches());
    }
  }, []);

  const getDemoMatches = (): Match[] => [
    {
      title: 'Blinding Lights',
      artist: 'The Weeknd',
      album: 'After Hours',
      confidence: 98,
      duration: '3:20',
      year: 2020,
      links: { youtube: '#', spotify: 'https://open.spotify.com/track/0VjIjW4GlUZAMYd2vXMi3b' },
    },
    {
      title: 'Blinding Light (Cover)',
      artist: 'The Weekend Tribute',
      album: 'Covers',
      confidence: 76,
      duration: '3:18',
      year: 2021,
      links: { youtube: '#', spotify: '#' },
    },
  ];

  const handleMatchClick = (match: Match) => {
    sessionStorage.setItem('selectedSong', JSON.stringify(match));
    navigate('/song');
  };

  return (
    <AppLayout currentRoute="/" navigate={navigate}>
      <main className="matches-page">
        <div className="page-header">
          <button onClick={() => navigate('/')} className="back-btn">
            <X className="w-6 h-6" />
          </button>
          <h2 className="page-title">Match Results</h2>
          <div className="w-6" />
        </div>

        <div className="matches-container">
          <p className="matches-subtitle">
            Found {matches.length} possible {matches.length === 1 ? 'match' : 'matches'} — tap to explore
          </p>

          <div className="matches-list">
            {matches.map((match, index) => (
              <div
                key={index}
                className="match-card"
                onClick={() => handleMatchClick(match)}
                style={{ cursor: 'pointer' }}
              >
                <div className="match-rank">#{index + 1}</div>

                <div className="match-cover">
                  {match.coverArtUrl ? (
                    <img
                      src={match.coverArtUrl}
                      alt={match.title}
                      style={{ width: '80px', height: '80px', borderRadius: '1rem', objectFit: 'cover' }}
                    />
                  ) : (
                    <div style={{
                      width: '80px', height: '80px', borderRadius: '1rem',
                      background: 'linear-gradient(135deg, #4a8ca8, #1a4d5c)',
                    }} />
                  )}
                  <div className="confidence-badge">{match.confidence}%</div>
                </div>

                <div className="match-info">
                  <h3 className="match-title">{match.title}</h3>
                  <p className="match-artist">{match.artist}</p>
                  <div className="match-meta">
                    {match.album && <span>{match.album}</span>}
                    {match.year && <><span>•</span><span>{match.year}</span></>}
                    {match.duration && <><span>•</span><span>{match.duration}</span></>}
                  </div>
                  {/* Platform pills */}
                  <div style={{ display: 'flex', gap: '6px', marginTop: '8px', flexWrap: 'wrap' }}>
                    {match.links?.spotify && match.links.spotify !== '#' && (
                      <span style={{
                        fontSize: '10px', padding: '2px 8px', borderRadius: '999px',
                        background: 'rgba(30, 215, 96, 0.15)',
                        border: '1px solid rgba(30, 215, 96, 0.3)',
                        color: '#1ed760', fontWeight: 600,
                      }}>Spotify</span>
                    )}
                    {match.links?.deezer && (
                      <span style={{
                        fontSize: '10px', padding: '2px 8px', borderRadius: '999px',
                        background: 'rgba(253, 185, 36, 0.15)',
                        border: '1px solid rgba(253, 185, 36, 0.3)',
                        color: '#fdb924', fontWeight: 600,
                      }}>Deezer</span>
                    )}
                    {match.links?.youtube && (
                      <span style={{
                        fontSize: '10px', padding: '2px 8px', borderRadius: '999px',
                        background: 'rgba(255, 0, 0, 0.12)',
                        border: '1px solid rgba(255, 0, 0, 0.25)',
                        color: '#ff4444', fontWeight: 600,
                      }}>YouTube</span>
                    )}
                  </div>
                </div>

                {/* Arrow */}
                <div style={{ flexShrink: 0, color: 'rgba(255,255,255,0.25)' }}>
                  <ChevronRight style={{ width: 20, height: 20 }} />
                </div>
              </div>
            ))}
          </div>

          <button onClick={() => navigate('/')} className="try-again-btn">
            Try Another Song
          </button>
        </div>
      </main>
    </AppLayout>
  );
}