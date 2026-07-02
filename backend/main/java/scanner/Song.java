package scanner;

public class Song {
    private int id;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private int albumId;
    private int duration;
    private transient String filePath;
    private transient long fileSize;
    private transient long lastModified;
    private transient String artworkPath;

    public Song(String title, String artist, String album, String genre, int albumId, int duration, String filePath, long fileSize, long lastModified, String artworkPath) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.albumId = albumId;
        this.duration = duration;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.artworkPath = artworkPath;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getGenre() {
        return genre;
    }

    public int getDuration() {
        return duration;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getArtworkPath() {
        return artworkPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAlbumId() {
        return albumId;
    }
}
