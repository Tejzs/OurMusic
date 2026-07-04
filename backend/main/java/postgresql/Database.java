package postgresql;

import auth.User;
import config.Properties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import scanner.Album;
import scanner.Artist;
import scanner.MostPlayedSong;
import scanner.Playlist;
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
                    name TEXT UNIQUE NOT NULL
                );
                """;
        connection.prepareStatement(artistsTable).execute();

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
                    is_admin BOOLEAN DEFAULT FALSE
                );
                """;
        connection.prepareStatement(usersTable).execute();

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
                    cover_path TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
                """;
        connection.prepareStatement(playlistsTable).execute();

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
                    album_id,
                    duration_seconds,
                    file_path,
                    file_size,
                    file_modified_time,
                    artwork_path
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (file_path)
                DO UPDATE SET
                    title = EXCLUDED.title,
                    album_id = EXCLUDED.album_id,
                    duration_seconds = EXCLUDED.duration_seconds,
                    file_size = EXCLUDED.file_size,
                    file_modified_time = EXCLUDED.file_modified_time,
                    artwork_path = EXCLUDED.artwork_path
                RETURNING id;
                """;

        PreparedStatement stmt = connection.prepareStatement(sql);

        stmt.setString(1, song.getTitle());
        stmt.setInt(2, song.getAlbumId());
        stmt.setInt(3, song.getDuration());
        stmt.setString(4, song.getFilePath());
        stmt.setLong(5, song.getFileSize());
        stmt.setLong(6, song.getLastModified());
        stmt.setString(7, song.getArtworkPath());

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
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
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
        List<Song> songs = new ArrayList();

        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
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

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            String searchPattern = "%" + pattern + "%";

            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
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

    public static int searchSongFromFilePath(String path) {
        String sql = """
                SELECT id
                FROM songs
                WHERE file_path LIKE ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            String searchPattern = "%/" + path;

            stmt.setString(1, searchPattern);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            System.out.println("Failed to find: " + path);
            System.out.println(e.getMessage());
        }
        return -1;
    }

    public static List<Song> scanSongs(int limit, int offset) {
        List<Song> songs = new ArrayList<>();

        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
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
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));

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
                    MIN(songs.id) AS artwork_song_id
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

    public static List<Song> getAlbumsSongs(int id) {
        List<Song> songs = new ArrayList<>();

        String sql = """
                SELECT
                    songs.id AS song_id,
                    songs.title AS song_title,
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
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));

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
                    COUNT(DISTINCT song_artists.song_id) AS song_count
                FROM artists
                LEFT JOIN albums ON albums.artist_id = artists.id
                LEFT JOIN song_artists ON song_artists.artist_id = artists.id
                GROUP BY artists.id, artists.name
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
                Artist artist = new Artist(rs.getInt("artist_id"), rs.getString("artist_name"), rs.getInt("album_count"), rs.getInt("song_count"));

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
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
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
                    MIN(songs.id) AS artwork_song_id
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
        String checkSql = """
                SELECT id
                FROM users
                WHERE username = ?;
                """;

        String insertSql = """
                INSERT INTO users (
                    username,
                    password_hash
                )
                VALUES (?, ?)
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
        String sql = """
                UPDATE users
                SET password_hash = ?
                WHERE id = ?;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, passwordHash);
            stmt.setInt(2, id);
            int row = stmt.executeUpdate();
            return row > 0;
        } catch (Exception e) {
            System.out.println("Delete session failed");
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static int createPlaylist(int userId, String playlistName) {
        String sql = """
                INSERT INTO playlists (
                    user_id,
                    name
                )
                VALUES (?, ?)
                RETURNING id;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setString(2, playlistName);
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
                    cover_path IS NOT NULL AS has_cover
                FROM playlists
                WHERE user_id = ?
                ORDER BY name;
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Playlist playlist = new Playlist(rs.getInt("id"), rs.getString("name"), rs.getBoolean("has_cover"));
                playlists.add(playlist);
            }
        } catch (Exception e) {
            System.out.println("Get playlists failed");
            System.out.println(e.getMessage());
        }
        return playlists;
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
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
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
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
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
        String sql = """
                INSERT INTO recently_played (
                    user_id,
                    song_id
                )
                VALUES (?, ?);
                """;
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, songId);
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
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
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
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getInt("album_id"), rs.getInt("duration_seconds"), rs.getString("file_path"), rs.getLong("file_size"), rs.getLong("file_modified_time"), rs.getString("artwork_path"));
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
                Song song = new Song(rs.getString("song_title"), rs.getString("artist_name"), rs.getString("album_title"), rs.getInt("album_id"), rs.getInt("duration_seconds"), "", 0L, 0L, "");
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
}
