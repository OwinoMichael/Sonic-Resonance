import React, { useEffect, useState } from 'react';
import { X } from 'lucide-react';
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
    apple?: string;
    deezer?: string;   // ← add
    soundcloud?: string;
  };
  coverArtUrl?: string;
}

export default function MatchesPage({ navigate }: MatchesPageProps) {
  const [matches, setMatches] = useState<Match[]>([]);

  useEffect(() => {
    // Load matches from sessionStorage
    const storedMatches = sessionStorage.getItem("matchResults");
    if (storedMatches) {
      try {
        const parsed = JSON.parse(storedMatches);
        setMatches(parsed);
      } catch (error) {
        console.error("Error parsing match results:", error);
        setMatches(getDemoMatches());
      }
    } else {
      setMatches(getDemoMatches());
    }
  }, []);

  const getDemoMatches = (): Match[] => [
    {
      title: "Blinding Lights",
      artist: "The Weeknd",
      album: "After Hours",
      confidence: 98,
      duration: "3:20",
      year: 2020,
      links: { youtube: "#", spotify: "#", apple: "#" },
    },
    {
      title: "Blinding Light (Cover)",
      artist: "The Weekend Tribute",
      album: "Covers",
      confidence: 76,
      duration: "3:18",
      year: 2021,
      links: { youtube: "#", spotify: "#" },
    },
  ];

  return (
    <AppLayout currentRoute="/" navigate={navigate}>
      <main className="matches-page">
        <div className="page-header">
          <button onClick={() => navigate("/")} className="back-btn">
            <X className="w-6 h-6" />
          </button>
          <h2 className="page-title">Match Results</h2>
          <div className="w-6"></div>
        </div>

        <div className="matches-container">
          <p className="matches-subtitle">
            Found {matches.length} possible matches
          </p>

          <div className="matches-list">
            {matches.map((match, index) => (
              <div key={index} className="match-card">
                <div className="match-rank">#{index + 1}</div>

                <div className="match-cover">
                  {match.coverArtUrl ? (
                    <img src={match.coverArtUrl} alt={match.title}
                      style={{ width: "80px", height: "80px", borderRadius: "1rem", objectFit: "cover" }} />
                  ) : (
                    <div style={{ width: "80px", height: "80px", borderRadius: "1rem",
                      background: "linear-gradient(135deg, #4a8ca8, #1a4d5c)" }}></div>
                  )}
                  <div className="confidence-badge">
                    {match.confidence}%
                  </div>
                </div>

                <div className="match-info">
                  <h3 className="match-title">{match.title}</h3>
                  <p className="match-artist">{match.artist}</p>
                  <div className="match-meta">
                    <span>{match.album}</span>
                    <span>•</span>
                    <span>{match.year}</span>
                    <span>•</span>
                    <span>{match.duration}</span>
                  </div>
                </div>

                <div className="match-links">
                  {match.links?.youtube && (
                    <a href={match.links.youtube} className="platform-link youtube" target='_blank'>
                      {/* YouTube Icon */}
                      <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
                      </svg>
                    </a>
                  )}
                  {match.links?.spotify && (
                    <a href={match.links.spotify} className="platform-link spotify" target='_blank'>
                      {/* Spotify Icon */}
                      <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2z"/>
                      </svg>
                    </a>
                  )}
                  {match.links?.deezer && (
                    <a href={match.links.deezer} className="platform-link deezer" target="_blank" rel="noreferrer">
                      <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M18.81 11.283H24v1.422h-5.19zm0-2.79H24v1.422h-5.19zm0 5.58H24v1.422h-5.19zM0 16.495h5.19v-1.422H0zm6.248 0h5.19v-1.422h-5.19zm6.31 0H17.747v-1.422h-5.19zm6.252 0H24v-1.422h-5.19zM6.248 13.705h5.19v-1.422h-5.19zm6.31 0H17.747v-1.422h-5.19zM6.248 10.915h5.19v-1.422h-5.19z"/>
                      </svg>
                    </a>
                  )}
                </div>
              </div>
            ))}
          </div>

          <button onClick={() => navigate("/")} className="try-again-btn">
            Try Another Song
          </button>
        </div>
      </main>
    </AppLayout>
  );
}