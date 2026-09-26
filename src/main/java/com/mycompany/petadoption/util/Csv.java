package com.mycompany.petadoption.util;

import com.mycompany.petadoption.model.Models.Report;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class Csv {
  private Csv() {}

  public static String cell(Object value) {
    String text = Objects.toString(value, "");
    String trimmed = text.stripLeading();
    if (!trimmed.isEmpty() && "=+@-".indexOf(trimmed.charAt(0)) >= 0) text = "'" + text;
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }

  public static void write(Path path, Report report) throws IOException {
    try (var w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      w.write(
          report.columns().stream()
              .map(Csv::cell)
              .collect(java.util.stream.Collectors.joining(",")));
      w.newLine();
      for (var row : report.rows()) {
        w.write(row.stream().map(Csv::cell).collect(java.util.stream.Collectors.joining(",")));
        w.newLine();
      }
    }
  }
}
