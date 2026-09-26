# Initial Git history

This repository is a logical import of an already implemented application. These commits organize the current files by responsibility and dependency; they do not claim to reconstruct the application's earlier development timeline. All commits use the real time at which they are created. No dates are backdated or overridden, and no classes are split into fabricated stages.

Each staged snapshot is compiled independently before its commit. Early snapshots are configuration/library stages; the executable application is assembled in commit 12. Unit tests are included in commit 13, with PostgreSQL/GUI validation performed against the complete project before publication. Local credentials, the original snapshot, original adoption log, generated output and database files are excluded.

## 1. `chore: initialize Maven project and repository safeguards`

Build configuration, portable NetBeans actions and exclusions.

- `.gitignore`
- `.gitattributes`
- `pom.xml`
- `nbactions.xml`
- `config/application.example.properties`

## 2. `feat: add domain models and input validation`

Immutable domain records and shared input rules.

- `src/main/java/com/mycompany/petadoption/model/Models.java`
- `src/main/java/com/mycompany/petadoption/util/Validation.java`

## 3. `feat: configure PostgreSQL schema and JDBC transactions`

Connection configuration, schema and scoped JDBC infrastructure.

- `src/main/java/com/mycompany/petadoption/config/AppConfig.java`
- `src/main/java/com/mycompany/petadoption/config/Database.java`
- `src/main/java/com/mycompany/petadoption/repository/Sql.java`
- `src/main/resources/db/V001__schema.sql`

## 4. `feat: add repositories for shelter records and reports`

Persistence for accounts, animals, applications, inventory and report queries.

- `src/main/java/com/mycompany/petadoption/repository/UserRepository.java`
- `src/main/java/com/mycompany/petadoption/repository/PetRepository.java`
- `src/main/java/com/mycompany/petadoption/repository/AdoptionRepository.java`
- `src/main/java/com/mycompany/petadoption/repository/InventoryRepository.java`
- `src/main/java/com/mycompany/petadoption/repository/ReportRepository.java`

## 5. `feat: implement password authentication and role checks`

Authentication, authorization, opaque sessions and password hashing.

- `src/main/java/com/mycompany/petadoption/service/AuthService.java`
- `src/main/java/com/mycompany/petadoption/service/Session.java`
- `src/main/java/com/mycompany/petadoption/util/Passwords.java`

## 6. `feat: implement pet management and adoption decisions`

Pet lifecycle, application rules and atomic decisions.

- `src/main/java/com/mycompany/petadoption/service/PetService.java`
- `src/main/java/com/mycompany/petadoption/service/AdoptionService.java`

## 7. `feat: add administration services and safe report exports`

Account and stock administration, scoped reports and safe CSV encoding.

- `src/main/java/com/mycompany/petadoption/service/AdminService.java`
- `src/main/java/com/mycompany/petadoption/util/Csv.java`

## 8. `feat: add migration tools and local database setup`

Explicit migration/demo setup and portable operational scripts.

- `src/main/java/com/mycompany/petadoption/config/DemoSetup.java`
- `src/main/java/com/mycompany/petadoption/config/LegacyImport.java`
- `src/main/resources/db/demo.sql`
- `scripts/grant-runtime.sql`
- `scripts/prepare-review.ps1`
- `scripts/run.ps1`
- `scripts/start-local-db.ps1`
- `scripts/stop-local-db.ps1`

## 9. `style: add shared Swing theme and portable resources`

Reusable presentation components, localization, packaged images and managed uploads.

- `src/main/java/com/mycompany/petadoption/ui/Theme.java`
- `src/main/java/com/mycompany/petadoption/ui/WrapLayout.java`
- `src/main/java/com/mycompany/petadoption/ui/Ui.java`
- `src/main/java/com/mycompany/petadoption/ui/Strings.java`
- `src/main/java/com/mycompany/petadoption/ui/PhotoPanel.java`
- `src/main/java/com/mycompany/petadoption/util/Images.java`
- `src/main/resources/i18n/Messages.properties`
- `src/main/resources/i18n/Messages_fr.properties`
- `src/main/resources/images/american-shorthair.jpg`
- `src/main/resources/images/british-shorthair.jpg`
- `src/main/resources/images/dachshund.jpg`
- `src/main/resources/images/labrador.jpg`
- `src/main/resources/images/persian.jpg`
- `src/main/resources/images/rottweiler.jpg`
- `src/main/resources/images/shihtzu.jpg`
- `src/main/resources/images/welcome.jpg`

## 10. `feat: add inventory account and reporting screens`

Administrator inventory/account interfaces and reusable report previews.

- `src/main/java/com/mycompany/petadoption/ui/InventoryPanel.java`
- `src/main/java/com/mycompany/petadoption/ui/UsersPanel.java`
- `src/main/java/com/mycompany/petadoption/ui/ReportsPanel.java`

## 11. `feat: add authentication pet browsing and application screens`

Authentication/profile forms, browsing/editing and application views.

- `src/main/java/com/mycompany/petadoption/ui/AuthPanel.java`
- `src/main/java/com/mycompany/petadoption/ui/ProfilePanel.java`
- `src/main/java/com/mycompany/petadoption/ui/PetsPanel.java`
- `src/main/java/com/mycompany/petadoption/ui/PetEditor.java`
- `src/main/java/com/mycompany/petadoption/ui/RequestsPanel.java`

## 12. `feat: connect role dashboards and application launchers`

Assembles the modules into an executable application with role navigation.

- `src/main/java/com/mycompany/petadoption/ui/MainFrame.java`
- `src/main/java/com/mycompany/petadoption/Application.java`
- `src/main/java/com/mycompany/petadoption/LoginForm.java`

## 13. `test: cover validation PostgreSQL workflows and Swing navigation`

Unit, PostgreSQL and real Swing workflow verification.

- `src/test/java/com/mycompany/petadoption/GuiSmoke.java`
- `src/test/java/com/mycompany/petadoption/PostgresWorkflowTest.java`
- `src/test/java/com/mycompany/petadoption/ValidationTest.java`

## 14. `docs: document architecture setup migration and initial history`

Public setup/architecture/migration documentation and historical review evidence.

- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/ASSETS.md`
- `docs/AUDIT.md`
- `docs/FILE_CHANGES.md`
- `docs/FINAL_REVIEW.md`
- `docs/MIGRATION.md`
- `docs/SCREENSHOTS.md`
- `docs/COMMIT_PLAN.md`
- `docs/screenshots/application-review.png`
- `docs/screenshots/browse.png`
- `docs/screenshots/dashboard.png`
- `docs/screenshots/details.png`
- `docs/screenshots/login.png`
- `docs/screenshots/receipt.png`

The original modernization audit, file manifest and final review are archived records of the pre-publication state. References there to a missing Git repository or uncommitted work describe that earlier review, not the published repository's current status.
