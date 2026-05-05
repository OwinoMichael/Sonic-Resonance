import React, { useEffect, useState } from 'react';
import { X, Music2, ExternalLink, Mic2, Disc3 } from 'lucide-react';
import AppLayout from '../components/AppLayout';

interface SongDetailPageProps {
  navigate: (path: string) => void;
}

interface AudioFeatures {
  bpm: number;
  rank: number;
  duration: number;
  explicit: boolean;
  preview: string;
  gain: number;
}

interface SongDetail {
  title: string;
  artist: string;
  album?: string;
  confidence: number;
  duration?: string;
  year?: number;
  coverArtUrl?: string;
  links?: {
    spotify?: string;
    deezer?: string;
    youtube?: string;
  };
}

export default function SongDetailPage({ navigate }: SongDetailPageProps) {
  const [song, setSong] = useState<SongDetail | null>(null);
  const [lyrics, setLyrics] = useState<string | null>(null);
  const [lyricsLoading, setLyricsLoading] = useState(false);
  const [features, setFeatures] = useState<AudioFeatures | null>(null);
  const [featuresLoading, setFeaturesLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'lyrics' | 'vibe'>('lyrics');

  useEffect(() => {
    const stored = sessionStorage.getItem('selectedSong');
    if (stored) {
      const parsed = JSON.parse(stored);
      setSong(parsed);
      fetchLyrics(parsed.artist, parsed.title);
      if (parsed.links?.deezer) {
        const deezerTrackId = parsed.links.deezer.split('/track/')[1];
        if (deezerTrackId) fetchDeezerFeatures(deezerTrackId);
      }
    }
  }, []);

  const fetchLyrics = async (artist: string, title: string) => {
    setLyricsLoading(true);
    try {
      const res = await fetch(
        `https://api.lyrics.ovh/v1/${encodeURIComponent(artist)}/${encodeURIComponent(title)}`
      );
      if (res.ok) {
        const data = await res.json();
        setLyrics(data.lyrics || null);
      } else {
        setLyrics(null);
      }
    } catch {
      setLyrics(null);
    }
    setLyricsLoading(false);
  };

  const fetchDeezerFeatures = async (deezerTrackId: string) => {
    setFeaturesLoading(true);
    try {
      const res = await fetch(`https://api.deezer.com/track/${deezerTrackId}`);
      if (res.ok) {
        const data = await res.json();
        setFeatures({
          bpm: data.bpm,
          rank: data.rank,
          duration: data.duration,
          explicit: data.explicit_lyrics,
          preview: data.preview,
          gain: data.gain,
        });
      }
    } catch {
      setFeatures(null);
    }
    setFeaturesLoading(false);
  };

  if (!song) {
    return (
      <AppLayout currentRoute="/" navigate={navigate}>
        <main className="matches-page">
          <div style={{ textAlign: 'center', padding: '4rem', color: 'rgba(255,255,255,0.4)' }}>
            No song selected
          </div>
        </main>
      </AppLayout>
    );
  }

  return (
    <AppLayout currentRoute="/" navigate={navigate}>
      <main className="matches-page">
        <div className="page-header">
          <button onClick={() => navigate('/matches')} className="back-btn">
            <X className="w-6 h-6" />
          </button>
          <h2 className="page-title">Song Detail</h2>
          <div className="w-6" />
        </div>

        <div style={{ maxWidth: '680px', margin: '0 auto', width: '100%', paddingBottom: '2rem' }}>

          {/* Hero Card */}
          <div style={{
            background: 'rgba(10, 20, 32, 0.6)',
            backdropFilter: 'blur(40px)',
            border: '1px solid rgba(253, 185, 36, 0.15)',
            borderRadius: '1.5rem',
            padding: '1.5rem',
            display: 'flex',
            gap: '1.25rem',
            alignItems: 'center',
            marginBottom: '1.5rem',
          }}>
            <div style={{ position: 'relative', flexShrink: 0 }}>
              {song.coverArtUrl ? (
                <img src={song.coverArtUrl} alt={song.title} style={{
                  width: '100px', height: '100px', borderRadius: '1rem', objectFit: 'cover'
                }} />
              ) : (
                <div style={{
                  width: '100px', height: '100px', borderRadius: '1rem',
                  background: 'linear-gradient(135deg, #1a4d5c, #0a1420)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center'
                }}>
                  <Disc3 style={{ width: 40, height: 40, color: 'rgba(253,185,36,0.5)' }} />
                </div>
              )}
              <div style={{
                position: 'absolute', bottom: '-8px', right: '-8px',
                background: 'rgba(74, 140, 168, 0.9)',
                borderRadius: '0.5rem', padding: '2px 8px',
                fontSize: '11px', fontWeight: 700, color: 'white',
                backdropFilter: 'blur(10px)',
              }}>
                {song.confidence}%
              </div>
            </div>

            <div style={{ flex: 1, minWidth: 0 }}>
              <h3 style={{
                fontSize: '1.4rem', fontWeight: 800, color: 'white',
                margin: 0, marginBottom: '4px', overflow: 'hidden',
                textOverflow: 'ellipsis', whiteSpace: 'nowrap'
              }}>{song.title}</h3>
              <p style={{ color: '#a8c5d1', margin: 0, marginBottom: '8px', fontSize: '1rem' }}>{song.artist}</p>
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', fontSize: '12px', color: 'rgba(255,255,255,0.4)' }}>
                {song.album && <span>{song.album}</span>}
                {song.year && <><span>•</span><span>{song.year}</span></>}
                {song.duration && <><span>•</span><span>{song.duration}</span></>}
              </div>
              <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
                {song.links?.spotify && (
                  <a href={song.links.spotify} target="_blank" rel="noreferrer" className="platform-link spotify">
                    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2z"/>
                    </svg>
                  </a>
                )}
                {song.links?.deezer && (
                  <a href={song.links.deezer} target="_blank" rel="noreferrer" className="platform-link deezer">
                    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M18.81 11.283H24v1.422h-5.19zm0-2.79H24v1.422h-5.19zm0 5.58H24v1.422h-5.19zM0 16.495h5.19v-1.422H0zm6.248 0h5.19v-1.422h-5.19zm6.31 0H17.747v-1.422h-5.19zm6.252 0H24v-1.422h-5.19zM6.248 13.705h5.19v-1.422h-5.19zm6.31 0H17.747v-1.422h-5.19zM6.248 10.915h5.19v-1.422h-5.19z"/>
                    </svg>
                  </a>
                )}
                {song.links?.youtube && (
                  <a href={song.links.youtube} target="_blank" rel="noreferrer" className="platform-link youtube">
                    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
                    </svg>
                  </a>
                )}
              </div>
            </div>
          </div>

          {/* Tabs */}
          <div style={{ display: 'flex', gap: '8px', marginBottom: '1rem' }}>
            {(['lyrics', 'vibe'] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                style={{
                  flex: 1, padding: '10px', borderRadius: '0.75rem',
                  border: activeTab === tab
                    ? '1px solid rgba(253, 185, 36, 0.5)'
                    : '1px solid rgba(255,255,255,0.08)',
                  background: activeTab === tab
                    ? 'rgba(253, 185, 36, 0.15)'
                    : 'rgba(10, 20, 32, 0.3)',
                  color: activeTab === tab ? '#fdb924' : 'rgba(255,255,255,0.5)',
                  fontWeight: 600, fontSize: '14px',
                  cursor: 'pointer', transition: 'all 0.2s',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
                }}
              >
                {tab === 'lyrics'
                  ? <Mic2 style={{ width: 16, height: 16 }} />
                  : <Music2 style={{ width: 16, height: 16 }} />}
                {tab === 'lyrics' ? 'Lyrics' : 'Vibe Check'}
              </button>
            ))}
          </div>

          {/* Lyrics Panel */}
          {activeTab === 'lyrics' && (
            <div style={{
              background: 'rgba(10, 20, 32, 0.4)',
              backdropFilter: 'blur(40px)',
              border: '1px solid rgba(255,255,255,0.08)',
              borderRadius: '1.5rem',
              padding: '1.5rem',
              minHeight: '300px',
            }}>
              {lyricsLoading ? (
                <div style={{ textAlign: 'center', padding: '3rem', color: 'rgba(255,255,255,0.3)' }}>
                  <Music2 style={{ width: 32, height: 32, margin: '0 auto 1rem', display: 'block', opacity: 0.4 }} />
                  <p style={{ margin: 0 }}>Fetching lyrics...</p>
                </div>
              ) : lyrics ? (
                <div
                  className="lyrics-scroll"
                  style={{
                    whiteSpace: 'pre-line',
                    lineHeight: '1.9',
                    color: 'rgba(255,255,255,0.85)',
                    fontSize: '15px',
                    maxHeight: '420px',
                    overflowY: 'auto',
                    paddingRight: '8px',
                  }}
                >
                  {lyrics}
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: '3rem', color: 'rgba(255,255,255,0.3)' }}>
                  <Mic2 style={{ width: 32, height: 32, margin: '0 auto 1rem', display: 'block', opacity: 0.3 }} />
                  <p style={{ margin: 0, marginBottom: '1rem' }}>Lyrics not found for this track</p>
                  {song.links?.spotify && (
                    <a
                      href={song.links.spotify}
                      target="_blank"
                      rel="noreferrer"
                      style={{
                        display: 'inline-flex', alignItems: 'center', gap: '6px',
                        color: '#1ed760', fontSize: '13px', textDecoration: 'none',
                      }}
                    >
                      Open in Spotify <ExternalLink style={{ width: 12, height: 12 }} />
                    </a>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Vibe Check Panel */}
          {activeTab === 'vibe' && (
            <div style={{
              background: 'rgba(10, 20, 32, 0.4)',
              backdropFilter: 'blur(40px)',
              border: '1px solid rgba(255,255,255,0.08)',
              borderRadius: '1.5rem',
              padding: '1.5rem',
              minHeight: '300px',
            }}>
              {featuresLoading ? (
                <div style={{ textAlign: 'center', padding: '3rem', color: 'rgba(255,255,255,0.3)' }}>
                  <Music2 style={{ width: 32, height: 32, margin: '0 auto 1rem', display: 'block', opacity: 0.4 }} />
                  <p style={{ margin: 0 }}>Loading track data...</p>
                </div>
              ) : features ? (
                <div>
                  {features.preview && (
                    <div style={{
                      background: 'rgba(253, 185, 36, 0.08)',
                      border: '1px solid rgba(253, 185, 36, 0.2)',
                      borderRadius: '1rem',
                      padding: '1rem',
                      marginBottom: '1.5rem',
                    }}>
                      <p style={{ margin: 0, marginBottom: '8px', fontSize: '12px', color: 'rgba(255,255,255,0.4)' }}>
                        30s Preview
                      </p>
                      <audio
                        controls
                        src={features.preview}
                        style={{ width: '100%', height: '36px', accentColor: '#fdb924' }}
                      />
                    </div>
                  )}

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '1.5rem' }}>
                    {[
                      { label: 'BPM', value: features.bpm ? Math.round(features.bpm) : '—' },
                      { label: 'Duration', value: `${Math.floor(features.duration / 60)}:${String(features.duration % 60).padStart(2, '0')}` },
                      { label: 'Deezer Rank', value: features.rank ? features.rank.toLocaleString() : '—' },
                      { label: 'Explicit', value: features.explicit ? 'Yes' : 'No' },
                    ].map((stat) => (
                      <div key={stat.label} style={{
                        background: 'rgba(255,255,255,0.04)',
                        border: '1px solid rgba(255,255,255,0.08)',
                        borderRadius: '0.75rem',
                        padding: '1rem',
                        textAlign: 'center',
                      }}>
                        <p style={{ margin: 0, fontSize: '11px', color: 'rgba(255,255,255,0.4)', marginBottom: '6px' }}>
                          {stat.label}
                        </p>
                        <p style={{ margin: 0, fontSize: '1.3rem', fontWeight: 800, color: '#fdb924' }}>
                          {stat.value}
                        </p>
                      </div>
                    ))}
                  </div>

                  {features.bpm > 0 && (
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                        <span style={{ fontSize: '12px', color: 'rgba(255,255,255,0.4)' }}>Tempo Vibe</span>
                        <span style={{ fontSize: '12px', color: '#fdb924', fontWeight: 700 }}>
                          {features.bpm < 80 ? '😌 Slow & Chill'
                            : features.bpm < 120 ? '🎵 Mid Tempo'
                            : features.bpm < 150 ? '🔥 Energetic'
                            : '⚡ Fast & Intense'}
                        </span>
                      </div>
                      <div style={{ height: '6px', borderRadius: '3px', background: 'rgba(255,255,255,0.08)', overflow: 'hidden' }}>
                        <div style={{
                          height: '100%', borderRadius: '3px',
                          background: 'linear-gradient(to right, #4a8ca8, #fdb924)',
                          width: `${Math.min((features.bpm / 200) * 100, 100)}%`,
                          transition: 'width 0.6s ease',
                        }} />
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '4px' }}>
                        <span style={{ fontSize: '10px', color: 'rgba(255,255,255,0.2)' }}>60 BPM</span>
                        <span style={{ fontSize: '10px', color: 'rgba(255,255,255,0.2)' }}>200 BPM</span>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: '3rem', color: 'rgba(255,255,255,0.3)' }}>
                  <Music2 style={{ width: 32, height: 32, margin: '0 auto 1rem', display: 'block', opacity: 0.3 }} />
                  <p style={{ margin: 0 }}>Vibe data unavailable for this track</p>
                </div>
              )}
            </div>
          )}
        </div>
      </main>
    </AppLayout>
  );
}