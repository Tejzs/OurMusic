package routes;

import auth.User;
import com.google.gson.Gson;
import io.javalin.Javalin;
import postgresql.Database;
import scanner.MostPlayedSong;
import scanner.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserRoutes {
    public static void register(Javalin app) {
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

        app.post("/api/me/liked-songs/{songID}", ctx -> {
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

            boolean isLiked = Database.likeSong(userId, Integer.valueOf(songId));

            if (!isLiked) {
                ctx.status(500).json(Map.of("message", "database error"));
                return;
            }
            ctx.status(200).json(Map.of("message", "success"));
        });

        app.delete("/api/me/liked-songs/{songID}", ctx -> {
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

            boolean isUnliked = Database.unlikeSong(userId, Integer.valueOf(songId));

            if (!isUnliked) {
                ctx.status(500).json(Map.of("message", "database error"));
                return;
            }
            ctx.status(200).json(Map.of("message", "success"));
        });

        app.get("/api/me/liked-songs", ctx -> {
            List<Song> songs = new ArrayList<>();
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

            songs = Database.getAllLikedSongs(userId);
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(songs));
        });

        app.post("/api/me/recently-played/{songID}", ctx -> {
            String songId = ctx.pathParam("songID");
            String token = ctx.cookie("ourmusic_session");

            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);
            Database.addToRecentlyPlayer(userId, Integer.valueOf(songId));
        });

        app.get("/api/me/recently-played", ctx -> {
            List<Song> songs = new ArrayList<>();
            String offset = ctx.queryParam("offset");
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

            songs = Database.getAllRecentlyPlayedSongs(userId, 50, Integer.valueOf(offset));
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(songs));
        });

        app.get("/api/me/most-played-songs", ctx -> {
            List<MostPlayedSong> songs = new ArrayList<>();
            String offset = ctx.queryParam("offset");
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

            songs = Database.getAllMostPlayedSongs(userId, 50, Integer.valueOf(offset));
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(songs));
        });

        app.get("/api/stats", ctx -> {
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

            Map<String, Integer> map = Database.getStats(userId);
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(map));
        });

    }
}