package scanner;

public class Artist {
    private int id;
    private String name;
    private int albumCount;
    private int songCount;
    
    public Artist(int id, String name, int albumCount, int songCount) {
        this.id = id;
        this.name = name;
        this.albumCount = albumCount;
        this.songCount = songCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public void setAlbumCount(int albumCount) {
        this.albumCount = albumCount;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }
}