# OurMusic

OurMusic is a self-hosted music server with a modern web player and a Subsonic/OpenSubsonic-compatible API. It scans a local music folder into PostgreSQL, extracts tags and artwork, supports playlists and user accounts, and can be used from Subsonic clients such as Feishin and Supersonic.

## Tech Stack

- Frontend: Next.js, React, TypeScript, Tailwind CSS
- Backend: Java, Javalin
- Database: PostgreSQL
- Audio metadata: jaudiotagger
- Optional transcoding: FFmpeg

## Features

- Scan a local music folder into a structured library
- Browse songs, albums, artists, genres, and playlists
- Play tracks in the built-in web player
- Manage users, sessions, admin access, liked songs, and recently played history
- Create, update, reorder, and delete playlists
- Import playlist tracks from `.m3u8` files
- Upload playlist cover art
- Fetch lyrics through `lrclib.net`
- Serve album/song artwork from embedded audio metadata
- Store richer audio metadata such as bitrate, sample rate, channels, year, track, and disc number
- Expose a Subsonic/OpenSubsonic API for external music clients

## Subsonic Support

OurMusic implements the core Subsonic routes needed for browsing and playback from compatible clients.

Implemented areas include:

- System: `ping`, `getLicense`, `startScan`, `getScanStatus`, `getOpenSubsonicExtensions`
- Library browsing: `getMusicFolders`, `getArtists`, `getIndexes`, `getArtist`, `getAlbum`, `getSong`
- Album lists: `getAlbumList`, `getAlbumList2`
- Search and discovery: `search3`, `getGenres`, `getSongsByGenre`, `getRandomSongs`
- Playback and media: `stream`, `download`, `getCoverArt`
- Playlists: `getPlaylists`, `getPlaylist`, `createPlaylist`, `updatePlaylist`, `deletePlaylist`
- Stars and scrobbling: `getStarred`, `getStarred2`, `star`, `unstar`, `scrobble`
- Users: `getUser`, `createUser`, `updateUser`, `deleteUser`

Both `.view` and non-`.view` route variants are registered for Subsonic endpoints. The API supports legacy username/password auth and token/salt auth.

## How It Works

- The backend scans audio files from the configured `songs.folder`.
- Tags are read from each file and upserted into PostgreSQL.
- Songs are keyed by file path, so rescans update existing rows instead of duplicating tracks.
- Embedded artwork is extracted when available and saved into the configured artwork folder.
- Artists, albums, songs, playlists, users, sessions, likes, recently played history, and play counts are stored in PostgreSQL.
- The web frontend uses session-based auth, while Subsonic clients authenticate through Subsonic query/form parameters.

## Configuration

OurMusic is configured through environment variables. Copy the example environment file and fill in your values:

```bash
cp .env.example .env
```

Important settings:

```env
OURMUSIC_PORT=8808

POSTGRES_DB=ourmusic
POSTGRES_USER=ourmusic_user
POSTGRES_PASSWORD=change_me

MUSIC_PATH=/path/to/music
ARTWORK_PATH=/path/to/ourmusic-artwork

APP_PORT=8808
CORS_ALLOWED_ORIGINS=http://localhost:3000
SESSION_COOKIE_SECURE=false

ADMIN_USERNAME=admin
ADMIN_PASSWORD=change_me

FFMPEG_PATH=
SUBSONIC_AUTH_SECRET=
REQUEST_LOGGING_ENABLED=false
```

Notes:

- Keep `.env` private. It contains database and admin credentials.
- In Docker, `MUSIC_PATH` and `ARTWORK_PATH` are host paths mounted into the backend container as `/music` and `/artwork`.
- In Docker, the backend connects to PostgreSQL through the Compose service name `db`, not `localhost`.
- Set `SESSION_COOKIE_SECURE=true` when serving over HTTPS.
- Set a dedicated `SUBSONIC_AUTH_SECRET` for non-local deployments.
- FFmpeg is only needed for optional transcoding support.

## Running Locally

Install frontend dependencies:

```bash
npm ci --prefix frontend
```

Run the frontend:

```bash
npm run dev --prefix frontend
```

Compile the backend:

```bash
mvn -q compile
```

Run the backend:

```bash
set -a
source .env
set +a
mvn -q compile exec:java -Dexec.mainClass=Server
```

By default, the backend runs on the configured `app.port`, usually:

```text
http://localhost:8808
```

## Running With Docker

Create and edit the required environment file:

```bash
cp .env.example .env
```

Start the backend and PostgreSQL:

```bash
make docker-up
```

Check running containers:

```bash
make docker-ps
```

View backend logs:

```bash
make docker-logs
```

Rebuild after backend code changes:

```bash
make docker-restart
```

Test the Subsonic ping endpoint:

```bash
curl "http://localhost:8808/rest/ping.view?u=admin&p=ourmusic&v=1.16.1&c=test&f=json"
```

Stop the Docker stack:

```bash
make docker-down
```

PostgreSQL data is stored in the `ourmusic_postgres_data` Docker volume. To delete the database and start fresh:

```bash
docker compose down -v
```

## Screenshots

### Home

![Home screen](showcase/Home.png)

### Songs

![Songs screen](showcase/Songs.png)

### Playlists

![Playlists screen](showcase/Playlists.png)
