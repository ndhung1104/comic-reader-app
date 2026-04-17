ALTER TABLE users
    ADD COLUMN date_of_birth DATE;

ALTER TABLE users
    ADD COLUMN language_code VARCHAR(10);

ALTER TABLE users
    ADD COLUMN preferred_genres TEXT;
