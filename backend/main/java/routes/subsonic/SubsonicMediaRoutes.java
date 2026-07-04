package routes.subsonic;

import auth.User;
import io.javalin.Javalin;
import postgresql.Database;
import scanner.Album;
import scanner.Artist;
import scanner.Song;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SubsonicMediaRoutes {
    private static final String PLAYLIST_COVER_ART_PREFIX = "playlist:";

    private SubsonicMediaRoutes() {
    }

    public static void register(Javalin app) {
        SubsonicRequest.register(app, "/rest/getArtist.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            Integer artistId = parseRequiredId(ctx, "id");
            if (artistId == null) {
                return;
            }

            Artist artist = Database.getArtistById(artistId);
            if (artist == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "Artist not found.");
                return;
            }

            List<Album> albums = Database.getArtistAlbums(artistId);
            SubsonicResponses.writeArtist(ctx, artist, albums);
        });

        SubsonicRequest.register(app, "/rest/getArtistInfo2.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            Integer requestedId = parseRequiredId(ctx, "id");
            if (requestedId == null) {
                return;
            }

            Artist artist = resolveArtistInfoTarget(requestedId);
            if (artist == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "Artist not found.");
                return;
            }

            SubsonicResponses.writeArtistInfo2(ctx);
        });

        SubsonicRequest.register(app, "/rest/getAlbum.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            Integer albumId = parseRequiredId(ctx, "id");
            if (albumId == null) {
                return;
            }

            Album album = Database.getAlbumById(albumId);
            if (album == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "Album not found.");
                return;
            }

            List<Song> songs = Database.getAlbumsSongs(albumId);
            SubsonicResponses.writeAlbum(ctx, album, songs);
        });

        SubsonicRequest.register(app, "/rest/getAlbumInfo2.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            Integer requestedId = parseRequiredId(ctx, "id");
            if (requestedId == null) {
                return;
            }

            Album album = resolveAlbumInfoTarget(requestedId);
            if (album == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "Album not found.");
                return;
            }

            SubsonicResponses.writeAlbumInfo2(ctx);
        });

        SubsonicRequest.register(app, "/rest/getSong.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            Integer songId = parseRequiredId(ctx, "id");
            if (songId == null) {
                return;
            }

            Song song = Database.getSong(songId);
            if (song == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "Song not found.");
                return;
            }

            SubsonicResponses.writeSong(ctx, song);
        });

        SubsonicRequest.register(app, "/rest/stream.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            Integer songId = parseRequiredId(ctx, "id");
            if (songId == null) {
                return;
            }

            Song song = Database.getSong(songId);
            if (song == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "Song not found.");
                return;
            }

            File file = new File(song.getFilePath());
            if (!file.exists()) {
                SubsonicResponses.writeError(ctx, 404, 70, "Media file not found.");
                return;
            }

            try {
                SubsonicTranscoder.TranscodeSession transcodeSession = SubsonicTranscoder.startIfRequested(ctx, song);
                if (transcodeSession != null) {
                    ctx.contentType(transcodeSession.contentType());
                    ctx.header("Content-Disposition", "inline; filename=\"" + escapeHeaderValue(transcodeSession.outputFileName()) + "\"");
                    ctx.result(transcodeSession.stream());
                    return;
                }
            } catch (IllegalArgumentException e) {
                SubsonicResponses.writeError(ctx, 400, 10, e.getMessage());
                return;
            } catch (Exception e) {
                SubsonicResponses.writeError(ctx, 500, 0, "Unable to start transcoding.");
                return;
            }

            String mimeType = probeMimeType(file.toPath());
            String range = ctx.header("Range");
            long fileLength = file.length();

            ctx.contentType(mimeType);
            ctx.header("Accept-Ranges", "bytes");
            ctx.header("Content-Disposition", "inline; filename=\"" + escapeHeaderValue(file.getName()) + "\"");

            if (isBlank(range)) {
                ctx.header("Content-Length", String.valueOf(fileLength));
                try {
                    ctx.result(new FileInputStream(file));
                } catch (IOException e) {
                    SubsonicResponses.writeError(ctx, 500, 0, "Unable to open media file.");
                }
                return;
            }

            ByteRange byteRange = parseRange(range, fileLength);
            if (byteRange == null) {
                ctx.status(416);
                ctx.header("Content-Range", "bytes */" + fileLength);
                return;
            }

            long contentLength = byteRange.end() - byteRange.start() + 1;
            ctx.status(206);
            ctx.header("Content-Range", "bytes " + byteRange.start() + "-" + byteRange.end() + "/" + fileLength);
            ctx.header("Content-Length", String.valueOf(contentLength));

            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                fileInputStream.getChannel().position(byteRange.start());
                ctx.result(new LimitedInputStream(fileInputStream, contentLength));
            } catch (IOException e) {
                SubsonicResponses.writeError(ctx, 500, 0, "Unable to read requested media range.");
            }
        });

        SubsonicRequest.register(app, "/rest/getCoverArt.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            String rawRequestedId = SubsonicRequest.param(ctx, "id");
            if (isBlank(rawRequestedId)) {
                SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
                return;
            }

            Integer artistId = SubsonicArtistImageService.parseArtistId(rawRequestedId);
            if (artistId != null) {
                try {
                    File artistImageFile = SubsonicArtistImageService.resolveImageFile(artistId);
                    if (artistImageFile == null) {
                        SubsonicResponses.writeError(ctx, 404, 70, "Cover art not found.");
                        return;
                    }

                    ctx.contentType(probeMimeType(artistImageFile.toPath()));
                    ctx.header("Content-Length", String.valueOf(artistImageFile.length()));
                    ctx.result(new FileInputStream(artistImageFile));
                    return;
                } catch (IOException e) {
                    SubsonicResponses.writeError(ctx, 500, 0, "Unable to open cover art file.");
                    return;
                } catch (Exception e) {
                    SubsonicResponses.writeError(ctx, 500, 0, "Unable to resolve artist cover art.");
                    return;
                }
            }

            Integer playlistId = parsePlaylistCoverArtId(rawRequestedId);
            if (playlistId != null) {
                String playlistCoverArtPath = Database.getPlaylistCoverartPath(playlistId);
                if (isBlank(playlistCoverArtPath)) {
                    SubsonicResponses.writeError(ctx, 404, 70, "Cover art not found.");
                    return;
                }

                File file = new File(playlistCoverArtPath);
                if (!file.exists()) {
                    SubsonicResponses.writeError(ctx, 404, 70, "Cover art file not found.");
                    return;
                }

                ctx.contentType(probeMimeType(file.toPath()));
                ctx.header("Content-Length", String.valueOf(file.length()));
                try {
                    ctx.result(new FileInputStream(file));
                } catch (IOException e) {
                    SubsonicResponses.writeError(ctx, 500, 0, "Unable to open cover art file.");
                }
                return;
            }

            if (SubsonicIds.parsePlaylistId(rawRequestedId) != null && !rawRequestedId.matches("\\d+")) {
                SubsonicResponses.writeError(ctx, 404, 70, "Cover art not found.");
                return;
            }

            Integer requestedId = parseNumericId(rawRequestedId);
            if (requestedId == null) {
                SubsonicResponses.writeError(ctx, 400, 10, "Invalid numeric parameter.");
                return;
            }

            Song artworkSong = Database.getSong(requestedId);
            if (artworkSong == null) {
                Album album = Database.getAlbumById(requestedId);
                if (album != null && album.getArtworkSongId() > 0) {
                    artworkSong = Database.getSong(album.getArtworkSongId());
                }
            }

            if (artworkSong == null || isBlank(artworkSong.getArtworkPath())) {
                SubsonicResponses.writeError(ctx, 404, 70, "Cover art not found.");
                return;
            }

            File file = new File(artworkSong.getArtworkPath());
            if (!file.exists()) {
                SubsonicResponses.writeError(ctx, 404, 70, "Cover art file not found.");
                return;
            }

            ctx.contentType(probeMimeType(file.toPath()));
            ctx.header("Content-Length", String.valueOf(file.length()));
            try {
                ctx.result(new FileInputStream(file));
            } catch (IOException e) {
                SubsonicResponses.writeError(ctx, 500, 0, "Unable to open cover art file.");
            }
        });
    }

    private static Integer parseNumericId(String rawValue) {
        try {
            return Integer.valueOf(rawValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parsePlaylistCoverArtId(String rawValue) {
        if (isBlank(rawValue) || !rawValue.startsWith(PLAYLIST_COVER_ART_PREFIX)) {
            return null;
        }

        return parseNumericId(rawValue.substring(PLAYLIST_COVER_ART_PREFIX.length()));
    }

    private static Integer parseRequiredId(io.javalin.http.Context ctx, String name) {
        String rawValue = SubsonicRequest.param(ctx, name);
        if (isBlank(rawValue)) {
            SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
            return null;
        }

        try {
            return Integer.valueOf(rawValue);
        } catch (NumberFormatException e) {
            SubsonicResponses.writeError(ctx, 400, 10, "Invalid numeric parameter.");
            return null;
        }
    }

    private static String probeMimeType(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            if (mimeType != null && !mimeType.isBlank()) {
                return mimeType;
            }
        } catch (Exception ignored) {
        }

        return "application/octet-stream";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Artist resolveArtistInfoTarget(int requestedId) {
        Artist artist = Database.getArtistById(requestedId);
        if (artist != null) {
            return artist;
        }

        Album album = Database.getAlbumById(requestedId);
        if (album != null) {
            return Database.getArtistById(album.getArtistId());
        }

        Song song = Database.getSong(requestedId);
        if (song == null) {
            return null;
        }

        Album songAlbum = Database.getAlbumById(song.getAlbumId());
        if (songAlbum == null) {
            return null;
        }

        return Database.getArtistById(songAlbum.getArtistId());
    }

    private static Album resolveAlbumInfoTarget(int requestedId) {
        Album album = Database.getAlbumById(requestedId);
        if (album != null) {
            return album;
        }

        Song song = Database.getSong(requestedId);
        if (song == null) {
            return null;
        }

        return Database.getAlbumById(song.getAlbumId());
    }

    private static ByteRange parseRange(String rawRange, long fileLength) {
        if (fileLength <= 0 || rawRange == null) {
            return null;
        }

        String normalizedRange = rawRange.trim();
        if (!normalizedRange.startsWith("bytes=")) {
            return null;
        }

        String[] ranges = normalizedRange.substring("bytes=".length()).split(",");
        if (ranges.length != 1) {
            return null;
        }

        String[] parts = ranges[0].trim().split("-", -1);
        if (parts.length != 2) {
            return null;
        }

        try {
            if (parts[0].isBlank()) {
                long suffixLength = Long.parseLong(parts[1]);
                if (suffixLength <= 0) {
                    return null;
                }

                long contentLength = Math.min(suffixLength, fileLength);
                return new ByteRange(fileLength - contentLength, fileLength - 1);
            }

            long start = Long.parseLong(parts[0]);
            long end = parts[1].isBlank() ? fileLength - 1 : Long.parseLong(parts[1]);

            if (start < 0 || start >= fileLength || end < start) {
                return null;
            }

            end = Math.min(end, fileLength - 1);
            return new ByteRange(start, end);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String escapeHeaderValue(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "_").replace("\"", "_");
    }

    private record ByteRange(long start, long end) {
    }

    private static final class LimitedInputStream extends FilterInputStream {
        private long remaining;

        private LimitedInputStream(InputStream inputStream, long remaining) {
            super(inputStream);
            this.remaining = remaining;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }

            int result = super.read();
            if (result != -1) {
                remaining -= 1;
            }
            return result;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0) {
                return -1;
            }

            int cappedLength = (int) Math.min(length, remaining);
            int bytesRead = super.read(buffer, offset, cappedLength);
            if (bytesRead != -1) {
                remaining -= bytesRead;
            }
            return bytesRead;
        }
    }
}
