package scanner;

public class Playlist {
    private int id;
    private String name;
    private boolean hasCover;

    public Playlist(int id, String name, boolean hasCover) {
        this.id = id;
        this.name = name;
        this.hasCover = hasCover;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean getHasCover() {
        return hasCover;
    }
}
