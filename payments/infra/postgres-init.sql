-- One Postgres instance, one database per bounded context.
-- "pos" (the shared Transaction DB for cart, checkout, payment and the outbox) is created by the image
-- itself because POSTGRES_USER=pos.
CREATE DATABASE inventory;
CREATE DATABASE loyalty;
CREATE DATABASE assistant;
\connect assistant
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
