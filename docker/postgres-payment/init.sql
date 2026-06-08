CREATE USER payment_user WITH PASSWORD 'payment_pass';
CREATE DATABASE paymentdb OWNER payment_user;
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO payment_user;
