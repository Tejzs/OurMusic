package routes.subsonic;

public final class SubsonicIds {
    private static final String PLAYLIST_ID_PREFIX = "pl-";

    private SubsonicIds() {
    }

    public static String playlistId(int playlistId) {
        return PLAYLIST_ID_PREFIX + playlistId;
    }

    public static Integer parsePlaylistId(String rawId) {
        if (rawId == null) {
            return null;
        }

        String numericPart = rawId.startsWith(PLAYLIST_ID_PREFIX)
                ? rawId.substring(PLAYLIST_ID_PREFIX.length())
                : rawId;
        try {
            return Integer.valueOf(numericPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
