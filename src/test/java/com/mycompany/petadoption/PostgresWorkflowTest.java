package com.mycompany.petadoption;

import static org.junit.jupiter.api.Assertions.*;

import com.mycompany.petadoption.config.*;
import com.mycompany.petadoption.model.Models.*;
import com.mycompany.petadoption.repository.*;
import com.mycompany.petadoption.service.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

class PostgresWorkflowTest {
  static Database db, base;
  static String schema;
  static AuthService auth;
  static PetService pets;
  static AdoptionService adoptions;
  static AdminService admin;
  Session staff, user, other;
  long petId, secondPet;
  static final String PASSWORD = "Integration test password!";

  @BeforeAll
  static void setup() throws Exception {
    Assumptions.assumeTrue(
        Boolean.getBoolean("pet.integration"),
        "Enable with -Dpet.integration=true and local PostgreSQL configuration");
    AppConfig cfg = AppConfig.load();
    String url = cfg.url();
    if (!url.endsWith("/petadoption_review"))
      throw new IllegalArgumentException(
          "Integration tests require the isolated local review setup; see README.");
    base =
        new Database(
            new AppConfig(
                url.replace("/petadoption_review", "/petadoption_test"),
                cfg.user(),
                cfg.password(),
                cfg.currency()));
    schema = "test_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection c = base.open()) {
      Sql.update(c, "CREATE SCHEMA " + schema);
    }
    db =
        new Database(
            new AppConfig(
                url.replace("/petadoption_review", "/petadoption_test")
                    + "?currentSchema="
                    + schema,
                cfg.user(),
                cfg.password(),
                cfg.currency()));
    db.migrate();
    db.migrate();
    auth = new AuthService(db);
    pets = new PetService(db, auth);
    adoptions = new AdoptionService(db, auth);
    admin = new AdminService(db, auth);
  }

  @AfterAll
  static void cleanup() throws Exception {
    if (base != null && schema != null && schema.matches("test_[a-f0-9]{32}"))
      try (Connection c = base.open()) {
        Sql.update(c, "DROP SCHEMA " + schema + " CASCADE");
      }
  }

  @BeforeEach
  void fixtures() throws Exception {
    try (Connection c = db.open()) {
      Sql.update(
          c, "TRUNCATE users,species,inventory_categories,legacy_records RESTART IDENTITY CASCADE");
    }
    auth.bootstrap("Admin", "admin@test.example", "+94 770000001", PASSWORD.toCharArray());
    auth.register("User", "user@test.example", "+94 770000002", PASSWORD.toCharArray());
    auth.register("Other", "other@test.example", "+94 770000003", PASSWORD.toCharArray());
    staff = auth.login("admin@test.example", PASSWORD.toCharArray());
    user = auth.login("user@test.example", PASSWORD.toCharArray());
    other = auth.login("other@test.example", PASSWORD.toCharArray());
    pets.addSpecies(staff, "Dog");
    long species = pets.species(staff).getFirst().id();
    pets.addBreed(staff, species, "Mixed");
    long breed = pets.breeds(staff).getFirst().id();
    petId = pets.save(staff, null, 0, input("Milo", breed, PetStatus.AVAILABLE));
    secondPet = pets.save(staff, null, 0, input("Luna", breed, PetStatus.AVAILABLE));
  }

  PetInput input(String name, long breed, PetStatus status) {
    return new PetInput(
        name,
        breed,
        LocalDate.now().minusYears(2),
        "Unknown",
        "Needs a loving home.",
        "/images/labrador.jpg",
        new BigDecimal("100.00"),
        status);
  }

  @Test
  @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "pet.gui", matches = "true")
  void realSwingScreensOpenAndRespectRoleNavigation() throws Exception {
    GuiSmoke.run(db, PASSWORD);
  }

  long apply(Session s, long id) throws Exception {
    return adoptions.apply(
        s, id, "Experienced carer, daily walks.", "Secure garden, quiet household.", true);
  }

  @Test
  void registrationLoginRoleAndLogoutBoundaries() throws Exception {
    assertEquals(Role.USER, auth.current(user).role());
    assertThrows(
        SQLException.class,
        () ->
            auth.register(
                "Duplicate", "USER@test.example", "+94 777777777", PASSWORD.toCharArray()));
    assertThrows(SecurityException.class, () -> admin.users(user));
    assertThrows(SecurityException.class, () -> pets.addSpecies(user, "Cat"));
    assertThrows(SecurityException.class, () -> admin.report(user, ReportRepository.Type.USERS));
    assertThrows(SecurityException.class, () -> pets.search(null, "", null, null, null));
    auth.logout(user);
    assertThrows(SecurityException.class, () -> auth.current(user));
  }

  @Test
  void loginLockoutAndPasswordChange() throws Exception {
    for (int i = 0; i < 5; i++)
      assertThrows(
          IllegalArgumentException.class,
          () -> auth.login("user@test.example", "incorrect password".toCharArray()));
    assertThrows(
        IllegalArgumentException.class,
        () -> auth.login("user@test.example", PASSWORD.toCharArray()));
    auth.password(other, PASSWORD.toCharArray(), "A different long password".toCharArray());
    assertThrows(
        IllegalArgumentException.class,
        () -> auth.login("other@test.example", PASSWORD.toCharArray()));
    assertNotNull(auth.login("other@test.example", "A different long password".toCharArray()));
  }

  @Test
  void onePendingOwnershipWithdrawAndReject() throws Exception {
    long id = apply(user, petId);
    assertThrows(IllegalArgumentException.class, () -> apply(user, secondPet));
    assertThrows(SecurityException.class, () -> adoptions.withdraw(other, id));
    assertThrows(SecurityException.class, () -> adoptions.decide(user, id, true, "Approved"));
    assertTrue(adoptions.list(other, null).isEmpty());
    adoptions.withdraw(user, id);
    assertEquals(ApplicationStatus.WITHDRAWN, adoptions.list(user, null).getFirst().status());
    long next = apply(user, secondPet);
    adoptions.decide(staff, next, false, "Home needs additional preparation.");
    assertEquals(ApplicationStatus.REJECTED, adoptions.list(user, "REJECTED").getFirst().status());
  }

  @Test
  void approvalIsAtomicAndReceiptIsPrivate() throws Exception {
    long id = apply(user, petId), competing = apply(other, petId);
    adoptions.decide(staff, id, true, "Approved after review.");
    assertEquals(1, admin.receipt(user, id).rows().size());
    assertThrows(IllegalArgumentException.class, () -> admin.receipt(other, id));
    assertEquals(ApplicationStatus.REJECTED, adoptions.list(other, null).getFirst().status());
    assertEquals(1, pets.search(user, "", null, null, null).size());
    assertThrows(
        IllegalArgumentException.class,
        () -> adoptions.decide(staff, competing, true, "Late approval"));
    assertEquals(1, admin.report(staff, ReportRepository.Type.ADOPTIONS).rows().size());
  }

  @Test
  void concurrentApprovalsProduceOneAdoption() throws Exception {
    long first = apply(user, petId), second = apply(other, petId);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<Boolean> a =
          () -> {
            start.await();
            try {
              adoptions.decide(staff, first, true, "Reviewed first applicant");
              return true;
            } catch (IllegalArgumentException e) {
              return false;
            }
          };
      Callable<Boolean> b =
          () -> {
            start.await();
            try {
              adoptions.decide(staff, second, true, "Reviewed second applicant");
              return true;
            } catch (IllegalArgumentException e) {
              return false;
            }
          };
      var f = executor.submit(a);
      var g = executor.submit(b);
      start.countDown();
      assertNotEquals(f.get(20, TimeUnit.SECONDS), g.get(20, TimeUnit.SECONDS));
    }
    assertEquals(1, admin.report(staff, ReportRepository.Type.ADOPTIONS).rows().size());
  }

  @Test
  void inventoryCannotGoNegativeAndLedgerRollsBack() throws Exception {
    admin.category(staff, "Food");
    admin.item(staff, "Kibble", admin.categories(staff).getFirst().id(), "kg", 5);
    long item = admin.inventory(staff).getFirst().id();
    admin.adjust(staff, item, 10, "Opening stock");
    assertThrows(IllegalArgumentException.class, () -> admin.adjust(staff, item, -11, "Too much"));
    assertEquals(10, admin.inventory(staff).getFirst().quantity());
    assertEquals(1, admin.report(staff, ReportRepository.Type.STOCK_HISTORY).rows().size());
    assertThrows(SecurityException.class, () -> admin.adjust(user, item, 5, "Not allowed"));
    admin.adjust(staff, item, -3, "Daily feeding");
    assertEquals(7, admin.inventory(staff).getFirst().quantity());
  }

  @Test
  void staleEditsArchiveAndAccountRevocation() throws Exception {
    Pet p = pets.search(staff, "Milo", null, null, null).getFirst();
    long request = apply(user, petId);
    pets.archive(staff, p);
    assertEquals(ApplicationStatus.REJECTED, adoptions.list(user, null).getFirst().status());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            pets.save(
                staff, p.id(), p.version(), input("Old edit", p.breedId(), PetStatus.AVAILABLE)));
    long uid = auth.current(user).id();
    admin.access(staff, uid, Role.USER, false);
    assertThrows(SecurityException.class, () -> pets.search(user, "", null, null, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> admin.access(staff, auth.current(staff).id(), Role.USER, false));
  }

  @Test
  void legacyStagingIsIdempotentAndPreservesRawLines() throws Exception {
    Path log = Files.createTempFile("woof-legacy", ".txt");
    try {
      Files.writeString(log, "Legacy customer, raw record\nSecond record\n");
      assertEquals(2, LegacyImport.log(db, log, true));
      assertEquals(2, LegacyImport.log(db, log, false));
      assertEquals(0, LegacyImport.log(db, log, false));
      assertEquals(2, admin.report(staff, ReportRepository.Type.LEGACY).rows().size());
    } finally {
      Files.deleteIfExists(log);
    }
  }

  @Test
  void allReportsAndDashboardUseDatabaseRecords() throws Exception {
    for (var type : ReportRepository.Type.values())
      assertFalse(admin.report(staff, type).columns().isEmpty());
    assertEquals(2L, admin.dashboard(staff).get("Available pets"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            pets.save(
                staff,
                null,
                0,
                input("Bad status", pets.breeds(staff).getFirst().id(), PetStatus.ADOPTED)));
    char[] password = "short".toCharArray();
    assertThrows(
        IllegalArgumentException.class,
        () -> auth.register("Bad", "bad@test.example", "+94 770000005", password));
    assertArrayEquals(new char[password.length], password);
  }
}
