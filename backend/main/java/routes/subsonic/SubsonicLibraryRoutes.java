package routes.subsonic;

import auth.User;
import config.Properties;
import io.javalin.Javalin;
import postgresql.Database;
import scanner.Album;
import scanner.Artist;
import scanner.Song;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SubsonicLibraryRoutes {
    private static final String IGNORED_ARTICLES = "The An A Die Das Ein Eine Les Le La";
    private static final int DEFAULT_MUSIC_FOLDER_ID = 1;

    private SubsonicLibraryRoutes() {
    }

    public static void register(Javalin app) {
        SubsonicRequest.register(app, "/rest/getMusicFolders.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            SubsonicResponses.writeMusicFolders(ctx, List.of(Map.of(
                    "id", String.valueOf(DEFAULT_MUSIC_FOLDER_ID),
                    "name", getMusicFolderName()
            )));
        });

        SubsonicRequest.register(app, "/rest/getArtists.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            String musicFolderId = SubsonicRequest.param(ctx, "musicFolderId");
            if (!isBlank(musicFolderId) && !String.valueOf(DEFAULT_MUSIC_FOLDER_ID).equals(musicFolderId)) {
                SubsonicResponses.writeArtists(ctx, IGNORED_ARTICLES, Map.of());
                return;
            }

            SubsonicResponses.writeArtists(ctx, IGNORED_ARTICLES, getArtistsByIndex());
        });

        SubsonicRequest.register(app, "/rest/getIndexes.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            String musicFolderId = SubsonicRequest.param(ctx, "musicFolderId");
            if (!isBlank(musicFolderId) && !String.valueOf(DEFAULT_MUSIC_FOLDER_ID).equals(musicFolderId)) {
                SubsonicResponses.writeIndexes(ctx, IGNORED_ARTICLES, Map.of());
                return;
            }

            SubsonicResponses.writeIndexes(ctx, IGNORED_ARTICLES, getArtistsByIndex());
        });

        SubsonicRequest.register(app, "/rest/getAlbumList2.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            String type = SubsonicRequest.param(ctx, "type");
            if (isBlank(type)) {
                type = "alphabeticalByName";
            }

            int size = parseIntOrDefault(SubsonicRequest.param(ctx, "size"), 10);
            int offset = parseIntOrDefault(SubsonicRequest.param(ctx, "offset"), 0);
            String genre = SubsonicRequest.param(ctx, "genre");
            Integer fromYear = parseOptionalInt(SubsonicRequest.param(ctx, "fromYear"));
            Integer toYear = parseOptionalInt(SubsonicRequest.param(ctx, "toYear"));

            size = Math.max(0, Math.min(size, 500));
            offset = Math.max(0, offset);

            List<Album> albums = Database.getAlbumList(type, size, offset, user.getId(), genre, fromYear, toYear);
            if (albums.isEmpty() && !isSupportedAlbumListType(type)) {
                SubsonicResponses.writeError(ctx, 400, 0, "Unsupported album list type.");
                return;
            }

            SubsonicResponses.writeAlbumList2(ctx, albums);
        });

        SubsonicRequest.register(app, "/rest/search3.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            String query = SubsonicRequest.param(ctx, "query");
            int artistCount = clampSearchCount(SubsonicRequest.param(ctx, "artistCount"), 20);
            int albumCount = clampSearchCount(SubsonicRequest.param(ctx, "albumCount"), 20);
            int songCount = clampSearchCount(SubsonicRequest.param(ctx, "songCount"), 20);
            int artistOffset = Math.max(0, parseIntOrDefault(SubsonicRequest.param(ctx, "artistOffset"), 0));
            int albumOffset = Math.max(0, parseIntOrDefault(SubsonicRequest.param(ctx, "albumOffset"), 0));
            int songOffset = Math.max(0, parseIntOrDefault(SubsonicRequest.param(ctx, "songOffset"), 0));

            List<Artist> artists;
            List<Album> albums;
            List<Song> songs;

            if (isBlank(query)) {
                artists = artistCount == 0 ? List.of() : Database.getArtists(artistCount, artistOffset);
                albums = albumCount == 0 ? List.of() : Database.getAlbums(albumCount, albumOffset);
                songs = songCount == 0 ? List.of() : Database.scanSongs(songCount, songOffset);
            } else {
                artists = artistCount == 0 ? List.of() : Database.searchArtists(query, artistCount, artistOffset);
                albums = albumCount == 0 ? List.of() : Database.searchAlbums(query, albumCount, albumOffset);
                songs = songCount == 0 ? List.of() : Database.searchSongs(query, songCount, songOffset);
            }

            SubsonicResponses.writeSearchResult3(ctx, artists, albums, songs);
        });

        SubsonicRequest.register(app, "/rest/getGenres.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            SubsonicResponses.writeGenres(ctx, Database.getGenres());
        });

        SubsonicRequest.register(app, "/rest/getSongsByGenre.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            String genre = SubsonicRequest.param(ctx, "genre");
            if (isBlank(genre)) {
                SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
                return;
            }

            String musicFolderId = SubsonicRequest.param(ctx, "musicFolderId");
            if (!isBlank(musicFolderId) && !String.valueOf(DEFAULT_MUSIC_FOLDER_ID).equals(musicFolderId)) {
                SubsonicResponses.writeSongsByGenre(ctx, List.of());
                return;
            }

            int count = Math.max(0, Math.min(parseIntOrDefault(SubsonicRequest.param(ctx, "count"), 10), 500));
            int offset = Math.max(0, parseIntOrDefault(SubsonicRequest.param(ctx, "offset"), 0));

            SubsonicResponses.writeSongsByGenre(ctx, Database.getSongsByGenre(genre, count, offset));
        });

        SubsonicRequest.register(app, "/rest/getRandomSongs.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            String musicFolderId = SubsonicRequest.param(ctx, "musicFolderId");
            if (!isBlank(musicFolderId) && !String.valueOf(DEFAULT_MUSIC_FOLDER_ID).equals(musicFolderId)) {
                SubsonicResponses.writeRandomSongs(ctx, List.of());
                return;
            }

            int size = Math.max(0, Math.min(parseIntOrDefault(SubsonicRequest.param(ctx, "size"), 10), 500));
            String genre = SubsonicRequest.param(ctx, "genre");

            SubsonicResponses.writeRandomSongs(ctx, Database.getRandomSongs(size, genre));
        });

        SubsonicRequest.register(app, "/rest/getStarred.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            List<Song> songs = Database.getAllLikedSongs(user.getId());
            SubsonicResponses.writeStarred(ctx, songs);
        });

        SubsonicRequest.register(app, "/rest/star.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            applyStarMutation(ctx, user.getId(), true);
            SubsonicResponses.writeSuccess(ctx);
        });

        SubsonicRequest.register(app, "/rest/unstar.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            applyStarMutation(ctx, user.getId(), false);
            SubsonicResponses.writeSuccess(ctx);
        });

        SubsonicRequest.register(app, "/rest/scrobble.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            List<String> songIds = paramValues(ctx, "id");
            if (songIds.isEmpty()) {
                SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
                return;
            }

            boolean submission = parseSubmission(SubsonicRequest.param(ctx, "submission"));
            if (!submission) {
                SubsonicResponses.writeSuccess(ctx);
                return;
            }

            List<String> times = paramValues(ctx, "time");
            for (int index = 0; index < songIds.size(); index += 1) {
                String songIdValue = songIds.get(index);
                Integer songId = parseOptionalInt(songIdValue);
                if (songId != null && Database.getSong(songId) != null) {
                    Database.addToRecentlyPlayer(user.getId(), songId, parseScrobbleTime(index < times.size() ? times.get(index) : null));
                }
            }

            SubsonicResponses.writeSuccess(ctx);
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int clampSearchCount(String value, int defaultValue) {
        return Math.max(0, Math.min(parseIntOrDefault(value, defaultValue), 500));
    }

    private static Integer parseOptionalInt(String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Instant parseScrobbleTime(String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            long millis = Long.parseLong(value);
            return millis >= 0 ? Instant.ofEpochMilli(millis) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean parseSubmission(String value) {
        if (isBlank(value)) {
            return true;
        }

        return !"false".equalsIgnoreCase(value);
    }

    private static boolean isSupportedAlbumListType(String type) {
        return "alphabeticalByName".equals(type)
                || "alphabeticalByArtist".equals(type)
                || "byGenre".equals(type)
                || "byYear".equals(type)
                || "newest".equals(type)
                || "random".equals(type)
                || "recent".equals(type)
                || "frequent".equals(type)
                || "starred".equals(type);
    }

    private static void applyStarMutation(io.javalin.http.Context ctx, int userId, boolean star) {
        for (String songIdValue : paramValues(ctx, "id")) {
            Integer songId = parseOptionalInt(songIdValue);
            if (songId != null) {
                applySongLike(userId, songId, star);
            }
        }

        for (String albumIdValue : paramValues(ctx, "albumId")) {
            Integer albumId = parseOptionalInt(albumIdValue);
            if (albumId == null) {
                continue;
            }

            for (Song song : Database.getAlbumsSongs(albumId)) {
                applySongLike(userId, song.getId(), star);
            }
        }

        for (String artistIdValue : paramValues(ctx, "artistId")) {
            Integer artistId = parseOptionalInt(artistIdValue);
            if (artistId == null) {
                continue;
            }

            for (Song song : Database.getArtistSongs(artistId)) {
                applySongLike(userId, song.getId(), star);
            }
        }
    }

    private static List<String> paramValues(io.javalin.http.Context ctx, String name) {
        List<String> values = SubsonicRequest.params(ctx, name);
        return values == null ? List.of() : values;
    }

    private static void applySongLike(int userId, int songId, boolean star) {
        if (star) {
            Database.likeSong(userId, songId);
            return;
        }

        Database.unlikeSong(userId, songId);
    }

    private static Map<String, List<Artist>> getArtistsByIndex() {
        List<Artist> artists = Database.getAllArtists();
        Map<String, List<Artist>> artistsByIndex = new LinkedHashMap<>();

        for (Artist artist : artists) {
            String indexName = getArtistIndexName(artist.getName());
            artistsByIndex.computeIfAbsent(indexName, key -> new java.util.ArrayList<>()).add(artist);
        }

        return artistsByIndex;
    }

    private static String getMusicFolderName() {
        String songsFolder = Properties.getSongsFolder();
        if (isBlank(songsFolder)) {
            return "music";
        }

        Path path = Path.of(songsFolder);
        Path fileName = path.getFileName();
        if (fileName == null) {
            return songsFolder;
        }

        return fileName.toString();
    }

    private static String getArtistIndexName(String artistName) {
        String normalizedName = stripIgnoredArticle(artistName).trim();
        if (normalizedName.isEmpty()) {
            return "#";
        }

        char firstChar = Character.toUpperCase(normalizedName.charAt(0));
        if (Character.isLetter(firstChar)) {
            return String.valueOf(firstChar);
        }

        return "#";
    }

    private static String stripIgnoredArticle(String artistName) {
        if (artistName == null) {
            return "";
        }

        for (String article : IGNORED_ARTICLES.split(" ")) {
            String prefix = article + " ";
            if (artistName.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return artistName.substring(prefix.length());
            }
        }

        return artistName;
    }
}
