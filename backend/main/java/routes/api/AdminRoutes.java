package routes.api;

import auth.UpdatePasswordRequest;
import auth.UpdateUsernameRequest;
import auth.User;
import com.google.gson.Gson;
import io.javalin.Javalin;
import org.mindrot.jbcrypt.BCrypt;
import postgresql.Database;
import routes.subsonic.SubsonicTokenSecret;

import java.util.List;
import java.util.Map;

public class AdminRoutes {
    public static void register(Javalin app) {
        app.get("/api/admin/users", ctx -> {
            String token = ctx.cookie("ourmusic_session");
            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            User user = Database.getUserFromId(userId);

            if (!user.isAdmin()) {
                ctx.status(403).json(Map.of("message", "user not an admin"));
                return;
            }

            List<User> users = Database.getAllUsers();
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(users.stream()
                    .map(adminUser -> Map.<String, Object>of(
                            "id", adminUser.getId(),
                            "username", adminUser.getUsername(),
                            "isAdmin", adminUser.isAdmin()
                    ))
                    .toList()));
        });

        app.delete("/api/admin/users/{ID}", ctx -> {
            String ID = ctx.pathParam("ID");
            String token = ctx.cookie("ourmusic_session");
            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == Integer.valueOf(ID)) {
                ctx.status(400).json(Map.of("message", "cannot delete yourself"));
                return;
            }

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            User user = Database.getUserFromId(userId);

            if (!user.isAdmin()) {
                ctx.status(403).json(Map.of("message", "user not an admin"));
                return;
            }

            boolean ok = Database.deleteUser(Integer.valueOf(ID));

            if (!ok) {
                ctx.status(404).json(Map.of("message", "user not found"));
                return;
            }

            ctx.status(200).json(Map.of("message", "success"));
        });

        app.patch("/api/admin/users/{ID}/username", ctx -> {
            String ID = ctx.pathParam("ID");
            UpdateUsernameRequest req = ctx.bodyAsClass(UpdateUsernameRequest.class);

            String token = ctx.cookie("ourmusic_session");
            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == Integer.valueOf(ID)) {
                ctx.status(400).json(Map.of("message", "cannot delete yourself"));
                return;
            }

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            User user = Database.getUserFromId(userId);

            if (!user.isAdmin()) {
                ctx.status(403).json(Map.of("message", "user not an admin"));
                return;
            }

            boolean ok = Database.changeUsername(Integer.valueOf(ID), req.getUsername());

            if (!ok) {
                ctx.status(500).json(Map.of("message", "update failed"));
                return;
            }

            ctx.status(200).json(Map.of("message", "success"));
        });

        app.patch("/api/admin/users/{ID}/password", ctx -> {
            String ID = ctx.pathParam("ID");
            UpdatePasswordRequest req = ctx.bodyAsClass(UpdatePasswordRequest.class);
            String passwordHash = BCrypt.hashpw(req.getPassword(), BCrypt.gensalt());

            String token = ctx.cookie("ourmusic_session");
            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            int userId = Database.getUserIdFromSession(token);

            if (userId == Integer.valueOf(ID)) {
                ctx.status(400).json(Map.of("message", "cannot delete yourself"));
                return;
            }

            if (userId == -1) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            User user = Database.getUserFromId(userId);

            if (!user.isAdmin()) {
                ctx.status(403).json(Map.of("message", "user not an admin"));
                return;
            }

            boolean ok = Database.changePassword(
                    Integer.valueOf(ID),
                    passwordHash,
                    SubsonicTokenSecret.encrypt(req.getPassword())
            );

            if (!ok) {
                ctx.status(500).json(Map.of("message", "update failed"));
                return;
            }

            ctx.status(200).json(Map.of("message", "success"));
        });
    }
}
