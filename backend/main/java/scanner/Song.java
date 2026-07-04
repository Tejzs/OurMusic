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
    private Integer bitRate;
    private Integer samplingRate;
    private Integer channelCount;
    private Integer bitDepth;
    private Integer year;
    private Integer track;
    private Integer discNumber;

    public Song(String title, String artist, String album, String genre, int albumId, int duration, String filePath, long fileSize, long lastModified, String artworkPath) {
        this(title, artist, album, genre, albumId, duration, filePath, fileSize, lastModified, artworkPath, null, null, null, null, null, null, null);
    }

    public Song(String title, String artist, String album, String genre, int albumId, int duration, String filePath, long fileSize, long lastModified, String artworkPath, Integer bitRate, Integer samplingRate, Integer channelCount, Integer bitDepth, Integer year, Integer track, Integer discNumber) {
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
        this.bitRate = bitRate;
        this.samplingRate = samplingRate;
        this.channelCount = channelCount;
        this.bitDepth = bitDepth;
        this.year = year;
        this.track = track;
        this.discNumber = discNumber;
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

    public Integer getBitRate() {
        return bitRate;
    }

    public Integer getSamplingRate() {
        return samplingRate;
    }

    public Integer getChannelCount() {
        return channelCount;
    }

    public Integer getBitDepth() {
        return bitDepth;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getTrack() {
        return track;
    }

    public Integer getDiscNumber() {
        return discNumber;
    }

    public void setTechnicalMetadata(Integer bitRate, Integer samplingRate, Integer channelCount, Integer bitDepth, Integer year, Integer track, Integer discNumber) {
        this.bitRate = bitRate;
        this.samplingRate = samplingRate;
        this.channelCount = channelCount;
        this.bitDepth = bitDepth;
        this.year = year;
        this.track = track;
        this.discNumber = discNumber;
    }
}
