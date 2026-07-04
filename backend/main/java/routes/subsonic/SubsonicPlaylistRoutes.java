package routes.subsonic;

import auth.User;
import io.javalin.Javalin;
import io.javalin.http.Context;
import postgresql.Database;
import scanner.PlaylistInfo;
import scanner.Song;

import java.util.ArrayList;
import java.util.List;

public final class SubsonicPlaylistRoutes {
    private SubsonicPlaylistRoutes() {
    }

    public static void register(Javalin app) {
        SubsonicRequest.register(app, "/rest/getPlaylists.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            User targetUser = resolveTargetUser(ctx, user);
            if (targetUser == null) {
                return;
            }

            List<PlaylistInfo> playlists = Database.getPlaylistInfos(targetUser.getId(), user.isAdmin());
            SubsonicResponses.writePlaylists(ctx, playlists);
        });

        SubsonicRequest.register(app, "/rest/getPlaylist.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            Integer playlistId = parseRequiredId(ctx, "id");
            if (playlistId == null) {
                return;
            }

            PlaylistInfo playlist = Database.getPlaylistInfo(playlistId, user.getId(), user.isAdmin());
            if (playlist == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "Playlist not found.");
                return;
            }

            List<Song> songs = Database.getSongsPlaylist(playlistId);
            SubsonicResponses.writePlaylist(ctx, playlist, songs);
        });

        SubsonicRequest.register(app, "/rest/createPlaylist.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            String playlistIdParam = SubsonicRequest.param(ctx, "playlistId");
            String name = SubsonicRequest.param(ctx, "name");
            String comment = defaultString(SubsonicRequest.param(ctx, "comment"));
            Boolean isPublic = parseOptionalBoolean(SubsonicRequest.param(ctx, "public"));
            List<Integer> songIds = parseSongIds(ctx);
            boolean hasSongIdParam = hasParam(ctx, "songId");

            if (isBlank(playlistIdParam)) {
                if (isBlank(name)) {
                    SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
                    return;
                }

                int playlistId = Database.createPlaylist(user.getId(), name, comment, isPublic != null && isPublic);
                if (playlistId < 0) {
                    SubsonicResponses.writeError(ctx, 500, 0, "Failed to create playlist.");
                    return;
                }

                if (!songIds.isEmpty()) {
                    Database.replacePlaylistSongs(playlistId, songIds);
                }

                PlaylistInfo playlist = Database.getPlaylistInfo(playlistId, user.getId(), user.isAdmin());
                if (playlist == null) {
                    SubsonicResponses.writeError(ctx, 500, 0, "Failed to load playlist.");
                    return;
                }

                SubsonicResponses.writePlaylist(ctx, playlist, Database.getSongsPlaylist(playlistId));
                return;
            }

            Integer playlistId = SubsonicIds.parsePlaylistId(playlistIdParam);
            if (playlistId == null) {
                SubsonicResponses.writeError(ctx, 400, 10, "Invalid numeric parameter.");
                return;
            }

            if (!Database.verifyPlaylist(user.getId(), playlistId)) {
                SubsonicResponses.writeError(ctx, 403, 50, "Not authorized to modify this playlist.");
                return;
            }

            if (!isBlank(name) || SubsonicRequest.param(ctx, "comment") != null || isPublic != null) {
                boolean updated = Database.updatePlaylistMetadata(
                        playlistId,
                        user.getId(),
                        isBlank(name) ? null : name,
                        SubsonicRequest.param(ctx, "comment") != null ? comment : null,
                        isPublic
                );
                if (!updated) {
                    SubsonicResponses.writeError(ctx, 500, 0, "Failed to update playlist metadata.");
                    return;
                }
            }

            if (hasSongIdParam) {
                Database.replacePlaylistSongs(playlistId, songIds);
            }

            PlaylistInfo playlist = Database.getPlaylistInfo(playlistId, user.getId(), user.isAdmin());
            if (playlist == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "Playlist not found.");
                return;
            }

            SubsonicResponses.writePlaylist(ctx, playlist, Database.getSongsPlaylist(playlistId));
        });

        SubsonicRequest.register(app, "/rest/updatePlaylist.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            Integer playlistId = parseRequiredId(ctx, "playlistId");
            if (playlistId == null) {
                return;
            }

            if (!Database.verifyPlaylist(user.getId(), playlistId)) {
                SubsonicResponses.writeError(ctx, 403, 50, "Not authorized to modify this playlist.");
                return;
            }

            String name = SubsonicRequest.param(ctx, "name");
            String commentParam = SubsonicRequest.param(ctx, "comment");
            String publicParam = SubsonicRequest.param(ctx, "public");
            Boolean isPublic = parseOptionalBoolean(publicParam);

            if (!isBlank(name) || commentParam != null || publicParam != null) {
                boolean updated = Database.updatePlaylistMetadata(
                        playlistId,
                        user.getId(),
                        isBlank(name) ? null : name,
                        commentParam != null ? defaultString(commentParam) : null,
                        isPublic
                );
                if (!updated) {
                    SubsonicResponses.writeError(ctx, 500, 0, "Failed to update playlist metadata.");
                    return;
                }
            }

            for (Integer songId : parseSongIdsForKey(ctx, "songIdToAdd")) {
                Database.insertSongsToPlaylist(playlistId, songId);
            }

            for (Integer songIndex : parseSongIdsForKey(ctx, "songIndexToRemove")) {
                Database.deleteSongFromPlaylistByIndex(playlistId, songIndex);
            }

            PlaylistInfo playlist = Database.getPlaylistInfo(playlistId, user.getId(), user.isAdmin());
            if (playlist == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "Playlist not found.");
                return;
            }

            SubsonicResponses.writePlaylist(ctx, playlist, Database.getSongsPlaylist(playlistId));
        });

        SubsonicRequest.register(app, "/rest/deletePlaylist.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            Integer playlistId = parseRequiredId(ctx, "id");
            if (playlistId == null) {
                return;
            }

            if (!Database.verifyPlaylist(user.getId(), playlistId)) {
                SubsonicResponses.writeError(ctx, 403, 50, "Not authorized to delete this playlist.");
                return;
            }

            boolean deleted = Database.deletePlaylist(playlistId, user.getId());
            if (!deleted) {
                SubsonicResponses.writeError(ctx, 404, 70, "Playlist not found.");
                return;
            }

            SubsonicResponses.writeSuccess(ctx);
        });
    }

    private static User resolveTargetUser(Context ctx, User authenticatedUser) {
        String username = SubsonicRequest.param(ctx, "username");
        if (isBlank(username) || username.equals(authenticatedUser.getUsername())) {
            return authenticatedUser;
        }

        if (!authenticatedUser.isAdmin()) {
            SubsonicResponses.writeError(ctx, 403, 50, "Not authorized to view this user's playlists.");
            return null;
        }

        User targetUser = Database.getUserByName(username);
        if (targetUser == null) {
            SubsonicResponses.writeError(ctx, 404, 70, "User not found.");
            return null;
        }

        return targetUser;
    }

    private static Integer parseRequiredId(Context ctx, String name) {
        String rawValue = SubsonicRequest.param(ctx, name);
        if (isBlank(rawValue)) {
            SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
            return null;
        }

        Integer value = SubsonicIds.parsePlaylistId(rawValue);
        if (value == null) {
            SubsonicResponses.writeError(ctx, 400, 10, "Invalid numeric parameter.");
            return null;
        }

        return value;
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<Integer> parseSongIds(Context ctx) {
        return parseSongIdsForKey(ctx, "songId");
    }

    private static List<Integer> parseSongIdsForKey(Context ctx, String key) {
        List<Integer> values = new ArrayList<>();
        for (String rawValue : ctx.queryParams(key)) {
            Integer parsed = parseInteger(rawValue);
            if (parsed != null) {
                values.add(parsed);
            }
        }
        for (String rawValue : ctx.formParams(key)) {
            Integer parsed = parseInteger(rawValue);
            if (parsed != null) {
                values.add(parsed);
            }
        }
        return values;
    }

    private static Boolean parseOptionalBoolean(String value) {
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return false;
        }
        return null;
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean hasParam(Context ctx, String key) {
        return !ctx.queryParams(key).isEmpty() || !ctx.formParams(key).isEmpty();
    }
}
