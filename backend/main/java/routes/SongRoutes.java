package routes;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import io.javalin.Javalin;
import postgresql.Database;
import scanner.Scanner;
import scanner.Song;

public class SongRoutes {
    public static void register(Javalin app) {
        app.get("/api/songs", ctx -> {
            String pattern = ctx.queryParam("search");
            String limit = ctx.queryParam("limit");
            String offset = ctx.queryParam("offset");

            List<Song> songs;

            if (pattern != null && !pattern.isBlank()) {
                songs = Database.search(pattern);
            } else {
                songs = Database.scanSongs(Integer.valueOf(limit), Integer.valueOf(offset));
            }

            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(songs));
        });

        app.post("/api/library/scan/full", ctx -> {
            List<Song> songs = Scanner.scanLibrary();
            ctx.json(Map.of("status", "ok", "message", songs.size()));
        });

        app.get("/api/songs/{ID}/stream", ctx -> {
            String ID = ctx.pathParam("ID");
            Song song = Database.getSong(Integer.valueOf(ID));

            if (song == null) {
                ctx.status(404).result("Song not found");
            }

            File file = new File(song.getFilePath());
            if (!file.exists()) {
                ctx.status(404).result("File not found");
            }

            String range = ctx.header("Range");
            long fileLength = file.length();

            ctx.contentType("audio/flac");
            ctx.header("Accept-Ranges", "bytes");
            FileInputStream fileInputStream = new FileInputStream(file);

            if (range == null) {
                ctx.header("Content-Length", String.valueOf(fileLength));
                ctx.result(fileInputStream);
                return;
            }

            String bytes = range.replace("bytes=", "");
            String[] parts = bytes.split("-");

            long start = Long.parseLong(parts[0]);
            long end = fileLength - 1;

            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            }

            long contentLength = end - start + 1;

            ctx.status(206);
            ctx.header("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            ctx.header("Content-Length", String.valueOf(contentLength));

            fileInputStream.skip(start);
            ctx.result(fileInputStream);
        });

        app.get("/api/songs/{ID}/artwork", ctx -> {
            String ID = ctx.pathParam("ID");

            Song song = Database.getSong(Integer.valueOf(ID));

            if (song == null) {
                ctx.status(404).result("Song not found");
            }

            File file = new File(song.getArtworkPath());
            if (!file.exists()) {
                ctx.status(404).result("File not found");
            }

            ctx.contentType("image/jpg");
            ctx.result(new FileInputStream(file));
        });

        app.get("/api/songs/{songID}/lyrics", ctx -> {
            String songId = ctx.pathParam("songID");
            String token = ctx.cookie("ourmusic_session");

            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            Song song = Database.getSongInfo(Integer.valueOf(songId));

            if (song == null) {
                ctx.status(404).json(Map.of("message", "song not found"));
                return;
            }

            String url = "https://lrclib.net/api/get" + "?track_name="
                    + URLEncoder.encode(song.getTitle(), StandardCharsets.UTF_8) + "&artist_name="
                    + URLEncoder.encode(song.getArtist(), StandardCharsets.UTF_8) + "&album_name="
                    + URLEncoder.encode(song.getAlbum(), StandardCharsets.UTF_8) + "&duration=" + song.getDuration();

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();
            ctx.status(200).json(json);
        });

        app.get("/api/songs/{songID}/download", ctx -> {
            String songId = ctx.pathParam("songID");
            String token = ctx.cookie("ourmusic_session");

            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            Song song = Database.getSong(Integer.valueOf(songId));
            File file = new File(song.getFilePath());

            if (!file.exists()) {
                ctx.status(404).result("File not found");
            }

            ctx.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            ctx.result(new FileInputStream(file));
        });

    }
}
