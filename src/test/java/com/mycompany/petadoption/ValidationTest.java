package com.mycompany.petadoption;

import static org.junit.jupiter.api.Assertions.*;

import com.mycompany.petadoption.util.*;
import org.junit.jupiter.api.Test;

class ValidationTest {
  @Test
  void normalizesEmailWithoutChangingPassword() {
    assertEquals("person@example.com", Validation.email(" Person@Example.com "));
    assertThrows(IllegalArgumentException.class, () -> Validation.email("bad@"));
  }

  @Test
  void rejectsOverflowNegativeAndFractionalValues() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Validation.integer("99999999999999", "Count", 0, Integer.MAX_VALUE));
    assertThrows(IllegalArgumentException.class, () -> Validation.integer("-1", "Count", 0, 100));
    assertThrows(IllegalArgumentException.class, () -> Validation.money("1.001"));
    assertThrows(IllegalArgumentException.class, () -> Validation.money("-5"));
  }

  @Test
  void validatesDatesPhonesAndRequiredFields() {
    assertThrows(IllegalArgumentException.class, () -> Validation.birthDate("2999-01-01"));
    assertThrows(IllegalArgumentException.class, () -> Validation.birthDate("2025-02-30"));
    assertThrows(IllegalArgumentException.class, () -> Validation.phone("+++++++"));
    assertThrows(IllegalArgumentException.class, () -> Validation.text("  ", "Name", 100));
    assertEquals("+94 770000000", Validation.phone("+94 770000000"));
  }

  @Test
  void hashesWithIndependentSaltsAndVerifies() {
    char[] p = "A long test password!".toCharArray();
    String a = Passwords.hash(p), b = Passwords.hash(p);
    assertNotEquals(a, b);
    assertTrue(Passwords.verify(p, a));
    assertFalse(Passwords.verify("incorrect password".toCharArray(), a));
    assertFalse(Passwords.verify(p, "not a hash"));
    assertThrows(IllegalArgumentException.class, () -> Passwords.hash("short".toCharArray()));
  }

  @Test
  void csvEscapesQuotesAndFormulaCells() {
    assertEquals("\"' =SUM(A1)\"", Csv.cell(" =SUM(A1)"));
    assertEquals("\"a\"\"b\"", Csv.cell("a\"b"));
    assertEquals("\"line\nnext\"", Csv.cell("line\nnext"));
  }
}
