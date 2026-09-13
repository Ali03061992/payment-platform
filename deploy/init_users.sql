-- Insert users (without role column)
INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES (1, 'system.admin', 'admin@system.com', '$2a$10$X8qZ9vJ7wR5kL3mN2pQ1uO6yT8rE4wA3sD5fG7hJ9kL1mN3pQ5rT', 'System', 'Admin', '+21600000000', NULL, 'ACTIVE', 0, NOW(), NOW());

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES (2, 'covale.admin', 'admin@covale.com', '$2a$10$X8qZ9vJ7wR5kL3mN2pQ1uO6yT8rE4wA3sD5fG7hJ9kL1mN3pQ5rT', 'Covale', 'Admin', '+21611111111', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(), NOW());

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES (3, 'pointteck.admin', 'admin@pointteck.com', '$2a$10$X8qZ9vJ7wR5kL3mN2pQ1uO6yT8rE4wA3sD5fG7hJ9kL1mN3pQ5rT', 'Pointteck', 'Admin', '+21622222222', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(), NOW());

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES (4, 'covale.agent1', 'agent1@covale.com', '$2a$10$X8qZ9vJ7wR5kL3mN2pQ1uO6yT8rE4wA3sD5fG7hJ9kL1mN3pQ5rT', 'Agent', 'One', '+21611111112', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(), NOW());

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES (5, 'covale.agent2', 'agent2@covale.com', '$2a$10$X8qZ9vJ7wR5kL3mN2pQ1uO6yT8rE4wA3sD5fG7hJ9kL1mN3pQ5rT', 'Agent', 'Two', '+21611111113', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(), NOW());

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES (6, 'pointteck.agent1', 'agent1@pointteck.com', '$2a$10$X8qZ9vJ7wR5kL3mN2pQ1uO6yT8rE4wA3sD5fG7hJ9kL1mN3pQ5rT', 'Agent', 'One', '+21622222223', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(), NOW());

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES (7, 'pointteck.agent2', 'agent2@pointteck.com', '$2a$10$X8qZ9vJ7wR5kL3mN2pQ1uO6yT8rE4wA3sD5fG7hJ9kL1mN3pQ5rT', 'Agent', 'Two', '+21622222224', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(), NOW());

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES (8, 'abdelslam', 'abdelslam@tunis-soussa.com', '$2a$10$X8qZ9vJ7wR5kL3mN2pQ1uO6yT8rE4wA3sD5fG7hJ9kL1mN3pQ5rT', 'Abdel', 'Slam', '+21633333331', '20000000-0000-0000-0000-000000000002', 'ACTIVE', 0, NOW(), NOW());

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES (9, 'ali', 'ali@sfax-mahdiya.com', '$2a$10$X8qZ9vJ7wR5kL3mN2pQ1uO6yT8rE4wA3sD5fG7hJ9kL1mN3pQ5rT', 'Ali', 'Ben', '+21633333332', '20000000-0000-0000-0000-000000000002', 'ACTIVE', 0, NOW(), NOW());

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES (10, 'pointteck.tunis.admin', 'admin@pointteck-tunis.com', '$2a$10$X8qZ9vJ7wR5kL3mN2pQ1uO6yT8rE4wA3sD5fG7hJ9kL1mN3pQ5rT', 'Admin', 'Tunis', '+21644444441', '20000000-0000-0000-0000-000000000002', 'ACTIVE', 0, NOW(), NOW());

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES (11, 'pointteck.sfax.admin', 'admin@pointteck-sfax.com', '$2a$10$X8qZ9vJ7wR5kL3mN2pQ1uO6yT8rE4wA3sD5fG7hJ9kL1mN3pQ5rT', 'Admin', 'Sfax', '+21644444442', '20000000-0000-0000-0000-000000000002', 'ACTIVE', 0, NOW(), NOW());

-- Assign roles via user_roles
INSERT INTO user_roles (user_id, role_code) VALUES (1, 'SYSTEM_ADMIN');
INSERT INTO user_roles (user_id, role_code) VALUES (2, 'SUPPLIER_ADMIN');
INSERT INTO user_roles (user_id, role_code) VALUES (3, 'SUPPLIER_ADMIN');
INSERT INTO user_roles (user_id, role_code) VALUES (4, 'SUPPLIER_AGENT');
INSERT INTO user_roles (user_id, role_code) VALUES (5, 'SUPPLIER_AGENT');
INSERT INTO user_roles (user_id, role_code) VALUES (6, 'SUPPLIER_AGENT');
INSERT INTO user_roles (user_id, role_code) VALUES (7, 'SUPPLIER_AGENT');
INSERT INTO user_roles (user_id, role_code) VALUES (8, 'SHOP_ADMIN');
INSERT INTO user_roles (user_id, role_code) VALUES (9, 'SHOP_ADMIN');
INSERT INTO user_roles (user_id, role_code) VALUES (10, 'SHOP_ADMIN');
INSERT INTO user_roles (user_id, role_code) VALUES (11, 'SHOP_ADMIN');

SELECT u.id, u.username, u.organization_id, GROUP_CONCAT(ur.role_code) as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
GROUP BY u.id, u.username, u.organization_id;
