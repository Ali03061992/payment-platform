-- Remove SALES role and related permissions (replaced by SYSTEM_ADMIN managing all accounts)

DELETE FROM role_permissions WHERE role_code = 'SALES';
DELETE FROM roles WHERE code = 'SALES';
DELETE FROM permissions WHERE code = 'SALES_MANAGE_ACCOUNTS';
