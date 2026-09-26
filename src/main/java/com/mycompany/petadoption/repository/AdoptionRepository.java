package com.mycompany.petadoption.repository;

import static com.mycompany.petadoption.repository.Sql.*;

import com.mycompany.petadoption.model.Models.*;
import java.sql.*;
import java.util.*;

public final class AdoptionRepository {
  private static final String SELECT =
      "SELECT a.*,u.name applicant,u.email,p.name pet,s.name species,p.fee FROM"
          + " adoption_applications a JOIN users u ON u.id=a.user_id JOIN pets p ON p.id=a.pet_id"
          + " JOIN breeds b ON b.id=p.breed_id JOIN species s ON s.id=b.species_id ";

  private AdoptionRequest map(ResultSet r) throws SQLException {
    return new AdoptionRequest(
        r.getLong("id"),
        r.getLong("user_id"),
        r.getLong("pet_id"),
        r.getString("applicant"),
        r.getString("email"),
        r.getString("pet"),
        r.getString("species"),
        r.getString("motivation"),
        r.getString("housing"),
        ApplicationStatus.valueOf(r.getString("status")),
        r.getString("decision_note"),
        r.getTimestamp("created_at").toLocalDateTime().toString(),
        r.getBigDecimal("fee"));
  }

  public List<AdoptionRequest> requests(Connection c, Long userId, String status)
      throws SQLException {
    return list(
        c,
        SELECT
            + "WHERE (?::bigint IS NULL OR a.user_id=?) AND (?::text IS NULL OR a.status=?) ORDER"
            + " BY a.created_at DESC",
        this::map,
        userId,
        userId,
        status,
        status);
  }

  public AdoptionRequest find(Connection c, long id, boolean lock) throws SQLException {
    return list(c, SELECT + "WHERE a.id=?" + (lock ? " FOR UPDATE OF a" : ""), this::map, id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Application not found."));
  }

  public long apply(Connection c, long user, long pet, String motivation, String housing)
      throws SQLException {
    return id(
        c,
        "INSERT INTO adoption_applications(user_id,pet_id,motivation,housing) VALUES (?,?,?,?)"
            + " RETURNING id",
        user,
        pet,
        motivation,
        housing);
  }

  public void decision(Connection c, long id, ApplicationStatus status, String note, Long actor)
      throws SQLException {
    update(
        c,
        "UPDATE adoption_applications SET status=?,decision_note=?,reviewed_by=?,reviewed_at=now()"
            + " WHERE id=?",
        status.name(),
        note,
        actor,
        id);
  }

  public void complete(Connection c, long application, Pet pet) throws SQLException {
    update(
        c,
        "INSERT INTO adoptions(application_id,pet_id,fee) VALUES (?,?,?)",
        application,
        pet.id(),
        pet.fee());
  }

  public void closeOthers(Connection c, long pet, long selected, long actor) throws SQLException {
    update(
        c,
        "UPDATE adoption_applications SET status='REJECTED',decision_note='This pet has been"
            + " adopted by another applicant.',reviewed_by=?,reviewed_at=now() WHERE pet_id=? AND"
            + " id<>? AND status='PENDING'",
        actor,
        pet,
        selected);
  }

  public void archivePending(Connection c, long pet, long actor) throws SQLException {
    update(
        c,
        "UPDATE adoption_applications SET status='REJECTED',decision_note='This pet is no longer"
            + " available for adoption.',reviewed_by=?,reviewed_at=now() WHERE pet_id=? AND"
            + " status='PENDING'",
        actor,
        pet);
  }
}
