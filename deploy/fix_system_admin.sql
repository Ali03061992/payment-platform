-- Fix system.admin password hash
UPDATE users 
SET password_hash = '$2a$12$9TeAQ3fusKYnemCXSAtwje7Gv5QeHcgapYuVqCrt84.aSPhxagXkq'
WHERE username = 'system.admin';

SELECT id, username, password_hash, organization_id FROM users WHERE username='system.admin';