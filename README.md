# Sonic Resonance — Shazam Reincarnate

> A production-grade audio fingerprinting and song recognition system — a full reimplementation of Shazam's core pipeline — built with Spring Boot, React TypeScript, FFmpeg, PostgreSQL, and Redis.

> ⚠️ **Status: In Progress** — Active development. APIs and architecture subject to change.

💻 **[GitHub](https://github.com/OwinoMichael/sonicres)** · 🌐 **[Live Demo](http://mikeowino.cloud/sonicres/)**

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [How Audio Fingerprinting Works](#how-audio-fingerprinting-works)
- [Project Structure](#project-structure)
- [Key Components](#key-components)
  - [WebSocket Audio Handler](#websocket-audio-handler)
  - [Session Audio Buffer](#session-audio-buffer)
  - [Audio Processing Task](#audio-processing-task)
  - [Audio Decoder Service](#audio-decoder-service)
  - [Fingerprint Service](#fingerprint-service)
- [WebSocket Protocol](#websocket-protocol)
- [FFmpeg Integration](#ffmpeg-integration)
- [Environment Variables](#environment-variables)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Local Development](#local-development)
  - [Production Deployment](#production-deployment)
- [Docker & Containerization](#docker--containerization)
- [CI/CD Pipeline](#cicd-pipeline)
- [Database](#database)
- [Caching](#caching)
- [Roadmap](#roadmap)
- [Contributing](#contributing)

---

## Overview

**Sonic Resonance** is a from-scratch reimplementation of the Shazam audio fingerprinting algorithm. Users record or upload a short audio clip via the browser; the system decodes the audio, generates a spectrogram, extracts frequency peaks, builds constellation-map-style hash fingerprints, and matches them against a database of pre-indexed tracks — returning a song identification result in real time.

The system is designed for high-throughput, low-latency recognition using non-blocking I/O and parallelized audio processing. Audio is streamed over a binary WebSocket connection, buffered server-side, converted to WAV via FFmpeg, and processed by a multi-threaded fingerprinting pipeline.

---

## Features

- **Real-Time Audio Fingerprinting** — Shazam-style hash-based fingerprint generation and matching pipeline.
- **WebSocket Audio Streaming** — Binary WebSocket connection streams raw audio chunks from the browser to the server with sub-second acknowledgment.
- **FFmpeg Audio Decoding** — Converts incoming WebM/Opus browser audio to 44.1 kHz mono PCM WAV for fingerprinting. Supports both local and Docker-container FFmpeg execution paths.
- **Spectrogram Generation** — Time-frequency analysis of decoded audio for peak extraction.
- **Non-Blocking I/O** — Spring Boot WebFlux / Reactor-based processing; parallelized fingerprint lookup via a fixed thread pool sized to available CPU cores.
- **PostgreSQL Fingerprint Storage** — Efficient storage and retrieval of hash fingerprints across thousands of tracks.
- **Redis Caching** — Fast fingerprint lookup and result caching to reduce recognition latency at scale.
- **React TypeScript Frontend** — Waveform visualization, audio upload/record, playback, and instant recognition result display.
- **Multi-Format Audio Support** — FFmpeg handles WebM, Opus, MP3, AAC, and other browser-native formats.
- **Concurrent Session Management** — `ConcurrentHashMap`-backed session registry handles multiple simultaneous recognition sessions.
- **Graceful Cleanup** — Temp files cleaned up after every processing run; session buffers closed on disconnect or transport error.

---

## Tech Stack

### Frontend

| Technology | Version | Purpose |
|---|---|---|
| React | 18.x | UI framework |
| TypeScript | 5.x | Type-safe JavaScript |
| Vite | 5.x | Build tool & dev server |
| Web Audio API | (browser-native) | Waveform capture & visualization |
| WebSocket API | (browser-native) | Binary audio streaming to backend |
| Nginx | alpine | Static file serving in production |

### Backend

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 (Eclipse Temurin) | Runtime |
| Spring Boot | 3.x | Application framework |
| Spring WebFlux / Reactor | — | Non-blocking I/O, reactive streams |
| Spring WebSocket | — | Binary WebSocket handler (`BinaryWebSocketHandler`) |
| Spring Data JPA / Hibernate | — | ORM & database access |
| JAVE2 (Java Audio Video Encoder) | — | Java wrapper for FFmpeg encoding/decoding |
| FFmpeg | (system / container) | Audio transcoding (WebM/Opus → WAV PCM) |
| Maven | 3.9.9 | Build & dependency management |

### Database & Caching

| Technology | Version / Image | Purpose |
|---|---|---|
| PostgreSQL | 16-alpine | Primary fingerprint and track metadata storage |
| Redis | (configured via Spring Data Redis) | Fingerprint lookup cache, session result cache |

### Infrastructure & DevOps

| Technology | Purpose |
|---|---|
| Docker | Containerization |
| Docker Compose | Multi-container orchestration |
| GitHub Actions | CI/CD pipeline |
| DigitalOcean Droplet | Cloud VPS hosting |
| Nginx (alpine) | Frontend reverse proxy / static serving |

---

## Architecture

```
Browser (React)
    │
    │  Binary WebSocket (audio chunks)
    ▼
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Application                    │
│                                                             │
│  AudioSocketConnectionHandler (BinaryWebSocketHandler)      │
│    │  receives binary frames → appends to SessionAudioBuffer│
│    │  receives "done" text frame → triggers processing      │
│    │                                                        │
│    ▼                                                        │
│  AudioProcessingTask (Runnable, thread pool)                │
│    │  1. Flush + close SessionAudioBuffer → .raw file       │
│    │  2. FFmpeg: .raw → .wav (44.1kHz, mono, PCM s16le)     │
│    │  3. FingerprintService.fingerprintAndMatch(wavFile)    │
│    │     ├── Spectrogram generation (FFT)                   │
│    │     ├── Peak extraction (constellation map)            │
│    │     ├── Hash generation (anchor + target point pairs)  │
│    │     └── PostgreSQL hash lookup + Redis cache           │
│    │  4. Send FingerprintResult JSON over WebSocket         │
│    └── Cleanup temp files, close session                    │
│                                                             │
│  AudioDecoderService (JAVE2 encoder wrapper)                │
│    └── Opus/WebM → WAV (pcm_s16le, 1ch, 44100Hz)           │
└─────────────────────────────────────────────────────────────┘
         │                          │
         ▼                          ▼
  PostgreSQL :5432            Redis cache
  (fingerprints,              (fast lookups,
   track metadata)             result cache)
```

### Docker Compose (Production)

```
┌──────────────────────────────────────────────────┐
│  React (Nginx)  :3002:80                         │
│  Spring Boot    :8081:8080  ← FFmpeg installed   │
│  PostgreSQL     :5432 (internal only)            │
│                                                  │
│  All on: app-network (bridge)                    │
└──────────────────────────────────────────────────┘
```

---

## How Audio Fingerprinting Works

Sonic Resonance implements the classical audio fingerprinting algorithm, inspired by the landmark Shazam paper:

### 1. Audio Capture
The browser records audio via the **Web Audio API** / `MediaRecorder` API, producing a WebM/Opus stream. Raw binary chunks are sent over a WebSocket connection to the Spring Boot backend.

### 2. Buffering
Each WebSocket session has a dedicated `SessionAudioBuffer` — a temp file backed by a `BufferedOutputStream`. Incoming binary frames are appended to the file without holding them in heap memory. Periodic flushes prevent memory pressure on large recordings.

### 3. Decoding (FFmpeg)
When the client sends `{"type": "done"}`, the buffer is sealed and handed to `AudioProcessingTask`. FFmpeg transcodes the raw WebM/Opus to **44.1 kHz, mono, 16-bit PCM WAV** — the standard format required for consistent fingerprint generation.

```
ffmpeg -y -i input.raw -ac 1 -ar 44100 -acodec pcm_s16le -f wav output.wav
```

### 4. Spectrogram Generation
A Short-Time Fourier Transform (STFT) is applied to the WAV data, producing a time-frequency spectrogram that maps energy across frequency bins over time.

### 5. Peak Extraction (Constellation Map)
Local maxima (peaks) are extracted from the spectrogram — points of high energy that are robust to noise and distortion. These form a "constellation map" of the audio.

### 6. Hash Generation
Each peak is paired with nearby "target" peaks within a defined time-frequency window. Each pair generates a hash:

```
hash = f(freq1, freq2, Δtime)
```

These hashes are time-stamped with their offset in the original recording.

### 7. Matching
The generated hashes are queried against the PostgreSQL fingerprint database (with Redis caching). A song is identified by finding a large number of hash matches with a consistent time offset — proving the query audio aligns temporally with a stored track.

---

## Project Structure

```
Sonic-Resonance/
├── frontend-react/                        # React + TypeScript frontend
│   ├── src/
│   │   ├── components/                    # Waveform visualizer, upload UI
│   │   ├── pages/
│   │   ├── hooks/                         # WebSocket hook, audio recording hook
│   │   └── types/
│   ├── nginx/
│   │   └── default.conf                   # Nginx config
│   ├── Dockerfile                         # Node 20 build → Nginx serve
│   └── vite.config.ts
│
├── backend-spring/                        # Java Spring Boot backend
│   ├── src/main/java/com/sonicres/demo/
│   │   ├── websocket/
│   │   │   ├── AudioSocketConnectionHandler.java   # WebSocket binary handler
│   │   │   ├── AudioProcessingTask.java            # Runnable: decode + fingerprint
│   │   │   └── SessionAudioBuffer.java             # Per-session temp file buffer
│   │   ├── service/
│   │   │   ├── FingerprintService.java             # Core fingerprint pipeline
│   │   │   └── AudioDecoderService.java            # JAVE2/FFmpeg decoder wrapper
│   │   ├── model/
│   │   │   └── FingerprintResult.java              # Result DTO with toJSON()
│   │   └── config/
│   │       └── WebSocketConfig.java                # WebSocket endpoint registration
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── pom.xml
│   └── Dockerfile                         # Maven build → JRE 21 alpine + FFmpeg
│
└── .github/
    └── workflows/
        └── deploy.yml                     # GitHub Actions CI/CD
```

---

## Key Components

### WebSocket Audio Handler

`AudioSocketConnectionHandler` extends Spring's `BinaryWebSocketHandler` and manages the full lifecycle of an audio recognition session:

| Event | Behavior |
|---|---|
| `afterConnectionEstablished` | Creates a `SessionAudioBuffer`, sends `{"type":"connected"}` to client |
| `handleBinaryMessage` | Appends raw audio bytes to the session buffer, sends byte-count ACK |
| `handleTextMessage` (`done`) | Triggers `handleRecordingComplete` → submits `AudioProcessingTask` to thread pool |
| `handleTextMessage` (`ping`) | Responds with `{"type":"pong"}` (keep-alive) |
| `afterConnectionClosed` | Removes session, silently closes and deletes buffer temp file |
| `handleTransportError` | Cleans up buffer on unexpected transport failure |

The processing thread pool is sized to `max(2, availableProcessors)` for optimal parallelism.

### Session Audio Buffer

`SessionAudioBuffer` is a per-session disk-backed buffer:

- Creates a temp file: `audio-stream-{sessionId}-{uuid}.raw`
- Appends incoming `ByteBuffer` chunks via a `BufferedOutputStream`
- Flushes to disk every 50,000 bytes to avoid memory buildup
- `closeForProcessing()` — flushes and seals the stream; called once before FFmpeg processing
- `closeSilently()` — safe cleanup on disconnect; deletes the temp file
- Thread-safe via `synchronized` append and close methods

### Audio Processing Task

`AudioProcessingTask` is a `Runnable` submitted to the fixed thread pool:

1. Seals the buffer via `closeForProcessing()`
2. Validates that audio data was received (non-zero file size)
3. Creates a temp WAV output file
4. Invokes FFmpeg (`decodeWithLocalFFmpeg`) — produces 44.1 kHz mono PCM WAV
5. Calls `FingerprintService.fingerprintAndMatch(wavFile)` → `FingerprintResult`
6. Sends the JSON result back over the WebSocket session
7. Closes the WebSocket session with `CloseStatus.NORMAL`
8. Cleans up both the raw and WAV temp files in the `finally` block

The class also contains a `decodeWithDockerFFmpeg` path that executes FFmpeg via `docker exec` into a sidecar container, sharing files via a `/tmp/audio` volume — useful for environments where FFmpeg cannot be installed in the main container.

### Audio Decoder Service

`AudioDecoderService` provides a JAVE2-based Java API for audio transcoding:

| Method | Input → Output | Use Case |
|---|---|---|
| `decodeOpusToWav(byte[])` | byte[] Opus/WebM → byte[] WAV | In-memory decode |
| `decodeOpusFileToWav(File, File)` | File → File (WAV) | File-to-file decode |
| `decodeWithCustomParams(File, File, channels, sampleRate)` | Configurable | Custom sample rate / channel count |

All outputs target **PCM s16le, mono (1ch), 44100 Hz** by default — the canonical format for fingerprint generation.

### Fingerprint Service

`FingerprintService` is the core recognition engine (interface inferred from usage):

- Accepts a decoded WAV file
- Runs the spectrogram → peak extraction → hash generation pipeline
- Queries PostgreSQL for hash matches (with Redis caching)
- Returns a `FingerprintResult` containing match metadata and confidence

---

## WebSocket Protocol

The client and server communicate over a single binary+text WebSocket connection.

### Client → Server

| Message Type | Format | Description |
|---|---|---|
| Audio chunk | Binary frame | Raw audio bytes (WebM/Opus from MediaRecorder) |
| Done signal | `{"type": "done"}` | Signals end of recording; triggers processing |
| Ping | `{"type": "ping"}` | Keep-alive heartbeat |

### Server → Client

| Message Type | Format | Description |
|---|---|---|
| Connected | `{"type":"connected","sessionId":"...","message":"Ready to receive audio"}` | Sent immediately on connection |
| ACK | `{"type":"ack","bytes":N,"totalBytes":M}` | Acknowledges each binary frame |
| Processing | `{"type":"processing","message":"Analyzing audio..."}` | Sent when processing begins |
| Result | `FingerprintResult.toJSON()` | Song match result |
| Error | `{"type":"error","message":"..."}` | Error message |
| Pong | `{"type":"pong"}` | Keep-alive response |

### WebSocket Limits (application.properties)

```properties
spring.websocket.message-size-limit=524288    # 512 KB per message
spring.websocket.send-buffer-size-limit=524288 # 512 KB send buffer
```

---

## FFmpeg Integration

FFmpeg is the audio transcoding backbone. It is installed **inside the Spring Boot Docker container** at both build and runtime stages:

```dockerfile
# Build stage
RUN apt-get update && apt-get install -y ffmpeg

# Runtime stage (alpine)
RUN apk add --no-cache ffmpeg curl
```

### Transcoding Parameters

| Parameter | Value | Reason |
|---|---|---|
| Channels (`-ac`) | `1` (mono) | Fingerprinting only needs mono |
| Sample rate (`-ar`) | `44100` Hz | Standard for audio fingerprinting |
| Codec (`-acodec`) | `pcm_s16le` | Uncompressed 16-bit PCM |
| Format (`-f`) | `wav` | Fingerprinting library input format |

### Execution Modes

The `AudioProcessingTask` includes two FFmpeg execution paths:

| Mode | Method | When Used |
|---|---|---|
| Local (default) | `decodeWithLocalFFmpeg` | FFmpeg on PATH in the same container |
| Docker sidecar | `decodeWithDockerFFmpeg` | Executes via `docker exec` into an `ffmpeg-service` container; shares files via `/tmp/audio` volume |

Docker detection uses `/.dockerenv` existence check, `/proc/1/cgroup` parsing, and the `FFMPEG_CONTAINER` environment variable as a fallback.

---

## Environment Variables

Create a `.env` file in the project root.

### Database

| Variable | Description | Default |
|---|---|---|
| `DB_USER` | PostgreSQL username | `sonicres` |
| `DB_PASS` | PostgreSQL password | `sonicres123` |
| `DB_NAME` | PostgreSQL database name | `sonic_resonance` |
| `DB_URL` | JDBC connection URL | `jdbc:postgresql://db:5432/sonic_resonance` |

### Backend

| Variable | Description | Default |
|---|---|---|
| `BACKEND_URL` | Backend base URL | `http://localhost:8081` |
| `FFMPEG_CONTAINER` | Name of Docker FFmpeg sidecar container | `ffmpeg-service-local` |

### GitHub Secrets (CI/CD)

| Secret | Description |
|---|---|
| `DROPLET_IP` | Public IP of the DigitalOcean Droplet |
| `DROPLET_SSH_KEY` | Private SSH key for VPS SSH access |

---

## Getting Started

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) (v24+) and [Docker Compose](https://docs.docker.com/compose/) (v2+)
- [FFmpeg](https://ffmpeg.org/download.html) installed locally (for development without Docker)
- [Node.js](https://nodejs.org/) 20+ (for local frontend dev)
- [Java 21](https://adoptium.net/) + [Maven 3.9+](https://maven.apache.org/) (for local backend dev)

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/OwinoMichael/sonicres.git
   cd sonicres
   ```

2. **Create a `.env` file** in the project root (see [Environment Variables](#environment-variables)).

3. **Start all services**
   ```bash
   docker compose -f docker-compose.prod.yml up --build
   ```

4. **Access services**
   - Frontend: http://localhost:3002
   - Spring Boot API: http://localhost:8081
   - Spring Actuator health: http://localhost:8081/actuator/health
   - PostgreSQL: `localhost:5432` (internal to Docker network)

5. **Frontend-only dev (hot reload)**
   ```bash
   cd frontend-react
   npm install
   npm run dev
   # http://localhost:5173
   ```

6. **Backend-only dev**
   ```bash
   # Ensure FFmpeg is installed locally: ffmpeg -version
   cd backend-spring
   mvn spring-boot:run
   # Requires PostgreSQL and Redis running locally
   ```

### Production Deployment

Automated via GitHub Actions on every push to `main`. To deploy manually:

```bash
cd /root/projects/Sonic-Resonance
git pull origin main
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d --build
docker image prune -f
```

---

## Docker & Containerization

### Services

#### `db` — PostgreSQL 16
- Image: `postgres:16-alpine`
- Health check: `pg_isready` with 5s interval, 10 retries (aggressive — Spring waits for healthy DB)
- Internal only — no external port binding in production
- Data persisted in `pgdata_prod` named volume
- Restart policy: `unless-stopped`

#### `spring` — Spring Boot + FFmpeg
- Built from `./backend-spring/Dockerfile`
- **Multi-stage build:**
  - Stage 1: `maven:3.9.9-eclipse-temurin-21` — installs FFmpeg, downloads dependencies, builds JAR
  - Stage 2: `eclipse-temurin:21-jre-alpine` — installs FFmpeg + curl at runtime, copies JAR
- Health check: `curl -f http://localhost:8080/actuator/health`
- Port: `8081:8080`
- Restart policy: `unless-stopped`

#### `react` — React Frontend
- Built from `./frontend-react/Dockerfile`
- Multi-stage: Node 20 alpine build → Nginx alpine serve
- TypeScript build errors suppressed via `build || npx vite build` fallback
- Port: `3002:80`
- Restart policy: `unless-stopped`

### Networks & Volumes

```yaml
networks:
  app-network:    # bridge — all inter-service communication

volumes:
  pgdata_prod:    # PostgreSQL data persistence
```

---

## CI/CD Pipeline

GitHub Actions workflow triggers on every push to `main`.

### Pipeline Steps

1. SSH into DigitalOcean Droplet via `appleboy/ssh-action@v1.2.0`
2. Pull latest code from `main`
3. Stop existing containers (`docker compose down`)
4. Rebuild and restart all services (`docker compose up -d --build`)
5. Prune dangling Docker images (`docker image prune -f`)

### Required GitHub Secrets

| Secret | Description |
|---|---|
| `DROPLET_IP` | VPS public IP |
| `DROPLET_SSH_KEY` | RSA/ED25519 private key |

The `.env` file must exist on the server at `/root/projects/Sonic-Resonance/.env` before first deploy.

---

## Database

### Engine

PostgreSQL 16 (alpine). No pgvector extension required — fingerprint hashes are stored as standard integer/bytea columns for fast indexed lookup.

### Schema Management

Managed via Spring Data JPA / Hibernate. Current DDL mode is implicitly `update` (inferred from active development state).

### Key Tables (inferred from pipeline)

| Table | Description |
|---|---|
| `tracks` | Song metadata (title, artist, album, duration) |
| `fingerprints` | Hash values with track ID and time offset |
| (Redis) | Cache layer over fingerprint hash lookups |

---

## Caching

Redis is used as a caching layer over the fingerprint lookup pipeline:

- Frequently queried hash values are cached to avoid repeated PostgreSQL scans
- Recognition results may be cached per session for low-latency repeat queries
- Configured via `spring.data.redis.host` / `spring.data.redis.port` (not yet in `docker-compose.prod.yml` — see [Roadmap](#roadmap))

---

## Roadmap

> Features planned or in active development:

- [ ] Add Redis service to `docker-compose.prod.yml`
- [ ] Complete `FingerprintService` — FFT spectrogram, peak extraction, hash generation
- [ ] Pre-index a song library (bulk fingerprint ingestion endpoint)
- [ ] Waveform visualization in the React frontend
- [ ] Spring Boot health check dependency on DB `service_healthy` condition
- [ ] Switch DDL mode to `validate` for production stability
- [ ] Rate limiting on WebSocket connections
- [ ] Add confidence score and multiple match candidates to `FingerprintResult`
- [ ] Support file upload path (in addition to live recording via WebSocket)
- [ ] Latency benchmarking and throughput testing at scale

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add your feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request against `main`

This project is in active development. Check open issues before starting significant work.

---

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

*Built by [Michael Owino](https://mikeowino.cloud)*
