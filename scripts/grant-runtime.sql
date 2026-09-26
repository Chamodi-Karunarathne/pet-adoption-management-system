-- Run as schema owner using psql -v runtime_user=YOUR_EXISTING_LOGIN -f scripts/grant-runtime.sql
-- Create the login separately and set its password interactively with psql \password.
GRANT USAGE ON SCHEMA public TO :"runtime_user";
GRANT SELECT ON schema_version TO :"runtime_user";
GRANT SELECT, INSERT, UPDATE ON users, species, breeds, pets, adoption_applications,
 adoptions, inventory_categories, inventory, inventory_transactions TO :"runtime_user";
GRANT SELECT ON legacy_records TO :"runtime_user";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO :"runtime_user";
