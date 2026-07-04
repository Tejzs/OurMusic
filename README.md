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

Copy the example config and fill in local values:

```bash
cp application.example.properties application.properties
```

Important settings:

```properties
db.url=jdbc:postgresql://localhost:5432/ourmusic
db.user=ourmusic_user
db.password=

songs.folder=/path/to/music
artwork.folder=/path/to/artwork

app.port=8808
cors.allowed.origins=http://localhost:3000
session.cookie.secure=false

admin.username=admin
admin.password=ourmusic

ffmpeg.path=/usr/bin/ffmpeg
subsonic.auth.secret=
```

Notes:

- Keep `application.properties` private. It can contain database and auth secrets.
- Set `session.cookie.secure=true` when serving over HTTPS.
- Set a dedicated `subsonic.auth.secret` for non-local deployments.
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
mvn -q compile exec:java -Dexec.mainClass=Server
```

By default, the backend runs on the configured `app.port`, usually:

```text
http://localhost:8808
```

## Validation

Useful checks before pushing changes:

```bash
mvn -q clean compile
npm run lint --prefix frontend
graphify update .
```

## Screenshots

### Home

![Home screen](showcase/Home.png)

### Songs

![Songs screen](showcase/Songs.png)

### Playlists

![Playlists screen](showcase/Playlists.png)
