-- V8: Make category_id nullable in product_families.
-- V3 made it NOT NULL, but V7 added family_categories join table (many-to-many).
-- The JPA entity uses the join table, so category_id is now obsolete.

ALTER TABLE product_families
  DROP FOREIGN KEY fk_family_category;

ALTER TABLE product_families
  MODIFY COLUMN category_id VARCHAR(36) NULL;
