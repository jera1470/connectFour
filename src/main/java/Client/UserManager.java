package Client;

import java.io.*;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;

public class UserManager {
    private static final String USER_FILE = "users.txt";
    private static final Map<String, String> userCredentials = new ConcurrentHashMap<>();
    private static final Set<String> activeUsers = ConcurrentHashMap.newKeySet();

    static {
        loadUsers();
    }

    public static boolean isUsernameTaken(String username) {
        return activeUsers.contains(username.toLowerCase());
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public static boolean validateCredentials(String username, String password) {
        String lowerUsername = username.toLowerCase();
        if (!userCredentials.containsKey(lowerUsername)) {
            return false;
        }

        String hashedPassword = hashPassword(password);
        return userCredentials.get(lowerUsername).equals(hashedPassword);
    }


    public static synchronized boolean registerUser(String username, String password) {
        String lowerUsername = username.toLowerCase();

        if (userCredentials.containsKey(lowerUsername)) {
            return false;
        }

        String hashedPassword = hashPassword(password);
        userCredentials.put(lowerUsername, hashedPassword);
        saveUsers();

        return true;
    }

    public static void removeActiveUser(String username) {
        activeUsers.remove(username.toLowerCase());
    }

    private static void loadUsers() {
        try {
            if (Files.exists(Paths.get(USER_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(USER_FILE));
                for (String line : lines) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        userCredentials.put(parts[0].toLowerCase(), parts[1]);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    private static synchronized void saveUsers() {
        try {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, String> entry : userCredentials.entrySet()) {
                lines.add(entry.getKey() + ":" + entry.getValue());
            }
            Files.write(Paths.get(USER_FILE), lines);
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }

    public static boolean isRegisteredUser(String username) {
        return userCredentials.containsKey(username.toLowerCase());
    }

    public static synchronized void addUser(String username) {
        String lowerUsername = username.toLowerCase();
        if (!userCredentials.containsKey(lowerUsername)) {

            userCredentials.put(lowerUsername, hashPassword(""));
            saveUsers();
        }
        activeUsers.add(lowerUsername);
    }
}
