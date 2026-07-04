package routes.subsonic;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;

import java.util.List;

public final class SubsonicRequest {
    private SubsonicRequest() {
    }

    public static void register(Javalin app, String path, Handler handler) {
        app.get(path, handler);
        app.post(path, handler);

        if (path.endsWith(".view")) {
            String alternatePath = path.substring(0, path.length() - ".view".length());
            app.get(alternatePath, handler);
            app.post(alternatePath, handler);
        }
    }

    public static String param(Context ctx, String name) {
        String value = ctx.queryParam(name);
        if (value != null) {
            return value;
        }

        return ctx.formParam(name);
    }

    public static List<String> params(Context ctx, String name) {
        List<String> queryValues = ctx.queryParams(name);
        if (!queryValues.isEmpty()) {
            return queryValues;
        }

        List<String> formValues = ctx.formParams(name);
        if (!formValues.isEmpty()) {
            return formValues;
        }

        return List.of();
    }
}
