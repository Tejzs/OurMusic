package config;

import java.io.FileInputStream;

public class Properties {
    private static String DB_URL;
    private static String DB_USERNAME;
    private static String DB_PASSWORD;

    private static String SONGS_FOLDER;
    private static String ARTWORK_FOLDER;

    private static String PORT;
    private static String SUBSONIC_AUTH_SECRET;
    private static String ADMIN_USERNAME;
    private static String ADMIN_PASSWORD;
    private static String FFMPEG_PATH;
    private static String CORS_ALLOWED_ORIGINS;

    public static void loadConfigurations(String path) {
        java.util.Properties props = new java.util.Properties();

        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        DB_URL = props.getProperty("db.url");
        DB_USERNAME = props.getProperty("db.user");
        DB_PASSWORD = props.getProperty("db.password");

        SONGS_FOLDER = props.getProperty("songs.folder");
        ARTWORK_FOLDER = props.getProperty("artwork.folder");

        PORT = props.getProperty("app.port");
        SUBSONIC_AUTH_SECRET = props.getProperty("subsonic.auth.secret");
        ADMIN_USERNAME = props.getProperty("admin.username");
        ADMIN_PASSWORD = props.getProperty("admin.password");
        FFMPEG_PATH = props.getProperty("ffmpeg.path", "ffmpeg");
        CORS_ALLOWED_ORIGINS = props.getProperty("cors.allowed.origins");

        System.out.println("Properties Loaded");
    }

    public static String getDBURL() {
        return DB_URL;
    }

    public static String getDBUusername() {
        return DB_USERNAME;
    }

    public static String getDBPassword() {
        return DB_PASSWORD;
    }

    public static String getSongsFolder() {
        return SONGS_FOLDER;
    }

    public static String getSongsArtworkFolder() {
        return ARTWORK_FOLDER;
    }

    public static int getPort() {
        return Integer.valueOf(PORT);
    }

    public static String getSubsonicAuthSecret() {
        return SUBSONIC_AUTH_SECRET;
    }

    public static String getAdminUsername() {
        return ADMIN_USERNAME;
    }

    public static String getAdminPassword() {
        return ADMIN_PASSWORD;
    }

    public static String getFfmpegPath() {
        return FFMPEG_PATH;
    }

    public static String[] getCorsAllowedOrigins() {
        return CORS_ALLOWED_ORIGINS.split("\\s*,\\s*");
    }
}
