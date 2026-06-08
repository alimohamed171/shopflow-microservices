CREATE USER order_user WITH PASSWORD 'order_pass';
CREATE DATABASE orderdb OWNER order_user;
GRANT ALL PRIVILEGES ON DATABASE orderdb TO order_user;
