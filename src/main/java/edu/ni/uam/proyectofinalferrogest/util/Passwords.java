package edu.ni.uam.proyectofinalferrogest.util;

import org.mindrot.jbcrypt.BCrypt;
import java.nio.charset.StandardCharsets;
public final class Passwords {
    private Passwords() {}
    public static String hash(String password) {
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres y máximo 72 bytes UTF-8.");
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
    public static boolean verifica(String password, String hash) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72 || hash == null) return false;
        try { return BCrypt.checkpw(password, hash); }
        catch (IllegalArgumentException e) { return false; }
    }
}