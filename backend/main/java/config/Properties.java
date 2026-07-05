package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static String SESSION_COOKIE_SECURE;

    private static String REQUEST_LOGGING;
    
    private static final Logger LOG = LoggerFactory.getLogger(Properties.class);

    public static void loadConfigurations() {
        java.util.Properties props = new java.util.Properties();

        DB_URL = getConfig(props, "DB_URL", "db.url");
        DB_USERNAME = getConfig(props, "DB_USER", "db.user");
        DB_PASSWORD = getConfig(props, "DB_PASSWORD", "db.password");

        SONGS_FOLDER = getConfig(props, "SONGS_FOLDER", "songs.folder");
        ARTWORK_FOLDER = getConfig(props, "ARTWORK_FOLDER", "artwork.folder");

        PORT = getConfig(props, "APP_PORT", "app.port");

        ADMIN_USERNAME = getConfig(props, "ADMIN_USERNAME", "admin.username");
        ADMIN_PASSWORD = getConfig(props, "ADMIN_PASSWORD", "admin.password");

        FFMPEG_PATH = getConfig(props, "FFMPEG_PATH", "ffmpeg.path");
        SUBSONIC_AUTH_SECRET = getConfig(props, "SUBSONIC_AUTH_SECRET", "subsonic.auth.secret");

        CORS_ALLOWED_ORIGINS = getConfig(props, "CORS_ALLOWED_ORIGINS", "cors.allowed.origins");
        SESSION_COOKIE_SECURE = getConfig(props, "SESSION_COOKIE_SECURE", "session.cookie.secure");
        REQUEST_LOGGING = getConfig(props, "REQUEST_LOGGING_ENABLED", "request.logging.enabled");

        LOG.info("Properties loaded");
    }

    private static String getConfig(java.util.Properties props, String envKey, String propertyKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return props.getProperty(propertyKey);
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

    public static boolean isSessionCookieSecure() {
        return Boolean.parseBoolean(SESSION_COOKIE_SECURE);
    }

    public static boolean isRequestLoggingEnabled() {
        return Boolean.parseBoolean(REQUEST_LOGGING);
    }
}
