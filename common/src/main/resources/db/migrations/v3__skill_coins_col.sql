ALTER TABLE auraskills_users
    ADD COLUMN skill_coins DOUBLE
        NOT NULL
        DEFAULT 0.0;
