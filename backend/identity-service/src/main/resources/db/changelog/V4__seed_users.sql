--liquibase formatted sql

--changeset platform:4 splitStatements:true

-- password: Admin@123
INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'system.admin', 'admin@system.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'System', 'Admin', '+21600000000', NULL, 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('00000000-0000-0000-0000-000000000001', 'SYSTEM_ADMIN');

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('10000000-0000-0000-0000-000000000001', 'covale.admin', 'admin@covale.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'Covale', 'Admin', '+21611111111', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('10000000-0000-0000-0000-000000000001', 'SUPPLIER_ADMIN');

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('10000000-0000-0000-0000-000000000002', 'pointteck.admin', 'admin@pointteck.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'Pointteck', 'Admin', '+21622222222', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('10000000-0000-0000-0000-000000000002', 'SUPPLIER_ADMIN');

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('10000000-0000-0000-0000-000000000003', 'covale.agent1', 'agent1@covale.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'Agent', 'One', '+21611111112', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('10000000-0000-0000-0000-000000000003', 'SUPPLIER_AGENT');

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('10000000-0000-0000-0000-000000000004', 'covale.agent2', 'agent2@covale.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'Agent', 'Two', '+21611111113', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('10000000-0000-0000-0000-000000000004', 'SUPPLIER_AGENT');

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('10000000-0000-0000-0000-000000000005', 'pointteck.agent1', 'agent1@pointteck.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'Agent', 'One', '+21622222223', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('10000000-0000-0000-0000-000000000005', 'SUPPLIER_AGENT');

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('10000000-0000-0000-0000-000000000006', 'pointteck.agent2', 'agent2@pointteck.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'Agent', 'Two', '+21622222224', '20000000-0000-0000-0000-000000000001', 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('10000000-0000-0000-0000-000000000006', 'SUPPLIER_AGENT');

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('10000000-0000-0000-0000-000000000007', 'abdelslam', 'abdelslam@tunis-soussa.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'Abdel', 'Slam', '+21633333331', '20000000-0000-0000-0000-000000000002', 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('10000000-0000-0000-0000-000000000007', 'SHOP_ADMIN');

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('10000000-0000-0000-0000-000000000008', 'ali', 'ali@sfax-mahdiya.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'Ali', 'Ben', '+21633333332', '20000000-0000-0000-0000-000000000002', 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('10000000-0000-0000-0000-000000000008', 'SHOP_ADMIN');

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('10000000-0000-0000-0000-000000000009', 'pointteck.tunis.admin', 'admin@pointteck-tunis.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'Admin', 'Tunis', '+21644444441', '20000000-0000-0000-0000-000000000002', 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('10000000-0000-0000-0000-000000000009', 'SHOP_ADMIN');

INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone, organization_id, status, version, created_at, updated_at)
VALUES ('10000000-0000-0000-0000-00000000000a', 'pointteck.sfax.admin', 'admin@pointteck-sfax.com', '$2a$12$hTVjZRcL70nRV8Ich6nlR.aPJTGFat7B8wS2jgHta8Hw6q5QGTWfi', 'Admin', 'Sfax', '+21644444442', '20000000-0000-0000-0000-000000000002', 'ACTIVE', 0, NOW(6), NOW(6));
INSERT INTO user_roles (user_id, role_code) VALUES ('10000000-0000-0000-0000-00000000000a', 'SHOP_ADMIN');
