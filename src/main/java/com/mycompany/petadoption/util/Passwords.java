package com.mycompany.petadoption.util;

import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
  private static final int ITERATIONS = 600_000;

  private Passwords() {}

  public static String hash(char[] password) {
    Validation.password(password);
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return "pbkdf2-sha256$"
        + ITERATIONS
        + "$"
        + Base64.getEncoder().encodeToString(salt)
        + "$"
        + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
  }

  public static boolean verify(char[] password, String encoded) {
    if (password == null || password.length > 128 || encoded == null) return false;
    try {
      String[] parts = encoded.split("\\$");
      if (parts.length != 4 || !parts[0].equals("pbkdf2-sha256")) return false;
      int rounds = Integer.parseInt(parts[1]);
      if (rounds < 100_000 || rounds > 2_000_000) return false;
      return MessageDigest.isEqual(
          Base64.getDecoder().decode(parts[3]),
          derive(password, Base64.getDecoder().decode(parts[2]), rounds));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static byte[] derive(char[] password, byte[] salt, int rounds) {
    var spec = new PBEKeySpec(password, salt, rounds, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Password hashing unavailable", e);
    } finally {
      spec.clearPassword();
    }
  }
}
