package routes.subsonic;

import config.Properties;
import io.javalin.http.Context;
import scanner.Song;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SubsonicTranscoder {
    private static final String DEFAULT_FORMAT = "mp3";

    private SubsonicTranscoder() {
    }

    public static TranscodeSession startIfRequested(Context ctx, Song song) throws Exception {
        String requestedFormat = normalizeFormat(SubsonicRequest.param(ctx, "format"));
        Integer maxBitRate = parsePositiveInt(SubsonicRequest.param(ctx, "maxBitRate"));
        Double timeOffset = parsePositiveDouble(SubsonicRequest.param(ctx, "timeOffset"));

        String inputSuffix = fileSuffix(song.getFilePath());
        boolean formatRequestsTranscode = requestedFormat != null
                && !"raw".equals(requestedFormat)
                && !requestedFormat.equals(inputSuffix);

        if (!formatRequestsTranscode && maxBitRate == null && timeOffset == null) {
            return null;
        }

        String outputFormat = requestedFormat;
        if (outputFormat == null || "raw".equals(outputFormat) || outputFormat.equals(inputSuffix)) {
            outputFormat = DEFAULT_FORMAT;
        }

        EncoderProfile encoderProfile = encoderProfile(outputFormat);
        if (encoderProfile == null) {
            throw new IllegalArgumentException("Unsupported transcode format.");
        }

        List<String> command = new ArrayList<>();
        command.add(Properties.getFfmpegPath());
        command.add("-nostdin");
        command.add("-v");
        command.add("error");
        if (timeOffset != null) {
            command.add("-ss");
            command.add(String.valueOf(timeOffset));
        }
        command.add("-i");
        command.add(song.getFilePath());
        command.add("-map");
        command.add("0:a:0");
        command.add("-vn");
        if (encoderProfile.codec() != null) {
            command.add("-codec:a");
            command.add(encoderProfile.codec());
        }
        if (maxBitRate != null && encoderProfile.supportsBitrate()) {
            command.add("-b:a");
            command.add(maxBitRate + "k");
        }
        command.add("-f");
        command.add(encoderProfile.ffmpegFormat());
        command.add("pipe:1");

        Process process = new ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();

        return new TranscodeSession(
                process,
                new ProcessInputStream(process),
                encoderProfile.contentType(),
                outputFileName(song, encoderProfile.fileExtension())
        );
    }

    private static EncoderProfile encoderProfile(String format) {
        return switch (format) {
            case "mp3" -> new EncoderProfile("mp3", "libmp3lame", "audio/mpeg", "mp3", true);
            case "ogg" -> new EncoderProfile("ogg", "libvorbis", "audio/ogg", "ogg", true);
            case "opus" -> new EncoderProfile("opus", "libopus", "audio/ogg", "opus", true);
            case "aac" -> new EncoderProfile("adts", "aac", "audio/aac", "aac", true);
            case "flac" -> new EncoderProfile("flac", "flac", "audio/flac", "flac", false);
            default -> null;
        };
    }

    private static String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return null;
        }
        return format.trim().toLowerCase();
    }

    private static Integer parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parsePositiveDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value);
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String fileSuffix(String filePath) {
        String fileName = Path.of(filePath).getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private static String outputFileName(Song song, String extension) {
        String baseName = Path.of(song.getFilePath()).getFileName().toString();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex >= 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        return baseName + "." + extension;
    }

    private static final class ProcessInputStream extends FilterInputStream {
        private final Process process;

        private ProcessInputStream(Process process) {
            super(process.getInputStream());
            this.process = process;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                process.destroy();
            }
        }
    }

    private record EncoderProfile(String ffmpegFormat, String codec, String contentType, String fileExtension,
                                  boolean supportsBitrate) {
    }

    public record TranscodeSession(Process process, InputStream stream, String contentType, String outputFileName) {
    }
}
