package com.mycompany.petadoption.service;

import com.mycompany.petadoption.config.Database;
import com.mycompany.petadoption.model.Models.*;
import com.mycompany.petadoption.repository.*;
import com.mycompany.petadoption.util.Validation;
import java.sql.SQLException;
import java.util.List;

public final class AdoptionService {
  private final Database db;
  private final AuthService auth;
  private final PetRepository pets = new PetRepository();
  private final AdoptionRepository applications = new AdoptionRepository();

  public AdoptionService(Database db, AuthService auth) {
    this.db = db;
    this.auth = auth;
  }

  public List<AdoptionRequest> list(Session session, String status) throws SQLException {
    return db.transaction(
        c -> {
          User u = auth.require(c, session, null);
          return applications.requests(c, u.role() == Role.ADMIN ? null : u.id(), status);
        });
  }

  public long apply(Session session, long petId, String motivation, String housing, boolean consent)
      throws SQLException {
    if (!consent)
      throw new IllegalArgumentException(
          "Please confirm you can provide ongoing care before applying.");
    String m = Validation.text(motivation, "About your home and experience", 4000),
        h = Validation.text(housing, "Housing and other animals", 1000);
    return db.transaction(
        c -> {
          User u = auth.require(c, session, Role.USER);
          Pet p = pets.find(c, petId, true);
          if (p.status() != PetStatus.AVAILABLE)
            throw new IllegalArgumentException(
                "This pet is no longer available. Please choose another companion.");
          if (applications.requests(c, u.id(), "PENDING").size() > 0)
            throw new IllegalArgumentException(
                "You already have a pending request. Withdraw it or wait for a decision before"
                    + " applying again.");
          return applications.apply(c, u.id(), petId, m, h);
        });
  }

  public void decide(Session session, long id, boolean approve, String note) throws SQLException {
    String n = Validation.text(note, "Decision note", 2000);
    db.transaction(
        c -> {
          User admin = auth.require(c, session, Role.ADMIN);
          // Always lock pet before application: serializes competing approvals and withdrawals.
          AdoptionRequest initial = applications.find(c, id, false);
          Pet pet = pets.find(c, initial.petId(), true);
          AdoptionRequest a = applications.find(c, id, true);
          if (a.status() != ApplicationStatus.PENDING)
            throw new IllegalArgumentException("This request has already been reviewed.");
          if (approve) {
            if (pet.status() != PetStatus.AVAILABLE)
              throw new IllegalArgumentException("Only available pets can be adopted.");
            User applicant = new UserRepository().find(c, a.userId());
            if (!applicant.active())
              throw new IllegalArgumentException("The applicant account is inactive.");
            applications.complete(c, id, pet);
            pets.status(c, pet.id(), PetStatus.ADOPTED);
            applications.closeOthers(c, pet.id(), id, admin.id());
          }
          applications.decision(
              c,
              id,
              approve ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED,
              n,
              admin.id());
          return null;
        });
  }

  public void withdraw(Session session, long id) throws SQLException {
    db.transaction(
        c -> {
          User u = auth.require(c, session, Role.USER);
          AdoptionRequest initial = applications.find(c, id, false);
          pets.find(c, initial.petId(), true);
          AdoptionRequest a = applications.find(c, id, true);
          if (a.userId() != u.id())
            throw new SecurityException("This request belongs to another account.");
          if (a.status() != ApplicationStatus.PENDING)
            throw new IllegalArgumentException("Only pending requests can be withdrawn.");
          applications.decision(
              c, id, ApplicationStatus.WITHDRAWN, "Withdrawn by applicant.", null);
          return null;
        });
  }
}
