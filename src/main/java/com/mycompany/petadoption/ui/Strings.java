package com.mycompany.petadoption.ui;

import java.util.*;

public final class Strings {
  private static Locale locale = Locale.ENGLISH;

  private Strings() {}

  public static void french(boolean french) {
    locale = french ? Locale.FRENCH : Locale.ENGLISH;
  }

  public static String get(String key) {
    return ResourceBundle.getBundle("i18n.Messages", locale).getString(key);
  }
}
