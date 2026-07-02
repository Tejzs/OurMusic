package routes.subsonic;

import auth.User;
import io.javalin.Javalin;
import postgresql.Database;

public final class SubsonicUserRoutes {
    private SubsonicUserRoutes() {
    }

    public static void register(Javalin app) {
        SubsonicRequest.register(app, "/rest/getUser.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            String requestedUsername = SubsonicRequest.param(ctx, "username");
            if (isBlank(requestedUsername)) {
                requestedUsername = user.getUsername();
            }

            if (!user.isAdmin() && !user.getUsername().equals(requestedUsername)) {
                SubsonicResponses.writeError(ctx, 403, 50, "Not authorized to view this user.");
                return;
            }

            User requestedUser = Database.getUserByName(requestedUsername);
            if (requestedUser == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "User not found.");
                return;
            }

            SubsonicResponses.writeUser(ctx, requestedUser);
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
