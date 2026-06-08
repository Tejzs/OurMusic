import io.javalin.Javalin;
import postgresql.Database;
import scanner.Album;
import scanner.Artist;
import scanner.Scanner;
import scanner.Song;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;

import auth.RegisterRequest;
import auth.UpdatePasswordRequest;
import auth.UpdateUsernameRequest;
import auth.User;
import config.Properties;

import org.mindrot.jbcrypt.BCrypt;

public class Server {
    public static void main(String[] args) throws Exception {
        Properties.loadConfigurations("backend/main/java/application.properties");
        Database.setup();
        Database.init();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.anyHost();
                });
            });
        }).start(Properties.getPort());

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

        app.get("/api/albums", ctx -> {
            String limit = ctx.queryParam("limit");
            String offset = ctx.queryParam("offset");

            List<Album> albums = Database.getAlbums(Integer.valueOf(limit), Integer.valueOf(offset));

            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(albums));
        });

        app.get("/api/albums/{ID}/songs", ctx -> {
            String ID = ctx.pathParam("ID");
            List<Song> songs = Database.getAlbumsSongs(Integer.valueOf(ID));
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(songs));
        });

        app.get("/api/artists", ctx -> {
            String limit = ctx.queryParam("limit");
            String offset = ctx.queryParam("offset");

            List<Artist> artists = Database.getArtists(Integer.valueOf(limit), Integer.valueOf(offset));

            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(artists));
        });

        app.get("/api/artists/{ID}/songs", ctx -> {
            String ID = ctx.pathParam("ID");
            List<Song> songs = Database.getArtistSongs(Integer.valueOf(ID));
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(songs));
        });

        app.get("/api/artists/{ID}/albums", ctx -> {
            String ID = ctx.pathParam("ID");
            List<Album> albums = Database.getArtistAlbums(Integer.valueOf(ID));
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(albums));
        });

        app.post("/api/auth/register", ctx -> {
            RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);
            String passwordHash = BCrypt.hashpw(req.getPassword(), BCrypt.gensalt());
            int resp = Database.registerUser(req.getUsername(), passwordHash);
            if (resp > 0) {
                ctx.status(201).json(Map.of("message", "success"));
                return;
            }

            if (resp == -2) {
                ctx.status(409).json(Map.of("message", "exists"));
                return;
            }

            ctx.status(500).json(Map.of("message", "failure"));
        });

        app.post("/api/auth/login", ctx -> {
            RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);

            User user = Database.getUserByName(req.getUsername());

            if (user == null) {
                ctx.status(401).json(Map.of("message", "invalid"));
                return;
            }

            boolean ok = BCrypt.checkpw(req.getPassword(), user.getPassword());

            if (!ok) {
                ctx.status(401).json(Map.of("message", "invalid"));
                return;
            }

            String token = UUID.randomUUID().toString();
            Database.createSessionToken(user.getId(), token);

            ctx.cookie("ourmusic_session", token);
            ctx.json(Map.of("message", "success"));
        });

        app.get("/api/auth/me", ctx -> {
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

            User user = Database.getUserFromId(userId);
            ctx.status(200).json(Map.of("id", userId, "username", user.getUsername(), "isAdmin", user.isAdmin()));
        });

        app.post("/api/auth/logout", ctx -> {
            String token = ctx.cookie("ourmusic_session");

            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            boolean ok = Database.clearSession(token);

            if (!ok) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            ctx.removeCookie("ourmusic_session");
            ctx.json(Map.of("message", "success"));
        });

        app.get("/api/admin/users", ctx -> {
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

            User user = Database.getUserFromId(userId);

            if (!user.isAdmin()) {
                ctx.status(403).json(Map.of("message", "user not an admin"));
                return;
            }

            List<User> users = Database.getAllUsers();
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(users));
        });

        app.delete("/api/admin/users/{ID}", ctx -> {
            String ID = ctx.pathParam("ID");
            String token = ctx.cookie("ourmusic_session");
            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == Integer.valueOf(ID)) {
                ctx.status(400).json(Map.of("message", "cannot delete yourself"));
                return;
            }

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            User user = Database.getUserFromId(userId);

            if (!user.isAdmin()) {
                ctx.status(403).json(Map.of("message", "user not an admin"));
                return;
            }

            boolean ok = Database.deleteUser(Integer.valueOf(ID));

            if (!ok) {
                ctx.status(404).json(Map.of("message", "user not found"));
                return;
            }

            ctx.status(200).json(Map.of("message", "success"));
        });

        app.patch("/api/admin/users/{ID}/username", ctx -> {
            String ID = ctx.pathParam("ID");
            UpdateUsernameRequest req = ctx.bodyAsClass(UpdateUsernameRequest.class);

            String token = ctx.cookie("ourmusic_session");
            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == Integer.valueOf(ID)) {
                ctx.status(400).json(Map.of("message", "cannot delete yourself"));
                return;
            }

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            User user = Database.getUserFromId(userId);

            if (!user.isAdmin()) {
                ctx.status(403).json(Map.of("message", "user not an admin"));
                return;
            }

            boolean ok = Database.changeUsername(Integer.valueOf(ID), req.getUsername());

            if (!ok) {
                ctx.status(500).json(Map.of("message", "update failed"));
                return;
            }

            ctx.status(200).json(Map.of("message", "success"));
        });

        app.patch("/api/admin/users/{ID}/password", ctx -> {
            String ID = ctx.pathParam("ID");
            UpdatePasswordRequest req = ctx.bodyAsClass(UpdatePasswordRequest.class);
            String passwordHash = BCrypt.hashpw(req.getPassword(), BCrypt.gensalt());

            String token = ctx.cookie("ourmusic_session");
            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == Integer.valueOf(ID)) {
                ctx.status(400).json(Map.of("message", "cannot delete yourself"));
                return;
            }

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            User user = Database.getUserFromId(userId);

            if (!user.isAdmin()) {
                ctx.status(403).json(Map.of("message", "user not an admin"));
                return;
            }

            boolean ok = Database.changePassword(Integer.valueOf(ID), passwordHash);

            if (!ok) {
                ctx.status(500).json(Map.of("message", "update failed"));
                return;
            }

            ctx.status(200).json(Map.of("message", "success"));
        });
    }
}
