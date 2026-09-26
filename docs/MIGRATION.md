# Derby to PostgreSQL migration

## What was actually available

The supplied project has no Derby database directory, schema SQL or database export. No Derby server was listening at the original port. The old query vocabulary is known; exact column types, keys, indexes and live row counts are not. Do not describe inferred schema as a verified catalog.

| Legacy source | Observed columns / meaning | Destination strategy |
|---|---|---|
| CUSTOMERS | CUSTOMERNAME, TEL, PET preference | Stage raw rows; verify identity/email and enroll new accounts. Phone possession is not authentication. |
| STAFF | WORKERID, WORKERNAME | Stage raw rows; verify staff roles and issue new credentials. IDs must never become passwords. |
| PET | PET_ID, BREED, QTY, PRICE | Stage aggregate quantities; reconcile into identifiable animals with shelter records. Never manufacture names, ages or histories. |
| STOCKS | W_NAME, BREED, QTY, CHANGE, DATE | Preserve historical pet-stock evidence in staging, separate from supply inventory. |
| AdoptionDetails.txt | Unstructured comma-separated receipt lines | Preserve full raw lines with deterministic keys. Commas and repeated confirmations make automatic adoption matching unsafe. |

There are no observed account password columns to hash in place. If a real export contains additional password columns, stop the account import, inventory the format, and use forced credential enrollment/reset. Do not import plaintext or assign a common password. Original database credentials were exposed in source; an owner should rotate those separately if the old database still exists.

## PostgreSQL schema

`V001__schema.sql` creates users; species; breeds; pets; adoption_applications; adoptions; inventory_categories; inventory; inventory_transactions; legacy_records; schema_version. It uses generated bigint identities, FK relationships, unique emails/catalog entries, constrained role/status values, non-negative numeric checks, timestamps and lookup indexes. Fees use exact decimals. Demo SQL is not a production migration.

## Operator procedure

1. Back up the Derby database and adoption log. Record source version/schema, table counts and export checksums. Preserve the source unchanged.
2. Create a fresh PostgreSQL database; run `--migrate`. Keep `--demo` confined to the review database.
3. For the log, run `java -jar target/PetAdoption-1.0-SNAPSHOT.jar --check-log PATH` and then `--import-log PATH`. Imports are one transaction; repeating an unchanged source inserts zero additional records. An edited/reordered log must be treated as a new reconciliation source because line ordinals participate in its keys.
4. For Derby, add its matching JDBC client driver to the migration classpath (it is intentionally absent from the production JAR). Set `LEGACY_DB_URL`, `LEGACY_DB_USER`, `LEGACY_DB_PASSWORD`; run `java -cp "target/PetAdoption-1.0-SNAPSHOT.jar;PATH_TO_DERBY_DRIVER" com.mycompany.petadoption.Application --check-derby`, then `--import-derby`. On Unix use `:` rather than `;` as the classpath separator. The legacy login's default schema must contain the four observed tables.
5. The source connection is read-only with a consistent serializable read. Target staging is transactional. The importer preserves every column as JSON string/null, including unexpected additional columns. Identical duplicate rows receive distinct occurrence keys. Counts are reported without printing customer/staff data. Repeated unchanged imports insert zero duplicates.
6. Compare source counts with `SELECT source,count(*) FROM legacy_records GROUP BY source`. Review through Admin → Reports → Legacy records. Validate phones, duplicate staff/customer identities, breed spelling, quantity totals, fee units and dates. Reconcile historical adoptions manually before claiming a production cutover.
7. Enroll verified users, create confirmed individual animals, establish current supply balances with ledger reasons, and archive the signed reconciliation record. Back up PostgreSQL and managed images. Keep Derby read-only until reconciliation and user acceptance finish.

## Rollback and status

The importer never writes to Derby. A failed target transaction rolls back completely. Rollback of a trial migration means switching back to the preserved source and restoring the target backup, not deleting source evidence. The application does not offer a destructive migration-reset button.

The PostgreSQL schema and demo installation were applied successfully to the isolated local review database. The supplied adoption log's 26 lines were staged without changing their text; repeating the import inserted zero additional rows. Line endings are normalized by the line reader; this is not a byte-for-byte file archive (the original file remains preserved). Actual Derby connectivity, schema compatibility and record reconciliation cannot be verified until the source is supplied. No completed Derby cutover is claimed.
