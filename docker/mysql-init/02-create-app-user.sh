#!/bin/sh
# Create one application DB user and grant it access to each Inkwell schema.
# MYSQL_ROOT_PASSWORD, MYSQL_APP_USER, and MYSQL_APP_PASSWORD are read from .env.
set -eu

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
CREATE USER IF NOT EXISTS '${MYSQL_APP_USER}'@'%' IDENTIFIED BY '${MYSQL_APP_PASSWORD}';
GRANT ALL PRIVILEGES ON auth_db.* TO '${MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON post_db.* TO '${MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON comment_db.* TO '${MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON category_db.* TO '${MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON media_db.* TO '${MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON newsletter_db.* TO '${MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO '${MYSQL_APP_USER}'@'%';
FLUSH PRIVILEGES;
EOSQL
