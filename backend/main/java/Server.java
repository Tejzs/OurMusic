import config.Properties;
import io.javalin.Javalin;
import org.mindrot.jbcrypt.BCrypt;

import postgresql.Database;
import routes.api.AdminRoutes;
import routes.api.AlbumRoutes;
import routes.api.ArtistRoutes;
import routes.api.AuthRoutes;
import routes.api.PlaylistRoutes;
import routes.api.SongRoutes;
import routes.api.UserRoutes;
import routes.subsonic.OpenSubsonicRoutes;
import routes.subsonic.SubsonicTokenSecret;

public class Server {
    public static void main(String[] args) throws Exception {
        Properties.loadConfigurations("backend/main/java/application.properties");
        Database.setup();
        Database.init();
        Database.ensureAdminUser(
                Properties.getAdminUsername(),
                BCrypt.hashpw(Properties.getAdminPassword(), BCrypt.gensalt()),
                SubsonicTokenSecret.encrypt(Properties.getAdminPassword())
        );

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    for (String origin : Properties.getCorsAllowedOrigins()) {
                        if (!origin.isBlank()) {
                            rule.allowHost(origin);
                        }
                    }
                });
            });
        }).start(Properties.getPort());

        app.after(ctx -> {
            String queryString = sanitizeQueryString(ctx.queryString());
            String requestTarget = ctx.path();
            if (queryString != null && !queryString.isBlank()) {
                requestTarget += "?" + queryString;
            }

            System.out.printf("%s %s -> %s%n", ctx.method(), requestTarget, ctx.status());
        });

        // OurMuic API
        AdminRoutes.register(app);
        AlbumRoutes.register(app);
        ArtistRoutes.register(app);
        AuthRoutes.register(app);
        PlaylistRoutes.register(app);
        SongRoutes.register(app);
        UserRoutes.register(app);

        // Subsonic API
        OpenSubsonicRoutes.register(app);
    }

    private static String sanitizeQueryString(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return queryString;
        }

        String[] parts = queryString.split("&");
        for (int i = 0; i < parts.length; i += 1) {
            int separatorIndex = parts[i].indexOf('=');
            String key = separatorIndex >= 0 ? parts[i].substring(0, separatorIndex) : parts[i];
            if ("p".equals(key) || "t".equals(key) || "s".equals(key)) {
                parts[i] = key + "=***";
            }
        }

        return String.join("&", parts);
    }
}
