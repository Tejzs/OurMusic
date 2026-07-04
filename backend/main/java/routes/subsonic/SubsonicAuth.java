package routes.subsonic;

import auth.User;
import io.javalin.http.Context;
import org.mindrot.jbcrypt.BCrypt;
import postgresql.Database;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class SubsonicAuth {
    private SubsonicAuth() {
    }

    public static User authenticate(Context ctx) {
        String username = SubsonicRequest.param(ctx, "u");
        String password = SubsonicRequest.param(ctx, "p");
        String token = SubsonicRequest.param(ctx, "t");
        String salt = SubsonicRequest.param(ctx, "s");

        if (isBlank(username)) {
            SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
            return null;
        }

        User user = Database.getUserByName(username);
        if (user == null) {
            SubsonicResponses.writeError(ctx, 401, 40, "Wrong username or password.");
            return null;
        }

        if (isBlank(password)) {
            if (!isBlank(token) || !isBlank(salt)) {
                return authenticateToken(ctx, user, token, salt);
            }
            SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
            return null;
        }

        String normalizedPassword = normalizePassword(password);
        if (normalizedPassword == null) {
            SubsonicResponses.writeError(ctx, 401, 40, "Wrong username or password.");
            return null;
        }

        if (!BCrypt.checkpw(normalizedPassword, user.getPassword())) {
            SubsonicResponses.writeError(ctx, 401, 40, "Wrong username or password.");
            return null;
        }

        Database.updateSubsonicTokenSecret(user.getId(), SubsonicTokenSecret.encrypt(normalizedPassword));
        return user;
    }

    private static User authenticateToken(Context ctx, User user, String token, String salt) {
        if (isBlank(token) || isBlank(salt)) {
            SubsonicResponses.writeError(ctx, 400, 10, "Required parameter is missing.");
            return null;
        }

        String storedSecret = SubsonicTokenSecret.decrypt(Database.getSubsonicTokenSecret(user.getId()));
        if (storedSecret == null) {
            SubsonicResponses.writeError(ctx, 401, 40, "Wrong username or password.");
            return null;
        }

        String expectedToken = md5Hex(storedSecret + salt);
        if (expectedToken == null || !expectedToken.equalsIgnoreCase(token)) {
            SubsonicResponses.writeError(ctx, 401, 40, "Wrong username or password.");
            return null;
        }

        return user;
    }

    private static String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                hex.append(String.format("%02x", current));
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }

    static String normalizePassword(String password) {
        if (!password.startsWith("enc:")) {
            return password;
        }

        String hex = password.substring(4);
        if (hex.length() % 2 != 0) {
            return null;
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);

            if (high == -1 || low == -1) {
                return null;
            }

            bytes[i / 2] = (byte) ((high << 4) + low);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
