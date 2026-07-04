package routes.subsonic;

import com.google.gson.Gson;
import io.javalin.http.Context;

import scanner.Artist;
import scanner.Album;
import scanner.PlaylistInfo;
import auth.User;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SubsonicResponses {
    private static final Gson GSON = new Gson();
    private static final String API_VERSION = "1.16.1";
    private static final String SERVER_TYPE = "OurMusic";
    private static final String SERVER_VERSION = "0.1.0";

    private SubsonicResponses() {
    }

    public static void writeSuccess(Context ctx) {
        Map<String, Object> payload = baseResponse("ok");
        writeResponse(ctx, 200, payload, null);
    }

    public static void writeLicense(Context ctx, boolean valid, String email, String licenseExpires, String trialExpires) {
        Map<String, Object> payload = baseResponse("ok");
        Map<String, Object> license = new LinkedHashMap<>();
        license.put("valid", valid);
        license.put("email", email);
        license.put("licenseExpires", licenseExpires);
        license.put("trialExpires", trialExpires);
        payload.put("license", license);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<license");
        xml.append(" valid=\"").append(valid).append("\"");
        xml.append(" email=\"").append(escapeXml(email)).append("\"");
        xml.append(" licenseExpires=\"").append(escapeXml(licenseExpires)).append("\"");
        xml.append(" trialExpires=\"").append(escapeXml(trialExpires)).append("\"");
        xml.append("/>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeOpenSubsonicExtensions(Context ctx, List<Map<String, Object>> extensions) {
        Map<String, Object> payload = baseResponse("ok");
        payload.put("openSubsonicExtensions", extensions);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<openSubsonicExtensions>");
        for (Map<String, Object> extension : extensions) {
            xml.append("<openSubsonicExtension");
            xml.append(" name=\"").append(escapeXml(String.valueOf(extension.get("name")))).append("\"");
            xml.append(">");
            Object versions = extension.get("versions");
            if (versions instanceof List<?> versionList) {
                xml.append("<versions>");
                for (Object version : versionList) {
                    xml.append("<version>").append(escapeXml(String.valueOf(version))).append("</version>");
                }
                xml.append("</versions>");
            }
            xml.append("</openSubsonicExtension>");
        }
        xml.append("</openSubsonicExtensions>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeScanStatus(Context ctx, boolean scanning, int count) {
        Map<String, Object> scanStatus = new LinkedHashMap<>();
        scanStatus.put("scanning", scanning);
        scanStatus.put("count", Math.max(0, count));

        Map<String, Object> payload = baseResponse("ok");
        payload.put("scanStatus", scanStatus);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<scanStatus");
        xml.append(" scanning=\"").append(scanning).append("\"");
        xml.append(" count=\"").append(Math.max(0, count)).append("\"");
        xml.append("/>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeMusicFolders(Context ctx, List<Map<String, Object>> musicFolders) {
        Map<String, Object> payload = baseResponse("ok");
        payload.put("musicFolders", Map.of("musicFolder", musicFolders));

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<musicFolders>");
        for (Map<String, Object> musicFolder : musicFolders) {
            xml.append("<musicFolder");
            xml.append(" id=\"").append(escapeXml(String.valueOf(musicFolder.get("id")))).append("\"");
            xml.append(" name=\"").append(escapeXml(String.valueOf(musicFolder.get("name")))).append("\"");
            xml.append("/>");
        }
        xml.append("</musicFolders>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeArtists(Context ctx, String ignoredArticles, Map<String, List<Artist>> artistsByIndex) {
        Map<String, Object> artistsPayload = new LinkedHashMap<>();
        artistsPayload.put("ignoredArticles", ignoredArticles);

        List<Map<String, Object>> indexes = artistsByIndex.entrySet().stream()
                .map(entry -> Map.of(
                        "name", entry.getKey(),
                        "artist", entry.getValue().stream()
                                .map(SubsonicResponses::toArtistPayload)
                                .toList()
                ))
                .toList();

        artistsPayload.put("index", indexes);

        Map<String, Object> payload = baseResponse("ok");
        payload.put("artists", artistsPayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<artists ignoredArticles=\"").append(escapeXml(ignoredArticles)).append("\">");
        for (Map.Entry<String, List<Artist>> entry : artistsByIndex.entrySet()) {
            xml.append("<index name=\"").append(escapeXml(entry.getKey())).append("\">");
            for (Artist artist : entry.getValue()) {
                xml.append("<artist");
                xml.append(" id=\"").append(artist.getId()).append("\"");
                xml.append(" name=\"").append(escapeXml(artist.getName())).append("\"");
                xml.append(" albumCount=\"").append(artist.getAlbumCount()).append("\"");
                xml.append(" songCount=\"").append(artist.getSongCount()).append("\"");
                xml.append(" coverArt=\"").append(escapeXml(SubsonicArtistImageService.coverArtId(artist.getId()))).append("\"");
                xml.append("/>");
            }
            xml.append("</index>");
        }
        xml.append("</artists>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeIndexes(Context ctx, String ignoredArticles, Map<String, List<Artist>> artistsByIndex) {
        Map<String, Object> indexesPayload = new LinkedHashMap<>();
        indexesPayload.put("ignoredArticles", ignoredArticles);
        indexesPayload.put("lastModified", 0);

        List<Map<String, Object>> indexes = artistsByIndex.entrySet().stream()
                .map(entry -> Map.of(
                        "name", entry.getKey(),
                        "artist", entry.getValue().stream()
                                .map(SubsonicResponses::toArtistPayload)
                                .toList()
                ))
                .toList();

        indexesPayload.put("index", indexes);

        Map<String, Object> payload = baseResponse("ok");
        payload.put("indexes", indexesPayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<indexes ignoredArticles=\"").append(escapeXml(ignoredArticles)).append("\" lastModified=\"0\">");
        for (Map.Entry<String, List<Artist>> entry : artistsByIndex.entrySet()) {
            xml.append("<index name=\"").append(escapeXml(entry.getKey())).append("\">");
            for (Artist artist : entry.getValue()) {
                xml.append("<artist");
                xml.append(" id=\"").append(artist.getId()).append("\"");
                xml.append(" name=\"").append(escapeXml(artist.getName())).append("\"");
                xml.append(" albumCount=\"").append(artist.getAlbumCount()).append("\"");
                xml.append(" songCount=\"").append(artist.getSongCount()).append("\"");
                xml.append(" coverArt=\"").append(escapeXml(SubsonicArtistImageService.coverArtId(artist.getId()))).append("\"");
                xml.append("/>");
            }
            xml.append("</index>");
        }
        xml.append("</indexes>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeUser(Context ctx, User user) {
        Map<String, Object> userPayload = new LinkedHashMap<>();
        userPayload.put("username", user.getUsername());
        userPayload.put("scrobblingEnabled", true);
        userPayload.put("adminRole", user.isAdmin());
        userPayload.put("settingsRole", user.isAdmin());
        userPayload.put("downloadRole", true);
        userPayload.put("uploadRole", false);
        userPayload.put("playlistRole", true);
        userPayload.put("coverArtRole", true);
        userPayload.put("commentRole", false);
        userPayload.put("podcastRole", false);
        userPayload.put("streamRole", true);
        userPayload.put("jukeboxRole", false);
        userPayload.put("shareRole", false);
        userPayload.put("videoConversionRole", false);

        Map<String, Object> payload = baseResponse("ok");
        payload.put("user", userPayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<user");
        xml.append(" username=\"").append(escapeXml(user.getUsername())).append("\"");
        xml.append(" scrobblingEnabled=\"true\"");
        xml.append(" adminRole=\"").append(user.isAdmin()).append("\"");
        xml.append(" settingsRole=\"").append(user.isAdmin()).append("\"");
        xml.append(" downloadRole=\"true\"");
        xml.append(" uploadRole=\"false\"");
        xml.append(" playlistRole=\"true\"");
        xml.append(" coverArtRole=\"true\"");
        xml.append(" commentRole=\"false\"");
        xml.append(" podcastRole=\"false\"");
        xml.append(" streamRole=\"true\"");
        xml.append(" jukeboxRole=\"false\"");
        xml.append(" shareRole=\"false\"");
        xml.append(" videoConversionRole=\"false\"");
        xml.append("/>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeAlbumList2(Context ctx, List<Album> albums) {
        List<Map<String, Object>> albumPayload = albums.stream()
                .map(SubsonicResponses::toAlbumPayload)
                .toList();

        Map<String, Object> payload = baseResponse("ok");
        payload.put("albumList2", Map.of("album", albumPayload));

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<albumList2>");
        for (Album album : albums) {
            appendAlbumElement(xml, album);
        }
        xml.append("</albumList2>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }


    public static void writeArtist(Context ctx, Artist artist, List<Album> albums) {
        List<Map<String, Object>> albumPayload = albums.stream().map(SubsonicResponses::toAlbumPayload).toList();
        Map<String, Object> artistPayload = new LinkedHashMap<>();
        artistPayload.put("id", artist.getId());
        artistPayload.put("name", sanitizeText(artist.getName()));
        artistPayload.put("albumCount", artist.getAlbumCount());
        artistPayload.put("songCount", artist.getSongCount());
        artistPayload.put("coverArt", SubsonicArtistImageService.coverArtId(artist.getId()));
        artistPayload.put("album", albumPayload);

        Map<String, Object> payload = baseResponse("ok");
        payload.put("artist", artistPayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<artist");
        xml.append(" id=\"").append(artist.getId()).append("\"");
        xml.append(" name=\"").append(escapeXml(artist.getName())).append("\"");
        xml.append(" albumCount=\"").append(artist.getAlbumCount()).append("\"");
        xml.append(" songCount=\"").append(artist.getSongCount()).append("\"");
        xml.append(" coverArt=\"").append(escapeXml(SubsonicArtistImageService.coverArtId(artist.getId()))).append("\"");
        xml.append(">");
        for (Album album : albums) {
            appendAlbumElement(xml, album);
        }
        xml.append("</artist>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeAlbum(Context ctx, Album album, List<scanner.Song> songs) {
        List<Map<String, Object>> songPayload = songs.stream().map(SubsonicResponses::toSongPayload).toList();
        Map<String, Object> albumPayload = new LinkedHashMap<>();
        albumPayload.put("id", id(album.getId()));
        albumPayload.put("parent", id(album.getArtistId()));
        albumPayload.put("title", sanitizeText(album.getTitle()));
        albumPayload.put("name", sanitizeText(album.getTitle()));
        albumPayload.put("album", sanitizeText(album.getTitle()));
        albumPayload.put("artist", sanitizeText(album.getArtist()));
        albumPayload.put("artistId", id(album.getArtistId()));
        albumPayload.put("isDir", true);
        albumPayload.put("songCount", album.getSongCount());
        albumPayload.put("duration", songs.stream().mapToInt(scanner.Song::getDuration).sum());
        putIfNotBlank(albumPayload, "created", formatInstant(songs.stream()
                .mapToLong(scanner.Song::getLastModified)
                .filter(lastModified -> lastModified > 0)
                .min()
                .orElse(0L)));
        if (album.getArtworkSongId() > 0) {
            albumPayload.put("coverArt", id(album.getArtworkSongId()));
        }
        albumPayload.put("song", songPayload);

        Map<String, Object> payload = baseResponse("ok");
        payload.put("album", albumPayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<album");
        xml.append(" id=\"").append(album.getId()).append("\"");
        xml.append(" parent=\"").append(album.getArtistId()).append("\"");
        xml.append(" title=\"").append(escapeXml(album.getTitle())).append("\"");
        xml.append(" name=\"").append(escapeXml(album.getTitle())).append("\"");
        xml.append(" album=\"").append(escapeXml(album.getTitle())).append("\"");
        xml.append(" artist=\"").append(escapeXml(album.getArtist())).append("\"");
        xml.append(" artistId=\"").append(album.getArtistId()).append("\"");
        xml.append(" isDir=\"true\"");
        xml.append(" songCount=\"").append(album.getSongCount()).append("\"");
        xml.append(" duration=\"").append(songs.stream().mapToInt(scanner.Song::getDuration).sum()).append("\"");
        appendXmlAttribute(xml, "created", formatInstant(songs.stream()
                .mapToLong(scanner.Song::getLastModified)
                .filter(lastModified -> lastModified > 0)
                .min()
                .orElse(0L)));
        if (album.getArtworkSongId() > 0) {
            xml.append(" coverArt=\"").append(album.getArtworkSongId()).append("\"");
        }
        xml.append(">");
        for (scanner.Song song : songs) {
            appendSongElement(xml, song);
        }
        xml.append("</album>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeSearchResult3(Context ctx, List<Artist> artists, List<Album> albums, List<scanner.Song> songs) {
        Map<String, Object> searchResultPayload = new LinkedHashMap<>();
        searchResultPayload.put("artist", artists.stream().map(SubsonicResponses::toArtistPayload).toList());
        searchResultPayload.put("album", albums.stream().map(SubsonicResponses::toAlbumPayload).toList());
        searchResultPayload.put("song", songs.stream().map(SubsonicResponses::toSongPayload).toList());

        Map<String, Object> payload = baseResponse("ok");
        payload.put("searchResult3", searchResultPayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<searchResult3>");
        for (Artist artist : artists) {
            appendArtistElement(xml, artist);
        }
        for (Album album : albums) {
            appendAlbumElement(xml, album);
        }
        for (scanner.Song song : songs) {
            appendSongElement(xml, song);
        }
        xml.append("</searchResult3>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeArtistInfo2(Context ctx) {
        Map<String, Object> artistInfoPayload = new LinkedHashMap<>();
        artistInfoPayload.put("biography", "");
        artistInfoPayload.put("musicBrainzId", "");
        artistInfoPayload.put("lastFmUrl", "");
        artistInfoPayload.put("smallImageUrl", "");
        artistInfoPayload.put("mediumImageUrl", "");
        artistInfoPayload.put("largeImageUrl", "");
        artistInfoPayload.put("similarArtist", List.of());

        Map<String, Object> payload = baseResponse("ok");
        payload.put("artistInfo2", artistInfoPayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<artistInfo2");
        xml.append(" biography=\"\"");
        xml.append(" musicBrainzId=\"\"");
        xml.append(" lastFmUrl=\"\"");
        xml.append(" smallImageUrl=\"\"");
        xml.append(" mediumImageUrl=\"\"");
        xml.append(" largeImageUrl=\"\"");
        xml.append(">");
        xml.append("</artistInfo2>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeAlbumInfo2(Context ctx) {
        Map<String, Object> albumInfoPayload = new LinkedHashMap<>();
        albumInfoPayload.put("notes", "");
        albumInfoPayload.put("musicBrainzId", "");
        albumInfoPayload.put("lastFmUrl", "");
        albumInfoPayload.put("smallImageUrl", "");
        albumInfoPayload.put("mediumImageUrl", "");
        albumInfoPayload.put("largeImageUrl", "");

        Map<String, Object> payload = baseResponse("ok");
        payload.put("albumInfo2", albumInfoPayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<albumInfo2");
        xml.append(" notes=\"\"");
        xml.append(" musicBrainzId=\"\"");
        xml.append(" lastFmUrl=\"\"");
        xml.append(" smallImageUrl=\"\"");
        xml.append(" mediumImageUrl=\"\"");
        xml.append(" largeImageUrl=\"\"");
        xml.append(">");
        xml.append("</albumInfo2>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeGenres(Context ctx, List<Map<String, Object>> genres) {
        Map<String, Object> payload = baseResponse("ok");
        payload.put("genres", Map.of("genre", genres));

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<genres>");
        for (Map<String, Object> genre : genres) {
            xml.append("<genre");
            appendXmlAttribute(xml, "songCount", String.valueOf(genre.getOrDefault("songCount", "")));
            appendXmlAttribute(xml, "albumCount", String.valueOf(genre.getOrDefault("albumCount", "")));
            xml.append(">");
            xml.append(escapeXml(String.valueOf(genre.getOrDefault("value", ""))));
            xml.append("</genre>");
        }
        xml.append("</genres>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeSongsByGenre(Context ctx, List<scanner.Song> songs) {
        Map<String, Object> songsByGenrePayload = new LinkedHashMap<>();
        songsByGenrePayload.put("song", songs.stream().map(SubsonicResponses::toSongPayload).toList());

        Map<String, Object> payload = baseResponse("ok");
        payload.put("songsByGenre", songsByGenrePayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<songsByGenre>");
        for (scanner.Song song : songs) {
            appendSongElement(xml, song);
        }
        xml.append("</songsByGenre>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeRandomSongs(Context ctx, List<scanner.Song> songs) {
        Map<String, Object> randomSongsPayload = new LinkedHashMap<>();
        randomSongsPayload.put("song", songs.stream().map(SubsonicResponses::toSongPayload).toList());

        Map<String, Object> payload = baseResponse("ok");
        payload.put("randomSongs", randomSongsPayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<randomSongs>");
        for (scanner.Song song : songs) {
            appendSongElement(xml, song);
        }
        xml.append("</randomSongs>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writePlaylists(Context ctx, List<PlaylistInfo> playlists) {
        Map<String, Object> payload = baseResponse("ok");
        payload.put("playlists", Map.of("playlist", playlists.stream().map(SubsonicResponses::toPlaylistPayload).toList()));

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<playlists>");
        for (PlaylistInfo playlist : playlists) {
            appendPlaylistElement(xml, playlist, false, List.of());
        }
        xml.append("</playlists>");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeStarred(Context ctx, List<scanner.Song> songs) {
        writeStarredResponse(ctx, songs, "starred");
    }

    public static void writeStarred2(Context ctx, List<scanner.Song> songs) {
        writeStarredResponse(ctx, songs, "starred2");
    }

    private static void writeStarredResponse(Context ctx, List<scanner.Song> songs, String responseName) {
        Map<String, Object> starredPayload = new LinkedHashMap<>();
        starredPayload.put("song", songs.stream().map(SubsonicResponses::toSongPayload).toList());

        Map<String, Object> payload = baseResponse("ok");
        payload.put(responseName, starredPayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        xml.append("<").append(responseName).append(">");
        for (scanner.Song song : songs) {
            appendSongElement(xml, song);
        }
        xml.append("</").append(responseName).append(">");
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writePlaylist(Context ctx, PlaylistInfo playlist, List<scanner.Song> songs) {
        Map<String, Object> playlistPayload = toPlaylistPayload(playlist);
        playlistPayload.put("entry", songs.stream().map(SubsonicResponses::toSongPayload).toList());

        Map<String, Object> payload = baseResponse("ok");
        payload.put("playlist", playlistPayload);

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        appendPlaylistElement(xml, playlist, true, songs);
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeSong(Context ctx, scanner.Song song) {
        Map<String, Object> payload = baseResponse("ok");
        payload.put("song", toSongPayload(song));

        String format = requestedFormat(ctx);
        if ("json".equalsIgnoreCase(format)) {
            ctx.status(200);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(200);
        applyUtf8ContentType(ctx, "application/xml");
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));
        xml.append(">");
        appendSongElement(xml, song);
        xml.append("</subsonic-response>");
        ctx.result(xml.toString());
    }

    public static void writeError(Context ctx, int httpStatus, int errorCode, String message) {
        Map<String, Object> payload = baseResponse("failed");
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", errorCode);
        error.put("message", message);
        payload.put("error", error);
        writeResponse(ctx, httpStatus, payload, error);
    }

    private static Map<String, Object> baseResponse(String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("version", API_VERSION);
        payload.put("type", SERVER_TYPE);
        payload.put("serverVersion", SERVER_VERSION);
        payload.put("openSubsonic", true);
        return payload;
    }

    private static void writeResponse(Context ctx, int httpStatus, Map<String, Object> payload, Map<String, Object> error) {
        String format = requestedFormat(ctx);

        if ("json".equalsIgnoreCase(format)) {
            ctx.status(httpStatus);
            applyUtf8ContentType(ctx, "application/json");
            ctx.result(GSON.toJson(Map.of("subsonic-response", payload)));
            return;
        }

        ctx.status(httpStatus);
        applyUtf8ContentType(ctx, "application/xml");
        ctx.result(toXml(payload, error));
    }

    private static String requestedFormat(Context ctx) {
        String format = SubsonicRequest.param(ctx, "f");
        if (format == null || format.isBlank()) {
            return "xml";
        }

        return format;
    }

    private static void applyUtf8ContentType(Context ctx, String mimeType) {
        ctx.contentType(mimeType + "; charset=UTF-8");
        ctx.res().setCharacterEncoding("UTF-8");
    }

    private static String toXml(Map<String, Object> payload, Map<String, Object> error) {
        StringBuilder xml = new StringBuilder();
        xml.append(xmlDeclaration());
        xml.append(openRoot(payload));

        if (error == null) {
            xml.append("/>");
            return xml.toString();
        }

        xml.append(">");
        xml.append("<error code=\"").append(error.get("code")).append("\"");
        xml.append(" message=\"").append(escapeXml(String.valueOf(error.get("message")))).append("\"/>");
        xml.append("</subsonic-response>");
        return xml.toString();
    }

    private static String xmlDeclaration() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    }

    private static String openRoot(Map<String, Object> payload) {
        StringBuilder xml = new StringBuilder();
        xml.append("<subsonic-response xmlns=\"http://subsonic.org/restapi\"");
        xml.append(" status=\"").append(escapeXml(String.valueOf(payload.get("status")))).append("\"");
        xml.append(" version=\"").append(escapeXml(String.valueOf(payload.get("version")))).append("\"");
        xml.append(" type=\"").append(escapeXml(String.valueOf(payload.get("type")))).append("\"");
        xml.append(" serverVersion=\"").append(escapeXml(String.valueOf(payload.get("serverVersion")))).append("\"");
        xml.append(" openSubsonic=\"").append(payload.get("openSubsonic")).append("\"");
        return xml.toString();
    }

    private static Map<String, Object> toArtistPayload(Artist artist) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id(artist.getId()));
        payload.put("name", sanitizeText(artist.getName()));
        payload.put("albumCount", artist.getAlbumCount());
        payload.put("songCount", artist.getSongCount());
        payload.put("coverArt", SubsonicArtistImageService.coverArtId(artist.getId()));
        return payload;
    }

    private static Map<String, Object> toAlbumPayload(Album album) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id(album.getId()));
        payload.put("parent", id(album.getArtistId()));
        payload.put("title", sanitizeText(album.getTitle()));
        payload.put("name", sanitizeText(album.getTitle()));
        payload.put("album", sanitizeText(album.getTitle()));
        payload.put("artist", sanitizeText(album.getArtist()));
        payload.put("artistId", id(album.getArtistId()));
        payload.put("isDir", true);
        payload.put("songCount", album.getSongCount());
        if (album.getArtworkSongId() > 0) {
            payload.put("coverArt", id(album.getArtworkSongId()));
        }
        return payload;
    }

    private static Map<String, Object> toPlaylistPayload(PlaylistInfo playlist) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", SubsonicIds.playlistId(playlist.getId()));
        payload.put("name", sanitizeText(playlist.getName()));
        putIfNotBlank(payload, "comment", sanitizeText(playlist.getComment()));
        payload.put("owner", sanitizeText(playlist.getOwner()));
        payload.put("public", playlist.isPublic());
        putIfNotBlank(payload, "created", playlist.getCreated());
        putIfNotBlank(payload, "changed", playlist.getChanged());
        payload.put("songCount", playlist.getSongCount());
        payload.put("duration", playlist.getDuration());
        payload.put("allowedUser", new ArrayList<>());
        payload.put("readonly", playlist.isReadonly());
        if (!sanitizeText(playlist.getCoverArt()).isEmpty()) {
            payload.put("coverArt", playlist.getCoverArt());
        }
        return payload;
    }

    private static Map<String, Object> toSongPayload(scanner.Song song) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String contentType = probeContentType(song.getFilePath());
        String suffix = fileSuffix(song.getFilePath());
        String fileName = fileName(song.getFilePath());
        payload.put("id", id(song.getId()));
        payload.put("parent", id(song.getAlbumId()));
        payload.put("isDir", false);
        payload.put("title", sanitizeText(song.getTitle()));
        payload.put("artist", sanitizeText(song.getArtist()));
        payload.put("album", sanitizeText(song.getAlbum()));
        putIfNotBlank(payload, "genre", sanitizeText(song.getGenre()));
        payload.put("albumId", id(song.getAlbumId()));
        payload.put("isVideo", false);
        payload.put("type", "music");
        payload.put("duration", song.getDuration());
        if (song.getFileSize() > 0) {
            payload.put("size", song.getFileSize());
        }
        putIfPositive(payload, "bitRate", song.getBitRate());
        putIfPositive(payload, "samplingRate", song.getSamplingRate());
        putIfPositive(payload, "channelCount", song.getChannelCount());
        putIfPositive(payload, "bitDepth", song.getBitDepth());
        putIfPositive(payload, "year", song.getYear());
        putIfPositive(payload, "track", song.getTrack());
        putIfPositive(payload, "discNumber", song.getDiscNumber());
        putIfNotBlank(payload, "contentType", contentType);
        putIfNotBlank(payload, "suffix", suffix);
        putIfNotBlank(payload, "path", fileName);
        putIfNotBlank(payload, "created", formatInstant(song.getLastModified()));
        if (!sanitizeText(song.getArtworkPath()).isEmpty()) {
            payload.put("coverArt", id(song.getId()));
        }
        return payload;
    }

    private static String id(int id) {
        return String.valueOf(id);
    }

    private static void appendAlbumElement(StringBuilder xml, Album album) {
        xml.append("<album");
        xml.append(" id=\"").append(album.getId()).append("\"");
        xml.append(" parent=\"").append(album.getArtistId()).append("\"");
        xml.append(" title=\"").append(escapeXml(album.getTitle())).append("\"");
        xml.append(" name=\"").append(escapeXml(album.getTitle())).append("\"");
        xml.append(" album=\"").append(escapeXml(album.getTitle())).append("\"");
        xml.append(" artist=\"").append(escapeXml(album.getArtist())).append("\"");
        xml.append(" artistId=\"").append(album.getArtistId()).append("\"");
        xml.append(" isDir=\"true\"");
        xml.append(" songCount=\"").append(album.getSongCount()).append("\"");
        if (album.getArtworkSongId() > 0) {
            xml.append(" coverArt=\"").append(album.getArtworkSongId()).append("\"");
        }
        xml.append("/>");
    }

    private static void appendArtistElement(StringBuilder xml, Artist artist) {
        xml.append("<artist");
        xml.append(" id=\"").append(artist.getId()).append("\"");
        xml.append(" name=\"").append(escapeXml(artist.getName())).append("\"");
        xml.append(" albumCount=\"").append(artist.getAlbumCount()).append("\"");
        xml.append(" songCount=\"").append(artist.getSongCount()).append("\"");
        xml.append(" coverArt=\"").append(escapeXml(SubsonicArtistImageService.coverArtId(artist.getId()))).append("\"");
        xml.append("/>");
    }

    private static void appendPlaylistElement(StringBuilder xml, PlaylistInfo playlist, boolean includeEntries, List<scanner.Song> songs) {
        xml.append("<playlist");
        xml.append(" id=\"").append(escapeXml(SubsonicIds.playlistId(playlist.getId()))).append("\"");
        xml.append(" name=\"").append(escapeXml(playlist.getName())).append("\"");
        appendXmlAttribute(xml, "comment", sanitizeText(playlist.getComment()));
        xml.append(" owner=\"").append(escapeXml(playlist.getOwner())).append("\"");
        xml.append(" public=\"").append(playlist.isPublic()).append("\"");
        appendXmlAttribute(xml, "created", playlist.getCreated());
        appendXmlAttribute(xml, "changed", playlist.getChanged());
        xml.append(" songCount=\"").append(playlist.getSongCount()).append("\"");
        xml.append(" duration=\"").append(playlist.getDuration()).append("\"");
        xml.append(" readonly=\"").append(playlist.isReadonly()).append("\"");
        if (!sanitizeText(playlist.getCoverArt()).isEmpty()) {
            xml.append(" coverArt=\"").append(escapeXml(playlist.getCoverArt())).append("\"");
        }
        if (!includeEntries) {
            xml.append("/>");
            return;
        }

        xml.append(">");
        for (scanner.Song song : songs) {
            appendSongElement(xml, song);
        }
        xml.append("</playlist>");
    }

    private static void appendSongElement(StringBuilder xml, scanner.Song song) {
        String contentType = probeContentType(song.getFilePath());
        String suffix = fileSuffix(song.getFilePath());
        String fileName = fileName(song.getFilePath());
        xml.append("<song");
        xml.append(" id=\"").append(song.getId()).append("\"");
        xml.append(" parent=\"").append(song.getAlbumId()).append("\"");
        xml.append(" isDir=\"false\"");
        xml.append(" title=\"").append(escapeXml(song.getTitle())).append("\"");
        xml.append(" artist=\"").append(escapeXml(song.getArtist())).append("\"");
        xml.append(" album=\"").append(escapeXml(song.getAlbum())).append("\"");
        appendXmlAttribute(xml, "genre", sanitizeText(song.getGenre()));
        xml.append(" albumId=\"").append(song.getAlbumId()).append("\"");
        xml.append(" isVideo=\"false\"");
        xml.append(" type=\"music\"");
        xml.append(" duration=\"").append(song.getDuration()).append("\"");
        if (song.getFileSize() > 0) {
            xml.append(" size=\"").append(song.getFileSize()).append("\"");
        }
        appendPositiveXmlAttribute(xml, "bitRate", song.getBitRate());
        appendPositiveXmlAttribute(xml, "samplingRate", song.getSamplingRate());
        appendPositiveXmlAttribute(xml, "channelCount", song.getChannelCount());
        appendPositiveXmlAttribute(xml, "bitDepth", song.getBitDepth());
        appendPositiveXmlAttribute(xml, "year", song.getYear());
        appendPositiveXmlAttribute(xml, "track", song.getTrack());
        appendPositiveXmlAttribute(xml, "discNumber", song.getDiscNumber());
        appendXmlAttribute(xml, "contentType", contentType);
        appendXmlAttribute(xml, "suffix", suffix);
        appendXmlAttribute(xml, "path", fileName);
        appendXmlAttribute(xml, "created", formatInstant(song.getLastModified()));
        if (!sanitizeText(song.getArtworkPath()).isEmpty()) {
            xml.append(" coverArt=\"").append(song.getId()).append("\"");
        }
        xml.append("/>");
    }

    private static void putIfNotBlank(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }

    private static void putIfPositive(Map<String, Object> payload, String key, Integer value) {
        if (value != null && value > 0) {
            payload.put(key, value);
        }
    }

    private static void appendPositiveXmlAttribute(StringBuilder xml, String key, Integer value) {
        if (value != null && value > 0) {
            xml.append(" ").append(key).append("=\"").append(value).append("\"");
        }
    }

    private static void appendXmlAttribute(StringBuilder xml, String name, String value) {
        if (value != null && !value.isBlank()) {
            xml.append(" ")
                    .append(name)
                    .append("=\"")
                    .append(escapeXml(value))
                    .append("\"");
        }
    }

    private static String probeContentType(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }

        try {
            String contentType = Files.probeContentType(Path.of(filePath));
            return sanitizeText(contentType);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String fileSuffix(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }

        String fileName = Path.of(filePath).getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }

        return sanitizeText(fileName.substring(extensionIndex + 1));
    }

    private static String fileName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }

        return sanitizeText(Path.of(filePath).getFileName().toString());
    }

    private static String formatInstant(long epochMillis) {
        if (epochMillis <= 0) {
            return "";
        }

        return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(epochMillis));
    }

    private static String escapeXml(String value) {
        return sanitizeText(value)
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&apos;");
    }

    private static String sanitizeText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i += 1) {
            char current = value.charAt(i);

            if (Character.isHighSurrogate(current)) {
                if (i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1))) {
                    int codePoint = Character.toCodePoint(current, value.charAt(i + 1));
                    if (isValidXmlCodePoint(codePoint)) {
                        sanitized.append(current).append(value.charAt(i + 1));
                    } else {
                        sanitized.append('\uFFFD');
                    }
                    i += 1;
                } else {
                    sanitized.append('\uFFFD');
                }
                continue;
            }

            if (Character.isLowSurrogate(current)) {
                sanitized.append('\uFFFD');
                continue;
            }

            if (isValidXmlCodePoint(current)) {
                sanitized.append(current);
            } else {
                sanitized.append('\uFFFD');
            }
        }

        return sanitized.toString();
    }

    private static boolean isValidXmlCodePoint(int codePoint) {
        return codePoint == 0x9
                || codePoint == 0xA
                || codePoint == 0xD
                || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
    }
}
