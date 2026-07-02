package scanner;

import java.io.File;
import java.io.FileOutputStream;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import config.Properties;
import postgresql.Database;
import utils.Utils;

public class Scanner {


    public static void scanLibrary(String songsPath) {
        File songDir = new File(songsPath);
        File[] files = songDir.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                scanLibrary(file.getPath());
            }
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
                    artists = new String[]{"Unknown Artist"};
                }

                String album = tag != null ? tag.getFirst(FieldKey.ALBUM) : "Unknown Album";
                String genre = tag != null ? tag.getFirst(FieldKey.GENRE) : null;
                int albumId = Database.getAlbum(album, Database.getArtist(artists[0].trim()));
                Artwork artwork = tag != null ? tag.getFirstArtwork() : null;
                String artworkPath = null;

                if (artwork != null) {
                    artworkPath = Properties.getSongsArtworkFolder() + File.separator + Utils.sha256(title).substring(0, 16) + ".jpg";
                }

                if (artworkPath != null) {
                    try (FileOutputStream fileOutputStream = new FileOutputStream(artworkPath)) {
                        byte[] b = artwork.getBinaryData();
                        fileOutputStream.write(b);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        artworkPath = null;
                    }
                }

                int duration = audioFile.getAudioHeader().getTrackLength();
                String filePath = audioFile.getFile().getPath();
                long fileSize = audioFile.getFile().length();
                long lastModified = audioFile.getFile().lastModified();

                Song song = new Song(title, String.join(", ", artists), album, genre, albumId, duration, filePath, fileSize, lastModified, artworkPath);
                Database.insertSong(song);
                for (String artist : artists) {
                    int artistId = Database.getArtist(artist.trim());
                    Database.insertSongArtists(song.getId(), artistId);
                }
            } catch (Exception e) {
                System.out.println("Failed to scan: " + file.getAbsolutePath());
                System.out.println(e.getMessage());
            }
        }
    }
}
