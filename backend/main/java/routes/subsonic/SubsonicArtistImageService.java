package routes.subsonic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import config.Properties;
import postgresql.Database;
import scanner.Artist;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SubsonicArtistImageService {
    private static final String ARTIST_COVER_ART_PREFIX = "ar-";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private SubsonicArtistImageService() {
    }

    public static String coverArtId(int artistId) {
        return ARTIST_COVER_ART_PREFIX + artistId;
    }

    public static Integer parseArtistId(String rawId) {
        if (rawId == null || !rawId.startsWith(ARTIST_COVER_ART_PREFIX)) {
            return null;
        }

        try {
            return Integer.valueOf(rawId.substring(ARTIST_COVER_ART_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static File resolveImageFile(int artistId) throws Exception {
        Artist artist = Database.getArtistById(artistId);
        if (artist == null) {
            return null;
        }

        File cachedFile = fileFromPath(artist.getImagePath());
        if (cachedFile != null) {
            return cachedFile;
        }

        String imageUrl = fetchArtistImageUrl(artist.getName());
        if (isBlank(imageUrl)) {
            return null;
        }

        File downloadedFile = downloadArtistImage(artistId, imageUrl);
        if (downloadedFile == null) {
            return null;
        }

        Database.updateArtistImagePath(artistId, downloadedFile.getAbsolutePath());
        artist.setImagePath(downloadedFile.getAbsolutePath());
        return downloadedFile;
    }

    private static String fetchArtistImageUrl(String artistName) throws Exception {
        return fetchDeezerArtistImageUrl(artistName);
    }

    private static String fetchDeezerArtistImageUrl(String artistName) throws Exception {
        if (isBlank(artistName)) {
            return null;
        }

        String url = "https://api.deezer.com/search/artist?q="
                + URLEncoder.encode(artistName, StandardCharsets.UTF_8);

        JsonObject json = fetchJson(url);
        if (json == null || !json.has("data") || !json.get("data").isJsonArray()) {
            return null;
        }

        JsonArray data = json.getAsJsonArray("data");
        String normalizedRequestedName = normalizeName(artistName);
        String fallbackUrl = null;
        for (JsonElement element : data) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject artistObject = element.getAsJsonObject();
            String candidateName = stringValue(artistObject, "name");
            String imageUrl = firstNonBlank(
                    stringValue(artistObject, "picture_xl"),
                    stringValue(artistObject, "picture_big"),
                    stringValue(artistObject, "picture_medium"),
                    stringValue(artistObject, "picture")
            );

            if (isBlank(imageUrl)) {
                continue;
            }

            if (fallbackUrl == null) {
                fallbackUrl = imageUrl;
            }

            if (normalizeName(candidateName).equals(normalizedRequestedName)) {
                return imageUrl;
            }
        }

        return fallbackUrl;
    }

    private static JsonObject fetchJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }

        JsonElement parsed = JsonParser.parseString(response.body());
        return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
    }

    private static File downloadArtistImage(int artistId, String imageUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .header("Accept", "image/*")
                .GET()
                .build();

        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length == 0) {
            return null;
        }

        Path artistDirectory = Path.of(Properties.getSongsArtworkFolder(), "artists");
        Files.createDirectories(artistDirectory);

        Path imagePath = artistDirectory.resolve(artistId + ".jpg");
        Files.write(imagePath, response.body());
        return imagePath.toFile();
    }

    private static File fileFromPath(String imagePath) {
        if (isBlank(imagePath)) {
            return null;
        }

        File file = new File(imagePath);
        return file.exists() && file.isFile() && file.length() > 0 ? file : null;
    }

    private static String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String stringValue(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsString();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
