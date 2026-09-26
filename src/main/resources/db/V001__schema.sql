CREATE TABLE schema_version(version integer PRIMARY KEY, installed_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE users (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name varchar(100) NOT NULL CHECK(length(trim(name)) > 0),
 email varchar(254) NOT NULL UNIQUE CHECK(email = lower(email)),
 phone varchar(30) NOT NULL,
 password_hash varchar(256) NOT NULL,
 role varchar(10) NOT NULL CHECK(role IN ('ADMIN','USER')),
 active boolean NOT NULL DEFAULT true,
 failed_attempts integer NOT NULL DEFAULT 0,
 locked_until timestamptz,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE species(id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, name varchar(80) NOT NULL UNIQUE);
CREATE TABLE breeds(id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, species_id bigint NOT NULL REFERENCES species(id), name varchar(100) NOT NULL, UNIQUE(species_id,name));
CREATE TABLE pets (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name varchar(100) NOT NULL, breed_id bigint NOT NULL REFERENCES breeds(id),
 birth_date date NOT NULL CHECK(birth_date >= DATE '1980-01-01'),
 sex varchar(10) NOT NULL CHECK(sex IN ('Male','Female','Unknown')),
 description varchar(4000) NOT NULL, image_path varchar(255) NOT NULL DEFAULT '',
 fee numeric(12,2) NOT NULL DEFAULT 0 CHECK(fee >= 0),
 status varchar(12) NOT NULL DEFAULT 'AVAILABLE' CHECK(status IN ('AVAILABLE','ON_HOLD','ADOPTED','ARCHIVED')),
 version integer NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX pets_status_breed ON pets(status,breed_id);
CREATE TABLE adoption_applications (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 user_id bigint NOT NULL REFERENCES users(id), pet_id bigint NOT NULL REFERENCES pets(id),
 motivation varchar(4000) NOT NULL, housing varchar(1000) NOT NULL,
 status varchar(12) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','APPROVED','REJECTED','WITHDRAWN')),
 decision_note varchar(2000) NOT NULL DEFAULT '', reviewed_by bigint REFERENCES users(id),
 created_at timestamptz NOT NULL DEFAULT now(), reviewed_at timestamptz
);
CREATE UNIQUE INDEX one_pending_per_user ON adoption_applications(user_id) WHERE status='PENDING';
CREATE UNIQUE INDEX one_active_pair ON adoption_applications(user_id,pet_id) WHERE status IN ('PENDING','APPROVED');
CREATE INDEX applications_status ON adoption_applications(status,created_at);
CREATE INDEX applications_pet ON adoption_applications(pet_id);
CREATE TABLE adoptions (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 application_id bigint NOT NULL UNIQUE REFERENCES adoption_applications(id),
 pet_id bigint NOT NULL UNIQUE REFERENCES pets(id),
 fee numeric(12,2) NOT NULL CHECK(fee >= 0), adopted_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE inventory_categories(id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, name varchar(80) NOT NULL UNIQUE);
CREATE TABLE inventory (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, name varchar(120) NOT NULL UNIQUE,
 category_id bigint NOT NULL REFERENCES inventory_categories(id), unit varchar(30) NOT NULL,
 quantity integer NOT NULL DEFAULT 0 CHECK(quantity >= 0), reorder_level integer NOT NULL DEFAULT 0 CHECK(reorder_level >= 0)
);
CREATE TABLE inventory_transactions (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, item_id bigint NOT NULL REFERENCES inventory(id),
 actor_id bigint NOT NULL REFERENCES users(id), delta integer NOT NULL CHECK(delta <> 0),
 balance integer NOT NULL CHECK(balance >= 0), reason varchar(500) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX inventory_transactions_item ON inventory_transactions(item_id,created_at);
CREATE TABLE legacy_records (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 source varchar(120) NOT NULL, source_key varchar(128) NOT NULL,
 payload text NOT NULL, imported_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(source,source_key)
);
INSERT INTO schema_version(version) VALUES (1);
