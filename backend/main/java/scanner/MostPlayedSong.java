package scanner;

public class MostPlayedSong {
    private Song song;
    private int playCount;

    public MostPlayedSong(Song song, int playCount) {
        this.song = song;
        this.playCount = playCount;
    }

    public Song getSong() {
        return song;
    }

    public int getPlayCount() {
        return playCount;
    }
}
