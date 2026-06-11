ALTER TABLE transactions
    ADD COLUMN auto_categorized BOOLEAN NOT NULL DEFAULT false;
