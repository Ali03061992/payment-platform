CREATE TABLE IF NOT EXISTS family_categories (
    family_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (family_id, category_id),
    CONSTRAINT fk_fc_family FOREIGN KEY (family_id) REFERENCES product_families(id),
    CONSTRAINT fk_fc_category FOREIGN KEY (category_id) REFERENCES product_categories(id)
);
