package com.mycompany.petadoption.repository;

import static com.mycompany.petadoption.repository.Sql.*;

import com.mycompany.petadoption.model.Models.*;
import java.sql.*;
import java.util.*;

public final class PetRepository {
  private static final String SELECT =
      "SELECT p.*,b.name breed,s.name species FROM pets p JOIN breeds b ON b.id=p.breed_id JOIN"
          + " species s ON s.id=b.species_id ";

  private Pet map(ResultSet r) throws SQLException {
    return new Pet(
        r.getLong("id"),
        r.getString("name"),
        r.getLong("breed_id"),
        r.getString("species"),
        r.getString("breed"),
        r.getDate("birth_date").toLocalDate(),
        r.getString("sex"),
        r.getString("description"),
        r.getString("image_path"),
        r.getBigDecimal("fee"),
        PetStatus.valueOf(r.getString("status")),
        r.getInt("version"));
  }

  public List<Pet> search(Connection c, String term, Long speciesId, Long breedId, String status)
      throws SQLException {
    return list(
        c,
        SELECT
            + "WHERE (lower(p.name) LIKE ? OR lower(b.name) LIKE ?) AND (?::bigint IS NULL OR"
            + " s.id=?) AND (?::bigint IS NULL OR b.id=?) AND (?::text IS NULL OR p.status=?) ORDER"
            + " BY p.created_at DESC,p.id DESC",
        this::map,
        "%" + term.toLowerCase(Locale.ROOT) + "%",
        "%" + term.toLowerCase(Locale.ROOT) + "%",
        speciesId,
        speciesId,
        breedId,
        breedId,
        status,
        status);
  }

  public Pet find(Connection c, long id, boolean lock) throws SQLException {
    return list(c, SELECT + "WHERE p.id=?" + (lock ? " FOR UPDATE OF p" : ""), this::map, id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("This pet no longer exists."));
  }

  public long save(Connection c, Long id, int version, PetInput p) throws SQLException {
    if (id == null)
      return Sql.id(
          c,
          "INSERT INTO pets(name,breed_id,birth_date,sex,description,image_path,fee,status) VALUES"
              + " (?,?,?,?,?,?,?,?) RETURNING id",
          p.name(),
          p.breedId(),
          p.birthDate(),
          p.sex(),
          p.description(),
          p.imagePath(),
          p.fee(),
          p.status().name());
    int changed =
        update(
            c,
            "UPDATE pets SET"
                + " name=?,breed_id=?,birth_date=?,sex=?,description=?,image_path=?,fee=?,status=?,version=version+1,updated_at=now()"
                + " WHERE id=? AND version=?",
            p.name(),
            p.breedId(),
            p.birthDate(),
            p.sex(),
            p.description(),
            p.imagePath(),
            p.fee(),
            p.status().name(),
            id,
            version);
    if (changed != 1)
      throw new IllegalArgumentException(
          "This pet changed since you opened it. Refresh and try again.");
    return id;
  }

  public void status(Connection c, long id, PetStatus status) throws SQLException {
    update(
        c,
        "UPDATE pets SET status=?,version=version+1,updated_at=now() WHERE id=?",
        status.name(),
        id);
  }

  public List<Choice> species(Connection c) throws SQLException {
    return list(
        c,
        "SELECT * FROM species ORDER BY name",
        r -> new Choice(r.getLong("id"), r.getString("name")));
  }

  public List<Breed> breeds(Connection c) throws SQLException {
    return list(
        c,
        "SELECT b.*,s.name species FROM breeds b JOIN species s ON s.id=b.species_id ORDER BY"
            + " s.name,b.name",
        r ->
            new Breed(
                r.getLong("id"),
                r.getLong("species_id"),
                r.getString("species"),
                r.getString("name")));
  }

  public void addSpecies(Connection c, String name) throws SQLException {
    update(c, "INSERT INTO species(name) VALUES (?)", name);
  }

  public void addBreed(Connection c, long species, String name) throws SQLException {
    update(c, "INSERT INTO breeds(species_id,name) VALUES (?,?)", species, name);
  }
}
