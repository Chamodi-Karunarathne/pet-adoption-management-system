package com.mycompany.petadoption.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Models {
  private Models() {}

  public enum Role {
    ADMIN,
    USER
  }

  public enum PetStatus {
    AVAILABLE,
    ON_HOLD,
    ADOPTED,
    ARCHIVED
  }

  public enum ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    WITHDRAWN
  }

  public record User(long id, String name, String email, String phone, Role role, boolean active) {}

  public record Credential(User user, String hash, int failures, java.time.Instant lockedUntil) {}

  public record Choice(long id, String name) {
    @Override
    public String toString() {
      return name;
    }
  }

  public record Breed(long id, long speciesId, String species, String name) {
    @Override
    public String toString() {
      return name;
    }
  }

  public record Pet(
      long id,
      String name,
      long breedId,
      String species,
      String breed,
      LocalDate birthDate,
      String sex,
      String description,
      String imagePath,
      BigDecimal fee,
      PetStatus status,
      int version) {
    public String age() {
      var p = java.time.Period.between(birthDate, LocalDate.now());
      return p.getYears() > 0
          ? p.getYears() + " yr " + p.getMonths() + " mo"
          : Math.max(0, p.getMonths()) + " months";
    }
  }

  public record PetInput(
      String name,
      long breedId,
      LocalDate birthDate,
      String sex,
      String description,
      String imagePath,
      BigDecimal fee,
      PetStatus status) {}

  public record AdoptionRequest(
      long id,
      long userId,
      long petId,
      String applicant,
      String email,
      String pet,
      String species,
      String motivation,
      String housing,
      ApplicationStatus status,
      String note,
      String created,
      BigDecimal fee) {}

  public record Stock(
      long id,
      String name,
      long categoryId,
      String category,
      String unit,
      int quantity,
      int reorderLevel) {}

  public record Report(List<String> columns, List<List<Object>> rows) {}
}
