package postgresql;

import auth.User;
import config.Properties;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import scanner.Album;
import scanner.Artist;
import scanner.MostPlayedSong;
import scanner.Playlist;
import scanner.PlaylistInfo;
import scanner.Song;

public class Database {

    private static Connection connection;

    private static Connection createConnection() throws Exception {
        return DriverManager.getConnection(Properties.getDBURL(), Properties.getDBUusername(), Properties.getDBPassword());
    }

    public static void setup() throws Exception {
        connection = createConnection();
    }

    public static void init() throws SQLException {
        String artistsTable = """
                CREATE TABLE IF NOT EXISTS artists (
                    id SERIAL PRIMARY KEY,
                    name TEXT UNIQUE NOT NULL,
                    image_path TEXT
                );
                """;
        connection.prepareStatement(artistsTable).execute();
        connection.prepareStatement("ALTER TABLE artists ADD COLUMN IF NOT EXISTS image_path TEXT;").execute();
        connection.prepareStatement("ALTER TABLE artists DROP COLUMN IF EXISTS artwork_song_id;").execute();

        String albumsTable = """
                CREATE TABLE IF NOT EXISTS albums (
                    id SERIAL PRIMARY KEY,
                    title TEXT NOT NULL,
                    artist_id INT NOT NULL,
                    UNIQUE (title, artist_id),
                    FOREIGN KEY (artist_id) REFERENCES artists(id)
                );
                """;
        connection.prepareStatement(albumsTable).execute();

        String songsTable = """
                CREATE TABLE IF NOT EXISTS songs (
                    id SERIAL PRIMARY KEY,
                    title TEXT,
                    genre TEXT,
                    album_id INT NOT NULL,
                    duration_seconds INT,
                    file_path TEXT UNIQUE NOT NULL,
                    file_size BIGINT,
                    file_modified_time BIGINT,
                    artwork_path TEXT,
                    FOREIGN KEY (album_id) REFERENCES albums(id)
                );
                """;
        connection.prepareStatement(songsTable).execute();
        connection.prepareStatement("ALTER TABLE songs ADD COLUMN IF NOT EXISTS genre TEXT;").execute();

        String songArtistsTable = """
                CREATE TABLE IF NOT EXISTS song_artists (
                    song_id INT NOT NULL,
                    artist_id INT NOT NULL,
                    PRIMARY KEY (song_id, artist_id),
                    FOREIGN KEY (song_id) REFERENCES songs(id),
                    FOREIGN KEY (artist_id) REFERENCES artists(id)
                );
                """;
        connection.prepareStatement(songArtistsTable).execute();

        String usersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    subsonic_token_secret TEXT,
                    is_admin BOOLEAN DEFAULT FALSE
                );
                """;
        connection.prepareStatement(usersTable).execute();
        connection.prepareStatement("ALTER TABLE users ADD COLUMN IF NOT EXISTS subsonic_token_secret TEXT;").execute();

        String sessionsTable = """
                CREATE TABLE IF NOT EXISTS sessions (
                    id SERIAL PRIMARY KEY,
                    user_id INT NOT NULL,
                    token TEXT UNIQUE NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
                """;
        connection.prepareStatement(sessionsTable).execute();


        String playlistsTable = """
                CREATE TABLE IF NOT EXISTS playlists (
                    id SERIAL PRIMARY KEY,
                    user_id INT NOT NULL,
                    name TEXT NOT NULL,
                    comment TEXT DEFAULT '',
                    is_public BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMPTZ DEFAULT NOW(),
                    changed_at TIMESTAMPTZ DEFAULT NOW(),
                    cover_path TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
                """;
        connection.prepareStatement(playlistsTable).execute();
        connection.prepareStatement("ALTER TABLE playlists ADD COLUMN IF NOT EXISTS comment TEXT DEFAULT '';").execute();
        connection.prepareStatement("ALTER TABLE playlists ADD COLUMN IF NOT EXISTS is_public BOOLEAN DEFAULT FALSE;").execute();
        connection.prepareStatement("ALTER TABLE playlists ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();").execute();
        connection.prepareStatement("ALTER TABLE playlists ADD COLUMN IF NOT EXISTS changed_at TIMESTAMPTZ DEFAULT NOW();").execute();

        String playlistSongsTable = """
                CREATE TABLE IF NOT EXISTS playlist_songs (
                    playlist_id INT NOT NULL,
                    song_id INT NOT NULL,
                    position INT NOT NULL,
                    PRIMARY KEY (playlist_id, song_id),
                    FOREIGN KEY (playlist_id) REFERENCES playlists(id),
                    FOREIGN KEY (song_id) REFERENCES songs(id)
                );
                """;
        connection.prepareStatement(playlistSongsTable).execute();

        String likedSongsTable = """
                CREATE TABLE IF NOT EXISTS liked_songs (
                    user_id INT NOT NULL,
                    song_id INT NOT NULL,
                    liked_at TIMESTAMP DEFAULT NOW(),
                    PRIMARY KEY (user_id, song_id),
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (song_id) REFERENCES songs(id)
                );
                """;
        connection.prepareStatement(likedSongsTable).execute();

        String recentlyPLayedTable = """
                CREATE TABLE IF NOT EXISTS recently_played (
                    id SERIAL PRIMARY KEY,
                    user_id INT NOT NULL,
                    song_id INT NOT NULL,
                    played_at TIMESTAMP DEFAULT NOW(),
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (song_id) REFERENCES songs(id)
                );
                """;
        connection.prepareStatement(recentlyPLayedTable).execute();

    }

    public static void insertSong(Song song) throws SQLException {
        String sql = """
                INSERT INTO songs (
                    title,
                    genre,
                    album_id,
                    duration_seconds,
                    file_path,
                    file_size,
                    file_modified_time,
                    artwork_path
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (file_path)
                DO UPDATE SET
                    title = EXCLUDED.title,
                    genre = EXCLUDED.genre,
                    album_id = EXCLUDED.album_id,
                    duration_seconds = EXCLUDED.duration_seconds,
                    file_size = EXCLUDED.file_size,
                    file_modified_time = EXCLUDED.file_modified_time,
                    artwork_path = EXCLUDED.artwork_path
                RETURNING id;
                """;

        PreparedStatement stmt = connection.prepareStatement(sql);

        stmt.setString(1, song.getTitle());
        stmt.setString(2, song.getGenre());
        stmt.setInt(3, song.getAlbumId());
        stmt.setInt(4, song.getDuration());
        stmt.setString(5, song.getFilePath());
        stmt.setLong(6, song.getFileSize());
        stmt.setLong(7, song.getLastModified());
        stmt.setString(8, song.getArtworkPath());

        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            song.setId(rs.getInt("id"));
        }
    }

    public static Song getSong(int id) {
        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds,
                    songs.file_path AS file_path,
                    songs.file_size AS file_size,
                    songs.file_modified_time AS file_modified_time,
                    songs.artwork_path AS artwork_path
                FROM songs
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists ON song_artists.song_id = songs.id
                JOIN artists ON song_artists.artist_id = artists.id
                WHERE songs.id = ?
                GROUP BY songs.id, albums.id;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
                song.setId(rs.getInt("song_id"));
                return song;
            }
        } catch (Exception e) {
            System.out.println("Unknown ID: " + id);
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static List<Song> search(String pattern) {
        return searchSongs(pattern, 20);
    }

    public static List<Song> searchSongs(String pattern, int limit) {
        return searchSongs(pattern, limit, 0);
    }

    public static List<Song> searchSongs(String pattern, int limit, int offset) {
        List<Song> songs = new ArrayList();

        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds,
                    songs.file_path AS file_path,
                    songs.file_size AS file_size,
                    songs.file_modified_time AS file_modified_time,
                    songs.artwork_path AS artwork_path
                FROM songs
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists ON song_artists.song_id = songs.id
                JOIN artists ON song_artists.artist_id = artists.id
                WHERE songs.title ILIKE ?
                   OR albums.title ILIKE ?
                   OR songs.id IN (
                        SELECT song_id
                        FROM song_artists
                        JOIN artists ON song_artists.artist_id = artists.id
                        WHERE artists.name ILIKE ?
                   )
                GROUP BY songs.id, albums.id;
                """;
        String limitedSql = limit > 0
                ? sql.replace("GROUP BY songs.id, albums.id;", "GROUP BY songs.id, albums.id ORDER BY songs.title LIMIT ? OFFSET ?;")
                : sql.replace("GROUP BY songs.id, albums.id;", "GROUP BY songs.id, albums.id ORDER BY songs.title;");

        try {
            PreparedStatement stmt = connection.prepareStatement(limitedSql);
            String searchPattern = "%" + pattern + "%";

            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            if (limit > 0) {
                stmt.setInt(4, limit);
                stmt.setInt(5, Math.max(0, offset));
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
                song.setId(rs.getInt("song_id"));
                songs.add(song);
            }
            return songs;
        } catch (Exception e) {
            System.out.println("Failed to search: " + pattern);
            System.out.println(e.getMessage());
        }
        return songs;
    }

    public static List<Song> getSongsByGenre(String genre, int limit, int offset) {
        List<Song> songs = new ArrayList<>();

        if (genre == null || genre.isBlank()) {
            return songs;
        }

        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds,
                    songs.file_path AS file_path,
                    songs.file_size AS file_size,
                    songs.file_modified_time AS file_modified_time,
                    songs.artwork_path AS artwork_path
                FROM songs
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists ON song_artists.song_id = songs.id
                JOIN artists ON song_artists.artist_id = artists.id
                WHERE songs.genre IS NOT NULL
                  AND BTRIM(songs.genre) <> ''
                  AND LOWER(BTRIM(songs.genre)) = LOWER(BTRIM(?))
                GROUP BY songs.id, albums.id
                ORDER BY albums.title, songs.title
                LIMIT ?
                OFFSET ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, genre);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
                song.setId(rs.getInt("song_id"));
                songs.add(song);
            }
        } catch (Exception e) {
            System.out.println("Get songs by genre failed");
            System.out.println(e.getMessage());
        }

        return songs;
    }

    public static List<Song> getRandomSongs(int limit, String genre) {
        List<Song> songs = new ArrayList<>();

        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds,
                    songs.file_path AS file_path,
                    songs.file_size AS file_size,
                    songs.file_modified_time AS file_modified_time,
                    songs.artwork_path AS artwork_path
                FROM songs
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists ON song_artists.song_id = songs.id
                JOIN artists ON song_artists.artist_id = artists.id
                WHERE (? IS NULL OR (
                    songs.genre IS NOT NULL
                    AND BTRIM(songs.genre) <> ''
                    AND LOWER(BTRIM(songs.genre)) = LOWER(BTRIM(?))
                ))
                GROUP BY songs.id, albums.id
                ORDER BY RANDOM()
                LIMIT ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            String normalizedGenre = genre == null || genre.isBlank() ? null : genre;
            stmt.setString(1, normalizedGenre);
            stmt.setString(2, normalizedGenre);
            stmt.setInt(3, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
                song.setId(rs.getInt("song_id"));
                songs.add(song);
            }
        } catch (Exception e) {
            System.out.println("Get random songs failed");
            System.out.println(e.getMessage());
        }

        return songs;
    }

    public static List<Artist> searchArtists(String pattern, int limit) {
        return searchArtists(pattern, limit, 0);
    }

    public static List<Artist> searchArtists(String pattern, int limit, int offset) {
        List<Artist> artists = new ArrayList<>();

        String sql = """
                SELECT
                    artists.id AS artist_id,
                    artists.name AS artist_name,
                    COUNT(DISTINCT albums.id) AS album_count,
                    COUNT(DISTINCT song_artists.song_id) AS song_count,
                    artists.image_path AS image_path
                FROM artists
                LEFT JOIN albums ON albums.artist_id = artists.id
                LEFT JOIN song_artists ON song_artists.artist_id = artists.id
                WHERE artists.name ILIKE ?
                GROUP BY artists.id, artists.name, artists.image_path
                ORDER BY artists.name
                LIMIT ?
                OFFSET ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, "%" + pattern + "%");
            stmt.setInt(2, Math.max(0, limit));
            stmt.setInt(3, Math.max(0, offset));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                artists.add(new Artist(
                        rs.getInt("artist_id"),
                        rs.getString("artist_name"),
                        rs.getInt("album_count"),
                        rs.getInt("song_count"),
                        rs.getString("image_path")
                ));
            }
            return artists;
        } catch (Exception e) {
            System.out.println("Artist search failed: " + pattern);
            System.out.println(e.getMessage());
        }

        return artists;
    }

    public static List<Album> searchAlbums(String pattern, int limit) {
        return searchAlbums(pattern, limit, 0);
    }

    public static List<Album> searchAlbums(String pattern, int limit, int offset) {
        List<Album> albums = new ArrayList<>();

        String sql = """
                SELECT
                    albums.id AS album_id,
                    albums.title AS album_title,
                    albums.artist_id AS artist_id,
                    artists.name AS artist_name,
                    COUNT(DISTINCT songs.id) AS song_count,
                    MIN(CASE
                        WHEN songs.artwork_path IS NOT NULL AND songs.artwork_path <> '' THEN songs.id
                    END) AS artwork_song_id
                FROM albums
                JOIN artists ON albums.artist_id = artists.id
                LEFT JOIN songs ON songs.album_id = albums.id
                WHERE albums.title ILIKE ? OR artists.name ILIKE ?
                GROUP BY albums.id, albums.title, albums.artist_id, artists.name
                ORDER BY albums.title, artists.name
                LIMIT ?
                OFFSET ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            String searchPattern = "%" + pattern + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setInt(3, Math.max(0, limit));
            stmt.setInt(4, Math.max(0, offset));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                albums.add(new Album(
                        rs.getInt("album_id"),
                        rs.getString("album_title"),
                        rs.getInt("artist_id"),
                        rs.getString("artist_name"),
                        rs.getInt("song_count"),
                        rs.getInt("artwork_song_id")
                ));
            }
            return albums;
        } catch (Exception e) {
            System.out.println("Album search failed: " + pattern);
            System.out.println(e.getMessage());
        }

        return albums;
    }

    public static List<Song> scanSongs(int limit, int offset) {
        List<Song> songs = new ArrayList<>();

        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds,
                    songs.file_path AS file_path,
                    songs.file_size AS file_size,
                    songs.file_modified_time AS file_modified_time,
                    songs.artwork_path AS artwork_path
                FROM songs
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists ON song_artists.song_id = songs.id
                JOIN artists ON song_artists.artist_id = artists.id
                GROUP BY songs.id, albums.id
                ORDER BY songs.title
                LIMIT ?
                OFFSET ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));

                song.setId(rs.getInt("song_id"));
                songs.add(song);
            }
        } catch (Exception e) {
            System.out.println("Scan failed");
            System.out.println(e.getMessage());
        }

        return songs;
    }

    public static int getArtist(String name) {
        String sql = """
                INSERT INTO artists (name)
                VALUES (?)
                ON CONFLICT (name)
                DO UPDATE SET name = EXCLUDED.name
                RETURNING id;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return -1;
    }

    public static void updateArtistImagePath(int artistId, String imagePath) {
        if (artistId <= 0 || imagePath == null || imagePath.isBlank()) {
            return;
        }

        String sql = """
                UPDATE artists
                SET image_path = ?
                WHERE id = ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, imagePath);
            stmt.setInt(2, artistId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static int getAlbum(String title, int artistId) {
        String sql = """
                    INSERT INTO albums (title, artist_id)
                    VALUES (?, ?)
                    ON CONFLICT (title, artist_id)
                    DO UPDATE SET title = EXCLUDED.title
                    RETURNING id;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, title);
            stmt.setInt(2, artistId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return -1;
    }

    public static List<Album> getAlbums(int limit, int offset) {
        List<Album> albums = new ArrayList<>();
        String sql = """
                SELECT
                    albums.id AS album_id,
                    albums.title AS album_title,
                    albums.artist_id AS artist_id,
                    artists.name AS artist_name,
                    COUNT(songs.id) AS song_count,
                    MIN(CASE
                        WHEN songs.artwork_path IS NOT NULL AND songs.artwork_path <> '' THEN songs.id
                    END) AS artwork_song_id
                FROM albums
                JOIN artists ON albums.artist_id = artists.id
                LEFT JOIN songs ON songs.album_id = albums.id
                GROUP BY albums.id, albums.title, albums.artist_id, artists.name
                ORDER BY albums.title
                LIMIT ?
                OFFSET ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Album album = new Album(rs.getInt("album_id"), rs.getString("album_title"), rs.getInt("artist_id"), rs.getString("artist_name"), rs.getInt("song_count"), rs.getInt("artwork_song_id"));

                albums.add(album);
            }
            return albums;
        } catch (Exception e) {
            System.out.println("Scan failed");
            System.out.println(e.getMessage());
        }
        return albums;
    }

    public static List<Album> getAlbumList(String type, int limit, int offset, int userId) {
        return getAlbumList(type, limit, offset, userId, null, null, null);
    }

    public static List<Album> getAlbumList(String type, int limit, int offset, int userId, String genre, Integer fromYear, Integer toYear) {
        List<Album> albums = new ArrayList<>();

        String orderBy;
        boolean requiresUser = false;
        String whereClause = "";

        switch (type) {
            case "alphabeticalByName" -> orderBy = "albums.title ASC, artists.name ASC";
            case "alphabeticalByArtist" -> orderBy = "artists.name ASC, albums.title ASC";
            case "byGenre" -> {
                if (genre == null || genre.isBlank()) {
                    return albums;
                }
                orderBy = "albums.title ASC, artists.name ASC";
                whereClause = """
                        WHERE EXISTS (
                            SELECT 1
                            FROM songs genre_songs
                            WHERE genre_songs.album_id = albums.id
                              AND genre_songs.genre IS NOT NULL
                              AND BTRIM(genre_songs.genre) <> ''
                              AND LOWER(BTRIM(genre_songs.genre)) = LOWER(BTRIM(?))
                        )
                        """;
            }
            case "byYear" -> orderBy = "albums.title ASC, artists.name ASC";
            case "newest" -> orderBy = "MAX(songs.file_modified_time) DESC NULLS LAST, albums.id DESC";
            case "random" -> orderBy = "RANDOM()";
            case "recent" -> {
                orderBy = "MAX(recently_played.played_at) DESC NULLS LAST, albums.id DESC";
                requiresUser = true;
            }
            case "frequent" -> {
                orderBy = "COUNT(recently_played.id) DESC, albums.title ASC";
                requiresUser = true;
            }
            case "starred" -> {
                return getStarredAlbums(userId, limit, offset);
            }
            default -> {
                return albums;
            }
        }

        String recentlyPlayedJoin = requiresUser
                ? "LEFT JOIN recently_played ON recently_played.song_id = songs.id AND recently_played.user_id = ?"
                : "";

        String sql = """
                SELECT
                    albums.id AS album_id,
                    albums.title AS album_title,
                    albums.artist_id AS artist_id,
                    artists.name AS artist_name,
                    COUNT(DISTINCT songs.id) AS song_count,
                    MIN(CASE
                        WHEN songs.artwork_path IS NOT NULL AND songs.artwork_path <> '' THEN songs.id
                    END) AS artwork_song_id
                FROM albums
                JOIN artists ON albums.artist_id = artists.id
                LEFT JOIN songs ON songs.album_id = albums.id
                %s
                %s
                GROUP BY albums.id, albums.title, albums.artist_id, artists.name
                ORDER BY %s
                LIMIT ?
                OFFSET ?;
                """.formatted(recentlyPlayedJoin, whereClause, orderBy);

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            int parameterIndex = 1;

            if (requiresUser) {
                stmt.setInt(parameterIndex, userId);
                parameterIndex += 1;
            }

            if ("byGenre".equals(type)) {
                stmt.setString(parameterIndex, genre);
                parameterIndex += 1;
            }

            stmt.setInt(parameterIndex, limit);
            stmt.setInt(parameterIndex + 1, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Album album = new Album(
                        rs.getInt("album_id"),
                        rs.getString("album_title"),
                        rs.getInt("artist_id"),
                        rs.getString("artist_name"),
                        rs.getInt("song_count"),
                        rs.getInt("artwork_song_id")
                );

                albums.add(album);
            }
            return albums;
        } catch (Exception e) {
            System.out.println("Album list failed");
            System.out.println(e.getMessage());
        }

        return albums;
    }

    public static List<Map<String, Object>> getGenres() {
        List<Map<String, Object>> genres = new ArrayList<>();

        String sql = """
                SELECT
                    MIN(BTRIM(genre)) AS value,
                    COUNT(DISTINCT id) AS song_count,
                    COUNT(DISTINCT album_id) AS album_count
                FROM songs
                WHERE genre IS NOT NULL
                  AND BTRIM(genre) <> ''
                GROUP BY LOWER(BTRIM(genre))
                ORDER BY LOWER(BTRIM(genre));
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                genres.add(Map.of(
                        "value", rs.getString("value"),
                        "songCount", rs.getInt("song_count"),
                        "albumCount", rs.getInt("album_count")
                ));
            }
        } catch (Exception e) {
            System.out.println("Get genres failed");
            System.out.println(e.getMessage());
        }

        return genres;
    }

    public static List<Album> getStarredAlbums(int userId, int limit, int offset) {
        List<Album> albums = new ArrayList<>();

        String sql = """
                SELECT
                    albums.id AS album_id,
                    albums.title AS album_title,
                    albums.artist_id AS artist_id,
                    artists.name AS artist_name,
                    COUNT(DISTINCT songs.id) AS song_count,
                    MIN(CASE
                        WHEN songs.artwork_path IS NOT NULL AND songs.artwork_path <> '' THEN songs.id
                    END) AS artwork_song_id,
                    MAX(liked_songs.liked_at) AS liked_at
                FROM liked_songs
                JOIN songs liked_song ON liked_song.id = liked_songs.song_id
                JOIN albums ON liked_song.album_id = albums.id
                JOIN artists ON albums.artist_id = artists.id
                LEFT JOIN songs ON songs.album_id = albums.id
                WHERE liked_songs.user_id = ?
                GROUP BY albums.id, albums.title, albums.artist_id, artists.name
                ORDER BY liked_at DESC, albums.title
                LIMIT ?
                OFFSET ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                albums.add(new Album(
                        rs.getInt("album_id"),
                        rs.getString("album_title"),
                        rs.getInt("artist_id"),
                        rs.getString("artist_name"),
                        rs.getInt("song_count"),
                        rs.getInt("artwork_song_id")
                ));
            }
        } catch (Exception e) {
            System.out.println("Get starred albums failed");
            System.out.println(e.getMessage());
        }

        return albums;
    }

    public static List<Song> getAlbumsSongs(int id) {
        List<Song> songs = new ArrayList<>();

        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds,
                    songs.file_path AS file_path,
                    songs.file_size AS file_size,
                    songs.file_modified_time AS file_modified_time,
                    songs.artwork_path AS artwork_path
                FROM songs
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists ON song_artists.song_id = songs.id
                JOIN artists ON song_artists.artist_id = artists.id
                WHERE albums.id = ?
                GROUP BY songs.id, albums.id
                ORDER BY songs.title;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));

                song.setId(rs.getInt("song_id"));
                songs.add(song);
            }
            return songs;
        } catch (Exception e) {
            System.out.println("Scan failed");
            System.out.println(e.getMessage());
        }
        return songs;
    }

    public static List<Artist> getArtists(int limit, int offset) {
        List<Artist> artists = new ArrayList<>();

        String sql = """
                SELECT
                    artists.id AS artist_id,
                    artists.name AS artist_name,
                    COUNT(DISTINCT albums.id) AS album_count,
                    COUNT(DISTINCT song_artists.song_id) AS song_count,
                    artists.image_path AS image_path
                FROM artists
                LEFT JOIN albums ON albums.artist_id = artists.id
                LEFT JOIN song_artists ON song_artists.artist_id = artists.id
                GROUP BY artists.id, artists.name, artists.image_path
                ORDER BY artists.name
                LIMIT ?
                OFFSET ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Artist artist = new Artist(rs.getInt("artist_id"), rs.getString("artist_name"), rs.getInt("album_count"), rs.getInt("song_count"), rs.getString("image_path"));

                artists.add(artist);
            }
            return artists;
        } catch (Exception e) {
            System.out.println("Scan failed");
            System.out.println(e.getMessage());
        }
        return artists;
    }

    public static List<Artist> getAllArtists() {
        List<Artist> artists = new ArrayList<>();

        String sql = """
                SELECT
                    artists.id AS artist_id,
                    artists.name AS artist_name,
                    COUNT(DISTINCT albums.id) AS album_count,
                    COUNT(DISTINCT song_artists.song_id) AS song_count,
                    artists.image_path AS image_path
                FROM artists
                LEFT JOIN albums ON albums.artist_id = artists.id
                LEFT JOIN song_artists ON song_artists.artist_id = artists.id
                GROUP BY artists.id, artists.name, artists.image_path
                ORDER BY artists.name;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Artist artist = new Artist(
                        rs.getInt("artist_id"),
                        rs.getString("artist_name"),
                        rs.getInt("album_count"),
                        rs.getInt("song_count"),
                        rs.getString("image_path")
                );

                artists.add(artist);
            }
            return artists;
        } catch (Exception e) {
            System.out.println("Scan failed");
            System.out.println(e.getMessage());
        }
        return artists;
    }

    public static List<Song> getArtistSongs(int id) {
        List<Song> songs = new ArrayList<>();

        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(all_artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds,
                    songs.file_path AS file_path,
                    songs.file_size AS file_size,
                    songs.file_modified_time AS file_modified_time,
                    songs.artwork_path AS artwork_path
                FROM songs
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists filter_sa ON filter_sa.song_id = songs.id
                JOIN song_artists all_sa ON all_sa.song_id = songs.id
                JOIN artists all_artists ON all_sa.artist_id = all_artists.id
                WHERE filter_sa.artist_id = ?
                GROUP BY songs.id, albums.id
                ORDER BY albums.title, songs.title;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
                song.setId(rs.getInt("song_id"));
                songs.add(song);
            }
            return songs;
        } catch (Exception e) {
            System.out.println("Scan failed");
            System.out.println(e.getMessage());
        }
        return songs;
    }

    public static List<Album> getArtistAlbums(int id) {
        List<Album> albums = new ArrayList<>();

        String sql = """
                SELECT
                    albums.id AS album_id,
                    albums.title AS album_title,
                    albums.artist_id AS artist_id,
                    artists.name AS artist_name,
                    COUNT(songs.id) AS song_count,
                    MIN(CASE
                        WHEN songs.artwork_path IS NOT NULL AND songs.artwork_path <> '' THEN songs.id
                    END) AS artwork_song_id
                FROM albums
                JOIN artists ON albums.artist_id = artists.id
                LEFT JOIN songs ON songs.album_id = albums.id
                WHERE artists.id = ?
                GROUP BY albums.id, albums.title, albums.artist_id, artists.name
                ORDER BY albums.title;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Album album = new Album(rs.getInt("album_id"), rs.getString("album_title"), rs.getInt("artist_id"), rs.getString("artist_name"), rs.getInt("song_count"), rs.getInt("artwork_song_id"));

                albums.add(album);
            }
            return albums;
        } catch (Exception e) {
            System.out.println("Scan failed");
            System.out.println(e.getMessage());
        }
        return albums;
    }

    public static Artist getArtistById(int id) {
        String sql = """
                SELECT
                    artists.id AS artist_id,
                    artists.name AS artist_name,
                    COUNT(DISTINCT albums.id) AS album_count,
                    COUNT(DISTINCT song_artists.song_id) AS song_count,
                    artists.image_path AS image_path
                FROM artists
                LEFT JOIN albums ON albums.artist_id = artists.id
                LEFT JOIN song_artists ON song_artists.artist_id = artists.id
                WHERE artists.id = ?
                GROUP BY artists.id, artists.name, artists.image_path;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Artist(
                        rs.getInt("artist_id"),
                        rs.getString("artist_name"),
                        rs.getInt("album_count"),
                        rs.getInt("song_count"),
                        rs.getString("image_path")
                );
            }
        } catch (Exception e) {
            System.out.println("Artist lookup failed");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public static Album getAlbumById(int id) {
        String sql = """
                SELECT
                    albums.id AS album_id,
                    albums.title AS album_title,
                    albums.artist_id AS artist_id,
                    artists.name AS artist_name,
                    COUNT(songs.id) AS song_count,
                    MIN(CASE
                        WHEN songs.artwork_path IS NOT NULL AND songs.artwork_path <> '' THEN songs.id
                    END) AS artwork_song_id
                FROM albums
                JOIN artists ON albums.artist_id = artists.id
                LEFT JOIN songs ON songs.album_id = albums.id
                WHERE albums.id = ?
                GROUP BY albums.id, albums.title, albums.artist_id, artists.name;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Album(
                        rs.getInt("album_id"),
                        rs.getString("album_title"),
                        rs.getInt("artist_id"),
                        rs.getString("artist_name"),
                        rs.getInt("song_count"),
                        rs.getInt("artwork_song_id")
                );
            }
        } catch (Exception e) {
            System.out.println("Album lookup failed");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public static void insertSongArtists(int songId, int artistId) {
        String sql = """
                INSERT INTO song_artists (song_id, artist_id)
                VALUES (?, ?)
                ON CONFLICT (song_id, artist_id)
                DO NOTHING;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, songId);
            stmt.setInt(2, artistId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Insert failed");
            System.out.println(e.getMessage());
        }
    }

    public static int registerUser(String username, String passwordHash) {
        return registerUser(username, passwordHash, null);
    }

    public static int registerUser(String username, String passwordHash, String subsonicTokenSecret) {
        return registerUser(username, passwordHash, subsonicTokenSecret, false);
    }

    public static int registerUser(String username, String passwordHash, String subsonicTokenSecret, boolean isAdmin) {
        String checkSql = """
                SELECT id
                FROM users
                WHERE username = ?;
                """;

        String insertSql = """
                INSERT INTO users (
                    username,
                    password_hash,
                    subsonic_token_secret,
                    is_admin
                )
                VALUES (?, ?, ?, ?)
                RETURNING id;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(checkSql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return -2; // Username exists
            } else {
                stmt = connection.prepareStatement(insertSql);
                stmt.setString(1, username);
                stmt.setString(2, passwordHash);
                stmt.setString(3, subsonicTokenSecret);
                stmt.setBoolean(4, isAdmin);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (Exception e) {
            System.out.println("Insert failed");
            System.out.println(e.getMessage());
        }
        return -1; // Failure
    }

    public static void ensureAdminUser(String username, String passwordHash, String subsonicTokenSecret) {
        User existingUser = getUserByName(username);
        if (existingUser != null) {
            return;
        }

        int result = registerUser(username, passwordHash, subsonicTokenSecret, true);
        if (result > 0) {
            System.out.println("Seeded admin user: " + username);
            return;
        }

        System.out.println("Failed to seed admin user: " + username);
    }

    public static User getUserByName(String username) {
        String sql = """
                SELECT *
                FROM users
                WHERE username = ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"), rs.getBoolean("is_admin"));
                return user;
            }
        } catch (Exception e) {
            System.out.println("Failed");
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static void createSessionToken(int userId, String token) {
        String sql = """
                INSERT INTO sessions (
                    user_id,
                    token,
                    expires_at
                )
                VALUES (?, ?, NOW() + INTERVAL '7 days');
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setString(2, token);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Session Creation Failed");
            System.out.println(e.getMessage());
        }
    }

    public static int getUserIdFromSession(String token) {
        String sql = """
                SELECT user_id
                FROM sessions
                WHERE token = ?
                  AND expires_at > NOW();
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, token);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id");
            }
        } catch (Exception e) {
            System.out.println("Session check failed");
            System.out.println(e.getMessage());
        }

        return -1;
    }

    public static User getUserFromId(int userId) {
        String sql = """
                SELECT *
                FROM users
                WHERE id = ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"), rs.getBoolean("is_admin"));
                return user;
            }
        } catch (Exception e) {
            System.out.println("Failed");
            System.out.println(e.getMessage());
        }

        return null;
    }


    public static boolean clearSession(String token) {
        String sql = """
                DELETE FROM sessions
                WHERE token = ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, token);

            int rows = stmt.executeUpdate();

            return rows > 0;
        } catch (Exception e) {
            System.out.println("Clear session failed");
            System.out.println(e.getMessage());
        }

        return false;
    }

    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();

        String sql = """
                SELECT *
                FROM users
                ORDER BY username;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                User user = new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"), rs.getBoolean("is_admin"));

                users.add(user);
            }
        } catch (Exception e) {
            System.out.println("Scan failed");
            System.out.println(e.getMessage());
        }
        return users;
    }

    public static boolean deleteUser(int id) {
        String sessionSql = """
                DELETE FROM sessions
                WHERE user_id = ?;
                """;

        String userSql = """
                DELETE FROM users
                WHERE id = ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sessionSql);
            stmt.setInt(1, id);
            stmt.executeUpdate();

            stmt = connection.prepareStatement(userSql);
            stmt.setInt(1, id);
            int userRows = stmt.executeUpdate();
            return userRows > 0;
        } catch (Exception e) {
            System.out.println("Delete session failed");
            System.out.println(e.getMessage());
        }

        return false;
    }

    public static boolean changeUsername(int id, String username) {
        String sql = """
                UPDATE users
                SET username = ?
                WHERE id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setInt(2, id);
            int row = stmt.executeUpdate();
            return row > 0;
        } catch (Exception e) {
            System.out.println("Delete session failed");
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static boolean changePassword(int id, String passwordHash) {
        return changePassword(id, passwordHash, null);
    }

    public static boolean changePassword(int id, String passwordHash, String subsonicTokenSecret) {
        String sql = """
                UPDATE users
                SET password_hash = ?,
                    subsonic_token_secret = COALESCE(?, subsonic_token_secret)
                WHERE id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, passwordHash);
            stmt.setString(2, subsonicTokenSecret);
            stmt.setInt(3, id);
            int row = stmt.executeUpdate();
            return row > 0;
        } catch (Exception e) {
            System.out.println("Delete session failed");
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static int createPlaylist(int userId, String playlistName) {
        return createPlaylist(userId, playlistName, "", false);
    }

    public static int createPlaylist(int userId, String playlistName, String comment, boolean isPublic) {
        String sql = """
                INSERT INTO playlists (
                    user_id,
                    name,
                    comment,
                    is_public
                )
                VALUES (?, ?, ?, ?)
                RETURNING id;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setString(2, playlistName);
            stmt.setString(3, comment);
            stmt.setBoolean(4, isPublic);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            System.out.println("Create playlist failed");
            System.out.println(e.getMessage());
        }
        return -1;
    }

    public static List<Playlist> getPlaylists(int userId) {
        List<Playlist> playlists = new ArrayList<>();
        String sql = """
                SELECT
                    id,
                    name,
                    cover_path
                FROM playlists
                WHERE user_id = ?
                ORDER BY name;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Playlist playlist = new Playlist(rs.getInt("id"), rs.getString("name"), hasUsablePlaylistCoverPath(rs.getString("cover_path")));
                playlists.add(playlist);
            }
        } catch (Exception e) {
            System.out.println("Get playlists failed");
            System.out.println(e.getMessage());
        }
        return playlists;
    }

    public static List<PlaylistInfo> getPlaylistInfos(int userId, boolean isAdmin) {
        List<PlaylistInfo> playlists = new ArrayList<>();
        String sql = """
                SELECT
                    playlists.id AS playlist_id,
                    playlists.name AS playlist_name,
                    COALESCE(playlists.comment, '') AS playlist_comment,
                    users.username AS owner_username,
                    playlists.is_public AS is_public,
                    playlists.created_at AS created_at,
                    playlists.changed_at AS changed_at,
                    COUNT(DISTINCT playlist_songs.song_id) AS song_count,
                    COALESCE(SUM(DISTINCT songs.duration_seconds) FILTER (WHERE songs.id IS NOT NULL), 0) AS total_duration,
                    playlists.cover_path AS cover_path
                FROM playlists
                JOIN users ON users.id = playlists.user_id
                LEFT JOIN playlist_songs ON playlist_songs.playlist_id = playlists.id
                LEFT JOIN songs ON songs.id = playlist_songs.song_id
                WHERE playlists.user_id = ?
                GROUP BY playlists.id, users.username
                ORDER BY playlists.name;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                playlists.add(mapPlaylistInfo(rs, isAdmin, userId));
            }
        } catch (Exception e) {
            System.out.println("Get playlist infos failed");
            System.out.println(e.getMessage());
        }

        return playlists;
    }

    public static PlaylistInfo getPlaylistInfo(int playlistId, int requesterUserId, boolean requesterIsAdmin) {
        String sql = """
                SELECT
                    playlists.id AS playlist_id,
                    playlists.user_id AS owner_id,
                    playlists.name AS playlist_name,
                    COALESCE(playlists.comment, '') AS playlist_comment,
                    users.username AS owner_username,
                    playlists.is_public AS is_public,
                    playlists.created_at AS created_at,
                    playlists.changed_at AS changed_at,
                    COUNT(DISTINCT playlist_songs.song_id) AS song_count,
                    COALESCE(SUM(DISTINCT songs.duration_seconds) FILTER (WHERE songs.id IS NOT NULL), 0) AS total_duration,
                    playlists.cover_path AS cover_path
                FROM playlists
                JOIN users ON users.id = playlists.user_id
                LEFT JOIN playlist_songs ON playlist_songs.playlist_id = playlists.id
                LEFT JOIN songs ON songs.id = playlist_songs.song_id
                WHERE playlists.id = ?
                GROUP BY playlists.id, users.username;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, playlistId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int ownerId = rs.getInt("owner_id");
                boolean isPublic = rs.getBoolean("is_public");
                if (!requesterIsAdmin && ownerId != requesterUserId && !isPublic) {
                    return null;
                }
                return mapPlaylistInfo(rs, requesterIsAdmin, requesterUserId);
            }
        } catch (Exception e) {
            System.out.println("Get playlist info failed");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public static boolean verifyPlaylist(int userId, int playlistId) {
        String sql = """
                SELECT id
                FROM playlists
                WHERE id = ?
                  AND user_id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, playlistId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("Create playlist failed");
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static int nextPosition(int playlistId) {
        String sql = """
                SELECT COALESCE(MAX(position), 0) + 1 AS next_position
                FROM playlist_songs
                WHERE playlist_id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, playlistId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("next_position");
            }
        } catch (Exception e) {
            System.out.println("Create playlist failed");
            System.out.println(e.getMessage());
        }
        return -1;
    }

    public static void insertSongsToPlaylist(int playlistId, int songId) {
        String sql = """
                INSERT INTO playlist_songs (
                    playlist_id,
                    song_id,
                    position
                )
                VALUES (?, ?, ?);
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, playlistId);
            stmt.setInt(2, songId);
            stmt.setInt(3, nextPosition(playlistId));
            stmt.executeUpdate();
            touchPlaylist(playlistId);
        } catch (Exception e) {
            System.out.println("Insert Song Failed");
            System.out.println(e.getMessage());
        }
    }

    public static List<Song> getSongsPlaylist(int playlistId) {
        List<Song> songs = new ArrayList<>();
        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds,
                    songs.file_path AS file_path,
                    songs.file_size AS file_size,
                    songs.file_modified_time AS file_modified_time,
                    songs.artwork_path AS artwork_path,
                    playlist_songs.position AS position
                FROM playlist_songs
                JOIN songs ON playlist_songs.song_id = songs.id
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists ON song_artists.song_id = songs.id
                JOIN artists ON song_artists.artist_id = artists.id
                WHERE playlist_songs.playlist_id = ?
                GROUP BY songs.id, albums.id, playlist_songs.position
                ORDER BY playlist_songs.position;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, playlistId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
                song.setId(rs.getInt("song_id"));
                songs.add(song);
            }
        } catch (Exception e) {
            System.out.println("Get playlists failed");
            System.out.println(e.getMessage());
        }
        return songs;
    }

    public static boolean deleteSongFromPlaylist(int playlistId, int songId) {
        String sql = """
                DELETE FROM playlist_songs
                WHERE playlist_id = ?
                  AND song_id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, playlistId);
            stmt.setInt(2, songId);
            int row = stmt.executeUpdate();
            if (row > 0) {
                touchPlaylist(playlistId);
                return true;
            }
        } catch (Exception e) {
            System.out.println("Create playlist failed");
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static boolean deletePlaylist(int playlistId, int userId) {
        String deletePlaylistSongsSQl = """
                DELETE FROM playlist_songs
                WHERE playlist_id = ?;
                """;

        String deletePlaylistSql = """
                DELETE FROM playlists
                WHERE id = ?
                  AND user_id = ?;
                """;

        try {
            PreparedStatement stmt = connection.prepareStatement(deletePlaylistSongsSQl);
            stmt.setInt(1, playlistId);
            stmt.executeUpdate();

            stmt = connection.prepareStatement(deletePlaylistSql);
            stmt.setInt(1, playlistId);
            stmt.setInt(2, userId);
            int row = stmt.executeUpdate();

            if (row > 0) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("Create playlist failed");
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static boolean likeSong(int userId, int songId) {
        String sql = """
                INSERT INTO liked_songs (
                    user_id,
                    song_id
                )
                VALUES (?, ?)
                ON CONFLICT (user_id, song_id)
                DO NOTHING;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, songId);
            int row = stmt.executeUpdate();
            if (row > 0) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("Create playlist failed");
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static boolean unlikeSong(int userId, int songId) {
        String sql = """
                DELETE FROM liked_songs
                WHERE user_id = ?
                  AND song_id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, songId);
            int row = stmt.executeUpdate();
            if (row > 0) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("Create playlist failed");
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static List<Song> getAllLikedSongs(int userId) {
        List<Song> songs = new ArrayList<>();
        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds,
                    songs.file_path AS file_path,
                    songs.file_size AS file_size,
                    songs.file_modified_time AS file_modified_time,
                    songs.artwork_path AS artwork_path
                FROM liked_songs
                JOIN songs ON liked_songs.song_id = songs.id
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists ON song_artists.song_id = songs.id
                JOIN artists ON song_artists.artist_id = artists.id
                WHERE liked_songs.user_id = ?
                GROUP BY songs.id, albums.id, liked_songs.liked_at
                ORDER BY liked_songs.liked_at DESC;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
                song.setId(rs.getInt("song_id"));
                songs.add(song);
            }
        } catch (Exception e) {
            System.out.println("Get liked songs failed");
            System.out.println(e.getMessage());
        }
        return songs;
    }

    public static void addToRecentlyPlayer(int userId, int songId) {
        addToRecentlyPlayer(userId, songId, null);
    }

    public static void addToRecentlyPlayer(int userId, int songId, Instant playedAt) {
        String sql = """
                INSERT INTO recently_played (
                    user_id,
                    song_id,
                    played_at
                )
                VALUES (?, ?, ?);
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, songId);
            if (playedAt == null) {
                stmt.setTimestamp(3, Timestamp.from(Instant.now()));
            } else {
                stmt.setTimestamp(3, Timestamp.from(playedAt));
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Insert Recently played failed");
            System.out.println(e.getMessage());
        }
    }

    public static List<Song> getAllRecentlyPlayedSongs(int userId, int limit, int offset) {
        List<Song> songs = new ArrayList<>();
        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds,
                    songs.file_path AS file_path,
                    songs.file_size AS file_size,
                    songs.file_modified_time AS file_modified_time,
                    songs.artwork_path AS artwork_path
                FROM recently_played
                JOIN songs ON recently_played.song_id = songs.id
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists ON song_artists.song_id = songs.id
                JOIN artists ON song_artists.artist_id = artists.id
                WHERE recently_played.user_id = ?
                GROUP BY songs.id, albums.id, recently_played.id, recently_played.played_at
                ORDER BY recently_played.played_at DESC
                LIMIT ?
                OFFSET ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
                song.setId(rs.getInt("song_id"));
                songs.add(song);
            }
        } catch (Exception e) {
            System.out.println("Get Recently Played Songs Failed");
            System.out.println(e.getMessage());
        }
        return songs;
    }

    public static List<MostPlayedSong> getAllMostPlayedSongs(int userId, int limit, int offset) {
        List<MostPlayedSong> mostPlayedSongs = new ArrayList<>();
        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds,
                    songs.file_path AS file_path,
                    songs.file_size AS file_size,
                    songs.file_modified_time AS file_modified_time,
                    songs.artwork_path AS artwork_path,
                    COUNT(*) AS play_count
                FROM recently_played
                JOIN songs ON recently_played.song_id = songs.id
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists ON song_artists.song_id = songs.id
                JOIN artists ON song_artists.artist_id = artists.id
                WHERE recently_played.user_id = ?
                GROUP BY songs.id, albums.id
                ORDER BY play_count DESC, songs.title
                LIMIT ?
                OFFSET ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
                song.setId(rs.getInt("song_id"));
                mostPlayedSongs.add(new MostPlayedSong(song, rs.getInt("play_count")));
            }
        } catch (Exception e) {
            System.out.println("Get Recently Played Songs Failed");
            System.out.println(e.getMessage());
        }
        return mostPlayedSongs;
    }

    public static void reorderSongsInPlaylist(int position, int playlistId, int songId) {
        String sql = """
                UPDATE playlist_songs
                SET position = ?
                WHERE playlist_id = ?
                  AND song_id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, position);
            stmt.setInt(2, playlistId);
            stmt.setInt(3, songId);
            stmt.executeUpdate();
            touchPlaylist(playlistId);
        } catch (Exception e) {
            System.out.println("Song Reorder Failed");
            System.out.println(e.getMessage());
        }
    }

    public static Map<String, Integer> getStats(int userId) {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM songs) AS song_count,
                    (SELECT COUNT(*) FROM albums) AS album_count,
                    (SELECT COUNT(*) FROM artists) AS artist_count,
                    (SELECT COUNT(*) FROM playlists WHERE user_id = ?) AS playlist_count;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Map.of("songs:", rs.getInt("song_count"), "albums:", rs.getInt("album_count"), "artists", rs.getInt("artist_count"), "playlists", rs.getInt("playlist_count"));
            }
        } catch (Exception e) {
            System.out.println("Song Reorder Failed");
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static Song getSongInfo(int songId) {
        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
                    songs.genre AS genre,
                    STRING_AGG(artists.name, ', ') AS artist_name,
                    songs.album_id AS album_id,
                    albums.title AS album_title,
                    songs.duration_seconds AS duration_seconds
                FROM songs
                JOIN albums ON songs.album_id = albums.id
                JOIN song_artists ON song_artists.song_id = songs.id
                JOIN artists ON song_artists.artist_id = artists.id
                WHERE songs.id = ?
                GROUP BY songs.id, albums.id;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, songId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getString("genre"), rs.getInt("album_id"), rs.getInt("duration_seconds"), "", 0L, 0L, "");
                song.setId(rs.getInt("song_id"));
                return song;
            }
        } catch (Exception e) {
            System.out.println("Song Reorder Failed");
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static void setPlaylistCoverart(int playlistId, String path) {
        String sql = """
                UPDATE playlists
                SET cover_path = ?
                WHERE id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, path);
            stmt.setInt(2, playlistId);
            stmt.executeUpdate();
            touchPlaylist(playlistId);
        } catch (Exception e) {
            System.out.println("Song Reorder Failed");
            System.out.println(e.getMessage());
        }
    }

    public static String getPlaylistCoverartPath(int playlistId) {
        String sql = """
                SELECT cover_path
                FROM playlists
                WHERE id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, playlistId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("cover_path");
            }
        } catch (Exception e) {
            System.out.println("Song Reorder Failed");
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static void updateSubsonicTokenSecret(int userId, String subsonicTokenSecret) {
        String sql = """
                UPDATE users
                SET subsonic_token_secret = ?
                WHERE id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, subsonicTokenSecret);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Update Subsonic token secret failed");
            System.out.println(e.getMessage());
        }
    }

    public static String getSubsonicTokenSecret(int userId) {
        String sql = """
                SELECT subsonic_token_secret
                FROM users
                WHERE id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("subsonic_token_secret");
            }
        } catch (Exception e) {
            System.out.println("Get Subsonic token secret failed");
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static boolean updatePlaylistMetadata(int playlistId, int userId, String name, String comment, Boolean isPublic) {
        List<String> assignments = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (name != null) {
            assignments.add("name = ?");
            values.add(name);
        }
        if (comment != null) {
            assignments.add("comment = ?");
            values.add(comment);
        }
        if (isPublic != null) {
            assignments.add("is_public = ?");
            values.add(isPublic);
        }

        assignments.add("changed_at = NOW()");

        String sql = """
                UPDATE playlists
                SET %s
                WHERE id = ?
                  AND user_id = ?;
                """.formatted(String.join(", ", assignments));

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            int index = 1;
            for (Object value : values) {
                stmt.setObject(index, value);
                index += 1;
            }
            stmt.setInt(index, playlistId);
            stmt.setInt(index + 1, userId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Update playlist metadata failed");
            System.out.println(e.getMessage());
        }

        return false;
    }

    public static void replacePlaylistSongs(int playlistId, List<Integer> songIds) {
        try {
            PreparedStatement deleteStmt = connection.prepareStatement("""
                    DELETE FROM playlist_songs
                    WHERE playlist_id = ?;
                    """);
            deleteStmt.setInt(1, playlistId);
            deleteStmt.executeUpdate();

            if (songIds != null) {
                for (Integer songId : songIds) {
                    if (songId != null) {
                        insertSongsToPlaylist(playlistId, songId);
                    }
                }
            }

            touchPlaylist(playlistId);
        } catch (Exception e) {
            System.out.println("Replace playlist songs failed");
            System.out.println(e.getMessage());
        }
    }

    public static boolean deleteSongFromPlaylistByIndex(int playlistId, int songIndex) {
        if (songIndex < 0) {
            return false;
        }

        String sql = """
                DELETE FROM playlist_songs
                WHERE playlist_id = ?
                  AND position = (
                      SELECT position
                      FROM playlist_songs
                      WHERE playlist_id = ?
                      ORDER BY position
                      OFFSET ?
                      LIMIT 1
                  );
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, playlistId);
            stmt.setInt(2, playlistId);
            stmt.setInt(3, songIndex);
            int row = stmt.executeUpdate();
            if (row > 0) {
                touchPlaylist(playlistId);
                return true;
            }
        } catch (Exception e) {
            System.out.println("Delete playlist song by index failed");
            System.out.println(e.getMessage());
        }

        return false;
    }

    private static void touchPlaylist(int playlistId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("""
                    UPDATE playlists
                    SET changed_at = NOW()
                    WHERE id = ?;
                    """);
            stmt.setInt(1, playlistId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Touch playlist failed");
            System.out.println(e.getMessage());
        }
    }

    private static PlaylistInfo mapPlaylistInfo(ResultSet rs, boolean requesterIsAdmin, int requesterUserId) throws SQLException {
        int playlistId = rs.getInt("playlist_id");
        String owner = rs.getString("owner_username");
        boolean hasCover = hasUsablePlaylistCoverPath(rs.getString("cover_path"));
        boolean isPublic = rs.getBoolean("is_public");
        int ownerId = hasColumn(rs, "owner_id") ? rs.getInt("owner_id") : requesterUserId;
        boolean readonly = !requesterIsAdmin && ownerId != requesterUserId;
        String coverArt = hasCover ? "playlist:" + playlistId : "";

        return new PlaylistInfo(
                playlistId,
                rs.getString("playlist_name"),
                rs.getString("playlist_comment"),
                owner,
                isPublic,
                formatTimestamp(rs.getObject("created_at", OffsetDateTime.class)),
                formatTimestamp(rs.getObject("changed_at", OffsetDateTime.class)),
                rs.getInt("song_count"),
                rs.getInt("total_duration"),
                coverArt,
                readonly
        );
    }

    private static boolean hasUsablePlaylistCoverPath(String coverPath) {
        if (coverPath == null || coverPath.isBlank()) {
            return false;
        }

        File file = new File(coverPath);
        return file.exists() && file.isFile() && file.length() > 0;
    }

    private static boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static String formatTimestamp(OffsetDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }

        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp);
    }
}
