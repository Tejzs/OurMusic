package routes.subsonic;

import auth.User;
import io.javalin.Javalin;
import org.mindrot.jbcrypt.BCrypt;
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

        SubsonicRequest.register(app, "/rest/createUser.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            if (!user.isAdmin()) {
                SubsonicResponses.writeError(ctx, 403, 50, "Not authorized to create users.");
                return;
            }

            String username = SubsonicRequest.param(ctx, "username");
            String password = SubsonicRequest.param(ctx, "password");
            if (isBlank(username) || isBlank(password)) {
                SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
                return;
            }

            String normalizedPassword = SubsonicAuth.normalizePassword(password);
            if (normalizedPassword == null) {
                SubsonicResponses.writeError(ctx, 400, 10, "Invalid password encoding.");
                return;
            }

            int result = Database.registerUser(
                    username,
                    BCrypt.hashpw(normalizedPassword, BCrypt.gensalt()),
                    SubsonicTokenSecret.encrypt(normalizedPassword),
                    parseBoolean(ctx, "adminRole", false)
            );

            if (result == -2) {
                SubsonicResponses.writeError(ctx, 409, 0, "User already exists.");
                return;
            }

            if (result < 1) {
                SubsonicResponses.writeError(ctx, 500, 0, "Unable to create user.");
                return;
            }

            SubsonicResponses.writeSuccess(ctx);
        });

        SubsonicRequest.register(app, "/rest/updateUser.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            if (!user.isAdmin()) {
                SubsonicResponses.writeError(ctx, 403, 50, "Not authorized to update users.");
                return;
            }

            String username = SubsonicRequest.param(ctx, "username");
            if (isBlank(username)) {
                SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
                return;
            }

            User requestedUser = Database.getUserByName(username);
            if (requestedUser == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "User not found.");
                return;
            }

            String password = SubsonicRequest.param(ctx, "password");
            if (!isBlank(password) && !updatePassword(ctx, requestedUser, password)) {
                return;
            }

            String adminRole = SubsonicRequest.param(ctx, "adminRole");
            if (!isBlank(adminRole)) {
                boolean nextAdminRole = Boolean.parseBoolean(adminRole);
                if (requestedUser.getId() == user.getId() && !nextAdminRole) {
                    SubsonicResponses.writeError(ctx, 400, 0, "Cannot remove your own admin role.");
                    return;
                }

                if (!Database.changeAdminRole(requestedUser.getId(), nextAdminRole)) {
                    SubsonicResponses.writeError(ctx, 500, 0, "Unable to update user.");
                    return;
                }
            }

            SubsonicResponses.writeSuccess(ctx);
        });

        SubsonicRequest.register(app, "/rest/deleteUser.view", ctx -> {
            User user = SubsonicAuth.authenticate(ctx);
            if (user == null) {
                return;
            }

            if (!user.isAdmin()) {
                SubsonicResponses.writeError(ctx, 403, 50, "Not authorized to delete users.");
                return;
            }

            String username = SubsonicRequest.param(ctx, "username");
            if (isBlank(username)) {
                SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
                return;
            }

            User requestedUser = Database.getUserByName(username);
            if (requestedUser == null) {
                SubsonicResponses.writeError(ctx, 404, 70, "User not found.");
                return;
            }

            if (requestedUser.getId() == user.getId()) {
                SubsonicResponses.writeError(ctx, 400, 0, "Cannot delete yourself.");
                return;
            }

            if (!Database.deleteUser(requestedUser.getId())) {
                SubsonicResponses.writeError(ctx, 500, 0, "Unable to delete user.");
                return;
            }

            SubsonicResponses.writeSuccess(ctx);
        });
    }

    private static boolean updatePassword(io.javalin.http.Context ctx, User requestedUser, String password) {
        String normalizedPassword = SubsonicAuth.normalizePassword(password);
        if (normalizedPassword == null) {
            SubsonicResponses.writeError(ctx, 400, 10, "Invalid password encoding.");
            return false;
        }

        boolean ok = Database.changePassword(
                requestedUser.getId(),
                BCrypt.hashpw(normalizedPassword, BCrypt.gensalt()),
                SubsonicTokenSecret.encrypt(normalizedPassword)
        );

        if (!ok) {
            SubsonicResponses.writeError(ctx, 500, 0, "Unable to update user.");
            return false;
        }

        return true;
    }

    private static boolean parseBoolean(io.javalin.http.Context ctx, String name, boolean fallback) {
        String value = SubsonicRequest.param(ctx, name);
        if (isBlank(value)) {
            return fallback;
        }
        return Boolean.parseBoolean(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
