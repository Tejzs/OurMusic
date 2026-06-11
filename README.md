# OurMusic

OurMusic is a self-hosted, Navidrome-style music server with a clean library experience, playlist support, and a responsive web UI.

## Tech Stack

- Frontend: Next.js, React, TypeScript, Tailwind CSS
- Backend: Java, Javalin
- Database: PostgreSQL

## Features

- Drop your music files into a local folder and run a quick full library scan
- Browse songs, albums, artists, and playlists
- Play music through a built-in player
- Manage library and queue interactions
- User authentication and admin views

## How It Works

- The backend scans audio files, reads their metadata, and stores the library in PostgreSQL.
- Songs are keyed by file path, so rescans update existing entries instead of creating duplicates.
- The database keeps track of artists, albums, songs, playlists, users, sessions, likes, and recently played history.

## Database Logic

- The server loads database credentials from `application.properties`, opens one PostgreSQL connection, and creates the schema on startup.
- Core tables are `artists`, `albums`, `songs`, and `song_artists` for the music library, plus `users`, `sessions`, `playlists`, `playlist_songs`, `liked_songs`, and `recently_played` for app state.
- A library scan reads every audio file in the configured songs folder, extracts tags and artwork, then upserts the artist, album, and song records.
- `songs.file_path` is unique, so running a scan again updates the same row instead of duplicating tracks.
- `song_artists` stores the many-to-many relationship between songs and artists.
- Playlist order is stored with a `position` column in `playlist_songs`, and the next position is calculated before insert.
- Sessions are persisted in the database with an expiry time, so login state can be validated server-side and cleared on logout.
- Likes and recently played history are stored per user, which powers library views like liked songs, recent activity, and most played.

## Screenshots

### Home
![Home screen](showcase/Home.png)

### Songs
![Songs screen](showcase/Songs.png)

### Playlists
![Playlists screen](showcase/Playlists.png)
