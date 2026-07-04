package routes.subsonic;

import auth.User;
import io.javalin.Javalin;
import postgresql.Database;
import routes.api.ScanStatus;
import routes.api.SongRoutes;

import java.util.List;
import java.util.Map;

public final class SubsonicSystemRoutes {
    private static final String LICENSE_EMAIL = "support@ourmusic.local";
    private static final String LICENSE_EXPIRES = "2099-12-31T23:59:59Z";
    private static final String TRIAL_EXPIRES = "2099-12-31T23:59:59Z";
    private static final List<Map<String, Object>> SUPPORTED_EXTENSIONS = List.of(
            Map.of("name", "formPost", "versions", List.of(1)),
            Map.of("name", "transcodeOffset", "versions", List.of(1))
    );

    private SubsonicSystemRoutes() {
    }

    public static void register(Javalin app) {
        SubsonicRequest.register(app, "/rest/ping.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            SubsonicResponses.writeSuccess(ctx);
        });

        SubsonicRequest.register(app, "/rest/getLicense.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            SubsonicResponses.writeLicense(ctx, true, LICENSE_EMAIL, LICENSE_EXPIRES, TRIAL_EXPIRES);
        });

        SubsonicRequest.register(app, "/rest/startScan.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }
            if (!user.isAdmin()) {
                SubsonicResponses.writeError(ctx, 403, 50, "Not authorized to start scan.");
                return;
            }

            SongRoutes.startFullLibraryScan();
            SubsonicResponses.writeScanStatus(ctx, true, Database.getSongCount());
        });

        SubsonicRequest.register(app, "/rest/getScanStatus.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            boolean scanning = SongRoutes.getScanStatus() == ScanStatus.RUNNING;
            SubsonicResponses.writeScanStatus(ctx, scanning, Database.getSongCount());
        });

        SubsonicRequest.register(app, "/rest/getOpenSubsonicExtensions.view", ctx ->
                SubsonicResponses.writeOpenSubsonicExtensions(ctx, SUPPORTED_EXTENSIONS));
    }
}
