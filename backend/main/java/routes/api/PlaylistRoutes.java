package routes.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import config.Properties;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import postgresql.Database;
import scanner.Playlist;
import scanner.PlaylistRequest;
import scanner.Song;

public class PlaylistRoutes {
    public static void register(Javalin app) {
        app.post("/api/playlists", ctx -> {
            String token = ctx.cookie("ourmusic_session");
            PlaylistRequest req = ctx.bodyAsClass(PlaylistRequest.class);

            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            int playlistId = Database.createPlaylist(userId, req.getName());

            if (playlistId < 0) {
                ctx.status(500).json(Map.of("message", "failed to create playlist"));
                return;
            }

            ctx.status(201).json(Map.of("message", "success", "playlistId", playlistId));
        });

        app.get("/api/playlists", ctx -> {
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

            List<Playlist> playlists = Database.getPlaylists(userId);
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(playlists));
        });

        app.post("/api/playlists/{ID}/songs", ctx -> {
            String playlistId = ctx.pathParam("ID");
            String token = ctx.cookie("ourmusic_session");
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();

            int songId = body.get("songId").getAsInt();

            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            boolean ok = Database.verifyPlaylist(userId, Integer.valueOf(playlistId));

            if (!ok) {
                ctx.status(403).json(Map.of("message", "playlist not found or not yours"));
                return;
            }

            Database.insertSongsToPlaylist(Integer.valueOf(playlistId), songId);
            ctx.status(200).json(Map.of("message", "success"));
        });

        app.get("/api/playlists/{ID}/songs", ctx -> {
            String playlistId = ctx.pathParam("ID");
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

            boolean ok = Database.verifyPlaylist(userId, Integer.valueOf(playlistId));

            if (!ok) {
                ctx.status(403).json(Map.of("message", "playlist not found or not yours"));
                return;
            }

            List<Song> songs = Database.getSongsPlaylist(Integer.valueOf(playlistId));
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(songs));
        });

        app.delete("/api/playlists/{ID}/songs/{songID}", ctx -> {
            String playlistId = ctx.pathParam("ID");
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

            boolean ok = Database.verifyPlaylist(userId, Integer.valueOf(playlistId));

            if (!ok) {
                ctx.status(403).json(Map.of("message", "playlist not found or not yours"));
                return;
            }

            boolean isDeleted = Database.deleteSongFromPlaylist(Integer.valueOf(playlistId), Integer.valueOf(songId));

            if (!isDeleted) {
                ctx.status(404).json(Map.of("message", "song not found"));
                return;
            }
            ctx.status(200).json(Map.of("message", "success"));
        });

        app.delete("/api/playlists/{ID}", ctx -> {
            String playlistId = ctx.pathParam("ID");
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

            boolean ok = Database.verifyPlaylist(userId, Integer.valueOf(playlistId));

            if (!ok) {
                ctx.status(403).json(Map.of("message", "playlist not found or not yours"));
                return;
            }

            boolean isDeleted = Database.deletePlaylist(Integer.valueOf(playlistId), Integer.valueOf(userId));

            if (!isDeleted) {
                ctx.status(404).json(Map.of("message", "playlist not found"));
                return;
            }
            ctx.status(200).json(Map.of("message", "success"));
        });

        app.patch("/api/playlists/{playlistID}/songs/{songID}/position/{position}", ctx -> {
            String playlistId = ctx.pathParam("playlistID");
            String songId = ctx.pathParam("songID");
            String position = ctx.pathParam("position");
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

            boolean ok = Database.verifyPlaylist(userId, Integer.valueOf(playlistId));

            if (!ok) {
                ctx.status(403).json(Map.of("message", "playlist not found or not yours"));
                return;
            }

            Database.reorderSongsInPlaylist(Integer.valueOf(position), Integer.valueOf(playlistId),
                    Integer.valueOf(songId));
        });

        app.patch("/api/playlists/{playlistId}/cover", ctx -> {
            String playlistId = ctx.pathParam("playlistId");
            String token = ctx.cookie("ourmusic_session");
            UploadedFile file = ctx.uploadedFile("cover");

            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            boolean ok = Database.verifyPlaylist(userId, Integer.valueOf(playlistId));

            if (!ok) {
                ctx.status(403).json(Map.of("message", "playlist not found or not yours"));
                return;
            }

            if (file == null) {
                ctx.status(400).json(Map.of("message", "no file uploaded"));
                return;
            }

            try {
                Files.copy(file.content(), Path.of(Properties.getSongsArtworkFolder(), playlistId + ".jpg"),
                        StandardCopyOption.REPLACE_EXISTING);

                Database.setPlaylistCoverart(Integer.valueOf(playlistId),
                        Properties.getSongsArtworkFolder() + File.separator + playlistId + ".jpg");
            } catch (Exception e) {
                System.out.println("Playlist Cover Art Failed To Save");
                System.out.println(e.getMessage());
            }

            ctx.status(200).json(Map.of("message", "success"));
        });

        app.get("/api/playlists/{playlistId}/cover", ctx -> {
            String playlistId = ctx.pathParam("playlistId");
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

            boolean ok = Database.verifyPlaylist(userId, Integer.valueOf(playlistId));

            if (!ok) {
                ctx.status(403).json(Map.of("message", "playlist not found or not yours"));
                return;
            }

            String path = Database.getPlaylistCoverartPath(Integer.valueOf(playlistId));

            if (path == null) {
                ctx.status(404).result("File not found");
                return;
            }

            File file = new File(path);
            if (!file.exists()) {
                ctx.status(404).result("File not found");
                return;
            }

            ctx.contentType("image/jpg");
            ctx.result(new FileInputStream(file));
        });

        app.post("/api/playlists/{playlistId}/import/m3u8", ctx -> {
            String playlistId = ctx.pathParam("playlistId");
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

            boolean ok = Database.verifyPlaylist(userId, Integer.valueOf(playlistId));

            if (!ok) {
                ctx.status(403).json(Map.of("message", "playlist not found or not yours"));
                return;
            }

            UploadedFile uploadedFile = ctx.uploadedFile("file");

            if (uploadedFile == null) {
                ctx.status(400).json(Map.of("message", "no file uploaded"));
                return;
            }

            int total = 0;
            int imported = 0;

            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(uploadedFile.content()));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        total++;
                        int songId = Database.searchSongFromFilePath(line.substring(line.lastIndexOf("/") + 1));
                        if (songId != -1) {
                            Database.insertSongsToPlaylist(Integer.parseInt(playlistId), songId);         
                            imported++;                   
                        }
                    }
                }
    
                ctx.json(Map.of("imported", imported, "skipped", total - imported));

            } catch (Exception e) {
                System.out.println("Unable to read file");
            }
        });
    }
}
