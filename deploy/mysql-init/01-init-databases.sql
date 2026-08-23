CREATE DATABASE IF NOT EXISTS identity_db;
CREATE DATABASE IF NOT EXISTS organization_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS notification_db;

GRANT ALL PRIVILEGES ON identity_db.* TO 'payment_app'@'%';
GRANT ALL PRIVILEGES ON organization_db.* TO 'payment_app'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'payment_app'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'payment_app'@'%';
FLUSH PRIVILEGES;
