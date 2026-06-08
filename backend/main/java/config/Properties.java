package config;

import java.io.FileInputStream;

public class Properties {
    private static String DB_URL;
    private static String DB_USERNAME;
    private static String DB_PASSWORD;

    private static String SONGS_FOLDER;
    private static String ARTWORK_FOLDER;

    private static String PORT;

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
}