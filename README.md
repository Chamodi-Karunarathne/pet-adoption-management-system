# woof. — Pet Adoption

A Java 21 desktop application for adopters and shelter administrators. Swing/FlatLaf views use authenticated services and PostgreSQL repositories. All live catalog, application, inventory and dashboard data is read from the database. Demo data is separate and opt-in.

## Local review on Windows

Requirements: Java 21, Maven 3.9+, PostgreSQL binaries (`psql`, `initdb`, `pg_ctl`) on PATH. This machine has these prerequisites installed.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/prepare-review.ps1
powershell -ExecutionPolicy Bypass -File scripts/run.ps1
```

The review script creates an isolated cluster in ignored `.local/postgres`, bound to `127.0.0.1:55432`, and two databases: `petadoption_review` and `petadoption_test`. It does not change the existing PostgreSQL service. Database and demo passwords are randomly generated, stored only in ignored local files, and never placed in source. Read `.local/review-credentials.txt` to sign in as either role. Do not publish that file or `.local`.

Subsequent starts: run `scripts/start-local-db.ps1` if the local database is stopped, then `scripts/run.ps1`. Stop just the review database with `scripts/stop-local-db.ps1`. No Git operations are performed by these scripts.

## Existing / production PostgreSQL

1. Create an empty database and schema-owner login using PostgreSQL administration tools.
2. Copy `config/application.example.properties` to `config/application.properties` and fill in local values. Alternatively set `PET_DB_URL`, `PET_DB_USER`, `PET_DB_PASSWORD` (these override the file). A different config location can be selected using `-Dpet.config=...`.
3. Run `mvn clean package`, then `java -jar target/PetAdoption-1.0-SNAPSHOT.jar --migrate` as the schema owner. Migration is transactional and repeat-safe. Normal application startup only checks the schema; it never installs demo data or changes DDL.
4. With an empty users table, set `PET_ADMIN_NAME`, `PET_ADMIN_EMAIL`, `PET_ADMIN_PHONE`, `PET_ADMIN_PASSWORD` and run the JAR with `--bootstrap-admin`. Passwords must be 12–128 characters. Remove those environment variables afterward. Public registration always creates a USER.
5. Create a separate runtime database login, then apply `scripts/grant-runtime.sql` as owner using `psql -v runtime_user=your_runtime_login -f scripts/grant-runtime.sql`. Configure the application with that login. Configure TLS (`sslmode=verify-full`) and certificates for a remote database. The isolated local review uses its own owner login for convenience.
6. Launch the JAR. Administrators create species, breeds, pets and inventory categories through the UI. Production starts with no fabricated animals or accounts.

NetBeans run/debug/profile actions and the executable JAR use `Application`. `LoginForm.main` remains a compatibility launcher for Run File. Generated `.form` descriptors were replaced by maintainable Swing layout code.

## Workflows

**Adopter:** choose **User sign-in** → register or sign in → browse/search/filter → inspect pet details → submit an application with care consent → follow its status → withdraw a pending request or view/export/print an approved adoption receipt. Edit name/phone and change password in My profile.

**Administrator:** choose **Admin sign-in** → sign in with an administrator account → review dashboard → manage catalog and individual pets → review application details and approve/reject with a note → manage supply quantities with a reason → manage account roles/activation → generate and export/print reports. Self-demotion/deactivation is disallowed. Pets with completed adoptions cannot be silently returned to availability.

The selected sign-in role must match the account's database role. Choosing Admin does not grant permissions. Public registration creates User accounts only; administrators provision additional administrator accounts through People & access. For the first administrator on a new database, use the bootstrap command below.

One pending application per adopter preserves the legacy one-at-a-time rule. Approval is the completion boundary: it creates an adoption and marks the pet adopted atomically, then rejects competing pending applications for that pet. There is no separate payment or physical-handover subsystem.

Inventory means **supplies/resources** in the new application. Old `STOCKS` rows describe breed-quantity changes and are preserved separately in legacy staging; they are never relabeled as food/supply movements.

## Tests and checks

```powershell
mvn test
# With scripts/start-local-db.ps1 configuration:
mvn test '-Dpet.integration=true'
mvn clean test '-Dpet.integration=true' '-Dpet.gui=true'
mvn package
```

Unit tests run without a database. PostgreSQL tests opt in, use a unique temporary schema in **petadoption_test**, and remove only that schema afterward. They never truncate review or production records. The GUI flag opens real Swing windows and writes screenshots to ignored `.local/screenshots`. Run these checks when you are not interacting with those test windows.

Integration coverage includes login/lockout/password changes, duplicate accounts, role and ownership checks, one-pending rules, concurrent approvals, receipts, rejection/withdrawal/archive, stale edits, stock/ledger consistency, reports and repeat-safe legacy staging.

## Architecture, migration and review

- [Pre-change audit](docs/AUDIT.md)
- [Architecture and design decisions](docs/ARCHITECTURE.md)
- [Legacy migration and reconciliation](docs/MIGRATION.md)
- [Pre-publication modernization review](docs/FINAL_REVIEW.md)
- [Initial Git history and exact file grouping](docs/COMMIT_PLAN.md)
- [Source image provenance](docs/ASSETS.md)

## Scope and deployment boundary

This is a trusted-workstation desktop application. Service checks protect application workflows, but a user who possesses the shared database credentials can bypass Java code with a database client. An internet-facing deployment needs a server API that keeps database credentials away from end users. No web/email identity verification, self-service password recovery, online payments or multi-device image synchronization is included.

English/French entry and navigation labels are supported; extended form/help/error text is currently English. Printing uses the operating system print dialog (choose a PDF printer if installed); there is no bundled PDF engine. Large collections currently load into memory rather than server-side pages. Back up PostgreSQL and the managed image directory together. Uploaded images live in `~/.woof/images` or the `-Dpet.data=...` location; database image references contain only managed relative identifiers.

## Technical references

- [FlatLaf setup and customization](https://www.formdev.com/flatlaf/)
- [PostgreSQL JDBC driver](https://jdbc.postgresql.org/)
- [PostgreSQL row locking](https://www.postgresql.org/docs/current/explicit-locking.html)
- [OWASP password storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) — the JDK implementation here uses PBKDF2-HMAC-SHA256, 600,000 iterations, a random 16-byte salt and constant-time hash comparison. It is not a claim of FIPS certification.
