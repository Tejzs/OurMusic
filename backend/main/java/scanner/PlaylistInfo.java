package scanner;

public class PlaylistInfo {
    private final int id;
    private final String name;
    private final String comment;
    private final String owner;
    private final boolean isPublic;
    private final String created;
    private final String changed;
    private final int songCount;
    private final int duration;
    private final String coverArt;
    private final boolean readonly;

    public PlaylistInfo(int id, String name, String comment, String owner, boolean isPublic, String created,
                        String changed, int songCount, int duration, String coverArt, boolean readonly) {
        this.id = id;
        this.name = name;
        this.comment = comment;
        this.owner = owner;
        this.isPublic = isPublic;
        this.created = created;
        this.changed = changed;
        this.songCount = songCount;
        this.duration = duration;
        this.coverArt = coverArt;
        this.readonly = readonly;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public String getOwner() {
        return owner;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getCreated() {
        return created;
    }

    public String getChanged() {
        return changed;
    }

    public int getSongCount() {
        return songCount;
    }

    public int getDuration() {
        return duration;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public boolean isReadonly() {
        return readonly;
    }
}
