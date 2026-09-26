package com.mycompany.petadoption.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public final class Validation {
  private Validation() {}

  public static String text(String value, String label, int max) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException(label + " is required.");
    value = value.strip();
    if (value.length() > max)
      throw new IllegalArgumentException(label + " must be at most " + max + " characters.");
    if (value.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t'))
      throw new IllegalArgumentException(label + " contains unsupported characters.");
    return value;
  }

  public static String email(String v) {
    v = text(v, "Email", 254).toLowerCase(Locale.ROOT);
    if (!v.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      throw new IllegalArgumentException("Enter a valid email address.");
    return v;
  }

  public static String phone(String v) {
    v = text(v, "Phone", 30);
    if (!v.matches("[+()0-9 .-]{7,30}") || v.replaceAll("\\D", "").length() < 7)
      throw new IllegalArgumentException("Enter a valid phone number including area code.");
    return v;
  }

  public static void password(char[] v) {
    if (v == null || v.length < 12 || v.length > 128)
      throw new IllegalArgumentException("Use a password of 12 to 128 characters.");
  }

  public static int integer(String v, String label, int min, int max) {
    try {
      int n = Integer.parseInt(v.strip());
      if (n < min || n > max) throw new NumberFormatException();
      return n;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          label + " must be a whole number from " + min + " to " + max + ".");
    }
  }

  public static BigDecimal money(String v) {
    try {
      var n = new BigDecimal(v.strip());
      if (n.signum() < 0 || n.compareTo(new BigDecimal("9999999999.99")) > 0 || n.scale() > 2)
        throw new NumberFormatException();
      return n;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          "Enter a non-negative fee with at most two decimal places.");
    }
  }

  public static LocalDate birthDate(String v) {
    try {
      var d = LocalDate.parse(v.strip());
      if (d.isAfter(LocalDate.now()) || d.isBefore(LocalDate.of(1980, 1, 1)))
        throw new IllegalArgumentException();
      return d;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Birth date must be YYYY-MM-DD, between 1980 and today.");
    }
  }
}
