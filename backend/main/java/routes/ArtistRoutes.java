package routes;

import com.google.gson.Gson;
import io.javalin.Javalin;
import postgresql.Database;
import scanner.Artist;
import scanner.Song;

import java.util.List;

public class ArtistRoutes {
    public static void register(Javalin app) {
        app.get("/api/artists", ctx -> {
            String limit = ctx.queryParam("limit");
            String offset = ctx.queryParam("offset");

            List<Artist> artists = Database.getArtists(Integer.valueOf(limit), Integer.valueOf(offset));

            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(artists));
        });

        app.get("/api/artists/{ID}/songs", ctx -> {
            String ID = ctx.pathParam("ID");
            List<Song> songs = Database.getArtistSongs(Integer.valueOf(ID));
            Gson gson = new Gson();
            ctx.contentType("application/json");
            ctx.result(gson.toJson(songs));
        });
    }
}
