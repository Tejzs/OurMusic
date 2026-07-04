package scanner;

import java.io.File;
import java.io.FileOutputStream;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.Properties;
import postgresql.Database;
import utils.Utils;

public class Scanner {

    private static final Logger LOG = LoggerFactory.getLogger(Scanner.class);

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
                        LOG.warn("Failed to write artwork for {}", file.getAbsolutePath(), e);
                        artworkPath = null;
                    }
                }

                AudioHeader audioHeader = audioFile.getAudioHeader();
                int duration = audioHeader.getTrackLength();
                String filePath = audioFile.getFile().getPath();
                long fileSize = audioFile.getFile().length();
                long lastModified = audioFile.getFile().lastModified();
                Integer bitRate = positiveInt(audioHeader.getBitRateAsNumber());
                Integer samplingRate = positiveInt(audioHeader.getSampleRateAsNumber());
                Integer channelCount = parseChannelCount(audioHeader.getChannels());
                Integer bitDepth = positiveInt(audioHeader.getBitsPerSample());
                Integer year = tag != null ? parseTagNumber(tag.getFirst(FieldKey.YEAR)) : null;
                Integer track = tag != null ? parseTagNumber(tag.getFirst(FieldKey.TRACK)) : null;
                Integer discNumber = tag != null ? parseTagNumber(tag.getFirst(FieldKey.DISC_NO)) : null;

                Song song = new Song(title, String.join(", ", artists), album, genre, albumId, duration, filePath, fileSize, lastModified, artworkPath, bitRate, samplingRate, channelCount, bitDepth, year, track, discNumber);
                Database.insertSong(song);
                for (String artist : artists) {
                    int artistId = Database.getArtist(artist.trim());
                    Database.insertSongArtists(song.getId(), artistId);
                }
            } catch (Exception e) {
                LOG.warn("Failed to scan {}", file.getAbsolutePath(), e);
            }
        }
    }

    private static Integer positiveInt(long value) {
        return value > 0 && value <= Integer.MAX_VALUE ? (int) value : null;
    }

    private static Integer parseChannelCount(String channels) {
        if (channels == null || channels.isBlank()) {
            return null;
        }

        String normalizedChannels = channels.trim().toLowerCase();
        if (normalizedChannels.contains("mono")) {
            return 1;
        }
        if (normalizedChannels.contains("stereo")) {
            return 2;
        }

        return parseTagNumber(normalizedChannels);
    }

    private static Integer parseTagNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char current = trimmed.charAt(i);
            if (Character.isDigit(current)) {
                digits.append(current);
            } else if (!digits.isEmpty()) {
                break;
            }
        }

        if (digits.isEmpty()) {
            return null;
        }

        try {
            int parsed = Integer.parseInt(digits.toString());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
