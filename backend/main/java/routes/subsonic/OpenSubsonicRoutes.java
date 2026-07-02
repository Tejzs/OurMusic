package routes.subsonic;

import io.javalin.Javalin;

public class OpenSubsonicRoutes {
    public static void register(Javalin app) {
        SubsonicSystemRoutes.register(app);
        SubsonicLibraryRoutes.register(app);
        SubsonicUserRoutes.register(app);
        SubsonicMediaRoutes.register(app);
        SubsonicPlaylistRoutes.register(app);
    }
}
