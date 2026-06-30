import config.Properties;
import io.javalin.Javalin;

import postgresql.Database;
import routes.api.AdminRoutes;
import routes.api.AlbumRoutes;
import routes.api.ArtistRoutes;
import routes.api.AuthRoutes;
import routes.api.PlaylistRoutes;
import routes.api.SongRoutes;
import routes.api.UserRoutes;

public class Server {
    public static void main(String[] args) throws Exception {
        Properties.loadConfigurations("backend/main/java/application.properties");
        Database.setup();
        Database.init();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.anyHost();
                });
            });
        }).start(Properties.getPort());

        AdminRoutes.register(app);
        AlbumRoutes.register(app);
        ArtistRoutes.register(app);
        AuthRoutes.register(app);
        PlaylistRoutes.register(app);
        SongRoutes.register(app);
        UserRoutes.register(app);
    }
}
