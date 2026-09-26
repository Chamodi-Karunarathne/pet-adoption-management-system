# File change manifest

Compared against `.local/baseline`, captured before modernization. Generated artifacts, local secrets/database and private review files are excluded.

## Added (71)

- `.gitignore`
- `config/application.example.properties`
- `docs/ARCHITECTURE.md`
- `docs/ASSETS.md`
- `docs/AUDIT.md`
- `docs/FILE_CHANGES.md`
- `docs/FINAL_REVIEW.md`
- `docs/MIGRATION.md`
- `docs/SCREENSHOTS.md`
- `docs/screenshots/application-review.png`
- `docs/screenshots/browse.png`
- `docs/screenshots/dashboard.png`
- `docs/screenshots/details.png`
- `docs/screenshots/login.png`
- `docs/screenshots/receipt.png`
- `README.md`
- `scripts/grant-runtime.sql`
- `scripts/prepare-review.ps1`
- `scripts/run.ps1`
- `scripts/start-local-db.ps1`
- `scripts/stop-local-db.ps1`
- `src/main/java/com/mycompany/petadoption/Application.java`
- `src/main/java/com/mycompany/petadoption/config/AppConfig.java`
- `src/main/java/com/mycompany/petadoption/config/Database.java`
- `src/main/java/com/mycompany/petadoption/config/DemoSetup.java`
- `src/main/java/com/mycompany/petadoption/config/LegacyImport.java`
- `src/main/java/com/mycompany/petadoption/model/Models.java`
- `src/main/java/com/mycompany/petadoption/repository/AdoptionRepository.java`
- `src/main/java/com/mycompany/petadoption/repository/InventoryRepository.java`
- `src/main/java/com/mycompany/petadoption/repository/PetRepository.java`
- `src/main/java/com/mycompany/petadoption/repository/ReportRepository.java`
- `src/main/java/com/mycompany/petadoption/repository/Sql.java`
- `src/main/java/com/mycompany/petadoption/repository/UserRepository.java`
- `src/main/java/com/mycompany/petadoption/service/AdminService.java`
- `src/main/java/com/mycompany/petadoption/service/AdoptionService.java`
- `src/main/java/com/mycompany/petadoption/service/AuthService.java`
- `src/main/java/com/mycompany/petadoption/service/PetService.java`
- `src/main/java/com/mycompany/petadoption/service/Session.java`
- `src/main/java/com/mycompany/petadoption/ui/AuthPanel.java`
- `src/main/java/com/mycompany/petadoption/ui/InventoryPanel.java`
- `src/main/java/com/mycompany/petadoption/ui/MainFrame.java`
- `src/main/java/com/mycompany/petadoption/ui/PetEditor.java`
- `src/main/java/com/mycompany/petadoption/ui/PetsPanel.java`
- `src/main/java/com/mycompany/petadoption/ui/PhotoPanel.java`
- `src/main/java/com/mycompany/petadoption/ui/ProfilePanel.java`
- `src/main/java/com/mycompany/petadoption/ui/ReportsPanel.java`
- `src/main/java/com/mycompany/petadoption/ui/RequestsPanel.java`
- `src/main/java/com/mycompany/petadoption/ui/Strings.java`
- `src/main/java/com/mycompany/petadoption/ui/Theme.java`
- `src/main/java/com/mycompany/petadoption/ui/Ui.java`
- `src/main/java/com/mycompany/petadoption/ui/UsersPanel.java`
- `src/main/java/com/mycompany/petadoption/ui/WrapLayout.java`
- `src/main/java/com/mycompany/petadoption/util/Csv.java`
- `src/main/java/com/mycompany/petadoption/util/Images.java`
- `src/main/java/com/mycompany/petadoption/util/Passwords.java`
- `src/main/java/com/mycompany/petadoption/util/Validation.java`
- `src/main/resources/db/demo.sql`
- `src/main/resources/db/V001__schema.sql`
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
- `src/test/java/com/mycompany/petadoption/GuiSmoke.java`
- `src/test/java/com/mycompany/petadoption/PostgresWorkflowTest.java`
- `src/test/java/com/mycompany/petadoption/ValidationTest.java`

## Modified (3)

- `nbactions.xml`
- `pom.xml`
- `src/main/java/com/mycompany/petadoption/LoginForm.java`

## Removed (28)

- `src/main/java/com/mycompany/petadoption/AdoptionReport.form`
- `src/main/java/com/mycompany/petadoption/AdoptionReport.java`
- `src/main/java/com/mycompany/petadoption/Cats.form`
- `src/main/java/com/mycompany/petadoption/Cats.java`
- `src/main/java/com/mycompany/petadoption/Customer.form`
- `src/main/java/com/mycompany/petadoption/Customer.java`
- `src/main/java/com/mycompany/petadoption/DB_Manage.form`
- `src/main/java/com/mycompany/petadoption/DB_Manage.java`
- `src/main/java/com/mycompany/petadoption/Dogs.form`
- `src/main/java/com/mycompany/petadoption/Dogs.java`
- `src/main/java/com/mycompany/petadoption/Inventory.form`
- `src/main/java/com/mycompany/petadoption/Inventory.java`
- `src/main/java/com/mycompany/petadoption/Lang.form`
- `src/main/java/com/mycompany/petadoption/Lang.java`
- `src/main/java/com/mycompany/petadoption/LoginForm.form`
- `src/main/java/com/mycompany/petadoption/Pet_Manage.form`
- `src/main/java/com/mycompany/petadoption/Pet_Manage.java`
- `src/main/java/com/mycompany/petadoption/PetAdoption.java`
- `src/main/java/com/mycompany/petadoption/PupReg.form`
- `src/main/java/com/mycompany/petadoption/PupReg.java`
- `src/main/java/com/mycompany/petadoption/Sign_Up.form`
- `src/main/java/com/mycompany/petadoption/Sign_Up.java`
- `src/main/java/com/mycompany/petadoption/Staff.form`
- `src/main/java/com/mycompany/petadoption/Staff.java`
- `src/main/java/com/mycompany/petadoption/trytimedate.java`
- `src/main/resources/Bundle.properties`
- `src/main/resources/Bundle_fr_FR.properties`
- `src/main/resources/com/mycompany/petadoption/Bundle.properties`

The original `AdoptionDetails.txt` is unchanged and excluded from future version control.
Original Java/forms/bundles are preserved privately in `.local/baseline`.
