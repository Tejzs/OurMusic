package routes;

import com.google.gson.Gson;
import io.javalin.Javalin;
import postgresql.Database;
import scanner.Album;
import scanner.Song;

import java.util.List;

public class AlbumRoutes {
    public static void register(Javalin app) {
        app.get("/api/albums", ctx -> {
            String limit = ctx.queryParam("limit");
            String offset = ctx.queryParam("offset");

            List<Album> albums = Database.getAlbums(Integer.valueOf(limit), Integer.valueOf(offset));

            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(albums));
        });

        app.get("/api/albums/{ID}/songs", ctx -> {
            String ID = ctx.pathParam("ID");
            List<Song> songs = Database.getAlbumsSongs(Integer.valueOf(ID));
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(songs));
        });

        app.get("/api/artists/{ID}/albums", ctx -> {
            String ID = ctx.pathParam("ID");
            List<Album> albums = Database.getArtistAlbums(Integer.valueOf(ID));
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(albums));
        });
    }
}
