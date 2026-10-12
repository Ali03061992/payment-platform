#!/bin/bash
# Init databases + grants for the runtime user (env-driven: works with any
# MYSQL_USER, e.g. payment_app locally or test_user in CI).
# MySQL 8.x forbids creating users via GRANT, and the user name must come
# from the environment (the entrypoint already created $MYSQL_USER).
set -e

DB_USER="${MYSQL_USER:?MYSQL_USER must be set}"

mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<EOSQL
CREATE DATABASE IF NOT EXISTS identity_db;
CREATE DATABASE IF NOT EXISTS organization_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS notification_db;
GRANT ALL PRIVILEGES ON identity_db.* TO '${DB_USER}'@'%';
GRANT ALL PRIVILEGES ON organization_db.* TO '${DB_USER}'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO '${DB_USER}'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO '${DB_USER}'@'%';
FLUSH PRIVILEGES;
EOSQL
