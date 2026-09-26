package com.mycompany.petadoption.service;

import com.mycompany.petadoption.config.Database;
import com.mycompany.petadoption.model.Models.*;
import com.mycompany.petadoption.repository.*;
import com.mycompany.petadoption.util.Validation;
import java.sql.SQLException;
import java.util.*;

public final class PetService {
  private final Database db;
  private final AuthService auth;
  private final PetRepository pets = new PetRepository();

  public PetService(Database db, AuthService auth) {
    this.db = db;
    this.auth = auth;
  }

  public List<Pet> search(Session s, String term, Long species, Long breed, String status)
      throws SQLException {
    return db.transaction(
        c -> {
          User u = auth.require(c, s, null);
          return pets.search(
              c,
              Objects.requireNonNullElse(term, ""),
              species,
              breed,
              u.role() == Role.USER ? "AVAILABLE" : status);
        });
  }

  public List<Choice> species(Session s) throws SQLException {
    return db.transaction(
        c -> {
          auth.require(c, s, null);
          return pets.species(c);
        });
  }

  public List<Breed> breeds(Session s) throws SQLException {
    return db.transaction(
        c -> {
          auth.require(c, s, null);
          return pets.breeds(c);
        });
  }

  public void addSpecies(Session s, String name) throws SQLException {
    String n = Validation.text(name, "Species", 80);
    db.transaction(
        c -> {
          auth.require(c, s, Role.ADMIN);
          pets.addSpecies(c, n);
          return null;
        });
  }

  public void addBreed(Session s, long species, String name) throws SQLException {
    String n = Validation.text(name, "Breed", 100);
    db.transaction(
        c -> {
          auth.require(c, s, Role.ADMIN);
          pets.addBreed(c, species, n);
          return null;
        });
  }

  public long save(Session s, Long id, int version, PetInput input) throws SQLException {
    String name = Validation.text(input.name(), "Name", 100),
        description = Validation.text(input.description(), "Description", 4000);
    Validation.birthDate(input.birthDate().toString());
    Validation.money(input.fee().toPlainString());
    if (!Set.of("Male", "Female", "Unknown").contains(input.sex()))
      throw new IllegalArgumentException("Choose a sex.");
    String image = Objects.requireNonNullElse(input.imagePath(), "");
    if (image.length() > 255
        || (!image.isEmpty()
            && !image.matches("/images/[a-zA-Z0-9._-]+")
            && !image.matches("upload:[a-f0-9-]+\\.png")))
      throw new IllegalArgumentException("Choose an image using the image picker.");
    var p =
        new PetInput(
            name,
            input.breedId(),
            input.birthDate(),
            input.sex(),
            description,
            image,
            input.fee(),
            Objects.requireNonNull(input.status()));
    return db.transaction(
        c -> {
          auth.require(c, s, Role.ADMIN);
          if (id == null && p.status() == PetStatus.ADOPTED)
            throw new IllegalArgumentException("Adopt a pet by approving an application.");
          if (id != null) {
            Pet old = pets.find(c, id, true);
            if ((old.status() == PetStatus.ADOPTED) != (p.status() == PetStatus.ADOPTED))
              throw new IllegalArgumentException(
                  "Completed adoption status cannot be changed here.");
          }
          long saved = pets.save(c, id, version, p);
          if (p.status() == PetStatus.ARCHIVED)
            new AdoptionRepository().archivePending(c, saved, auth.require(c, s, Role.ADMIN).id());
          return saved;
        });
  }

  public void archive(Session s, Pet pet) throws SQLException {
    save(
        s,
        pet.id(),
        pet.version(),
        new PetInput(
            pet.name(),
            pet.breedId(),
            pet.birthDate(),
            pet.sex(),
            pet.description(),
            pet.imagePath(),
            pet.fee(),
            PetStatus.ARCHIVED));
  }
}
