import React, { useEffect, useState, useRef } from 'react';
import { X, Music2, ExternalLink, Mic2, Disc3 } from 'lucide-react';
import AppLayout from '../components/AppLayout';

interface SongDetailPageProps {
  navigate: (path: string) => void;
}

interface AudioFeatures {
  danceability: number;
  energy: number;
  valence: number;
  acousticness: number;
  instrumentalness: number;
  speechiness: number;
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
  spotifyTrackId?: string;
}

const SPOTIFY_CLIENT_ID = import.meta.env.VITE_SPOTIFY_CLIENT_ID || '';
const SPOTIFY_CLIENT_SECRET = import.meta.env.VITE_SPOTIFY_CLIENT_SECRET || '';

export default function SongDetailPage({ navigate }: SongDetailPageProps) {
  const [song, setSong] = useState<SongDetail | null>(null);
  const [lyrics, setLyrics] = useState<string | null>(null);
  const [lyricsLoading, setLyricsLoading] = useState(false);
  const [features, setFeatures] = useState<AudioFeatures | null>(null);
  const [featuresLoading, setFeaturesLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'lyrics' | 'vibe'>('lyrics');
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const stored = sessionStorage.getItem('selectedSong');
    if (stored) {
      const parsed = JSON.parse(stored);
      setSong(parsed);
      fetchLyrics(parsed.artist, parsed.title);
      if (parsed.links?.spotify) {
        const trackId = parsed.links.spotify.split('/track/')[1];
        if (trackId) fetchSpotifyFeatures(trackId);
      }
    }
  }, []);

  const fetchLyrics = async (artist: string, title: string) => {
    setLyricsLoading(true);
    try {
      // Using lyrics.ovh - free, no auth needed
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

  const fetchSpotifyFeatures = async (trackId: string) => {
    setFeaturesLoading(true);

    console.log('Track ID:', trackId);
    console.log('Client ID:', SPOTIFY_CLIENT_ID);
    console.log('Client Secret length:', SPOTIFY_CLIENT_SECRET?.length);
    console.log('Credentials b64:', btoa(`${SPOTIFY_CLIENT_ID}:${SPOTIFY_CLIENT_SECRET}`));


    try {
      // ✅ Correct Client Credentials flow
      const credentials = btoa(`${SPOTIFY_CLIENT_ID}:${SPOTIFY_CLIENT_SECRET}`);
      
      const tokenRes = await fetch('https://accounts.spotify.com/api/token', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Authorization': `Basic ${credentials}`,   // ← this was missing
        },
        body: 'grant_type=client_credentials',        // ← simplified body
      });

      const tokenData = await tokenRes.json();
      const token = tokenData.access_token;

      if (!token) {
        console.error('No token received:', tokenData);
        setFeaturesLoading(false);
        return;
      }

      const featRes = await fetch(
        `https://api.spotify.com/v1/audio-features/${trackId}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      if (featRes.ok) {
        const feat = await featRes.json();
        setFeatures({
          danceability: feat.danceability,
          energy: feat.energy,
          valence: feat.valence,
          acousticness: feat.acousticness,
          instrumentalness: feat.instrumentalness,
          speechiness: feat.speechiness,
        });
      }
    } catch (e) {
      console.error('Spotify fetch error:', e);
      setFeatures(null);
    }
    setFeaturesLoading(false);
  };

  useEffect(() => {
    if (features && canvasRef.current && activeTab === 'vibe') {
      drawRadar(features);
    }
  }, [features, activeTab]);

  const drawRadar = (f: AudioFeatures) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const size = 280;
    canvas.width = size;
    canvas.height = size;
    const cx = size / 2;
    const cy = size / 2;
    const radius = 100;

    const labels = ['Dance', 'Energy', 'Vibe', 'Acoustic', 'Instrumental', 'Speech'];
    const values = [
      f.danceability,
      f.energy,
      f.valence,
      f.acousticness,
      f.instrumentalness,
      f.speechiness,
    ];
    const n = labels.length;

    ctx.clearRect(0, 0, size, size);

    // Draw grid circles
    for (let r = 1; r <= 4; r++) {
      ctx.beginPath();
      for (let i = 0; i < n; i++) {
        const angle = (Math.PI * 2 * i) / n - Math.PI / 2;
        const x = cx + (radius * r) / 4 * Math.cos(angle);
        const y = cy + (radius * r) / 4 * Math.sin(angle);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.closePath();
      ctx.strokeStyle = 'rgba(255,255,255,0.08)';
      ctx.lineWidth = 1;
      ctx.stroke();
    }

    // Draw axis lines
    for (let i = 0; i < n; i++) {
      const angle = (Math.PI * 2 * i) / n - Math.PI / 2;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(cx + radius * Math.cos(angle), cy + radius * Math.sin(angle));
      ctx.strokeStyle = 'rgba(255,255,255,0.1)';
      ctx.lineWidth = 1;
      ctx.stroke();
    }

    // Draw data polygon
    ctx.beginPath();
    for (let i = 0; i < n; i++) {
      const angle = (Math.PI * 2 * i) / n - Math.PI / 2;
      const r = radius * values[i];
      const x = cx + r * Math.cos(angle);
      const y = cy + r * Math.sin(angle);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.closePath();
    ctx.fillStyle = 'rgba(253, 185, 36, 0.25)';
    ctx.fill();
    ctx.strokeStyle = 'rgba(253, 185, 36, 0.8)';
    ctx.lineWidth = 2;
    ctx.stroke();

    // Draw data points
    for (let i = 0; i < n; i++) {
      const angle = (Math.PI * 2 * i) / n - Math.PI / 2;
      const r = radius * values[i];
      const x = cx + r * Math.cos(angle);
      const y = cy + r * Math.sin(angle);
      ctx.beginPath();
      ctx.arc(x, y, 4, 0, Math.PI * 2);
      ctx.fillStyle = '#fdb924';
      ctx.fill();
    }

    // Draw labels
    ctx.font = '11px system-ui';
    ctx.fillStyle = 'rgba(255,255,255,0.6)';
    ctx.textAlign = 'center';
    for (let i = 0; i < n; i++) {
      const angle = (Math.PI * 2 * i) / n - Math.PI / 2;
      const labelR = radius + 20;
      const x = cx + labelR * Math.cos(angle);
      const y = cy + labelR * Math.sin(angle);
      ctx.fillText(labels[i], x, y + 4);
    }
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

  const vibeLabel = features
    ? features.valence > 0.6
      ? '😄 Happy & Upbeat'
      : features.valence > 0.4
      ? '😌 Chill & Neutral'
      : '😔 Melancholic'
    : null;

  return (
    <AppLayout currentRoute="/" navigate={navigate}>
      <main className="matches-page">
        {/* Header */}
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
            {/* Cover Art */}
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

            {/* Info */}
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
              {/* Platform links */}
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
          <div style={{
            display: 'flex', gap: '8px', marginBottom: '1rem',
          }}>
            {(['lyrics', 'vibe'] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => {
                  setActiveTab(tab);
                  if (tab === 'vibe' && features) setTimeout(() => drawRadar(features), 50);
                }}
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
                {tab === 'lyrics' ? <Mic2 style={{ width: 16, height: 16 }} /> : <Music2 style={{ width: 16, height: 16 }} />}
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
                <div style={{
                  whiteSpace: 'pre-line',
                  lineHeight: '1.9',
                  color: 'rgba(255,255,255,0.85)',
                  fontSize: '15px',
                  maxHeight: '420px',
                  overflowY: 'auto',
                  paddingRight: '8px',
                }}>
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
                  <p style={{ margin: 0 }}>Loading audio features...</p>
                </div>
              ) : features ? (
                <div>
                  {vibeLabel && (
                    <p style={{
                      textAlign: 'center', fontSize: '1.1rem', fontWeight: 700,
                      color: '#fdb924', marginBottom: '1rem', marginTop: 0,
                    }}>{vibeLabel}</p>
                  )}
                  {/* Radar Chart */}
                  <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1.5rem' }}>
                    <canvas ref={canvasRef} style={{ maxWidth: '100%' }} />
                  </div>
                  {/* Feature bars */}
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                    {[
                      { label: 'Danceability', value: features.danceability, color: '#fdb924' },
                      { label: 'Energy', value: features.energy, color: '#4a8ca8' },
                      { label: 'Happiness', value: features.valence, color: '#1ed760' },
                      { label: 'Acousticness', value: features.acousticness, color: '#7ab5cf' },
                      { label: 'Instrumental', value: features.instrumentalness, color: '#fec44d' },
                      { label: 'Speechiness', value: features.speechiness, color: '#a8c5d1' },
                    ].map((feat) => (
                      <div key={feat.label}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                          <span style={{ fontSize: '12px', color: 'rgba(255,255,255,0.5)' }}>{feat.label}</span>
                          <span style={{ fontSize: '12px', fontWeight: 700, color: feat.color }}>
                            {Math.round(feat.value * 100)}%
                          </span>
                        </div>
                        <div style={{
                          height: '4px', borderRadius: '2px',
                          background: 'rgba(255,255,255,0.08)', overflow: 'hidden',
                        }}>
                          <div style={{
                            height: '100%', borderRadius: '2px',
                            background: feat.color,
                            width: `${feat.value * 100}%`,
                            transition: 'width 0.6s ease',
                          }} />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: '3rem', color: 'rgba(255,255,255,0.3)' }}>
                  <Music2 style={{ width: 32, height: 32, margin: '0 auto 1rem', display: 'block', opacity: 0.3 }} />
                  <p style={{ margin: 0 }}>
                    {SPOTIFY_CLIENT_ID
                      ? 'Vibe data unavailable for this track'
                      : 'Add VITE_SPOTIFY_CLIENT_ID to enable vibe analysis'}
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      </main>
    </AppLayout>
  );
}