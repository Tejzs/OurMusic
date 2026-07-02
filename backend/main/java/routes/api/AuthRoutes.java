package routes.api;

import java.util.Map;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

import auth.RegisterRequest;
import auth.User;
import io.javalin.Javalin;
import postgresql.Database;
import routes.subsonic.SubsonicTokenSecret;

public class AuthRoutes {
    public static void register(Javalin app) {
        app.post("/api/auth/register", ctx -> {
            RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);
            String passwordHash = BCrypt.hashpw(req.getPassword(), BCrypt.gensalt());
            int resp = Database.registerUser(
                    req.getUsername(),
                    passwordHash,
                    SubsonicTokenSecret.encrypt(req.getPassword())
            );
            if (resp > 0) {
                ctx.status(201).json(Map.of("message", "success"));
                return;
            }

            if (resp == -2) {
                ctx.status(409).json(Map.of("message", "exists"));
                return;
            }

            ctx.status(500).json(Map.of("message", "failure"));
        });

        app.post("/api/auth/login", ctx -> {
            RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);

            User user = Database.getUserByName(req.getUsername());

            if (user == null) {
                ctx.status(401).json(Map.of("message", "invalid"));
                return;
            }

            boolean ok = BCrypt.checkpw(req.getPassword(), user.getPassword());

            if (!ok) {
                ctx.status(401).json(Map.of("message", "invalid"));
                return;
            }

            String token = UUID.randomUUID().toString();
            Database.createSessionToken(user.getId(), token);
            Database.updateSubsonicTokenSecret(user.getId(), SubsonicTokenSecret.encrypt(req.getPassword()));

            ctx.cookie("ourmusic_session", token);
            ctx.json(Map.of("message", "success"));
        });

        app.post("/api/auth/logout", ctx -> {
            String token = ctx.cookie("ourmusic_session");

            if (token == null) {
                ctx.status(401).json(Map.of("message", "not logged in"));
                return;
            }

            boolean ok = Database.clearSession(token);

            if (!ok) {
                ctx.status(401).json(Map.of("message", "invalid session"));
                return;
            }

            ctx.removeCookie("ourmusic_session");
            ctx.json(Map.of("message", "success"));
        });
    }
}
