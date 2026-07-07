package routes.api;

import io.javalin.Javalin;
import postgresql.Database;

import java.util.Map;

public class HealthRoutes {
    public static void register(Javalin app) {
        app.get("/api/health", ctx -> {
            boolean databaseHealthy = Database.healthCheck();

            if (!databaseHealthy) {
                ctx.status(503).json(Map.of(
                        "status", "error",
                        "database", "error"
                ));
                return;
            }

            ctx.status(200).json(Map.of(
                    "status", "ok",
                    "database", "ok"
            ));
        });
    }
}
