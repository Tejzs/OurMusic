package scanner;

public class Album {
    private int id;
    private String title;
    private int artistId;
    private String artist;
    private int songCount;
    private int artworkSongId;
    
    public Album(int id, String title, int artistId, String artist, int songCount, int artworkSongId) {
        this.id = id;
        this.title = title;
        this.artistId = artistId;
        this.artist = artist;
        this.songCount = songCount;
        this.artworkSongId = artworkSongId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getArtistId() {
        return artistId;
    }

    public void setArtistId(int artistId) {
        this.artistId = artistId;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public int getArtworkSongId() {
        return artworkSongId;
    }

    public void setArtworkSongId(int artworkSongId) {
        this.artworkSongId = artworkSongId;
    }
}
