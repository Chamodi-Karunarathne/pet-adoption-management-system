package com.mycompany.petadoption.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;

public final class Images {
  private Images() {}

  public static Path directory() {
    return Path.of(
        System.getProperty(
            "pet.data", Path.of(System.getProperty("user.home"), ".woof").toString()),
        "images");
  }

  public static BufferedImage read(String path) {
    if (path == null || path.isBlank()) return null;
    try {
      if (path.matches("upload:[a-f0-9-]+\\.png"))
        return ImageIO.read(directory().resolve(path.substring(7)).toFile());
      if (!path.matches("/images/[a-zA-Z0-9._-]+")) return null;
      try (var in = Images.class.getResourceAsStream(path)) {
        return in == null ? null : ImageIO.read(in);
      }
    } catch (IOException e) {
      return null;
    }
  }

  public static String importFile(Path file) throws IOException {
    if (Files.size(file) > 10 * 1024 * 1024)
      throw new IllegalArgumentException("Choose an image smaller than 10 MB.");
    try (var stream = ImageIO.createImageInputStream(file.toFile())) {
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext()) throw new IllegalArgumentException("Choose a PNG or JPEG image.");
      var reader = readers.next();
      try {
        reader.setInput(stream);
        int w = reader.getWidth(0), h = reader.getHeight(0);
        if ((long) w * h > 25_000_000L)
          throw new IllegalArgumentException("Image dimensions are too large.");
        BufferedImage source = reader.read(0);
        double scale = Math.min(1, 1600.0 / Math.max(w, h));
        var resized =
            new BufferedImage(
                Math.max(1, (int) (w * scale)),
                Math.max(1, (int) (h * scale)),
                BufferedImage.TYPE_INT_RGB);
        var g = resized.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, resized.getWidth(), resized.getHeight());
        g.drawImage(source, 0, 0, resized.getWidth(), resized.getHeight(), null);
        g.dispose();
        Files.createDirectories(directory());
        String name = UUID.randomUUID() + ".png";
        ImageIO.write(resized, "png", directory().resolve(name).toFile());
        return "upload:" + name;
      } finally {
        reader.dispose();
      }
    }
  }
}
