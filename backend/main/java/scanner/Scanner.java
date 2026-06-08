package scanner;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import postgresql.Database;
import utils.Utils;

public class Scanner {
    private static String songsPath = "/home/tejas/navidrome/music";
    private static String artworkPath = "/home/tejas/Projects/OurMusic/backend/main/resources/artwork";

    public static List<Song> scanLibrary() {
        List<Song> songs = new ArrayList();

        File songDir = new File(songsPath);
        File[] files = songDir.listFiles();
                
        for (File file : files) {
            try {
                AudioFile audioFile = AudioFileIO.read(file);
                Tag tag = audioFile.getTag();

                String title = tag != null ? tag.getFirst(FieldKey.TITLE) : file.getName();
                String[] artists;
                if (tag != null) {
                    artists = tag.getFirst(FieldKey.ARTIST).split("/ ");
                    for (String artist : artists) {
                        Database.getArtist(artist.trim());
                    }
                } else {
                    artists = new String[] {"Unknown Artist"};
                }

                String album = tag != null ? tag.getFirst(FieldKey.ALBUM) : "Unknown Album";
                int albumId = Database.getAlbum(album, Database.getArtist(artists[0].trim()));

                try (FileOutputStream fileOutputStream = new FileOutputStream(artworkPath + File.separator + Utils.sha256(title).substring(0, 16) + ".jpg")) {
                    Artwork artwork = tag != null ? tag.getFirstArtwork() : null;
                    if (artwork != null) {
                        byte[] b = artwork.getBinaryData();
                        fileOutputStream.write(b);
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                
                int duration = audioFile.getAudioHeader().getTrackLength();
                String filePath = audioFile.getFile().getPath();
                long fileSize = audioFile.getFile().length();
                long lastModified = audioFile.getFile().lastModified();

                Song song = new Song(title, String.join(", ", artists), album, albumId, duration, filePath, fileSize, lastModified, artworkPath + File.separator + Utils.sha256(title).substring(0, 16) + ".jpg");
                Database.insertSong(song);
                songs.add(song);
                for (String artist : artists) {
                    Database.insertSongArtists(song.getId(), Database.getArtist(artist));
                }
            } catch (Exception e) {
                System.out.println("Failed to scan: " + file.getAbsolutePath());
                System.out.println(e.getMessage());
            }
        }
        return songs;
    }
}
