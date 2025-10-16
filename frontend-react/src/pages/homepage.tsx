import React, { useState } from 'react';
import { Mic, Music2, Library, User, Search, TrendingUp, X, ExternalLink, Clock } from 'lucide-react';

export default function ShazamUI() {
  const [isListening, setIsListening] = useState(false);
  const [currentPage, setCurrentPage] = useState('home');
  
  const matchResults = [
    {
      id: 1,
      title: "Blinding Lights",
      artist: "The Weeknd",
      album: "After Hours",
      confidence: 98,
      duration: "3:20",
      year: 2020,
      links: { youtube: "#", spotify: "#", apple: "#", soundcloud: "#" }
    },
    {
      id: 2,
      title: "Blinding Light (Cover)",
      artist: "The Weekend Tribute",
      album: "Covers",
      confidence: 76,
      duration: "3:18",
      year: 2021,
      links: { youtube: "#", spotify: "#" }
    }
  ];

  const library = [
    { id: 1, title: "Shape of You", artist: "Ed Sheeran", date: "Today" },
    { id: 2, title: "Starboy", artist: "The Weeknd", date: "Yesterday" },
    { id: 3, title: "Levitating", artist: "Dua Lipa", date: "2 days ago" }
  ];

  const handleListenClick = () => {
    setIsListening(true);
    setTimeout(() => {
      setIsListening(false);
      setCurrentPage(Math.random() > 0.3 ? 'matches' : 'no-match');
    }, 3000);
  };

  return (
    <div className="app-container">
      <div className="app-background">
        <div className="gradient-shape gradient-shape-1"></div>
        <div className="gradient-shape gradient-shape-2"></div>
        <div className="gradient-shape gradient-shape-3"></div>
      </div>

      <div className="main-container">
        <header className="app-header">
          <div className="header-glass">
            <div className="header-left">
              <div className="logo-icon">
                <Music2 className="w-5 h-5 text-yellow-400" />
              </div>
              <h1 className="logo-text">Sonic Resonance</h1>
            </div>
            <div className="header-right">
              <button className="header-btn">
                <Search className="w-5 h-5 text-gray-300" />
              </button>
              <button className="header-btn header-btn-active">
                <User className="w-5 h-5 text-yellow-400" />
              </button>
            </div>
          </div>
        </header>

        {currentPage === 'home' && (
          <main className="main-content">
            <div className="content-header">
              <h2 className="main-title">
                {isListening ? 'Listening to Music' : 'Discover Any Song'}
              </h2>
              <p className="main-subtitle">
                {isListening ? 'Hold your device near the audio source' : 'Tap the button to identify music instantly'}
              </p>
            </div>

            <div className="button-container">
              {isListening && (
                <>
                  <div className="glow-ring glow-ring-1"></div>
                  <div className="glow-ring glow-ring-2"></div>
                </>
              )}
              
              <button
                onClick={handleListenClick}
                className={`listen-button ${isListening ? 'listening' : ''}`}
                disabled={isListening}
              >
                <div className="glass-highlight"></div>
                <div className="button-icon">
                  {isListening ? (
                    <div className="icon-wrapper">
                      <Music2 className="w-20 h-20 text-yellow-400 animate-pulse" strokeWidth={1.5} />
                      <div className="spinner-wrapper">
                        <div className="spinner"></div>
                      </div>
                    </div>
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

            <div className="activity-card" onClick={() => setCurrentPage('library')}>
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
        )}

        {currentPage === 'matches' && (
          <main className="matches-page">
            <div className="page-header">
              <button onClick={() => setCurrentPage('home')} className="back-btn">
                <X className="w-6 h-6" />
              </button>
              <h2 className="page-title">Match Results</h2>
              <div className="w-6"></div>
            </div>

            <div className="matches-container">
              <p className="matches-subtitle">Found {matchResults.length} possible matches</p>
              
              <div className="matches-list">
                {matchResults.map((match, index) => (
                  <div key={match.id} className="match-card">
                    <div className="match-rank">#{index + 1}</div>
                    
                    <div className="match-cover">
                      <div style={{width: '80px', height: '80px', borderRadius: '1rem', background: 'linear-gradient(135deg, #4a8ca8, #1a4d5c)'}}></div>
                      <div className="confidence-badge">{match.confidence}%</div>
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
                      {match.links.youtube && (
                        <a href={match.links.youtube} className="platform-link youtube">
                          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
                          </svg>
                        </a>
                      )}
                      {match.links.spotify && (
                        <a href={match.links.spotify} className="platform-link spotify">
                          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2z"/>
                          </svg>
                        </a>
                      )}
                      {match.links.apple && (
                        <a href={match.links.apple} className="platform-link apple">
                          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
                          </svg>
                        </a>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              <button onClick={() => setCurrentPage('home')} className="try-again-btn">
                Try Another Song
              </button>
            </div>
          </main>
        )}

        {currentPage === 'no-match' && (
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

              <button onClick={() => setCurrentPage('home')} className="try-again-btn">
                <Mic className="w-5 h-5" />
                Try Again
              </button>
            </div>
          </main>
        )}

        {currentPage === 'library' && (
          <main className="library-page">
            <div className="page-header">
              <button onClick={() => setCurrentPage('home')} className="back-btn">
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
        )}

        <nav className="bottom-nav">
          <div className="nav-glass">
            <button className={`nav-btn ${currentPage === 'home' ? 'nav-btn-active' : ''}`} onClick={() => setCurrentPage('home')}>
              <div className={`nav-btn-bg ${currentPage === 'home' ? 'nav-btn-bg-active' : ''}`}>
                <Mic className={`w-6 h-6 ${currentPage === 'home' ? 'text-yellow-400' : 'text-gray-300'}`} />
              </div>
              <span className={`nav-label ${currentPage === 'home' ? 'nav-label-active' : ''}`}>Listen</span>
            </button>
            
            <button className={`nav-btn ${currentPage === 'library' ? 'nav-btn-active' : ''}`} onClick={() => setCurrentPage('library')}>
              <div className={`nav-btn-bg ${currentPage === 'library' ? 'nav-btn-bg-active' : ''}`}>
                <Library className={`w-6 h-6 ${currentPage === 'library' ? 'text-yellow-400' : 'text-gray-300'}`} />
              </div>
              <span className={`nav-label ${currentPage === 'library' ? 'nav-label-active' : ''}`}>Library</span>
            </button>
            
            <button className="nav-btn">
              <div className="nav-btn-bg">
                <TrendingUp className="w-6 h-6 text-gray-300" />
              </div>
              <span className="nav-label">Charts</span>
            </button>
          </div>
        </nav>
      </div>
    </div>
  );
}