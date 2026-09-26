# Pre-modernization audit

> Historical audit captured before the redesign and before repository initialization. The later initial-history organization is documented in [COMMIT_PLAN.md](COMMIT_PLAN.md).

Inspected 14 Java files, 12 NetBeans form descriptors, three bundles, Maven/NetBeans configuration and the legacy adoption-log format. Clean Java 21 compilation succeeds; there are no tests. No Git repository, schema DDL, or Derby database was supplied. PostgreSQL 18 is installed. Derby is not listening on port 1527. Database findings below are inferred from queries, not verified against a live Derby catalog.

## 1. Current project summary
Java 21, Maven, Swing/NetBeans, Derby client 10.2.2.0. Four overlapping PDF dependencies are imported but the active print workflow uses a screenshot. Assets live outside the project. English/French bundles exist. No persistent authentication/session model.

## 2. Architecture and navigation map
`Lang -> LoginForm -> Customer | Sign_Up | Staff`.
`Customer/Sign_Up -> PupReg -> Dogs -> AdoptionReport`; `Cats` is an image-only dead end.
`Staff -> Pet_Manage | DB_Manage | Inventory`.
Frames directly execute SQL on Swing's event thread. PetAdoption is a receipt DTO despite being configured as Maven's executable main class. NetBeans instead launches Lang. trytimedate is an unreferenced console experiment.

## 3–7. Findings
| Severity | File / method | Problem and consequence | Correction |
|---|---|---|---|
| CRITICAL | Staff.jButton1ActionPerformed; Customer.jButton1ActionPerformed | Worker ID / phone number grants access without a secret; customer login even mutates data. | Password hashing, authenticated sessions, read-only login. |
| CRITICAL | DB_Manage, Pet_Manage, Inventory constructors/main | Direct invocation bypasses staff check. | Enforce current account role inside every protected service operation. |
| CRITICAL | All JDBC forms | Repeated literal database credentials disclose a reusable secret. | External ignored configuration; rotate legacy password outside this project. |
| HIGH | Dogs adoption handlers | Read/decrement/write without transaction or lock; stock decreases before receipt confirmation, allowing lost updates and incomplete adoptions. | Atomic approval transaction, row locks and unique adoption constraint. |
| HIGH | Dogs.jButton3ActionPerformed | Reads DASHSHUND but updates DASHSHUNDS; ignores update count. | Stable pet IDs and checked writes. |
| HIGH | Pet_Manage add/remove handlers | Quantity update and STOCKS insert are separate commits. | Atomic inventory adjustment and ledger. |
| HIGH | AdoptionReport confirmation/print | Plaintext local adoption log is the only completion record; repeated confirmation duplicates it; empty catches conceal failure. | Durable applications/adoptions; file chooser export and printer dialog. |
| HIGH | JDBC forms load/search/handlers | Connections/results/statements frequently leak, exceptions expose SQL or vanish. | Try-with-resources, sanitized errors, transaction rollback. |
| HIGH | Schema inferred from PET/CUSTOMERS/STAFF/STOCKS | No supplied constraints/DDL; counts by breed cannot identify an individual animal; historical log lacks stable identifiers. | Explicit normalized schema; preserve legacy records in staging and reconcile identities rather than fabricate pets. |
| HIGH | Cats constructor | No browsing or adoption controls. | Shared species-neutral browsing/details/application workflow. |
| MEDIUM | DB_Manage search/remove | LIKE on potentially numeric IDs can fail; wildcard deletion may remove multiple staff. | Typed IDs, exact selected-row updates, archive users. |
| MEDIUM | Pet_Manage search | LIKE on PET_ID may fail for numeric schema; broad catches hide problems. | Search named text columns and selected records. |
| MEDIUM | Sign_Up.isNumeric / numeric handlers | Digit-only checks do not prevent overflow; whitespace and sizes are inconsistent. | Central validation plus database checks. |
| MEDIUM | All JFrame layouts / form descriptors | Absolute bounds, 1500x1000 preference, inconsistent colors/contrast, clickable labels, multiple windows and EXIT_ON_CLOSE. | Responsive layouts, one shell, accessible buttons and consistent theme. |
| MEDIUM | All database events | Blocking JDBC on event thread freezes interaction. | Background workers, loading/error states and disabled in-flight actions. |
| MEDIUM | Dogs / PupReg / bundles / combo models | Breeds, prices, traits and five-week/male policy encoded as UI text. | Database catalog and explicit configurable policy; never treat old marketing text as verified animal data. |
| MEDIUM | All image paths; AdoptionReport print | User-specific absolute paths break portability and overwrite one desktop file. | Packaged resources, managed uploads and user-selected output. |
| MEDIUM | PupReg / AdoptionReport timers | Timers continue on hidden frames; hidden windows accumulate. | Single shell and lifecycle-managed components. |
| MEDIUM | Dogs default constructor / receipt constructor / combos | Null customer context or missing selected item can break handlers; report accepts unchecked DTO. | Typed session/domain objects, empty selection guards. |
| LOW | pom.xml / nbactions.xml | Main class mismatch; obsolete duplicate PDF stacks. | One real entry point, pinned minimal dependencies and executable JAR. |
| LOW | trytimedate / generated handlers / Inventory commented method | Dead experiment, empty generated callbacks, duplicate stock/adoption handlers, raw vectors and positional columns. | Remove superseded code after replacement; typed models and shared repositories. |

Prepared statements are already used for most dynamic queries: no confirmed concatenated-input SQL injection was found. Fixed Statement queries are not themselves injection findings. Schema constraints cannot be declared absent without the source database.

## 8. Target architecture
`Application -> ui -> service -> repository -> config.Database -> PostgreSQL` with immutable domain records and shared validation/password/export utilities. Swing + FlatLaf retains desktop deployment. Role checks use opaque sessions and re-read active account roles; UI visibility is not the authorization boundary. Desktop database credentials remain a trusted-workstation boundary, documented explicitly.

## 9. Migration plan
Create versioned PostgreSQL schema for users, catalog, individual pets, applications, adoptions, inventory and its ledger, plus legacy staging. Keep demo SQL separate and opt-in. No password fields were found in old user/staff queries: do not use phone/worker IDs as passwords or silently activate imported accounts. Preserve source data; require verified enrollment. PET quantities cannot safely become named animals: stage rows and reconcile manually. STOCKS remains historical pet-stock evidence, not invented supplies. Preserve raw adoption-log lines for reconciliation. Actual Derby transfer remains pending until a source database/export is supplied; implement import tooling and dry-run/reconciliation documentation.

## 10. UI redesign plan
Warm ivory surfaces, forest green primary, muted terracotta accents; consistent spacing, cards, tables, field labels, status feedback. Unified login/register, role dashboard, image cards/search/species/breed filtering, pet details/application, requests and profile. Admin pet editor/archive/catalog, application decisions, account management, supply stock/ledger, legacy stock history and selectable printable/exportable reports. Retain language selection for translated entry/navigation text; report translation gaps honestly.

## 11. Incremental phases / proposed commits (not executed)
1. `chore: document audit and establish portable project configuration`
2. `feat: add PostgreSQL schema and legacy migration tooling`
3. `refactor: separate persistence and transactional domain services`
4. `fix: enforce password authentication and role authorization`
5. `style: introduce shared theme and redesign authentication`
6. `feat: add pet browsing and adoption request workflows`
7. `feat: redesign administration inventory and reporting`
8. `test: verify services transactions and role boundaries`
9. `docs: document setup migration and final review`

Original files are preserved in ignored `.local/baseline` because no Git baseline exists. No commits, history changes or pushes are authorized. Each phase will compile before proceeding.
